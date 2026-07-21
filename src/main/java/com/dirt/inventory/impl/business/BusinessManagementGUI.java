package com.dirt.inventory.impl.business;

import com.cryptomorin.xseries.XMaterial;
import com.dirt.DirtEconomy;
import com.dirt.data.BusinessData;
import com.dirt.inventory.InventoryButton;
import com.dirt.inventory.InventoryGUI;
import com.dirt.inventory.impl.shops.ShopsAndStockGUI;
import com.dirt.inventory.impl.contract.ContractListGUI;
import com.dirt.util.ItemUtil;
import com.dirt.util.CurrencyUtil;
import com.dirt.util.LText;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.UUID;

public class BusinessManagementGUI extends InventoryGUI {
    private final DirtEconomy plugin;
    private final UUID businessId;

    public BusinessManagementGUI(DirtEconomy plugin, UUID businessId) {
        this.plugin = plugin;
        this.businessId = businessId;
    }

    @Override
    protected Inventory createInventory() {
        BusinessData biz = plugin.getBusinessManager().loadBusiness(businessId);
        String title = biz != null ? "\u00a76" + biz.getName() + " Management" : "\u00a76Business Management";
        return Bukkit.createInventory(null, 27, title);
    }

    @Override
    public void decorate(Player player) {
        fillGlass(27, XMaterial.LIME_STAINED_GLASS_PANE);

        BusinessData biz = plugin.getBusinessManager().loadBusiness(businessId);
        if (biz == null || (!biz.getOwnerUUID().equals(player.getUniqueId()) && biz.getRoleForEmployee(player.getUniqueId()) == null)) {
            addButton(13, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.BARRIER, "\u00a7cAccess Denied"))
                    .consumer(e -> {})
            );
            super.decorate(player);
            return;
        }

        addButton(10, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.NAME_TAG, "\u00a7eName & Description",
                        "\u00a77Change your business name and description.",
                        "\u00a77Cost: \u00a7e" + CurrencyUtil.symbol() + "10,000 \u00a77from business treasury."))
                .consumer(e -> handleRename(biz, (Player) e.getWhoClicked()))
        );

        addButton(12, new InventoryButton()
                .creator(p -> {
                    double balance = plugin.getBusinessManager().getTreasuryBalance(biz.getName());
                    return ItemUtil.buildItem(XMaterial.GOLD_INGOT, "\u00a7eTreasury & Selling",
                            "\u00a77Treasury: \u00a7e" + CurrencyUtil.symbol() + String.format("%.2f", balance),
                            "\u00a77For Sale: \u00a7f" + (biz.isForSale() ? "\u00a7aYes" : "\u00a7cNo"),
                            "\u00a77Manage funds and list for sale.");
                })
                .consumer(e -> plugin.getGUIManager().openGUI(new BusinessTreasuryGUI(plugin, businessId), (Player) e.getWhoClicked()))
        );

        addButton(14, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.PLAYER_HEAD, "\u00a7bEmployees & Payroll",
                        "\u00a77Manage employees and pay rates.",
                        "\u00a77Employees: \u00a7f" + biz.getEmployeeUUIDs().size()))
                .consumer(e -> plugin.getGUIManager().openGUI(new BusinessEmployeesGUI(plugin, businessId), (Player) e.getWhoClicked()))
        );

        addButton(16, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.OAK_DOOR, "\u00a7aBusiness Property",
                        "\u00a77View and purchase business properties.",
                        "\u00a77Properties: \u00a7f" + biz.getPropertyIds().size()))
                .consumer(e -> plugin.getGUIManager().openGUI(new BusinessPropertyGUI(plugin, businessId, 0), (Player) e.getWhoClicked()))
        );

        if (plugin.isDirtShopsEnabled()) {
            addButton(4, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.CHEST, "\u00a7eShops & Stock",
                            "\u00a77Manage shops, stockrooms, and online shop."))
                    .consumer(e -> plugin.getGUIManager().openGUI(new ShopsAndStockGUI(plugin, businessId), (Player) e.getWhoClicked()))
            );
        }

        addButton(20, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.NAME_TAG, "\u00a7dRoles",
                        "\u00a77Create and manage employee roles.",
                        "\u00a77Roles: \u00a7f" + biz.getRoles().size()))
                .consumer(e -> plugin.getGUIManager().openGUI(new BusinessRolesGUI(plugin, businessId), (Player) e.getWhoClicked()))
        );

        if (plugin.isContractsEnabled()) {
            addButton(24, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.WRITABLE_BOOK, "\u00a7eContracts",
                            "\u00a77View business contracts."))
                    .consumer(e -> plugin.getGUIManager().openGUI(
                            new ContractListGUI(plugin, businessId, null, 0), (Player) e.getWhoClicked()))
            );
        }

        addButton(22, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7cBack"))
                .consumer(e -> plugin.getGUIManager().openGUI(new MyBusinessesGUI(plugin, 0), (Player) e.getWhoClicked()))
        );

        super.decorate(player);
    }

    private void handleRename(BusinessData biz, Player player) {
        double balance = plugin.getBusinessManager().getTreasuryBalance(biz.getName());
        if (balance < 10000) {
            player.sendMessage("\u00a7cThe business treasury needs at least " + CurrencyUtil.symbol() + "10,000 to rename. Current balance: " + CurrencyUtil.symbol()
                    + String.format("%.2f", balance));
            return;
        }
        player.closeInventory();
        plugin.getChatInputManager().requestInput(player, "\u00a7eEnter the new name for the business:", newName -> {
            if (newName.trim().isEmpty()) { player.sendMessage("\u00a7cName cannot be empty."); return; }
            if (plugin.getBusinessManager().isNameTaken(newName.trim()) && !newName.trim().equalsIgnoreCase(biz.getName())) {
                player.sendMessage("\u00a7cA business with that name already exists."); return;
            }
            plugin.getChatInputManager().requestInput(player, "\u00a7eEnter the new description:\n\u00a77You can also paste a link to a document (PDF, TXT, etc.).", rawDesc -> {
                LText.handleInput(plugin, player, rawDesc, newDesc -> {
                    plugin.getBusinessManager().withdrawFromTreasury(biz.getName(), 10000);
                    biz.setName(newName.trim());
                    biz.setDescription(newDesc.trim());
                    plugin.getBusinessManager().saveBusiness(biz);
                    player.sendMessage("\u00a7aBusiness renamed to '\u00a76" + newName.trim() + "\u00a7a' for \u00a7e" + CurrencyUtil.symbol() + "10,000\u00a7a.");
                    plugin.getServer().getScheduler().runTask(plugin,
                            () -> plugin.getGUIManager().openGUI(new BusinessManagementGUI(plugin, businessId), player));
                });
            });
        });
    }
}
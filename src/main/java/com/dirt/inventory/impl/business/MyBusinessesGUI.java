package com.dirt.inventory.impl.business;

import com.cryptomorin.xseries.XMaterial;
import com.dirt.DirtEconomy;
import com.dirt.data.BusinessData;
import com.dirt.inventory.InventoryButton;
import com.dirt.inventory.InventoryGUI;
import com.dirt.inventory.impl.CharacterManagementGUI;
import com.dirt.util.ItemUtil;
import com.dirt.util.CurrencyUtil;
import com.dirt.util.LText;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.List;
import java.util.UUID;

public class MyBusinessesGUI extends InventoryGUI {
    private final DirtEconomy plugin;
    private final int page;
    private final UUID targetUuid;
    private final boolean adminMode;

    public MyBusinessesGUI(DirtEconomy plugin, int page) {
        this.plugin = plugin;
        this.page = page;
        this.targetUuid = null;
        this.adminMode = false;
    }

    public MyBusinessesGUI(DirtEconomy plugin, int page, UUID targetUuid) {
        this.plugin = plugin;
        this.page = page;
        this.targetUuid = targetUuid;
        this.adminMode = true;
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 54, adminMode ? "\u00a74Admin: Businesses" : "\u00a76My Businesses");
    }

    @Override
    public void decorate(Player player) {
        UUID effectiveUuid = targetUuid != null ? targetUuid : player.getUniqueId();

        fillPagedGui(54, XMaterial.LIME_STAINED_GLASS_PANE);

        List<BusinessData> businesses = plugin.getBusinessManager().getBusinessesByOwner(effectiveUuid);
        int perPage = 44;
        int start = page * perPage;
        int end = Math.min(start + perPage, businesses.size());

        for (int i = start; i < end; i++) {
            BusinessData biz = businesses.get(i);
            int slot = i - start;
            addButton(slot, new InventoryButton()
                    .creator(p -> {
                        double balance = plugin.getBusinessManager().getTreasuryBalance(biz.getName());
                        return ItemUtil.buildItem(XMaterial.EMERALD, "\u00a7a" + biz.getName(),
                                "\u00a77" + biz.getDescription(),
                                "\u00a77Employees: \u00a7f" + biz.getEmployeeUUIDs().size(),
                                "\u00a77Treasury: \u00a7e" + CurrencyUtil.symbol() + String.format("%.2f", balance),
                                "\u00a77Hiring: \u00a7f" + (biz.isHiring() ? "\u00a7aYes" : "\u00a7cNo"),
                                "\u00a77Click to manage.");
                    })
                    .consumer(e -> plugin.getGUIManager().openGUI(
                            new BusinessManagementGUI(plugin, biz.getBusinessId()),
                            (Player) e.getWhoClicked()))
            );
        }

        if (page > 0) {
            int prev = page - 1;
            addButton(45, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7ePrevious"))
                    .consumer(e -> {
                        Player p = (Player) e.getWhoClicked();
                        if (adminMode) plugin.getGUIManager().openGUI(new MyBusinessesGUI(plugin, prev, effectiveUuid), p);
                        else plugin.getGUIManager().openGUI(new MyBusinessesGUI(plugin, prev), p);
                    })
            );
        }
        if (end < businesses.size()) {
            int next = page + 1;
            addButton(53, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7eNext"))
                    .consumer(e -> {
                        Player p = (Player) e.getWhoClicked();
                        if (adminMode) plugin.getGUIManager().openGUI(new MyBusinessesGUI(plugin, next, effectiveUuid), p);
                        else plugin.getGUIManager().openGUI(new MyBusinessesGUI(plugin, next), p);
                    })
            );
        }

        if (!adminMode) {
            addButton(46, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.NETHER_STAR, "\u00a7aCreate New Business",
                            "\u00a77Start a new business.",
                            "\u00a77Cost: \u00a7e" + CurrencyUtil.symbol() + "1,000"))
                    .consumer(e -> startBusinessCreation((Player) e.getWhoClicked()))
            );
        }

        addButton(49, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7cBack"))
                .consumer(e -> {
                    Player p = (Player) e.getWhoClicked();
                    if (adminMode) plugin.getGUIManager().openGUI(new CharacterManagementGUI(plugin, effectiveUuid), p);
                    else plugin.getGUIManager().openGUI(new CharacterManagementGUI(plugin), p);
                })
        );

        super.decorate(player);
    }

    private void startBusinessCreation(Player player) {
        player.closeInventory();
        plugin.getChatInputManager().requestInput(player, "\u00a7eEnter the name for your new business:", name -> {
            if (name.trim().isEmpty()) { player.sendMessage("\u00a7cBusiness name cannot be empty."); return; }
            if (plugin.getBusinessManager().isNameTaken(name.trim())) {
                player.sendMessage("\u00a7cA business with that name already exists.");
                return;
            }
            plugin.getChatInputManager().requestInput(player, "\u00a7eEnter a description for \u00a76" + name.trim() + "\u00a7e:\n\u00a77You can also paste a link to a document (PDF, TXT, etc.).", rawDesc -> {
              LText.handleInput(plugin, player, rawDesc, description -> {
                plugin.getChatInputManager().requestInput(player, "\u00a7eEnter the default weekly payroll rate:", rateStr -> {
                    double rate;
                    try { rate = Double.parseDouble(rateStr.trim()); } catch (NumberFormatException ex) {
                        player.sendMessage("\u00a7cInvalid rate."); return;
                    }
                    if (rate < 0) { player.sendMessage("\u00a7cRate cannot be negative."); return; }
                    plugin.getChatInputManager().requestInput(player,
                            "\u00a7eConfirm creation of '\u00a76" + name.trim() + "\u00a7e' for \u00a7e" + CurrencyUtil.symbol() + "1,000\u00a7e? Type \u00a76yes\u00a7e to confirm:",
                            confirm -> {
                                if (!confirm.equalsIgnoreCase("yes")) {
                                    player.sendMessage("\u00a7cBusiness creation cancelled.");
                                    return;
                                }
                                if (!plugin.getEconomy().has(player, 1000)) {
                                    player.sendMessage("\u00a7cYou cannot afford the " + CurrencyUtil.symbol() + "1,000 creation fee.");
                                    return;
                                }
                                plugin.getEconomy().withdrawPlayer(player, 1000);
                                BusinessData biz = plugin.getBusinessManager().createBusiness(
                                        name.trim(), description.trim(), player.getUniqueId(), rate);
                                player.sendMessage("\u00a7aBusiness '\u00a76" + biz.getName() + "\u00a7a' created successfully!");
                                plugin.getServer().getScheduler().runTask(plugin,
                                        () -> plugin.getGUIManager().openGUI(new MyBusinessesGUI(plugin, 0), player));
                            });
                });
              });
            });
        });
    }
}
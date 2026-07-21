package com.dirt.inventory.impl.business;

import com.cryptomorin.xseries.XMaterial;
import com.dirt.DirtEconomy;
import com.dirt.data.BusinessData;
import com.dirt.inventory.InventoryButton;
import com.dirt.inventory.InventoryGUI;
import com.dirt.util.ItemUtil;
import com.dirt.util.CurrencyUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.UUID;

public class BusinessTreasuryGUI extends InventoryGUI {
    private final DirtEconomy plugin;
    private final UUID businessId;

    public BusinessTreasuryGUI(DirtEconomy plugin, UUID businessId) {
        this.plugin = plugin;
        this.businessId = businessId;
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 27, "\u00a7eTreasury & Selling");
    }

    @Override
    public void decorate(Player player) {
        fillGlass(27, XMaterial.LIME_STAINED_GLASS_PANE);

        BusinessData biz = plugin.getBusinessManager().loadBusiness(businessId);
        if (biz == null || !biz.getOwnerUUID().equals(player.getUniqueId())) {
            super.decorate(player); return;
        }

        double balance = plugin.getBusinessManager().getTreasuryBalance(biz.getName());

        addButton(4, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.GOLD_BLOCK, "\u00a7eTreasury Balance",
                        "\u00a7e" + CurrencyUtil.symbol() + String.format("%.2f", balance),
                        "\u00a77Total Earned: \u00a7e" + CurrencyUtil.symbol() + String.format("%.2f", biz.getTotalEarned())))
                .consumer(e -> {})
        );

        addButton(10, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.LIME_WOOL, "\u00a7aDeposit",
                        "\u00a77Transfer money from your wallet to the treasury."))
                .consumer(e -> handleDeposit(biz, (Player) e.getWhoClicked()))
        );

        addButton(12, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.RED_WOOL, "\u00a7cWithdraw",
                        "\u00a77Transfer money from the treasury to your wallet."))
                .consumer(e -> handleWithdraw(biz, (Player) e.getWhoClicked()))
        );

        addButton(14, new InventoryButton()
                .creator(p -> {
                    if (biz.isForSale()) {
                        return ItemUtil.buildItem(XMaterial.ORANGE_WOOL, "\u00a76For Sale",
                                "\u00a77Asking Price: \u00a7e" + CurrencyUtil.symbol() + String.format("%.2f", biz.getSalePrice()),
                                "\u00a77Click to take the business off sale.");
                    }
                    return ItemUtil.buildItem(XMaterial.EMERALD, "\u00a7eSell Business",
                            "\u00a77List this business for sale on /db view.");
                })
                .consumer(e -> handleSell(biz, (Player) e.getWhoClicked()))
        );

        addButton(22, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7cBack"))
                .consumer(e -> plugin.getGUIManager().openGUI(new BusinessManagementGUI(plugin, businessId), (Player) e.getWhoClicked()))
        );

        super.decorate(player);
    }

    private void handleDeposit(BusinessData biz, Player player) {
        player.closeInventory();
        plugin.getChatInputManager().requestInput(player,
                "\u00a7eEnter the amount to deposit into the \u00a76" + biz.getName() + "\u00a7e treasury:",
                amtStr -> {
                    double amount;
                    try { amount = Double.parseDouble(amtStr.trim()); } catch (NumberFormatException ex) {
                        player.sendMessage("\u00a7cInvalid amount."); return;
                    }
                    if (amount <= 0) { player.sendMessage("\u00a7cAmount must be greater than zero."); return; }
                    if (!plugin.getEconomy().has(player, amount)) {
                        player.sendMessage("\u00a7cYou don't have " + CurrencyUtil.symbol() + String.format("%.2f", amount) + "."); return;
                    }
                    plugin.getEconomy().withdrawPlayer(player, amount);
                    plugin.getBusinessManager().depositToTreasury(biz, amount);
                    player.sendMessage("\u00a7aDeposited \u00a7e" + CurrencyUtil.symbol() + String.format("%.2f", amount) + "\u00a7a into " + biz.getName() + " treasury.");
                    plugin.getServer().getScheduler().runTask(plugin,
                            () -> plugin.getGUIManager().openGUI(new BusinessTreasuryGUI(plugin, businessId), player));
                });
    }

    private void handleWithdraw(BusinessData biz, Player player) {
        player.closeInventory();
        double balance = plugin.getBusinessManager().getTreasuryBalance(biz.getName());
        plugin.getChatInputManager().requestInput(player,
                "\u00a7eEnter the amount to withdraw (available: \u00a7e" + CurrencyUtil.symbol() + String.format("%.2f", balance) + "\u00a7e):",
                amtStr -> {
                    double amount;
                    try { amount = Double.parseDouble(amtStr.trim()); } catch (NumberFormatException ex) {
                        player.sendMessage("\u00a7cInvalid amount."); return;
                    }
                    if (amount <= 0) { player.sendMessage("\u00a7cAmount must be greater than zero."); return; }
                    if (balance < amount) {
                        player.sendMessage("\u00a7cInsufficient treasury funds."); return;
                    }
                    plugin.getBusinessManager().withdrawFromTreasury(biz.getName(), amount);
                    plugin.getEconomy().depositPlayer(player, amount);
                    player.sendMessage("\u00a7aWithdrew \u00a7e" + CurrencyUtil.symbol() + String.format("%.2f", amount) + "\u00a7a from " + biz.getName() + " treasury.");
                    plugin.getServer().getScheduler().runTask(plugin,
                            () -> plugin.getGUIManager().openGUI(new BusinessTreasuryGUI(plugin, businessId), player));
                });
    }

    private void handleSell(BusinessData biz, Player player) {
        if (biz.isForSale()) {
            biz.setForSale(false);
            biz.setSalePrice(0.0);
            plugin.getBusinessManager().saveBusiness(biz);
            player.sendMessage("\u00a7a" + biz.getName() + " has been taken off sale.");
            plugin.getServer().getScheduler().runTask(plugin,
                    () -> plugin.getGUIManager().openGUI(new BusinessTreasuryGUI(plugin, businessId), player));
            return;
        }
        player.closeInventory();
        plugin.getChatInputManager().requestInput(player,
                "\u00a7eEnter the price you want to sell \u00a76" + biz.getName() + "\u00a7e for:",
                priceStr -> {
                    double price;
                    try { price = Double.parseDouble(priceStr.trim()); } catch (NumberFormatException ex) {
                        player.sendMessage("\u00a7cInvalid price."); return;
                    }
                    if (price <= 0) { player.sendMessage("\u00a7cPrice must be greater than zero."); return; }
                    biz.setForSale(true);
                    biz.setSalePrice(price);
                    plugin.getBusinessManager().saveBusiness(biz);
                    player.sendMessage("\u00a7a" + biz.getName() + " is now listed for sale at \u00a7e" + CurrencyUtil.symbol() + String.format("%.2f", price) + "\u00a7a on /db view.");
                    plugin.getServer().getScheduler().runTask(plugin,
                            () -> plugin.getGUIManager().openGUI(new BusinessTreasuryGUI(plugin, businessId), player));
                });
    }
}
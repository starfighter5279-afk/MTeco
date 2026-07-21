package com.dirt.inventory.impl.shops;

import com.cryptomorin.xseries.XMaterial;
import com.dirt.DirtEconomy;
import com.dirt.data.BusinessData;
import com.dirt.data.ShopData;
import com.dirt.data.StockroomData;
import com.dirt.inventory.InventoryButton;
import com.dirt.inventory.InventoryGUI;
import com.dirt.managers.ShopManager;
import com.dirt.util.ItemUtil;
import com.dirt.util.CurrencyUtil;
import org.bukkit.Bukkit;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class OnlineShopBrowseGUI extends InventoryGUI {
    private final DirtEconomy plugin;
    private final UUID businessId;
    private final int page;

    public OnlineShopBrowseGUI(DirtEconomy plugin, UUID businessId, int page) {
        this.plugin = plugin;
        this.businessId = businessId;
        this.page = page;
    }

    @Override
    protected Inventory createInventory() {
        BusinessData biz = plugin.getBusinessManager().loadBusiness(businessId);
        String title = biz != null ? "\u00a7d" + biz.getName() + " Online" : "\u00a7dOnline Shop";
        return Bukkit.createInventory(null, 54, title);
    }

    @Override
    public void decorate(Player player) {
        fillPagedGui(54, XMaterial.PURPLE_STAINED_GLASS_PANE);
        BusinessData biz = plugin.getBusinessManager().loadBusiness(businessId);
        if (biz == null) { super.decorate(player); return; }

        // Aggregate items across all stockroom chests
        Map<String, OnlineEntry> entries = new HashMap<>();

        // Pre-build a map of itemKey -> pricePerUnit from existing physical shops belonging to this business
        Map<String, Double> priceMap = new HashMap<>();
        for (ShopData s : plugin.getShopManager().getShopsByBusiness(businessId)) {
            if (s.getItemTemplate() == null) continue;
            ItemStack key = s.getItemTemplate().clone();
            key.setAmount(1);
            priceMap.putIfAbsent(key.toString(), s.getPricePerUnit());
        }

        for (StockroomData sr : plugin.getShopManager().getStockroomsByBusiness(businessId)) {
            for (Chest chest : plugin.getShopManager().getStockChests(sr)) {
                for (ItemStack stack : chest.getInventory().getContents()) {
                    if (stack == null) continue;
                    ItemStack template = stack.clone();
                    template.setAmount(1);
                    String key = template.toString();
                    Double price = priceMap.get(key);
                    if (price == null) continue; // not sold by any shop of this business
                    OnlineEntry e = entries.get(key);
                    if (e == null) {
                        e = new OnlineEntry(template, price);
                        entries.put(key, e);
                    }
                    e.total += stack.getAmount();
                }
            }
        }

        List<OnlineEntry> list = new ArrayList<>(entries.values());
        int perPage = 45;
        int start = page * perPage;
        int end = Math.min(start + perPage, list.size());

        for (int i = start; i < end; i++) {
            OnlineEntry entry = list.get(i);
            int slot = i - start;
            ItemStack icon = entry.template.clone();
            ItemMeta meta = icon.getItemMeta();
            if (meta != null) {
                meta.setLore(Arrays.asList(
                        "\u00a77Price: \u00a7e" + CurrencyUtil.symbol() + String.format("%.2f", entry.pricePerUnit),
                        "\u00a77Available: \u00a7f" + entry.total,
                        "\u00a77Click to purchase."
                ));
                icon.setItemMeta(meta);
            }
            final ItemStack finalIcon = icon;
            addButton(slot, new InventoryButton()
                    .creator(p -> finalIcon)
                    .consumer(e -> promptPurchase((Player) e.getWhoClicked(), entry))
            );
        }

        if (page > 0) {
            int prev = page - 1;
            addButton(45, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7ePrevious"))
                    .consumer(e -> plugin.getGUIManager().openGUI(new OnlineShopBrowseGUI(plugin, businessId, prev), (Player) e.getWhoClicked()))
            );
        }
        if (end < list.size()) {
            int next = page + 1;
            addButton(53, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7eNext"))
                    .consumer(e -> plugin.getGUIManager().openGUI(new OnlineShopBrowseGUI(plugin, businessId, next), (Player) e.getWhoClicked()))
            );
        }
        addButton(49, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7cBack"))
                .consumer(e -> plugin.getGUIManager().openGUI(new OnlineShopsListGUI(plugin, 0), (Player) e.getWhoClicked()))
        );
        super.decorate(player);
    }

    private void promptPurchase(Player player, OnlineEntry entry) {
        String name = ShopManager.friendlyName(entry.template);
        plugin.getChatInputManager().requestInput(player,
                "\u00a7eHow many \u00a76" + name + "\u00a7e would you like to purchase? (available: \u00a76"
                        + entry.total + "\u00a7e, \u00a7e" + CurrencyUtil.symbol() + String.format("%.2f", entry.pricePerUnit) + " each)",
                qtyStr -> {
                    int qty;
                    try { qty = Integer.parseInt(qtyStr.trim()); }
                    catch (NumberFormatException ex) { player.sendMessage("\u00a7cInvalid quantity."); return; }
                    if (qty <= 0) { player.sendMessage("\u00a7cQuantity must be positive."); return; }

                    int currentStock = plugin.getShopManager().countBusinessStock(businessId, entry.template);
                    if (qty > currentStock) {
                        player.sendMessage("\u00a7cOnly \u00a76" + currentStock + "\u00a7c units are available.");
                        return;
                    }
                    double totalCost = entry.pricePerUnit * qty;
                    if (!plugin.getEconomy().has(player, totalCost)) {
                        player.sendMessage("\u00a7cYou cannot afford \u00a7e" + CurrencyUtil.symbol() + String.format("%.2f", totalCost) + "\u00a7c.");
                        return;
                    }
                    int pulled = plugin.getShopManager().pullFromBusinessStock(businessId, entry.template, qty);
                    if (pulled < qty) {
                        // restore anything pulled (shouldn't happen due to check, but be safe)
                        player.sendMessage("\u00a7cFailed to pull stock from the online shop.");
                        return;
                    }
                    plugin.getEconomy().withdrawPlayer(player, totalCost);
                    BusinessData biz = plugin.getBusinessManager().loadBusiness(businessId);
                    if (biz != null) plugin.getBusinessManager().depositToTreasury(biz, totalCost);

                    ItemStack toGive = entry.template.clone();
                    toGive.setAmount(qty);
                    Map<Integer, ItemStack> overflow = player.getInventory().addItem(toGive);
                    for (ItemStack drop : overflow.values()) {
                        player.getWorld().dropItemNaturally(player.getLocation(), drop);
                    }
                    player.sendMessage("\u00a7aPurchased \u00a76" + qty + "x " + name
                            + "\u00a7a for \u00a7e" + CurrencyUtil.symbol() + String.format("%.2f", totalCost) + "\u00a7a.");
                });
    }

    private static class OnlineEntry {
        final ItemStack template;
        final double pricePerUnit;
        int total = 0;
        OnlineEntry(ItemStack template, double pricePerUnit) {
            this.template = template;
            this.pricePerUnit = pricePerUnit;
        }
    }
}
package com.dirt.inventory.impl.shops;

import com.cryptomorin.xseries.XMaterial;
import com.dirt.DirtEconomy;
import com.dirt.data.ShopData;
import com.dirt.inventory.InventoryButton;
import com.dirt.inventory.InventoryGUI;
import com.dirt.managers.ShopManager;
import com.dirt.util.ItemUtil;
import com.dirt.util.CurrencyUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class BusinessCurrentShopsGUI extends InventoryGUI {
    private final DirtEconomy plugin;
    private final UUID businessId;
    private final int page;

    public BusinessCurrentShopsGUI(DirtEconomy plugin, UUID businessId, int page) {
        this.plugin = plugin;
        this.businessId = businessId;
        this.page = page;
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 54, "\u00a7eCurrent Shops");
    }

    @Override
    public void decorate(Player player) {
        fillPagedGui(54, XMaterial.PURPLE_STAINED_GLASS_PANE);
        List<ShopData> shops = plugin.getShopManager().getShopsByBusiness(businessId);
        int perPage = 45;
        int start = page * perPage;
        int end = Math.min(start + perPage, shops.size());

        for (int i = start; i < end; i++) {
            ShopData shop = shops.get(i);
            int slot = i - start;
            ItemStack template = shop.getItemTemplate();
            int stock = plugin.getShopManager().countShopStock(shop);
            double monthly = plugin.getShopManager().getMonthlyProfit(shop);
            Location chestLoc = ShopManager.parseLoc(shop.getChestLocation());

            ItemStack icon = template != null ? template.clone() : new ItemStack(org.bukkit.Material.CHEST);
            ItemMeta meta = icon.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("\u00a7a" + ShopManager.friendlyName(template != null ? template : icon));
                meta.setLore(Arrays.asList(
                        "\u00a77Price: \u00a7e" + CurrencyUtil.symbol() + String.format("%.2f", shop.getPricePerUnit()),
                        "\u00a77Stock: \u00a7f" + stock,
                        "\u00a77Monthly Profit: \u00a7a" + CurrencyUtil.symbol() + String.format("%.2f", monthly),
                        chestLoc != null ? "\u00a77At: \u00a7f" + chestLoc.getWorld().getName()
                                + " " + chestLoc.getBlockX() + ", " + chestLoc.getBlockY() + ", " + chestLoc.getBlockZ()
                                : "\u00a77Location unknown"
                ));
                icon.setItemMeta(meta);
            }
            final ItemStack finalIcon = icon;
            addButton(slot, new InventoryButton().creator(p -> finalIcon).consumer(e -> {}));
        }

        if (page > 0) {
            int prev = page - 1;
            addButton(45, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7ePrevious"))
                    .consumer(e -> plugin.getGUIManager().openGUI(new BusinessCurrentShopsGUI(plugin, businessId, prev), (Player) e.getWhoClicked()))
            );
        }
        if (end < shops.size()) {
            int next = page + 1;
            addButton(53, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7eNext"))
                    .consumer(e -> plugin.getGUIManager().openGUI(new BusinessCurrentShopsGUI(plugin, businessId, next), (Player) e.getWhoClicked()))
            );
        }
        addButton(49, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7cBack"))
                .consumer(e -> plugin.getGUIManager().openGUI(new BusinessShopsGUI(plugin, businessId), (Player) e.getWhoClicked()))
        );
        super.decorate(player);
    }
}
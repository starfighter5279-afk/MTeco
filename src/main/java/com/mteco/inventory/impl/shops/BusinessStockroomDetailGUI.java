package com.mteco.inventory.impl.shops;

import com.cryptomorin.xseries.XMaterial;
import com.mteco.MTeco;
import com.mteco.data.StockroomData;
import com.mteco.inventory.InventoryButton;
import com.mteco.inventory.InventoryGUI;
import com.mteco.util.ItemUtil;
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

public class BusinessStockroomDetailGUI extends InventoryGUI {
    private final MTeco plugin;
    private final UUID businessId;
    private final UUID stockroomId;
    private final int page;

    public BusinessStockroomDetailGUI(MTeco plugin, UUID businessId, UUID stockroomId, int page) {
        this.plugin = plugin;
        this.businessId = businessId;
        this.stockroomId = stockroomId;
        this.page = page;
    }

    @Override
    protected Inventory createInventory() {
        StockroomData sr = plugin.getShopManager().loadStockroom(stockroomId);
        String title = sr != null ? "\u00a7a" + sr.getName() : "\u00a7aStockroom";
        return Bukkit.createInventory(null, 54, title);
    }

    @Override
    public void decorate(Player player) {
        fillPagedGui(54, XMaterial.PURPLE_STAINED_GLASS_PANE);
        StockroomData sr = plugin.getShopManager().loadStockroom(stockroomId);
        if (sr == null) { super.decorate(player); return; }

        // Aggregate items across all stock chests
        Map<String, ItemAggregate> aggregates = new HashMap<>();
        for (Chest chest : plugin.getShopManager().getStockChests(sr)) {
            for (ItemStack stack : chest.getInventory().getContents()) {
                if (stack == null) continue;
                ItemStack template = stack.clone();
                template.setAmount(1);
                String key = template.toString();
                ItemAggregate agg = aggregates.get(key);
                if (agg == null) {
                    agg = new ItemAggregate(template);
                    aggregates.put(key, agg);
                }
                agg.total += stack.getAmount();
            }
        }
        List<ItemAggregate> list = new ArrayList<>(aggregates.values());

        int perPage = 45;
        int start = page * perPage;
        int end = Math.min(start + perPage, list.size());

        for (int i = start; i < end; i++) {
            ItemAggregate agg = list.get(i);
            int slot = i - start;
            ItemStack icon = agg.template.clone();
            ItemMeta meta = icon.getItemMeta();
            if (meta != null) {
                List<String> lore = new ArrayList<>(Arrays.asList("\u00a77Total in stock: \u00a7e" + agg.total));
                meta.setLore(lore);
                icon.setItemMeta(meta);
            }
            final ItemStack finalIcon = icon;
            addButton(slot, new InventoryButton().creator(p -> finalIcon).consumer(e -> {}));
        }

        if (page > 0) {
            int prev = page - 1;
            addButton(45, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7ePrevious"))
                    .consumer(e -> plugin.getGUIManager().openGUI(new BusinessStockroomDetailGUI(plugin, businessId, stockroomId, prev), (Player) e.getWhoClicked()))
            );
        }
        if (end < list.size()) {
            int next = page + 1;
            addButton(53, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7eNext"))
                    .consumer(e -> plugin.getGUIManager().openGUI(new BusinessStockroomDetailGUI(plugin, businessId, stockroomId, next), (Player) e.getWhoClicked()))
            );
        }
        addButton(49, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7cBack"))
                .consumer(e -> plugin.getGUIManager().openGUI(new BusinessStockroomsListGUI(plugin, businessId, 0), (Player) e.getWhoClicked()))
        );
        super.decorate(player);
    }

    private static class ItemAggregate {
        final ItemStack template;
        int total = 0;
        ItemAggregate(ItemStack template) { this.template = template; }
    }
}
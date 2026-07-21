package com.dirt.inventory.impl.shops;

import com.cryptomorin.xseries.XMaterial;
import com.dirt.DirtEconomy;
import com.dirt.data.StockroomData;
import com.dirt.inventory.InventoryButton;
import com.dirt.inventory.InventoryGUI;
import com.dirt.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.List;
import java.util.UUID;

public class BusinessStockroomsListGUI extends InventoryGUI {
    private final DirtEconomy plugin;
    private final UUID businessId;
    private final int page;

    public BusinessStockroomsListGUI(DirtEconomy plugin, UUID businessId, int page) {
        this.plugin = plugin;
        this.businessId = businessId;
        this.page = page;
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 54, "\u00a7aCurrent Stockrooms");
    }

    @Override
    public void decorate(Player player) {
        fillPagedGui(54, XMaterial.PURPLE_STAINED_GLASS_PANE);
        List<StockroomData> stockrooms = plugin.getShopManager().getStockroomsByBusiness(businessId);
        int perPage = 45;
        int start = page * perPage;
        int end = Math.min(start + perPage, stockrooms.size());

        for (int i = start; i < end; i++) {
            StockroomData sr = stockrooms.get(i);
            int slot = i - start;
            int chests = plugin.getShopManager().getStockChests(sr).size();
            addButton(slot, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.BARREL, "\u00a7a" + sr.getName(),
                            "\u00a77World: \u00a7f" + sr.getWorld(),
                            "\u00a77Stock Chests: \u00a7f" + chests,
                            "\u00a77Click to view items.",
                            "\u00a7c\u00a7lShift-click or double-click to delete."))
                    .consumer(e -> {
                        if (e.isShiftClick() || e.getClick() == org.bukkit.event.inventory.ClickType.DOUBLE_CLICK) {
                            plugin.getShopManager().deleteStockroom(sr.getStockroomId());
                            ((Player) e.getWhoClicked()).sendMessage("\u00a7eStockroom '" + sr.getName() + "' deleted.");
                            plugin.getGUIManager().openGUI(new BusinessStockroomsListGUI(plugin, businessId, page), (Player) e.getWhoClicked());
                        } else {
                            plugin.getGUIManager().openGUI(new BusinessStockroomDetailGUI(plugin, businessId, sr.getStockroomId(), 0), (Player) e.getWhoClicked());
                        }
                    })
            );
        }

        if (page > 0) {
            int prev = page - 1;
            addButton(45, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7ePrevious"))
                    .consumer(e -> plugin.getGUIManager().openGUI(new BusinessStockroomsListGUI(plugin, businessId, prev), (Player) e.getWhoClicked()))
            );
        }
        if (end < stockrooms.size()) {
            int next = page + 1;
            addButton(53, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7eNext"))
                    .consumer(e -> plugin.getGUIManager().openGUI(new BusinessStockroomsListGUI(plugin, businessId, next), (Player) e.getWhoClicked()))
            );
        }
        addButton(49, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7cBack"))
                .consumer(e -> plugin.getGUIManager().openGUI(new BusinessStockManagementGUI(plugin, businessId), (Player) e.getWhoClicked()))
        );
        super.decorate(player);
    }
}
package com.dirt.inventory.impl.shops;

import com.cryptomorin.xseries.XMaterial;
import com.dirt.DirtEconomy;
import com.dirt.data.BusinessData;
import com.dirt.inventory.InventoryButton;
import com.dirt.inventory.InventoryGUI;
import com.dirt.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class OnlineShopsListGUI extends InventoryGUI {
    private final DirtEconomy plugin;
    private final int page;

    public OnlineShopsListGUI(DirtEconomy plugin, int page) {
        this.plugin = plugin;
        this.page = page;
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 54, "\u00a7dOnline Shops");
    }

    @Override
    public void decorate(Player player) {
        fillPagedGui(54, XMaterial.PURPLE_STAINED_GLASS_PANE);

        List<UUID> ids = plugin.getShopManager().getOnlineShopBusinessIds();
        List<BusinessData> businesses = new ArrayList<>();
        for (UUID id : ids) {
            BusinessData biz = plugin.getBusinessManager().loadBusiness(id);
            if (biz != null) businesses.add(biz);
        }

        int perPage = 45;
        int start = page * perPage;
        int end = Math.min(start + perPage, businesses.size());

        for (int i = start; i < end; i++) {
            BusinessData biz = businesses.get(i);
            int slot = i - start;
            addButton(slot, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.EMERALD, "\u00a7a" + biz.getName(),
                            "\u00a77" + biz.getDescription(),
                            "\u00a77Click to browse items."))
                    .consumer(e -> plugin.getGUIManager().openGUI(new OnlineShopBrowseGUI(plugin, biz.getBusinessId(), 0), (Player) e.getWhoClicked()))
            );
        }

        if (page > 0) {
            int prev = page - 1;
            addButton(45, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7ePrevious"))
                    .consumer(e -> plugin.getGUIManager().openGUI(new OnlineShopsListGUI(plugin, prev), (Player) e.getWhoClicked()))
            );
        }
        if (end < businesses.size()) {
            int next = page + 1;
            addButton(53, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7eNext"))
                    .consumer(e -> plugin.getGUIManager().openGUI(new OnlineShopsListGUI(plugin, next), (Player) e.getWhoClicked()))
            );
        }
        super.decorate(player);
    }
}
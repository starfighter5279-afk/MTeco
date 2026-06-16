package com.mteco.inventory.impl.business;

import com.cryptomorin.xseries.XMaterial;
import com.mteco.MTeco;
import com.mteco.data.BusinessData;
import com.mteco.data.BusinessPropertyData;
import com.mteco.inventory.InventoryButton;
import com.mteco.inventory.InventoryGUI;
import com.mteco.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.List;
import java.util.UUID;

public class BusinessPropertyGUI extends InventoryGUI {
    private final MTeco plugin;
    private final UUID businessId;
    private final int page;

    public BusinessPropertyGUI(MTeco plugin, UUID businessId, int page) {
        this.plugin = plugin;
        this.businessId = businessId;
        this.page = page;
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 54, "\u00a7aBusiness Properties");
    }

    @Override
    public void decorate(Player player) {
        fillPagedGui(54, XMaterial.LIME_STAINED_GLASS_PANE);

        BusinessData biz = plugin.getBusinessManager().loadBusiness(businessId);
        if (biz == null) { super.decorate(player); return; }

        List<UUID> propIds = biz.getPropertyIds();
        int perPage = 44;
        int start = page * perPage;
        int end = Math.min(start + perPage, propIds.size());

        for (int i = start; i < end; i++) {
            BusinessPropertyData prop = plugin.getBusinessManager().loadProperty(propIds.get(i));
            if (prop == null) continue;
            int slot = i - start;
            addButton(slot, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.OAK_DOOR, "\u00a7a" + prop.getName(),
                            "\u00a77Chunks: \u00a7f" + prop.getChunks().size(),
                            "\u00a77Click to manage."))
                    .consumer(e -> plugin.getGUIManager().openGUI(
                            new BusinessPropertyDetailGUI(plugin, businessId, prop.getPropertyId()), (Player) e.getWhoClicked()))
            );
        }

        if (page > 0) {
            int prev = page - 1;
            addButton(45, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7ePrevious"))
                    .consumer(e -> plugin.getGUIManager().openGUI(new BusinessPropertyGUI(plugin, businessId, prev), (Player) e.getWhoClicked()))
            );
        }
        if (end < propIds.size()) {
            int next = page + 1;
            addButton(53, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7eNext"))
                    .consumer(e -> plugin.getGUIManager().openGUI(new BusinessPropertyGUI(plugin, businessId, next), (Player) e.getWhoClicked()))
            );
        }

        addButton(46, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.EMERALD, "\u00a7aPurchase Property",
                        "\u00a77Select chunks within a nation region.",
                        "\u00a77Charged from business treasury."))
                .consumer(e -> {
                    Player p = (Player) e.getWhoClicked();
                    p.closeInventory();
                    plugin.getChunkSelectionManager().startBusinessPropertySelection(p, businessId);
                })
        );

        addButton(49, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7cBack"))
                .consumer(e -> plugin.getGUIManager().openGUI(new BusinessManagementGUI(plugin, businessId), (Player) e.getWhoClicked()))
        );

        super.decorate(player);
    }
}
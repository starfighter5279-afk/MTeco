package com.dirt.inventory.impl.shops;

import com.cryptomorin.xseries.XMaterial;
import com.dirt.DirtEconomy;
import com.dirt.data.BusinessData;
import com.dirt.inventory.InventoryButton;
import com.dirt.inventory.InventoryGUI;
import com.dirt.inventory.impl.business.BusinessManagementGUI;
import com.dirt.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.UUID;

public class ShopsAndStockGUI extends InventoryGUI {
    private final DirtEconomy plugin;
    private final UUID businessId;

    public ShopsAndStockGUI(DirtEconomy plugin, UUID businessId) {
        this.plugin = plugin;
        this.businessId = businessId;
    }

    @Override
    protected Inventory createInventory() {
        BusinessData biz = plugin.getBusinessManager().loadBusiness(businessId);
        String title = biz != null ? "\u00a76" + biz.getName() + " Shop & Stock" : "\u00a76Shop & Stock";
        return Bukkit.createInventory(null, 27, title);
    }

    @Override
    public void decorate(Player player) {
        fillGlass(27, XMaterial.PURPLE_STAINED_GLASS_PANE);
        addButton(11, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.CHEST, "\u00a7eManage Shops",
                        "\u00a77View current shops & create new ones."))
                .consumer(e -> plugin.getGUIManager().openGUI(new BusinessShopsGUI(plugin, businessId), (Player) e.getWhoClicked()))
        );
        if (plugin.isOnlineShopsEnabled()) {
            addButton(13, new InventoryButton()
                    .creator(p -> {
                        boolean online = plugin.getShopManager().isOnlineShopEnabled(businessId);
                        return ItemUtil.buildItem(XMaterial.ENDER_CHEST, "\u00a7dManage Online Shop",
                                "\u00a77Online Shop: " + (online ? "\u00a7aEnabled" : "\u00a7cDisabled"));
                    })
                    .consumer(e -> plugin.getGUIManager().openGUI(new BusinessOnlineShopGUI(plugin, businessId), (Player) e.getWhoClicked()))
            );
        }
        addButton(15, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.BARREL, "\u00a7aStockroom",
                        "\u00a77Manage stockrooms and stock chests."))
                .consumer(e -> plugin.getGUIManager().openGUI(new BusinessStockManagementGUI(plugin, businessId), (Player) e.getWhoClicked()))
        );
        addButton(22, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7cBack"))
                .consumer(e -> plugin.getGUIManager().openGUI(new BusinessManagementGUI(plugin, businessId), (Player) e.getWhoClicked()))
        );
        super.decorate(player);
    }
}
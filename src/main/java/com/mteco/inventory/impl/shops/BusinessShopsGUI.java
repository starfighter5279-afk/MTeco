package com.mteco.inventory.impl.shops;

import com.cryptomorin.xseries.XMaterial;
import com.mteco.MTeco;
import com.mteco.inventory.InventoryButton;
import com.mteco.inventory.InventoryGUI;
import com.mteco.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.UUID;

public class BusinessShopsGUI extends InventoryGUI {
    private final MTeco plugin;
    private final UUID businessId;

    public BusinessShopsGUI(MTeco plugin, UUID businessId) {
        this.plugin = plugin;
        this.businessId = businessId;
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 27, "\u00a7eShops");
    }

    @Override
    public void decorate(Player player) {
        fillGlass(27, XMaterial.PURPLE_STAINED_GLASS_PANE);
        addButton(11, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.CHEST, "\u00a7eCurrent Shops",
                        "\u00a77View every shop under this business."))
                .consumer(e -> plugin.getGUIManager().openGUI(new BusinessCurrentShopsGUI(plugin, businessId, 0), (Player) e.getWhoClicked()))
        );
        addButton(13, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.EMERALD, "\u00a7aCreate New Shop",
                        "\u00a77Place a chest, then left-click it",
                        "\u00a77while holding the item to sell."))
                .consumer(e -> {
                    Player p = (Player) e.getWhoClicked();
                    p.closeInventory();
                    if (plugin.getShopsListener() != null) {
                        plugin.getShopsListener().awaitBusinessShopChest(p, businessId);
                    }
                })
        );
        addButton(15, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.BARREL, "\u00a7aStock",
                        "\u00a77Manage stockrooms for this business."))
                .consumer(e -> plugin.getGUIManager().openGUI(new BusinessStockManagementGUI(plugin, businessId), (Player) e.getWhoClicked()))
        );
        addButton(22, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7cBack"))
                .consumer(e -> plugin.getGUIManager().openGUI(new ShopsAndStockGUI(plugin, businessId), (Player) e.getWhoClicked()))
        );
        super.decorate(player);
    }
}
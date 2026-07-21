package com.dirt.inventory.impl.shops;

import com.cryptomorin.xseries.XMaterial;
import com.dirt.DirtEconomy;
import com.dirt.inventory.InventoryButton;
import com.dirt.inventory.InventoryGUI;
import com.dirt.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.UUID;

public class BusinessOnlineShopGUI extends InventoryGUI {
    private final DirtEconomy plugin;
    private final UUID businessId;

    public BusinessOnlineShopGUI(DirtEconomy plugin, UUID businessId) {
        this.plugin = plugin;
        this.businessId = businessId;
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 27, "\u00a7dManage Online Shop");
    }

    @Override
    public void decorate(Player player) {
        fillGlass(27, XMaterial.PURPLE_STAINED_GLASS_PANE);
        addButton(13, new InventoryButton()
                .creator(p -> {
                    boolean enabled = plugin.getShopManager().isOnlineShopEnabled(businessId);
                    if (enabled) {
                        return ItemUtil.buildItem(XMaterial.LIME_WOOL, "\u00a7aOnline Shop: ENABLED",
                                "\u00a77Other players can browse and buy",
                                "\u00a77from your business via /ds online.",
                                "\u00a77Click to disable.");
                    }
                    return ItemUtil.buildItem(XMaterial.RED_WOOL, "\u00a7cOnline Shop: DISABLED",
                            "\u00a77Click to enable. Listed items pull",
                            "\u00a77from your stockrooms.");
                })
                .consumer(e -> {
                    boolean enabled = plugin.getShopManager().isOnlineShopEnabled(businessId);
                    plugin.getShopManager().setOnlineShopEnabled(businessId, !enabled);
                    Player p = (Player) e.getWhoClicked();
                    p.sendMessage(enabled ? "\u00a7cOnline shop disabled." : "\u00a7aOnline shop enabled.");
                    plugin.getGUIManager().openGUI(new BusinessOnlineShopGUI(plugin, businessId), p);
                })
        );
        addButton(22, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7cBack"))
                .consumer(e -> plugin.getGUIManager().openGUI(new ShopsAndStockGUI(plugin, businessId), (Player) e.getWhoClicked()))
        );
        super.decorate(player);
    }
}
package com.dirt.inventory.impl.phone;

import com.cryptomorin.xseries.XMaterial;
import com.dirt.DirtEconomy;
import com.dirt.inventory.InventoryButton;
import com.dirt.inventory.InventoryGUI;
import com.dirt.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class MapsMainGUI extends InventoryGUI {
    private final DirtEconomy plugin;

    public MapsMainGUI(DirtEconomy plugin) {
        this.plugin = plugin;
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 27, "\u00a7e\u00a7lMaps");
    }

    @Override
    public void decorate(Player player) {
        fillGlass(27, XMaterial.LIME_STAINED_GLASS_PANE);

        addButton(11, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.ENDER_EYE, "\u00a7b\u00a7lMy Locations",
                        "\u00a77Personal GPS locations,",
                        "\u00a77your properties, and",
                        "\u00a77places you work."))
                .consumer(e -> plugin.getGUIManager().openGUI(new MapsMyLocationsGUI(plugin, 0, null), (Player) e.getWhoClicked()))
        );

        addButton(13, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.NETHER_STAR, "\u00a76\u00a7lGlobal Locations",
                        "\u00a77Public GPS locations",
                        "\u00a77visible to everyone."))
                .consumer(e -> plugin.getGUIManager().openGUI(new MapsGlobalLocationsGUI(plugin, 0, null), (Player) e.getWhoClicked()))
        );

        if (plugin.isDirtBusinessEnabled() && plugin.getSettings().isGpsBusinessLocationsEnabled()) {
            addButton(15, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.COMPASS, "\u00a72\u00a7lBusinesses",
                            "\u00a77Business GPS locations"))
                    .consumer(e -> plugin.getGUIManager().openGUI(new MapsBusinessLocationsGUI(plugin, 0, null), (Player) e.getWhoClicked()))
            );
        }

        addButton(26, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7cBack"))
                .consumer(e -> plugin.getGUIManager().openGUI(new PhoneHomescreenGUI(plugin, 0), (Player) e.getWhoClicked()))
        );

        super.decorate(player);
    }
}
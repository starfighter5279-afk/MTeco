package com.dirt.inventory.impl.phone;

import com.cryptomorin.xseries.XMaterial;
import com.dirt.DirtEconomy;
import com.dirt.data.GPSLocation;
import com.dirt.inventory.InventoryButton;
import com.dirt.inventory.InventoryGUI;
import com.dirt.inventory.impl.gps.GPSDetailsGUI;
import com.dirt.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.List;

public class MapsGlobalLocationsGUI extends InventoryGUI {
    private final DirtEconomy plugin;
    private final int page;
    private final String searchFilter;

    public MapsGlobalLocationsGUI(DirtEconomy plugin, int page, String searchFilter) {
        this.plugin = plugin;
        this.page = page;
        this.searchFilter = searchFilter;
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 54, "\u00a76\u00a7lGlobal Locations");
    }

    @Override
    public void decorate(Player player) {
        fillPagedGui(54, XMaterial.ORANGE_STAINED_GLASS_PANE);

        List<GPSLocation> locations = new ArrayList<>(plugin.getGpsManager().getGlobalLocations());
        if (searchFilter != null) {
            locations.removeIf(l -> !l.getName().toLowerCase().contains(searchFilter.toLowerCase()));
        }

        int perPage = 45;
        int start = page * perPage;
        int end = Math.min(start + perPage, locations.size());

        for (int i = start; i < end; i++) {
            GPSLocation gps = locations.get(i);
            int slot = i - start;
            addButton(slot, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.NETHER_STAR, "\u00a7e" + gps.getName(),
                            gps.getDescription().isEmpty() ? "\u00a78No description" : "\u00a77" + gps.getDescription(),
                            "\u00a77" + (int) gps.getX() + ", " + (int) gps.getY() + ", " + (int) gps.getZ(),
                            "",
                            "\u00a7eLeft-click to navigate",
                            "\u00a76Right-click for details"))
                    .consumer(e -> {
                        Player p = (Player) e.getWhoClicked();
                        if (e.isRightClick()) {
                            plugin.getGUIManager().openGUI(new GPSDetailsGUI(plugin, gps.getLocationId()), p);
                        } else {
                            p.closeInventory();
                            plugin.getGpsManager().startTrail(p, gps);
                        }
                    })
            );
        }

        addButton(46, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.OAK_SIGN, "\u00a7eSearch"))
                .consumer(e -> {
                    Player p = (Player) e.getWhoClicked();
                    plugin.getChatInputManager().requestInput(p, "\u00a7eSearch global locations:", term ->
                            plugin.getGUIManager().openGUI(new MapsGlobalLocationsGUI(plugin, 0, term), p));
                })
        );

        if (page > 0) {
            addButton(45, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7ePrevious Page"))
                    .consumer(e -> plugin.getGUIManager().openGUI(new MapsGlobalLocationsGUI(plugin, page - 1, searchFilter), (Player) e.getWhoClicked()))
            );
        }
        if (end < locations.size()) {
            addButton(53, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7eNext Page"))
                    .consumer(e -> plugin.getGUIManager().openGUI(new MapsGlobalLocationsGUI(plugin, page + 1, searchFilter), (Player) e.getWhoClicked()))
            );
        }
        int totalPages = Math.max(1, (int) Math.ceil((double) locations.size() / perPage));
        addPageIndicator(48, page, totalPages);

        addButton(49, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7cBack"))
                .consumer(e -> plugin.getGUIManager().openGUI(new MapsMainGUI(plugin), (Player) e.getWhoClicked()))
        );

        super.decorate(player);
    }
}
package com.dirt.inventory.impl.gps;

import com.cryptomorin.xseries.XMaterial;
import com.dirt.DirtEconomy;
import com.dirt.inventory.InventoryButton;
import com.dirt.inventory.InventoryGUI;
import com.dirt.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class GPSTypeSelectGUI extends InventoryGUI {
    private final DirtEconomy plugin;
    private final String locationName;
    private final Location location;
    private final int maxPersonal;
    private final int currentPersonal;
    private final int maxGlobal;
    private final int currentGlobal;

    public GPSTypeSelectGUI(DirtEconomy plugin, String locationName, Location location,
                             int maxPersonal, int currentPersonal, int maxGlobal, int currentGlobal) {
        this.plugin = plugin;
        this.locationName = locationName;
        this.location = location;
        this.maxPersonal = maxPersonal;
        this.currentPersonal = currentPersonal;
        this.maxGlobal = maxGlobal;
        this.currentGlobal = currentGlobal;
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 27, "\u00a7eGPS: Location Visibility");
    }

    @Override
    public void decorate(Player player) {
        fillGlass(27, XMaterial.YELLOW_STAINED_GLASS_PANE);

        addButton(11, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.ENDER_EYE, "\u00a7b\u00a7lPersonal",
                        "\u00a77Only you can see and use",
                        "\u00a77this GPS location.",
                        "",
                        "\u00a77Slots: \u00a7f" + currentPersonal + "/" + maxPersonal,
                        "",
                        currentPersonal >= maxPersonal ? "\u00a7cMax personal locations reached!" : "\u00a7eClick to create"))
                .consumer(e -> {
                    Player p = (Player) e.getWhoClicked();
                    if (currentPersonal >= maxPersonal) {
                        p.sendMessage("\u00a7cYou have reached the max personal GPS locations.");
                        p.closeInventory();
                        return;
                    }
                    plugin.getGpsManager().createLocation(locationName, "", p.getUniqueId(), null, location, false);
                    p.sendMessage("\u00a7aPersonal GPS location '\u00a7e" + locationName + "\u00a7a' created!");
                    p.closeInventory();
                })
        );

        addButton(15, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.NETHER_STAR, "\u00a76\u00a7lGlobal",
                        "\u00a77All players can see and",
                        "\u00a77navigate to this location.",
                        "",
                        "\u00a77Global slots: \u00a7f" + currentGlobal + "/" + maxGlobal,
                        "",
                        currentGlobal >= maxGlobal ? "\u00a7cMax global locations reached!" : "\u00a7eClick to create"))
                .consumer(e -> {
                    Player p = (Player) e.getWhoClicked();
                    if (currentGlobal >= maxGlobal) {
                        p.sendMessage("\u00a7cThe server has reached the max global GPS locations.");
                        p.closeInventory();
                        return;
                    }
                    plugin.getGpsManager().createLocation(locationName, "", p.getUniqueId(), null, location, true);
                    p.sendMessage("\u00a7aGlobal GPS location '\u00a7e" + locationName + "\u00a7a' created!");
                    p.closeInventory();
                })
        );

        super.decorate(player);
    }
}
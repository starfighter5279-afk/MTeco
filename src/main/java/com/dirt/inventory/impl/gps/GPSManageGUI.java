package com.dirt.inventory.impl.gps;

import com.cryptomorin.xseries.XMaterial;
import com.dirt.DirtEconomy;
import com.dirt.data.BusinessData;
import com.dirt.data.BusinessPermission;
import com.dirt.data.GPSLocation;
import com.dirt.inventory.InventoryButton;
import com.dirt.inventory.InventoryGUI;
import com.dirt.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GPSManageGUI extends InventoryGUI {
    private final DirtEconomy plugin;
    private final UUID playerUuid;
    private final int page;

    public GPSManageGUI(DirtEconomy plugin, UUID playerUuid, int page) {
        this.plugin = plugin;
        this.playerUuid = playerUuid;
        this.page = page;
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 54, "\u00a7eGPS: Manage Locations");
    }

    @Override
    public void decorate(Player player) {
        fillPagedGui(54, XMaterial.YELLOW_STAINED_GLASS_PANE);

        List<GPSLocation> manageable = new ArrayList<>(plugin.getGpsManager().getLocationsByCreator(playerUuid));

        if (plugin.isDirtBusinessEnabled()) {
            for (BusinessData biz : plugin.getBusinessManager().getBusinessesByOwner(playerUuid)) {
                for (GPSLocation loc : plugin.getGpsManager().getBusinessLocations(biz.getBusinessId())) {
                    if (!manageable.contains(loc)) manageable.add(loc);
                }
            }
            for (BusinessData biz : plugin.getBusinessManager().getBusinessesByEmployee(playerUuid)) {
                if (biz.hasPermission(playerUuid, BusinessPermission.MANAGE_GPS)) {
                    for (GPSLocation loc : plugin.getGpsManager().getBusinessLocations(biz.getBusinessId())) {
                        if (!manageable.contains(loc)) manageable.add(loc);
                    }
                }
            }
        }

        int perPage = 45;
        int start = page * perPage;
        int end = Math.min(start + perPage, manageable.size());

        for (int i = start; i < end; i++) {
            GPSLocation gps = manageable.get(i);
            int slot = i - start;
            XMaterial icon = gps.getBusinessId() != null ? XMaterial.COMPASS : (gps.isGlobal() ? XMaterial.NETHER_STAR : XMaterial.ENDER_EYE);
            String type = gps.getBusinessId() != null ? "\u00a76Business" : (gps.isGlobal() ? "\u00a7aGlobal" : "\u00a7bPersonal");

            addButton(slot, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(icon, "\u00a7e" + gps.getName(),
                            "\u00a77Type: " + type,
                            "\u00a77Coords: \u00a7f" + (int) gps.getX() + ", " + (int) gps.getY() + ", " + (int) gps.getZ(),
                            "",
                            "\u00a7eLeft-click to navigate",
                            "\u00a7cRight-click to delete"))
                    .consumer(e -> {
                        Player p = (Player) e.getWhoClicked();
                        if (e.isRightClick()) {
                            plugin.getGpsManager().deleteLocation(gps.getLocationId());
                            p.sendMessage("\u00a7aGPS location '\u00a7e" + gps.getName() + "\u00a7a' deleted.");
                            plugin.getGUIManager().openGUI(new GPSManageGUI(plugin, playerUuid, page), p);
                        } else {
                            p.closeInventory();
                            plugin.getGpsManager().startTrail(p, gps);
                        }
                    })
            );
        }

        if (page > 0) {
            addButton(45, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7ePrevious Page"))
                    .consumer(e -> plugin.getGUIManager().openGUI(new GPSManageGUI(plugin, playerUuid, page - 1), (Player) e.getWhoClicked()))
            );
        }
        if (end < manageable.size()) {
            addButton(53, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7eNext Page"))
                    .consumer(e -> plugin.getGUIManager().openGUI(new GPSManageGUI(plugin, playerUuid, page + 1), (Player) e.getWhoClicked()))
            );
        }
        int totalPages = Math.max(1, (int) Math.ceil((double) manageable.size() / perPage));
        addPageIndicator(48, page, totalPages);

        addButton(49, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7cClose"))
                .consumer(e -> ((Player) e.getWhoClicked()).closeInventory())
        );

        super.decorate(player);
    }
}
package com.dirt.inventory.impl.phone;

import com.cryptomorin.xseries.XMaterial;
import com.dirt.DirtEconomy;
import com.dirt.data.BusinessData;
import com.dirt.data.BusinessPropertyData;
import com.dirt.data.GPSLocation;
import com.dirt.data.PhoneData;
import com.dirt.data.PropertyData;
import com.dirt.inventory.InventoryButton;
import com.dirt.inventory.InventoryGUI;
import com.dirt.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MapsMyLocationsGUI extends InventoryGUI {
    private final DirtEconomy plugin;
    private final int page;
    private final String searchFilter;

    public MapsMyLocationsGUI(DirtEconomy plugin, int page, String searchFilter) {
        this.plugin = plugin;
        this.page = page;
        this.searchFilter = searchFilter;
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 54, "\u00a7b\u00a7lMy Locations");
    }

    @Override
    public void decorate(Player player) {
        fillPagedGui(54, XMaterial.CYAN_STAINED_GLASS_PANE);

        List<LocationEntry> entries = new ArrayList<>();

        for (GPSLocation gps : plugin.getGpsManager().getPersonalLocations(player.getUniqueId())) {
            entries.add(new LocationEntry(gps.getName(), "\u00a7bPersonal GPS", gps.getLocationId(), XMaterial.ENDER_EYE));
        }

        if (plugin.getNationManager() != null) {
            for (PropertyData prop : plugin.getNationManager().getPropertiesByOwner(player.getUniqueId())) {
                if (prop.getChunks().isEmpty()) continue;
                entries.add(new LocationEntry(prop.getName() != null ? prop.getName() : "Property",
                        "\u00a7aOwned Property", chunkToLocation(prop.getChunks().get(0)), XMaterial.OAK_DOOR));
            }
        }

        if (plugin.isDirtBusinessEnabled()) {
            for (BusinessData biz : plugin.getBusinessManager().getBusinessesByOwner(player.getUniqueId())) {
                addBusinessPropertyEntries(biz, entries);
            }
            for (BusinessData biz : plugin.getBusinessManager().getBusinessesByEmployee(player.getUniqueId())) {
                addBusinessPropertyEntries(biz, entries);
            }
        }

        PhoneData phone = plugin.getPhoneManager() != null ? plugin.getPhoneManager().getPhone(player.getUniqueId()) : null;
        if (phone != null) {
            for (UUID pinnedId : phone.getPinnedGpsLocations()) {
                GPSLocation gps = plugin.getGpsManager().getLocation(pinnedId);
                if (gps != null) {
                    boolean alreadyListed = entries.stream().anyMatch(e -> pinnedId.equals(e.gpsId));
                    if (!alreadyListed) {
                        entries.add(new LocationEntry("\u2605 " + gps.getName(), "\u00a7ePinned", gps.getLocationId(), XMaterial.GOLD_NUGGET));
                    }
                }
            }
        }

        if (searchFilter != null) {
            entries.removeIf(e -> !e.name.toLowerCase().contains(searchFilter.toLowerCase()));
        }

        int perPage = 45;
        int start = page * perPage;
        int end = Math.min(start + perPage, entries.size());

        for (int i = start; i < end; i++) {
            LocationEntry entry = entries.get(i);
            int slot = i - start;
            addButton(slot, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(entry.icon, "\u00a7e" + entry.name,
                            "\u00a77" + entry.type,
                            "",
                            "\u00a7eLeft-click to navigate",
                            "\u00a76Right-click to pin"))
                    .consumer(e -> {
                        Player p = (Player) e.getWhoClicked();
                        if (e.isRightClick() && entry.gpsId != null && phone != null) {
                            if (!phone.getPinnedGpsLocations().contains(entry.gpsId)) {
                                phone.getPinnedGpsLocations().add(entry.gpsId);
                                plugin.getPhoneManager().savePhone(phone);
                                p.sendMessage("\u00a7aPinned to Maps home!");
                            }
                            return;
                        }
                        p.closeInventory();
                        if (entry.gpsId != null) {
                            GPSLocation gps = plugin.getGpsManager().getLocation(entry.gpsId);
                            if (gps != null) plugin.getGpsManager().startTrail(p, gps);
                        } else if (entry.fallbackLoc != null) {
                            GPSLocation temp = new GPSLocation();
                            temp.setName(entry.name);
                            temp.setWorld(entry.fallbackLoc.getWorld().getName());
                            temp.setX(entry.fallbackLoc.getX());
                            temp.setY(entry.fallbackLoc.getY());
                            temp.setZ(entry.fallbackLoc.getZ());
                            plugin.getGpsManager().startTrail(p, temp);
                        }
                    })
            );
        }

        addButton(46, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.OAK_SIGN, "\u00a7eSearch"))
                .consumer(e -> {
                    Player p = (Player) e.getWhoClicked();
                    plugin.getChatInputManager().requestInput(p, "\u00a7eSearch locations:", term ->
                            plugin.getGUIManager().openGUI(new MapsMyLocationsGUI(plugin, 0, term), p));
                })
        );

        if (page > 0) {
            addButton(45, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7ePrevious Page"))
                    .consumer(e -> plugin.getGUIManager().openGUI(new MapsMyLocationsGUI(plugin, page - 1, searchFilter), (Player) e.getWhoClicked()))
            );
        }
        if (end < entries.size()) {
            addButton(53, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7eNext Page"))
                    .consumer(e -> plugin.getGUIManager().openGUI(new MapsMyLocationsGUI(plugin, page + 1, searchFilter), (Player) e.getWhoClicked()))
            );
        }
        int totalPages = Math.max(1, (int) Math.ceil((double) entries.size() / perPage));
        addPageIndicator(48, page, totalPages);

        addButton(49, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7cBack"))
                .consumer(e -> plugin.getGUIManager().openGUI(new MapsMainGUI(plugin), (Player) e.getWhoClicked()))
        );

        super.decorate(player);
    }

    private void addBusinessPropertyEntries(BusinessData biz, List<LocationEntry> entries) {
        for (UUID propId : biz.getPropertyIds()) {
            BusinessPropertyData prop = plugin.getBusinessManager().loadProperty(propId);
            if (prop != null && !prop.getChunks().isEmpty()) {
                entries.add(new LocationEntry(biz.getName() + " - " + prop.getName(),
                        "\u00a76Business", chunkToLocation(prop.getChunks().get(0)), XMaterial.CHEST));
            }
        }
    }

    private Location chunkToLocation(String chunkKey) {
        String[] parts = chunkKey.split("_");
        if (parts.length >= 3) {
            World world = Bukkit.getWorld(parts[0]);
            if (world != null) {
                int cx = Integer.parseInt(parts[1]) * 16 + 8;
                int cz = Integer.parseInt(parts[2]) * 16 + 8;
                return new Location(world, cx, world.getHighestBlockYAt(cx, cz), cz);
            }
        }
        return null;
    }

    private static class LocationEntry {
        final String name;
        final String type;
        final UUID gpsId;
        final Location fallbackLoc;
        final XMaterial icon;

        LocationEntry(String name, String type, UUID gpsId, XMaterial icon) {
            this.name = name; this.type = type; this.gpsId = gpsId; this.fallbackLoc = null; this.icon = icon;
        }

        LocationEntry(String name, String type, Location fallbackLoc, XMaterial icon) {
            this.name = name; this.type = type; this.gpsId = null; this.fallbackLoc = fallbackLoc; this.icon = icon;
        }
    }
}
package com.dirt.inventory.impl.nation;

import com.cryptomorin.xseries.XMaterial;
import com.dirt.DirtEconomy;
import com.dirt.data.RegionData;
import com.dirt.inventory.InventoryButton;
import com.dirt.inventory.InventoryGUI;
import com.dirt.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.UUID;

public class GovernorRegionManagementGUI extends InventoryGUI {
    private final DirtEconomy plugin;
    private final UUID regionId;

    public GovernorRegionManagementGUI(DirtEconomy plugin, UUID regionId) {
        this.plugin = plugin;
        this.regionId = regionId;
    }

    @Override
    protected Inventory createInventory() {
        RegionData region = plugin.getNationManager().loadRegion(regionId);
        String name = region != null ? region.getName() : "Region";
        return Bukkit.createInventory(null, 27, "\u00a76Manage: " + name);
    }

    @Override
    public void decorate(Player player) {
        fillGlass(27, XMaterial.BLUE_STAINED_GLASS_PANE);

        addButton(11, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.OAK_DOOR, "\u00a7eProperties",
                        "\u00a77Manage properties in this region."))
                .consumer(e -> plugin.getGUIManager().openGUI(new GovernorPropertiesGUI(plugin, regionId), (Player) e.getWhoClicked()))
        );

        addButton(15, new InventoryButton()
                .creator(p -> {
                    UUID targeted = plugin.getChunkSelectionManager().getTargetedRegion(p.getUniqueId());
                    boolean isTargeted = regionId.equals(targeted);
                    return ItemUtil.buildItem(XMaterial.TARGET, "\u00a76Target Region",
                            isTargeted ? "\u00a7aThis region is currently targeted." : "\u00a77Click to target this region.",
                            "\u00a77Use \u00a76/dr claim\u00a77 and \u00a76/dr auto\u00a77 after targeting.");
                })
                .consumer(e -> {
                    Player p = (Player) e.getWhoClicked();
                    plugin.getChunkSelectionManager().setTargetedRegion(p.getUniqueId(), regionId);
                    RegionData r = plugin.getNationManager().loadRegion(regionId);
                    p.sendMessage("\u00a7aYou are now targeting region \u00a76" + (r != null ? r.getName() : regionId) + "\u00a7a. Use \u00a76/dr claim\u00a7a or \u00a76/dr auto\u00a7a to claim chunks.");
                })
        );

        if (plugin.getSettings().isPropertyPermissionsEnabled()) {
            addButton(13, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.COMPARATOR, "\u00a7ePermissions",
                            "\u00a77Manage who can interact",
                            "\u00a77in this region."))
                    .consumer(e -> {
                        Player p = (Player) e.getWhoClicked();
                        RegionData r = plugin.getNationManager().loadRegion(regionId);
                        if (r == null) return;
                        plugin.getGUIManager().openGUI(new PropertyPermissionsGUI(plugin,
                                r.getPermsSameRegion(), r.getPermsSameNation(), r.getPermsForeign(),
                                () -> plugin.getNationManager().saveRegion(r),
                                back -> plugin.getGUIManager().openGUI(new GovernorRegionManagementGUI(plugin, regionId), back),
                                r.getDeniedMobs(),
                                () -> plugin.getNationManager().saveRegion(r),
                                back -> plugin.getGUIManager().openGUI(new GovernorRegionManagementGUI(plugin, regionId), back)), p);
                    })
            );
        }

        addButton(26, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7cBack"))
                .consumer(e -> plugin.getGUIManager().openGUI(new GovernorRegionsGUI(plugin, 0), (Player) e.getWhoClicked()))
        );

        super.decorate(player);
    }
}
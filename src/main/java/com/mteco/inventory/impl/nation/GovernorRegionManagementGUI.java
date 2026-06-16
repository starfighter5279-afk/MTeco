package com.mteco.inventory.impl.nation;

import com.cryptomorin.xseries.XMaterial;
import com.mteco.MTeco;
import com.mteco.data.RegionData;
import com.mteco.inventory.InventoryButton;
import com.mteco.inventory.InventoryGUI;
import com.mteco.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.UUID;

public class GovernorRegionManagementGUI extends InventoryGUI {
    private final MTeco plugin;
    private final UUID regionId;

    public GovernorRegionManagementGUI(MTeco plugin, UUID regionId) {
        this.plugin = plugin;
        this.regionId = regionId;
    }

    @Override
    protected Inventory createInventory() {
        RegionData region = plugin.getNationManager().loadRegion(regionId);
        String name = region != null ? region.getName() : "Region";
        return Bukkit.createInventory(null, 27, "§6Manage: " + name);
    }

    @Override
    public void decorate(Player player) {
        fillGlass(27, XMaterial.BLUE_STAINED_GLASS_PANE);

        addButton(11, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.OAK_DOOR, "§eProperties",
                        "§7Manage properties in this region."))
                .consumer(e -> plugin.getGUIManager().openGUI(new GovernorPropertiesGUI(plugin, regionId), (Player) e.getWhoClicked()))
        );

        addButton(15, new InventoryButton()
                .creator(p -> {
                    UUID targeted = plugin.getChunkSelectionManager().getTargetedRegion(p.getUniqueId());
                    boolean isTargeted = regionId.equals(targeted);
                    return ItemUtil.buildItem(XMaterial.TARGET, "§6Target Region",
                            isTargeted ? "§aThis region is currently targeted." : "§7Click to target this region.",
                            "§7Use §6/mtr claim§7 and §6/mtr auto§7 after targeting.");
                })
                .consumer(e -> {
                    Player p = (Player) e.getWhoClicked();
                    plugin.getChunkSelectionManager().setTargetedRegion(p.getUniqueId(), regionId);
                    RegionData r = plugin.getNationManager().loadRegion(regionId);
                    p.sendMessage("§aYou are now targeting region §6" + (r != null ? r.getName() : regionId) + "§a. Use §6/mtr claim§a or §6/mtr auto§a to claim chunks.");
                })
        );

        if (plugin.getSettings().isPropertyPermissionsEnabled()) {
            addButton(13, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.COMPARATOR, "§ePermissions",
                            "§7Manage who can interact",
                            "§7in this region."))
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
                .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "§cBack"))
                .consumer(e -> plugin.getGUIManager().openGUI(new GovernorRegionsGUI(plugin, 0), (Player) e.getWhoClicked()))
        );

        super.decorate(player);
    }
}
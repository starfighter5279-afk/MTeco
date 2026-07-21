package com.dirt.inventory.impl.nation;

import com.cryptomorin.xseries.XMaterial;
import com.dirt.DirtEconomy;
import com.dirt.data.NationData;
import com.dirt.data.PropertyData;
import com.dirt.data.RegionData;
import com.dirt.inventory.InventoryButton;
import com.dirt.inventory.InventoryGUI;
import com.dirt.util.ItemUtil;
import com.dirt.util.CurrencyUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GovernmentPropertyDetailGUI extends InventoryGUI {
    private final DirtEconomy plugin;
    private final UUID nationId;
    private final UUID propertyId;

    public GovernmentPropertyDetailGUI(DirtEconomy plugin, UUID nationId, UUID propertyId) {
        this.plugin = plugin;
        this.nationId = nationId;
        this.propertyId = propertyId;
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 27, "\u00a7eGov. Property Detail");
    }

    @Override
    public void decorate(Player player) {
        fillGlass(27, XMaterial.BLUE_STAINED_GLASS_PANE);

        PropertyData prop = plugin.getNationManager().loadProperty(propertyId);
        if (prop == null) { super.decorate(player); return; }

        RegionData region = plugin.getNationManager().loadRegion(prop.getRegionId());
        String regionName = region != null ? region.getName() : "Unknown";
        String propName = prop.getName() != null && !prop.getName().isEmpty() ? prop.getName() : "Unnamed";

        addButton(4, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.DARK_OAK_DOOR,
                        "\u00a7a" + propName,
                        "\u00a77Region: \u00a7f" + regionName,
                        "\u00a77Chunks: \u00a7f" + prop.getChunks().size(),
                        "\u00a77For Sale: \u00a7f" + (prop.isForSale() ? "\u00a7aYes (" + CurrencyUtil.symbol() + String.format("%.2f", prop.getSalePrice()) + ")" : "\u00a7cNo"),
                        "\u00a7eClick to manage"))
                .consumer(e -> {
                    Player p = (Player) e.getWhoClicked();
                    plugin.getGUIManager().openGUI(new PropertyManageGUI(plugin, propertyId, PropertyManageGUI.PropertyType.GOVERNMENT, nationId), p);
                })
        );

        NationData nation = plugin.getNationManager().loadNation(nationId);
        boolean isPresident = nation != null && player.getUniqueId().equals(nation.getPresidentUUID());

        if (isPresident) {
            addButton(15, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.RED_DYE, "\u00a7cDelete Property",
                            "\u00a77Permanently remove this property.",
                            "\u00a7cThis cannot be undone!"))
                    .consumer(e -> {
                        Player p = (Player) e.getWhoClicked();
                        PropertyData pr = plugin.getNationManager().loadProperty(propertyId);
                        if (pr != null && pr.getSaleSignLocation() != null) {
                            removeSaleSign(pr.getSaleSignLocation());
                        }
                        plugin.getNationManager().deleteProperty(propertyId);
                        NationData n = plugin.getNationManager().loadNation(nationId);
                        if (n != null) {
                            n.getGovernmentPropertyIds().remove(propertyId);
                            plugin.getNationManager().saveNation(n);
                        }
                        p.sendMessage("\u00a7aGovernment property deleted.");
                        plugin.getGUIManager().openGUI(new GovernmentPropertiesGUI(plugin, nationId, 0), p);
                    })
            );
        }

        if (plugin.getSettings().isPropertyPermissionsEnabled()) {
            addButton(13, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.COMPARATOR, "\u00a7ePermissions",
                            "\u00a77Manage who can interact",
                            "\u00a77with this property."))
                    .consumer(e -> {
                        Player p = (Player) e.getWhoClicked();
                        PropertyData pr = plugin.getNationManager().loadProperty(propertyId);
                        if (pr == null) return;
                        PropertyPermissionsGUI permsGui = new PropertyPermissionsGUI(plugin,
                                pr.getPermsSameRegion(), pr.getPermsSameNation(), pr.getPermsForeign(),
                                () -> plugin.getNationManager().saveProperty(pr),
                                back -> plugin.getGUIManager().openGUI(new GovernmentPropertyDetailGUI(plugin, nationId, propertyId), back),
                                pr.getDeniedMobs(),
                                () -> plugin.getNationManager().saveProperty(pr),
                                back -> plugin.getGUIManager().openGUI(new GovernmentPropertyDetailGUI(plugin, nationId, propertyId), back));
                        permsGui.setRolePermsOpener(rp -> {
                            PropertyData rPr = plugin.getNationManager().loadProperty(propertyId);
                            if (rPr == null) return;
                            NationData rN = plugin.getNationManager().loadNation(nationId);
                            if (rN == null) return;
                            List<UUID> rIds = new ArrayList<>();
                            List<String> rNames = new ArrayList<>();
                            List<String> rColors = new ArrayList<>();
                            RolePermsGUI.buildGovRoles(plugin, rN, rIds, rNames, rColors);
                            plugin.getGUIManager().openGUI(new RolePermsGUI(plugin, rPr.getRolePerms(),
                                    rIds, rNames, rColors,
                                    () -> plugin.getNationManager().saveProperty(rPr),
                                    back -> plugin.getGUIManager().openGUI(new GovernmentPropertyDetailGUI(plugin, nationId, propertyId), back),
                                    0), rp);
                        });
                        plugin.getGUIManager().openGUI(permsGui, p);
                    })
            );
        }

        addButton(11, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.OAK_DOOR, "\u00a76Rooms",
                        "\u00a77Manage rooms within this property.",
                        "\u00a77Rooms: \u00a7f" + prop.getRoomIds().size()))
                .consumer(e -> {
                    Player p = (Player) e.getWhoClicked();
                    plugin.getGUIManager().openGUI(new RoomManagementGUI(plugin, propertyId, false, nationId, 0,
                            back -> plugin.getGUIManager().openGUI(new GovernmentPropertyDetailGUI(plugin, nationId, propertyId), back)), p);
                })
        );

        addButton(26, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7cBack"))
                .consumer(e -> plugin.getGUIManager().openGUI(new GovernmentPropertiesGUI(plugin, nationId, 0), (Player) e.getWhoClicked()))
        );

        super.decorate(player);
    }

    private void removeSaleSign(String locKey) {
        try {
            String[] parts = locKey.split(",");
            if (parts.length < 4) return;
            org.bukkit.World world = Bukkit.getWorld(parts[0]);
            if (world == null) return;
            Block block = world.getBlockAt(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
            if (block.getType().name().contains("SIGN")) block.setType(Material.AIR);
        } catch (Exception ignored) {}
    }
}
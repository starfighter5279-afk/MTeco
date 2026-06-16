package com.mteco.inventory.impl.nation;

import com.cryptomorin.xseries.XMaterial;
import com.mteco.MTeco;
import com.mteco.data.PropertyData;
import com.mteco.data.RegionData;
import com.mteco.inventory.InventoryButton;
import com.mteco.inventory.InventoryGUI;
import com.mteco.util.ItemUtil;
import com.mteco.util.CurrencyUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.UUID;

public class CharacterPropertyDetailGUI extends InventoryGUI {
    private final MTeco plugin;
    private final UUID propertyId;

    public CharacterPropertyDetailGUI(MTeco plugin, UUID propertyId) {
        this.plugin = plugin;
        this.propertyId = propertyId;
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 27, "\u00a7aProperty Detail");
    }

    @Override
    public void decorate(Player player) {
        fillGlass(27, XMaterial.BLUE_STAINED_GLASS_PANE);

        PropertyData prop = plugin.getNationManager().loadProperty(propertyId);
        if (prop == null) { super.decorate(player); return; }

        RegionData region = plugin.getNationManager().loadRegion(prop.getRegionId());
        String regionName = region != null ? region.getName() : "Unknown";
        String propName = prop.getName() != null && !prop.getName().isEmpty() ? prop.getName() : "Property";

        addButton(4, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.OAK_DOOR,
                        "\u00a7a" + propName,
                        "\u00a77Region: \u00a7f" + regionName,
                        "\u00a77Chunks: \u00a7f" + prop.getChunks().size(),
                        "\u00a77For Sale: \u00a7f" + (prop.isForSale() ? "\u00a7aYes (" + CurrencyUtil.symbol() + String.format("%.2f", prop.getSalePrice()) + ")" : "\u00a7cNo"),
                        "\u00a7eClick to manage"))
                .consumer(e -> {
                    Player p = (Player) e.getWhoClicked();
                    plugin.getGUIManager().openGUI(new PropertyManageGUI(plugin, propertyId, PropertyManageGUI.PropertyType.PERSONAL, null), p);
                })
        );

        if (plugin.getSettings().isPropertyPermissionsEnabled()) {
            addButton(13, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.COMPARATOR, "\u00a7ePermissions",
                            "\u00a77Manage who can interact",
                            "\u00a77with this property."))
                    .consumer(e -> {
                        Player p = (Player) e.getWhoClicked();
                        PropertyData pr = plugin.getNationManager().loadProperty(propertyId);
                        if (pr == null) return;
                        plugin.getGUIManager().openGUI(new PropertyPermissionsGUI(plugin, pr.getPermsSameRegion(),
                                pr.getPermsSameNation(), pr.getPermsForeign(),
                                () -> plugin.getNationManager().saveProperty(pr),
                                back -> plugin.getGUIManager().openGUI(new CharacterPropertyDetailGUI(plugin, propertyId), back),
                                pr.getDeniedMobs(),
                                () -> plugin.getNationManager().saveProperty(pr),
                                back -> plugin.getGUIManager().openGUI(new CharacterPropertyDetailGUI(plugin, propertyId), back)), p);
                    })
            );
        }

        addButton(11, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.OAK_DOOR, "\u00a76Rooms",
                        "\u00a77Manage rooms within this property.",
                        "\u00a77Rooms: \u00a7f" + prop.getRoomIds().size()))
                .consumer(e -> {
                    Player p = (Player) e.getWhoClicked();
                    plugin.getGUIManager().openGUI(new RoomManagementGUI(plugin, propertyId, false, null, 0,
                            back -> plugin.getGUIManager().openGUI(new CharacterPropertyDetailGUI(plugin, propertyId), back)), p);
                })
        );

        addButton(26, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7cBack"))
                .consumer(e -> plugin.getGUIManager().openGUI(new CharacterPropertiesGUI(plugin, 0), (Player) e.getWhoClicked()))
        );

        super.decorate(player);
    }
}
package com.dirt.inventory.impl.gps;

import com.cryptomorin.xseries.XMaterial;
import com.dirt.DirtEconomy;
import com.dirt.data.BusinessData;
import com.dirt.data.CharacterData;
import com.dirt.data.GPSLocation;
import com.dirt.inventory.InventoryButton;
import com.dirt.inventory.InventoryGUI;
import com.dirt.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

public class GPSDetailsGUI extends InventoryGUI {
    private final DirtEconomy plugin;
    private final UUID locationId;

    public GPSDetailsGUI(DirtEconomy plugin, UUID locationId) {
        this.plugin = plugin;
        this.locationId = locationId;
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 27, "\u00a7eGPS Location Details");
    }

    @Override
    public void decorate(Player player) {
        fillGlass(27, XMaterial.YELLOW_STAINED_GLASS_PANE);

        GPSLocation gps = plugin.getGpsManager().getLocation(locationId);
        if (gps == null) {
            addButton(13, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.BARRIER, "\u00a7cLocation not found"))
                    .consumer(e -> {}));
            super.decorate(player);
            return;
        }

        String creatorName = "Unknown";
        if (gps.getCreatorUuid() != null) {
            CharacterData creator = plugin.getCharacterManager().getCharacter(gps.getCreatorUuid());
            if (creator != null) creatorName = creator.getFirstName() + " " + creator.getLastName();
        }

        String businessName = "";
        if (gps.getBusinessId() != null) {
            BusinessData biz = plugin.getBusinessManager() != null ? plugin.getBusinessManager().loadBusiness(gps.getBusinessId()) : null;
            businessName = biz != null ? biz.getName() : "Unknown Business";
        }

        String dateStr = gps.getCreatedAt() > 0 ? new SimpleDateFormat("MM/dd/yyyy HH:mm").format(new Date(gps.getCreatedAt())) : "Unknown";

        String finalCreatorName = creatorName;
        String finalBizName = businessName;
        addButton(4, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.COMPASS, "\u00a7e\u00a7l" + gps.getName(),
                        "\u00a77Created by: \u00a7f" + finalCreatorName,
                        gps.getBusinessId() != null ? "\u00a77Business: \u00a76" + finalBizName : "",
                        "\u00a77Description: \u00a7f" + (gps.getDescription().isEmpty() ? "None" : gps.getDescription()),
                        "",
                        "\u00a77World: \u00a7f" + gps.getWorld(),
                        "\u00a77X: \u00a7f" + (int) gps.getX(),
                        "\u00a77Y: \u00a7f" + (int) gps.getY(),
                        "\u00a77Z: \u00a7f" + (int) gps.getZ(),
                        "",
                        "\u00a77Visibility: " + (gps.isGlobal() ? "\u00a7aGlobal" : "\u00a7bPersonal"),
                        "\u00a77Created: \u00a7f" + dateStr))
                .consumer(e -> {})
        );

        addButton(12, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.ENDER_PEARL, "\u00a7aNavigate Here",
                        "\u00a77Start GPS guidance to", "\u00a77this location."))
                .consumer(e -> {
                    Player p = (Player) e.getWhoClicked();
                    p.closeInventory();
                    plugin.getGpsManager().startTrail(p, gps);
                })
        );

        addButton(26, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7cClose"))
                .consumer(e -> ((Player) e.getWhoClicked()).closeInventory())
        );

        super.decorate(player);
    }
}
package com.mteco.inventory.impl.nation;

import com.cryptomorin.xseries.XMaterial;
import com.mteco.MTeco;
import com.mteco.data.NationData;
import com.mteco.data.PropertyData;
import com.mteco.data.RegionData;
import com.mteco.inventory.InventoryButton;
import com.mteco.inventory.InventoryGUI;
import com.mteco.managers.NationManager;
import com.mteco.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GovernmentPropertiesGUI extends InventoryGUI {
    private final MTeco plugin;
    private final UUID nationId;
    private final int page;

    public GovernmentPropertiesGUI(MTeco plugin, UUID nationId, int page) {
        this.plugin = plugin;
        this.nationId = nationId;
        this.page = page;
    }

    @Override
    protected Inventory createInventory() {
        NationData nation = plugin.getNationManager().loadNation(nationId);
        String name = nation != null ? nation.getColor1() + nation.getName() : "\u00a7eNation";
        return Bukkit.createInventory(null, 54, name + " \u00a78Gov. Properties");
    }

    @Override
    public void decorate(Player player) {
        fillPagedGui(54, XMaterial.BLUE_STAINED_GLASS_PANE);

        NationData nation = plugin.getNationManager().loadNation(nationId);
        if (nation == null) { super.decorate(player); return; }

        List<PropertyData> govProps = new ArrayList<>();
        for (UUID propId : nation.getGovernmentPropertyIds()) {
            PropertyData prop = plugin.getNationManager().loadProperty(propId);
            if (prop != null) govProps.add(prop);
        }

        int perPage = 44;
        int start = page * perPage;
        int end = Math.min(start + perPage, govProps.size());

        for (int i = start; i < end; i++) {
            PropertyData prop = govProps.get(i);
            RegionData region = plugin.getNationManager().loadRegion(prop.getRegionId());
            String regionName = region != null ? region.getName() : "Unknown";
            String propName = prop.getName() != null && !prop.getName().isEmpty() ? prop.getName() : "Unnamed";
            int chunkCount = prop.getChunks().size();
            final UUID propId = prop.getPropertyId();

            addButton(i - start, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.DARK_OAK_DOOR, "\u00a7a" + propName,
                            "\u00a77Region: \u00a7f" + regionName,
                            "\u00a77Chunks: \u00a7f" + chunkCount,
                            "\u00a77Click to manage."))
                    .consumer(e -> plugin.getGUIManager().openGUI(
                            new GovernmentPropertyDetailGUI(plugin, nationId, propId), (Player) e.getWhoClicked()))
            );
        }

        if (page > 0) {
            int prev = page - 1;
            addButton(45, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7ePrevious"))
                    .consumer(e -> plugin.getGUIManager().openGUI(new GovernmentPropertiesGUI(plugin, nationId, prev), (Player) e.getWhoClicked()))
            );
        }
        if (end < govProps.size()) {
            int next = page + 1;
            addButton(53, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7eNext"))
                    .consumer(e -> plugin.getGUIManager().openGUI(new GovernmentPropertiesGUI(plugin, nationId, next), (Player) e.getWhoClicked()))
            );
        }

        boolean isPresident = player.getUniqueId().equals(nation.getPresidentUUID());
        if (isPresident) {
            addButton(48, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.EMERALD, "\u00a7aCreate Government Property",
                            "\u00a77Select chunks within your nation.",
                            "\u00a77No purchase required."))
                    .consumer(e -> {
                        Player p = (Player) e.getWhoClicked();
                        p.closeInventory();
                        plugin.getChunkSelectionManager().startGovernmentPropertySelection(p, nationId);
                    })
            );
        }

        addButton(49, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7cBack"))
                .consumer(e -> plugin.getGUIManager().openGUI(new NationManagementGUI(plugin, nationId), (Player) e.getWhoClicked()))
        );

        if (plugin.getSettings().isCrimeEnabled()) {
            addButton(50, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.IRON_BARS, "\u00a7eJail Management",
                            "\u00a77Manage jails and cells."))
                    .consumer(e -> plugin.getGUIManager().openGUI(
                            new JailManagementGUI(plugin, nationId), (Player) e.getWhoClicked()))
            );
        }

        super.decorate(player);
    }
}
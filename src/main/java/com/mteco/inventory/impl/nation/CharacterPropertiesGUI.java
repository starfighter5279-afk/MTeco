package com.mteco.inventory.impl.nation;

import com.cryptomorin.xseries.XMaterial;
import com.mteco.MTeco;
import com.mteco.data.PropertyData;
import com.mteco.data.RegionData;
import com.mteco.inventory.InventoryButton;
import com.mteco.inventory.InventoryGUI;
import com.mteco.inventory.impl.CharacterManagementGUI;
import com.mteco.managers.NationManager;
import com.mteco.util.ItemUtil;
import com.mteco.util.CurrencyUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.List;
import java.util.UUID;

public class CharacterPropertiesGUI extends InventoryGUI {
    private final MTeco plugin;
    private final int page;
    private final UUID targetUuid;
    private final boolean adminMode;

    public CharacterPropertiesGUI(MTeco plugin, int page) {
        this.plugin = plugin;
        this.page = page;
        this.targetUuid = null;
        this.adminMode = false;
    }

    public CharacterPropertiesGUI(MTeco plugin, int page, UUID targetUuid) {
        this.plugin = plugin;
        this.page = page;
        this.targetUuid = targetUuid;
        this.adminMode = true;
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 54, adminMode ? "§4Admin: Properties" : "§6My Properties");
    }

    @Override
    public void decorate(Player player) {
        UUID effectiveUuid = targetUuid != null ? targetUuid : player.getUniqueId();

        fillPagedGui(54, XMaterial.BLUE_STAINED_GLASS_PANE);

        List<PropertyData> props = plugin.getNationManager().getPropertiesByOwner(effectiveUuid);
        int perPage = 44;
        int start = page * perPage;
        int end = Math.min(start + perPage, props.size());

        for (int i = start; i < end; i++) {
            PropertyData prop = props.get(i);
            RegionData region = plugin.getNationManager().loadRegion(prop.getRegionId());
            String regionName = region != null ? region.getName() : "Unknown";
            com.mteco.data.NationData nation = region != null ? plugin.getNationManager().loadNation(region.getNationId()) : null;
            String nationName = nation != null ? nation.getName() : "Unknown";
            int chunkCount = prop.getChunks().size();
            String propName = prop.getName() != null && !prop.getName().isEmpty() ? prop.getName() : "Unnamed Property";
            addButton(i - start, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.OAK_DOOR,
                            "§a" + propName,
                            "§7Chunks: §f" + chunkCount,
                            "§7Region: §f" + regionName,
                            "§7Nation: §f" + nationName,
                            "§7For Sale: §f" + (prop.isForSale() ? "§aYes (" + CurrencyUtil.symbol() + String.format("%.2f", prop.getSalePrice()) + ")" : "§cNo"),
                            adminMode ? "§7Admin view (read-only)" : "§eClick to manage"))
                    .consumer(e -> {
                        if (adminMode) return;
                        plugin.getGUIManager().openGUI(new CharacterPropertyDetailGUI(plugin, prop.getPropertyId()), (Player) e.getWhoClicked());
                    })
            );
        }

        if (page > 0) {
            int prev = page - 1;
            addButton(45, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "§ePrevious"))
                    .consumer(e -> {
                        Player p = (Player) e.getWhoClicked();
                        if (adminMode) plugin.getGUIManager().openGUI(new CharacterPropertiesGUI(plugin, prev, effectiveUuid), p);
                        else plugin.getGUIManager().openGUI(new CharacterPropertiesGUI(plugin, prev), p);
                    })
            );
        }
        if (end < props.size()) {
            int next = page + 1;
            addButton(53, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "§eNext"))
                    .consumer(e -> {
                        Player p = (Player) e.getWhoClicked();
                        if (adminMode) plugin.getGUIManager().openGUI(new CharacterPropertiesGUI(plugin, next, effectiveUuid), p);
                        else plugin.getGUIManager().openGUI(new CharacterPropertiesGUI(plugin, next), p);
                    })
            );
        }

        if (!adminMode) {
            addButton(48, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.EMERALD, "§aPurchase New Property",
                            "§7Select chunks within your nation's region.",
                            "§7You must be standing in a claimed region."))
                    .consumer(e -> {
                        Player p = (Player) e.getWhoClicked();
                        String chunkKey = NationManager.chunkKey(p.getWorld().getName(),
                                p.getLocation().getBlockX() >> 4, p.getLocation().getBlockZ() >> 4);
                        RegionData region = plugin.getNationManager().getRegionByChunk(chunkKey);
                        if (region == null) {
                            p.sendMessage("§cYou must be standing in a claimed region to purchase property.");
                            return;
                        }
                        com.mteco.data.NationData nation = plugin.getNationManager().loadNation(region.getNationId());
                        if (nation == null || !nation.getMemberUUIDs().contains(p.getUniqueId())) {
                            p.sendMessage("§cYou are not a member of the nation that owns this region.");
                            return;
                        }
                        p.closeInventory();
                        plugin.getChunkSelectionManager().startPropertySelection(p, region.getRegionId());
                    })
            );
        }

        addButton(49, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "§cBack"))
                .consumer(e -> {
                    Player p = (Player) e.getWhoClicked();
                    if (adminMode) plugin.getGUIManager().openGUI(new CharacterManagementGUI(plugin, effectiveUuid), p);
                    else plugin.getGUIManager().openGUI(new CharacterManagementGUI(plugin), p);
                })
        );

        super.decorate(player);
    }
}
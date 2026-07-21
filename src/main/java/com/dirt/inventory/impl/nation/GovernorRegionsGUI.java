package com.dirt.inventory.impl.nation;

import com.cryptomorin.xseries.XMaterial;
import com.dirt.DirtEconomy;
import com.dirt.data.RegionData;
import com.dirt.inventory.InventoryButton;
import com.dirt.inventory.InventoryGUI;
import com.dirt.util.ItemUtil;
import com.dirt.util.CurrencyUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.List;

public class GovernorRegionsGUI extends InventoryGUI {
    private final DirtEconomy plugin;
    private final int page;

    public GovernorRegionsGUI(DirtEconomy plugin, int page) {
        this.plugin = plugin;
        this.page = page;
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 54, "§6Your Regions");
    }

    @Override
    public void decorate(Player player) {
        fillPagedGui(54, XMaterial.BLUE_STAINED_GLASS_PANE);

        List<RegionData> regions = plugin.getNationManager().getRegionsByGovernor(player.getUniqueId());
        int perPage = 45;
        int start = page * perPage;
        int end = Math.min(start + perPage, regions.size());

        for (int i = start; i < end; i++) {
            RegionData region = regions.get(i);
            int slot = i - start;
            addButton(slot, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.MAP, "§6" + region.getName(),
                            "§7Chunks: §f" + region.getClaimedChunks().size(),
                            "§7Properties: §f" + region.getPropertyIds().size(),
                            "§7Chunk Rate: §e" + CurrencyUtil.symbol() + String.format("%.2f", region.getPropertyChunkRate()),
                            "§7Click to manage."))
                    .consumer(e -> plugin.getGUIManager().openGUI(new GovernorRegionManagementGUI(plugin, region.getRegionId()), (Player) e.getWhoClicked()))
            );
        }

        if (page > 0) {
            int prev = page - 1;
            addButton(45, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "§ePrevious"))
                    .consumer(e -> plugin.getGUIManager().openGUI(new GovernorRegionsGUI(plugin, prev), (Player) e.getWhoClicked()))
            );
        }
        if (end < regions.size()) {
            int next = page + 1;
            addButton(53, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "§eNext"))
                    .consumer(e -> plugin.getGUIManager().openGUI(new GovernorRegionsGUI(plugin, next), (Player) e.getWhoClicked()))
            );
        }

        super.decorate(player);
    }
}
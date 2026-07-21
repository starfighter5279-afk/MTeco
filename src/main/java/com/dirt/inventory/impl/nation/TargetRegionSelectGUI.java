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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TargetRegionSelectGUI extends InventoryGUI {
    private final DirtEconomy plugin;
    private final UUID attackingNationId;
    private final UUID defendingNationId;
    private final List<UUID> attackerRegionIds;
    private final List<UUID> selectedTargets;

    public TargetRegionSelectGUI(DirtEconomy plugin, UUID attackingNationId, UUID defendingNationId,
                                 List<UUID> attackerRegionIds, List<UUID> selectedTargets) {
        this.plugin = plugin;
        this.attackingNationId = attackingNationId;
        this.defendingNationId = defendingNationId;
        this.attackerRegionIds = new ArrayList<>(attackerRegionIds);
        this.selectedTargets = new ArrayList<>(selectedTargets);
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 54, "§cSelect Regions to Claim");
    }

    @Override
    public void decorate(Player player) {
        fillPagedGui(54, XMaterial.RED_STAINED_GLASS_PANE);

        List<RegionData> regions = plugin.getNationManager().getRegionsByNation(defendingNationId);
        for (int i = 0; i < Math.min(regions.size(), 45); i++) {
            RegionData region = regions.get(i);
            boolean selected = selectedTargets.contains(region.getRegionId());
            int slot = i;
            addButton(slot, new InventoryButton()
                    .creator(p -> {
                        XMaterial mat = selected ? XMaterial.RED_STAINED_GLASS_PANE : XMaterial.MAP;
                        String prefix = selected ? "§c✔ " : "§7";
                        return ItemUtil.buildItem(mat, prefix + region.getName(),
                                selected ? "§7Click to deselect." : "§7Click to mark for claiming.",
                                "§8Chunks: " + region.getClaimedChunks().size());
                    })
                    .consumer(e -> {
                        List<UUID> newTargets = new ArrayList<>(selectedTargets);
                        if (newTargets.contains(region.getRegionId())) {
                            newTargets.remove(region.getRegionId());
                        } else {
                            newTargets.add(region.getRegionId());
                        }
                        plugin.getGUIManager().openGUI(
                                new TargetRegionSelectGUI(plugin, attackingNationId, defendingNationId, attackerRegionIds, newTargets),
                                (Player) e.getWhoClicked());
                    })
            );
        }

        // Confirm
        if (!selectedTargets.isEmpty()) {
            addButton(49, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.EMERALD, "§aConfirm & Review War",
                            "§7Proceed to confirm and pay §a" + CurrencyUtil.symbol() + "10,000",
                            "§7to declare this war.",
                            "§eTargets selected: §f" + selectedTargets.size()))
                    .consumer(e -> plugin.getGUIManager().openGUI(
                            new WarConfirmGUI(plugin, attackingNationId, defendingNationId, attackerRegionIds, selectedTargets),
                            (Player) e.getWhoClicked()))
            );
        }

        // Back
        addButton(45, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.BARRIER, "§cBack"))
                .consumer(e -> plugin.getGUIManager().openGUI(
                        new AttackerRegionSelectGUI(plugin, attackingNationId, defendingNationId, attackerRegionIds),
                        (Player) e.getWhoClicked()))
        );

        super.decorate(player);
    }
}
package com.dirt.inventory.impl.nation;

import com.cryptomorin.xseries.XMaterial;
import com.dirt.DirtEconomy;
import com.dirt.data.NationData;
import com.dirt.data.RegionData;
import com.dirt.inventory.InventoryButton;
import com.dirt.inventory.InventoryGUI;
import com.dirt.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AttackerRegionSelectGUI extends InventoryGUI {
    private final DirtEconomy plugin;
    private final UUID attackingNationId;
    private final UUID defendingNationId;
    private final List<UUID> selectedRegions;

    public AttackerRegionSelectGUI(DirtEconomy plugin, UUID attackingNationId, UUID defendingNationId, List<UUID> selectedRegions) {
        this.plugin = plugin;
        this.attackingNationId = attackingNationId;
        this.defendingNationId = defendingNationId;
        this.selectedRegions = new ArrayList<>(selectedRegions);
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 54, "§6Select Attacking Regions (max 5)");
    }

    @Override
    public void decorate(Player player) {
        fillPagedGui(54, XMaterial.RED_STAINED_GLASS_PANE);

        List<RegionData> regions = plugin.getNationManager().getRegionsByNation(attackingNationId);
        int maxAttackers = plugin.getSettings().getMaxAttackingRegions();
        for (int i = 0; i < Math.min(regions.size(), 45); i++) {
            RegionData region = regions.get(i);
            boolean selected = selectedRegions.contains(region.getRegionId());
            int slot = i;
            addButton(slot, new InventoryButton()
                    .creator(p -> {
                        XMaterial mat = selected ? XMaterial.LIME_STAINED_GLASS_PANE : XMaterial.MAP;
                        String prefix = selected ? "§a✔ " : "§7";
                        return ItemUtil.buildItem(mat, prefix + region.getName(),
                                selected ? "§7Click to deselect." : "§7Click to select as attacker.",
                                "§8Chunks: " + region.getClaimedChunks().size());
                    })
                    .consumer(e -> {
                        List<UUID> newSelection = new ArrayList<>(selectedRegions);
                        if (newSelection.contains(region.getRegionId())) {
                            newSelection.remove(region.getRegionId());
                        } else {
                            if (newSelection.size() >= maxAttackers) {
                                ((Player) e.getWhoClicked()).sendMessage("§cYou can only select up to " + maxAttackers + " attacking regions.");
                                return;
                            }
                            newSelection.add(region.getRegionId());
                        }
                        plugin.getGUIManager().openGUI(
                                new AttackerRegionSelectGUI(plugin, attackingNationId, defendingNationId, newSelection),
                                (Player) e.getWhoClicked());
                    })
            );
        }

        // Confirm
        if (!selectedRegions.isEmpty()) {
            addButton(49, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.EMERALD, "§aConfirm Attackers",
                            "§7Proceed to select regions to claim.",
                            "§eSelected: §f" + selectedRegions.size()))
                    .consumer(e -> plugin.getGUIManager().openGUI(
                            new TargetRegionSelectGUI(plugin, attackingNationId, defendingNationId, selectedRegions, new ArrayList<>()),
                            (Player) e.getWhoClicked()))
            );
        }

        // Back
        addButton(45, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.BARRIER, "§cBack"))
                .consumer(e -> plugin.getGUIManager().openGUI(
                        new DeclareWarNationSelectGUI(plugin, attackingNationId),
                        (Player) e.getWhoClicked()))
        );

        super.decorate(player);
    }
}
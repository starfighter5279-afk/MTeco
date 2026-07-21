package com.dirt.inventory.impl.nation;

import com.cryptomorin.xseries.XMaterial;
import com.dirt.DirtEconomy;
import com.dirt.data.CharacterData;
import com.dirt.data.NationData;
import com.dirt.data.RegionData;
import com.dirt.inventory.InventoryButton;
import com.dirt.inventory.InventoryGUI;
import com.dirt.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class RegionGovernorsGUI extends InventoryGUI {
    private final DirtEconomy plugin;
    private final UUID nationId;

    public RegionGovernorsGUI(DirtEconomy plugin, UUID nationId) {
        this.plugin = plugin;
        this.nationId = nationId;
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 54, "§cRegion Governors");
    }

    @Override
    public void decorate(Player player) {
        fillPagedGui(54, XMaterial.BLUE_STAINED_GLASS_PANE);

        NationData nation = plugin.getNationManager().loadNation(nationId);
        List<RegionData> regions = plugin.getNationManager().getRegionsByNation(nationId);

        int slot = 0;
        for (RegionData region : regions) {
            if (slot >= 45) break;
            String govName = resolveCharName(region.getGovernorUUID());
            int finalSlot = slot;
            addButton(slot, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.MAP, "§6" + region.getName(),
                            "§7Governor: §f" + govName,
                            "§7Chunks: §f" + region.getClaimedChunks().size()))
                    .consumer(e -> {})
            );
            slot++;
        }

        // "Elect New Governor" - only if there are regions
        if (!regions.isEmpty()) {
            List<CharacterData> memberChars = nation == null ? List.of() : nation.getMemberUUIDs().stream()
                    .map(u -> plugin.getCharacterManager().getCharacter(u))
                    .filter(c -> c != null)
                    .collect(Collectors.toList());

            addButton(49, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.EMERALD, "§aElect New Governor",
                            "§7Select a region and assign a governor."))
                    .consumer(e -> {
                        Player p = (Player) e.getWhoClicked();
                        plugin.getGUIManager().openGUI(new RegionPickerGUI(plugin, regions, region -> {
                            plugin.getGUIManager().openGUI(new NationCharacterSelectGUI(plugin, "§eSelect New Governor for " + region.getName(), memberChars, c -> {
                                RegionData r = plugin.getNationManager().loadRegion(region.getRegionId());
                                if (r == null) return;
                                r.setGovernorUUID(c.getPlayerUuid());
                                plugin.getNationManager().saveRegion(r);
                                p.sendMessage("§a" + c.getFirstName() + " " + c.getLastName() + " is now governor of §6" + r.getName() + "§a.");
                                plugin.getGUIManager().openGUI(new RegionGovernorsGUI(plugin, nationId), p);
                            }, new RegionGovernorsGUI(plugin, nationId), 0), p);
                        }), p);
                    })
            );
        }

        addButton(45, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "§cBack"))
                .consumer(e -> plugin.getGUIManager().openGUI(new MemberManagementGUI(plugin, nationId), (Player) e.getWhoClicked()))
        );

        super.decorate(player);
    }

    private String resolveCharName(UUID uuid) {
        if (uuid == null) return "§7Unassigned";
        CharacterData c = plugin.getCharacterManager().getCharacter(uuid);
        return c != null ? c.getFirstName() + " " + c.getLastName() : "§7Unknown";
    }
}
package com.mteco.inventory.impl.nation;

import com.cryptomorin.xseries.XMaterial;
import com.mteco.MTeco;
import com.mteco.data.CharacterData;
import com.mteco.data.NationData;
import com.mteco.data.RegionData;
import com.mteco.inventory.InventoryButton;
import com.mteco.inventory.InventoryGUI;
import com.mteco.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class RegionsGUI extends InventoryGUI {
    private final MTeco plugin;
    private final UUID nationId;

    public RegionsGUI(MTeco plugin, UUID nationId) {
        this.plugin = plugin;
        this.nationId = nationId;
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 54, "§6Regions");
    }

    @Override
    public void decorate(Player player) {
        fillPagedGui(54, XMaterial.BLUE_STAINED_GLASS_PANE);

        List<RegionData> regions = plugin.getNationManager().getRegionsByNation(nationId);

        int slot = 0;
        for (RegionData region : regions) {
            if (slot >= 44) break;
            String govName = resolveCharName(region.getGovernorUUID());
            addButton(slot, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.MAP, "§6" + region.getName(),
                            "§7Governor: §f" + govName,
                            "§7Chunks: §f" + region.getClaimedChunks().size(),
                            "§7Properties: §f" + region.getPropertyIds().size()))
                    .consumer(e -> plugin.getGUIManager().openGUI(new PresidentRegionDetailGUI(plugin, region.getRegionId(), nationId), (Player) e.getWhoClicked()))
            );
            slot++;
        }

        addButton(49, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.EMERALD, "§aCreate Region",
                        "§7Create a new region within your nation."))
                .consumer(e -> startRegionCreation((Player) e.getWhoClicked()))
        );

        addButton(45, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "§cBack"))
                .consumer(e -> plugin.getGUIManager().openGUI(new NationManagementGUI(plugin, nationId), (Player) e.getWhoClicked()))
        );

        super.decorate(player);
    }

    private void startRegionCreation(Player player) {
        plugin.getChatInputManager().requestInput(player, "§eEnter the name for the new region:", name -> {
            if (name.isEmpty()) { player.sendMessage("§cRegion name cannot be empty."); return; }
            NationData nation = plugin.getNationManager().loadNation(nationId);
            List<CharacterData> memberChars = nation == null ? List.of() : nation.getMemberUUIDs().stream()
                    .map(u -> plugin.getCharacterManager().getCharacter(u))
                    .filter(c -> c != null)
                    .collect(Collectors.toList());

            plugin.getServer().getScheduler().runTask(plugin, () ->
                plugin.getGUIManager().openGUI(new NationCharacterSelectGUI(plugin, "§eSelect Governor for " + name, memberChars, govChar -> {
                    RegionData region = plugin.getNationManager().createRegion(nationId, name, govChar.getPlayerUuid());
                    player.sendMessage("§aRegion §6" + name + "§a created! Governor: §e" + govChar.getFirstName() + " " + govChar.getLastName() + "§a.");
                    Player govPlayer = Bukkit.getPlayer(govChar.getPlayerUuid());
                    if (govPlayer != null) govPlayer.sendMessage("§eYou are now governor of region §6" + name + "§e! Use §6/mtr§e to manage it.");
                    plugin.getGUIManager().openGUI(new RegionsGUI(plugin, nationId), player);
                }, new RegionsGUI(plugin, nationId), 0), player)
            );
        });
    }

    private String resolveCharName(UUID uuid) {
        if (uuid == null) return "§7Unassigned";
        CharacterData c = plugin.getCharacterManager().getCharacter(uuid);
        return c != null ? c.getFirstName() + " " + c.getLastName() : "§7Unknown";
    }
}
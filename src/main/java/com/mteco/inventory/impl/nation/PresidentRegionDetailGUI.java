package com.mteco.inventory.impl.nation;

import com.cryptomorin.xseries.XMaterial;
import com.mteco.MTeco;
import com.mteco.data.CharacterData;
import com.mteco.data.NationData;
import com.mteco.data.RegionData;
import com.mteco.inventory.InventoryButton;
import com.mteco.inventory.InventoryGUI;
import com.mteco.util.ItemUtil;
import com.mteco.util.CurrencyUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class PresidentRegionDetailGUI extends InventoryGUI {
    private final MTeco plugin;
    private final UUID regionId;
    private final UUID nationId;

    public PresidentRegionDetailGUI(MTeco plugin, UUID regionId, UUID nationId) {
        this.plugin = plugin;
        this.regionId = regionId;
        this.nationId = nationId;
    }

    @Override
    protected Inventory createInventory() {
        RegionData region = plugin.getNationManager().loadRegion(regionId);
        String name = region != null ? region.getName() : "Region";
        return Bukkit.createInventory(null, 27, "§6" + name);
    }

    @Override
    public void decorate(Player player) {
        fillGlass(27, XMaterial.BLUE_STAINED_GLASS_PANE);

        RegionData region = plugin.getNationManager().loadRegion(regionId);
        if (region == null) { super.decorate(player); return; }

        String govName = resolveCharName(region.getGovernorUUID());
        addButton(4, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.MAP, "§6" + region.getName(),
                        "§7Governor: §f" + govName,
                        "§7Chunks: §f" + region.getClaimedChunks().size(),
                        "§7Properties: §f" + region.getPropertyIds().size(),
                        "§7Chunk Rate: §e" + CurrencyUtil.symbol() + String.format("%.2f", region.getPropertyChunkRate())))
                .consumer(e -> {})
        );

        NationData nation = plugin.getNationManager().loadNation(nationId);
        List<CharacterData> memberChars = nation == null ? List.of() : nation.getMemberUUIDs().stream()
                .map(u -> plugin.getCharacterManager().getCharacter(u))
                .filter(c -> c != null)
                .collect(Collectors.toList());

        addButton(11, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.PLAYER_HEAD, "§eChange Governor",
                        "§7Current: §f" + govName))
                .consumer(e -> {
                    Player p = (Player) e.getWhoClicked();
                    plugin.getGUIManager().openGUI(new NationCharacterSelectGUI(plugin, "§eSelect New Governor", memberChars, c -> {
                        RegionData r = plugin.getNationManager().loadRegion(regionId);
                        if (r == null) return;
                        r.setGovernorUUID(c.getPlayerUuid());
                        plugin.getNationManager().saveRegion(r);
                        p.sendMessage("§a" + c.getFirstName() + " " + c.getLastName() + " is now governor of §6" + r.getName() + "§a.");
                        plugin.getGUIManager().openGUI(new PresidentRegionDetailGUI(plugin, regionId, nationId), p);
                    }, new PresidentRegionDetailGUI(plugin, regionId, nationId), 0), p);
                })
        );

        addButton(9, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.NAME_TAG, "§eRename Region",
                        "§7Current: §6" + region.getName(),
                        "§7Click to change."))
                .consumer(e -> {
                    Player p = (Player) e.getWhoClicked();
                    plugin.getChatInputManager().requestInput(p, "§eEnter the new name for region §6" + region.getName() + "§e:", input -> {
                        String trimmed = input.trim();
                        if (trimmed.isEmpty()) { p.sendMessage("§cRegion name cannot be empty."); return; }
                        RegionData r = plugin.getNationManager().loadRegion(regionId);
                        if (r == null) return;
                        String old = r.getName();
                        r.setName(trimmed);
                        plugin.getNationManager().saveRegion(r);
                        p.sendMessage("§aRegion §6" + old + "§a renamed to §6" + trimmed + "§a.");
                        plugin.getGUIManager().openGUI(new PresidentRegionDetailGUI(plugin, regionId, nationId), p);
                    });
                })
        );

        addButton(13, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.GOLD_NUGGET, "§eSet Property Chunk Rate",
                        "§7Current: §a" + CurrencyUtil.symbol() + String.format("%.2f", region.getPropertyChunkRate()),
                        "§7Click to change."))
                .consumer(e -> {
                    Player p = (Player) e.getWhoClicked();
                    plugin.getChatInputManager().requestInput(p, "§eEnter new property chunk rate (e.g. 150.00):", input -> {
                        try {
                            double rate = Double.parseDouble(input);
                            RegionData r = plugin.getNationManager().loadRegion(regionId);
                            if (r == null) return;
                            r.setPropertyChunkRate(rate);
                            plugin.getNationManager().saveRegion(r);
                            p.sendMessage("§aProperty chunk rate set to §e" + CurrencyUtil.symbol() + String.format("%.2f", rate) + "§a.");
                        } catch (NumberFormatException ex) { p.sendMessage("§cInvalid number."); }
                    });
                })
        );

        if (plugin.getSettings().isPropertyPermissionsEnabled()) {
            addButton(17, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.COMPARATOR, "§ePermissions",
                            "§7Manage who can interact",
                            "§7in this region."))
                    .consumer(e -> {
                        Player p = (Player) e.getWhoClicked();
                        RegionData r = plugin.getNationManager().loadRegion(regionId);
                        if (r == null) return;
                        plugin.getGUIManager().openGUI(new PropertyPermissionsGUI(plugin,
                                r.getPermsSameRegion(), r.getPermsSameNation(), r.getPermsForeign(),
                                () -> plugin.getNationManager().saveRegion(r),
                                back -> plugin.getGUIManager().openGUI(new PresidentRegionDetailGUI(plugin, regionId, nationId), back),
                                r.getDeniedMobs(),
                                () -> plugin.getNationManager().saveRegion(r),
                                back -> plugin.getGUIManager().openGUI(new PresidentRegionDetailGUI(plugin, regionId, nationId), back)), p);
                    })
            );
        }

        addButton(15, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.BARRIER, "§4Delete Region",
                        "§7Permanently delete this region.",
                        "§7All properties and pending purchase",
                        "§7requests will also be removed.",
                        "",
                        "§cClick to confirm by typing the name."))
                .consumer(e -> {
                    Player p = (Player) e.getWhoClicked();
                    String regionName = region.getName();
                    p.closeInventory();
                    plugin.getChatInputManager().requestInput(p, "§cType the region name §6" + regionName + "§c to confirm deletion, or anything else to cancel:", input -> {
                        if (!input.equalsIgnoreCase(regionName)) {
                            p.sendMessage("§7Region deletion cancelled.");
                            plugin.getServer().getScheduler().runTask(plugin, () ->
                                plugin.getGUIManager().openGUI(new PresidentRegionDetailGUI(plugin, regionId, nationId), p));
                            return;
                        }
                        plugin.getNationManager().deleteRegionAndCleanup(regionId);
                        p.sendMessage("§aRegion §6" + regionName + "§a has been deleted.");
                        plugin.getServer().getScheduler().runTask(plugin, () ->
                            plugin.getGUIManager().openGUI(new RegionsGUI(plugin, nationId), p));
                    });
                })
        );

        addButton(26, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "§cBack"))
                .consumer(e -> plugin.getGUIManager().openGUI(new RegionsGUI(plugin, nationId), (Player) e.getWhoClicked()))
        );

        super.decorate(player);
    }

    private String resolveCharName(UUID uuid) {
        if (uuid == null) return "§7Unassigned";
        CharacterData c = plugin.getCharacterManager().getCharacter(uuid);
        return c != null ? c.getFirstName() + " " + c.getLastName() : "§7Unknown";
    }
}
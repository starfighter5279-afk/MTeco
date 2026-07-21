package com.dirt.managers;

import com.dirt.DirtEconomy;
import com.dirt.data.PropertyData;
import com.dirt.data.RegionData;
import com.dirt.data.WarData;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class WarManager {
    private final DirtEconomy plugin;
    private final File warsFolder;
    private final Map<UUID, WarData> warCache = new HashMap<>();

    public WarManager(DirtEconomy plugin) {
        this.plugin = plugin;
        this.warsFolder = new File(plugin.getDataFolder(), "data/MTN/wars");
        warsFolder.mkdirs();
        loadAllFromDisk();
    }

    public void reloadCache() {
        warCache.clear();
        loadAllFromDisk();
    }

    private void loadAllFromDisk() {
        File[] files = warsFolder.listFiles((d, n) -> n.endsWith(".yml"));
        if (files == null) return;
        for (File f : files) {
            try {
                UUID id = UUID.fromString(f.getName().replace(".yml", ""));
                YamlConfiguration cfg = YamlConfiguration.loadConfiguration(f);
                WarData war = new WarData();
                war.setWarId(id);
                String atk = cfg.getString("attackingNationId");
                if (atk != null) war.setAttackingNationId(UUID.fromString(atk));
                String def = cfg.getString("defendingNationId");
                if (def != null) war.setDefendingNationId(UUID.fromString(def));
                List<UUID> atkRegions = new ArrayList<>();
                for (String s : cfg.getStringList("attackerRegionIds")) {
                    try { atkRegions.add(UUID.fromString(s)); } catch (IllegalArgumentException ignored) {}
                }
                war.setAttackerRegionIds(atkRegions);
                List<UUID> tgtRegions = new ArrayList<>();
                for (String s : cfg.getStringList("targetRegionIds")) {
                    try { tgtRegions.add(UUID.fromString(s)); } catch (IllegalArgumentException ignored) {}
                }
                war.setTargetRegionIds(tgtRegions);
                warCache.put(id, war);
            } catch (IllegalArgumentException ignored) {}
        }
        plugin.getLogger().info("[WarManager] Cached " + warCache.size() + " wars.");
    }

    public WarData createWar(UUID attackingNationId, UUID defendingNationId,
                             List<UUID> attackerRegionIds, List<UUID> targetRegionIds) {
        WarData war = new WarData();
        war.setWarId(UUID.randomUUID());
        war.setAttackingNationId(attackingNationId);
        war.setDefendingNationId(defendingNationId);
        war.setAttackerRegionIds(new ArrayList<>(attackerRegionIds));
        war.setTargetRegionIds(new ArrayList<>(targetRegionIds));
        saveWar(war);
        return war;
    }

    public WarData loadWar(UUID warId) {
        if (warId == null) return null;
        return warCache.get(warId);
    }

    public void saveWar(WarData war) {
        warCache.put(war.getWarId(), war);
        File file = new File(warsFolder, war.getWarId() + ".yml");
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("attackingNationId", war.getAttackingNationId() != null ? war.getAttackingNationId().toString() : null);
        cfg.set("defendingNationId", war.getDefendingNationId() != null ? war.getDefendingNationId().toString() : null);
        List<String> atk = new ArrayList<>();
        for (UUID u : war.getAttackerRegionIds()) atk.add(u.toString());
        cfg.set("attackerRegionIds", atk);
        List<String> tgt = new ArrayList<>();
        for (UUID u : war.getTargetRegionIds()) tgt.add(u.toString());
        cfg.set("targetRegionIds", tgt);
        try { cfg.save(file); } catch (IOException e) { e.printStackTrace(); }
    }

    public void deleteWar(UUID warId) {
        warCache.remove(warId);
        new File(warsFolder, warId + ".yml").delete();
    }

    public List<WarData> getAllWars() {
        return new ArrayList<>(warCache.values());
    }

    public List<WarData> getWarsByAttackingNation(UUID nationId) {
        List<WarData> list = new ArrayList<>();
        for (WarData w : warCache.values()) {
            if (nationId.equals(w.getAttackingNationId())) list.add(w);
        }
        return list;
    }

    public List<WarData> getWarsByDefendingNation(UUID nationId) {
        List<WarData> list = new ArrayList<>();
        for (WarData w : warCache.values()) {
            if (nationId.equals(w.getDefendingNationId())) list.add(w);
        }
        return list;
    }

    public List<WarData> getWarsByNation(UUID nationId) {
        List<WarData> list = new ArrayList<>();
        for (WarData w : warCache.values()) {
            if (nationId.equals(w.getAttackingNationId()) || nationId.equals(w.getDefendingNationId())) {
                list.add(w);
            }
        }
        return list;
    }

    public Set<UUID> getAttackerMembers(WarData war) {
        Set<UUID> members = new HashSet<>();
        for (UUID regionId : war.getAttackerRegionIds()) {
            for (PropertyData prop : plugin.getNationManager().getPropertiesByRegion(regionId)) {
                if (prop.getOwnerUUID() != null) members.add(prop.getOwnerUUID());
            }
        }
        return members;
    }

    public void surrender(WarData war) {
        for (UUID regionId : war.getTargetRegionIds()) {
            RegionData region = plugin.getNationManager().loadRegion(regionId);
            if (region == null) continue;
            UUID oldNationId = region.getNationId();
            region.setNationId(war.getAttackingNationId());
            plugin.getNationManager().saveRegion(region);

            com.dirt.data.NationData oldNation = plugin.getNationManager().loadNation(oldNationId);
            if (oldNation != null) {
                oldNation.getRegionIds().remove(regionId);
                plugin.getNationManager().saveNation(oldNation);
            }
            com.dirt.data.NationData newNation = plugin.getNationManager().loadNation(war.getAttackingNationId());
            if (newNation != null) {
                if (!newNation.getRegionIds().contains(regionId)) newNation.getRegionIds().add(regionId);
                plugin.getNationManager().saveNation(newNation);
            }
        }
        deleteWar(war.getWarId());
    }
}
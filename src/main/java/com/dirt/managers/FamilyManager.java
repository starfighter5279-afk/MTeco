package com.dirt.managers;

import com.dirt.DirtEconomy;
import com.dirt.data.FamilyData;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class FamilyManager {
    private final DirtEconomy plugin;
    private final File dataFolder;
    private final Map<UUID, FamilyData> familyCache = new HashMap<>();

    public FamilyManager(DirtEconomy plugin) {
        this.plugin = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "data/MTC/families");
        this.dataFolder.mkdirs();
        loadAllFromDisk();
    }

    public void reloadCache() {
        familyCache.clear();
        loadAllFromDisk();
    }

    private void loadAllFromDisk() {
        File[] files = dataFolder.listFiles((d, name) -> name.endsWith(".yml"));
        if (files == null) return;
        for (File file : files) {
            try {
                UUID id = UUID.fromString(file.getName().replace(".yml", ""));
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                FamilyData data = new FamilyData();
                data.setFamilyId(id);
                String s1 = config.getString("spouse1");
                if (s1 != null) data.setSpouse1(UUID.fromString(s1));
                String s2 = config.getString("spouse2");
                if (s2 != null) data.setSpouse2(UUID.fromString(s2));
                List<UUID> children = new ArrayList<>();
                for (String c : config.getStringList("children")) {
                    try { children.add(UUID.fromString(c)); } catch (IllegalArgumentException ignored) {}
                }
                data.setChildren(children);
                familyCache.put(id, data);
            } catch (IllegalArgumentException ignored) {}
        }
        plugin.getLogger().info("[FamilyManager] Cached " + familyCache.size() + " families.");
    }

    public FamilyData createFamily(UUID spouse1, UUID spouse2) {
        FamilyData family = new FamilyData();
        family.setFamilyId(UUID.randomUUID());
        family.setSpouse1(spouse1);
        family.setSpouse2(spouse2);
        saveFamily(family);
        return family;
    }

    public FamilyData loadFamily(UUID familyId) {
        if (familyId == null) return null;
        return familyCache.get(familyId);
    }

    public void saveFamily(FamilyData data) {
        familyCache.put(data.getFamilyId(), data);
        File file = new File(dataFolder, data.getFamilyId() + ".yml");
        YamlConfiguration config = new YamlConfiguration();
        config.set("spouse1", data.getSpouse1() != null ? data.getSpouse1().toString() : null);
        config.set("spouse2", data.getSpouse2() != null ? data.getSpouse2().toString() : null);
        List<String> children = new ArrayList<>();
        for (UUID c : data.getChildren()) {
            children.add(c.toString());
        }
        config.set("children", children);
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void deleteFamily(UUID familyId) {
        familyCache.remove(familyId);
        new File(dataFolder, familyId + ".yml").delete();
    }
}
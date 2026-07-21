package com.dirt.managers;

import com.dirt.DirtEconomy;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ChestProtectionManager {
    private final File dataFile;
    // key: "world|x|y|z", value: inheritor UUID string or "NONE"
    private final Map<String, String> protectedChests = new HashMap<>();

    public ChestProtectionManager(DirtEconomy plugin) {
        this.dataFile = new File(plugin.getDataFolder(), "data/protected_chests.yml");
        load();
    }

    private String locationKey(Location loc) {
        return loc.getWorld().getName() + "|" + loc.getBlockX() + "|" + loc.getBlockY() + "|" + loc.getBlockZ();
    }

    public void addProtectedChest(Location loc, UUID inheritorUUID) {
        protectedChests.put(locationKey(loc), inheritorUUID != null ? inheritorUUID.toString() : "NONE");
        save();
    }

    public boolean isProtectedChest(Location loc) {
        return protectedChests.containsKey(locationKey(loc));
    }

    public UUID getInheritor(Location loc) {
        String val = protectedChests.get(locationKey(loc));
        if (val == null || val.equals("NONE")) return null;
        return UUID.fromString(val);
    }

    @SuppressWarnings("unchecked")
    private void load() {
        if (!dataFile.exists()) return;
        YamlConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
        List<?> raw = config.getList("chests");
        if (raw == null) return;
        for (Object obj : raw) {
            if (obj instanceof Map) {
                Map<?, ?> item = (Map<?, ?>) obj;
                String loc = (String) item.get("loc");
                String inheritor = (String) item.get("inheritor");
                if (loc != null && inheritor != null) {
                    protectedChests.put(loc, inheritor);
                }
            }
        }
    }

    private void save() {
        YamlConfiguration config = new YamlConfiguration();
        List<Map<String, String>> chestList = new ArrayList<>();
        for (Map.Entry<String, String> entry : protectedChests.entrySet()) {
            Map<String, String> item = new HashMap<>();
            item.put("loc", entry.getKey());
            item.put("inheritor", entry.getValue());
            chestList.add(item);
        }
        config.set("chests", chestList);
        try {
            config.save(dataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
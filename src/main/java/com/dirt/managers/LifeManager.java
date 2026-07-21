package com.dirt.managers;

import com.dirt.DirtEconomy;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class LifeManager {
    private final DirtEconomy plugin;
    private final File dataFile;
    private final Map<UUID, Integer> tokenCache = new HashMap<>();
    private final Map<UUID, String> pendingDeathScreens = new HashMap<>();
    private final Set<UUID> processingDeath = new HashSet<>();

    public LifeManager(DirtEconomy plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "data/MTC/life_tokens.yml");
        this.dataFile.getParentFile().mkdirs();
        loadTokens();
    }

    private void loadTokens() {
        if (!dataFile.exists()) return;
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(dataFile);
        for (String key : cfg.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                tokenCache.put(uuid, cfg.getInt(key));
            } catch (IllegalArgumentException ignored) {}
        }
        plugin.getLogger().info("[LifeManager] Loaded " + tokenCache.size() + " life token records.");
    }

    private void saveTokens() {
        YamlConfiguration cfg = new YamlConfiguration();
        for (Map.Entry<UUID, Integer> entry : tokenCache.entrySet()) {
            cfg.set(entry.getKey().toString(), entry.getValue());
        }
        try {
            cfg.save(dataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int getTokens(UUID uuid) {
        return tokenCache.getOrDefault(uuid, 0);
    }

    public void setTokens(UUID uuid, int amount) {
        tokenCache.put(uuid, Math.max(0, amount));
        saveTokens();
    }

    public void addTokens(UUID uuid, int amount) {
        setTokens(uuid, getTokens(uuid) + amount);
    }

    public boolean useToken(UUID uuid) {
        int current = getTokens(uuid);
        if (current <= 0) return false;
        setTokens(uuid, current - 1);
        return true;
    }

    public void setPendingDeathScreen(UUID uuid, String causeOfDeath) {
        pendingDeathScreens.put(uuid, causeOfDeath);
    }

    public String getPendingDeathCause(UUID uuid) {
        return pendingDeathScreens.get(uuid);
    }

    public String consumePendingDeathScreen(UUID uuid) {
        return pendingDeathScreens.remove(uuid);
    }

    public boolean hasPendingDeathScreen(UUID uuid) {
        return pendingDeathScreens.containsKey(uuid);
    }

    public void setProcessingDeath(UUID uuid) {
        processingDeath.add(uuid);
    }

    public void clearProcessingDeath(UUID uuid) {
        processingDeath.remove(uuid);
    }

    public boolean isProcessingDeath(UUID uuid) {
        return processingDeath.contains(uuid);
    }
}
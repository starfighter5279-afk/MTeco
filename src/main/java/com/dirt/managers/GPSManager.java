package com.dirt.managers;

import com.dirt.DirtEconomy;
import com.dirt.data.GPSLocation;
import com.dirt.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class GPSManager {
    private final DirtEconomy plugin;
    private final File dataFolder;
    private final Map<UUID, GPSLocation> locationCache = new HashMap<>();
    private final Map<UUID, ScheduledTask> activeTrails = new HashMap<>();

    public GPSManager(DirtEconomy plugin) {
        this.plugin = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "data/GPS");
        dataFolder.mkdirs();
        loadAllFromDisk();
    }

    public void reloadCache() {
        locationCache.clear();
        stopAllTrails();
        loadAllFromDisk();
    }

    private void loadAllFromDisk() {
        File[] files = dataFolder.listFiles((d, n) -> n.endsWith(".yml"));
        if (files == null) return;
        for (File f : files) {
            try {
                UUID id = UUID.fromString(f.getName().replace(".yml", ""));
                GPSLocation loc = readFile(id, f);
                if (loc != null) locationCache.put(id, loc);
            } catch (IllegalArgumentException ignored) {}
        }
        plugin.getLogger().info("[GPSManager] Cached " + locationCache.size() + " GPS locations.");
    }

    public GPSLocation createLocation(String name, String description, UUID creatorUuid, UUID businessId,
                                       Location loc, boolean global) {
        GPSLocation gps = new GPSLocation();
        gps.setLocationId(UUID.randomUUID());
        gps.setName(name);
        gps.setDescription(description);
        gps.setCreatorUuid(creatorUuid);
        gps.setBusinessId(businessId);
        gps.setWorld(loc.getWorld().getName());
        gps.setX(loc.getX());
        gps.setY(loc.getY());
        gps.setZ(loc.getZ());
        gps.setGlobal(global);
        gps.setCreatedAt(System.currentTimeMillis());
        saveLocation(gps);
        return gps;
    }

    public void saveLocation(GPSLocation gps) {
        locationCache.put(gps.getLocationId(), gps);
        File file = new File(dataFolder, gps.getLocationId() + ".yml");
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("name", gps.getName());
        cfg.set("description", gps.getDescription());
        cfg.set("creatorUuid", gps.getCreatorUuid() != null ? gps.getCreatorUuid().toString() : null);
        cfg.set("businessId", gps.getBusinessId() != null ? gps.getBusinessId().toString() : null);
        cfg.set("world", gps.getWorld());
        cfg.set("x", gps.getX());
        cfg.set("y", gps.getY());
        cfg.set("z", gps.getZ());
        cfg.set("global", gps.isGlobal());
        cfg.set("createdAt", gps.getCreatedAt());
        try { cfg.save(file); } catch (IOException e) { e.printStackTrace(); }
    }

    public void deleteLocation(UUID id) {
        locationCache.remove(id);
        new File(dataFolder, id + ".yml").delete();
    }

    public GPSLocation getLocation(UUID id) {
        return locationCache.get(id);
    }

    public GPSLocation findByName(String name) {
        for (GPSLocation loc : locationCache.values()) {
            if (loc.getName().equalsIgnoreCase(name)) return loc;
        }
        return null;
    }

    public List<GPSLocation> getGlobalLocations() {
        List<GPSLocation> list = new ArrayList<>();
        for (GPSLocation loc : locationCache.values()) {
            if (loc.isGlobal() && loc.getBusinessId() == null) list.add(loc);
        }
        return list;
    }

    public List<GPSLocation> getPersonalLocations(UUID playerUuid) {
        List<GPSLocation> list = new ArrayList<>();
        for (GPSLocation loc : locationCache.values()) {
            if (!loc.isGlobal() && playerUuid.equals(loc.getCreatorUuid())) list.add(loc);
        }
        return list;
    }

    public List<GPSLocation> getBusinessLocations() {
        List<GPSLocation> list = new ArrayList<>();
        for (GPSLocation loc : locationCache.values()) {
            if (loc.getBusinessId() != null) list.add(loc);
        }
        return list;
    }

    public List<GPSLocation> getBusinessLocations(UUID businessId) {
        List<GPSLocation> list = new ArrayList<>();
        for (GPSLocation loc : locationCache.values()) {
            if (businessId.equals(loc.getBusinessId())) list.add(loc);
        }
        return list;
    }

    public List<GPSLocation> getLocationsByCreator(UUID creatorUuid) {
        List<GPSLocation> list = new ArrayList<>();
        for (GPSLocation loc : locationCache.values()) {
            if (creatorUuid.equals(loc.getCreatorUuid())) list.add(loc);
        }
        return list;
    }

    public List<GPSLocation> getAccessibleLocations(UUID playerUuid) {
        List<GPSLocation> list = new ArrayList<>();
        for (GPSLocation loc : locationCache.values()) {
            if (loc.isGlobal() || playerUuid.equals(loc.getCreatorUuid())) {
                list.add(loc);
            }
        }
        return list;
    }

    public void startTrail(Player player, GPSLocation gps) {
        stopTrail(player.getUniqueId());
        World world = Bukkit.getWorld(gps.getWorld());
        if (world == null) {
            player.sendMessage("\u00a7cThe destination world is not loaded.");
            return;
        }
        Location dest = new Location(world, gps.getX(), gps.getY(), gps.getZ());
        int interval = plugin.getSettings().getGpsParticleIntervalTicks();
        int maxDist = plugin.getSettings().getGpsParticleDistanceBlocks();
        double arrivalRadius = plugin.getSettings().getGpsArrivalRadius();

        ScheduledTask task = plugin.getPlatformScheduler().runAtEntityTimer(player, () -> {
            if (!player.isOnline()) {
                stopTrail(player.getUniqueId());
                return;
            }
            if (!player.getWorld().equals(world)) {
                player.sendMessage("\u00a7eGPS: You are in a different world than your destination.");
                stopTrail(player.getUniqueId());
                return;
            }
            Location pLoc = player.getLocation().add(0, 1.2, 0);
            double distToDest = pLoc.distance(dest);
            if (distToDest <= arrivalRadius) {
                player.sendMessage("\u00a7aGPS: You have arrived at \u00a7e" + gps.getName() + "\u00a7a!");
                stopTrail(player.getUniqueId());
                return;
            }
            double trailDist = Math.min(distToDest, maxDist);
            org.bukkit.util.Vector dir = dest.toVector().subtract(pLoc.toVector()).normalize();
            for (double d = 2.0; d <= trailDist; d += 1.5) {
                Location particleLoc = pLoc.clone().add(dir.clone().multiply(d));
                double progress = d / trailDist;
                if (progress < 0.33) {
                    player.spawnParticle(Particle.HAPPY_VILLAGER, particleLoc, 1, 0, 0, 0, 0);
                } else if (progress < 0.66) {
                    player.spawnParticle(Particle.COMPOSTER, particleLoc, 1, 0, 0, 0, 0);
                } else {
                    player.spawnParticle(Particle.END_ROD, particleLoc, 1, 0, 0, 0, 0);
                }
            }
        }, 0L, interval);

        activeTrails.put(player.getUniqueId(), task);
        player.sendMessage("\u00a7aGPS: Navigating to \u00a7e" + gps.getName() + " \u00a7a(" + (int) player.getLocation().distance(dest) + " blocks away)");
    }

    public void stopTrail(UUID playerUuid) {
        ScheduledTask task = activeTrails.remove(playerUuid);
        if (task != null) task.cancel();
    }

    public boolean hasActiveTrail(UUID playerUuid) {
        return activeTrails.containsKey(playerUuid);
    }

    public void stopAllTrails() {
        for (ScheduledTask task : activeTrails.values()) task.cancel();
        activeTrails.clear();
    }

    private GPSLocation readFile(UUID id, File file) {
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        GPSLocation loc = new GPSLocation();
        loc.setLocationId(id);
        loc.setName(cfg.getString("name", "Unknown"));
        loc.setDescription(cfg.getString("description", ""));
        String creator = cfg.getString("creatorUuid");
        if (creator != null) { try { loc.setCreatorUuid(UUID.fromString(creator)); } catch (IllegalArgumentException ignored) {} }
        String biz = cfg.getString("businessId");
        if (biz != null) { try { loc.setBusinessId(UUID.fromString(biz)); } catch (IllegalArgumentException ignored) {} }
        loc.setWorld(cfg.getString("world", "world"));
        loc.setX(cfg.getDouble("x"));
        loc.setY(cfg.getDouble("y"));
        loc.setZ(cfg.getDouble("z"));
        loc.setGlobal(cfg.getBoolean("global", false));
        loc.setCreatedAt(cfg.getLong("createdAt", 0));
        return loc;
    }
}
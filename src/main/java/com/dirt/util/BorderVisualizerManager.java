package com.dirt.util;

import com.dirt.DirtEconomy;
import com.dirt.data.BusinessPropertyData;
import com.dirt.data.PropertyData;
import com.dirt.data.RegionData;
import com.dirt.managers.NationManager;
import com.cryptomorin.xseries.particles.XParticle;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class BorderVisualizerManager {

    // Precomputed per-region display colors to distinguish neighboring regions
    private static final Color[] REGION_COLORS = {
        Color.fromRGB(255, 80,  80),   // red
        Color.fromRGB(80,  200, 255),  // cyan
        Color.fromRGB(80,  255, 80),   // green
        Color.fromRGB(255, 220, 50),   // yellow
        Color.fromRGB(200, 80,  255),  // purple
        Color.fromRGB(255, 140, 0),    // orange
        Color.fromRGB(255, 255, 255),  // white
        Color.fromRGB(80,  255, 200),  // teal
    };

    private final DirtEconomy plugin;
    private final Set<UUID> activePlayers = new HashSet<>();
    private final Set<UUID> pbordersPlayers = new HashSet<>();
    // Cache: regionId -> color index so each region keeps a stable color per server session
    private final Map<UUID, Integer> regionColorIndex = new HashMap<>();
    // Cache: propertyId -> color index so each property keeps a stable color per server session
    private final Map<UUID, Integer> propertyColorIndex = new HashMap<>();
    private int colorCounter = 0;
    private int propertyColorCounter = 0;

    public BorderVisualizerManager(DirtEconomy plugin) {
        this.plugin = plugin;
        long tickRate = plugin.getSettings().getBorderTickRate();
        plugin.getPlatformScheduler().runGlobalTimer(this::tick, tickRate, tickRate);
    }

    /** Toggles borders for the player. Returns true if now enabled, false if disabled. */
    public boolean toggle(Player player) {
        UUID uuid = player.getUniqueId();
        if (activePlayers.contains(uuid)) {
            activePlayers.remove(uuid);
            return false;
        } else {
            activePlayers.add(uuid);
            return true;
        }
    }

    /** Toggles property borders for the player. Returns true if now enabled, false if disabled. */
    public boolean togglePBorders(Player player) {
        UUID uuid = player.getUniqueId();
        if (pbordersPlayers.contains(uuid)) {
            pbordersPlayers.remove(uuid);
            return false;
        } else {
            pbordersPlayers.add(uuid);
            return true;
        }
    }

    public void remove(UUID playerUUID) {
        activePlayers.remove(playerUUID);
        pbordersPlayers.remove(playerUUID);
    }

    private void tick() {
        if (!activePlayers.isEmpty()) {
            // Build a map of chunkKey -> regionId once per tick, reused for all active players
            Map<String, UUID> chunkToRegion = new HashMap<>();
            for (RegionData region : plugin.getNationManager().getAllRegions()) {
                for (String key : region.getClaimedChunks()) {
                    chunkToRegion.put(key, region.getRegionId());
                }
            }

            for (UUID uuid : activePlayers) {
                Player player = plugin.getServer().getPlayer(uuid);
                if (player == null) continue;
                renderForPlayer(player, chunkToRegion);
            }
        }

        if (!pbordersPlayers.isEmpty()) {
            // Build chunkKey -> propertyId map for both player and business properties
            Map<String, UUID> chunkToProperty = new HashMap<>();
            for (PropertyData p : plugin.getNationManager().getAllProperties()) {
                if (p.getOwnerUUID() == null) continue;
                for (String key : p.getChunks()) {
                    chunkToProperty.put(key, p.getPropertyId());
                }
            }
            for (BusinessPropertyData bp : getAllBusinessProperties()) {
                for (String key : bp.getChunks()) {
                    chunkToProperty.put(key, bp.getPropertyId());
                }
            }

            for (UUID uuid : pbordersPlayers) {
                Player player = plugin.getServer().getPlayer(uuid);
                if (player == null) continue;
                renderPBordersForPlayer(player, chunkToProperty);
            }
        }
    }

    private List<BusinessPropertyData> getAllBusinessProperties() {
        java.util.ArrayList<BusinessPropertyData> list = new java.util.ArrayList<>();
        File folder = new File(plugin.getDataFolder(), "businessproperties");
        File[] files = folder.listFiles((d, n) -> n.endsWith(".yml"));
        if (files == null) return list;
        for (File f : files) {
            try {
                UUID id = UUID.fromString(f.getName().replace(".yml", ""));
                BusinessPropertyData p = plugin.getBusinessManager().loadProperty(id);
                if (p != null) list.add(p);
            } catch (IllegalArgumentException ignored) {}
        }
        return list;
    }

    private void renderForPlayer(Player player, Map<String, UUID> chunkToRegion) {
        World world = player.getWorld();
        String worldName = world.getName();
        int playerCX = player.getLocation().getBlockX() >> 4;
        int playerCZ = player.getLocation().getBlockZ() >> 4;
        double y = player.getLocation().getY() + 0.5;

        int radius = plugin.getSettings().getBorderViewRadiusChunks();

        for (int cx = playerCX - radius; cx <= playerCX + radius; cx++) {
            for (int cz = playerCZ - radius; cz <= playerCZ + radius; cz++) {
                String key = NationManager.chunkKey(worldName, cx, cz);
                UUID regionId = chunkToRegion.get(key);
                if (regionId == null) continue;

                Color color = getColor(regionId);

                // Check 4 neighbors: N (-Z), S (+Z), W (-X), E (+X)
                String northKey = NationManager.chunkKey(worldName, cx, cz - 1);
                if (!regionId.equals(chunkToRegion.get(northKey))) {
                    spawnEdgeNS(world, color, cx, cz, false, y);
                }
                String southKey = NationManager.chunkKey(worldName, cx, cz + 1);
                if (!regionId.equals(chunkToRegion.get(southKey))) {
                    spawnEdgeNS(world, color, cx, cz, true, y);
                }
                String westKey = NationManager.chunkKey(worldName, cx - 1, cz);
                if (!regionId.equals(chunkToRegion.get(westKey))) {
                    spawnEdgeEW(world, color, cx, cz, false, y);
                }
                String eastKey = NationManager.chunkKey(worldName, cx + 1, cz);
                if (!regionId.equals(chunkToRegion.get(eastKey))) {
                    spawnEdgeEW(world, color, cx, cz, true, y);
                }
            }
        }
    }

    private void renderPBordersForPlayer(Player player, Map<String, UUID> chunkToProperty) {
        World world = player.getWorld();
        String worldName = world.getName();
        int playerCX = player.getLocation().getBlockX() >> 4;
        int playerCZ = player.getLocation().getBlockZ() >> 4;
        double y = player.getLocation().getY() + 0.5;

        int radius = plugin.getSettings().getBorderViewRadiusChunks();

        for (int cx = playerCX - radius; cx <= playerCX + radius; cx++) {
            for (int cz = playerCZ - radius; cz <= playerCZ + radius; cz++) {
                String key = NationManager.chunkKey(worldName, cx, cz);
                UUID propId = chunkToProperty.get(key);
                if (propId == null) continue;

                Color color = getPropertyColor(propId);

                String northKey = NationManager.chunkKey(worldName, cx, cz - 1);
                if (!propId.equals(chunkToProperty.get(northKey))) {
                    spawnEdgeNS(world, color, cx, cz, false, y);
                }
                String southKey = NationManager.chunkKey(worldName, cx, cz + 1);
                if (!propId.equals(chunkToProperty.get(southKey))) {
                    spawnEdgeNS(world, color, cx, cz, true, y);
                }
                String westKey = NationManager.chunkKey(worldName, cx - 1, cz);
                if (!propId.equals(chunkToProperty.get(westKey))) {
                    spawnEdgeEW(world, color, cx, cz, false, y);
                }
                String eastKey = NationManager.chunkKey(worldName, cx + 1, cz);
                if (!propId.equals(chunkToProperty.get(eastKey))) {
                    spawnEdgeEW(world, color, cx, cz, true, y);
                }
            }
        }
    }

    // Spawns particles along the N or S edge of chunk (cx, cz)
    private void spawnEdgeNS(World world, Color color, int cx, int cz, boolean south, double y) {
        int baseX = cx * 16;
        int edgeZ = south ? cz * 16 + 16 : cz * 16;
        Particle.DustOptions dust = new Particle.DustOptions(color, 1.0f);
        final World finalWorld = world;
        final double finalY = y;
        final int finalBaseX = baseX;
        final int finalEdgeZ = edgeZ;
        final Particle.DustOptions finalDust = dust;
        for (int dx = 0; dx < 16; dx += 2) {
            final int finalDx = dx;
            XParticle.of("REDSTONE").ifPresent(p -> finalWorld.spawnParticle(p.get(), finalBaseX + finalDx + 0.5, finalY, finalEdgeZ, 1, 0, 0, 0, 0, finalDust));
        }
    }

    // Spawns particles along the W or E edge of chunk (cx, cz)
    private void spawnEdgeEW(World world, Color color, int cx, int cz, boolean east, double y) {
        int baseZ = cz * 16;
        int edgeX = east ? cx * 16 + 16 : cx * 16;
        Particle.DustOptions dust = new Particle.DustOptions(color, 1.0f);
        final World finalWorld = world;
        final double finalY = y;
        final int finalBaseZ = baseZ;
        final int finalEdgeX = edgeX;
        final Particle.DustOptions finalDust = dust;
        for (int dz = 0; dz < 16; dz += 2) {
            final int finalDz = dz;
            XParticle.of("REDSTONE").ifPresent(p -> finalWorld.spawnParticle(p.get(), finalEdgeX, finalY, finalBaseZ + finalDz + 0.5, 1, 0, 0, 0, 0, finalDust));
        }
    }

    private Color getColor(UUID regionId) {
        return REGION_COLORS[regionColorIndex.computeIfAbsent(regionId, k -> colorCounter++ % REGION_COLORS.length)];
    }

    private Color getPropertyColor(UUID propertyId) {
        return REGION_COLORS[propertyColorIndex.computeIfAbsent(propertyId, k -> propertyColorCounter++ % REGION_COLORS.length)];
    }
}
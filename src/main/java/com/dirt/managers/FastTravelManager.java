package com.dirt.managers;

import com.dirt.DirtEconomy;
import com.dirt.data.BusinessData;
import com.dirt.data.BusinessPropertyData;
import com.dirt.data.FastTravelPoint;
import com.dirt.data.PropertyData;
import com.dirt.data.RegionData;
import com.dirt.data.TravelDestination;
import com.dirt.inventory.impl.travel.FastTravelTransitionGUI;
import com.dirt.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.cryptomorin.xseries.XMaterial;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public class FastTravelManager {
    private final DirtEconomy plugin;
    private final File dataFolder;
    private final File nicknameFolder;
    private final Map<String, FastTravelPoint> points = new HashMap<>();
    private final Map<UUID, Map<UUID, String>> playerNicknames = new HashMap<>();
    private final Map<UUID, TravelCast> activeCasts = new HashMap<>();
    private final Map<UUID, Boolean> transitionProtection = new HashMap<>();

    public FastTravelManager(DirtEconomy plugin) {
        this.plugin = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "data/FastTravel");
        dataFolder.mkdirs();
        this.nicknameFolder = new File(dataFolder, "nicknames");
        nicknameFolder.mkdirs();
        loadPoints();
    }

    public FastTravelPoint createPoint(Block block, UUID creatorUuid) {
        FastTravelPoint point = new FastTravelPoint();
        point.setPointId(UUID.randomUUID());
        point.setWorld(block.getWorld().getName());
        point.setX(block.getX());
        point.setY(block.getY());
        point.setZ(block.getZ());
        point.setCreatorUuid(creatorUuid);
        point.setCreatedAt(System.currentTimeMillis());
        point.setVisits(0);
        points.put(key(block.getLocation()), point);
        savePoint(point);
        return point;
    }

    public FastTravelPoint getPoint(Block block) {
        return points.get(key(block.getLocation()));
    }

    public boolean isPoint(Block block) {
        return getPoint(block) != null;
    }

    public void deletePoint(Block block) {
        FastTravelPoint point = points.remove(key(block.getLocation()));
        if (point != null) {
            new File(dataFolder, point.getPointId() + ".yml").delete();
        }
    }

    public List<TravelDestination> getPublicDestinations() {
        List<FastTravelPoint> sortedPoints = new ArrayList<>(points.values());
        sortedPoints.sort(Comparator.comparingLong(FastTravelPoint::getCreatedAt));
        List<TravelDestination> destinations = new ArrayList<>();
        for (FastTravelPoint point : sortedPoints) {
            Location location = pointLocation(point);
            if (location != null) {
                destinations.add(new TravelDestination(pointName(point), "Public campfire travel point", location, point.getPointId()));
            }
        }
        return destinations;
    }

    public List<TravelDestination> getFrequentDestinations() {
        List<FastTravelPoint> sortedPoints = new ArrayList<>(points.values());
        sortedPoints.sort(Comparator.comparingLong(FastTravelPoint::getVisits).reversed());
        List<TravelDestination> destinations = new ArrayList<>();
        for (FastTravelPoint point : sortedPoints) {
            if (destinations.size() == 5) break;
            if (point.getVisits() <= 0) break;
            Location location = pointLocation(point);
            if (location != null) {
                destinations.add(new TravelDestination(pointName(point), point.getVisits() + " visits", location, point.getPointId()));
            }
        }
        return destinations;
    }

    public List<TravelDestination> getPersonalPropertyDestinations(UUID playerUuid) {
        List<TravelDestination> destinations = new ArrayList<>();
        if (plugin.getNationManager() == null) return destinations;

        for (PropertyData property : plugin.getNationManager().getPropertiesByOwner(playerUuid)) {
            Location location = locationFromChunks(property.getChunks());
            if (location != null) {
                String name = property.getName() == null || property.getName().isEmpty() ? "Unnamed Property" : property.getName();
                destinations.add(new TravelDestination(name, property.getChunks().size() + " claimed chunks", location, null));
            }
        }
        return destinations;
    }

    public List<TravelDestination> getBusinessPropertyDestinations(UUID playerUuid) {
        List<TravelDestination> destinations = new ArrayList<>();
        if (plugin.getBusinessManager() == null) return destinations;

        Map<UUID, BusinessData> businesses = new LinkedHashMap<>();
        for (BusinessData business : plugin.getBusinessManager().getBusinessesByOwner(playerUuid)) {
            businesses.put(business.getBusinessId(), business);
        }
        for (BusinessData business : plugin.getBusinessManager().getBusinessesByEmployee(playerUuid)) {
            businesses.put(business.getBusinessId(), business);
        }

        for (BusinessData business : businesses.values()) {
            for (UUID propertyId : business.getPropertyIds()) {
                BusinessPropertyData property = plugin.getBusinessManager().loadProperty(propertyId);
                if (property == null) continue;
                Location location = locationFromChunks(property.getChunks());
                if (location == null) continue;
                String name = property.getName() == null || property.getName().isEmpty() ? business.getName() : property.getName();
                destinations.add(new TravelDestination(name, business.getName() + " workplace", location, null));
            }
        }
        return destinations;
    }

    public List<TravelDestination> getGovernorRegionDestinations(UUID playerUuid) {
        List<TravelDestination> destinations = new ArrayList<>();
        if (plugin.getNationManager() == null) return destinations;

        for (RegionData region : plugin.getNationManager().getRegionsByGovernor(playerUuid)) {
            Location location = locationFromChunks(region.getClaimedChunks());
            if (location != null) {
                destinations.add(new TravelDestination(region.getName(), "Center of your governed region", location, null));
            }
        }
        return destinations;
    }

    public boolean hasGovernorRegions(UUID playerUuid) {
        return plugin.getNationManager() != null && !plugin.getNationManager().getRegionsByGovernor(playerUuid).isEmpty();
    }

    public String getDestinationName(UUID playerUuid, TravelDestination destination) {
        if (destination.getFastTravelPointId() == null) return destination.getName();
        String nickname = playerNicknames.computeIfAbsent(playerUuid, this::loadNicknames).get(destination.getFastTravelPointId());
        return nickname == null ? destination.getName() : nickname;
    }

    public boolean setPointNickname(UUID playerUuid, UUID pointId, String nickname) {
        if (getPoint(pointId) == null) return false;

        String trimmedNickname = nickname.trim();
        if (trimmedNickname.isEmpty() || trimmedNickname.length() > 32) return false;

        Map<UUID, String> nicknames = playerNicknames.computeIfAbsent(playerUuid, this::loadNicknames);
        nicknames.put(pointId, trimmedNickname);
        saveNicknames(playerUuid, nicknames);
        return true;
    }

    public void beginTravel(Player player, TravelDestination destination) {
        if (activeCasts.containsKey(player.getUniqueId())) {
            player.sendMessage("§eA fast travel cast is already in progress.");
            return;
        }
        if (!isHoldingEnderPearl(player)) {
            player.sendMessage("§cHold an ender pearl in your main hand to fast travel.");
            return;
        }
        if (destination.getLocation() == null || destination.getLocation().getWorld() == null) {
            player.sendMessage("§cThat destination is currently unavailable.");
            return;
        }

        player.closeInventory();
        TravelCast cast = new TravelCast(player.getUniqueId(), destination);
        activeCasts.put(player.getUniqueId(), cast);
        cast.task = plugin.getPlatformScheduler().runAtEntityTimer(player, () -> tickCast(cast), 20L, 20L);
        player.sendMessage("§dFast travel to §f" + destination.getName() + " §dstarts in 10 seconds. Keep your ender pearl equipped.");
    }

    public boolean isTransitionProtected(Player player) {
        return transitionProtection.containsKey(player.getUniqueId());
    }

    private void tickCast(TravelCast cast) {
        Player player = Bukkit.getPlayer(cast.playerUuid);
        if (player == null || !player.isOnline()) {
            cancelCast(cast, null);
            return;
        }

        if (!cast.transitioning) {
            if (!isHoldingEnderPearl(player)) {
                cancelCast(cast, "§cFast travel cancelled because you are no longer holding an ender pearl.");
                return;
            }

            cast.remainingSeconds--;
            if (cast.remainingSeconds == 0) {
                cast.transitioning = true;
                cast.remainingSeconds = 3;
                transitionProtection.put(cast.playerUuid, true);
                plugin.getGUIManager().openGUI(new FastTravelTransitionGUI(plugin, cast.destination.getName()), player);
            }
            return;
        }

        cast.remainingSeconds--;
        if (cast.remainingSeconds == 0) {
            completeCast(cast, player);
        }
    }

    private void completeCast(TravelCast cast, Player player) {
        activeCasts.remove(cast.playerUuid);
        transitionProtection.remove(cast.playerUuid);
        if (cast.task != null) cast.task.cancel();

        player.closeInventory();
        plugin.getPlatformScheduler().teleport(player, cast.destination.getLocation(), () -> {
            if (cast.destination.getFastTravelPointId() != null) {
                FastTravelPoint point = getPoint(cast.destination.getFastTravelPointId());
                if (point != null) {
                    point.setVisits(point.getVisits() + 1);
                    savePoint(point);
                }
            }
            player.sendMessage("§aYou arrived at §f" + cast.destination.getName() + "§a.");
        });
    }

    private void cancelCast(TravelCast cast, String message) {
        activeCasts.remove(cast.playerUuid);
        transitionProtection.remove(cast.playerUuid);
        if (cast.task != null) cast.task.cancel();
        if (message != null) {
            Player player = Bukkit.getPlayer(cast.playerUuid);
            if (player != null) player.sendMessage(message);
        }
    }

    private boolean isHoldingEnderPearl(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        return item != null && XMaterial.matchXMaterial(item.getType()).name().equals("ENDER_PEARL");
    }

    private FastTravelPoint getPoint(UUID pointId) {
        for (FastTravelPoint point : points.values()) {
            if (point.getPointId().equals(pointId)) return point;
        }
        return null;
    }

    private Location pointLocation(FastTravelPoint point) {
        World world = Bukkit.getWorld(point.getWorld());
        if (world == null) return null;
        return new Location(world, point.getX() + 0.5, point.getY() + 1.0, point.getZ() + 0.5);
    }

    private Location locationFromChunks(List<String> chunks) {
        if (chunks == null || chunks.isEmpty()) return null;

        String[] first = chunks.get(0).split(",", 3);
        if (first.length != 3) return null;

        World world = Bukkit.getWorld(first[0]);
        if (world == null) return null;

        long totalX = 0;
        long totalZ = 0;
        int count = 0;
        for (String chunk : chunks) {
            String[] parts = chunk.split(",", 3);
            if (parts.length != 3 || !parts[0].equals(world.getName())) continue;
            try {
                totalX += Integer.parseInt(parts[1]) * 16L + 8L;
                totalZ += Integer.parseInt(parts[2]) * 16L + 8L;
                count++;
            } catch (NumberFormatException ignored) {
            }
        }
        if (count == 0) return null;

        int x = (int) (totalX / count);
        int z = (int) (totalZ / count);
        return new Location(world, x + 0.5, world.getHighestBlockYAt(x, z) + 1.0, z + 0.5);
    }

    private String pointName(FastTravelPoint point) {
        return "Campfire " + point.getX() + ", " + point.getZ();
    }

    private String key(Location location) {
        return location.getWorld().getName() + "," + location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ();
    }

    private Map<UUID, String> loadNicknames(UUID playerUuid) {
        Map<UUID, String> nicknames = new HashMap<>();
        File file = new File(nicknameFolder, playerUuid + ".yml");
        if (!file.exists()) return nicknames;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        for (String key : config.getKeys(false)) {
            try {
                String nickname = config.getString(key);
                if (nickname != null && !nickname.trim().isEmpty()) {
                    nicknames.put(UUID.fromString(key), nickname);
                }
            } catch (IllegalArgumentException ignored) {
            }
        }
        return nicknames;
    }

    private void saveNicknames(UUID playerUuid, Map<UUID, String> nicknames) {
        YamlConfiguration config = new YamlConfiguration();
        for (Map.Entry<UUID, String> entry : nicknames.entrySet()) {
            config.set(entry.getKey().toString(), entry.getValue());
        }
        try {
            config.save(new File(nicknameFolder, playerUuid + ".yml"));
        } catch (IOException exception) {
            plugin.getLogger().warning("Unable to save fast travel nicknames for " + playerUuid + ": " + exception.getMessage());
        }
    }

    private void loadPoints() {
        File[] files = dataFolder.listFiles((directory, name) -> name.endsWith(".yml"));
        if (files == null) return;

        for (File file : files) {
            try {
                UUID pointId = UUID.fromString(file.getName().replace(".yml", ""));
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                FastTravelPoint point = new FastTravelPoint();
                point.setPointId(pointId);
                point.setWorld(config.getString("world"));
                point.setX(config.getInt("x"));
                point.setY(config.getInt("y"));
                point.setZ(config.getInt("z"));
                String creatorUuid = config.getString("creatorUuid");
                if (creatorUuid != null) point.setCreatorUuid(UUID.fromString(creatorUuid));
                point.setCreatedAt(config.getLong("createdAt"));
                point.setVisits(config.getLong("visits"));
                if (point.getWorld() != null) points.put(point.getWorld() + "," + point.getX() + "," + point.getY() + "," + point.getZ(), point);
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    private void savePoint(FastTravelPoint point) {
        YamlConfiguration config = new YamlConfiguration();
        config.set("world", point.getWorld());
        config.set("x", point.getX());
        config.set("y", point.getY());
        config.set("z", point.getZ());
        config.set("creatorUuid", point.getCreatorUuid() == null ? null : point.getCreatorUuid().toString());
        config.set("createdAt", point.getCreatedAt());
        config.set("visits", point.getVisits());
        try {
            config.save(new File(dataFolder, point.getPointId() + ".yml"));
        } catch (IOException exception) {
            plugin.getLogger().warning("Unable to save fast travel point " + point.getPointId() + ": " + exception.getMessage());
        }
    }

    private static class TravelCast {
        private final UUID playerUuid;
        private final TravelDestination destination;
        private int remainingSeconds = 10;
        private boolean transitioning;
        private ScheduledTask task;

        private TravelCast(UUID playerUuid, TravelDestination destination) {
            this.playerUuid = playerUuid;
            this.destination = destination;
        }
    }
}
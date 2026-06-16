package com.mteco.managers;

import com.mteco.MTeco;
import com.mteco.data.CharacterData;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ProximityManager {

    private final MTeco plugin;
    private int taskId = -1;
    private final Map<String, UUID> trackedPlayers = new ConcurrentHashMap<>();
    private final Map<String, String> playerChannelAssignments = new ConcurrentHashMap<>();
    private final Set<String> managedChannelIds = ConcurrentHashMap.newKeySet();
    private int channelCounter = 0;

    public ProximityManager(MTeco plugin) {
        this.plugin = plugin;
    }

    public void start() {
        long interval = plugin.getSettings().getProximityUpdateIntervalTicks();
        taskId = plugin.getServer().getScheduler().runTaskTimerAsynchronously(
                plugin, this::tick, interval, interval).getTaskId();
        plugin.getLogger().info("[Proximity] Voice chat system started.");
    }

    public void stop() {
        if (taskId != -1) {
            plugin.getServer().getScheduler().cancelTask(taskId);
            taskId = -1;
        }
        cleanupAllChannels();
    }

    public boolean isManagedChannel(String channelId) {
        return managedChannelIds.contains(channelId);
    }

    public void removeTrackedPlayer(String discordUserId) {
        trackedPlayers.remove(discordUserId);
        playerChannelAssignments.remove(discordUserId);
    }

    private void tick() {
        if (!plugin.getDiscordBotManager().isConnected()) return;

        String guildId = plugin.getSettings().getDiscordGuildId();
        String lobbyId = plugin.getSettings().getProximityVoiceChannelId();
        if (guildId == null || guildId.isEmpty() || lobbyId == null || lobbyId.isEmpty()) return;

        JDA jda = plugin.getDiscordBotManager().getJda();
        if (jda == null) return;

        Guild guild = jda.getGuildById(guildId);
        if (guild == null) return;

        VoiceChannel lobby = guild.getVoiceChannelById(lobbyId);
        if (lobby == null) return;

        syncTrackedPlayers(guild, lobby);
        if (trackedPlayers.isEmpty()) return;

        double radius = plugin.getSettings().getProximityRadius();

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            Map<String, double[]> locations = collectLocations();
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () ->
                    computeAndApply(guild, lobby, locations, radius));
        });
    }

    private void syncTrackedPlayers(Guild guild, VoiceChannel lobby) {
        Set<String> activeIds = new HashSet<>();

        for (Member m : lobby.getMembers()) {
            activeIds.add(m.getId());
            if (!trackedPlayers.containsKey(m.getId())) {
                UUID mcUuid = resolveMinecraftUuid(m.getId());
                if (mcUuid != null) {
                    trackedPlayers.put(m.getId(), mcUuid);
                }
            }
        }

        for (String channelId : managedChannelIds) {
            VoiceChannel vc = guild.getVoiceChannelById(channelId);
            if (vc == null) continue;
            for (Member m : vc.getMembers()) {
                activeIds.add(m.getId());
                if (!trackedPlayers.containsKey(m.getId())) {
                    UUID mcUuid = resolveMinecraftUuid(m.getId());
                    if (mcUuid != null) {
                        trackedPlayers.put(m.getId(), mcUuid);
                    }
                }
            }
        }

        trackedPlayers.keySet().removeIf(id -> !activeIds.contains(id));
    }

    private UUID resolveMinecraftUuid(String discordId) {
        for (CharacterData c : plugin.getCharacterManager().getAllCharacters()) {
            if (discordId.equals(c.getDiscordId())) return c.getPlayerUuid();
        }
        return null;
    }

    private Map<String, double[]> collectLocations() {
        Map<String, double[]> locations = new HashMap<>();
        for (Map.Entry<String, UUID> entry : trackedPlayers.entrySet()) {
            if (entry.getValue() == null) continue;
            Player player = plugin.getServer().getPlayer(entry.getValue());
            if (player != null && player.isOnline()) {
                Location loc = player.getLocation();
                locations.put(entry.getKey(), new double[]{
                        loc.getX(), loc.getY(), loc.getZ(),
                        loc.getWorld().getUID().getMostSignificantBits()
                });
            }
        }
        return locations;
    }

    private void computeAndApply(Guild guild, VoiceChannel lobby,
                                  Map<String, double[]> locations, double radius) {
        List<String> ids = new ArrayList<>(locations.keySet());
        if (ids.isEmpty()) return;

        Map<String, String> parent = new HashMap<>();
        for (String id : ids) parent.put(id, id);

        double radiusSq = radius * radius;

        for (int i = 0; i < ids.size(); i++) {
            for (int j = i + 1; j < ids.size(); j++) {
                double[] a = locations.get(ids.get(i));
                double[] b = locations.get(ids.get(j));
                if (a[3] != b[3]) continue;
                double dx = a[0] - b[0], dy = a[1] - b[1], dz = a[2] - b[2];
                if (dx * dx + dy * dy + dz * dz <= radiusSq) {
                    union(parent, ids.get(i), ids.get(j));
                }
            }
        }

        Map<String, Set<String>> groups = new HashMap<>();
        for (String id : ids) {
            groups.computeIfAbsent(find(parent, id), k -> new HashSet<>()).add(id);
        }

        Set<String> needsLobby = new HashSet<>();
        Map<Set<String>, String> groupToChannel = new HashMap<>();

        for (Set<String> group : groups.values()) {
            if (group.size() < 2) {
                needsLobby.addAll(group);
                continue;
            }
            String existing = findBestChannel(group);
            groupToChannel.put(group, existing);
        }

        for (String id : trackedPlayers.keySet()) {
            if (!locations.containsKey(id)) needsLobby.add(id);
        }

        Set<String> channelsInUse = new HashSet<>();

        for (Map.Entry<Set<String>, String> entry : groupToChannel.entrySet()) {
            Set<String> group = entry.getKey();
            String channelId = entry.getValue();

            if (channelId != null) {
                VoiceChannel vc = guild.getVoiceChannelById(channelId);
                if (vc != null) {
                    channelsInUse.add(channelId);
                    moveGroupToChannel(guild, vc, group);
                    continue;
                }
            }
            createAndMoveGroup(guild, lobby, group, channelsInUse);
        }

        for (String discordId : needsLobby) {
            String current = playerChannelAssignments.get(discordId);
            Member member = guild.getMemberById(discordId);
            if (member == null || member.getVoiceState() == null
                    || !member.getVoiceState().inAudioChannel()) continue;

            if (current != null && !current.equals(lobby.getId())) {
                guild.moveVoiceMember(member, lobby).queue(s -> {
                    playerChannelAssignments.put(discordId, lobby.getId());
                    guild.mute(member, true).queue(null, e -> {});
                }, e -> {});
            } else {
                if (!member.getVoiceState().isGuildMuted()) {
                    guild.mute(member, true).queue(null, e -> {});
                }
                playerChannelAssignments.put(discordId, lobby.getId());
            }
        }

        cleanupUnusedChannels(guild, channelsInUse);
    }

    private String findBestChannel(Set<String> group) {
        Map<String, Integer> counts = new HashMap<>();
        for (String id : group) {
            String ch = playerChannelAssignments.get(id);
            if (ch != null && managedChannelIds.contains(ch)) {
                counts.merge(ch, 1, Integer::sum);
            }
        }
        String best = null;
        int bestCount = 0;
        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            if (e.getValue() > bestCount) {
                bestCount = e.getValue();
                best = e.getKey();
            }
        }
        return best;
    }

    private void moveGroupToChannel(Guild guild, VoiceChannel channel, Set<String> group) {
        for (String discordId : group) {
            String current = playerChannelAssignments.get(discordId);
            Member m = guild.getMemberById(discordId);
            if (m == null || m.getVoiceState() == null || !m.getVoiceState().inAudioChannel()) continue;

            if (channel.getId().equals(current)) {
                if (m.getVoiceState().isGuildMuted()) {
                    guild.mute(m, false).queue(null, e -> {});
                }
            } else {
                guild.moveVoiceMember(m, channel).queue(s -> {
                    playerChannelAssignments.put(discordId, channel.getId());
                    guild.mute(m, false).queue(null, e -> {});
                }, e -> {});
            }
        }
    }

    private void createAndMoveGroup(Guild guild, VoiceChannel lobby,
                                     Set<String> group, Set<String> channelsInUse) {
        channelCounter++;
        var action = guild.createVoiceChannel("Proximity #" + channelCounter);

        String catId = plugin.getSettings().getProximitySubChannelCategoryId();
        Category category = null;
        if (catId != null && !catId.isEmpty()) category = guild.getCategoryById(catId);
        if (category == null) category = lobby.getParentCategory();
        if (category != null) action = action.setParent(category);

        action.queue(channel -> {
            managedChannelIds.add(channel.getId());
            channelsInUse.add(channel.getId());

            channel.upsertPermissionOverride(guild.getPublicRole())
                    .deny(Permission.VOICE_CONNECT)
                    .queue(null, e -> {});

            moveGroupToChannel(guild, channel, group);
        }, err -> plugin.getLogger().warning("[Proximity] Failed to create sub-channel: " + err.getMessage()));
    }

    private void cleanupUnusedChannels(Guild guild, Set<String> inUse) {
        Iterator<String> it = managedChannelIds.iterator();
        while (it.hasNext()) {
            String channelId = it.next();
            if (inUse.contains(channelId)) continue;
            VoiceChannel vc = guild.getVoiceChannelById(channelId);
            if (vc == null) {
                it.remove();
                continue;
            }
            if (vc.getMembers().isEmpty()) {
                vc.delete().queue(null, e -> {});
                it.remove();
            }
        }
    }

    private void cleanupAllChannels() {
        if (plugin.getDiscordBotManager() == null || !plugin.getDiscordBotManager().isConnected()) return;
        JDA jda = plugin.getDiscordBotManager().getJda();
        if (jda == null) return;
        String guildId = plugin.getSettings().getDiscordGuildId();
        if (guildId == null || guildId.isEmpty()) return;
        Guild guild = jda.getGuildById(guildId);
        if (guild == null) return;
        String lobbyId = plugin.getSettings().getProximityVoiceChannelId();
        VoiceChannel lobby = (lobbyId != null && !lobbyId.isEmpty()) ? guild.getVoiceChannelById(lobbyId) : null;

        for (String channelId : managedChannelIds) {
            VoiceChannel vc = guild.getVoiceChannelById(channelId);
            if (vc == null) continue;
            if (lobby != null) {
                for (Member m : vc.getMembers()) {
                    guild.moveVoiceMember(m, lobby).queue(null, e -> {});
                    guild.mute(m, false).queue(null, e -> {});
                }
            }
            vc.delete().queue(null, e -> {});
        }

        managedChannelIds.clear();
        playerChannelAssignments.clear();
        trackedPlayers.clear();
    }

    private String find(Map<String, String> parent, String x) {
        while (!parent.get(x).equals(x)) {
            parent.put(x, parent.get(parent.get(x)));
            x = parent.get(x);
        }
        return x;
    }

    private void union(Map<String, String> parent, String a, String b) {
        String ra = find(parent, a), rb = find(parent, b);
        if (!ra.equals(rb)) parent.put(ra, rb);
    }
}
package com.dirt.managers;

import com.dirt.DirtEconomy;
import com.dirt.data.PhoneContact;
import com.dirt.data.PhoneConversation;
import com.dirt.data.PhoneData;
import com.dirt.data.PhoneMessage;
import com.dirt.data.PhoneNotification;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class PhoneManager {
    private final DirtEconomy plugin;
    private final File phoneFolder;
    private final File conversationFolder;
    private final Map<UUID, PhoneData> phoneCache = new HashMap<>();
    private final Map<UUID, PhoneConversation> conversationCache = new HashMap<>();
    private final Map<String, UUID> usernameIndex = new HashMap<>();
    private final Set<UUID> activeInConversation = new HashSet<>();

    public PhoneManager(DirtEconomy plugin) {
        this.plugin = plugin;
        this.phoneFolder = new File(plugin.getDataFolder(), "data/PHONE/phones");
        this.conversationFolder = new File(plugin.getDataFolder(), "data/PHONE/conversations");
        phoneFolder.mkdirs();
        conversationFolder.mkdirs();
        loadAllFromDisk();
    }

    public void reloadCache() {
        phoneCache.clear();
        conversationCache.clear();
        usernameIndex.clear();
        activeInConversation.clear();
        loadAllFromDisk();
    }

    private void loadAllFromDisk() {
        File[] phoneFiles = phoneFolder.listFiles((d, n) -> n.endsWith(".yml"));
        if (phoneFiles != null) {
            for (File f : phoneFiles) {
                try {
                    UUID id = UUID.fromString(f.getName().replace(".yml", ""));
                    PhoneData data = readPhoneFile(id, f);
                    if (data != null) {
                        phoneCache.put(id, data);
                        if (data.getUsername() != null) usernameIndex.put(data.getUsername().toLowerCase(), id);
                    }
                } catch (IllegalArgumentException ignored) {}
            }
        }
        File[] convFiles = conversationFolder.listFiles((d, n) -> n.endsWith(".yml"));
        if (convFiles != null) {
            for (File f : convFiles) {
                try {
                    UUID id = UUID.fromString(f.getName().replace(".yml", ""));
                    PhoneConversation conv = readConversationFile(id, f);
                    if (conv != null) conversationCache.put(id, conv);
                } catch (IllegalArgumentException ignored) {}
            }
        }
        plugin.getLogger().info("[PhoneManager] Cached " + phoneCache.size() + " phones, " + conversationCache.size() + " conversations.");
    }

    public PhoneData getPhone(UUID playerUuid) {
        return phoneCache.get(playerUuid);
    }

    public PhoneData getOrCreatePhone(UUID playerUuid) {
        PhoneData data = phoneCache.get(playerUuid);
        if (data == null) {
            data = new PhoneData();
            data.setOwnerUuid(playerUuid);
            savePhone(data);
        }
        return data;
    }

    public boolean hasPhone(UUID playerUuid) {
        return phoneCache.containsKey(playerUuid);
    }

    public boolean isUsernameTaken(String username) {
        return usernameIndex.containsKey(username.toLowerCase());
    }

    public UUID getUuidByUsername(String username) {
        return usernameIndex.get(username.toLowerCase());
    }

    public PhoneData getPhoneByUsername(String username) {
        UUID uuid = usernameIndex.get(username.toLowerCase());
        return uuid != null ? phoneCache.get(uuid) : null;
    }

    public void setUsername(UUID playerUuid, String username) {
        PhoneData data = getOrCreatePhone(playerUuid);
        if (data.getUsername() != null) usernameIndex.remove(data.getUsername().toLowerCase());
        data.setUsername(username);
        usernameIndex.put(username.toLowerCase(), playerUuid);
        savePhone(data);
    }

    public void savePhone(PhoneData data) {
        phoneCache.put(data.getOwnerUuid(), data);
        File file = new File(phoneFolder, data.getOwnerUuid() + ".yml");
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("username", data.getUsername());
        for (int i = 0; i < data.getContacts().size(); i++) {
            PhoneContact c = data.getContacts().get(i);
            cfg.set("contacts." + i + ".username", c.getUsername());
            cfg.set("contacts." + i + ".displayName", c.getDisplayName());
        }
        List<String> convIds = new ArrayList<>();
        for (UUID id : data.getConversationIds()) convIds.add(id.toString());
        cfg.set("conversationIds", convIds);
        for (int i = 0; i < data.getNotifications().size(); i++) {
            PhoneNotification n = data.getNotifications().get(i);
            cfg.set("notifications." + i + ".type", n.getType());
            cfg.set("notifications." + i + ".title", n.getTitle());
            cfg.set("notifications." + i + ".preview", n.getPreview());
            cfg.set("notifications." + i + ".conversationId", n.getConversationId() != null ? n.getConversationId().toString() : null);
            cfg.set("notifications." + i + ".timestamp", n.getTimestamp());
            cfg.set("notifications." + i + ".read", n.isRead());
        }
        cfg.set("installedApps", new ArrayList<>(data.getInstalledApps()));
        List<String> pinned = new ArrayList<>();
        for (UUID id : data.getPinnedGpsLocations()) pinned.add(id.toString());
        cfg.set("pinnedGpsLocations", pinned);
        try { cfg.save(file); } catch (IOException e) { e.printStackTrace(); }
    }

    public PhoneConversation createConversation(String name, List<UUID> members, boolean groupChat) {
        PhoneConversation conv = new PhoneConversation();
        conv.setConversationId(UUID.randomUUID());
        conv.setName(name);
        conv.setMemberUuids(new ArrayList<>(members));
        conv.setGroupChat(groupChat);
        saveConversation(conv);
        for (UUID memberUuid : members) {
            PhoneData phone = getOrCreatePhone(memberUuid);
            if (!phone.getConversationIds().contains(conv.getConversationId())) {
                phone.getConversationIds().add(conv.getConversationId());
                savePhone(phone);
            }
        }
        return conv;
    }

    public PhoneConversation getConversation(UUID conversationId) {
        return conversationCache.get(conversationId);
    }

    public PhoneConversation findDirectConversation(UUID user1, UUID user2) {
        for (PhoneConversation conv : conversationCache.values()) {
            if (!conv.isGroupChat() && conv.getMemberUuids().size() == 2
                    && conv.getMemberUuids().contains(user1) && conv.getMemberUuids().contains(user2)) {
                return conv;
            }
        }
        return null;
    }

    public void sendMessage(UUID conversationId, UUID senderUuid, String senderUsername, String content) {
        PhoneConversation conv = conversationCache.get(conversationId);
        if (conv == null) return;
        PhoneMessage msg = new PhoneMessage();
        msg.setSenderUuid(senderUuid);
        msg.setSenderUsername(senderUsername);
        msg.setContent(content);
        msg.setTimestamp(System.currentTimeMillis());
        conv.getMessages().add(msg);

        int maxHistory = plugin.getSettings().getPhoneMaxMessageHistory();
        while (conv.getMessages().size() > maxHistory) {
            conv.getMessages().remove(0);
        }
        saveConversation(conv);

        for (UUID memberUuid : conv.getMemberUuids()) {
            if (memberUuid.equals(senderUuid)) continue;
            Player memberPlayer = Bukkit.getPlayer(memberUuid);
            if (memberPlayer != null && memberPlayer.isOnline()) {
                PhoneData phone = getPhone(memberUuid);
                String displaySender = resolveDisplayName(memberUuid, senderUsername);
                if (isInConversation(memberUuid)) {
                    memberPlayer.sendMessage("\u00a73[\u00a7bText\u00a73] \u00a7e" + displaySender + "\u00a77: " + content);
                } else {
                    memberPlayer.sendMessage("\u00a73[\u00a7bNew Message\u00a73] \u00a7e" + displaySender + "\u00a77: " + content);
                    if (phone != null) {
                        PhoneNotification notif = new PhoneNotification();
                        notif.setType("TEXT");
                        notif.setTitle("Message from " + displaySender);
                        notif.setPreview(content.length() > 40 ? content.substring(0, 40) + "..." : content);
                        notif.setConversationId(conversationId);
                        notif.setTimestamp(System.currentTimeMillis());
                        notif.setRead(false);
                        phone.getNotifications().add(notif);
                        savePhone(phone);
                    }
                }
            }
        }
    }

    public String resolveDisplayName(UUID viewerUuid, String username) {
        PhoneData viewer = getPhone(viewerUuid);
        if (viewer != null) {
            for (PhoneContact c : viewer.getContacts()) {
                if (c.getUsername().equalsIgnoreCase(username)) return c.getDisplayName();
            }
        }
        return username;
    }

    public void enterConversation(UUID playerUuid) {
        activeInConversation.add(playerUuid);
    }

    public void leaveConversation(UUID playerUuid) {
        activeInConversation.remove(playerUuid);
    }

    public boolean isInConversation(UUID playerUuid) {
        return activeInConversation.contains(playerUuid);
    }

    public void saveConversation(PhoneConversation conv) {
        conversationCache.put(conv.getConversationId(), conv);
        File file = new File(conversationFolder, conv.getConversationId() + ".yml");
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("name", conv.getName());
        cfg.set("groupChat", conv.isGroupChat());
        List<String> members = new ArrayList<>();
        for (UUID u : conv.getMemberUuids()) members.add(u.toString());
        cfg.set("members", members);
        for (int i = 0; i < conv.getMessages().size(); i++) {
            PhoneMessage m = conv.getMessages().get(i);
            String base = "messages." + i;
            cfg.set(base + ".senderUuid", m.getSenderUuid().toString());
            cfg.set(base + ".senderUsername", m.getSenderUsername());
            cfg.set(base + ".content", m.getContent());
            cfg.set(base + ".timestamp", m.getTimestamp());
        }
        try { cfg.save(file); } catch (IOException e) { e.printStackTrace(); }
    }

    public void deleteConversation(UUID conversationId) {
        PhoneConversation conv = conversationCache.remove(conversationId);
        if (conv != null) {
            for (UUID memberUuid : conv.getMemberUuids()) {
                PhoneData phone = getPhone(memberUuid);
                if (phone != null) {
                    phone.getConversationIds().remove(conversationId);
                    savePhone(phone);
                }
            }
        }
        new File(conversationFolder, conversationId + ".yml").delete();
    }

    private PhoneData readPhoneFile(UUID uuid, File file) {
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        PhoneData data = new PhoneData();
        data.setOwnerUuid(uuid);
        data.setUsername(cfg.getString("username"));
        List<PhoneContact> contacts = new ArrayList<>();
        if (cfg.isConfigurationSection("contacts")) {
            for (String key : cfg.getConfigurationSection("contacts").getKeys(false)) {
                PhoneContact c = new PhoneContact();
                c.setUsername(cfg.getString("contacts." + key + ".username", ""));
                c.setDisplayName(cfg.getString("contacts." + key + ".displayName", ""));
                contacts.add(c);
            }
        }
        data.setContacts(contacts);
        List<UUID> convIds = new ArrayList<>();
        for (String s : cfg.getStringList("conversationIds")) {
            try { convIds.add(UUID.fromString(s)); } catch (IllegalArgumentException ignored) {}
        }
        data.setConversationIds(convIds);
        List<PhoneNotification> notifs = new ArrayList<>();
        if (cfg.isConfigurationSection("notifications")) {
            for (String key : cfg.getConfigurationSection("notifications").getKeys(false)) {
                PhoneNotification n = new PhoneNotification();
                n.setType(cfg.getString("notifications." + key + ".type", "TEXT"));
                n.setTitle(cfg.getString("notifications." + key + ".title", ""));
                n.setPreview(cfg.getString("notifications." + key + ".preview", ""));
                String cid = cfg.getString("notifications." + key + ".conversationId");
                if (cid != null) { try { n.setConversationId(UUID.fromString(cid)); } catch (IllegalArgumentException ignored) {} }
                n.setTimestamp(cfg.getLong("notifications." + key + ".timestamp", 0));
                n.setRead(cfg.getBoolean("notifications." + key + ".read", false));
                notifs.add(n);
            }
        }
        data.setNotifications(notifs);
        data.setInstalledApps(new HashSet<>(cfg.getStringList("installedApps")));
        List<UUID> pinned = new ArrayList<>();
        for (String s : cfg.getStringList("pinnedGpsLocations")) {
            try { pinned.add(UUID.fromString(s)); } catch (IllegalArgumentException ignored) {}
        }
        data.setPinnedGpsLocations(pinned);
        return data;
    }

    private PhoneConversation readConversationFile(UUID id, File file) {
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        PhoneConversation conv = new PhoneConversation();
        conv.setConversationId(id);
        conv.setName(cfg.getString("name", ""));
        conv.setGroupChat(cfg.getBoolean("groupChat", false));
        List<UUID> members = new ArrayList<>();
        for (String s : cfg.getStringList("members")) {
            try { members.add(UUID.fromString(s)); } catch (IllegalArgumentException ignored) {}
        }
        conv.setMemberUuids(members);
        List<PhoneMessage> messages = new ArrayList<>();
        if (cfg.isConfigurationSection("messages")) {
            for (String key : cfg.getConfigurationSection("messages").getKeys(false)) {
                PhoneMessage m = new PhoneMessage();
                String su = cfg.getString("messages." + key + ".senderUuid");
                if (su != null) { try { m.setSenderUuid(UUID.fromString(su)); } catch (IllegalArgumentException ignored) {} }
                m.setSenderUsername(cfg.getString("messages." + key + ".senderUsername", "?"));
                m.setContent(cfg.getString("messages." + key + ".content", ""));
                m.setTimestamp(cfg.getLong("messages." + key + ".timestamp", 0));
                messages.add(m);
            }
        }
        conv.setMessages(messages);
        return conv;
    }
}
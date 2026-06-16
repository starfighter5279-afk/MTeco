package com.mteco.managers;

import com.mteco.MTeco;
import com.mteco.data.CharacterData;
import com.mteco.data.MailItem;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class CharacterManager {
    private final MTeco plugin;
    private final File dataFolder;
    private final IDManager idManager;
    private final Map<UUID, String[]> pendingCreations = new HashMap<>();
    private final Map<UUID, String[]> pendingNameChanges = new HashMap<>();
    private final Set<UUID> forcedCreation = new HashSet<>();
    private final Map<UUID, CharacterData> characterCache = new HashMap<>();

    public CharacterManager(MTeco plugin, IDManager idManager) {
        this.plugin = plugin;
        this.idManager = idManager;
        this.dataFolder = new File(plugin.getDataFolder(), "data/MTC/characters");
        this.dataFolder.mkdirs();
        loadAllFromDisk();
    }

    public void reloadCache() {
        characterCache.clear();
        loadAllFromDisk();
    }

    private void loadAllFromDisk() {
        File[] files = dataFolder.listFiles((d, name) -> name.endsWith(".yml"));
        if (files == null) return;
        for (File file : files) {
            try {
                UUID uuid = UUID.fromString(file.getName().replace(".yml", ""));
                CharacterData data = readCharacterFile(uuid, file);
                if (data != null) characterCache.put(uuid, data);
            } catch (IllegalArgumentException ignored) {}
        }
        plugin.getLogger().info("[CharacterManager] Cached " + characterCache.size() + " characters.");
    }

    public boolean hasCharacter(UUID uuid) {
        return characterCache.containsKey(uuid);
    }

    public CharacterData getCharacter(UUID uuid) {
        return characterCache.get(uuid);
    }

    public void saveCharacter(CharacterData data) {
        characterCache.put(data.getPlayerUuid(), data);

        File file = new File(dataFolder, data.getPlayerUuid() + ".yml");
        YamlConfiguration config = new YamlConfiguration();
        config.set("firstName", data.getFirstName());
        config.set("middleName", data.getMiddleName());
        config.set("lastName", data.getLastName());
        config.set("gender", data.getGender());
        config.set("birthDate", data.getBirthDate());
        config.set("alive", data.isAlive());
        config.set("firstJoinDate", data.getFirstJoinDate());
        config.set("familyId", data.getFamilyId() != null ? data.getFamilyId().toString() : null);
        config.set("birthFamilyId", data.getBirthFamilyId() != null ? data.getBirthFamilyId().toString() : null);
        config.set("familyRole", data.getFamilyRole());
        config.set("inheritorUuid", data.getInheritorUuid() != null ? data.getInheritorUuid().toString() : null);
        config.set("pendingChildSelection", data.isPendingChildSelection());
        config.set("lastDailyReward", data.getLastDailyReward());
        config.set("discordId", data.getDiscordId());

        List<String> prevFamilyStrings = new ArrayList<>();
        for (UUID pfid : data.getPreviousFamilyIds()) {
            prevFamilyStrings.add(pfid.toString());
        }
        config.set("previousFamilyIds", prevFamilyStrings);

        for (MailItem item : data.getMailbox()) {
            String base = "mailbox." + item.getId();
            config.set(base + ".type", item.getType());
            config.set(base + ".fromPlayer", item.getFromPlayerUuid() != null ? item.getFromPlayerUuid().toString() : null);
            config.set(base + ".timestamp", item.getTimestamp());
            if (item.getData() != null) {
                for (Map.Entry<String, String> entry : item.getData().entrySet()) {
                    config.set(base + ".data." + entry.getKey(), entry.getValue());
                }
            }
        }

        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
        idManager.archiveID(data);
    }

    public void deleteCharacter(UUID uuid) {
        characterCache.remove(uuid);
        new File(dataFolder, uuid + ".yml").delete();
    }

    public List<CharacterData> getAllCharacters() {
        return new ArrayList<>(characterCache.values());
    }

    public CharacterData getArchivedCharacter(UUID uuid) {
        List<CharacterData> archived = idManager.getAllArchivedCharacters();
        CharacterData best = null;
        for (CharacterData c : archived) {
            if (c.getPlayerUuid().equals(uuid)) {
                if (best == null || c.getBirthDate() > best.getBirthDate()) {
                    best = c;
                }
            }
        }
        return best;
    }

    public List<CharacterData> getAllCharactersEver() {
        List<CharacterData> archived = idManager.getAllArchivedCharacters();
        for (CharacterData c : archived) {
            CharacterData live = characterCache.get(c.getPlayerUuid());
            if (live != null && live.getBirthDate() == c.getBirthDate()) {
                c.setAlive(true);
            }
        }
        return archived;
    }

    public void addMailItem(UUID playerUuid, MailItem item) {
        CharacterData data = characterCache.get(playerUuid);
        if (data == null) return;
        data.getMailbox().add(item);
        saveCharacter(data);
        if (plugin.getDiscordBotManager() != null) {
            plugin.getDiscordBotManager().sendMailNotification(playerUuid, item);
        }
    }

    public void removeMailItem(UUID playerUuid, String mailId) {
        CharacterData data = characterCache.get(playerUuid);
        if (data == null) return;
        data.getMailbox().removeIf(m -> m.getId().equals(mailId));
        saveCharacter(data);
    }

    public void initPendingCreation(UUID uuid) {
        pendingCreations.put(uuid, new String[]{null, null, null, "MALE"});
    }

    public void setPendingCreation(UUID uuid, String[] state) {
        pendingCreations.put(uuid, state);
    }

    public String[] getPendingCreation(UUID uuid) {
        return pendingCreations.computeIfAbsent(uuid, k -> new String[]{null, null, null, "MALE"});
    }

    public void clearPendingCreation(UUID uuid) {
        pendingCreations.remove(uuid);
    }

    public void initPendingNameChange(UUID uuid) {
        CharacterData data = characterCache.get(uuid);
        if (data != null) {
            pendingNameChanges.put(uuid, new String[]{data.getFirstName(), data.getMiddleName(), data.getLastName()});
        }
    }

    public void setPendingNameChange(UUID uuid, String[] state) {
        pendingNameChanges.put(uuid, state);
    }

    public String[] getPendingNameChange(UUID uuid) {
        return pendingNameChanges.get(uuid);
    }

    public void clearPendingNameChange(UUID uuid) {
        pendingNameChanges.remove(uuid);
    }

    public void addForcedCreation(UUID uuid) {
        forcedCreation.add(uuid);
    }

    public void removeForcedCreation(UUID uuid) {
        forcedCreation.remove(uuid);
    }

    public boolean isForcedCreation(UUID uuid) {
        return forcedCreation.contains(uuid);
    }

    private CharacterData readCharacterFile(UUID uuid, File file) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        CharacterData data = new CharacterData();
        data.setPlayerUuid(uuid);
        data.setFirstName(config.getString("firstName"));
        data.setMiddleName(config.getString("middleName"));
        data.setLastName(config.getString("lastName"));
        data.setGender(config.getString("gender", "MALE"));
        data.setBirthDate(config.getLong("birthDate"));
        data.setAlive(config.getBoolean("alive", true));
        data.setFirstJoinDate(config.getLong("firstJoinDate", 0));
        String fid = config.getString("familyId");
        if (fid != null) data.setFamilyId(UUID.fromString(fid));
        String bfid = config.getString("birthFamilyId");
        if (bfid != null) data.setBirthFamilyId(UUID.fromString(bfid));
        data.setFamilyRole(config.getString("familyRole"));
        String iid = config.getString("inheritorUuid");
        if (iid != null) data.setInheritorUuid(UUID.fromString(iid));
        data.setPendingChildSelection(config.getBoolean("pendingChildSelection", false));
        data.setLastDailyReward(config.getLong("lastDailyReward", 0));
        data.setDiscordId(config.getString("discordId"));

        List<UUID> prevFamilies = new ArrayList<>();
        for (String pfid : config.getStringList("previousFamilyIds")) {
            try { prevFamilies.add(UUID.fromString(pfid)); } catch (IllegalArgumentException ignored) {}
        }
        data.setPreviousFamilyIds(prevFamilies);

        List<MailItem> mailbox = new ArrayList<>();
        if (config.isConfigurationSection("mailbox")) {
            for (String key : config.getConfigurationSection("mailbox").getKeys(false)) {
                MailItem item = new MailItem();
                item.setId(key);
                item.setType(config.getString("mailbox." + key + ".type"));
                String fp = config.getString("mailbox." + key + ".fromPlayer");
                if (fp != null) item.setFromPlayerUuid(UUID.fromString(fp));
                item.setTimestamp(config.getLong("mailbox." + key + ".timestamp"));
                Map<String, String> extraData = new HashMap<>();
                if (config.isConfigurationSection("mailbox." + key + ".data")) {
                    for (String dk : config.getConfigurationSection("mailbox." + key + ".data").getKeys(false)) {
                        extraData.put(dk, config.getString("mailbox." + key + ".data." + dk));
                    }
                }
                item.setData(extraData);
                mailbox.add(item);
            }
        }
        data.setMailbox(mailbox);
        return data;
    }
}
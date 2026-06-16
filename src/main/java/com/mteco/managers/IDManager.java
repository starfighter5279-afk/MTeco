package com.mteco.managers;

import com.mteco.MTeco;
import com.mteco.data.CharacterData;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class IDManager {
    private final File idsFolder;

    public IDManager(MTeco plugin) {
        this.idsFolder = new File(plugin.getDataFolder(), "data/MTC/ids");
        this.idsFolder.mkdirs();
    }

    /**
     * Archives the character's ID snapshot. Uses playerUUID + birthDate as a unique
     * filename so each distinct character incarnation is preserved permanently.
     * Safe to call on every save — skips if the archive file already exists.
     */
    public void archiveID(CharacterData data) {
        if (data.getPlayerUuid() == null || data.getBirthDate() == 0) return;

        String filename = data.getPlayerUuid() + "_" + data.getBirthDate() + ".yml";
        File file = new File(idsFolder, filename);
        if (file.exists()) return;

        YamlConfiguration config = new YamlConfiguration();
        config.set("playerUuid", data.getPlayerUuid().toString());
        config.set("firstName", data.getFirstName());
        config.set("middleName", data.getMiddleName());
        config.set("lastName", data.getLastName());
        config.set("gender", data.getGender());
        config.set("birthDate", data.getBirthDate());
        config.set("firstJoinDate", data.getFirstJoinDate());
        config.set("archivedAt", System.currentTimeMillis());
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<CharacterData> getAllArchivedCharacters() {
        List<CharacterData> list = new ArrayList<>();
        File[] files = idsFolder.listFiles((d, name) -> name.endsWith(".yml"));
        if (files == null) return list;
        for (File file : files) {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            String uuidStr = config.getString("playerUuid");
            if (uuidStr == null) continue;
            try {
                CharacterData data = new CharacterData();
                data.setPlayerUuid(UUID.fromString(uuidStr));
                data.setFirstName(config.getString("firstName"));
                data.setMiddleName(config.getString("middleName"));
                data.setLastName(config.getString("lastName"));
                data.setGender(config.getString("gender", "MALE"));
                data.setBirthDate(config.getLong("birthDate"));
                data.setFirstJoinDate(config.getLong("firstJoinDate", 0));
                data.setAlive(false);
                list.add(data);
            } catch (IllegalArgumentException ignored) {}
        }
        return list;
    }

    public void deleteArchive(UUID playerUuid) {
        File[] files = idsFolder.listFiles((d, name) -> name.startsWith(playerUuid.toString()) && name.endsWith(".yml"));
        if (files == null) return;
        for (File file : files) {
            file.delete();
        }
    }
}
package com.dirt.util;

import com.dirt.DirtEconomy;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class ConfigValidator {

    private static final String[] CONFIG_FILES = {
            "config.yml", "nations.yml", "business.yml", "discord.yml", "shops.yml", "roles.yml"
    };

    public static void validateAll(DirtEconomy plugin) {
        for (String filename : CONFIG_FILES) {
            validate(plugin, filename);
        }
    }

    private static void validate(DirtEconomy plugin, String filename) {
        File file = new File(plugin.getDataFolder(), filename);
        if (!file.exists()) {
            plugin.saveResource(filename, false);
            return;
        }

        InputStream defaultStream = plugin.getResource(filename);
        if (defaultStream == null) return;

        YamlConfiguration defaults;
        try (InputStreamReader reader = new InputStreamReader(defaultStream, StandardCharsets.UTF_8)) {
            defaults = YamlConfiguration.loadConfiguration(reader);
        } catch (IOException e) {
            plugin.getLogger().warning("[ConfigValidator] Failed to read default " + filename + ": " + e.getMessage());
            return;
        }

        YamlConfiguration current = YamlConfiguration.loadConfiguration(file);

        int added = 0;
        for (String key : defaults.getKeys(true)) {
            if (defaults.isConfigurationSection(key)) continue;
            if (!current.contains(key)) {
                current.set(key, defaults.get(key));
                added++;
            }
        }

        if (added > 0) {
            try {
                current.save(file);
                plugin.getLogger().info("[ConfigValidator] Added " + added + " missing option(s) to " + filename);
            } catch (IOException e) {
                plugin.getLogger().warning("[ConfigValidator] Failed to save " + filename + ": " + e.getMessage());
            }
        }
    }
}
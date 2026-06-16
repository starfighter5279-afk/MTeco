package com.mteco.util;

import com.mteco.MTeco;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class DataMigrator {

    private static final String[][] FOLDER_MIGRATIONS = {
            // MTC (Character-related)
            {"characters", "data/MTC/characters"},
            {"families", "data/MTC/families"},
            {"ids", "data/MTC/ids"},
            // MTN (Nation-related)
            {"nations", "data/MTN/nations"},
            {"regions", "data/MTN/regions"},
            {"properties", "data/MTN/properties"},
            {"purchaserequests", "data/MTN/purchaserequests"},
            {"nationtransactions", "data/MTN/nationtransactions"},
            {"conservationareas", "data/MTN/conservationareas"},
            {"wars", "data/MTN/wars"},
            {"contracts", "data/MTN/contracts"},
            {"lawbooks", "data/MTN/lawbooks"},
            {"legcrimes", "data/MTN/legcrimes"},
            {"criminals", "data/MTN/criminals"},
            {"jails", "data/MTN/jails"},
            {"cells", "data/MTN/cells"},
            {"minters", "data/MTN/minters"},
            // MTB (Business-related)
            {"businesses", "data/MTB/businesses"},
            {"businessproperties", "data/MTB/businessproperties"},
            {"shops", "data/MTB/shops"},
            {"stockrooms", "data/MTB/stockrooms"},
    };

    private static final String[][] FILE_MIGRATIONS = {
            {"online_shops.yml", "data/MTB/online_shops.yml"},
            {"protected_chests.yml", "data/protected_chests.yml"},
    };

    public static void migrate(MTeco plugin) {
        File base = plugin.getDataFolder();

        boolean anyMigrated = false;

        for (String[] entry : FOLDER_MIGRATIONS) {
            File oldDir = new File(base, entry[0]);
            if (!oldDir.exists() || !oldDir.isDirectory()) continue;
            File newDir = new File(base, entry[1]);
            if (migrateFolder(plugin, oldDir, newDir)) anyMigrated = true;
        }

        for (String[] entry : FILE_MIGRATIONS) {
            File oldFile = new File(base, entry[0]);
            if (!oldFile.exists() || oldFile.isDirectory()) continue;
            File newFile = new File(base, entry[1]);
            if (migrateSingleFile(plugin, oldFile, newFile)) anyMigrated = true;
        }

        if (anyMigrated) {
            plugin.getLogger().info("[DataMigrator] Data migration complete. Old folders cleaned up.");
        }
    }

    private static boolean migrateFolder(MTeco plugin, File oldDir, File newDir) {
        newDir.mkdirs();
        File[] files = oldDir.listFiles();
        if (files == null) return false;

        int moved = 0;
        for (File file : files) {
            File dest = new File(newDir, file.getName());
            if (dest.exists()) continue;
            try {
                Files.move(file.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                moved++;
            } catch (IOException e) {
                plugin.getLogger().warning("[DataMigrator] Failed to move " + file.getName() + ": " + e.getMessage());
            }
        }

        String[] remaining = oldDir.list();
        if (remaining == null || remaining.length == 0) {
            oldDir.delete();
        }

        if (moved > 0) {
            plugin.getLogger().info("[DataMigrator] Migrated " + moved + " file(s) from " + oldDir.getName() + "/ → " + newDir.getPath().substring(plugin.getDataFolder().getPath().length() + 1) + "/");
            return true;
        }
        return false;
    }

    private static boolean migrateSingleFile(MTeco plugin, File oldFile, File newFile) {
        if (newFile.exists()) return false;
        newFile.getParentFile().mkdirs();
        try {
            Files.move(oldFile.toPath(), newFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            plugin.getLogger().info("[DataMigrator] Migrated " + oldFile.getName() + " → " + newFile.getPath().substring(plugin.getDataFolder().getPath().length() + 1));
            return true;
        } catch (IOException e) {
            plugin.getLogger().warning("[DataMigrator] Failed to move " + oldFile.getName() + ": " + e.getMessage());
            return false;
        }
    }
}
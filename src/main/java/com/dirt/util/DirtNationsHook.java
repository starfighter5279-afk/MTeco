package com.dirt.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class DirtNationsHook {
    private final boolean available;

    public DirtNationsHook() {
        this.available = Bukkit.getPluginManager().getPlugin("DirtNations") != null;
    }

    public boolean isAvailable() {
        return available;
    }

    /**
     * Returns the player's current nation name, or null if unavailable.
     * TODO: Replace the body of this method with the actual DirtNations API call once known.
     * Example: return DirtNations.getInstance().getNationManager().getNation(player.getUniqueId()).getName();
     */
    public String getNation(Player player) {
        if (!available) return null;
        try {
            // Replace with the real DirtNations API call
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}
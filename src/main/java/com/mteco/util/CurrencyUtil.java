package com.mteco.util;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public class CurrencyUtil {
    private static String symbol = "$";

    public static void init() {
        Plugin essentials = Bukkit.getPluginManager().getPlugin("Essentials");
        if (essentials != null) {
            String s = essentials.getConfig().getString("currency-symbol", "$");
            if (s != null && !s.isEmpty()) {
                symbol = s;
            }
            Bukkit.getLogger().info("[MTeco] Detected EssentialsX currency symbol: " + symbol);
        }
    }

    public static String symbol() {
        return symbol;
    }
}
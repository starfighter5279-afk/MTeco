package com.mteco.util;

import com.cryptomorin.xseries.XMaterial;
import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ItemUtil {
    public static ItemStack buildItem(XMaterial material, String name, String... lore) {
        ItemStack item = material.parseItem();
        if (item == null) item = new ItemStack(org.bukkit.Material.STONE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            if (lore.length > 0) {
                List<String> loreList = Arrays.stream(lore)
                        .map(l -> ChatColor.translateAlternateColorCodes('&', l))
                        .collect(Collectors.toList());
                meta.setLore(loreList);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack buildGlassPane() {
        return buildItem(XMaterial.BLACK_STAINED_GLASS_PANE, " ");
    }

    public static ItemStack buildGlassPane(XMaterial material) {
        return buildItem(material, " ");
    }
}
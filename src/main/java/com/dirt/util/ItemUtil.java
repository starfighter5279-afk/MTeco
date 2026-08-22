package com.dirt.util;

import com.cryptomorin.xseries.XMaterial;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ItemUtil {
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacySection();

    public static Component legacyComponent(String text) {
        return LEGACY_SERIALIZER.deserialize(text == null ? "" : text);
    }

    public static String legacyString(Component component) {
        return component == null ? null : LEGACY_SERIALIZER.serialize(component);
    }

    public static void setDisplayName(ItemMeta meta, String name) {
        meta.displayName(legacyComponent(name));
    }

    public static void setLore(ItemMeta meta, List<String> lore) {
        meta.lore(lore.stream().map(ItemUtil::legacyComponent).toList());
    }

    public static boolean hasDisplayName(ItemMeta meta, String name) {
        return meta != null && name.equals(legacyString(meta.displayName()));
    }

    public static ItemStack buildItem(XMaterial material, String name, String... lore) {
        ItemStack item = material.parseItem();
        if (item == null) item = new ItemStack(org.bukkit.Material.STONE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            setDisplayName(meta, ChatColor.translateAlternateColorCodes('&', name));
            if (lore.length > 0) {
                List<String> loreList = Arrays.stream(lore)
                        .map(l -> ChatColor.translateAlternateColorCodes('&', l))
                        .collect(Collectors.toList());
                setLore(meta, loreList);
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
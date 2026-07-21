package com.dirt.inventory.impl.nation;

import com.cryptomorin.xseries.XMaterial;
import com.dirt.DirtEconomy;
import com.dirt.inventory.InventoryButton;
import com.dirt.inventory.InventoryGUI;
import com.dirt.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class MobPermissionsGUI extends InventoryGUI {
    private final DirtEconomy plugin;
    private final Set<String> deniedMobs;
    private final Runnable saveCallback;
    private final Consumer<Player> backCallback;
    private final int page;

    private static final List<MobEntry> HOSTILE_MOBS = new ArrayList<>();

    static {
        HOSTILE_MOBS.add(new MobEntry("ZOMBIE", XMaterial.ZOMBIE_HEAD, "Zombie"));
        HOSTILE_MOBS.add(new MobEntry("SKELETON", XMaterial.SKELETON_SKULL, "Skeleton"));
        HOSTILE_MOBS.add(new MobEntry("CREEPER", XMaterial.CREEPER_HEAD, "Creeper"));
        HOSTILE_MOBS.add(new MobEntry("SPIDER", XMaterial.SPIDER_EYE, "Spider"));
        HOSTILE_MOBS.add(new MobEntry("CAVE_SPIDER", XMaterial.FERMENTED_SPIDER_EYE, "Cave Spider"));
    https://cdn.discordapp.com/avatars/1495888771529441432/18e9e8addff111430992603ded930911.jpg$0    HOSTILE_MOBS.add(new MobEntry("ENDERMAN", XMaterial.ENDER_PEARL, "Enderman"));
        HOSTILE_MOBS.add(new MobEntry("WITCH", XMaterial.GLASS_BOTTLE, "Witch"));
        HOSTILE_MOBS.add(new MobEntry("BLAZE", XMaterial.BLAZE_ROD, "Blaze"));
        HOSTILE_MOBS.add(new MobEntry("GHAST", XMaterial.GHAST_TEAR, "Ghast"));
        HOSTILE_MOBS.add(new MobEntry("MAGMA_CUBE", XMaterial.MAGMA_CREAM, "Magma Cube"));
        HOSTILE_MOBS.add(new MobEntry("SLIME", XMaterial.SLIME_BALL, "Slime"));
        HOSTILE_MOBS.add(new MobEntry("PHANTOM", XMaterial.PHANTOM_MEMBRANE, "Phantom"));
        HOSTILE_MOBS.add(new MobEntry("DROWNED", XMaterial.TRIDENT, "Drowned"));
        HOSTILE_MOBS.add(new MobEntry("HUSK", XMaterial.SAND, "Husk"));
        HOSTILE_MOBS.add(new MobEntry("STRAY", XMaterial.ARROW, "Stray"));
        HOSTILE_MOBS.add(new MobEntry("ZOMBIE_VILLAGER", XMaterial.GOLDEN_APPLE, "Zombie Villager"));
        HOSTILE_MOBS.add(new MobEntry("ENDERMITE", XMaterial.ENDER_EYE, "Endermite"));
        HOSTILE_MOBS.add(new MobEntry("SILVERFISH", XMaterial.STONE, "Silverfish"));
        HOSTILE_MOBS.add(new MobEntry("VEX", XMaterial.IRON_SWORD, "Vex"));
        HOSTILE_MOBS.add(new MobEntry("VINDICATOR", XMaterial.IRON_AXE, "Vindicator"));
        HOSTILE_MOBS.add(new MobEntry("EVOKER", XMaterial.TOTEM_OF_UNDYING, "Evoker"));
        HOSTILE_MOBS.add(new MobEntry("PILLAGER", XMaterial.CROSSBOW, "Pillager"));
        HOSTILE_MOBS.add(new MobEntry("RAVAGER", XMaterial.SADDLE, "Ravager"));
        HOSTILE_MOBS.add(new MobEntry("GUARDIAN", XMaterial.PRISMARINE_SHARD, "Guardian"));
        HOSTILE_MOBS.add(new MobEntry("ELDER_GUARDIAN", XMaterial.PRISMARINE_CRYSTALS, "Elder Guardian"));
        HOSTILE_MOBS.add(new MobEntry("SHULKER", XMaterial.SHULKER_SHELL, "Shulker"));
        HOSTILE_MOBS.add(new MobEntry("WITHER_SKELETON", XMaterial.WITHER_SKELETON_SKULL, "Wither Skeleton"));
        HOSTILE_MOBS.add(new MobEntry("PIGLIN", XMaterial.GOLD_INGOT, "Piglin"));
        HOSTILE_MOBS.add(new MobEntry("PIGLIN_BRUTE", XMaterial.GOLDEN_AXE, "Piglin Brute"));
        HOSTILE_MOBS.add(new MobEntry("HOGLIN", XMaterial.PORKCHOP, "Hoglin"));
        HOSTILE_MOBS.add(new MobEntry("ZOGLIN", XMaterial.ROTTEN_FLESH, "Zoglin"));
        HOSTILE_MOBS.add(new MobEntry("ZOMBIFIED_PIGLIN", XMaterial.GOLDEN_SWORD, "Zombified Piglin"));
    }

    public MobPermissionsGUI(DirtEconomy plugin, Set<String> deniedMobs, Runnable saveCallback, Consumer<Player> backCallback, int page) {
        this.plugin = plugin;
        this.deniedMobs = deniedMobs;
        this.saveCallback = saveCallback;
        this.backCallback = backCallback;
        this.page = page;
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 54, "\u00a76Mob Management");
    }

    @Override
    public void decorate(Player player) {
        fillPagedGui(54, XMaterial.RED_STAINED_GLASS_PANE);

        addButton(4, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.SPAWNER, "\u00a76Mob Management",
                        "\u00a77Toggle which hostile mobs",
                        "\u00a77can spawn in this area.",
                        "\u00a7aGreen \u00a77= Allowed",
                        "\u00a7cRed \u00a77= Denied"))
                .consumer(e -> {})
        );

        int itemsPerPage = 28;
        int startIndex = page * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, HOSTILE_MOBS.size());

        int[] slots = {
                10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34,
                37, 38, 39, 40, 41, 42, 43
        };

        int slotIndex = 0;
        for (int i = startIndex; i < endIndex; i++) {
            if (slotIndex >= slots.length) break;
            MobEntry mob = HOSTILE_MOBS.get(i);
            int slot = slots[slotIndex++];

            addButton(slot, new InventoryButton()
                    .creator(p -> {
                        boolean denied = deniedMobs.contains(mob.type);
                        XMaterial icon = denied ? XMaterial.RED_CONCRETE : XMaterial.LIME_CONCRETE;
                        String status = denied ? "\u00a7c\u2718 Denied" : "\u00a7a\u2714 Allowed";
                        return ItemUtil.buildItem(icon, (denied ? "\u00a7c" : "\u00a7a") + mob.displayName,
                                status,
                                "\u00a77Click to " + (denied ? "allow" : "deny") + ".");
                    })
                    .consumer(e -> {
                        if (deniedMobs.contains(mob.type)) {
                            deniedMobs.remove(mob.type);
                        } else {
                            deniedMobs.add(mob.type);
                        }
                        saveCallback.run();
                        plugin.getGUIManager().openGUI(new MobPermissionsGUI(plugin, deniedMobs, saveCallback, backCallback, page), (Player) e.getWhoClicked());
                    })
            );
        }

        if (page > 0) {
            addButton(48, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7ePrevious Page"))
                    .consumer(e -> plugin.getGUIManager().openGUI(new MobPermissionsGUI(plugin, deniedMobs, saveCallback, backCallback, page - 1), (Player) e.getWhoClicked()))
            );
        }

        if (endIndex < HOSTILE_MOBS.size()) {
            addButton(50, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7eNext Page"))
                    .consumer(e -> plugin.getGUIManager().openGUI(new MobPermissionsGUI(plugin, deniedMobs, saveCallback, backCallback, page + 1), (Player) e.getWhoClicked()))
            );
        }

        addButton(53, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.BARRIER, "\u00a7cBack"))
                .consumer(e -> backCallback.accept((Player) e.getWhoClicked()))
        );

        addButton(45, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.REDSTONE_BLOCK, "\u00a7c\u00a7lAll Off",
                        "\u00a77Deny all hostile mob spawns."))
                .consumer(e -> {
                    for (MobEntry mob : HOSTILE_MOBS) {
                        deniedMobs.add(mob.type);
                    }
                    saveCallback.run();
                    plugin.getGUIManager().openGUI(new MobPermissionsGUI(plugin, deniedMobs, saveCallback, backCallback, page), (Player) e.getWhoClicked());
                })
        );

        super.decorate(player);
    }

    private static class MobEntry {
        final String type;
        final XMaterial icon;
        final String displayName;

        MobEntry(String type, XMaterial icon, String displayName) {
            this.type = type;
            this.icon = icon;
            this.displayName = displayName;
        }
    }
}
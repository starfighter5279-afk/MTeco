package com.dirt.inventory.impl.nation;

import com.cryptomorin.xseries.XMaterial;
import com.dirt.DirtEconomy;
import com.dirt.inventory.InventoryButton;
import com.dirt.inventory.InventoryGUI;
import com.dirt.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class PropertyPermissionsGUI extends InventoryGUI {
    private final DirtEconomy plugin;
    private final Map<String, Boolean> permsSameRegion;
    private final Map<String, Boolean> permsSameNation;
    private final Map<String, Boolean> permsForeign;
    private final Runnable saveCallback;
    private final Consumer<Player> backCallback;
    private final Set<String> deniedMobs;
    private final Runnable mobSaveCallback;
    private final Consumer<Player> mobBackCallback;
    private Consumer<Player> rolePermsOpener;

    private static final String[] PERM_KEYS = {"enter", "interact", "build", "containers"};
    private static final String[] PERM_LABELS = {"Enter", "Interact", "Build", "Open Containers"};

    public PropertyPermissionsGUI(DirtEconomy plugin,
                                  Map<String, Boolean> permsSameRegion,
                                  Map<String, Boolean> permsSameNation,
                                  Map<String, Boolean> permsForeign,
                                  Runnable saveCallback,
                                  Consumer<Player> backCallback) {
        this(plugin, permsSameRegion, permsSameNation, permsForeign, saveCallback, backCallback, null, null, null);
    }

    public PropertyPermissionsGUI(DirtEconomy plugin,
                                  Map<String, Boolean> permsSameRegion,
                                  Map<String, Boolean> permsSameNation,
                                  Map<String, Boolean> permsForeign,
                                  Runnable saveCallback,
                                  Consumer<Player> backCallback,
                                  Set<String> deniedMobs,
                                  Runnable mobSaveCallback,
                                  Consumer<Player> mobBackCallback) {
        this.plugin = plugin;
        this.permsSameRegion = permsSameRegion;
        this.permsSameNation = permsSameNation;
        this.permsForeign = permsForeign;
        this.saveCallback = saveCallback;
        this.backCallback = backCallback;
        this.deniedMobs = deniedMobs;
        this.mobSaveCallback = mobSaveCallback;
        this.mobBackCallback = mobBackCallback;
    }

    public void setRolePermsOpener(Consumer<Player> rolePermsOpener) {
        this.rolePermsOpener = rolePermsOpener;
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 54, "\u00a7eProperty Permissions");
    }

    @Override
    public void decorate(Player player) {
        fillPagedGui(54, XMaterial.BLUE_STAINED_GLASS_PANE);

        addButton(4, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.COMPARATOR, "\u00a7eProperty Permissions",
                        "\u00a77Configure access for each group.",
                        "\u00a77Click toggles to allow or deny."))
                .consumer(e -> {})
        );

        addButton(11, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.LIME_BANNER, "\u00a7aSame Region",
                        "\u00a77Players from the same region."))
                .consumer(e -> {})
        );
        addButton(13, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.YELLOW_BANNER, "\u00a7eSame Nation",
                        "\u00a77Players from the same nation",
                        "\u00a77but a different region."))
                .consumer(e -> {})
        );
        addButton(15, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.RED_BANNER, "\u00a7cForeign",
                        "\u00a77Players not from this nation."))
                .consumer(e -> {})
        );

        int[] rows = {20, 29, 38, 47};
        for (int i = 0; i < PERM_KEYS.length; i++) {
            final String key = PERM_KEYS[i];
            final String label = PERM_LABELS[i];
            int baseSlot = rows[i];

            addButton(baseSlot - 2, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.PAPER, "\u00a7f" + label))
                    .consumer(e -> {})
            );

            addToggle(baseSlot, key, label, permsSameRegion, "\u00a7aSame Region", player);
            addToggle(baseSlot + 2, key, label, permsSameNation, "\u00a7eSame Nation", player);
            addToggle(baseSlot + 4, key, label, permsForeign, "\u00a7cForeign", player);
        }

        addButton(53, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7cBack"))
                .consumer(e -> backCallback.accept((Player) e.getWhoClicked()))
        );

        if (rolePermsOpener != null) {
            addButton(52, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.NAME_TAG, "\u00a7eRole Permissions",
                            "\u00a77Set permissions per role.",
                            "\u00a7eClick to manage."))
                    .consumer(e -> rolePermsOpener.accept((Player) e.getWhoClicked()))
            );
        }

        if (plugin.getSettings().isMobPermissionsEnabled() && deniedMobs != null) {
            addButton(50, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.SPAWNER, "\u00a76Mob Management",
                            "\u00a77Control which hostile mobs",
                            "\u00a77can spawn in this area.",
                            "\u00a7eClick to configure."))
                    .consumer(e -> {
                        Player p = (Player) e.getWhoClicked();
                        Consumer<Player> mobBack = mobBackCallback != null ? mobBackCallback : bp ->
                            plugin.getGUIManager().openGUI(new PropertyPermissionsGUI(plugin,
                                    permsSameRegion, permsSameNation, permsForeign,
                                    saveCallback, backCallback, deniedMobs, mobSaveCallback, mobBackCallback), bp);
                        Runnable mobSave = mobSaveCallback != null ? mobSaveCallback : saveCallback;
                        plugin.getGUIManager().openGUI(new MobPermissionsGUI(plugin, deniedMobs,
                                mobSave, mobBack, 0), p);
                    })
            );
        }

        super.decorate(player);
    }

    private void addToggle(int slot, String permKey, String permLabel,
                           Map<String, Boolean> map, String groupName, Player viewer) {
        boolean allowed = map.getOrDefault(permKey, false);
        addButton(slot, new InventoryButton()
                .creator(p -> {
                    boolean val = map.getOrDefault(permKey, false);
                    if (val) {
                        return ItemUtil.buildItem(XMaterial.LIME_DYE, "\u00a7a\u2714 " + permLabel,
                                groupName + " \u00a7f\u2192 \u00a7aAllowed",
                                "\u00a77Click to deny.");
                    } else {
                        return ItemUtil.buildItem(XMaterial.GRAY_DYE, "\u00a7c\u2718 " + permLabel,
                                groupName + " \u00a7f\u2192 \u00a7cDenied",
                                "\u00a77Click to allow.");
                    }
                })
                .consumer(e -> {
                    boolean current = map.getOrDefault(permKey, false);
                    map.put(permKey, !current);
                    saveCallback.run();
                    plugin.getGUIManager().openGUI(new PropertyPermissionsGUI(plugin,
                            permsSameRegion, permsSameNation, permsForeign,
                            saveCallback, backCallback, deniedMobs, mobSaveCallback, mobBackCallback), (Player) e.getWhoClicked());
                })
        );
    }
}
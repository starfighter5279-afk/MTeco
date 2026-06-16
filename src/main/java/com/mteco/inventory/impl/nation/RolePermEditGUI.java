package com.mteco.inventory.impl.nation;

import com.cryptomorin.xseries.XMaterial;
import com.mteco.MTeco;
import com.mteco.inventory.InventoryButton;
import com.mteco.inventory.InventoryGUI;
import com.mteco.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public class RolePermEditGUI extends InventoryGUI {
    private final MTeco plugin;
    private final Map<UUID, Map<String, Boolean>> rolePerms;
    private final UUID roleId;
    private final String roleDisplayName;
    private final Runnable saveCallback;
    private final Consumer<Player> backCallback;

    private static final String[] PERM_KEYS = {"enter", "interact", "build", "containers"};
    private static final String[] PERM_LABELS = {"Enter", "Interact", "Build", "Open Containers"};

    public RolePermEditGUI(MTeco plugin, Map<UUID, Map<String, Boolean>> rolePerms,
                           UUID roleId, String roleDisplayName,
                           Runnable saveCallback, Consumer<Player> backCallback) {
        this.plugin = plugin;
        this.rolePerms = rolePerms;
        this.roleId = roleId;
        this.roleDisplayName = roleDisplayName;
        this.saveCallback = saveCallback;
        this.backCallback = backCallback;
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 27, "\u00a7eEdit Role Perms");
    }

    @Override
    public void decorate(Player player) {
        fillGlass(27, XMaterial.BLUE_STAINED_GLASS_PANE);

        Map<String, Boolean> perms = rolePerms.computeIfAbsent(roleId, k -> {
            Map<String, Boolean> def = new HashMap<>();
            def.put("enter", false);
            def.put("interact", false);
            def.put("build", false);
            def.put("containers", false);
            return def;
        });

        addButton(4, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.NAME_TAG, roleDisplayName,
                        "\u00a77Toggle permissions for this role."))
                .consumer(e -> {})
        );

        int[] slots = {10, 12, 14, 16};
        for (int i = 0; i < PERM_KEYS.length; i++) {
            final String key = PERM_KEYS[i];
            final String label = PERM_LABELS[i];
            int slot = slots[i];

            addButton(slot, new InventoryButton()
                    .creator(p -> {
                        boolean val = perms.getOrDefault(key, false);
                        if (val) {
                            return ItemUtil.buildItem(XMaterial.LIME_DYE, "\u00a7a\u2714 " + label,
                                    "\u00a7aAllowed", "\u00a77Click to deny.");
                        } else {
                            return ItemUtil.buildItem(XMaterial.GRAY_DYE, "\u00a7c\u2718 " + label,
                                    "\u00a7cDenied", "\u00a77Click to allow.");
                        }
                    })
                    .consumer(e -> {
                        boolean current = perms.getOrDefault(key, false);
                        perms.put(key, !current);
                        saveCallback.run();
                        plugin.getGUIManager().openGUI(new RolePermEditGUI(plugin, rolePerms, roleId, roleDisplayName, saveCallback, backCallback), (Player) e.getWhoClicked());
                    })
            );
        }

        addButton(22, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7cBack"))
                .consumer(e -> backCallback.accept((Player) e.getWhoClicked()))
        );

        super.decorate(player);
    }
}
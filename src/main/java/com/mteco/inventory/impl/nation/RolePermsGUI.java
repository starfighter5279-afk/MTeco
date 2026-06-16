package com.mteco.inventory.impl.nation;

import com.cryptomorin.xseries.XMaterial;
import com.mteco.MTeco;
import com.mteco.data.BusinessData;
import com.mteco.data.BusinessRole;
import com.mteco.data.CustomRole;
import com.mteco.data.NationData;
import com.mteco.inventory.InventoryButton;
import com.mteco.inventory.InventoryGUI;
import com.mteco.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public class RolePermsGUI extends InventoryGUI {
    private final MTeco plugin;
    private final Map<UUID, Map<String, Boolean>> rolePerms;
    private final List<UUID> roleIds;
    private final List<String> roleNames;
    private final List<String> roleColors;
    private final Runnable saveCallback;
    private final Consumer<Player> backCallback;
    private final int page;

    private static final String[] PERM_KEYS = {"enter", "interact", "build", "containers"};
    private static final String[] PERM_LABELS = {"Enter", "Interact", "Build", "Containers"};

    public RolePermsGUI(MTeco plugin, Map<UUID, Map<String, Boolean>> rolePerms,
                        List<UUID> roleIds, List<String> roleNames, List<String> roleColors,
                        Runnable saveCallback, Consumer<Player> backCallback, int page) {
        this.plugin = plugin;
        this.rolePerms = rolePerms;
        this.roleIds = roleIds;
        this.roleNames = roleNames;
        this.roleColors = roleColors;
        this.saveCallback = saveCallback;
        this.backCallback = backCallback;
        this.page = page;
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 54, "\u00a7eRole Permissions");
    }

    @Override
    public void decorate(Player player) {
        fillPagedGui(54, XMaterial.BLUE_STAINED_GLASS_PANE);

        addButton(4, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.NAME_TAG, "\u00a7eRole Permissions",
                        "\u00a77Set property/room permissions per role.",
                        "\u00a77Click a role to edit its permissions."))
                .consumer(e -> {})
        );

        int itemsPerPage = 28;
        int startIndex = page * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, roleIds.size());

        int[] slots = {
                10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34,
                37, 38, 39, 40, 41, 42, 43
        };

        int slotIndex = 0;
        for (int i = startIndex; i < endIndex; i++) {
            if (slotIndex >= slots.length) break;
            UUID roleId = roleIds.get(i);
            String roleName = roleNames.get(i);
            String roleColor = roleColors.get(i);
            Map<String, Boolean> perms = rolePerms.getOrDefault(roleId, new HashMap<>());

            List<String> lore = new ArrayList<>();
            for (int j = 0; j < PERM_KEYS.length; j++) {
                boolean val = perms.getOrDefault(PERM_KEYS[j], false);
                lore.add((val ? "\u00a7a\u2714 " : "\u00a7c\u2718 ") + PERM_LABELS[j]);
            }
            lore.add("");
            lore.add("\u00a7eClick to edit permissions");

            final UUID fRoleId = roleId;
            final String fRoleName = roleName;
            final String fRoleColor = roleColor;
            int slot = slots[slotIndex++];

            addButton(slot, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.NAME_TAG, fRoleColor + fRoleName, lore.toArray(new String[0])))
                    .consumer(e -> {
                        Player p = (Player) e.getWhoClicked();
                        if (!rolePerms.containsKey(fRoleId)) {
                            Map<String, Boolean> defPerms = new HashMap<>();
                            defPerms.put("enter", false);
                            defPerms.put("interact", false);
                            defPerms.put("build", false);
                            defPerms.put("containers", false);
                            rolePerms.put(fRoleId, defPerms);
                            saveCallback.run();
                        }
                        plugin.getGUIManager().openGUI(new RolePermEditGUI(plugin, rolePerms, fRoleId, fRoleColor + fRoleName,
                                saveCallback,
                                back -> plugin.getGUIManager().openGUI(new RolePermsGUI(plugin, rolePerms, roleIds, roleNames, roleColors, saveCallback, backCallback, page), back)), p);
                    })
            );
        }

        if (page > 0) {
            addButton(47, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7ePrevious Page"))
                    .consumer(e -> plugin.getGUIManager().openGUI(new RolePermsGUI(plugin, rolePerms, roleIds, roleNames, roleColors, saveCallback, backCallback, page - 1), (Player) e.getWhoClicked()))
            );
        }

        if (endIndex < roleIds.size()) {
            addButton(50, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7eNext Page"))
                    .consumer(e -> plugin.getGUIManager().openGUI(new RolePermsGUI(plugin, rolePerms, roleIds, roleNames, roleColors, saveCallback, backCallback, page + 1), (Player) e.getWhoClicked()))
            );
        }

        addButton(53, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.BARRIER, "\u00a7cBack"))
                .consumer(e -> backCallback.accept((Player) e.getWhoClicked()))
        );

        super.decorate(player);
    }

    public static void buildGovRoles(MTeco plugin, NationData nation,
                                     List<UUID> ids, List<String> names, List<String> colors) {
        if (plugin.getRolesConfig().isCustomGovernmentEnabled()) {
            for (CustomRole role : nation.getCustomRoles()) {
                ids.add(role.getRoleId());
                names.add(role.getName());
                colors.add(role.getColorCode());
            }
        } else {
            if (plugin.getRolesConfig().isVicePresidentEnabled()) {
                ids.add(UUID.nameUUIDFromBytes("legacy-vice-president".getBytes()));
                names.add("Vice President");
                colors.add("\u00a79");
            }
            if (plugin.getRolesConfig().isTreasurerEnabled()) {
                ids.add(UUID.nameUUIDFromBytes("legacy-treasurer".getBytes()));
                names.add("Treasurer");
                colors.add("\u00a7e");
            }
            if (plugin.getRolesConfig().isSecurityHeadEnabled()) {
                ids.add(UUID.nameUUIDFromBytes("legacy-security-head".getBytes()));
                names.add("Security Head");
                colors.add("\u00a7c");
            }
        }
    }

    public static void buildBizRoles(BusinessData biz,
                                     List<UUID> ids, List<String> names, List<String> colors) {
        for (BusinessRole role : biz.getRoles()) {
            ids.add(role.getRoleId());
            names.add(role.getName());
            colors.add(role.getColorCode());
        }
    }
}
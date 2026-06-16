package com.mteco.inventory.impl.nation;

import com.cryptomorin.xseries.XMaterial;
import com.mteco.MTeco;
import com.mteco.data.CustomRole;
import com.mteco.data.NationData;
import com.mteco.data.RoleMode;
import com.mteco.inventory.InventoryButton;
import com.mteco.inventory.InventoryGUI;
import com.mteco.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.UUID;

public class CustomRoleEditGUI extends InventoryGUI {
    private final MTeco plugin;
    private final UUID nationId;
    private final UUID roleId;

    private static final XMaterial[] COLOR_MATS = {
            XMaterial.WHITE_WOOL, XMaterial.RED_WOOL, XMaterial.ORANGE_WOOL, XMaterial.YELLOW_WOOL,
            XMaterial.LIME_WOOL, XMaterial.LIGHT_BLUE_WOOL, XMaterial.BLUE_WOOL, XMaterial.PURPLE_WOOL
    };
    private static final String[] COLOR_CODES = {"\u00a7f", "\u00a7c", "\u00a76", "\u00a7e", "\u00a7a", "\u00a7b", "\u00a79", "\u00a75"};
    private static final String[] COLOR_NAMES = {"White", "Red", "Gold", "Yellow", "Green", "Aqua", "Blue", "Purple"};

    public CustomRoleEditGUI(MTeco plugin, UUID nationId, UUID roleId) {
        this.plugin = plugin;
        this.nationId = nationId;
        this.roleId = roleId;
    }

    @Override
    protected Inventory createInventory() {
        CustomRole role = findRole();
        String title = role != null ? role.getColorCode() + role.getName() + " \u00a78Role" : "\u00a78Edit Role";
        return Bukkit.createInventory(null, 36, title);
    }

    @Override
    public void decorate(Player player) {
        fillGlass(36, XMaterial.BLUE_STAINED_GLASS_PANE);

        CustomRole role = findRole();
        if (role == null) {
            player.sendMessage("\u00a7cRole not found.");
            return;
        }

        addButton(10, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.WRITABLE_BOOK, "\u00a7aEdit Permissions",
                        "\u00a77Active: \u00a7f" + role.getPermissions().size() + " \u00a77permissions",
                        "\u00a7eClick to toggle permissions."))
                .consumer(e -> plugin.getGUIManager().openGUI(
                        new CustomRolePermissionsGUI(plugin, nationId, roleId), (Player) e.getWhoClicked()))
        );

        addButton(11, createModeButton(role));

        addButton(12, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.PLAYER_HEAD, "\u00a7eManage Members",
                        "\u00a77Members: \u00a7f" + role.getMemberUUIDs().size(),
                        "\u00a7eClick to add or remove members."))
                .consumer(e -> plugin.getGUIManager().openGUI(
                        new CustomRoleMembersGUI(plugin, nationId, roleId, 0), (Player) e.getWhoClicked()))
        );

        addButton(14, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.NAME_TAG, "\u00a76Rename Role",
                        "\u00a77Current: " + role.getColorCode() + role.getName(),
                        "\u00a7eClick to rename."))
                .consumer(e -> {
                    Player p = (Player) e.getWhoClicked();
                    p.closeInventory();
                    plugin.getChatInputManager().requestInput(p, "\u00a7eEnter new role name:", name -> {
                        if (name.isEmpty()) { p.sendMessage("\u00a7cName cannot be empty."); return; }
                        CustomRole r = findRole();
                        if (r == null) return;
                        r.setName(name);
                        NationData n = plugin.getNationManager().loadNation(nationId);
                        if (n != null) plugin.getNationManager().saveNation(n);
                        p.sendMessage("\u00a7aRole renamed to \u00a7f" + name + "\u00a7a.");
                        plugin.getServer().getScheduler().runTaskLater(plugin, () ->
                            plugin.getGUIManager().openGUI(new CustomRoleEditGUI(plugin, nationId, roleId), p), 1L);
                    });
                })
        );

        addButton(16, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.RED_DYE, "\u00a7cDelete Role",
                        "\u00a77Permanently delete this role.",
                        "\u00a77All members will lose their permissions.",
                        "\u00a7cClick to delete."))
                .consumer(e -> {
                    Player p = (Player) e.getWhoClicked();
                    NationData n = plugin.getNationManager().loadNation(nationId);
                    if (n == null) return;
                    CustomRole deleted = null;
                    for (CustomRole r : n.getCustomRoles()) {
                        if (r.getRoleId().equals(roleId)) { deleted = r; break; }
                    }
                    if (deleted != null) {
                        n.getCustomRoles().remove(deleted);
                        if (plugin.getDiscordBotManager() != null) {
                            plugin.getDiscordBotManager().onCustomRoleDeleted(n, deleted);
                        }
                    }
                    plugin.getNationManager().saveNation(n);
                    p.sendMessage("\u00a7cRole deleted.");
                    plugin.getGUIManager().openGUI(new CustomRolesGUI(plugin, nationId), p);
                })
        );

        for (int i = 0; i < COLOR_MATS.length; i++) {
            final int idx = i;
            final boolean isCurrent = COLOR_CODES[idx].equals(role.getColorCode());
            addButton(18 + i, new InventoryButton()
                    .creator(p -> {
                        String display = (isCurrent ? "\u00a7a\u25b6 " : "\u00a77") + COLOR_CODES[idx] + COLOR_NAMES[idx];
                        return ItemUtil.buildItem(COLOR_MATS[idx], display,
                                isCurrent ? "\u00a7aCurrent color" : "\u00a7eClick to select");
                    })
                    .consumer(e -> {
                        CustomRole r = findRole();
                        if (r == null) return;
                        r.setColorCode(COLOR_CODES[idx]);
                        NationData n = plugin.getNationManager().loadNation(nationId);
                        if (n != null) plugin.getNationManager().saveNation(n);
                        plugin.getGUIManager().openGUI(new CustomRoleEditGUI(plugin, nationId, roleId), (Player) e.getWhoClicked());
                    })
            );
        }

        addButton(31, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7cBack"))
                .consumer(e -> plugin.getGUIManager().openGUI(new CustomRolesGUI(plugin, nationId), (Player) e.getWhoClicked()))
        );

        super.decorate(player);
    }

    private InventoryButton createModeButton(CustomRole role) {
        RoleMode currentMode = role.getRoleMode();
        XMaterial modeIcon;
        String modeColor;
        switch (currentMode) {
            case LEADER:
                modeIcon = XMaterial.GOLDEN_HELMET;
                modeColor = "\u00a7c";
                break;
            case COUNCIL:
                modeIcon = XMaterial.BOOK;
                modeColor = "\u00a76";
                break;
            default:
                modeIcon = XMaterial.LEATHER_CHESTPLATE;
                modeColor = "\u00a7a";
                break;
        }

        return new InventoryButton()
                .creator(p -> ItemUtil.buildItem(modeIcon, modeColor + "Role Mode: " + currentMode.getDisplayName(),
                        "\u00a77" + currentMode.getDescription(),
                        "",
                        "\u00a77Modes:",
                        (plugin.getRolesConfig().isLeaderModeEnabled() ? "\u00a7c \u2022 Leader " : "\u00a78 \u2022 Leader (disabled) ") + (currentMode == RoleMode.LEADER ? "\u00a7a\u25c0" : ""),
                        (plugin.getRolesConfig().isCouncilModeEnabled() ? "\u00a76 \u2022 Council " : "\u00a78 \u2022 Council (disabled) ") + (currentMode == RoleMode.COUNCIL ? "\u00a7a\u25c0" : ""),
                        (plugin.getRolesConfig().isEmployeeModeEnabled() ? "\u00a7a \u2022 Employee " : "\u00a78 \u2022 Employee (disabled) ") + (currentMode == RoleMode.EMPLOYEE ? "\u00a7a\u25c0" : ""),
                        "",
                        "\u00a7eClick to cycle mode."))
                .consumer(e -> {
                    CustomRole r = findRole();
                    if (r == null) return;
                    RoleMode next = cycleRoleMode(r.getRoleMode());
                    r.setRoleMode(next);
                    NationData n = plugin.getNationManager().loadNation(nationId);
                    if (n != null) plugin.getNationManager().saveNation(n);
                    plugin.getGUIManager().openGUI(new CustomRoleEditGUI(plugin, nationId, roleId), (Player) e.getWhoClicked());
                });
    }

    private RoleMode cycleRoleMode(RoleMode current) {
        RoleMode[] modes = RoleMode.values();
        int start = current.ordinal();
        for (int i = 1; i <= modes.length; i++) {
            RoleMode candidate = modes[(start + i) % modes.length];
            if (plugin.getRolesConfig().isRoleModeEnabled(candidate)) {
                return candidate;
            }
        }
        return current;
    }

    private CustomRole findRole() {
        NationData nation = plugin.getNationManager().loadNation(nationId);
        if (nation == null) return null;
        for (CustomRole r : nation.getCustomRoles()) {
            if (r.getRoleId().equals(roleId)) return r;
        }
        return null;
    }
}
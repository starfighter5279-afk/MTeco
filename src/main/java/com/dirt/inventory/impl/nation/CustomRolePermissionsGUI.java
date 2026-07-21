package com.dirt.inventory.impl.nation;

import com.cryptomorin.xseries.XMaterial;
import com.dirt.DirtEconomy;
import com.dirt.data.CustomRole;
import com.dirt.data.NationData;
import com.dirt.data.NationPermission;
import com.dirt.data.RoleMode;
import com.dirt.inventory.InventoryButton;
import com.dirt.inventory.InventoryGUI;
import com.dirt.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class CustomRolePermissionsGUI extends InventoryGUI {
    private final DirtEconomy plugin;
    private final UUID nationId;
    private final UUID roleId;

    public CustomRolePermissionsGUI(DirtEconomy plugin, UUID nationId, UUID roleId) {
        this.plugin = plugin;
        this.nationId = nationId;
        this.roleId = roleId;
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 54, "\u00a7aEdit Permissions");
    }

    @Override
    public void decorate(Player player) {
        fillGlass(54, XMaterial.BLUE_STAINED_GLASS_PANE);

        CustomRole role = findRole();
        if (role == null) return;

        if (role.getRoleMode() == RoleMode.LEADER) {
            addButton(22, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.GOLDEN_HELMET, "\u00a7c\u00a7lLeader Mode",
                            "\u00a77This role is in Leader mode.",
                            "\u00a77Leaders have \u00a7aall permissions \u00a77automatically.",
                            "",
                            "\u00a77Change the role mode in the",
                            "\u00a77role editor to assign individual permissions."))
                    .consumer(e -> {})
            );
            addButton(49, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7cBack"))
                    .consumer(e -> plugin.getGUIManager().openGUI(
                            new CustomRoleEditGUI(plugin, nationId, roleId), (Player) e.getWhoClicked()))
            );
            super.decorate(player);
            return;
        }

        Set<NationPermission> enabledPerms = plugin.getRolesConfig().getEnabledPermissions();

        Map<String, List<NationPermission>> categories = new LinkedHashMap<>();
        for (NationPermission perm : NationPermission.values()) {
            if (!enabledPerms.contains(perm)) continue;
            categories.computeIfAbsent(perm.getCategory(), k -> new ArrayList<>()).add(perm);
        }

        Map<String, XMaterial> categoryColors = new LinkedHashMap<>();
        categoryColors.put("Government", XMaterial.ORANGE_STAINED_GLASS_PANE);
        categoryColors.put("Territory", XMaterial.GREEN_STAINED_GLASS_PANE);
        categoryColors.put("Economy", XMaterial.YELLOW_STAINED_GLASS_PANE);
        categoryColors.put("Security", XMaterial.RED_STAINED_GLASS_PANE);
        categoryColors.put("Diplomacy", XMaterial.LIGHT_BLUE_STAINED_GLASS_PANE);

        int row = 0;
        for (Map.Entry<String, List<NationPermission>> entry : categories.entrySet()) {
            if (row >= 5) break;
            String category = entry.getKey();
            List<NationPermission> perms = entry.getValue();
            int baseSlot = row * 9;
            XMaterial color = categoryColors.getOrDefault(category, XMaterial.WHITE_STAINED_GLASS_PANE);

            addButton(baseSlot, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(color, "\u00a7e\u00a7l" + category))
                    .consumer(e -> {})
            );

            for (int i = 0; i < perms.size() && i < 7; i++) {
                addPermButton(baseSlot + 1 + i, perms.get(i), role);
            }

            row++;
        }

        addButton(49, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7cBack"))
                .consumer(e -> plugin.getGUIManager().openGUI(
                        new CustomRoleEditGUI(plugin, nationId, roleId), (Player) e.getWhoClicked()))
        );

        super.decorate(player);
    }

    private void addPermButton(int slot, NationPermission perm, CustomRole role) {
        boolean enabled = role.getPermissions().contains(perm);
        addButton(slot, new InventoryButton()
                .creator(p -> {
                    XMaterial icon = enabled ? XMaterial.LIME_DYE : XMaterial.GRAY_DYE;
                    String prefix = enabled ? "\u00a7a\u2714 " : "\u00a7c\u2718 ";
                    return ItemUtil.buildItem(icon, prefix + perm.getDisplayName(),
                            "\u00a77" + perm.getDescription(),
                            "",
                            enabled ? "\u00a7eClick to disable" : "\u00a7eClick to enable");
                })
                .consumer(e -> {
                    CustomRole r = findRole();
                    if (r == null) return;
                    if (r.getPermissions().contains(perm)) {
                        r.getPermissions().remove(perm);
                    } else {
                        r.getPermissions().add(perm);
                    }
                    NationData n = plugin.getNationManager().loadNation(nationId);
                    if (n != null) plugin.getNationManager().saveNation(n);
                    plugin.getGUIManager().openGUI(
                            new CustomRolePermissionsGUI(plugin, nationId, roleId), (Player) e.getWhoClicked());
                })
        );
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
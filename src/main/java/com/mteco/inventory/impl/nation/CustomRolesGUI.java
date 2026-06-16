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

import java.util.List;
import java.util.UUID;

public class CustomRolesGUI extends InventoryGUI {
    private final MTeco plugin;
    private final UUID nationId;
    private final int page;

    public CustomRolesGUI(MTeco plugin, UUID nationId) {
        this(plugin, nationId, 0);
    }

    public CustomRolesGUI(MTeco plugin, UUID nationId, int page) {
        this.plugin = plugin;
        this.nationId = nationId;
        this.page = page;
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 54, "\u00a7bNation Roles");
    }

    @Override
    public void decorate(Player player) {
        fillGlass(54, XMaterial.BLUE_STAINED_GLASS_PANE);

        NationData nation = plugin.getNationManager().loadNation(nationId);
        List<CustomRole> roles = nation != null ? nation.getCustomRoles() : List.of();

        int perPage = 45;
        int start = page * perPage;
        int end = Math.min(start + perPage, roles.size());

        for (int i = start; i < end; i++) {
            CustomRole role = roles.get(i);
            int slot = i - start;
            addButton(slot, new InventoryButton()
                    .creator(p -> {
                        String modePrefix = "";
                        switch (role.getRoleMode()) {
                            case LEADER: modePrefix = "\u00a7c[Leader] "; break;
                            case COUNCIL: modePrefix = "\u00a76[Council] "; break;
                            case EMPLOYEE: modePrefix = "\u00a7a[Employee] "; break;
                        }
                        return ItemUtil.buildItem(XMaterial.NAME_TAG,
                            role.getColorCode() + role.getName(),
                            "\u00a77Mode: " + modePrefix + role.getRoleMode().getDisplayName(),
                            "\u00a77Members: \u00a7f" + role.getMemberUUIDs().size(),
                            "\u00a77Permissions: \u00a7f" + (role.getRoleMode() == RoleMode.LEADER ? "ALL" : String.valueOf(role.getPermissions().size())),
                            "",
                            "\u00a7eClick to edit");
                    })
                    .consumer(e -> plugin.getGUIManager().openGUI(
                            new CustomRoleEditGUI(plugin, nationId, role.getRoleId()), (Player) e.getWhoClicked()))
            );
        }

        if (page > 0) {
            addButton(45, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7ePrevious Page"))
                    .consumer(e -> plugin.getGUIManager().openGUI(new CustomRolesGUI(plugin, nationId, page - 1), (Player) e.getWhoClicked()))
            );
        }
        if (end < roles.size()) {
            addButton(53, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7eNext Page"))
                    .consumer(e -> plugin.getGUIManager().openGUI(new CustomRolesGUI(plugin, nationId, page + 1), (Player) e.getWhoClicked()))
            );
        }

        int maxRoles = plugin.getSettings().getMaxCustomRoles();
        boolean canCreate = roles.size() < maxRoles;
        addButton(49, new InventoryButton()
                .creator(p -> canCreate
                        ? ItemUtil.buildItem(XMaterial.EMERALD, "\u00a7aCreate New Role",
                                "\u00a77Create a custom role for your nation.",
                                "\u00a77Roles: \u00a7f" + roles.size() + "\u00a77/\u00a7f" + maxRoles)
                        : ItemUtil.buildItem(XMaterial.BARRIER, "\u00a7cRole Limit Reached",
                                "\u00a77Maximum roles: \u00a7f" + maxRoles))
                .consumer(e -> {
                    if (!canCreate) return;
                    Player p = (Player) e.getWhoClicked();
                    p.closeInventory();
                    plugin.getChatInputManager().requestInput(p, "\u00a7eEnter a name for the new role:", name -> {
                        if (name.isEmpty()) {
                            p.sendMessage("\u00a7cRole name cannot be empty.");
                            return;
                        }
                        NationData n = plugin.getNationManager().loadNation(nationId);
                        if (n == null) return;
                        if (n.getCustomRoles().size() >= plugin.getSettings().getMaxCustomRoles()) {
                            p.sendMessage("\u00a7cRole limit reached.");
                            return;
                        }
                        CustomRole newRole = new CustomRole();
                        newRole.setRoleId(UUID.randomUUID());
                        newRole.setName(name);
                        n.getCustomRoles().add(newRole);
                        plugin.getNationManager().saveNation(n);
                        p.sendMessage("\u00a7aRole \u00a7f" + name + " \u00a7acreated! Opening editor...");
                        plugin.getServer().getScheduler().runTaskLater(plugin, () ->
                            plugin.getGUIManager().openGUI(new CustomRoleEditGUI(plugin, nationId, newRole.getRoleId()), p), 1L);
                    });
                })
        );

        addButton(46, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7cBack"))
                .consumer(e -> plugin.getGUIManager().openGUI(new MemberManagementGUI(plugin, nationId), (Player) e.getWhoClicked()))
        );

        super.decorate(player);
    }
}
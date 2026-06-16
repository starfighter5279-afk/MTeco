package com.mteco.inventory.impl.business;

import com.cryptomorin.xseries.XMaterial;
import com.mteco.MTeco;
import com.mteco.data.BusinessData;
import com.mteco.data.BusinessRole;
import com.mteco.inventory.InventoryButton;
import com.mteco.inventory.InventoryGUI;
import com.mteco.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.UUID;

public class BusinessRoleEditGUI extends InventoryGUI {
    private final MTeco plugin;
    private final UUID businessId;
    private final UUID roleId;

    public BusinessRoleEditGUI(MTeco plugin, UUID businessId, UUID roleId) {
        this.plugin = plugin;
        this.businessId = businessId;
        this.roleId = roleId;
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 27, "\u00a76Edit Role");
    }

    @Override
    public void decorate(Player player) {
        fillGlass(27, XMaterial.LIME_STAINED_GLASS_PANE);

        BusinessData biz = plugin.getBusinessManager().loadBusiness(businessId);
        if (biz == null) { super.decorate(player); return; }

        BusinessRole role = findRole(biz);
        if (role == null) { super.decorate(player); return; }

        boolean isDefault = role.getRoleId().equals(biz.getDefaultRoleId());

        addButton(4, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.NAME_TAG,
                        role.getColorCode() + role.getName(),
                        "\u00a77Members: \u00a7f" + role.getMemberUUIDs().size(),
                        "\u00a77Permissions: \u00a7f" + role.getPermissions().size(),
                        isDefault ? "\u00a7a\u2605 Default hire role" : ""))
                .consumer(e -> {})
        );

        addButton(10, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.WRITABLE_BOOK, "\u00a7eRename Role",
                        "\u00a77Click to change this role's name."))
                .consumer(e -> renameRole(biz, role, (Player) e.getWhoClicked()))
        );

        addButton(11, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.COMPARATOR, "\u00a7bPermissions",
                        "\u00a77Click to manage permissions."))
                .consumer(e -> plugin.getGUIManager().openGUI(
                        new BusinessRolePermissionsGUI(plugin, businessId, roleId),
                        (Player) e.getWhoClicked()))
        );

        addButton(12, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.PLAYER_HEAD, "\u00a7aMembers",
                        "\u00a77Click to manage role members."))
                .consumer(e -> plugin.getGUIManager().openGUI(
                        new BusinessRoleMembersGUI(plugin, businessId, roleId),
                        (Player) e.getWhoClicked()))
        );

        addButton(14, new InventoryButton()
                .creator(p -> {
                    if (isDefault) {
                        return ItemUtil.buildItem(XMaterial.LIME_WOOL, "\u00a7a\u2605 Default Hire Role",
                                "\u00a77New employees are assigned this role.",
                                "\u00a77Click to unset.");
                    }
                    return ItemUtil.buildItem(XMaterial.GRAY_WOOL, "\u00a77Set as Default Hire Role",
                            "\u00a77New employees will be assigned this role.",
                            "\u00a77Click to set.");
                })
                .consumer(e -> {
                    if (isDefault) {
                        biz.setDefaultRoleId(null);
                    } else {
                        biz.setDefaultRoleId(role.getRoleId());
                    }
                    plugin.getBusinessManager().saveBusiness(biz);
                    plugin.getGUIManager().openGUI(new BusinessRoleEditGUI(plugin, businessId, roleId), (Player) e.getWhoClicked());
                })
        );

        addButton(16, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.RED_WOOL, "\u00a7cDelete Role",
                        "\u00a77Permanently delete this role."))
                .consumer(e -> {
                    biz.getRoles().removeIf(r -> r.getRoleId().equals(roleId));
                    if (roleId.equals(biz.getDefaultRoleId())) biz.setDefaultRoleId(null);
                    plugin.getBusinessManager().saveBusiness(biz);
                    Player p = (Player) e.getWhoClicked();
                    p.sendMessage("\u00a7cRole deleted.");
                    plugin.getGUIManager().openGUI(new BusinessRolesGUI(plugin, businessId), p);
                })
        );

        addButton(22, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7cBack"))
                .consumer(e -> plugin.getGUIManager().openGUI(
                        new BusinessRolesGUI(plugin, businessId), (Player) e.getWhoClicked()))
        );

        super.decorate(player);
    }

    private BusinessRole findRole(BusinessData biz) {
        for (BusinessRole r : biz.getRoles()) {
            if (r.getRoleId().equals(roleId)) return r;
        }
        return null;
    }

    private void renameRole(BusinessData biz, BusinessRole role, Player player) {
        player.closeInventory();
        plugin.getChatInputManager().requestInput(player, "\u00a7eEnter the new name for this role:", name -> {
            if (name.trim().isEmpty()) {
                player.sendMessage("\u00a7cName cannot be empty.");
                return;
            }
            role.setName(name.trim());
            plugin.getBusinessManager().saveBusiness(biz);
            player.sendMessage("\u00a7aRole renamed to '\u00a7f" + name.trim() + "\u00a7a'.");
            plugin.getServer().getScheduler().runTask(plugin,
                    () -> plugin.getGUIManager().openGUI(new BusinessRoleEditGUI(plugin, businessId, roleId), player));
        });
    }
}
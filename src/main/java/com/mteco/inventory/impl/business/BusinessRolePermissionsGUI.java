package com.mteco.inventory.impl.business;

import com.cryptomorin.xseries.XMaterial;
import com.mteco.MTeco;
import com.mteco.data.BusinessData;
import com.mteco.data.BusinessPermission;
import com.mteco.data.BusinessRole;
import com.mteco.inventory.InventoryButton;
import com.mteco.inventory.InventoryGUI;
import com.mteco.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.UUID;

public class BusinessRolePermissionsGUI extends InventoryGUI {
    private final MTeco plugin;
    private final UUID businessId;
    private final UUID roleId;

    public BusinessRolePermissionsGUI(MTeco plugin, UUID businessId, UUID roleId) {
        this.plugin = plugin;
        this.businessId = businessId;
        this.roleId = roleId;
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 27, "\u00a76Role Permissions");
    }

    @Override
    public void decorate(Player player) {
        fillGlass(27, XMaterial.LIME_STAINED_GLASS_PANE);

        BusinessData biz = plugin.getBusinessManager().loadBusiness(businessId);
        if (biz == null) { super.decorate(player); return; }

        BusinessRole role = null;
        for (BusinessRole r : biz.getRoles()) {
            if (r.getRoleId().equals(roleId)) { role = r; break; }
        }
        if (role == null) { super.decorate(player); return; }

        BusinessPermission[] perms = BusinessPermission.values();
        for (int i = 0; i < perms.length && i < 18; i++) {
            BusinessPermission perm = perms[i];
            boolean has = role.getPermissions().contains(perm);
            final BusinessRole finalRole = role;
            addButton(i, new InventoryButton()
                    .creator(p -> {
                        boolean enabled = finalRole.getPermissions().contains(perm);
                        XMaterial mat = enabled ? XMaterial.LIME_DYE : XMaterial.GRAY_DYE;
                        return ItemUtil.buildItem(mat,
                                (enabled ? "\u00a7a\u2713 " : "\u00a7c\u2717 ") + perm.getDisplayName(),
                                "\u00a77" + perm.getDescription(),
                                enabled ? "\u00a77Click to \u00a7cremove" : "\u00a77Click to \u00a7aadd");
                    })
                    .consumer(e -> {
                        if (finalRole.getPermissions().contains(perm)) {
                            finalRole.getPermissions().remove(perm);
                        } else {
                            finalRole.getPermissions().add(perm);
                        }
                        plugin.getBusinessManager().saveBusiness(biz);
                        plugin.getGUIManager().openGUI(
                                new BusinessRolePermissionsGUI(plugin, businessId, roleId),
                                (Player) e.getWhoClicked());
                    })
            );
        }

        addButton(22, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7cBack"))
                .consumer(e -> plugin.getGUIManager().openGUI(
                        new BusinessRoleEditGUI(plugin, businessId, roleId),
                        (Player) e.getWhoClicked()))
        );

        super.decorate(player);
    }
}
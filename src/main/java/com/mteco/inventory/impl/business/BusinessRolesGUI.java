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

import java.util.List;
import java.util.UUID;

public class BusinessRolesGUI extends InventoryGUI {
    private final MTeco plugin;
    private final UUID businessId;

    public BusinessRolesGUI(MTeco plugin, UUID businessId) {
        this.plugin = plugin;
        this.businessId = businessId;
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 54, "\u00a76Business Roles");
    }

    @Override
    public void decorate(Player player) {
        fillPagedGui(54, XMaterial.LIME_STAINED_GLASS_PANE);

        BusinessData biz = plugin.getBusinessManager().loadBusiness(businessId);
        if (biz == null) { super.decorate(player); return; }

        List<BusinessRole> roles = biz.getRoles();
        int slot = 0;
        for (BusinessRole role : roles) {
            if (slot >= 36) break;
            final BusinessRole finalRole = role;
            String defaultTag = role.getRoleId().equals(biz.getDefaultRoleId()) ? " \u00a7a(Default)" : "";
            addButton(slot, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.NAME_TAG,
                            finalRole.getColorCode() + finalRole.getName() + defaultTag,
                            "\u00a77Members: \u00a7f" + finalRole.getMemberUUIDs().size(),
                            "\u00a77Permissions: \u00a7f" + finalRole.getPermissions().size(),
                            "\u00a77Click to manage."))
                    .consumer(e -> plugin.getGUIManager().openGUI(
                            new BusinessRoleEditGUI(plugin, businessId, finalRole.getRoleId()),
                            (Player) e.getWhoClicked()))
            );
            slot++;
        }

        addButton(45, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.LIME_WOOL, "\u00a7aCreate New Role",
                        "\u00a77Click to create a new business role."))
                .consumer(e -> createRole(biz, (Player) e.getWhoClicked()))
        );

        addButton(49, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7cBack"))
                .consumer(e -> plugin.getGUIManager().openGUI(
                        new BusinessManagementGUI(plugin, businessId), (Player) e.getWhoClicked()))
        );

        super.decorate(player);
    }

    private void createRole(BusinessData biz, Player player) {
        player.closeInventory();
        plugin.getChatInputManager().requestInput(player, "\u00a7eEnter the name for the new role:", name -> {
            if (name.trim().isEmpty()) {
                player.sendMessage("\u00a7cRole name cannot be empty.");
                return;
            }
            BusinessRole role = new BusinessRole();
            role.setRoleId(UUID.randomUUID());
            role.setName(name.trim());
            biz.getRoles().add(role);
            plugin.getBusinessManager().saveBusiness(biz);
            player.sendMessage("\u00a7aRole '\u00a7f" + name.trim() + "\u00a7a' created!");
            plugin.getServer().getScheduler().runTask(plugin,
                    () -> plugin.getGUIManager().openGUI(new BusinessRolesGUI(plugin, businessId), player));
        });
    }
}
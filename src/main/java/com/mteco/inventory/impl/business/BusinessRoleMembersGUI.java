package com.mteco.inventory.impl.business;

import com.cryptomorin.xseries.XMaterial;
import com.mteco.MTeco;
import com.mteco.data.BusinessData;
import com.mteco.data.BusinessRole;
import com.mteco.data.CharacterData;
import com.mteco.inventory.InventoryButton;
import com.mteco.inventory.InventoryGUI;
import com.mteco.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BusinessRoleMembersGUI extends InventoryGUI {
    private final MTeco plugin;
    private final UUID businessId;
    private final UUID roleId;

    public BusinessRoleMembersGUI(MTeco plugin, UUID businessId, UUID roleId) {
        this.plugin = plugin;
        this.businessId = businessId;
        this.roleId = roleId;
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 54, "\u00a76Role Members");
    }

    @Override
    public void decorate(Player player) {
        fillPagedGui(54, XMaterial.LIME_STAINED_GLASS_PANE);

        BusinessData biz = plugin.getBusinessManager().loadBusiness(businessId);
        if (biz == null) { super.decorate(player); return; }

        BusinessRole role = null;
        for (BusinessRole r : biz.getRoles()) {
            if (r.getRoleId().equals(roleId)) { role = r; break; }
        }
        if (role == null) { super.decorate(player); return; }

        int slot = 0;
        for (UUID memberUuid : new ArrayList<>(role.getMemberUUIDs())) {
            if (slot >= 36) break;
            CharacterData ch = plugin.getCharacterManager().getCharacter(memberUuid);
            String name = ch != null ? ch.getFirstName() + " " + ch.getLastName() : memberUuid.toString().substring(0, 8);
            final String finalName = name;
            final UUID fMember = memberUuid;
            final BusinessRole fRole = role;
            addButton(slot, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.PLAYER_HEAD, "\u00a7f" + finalName,
                            "\u00a77Click to remove from role."))
                    .consumer(e -> {
                        fRole.getMemberUUIDs().remove(fMember);
                        plugin.getBusinessManager().saveBusiness(biz);
                        ((Player) e.getWhoClicked()).sendMessage("\u00a7c" + finalName + " removed from role.");
                        plugin.getGUIManager().openGUI(
                                new BusinessRoleMembersGUI(plugin, businessId, roleId),
                                (Player) e.getWhoClicked());
                    })
            );
            slot++;
        }

        final BusinessRole finalRole = role;
        addButton(45, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.LIME_WOOL, "\u00a7aAdd Member",
                        "\u00a77Select an employee to add."))
                .consumer(e -> {
                    List<UUID> available = new ArrayList<>();
                    for (UUID emp : biz.getEmployeeUUIDs()) {
                        if (!finalRole.getMemberUUIDs().contains(emp)) available.add(emp);
                    }
                    if (available.isEmpty()) {
                        ((Player) e.getWhoClicked()).sendMessage("\u00a7cNo employees available to add.");
                        return;
                    }
                    List<CharacterData> candidates = new ArrayList<>();
                    for (UUID uid : available) {
                        CharacterData ch = plugin.getCharacterManager().getCharacter(uid);
                        if (ch != null) candidates.add(ch);
                    }
                    plugin.getGUIManager().openGUI(
                            new com.mteco.inventory.impl.nation.NationCharacterSelectGUI(plugin,
                                    "\u00a7aSelect Employee", candidates,
                                    target -> {
                                        finalRole.getMemberUUIDs().add(target.getPlayerUuid());
                                        plugin.getBusinessManager().saveBusiness(biz);
                                        Player p = (Player) e.getWhoClicked();
                                        p.sendMessage("\u00a7a" + target.getFirstName() + " " + target.getLastName() + " added to role.");
                                        plugin.getServer().getScheduler().runTask(plugin,
                                                () -> plugin.getGUIManager().openGUI(
                                                        new BusinessRoleMembersGUI(plugin, businessId, roleId), p));
                                    },
                                    new BusinessRoleMembersGUI(plugin, businessId, roleId), 0),
                            (Player) e.getWhoClicked());
                })
        );

        addButton(49, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7cBack"))
                .consumer(e -> plugin.getGUIManager().openGUI(
                        new BusinessRoleEditGUI(plugin, businessId, roleId),
                        (Player) e.getWhoClicked()))
        );

        super.decorate(player);
    }
}
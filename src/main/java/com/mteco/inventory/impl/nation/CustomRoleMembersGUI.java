package com.mteco.inventory.impl.nation;

import com.cryptomorin.xseries.XMaterial;
import com.mteco.MTeco;
import com.mteco.data.CharacterData;
import com.mteco.data.CustomRole;
import com.mteco.data.NationData;
import com.mteco.inventory.InventoryButton;
import com.mteco.inventory.InventoryGUI;
import com.mteco.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class CustomRoleMembersGUI extends InventoryGUI {
    private final MTeco plugin;
    private final UUID nationId;
    private final UUID roleId;
    private final int page;

    public CustomRoleMembersGUI(MTeco plugin, UUID nationId, UUID roleId, int page) {
        this.plugin = plugin;
        this.nationId = nationId;
        this.roleId = roleId;
        this.page = page;
    }

    @Override
    protected Inventory createInventory() {
        CustomRole role = findRole();
        String title = role != null ? role.getColorCode() + role.getName() + " \u00a78Members" : "\u00a78Role Members";
        return Bukkit.createInventory(null, 54, title);
    }

    @Override
    public void decorate(Player player) {
        fillGlass(54, XMaterial.BLUE_STAINED_GLASS_PANE);

        CustomRole role = findRole();
        NationData nation = plugin.getNationManager().loadNation(nationId);
        if (role == null || nation == null) return;

        List<UUID> members = role.getMemberUUIDs();
        int perPage = 45;
        int start = page * perPage;
        int end = Math.min(start + perPage, members.size());

        for (int i = start; i < end; i++) {
            UUID memberUUID = members.get(i);
            CharacterData c = plugin.getCharacterManager().getCharacter(memberUUID);
            int slot = i - start;
            addButton(slot, new InventoryButton()
                    .creator(p -> {
                        String name = c != null ? c.getFirstName() + " " + c.getLastName() : "Unknown";
                        return ItemUtil.buildItem(XMaterial.PLAYER_HEAD, "\u00a7e" + name,
                                "\u00a7cClick to remove from role");
                    })
                    .consumer(e -> {
                        CustomRole r = findRole();
                        if (r == null) return;
                        r.getMemberUUIDs().remove(memberUUID);
                        NationData n = plugin.getNationManager().loadNation(nationId);
                        if (n != null) plugin.getNationManager().saveNation(n);
                        Player p = (Player) e.getWhoClicked();
                        CharacterData ch = plugin.getCharacterManager().getCharacter(memberUUID);
                        String removedName = ch != null ? ch.getFirstName() + " " + ch.getLastName() : "Unknown";
                        p.sendMessage("\u00a7c" + removedName + " \u00a77removed from role.");
                        plugin.getGUIManager().openGUI(new CustomRoleMembersGUI(plugin, nationId, roleId, page), p);
                    })
            );
        }

        if (page > 0) {
            addButton(45, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7ePrevious Page"))
                    .consumer(e -> plugin.getGUIManager().openGUI(
                            new CustomRoleMembersGUI(plugin, nationId, roleId, page - 1), (Player) e.getWhoClicked()))
            );
        }
        if (end < members.size()) {
            addButton(53, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7eNext Page"))
                    .consumer(e -> plugin.getGUIManager().openGUI(
                            new CustomRoleMembersGUI(plugin, nationId, roleId, page + 1), (Player) e.getWhoClicked()))
            );
        }

        addButton(49, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.EMERALD, "\u00a7aAdd Member",
                        "\u00a77Select a nation member to add to this role."))
                .consumer(e -> {
                    Player p = (Player) e.getWhoClicked();
                    CustomRole r = findRole();
                    if (r == null) return;
                    List<CharacterData> candidates = nation.getMemberUUIDs().stream()
                            .filter(u -> !u.equals(nation.getPresidentUUID()))
                            .filter(u -> !r.getMemberUUIDs().contains(u))
                            .map(u -> plugin.getCharacterManager().getCharacter(u))
                            .filter(ch -> ch != null)
                            .collect(Collectors.toList());
                    if (candidates.isEmpty()) {
                        p.sendMessage("\u00a7cNo eligible members to add.");
                        return;
                    }
                    plugin.getGUIManager().openGUI(new NationCharacterSelectGUI(plugin,
                            "\u00a7eAdd to " + r.getColorCode() + r.getName(), candidates, selected -> {
                        CustomRole fresh = findRole();
                        if (fresh == null) return;
                        if (fresh.getMemberUUIDs().contains(selected.getPlayerUuid())) {
                            p.sendMessage("\u00a7cAlready in this role.");
                            return;
                        }
                        fresh.getMemberUUIDs().add(selected.getPlayerUuid());
                        NationData nf = plugin.getNationManager().loadNation(nationId);
                        if (nf != null) plugin.getNationManager().saveNation(nf);
                        p.sendMessage("\u00a7a" + selected.getFirstName() + " " + selected.getLastName() +
                                " \u00a7aadded to " + fresh.getColorCode() + fresh.getName() + "\u00a7a.");
                        plugin.getGUIManager().openGUI(new CustomRoleMembersGUI(plugin, nationId, roleId, 0), p);
                    }, new CustomRoleMembersGUI(plugin, nationId, roleId, page), 0), p);
                })
        );

        addButton(46, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7cBack"))
                .consumer(e -> plugin.getGUIManager().openGUI(
                        new CustomRoleEditGUI(plugin, nationId, roleId), (Player) e.getWhoClicked()))
        );

        super.decorate(player);
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
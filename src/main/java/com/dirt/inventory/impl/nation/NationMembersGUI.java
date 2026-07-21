package com.dirt.inventory.impl.nation;

import com.cryptomorin.xseries.XMaterial;
import com.dirt.DirtEconomy;
import com.dirt.data.CharacterData;
import com.dirt.data.MailItem;
import com.dirt.data.NationData;
import com.dirt.data.NationPermission;
import com.dirt.inventory.InventoryButton;
import com.dirt.inventory.InventoryGUI;
import com.dirt.util.ItemUtil;
import com.dirt.util.NationPermissionUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class NationMembersGUI extends InventoryGUI {
    private final DirtEconomy plugin;
    private final UUID nationId;
    private final int page;

    public NationMembersGUI(DirtEconomy plugin, UUID nationId, int page) {
        this.plugin = plugin;
        this.nationId = nationId;
        this.page = page;
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 54, "§aNation Members");
    }

    @Override
    public void decorate(Player player) {
        fillPagedGui(54, XMaterial.BLUE_STAINED_GLASS_PANE);

        NationData nation = plugin.getNationManager().loadNation(nationId);
        List<UUID> members = nation != null ? nation.getMemberUUIDs() : List.of();
        boolean isPresident = nation != null && player.getUniqueId().equals(nation.getPresidentUUID());
        boolean canManageMembers = nation != null && NationPermissionUtil.hasPermission(plugin, nation, player.getUniqueId(), NationPermission.MANAGE_MEMBERS);

        int perPage = 45;
        int start = page * perPage;
        int end = Math.min(start + perPage, members.size());

        for (int i = start; i < end; i++) {
            UUID memberUUID = members.get(i);
            CharacterData c = plugin.getCharacterManager().getCharacter(memberUUID);
            int slot = i - start;

            String role = nation != null
                    ? NationPermissionUtil.getRoleDisplayName(plugin, nation, memberUUID)
                    : "§7Member";

            String finalRole = role;
            addButton(slot, new InventoryButton()
                    .creator(p -> {
                        if (c == null) return ItemUtil.buildItem(XMaterial.PLAYER_HEAD, "§cUnknown Member",
                                "§7Role: §7Member", "", "§eClick to view details");
                        return ItemUtil.buildItem(XMaterial.PLAYER_HEAD, "§e" + c.getFirstName() + " " + c.getLastName(),
                                "§7Gender: §f" + c.getGender(),
                                "§7Role: " + finalRole,
                                "",
                                "§eClick to view details");
                    })
                    .consumer(e -> plugin.getGUIManager().openGUI(
                            new NationMemberDetailGUI(plugin, nationId, memberUUID, page), (Player) e.getWhoClicked()))
            );
        }

        if (page > 0) {
            int prev = page - 1;
            addButton(45, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "§ePrevious Page"))
                    .consumer(e -> plugin.getGUIManager().openGUI(new NationMembersGUI(plugin, nationId, prev), (Player) e.getWhoClicked()))
            );
        }
        if (end < members.size()) {
            int next = page + 1;
            addButton(53, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "§eNext Page"))
                    .consumer(e -> plugin.getGUIManager().openGUI(new NationMembersGUI(plugin, nationId, next), (Player) e.getWhoClicked()))
            );
        }

        if (canManageMembers) {
            List<CharacterData> nonMembers = plugin.getCharacterManager().getAllCharacters().stream()
                    .filter(ch -> nation == null || !nation.getMemberUUIDs().contains(ch.getPlayerUuid()))
                    .collect(Collectors.toList());

            addButton(48, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.WRITABLE_BOOK, "§aSend Nation Invite",
                            "§7Invite a character to join this nation."))
                    .consumer(e -> {
                        Player p = (Player) e.getWhoClicked();
                        CharacterData presChar = plugin.getCharacterManager().getCharacter(p.getUniqueId());
                        plugin.getGUIManager().openGUI(new NationCharacterSelectGUI(plugin, "§eInvite to Nation", nonMembers, target -> {
                            NationData n = plugin.getNationManager().loadNation(nationId);
                            if (n == null) return;
                            MailItem invite = new MailItem();
                            invite.setId(UUID.randomUUID().toString());
                            invite.setType("NATION_INVITE");
                            invite.setFromPlayerUuid(p.getUniqueId());
                            invite.setTimestamp(System.currentTimeMillis());
                            Map<String, String> data = new HashMap<>();
                            data.put("nationId", nationId.toString());
                            data.put("nationName", n.getName());
                            data.put("inviterName", presChar != null ? presChar.getFirstName() + " " + presChar.getLastName() : p.getName());
                            invite.setData(data);
                            plugin.getCharacterManager().addMailItem(target.getPlayerUuid(), invite);
                            p.sendMessage("§aNation invite sent to §e" + target.getFirstName() + " " + target.getLastName() + "§a.");

                            Player targetPlayer = Bukkit.getPlayer(target.getPlayerUuid());
                            if (targetPlayer != null) targetPlayer.sendMessage("fffffc7eYou have received a nation invite from fffffc7f" + (presChar != null ? presChar.getFirstName() : p.getName()) + "fffffc7e. Check your mail (/dc).");
                            plugin.getGUIManager().openGUI(new NationMembersGUI(plugin, nationId, 0), p);
                        }, new NationMembersGUI(plugin, nationId, page), 0), p);
                    })
            );

            int requestCount = nation != null ? nation.getJoinRequestUUIDs().size() : 0;
            addButton(50, new InventoryButton()
                    .creator(p -> {
                        if (requestCount > 0) {
                            return ItemUtil.buildItem(XMaterial.BELL, "§6Join Requests §7(" + requestCount + ")",
                                    "§7Review players who want to join.",
                                    "§eClick to view requests.");
                        }
                        return ItemUtil.buildItem(XMaterial.BELL, "§6Join Requests",
                                "§7No pending requests.");
                    })
                    .consumer(e -> plugin.getGUIManager().openGUI(new NationJoinRequestsGUI(plugin, nationId, 0), (Player) e.getWhoClicked()))
            );
        }

        addButton(46, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "§cBack"))
                .consumer(e -> plugin.getGUIManager().openGUI(new MemberManagementGUI(plugin, nationId), (Player) e.getWhoClicked()))
        );

        super.decorate(player);
    }
}
package com.dirt.inventory.impl.nation;

import com.cryptomorin.xseries.XMaterial;
import com.dirt.DirtEconomy;
import com.dirt.data.CharacterData;
import com.dirt.data.NationData;
import com.dirt.data.NationPermission;
import com.dirt.inventory.InventoryButton;
import com.dirt.inventory.InventoryGUI;
import com.dirt.util.ItemUtil;
import com.dirt.util.NationPermissionUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

public class NationMemberDetailGUI extends InventoryGUI {
    private final DirtEconomy plugin;
    private final UUID nationId;
    private final UUID memberUUID;
    private final int returnPage;

    public NationMemberDetailGUI(DirtEconomy plugin, UUID nationId, UUID memberUUID, int returnPage) {
        this.plugin = plugin;
        this.nationId = nationId;
        this.memberUUID = memberUUID;
        this.returnPage = returnPage;
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 27, "§eMember Details");
    }

    @Override
    public void decorate(Player player) {
        fillGlass(27, XMaterial.BLUE_STAINED_GLASS_PANE);

        NationData nation = plugin.getNationManager().loadNation(nationId);
        CharacterData c = plugin.getCharacterManager().getCharacter(memberUUID);

        String name = c != null ? c.getFirstName() + " " + c.getLastName() : "Unknown";
        String gender = c != null ? c.getGender() : "Unknown";
        String birthStr = c != null ? new SimpleDateFormat("MM/dd/yyyy").format(new Date(c.getBirthDate())) : "Unknown";

        String role = nation != null
                ? NationPermissionUtil.getRoleDisplayName(plugin, nation, memberUUID)
                : "§7Member";
        if (nation != null && (role.equals("§7Member") || role.equals("§7Citizen"))) {
            for (UUID rId : nation.getRegionIds()) {
                var region = plugin.getNationManager().loadRegion(rId);
                if (region != null && memberUUID.equals(region.getGovernorUUID())) {
                    role = "§aRegion Governor (" + region.getName() + ")";
                    break;
                }
            }
        }

        String finalRole = role;
        addButton(13, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.PLAYER_HEAD, "§e" + name,
                        "§7Gender: §f" + gender,
                        "§7Born: §f" + birthStr,
                        "§7Role: " + finalRole))
                .consumer(e -> {})
        );

        boolean canManageMembers = nation != null && NationPermissionUtil.hasPermission(plugin, nation, player.getUniqueId(), NationPermission.MANAGE_MEMBERS);
        boolean targetIsPresident = nation != null && memberUUID.equals(nation.getPresidentUUID());

        if ((canManageMembers || player.isOp()) && !targetIsPresident) {
            addButton(15, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.RED_DYE, "§cKick from Nation",
                            "§7Remove this member from the nation.",
                            "§cClick to confirm."))
                    .consumer(e -> {
                        Player p = (Player) e.getWhoClicked();
                        NationData n = plugin.getNationManager().loadNation(nationId);
                        if (n == null) return;
                        if (!NationPermissionUtil.hasPermission(plugin, n, p.getUniqueId(), NationPermission.MANAGE_MEMBERS) && !p.isOp()) {
                            p.sendMessage("§cOnly the President or an OP can kick members.");
                            return;
                        }
                        if (!n.getMemberUUIDs().contains(memberUUID)) {
                            p.sendMessage("§cThis player is no longer a member.");
                            plugin.getGUIManager().openGUI(new NationMembersGUI(plugin, nationId, returnPage), p);
                            return;
                        }
                        CharacterData kicked = plugin.getCharacterManager().getCharacter(memberUUID);
                        String kickedName = kicked != null ? kicked.getFirstName() + " " + kicked.getLastName() : "Unknown";
                        plugin.getNationManager().removeMember(n, memberUUID);
                        p.sendMessage("§c" + kickedName + " §7has been kicked from §e" + n.getName() + "§7.");
                        Player kickedPlayer = Bukkit.getPlayer(memberUUID);
                        if (kickedPlayer != null) {
                            kickedPlayer.sendMessage("§cYou have been kicked from " + n.getColor1() + n.getName() + "§c.");
                        }
                        plugin.getGUIManager().openGUI(new NationMembersGUI(plugin, nationId, 0), p);
                    })
            );
        }

        addButton(26, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "§cBack"))
                .consumer(e -> plugin.getGUIManager().openGUI(new NationMembersGUI(plugin, nationId, returnPage), (Player) e.getWhoClicked()))
        );

        super.decorate(player);
    }
}
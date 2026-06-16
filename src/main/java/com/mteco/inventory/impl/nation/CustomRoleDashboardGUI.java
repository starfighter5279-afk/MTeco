package com.mteco.inventory.impl.nation;

import com.cryptomorin.xseries.XMaterial;
import com.mteco.MTeco;
import com.mteco.data.CharacterData;
import com.mteco.data.CouncilVote;
import com.mteco.data.CustomRole;
import com.mteco.data.NationData;
import com.mteco.data.NationPermission;
import com.mteco.data.RoleMode;
import com.mteco.inventory.InventoryButton;
import com.mteco.inventory.InventoryGUI;
import com.mteco.util.CurrencyUtil;
import com.mteco.util.ItemUtil;
import com.mteco.util.NationPermissionUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

public class CustomRoleDashboardGUI extends InventoryGUI {
    private final MTeco plugin;
    private final UUID nationId;

    public CustomRoleDashboardGUI(MTeco plugin, UUID nationId) {
        this.plugin = plugin;
        this.nationId = nationId;
    }

    @Override
    protected Inventory createInventory() {
        NationData nation = plugin.getNationManager().loadNation(nationId);
        String name = nation != null ? nation.getColor1() + nation.getName() : "\u00a7eNation";
        return Bukkit.createInventory(null, 54, name + " \u00a78Dashboard");
    }

    @Override
    public void decorate(Player player) {
        fillGlass(54, XMaterial.BLUE_STAINED_GLASS_PANE);

        NationData nation = plugin.getNationManager().loadNation(nationId);
        if (nation == null) return;

        Set<NationPermission> perms = NationPermissionUtil.getPlayerPermissions(plugin, nation, player.getUniqueId());
        boolean needsVote = NationPermissionUtil.requiresCouncilVote(plugin, nation, player.getUniqueId());

        if (perms.contains(NationPermission.MANAGE_MEMBERS)) {
            addButton(10, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.PLAYER_HEAD, "\u00a7eManage Members",
                            "\u00a77View, invite, and manage nation members."))
                    .consumer(e -> plugin.getGUIManager().openGUI(
                            new NationMembersGUI(plugin, nationId, 0), (Player) e.getWhoClicked()))
            );
        }

        if (perms.contains(NationPermission.MANAGE_REGIONS)) {
            addButton(11, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.MAP, "\u00a7eManage Regions",
                            "\u00a77Create and manage regions."))
                    .consumer(e -> plugin.getGUIManager().openGUI(
                            new RegionsGUI(plugin, nationId), (Player) e.getWhoClicked()))
            );
        }

        if (perms.contains(NationPermission.MANAGE_TAX)) {
            addButton(12, new InventoryButton()
                    .creator(p -> {
                        double rate = nation.getTaxRate();
                        return ItemUtil.buildItem(XMaterial.GOLD_NUGGET, "\u00a7eManage Tax",
                                "\u00a77Current rate: \u00a7a" + String.format("%.1f", rate) + "%");
                    })
                    .consumer(e -> plugin.getGUIManager().openGUI(
                            new NationwideTaxGUI(plugin, nationId), (Player) e.getWhoClicked()))
            );
        }

        if (perms.contains(NationPermission.MANAGE_TREASURY)) {
            addButton(13, new InventoryButton()
                    .creator(p -> {
                        double bal = plugin.getNationManager().getTreasuryBalance(nation.getName());
                        return ItemUtil.buildItem(XMaterial.GOLD_INGOT, "\u00a76National Treasury",
                                "\u00a77Balance: \u00a7a" + CurrencyUtil.symbol() + String.format("%.2f", bal));
                    })
                    .consumer(e -> plugin.getGUIManager().openGUI(
                            new NationTreasuryGUI(plugin, nationId, true), (Player) e.getWhoClicked()))
            );
        }

        if (perms.contains(NationPermission.MANAGE_WAR)) {
            addButton(14, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.DIAMOND_SWORD, "\u00a7cWar",
                            "\u00a77Declare and manage wars."))
                    .consumer(e -> plugin.getGUIManager().openGUI(
                            new NationWarGUI(plugin, nationId), (Player) e.getWhoClicked()))
            );
        }

        if (perms.contains(NationPermission.MANAGE_PROPERTIES)) {
            addButton(15, new InventoryButton()
                    .creator(p -> {
                        int count = nation.getGovernmentPropertyIds().size();
                        return ItemUtil.buildItem(XMaterial.DARK_OAK_DOOR, "\u00a7eGovernment Properties",
                                "\u00a77Properties: \u00a7f" + count);
                    })
                    .consumer(e -> plugin.getGUIManager().openGUI(
                            new GovernmentPropertiesGUI(plugin, nationId, 0), (Player) e.getWhoClicked()))
            );
        }

        if (perms.contains(NationPermission.MANAGE_ROLES)) {
            addButton(16, new InventoryButton()
                    .creator(p -> {
                        int roleCount = nation.getCustomRoles().size();
                        return ItemUtil.buildItem(XMaterial.NAME_TAG, "\u00a7bManage Roles",
                                "\u00a77Roles: \u00a7f" + roleCount);
                    })
                    .consumer(e -> plugin.getGUIManager().openGUI(
                            new CustomRolesGUI(plugin, nationId), (Player) e.getWhoClicked()))
            );
        }

        if (perms.contains(NationPermission.MANAGE_MINTERS) && plugin.getSettings().isMintersEnabled()) {
            addButton(19, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.FURNACE, "\u00a7eMinters",
                            "\u00a77Create and manage minter stations.",
                            "\u00a7eClick to place a minter."))
                    .consumer(e -> {
                        Player p = (Player) e.getWhoClicked();
                        p.closeInventory();
                        plugin.getLocationSelectionManager().requestLocation(p,
                                "\u00a7eRight-click a block to place the minter base.", loc -> {
                                    plugin.getMinterManager().createMinterAtLocation(p, nationId, loc);
                                });
                    })
            );
        }

        if (perms.contains(NationPermission.MANAGE_ENFORCERS) && plugin.getSettings().isEnforcersEnabled()) {
            Supplier<InventoryGUI> backToDash = () -> new CustomRoleDashboardGUI(plugin, nationId);
            addButton(20, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.IRON_CHESTPLATE, "\u00a7eEnforcers",
                            "\u00a77Hire and manage enforcers."))
                    .consumer(e -> plugin.getGUIManager().openGUI(
                            new EnforcerManagementGUI(plugin, nationId, backToDash, 0), (Player) e.getWhoClicked()))
            );
        }

        if (perms.contains(NationPermission.MANAGE_LAWS) && plugin.getSettings().isLawsEnabled()) {
            addButton(21, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.WRITABLE_BOOK, "\u00a7eLaws",
                            "\u00a77Create and manage laws."))
                    .consumer(e -> {
                        boolean isPresident = player.getUniqueId().equals(nation.getPresidentUUID());
                        plugin.getGUIManager().openGUI(
                                new LawsGUI(plugin, nationId, isPresident), (Player) e.getWhoClicked());
                    })
            );
        }

        if (perms.contains(NationPermission.MANAGE_CRIME) && plugin.getSettings().isCrimeEnabled()) {
            Supplier<InventoryGUI> backToDash = () -> new CustomRoleDashboardGUI(plugin, nationId);
            addButton(22, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.IRON_BARS, "\u00a7eCrime Management",
                            "\u00a77Manage criminals and convictions."))
                    .consumer(e -> plugin.getGUIManager().openGUI(
                            new CrimeManagementGUI(plugin, nationId, backToDash), (Player) e.getWhoClicked()))
            );
        }

        if (perms.contains(NationPermission.MANAGE_JAILS) && plugin.getSettings().isCrimeEnabled()) {
            addButton(23, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.IRON_BARS, "\u00a7eJail Management",
                            "\u00a77Create and manage jails and cells.",
                            "\u00a7eClick to create a jail."))
                    .consumer(e -> {
                        Player p = (Player) e.getWhoClicked();
                        p.closeInventory();
                        if (plugin.getChunkSelectionManager().hasSession(p.getUniqueId())) {
                            p.sendMessage("\u00a7cYou already have an active selection session.");
                            return;
                        }
                        plugin.getChatInputManager().requestInput(p, "\u00a7eEnter a name for the new jail:", jailName -> {
                            if (jailName.isEmpty()) { p.sendMessage("\u00a7cJail name cannot be empty."); return; }
                            plugin.getChunkSelectionManager().startJailCreation(p, nationId, jailName);
                        });
                    })
            );
        }

        if (perms.contains(NationPermission.MANAGE_ANNOUNCEMENTS)) {
            addButton(24, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.BELL, "\u00a7eAnnouncements",
                            "\u00a77Broadcast a message to all members.",
                            "\u00a7eClick to write an announcement."))
                    .consumer(e -> {
                        Player p = (Player) e.getWhoClicked();
                        p.closeInventory();
                        plugin.getChatInputManager().requestInput(p, "\u00a7eType your announcement message:", msg -> {
                            NationData n = plugin.getNationManager().loadNation(nationId);
                            if (n == null) return;
                            CharacterData senderChar = plugin.getCharacterManager().getCharacter(p.getUniqueId());
                            String senderName = senderChar != null ? senderChar.getFirstName() + " " + senderChar.getLastName() : p.getName();
                            Runnable sendAnnouncement = () -> {
                                String header = n.getColor1() + "\u00a7l[" + n.getName() + " Announcement]";
                                for (UUID memberUUID : n.getMemberUUIDs()) {
                                    Player member = Bukkit.getPlayer(memberUUID);
                                    if (member != null && member.isOnline()) {
                                        member.sendMessage("");
                                        member.sendMessage(header);
                                        member.sendMessage("\u00a7f" + msg);
                                        member.sendMessage("\u00a78\u2014 " + senderName);
                                        member.sendMessage("");
                                    }
                                }
                                p.sendMessage("\u00a7aAnnouncement sent to \u00a7e" + n.getMemberUUIDs().size() + "\u00a7a members.");
                            };
                            NationPermissionUtil.executeOrVote(plugin, n, p.getUniqueId(),
                                    NationPermission.MANAGE_ANNOUNCEMENTS, "Send announcement: " + msg, sendAnnouncement);
                        });
                    })
            );
        }

        if (perms.contains(NationPermission.MANAGE_CONSERVATION)) {
            addButton(25, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.OAK_SAPLING, "\u00a7eConservation Areas",
                            "\u00a77Create and manage conservation areas.",
                            "\u00a7eClick to start selection."))
                    .consumer(e -> {
                        Player p = (Player) e.getWhoClicked();
                        p.closeInventory();
                        if (plugin.getChunkSelectionManager().hasSession(p.getUniqueId())) {
                            p.sendMessage("\u00a7cYou already have an active selection session.");
                            return;
                        }
                        plugin.getChunkSelectionManager().startConservationSelection(p, nationId);
                    })
            );
        }

        List<CouncilVote> pendingVotes = plugin.getCouncilVoteManager().getVotesForNation(nationId);
        boolean isCouncilMember = false;
        for (CustomRole role : nation.getCustomRoles()) {
            if (role.getRoleMode() == RoleMode.COUNCIL && role.getMemberUUIDs().contains(player.getUniqueId())) {
                isCouncilMember = true;
                break;
            }
        }

        if (isCouncilMember || player.getUniqueId().equals(nation.getPresidentUUID())) {
            int voteCount = pendingVotes.size();
            addButton(34, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(
                            voteCount > 0 ? XMaterial.WRITABLE_BOOK : XMaterial.BOOK,
                            "\u00a76Council Votes" + (voteCount > 0 ? " \u00a7c(" + voteCount + " pending)" : ""),
                            "\u00a77View and vote on pending council actions.",
                            voteCount > 0 ? "\u00a7eClick to review votes." : "\u00a77No pending votes."))
                    .consumer(e -> plugin.getGUIManager().openGUI(
                            new CouncilVotesGUI(plugin, nationId, 0), (Player) e.getWhoClicked()))
            );
        }

        if (needsVote) {
            addButton(43, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.PAPER, "\u00a76\u00a7lCouncil Check Active",
                            "\u00a77Your government actions will be",
                            "\u00a77submitted for council approval",
                            "\u00a77before taking effect."))
                    .consumer(e -> {})
            );
        }

        addButton(46, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.OAK_DOOR, "\u00a7cLeave Nation",
                        "\u00a77Leave your nation.",
                        "\u00a7cClick to leave."))
                .consumer(e -> {
                    Player p = (Player) e.getWhoClicked();
                    if (p.getUniqueId().equals(nation.getPresidentUUID())) {
                        p.sendMessage("\u00a7cThe president cannot leave the nation.");
                        return;
                    }
                    plugin.getNationManager().removeMember(nation, p.getUniqueId());
                    CharacterData character = plugin.getCharacterManager().getCharacter(p.getUniqueId());
                    String charName = character != null ? character.getFirstName() + " " + character.getLastName() : p.getName();
                    p.sendMessage("\u00a7aYou have left " + nation.getColor1() + nation.getName() + "\u00a7a.");
                    Player president = Bukkit.getPlayer(nation.getPresidentUUID());
                    if (president != null) {
                        president.sendMessage("\u00a7e" + charName + "\u00a7e has left your nation.");
                    }
                    p.closeInventory();
                })
        );

        addButton(49, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.GRASS_BLOCK, "\u00a7aMy Properties",
                        "\u00a77View your owned properties."))
                .consumer(e -> plugin.getGUIManager().openGUI(
                        new CharacterPropertiesGUI(plugin, 0, ((Player) e.getWhoClicked()).getUniqueId()),
                        (Player) e.getWhoClicked()))
        );

        super.decorate(player);
    }
}
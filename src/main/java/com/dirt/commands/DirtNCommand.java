package com.dirt.commands;

import com.dirt.DirtEconomy;
import com.dirt.config.RolesConfig;
import com.dirt.data.CharacterData;
import com.dirt.data.NationData;
import com.dirt.data.NationPermission;
import com.dirt.data.RegionData;
import com.dirt.inventory.impl.nation.AdminNationGUI;
import com.dirt.inventory.impl.nation.CustomRoleDashboardGUI;
import com.dirt.inventory.impl.nation.CustomRolesGUI;
import com.dirt.inventory.impl.nation.NationCharacterSelectGUI;
import com.dirt.inventory.impl.nation.NationCitizenGUI;
import com.dirt.inventory.impl.nation.NationColorSelectGUI;
import com.dirt.inventory.impl.nation.NationManagementGUI;
import com.dirt.inventory.impl.nation.NationElitesGUI;
import com.dirt.inventory.impl.nation.NationSecurityGUI;
import com.dirt.inventory.impl.nation.VicePresidentManagementGUI;
import com.dirt.inventory.impl.nation.TreasurerGUI;
import com.dirt.inventory.impl.nation.NationPayGUI;
import com.dirt.inventory.impl.nation.CouncilVotesGUI;
import com.dirt.managers.NationManager;
import com.dirt.util.CurrencyUtil;
import com.dirt.util.NationPermissionUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class DirtNCommand implements CommandExecutor, TabCompleter {
    private final DirtEconomy plugin;

    public DirtNCommand(DirtEconomy plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return true;
        }
        if (!plugin.isDirtNationsEnabled()) {
            player.sendMessage("§cNations are not enabled on this server.");
            return true;
        }

        // /dirtn ns — National Security Head
        if (args.length >= 1 && args[0].equalsIgnoreCase("ns")) {
            if (!plugin.getRolesConfig().isSecurityHeadEnabled()) {
                player.sendMessage("§cThe National Security Head role is disabled on this server.");
                return true;
            }
            NationData secNation = findNationBySecurityHead(player.getUniqueId());
            if (secNation == null) {
                player.sendMessage("§cYou are not a National Security Head of any nation.");
                return true;
            }
            if (secNation.isCustomGovernment()) {
                player.sendMessage("§cUse §e/dn §cto open your nation dashboard.");
                return true;
            }
            plugin.getGUIManager().openGUI(new NationSecurityGUI(plugin, secNation.getNationId()), player);
            return true;
        }

        // /dirtn vp — Vice President
        if (args.length >= 1 && args[0].equalsIgnoreCase("vp")) {
            if (!plugin.getRolesConfig().isVicePresidentEnabled()) {
                player.sendMessage("§cThe Vice President role is disabled on this server.");
                return true;
            }
            NationData vpNation = findNationByVicePresident(player.getUniqueId());
            if (vpNation == null) {
                player.sendMessage("§cYou are not a Vice President of any nation.");
                return true;
            }
            if (vpNation.isCustomGovernment()) {
                player.sendMessage("§cUse §e/dn §cto open your nation dashboard.");
                return true;
            }
            plugin.getGUIManager().openGUI(new VicePresidentManagementGUI(plugin, vpNation.getNationId()), player);
            return true;
        }

        // /dirtn ct — Treasurer
        if (args.length >= 1 && args[0].equalsIgnoreCase("ct")) {
            if (!plugin.getRolesConfig().isTreasurerEnabled()) {
                player.sendMessage("§cThe Treasurer role is disabled on this server.");
                return true;
            }
            NationData treasurerNation = findNationByTreasurer(player.getUniqueId());
            if (treasurerNation == null) {
                player.sendMessage("§cYou are not a Treasurer of any nation.");
                return true;
            }
            if (treasurerNation.isCustomGovernment()) {
                player.sendMessage("§cUse §e/dn §cto open your nation dashboard.");
                return true;
            }
            plugin.getGUIManager().openGUI(new TreasurerGUI(plugin, treasurerNation.getNationId()), player);
            return true;
        }

        // /dirtn pay — President or Treasury permission
        if (args.length >= 1 && args[0].equalsIgnoreCase("pay")) {
            NationData payNation = findNationWithPermission(player.getUniqueId(), NationPermission.MANAGE_TREASURY);
            if (payNation == null) {
                player.sendMessage("§cYou need treasury management permissions to use this.");
                return true;
            }
            plugin.getGUIManager().openGUI(new NationPayGUI(plugin, payNation.getNationId()), player);
            return true;
        }

        // /dirtn claim biome
        if (args.length >= 2 && args[0].equalsIgnoreCase("claim") && args[1].equalsIgnoreCase("biome")) {
            handleClaimBiome(player);
            return true;
        }

        // /dirtn conserve
        if (args.length >= 1 && args[0].equalsIgnoreCase("conserve")) {
            handleConserve(player);
            return true;
        }

        // /dirtn minter
        if (args.length >= 1 && args[0].equalsIgnoreCase("minter")) {
            handleMinter(player);
            return true;
        }

        // /dirtn enforcers
        if (args.length >= 1 && args[0].equalsIgnoreCase("enforcers")) {
            handleEnforcers(player);
            return true;
        }

        // /dirtn crime
        if (args.length >= 1 && args[0].equalsIgnoreCase("crime")) {
            handleCrime(player);
            return true;
        }

        // /dirtn laws
        if (args.length >= 1 && args[0].equalsIgnoreCase("laws")) {
            handleLaws(player);
            return true;
        }

        // /dirtn votes
        if (args.length >= 1 && args[0].equalsIgnoreCase("votes")) {
            NationData voteNation = plugin.getNationManager().getNationByMember(player.getUniqueId());
            if (voteNation == null) {
                player.sendMessage("\u00a7cYou are not a member of any nation.");
                return true;
            }
            plugin.getGUIManager().openGUI(new CouncilVotesGUI(plugin, voteNation.getNationId(), 0), player);
            return true;
        }

        // /dirtn borders
        if (args.length >= 1 && args[0].equalsIgnoreCase("borders")) {
            boolean enabled = plugin.getBorderVisualizerManager().toggle(player);
            if (enabled) {
                player.sendMessage("§aRegion borders enabled. Run §6/dn borders§a again to turn off.");
            } else {
                player.sendMessage("§cRegion borders disabled.");
            }
            return true;
        }

        // /dirtn pborders
        if (args.length >= 1 && args[0].equalsIgnoreCase("pborders")) {
            boolean enabled = plugin.getBorderVisualizerManager().togglePBorders(player);
            if (enabled) {
                player.sendMessage("§aProperty borders enabled. Run §6/dn pborders§a again to turn off.");
            } else {
                player.sendMessage("§cProperty borders disabled.");
            }
            return true;
        }

        // /dirtn donate <amount>
        if (args.length >= 2 && args[0].equalsIgnoreCase("donate")) {
            handleDonate(player, args[1]);
            return true;
        }

        // /dirtn announce <message>
        if (args.length >= 2 && args[0].equalsIgnoreCase("announce")) {
            handleAnnounce(player, args);
            return true;
        }

        // /dirtn join <nation>
        if (args.length >= 2 && args[0].equalsIgnoreCase("join")) {
            handleJoinRequest(player, args);
            return true;
        }

        // /dirtn accept <player>
        if (args.length >= 2 && args[0].equalsIgnoreCase("accept")) {
            handleAcceptJoin(player, args[1]);
            return true;
        }

        // /dirtn deny <player>
        if (args.length >= 2 && args[0].equalsIgnoreCase("deny")) {
            handleDenyJoin(player, args[1]);
            return true;
        }

        // /dirtn requests
        if (args.length >= 1 && args[0].equalsIgnoreCase("requests")) {
            handleViewRequests(player);
            return true;
        }

        // /dirtn admin [define nation]
        if (args.length >= 1 && args[0].equalsIgnoreCase("admin")) {
            if (!player.hasPermission("DirtNations.admin")) {
                return true;
            }
            if (args.length >= 4 && args[1].equalsIgnoreCase("tp")) {
                handleAdminTeleport(player, args[2].replace('_', ' '), args[3].replace('_', ' '));
            } else if (args.length >= 3 && args[1].equalsIgnoreCase("define") && args[2].equalsIgnoreCase("nation")) {
                startNationCreation(player);
            } else {
                plugin.getGUIManager().openGUI(new AdminNationGUI(plugin, 0), player);
            }
            return true;
        }

        // /dirtn pcr <regionName> <price>
        if (args.length >= 3 && args[0].equalsIgnoreCase("pcr")) {
            handlePCR(player, args);
            return true;
        }

        // /dirtn — auto-detect role and open appropriate GUI
        UUID uuid = player.getUniqueId();
        RolesConfig rc = plugin.getRolesConfig();

        // Check president first
        NationData nation = plugin.getNationManager().getNationByPresident(uuid);
        if (nation != null) {
            if (!nation.isCustomGovernment() && rc.isElitesEnabled()) {
                if (!nation.isElitesConfigured()) {
                    boolean allFilled = true;
                    if (rc.isTreasurerEnabled() && nation.getTreasurerUUID() == null) allFilled = false;
                    if (rc.isVicePresidentEnabled() && nation.getVicePresidentUUID() == null) allFilled = false;
                    if (rc.isSecurityHeadEnabled() && nation.getSecurityHeadUUID() == null) allFilled = false;
                    if (allFilled) {
                        nation.setElitesConfigured(true);
                        plugin.getNationManager().saveNation(nation);
                    } else {
                        player.sendMessage("§eYou must assign Nation Elites before managing your nation.");
                        plugin.getGUIManager().openGUI(new NationElitesGUI(plugin, nation.getNationId()), player);
                        return true;
                    }
                }
            }
            plugin.getGUIManager().openGUI(new NationManagementGUI(plugin, nation.getNationId()), player);
            return true;
        }

        // Not president — check member status
        NationData memberNation = plugin.getNationManager().getNationByMember(uuid);
        if (memberNation != null) {
            if (memberNation.isCustomGovernment()) {
                Set<NationPermission> perms = NationPermissionUtil.getPlayerPermissions(plugin, memberNation, uuid);
                if (!perms.isEmpty()) {
                    plugin.getGUIManager().openGUI(new CustomRoleDashboardGUI(plugin, memberNation.getNationId()), player);
                } else {
                    plugin.getGUIManager().openGUI(new NationCitizenGUI(plugin, memberNation.getNationId()), player);
                }
            } else {
                if (rc.isVicePresidentEnabled() && uuid.equals(memberNation.getVicePresidentUUID())) {
                    plugin.getGUIManager().openGUI(new VicePresidentManagementGUI(plugin, memberNation.getNationId()), player);
                } else if (rc.isTreasurerEnabled() && uuid.equals(memberNation.getTreasurerUUID())) {
                    plugin.getGUIManager().openGUI(new TreasurerGUI(plugin, memberNation.getNationId()), player);
                } else if (rc.isSecurityHeadEnabled() && uuid.equals(memberNation.getSecurityHeadUUID())) {
                    plugin.getGUIManager().openGUI(new NationSecurityGUI(plugin, memberNation.getNationId()), player);
                } else {
                    plugin.getGUIManager().openGUI(new NationCitizenGUI(plugin, memberNation.getNationId()), player);
                }
            }
            return true;
        }

        player.sendMessage("§cYou are not a member of any nation.");
        return true;
    }

    // ---- Permission helper ----

    private NationData findNationWithPermission(UUID playerUUID, NationPermission permission) {
        NationData nation = plugin.getNationManager().getNationByMember(playerUUID);
        if (nation == null) return null;
        if (NationPermissionUtil.hasPermission(plugin, nation, playerUUID, permission)) return nation;
        return null;
    }

    private void handleAdminTeleport(Player player, String nationName, String regionName) {
        NationData nation = findNationByCommandName(nationName);
        if (nation == null) {
            player.sendMessage("§cNation §e" + nationName.replace('_', ' ') + "§c was not found.");
            return;
        }

        RegionData region = findRegionByCommandName(nation, regionName);
        if (region == null) {
            player.sendMessage("§cRegion §e" + regionName.replace('_', ' ') + "§c was not found in §6" + nation.getName() + "§c.");
            return;
        }
        if (region.getClaimedChunks().isEmpty()) {
            player.sendMessage("§cRegion §e" + region.getName() + "§c has no claimed chunks.");
            return;
        }

        String[] firstChunk = region.getClaimedChunks().get(0).split(",");
        if (firstChunk.length != 3) {
            player.sendMessage("§cRegion §e" + region.getName() + "§c has invalid claim data.");
            return;
        }
        World world = Bukkit.getWorld(firstChunk[0]);
        if (world == null) {
            player.sendMessage("§cThe region's world is not loaded.");
            return;
        }

        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (String chunkKey : region.getClaimedChunks()) {
            String[] parts = chunkKey.split(",");
            if (parts.length != 3 || !parts[0].equals(world.getName())) continue;
            try {
                int chunkX = Integer.parseInt(parts[1]);
                int chunkZ = Integer.parseInt(parts[2]);
                minX = Math.min(minX, chunkX * 16);
                maxX = Math.max(maxX, chunkX * 16 + 15);
                minZ = Math.min(minZ, chunkZ * 16);
                maxZ = Math.max(maxZ, chunkZ * 16 + 15);
            } catch (NumberFormatException ignored) {
            }
        }
        if (minX == Integer.MAX_VALUE) {
            player.sendMessage("§cRegion §e" + region.getName() + "§c has invalid claim data.");
            return;
        }

        int centerX = (minX + maxX) / 2;
        int centerZ = (minZ + maxZ) / 2;
        player.teleport(new Location(world, centerX + 0.5, world.getHighestBlockYAt(centerX, centerZ) + 1, centerZ + 0.5));
        player.sendMessage("§aTeleported to the center of §6" + region.getName() + "§a in §6" + nation.getName() + "§a.");
    }

    private NationData findNationByCommandName(String commandName) {
        for (NationData nation : plugin.getNationManager().getAllNations()) {
            if (commandName(nation.getName()).equalsIgnoreCase(commandName)) return nation;
        }
        return null;
    }

    private RegionData findRegionByCommandName(NationData nation, String commandName) {
        for (UUID regionId : nation.getRegionIds()) {
            RegionData region = plugin.getNationManager().loadRegion(regionId);
            if (region != null && commandName(region.getName()).equalsIgnoreCase(commandName)) return region;
        }
        return null;
    }

    private String commandName(String name) {
        return name.replace(' ', '_');
    }

    // ---- Announce ----

    private void handleAnnounce(Player player, String[] args) {
        NationData nation = findNationWithPermission(player.getUniqueId(), NationPermission.MANAGE_ANNOUNCEMENTS);
        if (nation == null) {
            player.sendMessage("§cYou need announcement permissions to use this.");
            return;
        }
        StringBuilder msg = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            if (i > 1) msg.append(" ");
            msg.append(args[i]);
        }
        String announcement = msg.toString();
        CharacterData senderChar = plugin.getCharacterManager().getCharacter(player.getUniqueId());
        String senderName = senderChar != null ? senderChar.getFirstName() + " " + senderChar.getLastName() : player.getName();

        String header = nation.getColor1() + "§l[" + nation.getName() + " Announcement]";
        String body = "§f" + announcement;
        String footer = "§8— " + senderName;

        for (UUID memberUUID : nation.getMemberUUIDs()) {
            Player member = Bukkit.getPlayer(memberUUID);
            if (member != null && member.isOnline()) {
                member.sendMessage("");
                member.sendMessage(header);
                member.sendMessage(body);
                member.sendMessage(footer);
                member.sendMessage("");
            }
        }
        player.sendMessage("§aAnnouncement sent to §e" + nation.getMemberUUIDs().size() + "§a nation members.");
    }

    // ---- Join Requests ----

    private void handleJoinRequest(Player player, String[] args) {
        NationData existing = plugin.getNationManager().getNationByMember(player.getUniqueId());
        if (existing != null) {
            player.sendMessage("§cYou are already a member of §e" + existing.getName() + "§c. Leave your current nation first.");
            return;
        }
        StringBuilder nameBuilder = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            if (i > 1) nameBuilder.append(" ");
            nameBuilder.append(args[i]);
        }
        String nationName = nameBuilder.toString();
        NationData target = null;
        for (NationData n : plugin.getNationManager().getAllNations()) {
            if (n.getName().equalsIgnoreCase(nationName)) {
                target = n;
                break;
            }
        }
        if (target == null) {
            player.sendMessage("§cNation '§e" + nationName + "§c' not found.");
            return;
        }
        if (target.getJoinRequestUUIDs().contains(player.getUniqueId())) {
            player.sendMessage("§cYou already have a pending request to join §e" + target.getName() + "§c.");
            return;
        }
        target.getJoinRequestUUIDs().add(player.getUniqueId());
        plugin.getNationManager().saveNation(target);
        player.sendMessage("§aJoin request sent to §e" + target.getName() + "§a! The leadership will review it.");

        Player president = Bukkit.getPlayer(target.getPresidentUUID());
        if (president != null) {
            CharacterData reqChar = plugin.getCharacterManager().getCharacter(player.getUniqueId());
            String reqName = reqChar != null ? reqChar.getFirstName() + " " + reqChar.getLastName() : player.getName();
            president.sendMessage("§e" + reqName + " §7has requested to join your nation. Use §e/dn requests§7 to view.");
        }
    }

    private void handleAcceptJoin(Player player, String targetName) {
        NationData nation = findNationWithPermission(player.getUniqueId(), NationPermission.MANAGE_MEMBERS);
        if (nation == null) {
            player.sendMessage("§cYou need member management permissions to accept requests.");
            return;
        }
        UUID targetUUID = resolvePlayerUUID(targetName);
        if (targetUUID == null) {
            player.sendMessage("§cPlayer '§e" + targetName + "§c' not found.");
            return;
        }
        if (!nation.getJoinRequestUUIDs().contains(targetUUID)) {
            player.sendMessage("§cThat player hasn't requested to join your nation.");
            return;
        }
        nation.getJoinRequestUUIDs().remove(targetUUID);
        nation.getMemberUUIDs().add(targetUUID);
        plugin.getNationManager().saveNation(nation);

        CharacterData acceptedChar = plugin.getCharacterManager().getCharacter(targetUUID);
        String acceptedName = acceptedChar != null ? acceptedChar.getFirstName() + " " + acceptedChar.getLastName() : targetName;
        player.sendMessage("§a" + acceptedName + " has been accepted into §e" + nation.getName() + "§a!");

        Player accepted = Bukkit.getPlayer(targetUUID);
        if (accepted != null) {
            accepted.sendMessage("§aYour request to join " + nation.getColor1() + nation.getName() + " §ahas been accepted!");
        }
    }

    private void handleDenyJoin(Player player, String targetName) {
        NationData nation = findNationWithPermission(player.getUniqueId(), NationPermission.MANAGE_MEMBERS);
        if (nation == null) {
            player.sendMessage("§cYou need member management permissions to deny requests.");
            return;
        }
        UUID targetUUID = resolvePlayerUUID(targetName);
        if (targetUUID == null) {
            player.sendMessage("§cPlayer '§e" + targetName + "§c' not found.");
            return;
        }
        if (!nation.getJoinRequestUUIDs().contains(targetUUID)) {
            player.sendMessage("§cThat player hasn't requested to join your nation.");
            return;
        }
        nation.getJoinRequestUUIDs().remove(targetUUID);
        plugin.getNationManager().saveNation(nation);
        player.sendMessage("§cJoin request denied.");

        Player denied = Bukkit.getPlayer(targetUUID);
        if (denied != null) {
            denied.sendMessage("§cYour request to join " + nation.getColor1() + nation.getName() + " §cwas denied.");
        }
    }

    private void handleViewRequests(Player player) {
        NationData nation = findNationWithPermission(player.getUniqueId(), NationPermission.MANAGE_MEMBERS);
        if (nation == null) {
            player.sendMessage("§cYou need member management permissions to view requests.");
            return;
        }
        List<UUID> requests = nation.getJoinRequestUUIDs();
        if (requests.isEmpty()) {
            player.sendMessage("§7No pending join requests.");
            return;
        }
        player.sendMessage("§6§l▬▬▬▬▬▬▬ Pending Join Requests ▬▬▬▬▬▬▬");
        for (UUID reqUUID : requests) {
            CharacterData reqChar = plugin.getCharacterManager().getCharacter(reqUUID);
            String name = reqChar != null ? reqChar.getFirstName() + " " + reqChar.getLastName() : Bukkit.getOfflinePlayer(reqUUID).getName();
            String playerName = Bukkit.getOfflinePlayer(reqUUID).getName();
            player.sendMessage("§e" + name + " §8(§7" + playerName + "§8)");
        }
        player.sendMessage("§7Use §e/dn accept <player> §7or §e/dn deny <player>");
        player.sendMessage("§6§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
    }

    private UUID resolvePlayerUUID(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) return online.getUniqueId();
        for (CharacterData c : plugin.getCharacterManager().getAllCharacters()) {
            String fullName = c.getFirstName() + " " + c.getLastName();
            if (fullName.equalsIgnoreCase(name) || (c.getFirstName() != null && c.getFirstName().equalsIgnoreCase(name))) {
                return c.getPlayerUuid();
            }
        }
        @SuppressWarnings("deprecation")
        org.bukkit.OfflinePlayer op = Bukkit.getOfflinePlayer(name);
        return op.hasPlayedBefore() ? op.getUniqueId() : null;
    }

    // ---- Existing Helpers ----

    private NationData findNationBySecurityHead(UUID uuid) {
        for (NationData n : plugin.getNationManager().getAllNations()) {
            if (uuid.equals(n.getSecurityHeadUUID())) return n;
        }
        return null;
    }

    private NationData findNationByVicePresident(UUID uuid) {
        for (NationData n : plugin.getNationManager().getAllNations()) {
            if (uuid.equals(n.getVicePresidentUUID())) return n;
        }
        return null;
    }

    private NationData findNationByTreasurer(UUID uuid) {
        for (NationData n : plugin.getNationManager().getAllNations()) {
            if (uuid.equals(n.getTreasurerUUID())) return n;
        }
        return null;
    }

    private void startNationCreation(Player admin) {
        plugin.getChatInputManager().requestInput(admin, "§eEnter the name for the new nation:", name -> {
            if (name.isEmpty()) { admin.sendMessage("§cNation name cannot be empty."); return; }
            plugin.getGUIManager().openGUI(
                new NationColorSelectGUI(plugin, name, null, (c1, c2) -> {
                    if (plugin.getRolesConfig().isCustomGovernmentEnabled()) {
                        plugin.getChatInputManager().requestInput(admin, "§eUse custom roles for this nation? §a(yes/no):", answer -> {
                            if (answer.equalsIgnoreCase("yes")) {
                                NationData nation = plugin.getNationManager().createNationCustom(name, c1, c2);
                                admin.sendMessage("§aNation §6" + name + "§a created with custom government! Opening role manager...");
                                plugin.getServer().getScheduler().runTaskLater(plugin, () ->
                                    plugin.getGUIManager().openGUI(new CustomRolesGUI(plugin, nation.getNationId()), admin), 5L);
                            } else {
                                selectPresidentAndFinish(admin, name, c1, c2);
                            }
                        });
                    } else {
                        selectPresidentAndFinish(admin, name, c1, c2);
                    }
                }),
            admin);
        });
    }

    private void selectPresidentAndFinish(Player admin, String name, String c1, String c2) {
        List<CharacterData> allChars = plugin.getCharacterManager().getAllCharacters();
        plugin.getGUIManager().openGUI(
            new NationCharacterSelectGUI(plugin, "§eSelect President", allChars, presChar -> {
                NationData existingNation = plugin.getNationManager().getNationByMember(presChar.getPlayerUuid());
                if (existingNation != null) {
                    admin.sendMessage("§c" + presChar.getFirstName() + " " + presChar.getLastName() + " is already a member of §e" + existingNation.getName() + "§c. They must leave that nation first.");
                    return;
                }
                NationData nation = plugin.getNationManager().createNation(name, c1, c2, presChar.getPlayerUuid());
                admin.sendMessage("§aNation §6" + name + "§a created! President: §e" + presChar.getFirstName() + " " + presChar.getLastName() + "§a.");
                Player presPlayer = plugin.getServer().getPlayer(presChar.getPlayerUuid());
                if (presPlayer != null) presPlayer.sendMessage("§eYou have been made President of " + c1 + name + "§e!");
            }, null, 0),
        admin);
    }

    private void handlePCR(Player player, String[] args) {
        String regionName = args[1];
        double price;
        try { price = Double.parseDouble(args[2]); } catch (NumberFormatException e) {
            player.sendMessage("§cInvalid price."); return;
        }
        if (price < 0) { player.sendMessage("§cPrice cannot be negative."); return; }

        NationData nation = plugin.getNationManager().getNationByPresident(player.getUniqueId());
        if (nation == null && !player.hasPermission("DirtNations.admin")) {
            player.sendMessage("§cOnly the nation's president can set the property chunk rate.");
            return;
        }

        NationData searchNation = nation != null ? nation : null;
        if (searchNation == null) {
            // Admin: search all nations
            RegionData found = findRegionByNameGlobal(regionName);
            if (found == null) { player.sendMessage("§cRegion '§e" + regionName + "§c' not found."); return; }
            found.setPropertyChunkRate(price);
            plugin.getNationManager().saveRegion(found);
            player.sendMessage("§aProperty chunk rate for §6" + found.getName() + "§a set to §e" + CurrencyUtil.symbol() + String.format("%.2f", price) + "§a per chunk.");
            return;
        }

        RegionData found = findRegionByName(searchNation, regionName);
        if (found == null) { player.sendMessage("§cRegion '§e" + regionName + "§c' not found in your nation."); return; }
        found.setPropertyChunkRate(price);
        plugin.getNationManager().saveRegion(found);
        player.sendMessage("§aProperty chunk rate for §6" + found.getName() + "§a set to §e" + CurrencyUtil.symbol() + String.format("%.2f", price) + "§a per chunk.");
    }

    private RegionData findRegionByName(NationData nation, String name) {
        for (java.util.UUID rid : nation.getRegionIds()) {
            RegionData r = plugin.getNationManager().loadRegion(rid);
            if (r != null && r.getName().equalsIgnoreCase(name)) return r;
        }
        return null;
    }

    private RegionData findRegionByNameGlobal(String name) {
        for (RegionData r : plugin.getNationManager().getAllRegions()) {
            if (r.getName().equalsIgnoreCase(name)) return r;
        }
        return null;
    }

    private void handleClaimBiome(Player player) {
        UUID regionId = plugin.getChunkSelectionManager().getTargetedRegion(player.getUniqueId());
        if (regionId == null) {
            player.sendMessage("§cYou have no targeted region. Use the Region Management GUI (/dr) to target a region first.");
            return;
        }
        RegionData region = plugin.getNationManager().loadRegion(regionId);
        if (region == null) { player.sendMessage("§cRegion not found."); return; }
        if (!player.getUniqueId().equals(region.getGovernorUUID())) {
            player.sendMessage("§cYou are not the governor of your targeted region.");
            return;
        }

        int maxRegionChunks = plugin.getSettings().getMaxRegionChunks();
        int available = maxRegionChunks - region.getClaimedChunks().size();
        if (available <= 0) {
            player.sendMessage("§cRegion §6" + region.getName() + "§c has reached the maximum of §e" + maxRegionChunks + "§c chunks.");
            return;
        }

        String worldName = player.getWorld().getName();
        int startCX = player.getLocation().getBlockX() >> 4;
        int startCZ = player.getLocation().getBlockZ() >> 4;
        int sampleY = player.getLocation().getBlockY();
        Biome targetBiome = player.getWorld().getBiome((startCX << 4) + 8, sampleY, (startCZ << 4) + 8);

        final int searchRadius = 64;
        Set<Long> visited = new HashSet<>();
        Deque<int[]> queue = new ArrayDeque<>();
        queue.add(new int[]{startCX, startCZ});
        visited.add(((long) startCX << 32) | (startCZ & 0xffffffffL));

        int added = 0;
        int hitLimit = 0;
        while (!queue.isEmpty()) {
            int[] c = queue.poll();
            int cx = c[0], cz = c[1];
            Biome biome = player.getWorld().getBiome((cx << 4) + 8, sampleY, (cz << 4) + 8);
            if (biome != targetBiome) continue;

            String ck = NationManager.chunkKey(worldName, cx, cz);
            if (!region.getClaimedChunks().contains(ck)) {
                if (added >= available) { hitLimit++; continue; }
                region.getClaimedChunks().add(ck);
                added++;
            }

            int[][] neighbors = {{cx + 1, cz}, {cx - 1, cz}, {cx, cz + 1}, {cx, cz - 1}};
            for (int[] n : neighbors) {
                if (Math.abs(n[0] - startCX) > searchRadius || Math.abs(n[1] - startCZ) > searchRadius) continue;
                long key = ((long) n[0] << 32) | (n[1] & 0xffffffffL);
                if (visited.add(key)) queue.add(n);
            }
        }

        plugin.getNationManager().saveRegion(region);
        player.sendMessage("§a" + added + " chunk(s) of biome §6" + targetBiome.name() + "§a claimed to region §6" + region.getName() + "§a." +
                (hitLimit > 0 ? " §e" + hitLimit + " chunk(s) skipped (" + maxRegionChunks + "-chunk limit reached)." : ""));
    }

    private void handleConserve(Player player) {
        NationData nation = findNationWithPermission(player.getUniqueId(), NationPermission.MANAGE_CONSERVATION);
        if (nation == null) {
            player.sendMessage("§cYou need conservation management permissions to use this.");
            return;
        }
        if (plugin.getChunkSelectionManager().hasSession(player.getUniqueId())) {
            player.sendMessage("§cYou already have an active selection session.");
            return;
        }
        plugin.getChunkSelectionManager().startConservationSelection(player, nation.getNationId());
    }

    private void handleMinter(Player player) {
        if (!plugin.getSettings().isMintersEnabled()) { player.sendMessage("§cMinters are disabled."); return; }
        NationData nation = findNationWithPermission(player.getUniqueId(), NationPermission.MANAGE_MINTERS);
        if (nation == null) { player.sendMessage("§cYou need minter management permissions to use this."); return; }
        UUID nid = nation.getNationId();
        player.closeInventory();
        plugin.getLocationSelectionManager().requestLocation(player,
                "§eRight-click a block to place the minter base.", loc -> {
                    plugin.getMinterManager().createMinterAtLocation(player, nid, loc);
                });
    }

    private void handleEnforcers(Player player) {
        if (!plugin.getSettings().isEnforcersEnabled()) { player.sendMessage("§cEnforcers are disabled."); return; }
        NationData nation = findNationWithPermission(player.getUniqueId(), NationPermission.MANAGE_ENFORCERS);
        if (nation == null) { player.sendMessage("§cYou need enforcer management permissions to use this."); return; }
        plugin.getGUIManager().openGUI(new com.dirt.inventory.impl.nation.EnforcerManagementGUI(
                plugin, nation.getNationId(), () -> null, 0), player);
    }

    private void handleCrime(Player player) {
        if (!plugin.getSettings().isCrimeEnabled()) { player.sendMessage("§cCrime system is disabled."); return; }
        NationData nation = findNationWithPermission(player.getUniqueId(), NationPermission.MANAGE_CRIME);
        if (nation == null) { player.sendMessage("§cYou need crime management permissions to use this."); return; }
        plugin.getGUIManager().openGUI(new com.dirt.inventory.impl.nation.CrimeManagementGUI(
                plugin, nation.getNationId(), () -> null), player);
    }

    private void handleLaws(Player player) {
        if (!plugin.getSettings().isLawsEnabled()) { player.sendMessage("§cLaws system is disabled."); return; }
        NationData nation = findNationWithPermission(player.getUniqueId(), NationPermission.MANAGE_LAWS);
        if (nation == null) { player.sendMessage("§cYou need law management permissions to use this."); return; }
        boolean isPresident = player.getUniqueId().equals(nation.getPresidentUUID());
        plugin.getGUIManager().openGUI(new com.dirt.inventory.impl.nation.LawsGUI(
                plugin, nation.getNationId(), isPresident), player);
    }

    private void handleDonate(Player player, String amountArg) {
        NationData nation = plugin.getNationManager().getNationByMember(player.getUniqueId());
        if (nation == null) {
            player.sendMessage("§cYou are not a member of any nation.");
            return;
        }
        double amount;
        try { amount = Double.parseDouble(amountArg); } catch (NumberFormatException e) {
            player.sendMessage("§cInvalid amount.");
            return;
        }
        if (amount <= 0) {
            player.sendMessage("§cAmount must be greater than zero.");
            return;
        }
        if (!plugin.getEconomy().has(player, amount)) {
            player.sendMessage("§cYou cannot afford to donate §e" + CurrencyUtil.symbol() + String.format("%.2f", amount) + "§c.");
            return;
        }
        plugin.getEconomy().withdrawPlayer(player, amount);
        plugin.getNationManager().depositToTreasury(nation.getName(), amount);
        player.sendMessage("§aYou donated §e" + CurrencyUtil.symbol() + String.format("%.2f", amount) + "§a to the " + nation.getName() + " treasury.");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player)) return Collections.emptyList();
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            completions.add("borders");
            completions.add("pborders");
            completions.add("donate");
            completions.add("votes");
            if (!plugin.getSettings().isCustomGovernmentEnabled()) {
                if (plugin.getRolesConfig().isSecurityHeadEnabled()) completions.add("ns");
                if (plugin.getRolesConfig().isVicePresidentEnabled()) completions.add("vp");
                if (plugin.getRolesConfig().isTreasurerEnabled()) completions.add("ct");
            }
            completions.add("pay");
            completions.add("conserve");
            completions.add("claim");
            completions.add("pcr");
            completions.add("announce");
            completions.add("join");
            completions.add("requests");
            completions.add("accept");
            completions.add("deny");
            if (plugin.getSettings().isMintersEnabled()) completions.add("minter");
            if (plugin.getSettings().isEnforcersEnabled()) completions.add("enforcers");
            if (plugin.getSettings().isCrimeEnabled()) completions.add("crime");
            if (plugin.getSettings().isLawsEnabled()) completions.add("laws");
            if (sender.hasPermission("DirtNations.admin")) completions.add("admin");
            return filter(completions, args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("claim")) {
            return filter(List.of("biome"), args[1]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("donate")) {
            return filter(List.of("10", "50", "100", "500", "1000"), args[1]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("join")) {
            List<String> nationNames = new ArrayList<>();
            for (NationData n : plugin.getNationManager().getAllNations()) {
                nationNames.add(n.getName());
            }
            return filter(nationNames, args[1]);
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("accept") || args[0].equalsIgnoreCase("deny"))) {
            NationData reqNation = findNationWithPermission(player.getUniqueId(), NationPermission.MANAGE_MEMBERS);
            if (reqNation != null) {
                List<String> names = new ArrayList<>();
                for (UUID reqUUID : reqNation.getJoinRequestUUIDs()) {
                    String pName = Bukkit.getOfflinePlayer(reqUUID).getName();
                    if (pName != null) names.add(pName);
                }
                return filter(names, args[1]);
            }
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("pcr")) {
            List<String> regionNames = new ArrayList<>();
            NationData presNation = plugin.getNationManager().getNationByPresident(player.getUniqueId());
            if (presNation != null) {
                for (UUID rid : presNation.getRegionIds()) {
                    RegionData r = plugin.getNationManager().loadRegion(rid);
                    if (r != null) regionNames.add(r.getName());
                }
            } else if (player.hasPermission("DirtNations.admin")) {
                for (RegionData r : plugin.getNationManager().getAllRegions()) regionNames.add(r.getName());
            }
            return filter(regionNames, args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("pcr")) {
            return filter(List.of("10", "50", "100", "500", "1000"), args[2]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("admin")) {
            return filter(List.of("define", "tp"), args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("admin") && args[1].equalsIgnoreCase("define")) {
            return filter(List.of("nation"), args[2]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("admin") && args[1].equalsIgnoreCase("tp")) {
            List<String> nationNames = new ArrayList<>();
            for (NationData nation : plugin.getNationManager().getAllNations()) nationNames.add(commandName(nation.getName()));
            return filter(nationNames, args[2]);
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("admin") && args[1].equalsIgnoreCase("tp")) {
            NationData nation = findNationByCommandName(args[2]);
            if (nation == null) return Collections.emptyList();
            List<String> regionNames = new ArrayList<>();
            for (UUID regionId : nation.getRegionIds()) {
                RegionData region = plugin.getNationManager().loadRegion(regionId);
                if (region != null) regionNames.add(commandName(region.getName()));
            }
            return filter(regionNames, args[3]);
        }
        return Collections.emptyList();
    }

    private List<String> filter(List<String> options, String input) {
        String lower = input.toLowerCase();
        List<String> result = new ArrayList<>();
        for (String opt : options) {
            if (opt.toLowerCase().startsWith(lower)) result.add(opt);
        }
        return result;
    }
}
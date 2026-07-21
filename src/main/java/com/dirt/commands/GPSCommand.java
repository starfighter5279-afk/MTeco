package com.dirt.commands;

import com.dirt.DirtEconomy;
import com.dirt.data.BusinessData;
import com.dirt.data.BusinessPermission;
import com.dirt.data.CharacterData;
import com.dirt.data.GPSLocation;
import com.dirt.inventory.impl.gps.GPSDetailsGUI;
import com.dirt.inventory.impl.gps.GPSManageGUI;
import com.dirt.inventory.impl.gps.GPSTypeSelectGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GPSCommand implements CommandExecutor, TabCompleter {
    private final DirtEconomy plugin;

    public GPSCommand(DirtEconomy plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("\u00a7cOnly players can use this command.");
            return true;
        }

        CharacterData character = plugin.getCharacterManager().getCharacter(player.getUniqueId());
        if (character == null) {
            player.sendMessage("\u00a7cYou need an active character to use this.");
            return true;
        }

        if (label.equalsIgnoreCase("setgps")) {
            return handleSetGPS(player, character, args);
        }

        return handleGPS(player, character, args);
    }

    private boolean handleGPS(Player player, CharacterData character, String[] args) {
        if (args.length == 0) {
            player.sendMessage("\u00a7eGPS Commands:");
            player.sendMessage("\u00a76/gps <location> \u00a7- Navigate to a location");
            player.sendMessage("\u00a76/gps <location> details \u00a7- View location details");
            player.sendMessage("\u00a76/gps stop \u00a7- Stop current navigation");
            player.sendMessage("\u00a76/gps manage \u00a7- Manage your GPS locations");
            player.sendMessage("\u00a76/setgps <name> \u00a7- Create a GPS location");
            player.sendMessage("\u00a76/setgps business <business> \u00a7- Create a business GPS location");
            return true;
        }

        if (args[0].equalsIgnoreCase("stop")) {
            if (plugin.getGpsManager().hasActiveTrail(player.getUniqueId())) {
                plugin.getGpsManager().stopTrail(player.getUniqueId());
                player.sendMessage("\u00a7eGPS navigation stopped.");
            } else {
                player.sendMessage("\u00a7cYou don't have an active GPS route.");
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("manage")) {
            plugin.getGUIManager().openGUI(new GPSManageGUI(plugin, player.getUniqueId(), 0), player);
            return true;
        }

        String locationName = String.join(" ", args);
        boolean showDetails = false;
        if (args.length >= 2 && args[args.length - 1].equalsIgnoreCase("details")) {
            showDetails = true;
            String[] sub = new String[args.length - 1];
            System.arraycopy(args, 0, sub, 0, args.length - 1);
            locationName = String.join(" ", sub);
        }

        GPSLocation gps = plugin.getGpsManager().findByName(locationName);
        if (gps == null) {
            player.sendMessage("\u00a7cGPS location '\u00a7e" + locationName + "\u00a7c' not found.");
            return true;
        }

        if (!gps.isGlobal() && !player.getUniqueId().equals(gps.getCreatorUuid())) {
            player.sendMessage("\u00a7cYou don't have access to that GPS location.");
            return true;
        }

        if (showDetails) {
            plugin.getGUIManager().openGUI(new GPSDetailsGUI(plugin, gps.getLocationId()), player);
        } else {
            plugin.getGpsManager().startTrail(player, gps);
        }
        return true;
    }

    private boolean handleSetGPS(Player player, CharacterData character, String[] args) {
        if (args.length == 0) {
            player.sendMessage("\u00a7cUsage: /setgps <name> or /setgps business <business name>");
            return true;
        }

        if (args[0].equalsIgnoreCase("business")) {
            if (!plugin.isDirtBusinessEnabled()) {
                player.sendMessage("\u00a7cBusinesses are not enabled.");
                return true;
            }
            if (args.length < 2) {
                player.sendMessage("\u00a7cUsage: /setgps business <business name>");
                return true;
            }
            String bizName = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
            BusinessData biz = null;
            for (BusinessData b : plugin.getBusinessManager().getAllBusinesses()) {
                if (b.getName().equalsIgnoreCase(bizName)) { biz = b; break; }
            }
            if (biz == null) {
                player.sendMessage("\u00a7cBusiness '\u00a7e" + bizName + "\u00a7c' not found.");
                return true;
            }
            boolean isOwner = player.getUniqueId().equals(biz.getOwnerUUID());
            boolean hasGpsPerm = biz.hasPermission(player.getUniqueId(), BusinessPermission.MANAGE_GPS);
            if (!isOwner && !hasGpsPerm) {
                player.sendMessage("\u00a7cYou don't have GPS permissions for that business.");
                return true;
            }
            if (!plugin.getSettings().isGpsBusinessLocationsEnabled()) {
                player.sendMessage("\u00a7cBusiness GPS locations are disabled.");
                return true;
            }
            final BusinessData finalBiz = biz;
            plugin.getChatInputManager().requestInput(player, "\u00a7eEnter a name for this business GPS location:", name -> {
                plugin.getChatInputManager().requestInput(player, "\u00a7eEnter a description for this location (or type 'none'):", desc -> {
                    String description = desc.equalsIgnoreCase("none") ? "" : desc;
                    GPSLocation gps = plugin.getGpsManager().createLocation(name, description,
                            player.getUniqueId(), finalBiz.getBusinessId(), player.getLocation(), true);
                    player.sendMessage("\u00a7aBusiness GPS location '\u00a7e" + gps.getName() + "\u00a7a' created for \u00a76" + finalBiz.getName() + "\u00a7a!");
                });
            });
            return true;
        }

        String locName = String.join(" ", args);
        int maxPersonal = plugin.getSettings().getGpsMaxPersonalLocations();
        long personalCount = plugin.getGpsManager().getPersonalLocations(player.getUniqueId()).size();
        int maxGlobal = plugin.getSettings().getGpsMaxGlobalLocations();
        long globalCount = plugin.getGpsManager().getGlobalLocations().size();

        GPSLocation existing = plugin.getGpsManager().findByName(locName);
        if (existing != null) {
            player.sendMessage("\u00a7cA GPS location with that name already exists.");
            return true;
        }

        plugin.getGUIManager().openGUI(new GPSTypeSelectGUI(plugin, locName, player.getLocation(),
                maxPersonal, (int) personalCount, maxGlobal, (int) globalCount), player);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player)) return Collections.emptyList();

        if (alias.equalsIgnoreCase("setgps")) {
            if (args.length == 1) {
                List<String> completions = new ArrayList<>();
                completions.add("business");
                return filter(completions, args[0]);
            }
            if (args.length >= 2 && args[0].equalsIgnoreCase("business") && plugin.isDirtBusinessEnabled()) {
                List<String> bizNames = new ArrayList<>();
                for (BusinessData b : plugin.getBusinessManager().getBusinessesByOwner(player.getUniqueId())) {
                    bizNames.add(b.getName());
                }
                for (BusinessData b : plugin.getBusinessManager().getBusinessesByEmployee(player.getUniqueId())) {
                    if (b.hasPermission(player.getUniqueId(), BusinessPermission.MANAGE_GPS)) {
                        bizNames.add(b.getName());
                    }
                }
                String partial = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
                return filter(bizNames, partial);
            }
            return Collections.emptyList();
        }

        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            completions.add("stop");
            completions.add("manage");
            if (plugin.getGpsManager() != null) {
                for (GPSLocation loc : plugin.getGpsManager().getAccessibleLocations(player.getUniqueId())) {
                    completions.add(loc.getName());
                }
            }
            return filter(completions, args[0]);
        }
        if (args.length == 2) {
            return filter(List.of("details"), args[1]);
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
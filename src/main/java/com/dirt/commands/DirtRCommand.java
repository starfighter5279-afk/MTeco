package com.dirt.commands;

import com.dirt.DirtEconomy;
import com.dirt.data.NationData;
import com.dirt.data.NationData;
import com.dirt.data.RegionData;
import com.dirt.inventory.impl.nation.GovernorRegionsGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class DirtRCommand implements CommandExecutor, TabCompleter {
    private final DirtEconomy plugin;

    public DirtRCommand(DirtEconomy plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("\u00a7cOnly players can use this command.");
            return true;
        }
        if (!plugin.isDirtNationsEnabled()) {
            player.sendMessage("\u00a7cNations are not enabled on this server.");
            return true;
        }

        if (args.length >= 2 && args[0].equalsIgnoreCase("auto") && args[1].equalsIgnoreCase("off")) {
            plugin.getChunkSelectionManager().stopAutoClaim(player);
            return true;
        }

        if (args.length >= 1 && args[0].equalsIgnoreCase("auto")) {
            String regionName = args.length >= 2 ? args[1] : null;
            handleAuto(player, regionName);
            return true;
        }

        if (args.length >= 1 && args[0].equalsIgnoreCase("claim")) {
            String regionName = args.length >= 2 && !isNumber(args[1]) ? args[1] : null;
            int radius = 0;
            if (args.length >= 2 && isNumber(args[args.length - 1])) {
                radius = Math.min(100, Math.max(0, Integer.parseInt(args[args.length - 1])));
            }
            handleClaim(player, regionName, radius);
            return true;
        }

        if (plugin.getNationManager().getRegionsByGovernor(player.getUniqueId()).isEmpty()) {
            player.sendMessage("\u00a7cYou are not a governor of any region.");
            return true;
        }
        plugin.getGUIManager().openGUI(new GovernorRegionsGUI(plugin, 0), player);
        return true;
    }

    private void handleClaim(Player player, String regionName, int radius) {
        if (plugin.getChunkSelectionManager().hasSession(player.getUniqueId())) {
            player.sendMessage("\u00a7cYou already have an active selection session.");
            return;
        }

        NationData presNation = plugin.getNationManager().getNationByPresident(player.getUniqueId());
        if (presNation != null && regionName != null) {
            RegionData targetRegion = findRegionByName(presNation, regionName);
            if (targetRegion == null) { player.sendMessage("\u00a7cRegion '\u00a7e" + regionName + "\u00a7c' not found in your nation."); return; }
            plugin.getChunkSelectionManager().startManualClaim(player, targetRegion.getRegionId(), radius);
            return;
        }

        UUID targeted = plugin.getChunkSelectionManager().getTargetedRegion(player.getUniqueId());
        if (targeted == null) { player.sendMessage("\u00a7cYou have no targeted region. Use \u00a7e/dr \u00a7cto target a region first."); return; }
        plugin.getChunkSelectionManager().startManualClaim(player, targeted, radius);
    }

    private void handleAuto(Player player, String regionName) {
        if (plugin.getChunkSelectionManager().hasSession(player.getUniqueId())) {
            plugin.getChunkSelectionManager().stopAutoClaim(player);
            return;
        }

        NationData presNation = plugin.getNationManager().getNationByPresident(player.getUniqueId());
        if (presNation != null && regionName != null) {
            RegionData targetRegion = findRegionByName(presNation, regionName);
            if (targetRegion == null) { player.sendMessage("\u00a7cRegion '\u00a7e" + regionName + "\u00a7c' not found in your nation."); return; }
            plugin.getChunkSelectionManager().startAutoClaimForRegion(player, targetRegion.getRegionId());
            return;
        }

        UUID targeted = plugin.getChunkSelectionManager().getTargetedRegion(player.getUniqueId());
        if (targeted == null) { player.sendMessage("\u00a7cYou have no targeted region. Use \u00a7e/dr \u00a7cto target a region first."); return; }
        plugin.getChunkSelectionManager().startAutoClaimForRegion(player, targeted);
    }

    private RegionData findRegionByName(NationData nation, String name) {
        for (UUID rid : nation.getRegionIds()) {
            RegionData r = plugin.getNationManager().loadRegion(rid);
            if (r != null && r.getName().equalsIgnoreCase(name)) return r;
        }
        return null;
    }

    private boolean isNumber(String s) {
        try { Integer.parseInt(s); return true; } catch (NumberFormatException e) { return false; }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!plugin.isDirtNationsEnabled()) return Collections.emptyList();
        if (args.length == 1) {
            return filter(List.of("claim", "auto"), args[0]);
        }
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("auto")) {
                List<String> completions = new ArrayList<>();
                completions.add("off");
                if (sender instanceof Player player) {
                    for (RegionData r : plugin.getNationManager().getRegionsByGovernor(player.getUniqueId())) {
                        completions.add(r.getName());
                    }
                    NationData presNation = plugin.getNationManager().getNationByPresident(player.getUniqueId());
                    if (presNation != null) {
                        for (UUID rid : presNation.getRegionIds()) {
                            RegionData r = plugin.getNationManager().loadRegion(rid);
                            if (r != null && !completions.contains(r.getName())) completions.add(r.getName());
                        }
                    }
                }
                return filter(completions, args[1]);
            }
            if (args[0].equalsIgnoreCase("claim")) {
                List<String> completions = new ArrayList<>();
                if (sender instanceof Player player) {
                    for (RegionData r : plugin.getNationManager().getRegionsByGovernor(player.getUniqueId())) {
                        completions.add(r.getName());
                    }
                    NationData presNation = plugin.getNationManager().getNationByPresident(player.getUniqueId());
                    if (presNation != null) {
                        for (UUID rid : presNation.getRegionIds()) {
                            RegionData r = plugin.getNationManager().loadRegion(rid);
                            if (r != null && !completions.contains(r.getName())) completions.add(r.getName());
                        }
                    }
                }
                return filter(completions, args[1]);
            }
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("claim")) {
            return filter(List.of("1", "5", "10", "25", "50"), args[2]);
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
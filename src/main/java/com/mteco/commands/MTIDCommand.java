package com.mteco.commands;

import com.mteco.MTeco;
import com.mteco.data.CharacterData;
import com.mteco.inventory.impl.IDViewGUI;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class MTIDCommand implements CommandExecutor, TabCompleter {
    private final MTeco plugin;

    public MTIDCommand(MTeco plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return true;
        }

        if (args.length >= 2 && args[0].equalsIgnoreCase("view")) {
            openIDForTarget(player, args[1]);
            return true;
        }

        CharacterData data = plugin.getCharacterManager().getCharacter(player.getUniqueId());
        if (data == null) {
            player.sendMessage("§cYou do not have an active character.");
            return true;
        }
        plugin.getGUIManager().openGUI(new IDViewGUI(plugin, data, player.getUniqueId(), true), player);
        return true;
    }

    private void openIDForTarget(Player viewer, String targetName) {
        Player onlineTarget = Bukkit.getPlayer(targetName);
        if (onlineTarget != null) {
            CharacterData data = plugin.getCharacterManager().getCharacter(onlineTarget.getUniqueId());
            if (data == null) {
                viewer.sendMessage("§c" + onlineTarget.getName() + " has no active character.");
                return;
            }
            plugin.getGUIManager().openGUI(new IDViewGUI(plugin, data, onlineTarget.getUniqueId(), false), viewer);
            return;
        }

        @SuppressWarnings("deprecation")
        OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(targetName);
        if (!offlineTarget.hasPlayedBefore() && !offlineTarget.isOnline()) {
            viewer.sendMessage("§cPlayer '§e" + targetName + "§c' was not found.");
            return;
        }

        UUID targetUUID = offlineTarget.getUniqueId();
        CharacterData data = plugin.getCharacterManager().getCharacter(targetUUID);
        if (data == null) {
            viewer.sendMessage("§c" + targetName + " has no active character.");
            return;
        }
        plugin.getGUIManager().openGUI(new IDViewGUI(plugin, data, targetUUID, false), viewer);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(List.of("view"), args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("view")) {
            List<String> names = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) names.add(p.getName());
            return filter(names, args[1]);
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
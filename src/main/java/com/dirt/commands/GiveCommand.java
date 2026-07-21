package com.dirt.commands;

import com.dirt.DirtEconomy;
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

public class GiveCommand implements CommandExecutor, TabCompleter {

    private final DirtEconomy plugin;

    public GiveCommand(DirtEconomy plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("dirt.admin")) {
            sender.sendMessage("§cYou don't have permission to do that.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("§e§m                                          §r");
            sender.sendMessage("§e§l  Give Commands:");
            sender.sendMessage("§f  /give phone <player>");
            sender.sendMessage("§f  /give life_token <player> [amount]");
            sender.sendMessage("§e§m                                          §r");
            return true;
        }

        if (args[0].equalsIgnoreCase("life_token")) {
            return handleLifeToken(sender, args);
        }

        if (!args[0].equalsIgnoreCase("phone")) {
            sender.sendMessage("§e§m                                          §r");
            sender.sendMessage("§e§l  Give Commands:");
            sender.sendMessage("§f  /give phone <player>");
            sender.sendMessage("§f  /give life_token <player> [amount]");
            sender.sendMessage("§e§m                                          §r");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage("§cPlayer not found.");
            return true;
        }

        if (plugin.getPhoneListener() == null) {
            sender.sendMessage("§cThe phone system is not enabled.");
            return true;
        }

        target.getInventory().addItem(plugin.getPhoneListener().createPhoneItem());
        sender.sendMessage("§a§l✦ §eGave a phone to §b" + target.getName() + "§e!");
        return true;
    }

    private boolean handleLifeToken(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§eUsage: §f/give life_token <player> [amount]");
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage("§cPlayer has never joined the server.");
            return true;
        }

        int amount = 1;
        if (args.length >= 3) {
            try {
                amount = Integer.parseInt(args[2]);
                if (amount < 1) {
                    sender.sendMessage("§cAmount must be at least 1.");
                    return true;
                }
            } catch (NumberFormatException e) {
                sender.sendMessage("§cInvalid number: §f" + args[2]);
                return true;
            }
        }

        plugin.getLifeManager().addTokens(target.getUniqueId(), amount);
        int total = plugin.getLifeManager().getTokens(target.getUniqueId());
        sender.sendMessage("");
        sender.sendMessage("§a§l✦ §eGave §a§l" + amount + " Life Token" + (amount != 1 ? "s" : "") + " §eto §b" + target.getName() + "§e!");
        sender.sendMessage("§7  They now have §e" + total + " §7total token" + (total != 1 ? "s" : "") + ".");
        sender.sendMessage("");

        if (target.isOnline()) {
            Player onlineTarget = target.getPlayer();
            if (onlineTarget != null) {
                onlineTarget.sendMessage("");
                onlineTarget.sendMessage("§a§m                                          §r");
                onlineTarget.sendMessage("§a§l  ✦ §eYou received §a§l" + amount + " Life Token" + (amount != 1 ? "s" : "") + "§e!");
                onlineTarget.sendMessage("§7  You now have §e" + total + " §7total.");
                onlineTarget.sendMessage("§a§m                                          §r");
                onlineTarget.sendMessage("");
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("dirt.admin")) return Collections.emptyList();

        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            completions.add("phone");
            completions.add("life_token");
            String lower = args[0].toLowerCase();
            completions.removeIf(s -> !s.startsWith(lower));
            return completions;
        }

        if (args.length == 2 && (args[0].equalsIgnoreCase("phone") || args[0].equalsIgnoreCase("life_token"))) {
            List<String> names = new ArrayList<>();
            String lower = args[1].toLowerCase();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(lower)) {
                    names.add(p.getName());
                }
            }
            return names;
        }

        return Collections.emptyList();
    }
}
package com.dirt.commands;

import com.dirt.DirtEconomy;
import com.dirt.data.CharacterData;
import com.dirt.util.DiscordBotManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DirtCommand implements CommandExecutor, TabCompleter {

    private final DirtEconomy plugin;

    public DirtCommand(DirtEconomy plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("dirt.reload")) {
                sender.sendMessage("§cYou don't have permission to do that.");
                return true;
            }
            plugin.getSettings().reload();
            plugin.getRolesConfig().reload();
            plugin.reloadData();
            sender.sendMessage("§aConfiguration and data caches reloaded successfully.");
            return true;
        }

        if (args.length > 0 && (args[0].equalsIgnoreCase("link") || args[0].equalsIgnoreCase("unlink"))) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("§cOnly players can use this command.");
                return true;
            }
            return handleDiscord(player, args);
        }

        sender.sendMessage("§eUsage: §f/d reload §7| §f/d link §7| §f/d unlink");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            if (sender.hasPermission("dirt.reload")) completions.add("reload");
            completions.add("link");
            completions.add("unlink");
            String lower = args[0].toLowerCase();
            completions.removeIf(s -> !s.startsWith(lower));
            return completions;
        }
        return Collections.emptyList();
    }

    private boolean handleDiscord(Player player, String[] args) {
        if (!plugin.getSettings().isDiscordEnabled()) {
            player.sendMessage("§cDiscord integration is not enabled on this server.");
            return true;
        }
        DiscordBotManager bot = plugin.getDiscordBotManager();
        if (bot == null || !bot.isConnected()) {
            player.sendMessage("§cThe Discord bot is not connected. Please contact an admin.");
            return true;
        }
        if (!plugin.getCharacterManager().hasCharacter(player.getUniqueId())) {
            player.sendMessage("§cYou need a character first. Use §e/dc create§c.");
            return true;
        }

        if (args[0].equalsIgnoreCase("link")) {
            CharacterData data = plugin.getCharacterManager().getCharacter(player.getUniqueId());
            if (data.getDiscordId() != null) {
                player.sendMessage("§eYour character is already linked. Use §f/d unlink §eto remove it first.");
                return true;
            }
            String code = bot.generateLinkCode(player.getUniqueId());
            String channelId = plugin.getSettings().getDiscordLinkChannelId();
            player.sendMessage("§a§lYour link code: §f§l" + code);
            if (channelId != null && !channelId.isEmpty()) {
                player.sendMessage("§7Send this code in the link channel on Discord or DM the bot.");
            } else {
                player.sendMessage("§7DM this code to the Discord bot to link your account.");
            }
            player.sendMessage("§7The code expires in 5 minutes.");
            return true;
        }

        if (args[0].equalsIgnoreCase("unlink")) {
            CharacterData data = plugin.getCharacterManager().getCharacter(player.getUniqueId());
            if (data.getDiscordId() == null) {
                player.sendMessage("§cYour character is not linked to Discord.");
                return true;
            }
            data.setDiscordId(null);
            plugin.getCharacterManager().saveCharacter(data);
            player.sendMessage("§aDiscord account unlinked. You will no longer receive notifications.");
            return true;
        }

        return true;
    }
}
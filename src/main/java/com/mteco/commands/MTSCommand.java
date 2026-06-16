package com.mteco.commands;

import com.mteco.MTeco;
import com.mteco.data.CharacterData;
import com.mteco.inventory.impl.shops.OnlineShopsListGUI;
import com.mteco.util.CurrencyUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MTSCommand implements CommandExecutor, TabCompleter {
    private final MTeco plugin;

    public MTSCommand(MTeco plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("\u00a7cOnly players can use this command.");
            return true;
        }
        if (!plugin.isMTShopsEnabled()) {
            player.sendMessage("\u00a7cShops are not enabled on this server.");
            return true;
        }
        if (args.length >= 1 && args[0].equalsIgnoreCase("online")) {
            if (!plugin.isOnlineShopsEnabled()) {
                player.sendMessage("\u00a7cOnline shops are disabled on this server.");
                return true;
            }
            plugin.getGUIManager().openGUI(new OnlineShopsListGUI(plugin, 0), player);
            return true;
        }
        if (args.length >= 1 && args[0].equalsIgnoreCase("top")) {
            showLeaderboard(player);
            return true;
        }
        player.sendMessage("\u00a76MTeco \u00a78\u2014 \u00a77Shop Commands");
        player.sendMessage("\u00a7e/mts online \u00a78- \u00a77Browse online shops");
        player.sendMessage("\u00a7e/mts top \u00a78- \u00a77View wealthiest players");
        return true;
    }

    private void showLeaderboard(Player player) {
        List<CharacterData> characters = plugin.getCharacterManager().getAllCharacters();
        List<CharacterData> sorted = new ArrayList<>(characters);
        sorted.sort(Comparator.comparingDouble((CharacterData c) -> {
            OfflinePlayer op = Bukkit.getOfflinePlayer(c.getPlayerUuid());
            return plugin.getEconomy().getBalance(op);
        }).reversed());

        player.sendMessage("§6§l▬▬▬▬▬▬▬ Wealthiest Citizens ▬▬▬▬▬▬▬");
        int limit = Math.min(sorted.size(), 10);
        for (int i = 0; i < limit; i++) {
            CharacterData c = sorted.get(i);
            OfflinePlayer op = Bukkit.getOfflinePlayer(c.getPlayerUuid());
            double balance = plugin.getEconomy().getBalance(op);
            String name = c.getFirstName() + " " + c.getLastName();
            String medal = switch (i) {
                case 0 -> "§e§l\u2B50 ";
                case 1 -> "§7§l\u2B50 ";
                case 2 -> "§6§l\u2B50 ";
                default -> "§8" + (i + 1) + ". ";
            };
            player.sendMessage(medal + "fffffa7f" + name + " fffffa78- fffffa7a" + CurrencyUtil.symbol() + String.format("%,.0f", balance));
        }
        if (sorted.isEmpty()) {
            player.sendMessage("§7No characters found.");
        }
        player.sendMessage("§6§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(List.of("online", "top"), args[0]);
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
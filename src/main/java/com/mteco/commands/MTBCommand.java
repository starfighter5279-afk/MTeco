package com.mteco.commands;

import com.mteco.MTeco;
import com.mteco.inventory.impl.business.AllBusinessesGUI;
import com.mteco.inventory.impl.business.MyBusinessesGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MTBCommand implements CommandExecutor, TabCompleter {
    private final MTeco plugin;

    public MTBCommand(MTeco plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("\u00a7cOnly players can use this command.");
            return true;
        }
        if (!plugin.isMTBusinessEnabled()) {
            player.sendMessage("\u00a7cBusinesses are not enabled on this server.");
            return true;
        }
        if (!plugin.getCharacterManager().hasCharacter(player.getUniqueId())) {
            player.sendMessage("\u00a7cYou must have a character to use this command.");
            return true;
        }
        if (args.length >= 1 && args[0].equalsIgnoreCase("view")) {
            plugin.getGUIManager().openGUI(new AllBusinessesGUI(plugin, 0), player);
        } else {
            plugin.getGUIManager().openGUI(new MyBusinessesGUI(plugin, 0), player);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(List.of("view"), args[0]);
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
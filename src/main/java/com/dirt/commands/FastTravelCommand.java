package com.dirt.commands;

import com.dirt.DirtEconomy;
import com.dirt.inventory.impl.travel.FastTravelGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class FastTravelCommand implements CommandExecutor {
    private final DirtEconomy plugin;

    public FastTravelCommand(DirtEconomy plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use fast travel.");
            return true;
        }

        plugin.getGUIManager().openGUI(new FastTravelGUI(plugin, 0), player);
        return true;
    }
}
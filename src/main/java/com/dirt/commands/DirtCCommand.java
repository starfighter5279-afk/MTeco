package com.dirt.commands;

import com.dirt.DirtEconomy;
import com.dirt.data.CharacterData;
import com.dirt.inventory.impl.CharacterCreationGUI;
import com.dirt.inventory.impl.CharacterManagementGUI;
import com.dirt.inventory.impl.LifeGUI;
import com.dirt.inventory.impl.ServerCharacterListGUI;
import com.dirt.inventory.impl.contract.ContractCreationGUI;
import com.dirt.util.CurrencyUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DirtCCommand implements CommandExecutor, TabCompleter {
    private final DirtEconomy plugin;

    public DirtCCommand(DirtEconomy plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("bal")) {
            if (!plugin.getCharacterManager().hasCharacter(player.getUniqueId())) {
                player.sendMessage("§cYou do not have a character yet.");
                return true;
            }
            double balance = plugin.getEconomy().getBalance(player);
            player.sendMessage("§eYour balance: §a" + CurrencyUtil.symbol() + String.format("%.2f", balance));
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("create")) {
            if (plugin.getCharacterManager().hasCharacter(player.getUniqueId())) {
                player.sendMessage("§eYou already have an active character. Opening your life menu so you can create a death if you wish.");
                plugin.getGUIManager().openGUI(new LifeGUI(plugin), player);
                return true;
            }
            plugin.getCharacterManager().initPendingCreation(player.getUniqueId());
            plugin.getGUIManager().openGUI(new CharacterCreationGUI(plugin), player);
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("contract")) {
            if (!plugin.isContractsEnabled()) {
                player.sendMessage("\u00a7cContracts are disabled on this server.");
                return true;
            }
            if (!plugin.getCharacterManager().hasCharacter(player.getUniqueId())) {
                player.sendMessage("\u00a7cYou need a character to create contracts. Use \u00a7e/dc create\u00a7c.");
                return true;
            }
            plugin.getGUIManager().openGUI(new ContractCreationGUI(plugin, player.getUniqueId()), player);
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("admin")) {
            if (!player.isOp()) {
                player.sendMessage("§cOnly server operators can use this command.");
                return true;
            }
            plugin.getGUIManager().openGUI(new ServerCharacterListGUI(plugin, 0, null), player);
            return true;
        }

        if (!plugin.getCharacterManager().hasCharacter(player.getUniqueId())) {
            player.sendMessage("§cYou do not have a character yet. Use §e/dc create §cto get started.");
            return true;
        }

        plugin.getGUIManager().openGUI(new CharacterManagementGUI(plugin), player);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            completions.add("create");
            completions.add("bal");
            if (plugin.isContractsEnabled()) completions.add("contract");
            if (sender.isOp()) completions.add("admin");
            return filter(completions, args[0]);
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
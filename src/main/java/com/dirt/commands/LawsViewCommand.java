package com.dirt.commands;

import com.dirt.DirtEconomy;
import com.dirt.data.CharacterData;
import com.dirt.data.LawBookData;
import com.dirt.data.NationData;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LawsViewCommand implements CommandExecutor, TabCompleter {
    private final DirtEconomy plugin;

    public LawsViewCommand(DirtEconomy plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("\u00a7cOnly players can use this command.");
            return true;
        }
        if (!plugin.isDirtNationsEnabled() || !plugin.getSettings().isLawsEnabled()) {
            player.sendMessage("\u00a7cLaws system is not enabled.");
            return true;
        }

        NationData nation = plugin.getNationManager().getNationByMember(player.getUniqueId());
        if (nation == null) {
            player.sendMessage("\u00a7cYou are not a member of any nation.");
            return true;
        }

        if (args.length >= 1 && args[0].equalsIgnoreCase("view")) {
            List<LawBookData> books = plugin.getLawCrimeManager().getLawBooksByNation(nation.getNationId(), true);
            if (books.isEmpty()) {
                player.sendMessage("\u00a77Your nation has no published law books.");
                return true;
            }
            for (LawBookData book : books) {
                ItemStack bookItem = new ItemStack(Material.WRITTEN_BOOK);
                BookMeta meta = (BookMeta) bookItem.getItemMeta();
                if (meta != null) {
                    meta.setTitle(book.getTitle());
                    CharacterData author = plugin.getCharacterManager().getCharacter(book.getAuthorUUID());
                    meta.setAuthor(author != null ? author.getFirstName() + " " + author.getLastName() : "Unknown");
                    for (String section : book.getSections()) {
                        String pageText = section;
                        while (pageText.length() > 256) {
                            meta.addPage(pageText.substring(0, 256));
                            pageText = pageText.substring(256);
                        }
                        if (!pageText.isEmpty()) meta.addPage(pageText);
                    }
                    bookItem.setItemMeta(meta);
                }
                NamespacedKey key = new NamespacedKey(plugin, "law_book");
                BookMeta tagged = (BookMeta) bookItem.getItemMeta();
                if (tagged != null) {
                    tagged.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
                    bookItem.setItemMeta(tagged);
                }
                player.getInventory().addItem(bookItem);
            }
            player.sendMessage("\u00a7a" + books.size() + " law book(s) added to your inventory. \u00a77Close your inventory to remove them.");
            return true;
        }

        player.sendMessage("\u00a7eUsage: /laws view");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            completions.add("view");
            String lower = args[0].toLowerCase();
            List<String> result = new ArrayList<>();
            for (String opt : completions) {
                if (opt.startsWith(lower)) result.add(opt);
            }
            return result;
        }
        return Collections.emptyList();
    }
}
package com.mteco.commands;

import com.mteco.MTeco;
import com.mteco.data.BusinessData;
import com.mteco.data.BusinessPropertyData;
import com.mteco.data.CharacterData;
import com.mteco.data.NationData;
import com.mteco.data.PropertyData;
import com.mteco.data.RegionData;
import com.mteco.managers.NationManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class HomeCommand implements CommandExecutor, TabCompleter {
    private final MTeco plugin;

    public HomeCommand(MTeco plugin) {
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

        if (label.equalsIgnoreCase("sethome")) {
            return handleSetHome(player, character);
        } else {
            return handleHome(player, character);
        }
    }

    private boolean handleSetHome(Player player, CharacterData character) {
        if (plugin.getNationManager() == null) {
            player.sendMessage("\u00a7cNations are not enabled.");
            return true;
        }

        Location loc = player.getLocation();
        String chunkKey = NationManager.chunkKey(loc.getWorld().getName(), loc.getBlockX() >> 4, loc.getBlockZ() >> 4);

        RegionData region = plugin.getNationManager().getRegionByChunk(chunkKey);
        if (region == null) {
            player.sendMessage("\u00a7cYou can only set your home within nation territory.");
            return true;
        }

        NationData nation = plugin.getNationManager().loadNation(region.getNationId());
        if (nation == null || !nation.getMemberUUIDs().contains(player.getUniqueId())) {
            player.sendMessage("\u00a7cYou can only set your home in your own nation's territory.");
            return true;
        }

        PropertyData property = plugin.getNationManager().getPropertyByChunk(chunkKey);
        if (property != null) {
            NationData ownerNation = plugin.getNationManager().loadNation(property.getOwnerUUID());
            if (ownerNation == null && !property.getOwnerUUID().equals(player.getUniqueId())) {
                player.sendMessage("\u00a7cYou cannot set your home on someone else's property.");
                return true;
            }
        }

        if (plugin.isMTBusinessEnabled()) {
            BusinessPropertyData bizProp = plugin.getBusinessManager().getPropertyByChunk(chunkKey);
            if (bizProp != null) {
                BusinessData biz = plugin.getBusinessManager().loadBusiness(bizProp.getBusinessId());
                if (biz != null && !biz.getOwnerUUID().equals(player.getUniqueId())
                        && !biz.getEmployeeUUIDs().contains(player.getUniqueId())) {
                    player.sendMessage("\u00a7cYou cannot set your home on a business property you don't work at.");
                    return true;
                }
            }
        }

        String homeStr = loc.getWorld().getName() + "," + loc.getX() + "," + loc.getY() + "," + loc.getZ() + "," + loc.getYaw() + "," + loc.getPitch();
        character.setHomeLocation(homeStr);
        plugin.getCharacterManager().saveCharacter(character);
        player.sendMessage("\u00a7aHome set at your current location.");
        return true;
    }

    private boolean handleHome(Player player, CharacterData character) {
        String homeStr = character.getHomeLocation();
        if (homeStr == null || homeStr.isEmpty()) {
            player.sendMessage("\u00a7cYou haven't set a home yet. Use \u00a7e/sethome\u00a7c first.");
            return true;
        }

        try {
            String[] parts = homeStr.split(",");
            World world = Bukkit.getWorld(parts[0]);
            if (world == null) {
                player.sendMessage("\u00a7cHome world no longer exists.");
                return true;
            }
            double x = Double.parseDouble(parts[1]);
            double y = Double.parseDouble(parts[2]);
            double z = Double.parseDouble(parts[3]);
            float yaw = parts.length > 4 ? Float.parseFloat(parts[4]) : 0;
            float pitch = parts.length > 5 ? Float.parseFloat(parts[5]) : 0;
            player.teleport(new Location(world, x, y, z, yaw, pitch));
            player.sendMessage("\u00a7aTeleported to your home.");
        } catch (Exception e) {
            player.sendMessage("\u00a7cFailed to teleport to home. Try setting it again with \u00a7e/sethome\u00a7c.");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        return Collections.emptyList();
    }
}
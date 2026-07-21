package com.dirt.listeners;

import com.dirt.DirtEconomy;
import org.bukkit.Location;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.InventoryHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ChestProtectionListener implements Listener {
    private final DirtEconomy plugin;

    public ChestProtectionListener(DirtEconomy plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        InventoryHolder holder = event.getInventory().getHolder();
        List<Location> locations = new ArrayList<>();

        if (holder instanceof DoubleChest dc) {
            if (dc.getLeftSide() instanceof Chest left) locations.add(left.getLocation());
            if (dc.getRightSide() instanceof Chest right) locations.add(right.getLocation());
        } else if (holder instanceof Chest chest) {
            locations.add(chest.getLocation());
        }

        for (Location loc : locations) {
            if (!plugin.getChestProtectionManager().isProtectedChest(loc)) continue;
            UUID inheritorUUID = plugin.getChestProtectionManager().getInheritor(loc);
            if (inheritorUUID == null || !player.getUniqueId().equals(inheritorUUID)) {
                event.setCancelled(true);
                player.sendMessage("§cThis inheritance chest can only be opened by its inheritor.");
                return;
            }
        }
    }
}
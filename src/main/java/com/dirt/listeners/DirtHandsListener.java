package com.dirt.listeners;

import com.dirt.DirtEconomy;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DirtHandsListener implements Listener {

    private static final int RESERVED_SLOT = 8;

    private final DirtEconomy plugin;
    // Stores the offhand item that was displaced when the player equipped slot 8
    private final Map<UUID, ItemStack> storedOffhand = new HashMap<>();

    public DirtHandsListener(DirtEconomy plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHeldChange(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        PlayerInventory inv = player.getInventory();
        int prev = event.getPreviousSlot();
        int next = event.getNewSlot();

        if (next == RESERVED_SLOT) {
            // Switching TO the empty slot — store offhand and clear it (no dropping)
            ItemStack offhand = inv.getItemInOffHand();
            if (offhand != null && offhand.getType() != Material.AIR) {
                storedOffhand.put(player.getUniqueId(), offhand.clone());
                inv.setItemInOffHand(new ItemStack(Material.AIR));
            }
        } else if (prev == RESERVED_SLOT) {
            // Switching FROM the empty slot — restore stored offhand
            ItemStack stored = storedOffhand.remove(player.getUniqueId());
            if (stored != null && stored.getType() != Material.AIR) {
                ItemStack currentOffhand = inv.getItemInOffHand();
                inv.setItemInOffHand(stored);
                // If something was in the offhand already (shouldn't be, but be safe), move it
                if (currentOffhand != null && currentOffhand.getType() != Material.AIR) {
                    sendToInventoryOrDrop(player, inv, currentOffhand);
                }
            }
        }
    }

    // After a pickup, if the item landed in slot 8, relocate it.
    // If slot 8 is currently held, also redirect items that landed in the offhand to main inventory.
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            PlayerInventory inv = player.getInventory();

            // If holding slot 8, move any item that landed in the offhand to main inventory
            if (player.getInventory().getHeldItemSlot() == RESERVED_SLOT) {
                ItemStack offhandItem = inv.getItemInOffHand();
                if (offhandItem != null && offhandItem.getType() != Material.AIR) {
                    inv.setItemInOffHand(new ItemStack(Material.AIR));
                    sendToInventoryOrDrop(player, inv, offhandItem);
                }
            }

            // If item landed in slot 8, relocate it
            ItemStack inSlot = inv.getItem(RESERVED_SLOT);
            if (inSlot == null || inSlot.getType() == Material.AIR) return;
            inv.setItem(RESERVED_SLOT, new ItemStack(Material.AIR));
            sendToInventoryOrDrop(player, inv, inSlot);
        });
    }

    // Prevent players from manually placing items into slot 8 via inventory click
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (event.getClickedInventory() == null) return;
        if (event.getClickedInventory().getType() != InventoryType.PLAYER) return;
        if (event.getSlot() == RESERVED_SLOT) {
            if (event.getCursor() != null && event.getCursor().getType() != Material.AIR) {
                event.setCancelled(true);
                ItemStack cursor = event.getCursor().clone();
                Player player = (Player) event.getWhoClicked();
                sendToInventoryOrDrop(player, player.getInventory(), cursor);
                event.setCursor(new ItemStack(Material.AIR));
            }
        }
    }

    // Clean up stored offhand on disconnect
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        ItemStack stored = storedOffhand.remove(id);
        // If the player had a stored offhand and disconnects while on slot 8,
        // give it back so it's not lost when they rejoin
        if (stored != null && stored.getType() != Material.AIR) {
            Player player = event.getPlayer();
            PlayerInventory inv = player.getInventory();
            ItemStack currentOffhand = inv.getItemInOffHand();
            if (currentOffhand == null || currentOffhand.getType() == Material.AIR) {
                inv.setItemInOffHand(stored);
            } else {
                sendToInventoryOrDrop(player, inv, stored);
            }
        }
    }

    private void sendToInventoryOrDrop(Player player, PlayerInventory inv, ItemStack item) {
        for (int i = 0; i < 36; i++) {
            if (i == RESERVED_SLOT) continue;
            ItemStack existing = inv.getItem(i);
            if (existing == null || existing.getType() == Material.AIR) {
                inv.setItem(i, item);
                return;
            }
        }
        player.getWorld().dropItem(player.getLocation(), item);
    }
}
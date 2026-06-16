package com.mteco.listeners;

import com.mteco.MTeco;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class LawBookListener implements Listener {
    private final NamespacedKey key;
    private final MTeco plugin;

    public LawBookListener(MTeco plugin) {
        this.plugin = plugin;
        this.key = new NamespacedKey(plugin, "law_book");
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> removeLawBooks(player), 1L);
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        ItemStack item = event.getItemDrop().getItemStack();
        if (isLawBook(item)) {
            event.getItemDrop().remove();
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        removeLawBooks(event.getPlayer());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        event.getDrops().removeIf(this::isLawBook);
    }

    private boolean isLawBook(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(key, PersistentDataType.BYTE);
    }

    private void removeLawBooks(Player player) {
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            if (isLawBook(contents[i])) {
                player.getInventory().setItem(i, null);
            }
        }
    }
}
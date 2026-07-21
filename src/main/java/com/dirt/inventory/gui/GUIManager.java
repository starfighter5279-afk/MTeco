package com.dirt.inventory.gui;

import com.dirt.inventory.InventoryGUI;
import com.dirt.inventory.InventoryHandler;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;

import java.util.HashMap;
import java.util.Map;

public class GUIManager {
    private final JavaPlugin plugin;
    private final Map<Inventory, InventoryHandler> activeInventories = new HashMap<>();

    public GUIManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void openGUI(InventoryGUI gui, Player player) {
        Inventory inv = gui.getInventory();
        gui.decorate(player);
        this.registerHandledInventory(inv, gui);
        player.openInventory(inv);
    }

    public void registerHandledInventory(Inventory inventory, InventoryHandler handler) {
        this.activeInventories.put(inventory, handler);
    }

    public void unregisterInventory(Inventory inventory) {
        this.activeInventories.remove(inventory);
    }

    public boolean isRegistered(Inventory inventory) {
        return this.activeInventories.containsKey(inventory);
    }

    public void handleClick(InventoryClickEvent event) {
        InventoryHandler handler = this.activeInventories.get(event.getInventory());
        if (handler != null) {
            handler.onClick(event);
        }
    }

    public void handleOpen(InventoryOpenEvent event) {
        InventoryHandler handler = this.activeInventories.get(event.getInventory());
        if (handler != null) {
            handler.onOpen(event);
        }
    }

    public void handleClose(InventoryCloseEvent event) {
        Inventory inventory = event.getInventory();
        InventoryHandler handler = this.activeInventories.get(inventory);
        if (handler != null) {
            handler.onClose(event);
            // Delay unregistration by 1 tick — Geyser (Bedrock) fires a close packet
            // immediately followed by a reopen for the same inventory, so unregistering
            // instantly would leave the handler missing when InventoryOpenEvent fires.
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                // Only unregister if the inventory hasn't been re-registered in the meantime
                if (this.activeInventories.get(inventory) == handler) {
                    this.unregisterInventory(inventory);
                }
            }, 1L);
        }
    }

    public void handleDrag(InventoryDragEvent event) {
        InventoryHandler handler = this.activeInventories.get(event.getInventory());
        if (handler != null) {
            event.setCancelled(true);
        }
    }
}
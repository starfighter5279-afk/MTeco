package com.dirt.util;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public class LocationSelectionManager implements Listener {
    private final Map<UUID, Consumer<Location>> pendingSelections = new HashMap<>();

    public void requestLocation(Player player, String prompt, Consumer<Location> callback) {
        player.sendMessage(prompt);
        pendingSelections.put(player.getUniqueId(), callback);
    }

    public boolean hasPendingSelection(UUID uuid) {
        return pendingSelections.containsKey(uuid);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null) return;
        UUID uuid = event.getPlayer().getUniqueId();
        if (!pendingSelections.containsKey(uuid)) return;
        event.setCancelled(true);
        Consumer<Location> callback = pendingSelections.remove(uuid);
        callback.accept(event.getClickedBlock().getLocation());
    }
}
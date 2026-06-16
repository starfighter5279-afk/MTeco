package com.mteco.listeners;

import com.mteco.MTeco;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ProximityChatListener implements Listener {

    private final MTeco plugin;

    public ProximityChatListener(MTeco plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        Player sender = event.getPlayer();
        Location senderLoc = sender.getLocation();
        double radius = plugin.getSettings().getProximityTextChatRadius();
        double radiusSq = radius * radius;

        event.getRecipients().removeIf(recipient -> {
            if (recipient.getUniqueId().equals(sender.getUniqueId())) return false;
            if (!recipient.getWorld().equals(sender.getWorld())) return true;
            return recipient.getLocation().distanceSquared(senderLoc) > radiusSq;
        });
    }
}
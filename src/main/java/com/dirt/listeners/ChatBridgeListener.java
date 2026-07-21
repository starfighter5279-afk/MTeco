package com.dirt.listeners;

import com.dirt.DirtEconomy;
import com.dirt.data.CharacterData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatBridgeListener implements Listener {

    private final DirtEconomy plugin;

    public ChatBridgeListener(DirtEconomy plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        if (plugin.getDiscordBotManager() == null) return;
        Player player = event.getPlayer();
        CharacterData cd = plugin.getCharacterManager().getCharacter(player.getUniqueId());
        String name = cd != null ? cd.getFirstName() + " " + cd.getLastName() : player.getName();
        plugin.getDiscordBotManager().broadcastToDiscord(name, event.getMessage());
    }
}
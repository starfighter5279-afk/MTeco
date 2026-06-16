package com.mteco.util;

import com.mteco.MTeco;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class ChatInputManager implements Listener {
    private final MTeco plugin;
    private final Map<UUID, Consumer<String>> pendingInputs = new ConcurrentHashMap<>();
    private final Map<UUID, MultiInputSession> multiInputSessions = new ConcurrentHashMap<>();

    public ChatInputManager(MTeco plugin) {
        this.plugin = plugin;
    }

    public void requestInput(Player player, String prompt, Consumer<String> callback) {
        pendingInputs.put(player.getUniqueId(), callback);
        player.sendMessage(prompt);
        player.closeInventory();
    }

    public boolean hasPendingInput(UUID uuid) {
        return pendingInputs.containsKey(uuid) || multiInputSessions.containsKey(uuid);
    }

    public void requestMultiInput(Player player, String prompt, Consumer<List<String>> callback) {
        multiInputSessions.put(player.getUniqueId(), new MultiInputSession(callback));
        player.sendMessage(prompt);
        player.closeInventory();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();

        MultiInputSession session = multiInputSessions.get(uuid);
        if (session != null) {
            event.setCancelled(true);
            String message = event.getMessage().trim();
            if (message.equalsIgnoreCase("done")) {
                multiInputSessions.remove(uuid);
                List<String> pages = session.pages;
                plugin.getServer().getScheduler().runTask(plugin, () -> session.callback.accept(pages));
            } else {
                session.pages.add(message);
                event.getPlayer().sendMessage("\u00a7aPage " + session.pages.size() + " added. \u00a7ePaste the next page or type \u00a76done \u00a7eto finish.");
            }
            return;
        }

        if (!pendingInputs.containsKey(uuid)) return;
        event.setCancelled(true);
        Consumer<String> callback = pendingInputs.remove(uuid);
        String message = event.getMessage().trim();
        plugin.getServer().getScheduler().runTask(plugin, () -> callback.accept(message));
    }

    private static class MultiInputSession {
        final Consumer<List<String>> callback;
        final List<String> pages = new ArrayList<>();

        MultiInputSession(Consumer<List<String>> callback) {
            this.callback = callback;
        }
    }
}
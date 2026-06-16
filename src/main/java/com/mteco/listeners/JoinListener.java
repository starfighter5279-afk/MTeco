package com.mteco.listeners;

import com.mteco.MTeco;
import com.mteco.data.CharacterData;
import com.mteco.inventory.impl.CharacterCreationGUI;
import com.mteco.inventory.impl.ChildSelectionGUI;
import com.mteco.util.CurrencyUtil;
import com.mteco.util.DiscordBotManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class JoinListener implements Listener {
    private final MTeco plugin;

    public JoinListener(MTeco plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        var player = event.getPlayer();
        var uuid = player.getUniqueId();

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!plugin.getCharacterManager().hasCharacter(uuid)) {
                plugin.getCharacterManager().initPendingCreation(uuid);
                plugin.getCharacterManager().addForcedCreation(uuid);
                plugin.getGUIManager().openGUI(new CharacterCreationGUI(plugin), player);
            } else {
                CharacterData data = plugin.getCharacterManager().getCharacter(uuid);
                if (data != null && data.getFirstJoinDate() == 0) {
                    data.setFirstJoinDate(System.currentTimeMillis());
                    plugin.getCharacterManager().saveCharacter(data);
                }
                if (data != null && data.isPendingChildSelection()) {
                    player.sendMessage("§eYour spouse accepted your child bearing request. Please select a child for your family!");
                    plugin.getGUIManager().openGUI(new ChildSelectionGUI(plugin, 0), player);
                }
                if (data != null) {
                    handleDailyReward(player, data);
                    checkDiscordLink(player, data);
                }
            }
        }, plugin.getSettings().getJoinDelayTicks());
    }

    private void handleDailyReward(Player player, CharacterData data) {
        double reward = plugin.getSettings().getDailyLoginReward();
        if (reward <= 0) return;
        long now = System.currentTimeMillis();
        long last = data.getLastDailyReward();
        long oneDayMs = 24L * 60L * 60L * 1000L;
        if (now - last < oneDayMs) return;
        data.setLastDailyReward(now);
        plugin.getCharacterManager().saveCharacter(data);
        plugin.getEconomy().depositPlayer(player, reward);
        player.sendMessage("§a§lDaily Reward! §e" + CurrencyUtil.symbol() + String.format("%.0f", reward) + " §ahas been added to your balance.");
    }

    private void checkDiscordLink(Player player, CharacterData data) {
        if (!plugin.getSettings().isDiscordEnabled()) return;
        DiscordBotManager bot = plugin.getDiscordBotManager();
        if (bot == null || !bot.isConnected()) return;
        if (data.getDiscordId() != null) {
            player.sendMessage("§7§o[Discord] Your mailbox is linked to Discord. Notifications will be sent as DMs.");
        } else {
            bot.tryAutoLink(player.getUniqueId(), player.getName());
            player.sendMessage("§e§o[Discord] Link your mailbox to Discord to get notifications on the go! Use §f/mteco link");
        }
    }
}
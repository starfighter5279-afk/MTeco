package com.dirt.listeners;

import com.dirt.DirtEconomy;
import com.dirt.data.CharacterData;
import com.dirt.inventory.impl.DeathScreenGUI;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public class DeathListener implements Listener {
    private final DirtEconomy plugin;

    public DeathListener(DirtEconomy plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!plugin.getLifeConfig().isLifeSystemEnabled()) return;

        Player player = (Player) event.getEntity();
        if (!plugin.getCharacterManager().hasCharacter(player.getUniqueId())) return;

        CharacterData data = plugin.getCharacterManager().getCharacter(player.getUniqueId());
        if (data == null || !data.isAlive()) return;

        String deathMessage = event.getDeathMessage();
        String cause = "unknown causes";
        if (deathMessage != null) {
            String playerName = player.getName();
            if (deathMessage.startsWith(playerName + " ")) {
                cause = deathMessage.substring(playerName.length() + 1);
            } else {
                cause = deathMessage;
            }
        }

        event.setKeepInventory(true);
        event.getDrops().clear();
        event.setKeepLevel(true);
        event.setDroppedExp(0);

        plugin.getLifeManager().setPendingDeathScreen(player.getUniqueId(), cause);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (!plugin.getLifeManager().hasPendingDeathScreen(player.getUniqueId())) return;

        String cause = plugin.getLifeManager().getPendingDeathCause(player.getUniqueId());
        CharacterData data = plugin.getCharacterManager().getCharacter(player.getUniqueId());
        if (data == null) {
            plugin.getLifeManager().consumePendingDeathScreen(player.getUniqueId());
            return;
        }

        String charName = data.getFirstName() + " " + data.getLastName();

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (plugin.getLifeManager().hasPendingDeathScreen(player.getUniqueId())) {
                plugin.getGUIManager().openGUI(new DeathScreenGUI(plugin, charName, cause), player);
            }
        }, 5L);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!plugin.getLifeManager().hasPendingDeathScreen(player.getUniqueId())) return;

        String cause = plugin.getLifeManager().getPendingDeathCause(player.getUniqueId());
        CharacterData data = plugin.getCharacterManager().getCharacter(player.getUniqueId());
        if (data == null) {
            plugin.getLifeManager().consumePendingDeathScreen(player.getUniqueId());
            return;
        }

        String charName = data.getFirstName() + " " + data.getLastName();

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (plugin.getLifeManager().hasPendingDeathScreen(player.getUniqueId())) {
                plugin.getGUIManager().openGUI(new DeathScreenGUI(plugin, charName, cause), player);
            }
        }, plugin.getSettings().getJoinDelayTicks() + 5L);
    }
}
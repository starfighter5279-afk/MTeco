package com.dirt.listeners;

import com.dirt.DirtEconomy;
import com.dirt.data.CriminalRecord;
import com.dirt.data.NationData;
import com.dirt.managers.LawCrimeManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.UUID;

public class CrimeListener implements Listener {
    private final DirtEconomy plugin;

    public CrimeListener(DirtEconomy plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        if (killer == null) return;
        if (plugin.getLawCrimeManager() == null) return;

        NationData killerNation = plugin.getNationManager().getNationByMember(killer.getUniqueId());
        if (killerNation == null) return;

        boolean isNS = killer.getUniqueId().equals(killerNation.getSecurityHeadUUID());
        boolean isEnforcer = killerNation.getEnforcerUUIDs().contains(killer.getUniqueId());
        if (!isNS && !isEnforcer) return;

        CriminalRecord record = plugin.getLawCrimeManager().getActiveRecordForPlayer(victim.getUniqueId(), killerNation.getNationId());
        if (record == null) return;

        record.setServing(true);
        record.setEscaped(false);
        plugin.getLawCrimeManager().saveCriminalRecord(record);
        plugin.getLawCrimeManager().markForJailRespawn(victim.getUniqueId(), killerNation.getNationId());

        if (isEnforcer) {
            killerNation.getEnforcerCriminalsCaught().merge(killer.getUniqueId(), 1, Integer::sum);
            plugin.getNationManager().saveNation(killerNation);
        }

        killer.sendMessage("\u00a7aCriminal apprehended and sent to jail.");
        victim.sendMessage("\u00a7cYou have been apprehended and will respawn in jail.");
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        if (plugin.getLawCrimeManager() == null) return;
        Player player = event.getPlayer();
        UUID nationId = plugin.getLawCrimeManager().consumeJailRespawn(player.getUniqueId());
        if (nationId == null) return;

        Location cell = plugin.getLawCrimeManager().getAvailableCellForCriminal(nationId, player.getUniqueId());
        if (cell != null) {
            event.setRespawnLocation(cell);
            player.sendMessage("\u00a7cYou have been sent to jail.");
        }
    }

    @EventHandler
    public void onBedEnter(PlayerBedEnterEvent event) {
        if (plugin.getLawCrimeManager() == null) return;
        Player player = event.getPlayer();
        CriminalRecord record = plugin.getLawCrimeManager().getServingRecord(player.getUniqueId());
        if (record == null) return;

        String assignedBed = record.getAssignedBedLocation();
        if (assignedBed == null) {
            event.setCancelled(true);
            player.sendMessage("\u00a7cYou have not been assigned a bed.");
            return;
        }

        String clickedBedHead = LawCrimeManager.getBedHeadKey(event.getBed());
        if (!assignedBed.equals(clickedBedHead)) {
            event.setCancelled(true);
            player.sendMessage("\u00a7cThis is not your assigned bed.");
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onBlockBreak(BlockBreakEvent event) {
        if (plugin.getLawCrimeManager() == null) return;
        CriminalRecord record = plugin.getLawCrimeManager().getServingRecord(event.getPlayer().getUniqueId());
        if (record != null) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("\u00a7cYou cannot break blocks while in jail.");
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (plugin.getLawCrimeManager() == null) return;
        CriminalRecord record = plugin.getLawCrimeManager().getServingRecord(event.getPlayer().getUniqueId());
        if (record != null) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("\u00a7cYou cannot place blocks while in jail.");
        }
    }
}
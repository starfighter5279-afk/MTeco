package com.mteco.listeners;

import com.mteco.MTeco;
import com.mteco.data.CriminalRecord;
import com.mteco.data.NationData;
import com.mteco.managers.LawCrimeManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public class CrimeListener implements Listener {
    private final MTeco plugin;

    public CrimeListener(MTeco plugin) {
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
        plugin.getLawCrimeManager().saveCriminalRecord(record);
        plugin.getLawCrimeManager().markForJailRespawn(victim.getUniqueId());

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
        if (!plugin.getLawCrimeManager().consumeJailRespawn(player.getUniqueId())) return;

        NationData nation = plugin.getNationManager().getNationByMember(player.getUniqueId());
        if (nation == null) return;

        Location cell = plugin.getLawCrimeManager().getAvailableCellForCriminal(nation.getNationId(), player.getUniqueId());
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
}
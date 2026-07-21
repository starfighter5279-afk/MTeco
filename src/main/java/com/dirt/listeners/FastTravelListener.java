package com.dirt.listeners;

import com.cryptomorin.xseries.XMaterial;
import com.dirt.DirtEconomy;
import com.dirt.inventory.impl.travel.FastTravelGUI;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Campfire;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class FastTravelListener implements Listener {
    private final DirtEconomy plugin;

    public FastTravelListener(DirtEconomy plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onCampfireInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || !event.getAction().isRightClick()) return;
        Block block = event.getClickedBlock();
        if (block == null || !isLitCampfire(block) || !isHoldingEnderPearl(event.getPlayer())) return;

        event.setCancelled(true);
        Player player = event.getPlayer();
        if (!plugin.getFastTravelManager().isPoint(block)) {
            plugin.getFastTravelManager().createPoint(block, player.getUniqueId());
            player.sendMessage("§aThis campfire is now a public fast travel point.");
            return;
        }

        plugin.getGUIManager().openGUI(new FastTravelGUI(plugin, 0), player);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCampfireBreak(BlockBreakEvent event) {
        if (plugin.getFastTravelManager().isPoint(event.getBlock())) {
            plugin.getFastTravelManager().deletePoint(event.getBlock());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player && plugin.getFastTravelManager().isTransitionProtected(player)) {
            event.setCancelled(true);
        }
    }

    private boolean isLitCampfire(Block block) {
        return XMaterial.matchXMaterial(block.getType()).name().equals("CAMPFIRE")
                && block.getBlockData() instanceof Campfire campfire
                && campfire.isLit();
    }

    private boolean isHoldingEnderPearl(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        return item != null && XMaterial.matchXMaterial(item.getType()).name().equals("ENDER_PEARL");
    }
}
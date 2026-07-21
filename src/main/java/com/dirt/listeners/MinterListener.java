package com.dirt.listeners;

import com.dirt.DirtEconomy;
import com.dirt.data.MinterData;
import com.dirt.data.NationData;
import com.dirt.data.NationPermission;
import com.dirt.inventory.impl.nation.MinterGUI;
import com.dirt.util.NationPermissionUtil;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class MinterListener implements Listener {
    private final DirtEconomy plugin;

    public MinterListener(DirtEconomy plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (plugin.getMinterManager() == null) return;
        MinterData minter = plugin.getMinterManager().getMinterByBlock(event.getBlock().getLocation());
        if (minter == null) return;
        event.setCancelled(true);

        Player player = event.getPlayer();
        NationData nation = plugin.getNationManager().loadNation(minter.getNationId());
        if (nation == null) return;

        boolean canManage = NationPermissionUtil.hasPermission(plugin, nation, player.getUniqueId(), NationPermission.MANAGE_MINTERS);
        if (!canManage) {
            player.sendMessage("\u00a7cYou don't have permission to destroy this minter.");
            return;
        }

        plugin.getMinterManager().deleteMinter(minter.getMinterId());
        player.sendMessage("\u00a7cMinter has been destroyed.");
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (plugin.getMinterManager() == null) return;
        Block block = event.getClickedBlock();
        if (block == null) return;
        MinterData minter = plugin.getMinterManager().getMinterByBlock(block.getLocation());
        if (minter == null) return;
        event.setCancelled(true);

        Player player = event.getPlayer();
        NationData nation = plugin.getNationManager().loadNation(minter.getNationId());
        if (nation == null) return;

        if (!nation.getMemberUUIDs().contains(player.getUniqueId())
                && !player.getUniqueId().equals(nation.getPresidentUUID())) {
            player.sendMessage("\u00a7cYou are not a member of this nation.");
            return;
        }

        plugin.getGUIManager().openGUI(new MinterGUI(plugin, minter.getMinterId()), player);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (plugin.getMinterManager() == null) return;
        Inventory topInv = event.getView().getTopInventory();
        if (!plugin.getMinterManager().isOpenMinterInventory(topInv)) return;

        if (event.getClickedInventory() == topInv && event.getSlot() >= 45) {
            event.setCancelled(true);
            return;
        }

        if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY && event.getClickedInventory() != topInv) {
            ItemStack item = event.getCurrentItem();
            if (item != null && item.getType() != Material.GOLD_INGOT && item.getType() != Material.GOLD_NUGGET) {
                event.setCancelled(true);
            }
            return;
        }

        if (event.getClickedInventory() == topInv && event.getSlot() >= 0 && event.getSlot() <= 44) {
            ItemStack cursor = event.getCursor();
            if (cursor != null && !cursor.getType().isAir()
                    && cursor.getType() != Material.GOLD_INGOT
                    && cursor.getType() != Material.GOLD_NUGGET) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (plugin.getMinterManager() == null) return;
        Inventory inv = event.getInventory();
        UUID minterId = plugin.getMinterManager().getOpenInventoryMinterId(inv);
        if (minterId == null) return;

        Player player = (Player) event.getPlayer();
        int bars = 0, nuggets = 0;

        for (int i = 0; i <= 44; i++) {
            ItemStack item = inv.getItem(i);
            if (item == null) continue;
            if (item.getType() == Material.GOLD_INGOT) {
                bars += item.getAmount();
            } else if (item.getType() == Material.GOLD_NUGGET) {
                nuggets += item.getAmount();
            } else {
                player.getWorld().dropItem(player.getLocation(), item);
            }
        }

        plugin.getMinterManager().addToQueue(minterId, "BAR", bars);
        plugin.getMinterManager().addToQueue(minterId, "NUGGET", nuggets);
        plugin.getMinterManager().unregisterInventory(inv);

        if (bars > 0 || nuggets > 0) {
            player.sendMessage("\u00a7aAdded \u00a76" + bars + "\u00a7a bar(s) and \u00a76" + nuggets + "\u00a7a nugget(s) to the minter queue.");
        }
    }
}
package com.dirt.util;

import com.dirt.DirtEconomy;
import com.dirt.data.BusinessPropertyData;
import com.dirt.data.StockroomData;
import com.dirt.managers.NationManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StockroomSelectionManager implements Listener {

    public static class Session {
        public final UUID businessId;
        public Location corner1;
        public Location corner2;
        public boolean awaitingConfirm = false;

        public Session(UUID businessId) {
            this.businessId = businessId;
        }
    }

    private final DirtEconomy plugin;
    private final Map<UUID, Session> sessions = new HashMap<>();

    public StockroomSelectionManager(DirtEconomy plugin) {
        this.plugin = plugin;
    }

    public void startSelection(Player player, UUID businessId) {
        sessions.put(player.getUniqueId(), new Session(businessId));
        player.getInventory().addItem(buildHoe());
        player.sendMessage("\u00a7eYou received a \u00a76Stockroom Selection Tool\u00a7e.");
        player.sendMessage("\u00a7eLeft-click corner 1, right-click corner 2, then type \u00a76confirm\u00a7e or \u00a7ccancel\u00a7e.");
    }

    private ItemStack buildHoe() {
        ItemStack hoe = new ItemStack(Material.GOLDEN_HOE);
        ItemMeta meta = hoe.getItemMeta();
        if (meta != null) {
            ItemUtil.setDisplayName(meta, "\u00a76Stockroom Selection Tool");
            ItemUtil.setLore(meta, Arrays.asList("\u00a77Left-click corner 1, right-click corner 2."));
            hoe.setItemMeta(meta);
        }
        return hoe;
    }

    private void removeHoe(Player player) {
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item != null && item.getType() == Material.GOLDEN_HOE
                    && item.getItemMeta() != null
                    && ItemUtil.hasDisplayName(item.getItemMeta(), "\u00a76Stockroom Selection Tool")) {
                player.getInventory().setItem(i, null);
                break;
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Session session = sessions.get(player.getUniqueId());
        if (session == null) return;
        if (event.getClickedBlock() == null) return;
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() != Material.GOLDEN_HOE) return;
        if (item.getItemMeta() == null
                || !ItemUtil.hasDisplayName(item.getItemMeta(), "\u00a76Stockroom Selection Tool")) return;
        event.setCancelled(true);

        Location loc = event.getClickedBlock().getLocation();
        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            session.corner1 = loc;
            session.awaitingConfirm = false;
            player.sendMessage("\u00a7aCorner 1 set at (" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + ").");
        } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (session.corner1 == null) {
                player.sendMessage("\u00a7cSet corner 1 first by left-clicking.");
                return;
            }
            if (!session.corner1.getWorld().equals(loc.getWorld())) {
                player.sendMessage("\u00a7cBoth corners must be in the same world.");
                return;
            }
            session.corner2 = loc;
            player.sendMessage("\u00a7aCorner 2 set at (" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + "). Type \u00a76confirm\u00a7a or \u00a7ccancel\u00a7a.");
            session.awaitingConfirm = true;
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        Session session = sessions.get(player.getUniqueId());
        if (session == null || !session.awaitingConfirm) return;
        String msg = event.getMessage().trim().toLowerCase();
        if (!msg.equals("confirm") && !msg.equals("cancel")) return;
        event.setCancelled(true);

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (msg.equals("cancel")) {
                sessions.remove(player.getUniqueId());
                removeHoe(player);
                player.sendMessage("\u00a7cStockroom selection cancelled.");
                return;
            }

            Location c1 = session.corner1;
            Location c2 = session.corner2;
            int minX = Math.min(c1.getBlockX(), c2.getBlockX());
            int minY = Math.min(c1.getBlockY(), c2.getBlockY());
            int minZ = Math.min(c1.getBlockZ(), c2.getBlockZ());
            int maxX = Math.max(c1.getBlockX(), c2.getBlockX());
            int maxY = Math.max(c1.getBlockY(), c2.getBlockY());
            int maxZ = Math.max(c1.getBlockZ(), c2.getBlockZ());

            // Verify entire cuboid is within at least one business property of this business
            UUID businessId = session.businessId;
            String world = c1.getWorld().getName();
            boolean allInside = true;
            outer:
            for (int x = minX; x <= maxX; x += 16) {
                for (int z = minZ; z <= maxZ; z += 16) {
                    int cx = x >> 4;
                    int cz = z >> 4;
                    String key = NationManager.chunkKey(world, cx, cz);
                    BusinessPropertyData prop = plugin.getBusinessManager().getPropertyByChunk(key);
                    if (prop == null || !businessId.equals(prop.getBusinessId())) {
                        allInside = false;
                        break outer;
                    }
                }
            }
            // Also check the max corners
            if (allInside) {
                int[] cornersX = {minX, maxX};
                int[] cornersZ = {minZ, maxZ};
                for (int x : cornersX) {
                    for (int z : cornersZ) {
                        String key = NationManager.chunkKey(world, x >> 4, z >> 4);
                        BusinessPropertyData prop = plugin.getBusinessManager().getPropertyByChunk(key);
                        if (prop == null || !businessId.equals(prop.getBusinessId())) {
                            allInside = false;
                            break;
                        }
                    }
                    if (!allInside) break;
                }
            }

            if (!allInside) {
                player.sendMessage("\u00a7cThe selection must be entirely within this business's property.");
                sessions.remove(player.getUniqueId());
                removeHoe(player);
                return;
            }

            plugin.getChatInputManager().requestInput(player, "\u00a7eEnter a name for this stockroom:", name -> {
                if (name.trim().isEmpty()) {
                    player.sendMessage("\u00a7cName cannot be empty.");
                    return;
                }
                StockroomData sr = new StockroomData();
                sr.setBusinessId(businessId);
                sr.setName(name.trim());
                sr.setWorld(world);
                sr.setMinX(minX); sr.setMinY(minY); sr.setMinZ(minZ);
                sr.setMaxX(maxX); sr.setMaxY(maxY); sr.setMaxZ(maxZ);
                plugin.getShopManager().createStockroom(sr);
                player.sendMessage("\u00a7aStockroom '\u00a76" + sr.getName() + "\u00a7a' created. Chests placed inside become Stock Chests.");
            });

            sessions.remove(player.getUniqueId());
            removeHoe(player);
        });
    }
}
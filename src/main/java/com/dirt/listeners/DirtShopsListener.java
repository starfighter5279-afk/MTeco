package com.dirt.listeners;

import com.dirt.DirtEconomy;
import com.dirt.data.BusinessData;
import com.dirt.data.CharacterData;
import com.dirt.data.ShopData;
import com.dirt.managers.ShopManager;
import com.dirt.util.CurrencyUtil;
import com.dirt.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DirtShopsListener implements Listener {

    private final DirtEconomy plugin;
    private final Map<UUID, UUID> pendingBusinessShopCreation = new HashMap<>();

    public DirtShopsListener(DirtEconomy plugin) {
        this.plugin = plugin;
    }

    public void awaitBusinessShopChest(Player player, UUID businessId) {
        pendingBusinessShopCreation.put(player.getUniqueId(), businessId);
        player.sendMessage("\u00a7ePlace a chest, then \u00a76left-click\u00a7e it while holding the item you want to sell.");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Block clicked = event.getClickedBlock();
        if (clicked == null) return;

        // --- Buying flow: left-click a shop sign ---
        if (event.getAction() == Action.LEFT_CLICK_BLOCK && isSign(clicked.getType())) {
            ShopData shop = plugin.getShopManager().getShopBySign(clicked.getLocation());
            if (shop != null) {
                event.setCancelled(true);
                handleBuyPrompt(player, shop);
                return;
            }
        }

        // --- Create shop flow: left-click a chest with item in hand ---
        if (event.getAction() != Action.LEFT_CLICK_BLOCK) return;
        if (clicked.getType() != Material.CHEST && clicked.getType() != Material.TRAPPED_CHEST) return;

        // Already a shop? ignore
        if (plugin.getShopManager().getShopByChest(clicked.getLocation()) != null) return;

        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand == null || hand.getType() == Material.AIR) return;

        // Don't intercept golden shovel / hoe selection tools
        if (hand.getItemMeta() != null
                && (ItemUtil.hasDisplayName(hand.getItemMeta(), "\u00a76Selection Tool")
                || ItemUtil.hasDisplayName(hand.getItemMeta(), "\u00a76Stockroom Selection Tool"))) return;

        UUID businessId = pendingBusinessShopCreation.remove(player.getUniqueId());
        if (businessId != null) {
            BusinessData biz = plugin.getBusinessManager().loadBusiness(businessId);
            if (biz == null) { player.sendMessage("\u00a7cBusiness not found."); return; }
            if (!biz.getOwnerUUID().equals(player.getUniqueId())) {
                player.sendMessage("\u00a7cOnly the business owner can create shops.");
                return;
            }
            event.setCancelled(true);
            startShopCreation(player, clicked.getLocation(), hand,
                    ShopData.OwnerType.BUSINESS, businessId, biz.getName());
            return;
        }

        // Personal MTC shop: requires character
        CharacterData ch = plugin.getCharacterManager().getCharacter(player.getUniqueId());
        if (ch == null) return;

        event.setCancelled(true);
        String charName = ch.getFirstName() + " " + ch.getLastName();
        startShopCreation(player, clicked.getLocation(), hand,
                ShopData.OwnerType.CHARACTER, player.getUniqueId(), charName);
    }

    private void startShopCreation(Player player, Location chestLoc, ItemStack hand,
                                   ShopData.OwnerType ownerType, UUID ownerUUID, String ownerDisplay) {
        ItemStack template = hand.clone();
        template.setAmount(1);
        String itemName = ShopManager.friendlyName(template);

        plugin.getChatInputManager().requestInput(player,
                "\u00a7eHow much would you like to sell \u00a76" + itemName + "\u00a7e for per unit? (enter a number)",
                priceStr -> {
                    double price;
                    try { price = Double.parseDouble(priceStr.trim()); }
                    catch (NumberFormatException ex) { player.sendMessage("\u00a7cInvalid price."); return; }
                    if (price <= 0) { player.sendMessage("\u00a7cPrice must be greater than zero."); return; }

                    // Place sign on top of chest (or against side if top blocked)
                    Block chestBlock = chestLoc.getBlock();
                    if (chestBlock.getType() != Material.CHEST && chestBlock.getType() != Material.TRAPPED_CHEST) {
                        player.sendMessage("\u00a7cChest was removed before shop creation completed.");
                        return;
                    }
                    Block signBlock = placeShopSign(chestBlock);
                    if (signBlock == null) {
                        player.sendMessage("\u00a7cCould not place sign - clear the space above or beside the chest.");
                        return;
                    }

                    ShopData shop = new ShopData();
                    shop.setOwnerType(ownerType);
                    shop.setOwnerUUID(ownerUUID);
                    shop.setOwnerDisplay(ownerDisplay);
                    shop.setChestLocation(ShopManager.locKey(chestLoc));
                    shop.setSignLocation(ShopManager.locKey(signBlock.getLocation()));
                    shop.setItemTemplate(template);
                    shop.setPricePerUnit(price);
                    plugin.getShopManager().createShop(shop);
                    plugin.getShopManager().updateShopSign(shop);
                    player.sendMessage("\u00a7aShop created for \u00a76" + ShopManager.friendlyName(template)
                            + "\u00a7a at \u00a7e" + CurrencyUtil.symbol() + String.format("%.2f", price) + "\u00a7a per unit.");
                });
    }

    private Block placeShopSign(Block chestBlock) {
        // Try block above
        Block above = chestBlock.getRelative(BlockFace.UP);
        if (above.getType() == Material.AIR || above.getType() == Material.CAVE_AIR) {
            above.setType(Material.OAK_SIGN);
            return above;
        }
        // Try each horizontal face as wall sign
        for (BlockFace face : new BlockFace[]{BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST}) {
            Block side = chestBlock.getRelative(face);
            if (side.getType() == Material.AIR || side.getType() == Material.CAVE_AIR) {
                side.setType(Material.OAK_WALL_SIGN);
                BlockData data = side.getBlockData();
                if (data instanceof WallSign ws) {
                    ws.setFacing(face);
                    side.setBlockData(ws);
                }
                return side;
            }
        }
        return null;
    }

    private void handleBuyPrompt(Player player, ShopData shop) {
        // Don't allow owner to "buy from self"
        if (plugin.getShopManager().isOwnerOrAuthorized(shop, player.getUniqueId())) {
            player.sendMessage("\u00a7eYou own this shop. Right-click the chest to manage stock.");
            return;
        }
        int stock = plugin.getShopManager().countShopStock(shop);
        boolean isBusiness = shop.getOwnerType() == ShopData.OwnerType.BUSINESS;
        int totalAvailable = stock;
        if (isBusiness) {
            // Also count stockroom inventory for display, but actual purchase consumes shop chest after refill
            totalAvailable += plugin.getShopManager().countBusinessStock(shop.getOwnerUUID(), shop.getItemTemplate());
        }
        if (totalAvailable <= 0) {
            player.sendMessage("\u00a7cThis shop is out of stock.");
            return;
        }
        String itemName = ShopManager.friendlyName(shop.getItemTemplate());
        int finalAvailable = totalAvailable;
        plugin.getChatInputManager().requestInput(player,
                "\u00a7eHow many \u00a76" + itemName + "\u00a7e would you like to purchase? (available: \u00a76"
                        + finalAvailable + "\u00a7e, \u00a7e" + CurrencyUtil.symbol() + String.format("%.2f", shop.getPricePerUnit()) + " each)",
                qtyStr -> {
                    int qty;
                    try { qty = Integer.parseInt(qtyStr.trim()); }
                    catch (NumberFormatException ex) { player.sendMessage("\u00a7cInvalid quantity."); return; }
                    if (qty <= 0) { player.sendMessage("\u00a7cQuantity must be positive."); return; }
                    completePurchase(player, shop, qty);
                });
    }

    private void completePurchase(Player player, ShopData shop, int qty) {
        // Refresh shop reference from disk in case it was modified
        ShopData current = plugin.getShopManager().loadShop(shop.getShopId());
        if (current == null) { player.sendMessage("\u00a7cShop no longer exists."); return; }
        shop = current;

        // If business, attempt refill before counting
        if (shop.getOwnerType() == ShopData.OwnerType.BUSINESS) {
            plugin.getShopManager().refillShopFromStock(shop);
        }
        int stock = plugin.getShopManager().countShopStock(shop);
        if (stock <= 0) { player.sendMessage("\u00a7cShop is out of stock."); return; }
        if (qty > stock) {
            player.sendMessage("\u00a7cOnly \u00a76" + stock + "\u00a7c units are available.");
            return;
        }

        double totalCost = shop.getPricePerUnit() * qty;
        if (!plugin.getEconomy().has(player, totalCost)) {
            player.sendMessage("\u00a7cYou cannot afford \u00a7e" + CurrencyUtil.symbol() + String.format("%.2f", totalCost) + "\u00a7c.");
            return;
        }

        // Remove items from chest
        Location chestLoc = ShopManager.parseLoc(shop.getChestLocation());
        if (chestLoc == null || !(chestLoc.getBlock().getState() instanceof Chest chest)) {
            player.sendMessage("\u00a7cShop chest missing.");
            return;
        }
        Inventory chestInv = chest.getInventory();
        int remaining = qty;
        ItemStack template = shop.getItemTemplate();
        for (int i = 0; i < chestInv.getSize() && remaining > 0; i++) {
            ItemStack s = chestInv.getItem(i);
            if (s == null || !s.isSimilar(template)) continue;
            int take = Math.min(s.getAmount(), remaining);
            s.setAmount(s.getAmount() - take);
            if (s.getAmount() <= 0) chestInv.setItem(i, null);
            else chestInv.setItem(i, s);
            remaining -= take;
        }
        if (remaining > 0) {
            player.sendMessage("\u00a7cShop did not have enough stock to fulfil the order.");
            return;
        }

        // Pay
        plugin.getEconomy().withdrawPlayer(player, totalCost);
        if (shop.getOwnerType() == ShopData.OwnerType.CHARACTER) {
            plugin.getEconomy().depositPlayer(Bukkit.getOfflinePlayer(shop.getOwnerUUID()), totalCost);
        } else {
            BusinessData biz = plugin.getBusinessManager().loadBusiness(shop.getOwnerUUID());
            if (biz != null) plugin.getBusinessManager().depositToTreasury(biz, totalCost);
        }
        plugin.getShopManager().recordSale(shop, totalCost);

        // Grant items
        ItemStack toGive = template.clone();
        toGive.setAmount(qty);
        Map<Integer, ItemStack> overflow = player.getInventory().addItem(toGive);
        for (ItemStack drop : overflow.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), drop);
        }

        player.sendMessage("\u00a7aPurchased \u00a76" + qty + "x " + ShopManager.friendlyName(template)
                + "\u00a7a for \u00a7e" + CurrencyUtil.symbol() + String.format("%.2f", totalCost) + "\u00a7a.");

        // Refill if business
        if (shop.getOwnerType() == ShopData.OwnerType.BUSINESS) {
            plugin.getShopManager().refillShopFromStock(shop);
        }
        plugin.getShopManager().updateShopSign(shop);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onChestOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        InventoryHolder holder = event.getInventory().getHolder();
        Location loc = null;
        if (holder instanceof Chest c) loc = c.getLocation();
        else if (holder instanceof DoubleChest dc) {
            if (dc.getLeftSide() instanceof Chest left) loc = left.getLocation();
        }
        if (loc == null) return;

        ShopData shop = plugin.getShopManager().getShopByChest(loc);
        if (shop == null) return;

        if (!plugin.getShopManager().isOwnerOrAuthorized(shop, player.getUniqueId())) {
            event.setCancelled(true);
            player.sendMessage("\u00a7cOnly the shop owner can open this chest.");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChestClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        InventoryHolder holder = event.getInventory().getHolder();
        Location loc = null;
        if (holder instanceof Chest c) loc = c.getLocation();
        else if (holder instanceof DoubleChest dc) {
            if (dc.getLeftSide() instanceof Chest left) loc = left.getLocation();
        }
        if (loc == null) return;
        ShopData shop = plugin.getShopManager().getShopByChest(loc);
        if (shop != null) {
            plugin.getShopManager().updateShopSign(shop);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Location loc = block.getLocation();

        // Shop chest break
        ShopData shop = plugin.getShopManager().getShopByChest(loc);
        if (shop != null) {
            if (!plugin.getShopManager().isOwnerOrAuthorized(shop, player.getUniqueId())) {
                event.setCancelled(true);
                player.sendMessage("\u00a7cYou cannot break another player's shop chest.");
                return;
            }
            // Owner broke chest: remove shop + sign
            Location signLoc = ShopManager.parseLoc(shop.getSignLocation());
            if (signLoc != null && isSign(signLoc.getBlock().getType())) signLoc.getBlock().setType(Material.AIR);
            plugin.getShopManager().deleteShop(shop.getShopId());
            player.sendMessage("\u00a7eShop removed.");
            return;
        }

        // Shop sign break
        if (isSign(block.getType())) {
            ShopData signShop = plugin.getShopManager().getShopBySign(loc);
            if (signShop != null) {
                if (!plugin.getShopManager().isOwnerOrAuthorized(signShop, player.getUniqueId())) {
                    event.setCancelled(true);
                    player.sendMessage("\u00a7cYou cannot break another player's shop sign.");
                    return;
                }
                plugin.getShopManager().deleteShop(signShop.getShopId());
                player.sendMessage("\u00a7eShop removed.");
            }
        }
    }

    private boolean isSign(Material m) {
        if (m == null) return false;
        String n = m.name();
        return n.endsWith("_SIGN") || n.endsWith("_WALL_SIGN");
    }
}
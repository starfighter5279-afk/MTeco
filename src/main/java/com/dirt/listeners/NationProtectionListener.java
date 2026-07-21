package com.dirt.listeners;

import com.dirt.DirtEconomy;
import com.dirt.data.CharacterData;
import com.dirt.data.ConservationAreaData;
import com.dirt.data.NationData;
import com.dirt.data.PropertyData;
import com.dirt.data.RegionData;
import com.dirt.data.WarData;
import com.dirt.managers.NationManager;
import com.dirt.data.BusinessData;
import com.dirt.data.BusinessPropertyData;
import com.dirt.util.CurrencyUtil;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import com.cryptomorin.xseries.particles.XParticle;

public class NationProtectionListener implements Listener {
    private final DirtEconomy plugin;

    public NationProtectionListener(DirtEconomy plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("DirtNations.admin")) return;
        Block block = event.getBlock();
        String chunkKey = NationManager.chunkKey(block.getWorld().getName(), block.getChunk().getX(), block.getChunk().getZ());
        RegionData region = plugin.getNationManager().getRegionByChunk(chunkKey);
        if (region == null) return;
        NationData nation = plugin.getNationManager().loadNation(region.getNationId());
        if (nation == null) return;
        if (!nation.getMemberUUIDs().contains(player.getUniqueId())) {
            if (plugin.getWarManager() != null && isWarAttacker(player, region)) return;
            event.setCancelled(true);
            player.sendMessage("\u00a7cYou cannot break blocks in " + nation.getColor1() + nation.getName() + "\u00a7c territory.");
            return;
        }
        ConservationAreaData breakArea = plugin.getNationManager().getConservationAreaByChunk(chunkKey);
        if (breakArea != null && !player.getUniqueId().equals(nation.getPresidentUUID())) {
            event.setCancelled(true);
            player.sendMessage("\u00a7cOnly the President can break blocks in the \u00a76" + breakArea.getName() + "\u00a7c Conservation Area.");
            return;
        }
        if (plugin.isDirtBusinessEnabled()) {
            BusinessPropertyData bizProp = plugin.getBusinessManager().getPropertyByChunk(chunkKey);
            if (bizProp != null) {
                BusinessData biz = plugin.getBusinessManager().loadBusiness(bizProp.getBusinessId());
                if (biz != null && !biz.getOwnerUUID().equals(player.getUniqueId()) && !biz.getEmployeeUUIDs().contains(player.getUniqueId())) {
                    event.setCancelled(true);
                    player.sendMessage("\u00a7cYou cannot break blocks on \u00a76" + biz.getName() + "\u00a7c's property.");
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("DirtNations.admin")) return;
        Block block = event.getBlock();
        String chunkKey = NationManager.chunkKey(block.getWorld().getName(), block.getChunk().getX(), block.getChunk().getZ());
        RegionData region = plugin.getNationManager().getRegionByChunk(chunkKey);
        if (region == null) return;
        NationData nation = plugin.getNationManager().loadNation(region.getNationId());
        if (nation == null) return;
        if (!nation.getMemberUUIDs().contains(player.getUniqueId())) {
            if (plugin.getWarManager() != null && isWarAttacker(player, region)) return;
            event.setCancelled(true);
            player.sendMessage("\u00a7cYou cannot place blocks in " + nation.getColor1() + nation.getName() + "\u00a7c territory.");
            return;
        }
        ConservationAreaData placeArea = plugin.getNationManager().getConservationAreaByChunk(chunkKey);
        if (placeArea != null && !player.getUniqueId().equals(nation.getPresidentUUID())) {
            event.setCancelled(true);
            player.sendMessage("\u00a7cOnly the President can place blocks in the \u00a76" + placeArea.getName() + "\u00a7c Conservation Area.");
            return;
        }
        if (plugin.isDirtBusinessEnabled()) {
            BusinessPropertyData bizProp = plugin.getBusinessManager().getPropertyByChunk(chunkKey);
            if (bizProp != null) {
                BusinessData biz = plugin.getBusinessManager().loadBusiness(bizProp.getBusinessId());
                if (biz != null && !biz.getOwnerUUID().equals(player.getUniqueId()) && !biz.getEmployeeUUIDs().contains(player.getUniqueId())) {
                    event.setCancelled(true);
                    player.sendMessage("\u00a7cYou cannot place blocks on \u00a76" + biz.getName() + "\u00a7c's property.");
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getTo() == null) return;
        int fromCX = event.getFrom().getBlockX() >> 4;
        int fromCZ = event.getFrom().getBlockZ() >> 4;
        int toCX = event.getTo().getBlockX() >> 4;
        int toCZ = event.getTo().getBlockZ() >> 4;
        if (fromCX == toCX && fromCZ == toCZ) return;

        Player player = event.getPlayer();
        String worldName = player.getWorld().getName();
        String chunkKey = NationManager.chunkKey(worldName, toCX, toCZ);
        RegionData region = plugin.getNationManager().getRegionByChunk(chunkKey);
        if (region != null) {
            showRegionBorders(player, region);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onSignChange(SignChangeEvent event) {
        String line0 = event.getLine(0);
        if (line0 == null || !line0.trim().toLowerCase().contains("for sale")) return;

        Player player = event.getPlayer();
        Block block = event.getBlock();
        String chunkKey = NationManager.chunkKey(block.getWorld().getName(), block.getChunk().getX(), block.getChunk().getZ());

        PropertyData prop = plugin.getNationManager().getPropertyByChunk(chunkKey);
        if (prop == null) return;
        if (!prop.isForSale() || prop.getSaleSignLocation() != null) return;

        boolean isOwner = prop.getOwnerUUID().equals(player.getUniqueId());
        if (!isOwner) {
            NationData asNation = plugin.getNationManager().loadNation(prop.getOwnerUUID());
            if (asNation != null && player.getUniqueId().equals(asNation.getPresidentUUID())) {
                isOwner = true;
            }
        }
        if (!isOwner) return;

        CharacterData sellerChar = plugin.getCharacterManager().getCharacter(player.getUniqueId());
        String sellerName = sellerChar != null ? sellerChar.getFirstName() + " " + sellerChar.getLastName() : player.getName();

        event.setLine(0, "\u00a74[For Sale]");
        event.setLine(1, "\u00a77By: \u00a7f" + (sellerName.length() > 12 ? sellerName.substring(0, 12) : sellerName));
        event.setLine(2, "\u00a77" + CurrencyUtil.symbol() + String.format("%.0f", prop.getSalePrice()));
        event.setLine(3, "\u00a7aRight-click");

        String locKey = block.getWorld().getName() + "," + block.getX() + "," + block.getY() + "," + block.getZ();
        prop.setSaleSignLocation(locKey);
        plugin.getNationManager().saveProperty(prop);

        player.sendMessage("\u00a7aSale sign registered! Players can right-click to buy or make an offer.");
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null) return;
        if (!block.getType().name().contains("SIGN")) return;

        String locKey = block.getWorld().getName() + "," + block.getX() + "," + block.getY() + "," + block.getZ();
        PropertyData prop = plugin.getNationManager().getPropertyBySaleSignLocation(locKey);
        if (prop == null) return;

        Player buyer = event.getPlayer();
        CharacterData buyerChar = plugin.getCharacterManager().getCharacter(buyer.getUniqueId());
        if (buyerChar == null) { buyer.sendMessage("\u00a7cYou need an active character to purchase property."); return; }

        RegionData region = plugin.getNationManager().loadRegion(prop.getRegionId());
        if (region == null) return;
        NationData nation = plugin.getNationManager().loadNation(region.getNationId());
        if (nation == null) return;
        if (!nation.getMemberUUIDs().contains(buyer.getUniqueId())) {
            buyer.sendMessage("\u00a7cOnly members of " + nation.getColor1() + nation.getName() + "\u00a7c can purchase property here.");
            return;
        }
        if (buyer.getUniqueId().equals(prop.getOwnerUUID())) {
            buyer.sendMessage("\u00a7cYou already own this property.");
            return;
        }

        NationData ownerNation = plugin.getNationManager().loadNation(prop.getOwnerUUID());
        boolean isGovProperty = ownerNation != null;

        CharacterData sellerChar = isGovProperty ? null : plugin.getCharacterManager().getCharacter(prop.getOwnerUUID());
        String sellerName = isGovProperty ? (ownerNation.getName() + " Gov") : (sellerChar != null ? sellerChar.getFirstName() + " " + sellerChar.getLastName() : "Unknown");
        double price = prop.getSalePrice();

        plugin.getChatInputManager().requestInput(buyer,
                "\u00a7e" + sellerName + " is selling this property for \u00a7a" + CurrencyUtil.symbol() + String.format("%.2f", price) +
                "\u00a7e. Type \u00a76buy\u00a7e to purchase, type an \u00a76amount\u00a7e to make an offer, or \u00a7ccancel\u00a7e.",
                answer -> {
                    if (answer.equalsIgnoreCase("cancel")) {
                        buyer.sendMessage("\u00a7cCancelled.");
                        return;
                    }
                    if (answer.equalsIgnoreCase("buy")) {
                        if (!plugin.getEconomy().has(buyer, price)) {
                            buyer.sendMessage("\u00a7cYou cannot afford this property (\u00a7a" + CurrencyUtil.symbol() + String.format("%.2f", price) + "\u00a7c needed).");
                            return;
                        }
                        plugin.getEconomy().withdrawPlayer(buyer, price);

                        if (isGovProperty) {
                            plugin.getNationManager().depositToTreasury(ownerNation.getName(), price);
                        } else {
                            plugin.getEconomy().depositPlayer(plugin.getServer().getOfflinePlayer(prop.getOwnerUUID()), price);
                        }

                        java.util.UUID previousOwner = prop.getOwnerUUID();
                        prop.setForSale(false);
                        prop.setOwnerUUID(buyer.getUniqueId());
                        String signLocKey = prop.getSaleSignLocation();
                        prop.setSaleSignLocation(null);
                        prop.getOffers().clear();
                        plugin.getNationManager().saveProperty(prop);

                        if (signLocKey != null) removeSaleSign(signLocKey);

                        if (isGovProperty) {
                            ownerNation.getGovernmentPropertyIds().remove(prop.getPropertyId());
                            plugin.getNationManager().saveNation(ownerNation);
                        }

                        buyer.sendMessage("\u00a7aProperty purchased for \u00a7e" + CurrencyUtil.symbol() + String.format("%.2f", price) + "\u00a7a!");
                        if (!isGovProperty) {
                            Player seller = plugin.getServer().getPlayer(previousOwner);
                            if (seller != null) {
                                seller.sendMessage("\u00a7eYour property was sold to \u00a7f" + buyer.getName() + "\u00a7e for \u00a7a" + CurrencyUtil.symbol() + String.format("%.2f", price) + "\u00a7e.");
                            }
                        }
                    } else {
                        try {
                            double offerAmount = Double.parseDouble(answer);
                            if (offerAmount <= 0) { buyer.sendMessage("\u00a7cOffer must be a positive amount."); return; }
                            prop.getOffers().put(buyer.getUniqueId(), offerAmount);
                            plugin.getNationManager().saveProperty(prop);
                            buyer.sendMessage("\u00a7aOffer of \u00a7e" + CurrencyUtil.symbol() + String.format("%.2f", offerAmount) + "\u00a7a submitted!");

                            if (!isGovProperty) {
                                Player seller = plugin.getServer().getPlayer(prop.getOwnerUUID());
                                if (seller != null) {
                                    seller.sendMessage("\u00a7eYou received a new offer of \u00a7a" + CurrencyUtil.symbol() + String.format("%.2f", offerAmount) + "\u00a7e on your property!");
                                }
                            } else if (ownerNation.getPresidentUUID() != null) {
                                Player president = plugin.getServer().getPlayer(ownerNation.getPresidentUUID());
                                if (president != null) {
                                    president.sendMessage("\u00a7eA new offer of \u00a7a" + CurrencyUtil.symbol() + String.format("%.2f", offerAmount) + "\u00a7e was received on a government property!");
                                }
                            }
                        } catch (NumberFormatException ex) {
                            buyer.sendMessage("\u00a7cInvalid input. Type 'buy', an amount, or 'cancel'.");
                        }
                    }
                }
        );
    }

    private boolean isWarAttacker(Player player, RegionData targetRegion) {
        for (WarData war : plugin.getWarManager().getWarsByDefendingNation(targetRegion.getNationId())) {
            if (!war.getTargetRegionIds().contains(targetRegion.getRegionId())) continue;
            if (plugin.getWarManager().getAttackerMembers(war).contains(player.getUniqueId())) return true;
        }
        return false;
    }

    private void showRegionBorders(Player player, RegionData region) {
        World world = player.getWorld();
        int playerCX = player.getLocation().getBlockX() >> 4;
        int playerCZ = player.getLocation().getBlockZ() >> 4;
        java.util.Set<String> chunkSet = new java.util.HashSet<>(region.getClaimedChunks());

        for (String chunkKey : region.getClaimedChunks()) {
            String[] parts = chunkKey.split(",");
            if (parts.length < 3 || !parts[0].equals(world.getName())) continue;
            int cx, cz;
            try {
                cx = Integer.parseInt(parts[1]);
                cz = Integer.parseInt(parts[2]);
            } catch (NumberFormatException e) { continue; }

            if (Math.abs(cx - playerCX) > 4 || Math.abs(cz - playerCZ) > 4) continue;

            if (!chunkSet.contains(NationManager.chunkKey(world.getName(), cx, cz - 1))) {
                spawnBorderLine(player, world, cx * 16, player.getLocation().getY(), cz * 16, true);
            }
            if (!chunkSet.contains(NationManager.chunkKey(world.getName(), cx, cz + 1))) {
                spawnBorderLine(player, world, cx * 16, player.getLocation().getY(), (cz + 1) * 16, true);
            }
            if (!chunkSet.contains(NationManager.chunkKey(world.getName(), cx - 1, cz))) {
                spawnBorderLine(player, world, cx * 16, player.getLocation().getY(), cz * 16, false);
            }
            if (!chunkSet.contains(NationManager.chunkKey(world.getName(), cx + 1, cz))) {
                spawnBorderLine(player, world, (cx + 1) * 16, player.getLocation().getY(), cz * 16, false);
            }
        }
    }

    private void spawnBorderLine(Player player, World world, int startX, double y, int startZ, boolean varyX) {
        final Particle.DustOptions dust = new Particle.DustOptions(Color.RED, 1.0f);
        for (int i = 0; i <= 16; i += 2) {
            double px = varyX ? (startX + i) : startX;
            double pz = varyX ? startZ : (startZ + i);
            for (int dy = 0; dy <= 2; dy++) {
                final double finalPx = px;
                final double finalPz = pz;
                final double finalY = y;
                final int finalDy = dy;
                XParticle.of("REDSTONE").ifPresent(p -> player.spawnParticle(p.get(), finalPx, finalY + finalDy, finalPz, 1, dust));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (!plugin.getSettings().isMobPermissionsEnabled()) return;
        if (event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.CUSTOM
                || event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.SPAWNER_EGG
                || event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.COMMAND) return;

        String mobType = event.getEntityType().name();
        org.bukkit.Location loc = event.getLocation();
        String chunkKey = NationManager.chunkKey(loc.getWorld().getName(), loc.getBlockX() >> 4, loc.getBlockZ() >> 4);

        PropertyData prop = plugin.getNationManager().getPropertyByChunk(chunkKey);
        if (prop != null && prop.getDeniedMobs().contains(mobType)) {
            event.setCancelled(true);
            return;
        }

        RegionData region = plugin.getNationManager().getRegionByChunk(chunkKey);
        if (region != null && region.getDeniedMobs().contains(mobType)) {
            event.setCancelled(true);
        }
    }

    private void removeSaleSign(String locKey) {
        try {
            String[] parts = locKey.split(",");
            if (parts.length < 4) return;
            World world = org.bukkit.Bukkit.getWorld(parts[0]);
            if (world == null) return;
            int x = Integer.parseInt(parts[1]);
            int y = Integer.parseInt(parts[2]);
            int z = Integer.parseInt(parts[3]);
            Block block = world.getBlockAt(x, y, z);
            if (block.getType().name().contains("SIGN")) block.setType(Material.AIR);
        } catch (Exception ignored) {}
    }
}
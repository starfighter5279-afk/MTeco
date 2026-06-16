package com.mteco.util;

import com.mteco.MTeco;
import com.mteco.data.BusinessData;
import com.mteco.data.BusinessPropertyData;
import com.mteco.data.NationData;
import com.mteco.data.PropertyData;
import com.mteco.data.PropertyPurchaseRequest;
import com.mteco.data.RegionData;
import com.mteco.data.JailData;
import com.mteco.managers.LawCrimeManager;
import com.mteco.managers.NationManager;
import com.mteco.util.CurrencyUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ChunkSelectionManager implements Listener {

    public enum SelectionMode { CLAIM_REGION, BUY_PROPERTY, BUSINESS_PROPERTY, CONSERVE_AREA, GOVERNMENT_PROPERTY, CREATE_JAIL, CREATE_CELL, CREATE_ROOM, EXTEND_PROPERTY, EXTEND_BUSINESS_PROPERTY }

    public static class SelectionSession {
        public final SelectionMode mode;
        public UUID regionId;
        public UUID businessId;
        public UUID nationId;
        public UUID propertyId;
        public int[] corner1;
        public int[] corner2;
        public String world;
        public boolean autoMode = false;
        public boolean awaitingConfirm = false;
        public String jailName;
        public UUID jailId;
        public int[] blockCorner1;
        public int[] blockCorner2;
        public boolean governmentExtend = false;

        public SelectionSession(SelectionMode mode, UUID regionId) {
            this.mode = mode;
            this.regionId = regionId;
        }
    }

    private final MTeco plugin;
    private final Map<UUID, SelectionSession> sessions = new HashMap<>();
    private final Map<UUID, UUID> targetedRegions = new HashMap<>();

    public ChunkSelectionManager(MTeco plugin) {
        this.plugin = plugin;
        this.maxRegionChunks = plugin.getSettings().getMaxRegionChunks();
    }

    public void setTargetedRegion(UUID playerUUID, UUID regionId) {
        targetedRegions.put(playerUUID, regionId);
    }

    public UUID getTargetedRegion(UUID playerUUID) {
        return targetedRegions.get(playerUUID);
    }

    public boolean hasSession(UUID playerUUID) {
        return sessions.containsKey(playerUUID);
    }

    public void cancelSession(UUID playerUUID) {
        sessions.remove(playerUUID);
    }

    private final int maxRegionChunks;

    public void startManualClaim(Player player, UUID regionId, int radius) {
        RegionData region = plugin.getNationManager().loadRegion(regionId);
        if (region == null) { player.sendMessage("\u00a7cRegion not found."); return; }
        int current = region.getClaimedChunks().size();
        if (current >= maxRegionChunks) {
            player.sendMessage("\u00a7cRegion \u00a76" + region.getName() + "\u00a7c has reached the maximum of \u00a7e" + maxRegionChunks + "\u00a7c chunks.");
            return;
        }

        if (radius > 0) {
            int cx = player.getLocation().getBlockX() >> 4;
            int cz = player.getLocation().getBlockZ() >> 4;
            String worldName = player.getWorld().getName();
            int added = 0;
            int skipped = 0;
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (current + added >= maxRegionChunks) { skipped++; continue; }
                    String ck = NationManager.chunkKey(worldName, cx + dx, cz + dz);
                    if (!region.getClaimedChunks().contains(ck)) {
                        RegionData existing = plugin.getNationManager().getRegionByChunk(ck);
                        if (existing != null) { skipped++; continue; }
                        region.getClaimedChunks().add(ck);
                        added++;
                    }
                }
            }
            plugin.getNationManager().saveRegion(region);
            player.sendMessage("\u00a7a" + added + " chunk(s) claimed to region \u00a76" + region.getName() + "\u00a7a." +
                    (skipped > 0 ? " \u00a7e" + skipped + " chunk(s) skipped (limit or overlap)." : ""));
            return;
        }

        SelectionSession session = new SelectionSession(SelectionMode.CLAIM_REGION, regionId);
        sessions.put(player.getUniqueId(), session);
        player.getInventory().addItem(buildGoldenShovel());
        player.sendMessage("\u00a7eYou have been given a \u00a76Golden Shovel\u00a7e.");
        player.sendMessage("\u00a7eLeft-click for the first corner, right-click for the second corner, then type \u00a76confirm\u00a7e.");
    }

    public void startAutoClaimForRegion(Player player, UUID regionId) {
        SelectionSession session = new SelectionSession(SelectionMode.CLAIM_REGION, regionId);
        session.autoMode = true;
        sessions.put(player.getUniqueId(), session);
        RegionData region = plugin.getNationManager().loadRegion(regionId);
        String name = region != null ? region.getName() : regionId.toString();
        player.sendMessage("\u00a7eAuto-claim enabled for region \u00a76" + name + "\u00a7e. Walk over chunks to claim them. Use \u00a76/mtr auto off\u00a7e to stop.");
    }

    public void stopAutoClaim(Player player) {
        SelectionSession session = sessions.get(player.getUniqueId());
        if (session != null && session.autoMode) {
            sessions.remove(player.getUniqueId());
            player.sendMessage("\u00a7eAuto-claim disabled.");
        } else {
            player.sendMessage("\u00a7cYou do not have auto-claim active.");
        }
    }

    public void startPropertySelection(Player player, UUID regionId) {
        SelectionSession session = new SelectionSession(SelectionMode.BUY_PROPERTY, regionId);
        sessions.put(player.getUniqueId(), session);
        player.getInventory().addItem(buildGoldenShovel());
        player.sendMessage("\u00a7eYou have been given a \u00a76Golden Shovel\u00a7e.");
        player.sendMessage("\u00a7eLeft-click for the first corner, right-click for the second corner, then type \u00a76confirm\u00a7e.");
    }

    public void startBusinessPropertySelection(Player player, UUID businessId) {
        SelectionSession session = new SelectionSession(SelectionMode.BUSINESS_PROPERTY, null);
        session.businessId = businessId;
        sessions.put(player.getUniqueId(), session);
        player.getInventory().addItem(buildGoldenShovel());
        player.sendMessage("\u00a7eYou have been given a \u00a76Golden Shovel\u00a7e.");
        player.sendMessage("\u00a7eLeft-click for the first corner, right-click for the second corner, then type \u00a76confirm\u00a7e.");
    }

    public void startConservationSelection(Player player, UUID nationId) {
        SelectionSession session = new SelectionSession(SelectionMode.CONSERVE_AREA, null);
        session.nationId = nationId;
        sessions.put(player.getUniqueId(), session);
        player.getInventory().addItem(buildGoldenShovel());
        player.sendMessage("\u00a7eYou have been given a \u00a76Golden Shovel\u00a7e.");
        player.sendMessage("\u00a7eLeft-click for the first corner, right-click for the second corner, then type \u00a76confirm\u00a7e.");
    }

    public void startGovernmentPropertySelection(Player player, UUID nationId) {
        SelectionSession session = new SelectionSession(SelectionMode.GOVERNMENT_PROPERTY, null);
        session.nationId = nationId;
        sessions.put(player.getUniqueId(), session);
        player.getInventory().addItem(buildGoldenShovel());
        player.sendMessage("\u00a7eYou have been given a \u00a76Golden Shovel\u00a7e.");
        player.sendMessage("\u00a7eLeft-click for the first corner, right-click for the second corner, then type \u00a76confirm\u00a7e.");
    }

    public void startJailCreation(Player player, UUID nationId, String jailName) {
        SelectionSession session = new SelectionSession(SelectionMode.CREATE_JAIL, null);
        session.nationId = nationId;
        session.jailName = jailName;
        sessions.put(player.getUniqueId(), session);
        player.getInventory().addItem(buildGoldenShovel());
        player.sendMessage("\u00a7eYou have been given a \u00a76Golden Shovel\u00a7e.");
        player.sendMessage("\u00a7eLeft-click for the first corner, right-click for the second corner, then type \u00a76confirm\u00a7e.");
    }

    public void startCellCreation(Player player, UUID jailId) {
        SelectionSession session = new SelectionSession(SelectionMode.CREATE_CELL, null);
        session.jailId = jailId;
        sessions.put(player.getUniqueId(), session);
        player.getInventory().addItem(buildGoldenShovel());
        player.sendMessage("\u00a7eYou have been given a \u00a76Golden Shovel\u00a7e.");
        player.sendMessage("\u00a7eLeft-click for the first corner, right-click for the second corner, then type \u00a76confirm\u00a7e.");
    }

    public void startRoomCreation(Player player, UUID propertyId) {
        SelectionSession session = new SelectionSession(SelectionMode.CREATE_ROOM, null);
        session.propertyId = propertyId;
        sessions.put(player.getUniqueId(), session);
        player.getInventory().addItem(buildGoldenShovel());
        player.sendMessage("\u00a7eYou have been given a \u00a76Golden Shovel\u00a7e.");
        player.sendMessage("\u00a7eSelect two corners of the room. The ceiling will be detected automatically.");
        player.sendMessage("\u00a7eLeft-click for the first corner, right-click for the second corner, then type \u00a76confirm\u00a7e.");
    }

    public void startExtendProperty(Player player, UUID propertyId, boolean government, UUID nationId) {
        PropertyData prop = plugin.getNationManager().loadProperty(propertyId);
        if (prop == null) { player.sendMessage("\u00a7cProperty not found."); return; }
        SelectionSession session = new SelectionSession(SelectionMode.EXTEND_PROPERTY, prop.getRegionId());
        session.propertyId = propertyId;
        session.governmentExtend = government;
        session.nationId = nationId;
        sessions.put(player.getUniqueId(), session);
        player.getInventory().addItem(buildGoldenShovel());
        player.sendMessage("\u00a7eSelect additional chunks to add to your property.");
        player.sendMessage("\u00a7eLeft-click for the first corner, right-click for the second corner, then type \u00a76confirm\u00a7e.");
    }

    public void startExtendBusinessProperty(Player player, UUID businessId, UUID propertyId) {
        BusinessPropertyData prop = plugin.getBusinessManager().loadProperty(propertyId);
        if (prop == null) { player.sendMessage("\u00a7cProperty not found."); return; }
        SelectionSession session = new SelectionSession(SelectionMode.EXTEND_BUSINESS_PROPERTY, null);
        session.businessId = businessId;
        session.propertyId = propertyId;
        sessions.put(player.getUniqueId(), session);
        player.getInventory().addItem(buildGoldenShovel());
        player.sendMessage("\u00a7eSelect additional chunks for this business property.");
        player.sendMessage("\u00a7eLeft-click for the first corner, right-click for the second corner, then type \u00a76confirm\u00a7e.");
    }

    private ItemStack buildGoldenShovel() {
        ItemStack shovel = new ItemStack(Material.GOLDEN_SHOVEL);
        ItemMeta meta = shovel.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("\u00a76Selection Tool");
            meta.setLore(Arrays.asList("\u00a77Left-click for corner 1, right-click for corner 2."));
            shovel.setItemMeta(meta);
        }
        return shovel;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        SelectionSession session = sessions.get(player.getUniqueId());
        if (session == null || session.autoMode) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_AIR
                && event.getAction() != Action.LEFT_CLICK_BLOCK && event.getAction() != Action.LEFT_CLICK_AIR) return;
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() != Material.GOLDEN_SHOVEL) return;
        if (item.getItemMeta() == null || !"\u00a76Selection Tool".equals(item.getItemMeta().getDisplayName())) return;
        event.setCancelled(true);

        boolean isLeftClick = event.getAction() == Action.LEFT_CLICK_BLOCK || event.getAction() == Action.LEFT_CLICK_AIR;
        boolean blockLevel = session.mode == SelectionMode.CREATE_JAIL || session.mode == SelectionMode.CREATE_CELL || session.mode == SelectionMode.CREATE_ROOM;

        if (blockLevel) {
            if (event.getClickedBlock() == null) {
                player.sendMessage("\u00a7cYou must click a block.");
                return;
            }
            int bx = event.getClickedBlock().getX();
            int by = event.getClickedBlock().getY();
            int bz = event.getClickedBlock().getZ();
            String worldName = event.getClickedBlock().getWorld().getName();
            if (isLeftClick) {
                session.blockCorner1 = new int[]{bx, by, bz};
                session.world = worldName;
                session.blockCorner2 = null;
                session.awaitingConfirm = false;
                player.sendMessage("\u00a7aCorner 1 set at (" + bx + ", " + by + ", " + bz + "). Now right-click the second corner.");
            } else {
                if (session.blockCorner1 == null) {
                    player.sendMessage("\u00a7cSet corner 1 first by left-clicking.");
                    return;
                }
                if (!worldName.equals(session.world)) {
                    player.sendMessage("\u00a7cBoth corners must be in the same world.");
                    return;
                }
                session.blockCorner2 = new int[]{bx, by, bz};
                org.bukkit.World w = event.getClickedBlock().getWorld();
                int floorY = Math.min(session.blockCorner1[1], session.blockCorner2[1]);
                int sampleX = (session.blockCorner1[0] + session.blockCorner2[0]) / 2;
                int sampleZ = (session.blockCorner1[2] + session.blockCorner2[2]) / 2;
                for (int y = floorY + 1; y < w.getMaxHeight(); y++) {
                    if (w.getBlockAt(sampleX, y, sampleZ).getType().isSolid()) {
                        int cappedY = y - 1;
                        if (cappedY > floorY) {
                            if (session.blockCorner1[1] > cappedY) session.blockCorner1[1] = cappedY;
                            if (session.blockCorner2[1] > cappedY) session.blockCorner2[1] = cappedY;
                            player.sendMessage("\u00a7eCeiling detected at Y=" + y + ". Selection capped to Y=" + cappedY + ".");
                        }
                        break;
                    }
                }
                player.sendMessage("\u00a7aCorner 2 set. Type \u00a76confirm\u00a7e to proceed or \u00a7ccancel\u00a7e to abort.");
                session.awaitingConfirm = true;
            }
            return;
        }

        int cx = player.getLocation().getBlockX() >> 4;
        int cz = player.getLocation().getBlockZ() >> 4;
        String worldName = player.getWorld().getName();

        if (isLeftClick) {
            session.corner1 = new int[]{cx, cz};
            session.world = worldName;
            session.corner2 = null;
            session.awaitingConfirm = false;
            player.sendMessage("\u00a7aCorner 1 set at chunk (" + cx + ", " + cz + "). Now right-click the second corner.");
        } else {
            if (session.corner1 == null) {
                player.sendMessage("\u00a7cSet corner 1 first by left-clicking.");
                return;
            }
            if (!worldName.equals(session.world)) {
                player.sendMessage("\u00a7cBoth corners must be in the same world.");
                return;
            }
            session.corner2 = new int[]{cx, cz};
            List<String> chunks = computeChunks(session);
            int count = chunks.size();
            player.sendMessage("\u00a7aCorner 2 set. \u00a7e" + count + " chunk(s) selected. Type \u00a76confirm\u00a7e to proceed or \u00a7ccancel\u00a7e to abort.");
            session.awaitingConfirm = true;
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        SelectionSession session = sessions.get(player.getUniqueId());
        if (session == null || !session.awaitingConfirm) return;
        String msg = event.getMessage().trim().toLowerCase();
        if (!msg.equals("confirm") && !msg.equals("cancel")) return;
        event.setCancelled(true);

        if (msg.equals("cancel")) {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                sessions.remove(player.getUniqueId());
                removeGoldenShovel(player);
                player.sendMessage("\u00a7cSelection cancelled.");
            });
            return;
        }

        boolean blockLevel = session.mode == SelectionMode.CREATE_JAIL || session.mode == SelectionMode.CREATE_CELL || session.mode == SelectionMode.CREATE_ROOM;
        List<String> selectedChunks = blockLevel ? null : computeChunks(session);
        SelectionSession finalSession = session;
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (finalSession.mode == SelectionMode.CREATE_JAIL) {
                processJailCreation(player, finalSession);
            } else if (finalSession.mode == SelectionMode.CREATE_CELL) {
                processCellCreation(player, finalSession);
                finalSession.blockCorner1 = null;
                finalSession.blockCorner2 = null;
                finalSession.awaitingConfirm = false;
                player.sendMessage("\u00a7eSelect corners for another cell, or type \u00a76cancel\u00a7e to stop.");
                return;
            } else if (finalSession.mode == SelectionMode.CREATE_ROOM) {
                processRoomCreation(player, finalSession);
            } else if (finalSession.mode == SelectionMode.CLAIM_REGION) {
                processRegionClaim(player, finalSession.regionId, selectedChunks);
            } else if (finalSession.mode == SelectionMode.BUY_PROPERTY) {
                processPropertySelection(player, finalSession.regionId, selectedChunks);
            } else if (finalSession.mode == SelectionMode.BUSINESS_PROPERTY) {
                processBusinessPropertySelection(player, finalSession.businessId, selectedChunks);
            } else if (finalSession.mode == SelectionMode.CONSERVE_AREA) {
                processConservationSelection(player, finalSession.nationId, selectedChunks);
            } else if (finalSession.mode == SelectionMode.GOVERNMENT_PROPERTY) {
                processGovernmentPropertySelection(player, finalSession.nationId, selectedChunks);
            } else if (finalSession.mode == SelectionMode.EXTEND_PROPERTY) {
                processExtendProperty(player, finalSession, selectedChunks);
            } else if (finalSession.mode == SelectionMode.EXTEND_BUSINESS_PROPERTY) {
                processExtendBusinessProperty(player, finalSession, selectedChunks);
            }
            sessions.remove(player.getUniqueId());
            removeGoldenShovel(player);
        });
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
        SelectionSession session = sessions.get(player.getUniqueId());
        if (session == null || !session.autoMode) return;

        String worldName = player.getWorld().getName();
        String chunkKey = NationManager.chunkKey(worldName, toCX, toCZ);
        RegionData region = plugin.getNationManager().loadRegion(session.regionId);
        if (region == null) return;

        if (!region.getClaimedChunks().contains(chunkKey)) {
            if (region.getClaimedChunks().size() >= maxRegionChunks) {
                player.sendMessage("\u00a7cRegion \u00a76" + region.getName() + "\u00a7c has reached the maximum of \u00a7e" + maxRegionChunks + "\u00a7c chunks. Auto-claim stopped.");
                sessions.remove(player.getUniqueId());
                return;
            }
            RegionData existing = plugin.getNationManager().getRegionByChunk(chunkKey);
            if (existing != null) {
                player.sendMessage("\u00a7cChunk (" + toCX + ", " + toCZ + ") is already claimed by region \u00a76" + existing.getName() + "\u00a7c.");
                return;
            }
            region.getClaimedChunks().add(chunkKey);
            plugin.getNationManager().saveRegion(region);
            player.sendMessage("\u00a7aChunk (" + toCX + ", " + toCZ + ") claimed to region \u00a76" + region.getName() + "\u00a7a.");
        }
    }

    private List<String> computeChunks(SelectionSession session) {
        List<String> chunks = new ArrayList<>();
        int minX = Math.min(session.corner1[0], session.corner2[0]);
        int maxX = Math.max(session.corner1[0], session.corner2[0]);
        int minZ = Math.min(session.corner1[1], session.corner2[1]);
        int maxZ = Math.max(session.corner1[1], session.corner2[1]);
        for (int cx = minX; cx <= maxX; cx++) {
            for (int cz = minZ; cz <= maxZ; cz++) {
                chunks.add(NationManager.chunkKey(session.world, cx, cz));
            }
        }
        return chunks;
    }

    private void processRegionClaim(Player player, UUID regionId, List<String> chunks) {
        RegionData region = plugin.getNationManager().loadRegion(regionId);
        if (region == null) { player.sendMessage("\u00a7cRegion not found."); return; }
        int available = maxRegionChunks - region.getClaimedChunks().size();
        if (available <= 0) {
            player.sendMessage("\u00a7cRegion \u00a76" + region.getName() + "\u00a7c has reached the maximum of \u00a7e" + maxRegionChunks + "\u00a7c chunks.");
            return;
        }
        int added = 0;
        int skipped = 0;
        for (String ck : chunks) {
            if (added >= available) { skipped++; continue; }
            if (!region.getClaimedChunks().contains(ck)) {
                RegionData existing = plugin.getNationManager().getRegionByChunk(ck);
                if (existing != null) { skipped++; continue; }
                region.getClaimedChunks().add(ck);
                added++;
            }
        }
        plugin.getNationManager().saveRegion(region);
        player.sendMessage("\u00a7a" + added + " chunk(s) claimed to region \u00a76" + region.getName() + "\u00a7a." +
                (skipped > 0 ? " \u00a7e" + skipped + " chunk(s) skipped (limit or overlap)." : ""));
    }

    private void processPropertySelection(Player player, UUID regionId, List<String> selectedChunks) {
        RegionData region = plugin.getNationManager().loadRegion(regionId);
        if (region == null) { player.sendMessage("\u00a7cRegion not found."); return; }

        Set<String> regionChunkSet = new HashSet<>(region.getClaimedChunks());
        List<String> validChunks = new ArrayList<>();
        int outsideRegion = 0;
        int alreadyOwned = 0;
        for (String ck : selectedChunks) {
            if (!regionChunkSet.contains(ck)) { outsideRegion++; continue; }
            if (plugin.getNationManager().getPropertyByChunk(ck) != null) { alreadyOwned++; continue; }
            if (plugin.isMTBusinessEnabled() && plugin.getBusinessManager() != null && plugin.getBusinessManager().getPropertyByChunk(ck) != null) { alreadyOwned++; continue; }
            validChunks.add(ck);
        }
        if (outsideRegion > 0) {
            player.sendMessage("\u00a7e" + outsideRegion + " chunk(s) outside this region were removed from the selection.");
        }
        if (alreadyOwned > 0) {
            player.sendMessage("\u00a7e" + alreadyOwned + " chunk(s) already owned by other properties were removed.");
        }
        if (validChunks.isEmpty()) {
            player.sendMessage("\u00a7cNo valid chunks in selection.");
            return;
        }

        double price = region.getPropertyChunkRate() * validChunks.size();
        NationData nation = plugin.getNationManager().loadNation(region.getNationId());
        if (nation == null || !nation.getMemberUUIDs().contains(player.getUniqueId())) {
            player.sendMessage("\u00a7cYou must be a member of the nation that owns this region.");
            return;
        }

        final List<String> finalChunks = validChunks;
        plugin.getChatInputManager().requestInput(player,
                "\u00a7eEnter a name for this property:",
                propName -> {
                    if (propName.trim().isEmpty()) { player.sendMessage("\u00a7cProperty name cannot be empty."); return; }
                    plugin.getChatInputManager().requestInput(player,
                            "\u00a7eYou are purchasing \u00a76" + finalChunks.size() + " chunk(s)\u00a7e named '\u00a76" + propName.trim() + "\u00a7e' for \u00a7a" + CurrencyUtil.symbol() + String.format("%.2f", price) +
                            "\u00a7e. Type \u00a76yes\u00a7e to confirm or anything else to cancel.",
                            answer -> {
                                if (!answer.equalsIgnoreCase("yes")) {
                                    player.sendMessage("\u00a7cProperty purchase cancelled.");
                                    return;
                                }
                                PropertyPurchaseRequest req = plugin.getNationManager().createPurchaseRequest(regionId, player.getUniqueId(), finalChunks, price);
                                req.setPropertyName(propName.trim());
                                plugin.getNationManager().savePurchaseRequest(req);
                                player.sendMessage("\u00a7aPurchase request submitted! Awaiting approval from the Treasurer or Region Governor.");

                                if (region.getGovernorUUID() != null) {
                                    Player gov = plugin.getServer().getPlayer(region.getGovernorUUID());
                                    if (gov != null) gov.sendMessage("\u00a7eA new property purchase request is awaiting your approval. Check /mtr.");
                                }
                                if (plugin.getRolesConfig().isTreasurerEnabled() && nation.getTreasurerUUID() != null) {
                                    Player tres = plugin.getServer().getPlayer(nation.getTreasurerUUID());
                                    if (tres != null) tres.sendMessage("\u00a7eA new property purchase request is awaiting your approval. Check /mtr.");
                                }
                            }
                    );
                }
        );
    }

    private void processConservationSelection(Player player, UUID nationId, List<String> selectedChunks) {
        if (selectedChunks.isEmpty()) {
            player.sendMessage("\u00a7cNo chunks selected.");
            return;
        }

        RegionData detectedRegion = null;
        for (String ck : selectedChunks) {
            RegionData r = plugin.getNationManager().getRegionByChunk(ck);
            if (r == null || !nationId.equals(r.getNationId())) {
                player.sendMessage("\u00a7cAll chunks must be within a region owned by your nation.");
                return;
            }
            if (detectedRegion == null) detectedRegion = r;
            else if (!detectedRegion.getRegionId().equals(r.getRegionId())) {
                player.sendMessage("\u00a7cAll chunks must be within the same region.");
                return;
            }
        }
        if (detectedRegion == null) { player.sendMessage("\u00a7cNo valid region found."); return; }

        final RegionData finalRegion = detectedRegion;
        final List<String> finalChunks = selectedChunks;
        plugin.getChatInputManager().requestInput(player,
                "\u00a7eYou are creating a Conservation Area of \u00a76" + selectedChunks.size() + " chunk(s)\u00a7e in region \u00a76"
                        + detectedRegion.getName() + "\u00a7e. Type \u00a76yes\u00a7e to confirm or anything else to cancel.",
                answer -> {
                    if (!answer.equalsIgnoreCase("yes")) {
                        player.sendMessage("\u00a7cConservation Area creation cancelled.");
                        return;
                    }
                    plugin.getChatInputManager().requestInput(player,
                            "\u00a7eEnter a name for this Conservation Area:",
                            name -> {
                                String trimmed = name.trim();
                                if (trimmed.isEmpty()) { player.sendMessage("\u00a7cName cannot be empty."); return; }
                                plugin.getNationManager().createConservationArea(finalRegion.getRegionId(), trimmed, finalChunks);
                                player.sendMessage("\u00a7aConservation Area '\u00a76" + trimmed + "\u00a7a' created in region \u00a76"
                                        + finalRegion.getName() + "\u00a7a with \u00a7e" + finalChunks.size() + "\u00a7a chunk(s).");
                            }
                    );
                }
        );
    }

    private void processBusinessPropertySelection(Player player, UUID businessId, List<String> selectedChunks) {
        if (!plugin.isMTBusinessEnabled()) return;
        BusinessData biz = plugin.getBusinessManager().loadBusiness(businessId);
        if (biz == null) { player.sendMessage("\u00a7cBusiness not found."); return; }

        RegionData detectedRegion = null;
        for (String ck : selectedChunks) {
            RegionData r = plugin.getNationManager().getRegionByChunk(ck);
            if (r == null) { player.sendMessage("\u00a7cAll chunks must be within a claimed nation region."); return; }
            if (detectedRegion == null) detectedRegion = r;
            else if (!detectedRegion.getRegionId().equals(r.getRegionId())) {
                player.sendMessage("\u00a7cAll chunks must be within the same region."); return;
            }
        }
        if (detectedRegion == null) { player.sendMessage("\u00a7cNo valid region found."); return; }

        List<String> validChunks = new ArrayList<>();
        int overlap = 0;
        for (String ck : selectedChunks) {
            if (plugin.getNationManager().getPropertyByChunk(ck) != null) { overlap++; continue; }
            if (plugin.getBusinessManager().getPropertyByChunk(ck) != null) { overlap++; continue; }
            validChunks.add(ck);
        }
        if (overlap > 0) {
            player.sendMessage("\u00a7e" + overlap + " chunk(s) already owned by other properties were removed.");
        }
        if (validChunks.isEmpty()) {
            player.sendMessage("\u00a7cAll selected chunks are already owned.");
            return;
        }

        NationData nation = plugin.getNationManager().loadNation(detectedRegion.getNationId());
        if (nation == null) { player.sendMessage("\u00a7cRegion nation not found."); return; }

        final RegionData finalRegion = detectedRegion;
        final NationData finalNation = nation;
        double chunkRate = detectedRegion.getPropertyChunkRate();
        double totalCost = chunkRate * validChunks.size();
        double balance = plugin.getBusinessManager().getTreasuryBalance(biz.getName());
        if (balance < totalCost) {
            player.sendMessage("\u00a7cThe business treasury has insufficient funds. Cost: \u00a7e" + CurrencyUtil.symbol()
                    + String.format("%.2f", totalCost) + "\u00a7c, Balance: \u00a7e" + CurrencyUtil.symbol() + String.format("%.2f", balance));
            return;
        }

        final List<String> finalChunks = validChunks;
        plugin.getChatInputManager().requestInput(player,
                "\u00a7eEnter a name for this business property (\u00a76" + validChunks.size()
                        + " chunks\u00a7e, cost \u00a7a" + CurrencyUtil.symbol() + String.format("%.2f", totalCost) + "\u00a7e):",
                propName -> {
                    if (propName.trim().isEmpty()) { player.sendMessage("\u00a7cProperty name cannot be empty."); return; }
                    plugin.getBusinessManager().withdrawFromTreasury(biz.getName(), totalCost);
                    plugin.getNationManager().depositToTreasury(finalNation.getName(), totalCost);
                    plugin.getBusinessManager().createProperty(businessId, propName.trim(), finalChunks);
                    player.sendMessage("\u00a7aProperty '\u00a76" + propName.trim() + "\u00a7a' purchased for \u00a7e" + CurrencyUtil.symbol()
                            + String.format("%.2f", totalCost) + "\u00a7a and added to \u00a76" + biz.getName() + "\u00a7a.");
                }
        );
    }

    private void processGovernmentPropertySelection(Player player, UUID nationId, List<String> selectedChunks) {
        if (selectedChunks.isEmpty()) { player.sendMessage("\u00a7cNo chunks selected."); return; }

        RegionData detectedRegion = null;
        for (String ck : selectedChunks) {
            RegionData r = plugin.getNationManager().getRegionByChunk(ck);
            if (r == null || !nationId.equals(r.getNationId())) {
                player.sendMessage("\u00a7cAll chunks must be within a region owned by your nation.");
                return;
            }
            if (detectedRegion == null) detectedRegion = r;
            else if (!detectedRegion.getRegionId().equals(r.getRegionId())) {
                player.sendMessage("\u00a7cAll chunks must be within the same region.");
                return;
            }
        }
        if (detectedRegion == null) { player.sendMessage("\u00a7cNo valid region found."); return; }

        final RegionData finalRegion = detectedRegion;
        final List<String> finalChunks = selectedChunks;
        plugin.getChatInputManager().requestInput(player,
                "\u00a7eEnter a name for this government property (\u00a76" + selectedChunks.size() + " chunk(s)\u00a7e in region \u00a76" + detectedRegion.getName() + "\u00a7e):",
                propName -> {
                    if (propName.trim().isEmpty()) { player.sendMessage("\u00a7cProperty name cannot be empty."); return; }
                    PropertyData prop = plugin.getNationManager().createProperty(finalRegion.getRegionId(), nationId, finalChunks, 0);
                    prop.setName(propName.trim());
                    plugin.getNationManager().saveProperty(prop);
                    NationData nation = plugin.getNationManager().loadNation(nationId);
                    if (nation != null) {
                        nation.getGovernmentPropertyIds().add(prop.getPropertyId());
                        plugin.getNationManager().saveNation(nation);
                    }
                    player.sendMessage("\u00a7aGovernment property '\u00a76" + propName.trim() + "\u00a7a' created with \u00a7e" + finalChunks.size() + "\u00a7a chunk(s).");
                }
        );
    }

    private void processJailCreation(Player player, SelectionSession session) {
        plugin.getLawCrimeManager().createJail(session.nationId, session.jailName,
                session.world, session.blockCorner1, session.blockCorner2);
        player.sendMessage("\u00a7aJail '\u00a7e" + session.jailName + "\u00a7a' created! Add cells from Jail Management.");
    }

    private void processCellCreation(Player player, SelectionSession session) {
        JailData jail = plugin.getLawCrimeManager().loadJail(session.jailId);
        if (jail == null) { player.sendMessage("\u00a7cJail not found."); return; }
        int cx = (session.blockCorner1[0] + session.blockCorner2[0]) / 2;
        int cy = Math.min(session.blockCorner1[1], session.blockCorner2[1]);
        int cz = (session.blockCorner1[2] + session.blockCorner2[2]) / 2;
        org.bukkit.World w = Bukkit.getWorld(session.world);
        if (w == null) { player.sendMessage("\u00a7cWorld not found."); return; }
        Location center = new Location(w, cx, cy, cz);
        com.mteco.data.CellData cell = plugin.getLawCrimeManager().createCell(
                session.jailId, session.blockCorner1, session.blockCorner2, center);
        jail.getCellIds().add(cell.getCellId());
        plugin.getLawCrimeManager().saveJail(jail);
        player.sendMessage("\u00a7aCell added. Total cells: " + jail.getCellIds().size());
    }

    private void processRoomCreation(Player player, SelectionSession session) {
        com.mteco.data.PropertyData prop = plugin.getNationManager().loadProperty(session.propertyId);
        if (prop == null) {
            com.mteco.data.BusinessPropertyData bprop = plugin.getBusinessManager() != null ? plugin.getBusinessManager().loadProperty(session.propertyId) : null;
            if (bprop == null) { player.sendMessage("\u00a7cProperty not found."); return; }
            com.mteco.data.RoomData room = plugin.getNationManager().createRoom(
                    session.propertyId, "Room", session.world, session.blockCorner1, session.blockCorner2);
            bprop.getRoomIds().add(room.getRoomId());
            plugin.getBusinessManager().saveProperty(bprop);
            player.sendMessage("\u00a7aRoom created! Total rooms: " + bprop.getRoomIds().size());
            return;
        }
        com.mteco.data.RoomData room = plugin.getNationManager().createRoom(
                session.propertyId, "Room", session.world, session.blockCorner1, session.blockCorner2);
        prop.getRoomIds().add(room.getRoomId());
        plugin.getNationManager().saveProperty(prop);
        player.sendMessage("\u00a7aRoom created! Total rooms: " + prop.getRoomIds().size());
    }

    private void processExtendProperty(Player player, SelectionSession session, List<String> selectedChunks) {
        PropertyData prop = plugin.getNationManager().loadProperty(session.propertyId);
        if (prop == null) { player.sendMessage("\u00a7cProperty not found."); return; }
        RegionData region = plugin.getNationManager().loadRegion(prop.getRegionId());
        if (region == null) { player.sendMessage("\u00a7cRegion not found."); return; }

        Set<String> regionChunkSet = new HashSet<>(region.getClaimedChunks());
        Set<String> existingPropChunks = new HashSet<>(prop.getChunks());
        List<String> validChunks = new ArrayList<>();
        int skipped = 0;
        for (String ck : selectedChunks) {
            if (!regionChunkSet.contains(ck)) { skipped++; continue; }
            if (existingPropChunks.contains(ck)) { skipped++; continue; }
            if (plugin.getNationManager().getPropertyByChunk(ck) != null) { skipped++; continue; }
            if (plugin.isMTBusinessEnabled() && plugin.getBusinessManager() != null && plugin.getBusinessManager().getPropertyByChunk(ck) != null) { skipped++; continue; }
            validChunks.add(ck);
        }
        if (skipped > 0) {
            player.sendMessage("\u00a7e" + skipped + " chunk(s) were skipped (outside region, already owned, or overlap).");
        }
        if (validChunks.isEmpty()) {
            player.sendMessage("\u00a7cNo valid chunks to add.");
            return;
        }

        double chunkRate = region.getPropertyChunkRate();
        double cost = chunkRate * validChunks.size();

        final List<String> finalChunks = validChunks;
        plugin.getChatInputManager().requestInput(player,
                "\u00a7eExtend property with \u00a76" + validChunks.size() + " chunk(s)\u00a7e for \u00a7a" + CurrencyUtil.symbol() + String.format("%.2f", cost)
                        + "\u00a7e? Type \u00a76yes\u00a7e to confirm.",
                answer -> {
                    if (!answer.equalsIgnoreCase("yes")) {
                        player.sendMessage("\u00a7cExtension cancelled.");
                        return;
                    }
                    if (session.governmentExtend && session.nationId != null) {
                        NationData nation = plugin.getNationManager().loadNation(session.nationId);
                        if (nation == null || nation.getTreasuryBalance() < cost) {
                            player.sendMessage("\u00a7cNation treasury has insufficient funds.");
                            return;
                        }
                        plugin.getNationManager().withdrawFromTreasury(nation.getName(), cost);
                    } else {
                        if (!plugin.getEconomy().has(player, cost)) {
                            player.sendMessage("\u00a7cYou cannot afford this (\u00a7a" + CurrencyUtil.symbol() + String.format("%.2f", cost) + "\u00a7c needed).");
                            return;
                        }
                        plugin.getEconomy().withdrawPlayer(player, cost);
                        NationData payNation = plugin.getNationManager().loadNation(region.getNationId());
                        if (payNation != null) plugin.getNationManager().depositToTreasury(payNation.getName(), cost);
                    }
                    PropertyData pr = plugin.getNationManager().loadProperty(session.propertyId);
                    if (pr == null) return;
                    pr.getChunks().addAll(finalChunks);
                    plugin.getNationManager().saveProperty(pr);
                    player.sendMessage("\u00a7aProperty extended by \u00a7e" + finalChunks.size() + "\u00a7a chunk(s)!");
                }
        );
    }

    private void processExtendBusinessProperty(Player player, SelectionSession session, List<String> selectedChunks) {
        if (!plugin.isMTBusinessEnabled()) return;
        BusinessPropertyData prop = plugin.getBusinessManager().loadProperty(session.propertyId);
        if (prop == null) { player.sendMessage("\u00a7cProperty not found."); return; }
        BusinessData biz = plugin.getBusinessManager().loadBusiness(session.businessId);
        if (biz == null) { player.sendMessage("\u00a7cBusiness not found."); return; }

        RegionData detectedRegion = null;
        for (String ck : prop.getChunks()) {
            RegionData r = plugin.getNationManager().getRegionByChunk(ck);
            if (r != null) { detectedRegion = r; break; }
        }
        if (detectedRegion == null) { player.sendMessage("\u00a7cCould not determine property region."); return; }

        Set<String> regionChunkSet = new HashSet<>(detectedRegion.getClaimedChunks());
        Set<String> existingChunks = new HashSet<>(prop.getChunks());
        List<String> validChunks = new ArrayList<>();
        int skipped = 0;
        for (String ck : selectedChunks) {
            if (!regionChunkSet.contains(ck)) { skipped++; continue; }
            if (existingChunks.contains(ck)) { skipped++; continue; }
            if (plugin.getNationManager().getPropertyByChunk(ck) != null) { skipped++; continue; }
            if (plugin.getBusinessManager().getPropertyByChunk(ck) != null) { skipped++; continue; }
            validChunks.add(ck);
        }
        if (skipped > 0) {
            player.sendMessage("\u00a7e" + skipped + " chunk(s) were skipped (outside region, already owned, or overlap).");
        }
        if (validChunks.isEmpty()) {
            player.sendMessage("\u00a7cNo valid chunks to add.");
            return;
        }

        double chunkRate = detectedRegion.getPropertyChunkRate();
        double cost = chunkRate * validChunks.size();
        double balance = plugin.getBusinessManager().getTreasuryBalance(biz.getName());
        if (balance < cost) {
            player.sendMessage("\u00a7cBusiness treasury has insufficient funds. Cost: \u00a7e" + CurrencyUtil.symbol()
                    + String.format("%.2f", cost) + "\u00a7c, Balance: \u00a7e" + CurrencyUtil.symbol() + String.format("%.2f", balance));
            return;
        }

        final List<String> finalChunks = validChunks;
        final RegionData finalRegion = detectedRegion;
        plugin.getChatInputManager().requestInput(player,
                "\u00a7eExtend business property with \u00a76" + validChunks.size() + " chunk(s)\u00a7e for \u00a7a" + CurrencyUtil.symbol() + String.format("%.2f", cost)
                        + "\u00a7e from business treasury? Type \u00a76yes\u00a7e.",
                answer -> {
                    if (!answer.equalsIgnoreCase("yes")) {
                        player.sendMessage("\u00a7cExtension cancelled.");
                        return;
                    }
                    plugin.getBusinessManager().withdrawFromTreasury(biz.getName(), cost);
                    NationData payNation = plugin.getNationManager().loadNation(finalRegion.getNationId());
                    if (payNation != null) plugin.getNationManager().depositToTreasury(payNation.getName(), cost);
                    BusinessPropertyData pr = plugin.getBusinessManager().loadProperty(session.propertyId);
                    if (pr == null) return;
                    pr.getChunks().addAll(finalChunks);
                    plugin.getBusinessManager().saveProperty(pr);
                    player.sendMessage("\u00a7aBusiness property extended by \u00a7e" + finalChunks.size() + "\u00a7a chunk(s)!");
                }
        );
    }

    private void removeGoldenShovel(Player player) {
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item != null && item.getType() == Material.GOLDEN_SHOVEL
                    && item.getItemMeta() != null && "\u00a76Selection Tool".equals(item.getItemMeta().getDisplayName())) {
                player.getInventory().setItem(i, null);
                break;
            }
        }
    }
}
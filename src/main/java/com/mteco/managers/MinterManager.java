package com.mteco.managers;

import com.mteco.MTeco;
import com.mteco.data.MinterData;
import com.mteco.data.NationData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MinterManager {
    private final MTeco plugin;
    private final File mintersFolder;
    private final Map<UUID, MinterData> minterCache = new HashMap<>();
    private final Map<String, UUID> blockToMinter = new HashMap<>();
    private final Map<Inventory, UUID> openInventories = new HashMap<>();

    public MinterManager(MTeco plugin) {
        this.plugin = plugin;
        this.mintersFolder = new File(plugin.getDataFolder(), "data/MTN/minters");
        mintersFolder.mkdirs();
        loadFromDisk();
        startProcessingTask();
    }

    public void createMinterAtLocation(Player player, UUID nationId, Location baseLoc) {
        createMinter(nationId, baseLoc);
        player.sendMessage("\u00a7aMinter created successfully!");
    }

    public MinterData createMinter(UUID nationId, Location baseLoc) {
        MinterData minter = new MinterData();
        minter.setMinterId(UUID.randomUUID());
        minter.setNationId(nationId);
        minter.setWorld(baseLoc.getWorld().getName());
        minter.setBaseX(baseLoc.getBlockX());
        minter.setBaseY(baseLoc.getBlockY());
        minter.setBaseZ(baseLoc.getBlockZ());

        World world = baseLoc.getWorld();
        Block base = world.getBlockAt(baseLoc);
        Block upper = base.getRelative(BlockFace.UP);
        Block cauldron = upper.getRelative(BlockFace.UP);

        base.setType(Material.GOLD_BLOCK);
        upper.setType(Material.GOLD_BLOCK);
        cauldron.setType(Material.CAULDRON);

        saveMinter(minter);
        return minter;
    }

    public void saveMinter(MinterData data) {
        minterCache.put(data.getMinterId(), data);
        rebuildBlockIndex(data);

        File file = new File(mintersFolder, data.getMinterId() + ".yml");
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("nationId", data.getNationId().toString());
        cfg.set("world", data.getWorld());
        cfg.set("baseX", data.getBaseX());
        cfg.set("baseY", data.getBaseY());
        cfg.set("baseZ", data.getBaseZ());
        cfg.set("signFace", data.getSignFace());
        cfg.set("queue", data.getQueue());
        cfg.set("processingStartTime", data.getProcessingStartTime());
        try { cfg.save(file); } catch (IOException e) { e.printStackTrace(); }
    }

    public void deleteMinter(UUID minterId) {
        MinterData minter = minterCache.remove(minterId);
        if (minter != null) {
            for (String key : getBlockKeys(minter)) blockToMinter.remove(key);
            World world = Bukkit.getWorld(minter.getWorld());
            if (world != null) {
                world.getBlockAt(minter.getBaseX(), minter.getBaseY(), minter.getBaseZ()).setType(Material.AIR);
                world.getBlockAt(minter.getBaseX(), minter.getBaseY() + 1, minter.getBaseZ()).setType(Material.AIR);
                world.getBlockAt(minter.getBaseX(), minter.getBaseY() + 2, minter.getBaseZ()).setType(Material.AIR);
                if (minter.getSignFace() != null) {
                    BlockFace face = BlockFace.valueOf(minter.getSignFace());
                    world.getBlockAt(minter.getBaseX() + face.getModX(), minter.getBaseY() + 2, minter.getBaseZ() + face.getModZ()).setType(Material.AIR);
                }
            }
        }
        new File(mintersFolder, minterId + ".yml").delete();
    }

    public MinterData getMinter(UUID minterId) {
        return minterCache.get(minterId);
    }

    public MinterData getMinterByBlock(Location loc) {
        String key = blockKey(loc);
        UUID id = blockToMinter.get(key);
        return id != null ? minterCache.get(id) : null;
    }

    public List<MinterData> getMintersByNation(UUID nationId) {
        List<MinterData> list = new ArrayList<>();
        for (MinterData m : minterCache.values()) {
            if (nationId.equals(m.getNationId())) list.add(m);
        }
        return list;
    }

    public void addToQueue(UUID minterId, String type, int amount) {
        MinterData minter = minterCache.get(minterId);
        if (minter == null) return;
        for (int i = 0; i < amount; i++) minter.getQueue().add(type);
        if (minter.getProcessingStartTime() == 0 && !minter.getQueue().isEmpty()) {
            minter.setProcessingStartTime(System.currentTimeMillis());
        }
        saveMinter(minter);
    }

    public Inventory openDepositInventory(Player player, UUID minterId) {
        Inventory inv = Bukkit.createInventory(null, 54, "\u00a76Minter \u00a78- Deposit Gold");
        openInventories.put(inv, minterId);
        player.openInventory(inv);
        return inv;
    }

    public UUID getOpenInventoryMinterId(Inventory inv) {
        return openInventories.get(inv);
    }

    public boolean isOpenMinterInventory(Inventory inv) {
        return openInventories.containsKey(inv);
    }

    public void unregisterInventory(Inventory inv) {
        openInventories.remove(inv);
    }

    public void reloadCache() {
        minterCache.clear();
        blockToMinter.clear();
        loadFromDisk();
    }

    private void startProcessingTask() {
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::processTick, 20L, 20L);
    }

    private void processTick() {
        for (MinterData minter : new ArrayList<>(minterCache.values())) {
            if (minter.getQueue().isEmpty()) continue;
            if (minter.getProcessingStartTime() == 0) {
                minter.setProcessingStartTime(System.currentTimeMillis());
                saveMinter(minter);
            }
            String type = minter.getQueue().get(0);
            long elapsed = System.currentTimeMillis() - minter.getProcessingStartTime();
            long totalMs = type.equals("BAR")
                    ? plugin.getSettings().getMinterBarProcessingSeconds() * 1000L
                    : plugin.getSettings().getMinterNuggetProcessingSeconds() * 1000L;

            if (elapsed >= totalMs) {
                minter.getQueue().remove(0);
                double amount = type.equals("BAR")
                        ? plugin.getSettings().getMinterBarVaultAmount()
                        : plugin.getSettings().getMinterNuggetVaultAmount();
                NationData nation = plugin.getNationManager().loadNation(minter.getNationId());
                if (nation != null) {
                    plugin.getNationManager().depositToTreasury(nation.getName(), amount);
                    plugin.getNationManager().logTransaction(nation.getNationId(), "MINTER", amount, type.equals("BAR") ? "Gold bar processed" : "Gold nugget processed");
                }
                if (!minter.getQueue().isEmpty()) {
                    minter.setProcessingStartTime(System.currentTimeMillis());
                } else {
                    minter.setProcessingStartTime(0);
                }
                saveMinter(minter);
            }
        }
    }

    private void rebuildBlockIndex(MinterData minter) {
        for (String key : getBlockKeys(minter)) blockToMinter.put(key, minter.getMinterId());
    }

    private List<String> getBlockKeys(MinterData m) {
        List<String> keys = new ArrayList<>();
        keys.add(blockKey(m.getWorld(), m.getBaseX(), m.getBaseY(), m.getBaseZ()));
        keys.add(blockKey(m.getWorld(), m.getBaseX(), m.getBaseY() + 1, m.getBaseZ()));
        keys.add(blockKey(m.getWorld(), m.getBaseX(), m.getBaseY() + 2, m.getBaseZ()));
        if (m.getSignFace() != null) {
            BlockFace face = BlockFace.valueOf(m.getSignFace());
            keys.add(blockKey(m.getWorld(), m.getBaseX() + face.getModX(), m.getBaseY() + 2, m.getBaseZ() + face.getModZ()));
        }
        return keys;
    }

    private static String blockKey(Location loc) {
        return blockKey(loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    private static String blockKey(String world, int x, int y, int z) {
        return world + "," + x + "," + y + "," + z;
    }

    private void loadFromDisk() {
        File[] files = mintersFolder.listFiles((d, n) -> n.endsWith(".yml"));
        if (files == null) return;
        for (File f : files) {
            try {
                UUID id = UUID.fromString(f.getName().replace(".yml", ""));
                YamlConfiguration cfg = YamlConfiguration.loadConfiguration(f);
                MinterData m = new MinterData();
                m.setMinterId(id);
                m.setNationId(UUID.fromString(cfg.getString("nationId")));
                m.setWorld(cfg.getString("world"));
                m.setBaseX(cfg.getInt("baseX"));
                m.setBaseY(cfg.getInt("baseY"));
                m.setBaseZ(cfg.getInt("baseZ"));
                m.setSignFace(cfg.getString("signFace", "SOUTH"));
                m.setQueue(new ArrayList<>(cfg.getStringList("queue")));
                m.setProcessingStartTime(cfg.getLong("processingStartTime", 0));
                minterCache.put(id, m);
                rebuildBlockIndex(m);
            } catch (Exception ignored) {}
        }
        plugin.getLogger().info("[MinterManager] Cached " + minterCache.size() + " minters.");
    }
}
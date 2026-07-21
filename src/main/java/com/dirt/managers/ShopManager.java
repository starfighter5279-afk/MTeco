package com.dirt.managers;

import com.dirt.DirtEconomy;
import com.dirt.data.BusinessData;
import com.dirt.data.ShopData;
import com.dirt.data.StockroomData;
import com.dirt.util.CurrencyUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ShopManager {
    private final DirtEconomy plugin;
    private final File shopsFolder;
    private final File stockroomsFolder;
    private final File onlineShopsFile;

    private final Map<UUID, ShopData> shopCache = new HashMap<>();
    private final Map<UUID, StockroomData> stockroomCache = new HashMap<>();
    private final Map<String, UUID> chestToShop = new HashMap<>();
    private final Map<String, UUID> signToShop = new HashMap<>();

    public ShopManager(DirtEconomy plugin) {
        this.plugin = plugin;
        this.shopsFolder = new File(plugin.getDataFolder(), "data/MTB/shops");
        this.stockroomsFolder = new File(plugin.getDataFolder(), "data/MTB/stockrooms");
        this.onlineShopsFile = new File(plugin.getDataFolder(), "data/MTB/online_shops.yml");
        shopsFolder.mkdirs();
        stockroomsFolder.mkdirs();
        loadAllFromDisk();
    }

    public void reloadCache() {
        shopCache.clear();
        stockroomCache.clear();
        chestToShop.clear();
        signToShop.clear();
        loadAllFromDisk();
    }

    private void loadAllFromDisk() {
        File[] shopFiles = shopsFolder.listFiles((d, n) -> n.endsWith(".yml"));
        if (shopFiles != null) {
            for (File f : shopFiles) {
                try {
                    UUID id = UUID.fromString(f.getName().replace(".yml", ""));
                    ShopData s = readShopFile(id, f);
                    if (s != null) {
                        shopCache.put(id, s);
                        if (s.getChestLocation() != null) chestToShop.put(s.getChestLocation(), id);
                        if (s.getSignLocation() != null) signToShop.put(s.getSignLocation(), id);
                    }
                } catch (IllegalArgumentException ignored) {}
            }
        }
        File[] srFiles = stockroomsFolder.listFiles((d, n) -> n.endsWith(".yml"));
        if (srFiles != null) {
            for (File f : srFiles) {
                try {
                    UUID id = UUID.fromString(f.getName().replace(".yml", ""));
                    StockroomData sr = readStockroomFile(id, f);
                    if (sr != null) stockroomCache.put(id, sr);
                } catch (IllegalArgumentException ignored) {}
            }
        }
        plugin.getLogger().info("[ShopManager] Cached " + shopCache.size() + " shops, " + stockroomCache.size() + " stockrooms.");
    }

    public static String locKey(Location loc) {
        return loc.getWorld().getName() + "|" + loc.getBlockX() + "|" + loc.getBlockY() + "|" + loc.getBlockZ();
    }

    public static Location parseLoc(String key) {
        if (key == null) return null;
        String[] parts = key.split("\\|");
        if (parts.length != 4) return null;
        World w = Bukkit.getWorld(parts[0]);
        if (w == null) return null;
        return new Location(w, Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
    }

    // ---- Shops ----

    public ShopData createShop(ShopData shop) {
        if (shop.getShopId() == null) shop.setShopId(UUID.randomUUID());
        saveShop(shop);
        return shop;
    }

    public void saveShop(ShopData shop) {
        chestToShop.values().removeIf(sid -> sid.equals(shop.getShopId()));
        signToShop.values().removeIf(sid -> sid.equals(shop.getShopId()));
        if (shop.getChestLocation() != null) chestToShop.put(shop.getChestLocation(), shop.getShopId());
        if (shop.getSignLocation() != null) signToShop.put(shop.getSignLocation(), shop.getShopId());
        shopCache.put(shop.getShopId(), shop);

        File file = new File(shopsFolder, shop.getShopId() + ".yml");
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("ownerType", shop.getOwnerType().name());
        cfg.set("ownerUUID", shop.getOwnerUUID() != null ? shop.getOwnerUUID().toString() : null);
        cfg.set("ownerDisplay", shop.getOwnerDisplay());
        cfg.set("chestLocation", shop.getChestLocation());
        cfg.set("signLocation", shop.getSignLocation());
        cfg.set("pricePerUnit", shop.getPricePerUnit());
        cfg.set("itemTemplate", shop.getItemTemplate());
        cfg.set("saleTimestamps", shop.getSaleTimestamps());
        cfg.set("saleAmounts", shop.getSaleAmounts());
        try { cfg.save(file); } catch (IOException e) { e.printStackTrace(); }
    }

    public ShopData loadShop(UUID id) {
        if (id == null) return null;
        return shopCache.get(id);
    }

    public void deleteShop(UUID id) {
        ShopData removed = shopCache.remove(id);
        if (removed != null) {
            chestToShop.values().removeIf(sid -> sid.equals(id));
            signToShop.values().removeIf(sid -> sid.equals(id));
        }
        new File(shopsFolder, id + ".yml").delete();
    }

    public List<ShopData> getAllShops() {
        return new ArrayList<>(shopCache.values());
    }

    public ShopData getShopByChest(Location loc) {
        String key = locKey(loc);
        UUID id = chestToShop.get(key);
        if (id != null) return shopCache.get(id);
        Block b = loc.getBlock();
        if (b.getState() instanceof Chest chest) {
            InventoryHolder holder = chest.getInventory().getHolder();
            if (holder instanceof DoubleChest dc) {
                if (dc.getLeftSide() instanceof Chest left) {
                    id = chestToShop.get(locKey(left.getLocation()));
                    if (id != null) return shopCache.get(id);
                }
                if (dc.getRightSide() instanceof Chest right) {
                    id = chestToShop.get(locKey(right.getLocation()));
                    if (id != null) return shopCache.get(id);
                }
            }
        }
        return null;
    }

    public ShopData getShopBySign(Location loc) {
        String key = locKey(loc);
        UUID id = signToShop.get(key);
        return id != null ? shopCache.get(id) : null;
    }

    public List<ShopData> getShopsByBusiness(UUID businessId) {
        List<ShopData> list = new ArrayList<>();
        for (ShopData s : shopCache.values()) {
            if (s.getOwnerType() == ShopData.OwnerType.BUSINESS && businessId.equals(s.getOwnerUUID())) list.add(s);
        }
        return list;
    }

    public void recordSale(ShopData shop, double amount) {
        shop.getSaleTimestamps().add(System.currentTimeMillis());
        shop.getSaleAmounts().add(amount);
        long cutoff = System.currentTimeMillis() - (plugin.getSettings().getSaleHistoryDays() * 24L * 60L * 60L * 1000L);
        List<Long> ts = shop.getSaleTimestamps();
        List<Double> am = shop.getSaleAmounts();
        while (!ts.isEmpty() && ts.get(0) < cutoff) {
            ts.remove(0);
            am.remove(0);
        }
        saveShop(shop);
    }

    public double getMonthlyProfit(ShopData shop) {
        long cutoff = System.currentTimeMillis() - (plugin.getSettings().getMonthlyProfitDays() * 24L * 60L * 60L * 1000L);
        double total = 0;
        List<Long> ts = shop.getSaleTimestamps();
        List<Double> am = shop.getSaleAmounts();
        for (int i = 0; i < ts.size(); i++) {
            if (ts.get(i) >= cutoff) total += am.get(i);
        }
        return total;
    }

    // ---- Stockrooms ----

    public StockroomData createStockroom(StockroomData sr) {
        if (sr.getStockroomId() == null) sr.setStockroomId(UUID.randomUUID());
        saveStockroom(sr);
        return sr;
    }

    public void saveStockroom(StockroomData sr) {
        stockroomCache.put(sr.getStockroomId(), sr);
        File file = new File(stockroomsFolder, sr.getStockroomId() + ".yml");
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("businessId", sr.getBusinessId().toString());
        cfg.set("name", sr.getName());
        cfg.set("world", sr.getWorld());
        cfg.set("minX", sr.getMinX());
        cfg.set("minY", sr.getMinY());
        cfg.set("minZ", sr.getMinZ());
        cfg.set("maxX", sr.getMaxX());
        cfg.set("maxY", sr.getMaxY());
        cfg.set("maxZ", sr.getMaxZ());
        try { cfg.save(file); } catch (IOException e) { e.printStackTrace(); }
    }

    public StockroomData loadStockroom(UUID id) {
        if (id == null) return null;
        return stockroomCache.get(id);
    }

    public void deleteStockroom(UUID id) {
        stockroomCache.remove(id);
        new File(stockroomsFolder, id + ".yml").delete();
    }

    public List<StockroomData> getAllStockrooms() {
        return new ArrayList<>(stockroomCache.values());
    }

    public List<StockroomData> getStockroomsByBusiness(UUID businessId) {
        List<StockroomData> list = new ArrayList<>();
        for (StockroomData s : stockroomCache.values()) {
            if (businessId.equals(s.getBusinessId())) list.add(s);
        }
        return list;
    }

    public StockroomData getStockroomAt(Location loc) {
        String w = loc.getWorld().getName();
        int x = loc.getBlockX(), y = loc.getBlockY(), z = loc.getBlockZ();
        for (StockroomData s : stockroomCache.values()) {
            if (s.contains(w, x, y, z)) return s;
        }
        return null;
    }

    public List<Chest> getStockChests(StockroomData sr) {
        List<Chest> chests = new ArrayList<>();
        World world = Bukkit.getWorld(sr.getWorld());
        if (world == null) return chests;
        java.util.Set<InventoryHolder> seenHolders = new java.util.HashSet<>();
        for (int x = sr.getMinX(); x <= sr.getMaxX(); x++) {
            for (int y = sr.getMinY(); y <= sr.getMaxY(); y++) {
                for (int z = sr.getMinZ(); z <= sr.getMaxZ(); z++) {
                    Block b = world.getBlockAt(x, y, z);
                    if (b.getType() == Material.CHEST || b.getType() == Material.TRAPPED_CHEST) {
                        BlockState st = b.getState();
                        if (st instanceof Chest c) {
                            InventoryHolder holder = c.getInventory().getHolder();
                            if (holder != null && !seenHolders.add(holder)) continue;
                            chests.add(c);
                        }
                    }
                }
            }
        }
        return chests;
    }

    public int pullFromBusinessStock(UUID businessId, ItemStack template, int amount) {
        int remaining = amount;
        for (StockroomData sr : getStockroomsByBusiness(businessId)) {
            for (Chest chest : getStockChests(sr)) {
                Inventory inv = chest.getInventory();
                for (int i = 0; i < inv.getSize() && remaining > 0; i++) {
                    ItemStack stack = inv.getItem(i);
                    if (stack == null) continue;
                    if (!stack.isSimilar(template)) continue;
                    int take = Math.min(stack.getAmount(), remaining);
                    stack.setAmount(stack.getAmount() - take);
                    if (stack.getAmount() <= 0) inv.setItem(i, null);
                    else inv.setItem(i, stack);
                    remaining -= take;
                }
            }
            if (remaining <= 0) break;
        }
        return amount - remaining;
    }

    public int countBusinessStock(UUID businessId, ItemStack template) {
        int total = 0;
        for (StockroomData sr : getStockroomsByBusiness(businessId)) {
            for (Chest chest : getStockChests(sr)) {
                for (ItemStack stack : chest.getInventory().getContents()) {
                    if (stack != null && stack.isSimilar(template)) total += stack.getAmount();
                }
            }
        }
        return total;
    }

    public void refillShopFromStock(ShopData shop) {
        if (shop.getOwnerType() != ShopData.OwnerType.BUSINESS) return;
        Location chestLoc = parseLoc(shop.getChestLocation());
        if (chestLoc == null) return;
        Block b = chestLoc.getBlock();
        if (!(b.getState() instanceof Chest chest)) return;
        Inventory shopInv = chest.getInventory();

        ItemStack template = shop.getItemTemplate();
        if (template == null) return;

        int free = 0;
        for (ItemStack s : shopInv.getContents()) {
            if (s == null) free += template.getMaxStackSize();
            else if (s.isSimilar(template)) free += (template.getMaxStackSize() - s.getAmount());
        }
        if (free <= 0) return;

        int pulled = pullFromBusinessStock(shop.getOwnerUUID(), template, free);
        if (pulled <= 0) return;
        ItemStack toAdd = template.clone();
        toAdd.setAmount(pulled);
        shopInv.addItem(toAdd);
        updateShopSign(shop);
    }

    // ---- Online Shops ----

    public boolean isOnlineShopEnabled(UUID businessId) {
        if (!onlineShopsFile.exists()) return false;
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(onlineShopsFile);
        return cfg.getBoolean("enabled." + businessId.toString(), false);
    }

    public void setOnlineShopEnabled(UUID businessId, boolean enabled) {
        YamlConfiguration cfg = onlineShopsFile.exists()
                ? YamlConfiguration.loadConfiguration(onlineShopsFile) : new YamlConfiguration();
        cfg.set("enabled." + businessId.toString(), enabled);
        try { cfg.save(onlineShopsFile); } catch (IOException e) { e.printStackTrace(); }
    }

    public List<UUID> getOnlineShopBusinessIds() {
        List<UUID> list = new ArrayList<>();
        if (!onlineShopsFile.exists()) return list;
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(onlineShopsFile);
        if (!cfg.isConfigurationSection("enabled")) return list;
        for (String key : cfg.getConfigurationSection("enabled").getKeys(false)) {
            if (cfg.getBoolean("enabled." + key, false)) {
                try { list.add(UUID.fromString(key)); } catch (IllegalArgumentException ignored) {}
            }
        }
        return list;
    }

    // ---- Sign refresh ----

    public void updateShopSign(ShopData shop) {
        Location signLoc = parseLoc(shop.getSignLocation());
        if (signLoc == null) return;
        Block b = signLoc.getBlock();
        BlockState state = b.getState();
        if (!(state instanceof org.bukkit.block.Sign sign)) return;

        int stock = countShopStock(shop);

        ItemStack template = shop.getItemTemplate();
        String itemName = template != null ? friendlyName(template) : "Item";

        boolean isBusiness = shop.getOwnerType() == ShopData.OwnerType.BUSINESS;
        int displayStock = stock;
        if (isBusiness && stock == 0) {
            displayStock = countBusinessStock(shop.getOwnerUUID(), template);
        }

        if (displayStock <= 0) {
            sign.setLine(0, "\u00a7c\u00a7lOUT OF STOCK");
            sign.setLine(1, "\u00a7c" + truncate(itemName, 15));
            sign.setLine(2, "\u00a7c" + CurrencyUtil.symbol() + String.format("%.2f", shop.getPricePerUnit()));
            sign.setLine(3, "\u00a78" + truncate(shop.getOwnerDisplay(), 15));
        } else {
            sign.setLine(0, "\u00a71\u00a7l" + truncate(itemName, 15));
            sign.setLine(1, "\u00a7aQty: " + displayStock);
            sign.setLine(2, "\u00a7e" + CurrencyUtil.symbol() + String.format("%.2f", shop.getPricePerUnit()));
            sign.setLine(3, "\u00a78" + truncate(shop.getOwnerDisplay(), 15));
        }
        sign.update(true, false);
    }

    public int countShopStock(ShopData shop) {
        Location chestLoc = parseLoc(shop.getChestLocation());
        if (chestLoc == null) return 0;
        Block b = chestLoc.getBlock();
        if (!(b.getState() instanceof Chest chest)) return 0;
        ItemStack template = shop.getItemTemplate();
        if (template == null) return 0;
        int total = 0;
        for (ItemStack stack : chest.getInventory().getContents()) {
            if (stack != null && stack.isSimilar(template)) total += stack.getAmount();
        }
        return total;
    }

    public static String friendlyName(ItemStack item) {
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return org.bukkit.ChatColor.stripColor(item.getItemMeta().getDisplayName());
        }
        String raw = item.getType().name().toLowerCase().replace('_', ' ');
        StringBuilder sb = new StringBuilder();
        boolean cap = true;
        for (char c : raw.toCharArray()) {
            if (c == ' ') { cap = true; sb.append(c); }
            else if (cap) { sb.append(Character.toUpperCase(c)); cap = false; }
            else sb.append(c);
        }
        return sb.toString();
    }

    private static String truncate(String s, int len) {
        if (s == null) return "";
        if (s.length() <= len) return s;
        return s.substring(0, len);
    }

    public boolean isOwnerOrAuthorized(ShopData shop, UUID playerUUID) {
        if (shop.getOwnerType() == ShopData.OwnerType.CHARACTER) {
            return playerUUID.equals(shop.getOwnerUUID());
        }
        if (plugin.getBusinessManager() == null) return false;
        BusinessData biz = plugin.getBusinessManager().loadBusiness(shop.getOwnerUUID());
        if (biz == null) return false;
        if (playerUUID.equals(biz.getOwnerUUID())) return true;
        return biz.getEmployeeUUIDs().contains(playerUUID);
    }

    private ShopData readShopFile(UUID id, File file) {
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        ShopData s = new ShopData();
        s.setShopId(id);
        s.setOwnerType(ShopData.OwnerType.valueOf(cfg.getString("ownerType", "CHARACTER")));
        String ou = cfg.getString("ownerUUID");
        if (ou != null) s.setOwnerUUID(UUID.fromString(ou));
        s.setOwnerDisplay(cfg.getString("ownerDisplay", ""));
        s.setChestLocation(cfg.getString("chestLocation"));
        s.setSignLocation(cfg.getString("signLocation"));
        s.setPricePerUnit(cfg.getDouble("pricePerUnit", 0.0));
        s.setItemTemplate(cfg.getItemStack("itemTemplate"));
        List<Long> ts = new ArrayList<>();
        for (Object o : cfg.getList("saleTimestamps", new ArrayList<>())) {
            if (o instanceof Number) ts.add(((Number) o).longValue());
        }
        s.setSaleTimestamps(ts);
        List<Double> amounts = new ArrayList<>();
        for (Object o : cfg.getList("saleAmounts", new ArrayList<>())) {
            if (o instanceof Number) amounts.add(((Number) o).doubleValue());
        }
        s.setSaleAmounts(amounts);
        return s;
    }

    private StockroomData readStockroomFile(UUID id, File file) {
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        StockroomData sr = new StockroomData();
        sr.setStockroomId(id);
        sr.setBusinessId(UUID.fromString(cfg.getString("businessId")));
        sr.setName(cfg.getString("name", "Stockroom"));
        sr.setWorld(cfg.getString("world"));
        sr.setMinX(cfg.getInt("minX"));
        sr.setMinY(cfg.getInt("minY"));
        sr.setMinZ(cfg.getInt("minZ"));
        sr.setMaxX(cfg.getInt("maxX"));
        sr.setMaxY(cfg.getInt("maxY"));
        sr.setMaxZ(cfg.getInt("maxZ"));
        return sr;
    }
}
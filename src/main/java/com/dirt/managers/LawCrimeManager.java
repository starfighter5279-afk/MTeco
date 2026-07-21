package com.dirt.managers;

import com.dirt.DirtEconomy;
import com.dirt.data.CellData;
import com.dirt.data.CharacterData;
import com.dirt.data.CriminalRecord;
import com.dirt.data.JailData;
import com.dirt.data.NationData;
import com.dirt.data.LawBookData;
import com.dirt.data.LegislatedCrimeData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class LawCrimeManager {
    private final DirtEconomy plugin;
    private final File lawBooksFolder;
    private final File crimesFolder;
    private final File criminalsFolder;
    private final File jailsFolder;
    private final File cellsFolder;

    private final Map<UUID, LawBookData> lawBookCache = new HashMap<>();
    private final Map<UUID, LegislatedCrimeData> crimeCache = new HashMap<>();
    private final Map<UUID, CriminalRecord> criminalCache = new HashMap<>();
    private final Map<UUID, JailData> jailCache = new HashMap<>();
    private final Map<UUID, CellData> cellCache = new HashMap<>();
    private final Map<UUID, UUID> jailRespawnQueue = new HashMap<>();

    public LawCrimeManager(DirtEconomy plugin) {
        this.plugin = plugin;
        this.lawBooksFolder = new File(plugin.getDataFolder(), "data/MTN/lawbooks");
        this.crimesFolder = new File(plugin.getDataFolder(), "data/MTN/legcrimes");
        this.criminalsFolder = new File(plugin.getDataFolder(), "data/MTN/criminals");
        this.jailsFolder = new File(plugin.getDataFolder(), "data/MTN/jails");
        this.cellsFolder = new File(plugin.getDataFolder(), "data/MTN/cells");
        lawBooksFolder.mkdirs();
        crimesFolder.mkdirs();
        criminalsFolder.mkdirs();
        jailsFolder.mkdirs();
        cellsFolder.mkdirs();
        loadAllData();
        startJailCheckTask();
        startEscapeCheckTask();
    }

    public void reloadCache() {
        lawBookCache.clear();
        crimeCache.clear();
        criminalCache.clear();
        jailCache.clear();
        cellCache.clear();
        loadAllData();
    }

    // ---- Law Books ----

    public LawBookData createLawBook(UUID nationId, UUID authorUUID, String title, List<String> sections, boolean approved) {
        LawBookData book = new LawBookData();
        book.setBookId(UUID.randomUUID());
        book.setNationId(nationId);
        book.setAuthorUUID(authorUUID);
        book.setTitle(title);
        book.setSections(new ArrayList<>(sections));
        book.setApproved(approved);
        book.setPendingApproval(!approved);
        saveLawBook(book);
        return book;
    }

    public void saveLawBook(LawBookData data) {
        lawBookCache.put(data.getBookId(), data);
        File file = new File(lawBooksFolder, data.getBookId() + ".yml");
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("nationId", data.getNationId().toString());
        cfg.set("authorUUID", data.getAuthorUUID() != null ? data.getAuthorUUID().toString() : null);
        cfg.set("title", data.getTitle());
        cfg.set("sections", data.getSections());
        cfg.set("approved", data.isApproved());
        cfg.set("pendingApproval", data.isPendingApproval());
        try { cfg.save(file); } catch (IOException e) { e.printStackTrace(); }
    }

    public void deleteLawBook(UUID bookId) {
        lawBookCache.remove(bookId);
        new File(lawBooksFolder, bookId + ".yml").delete();
    }

    public LawBookData loadLawBook(UUID bookId) {
        return lawBookCache.get(bookId);
    }

    public List<LawBookData> getLawBooksByNation(UUID nationId, boolean approvedOnly) {
        List<LawBookData> list = new ArrayList<>();
        for (LawBookData b : lawBookCache.values()) {
            if (nationId.equals(b.getNationId()) && (!approvedOnly || b.isApproved())) list.add(b);
        }
        return list;
    }

    public List<LawBookData> getPendingLawBooks(UUID nationId) {
        List<LawBookData> list = new ArrayList<>();
        for (LawBookData b : lawBookCache.values()) {
            if (nationId.equals(b.getNationId()) && b.isPendingApproval()) list.add(b);
        }
        return list;
    }

    // ---- Legislated Crimes ----

    public LegislatedCrimeData createCrime(UUID nationId, String name, int jailDays, double fine, boolean approved) {
        LegislatedCrimeData crime = new LegislatedCrimeData();
        crime.setCrimeId(UUID.randomUUID());
        crime.setNationId(nationId);
        crime.setName(name);
        crime.setMinJailDays(jailDays);
        crime.setFineAmount(fine);
        crime.setApproved(approved);
        crime.setPendingApproval(!approved);
        saveCrime(crime);
        return crime;
    }

    public void saveCrime(LegislatedCrimeData data) {
        crimeCache.put(data.getCrimeId(), data);
        File file = new File(crimesFolder, data.getCrimeId() + ".yml");
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("nationId", data.getNationId().toString());
        cfg.set("name", data.getName());
        cfg.set("minJailDays", data.getMinJailDays());
        cfg.set("fineAmount", data.getFineAmount());
        cfg.set("approved", data.isApproved());
        cfg.set("pendingApproval", data.isPendingApproval());
        List<String> links = new ArrayList<>(data.getLinkedLawSections());
        cfg.set("linkedLawSections", links);
        try { cfg.save(file); } catch (IOException e) { e.printStackTrace(); }
    }

    public void deleteCrime(UUID crimeId) {
        crimeCache.remove(crimeId);
        new File(crimesFolder, crimeId + ".yml").delete();
    }

    public LegislatedCrimeData loadCrime(UUID crimeId) {
        return crimeCache.get(crimeId);
    }

    public List<LegislatedCrimeData> getCrimesByNation(UUID nationId, boolean approvedOnly) {
        List<LegislatedCrimeData> list = new ArrayList<>();
        for (LegislatedCrimeData c : crimeCache.values()) {
            if (nationId.equals(c.getNationId()) && (!approvedOnly || c.isApproved())) list.add(c);
        }
        return list;
    }

    public List<LegislatedCrimeData> getPendingCrimes(UUID nationId) {
        List<LegislatedCrimeData> list = new ArrayList<>();
        for (LegislatedCrimeData c : crimeCache.values()) {
            if (nationId.equals(c.getNationId()) && c.isPendingApproval()) list.add(c);
        }
        return list;
    }

    // ---- Criminal Records ----

    public CriminalRecord createCriminalRecord(UUID nationId, UUID criminalUUID, List<UUID> crimeIds, String description) {
        CriminalRecord record = new CriminalRecord();
        record.setRecordId(UUID.randomUUID());
        record.setNationId(nationId);
        record.setCriminalUUID(criminalUUID);
        record.setCrimeIds(new ArrayList<>(crimeIds));
        record.setDescription(description);
        record.setConvictedTimestamp(System.currentTimeMillis());

        int totalJailDays = 0;
        double totalFine = 0;
        for (UUID cid : crimeIds) {
            LegislatedCrimeData crime = crimeCache.get(cid);
            if (crime != null) {
                totalJailDays += crime.getMinJailDays();
                totalFine += crime.getFineAmount();
            }
        }
        long jailMs = totalJailDays * 20L * 60L * 1000L;
        record.setJailReleaseTime(System.currentTimeMillis() + jailMs);

        if (totalFine > 0) {
            Player criminal = Bukkit.getPlayer(criminalUUID);
            if (criminal != null && plugin.getEconomy().has(criminal, totalFine)) {
                plugin.getEconomy().withdrawPlayer(criminal, totalFine);
                com.dirt.data.NationData nation = plugin.getNationManager().loadNation(nationId);
                if (nation != null) {
                    plugin.getNationManager().depositToTreasury(nation.getName(), totalFine);
                    plugin.getNationManager().logTransaction(nationId, "FINE", totalFine, "Criminal fine from " + criminalUUID);
                }
            }
        }

        saveCriminalRecord(record);
        return record;
    }

    public void saveCriminalRecord(CriminalRecord data) {
        criminalCache.put(data.getRecordId(), data);
        File file = new File(criminalsFolder, data.getRecordId() + ".yml");
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("nationId", data.getNationId().toString());
        cfg.set("criminalUUID", data.getCriminalUUID().toString());
        List<String> ids = new ArrayList<>();
        for (UUID u : data.getCrimeIds()) ids.add(u.toString());
        cfg.set("crimeIds", ids);
        cfg.set("description", data.getDescription());
        cfg.set("convictedTimestamp", data.getConvictedTimestamp());
        cfg.set("jailReleaseTime", data.getJailReleaseTime());
        cfg.set("serving", data.isServing());
        cfg.set("cellId", data.getCellId() != null ? data.getCellId().toString() : null);
        cfg.set("escaped", data.isEscaped());
        cfg.set("assignedBedLocation", data.getAssignedBedLocation());
        try { cfg.save(file); } catch (IOException e) { e.printStackTrace(); }
    }

    public void deleteCriminalRecord(UUID recordId) {
        criminalCache.remove(recordId);
        new File(criminalsFolder, recordId + ".yml").delete();
    }

    public CriminalRecord loadCriminalRecord(UUID recordId) {
        return criminalCache.get(recordId);
    }

    public List<CriminalRecord> getRecordsByNation(UUID nationId) {
        List<CriminalRecord> list = new ArrayList<>();
        for (CriminalRecord r : criminalCache.values()) {
            if (nationId.equals(r.getNationId())) list.add(r);
        }
        return list;
    }

    public CriminalRecord getActiveRecordForPlayer(UUID playerUUID, UUID nationId) {
        for (CriminalRecord r : criminalCache.values()) {
            if (nationId.equals(r.getNationId()) && playerUUID.equals(r.getCriminalUUID())
                    && System.currentTimeMillis() < r.getJailReleaseTime()) {
                return r;
            }
        }
        return null;
    }

    public boolean isCriminal(UUID playerUUID, UUID nationId) {
        return getActiveRecordForPlayer(playerUUID, nationId) != null;
    }

    // ---- Jails ----

    public JailData createJail(UUID nationId, String name, String world, int[] corner1, int[] corner2) {
        JailData jail = new JailData();
        jail.setJailId(UUID.randomUUID());
        jail.setNationId(nationId);
        jail.setName(name);
        jail.setWorld(world);
        jail.setCorner1X(corner1[0]);
        jail.setCorner1Y(corner1[1]);
        jail.setCorner1Z(corner1[2]);
        jail.setCorner2X(corner2[0]);
        jail.setCorner2Y(corner2[1]);
        jail.setCorner2Z(corner2[2]);
        saveJail(jail);
        return jail;
    }

    public void saveJail(JailData data) {
        jailCache.put(data.getJailId(), data);
        File file = new File(jailsFolder, data.getJailId() + ".yml");
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("nationId", data.getNationId().toString());
        cfg.set("name", data.getName());
        cfg.set("world", data.getWorld());
        cfg.set("corner1X", data.getCorner1X());
        cfg.set("corner1Y", data.getCorner1Y());
        cfg.set("corner1Z", data.getCorner1Z());
        cfg.set("corner2X", data.getCorner2X());
        cfg.set("corner2Y", data.getCorner2Y());
        cfg.set("corner2Z", data.getCorner2Z());
        List<String> cellIdStrs = new ArrayList<>();
        for (UUID cid : data.getCellIds()) cellIdStrs.add(cid.toString());
        cfg.set("cellIds", cellIdStrs);
        try { cfg.save(file); } catch (IOException e) { e.printStackTrace(); }
    }

    public void deleteJail(UUID jailId) {
        jailCache.remove(jailId);
        new File(jailsFolder, jailId + ".yml").delete();
    }

    public JailData loadJail(UUID jailId) {
        return jailCache.get(jailId);
    }

    public List<JailData> getJailsByNation(UUID nationId) {
        List<JailData> list = new ArrayList<>();
        for (JailData j : jailCache.values()) {
            if (nationId.equals(j.getNationId())) list.add(j);
        }
        return list;
    }

    public Location getAvailableCell(UUID nationId) {
        for (JailData jail : getJailsByNation(nationId)) {
            for (UUID cellId : jail.getCellIds()) {
                CellData cell = loadCell(cellId);
                if (cell == null) continue;
                int occupants = getCriminalsInCell(cellId).size();
                int beds = countBedsInCell(cell);
                if (beds > 0 && occupants < beds) {
                    return parseLoc(cell.getLocation());
                }
            }
        }
        return null;
    }

    public Location getAvailableCellForCriminal(UUID nationId, UUID criminalUUID) {
        CriminalRecord record = getActiveRecordForPlayer(criminalUUID, nationId);
        for (JailData jail : getJailsByNation(nationId)) {
            for (UUID cellId : jail.getCellIds()) {
                CellData cell = loadCell(cellId);
                if (cell == null) continue;
                int occupants = getCriminalsInCell(cellId).size();
                int beds = countBedsInCell(cell);
                if (beds > 0 && occupants < beds) {
                    if (record != null) {
                        record.setCellId(cellId);
                        record.setEscaped(false);
                        Location bedLoc = findUnassignedBedInCell(cell);
                        if (bedLoc != null) {
                            record.setAssignedBedLocation(locKey(bedLoc));
                        }
                        saveCriminalRecord(record);
                    }
                    return parseLoc(cell.getLocation());
                }
            }
        }
        return null;
    }

    // ---- Cells ----

    public CellData createCell(UUID jailId, int[] corner1, int[] corner2, Location spawnPoint) {
        CellData cell = new CellData();
        cell.setCellId(UUID.randomUUID());
        cell.setJailId(jailId);
        cell.setLocation(locKey(spawnPoint));
        cell.setCorner1X(corner1[0]);
        cell.setCorner1Y(corner1[1]);
        cell.setCorner1Z(corner1[2]);
        cell.setCorner2X(corner2[0]);
        cell.setCorner2Y(corner2[1]);
        cell.setCorner2Z(corner2[2]);
        saveCell(cell);
        return cell;
    }

    public void saveCell(CellData data) {
        cellCache.put(data.getCellId(), data);
        File file = new File(cellsFolder, data.getCellId() + ".yml");
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("jailId", data.getJailId().toString());
        cfg.set("name", data.getName());
        cfg.set("location", data.getLocation());
        cfg.set("corner1X", data.getCorner1X());
        cfg.set("corner1Y", data.getCorner1Y());
        cfg.set("corner1Z", data.getCorner1Z());
        cfg.set("corner2X", data.getCorner2X());
        cfg.set("corner2Y", data.getCorner2Y());
        cfg.set("corner2Z", data.getCorner2Z());
        try { cfg.save(file); } catch (IOException e) { e.printStackTrace(); }
    }

    public void deleteCell(UUID cellId) {
        cellCache.remove(cellId);
        new File(cellsFolder, cellId + ".yml").delete();
    }

    public CellData loadCell(UUID cellId) {
        return cellCache.get(cellId);
    }

    public List<CellData> getCellsByJail(UUID jailId) {
        List<CellData> list = new ArrayList<>();
        for (CellData c : cellCache.values()) {
            if (jailId.equals(c.getJailId())) list.add(c);
        }
        return list;
    }

    public List<CriminalRecord> getCriminalsInCell(UUID cellId) {
        List<CriminalRecord> list = new ArrayList<>();
        for (CriminalRecord r : criminalCache.values()) {
            if (cellId.equals(r.getCellId()) && r.isServing()) list.add(r);
        }
        return list;
    }

    // ---- Jail Respawn Queue ----

    public void markForJailRespawn(UUID playerUUID, UUID nationId) {
        jailRespawnQueue.put(playerUUID, nationId);
    }

    public UUID consumeJailRespawn(UUID playerUUID) {
        return jailRespawnQueue.remove(playerUUID);
    }

    public void releasePlayer(CriminalRecord record) {
        record.setServing(false);
        record.setJailReleaseTime(System.currentTimeMillis());
        record.setCellId(null);
        record.setAssignedBedLocation(null);
        record.setEscaped(false);
        saveCriminalRecord(record);
        Player player = Bukkit.getPlayer(record.getCriminalUUID());
        if (player != null) {
            player.sendMessage("\u00a7aYou have been released from jail.");
            Location spawn = player.getWorld().getSpawnLocation();
            player.teleport(spawn.add(0.5, 0, 0.5));
        }
    }

    // ---- Helpers ----

    public static String locKey(Location loc) {
        return loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }

    public static Location parseLoc(String key) {
        String[] parts = key.split(",");
        if (parts.length != 4) return null;
        World world = Bukkit.getWorld(parts[0]);
        if (world == null) return null;
        return new Location(world, Double.parseDouble(parts[1]) + 0.5, Double.parseDouble(parts[2]), Double.parseDouble(parts[3]) + 0.5);
    }

    private void startJailCheckTask() {
        plugin.getPlatformScheduler().runGlobalTimer(() -> {
            for (CriminalRecord record : new ArrayList<>(criminalCache.values())) {
                if (record.isServing() && System.currentTimeMillis() >= record.getJailReleaseTime()) {
                    releasePlayer(record);
                }
            }
        }, 400L, 400L);
    }

    // ---- Bed & Escape ----

    public int countBedsInCell(CellData cell) {
        String locStr = cell.getLocation();
        if (locStr == null || locStr.isEmpty()) return 0;
        String worldName = locStr.split(",")[0];
        World world = Bukkit.getWorld(worldName);
        if (world == null) return 0;

        int minX = Math.min(cell.getCorner1X(), cell.getCorner2X());
        int maxX = Math.max(cell.getCorner1X(), cell.getCorner2X());
        int minY = Math.min(cell.getCorner1Y(), cell.getCorner2Y());
        int maxY = Math.max(cell.getCorner1Y(), cell.getCorner2Y());
        int minZ = Math.min(cell.getCorner1Z(), cell.getCorner2Z());
        int maxZ = Math.max(cell.getCorner1Z(), cell.getCorner2Z());

        int count = 0;
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Block block = world.getBlockAt(x, y, z);
                    String name = block.getType().name();
                    if (name.endsWith("_BED") || name.equals("BED_BLOCK") || name.equals("BED")) {
                        try {
                            org.bukkit.block.data.type.Bed bedData = (org.bukkit.block.data.type.Bed) block.getBlockData();
                            if (bedData.getPart() == org.bukkit.block.data.type.Bed.Part.HEAD) count++;
                        } catch (Exception e) {
                            count++;
                        }
                    }
                }
            }
        }
        return count;
    }

    public Location findUnassignedBedInCell(CellData cell) {
        String locStr = cell.getLocation();
        if (locStr == null || locStr.isEmpty()) return null;
        String worldName = locStr.split(",")[0];
        World world = Bukkit.getWorld(worldName);
        if (world == null) return null;

        Set<String> assignedBeds = new HashSet<>();
        for (CriminalRecord r : getCriminalsInCell(cell.getCellId())) {
            if (r.getAssignedBedLocation() != null) {
                assignedBeds.add(r.getAssignedBedLocation());
            }
        }

        int minX = Math.min(cell.getCorner1X(), cell.getCorner2X());
        int maxX = Math.max(cell.getCorner1X(), cell.getCorner2X());
        int minY = Math.min(cell.getCorner1Y(), cell.getCorner2Y());
        int maxY = Math.max(cell.getCorner1Y(), cell.getCorner2Y());
        int minZ = Math.min(cell.getCorner1Z(), cell.getCorner2Z());
        int maxZ = Math.max(cell.getCorner1Z(), cell.getCorner2Z());

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Block block = world.getBlockAt(x, y, z);
                    String name = block.getType().name();
                    if (name.endsWith("_BED") || name.equals("BED_BLOCK") || name.equals("BED")) {
                        boolean isHead = false;
                        try {
                            org.bukkit.block.data.type.Bed bedData = (org.bukkit.block.data.type.Bed) block.getBlockData();
                            isHead = bedData.getPart() == org.bukkit.block.data.type.Bed.Part.HEAD;
                        } catch (Exception e) {
                            isHead = true;
                        }
                        if (isHead) {
                            String bedKey = locKey(block.getLocation());
                            if (!assignedBeds.contains(bedKey)) {
                                return block.getLocation();
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    public CriminalRecord getServingRecord(UUID playerUUID) {
        for (CriminalRecord r : criminalCache.values()) {
            if (playerUUID.equals(r.getCriminalUUID()) && r.isServing() && !r.isEscaped()) {
                return r;
            }
        }
        return null;
    }

    public static String getBedHeadKey(Block block) {
        try {
            org.bukkit.block.data.type.Bed bedData = (org.bukkit.block.data.type.Bed) block.getBlockData();
            if (bedData.getPart() == org.bukkit.block.data.type.Bed.Part.HEAD) {
                return locKey(block.getLocation());
            }
            org.bukkit.block.BlockFace facing = bedData.getFacing();
            Block head = block.getRelative(facing);
            return locKey(head.getLocation());
        } catch (Exception e) {
            return locKey(block.getLocation());
        }
    }

    public boolean isInsideJail(JailData jail, Location loc) {
        if (loc.getWorld() == null || !jail.getWorld().equals(loc.getWorld().getName())) return false;
        int x = loc.getBlockX(), y = loc.getBlockY(), z = loc.getBlockZ();
        return x >= Math.min(jail.getCorner1X(), jail.getCorner2X())
                && x <= Math.max(jail.getCorner1X(), jail.getCorner2X())
                && y >= Math.min(jail.getCorner1Y(), jail.getCorner2Y())
                && y <= Math.max(jail.getCorner1Y(), jail.getCorner2Y())
                && z >= Math.min(jail.getCorner1Z(), jail.getCorner2Z())
                && z <= Math.max(jail.getCorner1Z(), jail.getCorner2Z());
    }

    private void startEscapeCheckTask() {
        plugin.getPlatformScheduler().runGlobalTimer(() -> {
            for (CriminalRecord record : new ArrayList<>(criminalCache.values())) {
                if (!record.isServing() || record.isEscaped() || record.getCellId() == null) continue;

                Player player = Bukkit.getPlayer(record.getCriminalUUID());
                if (player == null || !player.isOnline()) continue;

                CellData cell = loadCell(record.getCellId());
                if (cell == null) continue;

                JailData jail = loadJail(cell.getJailId());
                if (jail == null) continue;

                if (!isInsideJail(jail, player.getLocation())) {
                    long remaining = record.getJailReleaseTime() - System.currentTimeMillis();
                    if (remaining > 0) {
                        record.setJailReleaseTime(record.getJailReleaseTime() + (remaining / 2));
                    }
                    record.setEscaped(true);
                    record.setCellId(null);
                    record.setAssignedBedLocation(null);
                    saveCriminalRecord(record);
                    player.sendMessage("\u00a74\u00a7lESCAPED! \u00a7cYou are now a fugitive. Your sentence has been extended.");

                    NationData nation = plugin.getNationManager().loadNation(record.getNationId());
                    if (nation != null) {
                        String crimName = "Unknown";
                        CharacterData cd = plugin.getCharacterManager().getCharacter(record.getCriminalUUID());
                        if (cd != null) crimName = cd.getFirstName() + " " + cd.getLastName();
                        String alert = "\u00a7c\u00a7l[ALERT] \u00a7ePrisoner \u00a7f" + crimName + " \u00a7ehas escaped from jail!";
                        if (nation.getSecurityHeadUUID() != null) {
                            Player sh = Bukkit.getPlayer(nation.getSecurityHeadUUID());
                            if (sh != null) sh.sendMessage(alert);
                        }
                        for (UUID enfUUID : nation.getEnforcerUUIDs()) {
                            Player enf = Bukkit.getPlayer(enfUUID);
                            if (enf != null) enf.sendMessage(alert);
                        }
                    }
                }
            }
        }, 40L, 40L);
    }

    // ---- Disk Loading ----

    private void loadAllData() {
        loadLawBooks();
        loadCrimes();
        loadCriminals();
        loadCells();
        loadJails();
        plugin.getLogger().info("[LawCrimeManager] Cached " + lawBookCache.size() + " law books, " + crimeCache.size() + " crimes, " + criminalCache.size() + " records, " + jailCache.size() + " jails, " + cellCache.size() + " cells.");
    }

    private void loadLawBooks() {
        File[] files = lawBooksFolder.listFiles((d, n) -> n.endsWith(".yml"));
        if (files == null) return;
        for (File f : files) {
            try {
                UUID id = UUID.fromString(f.getName().replace(".yml", ""));
                YamlConfiguration cfg = YamlConfiguration.loadConfiguration(f);
                LawBookData b = new LawBookData();
                b.setBookId(id);
                b.setNationId(UUID.fromString(cfg.getString("nationId")));
                String auth = cfg.getString("authorUUID");
                if (auth != null) b.setAuthorUUID(UUID.fromString(auth));
                b.setTitle(cfg.getString("title", ""));
                b.setSections(new ArrayList<>(cfg.getStringList("sections")));
                b.setApproved(cfg.getBoolean("approved", true));
                b.setPendingApproval(cfg.getBoolean("pendingApproval", false));
                lawBookCache.put(id, b);
            } catch (Exception ignored) {}
        }
    }

    private void loadCrimes() {
        File[] files = crimesFolder.listFiles((d, n) -> n.endsWith(".yml"));
        if (files == null) return;
        for (File f : files) {
            try {
                UUID id = UUID.fromString(f.getName().replace(".yml", ""));
                YamlConfiguration cfg = YamlConfiguration.loadConfiguration(f);
                LegislatedCrimeData c = new LegislatedCrimeData();
                c.setCrimeId(id);
                c.setNationId(UUID.fromString(cfg.getString("nationId")));
                c.setName(cfg.getString("name", ""));
                c.setMinJailDays(cfg.getInt("minJailDays", 1));
                c.setFineAmount(cfg.getDouble("fineAmount", 0));
                c.setApproved(cfg.getBoolean("approved", true));
                c.setPendingApproval(cfg.getBoolean("pendingApproval", false));
                c.setLinkedLawSections(new ArrayList<>(cfg.getStringList("linkedLawSections")));
                crimeCache.put(id, c);
            } catch (Exception ignored) {}
        }
    }

    private void loadCriminals() {
        File[] files = criminalsFolder.listFiles((d, n) -> n.endsWith(".yml"));
        if (files == null) return;
        for (File f : files) {
            try {
                UUID id = UUID.fromString(f.getName().replace(".yml", ""));
                YamlConfiguration cfg = YamlConfiguration.loadConfiguration(f);
                CriminalRecord r = new CriminalRecord();
                r.setRecordId(id);
                r.setNationId(UUID.fromString(cfg.getString("nationId")));
                r.setCriminalUUID(UUID.fromString(cfg.getString("criminalUUID")));
                List<UUID> cids = new ArrayList<>();
                for (String s : cfg.getStringList("crimeIds")) {
                    try { cids.add(UUID.fromString(s)); } catch (Exception ignored) {}
                }
                r.setCrimeIds(cids);
                r.setDescription(cfg.getString("description", ""));
                r.setConvictedTimestamp(cfg.getLong("convictedTimestamp", 0));
                r.setJailReleaseTime(cfg.getLong("jailReleaseTime", 0));
                r.setServing(cfg.getBoolean("serving", false));
                String cellIdStr = cfg.getString("cellId");
                if (cellIdStr != null && !cellIdStr.isEmpty()) {
                    try { r.setCellId(UUID.fromString(cellIdStr)); } catch (Exception ignored) {}
                }
                r.setEscaped(cfg.getBoolean("escaped", false));
                r.setAssignedBedLocation(cfg.getString("assignedBedLocation"));
                criminalCache.put(id, r);
            } catch (Exception ignored) {}
        }
    }

    private void loadCells() {
        File[] files = cellsFolder.listFiles((d, n) -> n.endsWith(".yml"));
        if (files == null) return;
        for (File f : files) {
            try {
                UUID id = UUID.fromString(f.getName().replace(".yml", ""));
                YamlConfiguration cfg = YamlConfiguration.loadConfiguration(f);
                CellData c = new CellData();
                c.setCellId(id);
                c.setJailId(UUID.fromString(cfg.getString("jailId")));
                c.setName(cfg.getString("name", ""));
                c.setLocation(cfg.getString("location", ""));
                c.setCorner1X(cfg.getInt("corner1X", 0));
                c.setCorner1Y(cfg.getInt("corner1Y", 0));
                c.setCorner1Z(cfg.getInt("corner1Z", 0));
                c.setCorner2X(cfg.getInt("corner2X", 0));
                c.setCorner2Y(cfg.getInt("corner2Y", 0));
                c.setCorner2Z(cfg.getInt("corner2Z", 0));
                cellCache.put(id, c);
            } catch (Exception ignored) {}
        }
    }

    private void loadJails() {
        File[] files = jailsFolder.listFiles((d, n) -> n.endsWith(".yml"));
        if (files == null) return;
        for (File f : files) {
            try {
                UUID id = UUID.fromString(f.getName().replace(".yml", ""));
                YamlConfiguration cfg = YamlConfiguration.loadConfiguration(f);
                JailData j = new JailData();
                j.setJailId(id);
                j.setNationId(UUID.fromString(cfg.getString("nationId")));
                j.setName(cfg.getString("name", ""));
                j.setWorld(cfg.getString("world", ""));
                j.setCorner1X(cfg.getInt("corner1X", 0));
                j.setCorner1Y(cfg.getInt("corner1Y", 0));
                j.setCorner1Z(cfg.getInt("corner1Z", 0));
                j.setCorner2X(cfg.getInt("corner2X", 0));
                j.setCorner2Y(cfg.getInt("corner2Y", 0));
                j.setCorner2Z(cfg.getInt("corner2Z", 0));
                List<String> cellIdStrs = cfg.getStringList("cellIds");
                if (!cellIdStrs.isEmpty()) {
                    for (String s : cellIdStrs) {
                        try { j.getCellIds().add(UUID.fromString(s)); } catch (Exception ignored) {}
                    }
                } else {
                    List<String> oldLocs = cfg.getStringList("cellLocations");
                    for (String locStr : oldLocs) {
                        CellData cell = new CellData();
                        cell.setCellId(UUID.randomUUID());
                        cell.setJailId(id);
                        cell.setLocation(locStr);
                        cell.setCellLimit(2);
                        saveCell(cell);
                        j.getCellIds().add(cell.getCellId());
                    }
                    if (!oldLocs.isEmpty()) {
                        jailCache.put(id, j);
                        saveJail(j);
                    }
                }
                jailCache.put(id, j);
            } catch (Exception ignored) {}
        }
    }
}
package com.dirt.managers;

import com.dirt.DirtEconomy;
import com.dirt.config.RolesConfig;
import com.dirt.data.ConservationAreaData;
import com.dirt.data.CustomRole;
import com.dirt.data.NationData;
import com.dirt.data.NationPermission;
import com.dirt.data.NationTransaction;
import com.dirt.data.PropertyData;
import com.dirt.data.PropertyPurchaseRequest;
import com.dirt.data.RegionData;
import com.dirt.data.RoleMode;
import com.dirt.data.RoomData;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class NationManager {
    private final DirtEconomy plugin;
    private final File nationsFolder;
    private final File regionsFolder;
    private final File propertiesFolder;
    private final File requestsFolder;
    private final File transactionsFolder;
    private final File conservationFolder;
    private final File roomsFolder;

    private final Map<UUID, NationData> nationCache = new HashMap<>();
    private final Map<UUID, RegionData> regionCache = new HashMap<>();
    private final Map<UUID, PropertyData> propertyCache = new HashMap<>();
    private final Map<UUID, PropertyPurchaseRequest> requestCache = new HashMap<>();
    private final Map<UUID, ConservationAreaData> conservationCache = new HashMap<>();
    private final Map<UUID, RoomData> roomCache = new HashMap<>();

    private final Map<String, UUID> chunkToRegion = new HashMap<>();
    private final Map<String, UUID> chunkToProperty = new HashMap<>();
    private final Map<String, UUID> chunkToConservation = new HashMap<>();
    private final Map<String, UUID> signToProperty = new HashMap<>();
    private final Map<UUID, UUID> memberToNation = new HashMap<>();

    public NationManager(DirtEconomy plugin) {
        this.plugin = plugin;
        this.nationsFolder = new File(plugin.getDataFolder(), "data/MTN/nations");
        this.regionsFolder = new File(plugin.getDataFolder(), "data/MTN/regions");
        this.propertiesFolder = new File(plugin.getDataFolder(), "data/MTN/properties");
        this.requestsFolder = new File(plugin.getDataFolder(), "data/MTN/purchaserequests");
        this.transactionsFolder = new File(plugin.getDataFolder(), "data/MTN/nationtransactions");
        this.conservationFolder = new File(plugin.getDataFolder(), "data/MTN/conservationareas");
        this.roomsFolder = new File(plugin.getDataFolder(), "data/MTN/rooms");
        nationsFolder.mkdirs();
        regionsFolder.mkdirs();
        propertiesFolder.mkdirs();
        requestsFolder.mkdirs();
        transactionsFolder.mkdirs();
        conservationFolder.mkdirs();
        roomsFolder.mkdirs();
        loadAllData();
    }

    public void reloadCache() {
        nationCache.clear();
        regionCache.clear();
        propertyCache.clear();
        requestCache.clear();
        conservationCache.clear();
        roomCache.clear();
        chunkToRegion.clear();
        chunkToProperty.clear();
        chunkToConservation.clear();
        signToProperty.clear();
        memberToNation.clear();
        loadAllData();
    }

    private void loadAllData() {
        loadNationsFromDisk();
        loadRegionsFromDisk();
        loadPropertiesFromDisk();
        loadRequestsFromDisk();
        loadConservationFromDisk();
        loadRoomsFromDisk();
        rebuildAllIndexes();
        plugin.getLogger().info("[NationManager] Cached " + nationCache.size() + " nations, "
                + regionCache.size() + " regions, " + propertyCache.size() + " properties, "
                + conservationCache.size() + " conservation areas.");
    }

    private void rebuildAllIndexes() {
        chunkToRegion.clear();
        chunkToProperty.clear();
        chunkToConservation.clear();
        signToProperty.clear();
        memberToNation.clear();
        for (RegionData r : regionCache.values()) {
            for (String chunk : r.getClaimedChunks()) chunkToRegion.put(chunk, r.getRegionId());
        }
        for (PropertyData p : propertyCache.values()) {
            for (String chunk : p.getChunks()) chunkToProperty.put(chunk, p.getPropertyId());
            if (p.getSaleSignLocation() != null) signToProperty.put(p.getSaleSignLocation(), p.getPropertyId());
        }
        for (ConservationAreaData a : conservationCache.values()) {
            for (String chunk : a.getChunks()) chunkToConservation.put(chunk, a.getAreaId());
        }
        for (NationData n : nationCache.values()) {
            for (UUID member : n.getMemberUUIDs()) memberToNation.put(member, n.getNationId());
        }
    }

    // ---- Nations ----

    public NationData createNation(String name, String color1, String color2, UUID presidentUUID) {
        NationData nation = new NationData();
        nation.setNationId(UUID.randomUUID());
        nation.setName(name);
        nation.setColor1(color1);
        nation.setColor2(color2);
        nation.setPresidentUUID(presidentUUID);
        nation.getMemberUUIDs().add(presidentUUID);
        saveNation(nation);
        if (plugin.getEconomy().hasBankSupport()) {
            plugin.getEconomy().createBank(name, plugin.getServer().getOfflinePlayer(presidentUUID));
        }
        if (plugin.getDiscordBotManager() != null) {
            plugin.getDiscordBotManager().onNationCreated(nation);
        }
        return nation;
    }

    public NationData createNationCustom(String name, String color1, String color2) {
        NationData nation = new NationData();
        nation.setNationId(UUID.randomUUID());
        nation.setName(name);
        nation.setColor1(color1);
        nation.setColor2(color2);
        nation.setCustomGovernment(true);
        saveNation(nation);
        if (plugin.getDiscordBotManager() != null) {
            plugin.getDiscordBotManager().onNationCreated(nation);
        }
        return nation;
    }

    public NationData loadNation(UUID nationId) {
        if (nationId == null) return null;
        return nationCache.get(nationId);
    }

    public void saveNation(NationData data) {
        nationCache.put(data.getNationId(), data);
        memberToNation.values().removeIf(nid -> nid.equals(data.getNationId()));
        for (UUID member : data.getMemberUUIDs()) memberToNation.put(member, data.getNationId());

        File file = new File(nationsFolder, data.getNationId() + ".yml");
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("name", data.getName());
        cfg.set("color1", data.getColor1());
        cfg.set("color2", data.getColor2());
        cfg.set("elitesConfigured", data.isElitesConfigured());
        cfg.set("taxRate", data.getTaxRate());
        cfg.set("lowerClassTaxRate", data.getLowerClassTaxRate());
        cfg.set("middleClassTaxRate", data.getMiddleClassTaxRate());
        cfg.set("upperClassTaxRate", data.getUpperClassTaxRate());
        cfg.set("taxCollectionIntervalDays", data.getTaxCollectionIntervalDays());
        cfg.set("treasuryBalance", data.getTreasuryBalance());
        cfg.set("customGovernment", data.isCustomGovernment());
        cfg.set("presidentUUID", data.getPresidentUUID() != null ? data.getPresidentUUID().toString() : null);
        cfg.set("treasurerUUID", data.getTreasurerUUID() != null ? data.getTreasurerUUID().toString() : null);
        cfg.set("vicePresidentUUID", data.getVicePresidentUUID() != null ? data.getVicePresidentUUID().toString() : null);
        cfg.set("securityHeadUUID", data.getSecurityHeadUUID() != null ? data.getSecurityHeadUUID().toString() : null);
        List<String> members = new ArrayList<>();
        for (UUID u : data.getMemberUUIDs()) members.add(u.toString());
        cfg.set("memberUUIDs", members);
        List<String> regions = new ArrayList<>();
        for (UUID u : data.getRegionIds()) regions.add(u.toString());
        cfg.set("regionIds", regions);
        List<String> joinReqs = new ArrayList<>();
        for (UUID u : data.getJoinRequestUUIDs()) joinReqs.add(u.toString());
        cfg.set("joinRequestUUIDs", joinReqs);
        List<String> govProps = new ArrayList<>();
        for (UUID u : data.getGovernmentPropertyIds()) govProps.add(u.toString());
        cfg.set("governmentPropertyIds", govProps);
        List<String> enforcers = new ArrayList<>();
        for (UUID u : data.getEnforcerUUIDs()) enforcers.add(u.toString());
        cfg.set("enforcerUUIDs", enforcers);
        for (Map.Entry<UUID, Double> entry : data.getEnforcerSalaries().entrySet()) {
            cfg.set("enforcerSalaries." + entry.getKey().toString(), entry.getValue());
        }
        for (Map.Entry<UUID, Integer> entry : data.getEnforcerCriminalsCaught().entrySet()) {
            cfg.set("enforcerCriminalsCaught." + entry.getKey().toString(), entry.getValue());
        }
        if (data.getCustomRoles() != null) {
            for (CustomRole role : data.getCustomRoles()) {
                String base = "customRoles." + role.getRoleId().toString();
                cfg.set(base + ".name", role.getName());
                cfg.set(base + ".colorCode", role.getColorCode());
                cfg.set(base + ".roleMode", role.getRoleMode().name());
                List<String> permNames = new ArrayList<>();
                for (NationPermission p : role.getPermissions()) permNames.add(p.name());
                cfg.set(base + ".permissions", permNames);
                List<String> roleMembers = new ArrayList<>();
                for (UUID u : role.getMemberUUIDs()) roleMembers.add(u.toString());
                cfg.set(base + ".members", roleMembers);
            }
        }
        try { cfg.save(file); } catch (IOException e) { e.printStackTrace(); }
    }

    public void deleteNation(UUID nationId) {
        NationData removed = nationCache.remove(nationId);
        if (removed != null) {
            memberToNation.values().removeIf(nid -> nid.equals(nationId));
            if (plugin.getDiscordBotManager() != null) {
                plugin.getDiscordBotManager().onNationDeleted(removed);
            }
        }
        new File(nationsFolder, nationId + ".yml").delete();
    }

    public List<NationData> getAllNations() {
        return new ArrayList<>(nationCache.values());
    }

    public NationData getNationByPresident(UUID presidentUUID) {
        for (NationData n : nationCache.values()) {
            if (presidentUUID.equals(n.getPresidentUUID())) return n;
        }
        return null;
    }

    public NationData getNationByMember(UUID memberUUID) {
        UUID nationId = memberToNation.get(memberUUID);
        return nationId != null ? nationCache.get(nationId) : null;
    }

    public NationData getNationByName(String name) {
        for (NationData n : nationCache.values()) {
            if (n.getName().equalsIgnoreCase(name)) return n;
        }
        return null;
    }

    public void removeMember(NationData nation, UUID memberUUID) {
        nation.getMemberUUIDs().remove(memberUUID);
        if (memberUUID.equals(nation.getTreasurerUUID())) nation.setTreasurerUUID(null);
        if (memberUUID.equals(nation.getVicePresidentUUID())) nation.setVicePresidentUUID(null);
        if (memberUUID.equals(nation.getSecurityHeadUUID())) nation.setSecurityHeadUUID(null);
        for (CustomRole role : nation.getCustomRoles()) {
            role.getMemberUUIDs().remove(memberUUID);
        }
        RolesConfig rc = plugin.getRolesConfig();
        boolean filled = true;
        if (rc.isTreasurerEnabled() && nation.getTreasurerUUID() == null) filled = false;
        if (rc.isVicePresidentEnabled() && nation.getVicePresidentUUID() == null) filled = false;
        if (rc.isSecurityHeadEnabled() && nation.getSecurityHeadUUID() == null) filled = false;
        if (!filled) {
            nation.setElitesConfigured(false);
        }
        saveNation(nation);
    }

    public NationData getNationByGovernor(UUID governorUUID) {
        for (RegionData r : regionCache.values()) {
            if (governorUUID.equals(r.getGovernorUUID())) {
                return nationCache.get(r.getNationId());
            }
        }
        return null;
    }

    // ---- Regions ----

    public RegionData createRegion(UUID nationId, String name, UUID governorUUID) {
        RegionData region = new RegionData();
        region.setRegionId(UUID.randomUUID());
        region.setNationId(nationId);
        region.setName(name);
        region.setGovernorUUID(governorUUID);
        saveRegion(region);

        NationData nation = nationCache.get(nationId);
        if (nation != null) {
            nation.getRegionIds().add(region.getRegionId());
            saveNation(nation);
        }
        return region;
    }

    public RegionData loadRegion(UUID regionId) {
        if (regionId == null) return null;
        return regionCache.get(regionId);
    }

    public void saveRegion(RegionData data) {
        chunkToRegion.values().removeIf(rid -> rid.equals(data.getRegionId()));
        for (String chunk : data.getClaimedChunks()) chunkToRegion.put(chunk, data.getRegionId());
        regionCache.put(data.getRegionId(), data);

        File file = new File(regionsFolder, data.getRegionId() + ".yml");
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("nationId", data.getNationId() != null ? data.getNationId().toString() : null);
        cfg.set("name", data.getName());
        cfg.set("governorUUID", data.getGovernorUUID() != null ? data.getGovernorUUID().toString() : null);
        cfg.set("propertyChunkRate", data.getPropertyChunkRate());
        cfg.set("propertyCollectionIntervalDays", data.getPropertyCollectionIntervalDays());
        cfg.set("claimedChunks", data.getClaimedChunks());
        List<String> props = new ArrayList<>();
        for (UUID u : data.getPropertyIds()) props.add(u.toString());
        cfg.set("propertyIds", props);
        savePermMap(cfg, "permsSameRegion", data.getPermsSameRegion());
        savePermMap(cfg, "permsSameNation", data.getPermsSameNation());
        savePermMap(cfg, "permsForeign", data.getPermsForeign());
        cfg.set("deniedMobs", new ArrayList<>(data.getDeniedMobs()));
        try { cfg.save(file); } catch (IOException e) { e.printStackTrace(); }
    }

    public void deleteRegion(UUID regionId) {
        RegionData removed = regionCache.remove(regionId);
        if (removed != null) {
            chunkToRegion.values().removeIf(rid -> rid.equals(regionId));
        }
        new File(regionsFolder, regionId + ".yml").delete();
    }

    public void deleteRegionAndCleanup(UUID regionId) {
        RegionData region = regionCache.get(regionId);
        if (region == null) return;

        for (UUID propId : new ArrayList<>(region.getPropertyIds())) {
            deleteProperty(propId);
        }
        for (PropertyPurchaseRequest req : getPurchaseRequestsByRegion(regionId)) {
            deletePurchaseRequest(req.getRequestId());
        }
        if (region.getNationId() != null) {
            NationData nation = nationCache.get(region.getNationId());
            if (nation != null) {
                nation.getRegionIds().remove(regionId);
                saveNation(nation);
            }
        }
        deleteRegion(regionId);
    }

    public List<RegionData> getAllRegions() {
        return new ArrayList<>(regionCache.values());
    }

    public List<RegionData> getRegionsByNation(UUID nationId) {
        List<RegionData> list = new ArrayList<>();
        for (RegionData r : regionCache.values()) {
            if (nationId.equals(r.getNationId())) list.add(r);
        }
        return list;
    }

    public List<RegionData> getRegionsByGovernor(UUID governorUUID) {
        List<RegionData> list = new ArrayList<>();
        for (RegionData r : regionCache.values()) {
            if (governorUUID.equals(r.getGovernorUUID())) list.add(r);
        }
        return list;
    }

    public RegionData getRegionByChunk(String chunkKey) {
        UUID regionId = chunkToRegion.get(chunkKey);
        return regionId != null ? regionCache.get(regionId) : null;
    }

    // ---- Properties ----

    public PropertyData createProperty(UUID regionId, UUID ownerUUID, List<String> chunks, double price) {
        PropertyData prop = new PropertyData();
        prop.setPropertyId(UUID.randomUUID());
        prop.setRegionId(regionId);
        prop.setOwnerUUID(ownerUUID);
        prop.setChunks(new ArrayList<>(chunks));
        prop.setSalePrice(price);
        saveProperty(prop);

        RegionData region = regionCache.get(regionId);
        if (region != null) {
            region.getPropertyIds().add(prop.getPropertyId());
            saveRegion(region);
        }
        return prop;
    }

    public PropertyData loadProperty(UUID propertyId) {
        if (propertyId == null) return null;
        return propertyCache.get(propertyId);
    }

    public void saveProperty(PropertyData data) {
        chunkToProperty.values().removeIf(pid -> pid.equals(data.getPropertyId()));
        for (String chunk : data.getChunks()) chunkToProperty.put(chunk, data.getPropertyId());
        signToProperty.values().removeIf(pid -> pid.equals(data.getPropertyId()));
        if (data.getSaleSignLocation() != null) signToProperty.put(data.getSaleSignLocation(), data.getPropertyId());
        propertyCache.put(data.getPropertyId(), data);

        File file = new File(propertiesFolder, data.getPropertyId() + ".yml");
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("regionId", data.getRegionId() != null ? data.getRegionId().toString() : null);
        cfg.set("ownerUUID", data.getOwnerUUID() != null ? data.getOwnerUUID().toString() : null);
        cfg.set("name", data.getName());
        cfg.set("chunks", data.getChunks());
        cfg.set("forSale", data.isForSale());
        cfg.set("salePrice", data.getSalePrice());
        cfg.set("saleSignLocation", data.getSaleSignLocation());
        if (data.getOffers() != null && !data.getOffers().isEmpty()) {
            for (Map.Entry<UUID, Double> entry : data.getOffers().entrySet()) {
                cfg.set("offers." + entry.getKey().toString(), entry.getValue());
            }
        }
        savePermMap(cfg, "permsSameRegion", data.getPermsSameRegion());
        savePermMap(cfg, "permsSameNation", data.getPermsSameNation());
        savePermMap(cfg, "permsForeign", data.getPermsForeign());
        cfg.set("deniedMobs", new ArrayList<>(data.getDeniedMobs()));
        List<String> roomIdStrs = new ArrayList<>();
        for (UUID rid : data.getRoomIds()) roomIdStrs.add(rid.toString());
        cfg.set("roomIds", roomIdStrs);
        try { cfg.save(file); } catch (IOException e) { e.printStackTrace(); }
    }

    public void deleteProperty(UUID propertyId) {
        PropertyData prop = propertyCache.remove(propertyId);
        if (prop != null) {
            chunkToProperty.values().removeIf(pid -> pid.equals(propertyId));
            signToProperty.values().removeIf(pid -> pid.equals(propertyId));
            RegionData region = regionCache.get(prop.getRegionId());
            if (region != null) {
                region.getPropertyIds().remove(propertyId);
                saveRegion(region);
            }
        }
        new File(propertiesFolder, propertyId + ".yml").delete();
    }

    public List<PropertyData> getAllProperties() {
        return new ArrayList<>(propertyCache.values());
    }

    public List<PropertyData> getPropertiesByOwner(UUID ownerUUID) {
        List<PropertyData> list = new ArrayList<>();
        for (PropertyData p : propertyCache.values()) {
            if (ownerUUID.equals(p.getOwnerUUID())) list.add(p);
        }
        return list;
    }

    public List<PropertyData> getPropertiesByRegion(UUID regionId) {
        List<PropertyData> list = new ArrayList<>();
        for (PropertyData p : propertyCache.values()) {
            if (regionId.equals(p.getRegionId())) list.add(p);
        }
        return list;
    }

    public PropertyData getPropertyBySaleSignLocation(String locKey) {
        UUID propId = signToProperty.get(locKey);
        return propId != null ? propertyCache.get(propId) : null;
    }

    public PropertyData getPropertyByChunk(String chunkKey) {
        UUID propId = chunkToProperty.get(chunkKey);
        return propId != null ? propertyCache.get(propId) : null;
    }

    // ---- Purchase Requests ----

    public PropertyPurchaseRequest createPurchaseRequest(UUID regionId, UUID requesterUUID, List<String> chunks, double price) {
        PropertyPurchaseRequest req = new PropertyPurchaseRequest();
        req.setRequestId(UUID.randomUUID());
        req.setRegionId(regionId);
        req.setRequesterUUID(requesterUUID);
        req.setRequestedChunks(new ArrayList<>(chunks));
        req.setTotalPrice(price);
        savePurchaseRequest(req);
        return req;
    }

    public PropertyPurchaseRequest loadPurchaseRequest(UUID requestId) {
        if (requestId == null) return null;
        return requestCache.get(requestId);
    }

    public void savePurchaseRequest(PropertyPurchaseRequest req) {
        requestCache.put(req.getRequestId(), req);
        File file = new File(requestsFolder, req.getRequestId() + ".yml");
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("regionId", req.getRegionId() != null ? req.getRegionId().toString() : null);
        cfg.set("requesterUUID", req.getRequesterUUID() != null ? req.getRequesterUUID().toString() : null);
        cfg.set("requestedChunks", req.getRequestedChunks());
        cfg.set("totalPrice", req.getTotalPrice());
        cfg.set("governorApproved", req.isGovernorApproved());
        cfg.set("treasurerApproved", req.isTreasurerApproved());
        cfg.set("propertyName", req.getPropertyName());
        try { cfg.save(file); } catch (IOException e) { e.printStackTrace(); }
    }

    public void deletePurchaseRequest(UUID requestId) {
        requestCache.remove(requestId);
        new File(requestsFolder, requestId + ".yml").delete();
    }

    public List<PropertyPurchaseRequest> getAllPurchaseRequests() {
        return new ArrayList<>(requestCache.values());
    }

    public List<PropertyPurchaseRequest> getPurchaseRequestsByRegion(UUID regionId) {
        List<PropertyPurchaseRequest> list = new ArrayList<>();
        for (PropertyPurchaseRequest r : requestCache.values()) {
            if (regionId.equals(r.getRegionId())) list.add(r);
        }
        return list;
    }

    public List<PropertyPurchaseRequest> getPurchaseRequestsByNation(UUID nationId) {
        List<PropertyPurchaseRequest> list = new ArrayList<>();
        for (RegionData region : getRegionsByNation(nationId)) {
            list.addAll(getPurchaseRequestsByRegion(region.getRegionId()));
        }
        return list;
    }

    // ---- Conservation Areas ----

    public ConservationAreaData createConservationArea(UUID regionId, String name, List<String> chunks) {
        ConservationAreaData area = new ConservationAreaData();
        area.setAreaId(UUID.randomUUID());
        area.setRegionId(regionId);
        area.setName(name);
        area.setChunks(new ArrayList<>(chunks));
        saveConservationArea(area);
        return area;
    }

    public ConservationAreaData loadConservationArea(UUID areaId) {
        if (areaId == null) return null;
        return conservationCache.get(areaId);
    }

    public void saveConservationArea(ConservationAreaData data) {
        chunkToConservation.values().removeIf(aid -> aid.equals(data.getAreaId()));
        for (String chunk : data.getChunks()) chunkToConservation.put(chunk, data.getAreaId());
        conservationCache.put(data.getAreaId(), data);

        File file = new File(conservationFolder, data.getAreaId() + ".yml");
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("regionId", data.getRegionId() != null ? data.getRegionId().toString() : null);
        cfg.set("name", data.getName());
        cfg.set("chunks", data.getChunks());
        try { cfg.save(file); } catch (IOException e) { e.printStackTrace(); }
    }

    public void deleteConservationArea(UUID areaId) {
        ConservationAreaData removed = conservationCache.remove(areaId);
        if (removed != null) {
            chunkToConservation.values().removeIf(aid -> aid.equals(areaId));
        }
        new File(conservationFolder, areaId + ".yml").delete();
    }

    public List<ConservationAreaData> getAllConservationAreas() {
        return new ArrayList<>(conservationCache.values());
    }

    public List<ConservationAreaData> getConservationAreasByRegion(UUID regionId) {
        List<ConservationAreaData> list = new ArrayList<>();
        for (ConservationAreaData a : conservationCache.values()) {
            if (regionId.equals(a.getRegionId())) list.add(a);
        }
        return list;
    }

    public ConservationAreaData getConservationAreaByChunk(String chunkKey) {
        UUID areaId = chunkToConservation.get(chunkKey);
        return areaId != null ? conservationCache.get(areaId) : null;
    }

    // ---- Rooms ----

    public RoomData createRoom(UUID propertyId, String name, String world, int[] corner1, int[] corner2) {
        RoomData room = new RoomData();
        room.setRoomId(UUID.randomUUID());
        room.setPropertyId(propertyId);
        room.setName(name);
        room.setWorld(world);
        room.setCorner1X(corner1[0]);
        room.setCorner1Y(corner1[1]);
        room.setCorner1Z(corner1[2]);
        room.setCorner2X(corner2[0]);
        room.setCorner2Y(corner2[1]);
        room.setCorner2Z(corner2[2]);
        saveRoom(room);
        return room;
    }

    public void saveRoom(RoomData data) {
        roomCache.put(data.getRoomId(), data);
        File file = new File(roomsFolder, data.getRoomId() + ".yml");
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("propertyId", data.getPropertyId().toString());
        cfg.set("name", data.getName());
        cfg.set("world", data.getWorld());
        cfg.set("corner1X", data.getCorner1X());
        cfg.set("corner1Y", data.getCorner1Y());
        cfg.set("corner1Z", data.getCorner1Z());
        cfg.set("corner2X", data.getCorner2X());
        cfg.set("corner2Y", data.getCorner2Y());
        cfg.set("corner2Z", data.getCorner2Z());
        cfg.set("ceilingY", data.getCeilingY());
        savePermMap(cfg, "permsSameRegion", data.getPermsSameRegion());
        savePermMap(cfg, "permsSameNation", data.getPermsSameNation());
        savePermMap(cfg, "permsForeign", data.getPermsForeign());
        if (data.getCharacterPerms() != null) {
            for (Map.Entry<UUID, Map<String, Boolean>> entry : data.getCharacterPerms().entrySet()) {
                savePermMap(cfg, "characterPerms." + entry.getKey().toString(), entry.getValue());
            }
        }
        try { cfg.save(file); } catch (IOException e) { e.printStackTrace(); }
    }

    public RoomData loadRoom(UUID roomId) {
        return roomCache.get(roomId);
    }

    public void deleteRoom(UUID roomId) {
        roomCache.remove(roomId);
        new File(roomsFolder, roomId + ".yml").delete();
    }

    public List<RoomData> getRoomsByProperty(UUID propertyId) {
        List<RoomData> list = new ArrayList<>();
        for (RoomData r : roomCache.values()) {
            if (propertyId.equals(r.getPropertyId())) list.add(r);
        }
        return list;
    }

    // ---- Helpers ----

    public static String chunkKey(String world, int cx, int cz) {
        return world + "," + cx + "," + cz;
    }

    private static void savePermMap(YamlConfiguration cfg, String key, Map<String, Boolean> map) {
        if (map == null) return;
        for (Map.Entry<String, Boolean> entry : map.entrySet()) {
            cfg.set(key + "." + entry.getKey(), entry.getValue());
        }
    }

    private static Map<String, Boolean> loadPermMap(YamlConfiguration cfg, String key, Map<String, Boolean> defaults) {
        if (!cfg.isConfigurationSection(key)) return defaults;
        Map<String, Boolean> map = new HashMap<>(defaults);
        for (String k : cfg.getConfigurationSection(key).getKeys(false)) {
            map.put(k, cfg.getBoolean(key + "." + k));
        }
        return map;
    }

    // ---- Transactions ----

    public void logTransaction(UUID nationId, String type, double amount, String description) {
        File file = new File(transactionsFolder, nationId + ".yml");
        YamlConfiguration cfg = file.exists() ? YamlConfiguration.loadConfiguration(file) : new YamlConfiguration();
        String key = String.valueOf(System.currentTimeMillis()) + "_" + (int)(Math.random() * 10000);
        cfg.set("transactions." + key + ".type", type);
        cfg.set("transactions." + key + ".amount", amount);
        cfg.set("transactions." + key + ".description", description);
        cfg.set("transactions." + key + ".timestamp", System.currentTimeMillis());
        try { cfg.save(file); } catch (IOException e) { e.printStackTrace(); }
    }

    public List<NationTransaction> loadTransactions(UUID nationId) {
        List<NationTransaction> list = new ArrayList<>();
        File file = new File(transactionsFolder, nationId + ".yml");
        if (!file.exists()) return list;
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        if (!cfg.isConfigurationSection("transactions")) return list;
        for (String key : cfg.getConfigurationSection("transactions").getKeys(false)) {
            NationTransaction tx = new NationTransaction();
            tx.setType(cfg.getString("transactions." + key + ".type"));
            tx.setAmount(cfg.getDouble("transactions." + key + ".amount"));
            tx.setDescription(cfg.getString("transactions." + key + ".description"));
            tx.setTimestamp(cfg.getLong("transactions." + key + ".timestamp"));
            list.add(tx);
        }
        list.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
        return list;
    }

    public double getTreasuryBalance(String nationName) {
        NationData nation = getNationByName(nationName);
        return nation != null ? nation.getTreasuryBalance() : 0.0;
    }

    public boolean depositToTreasury(String nationName, double amount) {
        NationData nation = getNationByName(nationName);
        if (nation == null) return false;
        nation.setTreasuryBalance(nation.getTreasuryBalance() + amount);
        saveNation(nation);
        return true;
    }

    public boolean withdrawFromTreasury(String nationName, double amount) {
        NationData nation = getNationByName(nationName);
        if (nation == null || nation.getTreasuryBalance() < amount) return false;
        nation.setTreasuryBalance(nation.getTreasuryBalance() - amount);
        saveNation(nation);
        return true;
    }

    // ---- Disk loading (used only during startup / reload) ----

    private void loadNationsFromDisk() {
        File[] files = nationsFolder.listFiles((d, n) -> n.endsWith(".yml"));
        if (files == null) return;
        for (File f : files) {
            try {
                UUID id = UUID.fromString(f.getName().replace(".yml", ""));
                NationData data = readNationFile(id, f);
                if (data != null) nationCache.put(id, data);
            } catch (IllegalArgumentException ignored) {}
        }
    }

    private NationData readNationFile(UUID nationId, File file) {
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        NationData data = new NationData();
        data.setNationId(nationId);
        data.setName(cfg.getString("name"));
        data.setColor1(cfg.getString("color1", "§a"));
        data.setColor2(cfg.getString("color2", "§b"));
        data.setElitesConfigured(cfg.getBoolean("elitesConfigured", false));
        data.setTaxRate(cfg.getDouble("taxRate", 0.0));
        data.setLowerClassTaxRate(cfg.getDouble("lowerClassTaxRate", 0.0));
        data.setMiddleClassTaxRate(cfg.getDouble("middleClassTaxRate", 0.0));
        data.setUpperClassTaxRate(cfg.getDouble("upperClassTaxRate", 0.0));
        data.setTaxCollectionIntervalDays(cfg.getInt("taxCollectionIntervalDays", 1));
        data.setTreasuryBalance(cfg.getDouble("treasuryBalance", 0.0));
        data.setCustomGovernment(cfg.getBoolean("customGovernment", false));
        String pres = cfg.getString("presidentUUID");
        if (pres != null) data.setPresidentUUID(UUID.fromString(pres));
        String tres = cfg.getString("treasurerUUID");
        if (tres != null) data.setTreasurerUUID(UUID.fromString(tres));
        String vp = cfg.getString("vicePresidentUUID");
        if (vp != null) data.setVicePresidentUUID(UUID.fromString(vp));
        String sec = cfg.getString("securityHeadUUID");
        if (sec != null) data.setSecurityHeadUUID(UUID.fromString(sec));
        List<UUID> members = new ArrayList<>();
        for (String s : cfg.getStringList("memberUUIDs")) {
            try { members.add(UUID.fromString(s)); } catch (IllegalArgumentException ignored) {}
        }
        data.setMemberUUIDs(members);
        List<UUID> regions = new ArrayList<>();
        for (String s : cfg.getStringList("regionIds")) {
            try { regions.add(UUID.fromString(s)); } catch (IllegalArgumentException ignored) {}
        }
        data.setRegionIds(regions);
        List<UUID> joinReqs = new ArrayList<>();
        for (String s : cfg.getStringList("joinRequestUUIDs")) {
            try { joinReqs.add(UUID.fromString(s)); } catch (IllegalArgumentException ignored) {}
        }
        data.setJoinRequestUUIDs(joinReqs);
        List<UUID> govProps = new ArrayList<>();
        for (String s : cfg.getStringList("governmentPropertyIds")) {
            try { govProps.add(UUID.fromString(s)); } catch (IllegalArgumentException ignored) {}
        }
        data.setGovernmentPropertyIds(govProps);
        List<UUID> enforcerIds = new ArrayList<>();
        for (String s : cfg.getStringList("enforcerUUIDs")) {
            try { enforcerIds.add(UUID.fromString(s)); } catch (IllegalArgumentException ignored) {}
        }
        data.setEnforcerUUIDs(enforcerIds);
        Map<UUID, Double> salaries = new HashMap<>();
        if (cfg.isConfigurationSection("enforcerSalaries")) {
            for (String key : cfg.getConfigurationSection("enforcerSalaries").getKeys(false)) {
                try { salaries.put(UUID.fromString(key), cfg.getDouble("enforcerSalaries." + key)); } catch (IllegalArgumentException ignored) {}
            }
        }
        data.setEnforcerSalaries(salaries);
        Map<UUID, Integer> caught = new HashMap<>();
        if (cfg.isConfigurationSection("enforcerCriminalsCaught")) {
            for (String key : cfg.getConfigurationSection("enforcerCriminalsCaught").getKeys(false)) {
                try { caught.put(UUID.fromString(key), cfg.getInt("enforcerCriminalsCaught." + key)); } catch (IllegalArgumentException ignored) {}
            }
        }
        data.setEnforcerCriminalsCaught(caught);
        List<CustomRole> customRoles = new ArrayList<>();
        if (cfg.isConfigurationSection("customRoles")) {
            for (String key : cfg.getConfigurationSection("customRoles").getKeys(false)) {
                try {
                    CustomRole role = new CustomRole();
                    role.setRoleId(UUID.fromString(key));
                    String base = "customRoles." + key;
                    role.setName(cfg.getString(base + ".name", "Role"));
                    role.setColorCode(cfg.getString(base + ".colorCode", "ffffffffffffffa7f"));
                    String modeStr = cfg.getString(base + ".roleMode", "EMPLOYEE");
                    try { role.setRoleMode(RoleMode.valueOf(modeStr)); } catch (IllegalArgumentException e) { role.setRoleMode(RoleMode.EMPLOYEE); }
                    Set<NationPermission> perms = new HashSet<>();
                    for (String pName : cfg.getStringList(base + ".permissions")) {
                        try { perms.add(NationPermission.valueOf(pName)); } catch (IllegalArgumentException ignored) {}
                    }
                    role.setPermissions(perms);
                    List<UUID> roleMembers = new ArrayList<>();
                    for (String s : cfg.getStringList(base + ".members")) {
                        try { roleMembers.add(UUID.fromString(s)); } catch (IllegalArgumentException ignored) {}
                    }
                    role.setMemberUUIDs(roleMembers);
                    customRoles.add(role);
                } catch (IllegalArgumentException ignored) {}
            }
        }
        data.setCustomRoles(customRoles);
        return data;
    }

    private void loadRegionsFromDisk() {
        File[] files = regionsFolder.listFiles((d, n) -> n.endsWith(".yml"));
        if (files == null) return;
        for (File f : files) {
            try {
                UUID id = UUID.fromString(f.getName().replace(".yml", ""));
                YamlConfiguration cfg = YamlConfiguration.loadConfiguration(f);
                RegionData data = new RegionData();
                data.setRegionId(id);
                String nid = cfg.getString("nationId");
                if (nid != null) data.setNationId(UUID.fromString(nid));
                data.setName(cfg.getString("name"));
                String gov = cfg.getString("governorUUID");
                if (gov != null) data.setGovernorUUID(UUID.fromString(gov));
                data.setPropertyChunkRate(cfg.getDouble("propertyChunkRate", 100.0));
                data.setPropertyCollectionIntervalDays(cfg.getInt("propertyCollectionIntervalDays", 1));
                data.setClaimedChunks(new ArrayList<>(cfg.getStringList("claimedChunks")));
                List<UUID> props = new ArrayList<>();
                for (String s : cfg.getStringList("propertyIds")) {
                    try { props.add(UUID.fromString(s)); } catch (IllegalArgumentException ignored) {}
                }
                data.setPropertyIds(props);
                data.setPermsSameRegion(loadPermMap(cfg, "permsSameRegion", data.getPermsSameRegion()));
                data.setPermsSameNation(loadPermMap(cfg, "permsSameNation", data.getPermsSameNation()));
                data.setPermsForeign(loadPermMap(cfg, "permsForeign", data.getPermsForeign()));
                data.setDeniedMobs(new java.util.HashSet<>(cfg.getStringList("deniedMobs")));
                regionCache.put(id, data);
            } catch (IllegalArgumentException ignored) {}
        }
    }

    private void loadPropertiesFromDisk() {
        File[] files = propertiesFolder.listFiles((d, n) -> n.endsWith(".yml"));
        if (files == null) return;
        for (File f : files) {
            try {
                UUID id = UUID.fromString(f.getName().replace(".yml", ""));
                YamlConfiguration cfg = YamlConfiguration.loadConfiguration(f);
                PropertyData data = new PropertyData();
                data.setPropertyId(id);
                String rid = cfg.getString("regionId");
                if (rid != null) data.setRegionId(UUID.fromString(rid));
                String own = cfg.getString("ownerUUID");
                if (own != null) data.setOwnerUUID(UUID.fromString(own));
                data.setName(cfg.getString("name", ""));
                data.setChunks(new ArrayList<>(cfg.getStringList("chunks")));
                data.setForSale(cfg.getBoolean("forSale", false));
                data.setSalePrice(cfg.getDouble("salePrice", 0.0));
                data.setSaleSignLocation(cfg.getString("saleSignLocation"));
                Map<UUID, Double> offers = new HashMap<>();
                if (cfg.isConfigurationSection("offers")) {
                    for (String key : cfg.getConfigurationSection("offers").getKeys(false)) {
                        try { offers.put(UUID.fromString(key), cfg.getDouble("offers." + key)); } catch (IllegalArgumentException ignored) {}
                    }
                }
                data.setOffers(offers);
                data.setPermsSameRegion(loadPermMap(cfg, "permsSameRegion", data.getPermsSameRegion()));
                data.setPermsSameNation(loadPermMap(cfg, "permsSameNation", data.getPermsSameNation()));
                data.setPermsForeign(loadPermMap(cfg, "permsForeign", data.getPermsForeign()));
                data.setDeniedMobs(new java.util.HashSet<>(cfg.getStringList("deniedMobs")));
                List<String> rIdStrs = cfg.getStringList("roomIds");
                List<UUID> rIds = new ArrayList<>();
                for (String s : rIdStrs) {
                    try { rIds.add(UUID.fromString(s)); } catch (IllegalArgumentException ignored) {}
                }
                data.setRoomIds(rIds);
                propertyCache.put(id, data);
            } catch (IllegalArgumentException ignored) {}
        }
    }

    private void loadRequestsFromDisk() {
        File[] files = requestsFolder.listFiles((d, n) -> n.endsWith(".yml"));
        if (files == null) return;
        for (File f : files) {
            try {
                UUID id = UUID.fromString(f.getName().replace(".yml", ""));
                YamlConfiguration cfg = YamlConfiguration.loadConfiguration(f);
                PropertyPurchaseRequest req = new PropertyPurchaseRequest();
                req.setRequestId(id);
                String rid = cfg.getString("regionId");
                if (rid != null) req.setRegionId(UUID.fromString(rid));
                String reqr = cfg.getString("requesterUUID");
                if (reqr != null) req.setRequesterUUID(UUID.fromString(reqr));
                req.setRequestedChunks(new ArrayList<>(cfg.getStringList("requestedChunks")));
                req.setTotalPrice(cfg.getDouble("totalPrice", 0.0));
                req.setGovernorApproved(cfg.getBoolean("governorApproved", false));
                req.setTreasurerApproved(cfg.getBoolean("treasurerApproved", false));
                req.setPropertyName(cfg.getString("propertyName", ""));
                requestCache.put(id, req);
            } catch (IllegalArgumentException ignored) {}
        }
    }

    private void loadConservationFromDisk() {
        File[] files = conservationFolder.listFiles((d, n) -> n.endsWith(".yml"));
        if (files == null) return;
        for (File f : files) {
            try {
                UUID id = UUID.fromString(f.getName().replace(".yml", ""));
                YamlConfiguration cfg = YamlConfiguration.loadConfiguration(f);
                ConservationAreaData data = new ConservationAreaData();
                data.setAreaId(id);
                String rid = cfg.getString("regionId");
                if (rid != null) data.setRegionId(UUID.fromString(rid));
                data.setName(cfg.getString("name", ""));
                data.setChunks(new ArrayList<>(cfg.getStringList("chunks")));
                conservationCache.put(id, data);
            } catch (IllegalArgumentException ignored) {}
        }
    }

    private void loadRoomsFromDisk() {
        File[] files = roomsFolder.listFiles((d, n) -> n.endsWith(".yml"));
        if (files == null) return;
        for (File f : files) {
            try {
                UUID id = UUID.fromString(f.getName().replace(".yml", ""));
                YamlConfiguration cfg = YamlConfiguration.loadConfiguration(f);
                RoomData room = new RoomData();
                room.setRoomId(id);
                String pid = cfg.getString("propertyId");
                if (pid != null) room.setPropertyId(UUID.fromString(pid));
                room.setName(cfg.getString("name", ""));
                room.setWorld(cfg.getString("world", ""));
                room.setCorner1X(cfg.getInt("corner1X", 0));
                room.setCorner1Y(cfg.getInt("corner1Y", 0));
                room.setCorner1Z(cfg.getInt("corner1Z", 0));
                room.setCorner2X(cfg.getInt("corner2X", 0));
                room.setCorner2Y(cfg.getInt("corner2Y", 0));
                room.setCorner2Z(cfg.getInt("corner2Z", 0));
                room.setCeilingY(cfg.getInt("ceilingY", 0));
                room.setPermsSameRegion(loadPermMap(cfg, "permsSameRegion", room.getPermsSameRegion()));
                room.setPermsSameNation(loadPermMap(cfg, "permsSameNation", room.getPermsSameNation()));
                room.setPermsForeign(loadPermMap(cfg, "permsForeign", room.getPermsForeign()));
                if (cfg.isConfigurationSection("characterPerms")) {
                    Map<UUID, Map<String, Boolean>> charPerms = new HashMap<>();
                    for (String key : cfg.getConfigurationSection("characterPerms").getKeys(false)) {
                        try {
                            UUID charId = UUID.fromString(key);
                            Map<String, Boolean> perms = new HashMap<>();
                            if (cfg.isConfigurationSection("characterPerms." + key)) {
                                for (String pk : cfg.getConfigurationSection("characterPerms." + key).getKeys(false)) {
                                    perms.put(pk, cfg.getBoolean("characterPerms." + key + "." + pk));
                                }
                            }
                            charPerms.put(charId, perms);
                        } catch (IllegalArgumentException ignored) {}
                    }
                    room.setCharacterPerms(charPerms);
                }
                roomCache.put(id, room);
            } catch (IllegalArgumentException ignored) {}
        }
    }
}
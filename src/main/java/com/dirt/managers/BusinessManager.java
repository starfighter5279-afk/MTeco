package com.dirt.managers;

import com.dirt.DirtEconomy;
import com.dirt.data.BusinessData;
import com.dirt.data.BusinessPermission;
import com.dirt.data.BusinessPropertyData;
import com.dirt.data.BusinessRole;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BusinessManager {
    private final DirtEconomy plugin;
    private final File businessFolder;
    private final File propertyFolder;

    private final Map<UUID, BusinessData> businessCache = new HashMap<>();
    private final Map<UUID, BusinessPropertyData> bizPropertyCache = new HashMap<>();
    private final Map<String, UUID> chunkToBizProperty = new HashMap<>();

    public BusinessManager(DirtEconomy plugin) {
        this.plugin = plugin;
        this.businessFolder = new File(plugin.getDataFolder(), "data/MTB/businesses");
        this.propertyFolder = new File(plugin.getDataFolder(), "data/MTB/businessproperties");
        businessFolder.mkdirs();
        propertyFolder.mkdirs();
        loadAllFromDisk();
    }

    public void reloadCache() {
        businessCache.clear();
        bizPropertyCache.clear();
        chunkToBizProperty.clear();
        loadAllFromDisk();
    }

    private void loadAllFromDisk() {
        File[] bizFiles = businessFolder.listFiles((d, n) -> n.endsWith(".yml"));
        if (bizFiles != null) {
            for (File f : bizFiles) {
                try {
                    UUID id = UUID.fromString(f.getName().replace(".yml", ""));
                    BusinessData biz = readBusinessFile(id, f);
                    if (biz != null) businessCache.put(id, biz);
                } catch (IllegalArgumentException ignored) {}
            }
        }
        File[] propFiles = propertyFolder.listFiles((d, n) -> n.endsWith(".yml"));
        if (propFiles != null) {
            for (File f : propFiles) {
                try {
                    UUID id = UUID.fromString(f.getName().replace(".yml", ""));
                    BusinessPropertyData prop = readPropertyFile(id, f);
                    if (prop != null) {
                        bizPropertyCache.put(id, prop);
                        for (String chunk : prop.getChunks()) chunkToBizProperty.put(chunk, id);
                    }
                } catch (IllegalArgumentException ignored) {}
            }
        }
        plugin.getLogger().info("[BusinessManager] Cached " + businessCache.size() + " businesses, " + bizPropertyCache.size() + " properties.");
    }

    public BusinessData createBusiness(String name, String description, UUID ownerUUID, double payrollRate) {
        BusinessData biz = new BusinessData();
        biz.setBusinessId(UUID.randomUUID());
        biz.setName(name);
        biz.setDescription(description);
        biz.setOwnerUUID(ownerUUID);
        biz.setPayrollRate(payrollRate);
        saveBusiness(biz);
        if (plugin.getEconomy().hasBankSupport()) {
            plugin.getEconomy().createBank(name, plugin.getServer().getOfflinePlayer(ownerUUID));
        }
        return biz;
    }

    public BusinessData loadBusiness(UUID id) {
        if (id == null) return null;
        return businessCache.get(id);
    }

    public void saveBusiness(BusinessData biz) {
        businessCache.put(biz.getBusinessId(), biz);
        File file = new File(businessFolder, biz.getBusinessId() + ".yml");
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("name", biz.getName());
        cfg.set("description", biz.getDescription());
        cfg.set("ownerUUID", biz.getOwnerUUID() != null ? biz.getOwnerUUID().toString() : null);
        cfg.set("payrollRate", biz.getPayrollRate());
        cfg.set("totalEarned", biz.getTotalEarned());
        cfg.set("treasuryBalance", biz.getTreasuryBalance());
        cfg.set("hiring", biz.isHiring());
        cfg.set("forSale", biz.isForSale());
        cfg.set("salePrice", biz.getSalePrice());
        for (Map.Entry<UUID, Double> entry : biz.getEmployeeRates().entrySet()) {
            cfg.set("employeeRates." + entry.getKey().toString(), entry.getValue());
        }
        List<String> emps = new ArrayList<>();
        for (UUID u : biz.getEmployeeUUIDs()) emps.add(u.toString());
        cfg.set("employeeUUIDs", emps);
        List<String> props = new ArrayList<>();
        for (UUID u : biz.getPropertyIds()) props.add(u.toString());
        cfg.set("propertyIds", props);

        // Save roles
        cfg.set("defaultRoleId", biz.getDefaultRoleId() != null ? biz.getDefaultRoleId().toString() : null);
        for (int r = 0; r < biz.getRoles().size(); r++) {
            BusinessRole role = biz.getRoles().get(r);
            String base = "roles." + role.getRoleId().toString();
            cfg.set(base + ".name", role.getName());
            cfg.set(base + ".colorCode", role.getColorCode());
            List<String> permList = new ArrayList<>();
            for (BusinessPermission p : role.getPermissions()) permList.add(p.name());
            cfg.set(base + ".permissions", permList);
            List<String> members = new ArrayList<>();
            for (UUID u : role.getMemberUUIDs()) members.add(u.toString());
            cfg.set(base + ".members", members);
        }
        try { cfg.save(file); } catch (IOException e) { e.printStackTrace(); }
    }

    public void deleteBusiness(UUID id) {
        businessCache.remove(id);
        new File(businessFolder, id + ".yml").delete();
    }

    public List<BusinessData> getAllBusinesses() {
        return new ArrayList<>(businessCache.values());
    }

    public List<BusinessData> getBusinessesByOwner(UUID ownerUUID) {
        List<BusinessData> list = new ArrayList<>();
        for (BusinessData b : businessCache.values()) {
            if (ownerUUID.equals(b.getOwnerUUID())) list.add(b);
        }
        return list;
    }

    public List<BusinessData> getBusinessesByEmployee(UUID empUUID) {
        List<BusinessData> list = new ArrayList<>();
        for (BusinessData b : businessCache.values()) {
            if (b.getEmployeeUUIDs().contains(empUUID)) list.add(b);
        }
        return list;
    }

    public boolean isNameTaken(String name) {
        for (BusinessData b : businessCache.values()) {
            if (b.getName().equalsIgnoreCase(name)) return true;
        }
        return false;
    }

    // ---- Business Properties ----

    public BusinessPropertyData createProperty(UUID businessId, String name, List<String> chunks) {
        BusinessPropertyData prop = new BusinessPropertyData();
        prop.setPropertyId(UUID.randomUUID());
        prop.setBusinessId(businessId);
        prop.setName(name);
        prop.setChunks(new ArrayList<>(chunks));
        saveProperty(prop);
        BusinessData biz = businessCache.get(businessId);
        if (biz != null) {
            biz.getPropertyIds().add(prop.getPropertyId());
            saveBusiness(biz);
        }
        return prop;
    }

    public BusinessPropertyData loadProperty(UUID id) {
        if (id == null) return null;
        return bizPropertyCache.get(id);
    }

    public void saveProperty(BusinessPropertyData prop) {
        chunkToBizProperty.values().removeIf(pid -> pid.equals(prop.getPropertyId()));
        for (String chunk : prop.getChunks()) chunkToBizProperty.put(chunk, prop.getPropertyId());
        bizPropertyCache.put(prop.getPropertyId(), prop);

        File file = new File(propertyFolder, prop.getPropertyId() + ".yml");
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("businessId", prop.getBusinessId() != null ? prop.getBusinessId().toString() : null);
        cfg.set("name", prop.getName());
        cfg.set("chunks", prop.getChunks());
        for (Map.Entry<String, Boolean> e : prop.getPermsSameRegion().entrySet()) cfg.set("permsSameRegion." + e.getKey(), e.getValue());
        for (Map.Entry<String, Boolean> e : prop.getPermsSameNation().entrySet()) cfg.set("permsSameNation." + e.getKey(), e.getValue());
        for (Map.Entry<String, Boolean> e : prop.getPermsForeign().entrySet()) cfg.set("permsForeign." + e.getKey(), e.getValue());
        cfg.set("deniedMobs", new ArrayList<>(prop.getDeniedMobs()));
        List<String> roomIdStrs = new ArrayList<>();
        for (UUID rid : prop.getRoomIds()) roomIdStrs.add(rid.toString());
        cfg.set("roomIds", roomIdStrs);
        try { cfg.save(file); } catch (IOException e) { e.printStackTrace(); }
    }

    public void deleteProperty(UUID id) {
        BusinessPropertyData prop = bizPropertyCache.remove(id);
        if (prop != null) {
            chunkToBizProperty.values().removeIf(pid -> pid.equals(id));
            BusinessData biz = businessCache.get(prop.getBusinessId());
            if (biz != null) {
                biz.getPropertyIds().remove(id);
                saveBusiness(biz);
            }
        }
        new File(propertyFolder, id + ".yml").delete();
    }

    public BusinessPropertyData getPropertyByChunk(String chunkKey) {
        UUID propId = chunkToBizProperty.get(chunkKey);
        return propId != null ? bizPropertyCache.get(propId) : null;
    }

    public double getTreasuryBalance(String bizName) {
        for (BusinessData b : businessCache.values()) {
            if (b.getName().equalsIgnoreCase(bizName)) return b.getTreasuryBalance();
        }
        return 0.0;
    }

    public boolean depositToTreasury(BusinessData biz, double amount) {
        biz.setTreasuryBalance(biz.getTreasuryBalance() + amount);
        biz.setTotalEarned(biz.getTotalEarned() + amount);
        saveBusiness(biz);
        return true;
    }

    public boolean withdrawFromTreasury(String bizName, double amount) {
        BusinessData biz = null;
        for (BusinessData b : businessCache.values()) {
            if (b.getName().equalsIgnoreCase(bizName)) { biz = b; break; }
        }
        if (biz == null || biz.getTreasuryBalance() < amount) return false;
        biz.setTreasuryBalance(biz.getTreasuryBalance() - amount);
        saveBusiness(biz);
        return true;
    }

    private BusinessData readBusinessFile(UUID id, File file) {
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        BusinessData biz = new BusinessData();
        biz.setBusinessId(id);
        biz.setName(cfg.getString("name"));
        biz.setDescription(cfg.getString("description", ""));
        String owner = cfg.getString("ownerUUID");
        if (owner != null) biz.setOwnerUUID(UUID.fromString(owner));
        biz.setPayrollRate(cfg.getDouble("payrollRate", 0.0));
        biz.setTotalEarned(cfg.getDouble("totalEarned", 0.0));
        biz.setTreasuryBalance(cfg.getDouble("treasuryBalance", 0.0));
        biz.setHiring(cfg.getBoolean("hiring", false));
        biz.setForSale(cfg.getBoolean("forSale", false));
        biz.setSalePrice(cfg.getDouble("salePrice", 0.0));
        Map<UUID, Double> rates = new HashMap<>();
        if (cfg.isConfigurationSection("employeeRates")) {
            ConfigurationSection sec = cfg.getConfigurationSection("employeeRates");
            for (String key : sec.getKeys(false)) {
                try { rates.put(UUID.fromString(key), sec.getDouble(key)); } catch (IllegalArgumentException ignored) {}
            }
        }
        biz.setEmployeeRates(rates);
        List<UUID> emps = new ArrayList<>();
        for (String s : cfg.getStringList("employeeUUIDs")) {
            try { emps.add(UUID.fromString(s)); } catch (IllegalArgumentException ignored) {}
        }
        biz.setEmployeeUUIDs(emps);
        List<UUID> props = new ArrayList<>();
        for (String s : cfg.getStringList("propertyIds")) {
            try { props.add(UUID.fromString(s)); } catch (IllegalArgumentException ignored) {}
        }
        biz.setPropertyIds(props);

        // Load roles
        String defRoleStr = cfg.getString("defaultRoleId");
        if (defRoleStr != null) {
            try { biz.setDefaultRoleId(UUID.fromString(defRoleStr)); } catch (IllegalArgumentException ignored) {}
        }
        List<BusinessRole> roles = new ArrayList<>();
        if (cfg.isConfigurationSection("roles")) {
            for (String key : cfg.getConfigurationSection("roles").getKeys(false)) {
                try {
                    BusinessRole role = new BusinessRole();
                    role.setRoleId(UUID.fromString(key));
                    role.setName(cfg.getString("roles." + key + ".name", "Role"));
                    role.setColorCode(cfg.getString("roles." + key + ".colorCode", "\u00a7f"));
                    java.util.Set<BusinessPermission> perms = new java.util.HashSet<>();
                    for (String pStr : cfg.getStringList("roles." + key + ".permissions")) {
                        try { perms.add(BusinessPermission.valueOf(pStr)); } catch (IllegalArgumentException ignored) {}
                    }
                    role.setPermissions(perms);
                    List<UUID> members = new ArrayList<>();
                    for (String mStr : cfg.getStringList("roles." + key + ".members")) {
                        try { members.add(UUID.fromString(mStr)); } catch (IllegalArgumentException ignored) {}
                    }
                    role.setMemberUUIDs(members);
                    roles.add(role);
                } catch (IllegalArgumentException ignored) {}
            }
        }
        biz.setRoles(roles);
        return biz;
    }

    private BusinessPropertyData readPropertyFile(UUID id, File file) {
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        BusinessPropertyData prop = new BusinessPropertyData();
        prop.setPropertyId(id);
        String bid = cfg.getString("businessId");
        if (bid != null) prop.setBusinessId(UUID.fromString(bid));
        prop.setName(cfg.getString("name", ""));
        prop.setChunks(new ArrayList<>(cfg.getStringList("chunks")));
        if (cfg.isConfigurationSection("permsSameRegion")) {
            Map<String, Boolean> map = new HashMap<>(prop.getPermsSameRegion());
            for (String key : cfg.getConfigurationSection("permsSameRegion").getKeys(false)) map.put(key, cfg.getBoolean("permsSameRegion." + key));
            prop.setPermsSameRegion(map);
        }
        if (cfg.isConfigurationSection("permsSameNation")) {
            Map<String, Boolean> map = new HashMap<>(prop.getPermsSameNation());
            for (String key : cfg.getConfigurationSection("permsSameNation").getKeys(false)) map.put(key, cfg.getBoolean("permsSameNation." + key));
            prop.setPermsSameNation(map);
        }
        if (cfg.isConfigurationSection("permsForeign")) {
            Map<String, Boolean> map = new HashMap<>(prop.getPermsForeign());
            for (String key : cfg.getConfigurationSection("permsForeign").getKeys(false)) map.put(key, cfg.getBoolean("permsForeign." + key));
            prop.setPermsForeign(map);
        }
        prop.setDeniedMobs(new java.util.HashSet<>(cfg.getStringList("deniedMobs")));
        java.util.List<String> rIdStrs = cfg.getStringList("roomIds");
        java.util.List<UUID> rIds = new ArrayList<>();
        for (String s : rIdStrs) {
            try { rIds.add(UUID.fromString(s)); } catch (IllegalArgumentException ignored) {}
        }
        prop.setRoomIds(rIds);
        return prop;
    }
}
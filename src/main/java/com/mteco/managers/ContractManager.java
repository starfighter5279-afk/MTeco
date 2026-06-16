package com.mteco.managers;

import com.mteco.MTeco;
import com.mteco.data.ContractData;
import com.mteco.data.ContractSignature;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ContractManager {
    private final MTeco plugin;
    private final File contractFolder;
    private final Map<UUID, ContractData> contractCache = new HashMap<>();
    private final Map<UUID, ContractData> draftContracts = new HashMap<>();

    public ContractManager(MTeco plugin) {
        this.plugin = plugin;
        this.contractFolder = new File(plugin.getDataFolder(), "data/MTN/contracts");
        contractFolder.mkdirs();
        loadAllFromDisk();
    }

    public void reloadCache() {
        contractCache.clear();
        loadAllFromDisk();
    }

    private void loadAllFromDisk() {
        File[] files = contractFolder.listFiles((d, n) -> n.endsWith(".yml"));
        if (files != null) {
            for (File f : files) {
                try {
                    UUID id = UUID.fromString(f.getName().replace(".yml", ""));
                    ContractData contract = readContractFile(id, f);
                    if (contract != null) contractCache.put(id, contract);
                } catch (IllegalArgumentException ignored) {}
            }
        }
        plugin.getLogger().info("[ContractManager] Cached " + contractCache.size() + " contracts.");
    }

    public ContractData getDraft(UUID playerUuid) {
        return draftContracts.get(playerUuid);
    }

    public ContractData getOrCreateDraft(UUID playerUuid) {
        ContractData existing = draftContracts.get(playerUuid);
        if (existing != null) return existing;
        return createNewDraft(playerUuid);
    }

    public void removeDraft(UUID playerUuid) {
        draftContracts.remove(playerUuid);
    }

    public void setActiveDraft(UUID playerUuid, ContractData draft) {
        draftContracts.put(playerUuid, draft);
    }

    public ContractData getActiveDraft(UUID playerUuid) {
        return draftContracts.get(playerUuid);
    }

    public ContractData createNewDraft(UUID playerUuid) {
        ContractData draft = new ContractData();
        draft.setContractId(UUID.randomUUID());
        draft.setCreatorPlayerUuid(playerUuid);
        draft.setCreatedAt(System.currentTimeMillis());
        draft.setStatus("DRAFT");
        draftContracts.put(playerUuid, draft);
        saveDraft(draft);
        return draft;
    }

    public List<ContractData> getDraftsByPlayer(UUID playerUuid) {
        List<ContractData> result = new ArrayList<>();
        for (ContractData c : contractCache.values()) {
            if ("DRAFT".equals(c.getStatus()) && playerUuid.equals(c.getCreatorPlayerUuid())) {
                result.add(c);
            }
        }
        return result;
    }

    public void saveDraft(ContractData draft) {
        draft.setStatus("DRAFT");
        saveContract(draft);
    }

    public void deleteDraft(UUID contractId) {
        deleteContract(contractId);
        draftContracts.entrySet().removeIf(e -> contractId.equals(e.getValue().getContractId()));
    }

    public ContractData loadContract(UUID contractId) {
        return contractCache.get(contractId);
    }

    public void saveContract(ContractData contract) {
        contractCache.put(contract.getContractId(), contract);
        File file = new File(contractFolder, contract.getContractId() + ".yml");
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("title", contract.getTitle());
        cfg.set("body", contract.getBody());
        cfg.set("creatorPlayerUuid", contract.getCreatorPlayerUuid() != null ? contract.getCreatorPlayerUuid().toString() : null);
        cfg.set("creatorCharacterName", contract.getCreatorCharacterName());
        cfg.set("createdAt", contract.getCreatedAt());
        cfg.set("durationMinecraftDays", contract.getDurationMinecraftDays());
        cfg.set("expiresAt", contract.getExpiresAt());
        cfg.set("status", contract.getStatus());
        cfg.set("pendingRecipientUuid", contract.getPendingRecipientUuid() != null ? contract.getPendingRecipientUuid().toString() : null);
        cfg.set("secondRecipientUuid", contract.getSecondRecipientUuid() != null ? contract.getSecondRecipientUuid().toString() : null);

        List<String> linked = new ArrayList<>();
        for (UUID u : contract.getLinkedPlayerUuids()) linked.add(u.toString());
        cfg.set("linkedPlayerUuids", linked);

        List<String> linkedBiz = new ArrayList<>();
        for (UUID u : contract.getLinkedBusinessIds()) linkedBiz.add(u.toString());
        cfg.set("linkedBusinessIds", linkedBiz);

        List<String> linkedNation = new ArrayList<>();
        for (UUID u : contract.getLinkedNationIds()) linkedNation.add(u.toString());
        cfg.set("linkedNationIds", linkedNation);

        for (int i = 0; i < contract.getSignatures().size(); i++) {
            ContractSignature sig = contract.getSignatures().get(i);
            String base = "signatures." + i;
            cfg.set(base + ".playerUuid", sig.getPlayerUuid() != null ? sig.getPlayerUuid().toString() : null);
            cfg.set(base + ".characterName", sig.getCharacterName());
            cfg.set(base + ".characterCode", sig.getCharacterCode());
            cfg.set(base + ".onBehalfOf", sig.getOnBehalfOf());
            cfg.set(base + ".onBehalfOfId", sig.getOnBehalfOfId() != null ? sig.getOnBehalfOfId().toString() : null);
            cfg.set(base + ".onBehalfOfType", sig.getOnBehalfOfType());
            cfg.set(base + ".signedAt", sig.getSignedAt());
        }

        try { cfg.save(file); } catch (IOException e) { e.printStackTrace(); }
    }

    public void deleteContract(UUID contractId) {
        contractCache.remove(contractId);
        new File(contractFolder, contractId + ".yml").delete();
    }

    public List<ContractData> getContractsByPlayer(UUID playerUuid) {
        List<ContractData> result = new ArrayList<>();
        for (ContractData c : contractCache.values()) {
            if (c.getLinkedPlayerUuids().contains(playerUuid)
                    && ("ACTIVE".equals(c.getStatus()) || "BROKEN".equals(c.getStatus()))) {
                result.add(c);
            }
        }
        return result;
    }

    public List<ContractData> getContractsByBusiness(UUID businessId) {
        List<ContractData> result = new ArrayList<>();
        for (ContractData c : contractCache.values()) {
            if (c.getLinkedBusinessIds().contains(businessId)
                    && ("ACTIVE".equals(c.getStatus()) || "BROKEN".equals(c.getStatus()))) {
                result.add(c);
            }
        }
        return result;
    }

    public void checkExpiredContracts() {
        long now = System.currentTimeMillis();
        for (ContractData c : contractCache.values()) {
            if ("ACTIVE".equals(c.getStatus()) && c.getExpiresAt() > 0 && now >= c.getExpiresAt()) {
                c.setStatus("EXPIRED");
                saveContract(c);
                for (UUID playerUuid : c.getLinkedPlayerUuids()) {
                    org.bukkit.entity.Player p = plugin.getServer().getPlayer(playerUuid);
                    if (p != null) {
                        p.sendMessage("\u00a7eA contract you signed has expired.");
                    }
                }
            }
        }
    }

    public static String getCharacterCode(com.mteco.data.CharacterData data) {
        return data.getPlayerUuid().toString().substring(0, 8) + "-" + data.getBirthDate();
    }

    private ContractData readContractFile(UUID id, File file) {
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        ContractData contract = new ContractData();
        contract.setContractId(id);
        contract.setTitle(cfg.getString("title", ""));
        contract.setBody(cfg.getString("body", ""));
        String creator = cfg.getString("creatorPlayerUuid");
        if (creator != null) contract.setCreatorPlayerUuid(UUID.fromString(creator));
        contract.setCreatorCharacterName(cfg.getString("creatorCharacterName", ""));
        contract.setCreatedAt(cfg.getLong("createdAt", 0));
        contract.setDurationMinecraftDays(cfg.getInt("durationMinecraftDays", 0));
        contract.setExpiresAt(cfg.getLong("expiresAt", 0));
        contract.setStatus(cfg.getString("status", "DRAFT"));
        String pending = cfg.getString("pendingRecipientUuid");
        if (pending != null) contract.setPendingRecipientUuid(UUID.fromString(pending));
        String second = cfg.getString("secondRecipientUuid");
        if (second != null) contract.setSecondRecipientUuid(UUID.fromString(second));

        List<UUID> linkedPlayers = new ArrayList<>();
        for (String s : cfg.getStringList("linkedPlayerUuids")) {
            try { linkedPlayers.add(UUID.fromString(s)); } catch (IllegalArgumentException ignored) {}
        }
        contract.setLinkedPlayerUuids(linkedPlayers);

        List<UUID> linkedBiz = new ArrayList<>();
        for (String s : cfg.getStringList("linkedBusinessIds")) {
            try { linkedBiz.add(UUID.fromString(s)); } catch (IllegalArgumentException ignored) {}
        }
        contract.setLinkedBusinessIds(linkedBiz);

        List<UUID> linkedNations = new ArrayList<>();
        for (String s : cfg.getStringList("linkedNationIds")) {
            try { linkedNations.add(UUID.fromString(s)); } catch (IllegalArgumentException ignored) {}
        }
        contract.setLinkedNationIds(linkedNations);

        List<ContractSignature> sigs = new ArrayList<>();
        if (cfg.isConfigurationSection("signatures")) {
            ConfigurationSection sec = cfg.getConfigurationSection("signatures");
            for (String key : sec.getKeys(false)) {
                ContractSignature sig = new ContractSignature();
                String pu = cfg.getString("signatures." + key + ".playerUuid");
                if (pu != null) sig.setPlayerUuid(UUID.fromString(pu));
                sig.setCharacterName(cfg.getString("signatures." + key + ".characterName", ""));
                sig.setCharacterCode(cfg.getString("signatures." + key + ".characterCode", ""));
                sig.setOnBehalfOf(cfg.getString("signatures." + key + ".onBehalfOf"));
                String oboId = cfg.getString("signatures." + key + ".onBehalfOfId");
                if (oboId != null) sig.setOnBehalfOfId(UUID.fromString(oboId));
                sig.setOnBehalfOfType(cfg.getString("signatures." + key + ".onBehalfOfType"));
                sig.setSignedAt(cfg.getLong("signatures." + key + ".signedAt", 0));
                sigs.add(sig);
            }
        }
        contract.setSignatures(sigs);
        return contract;
    }
}
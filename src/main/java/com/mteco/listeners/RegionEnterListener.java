package com.mteco.listeners;

import com.mteco.MTeco;
import com.mteco.data.BusinessData;
import com.mteco.data.BusinessPropertyData;
import com.mteco.data.CharacterData;
import com.mteco.data.ConservationAreaData;
import com.mteco.data.MailItem;
import com.mteco.data.NationData;
import com.mteco.data.PropertyData;
import com.mteco.data.RegionData;
import com.mteco.managers.NationManager;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RegionEnterListener implements Listener {
    private final MTeco plugin;
    private final Map<UUID, UUID> lastPropertyId = new HashMap<>();
    private final Map<UUID, UUID> lastConservationArea = new HashMap<>();

    public RegionEnterListener(MTeco plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getFrom().getChunk().equals(event.getTo().getChunk())) return;

        Player player = event.getPlayer();
        Chunk toChunk = event.getTo().getChunk();
        String toKey = NationManager.chunkKey(toChunk.getWorld().getName(), toChunk.getX(), toChunk.getZ());

        RegionData toRegion = plugin.getNationManager().getRegionByChunk(toKey);

        Chunk fromChunk = event.getFrom().getChunk();
        String fromKey = NationManager.chunkKey(fromChunk.getWorld().getName(), fromChunk.getX(), fromChunk.getZ());
        RegionData fromRegion = plugin.getNationManager().getRegionByChunk(fromKey);

        if (toRegion == null) {
            lastPropertyId.remove(player.getUniqueId());
            return;
        }

        if (fromRegion == null || !fromRegion.getRegionId().equals(toRegion.getRegionId())) {
            NationData nation = plugin.getNationManager().loadNation(toRegion.getNationId());
            String nationName = nation != null ? nation.getColor1() + nation.getName() : "§7Unknown Nation";
            String regionName = "§7" + toRegion.getName();
            player.sendTitle(nationName, regionName, 0, 100, 0);
        }

        handlePropertyEntry(player, toKey);
        handleConservationEntry(player, toKey);
    }

    private void handleConservationEntry(Player player, String chunkKey) {
        ConservationAreaData area = plugin.getNationManager().getConservationAreaByChunk(chunkKey);
        UUID last = lastConservationArea.get(player.getUniqueId());
        if (area == null) {
            lastConservationArea.remove(player.getUniqueId());
            return;
        }
        if (area.getAreaId().equals(last)) return;
        lastConservationArea.put(player.getUniqueId(), area.getAreaId());
        player.sendTitle("\u00a76" + area.getName(), "\u00a77Conservation Area", 10, 60, 10);
    }

    private void handlePropertyEntry(Player player, String chunkKey) {
        // Check personal property
        PropertyData personalProp = plugin.getNationManager().getPropertyByChunk(chunkKey);
        if (personalProp != null && !player.getUniqueId().equals(personalProp.getOwnerUUID())) {
            UUID lastId = lastPropertyId.get(player.getUniqueId());
            if (personalProp.getPropertyId().equals(lastId)) return;
            lastPropertyId.put(player.getUniqueId(), personalProp.getPropertyId());
            String propName = (personalProp.getName() != null && !personalProp.getName().isEmpty())
                    ? personalProp.getName() : "Private Property";
            player.sendTitle("\u00a76" + propName, "", 10, 60, 10);
            sendPropertyEntryMail(personalProp.getOwnerUUID(), player, propName);
            return;
        }

        // Check business property
        if (plugin.isMTBusinessEnabled()) {
            BusinessPropertyData bizProp = plugin.getBusinessManager().getPropertyByChunk(chunkKey);
            if (bizProp != null) {
                BusinessData biz = plugin.getBusinessManager().loadBusiness(bizProp.getBusinessId());
                if (biz != null && !player.getUniqueId().equals(biz.getOwnerUUID())) {
                    UUID lastId = lastPropertyId.get(player.getUniqueId());
                    UUID bizPropId = bizProp.getPropertyId();
                    if (bizPropId != null && bizPropId.equals(lastId)) return;
                    lastPropertyId.put(player.getUniqueId(), bizPropId);
                    String propName = bizProp.getName() != null && !bizProp.getName().isEmpty()
                            ? bizProp.getName() : biz.getName();
                    player.sendTitle("\u00a76" + propName, "", 10, 60, 10);
                    sendPropertyEntryMail(biz.getOwnerUUID(), player, propName);
                    return;
                }
            }
        }

        lastPropertyId.remove(player.getUniqueId());
    }

    private void sendPropertyEntryMail(UUID ownerUUID, Player entrant, String propertyName) {
        CharacterData entrantChar = plugin.getCharacterManager().getCharacter(entrant.getUniqueId());
        String entrantName = entrantChar != null
                ? entrantChar.getFirstName() + " " + entrantChar.getLastName()
                : entrant.getName();

        MailItem mail = new MailItem();
        mail.setId(UUID.randomUUID().toString());
        mail.setType("PROPERTY_ENTRY");
        mail.setFromPlayerUuid(entrant.getUniqueId());
        mail.setTimestamp(System.currentTimeMillis());
        Map<String, String> data = new HashMap<>();
        data.put("entrantName", entrantName);
        data.put("propertyName", propertyName);
        mail.setData(data);
        plugin.getCharacterManager().addMailItem(ownerUUID, mail);
    }
}
package com.dirt.inventory.impl.nation;

import com.cryptomorin.xseries.XMaterial;
import com.dirt.DirtEconomy;
import com.dirt.data.CharacterData;
import com.dirt.data.CriminalRecord;
import com.dirt.data.LegislatedCrimeData;
import com.dirt.inventory.InventoryButton;
import com.dirt.inventory.InventoryGUI;
import com.dirt.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public class CriminalDetailGUI extends InventoryGUI {
    private final DirtEconomy plugin;
    private final UUID nationId;
    private final UUID recordId;
    private final Supplier<InventoryGUI> backSupplier;

    public CriminalDetailGUI(DirtEconomy plugin, UUID nationId, UUID recordId, Supplier<InventoryGUI> backSupplier) {
        this.plugin = plugin;
        this.nationId = nationId;
        this.recordId = recordId;
        this.backSupplier = backSupplier;
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 27, "\u00a7eCriminal Record");
    }

    @Override
    public void decorate(Player player) {
        fillGlass(27, XMaterial.RED_STAINED_GLASS_PANE);

        CriminalRecord record = plugin.getLawCrimeManager().loadCriminalRecord(recordId);
        if (record == null) { super.decorate(player); return; }

        CharacterData charData = plugin.getCharacterManager().getCharacter(record.getCriminalUUID());
        String name = charData != null ? charData.getFirstName() + " " + charData.getLastName() : "Unknown";
        String date = new SimpleDateFormat("MM/dd/yy HH:mm").format(new Date(record.getConvictedTimestamp()));
        boolean active = System.currentTimeMillis() < record.getJailReleaseTime();

        List<String> crimeLore = new ArrayList<>();
        crimeLore.add("\u00a77Convicted: \u00a7f" + date);
        crimeLore.add("\u00a77Serving: " + (record.isServing() ? "\u00a7cYes" : "\u00a7aNo"));
        crimeLore.add("\u00a77Active: " + (active ? "\u00a7cYes" : "\u00a7aExpired"));
        if (record.isEscaped()) {
            crimeLore.add("\u00a74\u2620 ESCAPED FUGITIVE");
        }
        if (active) {
            long remainMs = record.getJailReleaseTime() - System.currentTimeMillis();
            long totalMins = Math.max(0, remainMs / 60000);
            long hours = totalMins / 60;
            long mins = totalMins % 60;
            crimeLore.add("\u00a77Remaining: \u00a7f" + (hours > 0 ? hours + "h " : "") + mins + "m");
        }
        crimeLore.add("");
        crimeLore.add("\u00a77Crimes:");
        for (UUID crimeId : record.getCrimeIds()) {
            LegislatedCrimeData crime = plugin.getLawCrimeManager().loadCrime(crimeId);
            if (crime != null) crimeLore.add("\u00a7c - " + crime.getName());
        }
        if (record.getDescription() != null && !record.getDescription().isEmpty()) {
            crimeLore.add("");
            crimeLore.add("\u00a77" + record.getDescription());
        }

        addButton(4, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.PLAYER_HEAD, "\u00a7e" + name,
                        crimeLore.toArray(new String[0])))
                .consumer(e -> {})
        );

        if (active) {
            addButton(13, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.EMERALD, "\u00a7aRelease Criminal",
                            "\u00a77End their sentence early."))
                    .consumer(e -> {
                        CriminalRecord r = plugin.getLawCrimeManager().loadCriminalRecord(recordId);
                        if (r != null) {
                            plugin.getLawCrimeManager().releasePlayer(r);
                        }
                        Player p = (Player) e.getWhoClicked();
                        p.sendMessage("\u00a7aCriminal released.");
                        plugin.getGUIManager().openGUI(new ManageCriminalsGUI(plugin, nationId, backSupplier, 0), p);
                    })
            );
        }

        addButton(22, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7cBack"))
                .consumer(e -> plugin.getGUIManager().openGUI(new ManageCriminalsGUI(plugin, nationId, backSupplier, 0), (Player) e.getWhoClicked()))
        );

        super.decorate(player);
    }
}
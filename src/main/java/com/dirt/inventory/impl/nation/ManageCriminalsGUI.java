package com.dirt.inventory.impl.nation;

import com.cryptomorin.xseries.XMaterial;
import com.dirt.DirtEconomy;
import com.dirt.data.CharacterData;
import com.dirt.data.CriminalRecord;
import com.dirt.inventory.InventoryButton;
import com.dirt.inventory.InventoryGUI;
import com.dirt.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public class ManageCriminalsGUI extends InventoryGUI {
    private final DirtEconomy plugin;
    private final UUID nationId;
    private final Supplier<InventoryGUI> backSupplier;
    private final int page;

    public ManageCriminalsGUI(DirtEconomy plugin, UUID nationId, Supplier<InventoryGUI> backSupplier, int page) {
        this.plugin = plugin;
        this.nationId = nationId;
        this.backSupplier = backSupplier;
        this.page = page;
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 54, "\u00a7eManage Criminals");
    }

    @Override
    public void decorate(Player player) {
        fillPagedGui(54, XMaterial.RED_STAINED_GLASS_PANE);

        List<CriminalRecord> records = plugin.getLawCrimeManager().getRecordsByNation(nationId);
        int perPage = 44;
        int start = page * perPage;
        int end = Math.min(start + perPage, records.size());

        for (int i = start; i < end; i++) {
            CriminalRecord record = records.get(i);
            CharacterData charData = plugin.getCharacterManager().getCharacter(record.getCriminalUUID());
            String name = charData != null ? charData.getFirstName() + " " + charData.getLastName() : Bukkit.getOfflinePlayer(record.getCriminalUUID()).getName();
            String date = new SimpleDateFormat("MM/dd/yy").format(new Date(record.getConvictedTimestamp()));
            boolean active = System.currentTimeMillis() < record.getJailReleaseTime();
            String status;
            if (record.isEscaped()) {
                status = "\u00a74Escaped";
            } else if (record.isServing()) {
                long remainMs = record.getJailReleaseTime() - System.currentTimeMillis();
                long mins = Math.max(0, remainMs / 60000);
                status = "\u00a7cServing \u00a77(" + mins + "m left)";
            } else if (active) {
                status = "\u00a7eConvicted";
            } else {
                status = "\u00a7aReleased";
            }
            final UUID recordId = record.getRecordId();

            addButton(i - start, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(active ? XMaterial.ORANGE_WOOL : XMaterial.GREEN_WOOL,
                            "\u00a7e" + name,
                            "\u00a77Convicted: \u00a7f" + date,
                            "\u00a77Status: " + status,
                            "\u00a77Click to manage."))
                    .consumer(e -> plugin.getGUIManager().openGUI(new CriminalDetailGUI(plugin, nationId, recordId, backSupplier), (Player) e.getWhoClicked()))
            );
        }

        if (page > 0) {
            int prev = page - 1;
            addButton(45, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7ePrevious"))
                    .consumer(e -> plugin.getGUIManager().openGUI(new ManageCriminalsGUI(plugin, nationId, backSupplier, prev), (Player) e.getWhoClicked()))
            );
        }
        if (end < records.size()) {
            int next = page + 1;
            addButton(53, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7eNext"))
                    .consumer(e -> plugin.getGUIManager().openGUI(new ManageCriminalsGUI(plugin, nationId, backSupplier, next), (Player) e.getWhoClicked()))
            );
        }

        addButton(49, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7cBack"))
                .consumer(e -> plugin.getGUIManager().openGUI(new CrimeManagementGUI(plugin, nationId, backSupplier), (Player) e.getWhoClicked()))
        );

        super.decorate(player);
    }
}
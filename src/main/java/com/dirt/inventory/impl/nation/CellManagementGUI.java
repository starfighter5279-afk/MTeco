package com.dirt.inventory.impl.nation;

import com.cryptomorin.xseries.XMaterial;
import com.dirt.DirtEconomy;
import com.dirt.data.CellData;
import com.dirt.data.JailData;
import com.dirt.inventory.InventoryButton;
import com.dirt.inventory.InventoryGUI;
import com.dirt.managers.LawCrimeManager;
import com.dirt.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.List;
import java.util.UUID;

public class CellManagementGUI extends InventoryGUI {
    private final DirtEconomy plugin;
    private final UUID nationId;
    private final UUID jailId;

    public CellManagementGUI(DirtEconomy plugin, UUID nationId, UUID jailId) {
        this.plugin = plugin;
        this.nationId = nationId;
        this.jailId = jailId;
    }

    @Override
    protected Inventory createInventory() {
        JailData jail = plugin.getLawCrimeManager().loadJail(jailId);
        String title = jail != null ? "\u00a7eCells: " + jail.getName() : "\u00a7eCell Management";
        return Bukkit.createInventory(null, 54, title);
    }

    @Override
    public void decorate(Player player) {
        fillPagedGui(54, XMaterial.RED_STAINED_GLASS_PANE);

        JailData jail = plugin.getLawCrimeManager().loadJail(jailId);
        if (jail == null) { super.decorate(player); return; }

        List<UUID> cellIds = jail.getCellIds();
        int end = Math.min(44, cellIds.size());

        for (int i = 0; i < end; i++) {
            UUID cellId = cellIds.get(i);
            CellData cell = plugin.getLawCrimeManager().loadCell(cellId);
            if (cell == null) continue;

            final int idx = i;
            int occupants = plugin.getLawCrimeManager().getCriminalsInCell(cellId).size();
            int bedCount = plugin.getLawCrimeManager().countBedsInCell(cell);
            Location loc = LawCrimeManager.parseLoc(cell.getLocation());
            String locStr = loc != null
                    ? "\u00a77" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ()
                    : "\u00a77Unknown";

            addButton(i, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.IRON_BARS, "\u00a7eCell #" + (idx + 1),
                            locStr,
                            "\u00a77Occupancy: \u00a7f" + occupants + "/" + bedCount,
                            "\u00a7eClick to manage."))
                    .consumer(e -> plugin.getGUIManager().openGUI(
                            new CellDetailGUI(plugin, nationId, jailId, cellId), (Player) e.getWhoClicked()))
            );
        }

        addButton(47, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.LIME_DYE, "\u00a7aCreate Cell",
                        "\u00a77Use the golden shovel to select",
                        "\u00a77two corners for the cell."))
                .consumer(e -> {
                    Player p = (Player) e.getWhoClicked();
                    p.closeInventory();
                    plugin.getChunkSelectionManager().startCellCreation(p, jailId);
                })
        );

        addButton(49, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7cBack"))
                .consumer(e -> plugin.getGUIManager().openGUI(new JailDetailGUI(plugin, nationId, jailId), (Player) e.getWhoClicked()))
        );

        super.decorate(player);
    }
}
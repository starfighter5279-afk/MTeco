package com.dirt.inventory.impl.nation;

import com.cryptomorin.xseries.XMaterial;
import com.dirt.DirtEconomy;
import com.dirt.data.CellData;
import com.dirt.data.CriminalRecord;
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

public class CellMoveSelectGUI extends InventoryGUI {
    private final DirtEconomy plugin;
    private final UUID nationId;
    private final UUID jailId;
    private final UUID fromCellId;
    private final UUID recordId;

    public CellMoveSelectGUI(DirtEconomy plugin, UUID nationId, UUID jailId, UUID fromCellId, UUID recordId) {
        this.plugin = plugin;
        this.nationId = nationId;
        this.jailId = jailId;
        this.fromCellId = fromCellId;
        this.recordId = recordId;
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 54, "\u00a7eSelect Cell to Move To");
    }

    @Override
    public void decorate(Player player) {
        fillPagedGui(54, XMaterial.RED_STAINED_GLASS_PANE);

        List<JailData> allJails = plugin.getLawCrimeManager().getJailsByNation(nationId);
        int slot = 0;
        for (JailData j : allJails) {
            for (UUID cid : j.getCellIds()) {
                if (slot >= 44) break;
                if (cid.equals(fromCellId)) continue;

                CellData cell = plugin.getLawCrimeManager().loadCell(cid);
                if (cell == null) continue;
                int occupants = plugin.getLawCrimeManager().getCriminalsInCell(cid).size();
                int bedCount = plugin.getLawCrimeManager().countBedsInCell(cell);
                if (bedCount == 0 || occupants >= bedCount) continue;

                Location loc = LawCrimeManager.parseLoc(cell.getLocation());
                String locStr = loc != null
                        ? loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ()
                        : "Unknown";
                String jailName = j.getName();
                final UUID targetCellId = cid;
                final UUID targetJailId = j.getJailId();

                addButton(slot, new InventoryButton()
                        .creator(p -> ItemUtil.buildItem(XMaterial.IRON_BARS, "\u00a7e" + jailName + " Cell",
                                "\u00a77Location: \u00a7f" + locStr,
                                "\u00a77Occupancy: \u00a7f" + occupants + "/" + bedCount,
                                "\u00a7aClick to move here."))
                        .consumer(e -> {
                            Player p = (Player) e.getWhoClicked();
                            CriminalRecord rec = plugin.getLawCrimeManager().loadCriminalRecord(recordId);
                            if (rec != null) {
                                rec.setCellId(targetCellId);
                                Location bedLoc = plugin.getLawCrimeManager().findUnassignedBedInCell(cell);
                                rec.setAssignedBedLocation(bedLoc != null ? LawCrimeManager.locKey(bedLoc) : null);
                                plugin.getLawCrimeManager().saveCriminalRecord(rec);
                                Player criminal = Bukkit.getPlayer(rec.getCriminalUUID());
                                Location cellLoc = LawCrimeManager.parseLoc(cell.getLocation());
                                if (criminal != null && cellLoc != null) {
                                    criminal.teleport(cellLoc);
                                    criminal.sendMessage("\u00a7eYou have been moved to a different cell.");
                                }
                                p.sendMessage("\u00a7aPrisoner moved.");
                            }
                            plugin.getGUIManager().openGUI(new CellDetailGUI(plugin, nationId, targetJailId, targetCellId), p);
                        })
                );
                slot++;
            }
        }

        addButton(49, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7cBack"))
                .consumer(e -> plugin.getGUIManager().openGUI(new CellDetailGUI(plugin, nationId, jailId, fromCellId), (Player) e.getWhoClicked()))
        );

        super.decorate(player);
    }
}
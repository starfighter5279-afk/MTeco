package com.dirt.inventory.impl.nation;

import com.cryptomorin.xseries.XMaterial;
import com.dirt.DirtEconomy;
import com.dirt.data.CellData;
import com.dirt.data.CharacterData;
import com.dirt.data.CriminalRecord;
import com.dirt.data.JailData;
import com.dirt.inventory.InventoryButton;
import com.dirt.inventory.InventoryGUI;
import com.dirt.managers.LawCrimeManager;
import com.dirt.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;

import java.util.List;
import java.util.UUID;

public class CellDetailGUI extends InventoryGUI {
    private final DirtEconomy plugin;
    private final UUID nationId;
    private final UUID jailId;
    private final UUID cellId;

    public CellDetailGUI(DirtEconomy plugin, UUID nationId, UUID jailId, UUID cellId) {
        this.plugin = plugin;
        this.nationId = nationId;
        this.jailId = jailId;
        this.cellId = cellId;
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 54, "\u00a7eCell Detail");
    }

    @Override
    public void decorate(Player player) {
        fillPagedGui(54, XMaterial.RED_STAINED_GLASS_PANE);

        CellData cell = plugin.getLawCrimeManager().loadCell(cellId);
        if (cell == null) { super.decorate(player); return; }

        Location loc = LawCrimeManager.parseLoc(cell.getLocation());
        String locStr = loc != null
                ? loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ()
                : "Unknown";
        List<CriminalRecord> prisoners = plugin.getLawCrimeManager().getCriminalsInCell(cellId);
        int bedCount = plugin.getLawCrimeManager().countBedsInCell(cell);

        String cellName = cell.getName() != null && !cell.getName().isEmpty() ? cell.getName() : "Cell";

        addButton(4, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.IRON_BARS, "\u00a7e" + cellName,
                        "\u00a77Location: \u00a7f" + locStr,
                        "\u00a77Beds: \u00a7f" + bedCount,
                        "\u00a77Prisoners: \u00a7f" + prisoners.size() + "/" + bedCount))
                .consumer(e -> {})
        );

        addButton(3, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.NAME_TAG, "\u00a7eRename Cell",
                        "\u00a77Current: \u00a7f" + cellName))
                .consumer(e -> {
                    Player p = (Player) e.getWhoClicked();
                    plugin.getChatInputManager().requestInput(p, "\u00a7eEnter new cell name:", input -> {
                        String nm = input.trim();
                        if (nm.isEmpty()) { p.sendMessage("\u00a7cName cannot be empty."); return; }
                        CellData c = plugin.getLawCrimeManager().loadCell(cellId);
                        if (c == null) return;
                        c.setName(nm);
                        plugin.getLawCrimeManager().saveCell(c);
                        p.sendMessage("\u00a7aCell renamed to '\u00a76" + nm + "\u00a7a'.");
                        plugin.getGUIManager().openGUI(new CellDetailGUI(plugin, nationId, jailId, cellId), p);
                    });
                })
        );

        addButton(51, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.RED_DYE, "\u00a7cDelete Cell",
                        "\u00a77Permanently remove this cell."))
                .consumer(e -> {
                    Player p = (Player) e.getWhoClicked();
                    for (CriminalRecord rec : plugin.getLawCrimeManager().getCriminalsInCell(cellId)) {
                        plugin.getLawCrimeManager().releasePlayer(rec);
                    }
                    JailData jail = plugin.getLawCrimeManager().loadJail(jailId);
                    if (jail != null) {
                        jail.getCellIds().remove(cellId);
                        plugin.getLawCrimeManager().saveJail(jail);
                    }
                    plugin.getLawCrimeManager().deleteCell(cellId);
                    p.sendMessage("\u00a7cCell deleted.");
                    plugin.getGUIManager().openGUI(new CellManagementGUI(plugin, nationId, jailId), p);
                })
        );

        int slot = 9;
        for (CriminalRecord rec : prisoners) {
            if (slot >= 45) break;
            CharacterData cd = plugin.getCharacterManager().getCharacter(rec.getCriminalUUID());
            String name = cd != null ? cd.getFirstName() + " " + cd.getLastName() : "Unknown";
            final UUID recordId = rec.getRecordId();

            addButton(slot, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.PLAYER_HEAD, "\u00a7e" + name,
                            "\u00a77Left-click to \u00a7afree",
                            "\u00a77Right-click to \u00a7bmove"))
                    .consumer(e -> {
                        Player p = (Player) e.getWhoClicked();
                        if (e.getClick() == ClickType.LEFT || e.getClick() == ClickType.SHIFT_LEFT) {
                            CriminalRecord r = plugin.getLawCrimeManager().loadCriminalRecord(recordId);
                            if (r != null) {
                                plugin.getLawCrimeManager().releasePlayer(r);
                            }
                            p.sendMessage("\u00a7aPrisoner freed.");
                            plugin.getGUIManager().openGUI(new CellDetailGUI(plugin, nationId, jailId, cellId), p);
                        } else {
                            plugin.getGUIManager().openGUI(new CellMoveSelectGUI(plugin, nationId, jailId, cellId, recordId), p);
                        }
                    })
            );
            slot++;
        }

        addButton(49, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7cBack"))
                .consumer(e -> plugin.getGUIManager().openGUI(new CellManagementGUI(plugin, nationId, jailId), (Player) e.getWhoClicked()))
        );

        super.decorate(player);
    }
}
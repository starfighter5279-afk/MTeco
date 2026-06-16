package com.mteco.inventory.impl.nation;

import com.cryptomorin.xseries.XMaterial;
import com.mteco.MTeco;
import com.mteco.data.JailData;
import com.mteco.inventory.InventoryButton;
import com.mteco.inventory.InventoryGUI;
import com.mteco.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.UUID;

public class JailDetailGUI extends InventoryGUI {
    private final MTeco plugin;
    private final UUID nationId;
    private final UUID jailId;

    public JailDetailGUI(MTeco plugin, UUID nationId, UUID jailId) {
        this.plugin = plugin;
        this.nationId = nationId;
        this.jailId = jailId;
    }

    @Override
    protected Inventory createInventory() {
        JailData jail = plugin.getLawCrimeManager().loadJail(jailId);
        String title = jail != null ? "\u00a7eJail: " + jail.getName() : "\u00a7eJail Detail";
        return Bukkit.createInventory(null, 27, title);
    }

    @Override
    public void decorate(Player player) {
        fillGlass(27, XMaterial.RED_STAINED_GLASS_PANE);

        JailData jail = plugin.getLawCrimeManager().loadJail(jailId);
        if (jail == null) { super.decorate(player); return; }

        int cellCount = jail.getCellIds().size();

        addButton(4, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.IRON_DOOR, "\u00a7e" + jail.getName(),
                        "\u00a77Cells: \u00a7f" + cellCount,
                        "\u00a77World: \u00a7f" + jail.getWorld(),
                        "\u00a77Corner 1: \u00a7f" + jail.getCorner1X() + ", " + jail.getCorner1Y() + ", " + jail.getCorner1Z(),
                        "\u00a77Corner 2: \u00a7f" + jail.getCorner2X() + ", " + jail.getCorner2Y() + ", " + jail.getCorner2Z()))
                .consumer(e -> {})
        );

        addButton(11, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.IRON_BARS, "\u00a7eCells",
                        "\u00a77View and manage cells.",
                        "\u00a77Total: \u00a7f" + cellCount))
                .consumer(e -> plugin.getGUIManager().openGUI(
                        new CellManagementGUI(plugin, nationId, jailId), (Player) e.getWhoClicked()))
        );

        addButton(12, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.NAME_TAG, "\u00a7eRename Jail",
                        "\u00a77Current: \u00a7f" + jail.getName()))
                .consumer(e -> {
                    Player p = (Player) e.getWhoClicked();
                    plugin.getChatInputManager().requestInput(p, "\u00a7eEnter new jail name:", input -> {
                        String name = input.trim();
                        if (name.isEmpty()) { p.sendMessage("\u00a7cName cannot be empty."); return; }
                        JailData j = plugin.getLawCrimeManager().loadJail(jailId);
                        if (j == null) return;
                        j.setName(name);
                        plugin.getLawCrimeManager().saveJail(j);
                        p.sendMessage("\u00a7aJail renamed to '\u00a76" + name + "\u00a7a'.");
                        plugin.getGUIManager().openGUI(new JailDetailGUI(plugin, nationId, jailId), p);
                    });
                })
        );

        addButton(13, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.LIME_DYE, "\u00a7aCreate Cell",
                        "\u00a77Use the golden shovel to select",
                        "\u00a77two corners for the cell."))
                .consumer(e -> {
                    Player p = (Player) e.getWhoClicked();
                    p.closeInventory();
                    plugin.getChunkSelectionManager().startCellCreation(p, jailId);
                })
        );

        addButton(15, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.RED_DYE, "\u00a7cDelete Jail",
                        "\u00a77Permanently delete this jail",
                        "\u00a77and all of its cells."))
                .consumer(e -> {
                    Player p = (Player) e.getWhoClicked();
                    for (UUID cid : jail.getCellIds()) {
                        plugin.getLawCrimeManager().deleteCell(cid);
                    }
                    plugin.getLawCrimeManager().deleteJail(jailId);
                    p.sendMessage("\u00a7cJail deleted.");
                    plugin.getGUIManager().openGUI(new JailManagementGUI(plugin, nationId), p);
                })
        );

        addButton(22, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7cBack"))
                .consumer(e -> plugin.getGUIManager().openGUI(new JailManagementGUI(plugin, nationId), (Player) e.getWhoClicked()))
        );

        super.decorate(player);
    }
}
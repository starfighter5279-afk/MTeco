package com.dirt.inventory.impl.nation;

import com.cryptomorin.xseries.XMaterial;
import com.dirt.DirtEconomy;
import com.dirt.data.NationData;
import com.dirt.inventory.InventoryButton;
import com.dirt.inventory.InventoryGUI;
import com.dirt.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DeclareWarNationSelectGUI extends InventoryGUI {
    private static final int PAGE_SIZE = 45;

    private final DirtEconomy plugin;
    private final UUID attackingNationId;
    private final int page;

    public DeclareWarNationSelectGUI(DirtEconomy plugin, UUID attackingNationId) {
        this(plugin, attackingNationId, 0);
    }

    public DeclareWarNationSelectGUI(DirtEconomy plugin, UUID attackingNationId, int page) {
        this.plugin = plugin;
        this.attackingNationId = attackingNationId;
        this.page = page;
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 54, "§cDeclare War — Select Nation");
    }

    @Override
    public void decorate(Player player) {
        fillPagedGui(54, XMaterial.RED_STAINED_GLASS_PANE);

        List<NationData> others = new ArrayList<>();
        for (NationData n : plugin.getNationManager().getAllNations()) {
            if (!n.getNationId().equals(attackingNationId)) others.add(n);
        }

        int start = page * PAGE_SIZE;
        for (int i = 0; i < PAGE_SIZE && (start + i) < others.size(); i++) {
            NationData target = others.get(start + i);
            int slot = i;
            addButton(slot, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.WHITE_BANNER,
                            target.getColor1() + target.getName(),
                            "§7Click to start a war with §e" + target.getName() + "§7."))
                    .consumer(e -> plugin.getGUIManager().openGUI(
                            new AttackerRegionSelectGUI(plugin, attackingNationId, target.getNationId(), new ArrayList<>()),
                            (Player) e.getWhoClicked()))
            );
        }

        // Prev page
        if (page > 0) {
            int prevPage = page - 1;
            addButton(45, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "§ePrevious Page"))
                    .consumer(e -> plugin.getGUIManager().openGUI(
                            new DeclareWarNationSelectGUI(plugin, attackingNationId, prevPage),
                            (Player) e.getWhoClicked()))
            );
        }

        // Next page
        if (start + PAGE_SIZE < others.size()) {
            int nextPage = page + 1;
            addButton(53, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "§eNext Page"))
                    .consumer(e -> plugin.getGUIManager().openGUI(
                            new DeclareWarNationSelectGUI(plugin, attackingNationId, nextPage),
                            (Player) e.getWhoClicked()))
            );
        }

        // Back
        addButton(49, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.BARRIER, "§cBack"))
                .consumer(e -> plugin.getGUIManager().openGUI(
                        new NationWarGUI(plugin, attackingNationId),
                        (Player) e.getWhoClicked()))
        );

        super.decorate(player);
    }
}
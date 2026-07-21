package com.dirt.inventory.impl;

import com.cryptomorin.xseries.XMaterial;
import com.dirt.DirtEconomy;
import com.dirt.inventory.InventoryButton;
import com.dirt.inventory.InventoryGUI;
import com.dirt.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class LifeConfirmGUI extends InventoryGUI {
    private final DirtEconomy plugin;

    public LifeConfirmGUI(DirtEconomy plugin) {
        this.plugin = plugin;
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 27, "§4Confirm Death");
    }

    @Override
    public void decorate(Player player) {
        fillGlass(27, XMaterial.RED_STAINED_GLASS_PANE);

        addButton(4, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.BARRIER, "§4Death Warning",
                        "§cAre you sure you want to create a death?",
                        "§cYou will lose all of your funds, inventory",
                        "§citems, and any other progress.",
                        "§cThis action is IRREVERSIBLE!"))
                .consumer(e -> {})
        );

        addButton(11, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.LIME_WOOL, "§aYes, proceed"))
                .consumer(e -> {
                    Player p = (Player) e.getWhoClicked();
                    p.closeInventory();
                    p.sendMessage("§eRight-click any block to place your inheritance chest at that location.");
                    plugin.getLocationSelectionManager().requestLocation(p,
                            "§eRight-click a block to select the chest location.",
                            loc -> plugin.executeDeath(p, loc));
                })
        );

        addButton(15, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.RED_WOOL, "§cNo, go back"))
                .consumer(e -> plugin.getGUIManager().openGUI(new LifeGUI(plugin), (Player) e.getWhoClicked()))
        );

        super.decorate(player);
    }
}
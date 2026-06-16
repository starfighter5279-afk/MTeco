package com.mteco.inventory.impl.nation;

import com.cryptomorin.xseries.XMaterial;
import com.mteco.MTeco;
import com.mteco.data.NationData;
import com.mteco.inventory.InventoryButton;
import com.mteco.inventory.InventoryGUI;
import com.mteco.util.ItemUtil;
import com.mteco.util.CurrencyUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.UUID;

public class NationWarGUI extends InventoryGUI {
    private final MTeco plugin;
    private final UUID nationId;

    public NationWarGUI(MTeco plugin, UUID nationId) {
        this.plugin = plugin;
        this.nationId = nationId;
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 27, "§cWar");
    }

    @Override
    public void decorate(Player player) {
        fillGlass(27, XMaterial.RED_STAINED_GLASS_PANE);

        addButton(11, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.IRON_SWORD, "§cDeclare War",
                        "§7Declare war on another nation.",
                        "§7Select up to §e5 §7attacking regions",
                        "§7and choose regions to claim.",
                        "§7Cost: §a" + CurrencyUtil.symbol() + "10,000"))
                .consumer(e -> plugin.getGUIManager().openGUI(
                        new DeclareWarNationSelectGUI(plugin, nationId),
                        (Player) e.getWhoClicked()))
        );

        addButton(15, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.SHIELD, "§eManage Current Wars",
                        "§7View and manage wars your nation",
                        "§7is currently involved in."))
                .consumer(e -> plugin.getGUIManager().openGUI(
                        new ManageWarsGUI(plugin, nationId),
                        (Player) e.getWhoClicked()))
        );

        super.decorate(player);
    }
}
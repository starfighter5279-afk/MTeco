package com.mteco.inventory.impl.nation;

import com.cryptomorin.xseries.XMaterial;
import com.mteco.MTeco;
import com.mteco.config.RolesConfig;
import com.mteco.data.NationData;
import com.mteco.inventory.InventoryButton;
import com.mteco.inventory.InventoryGUI;
import com.mteco.util.ItemUtil;
import com.mteco.util.CurrencyUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.UUID;

public class VicePresidentManagementGUI extends InventoryGUI {
    private final MTeco plugin;
    private final UUID nationId;

    public VicePresidentManagementGUI(MTeco plugin, UUID nationId) {
        this.plugin = plugin;
        this.nationId = nationId;
    }

    @Override
    protected Inventory createInventory() {
        NationData nation = plugin.getNationManager().loadNation(nationId);
        String name = nation != null ? nation.getColor1() + nation.getName() : "§eNation";
        return Bukkit.createInventory(null, 27, name + " §8Management");
    }

    @Override
    public void decorate(Player player) {
        fillGlass(27, XMaterial.BLUE_STAINED_GLASS_PANE);

        RolesConfig rc = plugin.getRolesConfig();

        if (rc.canVpManageRegions()) {
            addButton(11, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.MAP, "§eManage Regions",
                            "§7Create and manage your nation's regions."))
                    .consumer(e -> plugin.getGUIManager().openGUI(new RegionsGUI(plugin, nationId), (Player) e.getWhoClicked()))
            );
        }

        if (rc.canVpManageWar()) {
            addButton(13, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.DIAMOND_SWORD, "§cWar",
                            "§7Declare war on another nation,",
                            "§7or manage your current wars."))
                    .consumer(e -> plugin.getGUIManager().openGUI(new NationWarGUI(plugin, nationId), (Player) e.getWhoClicked()))
            );
        }

        if (rc.canVpViewTreasury()) {
            addButton(15, new InventoryButton()
                    .creator(p -> {
                        NationData nation = plugin.getNationManager().loadNation(nationId);
                        double balance = nation != null ? plugin.getNationManager().getTreasuryBalance(nation.getName()) : 0.0;
                        return ItemUtil.buildItem(XMaterial.GOLD_INGOT, "§6National Treasury",
                                "§7Current Balance: §a" + CurrencyUtil.symbol() + String.format("%.2f", balance),
                                "§7Click to view treasury transactions.");
                    })
                    .consumer(e -> plugin.getGUIManager().openGUI(new NationTreasuryGUI(plugin, nationId, false), (Player) e.getWhoClicked()))
            );
        }

        if (plugin.getSettings().isLawsEnabled() || plugin.getSettings().isCrimeEnabled()) {
            addButton(22, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.WRITABLE_BOOK, "§eManage Laws And Crime",
                            "§7Laws, legislated crimes, and enforcement."))
                    .consumer(e -> plugin.getGUIManager().openGUI(
                            new LawsAndCrimeMenuGUI(plugin, nationId, false), (Player) e.getWhoClicked()))
            );
        }

        super.decorate(player);
    }
}
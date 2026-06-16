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

public class TreasurerGUI extends InventoryGUI {
    private final MTeco plugin;
    private final UUID nationId;

    public TreasurerGUI(MTeco plugin, UUID nationId) {
        this.plugin = plugin;
        this.nationId = nationId;
    }

    @Override
    protected Inventory createInventory() {
        NationData nation = plugin.getNationManager().loadNation(nationId);
        String name = nation != null ? nation.getColor1() + nation.getName() : "\u00a7eNation";
        return Bukkit.createInventory(null, 27, name + " \u00a76Treasurer");
    }

    @Override
    public void decorate(Player player) {
        fillGlass(27, XMaterial.BLUE_STAINED_GLASS_PANE);

        if (plugin.getRolesConfig().canTreasurerManageTreasury()) {
            addButton(13, new InventoryButton()
                    .creator(p -> {
                        NationData nation = plugin.getNationManager().loadNation(nationId);
                        double balance = nation != null ? plugin.getNationManager().getTreasuryBalance(nation.getName()) : 0.0;
                        return ItemUtil.buildItem(XMaterial.GOLD_INGOT, "\u00a76National Treasury",
                                "\u00a77Current Balance: \u00a7a" + CurrencyUtil.symbol() + String.format("%.2f", balance),
                                "\u00a77Click to view transactions and manage funds.");
                    })
                    .consumer(e -> plugin.getGUIManager().openGUI(new NationTreasuryGUI(plugin, nationId, true), (Player) e.getWhoClicked()))
            );
        }

        if (plugin.getSettings().isMintersEnabled()) {
            addButton(11, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.CAULDRON, "\u00a76Create Minter",
                            "\u00a77Select a block to place a minter.",
                            "\u00a77Minters process gold into vault units."))
                    .consumer(e -> {
                        Player p = (Player) e.getWhoClicked();
                        p.closeInventory();
                        plugin.getLocationSelectionManager().requestLocation(p,
                                "\u00a7eRight-click a block to place the minter base.", loc -> {
                                    plugin.getMinterManager().createMinterAtLocation(p, nationId, loc);
                                });
                    })
            );
        }

        super.decorate(player);
    }
}
package com.dirt.inventory.impl.nation;

import com.cryptomorin.xseries.XMaterial;
import com.dirt.DirtEconomy;
import com.dirt.data.RegionData;
import com.dirt.inventory.InventoryButton;
import com.dirt.inventory.InventoryGUI;
import com.dirt.util.ItemUtil;
import com.dirt.util.CurrencyUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;

import java.util.UUID;

public class GovernorPropertiesGUI extends InventoryGUI {
    private final DirtEconomy plugin;
    private final UUID regionId;

    public GovernorPropertiesGUI(DirtEconomy plugin, UUID regionId) {
        this.plugin = plugin;
        this.regionId = regionId;
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 27, "§ePlayer Properties");
    }

    @Override
    public void decorate(Player player) {
        fillGlass(27, XMaterial.BLUE_STAINED_GLASS_PANE);

        RegionData region = plugin.getNationManager().loadRegion(regionId);

        addButton(10, new InventoryButton()
                .creator(p -> {
                    int count = region != null ? region.getPropertyIds().size() : 0;
                    return ItemUtil.buildItem(XMaterial.OAK_DOOR, "§aActive Properties",
                            "§7" + count + " propert(ies) in this region.",
                            "§7Click to view all properties.");
                })
                .consumer(e -> plugin.getGUIManager().openGUI(new ActivePropertiesListGUI(plugin, regionId), (Player) e.getWhoClicked()))
        );

        addButton(13, new InventoryButton()
                .creator(p -> {
                    int pending = (int) plugin.getNationManager().getPurchaseRequestsByRegion(regionId).stream()
                            .filter(r -> !r.isGovernorApproved() && !r.isTreasurerApproved()).count();
                    return ItemUtil.buildItem(XMaterial.PAPER, "§ePurchase Requests",
                            "§7" + pending + " pending request(s).",
                            "§7Click to approve or deny.");
                })
                .consumer(e -> plugin.getGUIManager().openGUI(new PropertyPurchaseRequestsGUI(plugin, regionId), (Player) e.getWhoClicked()))
        );

        addButton(16, new InventoryButton()
                .creator(p -> {
                    double rate = region != null ? region.getPropertyChunkRate() : 0.0;
                    int intervalDays = region != null ? region.getPropertyCollectionIntervalDays() : 1;
                    return ItemUtil.buildItem(XMaterial.GOLD_NUGGET, "§6Property Chunk Rate",
                            "§7Current: §a" + CurrencyUtil.symbol() + String.format("%.2f", rate) + " per chunk",
                            "§7Collection interval: §b" + intervalDays + " MC day(s)",
                            "§7Left-click to change rate.",
                            "§7Right-click to set interval.");
                })
                .consumer(e -> {
                    Player p = (Player) e.getWhoClicked();
                    if (e.getClick() == ClickType.RIGHT) {
                        plugin.getChatInputManager().requestInput(p, "§eEnter property collection interval in MC days (1+):", input -> {
                            try {
                                int days = Integer.parseInt(input);
                                if (days < 1) { p.sendMessage("§cInterval must be at least 1."); return; }
                                RegionData r = plugin.getNationManager().loadRegion(regionId);
                                if (r == null) return;
                                r.setPropertyCollectionIntervalDays(days);
                                plugin.getNationManager().saveRegion(r);
                                p.sendMessage("§aProperty collection interval set to §e" + days + " MC day(s)§a.");
                            } catch (NumberFormatException ex) { p.sendMessage("§cInvalid number."); }
                        });
                    } else {
                        plugin.getChatInputManager().requestInput(p, "§eEnter new property chunk rate:", input -> {
                            try {
                                double rate = Double.parseDouble(input);
                                RegionData r = plugin.getNationManager().loadRegion(regionId);
                                if (r == null) return;
                                r.setPropertyChunkRate(rate);
                                plugin.getNationManager().saveRegion(r);
                                p.sendMessage("§aProperty chunk rate updated to §e" + CurrencyUtil.symbol() + String.format("%.2f", rate) + "§a.");
                            } catch (NumberFormatException ex) { p.sendMessage("§cInvalid number."); }
                        });
                    }
                })
        );

        addButton(26, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "§cBack"))
                .consumer(e -> plugin.getGUIManager().openGUI(new GovernorRegionManagementGUI(plugin, regionId), (Player) e.getWhoClicked()))
        );

        super.decorate(player);
    }
}
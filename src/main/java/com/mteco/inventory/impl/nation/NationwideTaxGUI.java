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

public class NationwideTaxGUI extends InventoryGUI {
    private final MTeco plugin;
    private final UUID nationId;

    public NationwideTaxGUI(MTeco plugin, UUID nationId) {
        this.plugin = plugin;
        this.nationId = nationId;
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 27, "\u00a78Nationwide Tax Classes");
    }

    @Override
    public void decorate(Player player) {
        fillGlass(27, XMaterial.BLUE_STAINED_GLASS_PANE);

        NationData nation = plugin.getNationManager().loadNation(nationId);
        double lower  = nation != null ? nation.getLowerClassTaxRate()  : 0.0;
        double middle = nation != null ? nation.getMiddleClassTaxRate() : 0.0;
        double upper  = nation != null ? nation.getUpperClassTaxRate()  : 0.0;

        double lowerMax = plugin.getSettings().getTaxBracketLowerMax();
        double middleMax = plugin.getSettings().getTaxBracketMiddleMax();

        addButton(9, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.IRON_NUGGET, "\u00a7eLower Class",
                        "\u00a77Balance: \u00a7f" + CurrencyUtil.symbol() + "0 \u00a78- \u00a7f" + CurrencyUtil.symbol() + String.format("%,.0f", lowerMax),
                        "\u00a77Current tax rate: \u00a7a" + String.format("%.1f", lower) + "%",
                        "\u00a77Click to set rate."))
                .consumer(e -> {
                    Player p = (Player) e.getWhoClicked();
                    p.closeInventory();
                    plugin.getChatInputManager().requestInput(p, "\u00a7eEnter tax rate for Lower Class (0-100):", input -> {
                        try {
                            double rate = Double.parseDouble(input);
                            if (rate < 0 || rate > 100) { p.sendMessage("\u00a7cRate must be between 0 and 100."); return; }
                            NationData n = plugin.getNationManager().loadNation(nationId);
                            if (n == null) return;
                            n.setLowerClassTaxRate(rate);
                            plugin.getNationManager().saveNation(n);
                            p.sendMessage("\u00a7aLower Class tax rate set to \u00a7e" + String.format("%.1f", rate) + "%\u00a7a.");
                        } catch (NumberFormatException ex) {
                            p.sendMessage("\u00a7cInvalid number.");
                        }
                    });
                })
        );

        addButton(13, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.GOLD_NUGGET, "\u00a7eMiddle Class",
                        "\u00a77Balance: \u00a7f" + CurrencyUtil.symbol() + String.format("%,.0f", lowerMax + 1) + " \u00a78- \u00a7f" + CurrencyUtil.symbol() + String.format("%,.0f", middleMax),
                        "\u00a77Current tax rate: \u00a7a" + String.format("%.1f", middle) + "%",
                        "\u00a77Click to set rate."))
                .consumer(e -> {
                    Player p = (Player) e.getWhoClicked();
                    p.closeInventory();
                    plugin.getChatInputManager().requestInput(p, "\u00a7eEnter tax rate for Middle Class (0-100):", input -> {
                        try {
                            double rate = Double.parseDouble(input);
                            if (rate < 0 || rate > 100) { p.sendMessage("\u00a7cRate must be between 0 and 100."); return; }
                            NationData n = plugin.getNationManager().loadNation(nationId);
                            if (n == null) return;
                            n.setMiddleClassTaxRate(rate);
                            plugin.getNationManager().saveNation(n);
                            p.sendMessage("\u00a7aMiddle Class tax rate set to \u00a7e" + String.format("%.1f", rate) + "%\u00a7a.");
                        } catch (NumberFormatException ex) {
                            p.sendMessage("\u00a7cInvalid number.");
                        }
                    });
                })
        );

        addButton(17, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.GOLD_INGOT, "\u00a7eUpper Class",
                        "\u00a77Balance: \u00a7f" + CurrencyUtil.symbol() + String.format("%,.0f", middleMax + 1) + "+",
                        "\u00a77Current tax rate: \u00a7a" + String.format("%.1f", upper) + "%",
                        "\u00a77Click to set rate."))
                .consumer(e -> {
                    Player p = (Player) e.getWhoClicked();
                    p.closeInventory();
                    plugin.getChatInputManager().requestInput(p, "\u00a7eEnter tax rate for Upper Class (0-100):", input -> {
                        try {
                            double rate = Double.parseDouble(input);
                            if (rate < 0 || rate > 100) { p.sendMessage("\u00a7cRate must be between 0 and 100."); return; }
                            NationData n = plugin.getNationManager().loadNation(nationId);
                            if (n == null) return;
                            n.setUpperClassTaxRate(rate);
                            plugin.getNationManager().saveNation(n);
                            p.sendMessage("\u00a7aUpper Class tax rate set to \u00a7e" + String.format("%.1f", rate) + "%\u00a7a.");
                        } catch (NumberFormatException ex) {
                            p.sendMessage("\u00a7cInvalid number.");
                        }
                    });
                })
        );

        super.decorate(player);
    }
}
package com.dirt.inventory.impl.nation;

import com.cryptomorin.xseries.XMaterial;
import com.dirt.DirtEconomy;
import com.dirt.data.NationData;
import com.dirt.inventory.InventoryButton;
import com.dirt.inventory.InventoryGUI;
import com.dirt.util.ItemUtil;
import com.dirt.util.CurrencyUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;

import java.util.UUID;

public class NationwideTaxGUI extends InventoryGUI {
    private final DirtEconomy plugin;
    private final UUID nationId;

    public NationwideTaxGUI(DirtEconomy plugin, UUID nationId) {
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
        int intervalDays = nation != null ? nation.getTaxCollectionIntervalDays() : 1;

        double lowerMax = plugin.getSettings().getTaxBracketLowerMax();
        double middleMax = plugin.getSettings().getTaxBracketMiddleMax();

        addButton(9, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.IRON_NUGGET, "\u00a7eLower Class",
                        "\u00a77Balance: \u00a7f" + CurrencyUtil.symbol() + "0 \u00a78- \u00a7f" + CurrencyUtil.symbol() + String.format("%,.0f", lowerMax),
                        "\u00a77Current tax rate: \u00a7a" + String.format("%.1f", lower) + "%",
                        "\u00a77Collection interval: \u00a7b" + intervalDays + " MC day(s)",
                        "\u00a77Left-click to set rate.",
                        "\u00a77Right-click to set interval."))
                .consumer(e -> {
                    Player p = (Player) e.getWhoClicked();
                    p.closeInventory();
                    if (e.getClick() == ClickType.RIGHT) {
                        plugin.getChatInputManager().requestInput(p, "\u00a7eEnter tax collection interval in MC days (1+):", input -> {
                            try {
                                int days = Integer.parseInt(input);
                                if (days < 1) { p.sendMessage("\u00a7cInterval must be at least 1."); return; }
                                NationData n = plugin.getNationManager().loadNation(nationId);
                                if (n == null) return;
                                n.setTaxCollectionIntervalDays(days);
                                plugin.getNationManager().saveNation(n);
                                p.sendMessage("\u00a7aTax collection interval set to \u00a7e" + days + " MC day(s)\u00a7a.");
                            } catch (NumberFormatException ex) {
                                p.sendMessage("\u00a7cInvalid number.");
                            }
                        });
                    } else {
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
                    }
                })
        );

        addButton(13, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.GOLD_NUGGET, "\u00a7eMiddle Class",
                        "\u00a77Balance: \u00a7f" + CurrencyUtil.symbol() + String.format("%,.0f", lowerMax + 1) + " \u00a78- \u00a7f" + CurrencyUtil.symbol() + String.format("%,.0f", middleMax),
                        "\u00a77Current tax rate: \u00a7a" + String.format("%.1f", middle) + "%",
                        "\u00a77Collection interval: \u00a7b" + intervalDays + " MC day(s)",
                        "\u00a77Left-click to set rate.",
                        "\u00a77Right-click to set interval."))
                .consumer(e -> {
                    Player p = (Player) e.getWhoClicked();
                    p.closeInventory();
                    if (e.getClick() == ClickType.RIGHT) {
                        plugin.getChatInputManager().requestInput(p, "\u00a7eEnter tax collection interval in MC days (1+):", input -> {
                            try {
                                int days = Integer.parseInt(input);
                                if (days < 1) { p.sendMessage("\u00a7cInterval must be at least 1."); return; }
                                NationData n = plugin.getNationManager().loadNation(nationId);
                                if (n == null) return;
                                n.setTaxCollectionIntervalDays(days);
                                plugin.getNationManager().saveNation(n);
                                p.sendMessage("\u00a7aTax collection interval set to \u00a7e" + days + " MC day(s)\u00a7a.");
                            } catch (NumberFormatException ex) {
                                p.sendMessage("\u00a7cInvalid number.");
                            }
                        });
                    } else {
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
                    }
                })
        );

        addButton(17, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.GOLD_INGOT, "\u00a7eUpper Class",
                        "\u00a77Balance: \u00a7f" + CurrencyUtil.symbol() + String.format("%,.0f", middleMax + 1) + "+",
                        "\u00a77Current tax rate: \u00a7a" + String.format("%.1f", upper) + "%",
                        "\u00a77Collection interval: \u00a7b" + intervalDays + " MC day(s)",
                        "\u00a77Left-click to set rate.",
                        "\u00a77Right-click to set interval."))
                .consumer(e -> {
                    Player p = (Player) e.getWhoClicked();
                    p.closeInventory();
                    if (e.getClick() == ClickType.RIGHT) {
                        plugin.getChatInputManager().requestInput(p, "\u00a7eEnter tax collection interval in MC days (1+):", input -> {
                            try {
                                int days = Integer.parseInt(input);
                                if (days < 1) { p.sendMessage("\u00a7cInterval must be at least 1."); return; }
                                NationData n = plugin.getNationManager().loadNation(nationId);
                                if (n == null) return;
                                n.setTaxCollectionIntervalDays(days);
                                plugin.getNationManager().saveNation(n);
                                p.sendMessage("\u00a7aTax collection interval set to \u00a7e" + days + " MC day(s)\u00a7a.");
                            } catch (NumberFormatException ex) {
                                p.sendMessage("\u00a7cInvalid number.");
                            }
                        });
                    } else {
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
                    }
                })
        );

        super.decorate(player);
    }
}
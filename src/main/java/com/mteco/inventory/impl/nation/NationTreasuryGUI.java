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

public class NationTreasuryGUI extends InventoryGUI {
    private final MTeco plugin;
    private final UUID nationId;
    private final boolean canWithdraw;

    public NationTreasuryGUI(MTeco plugin, UUID nationId, boolean canWithdraw) {
        this.plugin = plugin;
        this.nationId = nationId;
        this.canWithdraw = canWithdraw;
    }

    @Override
    protected Inventory createInventory() {
        NationData nation = plugin.getNationManager().loadNation(nationId);
        String name = nation != null ? nation.getColor1() + nation.getName() : "§eNation";
        return Bukkit.createInventory(null, 27, name + " §6Treasury");
    }

    @Override
    public void decorate(Player player) {
        fillGlass(27, XMaterial.BLUE_STAINED_GLASS_PANE);

        NationData nation = plugin.getNationManager().loadNation(nationId);
        double balance = nation != null ? plugin.getNationManager().getTreasuryBalance(nation.getName()) : 0.0;

        // Balance display
        addButton(4, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.GOLD_BLOCK, "§6National Treasury Balance",
                        "§7Current Balance: §a" + CurrencyUtil.symbol() + String.format("%.2f", balance)))
                .consumer(e -> {})
        );

        // View Transactions button
        addButton(12, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.BOOK, "§eView Transactions",
                        "§7Click to view all treasury",
                        "§7transaction history."))
                .consumer(e -> {
                    Player clicker = (Player) e.getWhoClicked();
                    plugin.getGUIManager().openGUI(new TransactionHistoryGUI(plugin, nationId, canWithdraw), clicker);
                })
        );

        if (canWithdraw) {
            addButton(14, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.EMERALD, "§aWithdraw from Treasury",
                            "§7Transfer funds from the nation treasury",
                            "§7to your personal balance."))
                    .consumer(e -> {
                        Player clicker = (Player) e.getWhoClicked();
                        clicker.closeInventory();
                        NationData n = plugin.getNationManager().loadNation(nationId);
                        if (n == null) return;
                        double current = plugin.getNationManager().getTreasuryBalance(n.getName());
                        plugin.getChatInputManager().requestInput(clicker,
                                "§eEnter the amount to withdraw (Treasury: §a" + CurrencyUtil.symbol() + String.format("%.2f", current) + "§e):",
                                input -> {
                                    double amount;
                                    try { amount = Double.parseDouble(input); } catch (NumberFormatException ex) {
                                        clicker.sendMessage("§cInvalid amount.");
                                        plugin.getGUIManager().openGUI(new NationTreasuryGUI(plugin, nationId, canWithdraw), clicker);
                                        return;
                                    }
                                    if (amount <= 0) {
                                        clicker.sendMessage("§cAmount must be greater than zero.");
                                        plugin.getGUIManager().openGUI(new NationTreasuryGUI(plugin, nationId, canWithdraw), clicker);
                                        return;
                                    }
                                    if (amount > plugin.getNationManager().getTreasuryBalance(n.getName())) {
                                        clicker.sendMessage("§cThe treasury does not have enough funds.");
                                        plugin.getGUIManager().openGUI(new NationTreasuryGUI(plugin, nationId, canWithdraw), clicker);
                                        return;
                                    }
                                    plugin.getNationManager().withdrawFromTreasury(n.getName(), amount);
                                    plugin.getEconomy().depositPlayer(clicker, amount);
                                    com.mteco.data.CharacterData charData = plugin.getCharacterManager().getCharacter(clicker.getUniqueId());
                                    String withdrawerName = charData != null ? charData.getFirstName() + " " + charData.getLastName() : clicker.getName();
                                    plugin.getNationManager().logTransaction(nationId, "WITHDRAWAL",
                                            amount, withdrawerName + " withdrew from treasury");
                                    clicker.sendMessage("§aYou withdrew §e" + CurrencyUtil.symbol() + String.format("%.2f", amount) + "§a from the treasury.");
                                    plugin.getGUIManager().openGUI(new NationTreasuryGUI(plugin, nationId, canWithdraw), clicker);
                                });
                    })
            );
        }

        super.decorate(player);
    }
}
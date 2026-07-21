package com.dirt.inventory.impl.nation;

import com.cryptomorin.xseries.XMaterial;
import com.dirt.DirtEconomy;
import com.dirt.data.CharacterData;
import com.dirt.data.NationData;
import com.dirt.inventory.InventoryButton;
import com.dirt.inventory.InventoryGUI;
import com.dirt.util.CurrencyUtil;
import com.dirt.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class NationPayNationsGUI extends InventoryGUI {
    private final DirtEconomy plugin;
    private final UUID nationId;
    private final int page;

    public NationPayNationsGUI(DirtEconomy plugin, UUID nationId, int page) {
        this.plugin = plugin;
        this.nationId = nationId;
        this.page = page;
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 54, "\u00a7ePay Other Nation");
    }

    @Override
    public void decorate(Player player) {
        fillPagedGui(54, XMaterial.BLUE_STAINED_GLASS_PANE);

        List<NationData> otherNations = plugin.getNationManager().getAllNations().stream()
                .filter(n -> !n.getNationId().equals(nationId))
                .collect(Collectors.toList());

        int perPage = 45;
        int start = page * perPage;
        int end = Math.min(start + perPage, otherNations.size());

        for (int i = start; i < end; i++) {
            NationData target = otherNations.get(i);
            int slot = i - start;
            addButton(slot, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.MAP,
                            target.getColor1() + target.getName(),
                            "\u00a77Click to send funds to this nation's treasury."))
                    .consumer(e -> {
                        Player clicker = (Player) e.getWhoClicked();
                        clicker.closeInventory();
                        promptPayment(clicker, target.getName(), amount -> {
                            NationData payer = plugin.getNationManager().loadNation(nationId);
                            if (payer == null) return;
                            if (!plugin.getNationManager().withdrawFromTreasury(payer.getName(), amount)) {
                                clicker.sendMessage("\u00a7cThe treasury does not have enough funds.");
                                plugin.getGUIManager().openGUI(new NationPayNationsGUI(plugin, nationId, page), clicker);
                                return;
                            }
                            plugin.getNationManager().depositToTreasury(target.getName(), amount);
                            CharacterData charData = plugin.getCharacterManager().getCharacter(clicker.getUniqueId());
                            String payerName = charData != null ? charData.getFirstName() + " " + charData.getLastName() : clicker.getName();
                            plugin.getNationManager().logTransaction(nationId, "PAYMENT_SENT", amount,
                                    "Paid to nation " + target.getName() + " by " + payerName);
                            plugin.getNationManager().logTransaction(target.getNationId(), "PAYMENT_RECEIVED", amount,
                                    "Received from nation " + payer.getName());
                            clicker.sendMessage("\u00a7aSent \u00a7e" + CurrencyUtil.symbol() + String.format("%.2f", amount) + "\u00a7a to the \u00a76" + target.getName() + "\u00a7a treasury.");
                            if (target.getPresidentUUID() != null) {
                                Player targetPres = Bukkit.getPlayer(target.getPresidentUUID());
                                if (targetPres != null) targetPres.sendMessage("\u00a7aYour nation received \u00a7e" + CurrencyUtil.symbol() + String.format("%.2f", amount) + "\u00a7a from " + payer.getName() + "\u00a7a.");
                            }
                            plugin.getGUIManager().openGUI(new NationPayNationsGUI(plugin, nationId, page), clicker);
                        });
                    })
            );
        }

        if (page > 0) {
            int prev = page - 1;
            addButton(45, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7ePrevious Page"))
                    .consumer(e -> plugin.getGUIManager().openGUI(new NationPayNationsGUI(plugin, nationId, prev), (Player) e.getWhoClicked()))
            );
        }
        if (end < otherNations.size()) {
            int next = page + 1;
            addButton(53, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7eNext Page"))
                    .consumer(e -> plugin.getGUIManager().openGUI(new NationPayNationsGUI(plugin, nationId, next), (Player) e.getWhoClicked()))
            );
        }

        addButton(49, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.BARRIER, "\u00a7cBack"))
                .consumer(e -> plugin.getGUIManager().openGUI(new NationPayGUI(plugin, nationId), (Player) e.getWhoClicked()))
        );

        super.decorate(player);
    }

    private void promptPayment(Player player, String targetName, java.util.function.Consumer<Double> onConfirm) {
        NationData nation = plugin.getNationManager().loadNation(nationId);
        double balance = nation != null ? plugin.getNationManager().getTreasuryBalance(nation.getName()) : 0.0;
        plugin.getChatInputManager().requestInput(player,
                "\u00a7eEnter amount to pay nation \u00a76" + targetName + "\u00a7e (Treasury: \u00a7a" + CurrencyUtil.symbol() + String.format("%.2f", balance) + "\u00a7e):",
                input -> {
                    double amount;
                    try { amount = Double.parseDouble(input); } catch (NumberFormatException ex) {
                        player.sendMessage("\u00a7cInvalid amount.");
                        plugin.getGUIManager().openGUI(new NationPayNationsGUI(plugin, nationId, page), player);
                        return;
                    }
                    if (amount <= 0) {
                        player.sendMessage("\u00a7cAmount must be greater than zero.");
                        plugin.getGUIManager().openGUI(new NationPayNationsGUI(plugin, nationId, page), player);
                        return;
                    }
                    onConfirm.accept(amount);
                });
    }
}
package com.mteco.inventory.impl.nation;

import com.cryptomorin.xseries.XMaterial;
import com.mteco.MTeco;
import com.mteco.data.CharacterData;
import com.mteco.data.NationData;
import com.mteco.inventory.InventoryButton;
import com.mteco.inventory.InventoryGUI;
import com.mteco.util.CurrencyUtil;
import com.mteco.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.List;
import java.util.UUID;

public class NationPayCharactersGUI extends InventoryGUI {
    private final MTeco plugin;
    private final UUID nationId;
    private final int page;

    public NationPayCharactersGUI(MTeco plugin, UUID nationId, int page) {
        this.plugin = plugin;
        this.nationId = nationId;
        this.page = page;
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 54, "\u00a7ePay Character");
    }

    @Override
    public void decorate(Player player) {
        fillPagedGui(54, XMaterial.BLUE_STAINED_GLASS_PANE);

        List<CharacterData> allChars = plugin.getCharacterManager().getAllCharacters();
        int perPage = 45;
        int start = page * perPage;
        int end = Math.min(start + perPage, allChars.size());

        for (int i = start; i < end; i++) {
            CharacterData c = allChars.get(i);
            int slot = i - start;
            addButton(slot, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.PLAYER_HEAD,
                            "\u00a7e" + c.getFirstName() + " " + c.getLastName(),
                            "\u00a77Gender: \u00a7f" + c.getGender(),
                            "\u00a77Click to pay this character."))
                    .consumer(e -> {
                        Player clicker = (Player) e.getWhoClicked();
                        clicker.closeInventory();
                        promptPayment(clicker, c.getFirstName() + " " + c.getLastName(), amount -> {
                            NationData nation = plugin.getNationManager().loadNation(nationId);
                            if (nation == null) return;
                            if (!plugin.getNationManager().withdrawFromTreasury(nation.getName(), amount)) {
                                clicker.sendMessage("\u00a7cThe treasury does not have enough funds.");
                                plugin.getGUIManager().openGUI(new NationPayCharactersGUI(plugin, nationId, page), clicker);
                                return;
                            }
                            plugin.getEconomy().depositPlayer(Bukkit.getOfflinePlayer(c.getPlayerUuid()), amount);
                            CharacterData payerChar = plugin.getCharacterManager().getCharacter(clicker.getUniqueId());
                            String payerName = payerChar != null ? payerChar.getFirstName() + " " + payerChar.getLastName() : clicker.getName();
                            plugin.getNationManager().logTransaction(nationId, "PAYMENT_SENT", amount,
                                    "Paid to " + c.getFirstName() + " " + c.getLastName() + " by " + payerName);
                            clicker.sendMessage("\u00a7aPaid \u00a7e" + CurrencyUtil.symbol() + String.format("%.2f", amount) + "\u00a7a to \u00a76" + c.getFirstName() + " " + c.getLastName() + "\u00a7a.");
                            Player target = Bukkit.getPlayer(c.getPlayerUuid());
                            if (target != null) target.sendMessage("\u00a7aYou received \u00a7e" + CurrencyUtil.symbol() + String.format("%.2f", amount) + "\u00a7a from the " + nation.getName() + " treasury.");
                            plugin.getGUIManager().openGUI(new NationPayCharactersGUI(plugin, nationId, page), clicker);
                        });
                    })
            );
        }

        if (page > 0) {
            int prev = page - 1;
            addButton(45, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7ePrevious Page"))
                    .consumer(e -> plugin.getGUIManager().openGUI(new NationPayCharactersGUI(plugin, nationId, prev), (Player) e.getWhoClicked()))
            );
        }
        if (end < allChars.size()) {
            int next = page + 1;
            addButton(53, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7eNext Page"))
                    .consumer(e -> plugin.getGUIManager().openGUI(new NationPayCharactersGUI(plugin, nationId, next), (Player) e.getWhoClicked()))
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
                "\u00a7eEnter amount to pay \u00a76" + targetName + "\u00a7e (Treasury: \u00a7a" + CurrencyUtil.symbol() + String.format("%.2f", balance) + "\u00a7e):",
                input -> {
                    double amount;
                    try { amount = Double.parseDouble(input); } catch (NumberFormatException ex) {
                        player.sendMessage("\u00a7cInvalid amount.");
                        plugin.getGUIManager().openGUI(new NationPayCharactersGUI(plugin, nationId, page), player);
                        return;
                    }
                    if (amount <= 0) {
                        player.sendMessage("\u00a7cAmount must be greater than zero.");
                        plugin.getGUIManager().openGUI(new NationPayCharactersGUI(plugin, nationId, page), player);
                        return;
                    }
                    onConfirm.accept(amount);
                });
    }
}
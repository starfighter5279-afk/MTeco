package com.dirt.inventory.impl.nation;

import com.cryptomorin.xseries.XMaterial;
import com.dirt.DirtEconomy;
import com.dirt.data.BusinessData;
import com.dirt.data.CharacterData;
import com.dirt.data.NationData;
import com.dirt.inventory.InventoryButton;
import com.dirt.inventory.InventoryGUI;
import com.dirt.util.CurrencyUtil;
import com.dirt.util.ItemUtil;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.Bukkit;

import java.util.List;
import java.util.UUID;

public class NationPayBusinessesGUI extends InventoryGUI {
    private final DirtEconomy plugin;
    private final UUID nationId;
    private final int page;

    public NationPayBusinessesGUI(DirtEconomy plugin, UUID nationId, int page) {
        this.plugin = plugin;
        this.nationId = nationId;
        this.page = page;
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 54, "\u00a7ePay Business");
    }

    @Override
    public void decorate(Player player) {
        fillPagedGui(54, XMaterial.BLUE_STAINED_GLASS_PANE);

        List<BusinessData> businesses = plugin.getBusinessManager().getAllBusinesses();
        int perPage = 45;
        int start = page * perPage;
        int end = Math.min(start + perPage, businesses.size());

        for (int i = start; i < end; i++) {
            BusinessData biz = businesses.get(i);
            int slot = i - start;
            addButton(slot, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.CHEST,
                            "\u00a76" + biz.getName(),
                            "\u00a77" + biz.getDescription(),
                            "\u00a77Click to send funds to this business."))
                    .consumer(e -> {
                        Player clicker = (Player) e.getWhoClicked();
                        clicker.closeInventory();
                        promptPayment(clicker, biz.getName(), amount -> {
                            NationData nation = plugin.getNationManager().loadNation(nationId);
                            if (nation == null) return;
                            if (!plugin.getNationManager().withdrawFromTreasury(nation.getName(), amount)) {
                                clicker.sendMessage("\u00a7cThe treasury does not have enough funds.");
                                plugin.getGUIManager().openGUI(new NationPayBusinessesGUI(plugin, nationId, page), clicker);
                                return;
                            }
                            BusinessData freshBiz = plugin.getBusinessManager().loadBusiness(biz.getBusinessId());
                            if (freshBiz == null) {
                                clicker.sendMessage("\u00a7cBusiness no longer exists.");
                                plugin.getGUIManager().openGUI(new NationPayBusinessesGUI(plugin, nationId, page), clicker);
                                return;
                            }
                            plugin.getBusinessManager().depositToTreasury(freshBiz, amount);
                            CharacterData charData = plugin.getCharacterManager().getCharacter(clicker.getUniqueId());
                            String payerName = charData != null ? charData.getFirstName() + " " + charData.getLastName() : clicker.getName();
                            plugin.getNationManager().logTransaction(nationId, "PAYMENT_SENT", amount,
                                    "Paid to business " + biz.getName() + " by " + payerName);
                            clicker.sendMessage("\u00a7aSent \u00a7e" + CurrencyUtil.symbol() + String.format("%.2f", amount) + "\u00a7a to \u00a76" + biz.getName() + "\u00a7a.");
                            // Notify business owner
                            if (freshBiz.getOwnerUUID() != null) {
                                Player owner = Bukkit.getPlayer(freshBiz.getOwnerUUID());
                                if (owner != null) owner.sendMessage("\u00a7aYour business \u00a76" + biz.getName() + "\u00a7a received \u00a7e" + CurrencyUtil.symbol() + String.format("%.2f", amount) + "\u00a7a from the " + nation.getName() + " treasury.");
                            }
                            plugin.getGUIManager().openGUI(new NationPayBusinessesGUI(plugin, nationId, page), clicker);
                        });
                    })
            );
        }

        if (page > 0) {
            int prev = page - 1;
            addButton(45, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7ePrevious Page"))
                    .consumer(e -> plugin.getGUIManager().openGUI(new NationPayBusinessesGUI(plugin, nationId, prev), (Player) e.getWhoClicked()))
            );
        }
        if (end < businesses.size()) {
            int next = page + 1;
            addButton(53, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7eNext Page"))
                    .consumer(e -> plugin.getGUIManager().openGUI(new NationPayBusinessesGUI(plugin, nationId, next), (Player) e.getWhoClicked()))
            );
        }

        addButton(49, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.BARRIER, "\u00a7cBack"))
                .consumer(e -> plugin.getGUIManager().openGUI(new NationPayGUI(plugin, nationId), (Player) e.getWhoClicked()))
        );

        super.decorate(player);
    }

    private void promptPayment(Player player, String bizName, java.util.function.Consumer<Double> onConfirm) {
        NationData nation = plugin.getNationManager().loadNation(nationId);
        double balance = nation != null ? plugin.getNationManager().getTreasuryBalance(nation.getName()) : 0.0;
        plugin.getChatInputManager().requestInput(player,
                "\u00a7eEnter amount to pay \u00a76" + bizName + "\u00a7e (Treasury: \u00a7a" + CurrencyUtil.symbol() + String.format("%.2f", balance) + "\u00a7e):",
                input -> {
                    double amount;
                    try { amount = Double.parseDouble(input); } catch (NumberFormatException ex) {
                        player.sendMessage("\u00a7cInvalid amount.");
                        plugin.getGUIManager().openGUI(new NationPayBusinessesGUI(plugin, nationId, page), player);
                        return;
                    }
                    if (amount <= 0) {
                        player.sendMessage("\u00a7cAmount must be greater than zero.");
                        plugin.getGUIManager().openGUI(new NationPayBusinessesGUI(plugin, nationId, page), player);
                        return;
                    }
                    onConfirm.accept(amount);
                });
    }
}
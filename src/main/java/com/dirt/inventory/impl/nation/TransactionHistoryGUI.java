package com.dirt.inventory.impl.nation;

import com.cryptomorin.xseries.XMaterial;
import com.dirt.DirtEconomy;
import com.dirt.data.NationData;
import com.dirt.data.NationTransaction;
import com.dirt.inventory.InventoryButton;
import com.dirt.inventory.InventoryGUI;
import com.dirt.util.ItemUtil;
import com.dirt.util.CurrencyUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class TransactionHistoryGUI extends InventoryGUI {
    private final DirtEconomy plugin;
    private final UUID nationId;
    private final boolean canWithdraw;
    private final int page;

    public TransactionHistoryGUI(DirtEconomy plugin, UUID nationId, boolean canWithdraw) {
        this(plugin, nationId, canWithdraw, 0);
    }

    public TransactionHistoryGUI(DirtEconomy plugin, UUID nationId, boolean canWithdraw, int page) {
        this.plugin = plugin;
        this.nationId = nationId;
        this.canWithdraw = canWithdraw;
        this.page = page;
    }

    @Override
    protected Inventory createInventory() {
        NationData nation = plugin.getNationManager().loadNation(nationId);
        String name = nation != null ? nation.getColor1() + nation.getName() : "§eNation";
        return Bukkit.createInventory(null, 54, name + " §6Transactions");
    }

    @Override
    public void decorate(Player player) {
        fillPagedGui(54, XMaterial.BLUE_STAINED_GLASS_PANE);

        List<NationTransaction> transactions = plugin.getNationManager().loadTransactions(nationId);
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm");
        int perPage = 36;
        int start = page * perPage;
        int end = Math.min(start + perPage, transactions.size());

        for (int i = start; i < end; i++) {
            NationTransaction tx = transactions.get(i);
            int slot = 9 + (i - start);
            boolean positive = isPositiveTransaction(tx.getType());
            String amountColor = positive ? "§a+" : "§c-";
            addButton(slot, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(positive ? XMaterial.LIME_DYE : XMaterial.RED_DYE,
                            "§e" + formatType(tx.getType()),
                            "§7Amount: " + amountColor + CurrencyUtil.symbol() + String.format("%.2f", tx.getAmount()),
                            "§7" + tx.getDescription(),
                            "§8" + sdf.format(new Date(tx.getTimestamp()))))
                    .consumer(e -> {})
            );
        }

        // Back button
        addButton(49, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.BARRIER, "§cBack to Treasury"))
                .consumer(e -> plugin.getGUIManager().openGUI(new NationTreasuryGUI(plugin, nationId, canWithdraw), (Player) e.getWhoClicked()))
        );

        if (page > 0) {
            int prev = page - 1;
            addButton(45, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "§ePrevious Page"))
                    .consumer(e -> plugin.getGUIManager().openGUI(new TransactionHistoryGUI(plugin, nationId, canWithdraw, prev), (Player) e.getWhoClicked()))
            );
        }
        if (end < transactions.size()) {
            int next = page + 1;
            addButton(53, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "§eNext Page"))
                    .consumer(e -> plugin.getGUIManager().openGUI(new TransactionHistoryGUI(plugin, nationId, canWithdraw, next), (Player) e.getWhoClicked()))
            );
        }

        super.decorate(player);
    }

    private boolean isPositiveTransaction(String type) {
        return switch (type) {
            case "TAX_RECEIVED", "DONATION", "PAYMENT_RECEIVED", "MINTER" -> true;
            default -> false;
        };
    }

    private String formatType(String type) {
        return switch (type) {
            case "TAX_RECEIVED" -> "Tax Received";
            case "WITHDRAWAL" -> "Government Withdrawal";
            case "PAYMENT_SENT" -> "Payment Sent";
            case "PAYMENT_RECEIVED" -> "Payment Received";
            case "DONATION" -> "Donation";
            case "MINTER" -> "Minter Deposit";
            default -> type;
        };
    }
}
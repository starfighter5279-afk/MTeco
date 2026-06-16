package com.mteco.inventory.impl.nation;

import com.cryptomorin.xseries.XMaterial;
import com.mteco.MTeco;
import com.mteco.data.MinterData;
import com.mteco.data.NationData;
import com.mteco.data.NationPermission;
import com.mteco.inventory.InventoryButton;
import com.mteco.inventory.InventoryGUI;
import com.mteco.util.ItemUtil;
import com.mteco.util.NationPermissionUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.UUID;

public class MinterGUI extends InventoryGUI {
    private final MTeco plugin;
    private final UUID minterId;

    public MinterGUI(MTeco plugin, UUID minterId) {
        this.plugin = plugin;
        this.minterId = minterId;
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 27, "\u00a76Minter");
    }

    @Override
    public void decorate(Player player) {
        fillGlass(27, XMaterial.BLUE_STAINED_GLASS_PANE);

        MinterData minter = plugin.getMinterManager().getMinter(minterId);
        if (minter == null) { super.decorate(player); return; }

        NationData nation = plugin.getNationManager().loadNation(minter.getNationId());
        boolean canUse = nation != null && NationPermissionUtil.hasPermission(
                plugin, nation, player.getUniqueId(), NationPermission.USE_MINTERS);
        boolean canManage = nation != null && NationPermissionUtil.hasPermission(
                plugin, nation, player.getUniqueId(), NationPermission.MANAGE_MINTERS);

        // ── Status Display ──
        addButton(4, new InventoryButton()
                .creator(p -> {
                    MinterData m = plugin.getMinterManager().getMinter(minterId);
                    if (m == null) return ItemUtil.buildItem(XMaterial.BARRIER, "\u00a7cMinter Not Found");

                    if (m.getQueue().isEmpty()) {
                        return ItemUtil.buildItem(XMaterial.CAULDRON, "\u00a76\u00a7lMinter Status",
                                "\u00a77Status: \u00a7aIdle",
                                "\u00a77Queue: \u00a7f0 items",
                                "",
                                "\u00a77Deposit gold to begin minting.");
                    }

                    long totalQueueMs = 0;
                    for (String item : m.getQueue()) {
                        totalQueueMs += item.equals("BAR")
                                ? plugin.getSettings().getMinterBarProcessingSeconds() * 1000L
                                : plugin.getSettings().getMinterNuggetProcessingSeconds() * 1000L;
                    }
                    String type = m.getQueue().get(0);
                    long currentItemTotalMs = type.equals("BAR")
                            ? plugin.getSettings().getMinterBarProcessingSeconds() * 1000L
                            : plugin.getSettings().getMinterNuggetProcessingSeconds() * 1000L;
                    long currentItemElapsed = Math.min(currentItemTotalMs, System.currentTimeMillis() - m.getProcessingStartTime());
                    long remainingMs = totalQueueMs - currentItemElapsed;
                    int pct = (int) Math.min(100, ((totalQueueMs - remainingMs) * 100) / Math.max(1, totalQueueMs));

                    int bars = 0, nuggets = 0;
                    for (String item : m.getQueue()) {
                        if (item.equals("BAR")) bars++;
                        else nuggets++;
                    }

                    long remainingSec = remainingMs / 1000;
                    String timeStr = remainingSec >= 60
                            ? (remainingSec / 60) + "m " + (remainingSec % 60) + "s"
                            : remainingSec + "s";

                    String progressBar = buildProgressBar(pct);

                    return ItemUtil.buildItem(XMaterial.CAULDRON, "\u00a76\u00a7lMinter Status",
                            "\u00a77Status: \u00a7eProcessing...",
                            "\u00a77Progress: " + progressBar + " \u00a7f" + pct + "%",
                            "\u00a77Time remaining: \u00a7f" + timeStr,
                            "",
                            "\u00a77Queue: \u00a7f" + m.getQueue().size() + " items",
                            "\u00a77  Gold Bars: \u00a76" + bars,
                            "\u00a77  Gold Nuggets: \u00a7e" + nuggets);
                })
                .consumer(e -> {})
        );

        // ── Deposit Gold Button ──
        if (canUse || canManage) {
            addButton(11, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.GOLD_INGOT, "\u00a7aDeposit Gold",
                            "\u00a77Place gold bars or nuggets",
                            "\u00a77to add them to the minting queue."))
                    .consumer(e -> {
                        Player p = (Player) e.getWhoClicked();
                        plugin.getMinterManager().openDepositInventory(p, minterId);
                    })
            );
        } else {
            addButton(11, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.GRAY_DYE, "\u00a77Deposit Gold",
                            "\u00a7cYou don't have permission",
                            "\u00a7cto use this minter."))
                    .consumer(e -> {})
            );
        }

        // ── Delete Minter Button ──
        if (canManage) {
            addButton(15, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.BARRIER, "\u00a7c\u00a7lDelete Minter",
                            "\u00a77Permanently destroy this minter.",
                            "\u00a77Any items in queue will be lost.",
                            "",
                            "\u00a7cClick to delete."))
                    .consumer(e -> {
                        Player p = (Player) e.getWhoClicked();
                        p.closeInventory();
                        plugin.getMinterManager().deleteMinter(minterId);
                        p.sendMessage("\u00a7cMinter has been destroyed.");
                    })
            );
        }

        // ── Close ──
        addButton(22, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7cClose"))
                .consumer(e -> ((Player) e.getWhoClicked()).closeInventory())
        );

        super.decorate(player);
    }

    private String buildProgressBar(int pct) {
        int filled = pct / 10;
        int empty = 10 - filled;
        StringBuilder bar = new StringBuilder("\u00a7a");
        for (int i = 0; i < filled; i++) bar.append("\u2588");
        bar.append("\u00a77");
        for (int i = 0; i < empty; i++) bar.append("\u2588");
        return bar.toString();
    }
}
package com.dirt.inventory.impl.nation;

import com.cryptomorin.xseries.XMaterial;
import com.dirt.DirtEconomy;
import com.dirt.data.LawBookData;
import com.dirt.data.LegislatedCrimeData;
import com.dirt.inventory.InventoryButton;
import com.dirt.inventory.InventoryGUI;
import com.dirt.util.ItemUtil;
import com.dirt.util.CurrencyUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PendingLawsGUI extends InventoryGUI {
    private final DirtEconomy plugin;
    private final UUID nationId;
    private final int page;

    public PendingLawsGUI(DirtEconomy plugin, UUID nationId, int page) {
        this.plugin = plugin;
        this.nationId = nationId;
        this.page = page;
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 54, "\u00a76Pending Approvals");
    }

    @Override
    public void decorate(Player player) {
        fillPagedGui(54, XMaterial.BLUE_STAINED_GLASS_PANE);

        List<Object> pending = new ArrayList<>();
        pending.addAll(plugin.getLawCrimeManager().getPendingLawBooks(nationId));
        pending.addAll(plugin.getLawCrimeManager().getPendingCrimes(nationId));

        int perPage = 44;
        int start = page * perPage;
        int end = Math.min(start + perPage, pending.size());

        for (int i = start; i < end; i++) {
            Object item = pending.get(i);
            if (item instanceof LawBookData book) {
                final UUID bookId = book.getBookId();
                addButton(i - start, new InventoryButton()
                        .creator(p -> ItemUtil.buildItem(XMaterial.WRITABLE_BOOK, "\u00a7e[Law] " + book.getTitle(),
                                "\u00a77Sections: " + book.getSections().size(),
                                "\u00a7aLeft-click to approve.",
                                "\u00a7cRight-click to deny."))
                        .consumer(e -> {
                            Player p = (Player) e.getWhoClicked();
                            LawBookData b = plugin.getLawCrimeManager().loadLawBook(bookId);
                            if (b == null) return;
                            if (e.isLeftClick()) {
                                b.setApproved(true);
                                b.setPendingApproval(false);
                                plugin.getLawCrimeManager().saveLawBook(b);
                                p.sendMessage("\u00a7aLaw book approved: " + b.getTitle());
                            } else {
                                plugin.getLawCrimeManager().deleteLawBook(bookId);
                                p.sendMessage("\u00a7cLaw book denied and removed.");
                            }
                            plugin.getGUIManager().openGUI(new PendingLawsGUI(plugin, nationId, 0), p);
                        })
                );
            } else if (item instanceof LegislatedCrimeData crime) {
                final UUID crimeId = crime.getCrimeId();
                addButton(i - start, new InventoryButton()
                        .creator(p -> ItemUtil.buildItem(XMaterial.IRON_BARS, "\u00a7c[Crime] " + crime.getName(),
                                "\u00a77Jail: " + crime.getMinJailDays() + "d | Fine: " + CurrencyUtil.symbol() + String.format("%.0f", crime.getFineAmount()),
                                "\u00a7aLeft-click to approve.",
                                "\u00a7cRight-click to deny."))
                        .consumer(e -> {
                            Player p = (Player) e.getWhoClicked();
                            LegislatedCrimeData c = plugin.getLawCrimeManager().loadCrime(crimeId);
                            if (c == null) return;
                            if (e.isLeftClick()) {
                                c.setApproved(true);
                                c.setPendingApproval(false);
                                plugin.getLawCrimeManager().saveCrime(c);
                                p.sendMessage("\u00a7aLegislated crime approved: " + c.getName());
                            } else {
                                plugin.getLawCrimeManager().deleteCrime(crimeId);
                                p.sendMessage("\u00a7cLegislated crime denied and removed.");
                            }
                            plugin.getGUIManager().openGUI(new PendingLawsGUI(plugin, nationId, 0), p);
                        })
                );
            }
        }

        if (page > 0) {
            addButton(45, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7ePrevious"))
                    .consumer(e -> plugin.getGUIManager().openGUI(new PendingLawsGUI(plugin, nationId, page - 1), (Player) e.getWhoClicked()))
            );
        }
        if (end < pending.size()) {
            addButton(53, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7eNext"))
                    .consumer(e -> plugin.getGUIManager().openGUI(new PendingLawsGUI(plugin, nationId, page + 1), (Player) e.getWhoClicked()))
            );
        }

        addButton(49, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7cBack"))
                .consumer(e -> plugin.getGUIManager().openGUI(new LawsGUI(plugin, nationId, true), (Player) e.getWhoClicked()))
        );

        super.decorate(player);
    }
}
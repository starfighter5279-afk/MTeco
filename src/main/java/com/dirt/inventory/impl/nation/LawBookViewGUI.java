package com.dirt.inventory.impl.nation;

import com.cryptomorin.xseries.XMaterial;
import com.dirt.DirtEconomy;
import com.dirt.data.LawBookData;
import com.dirt.data.LegislatedCrimeData;
import com.dirt.inventory.InventoryButton;
import com.dirt.inventory.InventoryGUI;
import com.dirt.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.List;
import java.util.UUID;

public class LawBookViewGUI extends InventoryGUI {
    private final DirtEconomy plugin;
    private final UUID nationId;
    private final UUID bookId;
    private final boolean fromPresident;
    private final int page;

    public LawBookViewGUI(DirtEconomy plugin, UUID nationId, UUID bookId, boolean fromPresident, int page) {
        this.plugin = plugin;
        this.nationId = nationId;
        this.bookId = bookId;
        this.fromPresident = fromPresident;
        this.page = page;
    }

    @Override
    protected Inventory createInventory() {
        LawBookData book = plugin.getLawCrimeManager().loadLawBook(bookId);
        String title = book != null ? book.getTitle() : "Law Book";
        return Bukkit.createInventory(null, 54, "\u00a7e" + title);
    }

    @Override
    public void decorate(Player player) {
        fillPagedGui(54, XMaterial.RED_STAINED_GLASS_PANE);

        LawBookData book = plugin.getLawCrimeManager().loadLawBook(bookId);
        if (book == null) { super.decorate(player); return; }

        List<String> sections = book.getSections();
        int perPage = 44;
        int start = page * perPage;
        int end = Math.min(start + perPage, sections.size());

        for (int i = start; i < end; i++) {
            String section = sections.get(i);
            String preview = section.length() > 40 ? section.substring(0, 40) + "..." : section;
            final int sectionIdx = i;

            addButton(i - start, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.PAPER, "\u00a7fSection " + (sectionIdx + 1),
                            "\u00a77" + preview,
                            "\u00a7eClick to link to a legislated crime."))
                    .consumer(e -> {
                        Player p = (Player) e.getWhoClicked();
                        List<LegislatedCrimeData> crimes = plugin.getLawCrimeManager().getCrimesByNation(nationId, true);
                        if (crimes.isEmpty()) {
                            p.sendMessage("\u00a7cNo legislated crimes exist to link to.");
                            return;
                        }
                        plugin.getGUIManager().openGUI(new LegislatedCrimesGUI(plugin, nationId, fromPresident, 0, linkBookId -> {
                            LegislatedCrimeData crime = plugin.getLawCrimeManager().loadCrime(linkBookId);
                            if (crime != null) {
                                String link = bookId + ":" + sectionIdx;
                                if (!crime.getLinkedLawSections().contains(link)) {
                                    crime.getLinkedLawSections().add(link);
                                    plugin.getLawCrimeManager().saveCrime(crime);
                                    p.sendMessage("\u00a7aSection linked to crime: \u00a7e" + crime.getName());
                                } else {
                                    p.sendMessage("\u00a7cSection already linked to that crime.");
                                }
                            }
                            plugin.getGUIManager().openGUI(new LawBookViewGUI(plugin, nationId, bookId, fromPresident, page), p);
                        }), p);
                    })
            );
        }

        if (page > 0) {
            int prev = page - 1;
            addButton(45, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7ePrevious"))
                    .consumer(e -> plugin.getGUIManager().openGUI(new LawBookViewGUI(plugin, nationId, bookId, fromPresident, prev), (Player) e.getWhoClicked()))
            );
        }
        if (end < sections.size()) {
            int next = page + 1;
            addButton(53, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7eNext"))
                    .consumer(e -> plugin.getGUIManager().openGUI(new LawBookViewGUI(plugin, nationId, bookId, fromPresident, next), (Player) e.getWhoClicked()))
            );
        }

        boolean fp = fromPresident;
        addButton(49, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7cBack"))
                .consumer(e -> plugin.getGUIManager().openGUI(new CurrentLawsGUI(plugin, nationId, fp, 0), (Player) e.getWhoClicked()))
        );

        super.decorate(player);
    }
}
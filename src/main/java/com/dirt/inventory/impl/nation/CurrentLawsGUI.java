package com.dirt.inventory.impl.nation;

import com.cryptomorin.xseries.XMaterial;
import com.dirt.DirtEconomy;
import com.dirt.data.CharacterData;
import com.dirt.data.LawBookData;
import com.dirt.inventory.InventoryButton;
import com.dirt.inventory.InventoryGUI;
import com.dirt.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.List;
import java.util.UUID;

public class CurrentLawsGUI extends InventoryGUI {
    private final DirtEconomy plugin;
    private final UUID nationId;
    private final boolean fromPresident;
    private final int page;

    public CurrentLawsGUI(DirtEconomy plugin, UUID nationId, boolean fromPresident, int page) {
        this.plugin = plugin;
        this.nationId = nationId;
        this.fromPresident = fromPresident;
        this.page = page;
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 54, "\u00a7eCurrent Laws");
    }

    @Override
    public void decorate(Player player) {
        fillPagedGui(54, XMaterial.RED_STAINED_GLASS_PANE);

        List<LawBookData> books = plugin.getLawCrimeManager().getLawBooksByNation(nationId, true);
        int perPage = 44;
        int start = page * perPage;
        int end = Math.min(start + perPage, books.size());

        for (int i = start; i < end; i++) {
            LawBookData book = books.get(i);
            CharacterData author = plugin.getCharacterManager().getCharacter(book.getAuthorUUID());
            String authorName = author != null ? author.getFirstName() + " " + author.getLastName() : "Unknown";
            final UUID bookId = book.getBookId();

            addButton(i - start, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.WRITTEN_BOOK, "\u00a7e" + book.getTitle(),
                            "\u00a77Author: \u00a7f" + authorName,
                            "\u00a77Sections: \u00a7f" + book.getSections().size(),
                            "\u00a77Click to view sections."))
                    .consumer(e -> plugin.getGUIManager().openGUI(
                            new LawBookViewGUI(plugin, nationId, bookId, fromPresident, 0),
                            (Player) e.getWhoClicked()))
            );
        }

        if (page > 0) {
            int prev = page - 1;
            addButton(45, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7ePrevious"))
                    .consumer(e -> plugin.getGUIManager().openGUI(new CurrentLawsGUI(plugin, nationId, fromPresident, prev), (Player) e.getWhoClicked()))
            );
        }
        if (end < books.size()) {
            int next = page + 1;
            addButton(53, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7eNext"))
                    .consumer(e -> plugin.getGUIManager().openGUI(new CurrentLawsGUI(plugin, nationId, fromPresident, next), (Player) e.getWhoClicked()))
            );
        }

        boolean fp = fromPresident;
        addButton(49, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7cBack"))
                .consumer(e -> plugin.getGUIManager().openGUI(new LawsGUI(plugin, nationId, fp), (Player) e.getWhoClicked()))
        );

        super.decorate(player);
    }
}
package com.mteco.inventory.impl.contract;

import com.cryptomorin.xseries.XMaterial;
import com.mteco.MTeco;
import com.mteco.data.ContractData;
import com.mteco.inventory.InventoryButton;
import com.mteco.inventory.InventoryGUI;
import com.mteco.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class ContractViewBodyGUI extends InventoryGUI {
    private static final int LINES_PER_PAGE = 14;
    private final MTeco plugin;
    private final ContractData draft;
    private final int page;
    private final List<List<String>> pages;

    public ContractViewBodyGUI(MTeco plugin, ContractData draft, int page) {
        this.plugin = plugin;
        this.draft = draft;
        this.pages = paginateBody(draft.getBody());
        this.page = Math.max(0, Math.min(page, Math.max(0, pages.size() - 1)));
    }

    @Override
    protected Inventory createInventory() {
        int totalPages = Math.max(1, pages.size());
        return Bukkit.createInventory(null, 27,
                "\u00a78Contract Body \u00a77(Page " + (page + 1) + "/" + totalPages + ")");
    }

    @Override
    public void decorate(Player player) {
        fillGlass(27, XMaterial.YELLOW_STAINED_GLASS_PANE);

        addButton(13, new InventoryButton()
                .creator(p -> {
                    ItemStack book = XMaterial.matchXMaterial("BOOK").map(XMaterial::parseItem)
                            .orElse(new ItemStack(org.bukkit.Material.PAPER));
                    ItemMeta meta = book.getItemMeta();
                    if (meta != null) {
                        meta.setDisplayName("\u00a7e\u00a7lPage " + (page + 1));
                        List<String> lore = new ArrayList<>();
                        if (pages.isEmpty()) {
                            lore.add("\u00a77(empty)");
                        } else {
                            for (String line : pages.get(page)) {
                                lore.add("\u00a7f" + ChatColor.translateAlternateColorCodes('&', line));
                            }
                        }
                        meta.setLore(lore);
                        book.setItemMeta(meta);
                    }
                    return book;
                })
                .consumer(e -> {})
        );

        if (page > 0) {
            addButton(18, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7a\u2190 Previous Page"))
                    .consumer(e -> {
                        Player clicker = (Player) e.getWhoClicked();
                        plugin.getGUIManager().openGUI(new ContractViewBodyGUI(plugin, draft, page - 1), clicker);
                    })
            );
        }

        if (page < pages.size() - 1) {
            addButton(26, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7aNext Page \u2192"))
                    .consumer(e -> {
                        Player clicker = (Player) e.getWhoClicked();
                        plugin.getGUIManager().openGUI(new ContractViewBodyGUI(plugin, draft, page + 1), clicker);
                    })
            );
        }

        addButton(22, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.BARRIER, "\u00a7cBack"))
                .consumer(e -> {
                    Player clicker = (Player) e.getWhoClicked();
                    plugin.getGUIManager().openGUI(new ContractEditBodyOptionsGUI(plugin, draft), clicker);
                })
        );

        super.decorate(player);
    }

    private static List<List<String>> paginateBody(String body) {
        List<List<String>> pages = new ArrayList<>();
        if (body == null || body.isEmpty()) return pages;

        String[] lines = body.split("\\|");
        List<String> currentPage = new ArrayList<>();
        for (String line : lines) {
            currentPage.add(line);
            if (currentPage.size() >= LINES_PER_PAGE) {
                pages.add(currentPage);
                currentPage = new ArrayList<>();
            }
        }
        if (!currentPage.isEmpty()) {
            pages.add(currentPage);
        }
        return pages;
    }
}
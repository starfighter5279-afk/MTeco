package com.mteco.inventory.impl.nation;

import com.cryptomorin.xseries.XMaterial;
import com.mteco.MTeco;
import com.mteco.data.LawBookData;
import com.mteco.data.NationData;
import com.mteco.inventory.InventoryButton;
import com.mteco.inventory.InventoryGUI;
import com.mteco.util.ItemUtil;
import com.mteco.util.LText;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

public class LawsGUI extends InventoryGUI {
    private final MTeco plugin;
    private final UUID nationId;
    private final boolean fromPresident;

    public LawsGUI(MTeco plugin, UUID nationId, boolean fromPresident) {
        this.plugin = plugin;
        this.nationId = nationId;
        this.fromPresident = fromPresident;
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 27, "\u00a7eLaws Management");
    }

    @Override
    public void decorate(Player player) {
        fillGlass(27, XMaterial.RED_STAINED_GLASS_PANE);

        boolean fp = fromPresident;

        addButton(10, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.BOOKSHELF, "\u00a7eCurrent Laws",
                        "\u00a77View all published law books."))
                .consumer(e -> plugin.getGUIManager().openGUI(new CurrentLawsGUI(plugin, nationId, fp, 0), (Player) e.getWhoClicked()))
        );

        addButton(12, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.IRON_BARS, "\u00a7eLegislated Crimes",
                        "\u00a77Create and manage legislated crimes."))
                .consumer(e -> plugin.getGUIManager().openGUI(new LegislatedCrimesGUI(plugin, nationId, fp, 0), (Player) e.getWhoClicked()))
        );

        addButton(14, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.WRITABLE_BOOK, "\u00a7aCreate New Law Book",
                        "\u00a77Write a new law book for your nation.",
                        "\u00a77You can paste a link (PDF, TXT, etc.)."))
                .consumer(e -> {
                    Player p = (Player) e.getWhoClicked();
                    p.closeInventory();
                    NationData nation = plugin.getNationManager().loadNation(nationId);
                    boolean isPresident = nation != null && p.getUniqueId().equals(nation.getPresidentUUID());

                    plugin.getChatInputManager().requestInput(p, "\u00a7eEnter a title for the law book:", title -> {
                        if (title.isEmpty()) { p.sendMessage("\u00a7cTitle cannot be empty."); return; }
                        plugin.getChatInputManager().requestInput(p,
                                "\u00a7ePaste a link to a document (PDF, TXT, etc.) or type your law text.\n\u00a77Use \u00a7b|\u00a77 for page breaks. Section headers like \u00a7bSEC.1\u00a77 are auto-detected.",
                                rawInput -> {
                            LText.handleInput(plugin, p, rawInput, text -> {
                                if (text.isEmpty()) {
                                    p.sendMessage("\u00a7cNo content provided. Law book creation cancelled.");
                                    return;
                                }
                                List<String> sections = new ArrayList<>();
                                String normalized = text.replace("\r\n", "\n");
                                String[] rawPages = normalized.split("\\||\n\n");
                                for (String page : rawPages) {
                                    String trimmed = page.trim();
                                    if (!trimmed.isEmpty()) {
                                        List<String> parsed = parseSections(trimmed);
                                        sections.addAll(parsed.isEmpty() ? List.of(trimmed) : parsed);
                                    }
                                }
                                if (sections.isEmpty()) {
                                    sections.add(text.trim());
                                }
                                boolean approved = isPresident;
                                plugin.getLawCrimeManager().createLawBook(nationId, p.getUniqueId(), title, sections, approved);
                                if (approved) {
                                    p.sendMessage("\u00a7aLaw book '\u00a7e" + title + "\u00a7a' published with " + sections.size() + " page(s).");
                                } else {
                                    p.sendMessage("\u00a7eLaw book '\u00a7f" + title + "\u00a7e' submitted for presidential approval.");
                                    if (nation != null && nation.getPresidentUUID() != null) {
                                        Player pres = Bukkit.getPlayer(nation.getPresidentUUID());
                                        if (pres != null) pres.sendMessage("\u00a7eA new law book is pending your approval.");
                                    }
                                }
                            });
                        });
                    });
                })
        );

        List<LawBookData> pending = plugin.getLawCrimeManager().getPendingLawBooks(nationId);
        int pendingCount = pending.size() + plugin.getLawCrimeManager().getPendingCrimes(nationId).size();
        if (fp && pendingCount > 0) {
            addButton(16, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.BELL, "\u00a76Pending Approvals",
                            "\u00a77" + pendingCount + " item(s) awaiting approval.",
                            "\u00a77Click to review."))
                    .consumer(e -> plugin.getGUIManager().openGUI(new PendingLawsGUI(plugin, nationId, 0), (Player) e.getWhoClicked()))
            );
        }

        addButton(22, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7cBack"))
                .consumer(e -> plugin.getGUIManager().openGUI(new LawsAndCrimeMenuGUI(plugin, nationId, fp), (Player) e.getWhoClicked()))
        );

        super.decorate(player);
    }

    private static List<String> parseSections(String rawText) {
        List<String> sections = new ArrayList<>();
        String[] parts = rawText.split("(?=SEC\\.\\d+)");
        for (String part : parts) {
            part = part.trim();
            if (part.isEmpty()) continue;
            if (part.matches("(?i)SEC\\.\\d+.*")) {
                int spaceIdx = part.indexOf(' ');
                if (spaceIdx > 0) {
                    String header = part.substring(0, spaceIdx);
                    String body = part.substring(spaceIdx + 1).trim();
                    sections.add("\u00a7l" + header + "\u00a7r " + body);
                } else {
                    sections.add("\u00a7l" + part + "\u00a7r");
                }
            } else {
                sections.add(part);
            }
        }
        return sections;
    }
}
package com.mteco.inventory.impl.nation;

import com.cryptomorin.xseries.XMaterial;
import com.mteco.MTeco;
import com.mteco.data.LawBookData;
import com.mteco.data.LegislatedCrimeData;
import com.mteco.inventory.InventoryButton;
import com.mteco.inventory.InventoryGUI;
import com.mteco.util.ItemUtil;
import com.mteco.util.CurrencyUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class LegislatedCrimeDetailGUI extends InventoryGUI {
    private final MTeco plugin;
    private final UUID nationId;
    private final UUID crimeId;
    private final boolean fromPresident;

    public LegislatedCrimeDetailGUI(MTeco plugin, UUID nationId, UUID crimeId, boolean fromPresident) {
        this.plugin = plugin;
        this.nationId = nationId;
        this.crimeId = crimeId;
        this.fromPresident = fromPresident;
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 27, "\u00a7eCrime Details");
    }

    @Override
    public void decorate(Player player) {
        fillGlass(27, XMaterial.RED_STAINED_GLASS_PANE);

        LegislatedCrimeData crime = plugin.getLawCrimeManager().loadCrime(crimeId);
        if (crime == null) { super.decorate(player); return; }

        List<String> infoLore = new ArrayList<>();
        infoLore.add("\u00a77Min Jail: \u00a7f" + crime.getMinJailDays() + " MC day(s)");
        infoLore.add("\u00a77Fine: \u00a7a" + CurrencyUtil.symbol() + String.format("%.2f", crime.getFineAmount()));
        for (String link : crime.getLinkedLawSections()) {
            String[] parts = link.split(":");
            if (parts.length == 2) {
                try {
                    LawBookData book = plugin.getLawCrimeManager().loadLawBook(UUID.fromString(parts[0]));
                    int secIdx = Integer.parseInt(parts[1]);
                    if (book != null && secIdx < book.getSections().size()) {
                        String preview = book.getSections().get(secIdx);
                        if (preview.length() > 35) preview = preview.substring(0, 35) + "...";
                        infoLore.add("\u00a78" + book.getTitle() + " S." + (secIdx + 1));
                    }
                } catch (Exception ignored) {}
            }
        }

        addButton(4, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.IRON_BARS, "\u00a7c" + crime.getName(),
                        infoLore.toArray(new String[0])))
                .consumer(e -> {})
        );

        addButton(11, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.CLOCK, "\u00a7eEdit Jail Time",
                        "\u00a77Current: \u00a7f" + crime.getMinJailDays() + " day(s)"))
                .consumer(e -> {
                    Player p = (Player) e.getWhoClicked();
                    p.closeInventory();
                    plugin.getChatInputManager().requestInput(p, "\u00a7eEnter new minimum jail time (MC days):", input -> {
                        int days;
                        try { days = Integer.parseInt(input); } catch (NumberFormatException ex) {
                            p.sendMessage("\u00a7cInvalid number."); return;
                        }
                        LegislatedCrimeData c = plugin.getLawCrimeManager().loadCrime(crimeId);
                        if (c != null) { c.setMinJailDays(days); plugin.getLawCrimeManager().saveCrime(c); }
                        p.sendMessage("\u00a7aJail time updated to " + days + " day(s).");
                        plugin.getGUIManager().openGUI(new LegislatedCrimeDetailGUI(plugin, nationId, crimeId, fromPresident), p);
                    });
                })
        );

        addButton(13, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.GOLD_INGOT, "\u00a7eEdit Fine",
                        "\u00a77Current: \u00a7a" + CurrencyUtil.symbol() + String.format("%.2f", crime.getFineAmount())))
                .consumer(e -> {
                    Player p = (Player) e.getWhoClicked();
                    p.closeInventory();
                    plugin.getChatInputManager().requestInput(p, "\u00a7eEnter new fine amount:", input -> {
                        double fine;
                        try { fine = Double.parseDouble(input); } catch (NumberFormatException ex) {
                            p.sendMessage("\u00a7cInvalid amount."); return;
                        }
                        LegislatedCrimeData c = plugin.getLawCrimeManager().loadCrime(crimeId);
                        if (c != null) { c.setFineAmount(fine); plugin.getLawCrimeManager().saveCrime(c); }
                        p.sendMessage("\u00a7aFine updated to " + CurrencyUtil.symbol() + String.format("%.2f", fine));
                        plugin.getGUIManager().openGUI(new LegislatedCrimeDetailGUI(plugin, nationId, crimeId, fromPresident), p);
                    });
                })
        );

        addButton(15, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.REDSTONE, "\u00a7cDelete Crime"))
                .consumer(e -> {
                    plugin.getLawCrimeManager().deleteCrime(crimeId);
                    Player p = (Player) e.getWhoClicked();
                    p.sendMessage("\u00a7cLegislated crime deleted.");
                    plugin.getGUIManager().openGUI(new LegislatedCrimesGUI(plugin, nationId, fromPresident, 0), p);
                })
        );

        boolean fp = fromPresident;
        addButton(22, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7cBack"))
                .consumer(e -> plugin.getGUIManager().openGUI(new LegislatedCrimesGUI(plugin, nationId, fp, 0), (Player) e.getWhoClicked()))
        );

        super.decorate(player);
    }
}
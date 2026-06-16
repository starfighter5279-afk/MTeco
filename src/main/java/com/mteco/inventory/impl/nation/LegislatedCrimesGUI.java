package com.mteco.inventory.impl.nation;

import com.cryptomorin.xseries.XMaterial;
import com.mteco.MTeco;
import com.mteco.data.LawBookData;
import com.mteco.data.LegislatedCrimeData;
import com.mteco.data.NationData;
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
import java.util.function.Consumer;

public class LegislatedCrimesGUI extends InventoryGUI {
    private final MTeco plugin;
    private final UUID nationId;
    private final boolean fromPresident;
    private final int page;
    private final Consumer<UUID> onSelect;

    public LegislatedCrimesGUI(MTeco plugin, UUID nationId, boolean fromPresident, int page) {
        this(plugin, nationId, fromPresident, page, null);
    }

    public LegislatedCrimesGUI(MTeco plugin, UUID nationId, boolean fromPresident, int page, Consumer<UUID> onSelect) {
        this.plugin = plugin;
        this.nationId = nationId;
        this.fromPresident = fromPresident;
        this.page = page;
        this.onSelect = onSelect;
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 54, "\u00a7eLegislated Crimes");
    }

    @Override
    public void decorate(Player player) {
        fillPagedGui(54, XMaterial.RED_STAINED_GLASS_PANE);

        List<LegislatedCrimeData> crimes = plugin.getLawCrimeManager().getCrimesByNation(nationId, true);
        int perPage = 44;
        int start = page * perPage;
        int end = Math.min(start + perPage, crimes.size());
        boolean fp = fromPresident;

        for (int i = start; i < end; i++) {
            LegislatedCrimeData crime = crimes.get(i);
            final UUID crimeId = crime.getCrimeId();

            List<String> lore = new ArrayList<>();
            lore.add("\u00a77Min Jail: \u00a7f" + crime.getMinJailDays() + " day(s)");
            lore.add("\u00a77Fine: \u00a7a" + CurrencyUtil.symbol() + String.format("%.2f", crime.getFineAmount()));
            for (String link : crime.getLinkedLawSections()) {
                String[] parts = link.split(":");
                if (parts.length == 2) {
                    LawBookData book = plugin.getLawCrimeManager().loadLawBook(UUID.fromString(parts[0]));
                    if (book != null) {
                        int secIdx = Integer.parseInt(parts[1]);
                        if (secIdx < book.getSections().size()) {
                            String preview = book.getSections().get(secIdx);
                            if (preview.length() > 30) preview = preview.substring(0, 30) + "...";
                            lore.add("\u00a78\u00a7o" + book.getTitle() + " S." + (secIdx + 1) + ": " + preview);
                        }
                    }
                }
            }
            if (onSelect != null) {
                lore.add("\u00a7eClick to select.");
            } else {
                lore.add("\u00a77Click to manage.");
            }

            addButton(i - start, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.IRON_BARS, "\u00a7c" + crime.getName(),
                            lore.toArray(new String[0])))
                    .consumer(e -> {
                        Player p = (Player) e.getWhoClicked();
                        if (onSelect != null) {
                            p.closeInventory();
                            plugin.getServer().getScheduler().runTaskLater(plugin, () -> onSelect.accept(crimeId), 1L);
                        } else {
                            plugin.getGUIManager().openGUI(new LegislatedCrimeDetailGUI(plugin, nationId, crimeId, fp), p);
                        }
                    })
            );
        }

        if (page > 0) {
            int prev = page - 1;
            addButton(45, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7ePrevious"))
                    .consumer(e -> plugin.getGUIManager().openGUI(new LegislatedCrimesGUI(plugin, nationId, fp, prev, onSelect), (Player) e.getWhoClicked()))
            );
        }
        if (end < crimes.size()) {
            int next = page + 1;
            addButton(53, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7eNext"))
                    .consumer(e -> plugin.getGUIManager().openGUI(new LegislatedCrimesGUI(plugin, nationId, fp, next, onSelect), (Player) e.getWhoClicked()))
            );
        }

        if (onSelect == null) {
            NationData nation = plugin.getNationManager().loadNation(nationId);
            boolean isPresident = nation != null && player.getUniqueId().equals(nation.getPresidentUUID());

            addButton(48, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.EMERALD, "\u00a7aCreate Legislated Crime",
                            "\u00a77Define a new criminal offense."))
                    .consumer(e -> {
                        Player p = (Player) e.getWhoClicked();
                        p.closeInventory();
                        plugin.getChatInputManager().requestInput(p, "\u00a7eEnter the name of the crime:", crimeName -> {
                            if (crimeName.isEmpty()) { p.sendMessage("\u00a7cName cannot be empty."); return; }
                            plugin.getChatInputManager().requestInput(p, "\u00a7eEnter minimum jail time (in MC days, 1 day = 20min):", jailInput -> {
                                int jailDays;
                                try { jailDays = Integer.parseInt(jailInput); } catch (NumberFormatException ex) {
                                    p.sendMessage("\u00a7cInvalid number."); return;
                                }
                                plugin.getChatInputManager().requestInput(p, "\u00a7eEnter fine amount (in vault units):", fineInput -> {
                                    double fine;
                                    try { fine = Double.parseDouble(fineInput); } catch (NumberFormatException ex) {
                                        p.sendMessage("\u00a7cInvalid amount."); return;
                                    }
                                    boolean approved = isPresident;
                                    plugin.getLawCrimeManager().createCrime(nationId, crimeName, jailDays, fine, approved);
                                    if (approved) {
                                        p.sendMessage("\u00a7aLegislated crime '\u00a7e" + crimeName + "\u00a7a' created.");
                                    } else {
                                        p.sendMessage("\u00a7eLegislated crime submitted for presidential approval.");
                                        if (nation != null && nation.getPresidentUUID() != null) {
                                            Player pres = Bukkit.getPlayer(nation.getPresidentUUID());
                                            if (pres != null) pres.sendMessage("\u00a7eA new legislated crime is pending your approval.");
                                        }
                                    }
                                });
                            });
                        });
                    })
            );
        }

        addButton(49, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7cBack"))
                .consumer(e -> plugin.getGUIManager().openGUI(new LawsGUI(plugin, nationId, fp), (Player) e.getWhoClicked()))
        );

        super.decorate(player);
    }
}
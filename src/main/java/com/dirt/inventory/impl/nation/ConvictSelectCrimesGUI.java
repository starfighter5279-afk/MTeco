package com.dirt.inventory.impl.nation;

import com.cryptomorin.xseries.XMaterial;
import com.dirt.DirtEconomy;
import com.dirt.data.LegislatedCrimeData;
import com.dirt.inventory.InventoryButton;
import com.dirt.inventory.InventoryGUI;
import com.dirt.util.ItemUtil;
import com.dirt.util.CurrencyUtil;
import com.dirt.util.LText;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

public class ConvictSelectCrimesGUI extends InventoryGUI {
    private final DirtEconomy plugin;
    private final UUID nationId;
    private final UUID criminalUUID;
    private final Supplier<InventoryGUI> backSupplier;
    private final Set<UUID> selectedCrimes;

    public ConvictSelectCrimesGUI(DirtEconomy plugin, UUID nationId, UUID criminalUUID, Supplier<InventoryGUI> backSupplier) {
        this(plugin, nationId, criminalUUID, backSupplier, new HashSet<>());
    }

    public ConvictSelectCrimesGUI(DirtEconomy plugin, UUID nationId, UUID criminalUUID, Supplier<InventoryGUI> backSupplier, Set<UUID> selectedCrimes) {
        this.plugin = plugin;
        this.nationId = nationId;
        this.criminalUUID = criminalUUID;
        this.backSupplier = backSupplier;
        this.selectedCrimes = selectedCrimes;
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 54, "\u00a7eSelect Crimes (max 3)");
    }

    @Override
    public void decorate(Player player) {
        fillPagedGui(54, XMaterial.RED_STAINED_GLASS_PANE);

        List<LegislatedCrimeData> crimes = plugin.getLawCrimeManager().getCrimesByNation(nationId, true);
        int end = Math.min(44, crimes.size());

        for (int i = 0; i < end; i++) {
            LegislatedCrimeData crime = crimes.get(i);
            final UUID crimeId = crime.getCrimeId();
            boolean selected = selectedCrimes.contains(crimeId);

            XMaterial mat = selected ? XMaterial.LIME_WOOL : XMaterial.RED_WOOL;
            String status = selected ? "\u00a7a\u2714 Selected" : "\u00a77Click to select";

            addButton(i, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(mat, "\u00a7c" + crime.getName(),
                            "\u00a77Jail: " + crime.getMinJailDays() + "d | Fine: " + CurrencyUtil.symbol() + String.format("%.0f", crime.getFineAmount()),
                            status))
                    .consumer(e -> {
                        Set<UUID> newSelected = new HashSet<>(selectedCrimes);
                        if (newSelected.contains(crimeId)) {
                            newSelected.remove(crimeId);
                        } else if (newSelected.size() < 3) {
                            newSelected.add(crimeId);
                        } else {
                            ((Player) e.getWhoClicked()).sendMessage("\u00a7cMaximum 3 crimes can be selected.");
                            return;
                        }
                        plugin.getGUIManager().openGUI(new ConvictSelectCrimesGUI(plugin, nationId, criminalUUID, backSupplier, newSelected), (Player) e.getWhoClicked());
                    })
            );
        }

        if (!selectedCrimes.isEmpty()) {
            addButton(48, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.EMERALD, "\u00a7aConfirm (" + selectedCrimes.size() + " selected)",
                            "\u00a77Click to confirm and enter description."))
                    .consumer(e -> {
                        Player p = (Player) e.getWhoClicked();
                        p.closeInventory();
                        plugin.getChatInputManager().requestInput(p,
                                "\u00a7eDescribe how and when the crimes were committed, and who the victims are.\n\u00a77You can also paste a link to a document (PDF, TXT, etc.).", rawInput -> {
                            LText.handleInput(plugin, p, rawInput, description -> {
                                List<UUID> crimeIds = new ArrayList<>(selectedCrimes);
                                plugin.getLawCrimeManager().createCriminalRecord(nationId, criminalUUID, crimeIds, description);
                                p.sendMessage("\u00a7aCriminal convicted and record created.");
                                Player criminal = Bukkit.getPlayer(criminalUUID);
                                if (criminal != null) {
                                    criminal.sendMessage("\u00a7cYou have been convicted of criminal offenses by your nation.");
                                }
                            });
                        });
                    })
            );
        }

        addButton(49, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7cBack"))
                .consumer(e -> plugin.getGUIManager().openGUI(new CrimeManagementGUI(plugin, nationId, backSupplier), (Player) e.getWhoClicked()))
        );

        super.decorate(player);
    }
}
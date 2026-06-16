package com.mteco.inventory.impl;

import com.cryptomorin.xseries.XMaterial;
import com.mteco.MTeco;
import com.mteco.data.CharacterData;
import com.mteco.inventory.InventoryButton;
import com.mteco.inventory.InventoryGUI;
import com.mteco.util.CurrencyUtil;
import com.mteco.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class SpouseSelectionGUI extends InventoryGUI {
    private final MTeco plugin;
    private final int page;

    public SpouseSelectionGUI(MTeco plugin, int page) {
        this.plugin = plugin;
        this.page = page;
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 54, "\u00a7dSelect Spouse");
    }

    @Override
    public void decorate(Player player) {
        CharacterData myData = plugin.getCharacterManager().getCharacter(player.getUniqueId());
        String myGender = myData != null ? myData.getGender() : "MALE";
        String oppositeGender = "MALE".equals(myGender) ? "FEMALE" : "MALE";

        List<CharacterData> candidates = plugin.getCharacterManager().getAllCharacters().stream()
                .filter(c -> !c.getPlayerUuid().equals(player.getUniqueId()))
                .filter(c -> oppositeGender.equals(c.getGender()))
                .filter(c -> c.getFamilyId() == null || "CHILD".equals(c.getFamilyRole()))
                .collect(Collectors.toList());

        fillPagedGui(54, XMaterial.PINK_STAINED_GLASS_PANE);

        int perPage = 45;
        int start = page * perPage;
        int end = Math.min(start + perPage, candidates.size());

        for (int i = start; i < end; i++) {
            CharacterData candidate = candidates.get(i);
            int slot = i - start;
            addButton(slot, new InventoryButton()
                    .creator(p -> {
                        String fullName = candidate.getFirstName() + " " + candidate.getMiddleName() + " " + candidate.getLastName();
                        String dateStr = new SimpleDateFormat("MM/dd/yyyy").format(new Date(candidate.getBirthDate()));
                        double balance = plugin.getEconomy().getBalance(Bukkit.getOfflinePlayer(candidate.getPlayerUuid()));
                        return ItemUtil.buildItem(XMaterial.PLAYER_HEAD, "\u00a7e" + fullName,
                                "\u00a77Gender: \u00a7f" + candidate.getGender(),
                                "\u00a77Born: \u00a7f" + dateStr,
                                "\u00a77Balance: \u00a7e" + CurrencyUtil.symbol() + String.format("%.0f", balance),
                                "\u00a77Click to send a marriage request");
                    })
                    .consumer(e -> plugin.getGUIManager().openGUI(
                            new MarriageConfirmGUI(plugin, candidate.getPlayerUuid()),
                            (Player) e.getWhoClicked()))
            );
        }

        if (page > 0) {
            int prevPage = page - 1;
            addButton(45, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7ePrevious Page"))
                    .consumer(e -> plugin.getGUIManager().openGUI(new SpouseSelectionGUI(plugin, prevPage), (Player) e.getWhoClicked()))
            );
        }

        if (end < candidates.size()) {
            int nextPage = page + 1;
            addButton(53, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7eNext Page"))
                    .consumer(e -> plugin.getGUIManager().openGUI(new SpouseSelectionGUI(plugin, nextPage), (Player) e.getWhoClicked()))
            );
        }

        addButton(49, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.BARRIER, "\u00a7cBack"))
                .consumer(e -> plugin.getGUIManager().openGUI(new FamilyGUI(plugin), (Player) e.getWhoClicked()))
        );

        super.decorate(player);
    }
}
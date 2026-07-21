package com.dirt.inventory.impl;

import com.cryptomorin.xseries.XMaterial;
import com.dirt.DirtEconomy;
import com.dirt.data.CharacterData;
import com.dirt.inventory.InventoryButton;
import com.dirt.inventory.InventoryGUI;
import com.dirt.util.CurrencyUtil;
import com.dirt.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class NameConfirmGUI extends InventoryGUI {
    private final DirtEconomy plugin;

    public NameConfirmGUI(DirtEconomy plugin) {
        this.plugin = plugin;
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 27, "\u00a76Confirm Name Change");
    }

    @Override
    public void decorate(Player player) {
        String[] state = plugin.getCharacterManager().getPendingNameChange(player.getUniqueId());

        fillGlass(27, XMaterial.ORANGE_STAINED_GLASS_PANE);

        String newName = state != null ? state[0] + " " + state[1] + " " + state[2] : "Unknown";
        double nameChangeCost = plugin.getSettings().getNameChangeCost();
        addButton(4, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.GOLD_INGOT, "\u00a76Name Change Confirmation",
                        "\u00a77New name: \u00a7f" + newName,
                        "\u00a77Cost: \u00a7e" + CurrencyUtil.symbol() + String.format("%.0f", nameChangeCost),
                        "\u00a77Are you sure you want to proceed?"))
                .consumer(e -> {})
        );

        addButton(11, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.LIME_WOOL, "\u00a7aYes, change my name"))
                .consumer(e -> {
                    Player p = (Player) e.getWhoClicked();
                    if (state == null) { p.closeInventory(); return; }
                    if (plugin.getEconomy().getBalance(p) < nameChangeCost) {
                        p.sendMessage("\u00a7cYou no longer have sufficient funds.");
                        p.closeInventory();
                        return;
                    }
                    plugin.getEconomy().withdrawPlayer(p, nameChangeCost);
                    CharacterData data = plugin.getCharacterManager().getCharacter(p.getUniqueId());
                    if (data != null) {
                        data.setFirstName(state[0]);
                        data.setMiddleName(state[1]);
                        data.setLastName(state[2]);
                        plugin.getCharacterManager().saveCharacter(data);
                    }
                    plugin.getCharacterManager().clearPendingNameChange(p.getUniqueId());
                    p.sendMessage("\u00a7aYour name has been changed to \u00a7e" + state[0] + " " + state[1] + " " + state[2] + "\u00a7a for \u00a7e" + CurrencyUtil.symbol() + String.format("%.0f", nameChangeCost) + "\u00a7a.");
                    p.closeInventory();
                })
        );

        addButton(15, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.RED_WOOL, "\u00a7cNo, go back"))
                .consumer(e -> plugin.getGUIManager().openGUI(new NameManagementGUI(plugin), (Player) e.getWhoClicked()))
        );

        super.decorate(player);
    }
}
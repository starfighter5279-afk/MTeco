package com.mteco.inventory.impl;

import com.cryptomorin.xseries.XMaterial;
import com.mteco.MTeco;
import com.mteco.inventory.InventoryButton;
import com.mteco.inventory.InventoryGUI;
import com.mteco.util.CurrencyUtil;
import com.mteco.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class NameManagementGUI extends InventoryGUI {
    private final MTeco plugin;

    public NameManagementGUI(MTeco plugin) {
        this.plugin = plugin;
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 27, "\u00a76Name Management");
    }

    @Override
    public void decorate(Player player) {
        String[] state = plugin.getCharacterManager().getPendingNameChange(player.getUniqueId());
        if (state == null) {
            plugin.getCharacterManager().initPendingNameChange(player.getUniqueId());
            state = plugin.getCharacterManager().getPendingNameChange(player.getUniqueId());
        }
        final String[] currentState = state;

        fillGlass(27, XMaterial.ORANGE_STAINED_GLASS_PANE);

        addButton(10, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.PAPER, "\u00a7eFirst Name",
                        "\u00a77Current: \u00a7f" + currentState[0],
                        "\u00a77Click to change"))
                .consumer(e -> {
                    Player p = (Player) e.getWhoClicked();
                    plugin.getChatInputManager().requestInput(p, "\u00a7eType the new first name:", input -> {
                        String[] s = plugin.getCharacterManager().getPendingNameChange(p.getUniqueId());
                        if (s != null) s[0] = capitalize(input.split(" ")[0]);
                        plugin.getGUIManager().openGUI(new NameManagementGUI(plugin), p);
                    });
                })
        );

        addButton(12, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.PAPER, "\u00a7eMiddle Name",
                        "\u00a77Current: \u00a7f" + currentState[1],
                        "\u00a77Click to change"))
                .consumer(e -> {
                    Player p = (Player) e.getWhoClicked();
                    plugin.getChatInputManager().requestInput(p, "\u00a7eType the new middle name:", input -> {
                        String[] s = plugin.getCharacterManager().getPendingNameChange(p.getUniqueId());
                        if (s != null) s[1] = capitalize(input.split(" ")[0]);
                        plugin.getGUIManager().openGUI(new NameManagementGUI(plugin), p);
                    });
                })
        );

        addButton(14, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.PAPER, "\u00a7eLast Name",
                        "\u00a77Current: \u00a7f" + currentState[2],
                        "\u00a77Click to change"))
                .consumer(e -> {
                    Player p = (Player) e.getWhoClicked();
                    plugin.getChatInputManager().requestInput(p, "\u00a7eType the new last name:", input -> {
                        String[] s = plugin.getCharacterManager().getPendingNameChange(p.getUniqueId());
                        if (s != null) s[2] = capitalize(input.split(" ")[0]);
                        plugin.getGUIManager().openGUI(new NameManagementGUI(plugin), p);
                    });
                })
        );

        double balance = plugin.getEconomy().getBalance(player);
        double nameChangeCost = plugin.getSettings().getNameChangeCost();
        boolean canAfford = balance >= nameChangeCost;

        addButton(22, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(
                        canAfford ? XMaterial.LIME_WOOL : XMaterial.RED_WOOL,
                        "\u00a7aConfirm Name Change",
                        "\u00a77Cost: \u00a7e" + CurrencyUtil.symbol() + String.format("%.0f", nameChangeCost),
                        canAfford ? "\u00a7aYou can afford this!" : "\u00a7cInsufficient funds!",
                        "\u00a77New name: \u00a7f" + currentState[0] + " " + currentState[1] + " " + currentState[2]))
                .consumer(e -> {
                    Player p = (Player) e.getWhoClicked();
                    if (!canAfford) {
                        p.sendMessage("\u00a7cYou cannot afford the name change fee of " + CurrencyUtil.symbol() + String.format("%.0f", nameChangeCost) + ".");
                        return;
                    }
                    plugin.getGUIManager().openGUI(new NameConfirmGUI(plugin), p);
                })
        );

        addButton(26, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7cBack"))
                .consumer(e -> {
                    Player p = (Player) e.getWhoClicked();
                    plugin.getCharacterManager().clearPendingNameChange(p.getUniqueId());
                    plugin.getGUIManager().openGUI(new CharacterManagementGUI(plugin), p);
                })
        );

        super.decorate(player);
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
    }
}
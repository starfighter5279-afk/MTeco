package com.dirt.inventory.impl;

import com.cryptomorin.xseries.XMaterial;
import com.dirt.DirtEconomy;
import com.dirt.data.CharacterData;
import com.dirt.inventory.InventoryButton;
import com.dirt.inventory.InventoryGUI;
import com.dirt.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;

public class CharacterCreationGUI extends InventoryGUI {
    private final DirtEconomy plugin;

    public CharacterCreationGUI(DirtEconomy plugin) {
        this.plugin = plugin;
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 27, "§6Character Creation");
    }

    @Override
    public void decorate(Player player) {
        String[] state = plugin.getCharacterManager().getPendingCreation(player.getUniqueId());
        String firstName = state[0];
        String middleName = state[1];
        String lastName = state[2];
        String gender = state[3];

        fillGlass(27, XMaterial.ORANGE_STAINED_GLASS_PANE);

        String fnDisplay = firstName != null ? "§a" + firstName : "§7Not Set";
        addButton(10, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.PAPER, "§eFirst Name",
                        "§7Current: " + fnDisplay,
                        "§7Click to set"))
                .consumer(e -> {
                    Player p = (Player) e.getWhoClicked();
                    plugin.getChatInputManager().requestInput(p, "§eType your character's first name:", input -> {
                        String[] s = plugin.getCharacterManager().getPendingCreation(p.getUniqueId());
                        s[0] = capitalize(input.split(" ")[0]);
                        plugin.getCharacterManager().setPendingCreation(p.getUniqueId(), s);
                        plugin.getGUIManager().openGUI(new CharacterCreationGUI(plugin), p);
                    });
                })
        );

        String mnDisplay = middleName != null ? "§a" + middleName : "§7Not Set";
        addButton(12, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.PAPER, "§eMiddle Name",
                        "§7Current: " + mnDisplay,
                        "§7Click to set"))
                .consumer(e -> {
                    Player p = (Player) e.getWhoClicked();
                    plugin.getChatInputManager().requestInput(p, "§eType your character's middle name:", input -> {
                        String[] s = plugin.getCharacterManager().getPendingCreation(p.getUniqueId());
                        s[1] = capitalize(input.split(" ")[0]);
                        plugin.getCharacterManager().setPendingCreation(p.getUniqueId(), s);
                        plugin.getGUIManager().openGUI(new CharacterCreationGUI(plugin), p);
                    });
                })
        );

        String lnDisplay = lastName != null ? "§a" + lastName : "§7Not Set";
        addButton(14, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.PAPER, "§eLast Name",
                        "§7Current: " + lnDisplay,
                        "§7Click to set"))
                .consumer(e -> {
                    Player p = (Player) e.getWhoClicked();
                    plugin.getChatInputManager().requestInput(p, "§eType your character's last name:", input -> {
                        String[] s = plugin.getCharacterManager().getPendingCreation(p.getUniqueId());
                        s[2] = capitalize(input.split(" ")[0]);
                        plugin.getCharacterManager().setPendingCreation(p.getUniqueId(), s);
                        plugin.getGUIManager().openGUI(new CharacterCreationGUI(plugin), p);
                    });
                })
        );

        boolean isMale = "MALE".equals(gender);
        XMaterial genderMat = isMale ? XMaterial.LIGHT_BLUE_WOOL : XMaterial.PINK_WOOL;
        String genderDisplay = isMale ? "§bMale" : "§dFemale";
        addButton(16, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(genderMat, "§eGender",
                        "§7Current: " + genderDisplay,
                        "§7Click to toggle"))
                .consumer(e -> {
                    Player p = (Player) e.getWhoClicked();
                    String[] s = plugin.getCharacterManager().getPendingCreation(p.getUniqueId());
                    s[3] = "MALE".equals(s[3]) ? "FEMALE" : "MALE";
                    plugin.getCharacterManager().setPendingCreation(p.getUniqueId(), s);
                    // Reopen with delay so the click event fully resolves first.
                    // The onClose reopen check runs at 3 ticks, so 2 ticks is safely ahead.
                    plugin.getServer().getScheduler().runTaskLater(plugin, () ->
                            plugin.getGUIManager().openGUI(new CharacterCreationGUI(plugin), p), 2L);
                })
        );

        boolean complete = firstName != null && middleName != null && lastName != null;
        XMaterial confirmMat = complete ? XMaterial.LIME_WOOL : XMaterial.GRAY_WOOL;
        String confirmLore = complete ? "§aClick to create your character!" : "§cFill in all fields first.";
        addButton(22, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(confirmMat, "§aConfirm Character", confirmLore))
                .consumer(e -> {
                    Player p = (Player) e.getWhoClicked();

                    // Geyser clicks may not close the inventory on first confirm.
                    // If the character was already saved on a previous click, just close.
                    if (plugin.getCharacterManager().hasCharacter(p.getUniqueId())) {
                        plugin.getServer().getScheduler().runTask(plugin, (Runnable) p::closeInventory);
                        return;
                    }

                    String[] s = plugin.getCharacterManager().getPendingCreation(p.getUniqueId());
                    if (s[0] == null || s[1] == null || s[2] == null) return;

                    CharacterData data = new CharacterData();
                    data.setPlayerUuid(p.getUniqueId());
                    data.setFirstName(s[0]);
                    data.setMiddleName(s[1]);
                    data.setLastName(s[2]);
                    data.setGender(s[3]);
                    data.setBirthDate(System.currentTimeMillis());
                    data.setAlive(true);

                    plugin.getCharacterManager().saveCharacter(data);
                    plugin.getCharacterManager().clearPendingCreation(p.getUniqueId());
                    plugin.getCharacterManager().removeForcedCreation(p.getUniqueId());

                    // Schedule close on next tick — Geyser/Bedrock ignores closeInventory
                    // during the click event, causing the GUI to stay open and the player
                    // to get stuck in a re-confirm loop.
                    plugin.getServer().getScheduler().runTask(plugin, (Runnable) p::closeInventory);
                    p.sendMessage("§aWelcome, §e" + s[0] + " " + s[1] + " " + s[2] + "§a! Your character has been created.");
                })
        );

        super.decorate(player);
    }

    @Override
    public void onClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();
        if (!plugin.getCharacterManager().isForcedCreation(player.getUniqueId())) return;
        // Do not re-open if chat is capturing their next message
        if (plugin.getChatInputManager().hasPendingInput(player.getUniqueId())) return;
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;
            if (plugin.getCharacterManager().hasCharacter(player.getUniqueId())) return;
            if (plugin.getChatInputManager().hasPendingInput(player.getUniqueId())) return;
            // Another handled GUI already replaced this one — don't fight it,
            // otherwise each reopen closes the previous GUI in an endless loop
            if (plugin.getGUIManager().isRegistered(player.getOpenInventory().getTopInventory())) return;
            plugin.getGUIManager().openGUI(new CharacterCreationGUI(plugin), player);
        }, 3L);
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
    }
}
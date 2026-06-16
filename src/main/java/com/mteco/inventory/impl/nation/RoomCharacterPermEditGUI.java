package com.mteco.inventory.impl.nation;

import com.cryptomorin.xseries.XMaterial;
import com.mteco.MTeco;
import com.mteco.data.CharacterData;
import com.mteco.data.RoomData;
import com.mteco.inventory.InventoryButton;
import com.mteco.inventory.InventoryGUI;
import com.mteco.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public class RoomCharacterPermEditGUI extends InventoryGUI {
    private final MTeco plugin;
    private final UUID roomId;
    private final UUID characterId;
    private final Runnable saveCallback;
    private final Consumer<Player> backCallback;

    private static final String[] PERM_KEYS = {"enter", "interact", "build", "containers"};
    private static final String[] PERM_LABELS = {"Enter", "Interact", "Build", "Open Containers"};

    public RoomCharacterPermEditGUI(MTeco plugin, UUID roomId, UUID characterId, Runnable saveCallback, Consumer<Player> backCallback) {
        this.plugin = plugin;
        this.roomId = roomId;
        this.characterId = characterId;
        this.saveCallback = saveCallback;
        this.backCallback = backCallback;
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 27, "\u00a76Edit Character Perms");
    }

    @Override
    public void decorate(Player player) {
        fillGlass(27, XMaterial.ORANGE_STAINED_GLASS_PANE);

        RoomData room = plugin.getNationManager().loadRoom(roomId);
        if (room == null) { super.decorate(player); return; }

        Map<String, Boolean> perms = room.getCharacterPerms().get(characterId);
        if (perms == null) { super.decorate(player); return; }

        CharacterData cd = plugin.getCharacterManager().getCharacter(characterId);
        String charName = cd != null ? cd.getFirstName() + " " + cd.getLastName() : "Unknown";

        addButton(4, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.PLAYER_HEAD, "\u00a7e" + charName,
                        "\u00a77Toggle permissions for this character."))
                .consumer(e -> {})
        );

        int[] slots = {10, 12, 14, 16};
        for (int i = 0; i < PERM_KEYS.length; i++) {
            final String key = PERM_KEYS[i];
            final String label = PERM_LABELS[i];
            int slot = slots[i];

            addButton(slot, new InventoryButton()
                    .creator(p -> {
                        boolean val = perms.getOrDefault(key, false);
                        if (val) {
                            return ItemUtil.buildItem(XMaterial.LIME_DYE, "\u00a7a\u2714 " + label,
                                    "\u00a7aAllowed", "\u00a77Click to deny.");
                        } else {
                            return ItemUtil.buildItem(XMaterial.GRAY_DYE, "\u00a7c\u2718 " + label,
                                    "\u00a7cDenied", "\u00a77Click to allow.");
                        }
                    })
                    .consumer(e -> {
                        boolean current = perms.getOrDefault(key, false);
                        perms.put(key, !current);
                        plugin.getNationManager().saveRoom(room);
                        saveCallback.run();
                        plugin.getGUIManager().openGUI(new RoomCharacterPermEditGUI(plugin, roomId, characterId, saveCallback, backCallback), (Player) e.getWhoClicked());
                    })
            );
        }

        addButton(22, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7cBack"))
                .consumer(e -> backCallback.accept((Player) e.getWhoClicked()))
        );

        super.decorate(player);
    }
}
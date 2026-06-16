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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public class RoomCharacterPermsGUI extends InventoryGUI {
    private final MTeco plugin;
    private final UUID roomId;
    private final Runnable saveCallback;
    private final Consumer<Player> backCallback;
    private final int page;

    private static final String[] PERM_KEYS = {"enter", "interact", "build", "containers"};
    private static final String[] PERM_LABELS = {"Enter", "Interact", "Build", "Containers"};

    public RoomCharacterPermsGUI(MTeco plugin, UUID roomId, Runnable saveCallback, Consumer<Player> backCallback, int page) {
        this.plugin = plugin;
        this.roomId = roomId;
        this.saveCallback = saveCallback;
        this.backCallback = backCallback;
        this.page = page;
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 54, "\u00a76Character Permissions");
    }

    @Override
    public void decorate(Player player) {
        fillPagedGui(54, XMaterial.ORANGE_STAINED_GLASS_PANE);

        RoomData room = plugin.getNationManager().loadRoom(roomId);
        if (room == null) { super.decorate(player); return; }

        addButton(4, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.PLAYER_HEAD, "\u00a76Character Permissions",
                        "\u00a77Toggle permissions per character.",
                        "\u00a77Click a character to toggle perms.",
                        "\u00a7eUse Add Character to add new entries."))
                .consumer(e -> {})
        );

        List<UUID> charIds = new ArrayList<>(room.getCharacterPerms().keySet());
        int itemsPerPage = 28;
        int startIndex = page * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, charIds.size());

        int[] slots = {
                10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34,
                37, 38, 39, 40, 41, 42, 43
        };

        int slotIndex = 0;
        for (int i = startIndex; i < endIndex; i++) {
            if (slotIndex >= slots.length) break;
            UUID charId = charIds.get(i);
            Map<String, Boolean> perms = room.getCharacterPerms().get(charId);
            CharacterData cd = plugin.getCharacterManager().getCharacter(charId);
            String charName = cd != null ? cd.getFirstName() + " " + cd.getLastName() : "Unknown";

            List<String> lore = new ArrayList<>();
            for (int j = 0; j < PERM_KEYS.length; j++) {
                boolean val = perms.getOrDefault(PERM_KEYS[j], false);
                lore.add((val ? "\u00a7a\u2714 " : "\u00a7c\u2718 ") + PERM_LABELS[j]);
            }
            lore.add("");
            lore.add("\u00a7eLeft-click to toggle permissions");
            lore.add("\u00a7cRight-click to remove");

            final UUID fCharId = charId;
            final String fCharName = charName;
            int slot = slots[slotIndex++];

            addButton(slot, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.PLAYER_HEAD, "\u00a7e" + fCharName, lore.toArray(new String[0])))
                    .consumer(e -> {
                        if (e.getClick().isRightClick()) {
                            RoomData rm = plugin.getNationManager().loadRoom(roomId);
                            if (rm == null) return;
                            rm.getCharacterPerms().remove(fCharId);
                            plugin.getNationManager().saveRoom(rm);
                            saveCallback.run();
                            plugin.getGUIManager().openGUI(new RoomCharacterPermsGUI(plugin, roomId, saveCallback, backCallback, page), (Player) e.getWhoClicked());
                            return;
                        }
                        plugin.getGUIManager().openGUI(new RoomCharacterPermEditGUI(plugin, roomId, fCharId, saveCallback,
                                back -> plugin.getGUIManager().openGUI(new RoomCharacterPermsGUI(plugin, roomId, saveCallback, backCallback, page), back)), (Player) e.getWhoClicked());
                    })
            );
        }

        addButton(48, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.LIME_DYE, "\u00a7aAdd Character",
                        "\u00a77Type a character name to add",
                        "\u00a77per-character permissions."))
                .consumer(e -> {
                    Player p = (Player) e.getWhoClicked();
                    plugin.getChatInputManager().requestInput(p, "\u00a7eEnter the character's first name (or first last):", input -> {
                        String query = input.trim().toLowerCase();
                        CharacterData found = null;
                        for (CharacterData cd2 : plugin.getCharacterManager().getAllCharacters()) {
                            String full = (cd2.getFirstName() + " " + cd2.getLastName()).toLowerCase();
                            if (full.contains(query) || cd2.getFirstName().toLowerCase().contains(query)) {
                                found = cd2;
                                break;
                            }
                        }
                        if (found == null) {
                            p.sendMessage("\u00a7cCharacter not found.");
                            plugin.getGUIManager().openGUI(new RoomCharacterPermsGUI(plugin, roomId, saveCallback, backCallback, page), p);
                            return;
                        }
                        RoomData rm = plugin.getNationManager().loadRoom(roomId);
                        if (rm == null) return;
                        UUID cId = found.getPlayerUuid();
                        if (!rm.getCharacterPerms().containsKey(cId)) {
                            Map<String, Boolean> defPerms = new HashMap<>();
                            defPerms.put("enter", true);
                            defPerms.put("interact", false);
                            defPerms.put("build", false);
                            defPerms.put("containers", false);
                            rm.getCharacterPerms().put(cId, defPerms);
                            plugin.getNationManager().saveRoom(rm);
                            saveCallback.run();
                        }
                        p.sendMessage("\u00a7aCharacter added to room permissions.");
                        plugin.getGUIManager().openGUI(new RoomCharacterPermsGUI(plugin, roomId, saveCallback, backCallback, page), p);
                    });
                })
        );

        if (page > 0) {
            addButton(47, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7ePrevious Page"))
                    .consumer(e -> plugin.getGUIManager().openGUI(new RoomCharacterPermsGUI(plugin, roomId, saveCallback, backCallback, page - 1), (Player) e.getWhoClicked()))
            );
        }

        if (endIndex < charIds.size()) {
            addButton(50, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7eNext Page"))
                    .consumer(e -> plugin.getGUIManager().openGUI(new RoomCharacterPermsGUI(plugin, roomId, saveCallback, backCallback, page + 1), (Player) e.getWhoClicked()))
            );
        }

        addButton(53, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.BARRIER, "\u00a7cBack"))
                .consumer(e -> backCallback.accept((Player) e.getWhoClicked()))
        );

        super.decorate(player);
    }
}
package com.dirt.inventory.impl.nation;

import com.cryptomorin.xseries.XMaterial;
import com.dirt.DirtEconomy;
import com.dirt.data.CharacterData;
import com.dirt.data.RoomData;
import com.dirt.inventory.InventoryButton;
import com.dirt.inventory.InventoryGUI;
import com.dirt.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public class RoomPermissionsGUI extends InventoryGUI {
    private final DirtEconomy plugin;
    private final UUID roomId;
    private final Runnable saveCallback;
    private final Consumer<Player> backCallback;
    private Consumer<Player> rolePermsOpener;

    private static final String[] PERM_KEYS = {"enter", "interact", "build", "containers"};
    private static final String[] PERM_LABELS = {"Enter", "Interact", "Build", "Open Containers"};

    public RoomPermissionsGUI(DirtEconomy plugin, UUID roomId, Runnable saveCallback, Consumer<Player> backCallback) {
        this.plugin = plugin;
        this.roomId = roomId;
        this.saveCallback = saveCallback;
        this.backCallback = backCallback;
    }

    public void setRolePermsOpener(Consumer<Player> rolePermsOpener) {
        this.rolePermsOpener = rolePermsOpener;
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 54, "\u00a76Room Permissions");
    }

    @Override
    public void decorate(Player player) {
        fillPagedGui(54, XMaterial.ORANGE_STAINED_GLASS_PANE);

        RoomData room = plugin.getNationManager().loadRoom(roomId);
        if (room == null) { super.decorate(player); return; }

        addButton(4, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.COMPARATOR, "\u00a76Room Permissions",
                        "\u00a77Configure access for each group.",
                        "\u00a77Click toggles to allow or deny.",
                        "\u00a77Use \u00a7eAdd Character \u00a77to set",
                        "\u00a77per-character permissions."))
                .consumer(e -> {})
        );

        addButton(11, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.LIME_BANNER, "\u00a7aSame Region",
                        "\u00a77Players from the same region."))
                .consumer(e -> {})
        );
        addButton(13, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.YELLOW_BANNER, "\u00a7eSame Nation",
                        "\u00a77Players from the same nation",
                        "\u00a77but a different region."))
                .consumer(e -> {})
        );
        addButton(15, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.RED_BANNER, "\u00a7cForeign",
                        "\u00a77Players not from this nation."))
                .consumer(e -> {})
        );

        int[][] layout = {{18, 20, 22, 24}, {27, 29, 31, 33}, {36, 38, 40, 42}, {45, 47, 49, 51}};
        for (int i = 0; i < PERM_KEYS.length; i++) {
            final String key = PERM_KEYS[i];
            final String label = PERM_LABELS[i];
            int labelSlot = layout[i][0];
            int srSlot = layout[i][1];
            int snSlot = layout[i][2];
            int fSlot = layout[i][3];

            addButton(labelSlot, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.PAPER, "\u00a7f" + label))
                    .consumer(e -> {})
            );
            addToggle(srSlot, key, label, room.getPermsSameRegion(), "\u00a7aSame Region", room);
            addToggle(snSlot, key, label, room.getPermsSameNation(), "\u00a7eSame Nation", room);
            addToggle(fSlot, key, label, room.getPermsForeign(), "\u00a7cForeign", room);
        }

        if (rolePermsOpener != null) {
            addButton(50, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.NAME_TAG, "\u00a7eRole Permissions",
                            "\u00a77Set permissions per role.",
                            "\u00a7eClick to manage."))
                    .consumer(e -> rolePermsOpener.accept((Player) e.getWhoClicked()))
            );
        }

        int charCount = room.getCharacterPerms().size();
        addButton(52, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.PLAYER_HEAD, "\u00a7eCharacter Permissions \u00a77(" + charCount + ")",
                        "\u00a77Set permissions for specific characters.",
                        "\u00a7eClick to manage."))
                .consumer(e -> {
                    Player p = (Player) e.getWhoClicked();
                    plugin.getGUIManager().openGUI(new RoomCharacterPermsGUI(plugin, roomId, saveCallback,
                            back -> plugin.getGUIManager().openGUI(new RoomPermissionsGUI(plugin, roomId, saveCallback, backCallback), back), 0), p);
                })
        );

        addButton(53, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.BARRIER, "\u00a7cBack"))
                .consumer(e -> backCallback.accept((Player) e.getWhoClicked()))
        );

        super.decorate(player);
    }

    private void addToggle(int slot, String permKey, String permLabel,
                           Map<String, Boolean> map, String groupName, RoomData room) {
        addButton(slot, new InventoryButton()
                .creator(p -> {
                    boolean val = map.getOrDefault(permKey, false);
                    if (val) {
                        return ItemUtil.buildItem(XMaterial.LIME_DYE, "\u00a7a\u2714 " + permLabel,
                                groupName + " \u00a7f\u2192 \u00a7aAllowed",
                                "\u00a77Click to deny.");
                    } else {
                        return ItemUtil.buildItem(XMaterial.GRAY_DYE, "\u00a7c\u2718 " + permLabel,
                                groupName + " \u00a7f\u2192 \u00a7cDenied",
                                "\u00a77Click to allow.");
                    }
                })
                .consumer(e -> {
                    boolean current = map.getOrDefault(permKey, false);
                    map.put(permKey, !current);
                    saveCallback.run();
                    plugin.getGUIManager().openGUI(new RoomPermissionsGUI(plugin, roomId, saveCallback, backCallback), (Player) e.getWhoClicked());
                })
        );
    }
}
package com.mteco.inventory.impl.nation;

import com.cryptomorin.xseries.XMaterial;
import com.mteco.MTeco;
import com.mteco.data.RoomData;
import com.mteco.inventory.InventoryButton;
import com.mteco.inventory.InventoryGUI;
import com.mteco.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public class RoomManagementGUI extends InventoryGUI {
    private final MTeco plugin;
    private final UUID propertyId;
    private final boolean isBusiness;
    private final UUID contextId;
    private final int page;
    private final Consumer<Player> backCallback;

    public RoomManagementGUI(MTeco plugin, UUID propertyId, boolean isBusiness, UUID contextId, int page, Consumer<Player> backCallback) {
        this.plugin = plugin;
        this.propertyId = propertyId;
        this.isBusiness = isBusiness;
        this.contextId = contextId;
        this.page = page;
        this.backCallback = backCallback;
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 54, "\u00a76Rooms");
    }

    @Override
    public void decorate(Player player) {
        fillPagedGui(54, XMaterial.ORANGE_STAINED_GLASS_PANE);

        List<UUID> roomIds;
        if (isBusiness) {
            com.mteco.data.BusinessPropertyData bprop = plugin.getBusinessManager().loadProperty(propertyId);
            roomIds = bprop != null ? bprop.getRoomIds() : new ArrayList<>();
        } else {
            com.mteco.data.PropertyData prop = plugin.getNationManager().loadProperty(propertyId);
            roomIds = prop != null ? prop.getRoomIds() : new ArrayList<>();
        }

        addButton(4, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.OAK_DOOR, "\u00a76Rooms",
                        "\u00a77Total: \u00a7f" + roomIds.size(),
                        "\u00a77Click a room to manage it."))
                .consumer(e -> {})
        );

        int itemsPerPage = 28;
        int startIndex = page * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, roomIds.size());

        int[] slots = {
                10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34,
                37, 38, 39, 40, 41, 42, 43
        };

        int slotIndex = 0;
        for (int i = startIndex; i < endIndex; i++) {
            if (slotIndex >= slots.length) break;
            UUID roomId = roomIds.get(i);
            RoomData room = plugin.getNationManager().loadRoom(roomId);
            if (room == null) { slotIndex++; continue; }
            int slot = slots[slotIndex++];
            String roomName = room.getName() != null && !room.getName().isEmpty() ? room.getName() : "Room";

            addButton(slot, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.OAK_DOOR, "\u00a7e" + roomName,
                            "\u00a77World: \u00a7f" + room.getWorld(),
                            "\u00a77Corner 1: \u00a7f" + room.getCorner1X() + ", " + room.getCorner1Y() + ", " + room.getCorner1Z(),
                            "\u00a77Corner 2: \u00a7f" + room.getCorner2X() + ", " + room.getCorner2Y() + ", " + room.getCorner2Z(),
                            "\u00a7eClick to manage"))
                    .consumer(e -> {
                        Player p = (Player) e.getWhoClicked();
                        plugin.getGUIManager().openGUI(new RoomDetailGUI(plugin, propertyId, roomId, isBusiness, contextId,
                                back -> plugin.getGUIManager().openGUI(new RoomManagementGUI(plugin, propertyId, isBusiness, contextId, page, backCallback), back)), p);
                    })
            );
        }

        int totalPages = Math.max(1, (int) Math.ceil((double) roomIds.size() / itemsPerPage));
        addPageIndicator(49, page, totalPages);

        if (page > 0) {
            addButton(48, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7ePrevious Page"))
                    .consumer(e -> plugin.getGUIManager().openGUI(new RoomManagementGUI(plugin, propertyId, isBusiness, contextId, page - 1, backCallback), (Player) e.getWhoClicked()))
            );
        }
        if (endIndex < roomIds.size()) {
            addButton(50, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7eNext Page"))
                    .consumer(e -> plugin.getGUIManager().openGUI(new RoomManagementGUI(plugin, propertyId, isBusiness, contextId, page + 1, backCallback), (Player) e.getWhoClicked()))
            );
        }

        addButton(46, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.LIME_DYE, "\u00a7aCreate Room",
                        "\u00a77Use the golden shovel to select",
                        "\u00a77two corners for the room.",
                        "\u00a77Ceiling is detected automatically."))
                .consumer(e -> {
                    Player p = (Player) e.getWhoClicked();
                    p.closeInventory();
                    plugin.getChunkSelectionManager().startRoomCreation(p, propertyId);
                })
        );

        addButton(53, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.BARRIER, "\u00a7cBack"))
                .consumer(e -> backCallback.accept((Player) e.getWhoClicked()))
        );

        super.decorate(player);
    }
}
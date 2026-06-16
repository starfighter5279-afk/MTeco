package com.mteco.inventory.impl.nation;

import com.cryptomorin.xseries.XMaterial;
import com.mteco.MTeco;
import com.mteco.data.BusinessData;
import com.mteco.data.BusinessPropertyData;
import com.mteco.data.NationData;
import com.mteco.data.PropertyData;
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

public class RoomDetailGUI extends InventoryGUI {
    private final MTeco plugin;
    private final UUID propertyId;
    private final UUID roomId;
    private final boolean isBusiness;
    private final UUID contextId;
    private final Consumer<Player> backCallback;

    public RoomDetailGUI(MTeco plugin, UUID propertyId, UUID roomId, boolean isBusiness, UUID contextId, Consumer<Player> backCallback) {
        this.plugin = plugin;
        this.propertyId = propertyId;
        this.roomId = roomId;
        this.isBusiness = isBusiness;
        this.contextId = contextId;
        this.backCallback = backCallback;
    }

    @Override
    protected Inventory createInventory() {
        RoomData room = plugin.getNationManager().loadRoom(roomId);
        String title = room != null && room.getName() != null && !room.getName().isEmpty()
                ? "\u00a76Room: " + room.getName() : "\u00a76Room Detail";
        return Bukkit.createInventory(null, 27, title);
    }

    @Override
    public void decorate(Player player) {
        fillGlass(27, XMaterial.ORANGE_STAINED_GLASS_PANE);

        RoomData room = plugin.getNationManager().loadRoom(roomId);
        if (room == null) { super.decorate(player); return; }

        String roomName = room.getName() != null && !room.getName().isEmpty() ? room.getName() : "Room";

        addButton(4, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.OAK_DOOR, "\u00a7e" + roomName,
                        "\u00a77World: \u00a7f" + room.getWorld(),
                        "\u00a77Corner 1: \u00a7f" + room.getCorner1X() + ", " + room.getCorner1Y() + ", " + room.getCorner1Z(),
                        "\u00a77Corner 2: \u00a7f" + room.getCorner2X() + ", " + room.getCorner2Y() + ", " + room.getCorner2Z()))
                .consumer(e -> {})
        );

        addButton(10, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.NAME_TAG, "\u00a7eRename Room",
                        "\u00a77Current: \u00a7f" + roomName))
                .consumer(e -> {
                    Player p = (Player) e.getWhoClicked();
                    plugin.getChatInputManager().requestInput(p, "\u00a7eEnter new room name:", input -> {
                        String name = input.trim();
                        if (name.isEmpty()) { p.sendMessage("\u00a7cName cannot be empty."); return; }
                        RoomData r = plugin.getNationManager().loadRoom(roomId);
                        if (r == null) return;
                        r.setName(name);
                        plugin.getNationManager().saveRoom(r);
                        p.sendMessage("\u00a7aRoom renamed to '\u00a76" + name + "\u00a7a'.");
                        plugin.getGUIManager().openGUI(new RoomDetailGUI(plugin, propertyId, roomId, isBusiness, contextId, backCallback), p);
                    });
                })
        );

        addButton(12, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.COMPARATOR, "\u00a7ePermissions",
                        "\u00a77Manage room access and",
                        "\u00a77character-specific permissions."))
                .consumer(e -> {
                    Player p = (Player) e.getWhoClicked();
                    RoomData r = plugin.getNationManager().loadRoom(roomId);
                    if (r == null) return;
                    RoomPermissionsGUI permsGui = new RoomPermissionsGUI(plugin, roomId,
                            () -> plugin.getNationManager().saveRoom(r),
                            back -> plugin.getGUIManager().openGUI(new RoomDetailGUI(plugin, propertyId, roomId, isBusiness, contextId, backCallback), back));
                    if (contextId != null) {
                        permsGui.setRolePermsOpener(rp -> {
                            RoomData rRoom = plugin.getNationManager().loadRoom(roomId);
                            if (rRoom == null) return;
                            List<UUID> rIds = new ArrayList<>();
                            List<String> rNames = new ArrayList<>();
                            List<String> rColors = new ArrayList<>();
                            if (isBusiness) {
                                BusinessData biz = plugin.getBusinessManager().loadBusiness(contextId);
                                if (biz != null) RolePermsGUI.buildBizRoles(biz, rIds, rNames, rColors);
                            } else {
                                NationData n = plugin.getNationManager().loadNation(contextId);
                                if (n != null) RolePermsGUI.buildGovRoles(plugin, n, rIds, rNames, rColors);
                            }
                            plugin.getGUIManager().openGUI(new RolePermsGUI(plugin, rRoom.getRolePerms(),
                                    rIds, rNames, rColors,
                                    () -> plugin.getNationManager().saveRoom(rRoom),
                                    back -> plugin.getGUIManager().openGUI(new RoomDetailGUI(plugin, propertyId, roomId, isBusiness, contextId, backCallback), back),
                                    0), rp);
                        });
                    }
                    plugin.getGUIManager().openGUI(permsGui, p);
                })
        );

        addButton(16, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.RED_DYE, "\u00a7cDelete Room",
                        "\u00a77Permanently remove this room."))
                .consumer(e -> {
                    Player p = (Player) e.getWhoClicked();
                    if (isBusiness) {
                        BusinessPropertyData bprop = plugin.getBusinessManager().loadProperty(propertyId);
                        if (bprop != null) {
                            bprop.getRoomIds().remove(roomId);
                            plugin.getBusinessManager().saveProperty(bprop);
                        }
                    } else {
                        PropertyData prop = plugin.getNationManager().loadProperty(propertyId);
                        if (prop != null) {
                            prop.getRoomIds().remove(roomId);
                            plugin.getNationManager().saveProperty(prop);
                        }
                    }
                    plugin.getNationManager().deleteRoom(roomId);
                    p.sendMessage("\u00a7cRoom deleted.");
                    backCallback.accept(p);
                })
        );

        addButton(22, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7cBack"))
                .consumer(e -> backCallback.accept((Player) e.getWhoClicked()))
        );

        super.decorate(player);
    }
}
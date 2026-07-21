package com.dirt.inventory.impl.phone;

import com.cryptomorin.xseries.XMaterial;
import com.dirt.DirtEconomy;
import com.dirt.data.PhoneData;
import com.dirt.inventory.InventoryButton;
import com.dirt.inventory.InventoryGUI;
import com.dirt.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class PhoneSettingsGUI extends InventoryGUI {
    private final DirtEconomy plugin;

    public PhoneSettingsGUI(DirtEconomy plugin) {
        this.plugin = plugin;
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 27, "\u00a77\u00a7lPhone Settings");
    }

    @Override
    public void decorate(Player player) {
        fillGlass(27, XMaterial.PURPLE_STAINED_GLASS_PANE);

        PhoneData phone = plugin.getPhoneManager().getPhone(player.getUniqueId());
        String username = phone != null ? phone.getUsername() : "None";
        int contacts = phone != null ? phone.getContacts().size() : 0;
        int conversations = phone != null ? phone.getConversationIds().size() : 0;
        int notifications = phone != null ? phone.getNotifications().size() : 0;
        long unread = phone != null ? phone.getNotifications().stream().filter(n -> !n.isRead()).count() : 0;

        addButton(4, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.NAME_TAG, "\u00a7b\u00a7lYour Username",
                        "\u00a7f" + username,
                        "",
                        "\u00a77Contacts: \u00a7f" + contacts,
                        "\u00a77Conversations: \u00a7f" + conversations,
                        "\u00a77Notifications: \u00a7f" + notifications + " (" + unread + " unread)"))
                .consumer(e -> {})
        );

        addButton(11, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.BELL, "\u00a76Clear Notifications",
                        "\u00a77Mark all notifications as read."))
                .consumer(e -> {
                    if (phone != null) {
                        for (var n : phone.getNotifications()) n.setRead(true);
                        plugin.getPhoneManager().savePhone(phone);
                        ((Player) e.getWhoClicked()).sendMessage("\u00a7aAll notifications cleared.");
                        plugin.getGUIManager().openGUI(new PhoneSettingsGUI(plugin), (Player) e.getWhoClicked());
                    }
                })
        );

        addButton(15, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.RED_DYE, "\u00a7cClear All Notifications",
                        "\u00a77Remove all notifications permanently."))
                .consumer(e -> {
                    if (phone != null) {
                        phone.getNotifications().clear();
                        plugin.getPhoneManager().savePhone(phone);
                        ((Player) e.getWhoClicked()).sendMessage("\u00a7aNotifications deleted.");
                        plugin.getGUIManager().openGUI(new PhoneSettingsGUI(plugin), (Player) e.getWhoClicked());
                    }
                })
        );

        addButton(26, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7cBack"))
                .consumer(e -> plugin.getGUIManager().openGUI(new PhoneHomescreenGUI(plugin, 0), (Player) e.getWhoClicked()))
        );

        super.decorate(player);
    }
}
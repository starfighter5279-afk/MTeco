package com.dirt.inventory.impl.phone;

import com.cryptomorin.xseries.XMaterial;
import com.dirt.DirtEconomy;
import com.dirt.data.PhoneData;
import com.dirt.data.PhoneNotification;
import com.dirt.inventory.InventoryButton;
import com.dirt.inventory.InventoryGUI;
import com.dirt.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.List;

public class PhoneLockscreenGUI extends InventoryGUI {
    private final DirtEconomy plugin;

    public PhoneLockscreenGUI(DirtEconomy plugin) {
        this.plugin = plugin;
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 27, "\u00a78\u2588 \u00a7b\u00a7lPhone \u00a78\u2588");
    }

    @Override
    public void decorate(Player player) {
        XMaterial[] topColors = {
                XMaterial.MAGENTA_STAINED_GLASS_PANE, XMaterial.PINK_STAINED_GLASS_PANE,
                XMaterial.LIGHT_BLUE_STAINED_GLASS_PANE, XMaterial.CYAN_STAINED_GLASS_PANE,
                XMaterial.LIGHT_BLUE_STAINED_GLASS_PANE, XMaterial.CYAN_STAINED_GLASS_PANE,
                XMaterial.LIGHT_BLUE_STAINED_GLASS_PANE, XMaterial.PINK_STAINED_GLASS_PANE,
                XMaterial.MAGENTA_STAINED_GLASS_PANE
        };
        for (int i = 0; i < 9; i++) {
            final XMaterial c = topColors[i];
            addButton(i, new InventoryButton().creator(p -> ItemUtil.buildGlassPane(c)).consumer(e -> {}));
        }
        for (int i = 9; i < 18; i++) {
            addButton(i, new InventoryButton().creator(p -> ItemUtil.buildGlassPane()).consumer(e -> {}));
        }
        for (int i = 18; i < 27; i++) {
            addButton(i, new InventoryButton().creator(p -> ItemUtil.buildGlassPane(XMaterial.GRAY_STAINED_GLASS_PANE)).consumer(e -> {}));
        }

        PhoneData phone = plugin.getPhoneManager().getPhone(player.getUniqueId());
        if (phone != null) {
            List<PhoneNotification> notifs = phone.getNotifications();
            int unread = 0;
            for (PhoneNotification n : notifs) { if (!n.isRead()) unread++; }

            final int finalUnread = unread;
            if (unread > 0) {
                addButton(4, new InventoryButton()
                        .creator(p -> ItemUtil.buildItem(XMaterial.BELL, "\u00a76\u00a7l" + finalUnread + " Notification" + (finalUnread > 1 ? "s" : ""),
                                "\u00a77Swipe below to view"))
                        .consumer(e -> {}));
            } else {
                addButton(4, new InventoryButton()
                        .creator(p -> ItemUtil.buildItem(XMaterial.GRAY_DYE, "\u00a77No new notifications"))
                        .consumer(e -> {}));
            }

            int shown = 0;
            for (int i = notifs.size() - 1; i >= 0 && shown < 5; i--) {
                PhoneNotification notif = notifs.get(i);
                if (notif.isRead()) continue;
                int slot = 11 + shown;
                addButton(slot, new InventoryButton()
                        .creator(p -> ItemUtil.buildItem(
                                notif.getType().equals("TEXT") ? XMaterial.WRITABLE_BOOK : XMaterial.PAPER,
                                "\u00a7e" + notif.getTitle(),
                                "\u00a77" + notif.getPreview(),
                                "",
                                "\u00a7eClick to open"))
                        .consumer(e -> {
                            Player p = (Player) e.getWhoClicked();
                            notif.setRead(true);
                            plugin.getPhoneManager().savePhone(phone);
                            if (notif.getConversationId() != null) {
                                plugin.getGUIManager().openGUI(new TexterChatGUI(plugin, notif.getConversationId(), 0), p);
                            }
                        })
                );
                shown++;
            }
        }

        addButton(22, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.LIME_DYE, "\u00a7a\u00a7l\u25CF Home",
                        "\u00a77Open your home screen"))
                .consumer(e -> {
                    Player p = (Player) e.getWhoClicked();
                    plugin.getGUIManager().openGUI(new PhoneHomescreenGUI(plugin, 0), p);
                })
        );

        super.decorate(player);
    }
}
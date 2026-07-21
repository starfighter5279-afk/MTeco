package com.dirt.inventory.impl.phone;

import com.cryptomorin.xseries.XMaterial;
import com.dirt.DirtEconomy;
import com.dirt.data.PhoneData;
import com.dirt.inventory.InventoryButton;
import com.dirt.inventory.InventoryGUI;
import com.dirt.inventory.impl.CharacterManagementGUI;
import com.dirt.inventory.impl.shops.OnlineShopsListGUI;
import com.dirt.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class PhoneHomescreenGUI extends InventoryGUI {
    private final DirtEconomy plugin;
    private final int page;

    private static final int[] APP_SLOTS = {2, 4, 6, 11, 13, 15};
    private static final int APPS_PER_PAGE = APP_SLOTS.length;

    public PhoneHomescreenGUI(DirtEconomy plugin, int page) {
        this.plugin = plugin;
        this.page = page;
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 27, "\u00a78\u2588 \u00a7b\u00a7lHome Screen \u00a78\u2588");
    }

    @Override
    public void decorate(Player player) {
        for (int i = 0; i < 18; i++) {
            addButton(i, new InventoryButton().creator(p -> ItemUtil.buildGlassPane(XMaterial.LIME_STAINED_GLASS_PANE)).consumer(e -> {}));
        }
        for (int i = 18; i < 27; i++) {
            addButton(i, new InventoryButton().creator(p -> ItemUtil.buildGlassPane(XMaterial.GREEN_STAINED_GLASS_PANE)).consumer(e -> {}));
        }

        List<AppIcon> apps = new ArrayList<>();
        if (plugin.getSettings().isPhoneTexterEnabled())
            apps.add(new AppIcon(XMaterial.WRITABLE_BOOK, "\u00a7a\u00a7lTexter",
                    new String[]{"\u00a77Send and receive messages"}, e -> {
                Player p = (Player) e.getWhoClicked();
                plugin.getGUIManager().openGUI(new TexterConversationListGUI(plugin, 0, null), p);
            }));
        if (plugin.getSettings().isPhoneContactsEnabled())
            apps.add(new AppIcon(XMaterial.PLAYER_HEAD, "\u00a7b\u00a7lContacts",
                    new String[]{"\u00a77Manage your contacts"}, e -> {
                Player p = (Player) e.getWhoClicked();
                plugin.getGUIManager().openGUI(new ContactsListGUI(plugin, 0, null), p);
            }));
        if (plugin.getSettings().isPhoneMapsEnabled() && plugin.getGpsManager() != null)
            apps.add(new AppIcon(XMaterial.MAP, "\u00a7e\u00a7lMaps",
                    new String[]{"\u00a77Navigate to locations"}, e -> {
                Player p = (Player) e.getWhoClicked();
                plugin.getGUIManager().openGUI(new MapsMainGUI(plugin), p);
            }));
        if (plugin.getSettings().isPhoneShopoholicEnabled() && plugin.isOnlineShopsEnabled())
            apps.add(new AppIcon(XMaterial.EMERALD, "\u00a72\u00a7lShopoholic",
                    new String[]{"\u00a77Browse online stores"}, e -> {
                Player p = (Player) e.getWhoClicked();
                plugin.getGUIManager().openGUI(new OnlineShopsListGUI(plugin, 0), p);
            }));
        if (plugin.getSettings().isPhoneLiferizeEnabled())
            apps.add(new AppIcon(XMaterial.TOTEM_OF_UNDYING, "\u00a7d\u00a7lLiferize",
                    new String[]{"\u00a77Character management"}, e -> {
                Player p = (Player) e.getWhoClicked();
                plugin.getGUIManager().openGUI(new CharacterManagementGUI(plugin), p);
            }));
        if (plugin.getSettings().isPhoneAppStoreEnabled())
            apps.add(new AppIcon(XMaterial.CHEST, "\u00a76\u00a7lAppitizer",
                    new String[]{"\u00a77Discover new apps"}, e -> {
                Player p = (Player) e.getWhoClicked();
                plugin.getGUIManager().openGUI(new AppStoreGUI(plugin, 0), p);
            }));

        int start = page * APPS_PER_PAGE;
        int end = Math.min(start + APPS_PER_PAGE, apps.size());
        int totalPages = Math.max(1, (int) Math.ceil((double) apps.size() / APPS_PER_PAGE));

        for (int i = start; i < end; i++) {
            AppIcon app = apps.get(i);
            int slot = APP_SLOTS[i - start];
            addButton(slot, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(app.icon, app.name, app.lore))
                    .consumer(app.action));
        }

        PhoneData phone = plugin.getPhoneManager().getPhone(player.getUniqueId());
        if (phone != null) {
            long unread = phone.getNotifications().stream().filter(n -> !n.isRead()).count();
            if (unread > 0) {
                addButton(0, new InventoryButton()
                        .creator(p -> ItemUtil.buildItem(XMaterial.BELL, "\u00a76" + unread + " unread",
                                "\u00a7eClick to view lockscreen"))
                        .consumer(e -> plugin.getGUIManager().openGUI(new PhoneLockscreenGUI(plugin), (Player) e.getWhoClicked()))
                );
            }
        }

        if (page > 0) {
            addButton(18, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7e\u00ab Previous Page"))
                    .consumer(e -> plugin.getGUIManager().openGUI(new PhoneHomescreenGUI(plugin, page - 1), (Player) e.getWhoClicked()))
            );
        }
        if (totalPages > 1) {
            addPageIndicator(22, page, totalPages);
        }
        if (end < apps.size()) {
            addButton(26, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7eNext Page \u00bb"))
                    .consumer(e -> plugin.getGUIManager().openGUI(new PhoneHomescreenGUI(plugin, page + 1), (Player) e.getWhoClicked()))
            );
        }

        addButton(24, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.COMPARATOR, "\u00a77\u00a7lSettings",
                        "\u00a77Phone settings"))
                .consumer(e -> plugin.getGUIManager().openGUI(new PhoneSettingsGUI(plugin), (Player) e.getWhoClicked()))
        );

        super.decorate(player);
    }

    private static class AppIcon {
        final XMaterial icon;
        final String name;
        final String[] lore;
        final Consumer<InventoryClickEvent> action;

        AppIcon(XMaterial icon, String name, String[] lore, Consumer<InventoryClickEvent> action) {
            this.icon = icon; this.name = name; this.lore = lore; this.action = action;
        }
    }
}
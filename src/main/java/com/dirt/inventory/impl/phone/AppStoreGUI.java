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

import java.util.ArrayList;
import java.util.List;

public class AppStoreGUI extends InventoryGUI {
    private final DirtEconomy plugin;
    private final int page;

    private static final int[] CONTENT_SLOTS = {1, 2, 3, 4, 5, 6, 7, 10, 11, 12, 13, 14, 15, 16};
    private static final int PER_PAGE = CONTENT_SLOTS.length;

    public AppStoreGUI(DirtEconomy plugin, int page) {
        this.plugin = plugin;
        this.page = page;
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 27, "\u00a76\u00a7lAppitizer");
    }

    @Override
    public void decorate(Player player) {
        for (int i = 0; i < 27; i++) {
            final XMaterial color;
            if (i == 0 || i == 8 || i == 9 || i == 17) color = XMaterial.ORANGE_STAINED_GLASS_PANE;
            else if (i >= 18) color = XMaterial.YELLOW_STAINED_GLASS_PANE;
            else color = XMaterial.BLACK_STAINED_GLASS_PANE;
            addButton(i, new InventoryButton().creator(p -> ItemUtil.buildGlassPane(color)).consumer(e -> {}));
        }

        PhoneData phone = plugin.getPhoneManager().getPhone(player.getUniqueId());
        if (phone == null) { super.decorate(player); return; }

        List<AppEntry> apps = new ArrayList<>();
        if (plugin.getSettings().isPhoneTexterEnabled())
            apps.add(new AppEntry("Texter", "Send messages to other players", XMaterial.WRITABLE_BOOK, "texter", true));
        if (plugin.getSettings().isPhoneContactsEnabled())
            apps.add(new AppEntry("Contacts", "Manage your phone contacts", XMaterial.PLAYER_HEAD, "contacts", true));
        if (plugin.getSettings().isPhoneMapsEnabled() && plugin.getGpsManager() != null)
            apps.add(new AppEntry("Maps", "Navigate with GPS", XMaterial.MAP, "maps", true));
        if (plugin.getSettings().isPhoneShopoholicEnabled() && plugin.isOnlineShopsEnabled())
            apps.add(new AppEntry("Shopoholic", "Browse online shops", XMaterial.EMERALD, "shopoholic", true));
        if (plugin.getSettings().isPhoneLiferizeEnabled())
            apps.add(new AppEntry("Liferize", "Character management", XMaterial.TOTEM_OF_UNDYING, "liferize", true));
        apps.add(new AppEntry("Settings", "Phone settings", XMaterial.COMPARATOR, "settings", true));

        int start = page * PER_PAGE;
        int end = Math.min(start + PER_PAGE, apps.size());

        for (int i = start; i < end; i++) {
            AppEntry app = apps.get(i);
            int slot = CONTENT_SLOTS[i - start];
            boolean installed = app.builtIn || phone.getInstalledApps().contains(app.id);

            addButton(slot, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(app.icon, "\u00a7e\u00a7l" + app.name,
                            "\u00a77" + app.description,
                            "",
                            installed ? "\u00a7aInstalled" : "\u00a7eFree \u2014 Click to install"))
                    .consumer(e -> {
                        if (!installed) {
                            phone.getInstalledApps().add(app.id);
                            plugin.getPhoneManager().savePhone(phone);
                            ((Player) e.getWhoClicked()).sendMessage("\u00a7a" + app.name + " installed!");
                            plugin.getGUIManager().openGUI(new AppStoreGUI(plugin, page), (Player) e.getWhoClicked());
                        }
                    })
            );
        }

        int totalPages = Math.max(1, (int) Math.ceil((double) apps.size() / PER_PAGE));

        if (page > 0) {
            addButton(18, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7e\u00ab Previous Page"))
                    .consumer(e -> plugin.getGUIManager().openGUI(new AppStoreGUI(plugin, page - 1), (Player) e.getWhoClicked()))
            );
        }
        addPageIndicator(22, page, totalPages);
        if (end < apps.size()) {
            addButton(26, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7eNext Page \u00bb"))
                    .consumer(e -> plugin.getGUIManager().openGUI(new AppStoreGUI(plugin, page + 1), (Player) e.getWhoClicked()))
            );
        }

        addButton(24, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7cBack"))
                .consumer(e -> plugin.getGUIManager().openGUI(new PhoneHomescreenGUI(plugin, 0), (Player) e.getWhoClicked()))
        );

        super.decorate(player);
    }

    private static class AppEntry {
        final String name, description, id;
        final XMaterial icon;
        final boolean builtIn;

        AppEntry(String name, String description, XMaterial icon, String id, boolean builtIn) {
            this.name = name; this.description = description; this.icon = icon; this.id = id; this.builtIn = builtIn;
        }
    }
}
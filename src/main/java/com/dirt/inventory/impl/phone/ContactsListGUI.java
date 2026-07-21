package com.dirt.inventory.impl.phone;

import com.cryptomorin.xseries.XMaterial;
import com.dirt.DirtEconomy;
import com.dirt.data.PhoneContact;
import com.dirt.data.PhoneConversation;
import com.dirt.data.PhoneData;
import com.dirt.inventory.InventoryButton;
import com.dirt.inventory.InventoryGUI;
import com.dirt.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ContactsListGUI extends InventoryGUI {
    private final DirtEconomy plugin;
    private final int page;
    private final String searchFilter;

    private static final int[] CONTENT_SLOTS = {1, 2, 3, 4, 5, 6, 7, 10, 11, 12, 13, 14, 15, 16};
    private static final int PER_PAGE = CONTENT_SLOTS.length;

    public ContactsListGUI(DirtEconomy plugin, int page, String searchFilter) {
        this.plugin = plugin;
        this.page = page;
        this.searchFilter = searchFilter;
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 27, "\u00a7b\u00a7lContacts" + (searchFilter != null ? " \u00a77\u00a7o" + searchFilter : ""));
    }

    @Override
    public void decorate(Player player) {
        for (int i = 0; i < 27; i++) {
            final XMaterial color;
            if (i == 0 || i == 8 || i == 9 || i == 17) color = XMaterial.PINK_STAINED_GLASS_PANE;
            else if (i >= 18) color = XMaterial.MAGENTA_STAINED_GLASS_PANE;
            else color = XMaterial.BLACK_STAINED_GLASS_PANE;
            addButton(i, new InventoryButton().creator(p -> ItemUtil.buildGlassPane(color)).consumer(e -> {}));
        }

        PhoneData phone = plugin.getPhoneManager().getPhone(player.getUniqueId());
        if (phone == null) { super.decorate(player); return; }

        List<PhoneContact> contacts = new ArrayList<>();
        for (PhoneContact c : phone.getContacts()) {
            if (searchFilter == null || c.getDisplayName().toLowerCase().contains(searchFilter.toLowerCase())
                    || c.getUsername().toLowerCase().contains(searchFilter.toLowerCase())) {
                contacts.add(c);
            }
        }

        int start = page * PER_PAGE;
        int end = Math.min(start + PER_PAGE, contacts.size());

        for (int i = start; i < end; i++) {
            PhoneContact contact = contacts.get(i);
            int slot = CONTENT_SLOTS[i - start];
            addButton(slot, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.PLAYER_HEAD, "\u00a7e" + contact.getDisplayName(),
                            "\u00a77Username: \u00a7f" + contact.getUsername(),
                            "",
                            "\u00a7eLeft-click to message",
                            "\u00a76Right-click to edit"))
                    .consumer(e -> {
                        Player p = (Player) e.getWhoClicked();
                        if (e.isRightClick()) {
                            plugin.getChatInputManager().requestInput(p, "\u00a7eEnter new display name for \u00a7b" + contact.getUsername() + "\u00a7e:", newName -> {
                                contact.setDisplayName(newName);
                                plugin.getPhoneManager().savePhone(phone);
                                p.sendMessage("\u00a7aContact updated to '\u00a7e" + newName + "\u00a7a'.");
                                plugin.getGUIManager().openGUI(new ContactsListGUI(plugin, page, searchFilter), p);
                            });
                        } else {
                            UUID targetUuid = plugin.getPhoneManager().getUuidByUsername(contact.getUsername());
                            if (targetUuid == null) {
                                p.sendMessage("\u00a7cThat contact's username is no longer valid.");
                                return;
                            }
                            PhoneConversation existing = plugin.getPhoneManager().findDirectConversation(p.getUniqueId(), targetUuid);
                            if (existing != null) {
                                plugin.getGUIManager().openGUI(new TexterChatGUI(plugin, existing.getConversationId(), 0), p);
                            } else {
                                List<UUID> members = List.of(p.getUniqueId(), targetUuid);
                                PhoneConversation conv = plugin.getPhoneManager().createConversation(contact.getDisplayName(), members, false);
                                plugin.getGUIManager().openGUI(new TexterChatGUI(plugin, conv.getConversationId(), 0), p);
                            }
                        }
                    })
            );
        }

        int totalPages = Math.max(1, (int) Math.ceil((double) contacts.size() / PER_PAGE));

        if (page > 0) {
            addButton(18, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7e\u00ab Previous Page"))
                    .consumer(e -> plugin.getGUIManager().openGUI(new ContactsListGUI(plugin, page - 1, searchFilter), (Player) e.getWhoClicked()))
            );
        }

        addButton(20, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.OAK_SIGN, "\u00a7eSearch",
                        "\u00a77Search contacts"))
                .consumer(e -> {
                    Player p = (Player) e.getWhoClicked();
                    plugin.getChatInputManager().requestInput(p, "\u00a7eEnter search term:", term ->
                            plugin.getGUIManager().openGUI(new ContactsListGUI(plugin, 0, term), p));
                })
        );

        addButton(21, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.LIME_DYE, "\u00a7a\u00a7lAdd Contact",
                        "\u00a77Add a new contact"))
                .consumer(e -> {
                    Player p = (Player) e.getWhoClicked();
                    int maxContacts = plugin.getSettings().getPhoneMaxContacts();
                    if (phone.getContacts().size() >= maxContacts) {
                        p.sendMessage("\u00a7cYou have reached the max contacts (" + maxContacts + ").");
                        return;
                    }
                    plugin.getChatInputManager().requestInput(p, "\u00a7eEnter the username of the contact:", username -> {
                        if (!plugin.getPhoneManager().isUsernameTaken(username)) {
                            p.sendMessage("\u00a7cNo phone found with username '\u00a7e" + username + "\u00a7c'.");
                            return;
                        }
                        if (username.equalsIgnoreCase(phone.getUsername())) {
                            p.sendMessage("\u00a7cYou can't add yourself as a contact.");
                            return;
                        }
                        for (PhoneContact existing : phone.getContacts()) {
                            if (existing.getUsername().equalsIgnoreCase(username)) {
                                p.sendMessage("\u00a7cYou already have that contact.");
                                return;
                            }
                        }
                        plugin.getChatInputManager().requestInput(p, "\u00a7eEnter a display name for this contact:", displayName -> {
                            PhoneContact c = new PhoneContact();
                            c.setUsername(username);
                            c.setDisplayName(displayName);
                            phone.getContacts().add(c);
                            plugin.getPhoneManager().savePhone(phone);
                            p.sendMessage("\u00a7aContact '\u00a7e" + displayName + "\u00a7a' added!");
                            plugin.getGUIManager().openGUI(new ContactsListGUI(plugin, page, searchFilter), p);
                        });
                    });
                })
        );

        addPageIndicator(22, page, totalPages);

        addButton(24, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7cBack"))
                .consumer(e -> plugin.getGUIManager().openGUI(new PhoneHomescreenGUI(plugin, 0), (Player) e.getWhoClicked()))
        );

        if (end < contacts.size()) {
            addButton(26, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7eNext Page \u00bb"))
                    .consumer(e -> plugin.getGUIManager().openGUI(new ContactsListGUI(plugin, page + 1, searchFilter), (Player) e.getWhoClicked()))
            );
        }

        super.decorate(player);
    }
}
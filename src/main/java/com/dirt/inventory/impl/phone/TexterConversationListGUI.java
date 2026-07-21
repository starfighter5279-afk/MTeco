package com.dirt.inventory.impl.phone;

import com.cryptomorin.xseries.XMaterial;
import com.dirt.DirtEconomy;
import com.dirt.data.PhoneConversation;
import com.dirt.data.PhoneData;
import com.dirt.data.PhoneMessage;
import com.dirt.inventory.InventoryButton;
import com.dirt.inventory.InventoryGUI;
import com.dirt.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TexterConversationListGUI extends InventoryGUI {
    private final DirtEconomy plugin;
    private final int page;
    private final String searchFilter;

    private static final int[] CONTENT_SLOTS = {1, 2, 3, 4, 5, 6, 7, 10, 11, 12, 13, 14, 15, 16};
    private static final int PER_PAGE = CONTENT_SLOTS.length;

    public TexterConversationListGUI(DirtEconomy plugin, int page, String searchFilter) {
        this.plugin = plugin;
        this.page = page;
        this.searchFilter = searchFilter;
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 27, "\u00a73\u00a7lTexter" + (searchFilter != null ? " \u00a77\u00a7o" + searchFilter : ""));
    }

    @Override
    public void decorate(Player player) {
        for (int i = 0; i < 27; i++) {
            final XMaterial color;
            if (i == 0 || i == 8 || i == 9 || i == 17) color = XMaterial.CYAN_STAINED_GLASS_PANE;
            else if (i >= 18) color = XMaterial.LIGHT_BLUE_STAINED_GLASS_PANE;
            else color = XMaterial.BLACK_STAINED_GLASS_PANE;
            addButton(i, new InventoryButton().creator(p -> ItemUtil.buildGlassPane(color)).consumer(e -> {}));
        }

        PhoneData phone = plugin.getPhoneManager().getPhone(player.getUniqueId());
        if (phone == null) { super.decorate(player); return; }

        List<PhoneConversation> conversations = new ArrayList<>();
        for (UUID convId : phone.getConversationIds()) {
            PhoneConversation conv = plugin.getPhoneManager().getConversation(convId);
            if (conv != null) {
                if (searchFilter != null && !conv.getName().toLowerCase().contains(searchFilter.toLowerCase())) continue;
                conversations.add(conv);
            }
        }

        conversations.sort((a, b) -> {
            long aTime = a.getMessages().isEmpty() ? 0 : a.getMessages().get(a.getMessages().size() - 1).getTimestamp();
            long bTime = b.getMessages().isEmpty() ? 0 : b.getMessages().get(b.getMessages().size() - 1).getTimestamp();
            return Long.compare(bTime, aTime);
        });

        int start = page * PER_PAGE;
        int end = Math.min(start + PER_PAGE, conversations.size());

        for (int i = start; i < end; i++) {
            PhoneConversation conv = conversations.get(i);
            int slot = CONTENT_SLOTS[i - start];
            String displayName = conv.getName();
            if (!conv.isGroupChat() && conv.getMemberUuids().size() == 2) {
                UUID otherUuid = conv.getMemberUuids().get(0).equals(player.getUniqueId()) ? conv.getMemberUuids().get(1) : conv.getMemberUuids().get(0);
                PhoneData otherPhone = plugin.getPhoneManager().getPhone(otherUuid);
                String otherUsername = otherPhone != null ? otherPhone.getUsername() : "Unknown";
                displayName = plugin.getPhoneManager().resolveDisplayName(player.getUniqueId(), otherUsername);
            }
            String lastMsg = "";
            if (!conv.getMessages().isEmpty()) {
                PhoneMessage last = conv.getMessages().get(conv.getMessages().size() - 1);
                String sender = plugin.getPhoneManager().resolveDisplayName(player.getUniqueId(), last.getSenderUsername());
                lastMsg = sender + ": " + (last.getContent().length() > 30 ? last.getContent().substring(0, 30) + "..." : last.getContent());
            }
            boolean isGroup = conv.isGroupChat();
            String finalDisplayName = displayName;
            String finalLastMsg = lastMsg;

            addButton(slot, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(isGroup ? XMaterial.BOOK : XMaterial.PAPER,
                            "\u00a7e" + finalDisplayName,
                            isGroup ? "\u00a77Group \u00b7 " + conv.getMemberUuids().size() + " members" : "\u00a77Direct Message",
                            finalLastMsg.isEmpty() ? "\u00a78No messages yet" : "\u00a77" + finalLastMsg,
                            "",
                            "\u00a7eClick to open"))
                    .consumer(e -> {
                        Player p = (Player) e.getWhoClicked();
                        plugin.getGUIManager().openGUI(new TexterChatGUI(plugin, conv.getConversationId(), 0), p);
                    })
            );
        }

        int totalPages = Math.max(1, (int) Math.ceil((double) conversations.size() / PER_PAGE));

        if (page > 0) {
            addButton(18, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7e\u00ab Previous Page"))
                    .consumer(e -> plugin.getGUIManager().openGUI(new TexterConversationListGUI(plugin, page - 1, searchFilter), (Player) e.getWhoClicked()))
            );
        }

        addButton(20, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.OAK_SIGN, "\u00a7eSearch",
                        "\u00a77Search conversations"))
                .consumer(e -> {
                    Player p = (Player) e.getWhoClicked();
                    plugin.getChatInputManager().requestInput(p, "\u00a7eEnter search term:", term ->
                            plugin.getGUIManager().openGUI(new TexterConversationListGUI(plugin, 0, term), p));
                })
        );

        addButton(21, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.LIME_DYE, "\u00a7a\u00a7lNew Chat",
                        "\u00a77Start a conversation"))
                .consumer(e -> {
                    Player p = (Player) e.getWhoClicked();
                    plugin.getChatInputManager().requestInput(p,
                            "\u00a7eEnter the username of the person to message \u00a77(or multiple, comma-separated):",
                            input -> handleCreateConversation(p, input));
                })
        );

        addPageIndicator(22, page, totalPages);

        addButton(24, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7cBack"))
                .consumer(e -> plugin.getGUIManager().openGUI(new PhoneHomescreenGUI(plugin, 0), (Player) e.getWhoClicked()))
        );

        if (end < conversations.size()) {
            addButton(26, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7eNext Page \u00bb"))
                    .consumer(e -> plugin.getGUIManager().openGUI(new TexterConversationListGUI(plugin, page + 1, searchFilter), (Player) e.getWhoClicked()))
            );
        }

        super.decorate(player);
    }

    private void handleCreateConversation(Player player, String input) {
        String[] usernames = input.split(",");
        List<UUID> memberUuids = new ArrayList<>();
        memberUuids.add(player.getUniqueId());

        for (String raw : usernames) {
            String username = raw.trim();
            UUID targetUuid = plugin.getPhoneManager().getUuidByUsername(username);
            if (targetUuid == null) {
                player.sendMessage("\u00a7cUsername '\u00a7e" + username + "\u00a7c' not found.");
                return;
            }
            if (targetUuid.equals(player.getUniqueId())) continue;
            if (!memberUuids.contains(targetUuid)) memberUuids.add(targetUuid);
        }

        if (memberUuids.size() < 2) {
            player.sendMessage("\u00a7cYou need at least one other person to start a conversation.");
            return;
        }

        if (memberUuids.size() == 2) {
            PhoneConversation existing = plugin.getPhoneManager().findDirectConversation(memberUuids.get(0), memberUuids.get(1));
            if (existing != null) {
                plugin.getGUIManager().openGUI(new TexterChatGUI(plugin, existing.getConversationId(), 0), player);
                return;
            }
            PhoneData otherPhone = plugin.getPhoneManager().getPhone(memberUuids.get(1));
            String convName = otherPhone != null ? otherPhone.getUsername() : "Chat";
            PhoneConversation conv = plugin.getPhoneManager().createConversation(convName, memberUuids, false);
            plugin.getGUIManager().openGUI(new TexterChatGUI(plugin, conv.getConversationId(), 0), player);
            return;
        }

        plugin.getChatInputManager().requestInput(player, "\u00a7eEnter a name for this group chat:", name -> {
            PhoneConversation conv = plugin.getPhoneManager().createConversation(name, memberUuids, true);
            player.sendMessage("\u00a7aGroup chat '\u00a7e" + name + "\u00a7a' created!");
            plugin.getGUIManager().openGUI(new TexterChatGUI(plugin, conv.getConversationId(), 0), player);
        });
    }
}
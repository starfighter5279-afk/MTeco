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
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class TexterChatGUI extends InventoryGUI {
    private final DirtEconomy plugin;
    private final UUID conversationId;
    private final int page;

    public TexterChatGUI(DirtEconomy plugin, UUID conversationId, int page) {
        this.plugin = plugin;
        this.conversationId = conversationId;
        this.page = page;
    }

    @Override
    protected Inventory createInventory() {
        PhoneConversation conv = plugin.getPhoneManager().getConversation(conversationId);
        String title = conv != null ? conv.getName() : "Chat";
        if (title.length() > 20) title = title.substring(0, 20) + "...";
        return Bukkit.createInventory(null, 54, "\u00a73\u00a7l" + title);
    }

    @Override
    public void decorate(Player player) {
        fillPagedGui(54, XMaterial.LIGHT_BLUE_STAINED_GLASS_PANE);

        PhoneConversation conv = plugin.getPhoneManager().getConversation(conversationId);
        if (conv == null) { super.decorate(player); return; }

        plugin.getPhoneManager().enterConversation(player.getUniqueId());

        List<PhoneMessage> messages = conv.getMessages();
        int perPage = 45;
        int totalPages = Math.max(1, (int) Math.ceil((double) messages.size() / perPage));
        int effectivePage = Math.min(page, totalPages - 1);
        int start = effectivePage * perPage;
        int end = Math.min(start + perPage, messages.size());

        for (int i = start; i < end; i++) {
            PhoneMessage msg = messages.get(i);
            int slot = i - start;
            boolean isMine = msg.getSenderUuid().equals(player.getUniqueId());
            String displayName = plugin.getPhoneManager().resolveDisplayName(player.getUniqueId(), msg.getSenderUsername());
            String dateStr = new SimpleDateFormat("HH:mm").format(new Date(msg.getTimestamp()));

            addButton(slot, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(isMine ? XMaterial.LIME_DYE : XMaterial.LIGHT_BLUE_DYE,
                            (isMine ? "\u00a7a" : "\u00a7b") + displayName + " \u00a78" + dateStr,
                            "\u00a7f" + msg.getContent()))
                    .consumer(e -> {})
            );
        }

        addButton(47, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.WRITABLE_BOOK, "\u00a7a\u00a7lSend Message",
                        "\u00a77Click to type a message"))
                .consumer(e -> {
                    Player p = (Player) e.getWhoClicked();
                    PhoneData phone = plugin.getPhoneManager().getPhone(p.getUniqueId());
                    String username = phone != null ? phone.getUsername() : "Unknown";
                    plugin.getChatInputManager().requestInput(p, "\u00a73[\u00a7bTexter\u00a73] \u00a7eType your message:", message -> {
                        plugin.getPhoneManager().sendMessage(conversationId, p.getUniqueId(), username, message);
                        p.sendMessage("\u00a73[\u00a7bTexter\u00a73] \u00a7aMessage sent!");
                        plugin.getGUIManager().openGUI(new TexterChatGUI(plugin, conversationId, totalPages - 1), p);
                    });
                })
        );

        addButton(46, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.RED_DYE, "\u00a7c\u00a7lLeave Chat",
                        "\u00a77Leave this conversation"))
                .consumer(e -> {
                    Player p = (Player) e.getWhoClicked();
                    conv.getMemberUuids().remove(p.getUniqueId());
                    PhoneData phone = plugin.getPhoneManager().getPhone(p.getUniqueId());
                    if (phone != null) {
                        phone.getConversationIds().remove(conversationId);
                        plugin.getPhoneManager().savePhone(phone);
                    }
                    plugin.getPhoneManager().saveConversation(conv);
                    plugin.getPhoneManager().leaveConversation(p.getUniqueId());
                    p.sendMessage("\u00a7eYou left the conversation.");
                    plugin.getGUIManager().openGUI(new TexterConversationListGUI(plugin, 0, null), p);
                })
        );

        if (effectivePage > 0) {
            addButton(45, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7eOlder Messages"))
                    .consumer(e -> plugin.getGUIManager().openGUI(new TexterChatGUI(plugin, conversationId, effectivePage - 1), (Player) e.getWhoClicked()))
            );
        }
        if (end < messages.size()) {
            addButton(53, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7eNewer Messages"))
                    .consumer(e -> plugin.getGUIManager().openGUI(new TexterChatGUI(plugin, conversationId, effectivePage + 1), (Player) e.getWhoClicked()))
            );
        }
        addPageIndicator(48, effectivePage, totalPages);

        addButton(49, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7cBack"))
                .consumer(e -> {
                    plugin.getPhoneManager().leaveConversation(player.getUniqueId());
                    plugin.getGUIManager().openGUI(new TexterConversationListGUI(plugin, 0, null), (Player) e.getWhoClicked());
                })
        );

        super.decorate(player);
    }

    @Override
    public void onClose(InventoryCloseEvent event) {
        plugin.getPhoneManager().leaveConversation(event.getPlayer().getUniqueId());
    }
}
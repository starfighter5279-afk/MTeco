package com.mteco.inventory.impl;

import com.cryptomorin.xseries.XMaterial;
import com.mteco.MTeco;
import com.mteco.data.CharacterData;
import com.mteco.data.FamilyData;
import com.mteco.data.MailItem;
import com.mteco.inventory.InventoryButton;
import com.mteco.inventory.InventoryGUI;
import com.mteco.util.CurrencyUtil;
import com.mteco.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FamilyGUI extends InventoryGUI {
    private final MTeco plugin;

    public FamilyGUI(MTeco plugin) {
        this.plugin = plugin;
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 27, "\u00a7dFamily");
    }

    @Override
    public void decorate(Player player) {
        CharacterData data = plugin.getCharacterManager().getCharacter(player.getUniqueId());

        fillGlass(27, XMaterial.PINK_STAINED_GLASS_PANE);

        String role = deriveRole(data);
        boolean isMarried = "PRIMARY".equals(role) || "SECONDARY".equals(role);
        boolean isChild = "CHILD".equals(role);
        boolean hasBirthFamily = data != null && data.getBirthFamilyId() != null;
        boolean hasPreviousChildren = data != null && !data.getPreviousFamilyIds().isEmpty();

        double marriageCost = plugin.getSettings().getMarriageCost();
        String costStr = String.format("%.0f", marriageCost);

        if (data == null || data.getFamilyId() == null) {
            // No family at all
            addButton(13, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.HEART_OF_THE_SEA, "\u00a7aCreate Family",
                            "\u00a77Start a family by selecting a spouse.",
                            "\u00a77Cost: \u00a7e" + CurrencyUtil.symbol() + costStr))
                    .consumer(e -> plugin.getGUIManager().openGUI(new SpouseSelectionGUI(plugin, 0), (Player) e.getWhoClicked()))
            );
            if (hasPreviousChildren) {
                addButton(22, new InventoryButton()
                        .creator(p -> ItemUtil.buildItem(XMaterial.BOOK, "\u00a7ePrevious Children",
                                "\u00a77View children from your previous families."))
                        .consumer(e -> plugin.getGUIManager().openGUI(new PreviousFamilyChildrenGUI(plugin, 0), (Player) e.getWhoClicked()))
                );
            }
        } else if (isChild) {
            decorateChildView(player, data, hasBirthFamily);
        } else if (isMarried) {
            decorateMarriedView(player, data, hasBirthFamily);
        }

        addButton(26, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7cBack"))
                .consumer(e -> plugin.getGUIManager().openGUI(new CharacterManagementGUI(plugin), (Player) e.getWhoClicked()))
        );

        super.decorate(player);
    }

    private void decorateChildView(Player player, CharacterData data, boolean hasBirthFamily) {
        long firstJoin = data.getFirstJoinDate();
        long eligibilityMs = plugin.getSettings().getMarriageEligibilityDays() * 24L * 60 * 60 * 1000L;
        boolean eligible = firstJoin > 0 && (System.currentTimeMillis() - firstJoin) >= eligibilityMs;

        FamilyData parentFamily = plugin.getFamilyManager().loadFamily(data.getFamilyId());
        String parentNames = "\u00a77Unknown";
        if (parentFamily != null) {
            String p1 = getCharacterName(parentFamily.getSpouse1());
            String p2 = getCharacterName(parentFamily.getSpouse2());
            parentNames = "\u00a77" + p1 + (p2 != null ? " & " + p2 : "");
        }
        final String finalParentNames = parentNames;

        addButton(4, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.HEART_OF_THE_SEA, "\u00a7dBirth Family",
                        "\u00a77You are a child in this family.",
                        "\u00a77Parents: " + finalParentNames))
                .consumer(e -> {})
        );

        double marriageCost = plugin.getSettings().getMarriageCost();
        String costStr = String.format("%.0f", marriageCost);
        int eligibilityDays = plugin.getSettings().getMarriageEligibilityDays();

        if (eligible) {
            addButton(13, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.GOLD_INGOT, "\u00a7dMarriage",
                            "\u00a77You are eligible to start your own family!",
                            "\u00a77Click to find a spouse.",
                            "\u00a77Cost: \u00a7e" + CurrencyUtil.symbol() + costStr))
                    .consumer(e -> plugin.getGUIManager().openGUI(new SpouseSelectionGUI(plugin, 0), (Player) e.getWhoClicked()))
            );
        } else {
            long remaining = firstJoin > 0 ? (eligibilityMs - (System.currentTimeMillis() - firstJoin)) : eligibilityMs;
            long daysLeft = Math.max(1, remaining / (24 * 60 * 60 * 1000L));
            addButton(13, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.CLOCK, "\u00a77Marriage (Locked)",
                            "\u00a77You must be on the server for " + eligibilityDays + " days",
                            "\u00a77before you can start your own family.",
                            "\u00a77Days remaining: \u00a7e" + daysLeft))
                    .consumer(e -> {})
            );
        }
    }

    private void decorateMarriedView(Player player, CharacterData data, boolean hasBirthFamily) {
        FamilyData family = data.getFamilyId() != null ? plugin.getFamilyManager().loadFamily(data.getFamilyId()) : null;
        UUID spouseUUID = null;
        if (family != null) {
            spouseUUID = player.getUniqueId().equals(family.getSpouse1()) ? family.getSpouse2() : family.getSpouse1();
        }
        String spouseName = getCharacterName(spouseUUID);

        final String finalSpouseName = spouseName != null ? spouseName : "Unknown";

        addButton(4, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.HEART_OF_THE_SEA, "\u00a7d\u2764 Married",
                        "\u00a77Spouse: \u00a7f" + finalSpouseName))
                .consumer(e -> {})
        );

        addButton(10, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.PINK_DYE, "\u00a7dBear a Child",
                        "\u00a77Send a request to your spouse to add a child."))
                .consumer(e -> sendChildBearingRequest((Player) e.getWhoClicked(), data))
        );

        addButton(13, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.GOLD_INGOT, "\u00a7eManage Inheritor",
                        "\u00a77Select a family member as your inheritor."))
                .consumer(e -> plugin.getGUIManager().openGUI(new InheritorGUI(plugin), (Player) e.getWhoClicked()))
        );

        double divorcePayout = plugin.getSettings().getDivorcePayout();
        String payoutStr = String.format("%.0f", divorcePayout);

        addButton(16, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.SHEARS, "\u00a7cDivorce Family",
                        "\u00a77Remove your spouse from the family.",
                        "\u00a77Your spouse receives \u00a7e" + CurrencyUtil.symbol() + payoutStr + "\u00a77."))
                .consumer(e -> plugin.getGUIManager().openGUI(new DivorceConfirmGUI(plugin), (Player) e.getWhoClicked()))
        );

        if (hasBirthFamily) {
            addButton(22, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.BOOK, "\u00a7eBirth Family",
                            "\u00a77View the family you were born into."))
                    .consumer(e -> plugin.getGUIManager().openGUI(new BirthFamilyGUI(plugin), (Player) e.getWhoClicked()))
            );
        }
    }

    private void sendChildBearingRequest(Player player, CharacterData data) {
        FamilyData family = plugin.getFamilyManager().loadFamily(data.getFamilyId());
        if (family == null) { player.sendMessage("\u00a7cFamily data not found."); return; }

        UUID spouseUUID = player.getUniqueId().equals(family.getSpouse1()) ? family.getSpouse2() : family.getSpouse1();
        if (spouseUUID == null) { player.sendMessage("\u00a7cYour spouse could not be found."); return; }

        CharacterData spouseData = plugin.getCharacterManager().getCharacter(spouseUUID);
        String spouseName = spouseData != null ? spouseData.getFirstName() + " " + spouseData.getLastName() : "your spouse";

        MailItem mail = new MailItem();
        mail.setId(UUID.randomUUID().toString());
        mail.setType("CHILD_BEARING_REQUEST");
        mail.setFromPlayerUuid(player.getUniqueId());
        mail.setTimestamp(System.currentTimeMillis());
        Map<String, String> mailData = new HashMap<>();
        mailData.put("fromName", data.getFirstName() + " " + data.getLastName());
        mail.setData(mailData);

        plugin.getCharacterManager().addMailItem(spouseUUID, mail);
        player.sendMessage("\u00a7aChild bearing request sent to \u00a7e" + spouseName + "\u00a7a.");
        player.closeInventory();

        Player spousePlayer = Bukkit.getPlayer(spouseUUID);
        if (spousePlayer != null) {
            spousePlayer.sendMessage("\u00a7eYou have received a child bearing request from \u00a7f" + data.getFirstName() + " " + data.getLastName() + "\u00a7e. Check your mailbox (/mtc).");
        }
    }

    private String deriveRole(CharacterData data) {
        if (data == null || data.getFamilyId() == null) return null;
        if (data.getFamilyRole() != null) return data.getFamilyRole();
        FamilyData family = plugin.getFamilyManager().loadFamily(data.getFamilyId());
        if (family == null) return null;
        if (data.getPlayerUuid().equals(family.getSpouse1())) return "PRIMARY";
        if (data.getPlayerUuid().equals(family.getSpouse2())) return "SECONDARY";
        if (family.getChildren().contains(data.getPlayerUuid())) return "CHILD";
        return null;
    }

    private String getCharacterName(UUID uuid) {
        if (uuid == null) return null;
        CharacterData d = plugin.getCharacterManager().getCharacter(uuid);
        return d != null ? d.getFirstName() + " " + d.getLastName() : null;
    }
}
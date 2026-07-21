package com.dirt.inventory.impl;

import com.cryptomorin.xseries.XMaterial;
import com.dirt.DirtEconomy;
import com.dirt.data.CharacterData;
import com.dirt.data.FamilyData;
import com.dirt.data.MailItem;
import com.dirt.data.NationData;
import com.dirt.managers.NationManager;
import com.dirt.data.BusinessData;
import com.dirt.data.ContractData;
import com.dirt.managers.BusinessManager;
import com.dirt.managers.ContractManager;
import com.dirt.inventory.InventoryButton;
import com.dirt.inventory.InventoryGUI;
import com.dirt.util.CurrencyUtil;
import com.dirt.util.ItemUtil;
import com.dirt.inventory.impl.contract.ContractViewGUI;
import com.dirt.inventory.impl.contract.ContractBehalfOfGUI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

public class MailItemGUI extends InventoryGUI {
    private final DirtEconomy plugin;
    private final MailItem mail;

    public MailItemGUI(DirtEconomy plugin, MailItem mail) {
        this.plugin = plugin;
        this.mail = mail;
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 27, "\u00a7bMail");
    }

    @Override
    public void decorate(Player player) {
        fillGlass(27, XMaterial.LIGHT_BLUE_STAINED_GLASS_PANE);

        switch (mail.getType()) {
            case "MARRIAGE_REQUEST" -> decorateMarriageRequest(player);
            case "CHILD_BEARING_REQUEST" -> decorateChildBearingRequest(player);
            case "CHILD_JOIN_REQUEST" -> decorateChildJoinRequest(player);
            case "NATION_INVITE" -> decorateNationInvite(player);
            case "JOB_OFFER" -> decorateJobOffer(player);
            case "JOB_APPLICATION" -> decorateJobApplication(player);
            case "BUSINESS_SALE_REQUEST" -> decorateBusinessSaleRequest(player);
            case "PROPERTY_ENTRY" -> decoratePropertyEntry(player);
            case "CONTRACT_OFFER", "CONTRACT_SIGN_REQUEST" -> decorateContractOffer(player);
            case "CONTRACT_BROKEN" -> decorateContractBroken(player);
            case "CONTRACT_MODIFICATION_REQUEST" -> decorateModificationRequest(player);
        }

        addButton(26, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7cBack"))
                .consumer(e -> plugin.getGUIManager().openGUI(new MailGUI(plugin, 0), (Player) e.getWhoClicked()))
        );

        super.decorate(player);
    }

    private void decorateMarriageRequest(Player player) {
        String fromName = mail.getData().getOrDefault("fromName", "Unknown");
        String birthDateStr;
        try {
            long bd = Long.parseLong(mail.getData().getOrDefault("fromBirthDate", "0"));
            birthDateStr = new SimpleDateFormat("MM/dd/yyyy").format(new Date(bd));
        } catch (NumberFormatException ignored) {
            birthDateStr = "Unknown";
        }
        String balanceStr = mail.getData().getOrDefault("fromBalance", "0");
        String formattedBalance;
        try {
            formattedBalance = CurrencyUtil.symbol() + String.format("%.0f", Double.parseDouble(balanceStr));
        } catch (NumberFormatException e) {
            formattedBalance = CurrencyUtil.symbol() + "0";
        }

        final String finalBirthDateStr = birthDateStr;
        final String finalFormattedBalance = formattedBalance;

        addButton(4, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.GOLD_INGOT, "\u00a7dMarriage Request",
                        "\u00a77From: \u00a7f" + fromName,
                        "\u00a77Their birthday: \u00a7f" + finalBirthDateStr,
                        "\u00a77Their balance: \u00a7e" + finalFormattedBalance,
                        "\u00a77Accepting this marriage will cause you to",
                        "\u00a77inherit this character's last name, and give",
                        "\u00a77access to their belongings. As well as create",
                        "\u00a77the ability to have children with this character."))
                .consumer(e -> {})
        );

        addButton(11, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.LIME_WOOL, "\u00a7aAccept Marriage"))
                .consumer(e -> acceptMarriage((Player) e.getWhoClicked()))
        );

        addButton(15, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.RED_WOOL, "\u00a7cDeny"))
                .consumer(e -> {
                    Player p = (Player) e.getWhoClicked();
                    plugin.getCharacterManager().removeMailItem(p.getUniqueId(), mail.getId());
                    p.sendMessage("\u00a7cYou denied the marriage request.");
                    plugin.getGUIManager().openGUI(new MailGUI(plugin, 0), p);
                })
        );
    }

    private void acceptMarriage(Player player) {
        UUID fromUUID = mail.getFromPlayerUuid();
        if (fromUUID == null) { player.sendMessage("\u00a7cInvalid mail data."); return; }

        CharacterData myData = plugin.getCharacterManager().getCharacter(player.getUniqueId());
        CharacterData fromData = plugin.getCharacterManager().getCharacter(fromUUID);

        if (myData == null || fromData == null) {
            player.sendMessage("\u00a7cCharacter data not found.");
            return;
        }

        boolean myBlocked = myData.getFamilyId() != null && !"CHILD".equals(myData.getFamilyRole());
        boolean fromBlocked = fromData.getFamilyId() != null && !"CHILD".equals(fromData.getFamilyRole());

        if (myBlocked) {
            player.sendMessage("\u00a7cYou are already married.");
            plugin.getCharacterManager().removeMailItem(player.getUniqueId(), mail.getId());
            return;
        }
        if (fromBlocked) {
            player.sendMessage("\u00a7cThe sender is already married. Request cancelled.");
            plugin.getCharacterManager().removeMailItem(player.getUniqueId(), mail.getId());
            return;
        }

        FamilyData newFamily = plugin.getFamilyManager().createFamily(fromUUID, player.getUniqueId());

        myData.setLastName(fromData.getLastName());

        if ("CHILD".equals(fromData.getFamilyRole())) {
            fromData.setBirthFamilyId(fromData.getFamilyId());
        }
        fromData.setFamilyId(newFamily.getFamilyId());
        fromData.setFamilyRole("PRIMARY");

        if ("CHILD".equals(myData.getFamilyRole())) {
            myData.setBirthFamilyId(myData.getFamilyId());
        }
        myData.setFamilyId(newFamily.getFamilyId());
        myData.setFamilyRole("SECONDARY");

        plugin.getCharacterManager().saveCharacter(myData);
        plugin.getCharacterManager().saveCharacter(fromData);
        plugin.getCharacterManager().removeMailItem(player.getUniqueId(), mail.getId());

        player.sendMessage("\u00a7aYou are now married to \u00a7e" + fromData.getFirstName() + " " + fromData.getLastName() + "\u00a7a! Your last name is now \u00a7e" + myData.getLastName() + "\u00a7a.");

        Player fromPlayer = Bukkit.getPlayer(fromUUID);
        if (fromPlayer != null) {
            fromPlayer.sendMessage("\u00a7a" + myData.getFirstName() + " " + myData.getLastName() + " accepted your marriage request! You are now married!");
        }

        plugin.getGUIManager().openGUI(new MailGUI(plugin, 0), player);
    }

    private void decorateChildBearingRequest(Player player) {
        String fromName = mail.getData().getOrDefault("fromName", "Unknown");

        addButton(4, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.PINK_DYE, "\u00a7aBear a Child Request",
                        "\u00a77From: \u00a7f" + fromName,
                        "\u00a77Your spouse wants to add a child to your family.",
                        "\u00a77If you accept, your spouse will select a character",
                        "\u00a77to become your child."))
                .consumer(e -> {})
        );

        addButton(11, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.LIME_WOOL, "\u00a7aAccept"))
                .consumer(e -> {
                    Player p = (Player) e.getWhoClicked();
                    UUID fromUUID = mail.getFromPlayerUuid();
                    plugin.getCharacterManager().removeMailItem(p.getUniqueId(), mail.getId());

                    Player fromPlayer = Bukkit.getPlayer(fromUUID);
                    if (fromPlayer != null) {
                        fromPlayer.sendMessage("\u00a7eYour spouse accepted! Please select a child for your family.");
                        plugin.getGUIManager().openGUI(new ChildSelectionGUI(plugin, 0), fromPlayer);
                    } else {
                        CharacterData fromData = plugin.getCharacterManager().getCharacter(fromUUID);
                        if (fromData != null) {
                            fromData.setPendingChildSelection(true);
                            plugin.getCharacterManager().saveCharacter(fromData);
                        }
                    }

                    p.sendMessage("\u00a7aYou accepted the child bearing request.");
                    plugin.getGUIManager().openGUI(new MailGUI(plugin, 0), p);
                })
        );

        addButton(15, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.RED_WOOL, "\u00a7cDeny"))
                .consumer(e -> {
                    Player p = (Player) e.getWhoClicked();
                    plugin.getCharacterManager().removeMailItem(p.getUniqueId(), mail.getId());
                    p.sendMessage("\u00a7cYou denied the child bearing request.");
                    plugin.getGUIManager().openGUI(new MailGUI(plugin, 0), p);
                })
        );
    }

    private void decorateChildJoinRequest(Player player) {
        String fromName = mail.getData().getOrDefault("fromName", "Unknown");
        String familyIdStr = mail.getData().getOrDefault("familyId", "");

        addButton(4, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.HEART_OF_THE_SEA, "\u00a7aFamily Join Request",
                        "\u00a77From: \u00a7f" + fromName,
                        "\u00a77" + fromName + " wants you to become",
                        "\u00a77a child member of their family."))
                .consumer(e -> {})
        );

        addButton(11, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.LIME_WOOL, "\u00a7aAccept"))
                .consumer(e -> {
                    Player p = (Player) e.getWhoClicked();
                    if (familyIdStr.isEmpty()) {
                        p.sendMessage("\u00a7cInvalid family data.");
                        return;
                    }

                    CharacterData myData = plugin.getCharacterManager().getCharacter(p.getUniqueId());
                    if (myData == null) return;
                    if (myData.getFamilyId() != null) {
                        p.sendMessage("\u00a7cYou are already in a family.");
                        plugin.getCharacterManager().removeMailItem(p.getUniqueId(), mail.getId());
                        return;
                    }

                    UUID familyId;
                    try {
                        familyId = UUID.fromString(familyIdStr);
                    } catch (IllegalArgumentException ex) {
                        p.sendMessage("\u00a7cInvalid family ID.");
                        return;
                    }

                    FamilyData family = plugin.getFamilyManager().loadFamily(familyId);
                    if (family == null) {
                        p.sendMessage("\u00a7cThis family no longer exists.");
                        plugin.getCharacterManager().removeMailItem(p.getUniqueId(), mail.getId());
                        return;
                    }

                    family.getChildren().add(p.getUniqueId());
                    plugin.getFamilyManager().saveFamily(family);

                    myData.setFamilyId(familyId);
                    myData.setFamilyRole("CHILD");
                    setDefaultInheritor(family, p.getUniqueId());
                    plugin.getCharacterManager().saveCharacter(myData);
                    plugin.getCharacterManager().removeMailItem(p.getUniqueId(), mail.getId());

                    p.sendMessage("\u00a7aYou have joined the family of \u00a7e" + fromName + "\u00a7a!");

                    Player fromPlayer = Bukkit.getPlayer(mail.getFromPlayerUuid());
                    if (fromPlayer != null) {
                        fromPlayer.sendMessage("\u00a7e" + myData.getFirstName() + " " + myData.getLastName() + " has joined your family as a child!");
                    }

                    plugin.getGUIManager().openGUI(new MailGUI(plugin, 0), p);
                })
        );

        addButton(15, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.RED_WOOL, "\u00a7cDeny"))
                .consumer(e -> {
                    Player p = (Player) e.getWhoClicked();
                    plugin.getCharacterManager().removeMailItem(p.getUniqueId(), mail.getId());
                    p.sendMessage("\u00a7cYou denied the family join request.");
                    plugin.getGUIManager().openGUI(new MailGUI(plugin, 0), p);
                })
        );
    }

    private void decorateNationInvite(Player player) {
        String nationName = mail.getData().getOrDefault("nationName", "Unknown");
        String inviterName = mail.getData().getOrDefault("inviterName", "Unknown");
        String nationIdStr = mail.getData().getOrDefault("nationId", "");

        addButton(4, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.GOLD_BLOCK, "\u00a7fNation Invite",
                        "\u00a7fNation: \u00a7f" + nationName,
                        "\u00a7fInvited by: \u00a7f" + inviterName,
                        "\u00a7fAccepting this will make you a member",
                        "\u00a7fof the " + nationName + " nation."))
                .consumer(e -> {})
        );

        addButton(11, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.LIME_WOOL, "\u00a7aAccept"))
                .consumer(e -> {
                    Player p = (Player) e.getWhoClicked();
                    if (nationIdStr.isEmpty() || !plugin.isDirtNationsEnabled()) {
                        p.sendMessage("\u00a7cInvalid invite.");
                        return;
                    }
                    UUID nationId;
                    try { nationId = UUID.fromString(nationIdStr); } catch (IllegalArgumentException ex) {
                        p.sendMessage("\u00a7cInvalid nation ID."); return;
                    }
                    NationManager nm = plugin.getNationManager();
                    NationData existingNation = nm.getNationByMember(p.getUniqueId());
                    if (existingNation != null) {
                        p.sendMessage("\u00a7cYou are already a member of \u00a7e" + existingNation.getName() + "\u00a7c. You must leave that nation before joining another.");
                        plugin.getCharacterManager().removeMailItem(p.getUniqueId(), mail.getId());
                        plugin.getGUIManager().openGUI(new MailGUI(plugin, 0), p);
                        return;
                    }
                    NationData nation = nm.loadNation(nationId);
                    if (nation == null) {
                        p.sendMessage("\u00a7cThis nation no longer exists.");
                        plugin.getCharacterManager().removeMailItem(p.getUniqueId(), mail.getId());
                        plugin.getGUIManager().openGUI(new MailGUI(plugin, 0), p);
                        return;
                    }
                    if (!nation.getMemberUUIDs().contains(p.getUniqueId())) {
                        nation.getMemberUUIDs().add(p.getUniqueId());
                        nm.saveNation(nation);
                    }
                    plugin.getCharacterManager().removeMailItem(p.getUniqueId(), mail.getId());
                    p.sendMessage("\u00a7aYou have joined the " + nation.getColor1() + nation.getName() + "\u00a7a!");
                    plugin.getGUIManager().openGUI(new MailGUI(plugin, 0), p);
                })
        );

        addButton(15, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.RED_WOOL, "\u00a7cDeny"))
                .consumer(e -> {
                    Player p = (Player) e.getWhoClicked();
                    plugin.getCharacterManager().removeMailItem(p.getUniqueId(), mail.getId());
                    p.sendMessage("\u00a7cYou declined the nation invite.");
                    plugin.getGUIManager().openGUI(new MailGUI(plugin, 0), p);
                })
        );
    }

    private void decorateJobOffer(Player player) {
        String businessId = mail.getData().getOrDefault("businessId", "");
        String businessName = mail.getData().getOrDefault("businessName", "Unknown");
        String payrollStr = mail.getData().getOrDefault("payrollRate", "0");
        String ownerName = mail.getData().getOrDefault("ownerName", "Unknown");

        addButton(4, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.GOLD_INGOT, "\u00a76Job Offer",
                        "\u00a77Business: \u00a7f" + businessName,
                        "\u00a77Owner: \u00a7f" + ownerName,
                        "\u00a77Weekly Pay: \u00a7e" + CurrencyUtil.symbol() + payrollStr,
                        "\u00a77Accepting makes you an employee."))
                .consumer(e -> {})
        );

        addButton(11, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.LIME_WOOL, "\u00a7aAccept"))
                .consumer(e -> {
                    Player p = (Player) e.getWhoClicked();
                    if (businessId.isEmpty() || !plugin.isDirtBusinessEnabled()) {
                        p.sendMessage("\u00a7cInvalid job offer."); return;
                    }
                    UUID bizId;
                    try { bizId = UUID.fromString(businessId); } catch (IllegalArgumentException ex) {
                        p.sendMessage("\u00a7cInvalid business ID."); return;
                    }
                    BusinessData biz = plugin.getBusinessManager().loadBusiness(bizId);
                    if (biz == null) {
                        p.sendMessage("\u00a7cThis business no longer exists.");
                        plugin.getCharacterManager().removeMailItem(p.getUniqueId(), mail.getId());
                        plugin.getGUIManager().openGUI(new MailGUI(plugin, 0), p);
                        return;
                    }
                    if (!biz.getEmployeeUUIDs().contains(p.getUniqueId())) {
                        biz.getEmployeeUUIDs().add(p.getUniqueId());
                        com.dirt.data.BusinessRole defaultRole = biz.getDefaultRole();
                        if (defaultRole != null && !defaultRole.getMemberUUIDs().contains(p.getUniqueId())) {
                            defaultRole.getMemberUUIDs().add(p.getUniqueId());
                        }
                        plugin.getBusinessManager().saveBusiness(biz);
                    }
                    plugin.getCharacterManager().removeMailItem(p.getUniqueId(), mail.getId());
                    p.sendMessage("\u00a7aYou accepted the job offer from \u00a76" + businessName + "\u00a7a!");
                    Player owner = plugin.getServer().getPlayer(mail.getFromPlayerUuid());
                    if (owner != null) owner.sendMessage("\u00a7e" + p.getName() + " accepted your job offer at \u00a76" + businessName + "\u00a7e!");
                    plugin.getGUIManager().openGUI(new MailGUI(plugin, 0), p);
                })
        );

        addButton(15, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.RED_WOOL, "\u00a7cDeny"))
                .consumer(e -> {
                    Player p = (Player) e.getWhoClicked();
                    plugin.getCharacterManager().removeMailItem(p.getUniqueId(), mail.getId());
                    p.sendMessage("\u00a7cYou declined the job offer from " + businessName + ".");
                    plugin.getGUIManager().openGUI(new MailGUI(plugin, 0), p);
                })
        );
    }

    private void decorateJobApplication(Player player) {
        String businessId = mail.getData().getOrDefault("businessId", "");
        String businessName = mail.getData().getOrDefault("businessName", "Unknown");
        String applicantName = mail.getData().getOrDefault("applicantName", "Unknown");

        addButton(4, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.PAPER, "\u00a73Job Application",
                        "\u00a77Business: \u00a7f" + businessName,
                        "\u00a77Applicant: \u00a7f" + applicantName,
                        "\u00a77" + applicantName + " is applying at your business."))
                .consumer(e -> {})
        );

        addButton(11, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.LIME_WOOL, "\u00a7aAccept Application"))
                .consumer(e -> {
                    Player p = (Player) e.getWhoClicked();
                    if (businessId.isEmpty() || !plugin.isDirtBusinessEnabled()) {
                        p.sendMessage("\u00a7cInvalid application."); return;
                    }
                    UUID bizId;
                    try { bizId = UUID.fromString(businessId); } catch (IllegalArgumentException ex) {
                        p.sendMessage("\u00a7cInvalid business ID."); return;
                    }
                    BusinessData biz = plugin.getBusinessManager().loadBusiness(bizId);
                    if (biz == null || !biz.getOwnerUUID().equals(p.getUniqueId())) {
                        p.sendMessage("\u00a7cYou are no longer the owner of this business.");
                        plugin.getCharacterManager().removeMailItem(p.getUniqueId(), mail.getId());
                        plugin.getGUIManager().openGUI(new MailGUI(plugin, 0), p);
                        return;
                    }
                    UUID applicantUUID = mail.getFromPlayerUuid();
                    if (!biz.getEmployeeUUIDs().contains(applicantUUID)) {
                        biz.getEmployeeUUIDs().add(applicantUUID);
                        com.dirt.data.BusinessRole defaultRole = biz.getDefaultRole();
                        if (defaultRole != null && !defaultRole.getMemberUUIDs().contains(applicantUUID)) {
                            defaultRole.getMemberUUIDs().add(applicantUUID);
                        }
                        plugin.getBusinessManager().saveBusiness(biz);
                    }
                    plugin.getCharacterManager().removeMailItem(p.getUniqueId(), mail.getId());
                    p.sendMessage("\u00a7aYou hired \u00a7e" + applicantName + "\u00a7a at \u00a76" + businessName + "\u00a7a!");
                    Player applicant = plugin.getServer().getPlayer(applicantUUID);
                    if (applicant != null) applicant.sendMessage("\u00a7aYour application to \u00a76" + businessName + "\u00a7a was accepted!");
                    plugin.getGUIManager().openGUI(new MailGUI(plugin, 0), p);
                })
        );

        addButton(15, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.RED_WOOL, "\u00a7cDeny Application"))
                .consumer(e -> {
                    Player p = (Player) e.getWhoClicked();
                    plugin.getCharacterManager().removeMailItem(p.getUniqueId(), mail.getId());
                    p.sendMessage("\u00a7cYou denied the application from " + applicantName + ".");
                    UUID applicantUUID = mail.getFromPlayerUuid();
                    Player applicant = plugin.getServer().getPlayer(applicantUUID);
                    if (applicant != null) applicant.sendMessage("\u00a7cYour application to \u00a76" + businessName + "\u00a7c was denied.");
                    plugin.getGUIManager().openGUI(new MailGUI(plugin, 0), p);
                })
        );
    }

    private void decorateBusinessSaleRequest(Player player) {
        String businessId = mail.getData().getOrDefault("businessId", "");
        String businessName = mail.getData().getOrDefault("businessName", "Unknown");
        String salePriceStr = mail.getData().getOrDefault("salePrice", "0");
        String buyerName = mail.getData().getOrDefault("buyerName", "Unknown");

        addButton(4, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.EMERALD, "\u00a76Business Purchase Request",
                        "\u00a77Business: \u00a7f" + businessName,
                        "\u00a77Buyer: \u00a7f" + buyerName,
                        "\u00a77Sale Price: \u00a7e" + CurrencyUtil.symbol() + salePriceStr,
                        "\u00a77" + buyerName + " wants to purchase your business."))
                .consumer(e -> {})
        );

        addButton(11, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.LIME_WOOL, "\u00a7aAccept Sale"))
                .consumer(e -> {
                    Player p = (Player) e.getWhoClicked();
                    if (businessId.isEmpty() || !plugin.isDirtBusinessEnabled()) {
                        p.sendMessage("\u00a7cInvalid request."); return;
                    }
                    UUID bizId;
                    try { bizId = UUID.fromString(businessId); } catch (IllegalArgumentException ex) {
                        p.sendMessage("\u00a7cInvalid business ID."); return;
                    }
                    BusinessData biz = plugin.getBusinessManager().loadBusiness(bizId);
                    if (biz == null || !biz.getOwnerUUID().equals(p.getUniqueId())) {
                        p.sendMessage("\u00a7cYou are no longer the owner of this business.");
                        plugin.getCharacterManager().removeMailItem(p.getUniqueId(), mail.getId());
                        plugin.getGUIManager().openGUI(new MailGUI(plugin, 0), p);
                        return;
                    }
                    double price = biz.getSalePrice();
                    UUID buyerUUID = mail.getFromPlayerUuid();
                    if (!plugin.getEconomy().has(plugin.getServer().getOfflinePlayer(buyerUUID), price)) {
                        p.sendMessage("\u00a7cThe buyer no longer has sufficient funds (" + CurrencyUtil.symbol() + String.format("%.2f", price) + ").");
                        plugin.getCharacterManager().removeMailItem(p.getUniqueId(), mail.getId());
                        plugin.getGUIManager().openGUI(new MailGUI(plugin, 0), p);
                        return;
                    }
                    plugin.getEconomy().withdrawPlayer(plugin.getServer().getOfflinePlayer(buyerUUID), price);
                    plugin.getEconomy().depositPlayer(p, price);
                    biz.setOwnerUUID(buyerUUID);
                    biz.setForSale(false);
                    biz.setSalePrice(0);
                    plugin.getBusinessManager().saveBusiness(biz);
                    plugin.getCharacterManager().removeMailItem(p.getUniqueId(), mail.getId());
                    p.sendMessage("\u00a7aYou sold \u00a76" + businessName + "\u00a7a to \u00a7e" + buyerName + "\u00a7a for \u00a7e" + CurrencyUtil.symbol() + String.format("%.2f", price) + "\u00a7a!");
                    Player buyer = plugin.getServer().getPlayer(buyerUUID);
                    if (buyer != null) buyer.sendMessage("\u00a7aYou are now the owner of \u00a76" + businessName + "\u00a7a!");
                    plugin.getGUIManager().openGUI(new MailGUI(plugin, 0), p);
                })
        );

        addButton(15, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.RED_WOOL, "\u00a7cDeny Sale"))
                .consumer(e -> {
                    Player p = (Player) e.getWhoClicked();
                    plugin.getCharacterManager().removeMailItem(p.getUniqueId(), mail.getId());
                    UUID bizId;
                    try { bizId = UUID.fromString(businessId); } catch (IllegalArgumentException ex) {
                        p.sendMessage("\u00a7cInvalid business ID.");
                        plugin.getGUIManager().openGUI(new MailGUI(plugin, 0), p);
                        return;
                    }
                    BusinessData biz = plugin.getBusinessManager().loadBusiness(bizId);
                    if (biz != null) {
                        plugin.getGUIManager().openGUI(
                                new com.dirt.inventory.impl.business.BusinessSaleDenyGUI(plugin, bizId), p);
                    } else {
                        plugin.getGUIManager().openGUI(new MailGUI(plugin, 0), p);
                    }
                })
        );
    }

    private void decoratePropertyEntry(Player player) {
        String entrantName = mail.getData().getOrDefault("entrantName", "Unknown");
        String propertyName = mail.getData().getOrDefault("propertyName", "Unknown");
        String timeStr = new SimpleDateFormat("MM/dd/yyyy HH:mm").format(new Date(mail.getTimestamp()));

        addButton(4, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.PAPER, "\u00a79Property Entry Notification",
                        "\u00a77Property: \u00a7f" + propertyName,
                        "\u00a77Entered by: \u00a7f" + entrantName,
                        "\u00a77Time: \u00a7f" + timeStr))
                .consumer(e -> {})
        );

        addButton(13, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.RED_WOOL, "\u00a7cDismiss"))
                .consumer(e -> {
                    Player p = (Player) e.getWhoClicked();
                    plugin.getCharacterManager().removeMailItem(p.getUniqueId(), mail.getId());
                    plugin.getGUIManager().openGUI(new MailGUI(plugin, 0), p);
                })
        );
    }

    private void setDefaultInheritor(FamilyData family, UUID childUUID) {
        if (family.getSpouse1() != null) {
            CharacterData s1 = plugin.getCharacterManager().getCharacter(family.getSpouse1());
            if (s1 != null && s1.getInheritorUuid() == null) {
                s1.setInheritorUuid(childUUID);
                plugin.getCharacterManager().saveCharacter(s1);
            }
        }
        if (family.getSpouse2() != null) {
            CharacterData s2 = plugin.getCharacterManager().getCharacter(family.getSpouse2());
            if (s2 != null && s2.getInheritorUuid() == null) {
                s2.setInheritorUuid(childUUID);
                plugin.getCharacterManager().saveCharacter(s2);
            }
        }
    }

    private void decorateContractOffer(Player player) {
        String contractIdStr = mail.getData().getOrDefault("contractId", "");
        String fromName = mail.getData().getOrDefault("fromName", "Unknown");
        String summary = mail.getData().getOrDefault("summary", "No summary");

        addButton(4, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.WRITABLE_BOOK, "\u00a7eContract",
                        "\u00a77From: \u00a7f" + fromName,
                        "\u00a77" + summary,
                        "\u00a77Click below to view and sign."))
                .consumer(e -> {})
        );

        addButton(11, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.LIME_WOOL, "\u00a7aView & Sign"))
                .consumer(e -> {
                    Player p = (Player) e.getWhoClicked();
                    if (contractIdStr.isEmpty() || plugin.getContractManager() == null) {
                        p.sendMessage("\u00a7cInvalid contract data."); return;
                    }
                    UUID contractId;
                    try { contractId = UUID.fromString(contractIdStr); } catch (IllegalArgumentException ex) {
                        p.sendMessage("\u00a7cInvalid contract ID."); return;
                    }
                    ContractData contract = plugin.getContractManager().loadContract(contractId);
                    if (contract == null) {
                        p.sendMessage("\u00a7cThis contract no longer exists.");
                        plugin.getCharacterManager().removeMailItem(p.getUniqueId(), mail.getId());
                        plugin.getGUIManager().openGUI(new MailGUI(plugin, 0), p);
                        return;
                    }
                    plugin.getGUIManager().openGUI(new ContractViewGUI(plugin, contractId, mail.getId()), p);
                })
        );

        addButton(13, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.ORANGE_WOOL, "\u00a76Request Modification"))
                .consumer(e -> {
                    Player p = (Player) e.getWhoClicked();
                    p.closeInventory();
                    plugin.getChatInputManager().requestInput(p, "\u00a7eType your modification request:", msg -> {
                        UUID senderUuid = mail.getFromPlayerUuid();
                        MailItem response = new MailItem();
                        response.setId(UUID.randomUUID().toString());
                        response.setType("CONTRACT_MODIFICATION_REQUEST");
                        response.setFromPlayerUuid(p.getUniqueId());
                        response.setTimestamp(System.currentTimeMillis());
                        CharacterData myChar = plugin.getCharacterManager().getCharacter(p.getUniqueId());
                        String myName = myChar != null ? myChar.getFirstName() + " " + myChar.getLastName() : p.getName();
                        java.util.Map<String, String> data = new java.util.HashMap<>();
                        data.put("fromName", myName);
                        data.put("contractId", contractIdStr);
                        data.put("message", msg);
                        response.setData(data);
                        plugin.getCharacterManager().addMailItem(senderUuid, response);
                        plugin.getCharacterManager().removeMailItem(p.getUniqueId(), mail.getId());
                        p.sendMessage("\u00a7aModification request sent.");
                        plugin.getServer().getScheduler().runTask(plugin,
                                () -> plugin.getGUIManager().openGUI(new MailGUI(plugin, 0), p));
                    });
                })
        );

        addButton(15, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.RED_WOOL, "\u00a7cDeny"))
                .consumer(e -> {
                    Player p = (Player) e.getWhoClicked();
                    plugin.getCharacterManager().removeMailItem(p.getUniqueId(), mail.getId());
                    if (!contractIdStr.isEmpty() && plugin.getContractManager() != null) {
                        try {
                            UUID cid = UUID.fromString(contractIdStr);
                            ContractData c = plugin.getContractManager().loadContract(cid);
                            if (c != null) {
                                c.setStatus("DENIED");
                                plugin.getContractManager().saveContract(c);
                            }
                        } catch (IllegalArgumentException ignored) {}
                    }
                    p.sendMessage("\u00a7cContract denied.");
                    plugin.getGUIManager().openGUI(new MailGUI(plugin, 0), p);
                })
        );
    }

    private void decorateContractBroken(Player player) {
        String fromName = mail.getData().getOrDefault("fromName", "Unknown");
        String contractSummary = mail.getData().getOrDefault("summary", "A contract");

        addButton(4, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.BARRIER, "\u00a7cContract Broken",
                        "\u00a77" + fromName + " has broken the contract:",
                        "\u00a77" + contractSummary))
                .consumer(e -> {})
        );

        addButton(13, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.RED_WOOL, "\u00a7cDismiss"))
                .consumer(e -> {
                    Player p = (Player) e.getWhoClicked();
                    plugin.getCharacterManager().removeMailItem(p.getUniqueId(), mail.getId());
                    plugin.getGUIManager().openGUI(new MailGUI(plugin, 0), p);
                })
        );
    }

    private void decorateModificationRequest(Player player) {
        String fromName = mail.getData().getOrDefault("fromName", "Unknown");
        String message = mail.getData().getOrDefault("message", "No message");

        addButton(4, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.WRITABLE_BOOK, "\u00a76Modification Request",
                        "\u00a77From: \u00a7f" + fromName,
                        "\u00a77Message: \u00a7f" + message))
                .consumer(e -> {})
        );

        addButton(13, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.RED_WOOL, "\u00a7cDismiss"))
                .consumer(e -> {
                    Player p = (Player) e.getWhoClicked();
                    plugin.getCharacterManager().removeMailItem(p.getUniqueId(), mail.getId());
                    plugin.getGUIManager().openGUI(new MailGUI(plugin, 0), p);
                })
        );
    }
}
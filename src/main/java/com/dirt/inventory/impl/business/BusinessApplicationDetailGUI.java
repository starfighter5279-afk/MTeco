package com.dirt.inventory.impl.business;

import com.cryptomorin.xseries.XMaterial;
import com.dirt.DirtEconomy;
import com.dirt.data.BusinessData;
import com.dirt.data.CharacterData;
import com.dirt.data.MailItem;
import com.dirt.inventory.InventoryButton;
import com.dirt.inventory.InventoryGUI;
import com.dirt.util.ItemUtil;
import com.dirt.util.CurrencyUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BusinessApplicationDetailGUI extends InventoryGUI {
    private final DirtEconomy plugin;
    private final UUID businessId;

    public BusinessApplicationDetailGUI(DirtEconomy plugin, UUID businessId) {
        this.plugin = plugin;
        this.businessId = businessId;
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 27, "\u00a76Business Details");
    }

    @Override
    public void decorate(Player player) {
        fillGlass(27, XMaterial.LIME_STAINED_GLASS_PANE);

        BusinessData biz = plugin.getBusinessManager().loadBusiness(businessId);
        if (biz == null) {
            addButton(13, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.BARRIER, "\u00a7cBusiness not found."))
                    .consumer(e -> {})
            );
        } else {
            double balance = plugin.getBusinessManager().getTreasuryBalance(biz.getName());
            String ownerName = "Unknown";
            CharacterData ownerChar = plugin.getCharacterManager().getCharacter(biz.getOwnerUUID());
            if (ownerChar != null) ownerName = ownerChar.getFirstName() + " " + ownerChar.getLastName();
            final String finalOwnerName = ownerName;

            addButton(4, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.EMERALD, "\u00a7a" + biz.getName(),
                            "\u00a77" + biz.getDescription()))
                    .consumer(e -> {})
            );

            addButton(10, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.GOLD_NUGGET, "\u00a7eTreasury",
                            "\u00a77Balance: \u00a7e" + CurrencyUtil.symbol() + String.format("%.2f", balance)))
                    .consumer(e -> {})
            );

            addButton(12, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.PAPER, "\u00a77Total Earned",
                            "\u00a7e" + CurrencyUtil.symbol() + String.format("%.2f", biz.getTotalEarned())))
                    .consumer(e -> {})
            );

            addButton(14, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.SKELETON_SKULL, "\u00a7eOwner",
                            "\u00a7f" + finalOwnerName))
                    .consumer(e -> {})
            );

            addButton(16, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.PLAYER_HEAD, "\u00a77Employees",
                            "\u00a7f" + biz.getEmployeeUUIDs().size()))
                    .consumer(e -> {})
            );

            if (biz.getEmployeeUUIDs().contains(player.getUniqueId())) {
                addButton(22, new InventoryButton()
                        .creator(p -> ItemUtil.buildItem(XMaterial.GRAY_DYE, "\u00a77Already Employed",
                                "\u00a77You already work here."))
                        .consumer(e -> {})
                );
            } else {
                addButton(22, new InventoryButton()
                        .creator(p -> ItemUtil.buildItem(XMaterial.LIME_WOOL, "\u00a7aApply",
                                "\u00a77Send a job application to the owner.",
                                "\u00a77Weekly Pay: \u00a7e" + CurrencyUtil.symbol() + String.format("%.2f", biz.getPayrollRate())))
                        .consumer(e -> sendApplication(biz, (Player) e.getWhoClicked()))
                );
            }
        }

        addButton(26, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7cBack"))
                .consumer(e -> plugin.getGUIManager().openGUI(new ApplyJobGUI(plugin, 0), (Player) e.getWhoClicked()))
        );

        super.decorate(player);
    }

    private void sendApplication(BusinessData biz, Player applicant) {
        CharacterData applicantChar = plugin.getCharacterManager().getCharacter(applicant.getUniqueId());
        String applicantName = applicantChar != null
                ? applicantChar.getFirstName() + " " + applicantChar.getLastName()
                : applicant.getName();

        MailItem mail = new MailItem();
        mail.setId(UUID.randomUUID().toString());
        mail.setType("JOB_APPLICATION");
        mail.setFromPlayerUuid(applicant.getUniqueId());
        mail.setTimestamp(System.currentTimeMillis());
        Map<String, String> data = new HashMap<>();
        data.put("businessId", biz.getBusinessId().toString());
        data.put("businessName", biz.getName());
        data.put("applicantName", applicantName);
        data.put("fromName", applicantName);
        mail.setData(data);

        plugin.getCharacterManager().addMailItem(biz.getOwnerUUID(), mail);
        applicant.sendMessage("\u00a7aYour application to \u00a76" + biz.getName() + "\u00a7a has been sent to the owner!");

        Player owner = Bukkit.getPlayer(biz.getOwnerUUID());
        if (owner != null) {
            owner.sendMessage("\u00a7e" + applicantName + " has applied for a job at \u00a76" + biz.getName() + "\u00a7e!");
        }
        applicant.closeInventory();
    }
}
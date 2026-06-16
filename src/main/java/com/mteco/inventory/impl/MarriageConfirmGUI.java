package com.mteco.inventory.impl;

import com.cryptomorin.xseries.XMaterial;
import com.mteco.MTeco;
import com.mteco.data.CharacterData;
import com.mteco.data.MailItem;
import com.mteco.inventory.InventoryButton;
import com.mteco.inventory.InventoryGUI;
import com.mteco.util.CurrencyUtil;
import com.mteco.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MarriageConfirmGUI extends InventoryGUI {
    private final MTeco plugin;
    private final UUID targetPlayerUUID;

    public MarriageConfirmGUI(MTeco plugin, UUID targetPlayerUUID) {
        this.plugin = plugin;
        this.targetPlayerUUID = targetPlayerUUID;
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 27, "§dMarriage Request");
    }

    @Override
    public void decorate(Player player) {
        CharacterData targetData = plugin.getCharacterManager().getCharacter(targetPlayerUUID);

        fillGlass(27, XMaterial.PINK_STAINED_GLASS_PANE);

        if (targetData == null) {
            addButton(13, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.BARRIER, "§cCharacter not found"))
                    .consumer(e -> plugin.getGUIManager().openGUI(new SpouseSelectionGUI(plugin, 0), (Player) e.getWhoClicked()))
            );
            super.decorate(player);
            return;
        }

        String fullName = targetData.getFirstName() + " " + targetData.getMiddleName() + " " + targetData.getLastName();
        String dateStr = new SimpleDateFormat("MM/dd/yyyy").format(new Date(targetData.getBirthDate()));
        double targetBalance = plugin.getEconomy().getBalance(Bukkit.getOfflinePlayer(targetPlayerUUID));
        double myBalance = plugin.getEconomy().getBalance(player);
        double marriageCost = plugin.getSettings().getMarriageCost();
        boolean canAfford = myBalance >= marriageCost;

        addButton(4, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.GOLD_INGOT, "§dMarriage Request",
                        "§7Sending request to: §f" + fullName,
                        "§7Their birthday: §f" + dateStr,
                        "fffffa77Their balance: fffffa7e" + CurrencyUtil.symbol() + String.format("%.0f", targetBalance),
                        "fffffa77Cost: fffffa7e" + CurrencyUtil.symbol() + String.format("%.0f", marriageCost),
                        canAfford ? "§aYou can afford this!" : "§cInsufficient funds!",
                        "§7A deniable request will be sent to their mailbox."))
                .consumer(e -> {})
        );

        addButton(11, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(canAfford ? XMaterial.LIME_WOOL : XMaterial.RED_WOOL, "§aSend Marriage Request"))
                .consumer(e -> {
                    Player p = (Player) e.getWhoClicked();
                    if (plugin.getEconomy().getBalance(p) < marriageCost) {
                        p.sendMessage("§cYou do not have enough funds. Marriage costs " + CurrencyUtil.symbol() + String.format("%.0f", marriageCost) + ".");
                        return;
                    }
                    CharacterData freshTarget = plugin.getCharacterManager().getCharacter(targetPlayerUUID);
                    if (freshTarget == null || (freshTarget.getFamilyId() != null && !"CHILD".equals(freshTarget.getFamilyRole()))) {
                        p.sendMessage("§cThis character is no longer available for marriage.");
                        plugin.getGUIManager().openGUI(new FamilyGUI(plugin), p);
                        return;
                    }
                    CharacterData myData = plugin.getCharacterManager().getCharacter(p.getUniqueId());
                    if (myData == null) return;

                    plugin.getEconomy().withdrawPlayer(p, marriageCost);

                    MailItem mail = new MailItem();
                    mail.setId(UUID.randomUUID().toString());
                    mail.setType("MARRIAGE_REQUEST");
                    mail.setFromPlayerUuid(p.getUniqueId());
                    mail.setTimestamp(System.currentTimeMillis());
                    Map<String, String> data = new HashMap<>();
                    data.put("fromName", myData.getFirstName() + " " + myData.getMiddleName() + " " + myData.getLastName());
                    data.put("fromBirthDate", String.valueOf(myData.getBirthDate()));
                    data.put("fromBalance", String.valueOf(plugin.getEconomy().getBalance(p)));
                    mail.setData(data);

                    plugin.getCharacterManager().addMailItem(targetPlayerUUID, mail);
                    p.sendMessage("§aMarriage request sent to §e" + freshTarget.getFirstName() + "§a! §e" + CurrencyUtil.symbol() + String.format("%.0f", marriageCost) + " has been deducted.");
                    p.closeInventory();

                    Player targetPlayer = Bukkit.getPlayer(targetPlayerUUID);
                    if (targetPlayer != null) {
                        targetPlayer.sendMessage("§eYou have received a marriage request from §f" + myData.getFirstName() + " " + myData.getLastName() + "§e. Check your mailbox (/mtc).");
                    }
                })
        );

        addButton(15, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.RED_WOOL, "§cCancel"))
                .consumer(e -> plugin.getGUIManager().openGUI(new SpouseSelectionGUI(plugin, 0), (Player) e.getWhoClicked()))
        );

        super.decorate(player);
    }
}
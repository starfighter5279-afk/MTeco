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

public class BusinessPurchaseConfirmGUI extends InventoryGUI {
    private final DirtEconomy plugin;
    private final UUID businessId;

    public BusinessPurchaseConfirmGUI(DirtEconomy plugin, UUID businessId) {
        this.plugin = plugin;
        this.businessId = businessId;
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 27, "\u00a76Purchase Business?");
    }

    @Override
    public void decorate(Player player) {
        fillGlass(27, XMaterial.LIME_STAINED_GLASS_PANE);

        BusinessData biz = plugin.getBusinessManager().loadBusiness(businessId);
        if (biz == null) {
            addButton(13, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.BARRIER, "\u00a7cBusiness not found."))
                    .consumer(e -> plugin.getGUIManager().openGUI(new AllBusinessesGUI(plugin, 0), (Player) e.getWhoClicked()))
            );
            super.decorate(player);
            return;
        }

        String ownerName = resolveOwnerName(biz);

        addButton(4, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.EMERALD,
                        "\u00a76" + biz.getName(),
                        "\u00a77Current Owner: \u00a7f" + ownerName,
                        "\u00a77Employees: \u00a7f" + biz.getEmployeeUUIDs().size(),
                        "\u00a77Total Earned: \u00a7e" + CurrencyUtil.symbol() + String.format("%.2f", biz.getTotalEarned()),
                        "\u00a77Sale Price: \u00a7e" + CurrencyUtil.symbol() + String.format("%.2f", biz.getSalePrice()),
                        "\u00a77A purchase request will be sent to",
                        "\u00a77the owner for their approval."))
                .consumer(e -> {})
        );

        addButton(11, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.LIME_WOOL, "\u00a7aConfirm Purchase",
                        "\u00a77Send a purchase request to the owner."))
                .consumer(e -> confirmPurchase((Player) e.getWhoClicked(), biz))
        );

        addButton(15, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.RED_WOOL, "\u00a7cCancel"))
                .consumer(e -> plugin.getGUIManager().openGUI(new AllBusinessesGUI(plugin, 0), (Player) e.getWhoClicked()))
        );

        super.decorate(player);
    }

    private void confirmPurchase(Player buyer, BusinessData biz) {
        if (!biz.isForSale()) {
            buyer.sendMessage("\u00a7cThis business is no longer for sale.");
            plugin.getGUIManager().openGUI(new AllBusinessesGUI(plugin, 0), buyer);
            return;
        }
        if (biz.getOwnerUUID().equals(buyer.getUniqueId())) {
            buyer.sendMessage("\u00a7cYou already own this business.");
            plugin.getGUIManager().openGUI(new AllBusinessesGUI(plugin, 0), buyer);
            return;
        }
        if (!plugin.getEconomy().has(buyer, biz.getSalePrice())) {
            buyer.sendMessage("\u00a7cYou cannot afford this business. Price: \u00a7e" + CurrencyUtil.symbol() + String.format("%.2f", biz.getSalePrice()));
            plugin.getGUIManager().openGUI(new AllBusinessesGUI(plugin, 0), buyer);
            return;
        }

        CharacterData buyerChar = plugin.getCharacterManager().getCharacter(buyer.getUniqueId());
        String buyerName = buyerChar != null
                ? buyerChar.getFirstName() + " " + buyerChar.getLastName()
                : buyer.getName();

        MailItem mail = new MailItem();
        mail.setId(UUID.randomUUID().toString());
        mail.setType("BUSINESS_SALE_REQUEST");
        mail.setFromPlayerUuid(buyer.getUniqueId());
        mail.setTimestamp(System.currentTimeMillis());
        Map<String, String> data = new HashMap<>();
        data.put("businessId", biz.getBusinessId().toString());
        data.put("businessName", biz.getName());
        data.put("salePrice", String.format("%.2f", biz.getSalePrice()));
        data.put("buyerName", buyerName);
        mail.setData(data);

        plugin.getCharacterManager().addMailItem(biz.getOwnerUUID(), mail);

        buyer.sendMessage("\u00a7aYour purchase request for \u00a76" + biz.getName()
                + "\u00a7a has been sent to the owner. Await their approval.");
        plugin.getGUIManager().openGUI(new AllBusinessesGUI(plugin, 0), buyer);

        Player owner = Bukkit.getPlayer(biz.getOwnerUUID());
        if (owner != null) {
            owner.sendMessage("\u00a7e" + buyerName + "\u00a7e wants to purchase your business \u00a76"
                    + biz.getName() + "\u00a7e. Check your mail to approve or deny.");
        }
    }

    private String resolveOwnerName(BusinessData biz) {
        if (biz.getOwnerUUID() == null) return "Unknown";
        CharacterData cd = plugin.getCharacterManager().getCharacter(biz.getOwnerUUID());
        if (cd != null) return cd.getFirstName() + " " + cd.getLastName();
        String name = Bukkit.getOfflinePlayer(biz.getOwnerUUID()).getName();
        return name != null ? name : "Unknown";
    }
}
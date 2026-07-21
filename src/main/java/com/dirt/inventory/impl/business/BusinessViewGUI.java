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
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BusinessViewGUI extends InventoryGUI {
    private final DirtEconomy plugin;
    private final int page;

    public BusinessViewGUI(DirtEconomy plugin, int page) {
        this.plugin = plugin;
        this.page = page;
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 54, "\u00a76All Businesses");
    }

    @Override
    public void decorate(Player player) {
        fillPagedGui(54, XMaterial.LIME_STAINED_GLASS_PANE);

        List<BusinessData> all = plugin.getBusinessManager().getAllBusinesses();
        int perPage = 45;
        int start = page * perPage;
        int end = Math.min(start + perPage, all.size());

        for (int i = start; i < end; i++) {
            BusinessData biz = all.get(i);
            int slot = i - start;
            boolean forSale = biz.isForSale();
            addButton(slot, new InventoryButton()
                    .creator(p -> {
                        XMaterial mat = forSale ? XMaterial.EMERALD : XMaterial.IRON_INGOT;
                        String name = (forSale ? "\u00a7a" : "\u00a77") + biz.getName();
                        if (forSale) {
                            return ItemUtil.buildItem(mat, name,
                                    "\u00a77" + biz.getDescription(),
                                    "\u00a77Employees: \u00a7f" + biz.getEmployeeUUIDs().size(),
                                    "\u00a77Total Earned: \u00a7e" + CurrencyUtil.symbol() + String.format("%.2f", biz.getTotalEarned()),
                                    "\u00a7aFor Sale: \u00a7e" + CurrencyUtil.symbol() + String.format("%.2f", biz.getSalePrice()),
                                    "\u00a7eClick to purchase this business.");
                        }
                        return ItemUtil.buildItem(mat, name,
                                "\u00a77" + biz.getDescription(),
                                "\u00a77Employees: \u00a7f" + biz.getEmployeeUUIDs().size(),
                                "\u00a77Total Earned: \u00a7e" + CurrencyUtil.symbol() + String.format("%.2f", biz.getTotalEarned()),
                                "\u00a77Hiring: \u00a7f" + (biz.isHiring() ? "\u00a7aYes" : "\u00a7cNo"));
                    })
                    .consumer(e -> {
                        Player p = (Player) e.getWhoClicked();
                        if (!forSale) return;
                        if (biz.getOwnerUUID().equals(p.getUniqueId())) {
                            p.sendMessage("\u00a7cYou cannot buy your own business.");
                            return;
                        }
                        confirmPurchase(biz, p);
                    })
            );
        }

        if (page > 0) {
            int prev = page - 1;
            addButton(45, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7ePrevious Page"))
                    .consumer(e -> plugin.getGUIManager().openGUI(new BusinessViewGUI(plugin, prev), (Player) e.getWhoClicked()))
            );
        }
        if (end < all.size()) {
            int next = page + 1;
            addButton(53, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7eNext Page"))
                    .consumer(e -> plugin.getGUIManager().openGUI(new BusinessViewGUI(plugin, next), (Player) e.getWhoClicked()))
            );
        }

        super.decorate(player);
    }

    private void confirmPurchase(BusinessData biz, Player buyer) {
        if (!plugin.getEconomy().has(buyer, biz.getSalePrice())) {
            buyer.sendMessage("\u00a7cYou cannot afford " + CurrencyUtil.symbol() + String.format("%.2f", biz.getSalePrice()) + ".");
            return;
        }

        CharacterData buyerChar = plugin.getCharacterManager().getCharacter(buyer.getUniqueId());
        String buyerName = buyerChar != null ? buyerChar.getFirstName() + " " + buyerChar.getLastName() : buyer.getName();
        final String finalBuyerName = buyerName;

        buyer.closeInventory();
        plugin.getChatInputManager().requestInput(buyer,
                "\u00a7ePurchase '\u00a76" + biz.getName() + "\u00a7e' for \u00a7e" + CurrencyUtil.symbol() + String.format("%.2f", biz.getSalePrice())
                        + "\u00a7e? Type \u00a76yes\u00a7e to confirm:",
                answer -> {
                    if (!answer.equalsIgnoreCase("yes")) {
                        buyer.sendMessage("\u00a7cPurchase cancelled.");
                        plugin.getServer().getScheduler().runTask(plugin,
                                () -> plugin.getGUIManager().openGUI(new BusinessViewGUI(plugin, 0), buyer));
                        return;
                    }
                    MailItem mail = new MailItem();
                    mail.setId(UUID.randomUUID().toString());
                    mail.setType("BUSINESS_SALE_REQUEST");
                    mail.setFromPlayerUuid(buyer.getUniqueId());
                    mail.setTimestamp(System.currentTimeMillis());
                    Map<String, String> data = new HashMap<>();
                    data.put("businessId", biz.getBusinessId().toString());
                    data.put("businessName", biz.getName());
                    data.put("salePrice", String.format("%.2f", biz.getSalePrice()));
                    data.put("buyerName", finalBuyerName);
                    data.put("fromName", finalBuyerName);
                    mail.setData(data);
                    plugin.getCharacterManager().addMailItem(biz.getOwnerUUID(), mail);
                    buyer.sendMessage("\u00a7aPurchase request sent to the business owner!");
                    Player owner = Bukkit.getPlayer(biz.getOwnerUUID());
                    if (owner != null) owner.sendMessage("\u00a7e" + finalBuyerName + " wants to buy \u00a76" + biz.getName() + "\u00a7e! Check your mail.");
                    plugin.getServer().getScheduler().runTask(plugin,
                            () -> plugin.getGUIManager().openGUI(new BusinessViewGUI(plugin, 0), buyer));
                });
    }
}
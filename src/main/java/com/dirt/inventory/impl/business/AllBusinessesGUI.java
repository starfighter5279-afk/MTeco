package com.dirt.inventory.impl.business;

import com.cryptomorin.xseries.XMaterial;
import com.dirt.DirtEconomy;
import com.dirt.data.BusinessData;
import com.dirt.data.CharacterData;
import com.dirt.inventory.InventoryButton;
import com.dirt.inventory.InventoryGUI;
import com.dirt.util.ItemUtil;
import com.dirt.util.CurrencyUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.List;

public class AllBusinessesGUI extends InventoryGUI {
    private final DirtEconomy plugin;
    private final int page;

    public AllBusinessesGUI(DirtEconomy plugin, int page) {
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

            String ownerName = resolveOwnerName(biz);

            if (biz.isForSale()) {
                addButton(slot, new InventoryButton()
                        .creator(p -> ItemUtil.buildItem(XMaterial.EMERALD,
                                "\u00a7a" + biz.getName() + " \u00a76[FOR SALE]",
                                "\u00a77Owner: \u00a7f" + ownerName,
                                "\u00a77Employees: \u00a7f" + biz.getEmployeeUUIDs().size(),
                                "\u00a77Total Earned: \u00a7e" + CurrencyUtil.symbol() + String.format("%.2f", biz.getTotalEarned()),
                                "\u00a77Sale Price: \u00a7e" + CurrencyUtil.symbol() + String.format("%.2f", biz.getSalePrice()),
                                "\u00a7aClick to purchase this business."))
                        .consumer(e -> plugin.getGUIManager().openGUI(
                                new BusinessPurchaseConfirmGUI(plugin, biz.getBusinessId()),
                                (Player) e.getWhoClicked()))
                );
            } else {
                addButton(slot, new InventoryButton()
                        .creator(p -> ItemUtil.buildItem(XMaterial.BOOK,
                                "\u00a7f" + biz.getName(),
                                "\u00a77Owner: \u00a7f" + ownerName,
                                "\u00a77Employees: \u00a7f" + biz.getEmployeeUUIDs().size(),
                                "\u00a77Total Earned: \u00a7e" + CurrencyUtil.symbol() + String.format("%.2f", biz.getTotalEarned()),
                                "\u00a77Hiring: \u00a7f" + (biz.isHiring() ? "\u00a7aYes" : "\u00a7cNo"),
                                "\u00a77Not for sale."))
                        .consumer(e -> {})
                );
            }
        }

        if (page > 0) {
            int prev = page - 1;
            addButton(45, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7ePrevious"))
                    .consumer(e -> plugin.getGUIManager().openGUI(new AllBusinessesGUI(plugin, prev), (Player) e.getWhoClicked()))
            );
        }
        if (end < all.size()) {
            int next = page + 1;
            addButton(53, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7eNext"))
                    .consumer(e -> plugin.getGUIManager().openGUI(new AllBusinessesGUI(plugin, next), (Player) e.getWhoClicked()))
            );
        }

        super.decorate(player);
    }

    private String resolveOwnerName(BusinessData biz) {
        if (biz.getOwnerUUID() == null) return "Unknown";
        CharacterData cd = plugin.getCharacterManager().getCharacter(biz.getOwnerUUID());
        if (cd != null) return cd.getFirstName() + " " + cd.getLastName();
        String name = Bukkit.getOfflinePlayer(biz.getOwnerUUID()).getName();
        return name != null ? name : "Unknown";
    }
}
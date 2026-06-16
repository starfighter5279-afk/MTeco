package com.mteco.inventory.impl.business;

import com.cryptomorin.xseries.XMaterial;
import com.mteco.MTeco;
import com.mteco.data.BusinessData;
import com.mteco.inventory.InventoryButton;
import com.mteco.inventory.InventoryGUI;
import com.mteco.inventory.impl.MailGUI;
import com.mteco.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.UUID;

public class BusinessSaleDenyGUI extends InventoryGUI {
    private final MTeco plugin;
    private final UUID businessId;

    public BusinessSaleDenyGUI(MTeco plugin, UUID businessId) {
        this.plugin = plugin;
        this.businessId = businessId;
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 27, "\u00a76Sale Options");
    }

    @Override
    public void decorate(Player player) {
        fillGlass(27, XMaterial.LIME_STAINED_GLASS_PANE);

        BusinessData biz = plugin.getBusinessManager().loadBusiness(businessId);
        String bizName = biz != null ? biz.getName() : "Business";

        addButton(4, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.PAPER, "\u00a7eYou denied the sale.",
                        "\u00a77What would you like to do?"))
                .consumer(e -> {})
        );

        addButton(11, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.RED_WOOL, "\u00a7cTake Off Sale",
                        "\u00a77Remove \u00a76" + bizName + "\u00a77 from the market."))
                .consumer(e -> {
                    Player p = (Player) e.getWhoClicked();
                    if (biz != null) {
                        biz.setForSale(false);
                        biz.setSalePrice(0);
                        plugin.getBusinessManager().saveBusiness(biz);
                        p.sendMessage("\u00a7a" + bizName + " has been taken off the market.");
                    }
                    plugin.getGUIManager().openGUI(new MailGUI(plugin, 0), p);
                })
        );

        addButton(15, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.LIME_WOOL, "\u00a7aKeep Selling",
                        "\u00a77Keep \u00a76" + bizName + "\u00a77 listed for sale."))
                .consumer(e -> plugin.getGUIManager().openGUI(new MailGUI(plugin, 0), (Player) e.getWhoClicked()))
        );

        super.decorate(player);
    }
}
package com.dirt.inventory.impl.business;

import com.cryptomorin.xseries.XMaterial;
import com.dirt.DirtEconomy;
import com.dirt.data.BusinessData;
import com.dirt.inventory.InventoryButton;
import com.dirt.inventory.InventoryGUI;
import com.dirt.util.ItemUtil;
import com.dirt.util.CurrencyUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.List;
import java.util.stream.Collectors;

public class ApplyJobGUI extends InventoryGUI {
    private final DirtEconomy plugin;
    private final int page;

    public ApplyJobGUI(DirtEconomy plugin, int page) {
        this.plugin = plugin;
        this.page = page;
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 54, "\u00a76Hiring Businesses");
    }

    @Override
    public void decorate(Player player) {
        fillPagedGui(54, XMaterial.LIME_STAINED_GLASS_PANE);

        List<BusinessData> hiring = plugin.getBusinessManager().getAllBusinesses().stream()
                .filter(BusinessData::isHiring)
                .collect(Collectors.toList());

        int perPage = 45;
        int start = page * perPage;
        int end = Math.min(start + perPage, hiring.size());

        for (int i = start; i < end; i++) {
            BusinessData biz = hiring.get(i);
            int slot = i - start;
            addButton(slot, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.EMERALD, "\u00a7a" + biz.getName(),
                            "\u00a77" + biz.getDescription(),
                            "\u00a77Employees: \u00a7f" + biz.getEmployeeUUIDs().size(),
                            "\u00a77Weekly Pay: \u00a7e" + CurrencyUtil.symbol() + String.format("%.2f", biz.getPayrollRate()),
                            "\u00a77Click to view details."))
                    .consumer(e -> plugin.getGUIManager().openGUI(
                            new BusinessApplicationDetailGUI(plugin, biz.getBusinessId()),
                            (Player) e.getWhoClicked()))
            );
        }

        if (page > 0) {
            int prev = page - 1;
            addButton(45, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7ePrevious Page"))
                    .consumer(e -> plugin.getGUIManager().openGUI(new ApplyJobGUI(plugin, prev), (Player) e.getWhoClicked()))
            );
        }
        if (end < hiring.size()) {
            int next = page + 1;
            addButton(53, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7eNext Page"))
                    .consumer(e -> plugin.getGUIManager().openGUI(new ApplyJobGUI(plugin, next), (Player) e.getWhoClicked()))
            );
        }

        addButton(49, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7cBack"))
                .consumer(e -> plugin.getGUIManager().openGUI(new JobStatusGUI(plugin), (Player) e.getWhoClicked()))
        );

        super.decorate(player);
    }
}
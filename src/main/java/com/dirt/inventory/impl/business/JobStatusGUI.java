package com.dirt.inventory.impl.business;

import com.cryptomorin.xseries.XMaterial;
import com.dirt.DirtEconomy;
import com.dirt.data.BusinessData;
import com.dirt.inventory.InventoryButton;
import com.dirt.inventory.InventoryGUI;
import com.dirt.inventory.impl.CharacterManagementGUI;
import com.dirt.util.ItemUtil;
import com.dirt.util.CurrencyUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.List;
import java.util.UUID;

public class JobStatusGUI extends InventoryGUI {
    private final DirtEconomy plugin;
    private final UUID targetUuid;
    private final boolean adminMode;

    public JobStatusGUI(DirtEconomy plugin) {
        this.plugin = plugin;
        this.targetUuid = null;
        this.adminMode = false;
    }

    public JobStatusGUI(DirtEconomy plugin, UUID targetUuid) {
        this.plugin = plugin;
        this.targetUuid = targetUuid;
        this.adminMode = true;
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 54, adminMode ? "\u00a74Admin: Job Status" : "\u00a76My Job Status");
    }

    @Override
    public void decorate(Player player) {
        UUID effectiveUuid = targetUuid != null ? targetUuid : player.getUniqueId();

        fillPagedGui(54, XMaterial.LIME_STAINED_GLASS_PANE);

        List<BusinessData> jobs = plugin.getBusinessManager().getBusinessesByEmployee(effectiveUuid);

        if (jobs.isEmpty()) {
            addButton(22, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.GRAY_DYE, "\u00a77Currently Unemployed",
                            "\u00a77Not employed at any business."))
                    .consumer(e -> {})
            );
        } else {
            int slot = 0;
            for (BusinessData biz : jobs) {
                if (slot >= 36) break;
                double rate = biz.getEmployeeRates().getOrDefault(effectiveUuid, biz.getPayrollRate());
                String bizName = biz.getName();
                addButton(slot, new InventoryButton()
                        .creator(p -> ItemUtil.buildItem(XMaterial.GOLD_INGOT, "\u00a76" + bizName,
                                "\u00a77Position: \u00a7fEmployee",
                                "\u00a77Weekly Pay: \u00a7e" + CurrencyUtil.symbol() + String.format("%.2f", rate),
                                "\u00a77Status: \u00a7a" + (biz.isHiring() ? "Hiring" : "Active")))
                        .consumer(e -> {})
                );
                slot++;
            }
        }

        if (!adminMode) {
            addButton(45, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.EMERALD, "\u00a7aApply for New Job",
                            "\u00a77Browse businesses that are hiring."))
                    .consumer(e -> plugin.getGUIManager().openGUI(new ApplyJobGUI(plugin, 0), (Player) e.getWhoClicked()))
            );
        }

        addButton(49, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7cBack"))
                .consumer(e -> {
                    Player p = (Player) e.getWhoClicked();
                    if (adminMode) plugin.getGUIManager().openGUI(new CharacterManagementGUI(plugin, effectiveUuid), p);
                    else plugin.getGUIManager().openGUI(new CharacterManagementGUI(plugin), p);
                })
        );

        super.decorate(player);
    }
}
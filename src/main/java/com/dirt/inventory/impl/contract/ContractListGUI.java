package com.dirt.inventory.impl.contract;

import com.cryptomorin.xseries.XMaterial;
import com.dirt.DirtEconomy;
import com.dirt.data.ContractData;
import com.dirt.data.ContractSignature;
import com.dirt.inventory.InventoryButton;
import com.dirt.inventory.InventoryGUI;
import com.dirt.inventory.impl.CharacterManagementGUI;
import com.dirt.inventory.impl.business.BusinessManagementGUI;
import com.dirt.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class ContractListGUI extends InventoryGUI {
    private final DirtEconomy plugin;
    private final List<ContractData> contracts;
    private final int page;
    private final InventoryGUI backGui;

    public ContractListGUI(DirtEconomy plugin, List<ContractData> contracts, int page, InventoryGUI backGui) {
        this.plugin = plugin;
        this.contracts = contracts;
        this.page = page;
        this.backGui = backGui;
    }

    public ContractListGUI(DirtEconomy plugin, UUID businessId, UUID playerUuid, int page) {
        this.plugin = plugin;
        this.page = page;
        if (plugin.getContractManager() != null) {
            if (businessId != null) {
                this.contracts = plugin.getContractManager().getContractsByBusiness(businessId);
                this.backGui = new BusinessManagementGUI(plugin, businessId);
            } else if (playerUuid != null) {
                this.contracts = plugin.getContractManager().getContractsByPlayer(playerUuid);
                this.backGui = new CharacterManagementGUI(plugin);
            } else {
                this.contracts = new ArrayList<>();
                this.backGui = null;
            }
        } else {
            this.contracts = new ArrayList<>();
            this.backGui = null;
        }
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 54, "\u00a76Contracts");
    }

    @Override
    public void decorate(Player player) {
        fillPagedGui(54, XMaterial.YELLOW_STAINED_GLASS_PANE);

        int perPage = 45;
        int start = page * perPage;
        int end = Math.min(start + perPage, contracts.size());

        for (int i = start; i < end; i++) {
            ContractData contract = contracts.get(i);
            int slot = i - start;
            addButton(slot, new InventoryButton()
                    .creator(p -> {
                        List<String> lore = new ArrayList<>();
                        lore.add("\u00a77Status: \u00a7f" + contract.getStatus());
                        lore.add("\u00a77Created: \u00a7f" + new SimpleDateFormat("MM/dd/yyyy").format(new Date(contract.getCreatedAt())));
                        if (contract.getDurationMinecraftDays() > 0) {
                            lore.add("\u00a77Duration: \u00a7f" + contract.getDurationMinecraftDays() + " MC days");
                        }
                        for (ContractSignature sig : contract.getSignatures()) {
                            lore.add("\u00a77Signed by: \u00a7f" + sig.getCharacterName());
                        }
                        lore.add("\u00a77Click to manage.");
                        String preview = contract.getBody().length() > 30 ?
                                contract.getBody().substring(0, 30) + "..." : contract.getBody();
                        return ItemUtil.buildItem(XMaterial.PAPER, "\u00a7e" + preview, lore.toArray(new String[0]));
                    })
                    .consumer(e -> plugin.getGUIManager().openGUI(
                            new ContractManageGUI(plugin, contract.getContractId(), backGui),
                            (Player) e.getWhoClicked()))
            );
        }

        if (page > 0) {
            int prev = page - 1;
            addButton(45, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7ePrevious Page"))
                    .consumer(e -> plugin.getGUIManager().openGUI(
                            new ContractListGUI(plugin, contracts, prev, backGui),
                            (Player) e.getWhoClicked()))
            );
        }

        if (end < contracts.size()) {
            int next = page + 1;
            addButton(53, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7eNext Page"))
                    .consumer(e -> plugin.getGUIManager().openGUI(
                            new ContractListGUI(plugin, contracts, next, backGui),
                            (Player) e.getWhoClicked()))
            );
        }

        addButton(49, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.BARRIER, "\u00a7cBack"))
                .consumer(e -> {
                    if (backGui != null) {
                        plugin.getGUIManager().openGUI(backGui, (Player) e.getWhoClicked());
                    } else {
                        ((Player) e.getWhoClicked()).closeInventory();
                    }
                })
        );

        super.decorate(player);
    }
}
package com.mteco.inventory.impl.contract;

import com.cryptomorin.xseries.XMaterial;
import com.mteco.MTeco;
import com.mteco.data.ContractData;
import com.mteco.inventory.InventoryButton;
import com.mteco.inventory.InventoryGUI;
import com.mteco.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class ContractDraftsGUI extends InventoryGUI {
    private final MTeco plugin;
    private final int page;

    public ContractDraftsGUI(MTeco plugin, int page) {
        this.plugin = plugin;
        this.page = page;
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 54, "\u00a76Contract Drafts");
    }

    @Override
    public void decorate(Player player) {
        fillPagedGui(54, XMaterial.YELLOW_STAINED_GLASS_PANE);

        List<ContractData> drafts = plugin.getContractManager().getDraftsByPlayer(player.getUniqueId());
        drafts.sort(Comparator.comparingLong(ContractData::getCreatedAt).reversed());

        int perPage = 45;
        int start = page * perPage;
        int end = Math.min(start + perPage, drafts.size());
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");

        for (int i = start; i < end; i++) {
            ContractData draft = drafts.get(i);
            int slot = i - start;
            addButton(slot, new InventoryButton()
                    .creator(p -> {
                        List<String> lore = new ArrayList<>();
                        lore.add("\u00a77Created: \u00a7f" + sdf.format(new Date(draft.getCreatedAt())));
                        if (draft.getBody().isEmpty()) {
                            lore.add("\u00a77Body: \u00a7cEmpty");
                        } else {
                            lore.add("\u00a77Body: \u00a7a" + draft.getBody().length() + " chars");
                        }
                        if (draft.getDurationMinecraftDays() > 0) {
                            lore.add("\u00a77Duration: \u00a7f" + draft.getDurationMinecraftDays() + " MC days");
                        } else {
                            lore.add("\u00a77Duration: \u00a7fIndefinite");
                        }
                        lore.add("");
                        lore.add("\u00a7eClick to edit");
                        lore.add("\u00a7cShift-click to delete");
                        String preview = draft.getBody().isEmpty() ? "Empty Draft" :
                                (draft.getBody().length() > 30 ? draft.getBody().substring(0, 30) + "..." : draft.getBody());
                        return ItemUtil.buildItem(XMaterial.PAPER, "\u00a7e" + preview, lore.toArray(new String[0]));
                    })
                    .consumer(e -> {
                        Player clicker = (Player) e.getWhoClicked();
                        if (e.isShiftClick()) {
                            plugin.getContractManager().deleteDraft(draft.getContractId());
                            clicker.sendMessage("\u00a7cDraft deleted.");
                            plugin.getGUIManager().openGUI(new ContractDraftsGUI(plugin, page), clicker);
                        } else {
                            plugin.getContractManager().setActiveDraft(clicker.getUniqueId(), draft);
                            plugin.getGUIManager().openGUI(new ContractCreationGUI(plugin, draft), clicker);
                        }
                    })
            );
        }

        addButton(47, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.EMERALD, "\u00a7aCreate New Draft",
                        "\u00a77Start a new contract draft."))
                .consumer(e -> {
                    Player clicker = (Player) e.getWhoClicked();
                    ContractData newDraft = plugin.getContractManager().createNewDraft(clicker.getUniqueId());
                    plugin.getGUIManager().openGUI(new ContractCreationGUI(plugin, newDraft), clicker);
                })
        );

        if (page > 0) {
            addButton(45, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7ePrevious Page"))
                    .consumer(e -> plugin.getGUIManager().openGUI(
                            new ContractDraftsGUI(plugin, page - 1), (Player) e.getWhoClicked()))
            );
        }

        if (end < drafts.size()) {
            addButton(53, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7eNext Page"))
                    .consumer(e -> plugin.getGUIManager().openGUI(
                            new ContractDraftsGUI(plugin, page + 1), (Player) e.getWhoClicked()))
            );
        }

        addButton(49, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.BARRIER, "\u00a7cBack"))
                .consumer(e -> {
                    Player clicker = (Player) e.getWhoClicked();
                    ContractData active = plugin.getContractManager().getActiveDraft(clicker.getUniqueId());
                    if (active != null) {
                        plugin.getGUIManager().openGUI(new ContractCreationGUI(plugin, active), clicker);
                    } else {
                        clicker.closeInventory();
                    }
                })
        );

        super.decorate(player);
    }
}
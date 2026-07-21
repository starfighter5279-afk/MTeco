package com.dirt.inventory.impl.contract;

import com.cryptomorin.xseries.XMaterial;
import com.dirt.DirtEconomy;
import com.dirt.data.ContractData;
import com.dirt.inventory.InventoryButton;
import com.dirt.inventory.InventoryGUI;
import com.dirt.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class ContractEditBodyOptionsGUI extends InventoryGUI {
    private final DirtEconomy plugin;
    private final ContractData draft;

    public ContractEditBodyOptionsGUI(DirtEconomy plugin, ContractData draft) {
        this.plugin = plugin;
        this.draft = draft;
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 27, "\u00a76Edit Contract Body");
    }

    @Override
    public void decorate(Player player) {
        fillGlass(27, XMaterial.YELLOW_STAINED_GLASS_PANE);

        addButton(11, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.RED_WOOL, "\u00a7cClear All & Restart",
                        "\u00a77Erase the current body and",
                        "\u00a77start writing from scratch."))
                .consumer(e -> {
                    draft.setBody("");
                    ContractCreationGUI.promptBodyInput(plugin, draft, (Player) e.getWhoClicked(), false);
                })
        );

        addButton(13, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.WRITABLE_BOOK, "\u00a7eView as Book",
                        "\u00a77Preview the current contract",
                        "\u00a77body in a readable format."))
                .consumer(e -> {
                    Player clicker = (Player) e.getWhoClicked();
                    plugin.getGUIManager().openGUI(new ContractViewBodyGUI(plugin, draft, 0), clicker);
                })
        );

        addButton(15, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.LIME_WOOL, "\u00a7aAdd More",
                        "\u00a77Keep the existing body and",
                        "\u00a77append additional text."))
                .consumer(e -> ContractCreationGUI.promptBodyInput(plugin, draft, (Player) e.getWhoClicked(), true))
        );

        super.decorate(player);
    }
}
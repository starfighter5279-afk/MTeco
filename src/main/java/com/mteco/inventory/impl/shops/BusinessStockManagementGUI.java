package com.mteco.inventory.impl.shops;

import com.cryptomorin.xseries.XMaterial;
import com.mteco.MTeco;
import com.mteco.inventory.InventoryButton;
import com.mteco.inventory.InventoryGUI;
import com.mteco.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.UUID;

public class BusinessStockManagementGUI extends InventoryGUI {
    private final MTeco plugin;
    private final UUID businessId;

    public BusinessStockManagementGUI(MTeco plugin, UUID businessId) {
        this.plugin = plugin;
        this.businessId = businessId;
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 27, "\u00a7aStock Management");
    }

    @Override
    public void decorate(Player player) {
        fillGlass(27, XMaterial.PURPLE_STAINED_GLASS_PANE);
        addButton(11, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.BARREL, "\u00a7aCurrent Stockrooms",
                        "\u00a77View existing stockrooms and their items."))
                .consumer(e -> plugin.getGUIManager().openGUI(new BusinessStockroomsListGUI(plugin, businessId, 0), (Player) e.getWhoClicked()))
        );
        addButton(15, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.GOLDEN_HOE, "\u00a7eCreate Stockroom",
                        "\u00a77Receive a Stockroom Selection Tool",
                        "\u00a77to mark an area as a stockroom."))
                .consumer(e -> {
                    Player p = (Player) e.getWhoClicked();
                    p.closeInventory();
                    plugin.getStockroomSelectionManager().startSelection(p, businessId);
                })
        );
        addButton(22, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7cBack"))
                .consumer(e -> plugin.getGUIManager().openGUI(new ShopsAndStockGUI(plugin, businessId), (Player) e.getWhoClicked()))
        );
        super.decorate(player);
    }
}
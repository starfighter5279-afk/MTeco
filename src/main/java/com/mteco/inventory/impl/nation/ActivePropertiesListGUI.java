package com.mteco.inventory.impl.nation;

import com.cryptomorin.xseries.XMaterial;
import com.mteco.MTeco;
import com.mteco.data.CharacterData;
import com.mteco.data.ConservationAreaData;
import com.mteco.data.PropertyData;
import com.mteco.inventory.InventoryButton;
import com.mteco.inventory.InventoryGUI;
import com.mteco.util.ItemUtil;
import com.mteco.util.CurrencyUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.List;
import java.util.UUID;

public class ActivePropertiesListGUI extends InventoryGUI {
    private final MTeco plugin;
    private final UUID regionId;

    public ActivePropertiesListGUI(MTeco plugin, UUID regionId) {
        this.plugin = plugin;
        this.regionId = regionId;
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 54, "§aActive Properties");
    }

    @Override
    public void decorate(Player player) {
        fillPagedGui(54, XMaterial.BLUE_STAINED_GLASS_PANE);

        int slot = 0;

        List<ConservationAreaData> conservationAreas = plugin.getNationManager().getConservationAreasByRegion(regionId);
        for (ConservationAreaData area : conservationAreas) {
            if (slot >= 45) break;
            int chunkCount = area.getChunks().size();
            addButton(slot, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.OAK_SAPLING,
                            "§6" + area.getName(),
                            "§eConserved Area",
                            "§7Chunks: §f" + chunkCount))
                    .consumer(e -> {})
            );
            slot++;
        }

        List<PropertyData> properties = plugin.getNationManager().getPropertiesByRegion(regionId);
        for (int i = 0; i < properties.size() && slot < 45; i++) {
            PropertyData prop = properties.get(i);
            CharacterData owner = plugin.getCharacterManager().getCharacter(prop.getOwnerUUID());
            String ownerName = owner != null ? owner.getFirstName() + " " + owner.getLastName() : "Unknown";
            int chunkCount = prop.getChunks().size();
            addButton(slot, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.OAK_DOOR,
                            "§aProperty (" + chunkCount + " chunks)",
                            "§7Owner: §f" + ownerName,
                            "§7For Sale: §f" + (prop.isForSale() ? "§aYes §e($" + CurrencyUtil.symbol() + String.format("%.2f", prop.getSalePrice()) + ")" : "§cNo")))
                    .consumer(e -> {})
            );
            slot++;
        }

        addButton(49, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "§cBack"))
                .consumer(e -> plugin.getGUIManager().openGUI(new GovernorPropertiesGUI(plugin, regionId), (Player) e.getWhoClicked()))
        );

        super.decorate(player);
    }
}
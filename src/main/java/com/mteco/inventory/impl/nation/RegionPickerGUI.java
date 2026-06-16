package com.mteco.inventory.impl.nation;

import com.cryptomorin.xseries.XMaterial;
import com.mteco.MTeco;
import com.mteco.data.RegionData;
import com.mteco.inventory.InventoryButton;
import com.mteco.inventory.InventoryGUI;
import com.mteco.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.List;
import java.util.function.Consumer;

public class RegionPickerGUI extends InventoryGUI {
    private final MTeco plugin;
    private final List<RegionData> regions;
    private final Consumer<RegionData> onSelect;

    public RegionPickerGUI(MTeco plugin, List<RegionData> regions, Consumer<RegionData> onSelect) {
        this.plugin = plugin;
        this.regions = regions;
        this.onSelect = onSelect;
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 54, "§eSelect a Region");
    }

    @Override
    public void decorate(Player player) {
        fillPagedGui(54, XMaterial.BLUE_STAINED_GLASS_PANE);

        for (int i = 0; i < regions.size() && i < 45; i++) {
            RegionData region = regions.get(i);
            addButton(i, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.MAP, "§6" + region.getName(),
                            "§7Chunks: §f" + region.getClaimedChunks().size(),
                            "§7Click to select."))
                    .consumer(e -> {
                        Player p = (Player) e.getWhoClicked();
                        p.closeInventory();
                        plugin.getServer().getScheduler().runTaskLater(plugin, () -> onSelect.accept(region), 1L);
                    })
            );
        }

        super.decorate(player);
    }
}
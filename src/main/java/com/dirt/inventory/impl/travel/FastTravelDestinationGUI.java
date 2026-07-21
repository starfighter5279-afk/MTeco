package com.dirt.inventory.impl.travel;

import com.cryptomorin.xseries.XMaterial;
import com.dirt.DirtEconomy;
import com.dirt.data.TravelDestination;
import com.dirt.inventory.InventoryButton;
import com.dirt.inventory.InventoryGUI;
import com.dirt.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.List;

public class FastTravelDestinationGUI extends InventoryGUI {
    private final DirtEconomy plugin;
    private final String title;
    private final List<TravelDestination> destinations;
    private final int page;

    public FastTravelDestinationGUI(DirtEconomy plugin, String title, List<TravelDestination> destinations, int page) {
        this.plugin = plugin;
        this.title = title;
        this.destinations = destinations;
        this.page = page;
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 54, title);
    }

    @Override
    public void decorate(Player player) {
        fillPagedGui(54, material("PURPLE_STAINED_GLASS_PANE"));

        int perPage = 45;
        int start = page * perPage;
        int end = Math.min(start + perPage, destinations.size());
        for (int index = start; index < end; index++) {
            TravelDestination destination = destinations.get(index);
            addButton(index - start, new InventoryButton()
                    .creator(viewer -> ItemUtil.buildItem(material("ENDER_PEARL"), "§a" + destination.getName(),
                            "§7" + destination.getDescription(),
                            "§dClick to begin fast travel"))
                    .consumer(event -> plugin.getFastTravelManager().beginTravel((Player) event.getWhoClicked(), destination)));
        }

        if (destinations.isEmpty()) {
            addButton(22, new InventoryButton()
                    .creator(viewer -> ItemUtil.buildItem(material("BARRIER"), "§cNo destinations available",
                            "§7You do not have access to any destinations here."))
                    .consumer(event -> {}));
        }

        if (page > 0) {
            addButton(45, new InventoryButton()
                    .creator(viewer -> ItemUtil.buildItem(material("ARROW"), "§ePrevious Page"))
                    .consumer(event -> plugin.getGUIManager().openGUI(new FastTravelDestinationGUI(plugin, title, destinations, page - 1), (Player) event.getWhoClicked())));
        }
        if (end < destinations.size()) {
            addButton(53, new InventoryButton()
                    .creator(viewer -> ItemUtil.buildItem(material("ARROW"), "§eNext Page"))
                    .consumer(event -> plugin.getGUIManager().openGUI(new FastTravelDestinationGUI(plugin, title, destinations, page + 1), (Player) event.getWhoClicked())));
        }

        addButton(49, new InventoryButton()
                .creator(viewer -> ItemUtil.buildItem(material("ARROW"), "§cBack"))
                .consumer(event -> plugin.getGUIManager().openGUI(new FastTravelGUI(plugin, 0), (Player) event.getWhoClicked())));

        super.decorate(player);
    }

    private XMaterial material(String name) {
        return XMaterial.matchXMaterial(name).orElseThrow(() -> new IllegalStateException("Missing material: " + name));
    }
}
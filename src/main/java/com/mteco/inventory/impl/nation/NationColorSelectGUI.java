package com.mteco.inventory.impl.nation;

import com.cryptomorin.xseries.XMaterial;
import com.mteco.MTeco;
import com.mteco.inventory.InventoryButton;
import com.mteco.inventory.InventoryGUI;
import com.mteco.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.function.BiConsumer;

public class NationColorSelectGUI extends InventoryGUI {
    private final MTeco plugin;
    private final String pendingName;
    private final String pendingColor1;
    private final BiConsumer<String, String> onBothSelected;

    private static final Object[][] COLORS = {
            {"§0Black",       XMaterial.BLACK_WOOL,      "§0"},
            {"§1Dark Blue",   XMaterial.BLUE_WOOL,       "§1"},
            {"§2Dark Green",  XMaterial.GREEN_WOOL,      "§2"},
            {"§3Dark Aqua",   XMaterial.CYAN_WOOL,       "§3"},
            {"§4Dark Red",    XMaterial.RED_WOOL,        "§4"},
            {"§5Purple",      XMaterial.PURPLE_WOOL,     "§5"},
            {"§6Gold",        XMaterial.ORANGE_WOOL,     "§6"},
            {"§7Gray",        XMaterial.LIGHT_GRAY_WOOL, "§7"},
            {"§8Dark Gray",   XMaterial.GRAY_WOOL,       "§8"},
            {"§9Blue",        XMaterial.LIGHT_BLUE_WOOL, "§9"},
            {"§aGreen",       XMaterial.LIME_WOOL,       "§a"},
            {"§bAqua",        XMaterial.LIGHT_BLUE_WOOL, "§b"},
            {"§cRed",         XMaterial.RED_WOOL,        "§c"},
            {"§dPink",        XMaterial.MAGENTA_WOOL,    "§d"},
            {"§eYellow",      XMaterial.YELLOW_WOOL,     "§e"},
            {"§fWhite",       XMaterial.WHITE_WOOL,      "§f"},
    };

    public NationColorSelectGUI(MTeco plugin, String pendingName, String pendingColor1, BiConsumer<String, String> onBothSelected) {
        this.plugin = plugin;
        this.pendingName = pendingName;
        this.pendingColor1 = pendingColor1;
        this.onBothSelected = onBothSelected;
    }

    @Override
    protected Inventory createInventory() {
        String step = pendingColor1 == null ? "Primary" : "Secondary";
        return Bukkit.createInventory(null, 27, "§eSelect " + step + " Color");
    }

    @Override
    public void decorate(Player player) {
        fillGlass(27, XMaterial.BLUE_STAINED_GLASS_PANE);

        int[] slots = {1, 2, 3, 4, 5, 6, 7, 10, 11, 12, 13, 14, 15, 16, 19, 20};
        for (int i = 0; i < COLORS.length && i < slots.length; i++) {
            final String label = (String) COLORS[i][0];
            final XMaterial mat = (XMaterial) COLORS[i][1];
            final String code = (String) COLORS[i][2];
            int slot = slots[i];
            addButton(slot, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(mat, label, "§7Click to select this color."))
                    .consumer(e -> {
                        Player p = (Player) e.getWhoClicked();
                        p.closeInventory();
                        if (pendingColor1 == null) {
                            // Picked color1, now pick color2
                            plugin.getServer().getScheduler().runTaskLater(plugin, () ->
                                    plugin.getGUIManager().openGUI(new NationColorSelectGUI(plugin, pendingName, code, onBothSelected), p), 1L);
                        } else {
                            // Picked color2, fire callback
                            plugin.getServer().getScheduler().runTaskLater(plugin, () ->
                                    onBothSelected.accept(pendingColor1, code), 1L);
                        }
                    })
            );
        }

        super.decorate(player);
    }
}
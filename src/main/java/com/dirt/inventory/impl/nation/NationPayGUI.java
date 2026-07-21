package com.dirt.inventory.impl.nation;

import com.cryptomorin.xseries.XMaterial;
import com.dirt.DirtEconomy;
import com.dirt.data.NationData;
import com.dirt.inventory.InventoryButton;
import com.dirt.inventory.InventoryGUI;
import com.dirt.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.UUID;

public class NationPayGUI extends InventoryGUI {
    private final DirtEconomy plugin;
    private final UUID nationId;

    public NationPayGUI(DirtEconomy plugin, UUID nationId) {
        this.plugin = plugin;
        this.nationId = nationId;
    }

    @Override
    protected Inventory createInventory() {
        NationData nation = plugin.getNationManager().loadNation(nationId);
        String name = nation != null ? nation.getColor1() + nation.getName() : "§eNation";
        return Bukkit.createInventory(null, 27, name + " §ePay");
    }

    @Override
    public void decorate(Player player) {
        fillGlass(27, XMaterial.BLUE_STAINED_GLASS_PANE);

        int[] slots = plugin.isDirtBusinessEnabled() ? new int[]{10, 12, 14, 16} : new int[]{11, 13, 15};
        int idx = 0;

        // Nation Members
        addButton(slots[idx++], new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.PLAYER_HEAD, "§eNation Members",
                        "§7Pay a member of your nation."))
                .consumer(e -> plugin.getGUIManager().openGUI(
                        new NationPayMembersGUI(plugin, nationId, 0),
                        (Player) e.getWhoClicked()))
        );

        // Other Nations
        addButton(slots[idx++], new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.GLOBE_BANNER_PATTERN, "§eOther Nations",
                        "§7Send funds to another nation's treasury."))
                .consumer(e -> plugin.getGUIManager().openGUI(
                        new NationPayNationsGUI(plugin, nationId, 0),
                        (Player) e.getWhoClicked()))
        );

        // Active Businesses (only if MTbusiness enabled)
        if (plugin.isDirtBusinessEnabled()) {
            addButton(slots[idx++], new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.CHEST, "§eActive Businesses",
                            "§7Send funds to a business's treasury."))
                    .consumer(e -> plugin.getGUIManager().openGUI(
                            new NationPayBusinessesGUI(plugin, nationId, 0),
                            (Player) e.getWhoClicked()))
            );
        }

        // All MTCs
        addButton(slots[idx], new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.NAME_TAG, "§eAll MTCs",
                        "§7Pay any living DirtEconomy character."))
                .consumer(e -> plugin.getGUIManager().openGUI(
                        new NationPayCharactersGUI(plugin, nationId, 0),
                        (Player) e.getWhoClicked()))
        );

        super.decorate(player);
    }
}
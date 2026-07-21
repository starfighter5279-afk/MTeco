package com.dirt.inventory.impl.nation;

import com.cryptomorin.xseries.XMaterial;
import com.dirt.DirtEconomy;
import com.dirt.data.JailData;
import com.dirt.inventory.InventoryButton;
import com.dirt.inventory.InventoryGUI;
import com.dirt.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.List;
import java.util.UUID;

public class JailManagementGUI extends InventoryGUI {
    private final DirtEconomy plugin;
    private final UUID nationId;

    public JailManagementGUI(DirtEconomy plugin, UUID nationId) {
        this.plugin = plugin;
        this.nationId = nationId;
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 54, "\u00a7eJail Management");
    }

    @Override
    public void decorate(Player player) {
        fillPagedGui(54, XMaterial.RED_STAINED_GLASS_PANE);

        List<JailData> jails = plugin.getLawCrimeManager().getJailsByNation(nationId);
        int end = Math.min(44, jails.size());

        for (int i = 0; i < end; i++) {
            JailData jail = jails.get(i);
            final UUID jailId = jail.getJailId();

            addButton(i, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.IRON_DOOR, "\u00a7e" + jail.getName(),
                            "\'u00a77Cells: \u00a7f" + jail.getCellIds().size(),
                            "\u00a7eClick to manage."))
                    .consumer(e -> {
                        Player p = (Player) e.getWhoClicked();
                        plugin.getGUIManager().openGUI(new JailDetailGUI(plugin, nationId, jailId), p);
                    })
            );
        }

        addButton(47, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.LIME_DYE, "\u00a7aCreate Jail",
                        "\u00a77Select an area for a new jail."))
                .consumer(e -> {
                    Player p = (Player) e.getWhoClicked();
                    p.closeInventory();
                    plugin.getChatInputManager().requestInput(p, "\u00a7eEnter a name for the jail:", jailName -> {
                        if (jailName.isEmpty()) { p.sendMessage("\u00a7cJail name cannot be empty."); return; }
                        plugin.getChunkSelectionManager().startJailCreation(p, nationId, jailName.trim());
                    });
                })
        );

        addButton(49, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7cBack"))
                .consumer(e -> plugin.getGUIManager().openGUI(new GovernmentPropertiesGUI(plugin, nationId, 0), (Player) e.getWhoClicked()))
        );

        super.decorate(player);
    }
}
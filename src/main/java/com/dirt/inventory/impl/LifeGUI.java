package com.dirt.inventory.impl;

import com.cryptomorin.xseries.XMaterial;
import com.dirt.DirtEconomy;
import com.dirt.data.CharacterData;
import com.dirt.inventory.InventoryButton;
import com.dirt.inventory.InventoryGUI;
import com.dirt.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.text.SimpleDateFormat;
import java.util.Date;

public class LifeGUI extends InventoryGUI {
    private final DirtEconomy plugin;

    public LifeGUI(DirtEconomy plugin) {
        this.plugin = plugin;
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 27, "§cLife Management");
    }

    @Override
    public void decorate(Player player) {
        CharacterData data = plugin.getCharacterManager().getCharacter(player.getUniqueId());

        fillGlass(27, XMaterial.RED_STAINED_GLASS_PANE);

        if (data != null) {
            String dateStr = new SimpleDateFormat("MM/dd/yyyy").format(new Date(data.getBirthDate()));
            addButton(4, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.TOTEM_OF_UNDYING, "§6Life Status",
                            "§7Name: §f" + data.getFirstName() + " " + data.getMiddleName() + " " + data.getLastName(),
                            "§7Born: §f" + dateStr,
                            "§7Status: §aAlive"))
                    .consumer(e -> {})
            );
        }

        addButton(13, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.WITHER_SKELETON_SKULL, "§4Create Death",
                        "§7This will end your character's life.",
                        "§cWarning: This action is irreversible!"))
                .consumer(e -> plugin.getGUIManager().openGUI(new LifeConfirmGUI(plugin), (Player) e.getWhoClicked()))
        );

        addButton(26, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "§cBack"))
                .consumer(e -> plugin.getGUIManager().openGUI(new CharacterManagementGUI(plugin), (Player) e.getWhoClicked()))
        );

        super.decorate(player);
    }
}
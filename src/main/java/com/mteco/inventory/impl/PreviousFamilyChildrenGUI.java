package com.mteco.inventory.impl;

import com.cryptomorin.xseries.XMaterial;
import com.mteco.MTeco;
import com.mteco.data.CharacterData;
import com.mteco.data.FamilyData;
import com.mteco.inventory.InventoryButton;
import com.mteco.inventory.InventoryGUI;
import com.mteco.util.ItemUtil;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PreviousFamilyChildrenGUI extends InventoryGUI {
    private final MTeco plugin;
    private final int page;

    public PreviousFamilyChildrenGUI(MTeco plugin, int page) {
        this.plugin = plugin;
        this.page = page;
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 54, "§ePrevious Children");
    }

    @Override
    public void decorate(Player player) {
        fillPagedGui(54, XMaterial.PINK_STAINED_GLASS_PANE);

        CharacterData myData = plugin.getCharacterManager().getCharacter(player.getUniqueId());
        List<UUID> children = collectChildrenAcrossFamilies(myData);

        int perPage = 45;
        int start = page * perPage;
        int end = Math.min(start + perPage, children.size());

        for (int i = start; i < end; i++) {
            UUID childUUID = children.get(i);
            CharacterData child = plugin.getCharacterManager().getCharacter(childUUID);
            int slot = i - start;
            addButton(slot, new InventoryButton()
                    .creator(p -> {
                        if (child == null) {
                            return ItemUtil.buildItem(XMaterial.SKELETON_SKULL, "§7Unknown Child");
                        }
                        String name = child.getFirstName() + " " + child.getMiddleName() + " " + child.getLastName();
                        return ItemUtil.buildItem(XMaterial.PLAYER_HEAD, "§a" + name,
                                "§7Gender: §f" + child.getGender());
                    })
                    .consumer(e -> {})
            );
        }

        if (page > 0) {
            int prevPage = page - 1;
            addButton(45, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "§ePrevious Page"))
                    .consumer(e -> plugin.getGUIManager().openGUI(new PreviousFamilyChildrenGUI(plugin, prevPage), (Player) e.getWhoClicked()))
            );
        }
        if (end < children.size()) {
            int nextPage = page + 1;
            addButton(53, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "§eNext Page"))
                    .consumer(e -> plugin.getGUIManager().openGUI(new PreviousFamilyChildrenGUI(plugin, nextPage), (Player) e.getWhoClicked()))
            );
        }

        addButton(49, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "§cBack"))
                .consumer(e -> plugin.getGUIManager().openGUI(new FamilyGUI(plugin), (Player) e.getWhoClicked()))
        );

        super.decorate(player);
    }

    private List<UUID> collectChildrenAcrossFamilies(CharacterData myData) {
        List<UUID> result = new ArrayList<>();
        if (myData == null) return result;
        for (UUID familyId : myData.getPreviousFamilyIds()) {
            FamilyData family = plugin.getFamilyManager().loadFamily(familyId);
            if (family == null) continue;
            for (UUID childUUID : family.getChildren()) {
                if (!result.contains(childUUID)) {
                    result.add(childUUID);
                }
            }
        }
        return result;
    }
}
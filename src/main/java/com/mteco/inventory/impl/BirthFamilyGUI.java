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

import java.util.List;
import java.util.UUID;

public class BirthFamilyGUI extends InventoryGUI {
    private final MTeco plugin;

    public BirthFamilyGUI(MTeco plugin) {
        this.plugin = plugin;
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 27, "§eBirth Family");
    }

    @Override
    public void decorate(Player player) {
        fillGlass(27, XMaterial.PINK_STAINED_GLASS_PANE);

        CharacterData data = plugin.getCharacterManager().getCharacter(player.getUniqueId());
        UUID birthFamilyId = data != null ? data.getBirthFamilyId() : null;

        if (birthFamilyId == null) {
            addButton(13, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.BARRIER, "§cNo birth family found."))
                    .consumer(e -> {})
            );
            addButton(26, backButton());
            super.decorate(player);
            return;
        }

        FamilyData family = plugin.getFamilyManager().loadFamily(birthFamilyId);
        if (family == null) {
            addButton(13, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.BARRIER, "§cBirth family no longer exists."))
                    .consumer(e -> {})
            );
            addButton(26, backButton());
            super.decorate(player);
            return;
        }

        addButton(4, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.BOOK, "§eBirth Family",
                        "§7The family you were born into."))
                .consumer(e -> {})
        );

        // Parent 1
        if (family.getSpouse1() != null) {
            CharacterData p1 = plugin.getCharacterManager().getCharacter(family.getSpouse1());
            addButton(10, new InventoryButton()
                    .creator(p -> {
                        String name = p1 != null ? p1.getFirstName() + " " + p1.getLastName() : "Unknown";
                        return ItemUtil.buildItem(XMaterial.PLAYER_HEAD, "§dParent (Primary)",
                                "§7" + name);
                    })
                    .consumer(e -> {})
            );
        }

        // Parent 2
        if (family.getSpouse2() != null) {
            CharacterData p2 = plugin.getCharacterManager().getCharacter(family.getSpouse2());
            addButton(16, new InventoryButton()
                    .creator(p -> {
                        String name = p2 != null ? p2.getFirstName() + " " + p2.getLastName() : "Unknown";
                        return ItemUtil.buildItem(XMaterial.PLAYER_HEAD, "§dParent (Secondary)",
                                "§7" + name);
                    })
                    .consumer(e -> {})
            );
        }

        // Siblings (other children in the birth family, excluding the player)
        List<UUID> siblings = family.getChildren().stream()
                .filter(u -> !u.equals(player.getUniqueId()))
                .toList();

        int[] siblingSlots = {18, 19, 20, 21, 22, 23, 24, 25};
        for (int i = 0; i < Math.min(siblings.size(), siblingSlots.length); i++) {
            CharacterData sibling = plugin.getCharacterManager().getCharacter(siblings.get(i));
            final int slot = siblingSlots[i];
            addButton(slot, new InventoryButton()
                    .creator(p -> {
                        String sibName = sibling != null ? sibling.getFirstName() + " " + sibling.getLastName() : "Unknown";
                        return ItemUtil.buildItem(XMaterial.PLAYER_HEAD, "§aSibling",
                                "§7" + sibName);
                    })
                    .consumer(e -> {})
            );
        }

        addButton(26, backButton());
        super.decorate(player);
    }

    private InventoryButton backButton() {
        return new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "§cBack"))
                .consumer(e -> plugin.getGUIManager().openGUI(new FamilyGUI(plugin), (Player) e.getWhoClicked()));
    }
}
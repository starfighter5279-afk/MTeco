package com.dirt.inventory.impl;

import com.cryptomorin.xseries.XMaterial;
import com.dirt.DirtEconomy;
import com.dirt.data.CharacterData;
import com.dirt.data.FamilyData;
import com.dirt.inventory.InventoryButton;
import com.dirt.inventory.InventoryGUI;
import com.dirt.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class InheritorGUI extends InventoryGUI {
    private final DirtEconomy plugin;

    public InheritorGUI(DirtEconomy plugin) {
        this.plugin = plugin;
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 54, "§6Select Inheritor");
    }

    @Override
    public void decorate(Player player) {
        CharacterData myData = plugin.getCharacterManager().getCharacter(player.getUniqueId());

        fillPagedGui(54, XMaterial.YELLOW_STAINED_GLASS_PANE);

        if (myData == null || myData.getFamilyId() == null) {
            player.sendMessage("§cYou are not in a family.");
            player.closeInventory();
            return;
        }

        FamilyData family = plugin.getFamilyManager().loadFamily(myData.getFamilyId());
        if (family == null) { player.closeInventory(); return; }

        List<UUID> members = new ArrayList<>();
        if (family.getSpouse1() != null && !family.getSpouse1().equals(player.getUniqueId())) members.add(family.getSpouse1());
        if (family.getSpouse2() != null && !family.getSpouse2().equals(player.getUniqueId())) members.add(family.getSpouse2());
        members.addAll(family.getChildren());

        int slot = 0;
        for (UUID memberUUID : members) {
            CharacterData memberData = plugin.getCharacterManager().getCharacter(memberUUID);
            if (memberData == null) continue;
            boolean isCurrent = memberUUID.equals(myData.getInheritorUuid());
            final UUID finalMemberUUID = memberUUID;
            final CharacterData finalMemberData = memberData;

            addButton(slot, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.PLAYER_HEAD,
                            "§e" + finalMemberData.getFirstName() + " " + finalMemberData.getLastName(),
                            "§7Gender: §f" + finalMemberData.getGender(),
                            isCurrent ? "§aCurrent Inheritor" : "§7Click to set as inheritor"))
                    .consumer(e -> {
                        Player p = (Player) e.getWhoClicked();
                        CharacterData updatedData = plugin.getCharacterManager().getCharacter(p.getUniqueId());
                        if (updatedData != null) {
                            updatedData.setInheritorUuid(finalMemberUUID);
                            plugin.getCharacterManager().saveCharacter(updatedData);
                            p.sendMessage("§a" + finalMemberData.getFirstName() + " " + finalMemberData.getLastName() + " is now your inheritor.");
                            plugin.getGUIManager().openGUI(new InheritorGUI(plugin), p);
                        }
                    })
            );
            slot++;
        }

        addButton(49, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "§cBack"))
                .consumer(e -> plugin.getGUIManager().openGUI(new FamilyGUI(plugin), (Player) e.getWhoClicked()))
        );

        super.decorate(player);
    }
}
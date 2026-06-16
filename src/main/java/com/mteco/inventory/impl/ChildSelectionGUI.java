package com.mteco.inventory.impl;

import com.cryptomorin.xseries.XMaterial;
import com.mteco.MTeco;
import com.mteco.data.CharacterData;
import com.mteco.data.MailItem;
import com.mteco.inventory.InventoryButton;
import com.mteco.inventory.InventoryGUI;
import com.mteco.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class ChildSelectionGUI extends InventoryGUI {
    private final MTeco plugin;
    private final int page;

    public ChildSelectionGUI(MTeco plugin, int page) {
        this.plugin = plugin;
        this.page = page;
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 54, "§aSelect Child");
    }

    @Override
    public void decorate(Player player) {
        CharacterData myData = plugin.getCharacterManager().getCharacter(player.getUniqueId());

        List<CharacterData> candidates = plugin.getCharacterManager().getAllCharacters().stream()
                .filter(c -> !c.getPlayerUuid().equals(player.getUniqueId()))
                .filter(c -> c.getFamilyId() == null)
                .collect(Collectors.toList());

        fillPagedGui(54, XMaterial.PINK_STAINED_GLASS_PANE);

        int perPage = 45;
        int start = page * perPage;
        int end = Math.min(start + perPage, candidates.size());

        for (int i = start; i < end; i++) {
            CharacterData candidate = candidates.get(i);
            int slot = i - start;
            addButton(slot, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.PLAYER_HEAD,
                            "§a" + candidate.getFirstName() + " " + candidate.getMiddleName() + " " + candidate.getLastName(),
                            "§7Gender: §f" + candidate.getGender(),
                            "§7Click to send child request"))
                    .consumer(e -> sendChildRequest((Player) e.getWhoClicked(), myData, candidate))
            );
        }

        if (page > 0) {
            int prevPage = page - 1;
            addButton(45, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "§ePrevious Page"))
                    .consumer(e -> plugin.getGUIManager().openGUI(new ChildSelectionGUI(plugin, prevPage), (Player) e.getWhoClicked()))
            );
        }

        if (end < candidates.size()) {
            int nextPage = page + 1;
            addButton(53, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "§eNext Page"))
                    .consumer(e -> plugin.getGUIManager().openGUI(new ChildSelectionGUI(plugin, nextPage), (Player) e.getWhoClicked()))
            );
        }

        addButton(49, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.BARRIER, "§cClose"))
                .consumer(e -> e.getWhoClicked().closeInventory())
        );

        super.decorate(player);
    }

    private void sendChildRequest(Player player, CharacterData myData, CharacterData targetData) {
        MailItem mail = new MailItem();
        mail.setId(UUID.randomUUID().toString());
        mail.setType("CHILD_JOIN_REQUEST");
        mail.setFromPlayerUuid(player.getUniqueId());
        mail.setTimestamp(System.currentTimeMillis());
        Map<String, String> data = new HashMap<>();
        if (myData != null) {
            data.put("familyId", myData.getFamilyId() != null ? myData.getFamilyId().toString() : "");
            data.put("fromName", myData.getFirstName() + " " + myData.getLastName());
        }
        mail.setData(data);

        plugin.getCharacterManager().addMailItem(targetData.getPlayerUuid(), mail);

        // Clear pending flag
        if (myData != null) {
            myData.setPendingChildSelection(false);
            plugin.getCharacterManager().saveCharacter(myData);
        }

        player.sendMessage("§aChild request sent to §e" + targetData.getFirstName() + " " + targetData.getLastName() + "§a.");
        player.closeInventory();

        Player targetPlayer = Bukkit.getPlayer(targetData.getPlayerUuid());
        if (targetPlayer != null) {
            targetPlayer.sendMessage("§eYou have received a request to join a family! Check your mailbox (/mtc).");
        }
    }
}
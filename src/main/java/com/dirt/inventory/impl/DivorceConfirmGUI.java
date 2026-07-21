package com.dirt.inventory.impl;

import com.cryptomorin.xseries.XMaterial;
import com.dirt.DirtEconomy;
import com.dirt.data.CharacterData;
import com.dirt.data.FamilyData;
import com.dirt.inventory.InventoryButton;
import com.dirt.inventory.InventoryGUI;
import com.dirt.util.CurrencyUtil;
import com.dirt.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.UUID;

public class DivorceConfirmGUI extends InventoryGUI {
    private final DirtEconomy plugin;

    public DivorceConfirmGUI(DirtEconomy plugin) {
        this.plugin = plugin;
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 27, "§cConfirm Divorce");
    }

    @Override
    public void decorate(Player player) {
        fillGlass(27, XMaterial.RED_STAINED_GLASS_PANE);

        double divorcePayout = plugin.getSettings().getDivorcePayout();

        addButton(4, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.BARRIER, "§cDivorce Confirmation",
                        "§7This will remove your spouse from the family and give",
                        "fffffa77your current spouse fffffa7e" + CurrencyUtil.symbol() + String.format("%.0f", divorcePayout) + "fffffa77.",
                        "§7Are you sure?"))
                .consumer(e -> {})
        );

        addButton(11, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.LIME_WOOL, "§aYes, divorce"))
                .consumer(e -> {
                    Player p = (Player) e.getWhoClicked();
                    CharacterData myData = plugin.getCharacterManager().getCharacter(p.getUniqueId());
                    if (myData == null || myData.getFamilyId() == null) {
                        p.sendMessage("§cYou are not in a family.");
                        p.closeInventory();
                        return;
                    }
                    FamilyData family = plugin.getFamilyManager().loadFamily(myData.getFamilyId());
                    if (family == null) {
                        p.sendMessage("§cFamily data not found.");
                        p.closeInventory();
                        return;
                    }
                    // The filing player is always the PRIMARY; the secondary is removed
                    UUID spouseUUID = p.getUniqueId().equals(family.getSpouse1()) ? family.getSpouse2() : family.getSpouse1();
                    if (spouseUUID != null) {
                        plugin.getEconomy().depositPlayer(Bukkit.getOfflinePlayer(spouseUUID), divorcePayout);
                        Player spousePlayer = Bukkit.getPlayer(spouseUUID);
                        if (spousePlayer != null) {
                            spousePlayer.sendMessage("§cYour spouse has divorced you. You received §a" + CurrencyUtil.symbol() + String.format("%.0f", divorcePayout) + "§c as settlement.");
                        }
                        removeSecondaryFromFamily(family, spouseUUID);
                    }
                    p.sendMessage("§aYou have divorced your spouse. §e" + CurrencyUtil.symbol() + String.format("%.0f", divorcePayout) + " §ahas been paid to them.");
                    p.closeInventory();
                })
        );

        addButton(15, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.RED_WOOL, "§cNo, go back"))
                .consumer(e -> plugin.getGUIManager().openGUI(new FamilyGUI(plugin), (Player) e.getWhoClicked()))
        );

        super.decorate(player);
    }

    private void removeSecondaryFromFamily(FamilyData family, UUID secondaryUUID) {
        // Null out spouse2 (secondary) — keep the family and its children intact
        family.setSpouse2(null);
        plugin.getFamilyManager().saveFamily(family);

        // Record this family in the secondary's history, then detach them
        CharacterData secondaryData = plugin.getCharacterManager().getCharacter(secondaryUUID);
        if (secondaryData != null) {
            secondaryData.getPreviousFamilyIds().add(family.getFamilyId());
            secondaryData.setFamilyId(null);
            secondaryData.setFamilyRole(null);
            secondaryData.setInheritorUuid(null);
            plugin.getCharacterManager().saveCharacter(secondaryData);
        }
    }
}
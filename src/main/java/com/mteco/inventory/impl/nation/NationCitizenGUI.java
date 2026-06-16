package com.mteco.inventory.impl.nation;

import com.cryptomorin.xseries.XMaterial;
import com.mteco.MTeco;
import com.mteco.data.CharacterData;
import com.mteco.data.NationData;
import com.mteco.inventory.InventoryButton;
import com.mteco.inventory.InventoryGUI;
import com.mteco.inventory.impl.nation.CharacterPropertiesGUI;
import com.mteco.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.UUID;

public class NationCitizenGUI extends InventoryGUI {
    private final MTeco plugin;
    private final UUID nationId;

    public NationCitizenGUI(MTeco plugin, UUID nationId) {
        this.plugin = plugin;
        this.nationId = nationId;
    }

    @Override
    protected Inventory createInventory() {
        NationData nation = plugin.getNationManager().loadNation(nationId);
        String name = nation != null ? nation.getColor1() + nation.getName() : "§eNation";
        return Bukkit.createInventory(null, 27, name + " §8Citizen");
    }

    @Override
    public void decorate(Player player) {
        fillGlass(27, XMaterial.BLUE_STAINED_GLASS_PANE);

        addButton(11, new InventoryButton()
                .creator(p -> {
                    NationData nation = plugin.getNationManager().loadNation(nationId);
                    String nationName = nation != null ? nation.getColor1() + nation.getName() : "§eNation";
                    return ItemUtil.buildItem(XMaterial.OAK_DOOR, "§cLeave Nation",
                            "§7Leave " + nationName + "§7.",
                            "§7You will lose access to all",
                            "§7nation properties and regions.",
                            "",
                            "§cClick to leave.");
                })
                .consumer(e -> {
                    Player p = (Player) e.getWhoClicked();
                    NationData nation = plugin.getNationManager().loadNation(nationId);
                    if (nation == null) {
                        p.closeInventory();
                        return;
                    }
                    if (p.getUniqueId().equals(nation.getPresidentUUID())) {
                        p.sendMessage("§cThe president cannot leave the nation.");
                        p.closeInventory();
                        return;
                    }
                    plugin.getNationManager().removeMember(nation, p.getUniqueId());
                    CharacterData character = plugin.getCharacterManager().getCharacter(p.getUniqueId());
                    String charName = character != null ? character.getFirstName() + " " + character.getLastName() : p.getName();
                    p.sendMessage("§aYou have left " + nation.getColor1() + nation.getName() + "§a.");
                    Player president = Bukkit.getPlayer(nation.getPresidentUUID());
                    if (president != null) {
                        president.sendMessage("§e" + charName + "§e has left your nation.");
                    }
                    p.closeInventory();
                })
        );

        addButton(15, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.GRASS_BLOCK, "§aMy Properties",
                        "§7View your owned properties",
                        "§7within this nation."))
                .consumer(e -> plugin.getGUIManager().openGUI(
                        new CharacterPropertiesGUI(plugin, 0, ((Player) e.getWhoClicked()).getUniqueId()),
                        (Player) e.getWhoClicked()))
        );

        super.decorate(player);
    }
}
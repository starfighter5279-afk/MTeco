package com.mteco.inventory.impl.nation;

import com.cryptomorin.xseries.XMaterial;
import com.mteco.MTeco;
import com.mteco.data.NationData;
import com.mteco.inventory.InventoryButton;
import com.mteco.inventory.InventoryGUI;
import com.mteco.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.UUID;

public class NationSecurityGUI extends InventoryGUI {
    private final MTeco plugin;
    private final UUID nationId;

    public NationSecurityGUI(MTeco plugin, UUID nationId) {
        this.plugin = plugin;
        this.nationId = nationId;
    }

    @Override
    protected Inventory createInventory() {
        NationData nation = plugin.getNationManager().loadNation(nationId);
        String name = nation != null ? nation.getColor1() + nation.getName() : "§e Nation";
        return Bukkit.createInventory(null, 27, name + " §8National Security");
    }

    @Override
    public void decorate(Player player) {
        fillGlass(27, XMaterial.RED_STAINED_GLASS_PANE);

        if (plugin.getSettings().isEnforcersEnabled()) {
            addButton(11, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.IRON_CHESTPLATE, "§eManage Enforcers",
                            "§7Hire, fire, and manage enforcers."))
                    .consumer(e -> plugin.getGUIManager().openGUI(
                            new EnforcerManagementGUI(plugin, nationId, () -> new NationSecurityGUI(plugin, nationId), 0),
                            (Player) e.getWhoClicked()))
            );
        }

        if (plugin.getRolesConfig().canSecurityManageWars()) {
            addButton(13, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.SHIELD, "§eManage Current Wars",
                            "§7View and manage your nation's",
                            "§7active wars: surrenders and give-ups."))
                    .consumer(e -> plugin.getGUIManager().openGUI(
                            new ManageWarsGUI(plugin, nationId),
                            (Player) e.getWhoClicked()))
            );
        }

        if (plugin.getSettings().isCrimeEnabled()) {
            addButton(15, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.IRON_BARS, "§eCrime",
                            "§7Manage criminals, convictions, and jails."))
                    .consumer(e -> plugin.getGUIManager().openGUI(
                            new CrimeManagementGUI(plugin, nationId, () -> new NationSecurityGUI(plugin, nationId)),
                            (Player) e.getWhoClicked()))
            );
        }

        super.decorate(player);
    }
}
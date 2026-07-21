package com.dirt.inventory.impl.nation;

import com.cryptomorin.xseries.XMaterial;
import com.dirt.DirtEconomy;
import com.dirt.inventory.InventoryButton;
import com.dirt.inventory.InventoryGUI;
import com.dirt.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.UUID;
import java.util.function.Supplier;

public class MemberManagementGUI extends InventoryGUI {
    private final DirtEconomy plugin;
    private final UUID nationId;

    public MemberManagementGUI(DirtEconomy plugin, UUID nationId) {
        this.plugin = plugin;
        this.nationId = nationId;
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 27, "\u00a7eMember Management");
    }

    @Override
    public void decorate(Player player) {
        fillGlass(27, XMaterial.BLUE_STAINED_GLASS_PANE);

        if (plugin.getSettings().isCustomGovernmentEnabled()) {
            addButton(11, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.NAME_TAG, "\u00a7bNation Roles",
                            "\u00a77Create and manage custom roles."))
                    .consumer(e -> plugin.getGUIManager().openGUI(new CustomRolesGUI(plugin, nationId), (Player) e.getWhoClicked()))
            );
        } else if (plugin.getRolesConfig().isElitesEnabled()) {
            addButton(11, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.DIAMOND, "\u00a7bNation Elites",
                            "\u00a77Manage Treasurer, VP, and Security Head."))
                    .consumer(e -> plugin.getGUIManager().openGUI(new NationElitesGUI(plugin, nationId), (Player) e.getWhoClicked()))
            );
        } else {
            addButton(11, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.GRAY_DYE, "\u00a77Nation Elites", "\u00a7cAll elite roles are disabled."))
                    .consumer(e -> {})
            );
        }

        addButton(13, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.IRON_SWORD, "\u00a7cRegion Governors",
                        "\u00a77View and elect region governors."))
                .consumer(e -> plugin.getGUIManager().openGUI(new RegionGovernorsGUI(plugin, nationId), (Player) e.getWhoClicked()))
        );

        addButton(15, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.PLAYER_HEAD, "\u00a7aMembers",
                        "\u00a77View members and send nation invites."))
                .consumer(e -> plugin.getGUIManager().openGUI(new NationMembersGUI(plugin, nationId, 0), (Player) e.getWhoClicked()))
        );

        if (plugin.getSettings().isEnforcersEnabled()) {
            addButton(22, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.IRON_CHESTPLATE, "\u00a7eManage Enforcers",
                            "\u00a77Hire, fire, and manage enforcers."))
                    .consumer(e -> {
                        Supplier<InventoryGUI> back = () -> new MemberManagementGUI(plugin, nationId);
                        plugin.getGUIManager().openGUI(
                                new EnforcerManagementGUI(plugin, nationId, back, 0),
                                (Player) e.getWhoClicked());
                    })
            );
        }

        addButton(26, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7cBack"))
                .consumer(e -> plugin.getGUIManager().openGUI(new NationManagementGUI(plugin, nationId), (Player) e.getWhoClicked()))
        );

        super.decorate(player);
    }
}
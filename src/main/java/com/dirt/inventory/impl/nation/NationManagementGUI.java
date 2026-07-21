package com.dirt.inventory.impl.nation;

import com.cryptomorin.xseries.XMaterial;
import com.dirt.DirtEconomy;
import com.dirt.data.NationData;
import com.dirt.inventory.InventoryButton;
import com.dirt.inventory.InventoryGUI;
import com.dirt.util.ItemUtil;
import com.dirt.util.CurrencyUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.UUID;

public class NationManagementGUI extends InventoryGUI {
    private final DirtEconomy plugin;
    private final UUID nationId;

    public NationManagementGUI(DirtEconomy plugin, UUID nationId) {
        this.plugin = plugin;
        this.nationId = nationId;
    }

    @Override
    protected Inventory createInventory() {
        NationData nation = plugin.getNationManager().loadNation(nationId);
        String name = nation != null ? nation.getColor1() + nation.getName() : "\u00a7eNation";
        return Bukkit.createInventory(null, 54, name + " \u00a78Management");
    }

    @Override
    public void decorate(Player player) {
        fillGlass(54, XMaterial.BLUE_STAINED_GLASS_PANE);

        // ── Row 0: Nation Info ──
        addButton(4, new InventoryButton()
                .creator(p -> {
                    NationData n = plugin.getNationManager().loadNation(nationId);
                    int members = n != null ? n.getMemberUUIDs().size() : 0;
                    int regions = n != null ? n.getRegionIds().size() : 0;
                    double balance = n != null ? plugin.getNationManager().getTreasuryBalance(n.getName()) : 0.0;
                    String nationName = n != null ? n.getColor1() + n.getName() : "\u00a7eNation";
                    return ItemUtil.buildItem(XMaterial.PLAYER_HEAD, nationName,
                            "\u00a77Members: \u00a7f" + members,
                            "\u00a77Regions: \u00a7f" + regions,
                            "\u00a77Treasury: \u00a7a" + CurrencyUtil.symbol() + String.format("%.2f", balance));
                })
                .consumer(e -> {})
        );

        // ── Row 1: Government ──
        addButton(9, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.ORANGE_STAINED_GLASS_PANE, "\u00a76\u00a7lGovernment"))
                .consumer(e -> {})
        );

        addButton(10, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.PLAYER_HEAD, "\u00a7eManage Members",
                        "\u00a77Manage nation elites,",
                        "\u00a77governors, and members."))
                .consumer(e -> plugin.getGUIManager().openGUI(new MemberManagementGUI(plugin, nationId), (Player) e.getWhoClicked()))
        );

        if (plugin.getSettings().isCustomGovernmentEnabled()) {
            addButton(11, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.NAME_TAG, "\u00a7bCustom Roles",
                            "\u00a77Create and manage custom",
                            "\u00a77nation roles and permissions."))
                    .consumer(e -> plugin.getGUIManager().openGUI(new CustomRolesGUI(plugin, nationId), (Player) e.getWhoClicked()))
            );
        }

        // ── Row 2: Territory ──
        addButton(18, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.GREEN_STAINED_GLASS_PANE, "\u00a72\u00a7lTerritory"))
                .consumer(e -> {})
        );

        addButton(19, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.MAP, "\u00a7eManage Regions",
                        "\u00a77Create and manage your",
                        "\u00a77nation's regions."))
                .consumer(e -> plugin.getGUIManager().openGUI(new RegionsGUI(plugin, nationId), (Player) e.getWhoClicked()))
        );

        addButton(20, new InventoryButton()
                .creator(p -> {
                    NationData n = plugin.getNationManager().loadNation(nationId);
                    int count = n != null ? n.getGovernmentPropertyIds().size() : 0;
                    return ItemUtil.buildItem(XMaterial.DARK_OAK_DOOR, "\u00a7eGovernment Properties",
                            "\u00a77Properties: \u00a7f" + count,
                            "\u00a77Manage government-owned properties.");
                })
                .consumer(e -> plugin.getGUIManager().openGUI(new GovernmentPropertiesGUI(plugin, nationId, 0), (Player) e.getWhoClicked()))
        );

        // ── Row 3: Economy ──
        addButton(27, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.YELLOW_STAINED_GLASS_PANE, "\u00a7e\u00a7lEconomy"))
                .consumer(e -> {})
        );

        addButton(28, new InventoryButton()
                .creator(p -> {
                    NationData n = plugin.getNationManager().loadNation(nationId);
                    double balance = n != null ? plugin.getNationManager().getTreasuryBalance(n.getName()) : 0.0;
                    return ItemUtil.buildItem(XMaterial.GOLD_INGOT, "\u00a76National Treasury",
                            "\u00a77Balance: \u00a7a" + CurrencyUtil.symbol() + String.format("%.2f", balance),
                            "\u00a77View transactions and manage funds.");
                })
                .consumer(e -> plugin.getGUIManager().openGUI(new NationTreasuryGUI(plugin, nationId, true), (Player) e.getWhoClicked()))
        );

        addButton(29, new InventoryButton()
                .creator(p -> {
                    NationData n = plugin.getNationManager().loadNation(nationId);
                    double rate = n != null ? n.getTaxRate() : 0.0;
                    return ItemUtil.buildItem(XMaterial.GOLD_NUGGET, "\u00a7eManage Tax",
                            "\u00a77Current rate: \u00a7a" + String.format("%.1f", rate) + "%",
                            "\u00a77Click to change the tax rate.");
                })
                .consumer(e -> plugin.getGUIManager().openGUI(new NationwideTaxGUI(plugin, nationId), (Player) e.getWhoClicked()))
        );

        if (plugin.getSettings().isMintersEnabled()) {
            addButton(30, new InventoryButton()
                    .creator(p -> {
                        int count = plugin.getMinterManager() != null
                                ? plugin.getMinterManager().getMintersByNation(nationId).size() : 0;
                        return ItemUtil.buildItem(XMaterial.CAULDRON, "\u00a76Minters",
                                "\u00a77Active minters: \u00a7f" + count,
                                "\u00a77Click to create a new minter.");
                    })
                    .consumer(e -> {
                        Player p = (Player) e.getWhoClicked();
                        p.closeInventory();
                        plugin.getLocationSelectionManager().requestLocation(p,
                                "\u00a7eRight-click a block to place the minter base.", loc -> {
                                    plugin.getMinterManager().createMinterAtLocation(p, nationId, loc);
                                });
                    })
            );
        }

        // ── Row 4: Security ──
        addButton(36, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.RED_STAINED_GLASS_PANE, "\u00a7c\u00a7lSecurity"))
                .consumer(e -> {})
        );

        if (plugin.getSettings().isLawsEnabled() || plugin.getSettings().isCrimeEnabled()) {
            addButton(37, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.WRITABLE_BOOK, "\u00a7eLaws & Crime",
                            "\u00a77Laws, legislated crimes,",
                            "\u00a77and enforcement."))
                    .consumer(e -> plugin.getGUIManager().openGUI(
                            new LawsAndCrimeMenuGUI(plugin, nationId, true), (Player) e.getWhoClicked()))
            );
        }

        addButton(38, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.DIAMOND_SWORD, "\u00a7cWar",
                        "\u00a77Declare war on another nation",
                        "\u00a77or manage current wars."))
                .consumer(e -> plugin.getGUIManager().openGUI(new NationWarGUI(plugin, nationId), (Player) e.getWhoClicked()))
        );

        // ── Row 5: Navigation ──
        addButton(49, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.BARRIER, "\u00a7cClose"))
                .consumer(e -> ((Player) e.getWhoClicked()).closeInventory())
        );

        super.decorate(player);
    }
}
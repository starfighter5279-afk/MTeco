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

public class LawsAndCrimeMenuGUI extends InventoryGUI {
    private final MTeco plugin;
    private final UUID nationId;
    private final boolean fromPresident;

    public LawsAndCrimeMenuGUI(MTeco plugin, UUID nationId, boolean fromPresident) {
        this.plugin = plugin;
        this.nationId = nationId;
        this.fromPresident = fromPresident;
    }

    @Override
    protected Inventory createInventory() {
        NationData nation = plugin.getNationManager().loadNation(nationId);
        String name = nation != null ? nation.getColor1() + nation.getName() : "\u00a7eNation";
        return Bukkit.createInventory(null, 27, name + " \u00a78Laws & Crime");
    }

    @Override
    public void decorate(Player player) {
        fillGlass(27, XMaterial.RED_STAINED_GLASS_PANE);

        boolean fp = fromPresident;

        if (plugin.getSettings().isEnforcersEnabled()) {
            addButton(11, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.IRON_CHESTPLATE, "\u00a7eManage Enforcers",
                            "\u00a77Hire, fire, and manage enforcers."))
                    .consumer(e -> plugin.getGUIManager().openGUI(
                            new EnforcerManagementGUI(plugin, nationId,
                                    () -> new LawsAndCrimeMenuGUI(plugin, nationId, fp), 0),
                            (Player) e.getWhoClicked()))
            );
        }

        if (plugin.getSettings().isLawsEnabled()) {
            addButton(13, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.BOOK, "\u00a7eLaws",
                            "\u00a77View and create law books,",
                            "\u00a77manage legislated crimes."))
                    .consumer(e -> plugin.getGUIManager().openGUI(
                            new LawsGUI(plugin, nationId, fp),
                            (Player) e.getWhoClicked()))
            );
        }

        if (plugin.getSettings().isCrimeEnabled()) {
            addButton(15, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.IRON_BARS, "\u00a7cCrime",
                            "\u00a77Convict criminals, manage jails."))
                    .consumer(e -> plugin.getGUIManager().openGUI(
                            new CrimeManagementGUI(plugin, nationId,
                                    () -> new LawsAndCrimeMenuGUI(plugin, nationId, fp)),
                            (Player) e.getWhoClicked()))
            );
        }

        addButton(22, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7cBack"))
                .consumer(e -> {
                    Player p = (Player) e.getWhoClicked();
                    if (fp) {
                        plugin.getGUIManager().openGUI(new NationManagementGUI(plugin, nationId), p);
                    } else {
                        plugin.getGUIManager().openGUI(new VicePresidentManagementGUI(plugin, nationId), p);
                    }
                })
        );

        super.decorate(player);
    }
}
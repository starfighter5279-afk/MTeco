package com.dirt.inventory.impl.nation;

import com.cryptomorin.xseries.XMaterial;
import com.dirt.DirtEconomy;
import com.dirt.data.CharacterData;
import com.dirt.data.NationData;
import com.dirt.inventory.InventoryButton;
import com.dirt.inventory.InventoryGUI;
import com.dirt.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public class CrimeManagementGUI extends InventoryGUI {
    private final DirtEconomy plugin;
    private final UUID nationId;
    private final Supplier<InventoryGUI> backSupplier;

    public CrimeManagementGUI(DirtEconomy plugin, UUID nationId, Supplier<InventoryGUI> backSupplier) {
        this.plugin = plugin;
        this.nationId = nationId;
        this.backSupplier = backSupplier;
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 27, "\u00a7eNation Crime Management");
    }

    @Override
    public void decorate(Player player) {
        fillGlass(27, XMaterial.RED_STAINED_GLASS_PANE);

        NationData nation = plugin.getNationManager().loadNation(nationId);
        if (nation == null) { super.decorate(player); return; }

        Supplier<InventoryGUI> bs = backSupplier;

        addButton(11, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.DIAMOND_SWORD, "\u00a7cConvict Criminal",
                        "\u00a77Select a nation member to convict."))
                .consumer(e -> {
                    Player p = (Player) e.getWhoClicked();
                    List<CharacterData> candidates = new ArrayList<>();
                    for (UUID memberUUID : nation.getMemberUUIDs()) {
                        CharacterData cd = plugin.getCharacterManager().getCharacter(memberUUID);
                        if (cd != null) candidates.add(cd);
                    }
                    plugin.getGUIManager().openGUI(new NationCharacterSelectGUI(plugin, "\u00a7eSelect Criminal", candidates, selected -> {
                        plugin.getGUIManager().openGUI(new ConvictSelectCrimesGUI(plugin, nationId, selected.getPlayerUuid(), bs), p);
                    }, null, 0), p);
                })
        );

        addButton(13, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.matchXMaterial("CHAIN").orElse(XMaterial.IRON_BARS), "\u00a7eManage Criminals",
                        "\u00a77View and manage convicted criminals."))
                .consumer(e -> plugin.getGUIManager().openGUI(new ManageCriminalsGUI(plugin, nationId, bs, 0), (Player) e.getWhoClicked()))
        );

        addButton(15, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.IRON_DOOR, "\u00a7eJail Management",
                        "\u00a77Manage jails and cells."))
                .consumer(e -> plugin.getGUIManager().openGUI(
                        new JailManagementGUI(plugin, nationId), (Player) e.getWhoClicked()))
        );

        addButton(22, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7cBack"))
                .consumer(e -> plugin.getGUIManager().openGUI(bs.get(), (Player) e.getWhoClicked()))
        );

        super.decorate(player);
    }
}
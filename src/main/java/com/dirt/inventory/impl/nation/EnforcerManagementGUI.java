package com.dirt.inventory.impl.nation;

import com.cryptomorin.xseries.XMaterial;
import com.dirt.DirtEconomy;
import com.dirt.data.CharacterData;
import com.dirt.data.NationData;
import com.dirt.inventory.InventoryButton;
import com.dirt.inventory.InventoryGUI;
import com.dirt.util.ItemUtil;
import com.dirt.util.CurrencyUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public class EnforcerManagementGUI extends InventoryGUI {
    private final DirtEconomy plugin;
    private final UUID nationId;
    private final Supplier<InventoryGUI> backSupplier;
    private final int page;

    public EnforcerManagementGUI(DirtEconomy plugin, UUID nationId, Supplier<InventoryGUI> backSupplier, int page) {
        this.plugin = plugin;
        this.nationId = nationId;
        this.backSupplier = backSupplier;
        this.page = page;
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 54, "\u00a7eEnforcer Management");
    }

    @Override
    public void decorate(Player player) {
        fillPagedGui(54, XMaterial.RED_STAINED_GLASS_PANE);

        NationData nation = plugin.getNationManager().loadNation(nationId);
        if (nation == null) { super.decorate(player); return; }

        List<UUID> enforcers = nation.getEnforcerUUIDs();
        int perPage = 44;
        int start = page * perPage;
        int end = Math.min(start + perPage, enforcers.size());

        for (int i = start; i < end; i++) {
            UUID enforcerUUID = enforcers.get(i);
            CharacterData charData = plugin.getCharacterManager().getCharacter(enforcerUUID);
            String name = charData != null ? charData.getFirstName() + " " + charData.getLastName() : Bukkit.getOfflinePlayer(enforcerUUID).getName();
            double salary = nation.getEnforcerSalaries().getOrDefault(enforcerUUID, 0.0);
            int caught = nation.getEnforcerCriminalsCaught().getOrDefault(enforcerUUID, 0);
            final String displayName = name;

            addButton(i - start, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.IRON_CHESTPLATE, "\u00a7e" + displayName,
                            "\u00a77Salary: \u00a7a" + CurrencyUtil.symbol() + String.format("%.2f", salary),
                            "\u00a77Criminals Caught: \u00a7f" + caught,
                            "\u00a77Click to manage."))
                    .consumer(e -> plugin.getGUIManager().openGUI(
                            new EnforcerDetailGUI(plugin, nationId, enforcerUUID, backSupplier),
                            (Player) e.getWhoClicked()))
            );
        }

        if (page > 0) {
            int prev = page - 1;
            addButton(45, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7ePrevious"))
                    .consumer(e -> plugin.getGUIManager().openGUI(new EnforcerManagementGUI(plugin, nationId, backSupplier, prev), (Player) e.getWhoClicked()))
            );
        }
        if (end < enforcers.size()) {
            int next = page + 1;
            addButton(53, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7eNext"))
                    .consumer(e -> plugin.getGUIManager().openGUI(new EnforcerManagementGUI(plugin, nationId, backSupplier, next), (Player) e.getWhoClicked()))
            );
        }

        addButton(48, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.EMERALD, "\u00a7aHire Enforcer",
                        "\u00a77Select a nation member to hire."))
                .consumer(e -> {
                    Player p = (Player) e.getWhoClicked();
                    List<CharacterData> candidates = new ArrayList<>();
                    for (UUID memberUUID : nation.getMemberUUIDs()) {
                        if (nation.getEnforcerUUIDs().contains(memberUUID)) continue;
                        if (memberUUID.equals(nation.getPresidentUUID())) continue;
                        if (memberUUID.equals(nation.getTreasurerUUID())) continue;
                        if (memberUUID.equals(nation.getVicePresidentUUID())) continue;
                        if (memberUUID.equals(nation.getSecurityHeadUUID())) continue;
                        CharacterData cd = plugin.getCharacterManager().getCharacter(memberUUID);
                        if (cd != null) candidates.add(cd);
                    }
                    if (candidates.isEmpty()) {
                        p.sendMessage("\u00a7cNo eligible nation members to hire.");
                        return;
                    }
                    Supplier<InventoryGUI> bs = backSupplier;
                    plugin.getGUIManager().openGUI(new NationCharacterSelectGUI(plugin, "\u00a7eSelect Enforcer", candidates, selected -> {
                        NationData n = plugin.getNationManager().loadNation(nationId);
                        if (n == null) return;
                        n.getEnforcerUUIDs().add(selected.getPlayerUuid());
                        n.getEnforcerSalaries().put(selected.getPlayerUuid(), 0.0);
                        plugin.getNationManager().saveNation(n);
                        p.sendMessage("\u00a7a" + selected.getFirstName() + " " + selected.getLastName() + " hired as enforcer.");
                        Player hired = Bukkit.getPlayer(selected.getPlayerUuid());
                        if (hired != null) hired.sendMessage("\u00a7eYou have been hired as an enforcer for your nation.");
                        plugin.getGUIManager().openGUI(new EnforcerManagementGUI(plugin, nationId, bs, 0), p);
                    }, null, 0), p);
                })
        );

        addButton(49, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7cBack"))
                .consumer(e -> plugin.getGUIManager().openGUI(backSupplier.get(), (Player) e.getWhoClicked()))
        );

        super.decorate(player);
    }
}
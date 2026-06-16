package com.mteco.inventory.impl.nation;

import com.cryptomorin.xseries.XMaterial;
import com.mteco.MTeco;
import com.mteco.data.CharacterData;
import com.mteco.data.NationData;
import com.mteco.inventory.InventoryButton;
import com.mteco.inventory.InventoryGUI;
import com.mteco.util.ItemUtil;
import com.mteco.util.CurrencyUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.UUID;
import java.util.function.Supplier;

public class EnforcerDetailGUI extends InventoryGUI {
    private final MTeco plugin;
    private final UUID nationId;
    private final UUID enforcerUUID;
    private final Supplier<InventoryGUI> backSupplier;

    public EnforcerDetailGUI(MTeco plugin, UUID nationId, UUID enforcerUUID, Supplier<InventoryGUI> backSupplier) {
        this.plugin = plugin;
        this.nationId = nationId;
        this.enforcerUUID = enforcerUUID;
        this.backSupplier = backSupplier;
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 27, "\u00a7eEnforcer Details");
    }

    @Override
    public void decorate(Player player) {
        fillGlass(27, XMaterial.RED_STAINED_GLASS_PANE);

        NationData nation = plugin.getNationManager().loadNation(nationId);
        if (nation == null) { super.decorate(player); return; }

        CharacterData charData = plugin.getCharacterManager().getCharacter(enforcerUUID);
        String name = charData != null ? charData.getFirstName() + " " + charData.getLastName() : Bukkit.getOfflinePlayer(enforcerUUID).getName();
        double salary = nation.getEnforcerSalaries().getOrDefault(enforcerUUID, 0.0);
        int caught = nation.getEnforcerCriminalsCaught().getOrDefault(enforcerUUID, 0);

        addButton(4, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.PLAYER_HEAD, "\u00a7e" + name,
                        "\u00a77Salary: \u00a7a" + CurrencyUtil.symbol() + String.format("%.2f", salary),
                        "\u00a77Criminals Caught: \u00a7f" + caught))
                .consumer(e -> {})
        );

        addButton(11, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.GOLD_INGOT, "\u00a76Give Raise",
                        "\u00a77Current salary: \u00a7a" + CurrencyUtil.symbol() + String.format("%.2f", salary),
                        "\u00a77Click to set new salary."))
                .consumer(e -> {
                    Player p = (Player) e.getWhoClicked();
                    p.closeInventory();
                    plugin.getChatInputManager().requestInput(p, "\u00a7eEnter new salary amount:", input -> {
                        double newSalary;
                        try { newSalary = Double.parseDouble(input); } catch (NumberFormatException ex) {
                            p.sendMessage("\u00a7cInvalid amount."); return;
                        }
                        if (newSalary < 0) { p.sendMessage("\u00a7cSalary cannot be negative."); return; }
                        NationData n = plugin.getNationManager().loadNation(nationId);
                        if (n == null) return;
                        n.getEnforcerSalaries().put(enforcerUUID, newSalary);
                        plugin.getNationManager().saveNation(n);
                        p.sendMessage("\u00a7aSalary updated to \u00a7e" + CurrencyUtil.symbol() + String.format("%.2f", newSalary));
                        plugin.getGUIManager().openGUI(new EnforcerDetailGUI(plugin, nationId, enforcerUUID, backSupplier), p);
                    });
                })
        );

        addButton(15, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.REDSTONE, "\u00a7cFire Enforcer",
                        "\u00a77Remove this enforcer from duty."))
                .consumer(e -> {
                    Player p = (Player) e.getWhoClicked();
                    NationData n = plugin.getNationManager().loadNation(nationId);
                    if (n == null) return;
                    n.getEnforcerUUIDs().remove(enforcerUUID);
                    n.getEnforcerSalaries().remove(enforcerUUID);
                    plugin.getNationManager().saveNation(n);
                    p.sendMessage("\u00a7cEnforcer has been fired.");
                    Player fired = Bukkit.getPlayer(enforcerUUID);
                    if (fired != null) fired.sendMessage("\u00a7cYou have been relieved of your enforcer duties.");
                    plugin.getGUIManager().openGUI(new EnforcerManagementGUI(plugin, nationId, backSupplier, 0), p);
                })
        );

        addButton(22, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7cBack"))
                .consumer(e -> plugin.getGUIManager().openGUI(new EnforcerManagementGUI(plugin, nationId, backSupplier, 0), (Player) e.getWhoClicked()))
        );

        super.decorate(player);
    }
}
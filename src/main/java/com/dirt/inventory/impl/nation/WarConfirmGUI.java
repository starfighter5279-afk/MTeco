package com.dirt.inventory.impl.nation;

import com.cryptomorin.xseries.XMaterial;
import com.dirt.DirtEconomy;
import com.dirt.data.NationData;
import com.dirt.data.PropertyData;
import com.dirt.data.RegionData;
import com.dirt.data.WarData;
import com.dirt.inventory.InventoryButton;
import com.dirt.inventory.InventoryGUI;
import com.dirt.util.ItemUtil;
import com.dirt.util.CurrencyUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class WarConfirmGUI extends InventoryGUI {
    private final DirtEconomy plugin;
    private final UUID attackingNationId;
    private final UUID defendingNationId;
    private final List<UUID> attackerRegionIds;
    private final List<UUID> targetRegionIds;

    public WarConfirmGUI(DirtEconomy plugin, UUID attackingNationId, UUID defendingNationId,
                         List<UUID> attackerRegionIds, List<UUID> targetRegionIds) {
        this.plugin = plugin;
        this.attackingNationId = attackingNationId;
        this.defendingNationId = defendingNationId;
        this.attackerRegionIds = new ArrayList<>(attackerRegionIds);
        this.targetRegionIds = new ArrayList<>(targetRegionIds);
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 27, "§cConfirm War Declaration");
    }

    @Override
    public void decorate(Player player) {
        fillGlass(27, XMaterial.RED_STAINED_GLASS_PANE);

        NationData defending = plugin.getNationManager().loadNation(defendingNationId);
        String defName = defending != null ? defending.getColor1() + defending.getName() : "Unknown";

        List<String> lore = new ArrayList<>();
        lore.add("§eTarget Nation: " + defName);
        lore.add("§eAttacking Regions:");
        for (UUID rid : attackerRegionIds) {
            RegionData r = plugin.getNationManager().loadRegion(rid);
            if (r != null) lore.add("  §7- " + r.getName());
        }
        lore.add("§eRegions to Claim:");
        for (UUID rid : targetRegionIds) {
            RegionData r = plugin.getNationManager().loadRegion(rid);
            if (r != null) lore.add("  §7- " + r.getName());
        }
        double warCost = plugin.getSettings().getWarCost();
        lore.add("§eCost: §a" + CurrencyUtil.symbol() + String.format("%.0f", warCost));
        lore.add("");
        lore.add("§aClick to confirm and declare war.");

        addButton(11, new InventoryButton()
                .creator(p -> {
                    ItemStack item = XMaterial.matchXMaterial("DIAMOND_SWORD")
                            .map(XMaterial::parseItem)
                            .orElse(new ItemStack(org.bukkit.Material.STONE));
                    ItemMeta meta = item.getItemMeta();
                    if (meta != null) {
                        meta.setDisplayName("§cDeclare War");
                        meta.setLore(lore);
                        item.setItemMeta(meta);
                    }
                    return item;
                })
                .consumer(e -> {
                    Player clicker = (Player) e.getWhoClicked();
                    NationData attacker = plugin.getNationManager().loadNation(attackingNationId);
                    NationData def = plugin.getNationManager().loadNation(defendingNationId);
                    if (attacker == null) return;

                    if (!plugin.getEconomy().has(clicker, warCost)) {
                        clicker.sendMessage("§cYou need §a" + CurrencyUtil.symbol() + String.format("%.0f", warCost) + "§c to declare war.");
                        return;
                    }

                    plugin.getEconomy().withdrawPlayer(clicker, warCost);
                    WarData war = plugin.getWarManager().createWar(
                            attackingNationId, defendingNationId, attackerRegionIds, targetRegionIds);

                    clicker.sendMessage("§cWar declared against " + defName + "§c!");
                    clicker.closeInventory();

                    notifyDefendingLeadership(war, attacker, def);
                    notifyAttackerMembers(war, attacker);
                    notifyDefenderMembers(war, def);
                })
        );

        addButton(15, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.BARRIER, "§cCancel"))
                .consumer(e -> plugin.getGUIManager().openGUI(
                        new TargetRegionSelectGUI(plugin, attackingNationId, defendingNationId, attackerRegionIds, targetRegionIds),
                        (Player) e.getWhoClicked()))
        );

        super.decorate(player);
    }

    private void notifyDefendingLeadership(WarData war, NationData attacker, NationData defending) {
        if (defending == null) return;

        List<String> attackedRegionLines = new ArrayList<>();
        for (UUID rid : war.getTargetRegionIds()) {
            RegionData r = plugin.getNationManager().loadRegion(rid);
            if (r != null) attackedRegionLines.add("§c- " + r.getName());
        }

        List<UUID> leaders = new ArrayList<>();
        if (defending.getPresidentUUID() != null) leaders.add(defending.getPresidentUUID());
        if (plugin.getRolesConfig().isVicePresidentEnabled() && defending.getVicePresidentUUID() != null) leaders.add(defending.getVicePresidentUUID());
        if (plugin.getRolesConfig().isSecurityHeadEnabled() && defending.getSecurityHeadUUID() != null) leaders.add(defending.getSecurityHeadUUID());

        for (UUID uuid : leaders) {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null) continue;
            p.sendMessage("§c⚔ WAR DECLARED! §e" + attacker.getColor1() + attacker.getName()
                    + "§e has declared war on your nation!");
            p.sendMessage("§eRegions under attack:");
            for (String line : attackedRegionLines) p.sendMessage(line);
        }
    }

    private void notifyAttackerMembers(WarData war, NationData attacker) {
        Set<UUID> members = plugin.getWarManager().getAttackerMembers(war);
        for (UUID uuid : members) {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null) continue;
            p.sendMessage("§e⚔ Your nation " + attacker.getColor1() + attacker.getName()
                    + "§e has declared war. Your region is an attacking force.");
            p.sendMessage("§7You may now build/break in the enemy's target regions.");
        }
    }

    private void notifyDefenderMembers(WarData war, NationData defending) {
        if (defending == null) return;
        for (UUID regionId : war.getTargetRegionIds()) {
            RegionData region = plugin.getNationManager().loadRegion(regionId);
            if (region == null) continue;
            for (PropertyData prop : plugin.getNationManager().getPropertiesByRegion(regionId)) {
                if (prop.getOwnerUUID() == null) continue;
                Player p = Bukkit.getPlayer(prop.getOwnerUUID());
                if (p == null) continue;
                p.sendMessage("§c⚔ Your region §e" + region.getName()
                        + "§c is under attack! Your nation is at war.");
            }
        }
    }
}
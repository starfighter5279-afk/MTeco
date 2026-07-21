package com.dirt.inventory.impl.nation;

import com.cryptomorin.xseries.XMaterial;
import com.dirt.DirtEconomy;
import com.dirt.data.NationData;
import com.dirt.data.RegionData;
import com.dirt.data.WarData;
import com.dirt.inventory.InventoryButton;
import com.dirt.inventory.InventoryGUI;
import com.dirt.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ManageWarsGUI extends InventoryGUI {
    private final DirtEconomy plugin;
    private final UUID nationId;

    public ManageWarsGUI(DirtEconomy plugin, UUID nationId) {
        this.plugin = plugin;
        this.nationId = nationId;
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 54, "§eManage Wars");
    }

    @Override
    public void decorate(Player player) {
        fillPagedGui(54, XMaterial.RED_STAINED_GLASS_PANE);

        List<WarData> wars = plugin.getWarManager().getWarsByNation(nationId);
        for (int i = 0; i < Math.min(wars.size(), 45); i++) {
            WarData war = wars.get(i);
            boolean isAttacker = nationId.equals(war.getAttackingNationId());
            int slot = i;

            UUID otherNationId = isAttacker ? war.getDefendingNationId() : war.getAttackingNationId();
            NationData otherNation = plugin.getNationManager().loadNation(otherNationId);
            String otherName = otherNation != null ? otherNation.getColor1() + otherNation.getName() : "Unknown";

            List<String> lore = new ArrayList<>();
            lore.add(isAttacker ? "§7Role: §cAttacker" : "§7Role: §eDefender");
            lore.add("§7Opponent: " + otherName);
            if (isAttacker) {
                lore.add("§eAttacking Regions:");
                for (UUID rid : war.getAttackerRegionIds()) {
                    RegionData r = plugin.getNationManager().loadRegion(rid);
                    if (r != null) lore.add("  §7- " + r.getName());
                }
                lore.add("§eTargeting Regions:");
            } else {
                lore.add("§eRegions Under Attack:");
            }
            for (UUID rid : war.getTargetRegionIds()) {
                RegionData r = plugin.getNationManager().loadRegion(rid);
                if (r != null) lore.add("  §7- " + r.getName());
            }
            lore.add("");
            if (isAttacker) {
                lore.add("§aLeft-click: §7Give Up (abandon war, no transfers)");
            } else {
                lore.add("§cLeft-click: §7Surrender (grant regions to attacker)");
            }

            addButton(slot, new InventoryButton()
                    .creator(p -> {
                        XMaterial mat = isAttacker ? XMaterial.DIAMOND_SWORD : XMaterial.SHIELD;
                        ItemStack item = mat.parseItem();
                        if (item == null) item = new ItemStack(org.bukkit.Material.STONE);
                        ItemMeta meta = item.getItemMeta();
                        if (meta != null) {
                            meta.setDisplayName(isAttacker ? "§cWar vs " + otherName : "§eWar vs " + otherName);
                            meta.setLore(lore);
                            item.setItemMeta(meta);
                        }
                        return item;
                    })
                    .consumer(e -> {
                        Player clicker = (Player) e.getWhoClicked();
                        if (isAttacker) {
                            // Attacker gives up — just delete the war
                            plugin.getWarManager().deleteWar(war.getWarId());
                            clicker.sendMessage("§eYou have given up the war against " + otherName + "§e. No regions were transferred.");
                            clicker.closeInventory();

                            // Notify defending president
                            NationData def = plugin.getNationManager().loadNation(war.getDefendingNationId());
                            if (def != null && def.getPresidentUUID() != null) {
                                Player defPres = Bukkit.getPlayer(def.getPresidentUUID());
                                if (defPres != null) {
                                    defPres.sendMessage("§aThe attacking nation has given up their war against you. The war is over.");
                                }
                            }
                        } else {
                            // Defender surrenders — transfer regions
                            plugin.getWarManager().surrender(war);
                            clicker.sendMessage("§cYou have surrendered. The targeted regions have been transferred to the attacking nation.");
                            clicker.closeInventory();

                            // Notify attacking president
                            NationData atk = plugin.getNationManager().loadNation(war.getAttackingNationId());
                            if (atk != null && atk.getPresidentUUID() != null) {
                                Player atkPres = Bukkit.getPlayer(atk.getPresidentUUID());
                                if (atkPres != null) {
                                    atkPres.sendMessage("§aThe defending nation has surrendered! The claimed regions now belong to your nation.");
                                }
                            }
                        }
                    })
            );
        }

        // Back
        addButton(49, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.BARRIER, "§cBack"))
                .consumer(e -> plugin.getGUIManager().openGUI(
                        new NationWarGUI(plugin, nationId),
                        (Player) e.getWhoClicked()))
        );

        super.decorate(player);
    }
}
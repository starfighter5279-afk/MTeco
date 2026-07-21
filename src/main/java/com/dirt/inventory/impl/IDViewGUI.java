package com.dirt.inventory.impl;

import com.cryptomorin.xseries.XMaterial;
import com.dirt.DirtEconomy;
import com.dirt.data.CharacterData;
import com.dirt.data.FamilyData;
import com.dirt.inventory.InventoryButton;
import com.dirt.inventory.InventoryGUI;
import com.dirt.util.BedrockUtil;
import com.dirt.util.CurrencyUtil;
import com.dirt.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class IDViewGUI extends InventoryGUI {
    private final DirtEconomy plugin;
    private final CharacterData targetData;
    private final UUID targetPlayerUUID;
    /** True when the viewer is looking at their own ID (shows a Back button). */
    private final boolean isSelf;

    public IDViewGUI(DirtEconomy plugin, CharacterData targetData, UUID targetPlayerUUID, boolean isSelf) {
        this.plugin = plugin;
        this.targetData = targetData;
        this.targetPlayerUUID = targetPlayerUUID;
        this.isSelf = isSelf;
    }

    @Override
    protected Inventory createInventory() {
        String title = targetData != null
                ? "§e" + targetData.getFirstName() + " " + targetData.getLastName() + "'s ID"
                : "§eID Card";
        return Bukkit.createInventory(null, 36, title);
    }

    @Override
    public void decorate(Player viewer) {
        fillGlass(36, XMaterial.ORANGE_STAINED_GLASS_PANE);

        if (targetData == null) {
            addButton(13, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.BARRIER, "§cNo character found.",
                            "§7This player has no active character."))
                    .consumer(e -> {})
            );
        } else {
            // Slot 4 — Player skull + full name
            addButton(4, new InventoryButton()
                    .creator(p -> buildSkullItem())
                    .consumer(e -> {})
            );

            // Slot 10 — Birthdate
            String dateStr = new SimpleDateFormat("MM/dd/yyyy").format(new Date(targetData.getBirthDate()));
            addButton(10, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.CLOCK, "§eBirthdate",
                            "§f" + dateStr))
                    .consumer(e -> {})
            );

            // Slot 12 — Gender
            XMaterial genderMat = "FEMALE".equalsIgnoreCase(targetData.getGender())
                    ? XMaterial.PINK_DYE : XMaterial.LIGHT_BLUE_DYE;
            addButton(12, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(genderMat, "§eGender",
                            "§f" + capitalize(targetData.getGender())))
                    .consumer(e -> {})
            );

            // Slot 14 — Family status (only if family is enabled)
            if (plugin.isFamilyEnabled()) {
                addButton(14, new InventoryButton()
                        .creator(p -> buildFamilyStatusItem())
                        .consumer(e -> {})
                );
            }

            // Slot 16 — Nation (only if MTnations is present)
            if (plugin.getNationsHook().isAvailable()) {
                Player targetOnline = Bukkit.getPlayer(targetPlayerUUID);
                String nation = targetOnline != null
                        ? plugin.getNationsHook().getNation(targetOnline)
                        : null;
                String nationDisplay = nation != null ? nation : "§7None";
                addButton(16, new InventoryButton()
                        .creator(p -> ItemUtil.buildItem(XMaterial.MAP, "§eNation",
                                nationDisplay))
                        .consumer(e -> {})
                );
            }

            // Slot 22 — Vault balance
            double balance = plugin.getEconomy().getBalance(Bukkit.getOfflinePlayer(targetPlayerUUID));
            String balanceStr = CurrencyUtil.symbol() + String.format("%.2f", balance);
            addButton(22, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.GOLD_NUGGET, "§eBalance",
                            "§f" + balanceStr))
                    .consumer(e -> {})
            );
        }

        // Slot 35 — Close / Back
        if (isSelf) {
            addButton(35, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "§cBack"))
                    .consumer(e -> plugin.getGUIManager().openGUI(new CharacterManagementGUI(plugin), (Player) e.getWhoClicked()))
            );
        } else {
            addButton(35, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.BARRIER, "§cClose"))
                    .consumer(e -> e.getWhoClicked().closeInventory())
            );
        }

        if (plugin.isDirtBusinessEnabled() && targetData != null) {
            java.util.List<com.dirt.data.BusinessData> ownedBiz = plugin.getBusinessManager().getBusinessesByOwner(targetPlayerUUID);
            java.util.List<String> bizLore = new java.util.ArrayList<>();
            if (ownedBiz.isEmpty()) {
                bizLore.add("§7None");
            } else {
                for (com.dirt.data.BusinessData b : ownedBiz) bizLore.add("§f" + b.getName());
            }
            bizLore.add(0, "§7Businesses owned: §f" + ownedBiz.size());
            addButton(28, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.EMERALD, "§aBusinesses Owned",
                            bizLore.toArray(new String[0])))
                    .consumer(e -> {})
            );
            java.util.List<com.dirt.data.BusinessData> jobs = plugin.getBusinessManager().getBusinessesByEmployee(targetPlayerUUID);
            java.util.List<String> jobLore = new java.util.ArrayList<>();
            if (jobs.isEmpty()) {
                jobLore.add("§7None");
            } else {
                for (com.dirt.data.BusinessData b : jobs) {
                    double rate = b.getEmployeeRates().getOrDefault(targetPlayerUUID, b.getPayrollRate());
                    jobLore.add("§f" + b.getName() + " §7(" + CurrencyUtil.symbol() + "§e" + String.format("%.2f", rate) + "§7/period)");
                }
            }
            jobLore.add(0, "§7Employed at: §f" + jobs.size());
            addButton(30, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.GOLD_INGOT, "§6Current Jobs",
                            jobLore.toArray(new String[0])))
                    .consumer(e -> {})
            );
        }

        super.decorate(viewer);
    }

    private ItemStack buildSkullItem() {
        ItemStack skull;
        Player online = Bukkit.getPlayer(targetPlayerUUID);
        if (online != null) {
            skull = BedrockUtil.createPlayerHead(online);
        } else {
            skull = BedrockUtil.createPlayerHead(targetPlayerUUID);
        }

        ItemMeta meta = skull.getItemMeta();
        if (meta != null) {
            String middle = targetData.getMiddleName() != null && !targetData.getMiddleName().isEmpty()
                    ? " " + targetData.getMiddleName() : "";
            meta.setDisplayName("§e" + targetData.getFirstName() + middle + " " + targetData.getLastName());
            meta.setLore(Arrays.asList(
                    "§7Character ID Card",
                    "§8" + targetPlayerUUID.toString().substring(0, 8) + "..."
            ));
            skull.setItemMeta(meta);
        }
        return skull;
    }

    private ItemStack buildFamilyStatusItem() {
        if (targetData.getFamilyId() == null && targetData.getBirthFamilyId() == null) {
            return ItemUtil.buildItem(XMaterial.GRAY_DYE, "§eFamily Status", "§7Single");
        }

        List<String> lore = new ArrayList<>();
        String role = targetData.getFamilyRole();

        if ("PRIMARY".equals(role) || "SECONDARY".equals(role)) {
            lore.add("§aMarried");
            FamilyData family = plugin.getFamilyManager().loadFamily(targetData.getFamilyId());
            if (family != null) {
                UUID spouseUUID = targetData.getPlayerUuid().equals(family.getSpouse1())
                        ? family.getSpouse2() : family.getSpouse1();
                if (spouseUUID != null) {
                    CharacterData spouse = plugin.getCharacterManager().getCharacter(spouseUUID);
                    if (spouse != null) {
                        lore.add("§7Spouse: §f" + spouse.getFirstName() + " " + spouse.getLastName());
                    }
                }
            }
        } else if ("CHILD".equals(role)) {
            lore.add("§bChild");
            if (targetData.getFamilyId() != null) {
                FamilyData family = plugin.getFamilyManager().loadFamily(targetData.getFamilyId());
                if (family != null) {
                    UUID p1 = family.getSpouse1();
                    UUID p2 = family.getSpouse2();
                    if (p1 != null) {
                        CharacterData parent = plugin.getCharacterManager().getCharacter(p1);
                        if (parent != null) lore.add("§7Parent: §f" + parent.getFirstName() + " " + parent.getLastName());
                    }
                    if (p2 != null) {
                        CharacterData parent = plugin.getCharacterManager().getCharacter(p2);
                        if (parent != null) lore.add("§7Parent: §f" + parent.getFirstName() + " " + parent.getLastName());
                    }
                }
            }
        } else {
            lore.add("§7Single");
        }

        return ItemUtil.buildItem(XMaterial.HEART_OF_THE_SEA, "§eFamily Status",
                lore.toArray(new String[0]));
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.charAt(0) + s.substring(1).toLowerCase();
    }
}
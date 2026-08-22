package com.dirt.inventory.impl;

import com.cryptomorin.xseries.XMaterial;
import com.dirt.DirtEconomy;
import com.dirt.data.CharacterData;
import com.dirt.data.FamilyData;
import com.dirt.inventory.InventoryButton;
import com.dirt.inventory.InventoryGUI;
import com.dirt.data.NationData;
import com.dirt.inventory.impl.business.JobStatusGUI;
import com.dirt.inventory.impl.business.MyBusinessesGUI;
import com.dirt.inventory.impl.nation.CharacterPropertiesGUI;
import com.dirt.inventory.impl.nation.NationMembersGUI;
import com.dirt.util.BedrockUtil;
import com.dirt.util.CurrencyUtil;
import com.dirt.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
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

public class ServerCharacterDetailGUI extends InventoryGUI {
    private final DirtEconomy plugin;
    private final UUID targetUuid;

    public ServerCharacterDetailGUI(DirtEconomy plugin, UUID targetUuid) {
        this.plugin = plugin;
        this.targetUuid = targetUuid;
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 54, "§8Manage Character");
    }

    @Override
    public void decorate(Player player) {
        if (!player.isOp()) {
            player.closeInventory();
            player.sendMessage("§cOnly server operators can access this menu.");
            return;
        }

        CharacterData data = plugin.getCharacterManager().getCharacter(targetUuid);
        boolean isArchived = false;
        if (data == null) {
            data = plugin.getCharacterManager().getArchivedCharacter(targetUuid);
            isArchived = true;
            if (data != null) data.setAlive(false);
        }

        fillGlass(54, XMaterial.RED_STAINED_GLASS_PANE);

        if (data == null) {
            addButton(22, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.BARRIER, "§cCharacter not found",
                            "§7This character no longer exists."))
                    .consumer(e -> {})
            );
            addButton(49, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "§cBack to List"))
                    .consumer(e -> {
                        Player p = (Player) e.getWhoClicked();
                        if (!p.isOp()) return;
                        plugin.getGUIManager().openGUI(new ServerCharacterListGUI(plugin, 0, null), p);
                    })
            );
            super.decorate(player);
            return;
        }

        final CharacterData finalData = data;
        final boolean archived = isArchived;
        SimpleDateFormat fmt = new SimpleDateFormat("MM/dd/yyyy");
        String born = data.getBirthDate() > 0 ? fmt.format(new Date(data.getBirthDate())) : "Unknown";
        String firstJoin = data.getFirstJoinDate() > 0 ? fmt.format(new Date(data.getFirstJoinDate())) : "Unknown";
        String ownerName = Bukkit.getOfflinePlayer(targetUuid).getName();

        // === Row 1: Character head with full info summary (slot 4) ===
        addButton(4, new InventoryButton()
                .creator(p -> {
                    ItemStack skull;
                    try {
                        skull = BedrockUtil.createPlayerHead(targetUuid);
                    } catch (Exception ex) {
                        skull = XMaterial.matchXMaterial("PLAYER_HEAD").map(XMaterial::parseItem).orElse(new ItemStack(org.bukkit.Material.BARRIER));
                    }
                    ItemMeta meta = skull.getItemMeta();
                    if (meta != null) {
                        String fullName = buildFullName(finalData);
                        ItemUtil.setDisplayName(meta, (finalData.isAlive() ? "§a" : "§c") + fullName);
                        List<String> lore = new ArrayList<>();
                        lore.add("§7Owner: §f" + (ownerName != null ? ownerName : targetUuid.toString()));
                        lore.add("§7UUID: §8" + targetUuid);
                        lore.add("§7Gender: §f" + (finalData.getGender() == null ? "Unknown" : finalData.getGender()));
                        lore.add("§7Born: §f" + born);
                        lore.add("§7First Join: §f" + firstJoin);
                        lore.add("§7Status: " + (finalData.isAlive() ? "§aAlive" : "§cDead"));
                        lore.add("§7Family: §f" + (finalData.getFamilyId() == null ? "(none)" : finalData.getFamilyId().toString().substring(0, 8) + "..."));
                        lore.add("§7Family Role: §f" + (finalData.getFamilyRole() == null ? "(none)" : finalData.getFamilyRole()));
                        lore.add("§7Birth Family: §f" + (finalData.getBirthFamilyId() == null ? "(none)" : finalData.getBirthFamilyId().toString().substring(0, 8) + "..."));
                        String inheritorName = "(none)";
                        if (finalData.getInheritorUuid() != null) {
                            OfflinePlayer inh = Bukkit.getOfflinePlayer(finalData.getInheritorUuid());
                            inheritorName = inh.getName() != null ? inh.getName() : finalData.getInheritorUuid().toString();
                        }
                        lore.add("§7Inheritor: §f" + inheritorName);
                        lore.add("§7Mailbox: §f" + finalData.getMailbox().size() + " message(s)");
                        lore.add("§7Previous Families: §f" + finalData.getPreviousFamilyIds().size());
                        lore.add("§7Pending Child Selection: §f" + finalData.isPendingChildSelection());
                        if (archived) lore.add("§8(Archived Character)");
                        ItemUtil.setLore(meta, lore);
                        skull.setItemMeta(meta);
                    }
                    return skull;
                })
                .consumer(e -> {})
        );

        // === Row 2: Editable fields ===

        // Slot 19 — Edit First Name
        addButton(19, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.NAME_TAG, "§eEdit First Name",
                        "§7Current: §f" + (finalData.getFirstName() == null ? "(none)" : finalData.getFirstName()),
                        archived ? "§8(Archived — read only)" : "§eClick to change"))
                .consumer(e -> {
                    if (archived) return;
                    Player p = (Player) e.getWhoClicked();
                    if (!p.isOp()) return;
                    plugin.getChatInputManager().requestInput(p, "§eType the new first name:", input -> {
                        CharacterData d = plugin.getCharacterManager().getCharacter(targetUuid);
                        if (d != null) {
                            d.setFirstName(capitalize(input.trim().split(" ")[0]));
                            plugin.getCharacterManager().saveCharacter(d);
                            p.sendMessage("§aFirst name updated.");
                        }
                        plugin.getGUIManager().openGUI(new ServerCharacterDetailGUI(plugin, targetUuid), p);
                    });
                })
        );

        // Slot 20 — Edit Middle Name
        addButton(20, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.NAME_TAG, "§eEdit Middle Name",
                        "§7Current: §f" + (finalData.getMiddleName() == null ? "(none)" : finalData.getMiddleName()),
                        archived ? "§8(Archived — read only)" : "§eClick to change"))
                .consumer(e -> {
                    if (archived) return;
                    Player p = (Player) e.getWhoClicked();
                    if (!p.isOp()) return;
                    plugin.getChatInputManager().requestInput(p, "§eType the new middle name (or 'none' to clear):", input -> {
                        CharacterData d = plugin.getCharacterManager().getCharacter(targetUuid);
                        if (d != null) {
                            if ("none".equalsIgnoreCase(input.trim())) {
                                d.setMiddleName(null);
                            } else {
                                d.setMiddleName(capitalize(input.trim().split(" ")[0]));
                            }
                            plugin.getCharacterManager().saveCharacter(d);
                            p.sendMessage("§aMiddle name updated.");
                        }
                        plugin.getGUIManager().openGUI(new ServerCharacterDetailGUI(plugin, targetUuid), p);
                    });
                })
        );

        // Slot 21 — Edit Last Name
        addButton(21, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.NAME_TAG, "§eEdit Last Name",
                        "§7Current: §f" + (finalData.getLastName() == null ? "(none)" : finalData.getLastName()),
                        archived ? "§8(Archived — read only)" : "§eClick to change"))
                .consumer(e -> {
                    if (archived) return;
                    Player p = (Player) e.getWhoClicked();
                    if (!p.isOp()) return;
                    plugin.getChatInputManager().requestInput(p, "§eType the new last name:", input -> {
                        CharacterData d = plugin.getCharacterManager().getCharacter(targetUuid);
                        if (d != null) {
                            d.setLastName(capitalize(input.trim().split(" ")[0]));
                            plugin.getCharacterManager().saveCharacter(d);
                            p.sendMessage("§aLast name updated.");
                        }
                        plugin.getGUIManager().openGUI(new ServerCharacterDetailGUI(plugin, targetUuid), p);
                    });
                })
        );

        // Slot 22 — Toggle Gender
        addButton(22, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(
                        "MALE".equalsIgnoreCase(finalData.getGender()) ? XMaterial.BLUE_DYE : XMaterial.PINK_DYE,
                        "§eToggle Gender",
                        "§7Current: §f" + (finalData.getGender() == null ? "Unknown" : finalData.getGender()),
                        archived ? "§8(Archived — read only)" : "§eClick to toggle"))
                .consumer(e -> {
                    if (archived) return;
                    Player p = (Player) e.getWhoClicked();
                    if (!p.isOp()) return;
                    CharacterData d = plugin.getCharacterManager().getCharacter(targetUuid);
                    if (d != null) {
                        d.setGender("MALE".equalsIgnoreCase(d.getGender()) ? "FEMALE" : "MALE");
                        plugin.getCharacterManager().saveCharacter(d);
                    }
                    plugin.getGUIManager().openGUI(new ServerCharacterDetailGUI(plugin, targetUuid), p);
                })
        );

        // Slot 23 — Edit Birth Date
        addButton(23, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.CLOCK, "§eEdit Birth Date",
                        "§7Current: §f" + born,
                        archived ? "§8(Archived — read only)" : "§eClick to set (MM/DD/YYYY)"))
                .consumer(e -> {
                    if (archived) return;
                    Player p = (Player) e.getWhoClicked();
                    if (!p.isOp()) return;
                    plugin.getChatInputManager().requestInput(p, "§eType the new birth date (MM/DD/YYYY):", input -> {
                        try {
                            Date parsed = new SimpleDateFormat("MM/dd/yyyy").parse(input.trim());
                            CharacterData d = plugin.getCharacterManager().getCharacter(targetUuid);
                            if (d != null) {
                                d.setBirthDate(parsed.getTime());
                                plugin.getCharacterManager().saveCharacter(d);
                                p.sendMessage("§aBirth date updated.");
                            }
                        } catch (Exception ex) {
                            p.sendMessage("§cInvalid date format. Use MM/DD/YYYY.");
                        }
                        plugin.getGUIManager().openGUI(new ServerCharacterDetailGUI(plugin, targetUuid), p);
                    });
                })
        );

        // Slot 24 — Toggle Alive/Dead
        addButton(24, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(
                        finalData.isAlive() ? XMaterial.TOTEM_OF_UNDYING : XMaterial.WITHER_SKELETON_SKULL,
                        "§eToggle Alive / Dead",
                        "§7Current: " + (finalData.isAlive() ? "§aAlive" : "§cDead"),
                        archived ? "§8(Archived — read only)" : "§eClick to flip",
                        archived ? "" : "§8(Does not trigger death routine)"))
                .consumer(e -> {
                    if (archived) return;
                    Player p = (Player) e.getWhoClicked();
                    if (!p.isOp()) return;
                    CharacterData d = plugin.getCharacterManager().getCharacter(targetUuid);
                    if (d != null) {
                        d.setAlive(!d.isAlive());
                        plugin.getCharacterManager().saveCharacter(d);
                    }
                    plugin.getGUIManager().openGUI(new ServerCharacterDetailGUI(plugin, targetUuid), p);
                })
        );

        // Slot 25 — Edit Family Role
        addButton(25, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.LEATHER_CHESTPLATE, "§eEdit Family Role",
                        "§7Current: §f" + (finalData.getFamilyRole() == null ? "(none)" : finalData.getFamilyRole()),
                        archived ? "§8(Archived — read only)" : "§eClick to cycle: PRIMARY / SECONDARY / CHILD / none"))
                .consumer(e -> {
                    if (archived) return;
                    Player p = (Player) e.getWhoClicked();
                    if (!p.isOp()) return;
                    CharacterData d = plugin.getCharacterManager().getCharacter(targetUuid);
                    if (d != null) {
                        String current = d.getFamilyRole();
                        if (current == null) d.setFamilyRole("PRIMARY");
                        else if ("PRIMARY".equals(current)) d.setFamilyRole("SECONDARY");
                        else if ("SECONDARY".equals(current)) d.setFamilyRole("CHILD");
                        else d.setFamilyRole(null);
                        plugin.getCharacterManager().saveCharacter(d);
                    }
                    plugin.getGUIManager().openGUI(new ServerCharacterDetailGUI(plugin, targetUuid), p);
                })
        );

        // === Row 3: Family, Inheritor, Balance ===

        // Slot 28 — Set Inheritor
        String inheritorDisplay = "(none)";
        if (finalData.getInheritorUuid() != null) {
            OfflinePlayer inh = Bukkit.getOfflinePlayer(finalData.getInheritorUuid());
            inheritorDisplay = inh.getName() != null ? inh.getName() : finalData.getInheritorUuid().toString();
        }
        final String finalInheritorDisplay = inheritorDisplay;
        addButton(28, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.GOLD_INGOT, "§eSet Inheritor",
                        "§7Current: §f" + finalInheritorDisplay,
                        archived ? "§8(Archived — read only)" : "§eLeft-click §7to set by player name",
                        archived ? "" : "§eRight-click §7to clear"))
                .consumer(e -> {
                    if (archived) return;
                    Player p = (Player) e.getWhoClicked();
                    if (!p.isOp()) return;
                    if (e.isRightClick()) {
                        CharacterData d = plugin.getCharacterManager().getCharacter(targetUuid);
                        if (d != null) {
                            d.setInheritorUuid(null);
                            plugin.getCharacterManager().saveCharacter(d);
                            p.sendMessage("§aInheritor cleared.");
                        }
                        plugin.getGUIManager().openGUI(new ServerCharacterDetailGUI(plugin, targetUuid), p);
                        return;
                    }
                    plugin.getChatInputManager().requestInput(p, "§eType the player name for the new inheritor:", input -> {
                        OfflinePlayer target = Bukkit.getOfflinePlayer(input.trim());
                        if (target.getName() == null && !target.hasPlayedBefore()) {
                            p.sendMessage("§cPlayer not found.");
                        } else {
                            CharacterData d = plugin.getCharacterManager().getCharacter(targetUuid);
                            if (d != null) {
                                d.setInheritorUuid(target.getUniqueId());
                                plugin.getCharacterManager().saveCharacter(d);
                                p.sendMessage("§aInheritor set to §f" + target.getName() + "§a.");
                            }
                        }
                        plugin.getGUIManager().openGUI(new ServerCharacterDetailGUI(plugin, targetUuid), p);
                    });
                })
        );

        // Slot 29 — Remove From Family
        addButton(29, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.SHEARS, "§cRemove From Family",
                        "§7Current family: §f" + (finalData.getFamilyId() == null ? "(none)" : finalData.getFamilyId().toString().substring(0, 8) + "..."),
                        archived ? "§8(Archived — read only)" : "§eClick to remove from family"))
                .consumer(e -> {
                    if (archived) return;
                    Player p = (Player) e.getWhoClicked();
                    if (!p.isOp()) return;
                    CharacterData d = plugin.getCharacterManager().getCharacter(targetUuid);
                    if (d == null || d.getFamilyId() == null) {
                        p.sendMessage("§cThis character is not in a family.");
                        plugin.getGUIManager().openGUI(new ServerCharacterDetailGUI(plugin, targetUuid), p);
                        return;
                    }
                    FamilyData family = plugin.getFamilyManager().loadFamily(d.getFamilyId());
                    if (family != null) {
                        if (targetUuid.equals(family.getSpouse1())) family.setSpouse1(null);
                        else if (targetUuid.equals(family.getSpouse2())) family.setSpouse2(null);
                        else family.getChildren().remove(targetUuid);

                        if (family.getSpouse1() == null && family.getSpouse2() == null && family.getChildren().isEmpty()) {
                            plugin.getFamilyManager().deleteFamily(family.getFamilyId());
                        } else {
                            plugin.getFamilyManager().saveFamily(family);
                        }
                    }
                    d.setFamilyId(null);
                    d.setFamilyRole(null);
                    plugin.getCharacterManager().saveCharacter(d);
                    p.sendMessage("§aCharacter removed from family.");
                    plugin.getGUIManager().openGUI(new ServerCharacterDetailGUI(plugin, targetUuid), p);
                })
        );

        // Slot 30 — Set Balance
        addButton(30, new InventoryButton()
                .creator(p -> {
                    double bal = 0;
                    OfflinePlayer op = Bukkit.getOfflinePlayer(targetUuid);
                    try { bal = plugin.getEconomy().getBalance(op); } catch (Exception ignored) {}
                    return ItemUtil.buildItem(XMaterial.EMERALD, "§eSet Balance",
                            "§7Current: §a" + CurrencyUtil.symbol() + String.format("%.2f", bal),
                            archived ? "§8(Archived — read only)" : "§eClick to set a new balance");
                })
                .consumer(e -> {
                    if (archived) return;
                    Player p = (Player) e.getWhoClicked();
                    if (!p.isOp()) return;
                    plugin.getChatInputManager().requestInput(p, "§eType the new balance amount:", input -> {
                        try {
                            double amount = Double.parseDouble(input.trim());
                            OfflinePlayer target = Bukkit.getOfflinePlayer(targetUuid);
                            double current = plugin.getEconomy().getBalance(target);
                            if (amount > current) {
                                plugin.getEconomy().depositPlayer(target, amount - current);
                            } else if (amount < current) {
                                plugin.getEconomy().withdrawPlayer(target, current - amount);
                            }
                            p.sendMessage("§aBalance set to §a" + CurrencyUtil.symbol() + String.format("%.2f", amount));
                        } catch (NumberFormatException ex) {
                            p.sendMessage("§cInvalid number.");
                        }
                        plugin.getGUIManager().openGUI(new ServerCharacterDetailGUI(plugin, targetUuid), p);
                    });
                })
        );

        // Slot 31 — Toggle Pending Child Selection
        addButton(31, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(
                        finalData.isPendingChildSelection() ? XMaterial.LIME_DYE : XMaterial.GRAY_DYE,
                        "§eToggle Pending Child Selection",
                        "§7Current: §f" + finalData.isPendingChildSelection(),
                        archived ? "§8(Archived — read only)" : "§eClick to toggle"))
                .consumer(e -> {
                    if (archived) return;
                    Player p = (Player) e.getWhoClicked();
                    if (!p.isOp()) return;
                    CharacterData d = plugin.getCharacterManager().getCharacter(targetUuid);
                    if (d != null) {
                        d.setPendingChildSelection(!d.isPendingChildSelection());
                        plugin.getCharacterManager().saveCharacter(d);
                    }
                    plugin.getGUIManager().openGUI(new ServerCharacterDetailGUI(plugin, targetUuid), p);
                })
        );

        // Slot 32 — View Previous Families
        addButton(32, new InventoryButton()
                .creator(p -> {
                    List<String> lore = new ArrayList<>();
                    lore.add("§7Count: §f" + finalData.getPreviousFamilyIds().size());
                    int shown = 0;
                    for (UUID pfid : finalData.getPreviousFamilyIds()) {
                        if (shown >= 5) {
                            lore.add("§8... and " + (finalData.getPreviousFamilyIds().size() - 5) + " more");
                            break;
                        }
                        lore.add("§8- " + pfid.toString().substring(0, 8) + "...");
                        shown++;
                    }
                    if (finalData.getPreviousFamilyIds().isEmpty()) lore.add("§8(none)");
                    return ItemUtil.buildItem(XMaterial.BOOKSHELF, "§ePrevious Families", lore.toArray(new String[0]));
                })
                .consumer(e -> {})
        );

        // === Row 5: Nation, Properties, Businesses, Jobs ===

        // Slot 37 — Nation Status
        NationData charNation = plugin.getNationManager().getNationByMember(targetUuid);
        addButton(37, new InventoryButton()
                .creator(p -> {
                    if (charNation == null) {
                        return ItemUtil.buildItem(XMaterial.RED_BANNER, "§eNation Status",
                                "§7Nation: §cNone",
                                archived ? "§8(Archived — read only)" : "§8No nation management available.");
                    }
                    String nationRole = "§7Member";
                    if (targetUuid.equals(charNation.getPresidentUUID())) nationRole = "§6President";
                    else if (targetUuid.equals(charNation.getVicePresidentUUID())) nationRole = "§9Vice President";
                    else if (targetUuid.equals(charNation.getTreasurerUUID())) nationRole = "§eTreasurer";
                    else if (targetUuid.equals(charNation.getSecurityHeadUUID())) nationRole = "§cSecurity Head";
                    return ItemUtil.buildItem(XMaterial.GREEN_BANNER, "§eNation Status",
                            "§7Nation: §f" + charNation.getName(),
                            "§7Role: " + nationRole,
                            "",
                            archived ? "§8(Archived — read only)" : "§eLeft-click §7to open nation management",
                            archived ? "" : "§eRight-click §7to remove from nation");
                })
                .consumer(e -> {
                    if (archived) return;
                    Player p = (Player) e.getWhoClicked();
                    if (!p.isOp()) return;
                    if (charNation == null) {
                        p.sendMessage("§cThis character is not in a nation.");
                        return;
                    }
                    if (e.isRightClick()) {
                        if (targetUuid.equals(charNation.getPresidentUUID())) {
                            p.sendMessage("§cCannot remove the president. Transfer presidency first.");
                            return;
                        }
                        plugin.getNationManager().removeMember(charNation, targetUuid);
                        p.sendMessage("§aRemoved from nation §f" + charNation.getName() + "§a.");
                        plugin.getGUIManager().openGUI(new ServerCharacterDetailGUI(plugin, targetUuid), p);
                        return;
                    }
                    plugin.getGUIManager().openGUI(new NationMembersGUI(plugin, charNation.getNationId(), 0), p);
                })
        );

        // Slot 38 — Properties
        addButton(38, new InventoryButton()
                .creator(p -> {
                    int propCount = plugin.getNationManager().getPropertiesByOwner(targetUuid).size();
                    return ItemUtil.buildItem(XMaterial.OAK_DOOR, "§eProperties",
                            "§7Owned: §f" + propCount,
                            "",
                            archived ? "§8(Archived — read only)" : "§eClick to manage properties");
                })
                .consumer(e -> {
                    if (archived) return;
                    Player p = (Player) e.getWhoClicked();
                    if (!p.isOp()) return;
                    plugin.getGUIManager().openGUI(new CharacterPropertiesGUI(plugin, 0, targetUuid), p);
                })
        );

        // Slot 39 — Businesses
        addButton(39, new InventoryButton()
                .creator(p -> {
                    int bizCount = plugin.getBusinessManager().getBusinessesByOwner(targetUuid).size();
                    return ItemUtil.buildItem(XMaterial.CHEST, "§eBusinesses",
                            "§7Owned: §f" + bizCount,
                            "",
                            archived ? "§8(Archived — read only)" : "§eClick to manage businesses");
                })
                .consumer(e -> {
                    if (archived) return;
                    Player p = (Player) e.getWhoClicked();
                    if (!p.isOp()) return;
                    plugin.getGUIManager().openGUI(new MyBusinessesGUI(plugin, 0, targetUuid), p);
                })
        );

        // Slot 41 — Jobs
        addButton(41, new InventoryButton()
                .creator(p -> {
                    int jobCount = plugin.getBusinessManager().getBusinessesByEmployee(targetUuid).size();
                    return ItemUtil.buildItem(XMaterial.IRON_PICKAXE, "§eJobs",
                            "§7Employed at: §f" + jobCount + " business(es)",
                            "",
                            archived ? "§8(Archived — read only)" : "§eClick to view job status");
                })
                .consumer(e -> {
                    if (archived) return;
                    Player p = (Player) e.getWhoClicked();
                    if (!p.isOp()) return;
                    plugin.getGUIManager().openGUI(new JobStatusGUI(plugin, targetUuid), p);
                })
        );

        // === Row 6: Mailbox, Birth Family, Dangerous actions ===

        // Slot 46 — Clear Mailbox
        addButton(46, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.PAPER, "§cClear Mailbox",
                        "§7Messages: §f" + finalData.getMailbox().size(),
                        archived ? "§8(Archived — read only)" : "§eClick to delete all messages"))
                .consumer(e -> {
                    if (archived) return;
                    Player p = (Player) e.getWhoClicked();
                    if (!p.isOp()) return;
                    CharacterData d = plugin.getCharacterManager().getCharacter(targetUuid);
                    if (d != null) {
                        d.getMailbox().clear();
                        plugin.getCharacterManager().saveCharacter(d);
                        p.sendMessage("§aMailbox cleared.");
                    }
                    plugin.getGUIManager().openGUI(new ServerCharacterDetailGUI(plugin, targetUuid), p);
                })
        );

        // Slot 47 — Clear Birth Family
        addButton(47, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.FLOWER_BANNER_PATTERN, "§cClear Birth Family",
                        "§7Current: §f" + (finalData.getBirthFamilyId() == null ? "(none)" : finalData.getBirthFamilyId().toString().substring(0, 8) + "..."),
                        archived ? "§8(Archived — read only)" : "§eClick to clear birth family reference"))
                .consumer(e -> {
                    if (archived) return;
                    Player p = (Player) e.getWhoClicked();
                    if (!p.isOp()) return;
                    CharacterData d = plugin.getCharacterManager().getCharacter(targetUuid);
                    if (d != null) {
                        d.setBirthFamilyId(null);
                        plugin.getCharacterManager().saveCharacter(d);
                        p.sendMessage("§aBirth family cleared.");
                    }
                    plugin.getGUIManager().openGUI(new ServerCharacterDetailGUI(plugin, targetUuid), p);
                })
        );

        // Slot 48 — Clear Previous Families
        addButton(48, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.WRITABLE_BOOK, "§cClear Previous Families",
                        "§7Count: §f" + finalData.getPreviousFamilyIds().size(),
                        archived ? "§8(Archived — read only)" : "§eClick to clear all previous family history"))
                .consumer(e -> {
                    if (archived) return;
                    Player p = (Player) e.getWhoClicked();
                    if (!p.isOp()) return;
                    CharacterData d = plugin.getCharacterManager().getCharacter(targetUuid);
                    if (d != null) {
                        d.getPreviousFamilyIds().clear();
                        plugin.getCharacterManager().saveCharacter(d);
                        p.sendMessage("§aPrevious families cleared.");
                    }
                    plugin.getGUIManager().openGUI(new ServerCharacterDetailGUI(plugin, targetUuid), p);
                })
        );

        // Slot 50 — Reset Daily Reward
        addButton(50, new InventoryButton()
                .creator(p -> {
                    String lastReward = finalData.getLastDailyReward() > 0
                            ? fmt.format(new Date(finalData.getLastDailyReward()))
                            : "Never";
                    return ItemUtil.buildItem(XMaterial.EXPERIENCE_BOTTLE, "§eReset Daily Reward",
                            "§7Last claimed: §f" + lastReward,
                            archived ? "§8(Archived — read only)" : "§eClick to reset (allows re-claim)");
                })
                .consumer(e -> {
                    if (archived) return;
                    Player p = (Player) e.getWhoClicked();
                    if (!p.isOp()) return;
                    CharacterData d = plugin.getCharacterManager().getCharacter(targetUuid);
                    if (d != null) {
                        d.setLastDailyReward(0);
                        plugin.getCharacterManager().saveCharacter(d);
                        p.sendMessage("§aDaily reward timer reset.");
                    }
                    plugin.getGUIManager().openGUI(new ServerCharacterDetailGUI(plugin, targetUuid), p);
                })
        );

        // Slot 52 — DELETE CHARACTER (dangerous)
        addButton(52, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.BARRIER, "§4§lDELETE CHARACTER",
                        "§cPermanently deletes this character",
                        "§cfrom the database. This cannot be undone.",
                        "",
                        "§7Type the character's first name to confirm.",
                        archived ? "§8(Deletes archive only)" : "§cFamily ties will be cleaned up."))
                .consumer(e -> {
                    Player p = (Player) e.getWhoClicked();
                    if (!p.isOp()) return;
                    String expected = finalData.getFirstName() == null ? "" : finalData.getFirstName();
                    plugin.getChatInputManager().requestInput(p,
                            "§c§lType §f§l" + expected + "§c§l to confirm deletion (anything else cancels):", input -> {
                                if (!input.trim().equalsIgnoreCase(expected)) {
                                    p.sendMessage("§eDeletion cancelled.");
                                    plugin.getGUIManager().openGUI(new ServerCharacterDetailGUI(plugin, targetUuid), p);
                                    return;
                                }
                                if (archived) {
                                    plugin.getIdManager().deleteArchive(targetUuid);
                                    p.sendMessage("§cArchived character deleted permanently.");
                                } else {
                                    CharacterData d = plugin.getCharacterManager().getCharacter(targetUuid);
                                    if (d != null && d.getFamilyId() != null) {
                                        FamilyData family = plugin.getFamilyManager().loadFamily(d.getFamilyId());
                                        if (family != null) {
                                            if (targetUuid.equals(family.getSpouse1())) family.setSpouse1(null);
                                            else if (targetUuid.equals(family.getSpouse2())) family.setSpouse2(null);
                                            else family.getChildren().remove(targetUuid);
                                            if (family.getSpouse1() == null && family.getSpouse2() == null && family.getChildren().isEmpty()) {
                                                plugin.getFamilyManager().deleteFamily(family.getFamilyId());
                                            } else {
                                                plugin.getFamilyManager().saveFamily(family);
                                            }
                                        }
                                    }
                                    plugin.getCharacterManager().deleteCharacter(targetUuid);
                                    plugin.getIdManager().deleteArchive(targetUuid);
                                    p.sendMessage("§cCharacter deleted permanently.");
                                }
                                plugin.getGUIManager().openGUI(new ServerCharacterListGUI(plugin, 0, null), p);
                            });
                })
        );

        // === Bottom row: Navigation ===

        // Slot 49 — Back to list
        addButton(49, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "§cBack to List"))
                .consumer(e -> {
                    Player p = (Player) e.getWhoClicked();
                    if (!p.isOp()) return;
                    plugin.getGUIManager().openGUI(new ServerCharacterListGUI(plugin, 0, null), p);
                })
        );

        super.decorate(player);
    }

    private String buildFullName(CharacterData data) {
        StringBuilder sb = new StringBuilder();
        if (data.getFirstName() != null) sb.append(data.getFirstName());
        if (data.getMiddleName() != null) sb.append(" ").append(data.getMiddleName());
        if (data.getLastName() != null) sb.append(" ").append(data.getLastName());
        return sb.toString().trim();
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
    }
}
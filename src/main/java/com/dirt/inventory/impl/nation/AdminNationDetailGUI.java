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

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class AdminNationDetailGUI extends InventoryGUI {
    private final DirtEconomy plugin;
    private final UUID nationId;

    public AdminNationDetailGUI(DirtEconomy plugin, UUID nationId) {
        this.plugin = plugin;
        this.nationId = nationId;
    }

    @Override
    protected Inventory createInventory() {
        NationData nation = plugin.getNationManager().loadNation(nationId);
        String name = nation != null ? nation.getColor1() + nation.getName() : "Nation";
        return Bukkit.createInventory(null, 54, "§4Admin: " + name);
    }

    @Override
    public void decorate(Player player) {
        fillPagedGui(54, XMaterial.RED_STAINED_GLASS_PANE);

        NationData nation = plugin.getNationManager().loadNation(nationId);
        if (nation == null) { super.decorate(player); return; }

        String presName = resolveCharName(nation.getPresidentUUID());
        String tresName = resolveCharName(nation.getTreasurerUUID());
        String vpName = resolveCharName(nation.getVicePresidentUUID());
        String secName = resolveCharName(nation.getSecurityHeadUUID());

        addButton(4, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.GOLD_BLOCK,
                        nation.getColor1() + nation.getName(),
                        "§7Colors: " + nation.getColor1() + "■ §f& " + nation.getColor2() + "■",
                        "§7Members: §f" + nation.getMemberUUIDs().size(),
                        "§7Regions: §f" + nation.getRegionIds().size(),
                        "§7Tax: §f" + String.format("%.1f", nation.getTaxRate()) + "%"))
                .consumer(e -> {})
        );

        // Edit Name
        addButton(9, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.NAME_TAG, "§eEdit Name",
                        "§7Current: §f" + nation.getName()))
                .consumer(e -> {
                    Player p = (Player) e.getWhoClicked();
                    plugin.getChatInputManager().requestInput(p, "§eEnter new nation name:", name -> {
                        NationData n = plugin.getNationManager().loadNation(nationId);
                        if (n == null) return;
                        n.setName(name);
                        plugin.getNationManager().saveNation(n);
                        p.sendMessage("§aNation name updated to §e" + name + "§a.");
                        plugin.getGUIManager().openGUI(new AdminNationDetailGUI(plugin, nationId), p);
                    });
                })
        );

        // Edit Colors
        addButton(10, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.WHITE_WOOL, "§eEdit Colors",
                        "§7Colors: " + nation.getColor1() + "■ §f& " + nation.getColor2() + "■"))
                .consumer(e -> {
                    Player p = (Player) e.getWhoClicked();
                    plugin.getGUIManager().openGUI(new NationColorSelectGUI(plugin, nation.getName(), null, (c1, c2) -> {
                        NationData n = plugin.getNationManager().loadNation(nationId);
                        if (n == null) return;
                        n.setColor1(c1);
                        n.setColor2(c2);
                        plugin.getNationManager().saveNation(n);
                        p.sendMessage("§aNation colors updated.");
                        plugin.getGUIManager().openGUI(new AdminNationDetailGUI(plugin, nationId), p);
                    }), p);
                })
        );

        // Change President
        List<CharacterData> allChars = plugin.getCharacterManager().getAllCharacters();
        addButton(11, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.PLAYER_HEAD, "§eChange President",
                        "§7Current: §f" + presName))
                .consumer(e -> {
                    Player p = (Player) e.getWhoClicked();
                    plugin.getGUIManager().openGUI(new NationCharacterSelectGUI(plugin, "§eSelect New President", allChars, c -> {
                        NationData n = plugin.getNationManager().loadNation(nationId);
                        if (n == null) return;
                        n.setPresidentUUID(c.getPlayerUuid());
                        if (!n.getMemberUUIDs().contains(c.getPlayerUuid())) {
                            NationData existingNation = plugin.getNationManager().getNationByMember(c.getPlayerUuid());
                            if (existingNation != null) {
                                p.sendMessage("§cThis character is already in §e" + existingNation.getName() + "§c. Remove them from that nation first.");
                                plugin.getGUIManager().openGUI(new AdminNationDetailGUI(plugin, nationId), p);
                                return;
                            }
                            n.getMemberUUIDs().add(c.getPlayerUuid());
                        }
                        plugin.getNationManager().saveNation(n);
                        p.sendMessage("§aNew president set: §e" + c.getFirstName() + " " + c.getLastName() + "§a.");
                        plugin.getGUIManager().openGUI(new AdminNationDetailGUI(plugin, nationId), p);
                    }, new AdminNationDetailGUI(plugin, nationId), 0), p);
                })
        );

        // Change Treasurer
        addButton(12, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.GOLD_INGOT, "§eChange Treasurer",
                        "§7Current: §f" + tresName))
                .consumer(e -> {
                    Player p = (Player) e.getWhoClicked();
                    plugin.getGUIManager().openGUI(new NationCharacterSelectGUI(plugin, "§eSelect Treasurer", allChars, c -> {
                        NationData n = plugin.getNationManager().loadNation(nationId);
                        if (n == null) return;
                        n.setTreasurerUUID(c.getPlayerUuid());
                        plugin.getNationManager().saveNation(n);
                        p.sendMessage("§aTreasurer set: §e" + c.getFirstName() + " " + c.getLastName() + "§a.");
                        plugin.getGUIManager().openGUI(new AdminNationDetailGUI(plugin, nationId), p);
                    }, new AdminNationDetailGUI(plugin, nationId), 0), p);
                })
        );

        // Change VP
        addButton(13, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.BOOK, "§eChange Vice President",
                        "§7Current: §f" + vpName))
                .consumer(e -> {
                    Player p = (Player) e.getWhoClicked();
                    plugin.getGUIManager().openGUI(new NationCharacterSelectGUI(plugin, "§eSelect Vice President", allChars, c -> {
                        NationData n = plugin.getNationManager().loadNation(nationId);
                        if (n == null) return;
                        n.setVicePresidentUUID(c.getPlayerUuid());
                        plugin.getNationManager().saveNation(n);
                        p.sendMessage("§aVice President set: §e" + c.getFirstName() + " " + c.getLastName() + "§a.");
                        plugin.getGUIManager().openGUI(new AdminNationDetailGUI(plugin, nationId), p);
                    }, new AdminNationDetailGUI(plugin, nationId), 0), p);
                })
        );

        // Change Security Head
        addButton(14, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.IRON_SWORD, "§eChange Security Head",
                        "§7Current: §f" + secName))
                .consumer(e -> {
                    Player p = (Player) e.getWhoClicked();
                    plugin.getGUIManager().openGUI(new NationCharacterSelectGUI(plugin, "§eSelect Security Head", allChars, c -> {
                        NationData n = plugin.getNationManager().loadNation(nationId);
                        if (n == null) return;
                        n.setSecurityHeadUUID(c.getPlayerUuid());
                        plugin.getNationManager().saveNation(n);
                        p.sendMessage("§aSecurity Head set: §e" + c.getFirstName() + " " + c.getLastName() + "§a.");
                        plugin.getGUIManager().openGUI(new AdminNationDetailGUI(plugin, nationId), p);
                    }, new AdminNationDetailGUI(plugin, nationId), 0), p);
                })
        );

        // Edit Tax Rate
        addButton(15, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.GOLD_NUGGET, "§eEdit Tax Rate",
                        "§7Current: §f" + String.format("%.1f", nation.getTaxRate()) + "%"))
                .consumer(e -> {
                    Player p = (Player) e.getWhoClicked();
                    plugin.getChatInputManager().requestInput(p, "§eEnter new tax rate (0-100):", input -> {
                        try {
                            double rate = Double.parseDouble(input);
                            if (rate < 0 || rate > 100) { p.sendMessage("§cMust be 0-100."); return; }
                            NationData n = plugin.getNationManager().loadNation(nationId);
                            if (n == null) return;
                            n.setTaxRate(rate);
                            plugin.getNationManager().saveNation(n);
                            p.sendMessage("§aTax rate updated to §f" + String.format("%.1f", rate) + "%§a.");
                            plugin.getGUIManager().openGUI(new AdminNationDetailGUI(plugin, nationId), p);
                        } catch (NumberFormatException ex) { p.sendMessage("§cInvalid number."); }
                    });
                })
        );

        // Manage Members (reuse president GUI)
        addButton(19, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.PLAYER_HEAD, "§aManage Members"))
                .consumer(e -> plugin.getGUIManager().openGUI(new MemberManagementGUI(plugin, nationId), (Player) e.getWhoClicked()))
        );

        // Manage Regions (reuse president GUI)
        addButton(21, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.MAP, "§6Manage Regions"))
                .consumer(e -> plugin.getGUIManager().openGUI(new RegionsGUI(plugin, nationId), (Player) e.getWhoClicked()))
        );

        // Manage Custom Roles
        addButton(25, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.NAME_TAG, "§bManage Custom Roles",
                        "§7" + (nation != null ? nation.getCustomRoles().size() : 0) + " roles"))
                .consumer(e -> plugin.getGUIManager().openGUI(new CustomRolesGUI(plugin, nationId), (Player) e.getWhoClicked()))
        );

        // Toggle Custom Government
        if (plugin.getRolesConfig().isCustomGovernmentEnabled()) {
            boolean isCustom = nation.isCustomGovernment();
            addButton(20, new InventoryButton()
                    .creator(p -> isCustom
                            ? ItemUtil.buildItem(XMaterial.LIME_DYE, "§aCustom Government: §lON",
                                    "§7This nation uses custom roles.",
                                    "§7Click to switch to standard government.",
                                    "§c(Removes all custom roles)")
                            : ItemUtil.buildItem(XMaterial.GRAY_DYE, "§7Custom Government: §lOFF",
                                    "§7This nation uses standard roles.",
                                    "§7Click to switch to custom government.",
                                    "§c(Clears President/VP/Treasurer/Security)"))
                    .consumer(e -> {
                        Player p = (Player) e.getWhoClicked();
                        p.closeInventory();
                        NationData n = plugin.getNationManager().loadNation(nationId);
                        if (n == null) return;
                        if (n.isCustomGovernment()) {
                            plugin.getChatInputManager().requestInput(p, "§cType §4CONFIRM§c to disable custom government and remove all custom roles:", input -> {
                                if (!input.equalsIgnoreCase("CONFIRM")) { p.sendMessage("§cCancelled."); return; }
                                n.getCustomRoles().clear();
                                n.setCustomGovernment(false);
                                plugin.getNationManager().saveNation(n);
                                p.sendMessage("§aCustom government disabled. Assign a president and elite roles.");
                                plugin.getGUIManager().openGUI(new AdminNationDetailGUI(plugin, nationId), p);
                            });
                        } else {
                            plugin.getChatInputManager().requestInput(p, "§cType §4CONFIRM§c to enable custom government (clears President/VP/Treasurer/Security Head):", input -> {
                                if (!input.equalsIgnoreCase("CONFIRM")) { p.sendMessage("§cCancelled."); return; }
                                n.setPresidentUUID(null);
                                n.setTreasurerUUID(null);
                                n.setVicePresidentUUID(null);
                                n.setSecurityHeadUUID(null);
                                n.setElitesConfigured(false);
                                n.setCustomGovernment(true);
                                plugin.getNationManager().saveNation(n);
                                p.sendMessage("§aCustom government enabled! Opening role manager...");
                                plugin.getServer().getScheduler().runTaskLater(plugin, () ->
                                    plugin.getGUIManager().openGUI(new CustomRolesGUI(plugin, nationId), p), 5L);
                            });
                        }
                    })
            );
        }

        // Delete Nation
        addButton(23, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.TNT, "§4Delete Nation",
                        "§cPERMANENTLY deletes this nation.",
                        "§cType the nation name in chat to confirm."))
                .consumer(e -> {
                    Player p = (Player) e.getWhoClicked();
                    NationData n = plugin.getNationManager().loadNation(nationId);
                    if (n == null) return;
                    plugin.getChatInputManager().requestInput(p, "§cType §4" + n.getName() + "§c to confirm deletion:", input -> {
                        if (!input.equalsIgnoreCase(n.getName())) { p.sendMessage("§cDeletion cancelled."); return; }
                        plugin.getNationManager().deleteNation(nationId);
                        p.sendMessage("§aNation §c" + n.getName() + "§a has been deleted.");
                        plugin.getGUIManager().openGUI(new AdminNationGUI(plugin, 0), p);
                    });
                })
        );

        addButton(49, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "§cBack"))
                .consumer(e -> plugin.getGUIManager().openGUI(new AdminNationGUI(plugin, 0), (Player) e.getWhoClicked()))
        );

        super.decorate(player);
    }

    private String resolveCharName(UUID uuid) {
        if (uuid == null) return "§7Unassigned";
        CharacterData c = plugin.getCharacterManager().getCharacter(uuid);
        return c != null ? c.getFirstName() + " " + c.getLastName() : "§7Unknown";
    }
}
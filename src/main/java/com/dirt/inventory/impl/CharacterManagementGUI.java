package com.dirt.inventory.impl;

import com.cryptomorin.xseries.XMaterial;
import com.dirt.DirtEconomy;
import com.dirt.data.CharacterData;
import com.dirt.inventory.InventoryButton;
import com.dirt.inventory.InventoryGUI;
import com.dirt.util.BedrockUtil;
import com.dirt.util.CurrencyUtil;
import com.dirt.util.ItemUtil;
import com.dirt.inventory.impl.nation.CharacterPropertiesGUI;
import com.dirt.inventory.impl.business.JobStatusGUI;
import com.dirt.inventory.impl.business.MyBusinessesGUI;
import com.dirt.inventory.impl.contract.ContractListGUI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.UUID;

public class CharacterManagementGUI extends InventoryGUI {
    private final DirtEconomy plugin;
    private final UUID targetUuid;
    private final boolean adminMode;

    public CharacterManagementGUI(DirtEconomy plugin) {
        this.plugin = plugin;
        this.targetUuid = null;
        this.adminMode = false;
    }

    public CharacterManagementGUI(DirtEconomy plugin, UUID targetUuid) {
        this.plugin = plugin;
        this.targetUuid = targetUuid;
        this.adminMode = true;
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 27, adminMode ? "§4Admin: Character Mgmt" : "§6Character Management");
    }

    @Override
    public void decorate(Player player) {
        UUID effectiveUuid = targetUuid != null ? targetUuid : player.getUniqueId();
        CharacterData data = plugin.getCharacterManager().getCharacter(effectiveUuid);

        fillGlass(27, XMaterial.ORANGE_STAINED_GLASS_PANE);

        addButton(4, new InventoryButton()
                .creator(p -> {
                    ItemStack skull = BedrockUtil.createPlayerHead(effectiveUuid);
                    if (data != null) {
                        ItemMeta meta = skull.getItemMeta();
                        if (meta != null) {
                            meta.setDisplayName("§e" + data.getFirstName() + " " + data.getMiddleName() + " " + data.getLastName());
                            String dateStr = new SimpleDateFormat("MM/dd/yyyy").format(new Date(data.getBirthDate()));
                            String owner = Bukkit.getOfflinePlayer(effectiveUuid).getName();
                            if (adminMode) {
                                meta.setLore(Arrays.asList(
                                        "§7Owner: §f" + (owner != null ? owner : effectiveUuid.toString()),
                                        "§7Gender: §f" + data.getGender(),
                                        "§7Born: §f" + dateStr,
                                        "§7Status: " + (data.isAlive() ? "§aAlive" : "§cDead"),
                                        "",
                                        "§4§lADMIN VIEW"
                                ));
                            } else {
                                meta.setLore(Arrays.asList(
                                        "§7Gender: §f" + data.getGender(),
                                        "§7Born: §f" + dateStr
                                ));
                            }
                            skull.setItemMeta(meta);
                        }
                    }
                    return skull;
                })
                .consumer(e -> {})
        );

        if (adminMode) {
            addButton(10, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.WRITABLE_BOOK, "§4Admin Edit",
                            "§7Edit name, gender, birthdate,",
                            "§7alive status, inheritor, family,",
                            "§7mailbox, or delete this character."))
                    .consumer(e -> plugin.getGUIManager().openGUI(new ServerCharacterDetailGUI(plugin, effectiveUuid), (Player) e.getWhoClicked()))
            );
        } else {
            addButton(10, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.NAME_TAG, "§6Name",
                            "§7Manage your character's name.",
                            "§7Cost to change: §e" + CurrencyUtil.symbol() + "100,000"))
                    .consumer(e -> {
                        Player p = (Player) e.getWhoClicked();
                        plugin.getCharacterManager().initPendingNameChange(p.getUniqueId());
                        plugin.getGUIManager().openGUI(new NameManagementGUI(plugin), p);
                    })
            );
        }

        if (adminMode) {
            addButton(12, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(
                            data != null && data.isAlive() ? XMaterial.TOTEM_OF_UNDYING : XMaterial.WITHER_SKELETON_SKULL,
                            "§cLife Status",
                            "§7Current: " + (data != null && data.isAlive() ? "§aAlive" : "§cDead"),
                            "§7Use Admin Edit to toggle."))
                    .consumer(e -> {})
            );
        } else {
            addButton(12, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.SKELETON_SKULL, "§cLife",
                            "§7Manage your character's life.",
                            "§7Here you can create a death."))
                    .consumer(e -> {
                        Player p = (Player) e.getWhoClicked();
                        plugin.getGUIManager().openGUI(new LifeGUI(plugin), p);
                    })
            );
        }

        if (plugin.isFamilyEnabled() && !adminMode) {
            addButton(14, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.HEART_OF_THE_SEA, "§dFamily",
                            "§7Manage your family."))
                    .consumer(e -> {
                        Player p = (Player) e.getWhoClicked();
                        plugin.getGUIManager().openGUI(new FamilyGUI(plugin), p);
                    })
            );
        } else if (plugin.isFamilyEnabled() && adminMode) {
            addButton(14, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.HEART_OF_THE_SEA, "§dFamily Info",
                            "§7Family: §f" + (data != null && data.getFamilyId() != null ? data.getFamilyId().toString() : "(none)"),
                            "§7Role: §f" + (data != null && data.getFamilyRole() != null ? data.getFamilyRole() : "(none)"),
                            "§7Use Admin Edit to modify."))
                    .consumer(e -> {})
            );
        }

        addButton(16, new InventoryButton()
                .creator(p -> {
                    int count = data != null ? data.getMailbox().size() : 0;
                    return ItemUtil.buildItem(XMaterial.PAPER, "§bMail",
                            "§7" + count + " message(s) in " + (adminMode ? "their" : "your") + " mailbox.",
                            "§7Click to view.");
                })
                .consumer(e -> {
                    Player p = (Player) e.getWhoClicked();
                    if (adminMode) {
                        plugin.getGUIManager().openGUI(new MailGUI(plugin, 0, effectiveUuid), p);
                    } else {
                        plugin.getGUIManager().openGUI(new MailGUI(plugin, 0), p);
                    }
                })
        );

        if (plugin.isDirtNationsEnabled()) {
            addButton(22, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.OAK_DOOR, "§aProperties",
                            "§7View " + (adminMode ? "their" : "and manage your") + " properties."))
                    .consumer(e -> {
                        Player p = (Player) e.getWhoClicked();
                        if (adminMode) {
                            plugin.getGUIManager().openGUI(new CharacterPropertiesGUI(plugin, 0, effectiveUuid), p);
                        } else {
                            plugin.getGUIManager().openGUI(new CharacterPropertiesGUI(plugin, 0), p);
                        }
                    })
            );
        }

        if (plugin.isDirtBusinessEnabled()) {
            addButton(20, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.GOLD_INGOT, "§6Job",
                            "§7View " + (adminMode ? "their" : "your") + " current job status."))
                    .consumer(e -> {
                        Player p = (Player) e.getWhoClicked();
                        if (adminMode) {
                            plugin.getGUIManager().openGUI(new JobStatusGUI(plugin, effectiveUuid), p);
                        } else {
                            plugin.getGUIManager().openGUI(new JobStatusGUI(plugin), p);
                        }
                    })
            );
            addButton(24, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.EMERALD, "§aBusinesses",
                            "§7View " + (adminMode ? "their" : "and manage your") + " businesses."))
                    .consumer(e -> {
                        Player p = (Player) e.getWhoClicked();
                        if (adminMode) {
                            plugin.getGUIManager().openGUI(new MyBusinessesGUI(plugin, 0, effectiveUuid), p);
                        } else {
                            plugin.getGUIManager().openGUI(new MyBusinessesGUI(plugin, 0), p);
                        }
                    })
            );
        }

        if (plugin.isContractsEnabled() && !adminMode) {
            addButton(18, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.WRITABLE_BOOK, "§eContracts",
                            "§7View your signed contracts."))
                    .consumer(e -> {
                        Player p = (Player) e.getWhoClicked();
                        plugin.getGUIManager().openGUI(new ContractListGUI(plugin, null, p.getUniqueId(), 0), p);
                    })
            );
        }

        if (adminMode) {
            addButton(0, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "§cBack to List"))
                    .consumer(e -> plugin.getGUIManager().openGUI(new ServerCharacterListGUI(plugin, 0, null), (Player) e.getWhoClicked()))
            );
        }

        super.decorate(player);
    }
}
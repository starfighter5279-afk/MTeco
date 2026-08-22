package com.dirt.inventory.impl;

import com.cryptomorin.xseries.XMaterial;
import com.dirt.DirtEconomy;
import com.dirt.data.CharacterData;
import com.dirt.inventory.InventoryButton;
import com.dirt.inventory.InventoryGUI;
import com.dirt.util.BedrockUtil;
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

public class ServerCharacterListGUI extends InventoryGUI {
    private final DirtEconomy plugin;
    private final int page;
    private final String filter;

    public ServerCharacterListGUI(DirtEconomy plugin, int page, String filter) {
        this.plugin = plugin;
        this.page = page;
        this.filter = filter;
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 54, "§8Server MTCs Management");
    }

    @Override
    public void decorate(Player player) {
        if (!player.isOp()) {
            player.closeInventory();
            player.sendMessage("§cOnly server operators can access this menu.");
            return;
        }

        List<CharacterData> all = plugin.getCharacterManager().getAllCharactersEver();
        List<CharacterData> filtered = new ArrayList<>();
        for (CharacterData c : all) {
            if (filter == null || filter.isEmpty()) {
                filtered.add(c);
            } else {
                String full = ((c.getFirstName() == null ? "" : c.getFirstName()) + " "
                        + (c.getMiddleName() == null ? "" : c.getMiddleName()) + " "
                        + (c.getLastName() == null ? "" : c.getLastName())).toLowerCase();
                String ownerName = Bukkit.getOfflinePlayer(c.getPlayerUuid()).getName();
                if (full.contains(filter.toLowerCase())
                        || (ownerName != null && ownerName.toLowerCase().contains(filter.toLowerCase()))) {
                    filtered.add(c);
                }
            }
        }
        filtered.sort((a, b) -> {
            if (a.isAlive() != b.isAlive()) return a.isAlive() ? -1 : 1;
            String an = (a.getFirstName() == null ? "" : a.getFirstName()) + (a.getLastName() == null ? "" : a.getLastName());
            String bn = (b.getFirstName() == null ? "" : b.getFirstName()) + (b.getLastName() == null ? "" : b.getLastName());
            return an.compareToIgnoreCase(bn);
        });

        for (int i = 45; i < 54; i++) {
            addButton(i, new InventoryButton().creator(p -> ItemUtil.buildGlassPane(XMaterial.RED_STAINED_GLASS_PANE)).consumer(e -> {}));
        }

        int perPage = 45;
        int start = page * perPage;
        int end = Math.min(start + perPage, filtered.size());
        int totalPages = Math.max(1, (int) Math.ceil(filtered.size() / (double) perPage));

        SimpleDateFormat fmt = new SimpleDateFormat("MM/dd/yyyy");

        for (int i = 0; i < perPage; i++) {
            int idx = start + i;
            if (idx >= end) {
                addButton(i, new InventoryButton().creator(p -> new ItemStack(org.bukkit.Material.AIR)).consumer(e -> {}));
                continue;
            }
            CharacterData c = filtered.get(idx);
            final int slot = i;
            addButton(slot, new InventoryButton()
                    .creator(p -> {
                        ItemStack skull;
                        try {
                            skull = BedrockUtil.createPlayerHead(c.getPlayerUuid());
                        } catch (Exception ex) {
                            skull = XMaterial.matchXMaterial("PLAYER_HEAD").map(XMaterial::parseItem).orElse(new ItemStack(org.bukkit.Material.BARRIER));
                        }
                        ItemMeta meta = skull.getItemMeta();
                        if (meta != null) {
                            String name = ((c.getFirstName() == null ? "" : c.getFirstName()) + " "
                                    + (c.getMiddleName() == null ? "" : c.getMiddleName()) + " "
                                    + (c.getLastName() == null ? "" : c.getLastName())).trim();
                            ItemUtil.setDisplayName(meta, (c.isAlive() ? "§a" : "§c") + name);
                            String born = c.getBirthDate() > 0 ? fmt.format(new Date(c.getBirthDate())) : "Unknown";
                            String owner = Bukkit.getOfflinePlayer(c.getPlayerUuid()).getName();
                            ItemUtil.setLore(meta, Arrays.asList(
                                    "§7Owner: §f" + (owner != null ? owner : c.getPlayerUuid().toString()),
                                    "§7Gender: §f" + (c.getGender() == null ? "Unknown" : c.getGender()),
                                    "§7Born: §f" + born,
                                    "§7Status: " + (c.isAlive() ? "§aAlive" : "§cDead"),
                                    "§7Family Role: §f" + (c.getFamilyRole() == null ? "(none)" : c.getFamilyRole()),
                                    "",
                                    "§eClick to manage this character"
                            ));
                            skull.setItemMeta(meta);
                        }
                        return skull;
                    })
                    .consumer(e -> {
                        Player p = (Player) e.getWhoClicked();
                        if (!p.isOp()) return;
                        plugin.getGUIManager().openGUI(new ServerCharacterDetailGUI(plugin, c.getPlayerUuid()), p);
                    })
            );
        }

        if (page > 0) {
            int prev = page - 1;
            addButton(45, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "§ePrevious Page",
                            "§7Page " + page + " / " + totalPages))
                    .consumer(e -> {
                        Player p = (Player) e.getWhoClicked();
                        if (!p.isOp()) return;
                        plugin.getGUIManager().openGUI(new ServerCharacterListGUI(plugin, prev, filter), p);
                    })
            );
        }

        addButton(48, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.BOOK, "§eCharacter Count",
                        "§7Total: §f" + filtered.size() + " character(s)",
                        "§7Page: §f" + (page + 1) + " / " + totalPages))
                .consumer(e -> {})
        );

        addButton(49, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.COMPASS, "§eSearch / Filter",
                        "§7Filter: §f" + (filter == null || filter.isEmpty() ? "(none)" : filter),
                        "",
                        "§eLeft-click §7to search by name or owner",
                        "§eRight-click §7to clear filter"))
                .consumer(e -> {
                    Player p = (Player) e.getWhoClicked();
                    if (!p.isOp()) return;
                    if (e.isRightClick()) {
                        plugin.getGUIManager().openGUI(new ServerCharacterListGUI(plugin, 0, null), p);
                        return;
                    }
                    plugin.getChatInputManager().requestInput(p, "§eType a name or owner to filter by (or 'clear' to reset):", input -> {
                        if ("clear".equalsIgnoreCase(input.trim())) {
                            plugin.getGUIManager().openGUI(new ServerCharacterListGUI(plugin, 0, null), p);
                        } else {
                            plugin.getGUIManager().openGUI(new ServerCharacterListGUI(plugin, 0, input.trim()), p);
                        }
                    });
                })
        );

        addButton(50, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.BARRIER, "§cClose"))
                .consumer(e -> e.getWhoClicked().closeInventory())
        );

        if (end < filtered.size()) {
            int next = page + 1;
            addButton(53, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "§eNext Page",
                            "§7Page " + (page + 2) + " / " + totalPages))
                    .consumer(e -> {
                        Player p = (Player) e.getWhoClicked();
                        if (!p.isOp()) return;
                        plugin.getGUIManager().openGUI(new ServerCharacterListGUI(plugin, next, filter), p);
                    })
            );
        }

        super.decorate(player);
    }
}
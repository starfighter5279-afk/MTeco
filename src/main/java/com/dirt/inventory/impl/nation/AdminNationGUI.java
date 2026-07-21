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

public class AdminNationGUI extends InventoryGUI {
    private final DirtEconomy plugin;
    private final int page;

    public AdminNationGUI(DirtEconomy plugin, int page) {
        this.plugin = plugin;
        this.page = page;
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 54, "§4Admin: Nations");
    }

    @Override
    public void decorate(Player player) {
        fillPagedGui(54, XMaterial.RED_STAINED_GLASS_PANE);

        List<NationData> nations = plugin.getNationManager().getAllNations();
        int perPage = 44;
        int start = page * perPage;
        int end = Math.min(start + perPage, nations.size());

        for (int i = start; i < end; i++) {
            NationData nation = nations.get(i);
            int slot = i - start;
            addButton(slot, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.GOLD_BLOCK,
                            nation.getColor1() + nation.getName(),
                            "§7Members: §f" + nation.getMemberUUIDs().size(),
                            "§7Regions: §f" + nation.getRegionIds().size(),
                            "§7Tax Rate: §f" + String.format("%.1f", nation.getTaxRate()) + "%",
                            "§7Click to manage."))
                    .consumer(e -> plugin.getGUIManager().openGUI(new AdminNationDetailGUI(plugin, nation.getNationId()), (Player) e.getWhoClicked()))
            );
        }

        if (page > 0) {
            int prev = page - 1;
            addButton(45, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "§ePrevious"))
                    .consumer(e -> plugin.getGUIManager().openGUI(new AdminNationGUI(plugin, prev), (Player) e.getWhoClicked()))
            );
        }
        if (end < nations.size()) {
            int next = page + 1;
            addButton(53, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "§eNext"))
                    .consumer(e -> plugin.getGUIManager().openGUI(new AdminNationGUI(plugin, next), (Player) e.getWhoClicked()))
            );
        }

        addButton(49, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.EMERALD, "§aCreate Nation",
                        "§7Click to create a new nation."))
                .consumer(e -> {
                    Player p = (Player) e.getWhoClicked();
                    p.closeInventory();
                    plugin.getChatInputManager().requestInput(p, "§eEnter the name for the new nation:", name -> {
                        if (name.isEmpty()) { p.sendMessage("§cNation name cannot be empty."); return; }
                        plugin.getGUIManager().openGUI(
                            new NationColorSelectGUI(plugin, name, null, (c1, c2) -> {
                                if (plugin.getRolesConfig().isCustomGovernmentEnabled()) {
                                    plugin.getChatInputManager().requestInput(p, "§eUse custom roles for this nation? §a(yes/no):", answer -> {
                                        if (answer.equalsIgnoreCase("yes")) {
                                            NationData nation = plugin.getNationManager().createNationCustom(name, c1, c2);
                                            p.sendMessage("§aNation §6" + name + "§a created with custom government! Opening role manager...");
                                            plugin.getServer().getScheduler().runTaskLater(plugin, () ->
                                                plugin.getGUIManager().openGUI(new CustomRolesGUI(plugin, nation.getNationId()), p), 5L);
                                        } else {
                                            selectPresidentAndCreate(p, name, c1, c2);
                                        }
                                    });
                                } else {
                                    selectPresidentAndCreate(p, name, c1, c2);
                                }
                            }),
                        p);
                    });
                })
        );

        super.decorate(player);
    }

    private void selectPresidentAndCreate(Player admin, String name, String c1, String c2) {
        List<CharacterData> allChars = plugin.getCharacterManager().getAllCharacters();
        plugin.getGUIManager().openGUI(
            new NationCharacterSelectGUI(plugin, "§eSelect President", allChars, presChar -> {
                NationData existingNation = plugin.getNationManager().getNationByMember(presChar.getPlayerUuid());
                if (existingNation != null) {
                    admin.sendMessage("§c" + presChar.getFirstName() + " " + presChar.getLastName() + " is already in §e" + existingNation.getName() + "§c.");
                    return;
                }
                NationData nation = plugin.getNationManager().createNation(name, c1, c2, presChar.getPlayerUuid());
                admin.sendMessage("§aNation §6" + name + "§a created! President: §e" + presChar.getFirstName() + " " + presChar.getLastName() + "§a.");
                Player presPlayer = plugin.getServer().getPlayer(presChar.getPlayerUuid());
                if (presPlayer != null) presPlayer.sendMessage("§eYou have been made President of " + c1 + name + "§e!");
            }, null, 0),
        admin);
    }
}
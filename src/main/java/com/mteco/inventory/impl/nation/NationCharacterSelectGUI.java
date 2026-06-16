package com.mteco.inventory.impl.nation;

import com.cryptomorin.xseries.XMaterial;
import com.mteco.MTeco;
import com.mteco.data.CharacterData;
import com.mteco.inventory.InventoryButton;
import com.mteco.inventory.InventoryGUI;
import com.mteco.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;

public class NationCharacterSelectGUI extends InventoryGUI {
    private final MTeco plugin;
    private final String title;
    private final List<CharacterData> candidates;
    private final Consumer<CharacterData> onSelect;
    private final InventoryGUI backGui;
    private final int page;

    public NationCharacterSelectGUI(MTeco plugin, String title, List<CharacterData> candidates,
                                    Consumer<CharacterData> onSelect, InventoryGUI backGui, int page) {
        this.plugin = plugin;
        this.title = title;
        this.candidates = candidates;
        this.onSelect = onSelect;
        this.backGui = backGui;
        this.page = page;
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 54, title);
    }

    @Override
    public void decorate(Player player) {
        fillPagedGui(54, XMaterial.BLUE_STAINED_GLASS_PANE);

        int perPage = 45;
        int start = page * perPage;
        int end = Math.min(start + perPage, candidates.size());

        for (int i = start; i < end; i++) {
            CharacterData candidate = candidates.get(i);
            int slot = i - start;
            addButton(slot, new InventoryButton()
                    .creator(p -> {
                        String fullName = candidate.getFirstName() + " " + candidate.getLastName();
                        String dateStr = new SimpleDateFormat("MM/dd/yyyy").format(new Date(candidate.getBirthDate()));
                        return ItemUtil.buildItem(XMaterial.PLAYER_HEAD, "§e" + fullName,
                                "§7Gender: §f" + candidate.getGender(),
                                "§7Born: §f" + dateStr,
                                "§7Click to select.");
                    })
                    .consumer(e -> {
                        Player p = (Player) e.getWhoClicked();
                        p.closeInventory();
                        plugin.getServer().getScheduler().runTaskLater(plugin, () -> onSelect.accept(candidate), 1L);
                    })
            );
        }

        if (page > 0) {
            int prev = page - 1;
            addButton(45, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "§ePrevious Page"))
                    .consumer(e -> plugin.getGUIManager().openGUI(
                            new NationCharacterSelectGUI(plugin, title, candidates, onSelect, backGui, prev),
                            (Player) e.getWhoClicked()))
            );
        }
        if (end < candidates.size()) {
            int next = page + 1;
            addButton(53, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "§eNext Page"))
                    .consumer(e -> plugin.getGUIManager().openGUI(
                            new NationCharacterSelectGUI(plugin, title, candidates, onSelect, backGui, next),
                            (Player) e.getWhoClicked()))
            );
        }

        if (backGui != null) {
            addButton(49, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.BARRIER, "§cBack"))
                    .consumer(e -> plugin.getGUIManager().openGUI(backGui, (Player) e.getWhoClicked()))
            );
        }

        super.decorate(player);
    }
}
package com.mteco.inventory.impl.nation;

import com.cryptomorin.xseries.XMaterial;
import com.mteco.MTeco;
import com.mteco.data.CharacterData;
import com.mteco.data.NationData;
import com.mteco.inventory.InventoryButton;
import com.mteco.inventory.InventoryGUI;
import com.mteco.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;

import java.util.List;
import java.util.UUID;

public class NationJoinRequestsGUI extends InventoryGUI {
    private final MTeco plugin;
    private final UUID nationId;
    private final int page;

    public NationJoinRequestsGUI(MTeco plugin, UUID nationId, int page) {
        this.plugin = plugin;
        this.nationId = nationId;
        this.page = page;
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 54, "§6Join Requests");
    }

    @Override
    public void decorate(Player player) {
        fillPagedGui(54, XMaterial.BLUE_STAINED_GLASS_PANE);

        NationData nation = plugin.getNationManager().loadNation(nationId);
        List<UUID> requests = nation != null ? nation.getJoinRequestUUIDs() : List.of();

        int perPage = 45;
        int start = page * perPage;
        int end = Math.min(start + perPage, requests.size());

        for (int i = start; i < end; i++) {
            UUID requestUUID = requests.get(i);
            CharacterData c = plugin.getCharacterManager().getCharacter(requestUUID);
            int slot = i - start;
            addButton(slot, new InventoryButton()
                    .creator(p -> {
                        if (c == null) return ItemUtil.buildItem(XMaterial.PLAYER_HEAD, "§eUnknown Player",
                                "", "§aLeft-click to accept", "§cRight-click to deny");
                        return ItemUtil.buildItem(XMaterial.PLAYER_HEAD,
                                "§e" + c.getFirstName() + " " + c.getLastName(),
                                "§7Gender: §f" + c.getGender(),
                                "",
                                "§aLeft-click to accept",
                                "§cRight-click to deny");
                    })
                    .consumer(e -> {
                        Player p = (Player) e.getWhoClicked();
                        NationData n = plugin.getNationManager().loadNation(nationId);
                        if (n == null) return;
                        if (!p.getUniqueId().equals(n.getPresidentUUID())) {
                            p.sendMessage("§cOnly the President can manage join requests.");
                            return;
                        }
                        if (!n.getJoinRequestUUIDs().contains(requestUUID)) {
                            p.sendMessage("§cThis request no longer exists.");
                            plugin.getGUIManager().openGUI(new NationJoinRequestsGUI(plugin, nationId, 0), p);
                            return;
                        }

                        CharacterData reqChar = plugin.getCharacterManager().getCharacter(requestUUID);
                        String reqName = reqChar != null ? reqChar.getFirstName() + " " + reqChar.getLastName() : "Unknown";

                        if (e.getClick() == ClickType.LEFT) {
                            n.getJoinRequestUUIDs().remove(requestUUID);
                            n.getMemberUUIDs().add(requestUUID);
                            plugin.getNationManager().saveNation(n);
                            p.sendMessage("§a" + reqName + " has been accepted into §e" + n.getName() + "§a!");
                            Player accepted = Bukkit.getPlayer(requestUUID);
                            if (accepted != null) {
                                accepted.sendMessage("§aYour request to join " + n.getColor1() + n.getName() + " §ahas been accepted!");
                            }
                        } else if (e.getClick() == ClickType.RIGHT) {
                            n.getJoinRequestUUIDs().remove(requestUUID);
                            plugin.getNationManager().saveNation(n);
                            p.sendMessage("§c" + reqName + "'s join request has been denied.");
                            Player denied = Bukkit.getPlayer(requestUUID);
                            if (denied != null) {
                                denied.sendMessage("§cYour request to join " + n.getColor1() + n.getName() + " §cwas denied.");
                            }
                        } else {
                            return;
                        }

                        int newSize = n.getJoinRequestUUIDs().size();
                        int maxPage = Math.max(0, (newSize - 1) / perPage);
                        int safePage = Math.min(page, maxPage);
                        plugin.getGUIManager().openGUI(new NationJoinRequestsGUI(plugin, nationId, safePage), p);
                    })
            );
        }

        if (page > 0) {
            int prev = page - 1;
            addButton(45, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "§ePrevious Page"))
                    .consumer(e -> plugin.getGUIManager().openGUI(new NationJoinRequestsGUI(plugin, nationId, prev), (Player) e.getWhoClicked()))
            );
        }
        if (end < requests.size()) {
            int next = page + 1;
            addButton(53, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "§eNext Page"))
                    .consumer(e -> plugin.getGUIManager().openGUI(new NationJoinRequestsGUI(plugin, nationId, next), (Player) e.getWhoClicked()))
            );
        }

        addButton(49, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "§cBack"))
                .consumer(e -> plugin.getGUIManager().openGUI(new NationMembersGUI(plugin, nationId, 0), (Player) e.getWhoClicked()))
        );

        super.decorate(player);
    }
}
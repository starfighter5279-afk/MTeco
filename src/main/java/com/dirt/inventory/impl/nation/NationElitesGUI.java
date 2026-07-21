package com.dirt.inventory.impl.nation;

import com.cryptomorin.xseries.XMaterial;
import com.dirt.DirtEconomy;
import com.dirt.config.RolesConfig;
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

public class NationElitesGUI extends InventoryGUI {
    private final DirtEconomy plugin;
    private final UUID nationId;

    public NationElitesGUI(DirtEconomy plugin, UUID nationId) {
        this.plugin = plugin;
        this.nationId = nationId;
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 27, "§bNation Elites");
    }

    @Override
    public void decorate(Player player) {
        NationData nation = plugin.getNationManager().loadNation(nationId);
        fillGlass(27, XMaterial.BLUE_STAINED_GLASS_PANE);

        RolesConfig rc = plugin.getRolesConfig();

        List<CharacterData> memberChars = nation == null ? List.of() : nation.getMemberUUIDs().stream()
                .filter(u -> !u.equals(nation.getPresidentUUID()))
                .map(u -> plugin.getCharacterManager().getCharacter(u))
                .filter(c -> c != null)
                .collect(Collectors.toList());

        String tres = resolveCharName(nation != null ? nation.getTreasurerUUID() : null);
        String vp = resolveCharName(nation != null ? nation.getVicePresidentUUID() : null);
        String sec = resolveCharName(nation != null ? nation.getSecurityHeadUUID() : null);

        if (rc.isTreasurerEnabled()) {
            addButton(11, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.GOLD_INGOT, "§eTreasurer",
                            "§7Current: §f" + tres,
                            "§7Click to change."))
                    .consumer(e -> {
                        Player p = (Player) e.getWhoClicked();
                        plugin.getGUIManager().openGUI(new NationCharacterSelectGUI(plugin, "§eSelect Treasurer", memberChars, c -> {
                            NationData n = plugin.getNationManager().loadNation(nationId);
                            if (n == null) return;
                            n.setTreasurerUUID(c.getPlayerUuid());
                            checkAndSetElitesConfigured(n);
                            plugin.getNationManager().saveNation(n);
                            p.sendMessage("§a" + c.getFirstName() + " " + c.getLastName() + " has been appointed §eTreasurer§a.");
                            plugin.getGUIManager().openGUI(new NationElitesGUI(plugin, nationId), p);
                        }, new NationElitesGUI(plugin, nationId), 0), p);
                    })
            );
        } else {
            addButton(11, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.GRAY_DYE, "§7Treasurer", "§cDisabled by server."))
                    .consumer(e -> {}));
        }

        if (rc.isVicePresidentEnabled()) {
            addButton(13, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.BOOK, "§aVice President",
                            "§7Current: §f" + vp,
                            "§7Click to change."))
                    .consumer(e -> {
                        Player p = (Player) e.getWhoClicked();
                        plugin.getGUIManager().openGUI(new NationCharacterSelectGUI(plugin, "§eSelect Vice President", memberChars, c -> {
                            NationData n = plugin.getNationManager().loadNation(nationId);
                            if (n == null) return;
                            n.setVicePresidentUUID(c.getPlayerUuid());
                            checkAndSetElitesConfigured(n);
                            plugin.getNationManager().saveNation(n);
                            p.sendMessage("§a" + c.getFirstName() + " " + c.getLastName() + " has been appointed §aVice President§a.");
                            plugin.getGUIManager().openGUI(new NationElitesGUI(plugin, nationId), p);
                        }, new NationElitesGUI(plugin, nationId), 0), p);
                    })
            );
        } else {
            addButton(13, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.GRAY_DYE, "§7Vice President", "§cDisabled by server."))
                    .consumer(e -> {}));
        }

        if (rc.isSecurityHeadEnabled()) {
            addButton(15, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.IRON_SWORD, "§cNational Security Head",
                            "§7Current: §f" + sec,
                            "§7Click to change."))
                    .consumer(e -> {
                        Player p = (Player) e.getWhoClicked();
                        plugin.getGUIManager().openGUI(new NationCharacterSelectGUI(plugin, "§eSelect National Security Head", memberChars, c -> {
                            NationData n = plugin.getNationManager().loadNation(nationId);
                            if (n == null) return;
                            n.setSecurityHeadUUID(c.getPlayerUuid());
                            checkAndSetElitesConfigured(n);
                            plugin.getNationManager().saveNation(n);
                            p.sendMessage("§a" + c.getFirstName() + " " + c.getLastName() + " has been appointed §cNational Security Head§a.");
                            plugin.getGUIManager().openGUI(new NationElitesGUI(plugin, nationId), p);
                        }, new NationElitesGUI(plugin, nationId), 0), p);
                    })
            );
        } else {
            addButton(15, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.GRAY_DYE, "§7National Security Head", "§cDisabled by server."))
                    .consumer(e -> {}));
        }

        NationData finalNation = nation;
        boolean allRequiredFilled = areAllEnabledElitesFilled(finalNation, rc);
        if (finalNation != null && allRequiredFilled) {
            addButton(26, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "§cBack"))
                    .consumer(e -> plugin.getGUIManager().openGUI(new MemberManagementGUI(plugin, nationId), (Player) e.getWhoClicked()))
            );
        }

        super.decorate(player);
    }

    private boolean areAllEnabledElitesFilled(NationData nation, RolesConfig rc) {
        if (nation == null) return false;
        if (rc.isTreasurerEnabled() && nation.getTreasurerUUID() == null) return false;
        if (rc.isVicePresidentEnabled() && nation.getVicePresidentUUID() == null) return false;
        if (rc.isSecurityHeadEnabled() && nation.getSecurityHeadUUID() == null) return false;
        return true;
    }

    private void checkAndSetElitesConfigured(NationData nation) {
        RolesConfig rc = plugin.getRolesConfig();
        boolean filled = true;
        if (rc.isTreasurerEnabled() && nation.getTreasurerUUID() == null) filled = false;
        if (rc.isVicePresidentEnabled() && nation.getVicePresidentUUID() == null) filled = false;
        if (rc.isSecurityHeadEnabled() && nation.getSecurityHeadUUID() == null) filled = false;
        nation.setElitesConfigured(filled);
    }

    private String resolveCharName(UUID uuid) {
        if (uuid == null) return "§7Unassigned";
        CharacterData c = plugin.getCharacterManager().getCharacter(uuid);
        return c != null ? c.getFirstName() + " " + c.getLastName() : "§7Unknown";
    }
}
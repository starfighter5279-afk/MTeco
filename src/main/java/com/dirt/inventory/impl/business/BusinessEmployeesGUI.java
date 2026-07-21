package com.dirt.inventory.impl.business;

import com.cryptomorin.xseries.XMaterial;
import com.dirt.DirtEconomy;
import com.dirt.data.BusinessData;
import com.dirt.data.BusinessRole;
import com.dirt.data.CharacterData;
import com.dirt.data.MailItem;
import com.dirt.inventory.InventoryButton;
import com.dirt.inventory.InventoryGUI;
import com.dirt.inventory.impl.nation.NationCharacterSelectGUI;
import com.dirt.util.ItemUtil;
import com.dirt.util.CurrencyUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class BusinessEmployeesGUI extends InventoryGUI {
    private final DirtEconomy plugin;
    private final UUID businessId;

    public BusinessEmployeesGUI(DirtEconomy plugin, UUID businessId) {
        this.plugin = plugin;
        this.businessId = businessId;
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 54, "\u00a7bEmployees & Payroll");
    }

    @Override
    public void decorate(Player player) {
        fillPagedGui(54, XMaterial.LIME_STAINED_GLASS_PANE);

        BusinessData biz = plugin.getBusinessManager().loadBusiness(businessId);
        if (biz == null) { super.decorate(player); return; }

        List<UUID> employees = biz.getEmployeeUUIDs();
        int slot = 0;
        for (UUID empUUID : employees) {
            if (slot >= 36) break;
            CharacterData empChar = plugin.getCharacterManager().getCharacter(empUUID);
            String empName = empChar != null ? empChar.getFirstName() + " " + empChar.getLastName() : empUUID.toString().substring(0, 8);
            double rate = biz.getEmployeeRates().getOrDefault(empUUID, biz.getPayrollRate());
            BusinessRole empRole = biz.getRoleForEmployee(empUUID);
            String roleStr = empRole != null ? empRole.getColorCode() + empRole.getName() : "\u00a77No Role";
            final String finalName = empName;
            final String finalRole = roleStr;
            addButton(slot, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.PLAYER_HEAD, "\u00a7f" + finalName,
                            "\u00a77Role: " + finalRole,
                            "\u00a77Weekly Pay: \u00a7e" + CurrencyUtil.symbol() + String.format("%.2f", rate),
                            "\u00a77Click to manage."))
                    .consumer(e -> plugin.getGUIManager().openGUI(
                            new EmployeeActionGUI(plugin, businessId, empUUID), (Player) e.getWhoClicked()))
            );
            slot++;
        }

        addButton(45, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.GOLD_INGOT, "\u00a7eSet Default Payroll Rate",
                        "\u00a77Current: \u00a7e" + CurrencyUtil.symbol() + String.format("%.2f", biz.getPayrollRate()),
                        "\u00a77This rate applies to employees with no personal rate."))
                .consumer(e -> setPayrollRate(biz, (Player) e.getWhoClicked()))
        );

        addButton(47, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.EMERALD, "\u00a7aOffer Job",
                        "\u00a77Select a character to send a job offer to."))
                .consumer(e -> {
                    Player p = (Player) e.getWhoClicked();
                    List<CharacterData> living = plugin.getCharacterManager().getAllCharacters().stream()
                            .filter(CharacterData::isAlive)
                            .filter(c -> !c.getPlayerUuid().equals(p.getUniqueId()))
                            .collect(Collectors.toList());
                    plugin.getGUIManager().openGUI(
                            new NationCharacterSelectGUI(plugin, "\u00a7aSelect Character to Offer Job", living,
                                    target -> sendJobOffer(biz, p, target),
                                    new BusinessEmployeesGUI(plugin, businessId), 0),
                            p);
                })
        );

        addButton(51, new InventoryButton()
                .creator(p -> {
                    if (biz.isHiring()) {
                        return ItemUtil.buildItem(XMaterial.LIME_WOOL, "\u00a7aHiring: ON",
                                "\u00a77The business is currently accepting applications.",
                                "\u00a77Click to stop hiring.");
                    }
                    return ItemUtil.buildItem(XMaterial.RED_WOOL, "\u00a7cHiring: OFF",
                            "\u00a77The business is not accepting applications.",
                            "\u00a77Click to start hiring.");
                })
                .consumer(e -> {
                    biz.setHiring(!biz.isHiring());
                    plugin.getBusinessManager().saveBusiness(biz);
                    Player p = (Player) e.getWhoClicked();
                    p.sendMessage(biz.isHiring() ? "\u00a7aHiring is now \u00a7eON\u00a7a for " + biz.getName() + "." : "\u00a7cHiring is now \u00a7eOFF\u00a7c for " + biz.getName() + ".");
                    plugin.getServer().getScheduler().runTask(plugin,
                            () -> plugin.getGUIManager().openGUI(new BusinessEmployeesGUI(plugin, businessId), p));
                })
        );

        addButton(49, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7cBack"))
                .consumer(e -> plugin.getGUIManager().openGUI(new BusinessManagementGUI(plugin, businessId), (Player) e.getWhoClicked()))
        );

        super.decorate(player);
    }

    private void setPayrollRate(BusinessData biz, Player player) {
        player.closeInventory();
        plugin.getChatInputManager().requestInput(player,
                "\u00a7eEnter the new default payroll rate (current: \u00a7e" + CurrencyUtil.symbol() + String.format("%.2f", biz.getPayrollRate()) + "\u00a7e):",
                rateStr -> {
                    double rate;
                    try { rate = Double.parseDouble(rateStr.trim()); } catch (NumberFormatException ex) {
                        player.sendMessage("\u00a7cInvalid rate."); return;
                    }
                    if (rate < 0) { player.sendMessage("\u00a7cRate cannot be negative."); return; }
                    biz.setPayrollRate(rate);
                    plugin.getBusinessManager().saveBusiness(biz);
                    player.sendMessage("\u00a7aDefault payroll rate set to \u00a7e" + CurrencyUtil.symbol() + String.format("%.2f", rate) + "\u00a7a.");
                    plugin.getServer().getScheduler().runTask(plugin,
                            () -> plugin.getGUIManager().openGUI(new BusinessEmployeesGUI(plugin, businessId), player));
                });
    }

    private void sendJobOffer(BusinessData biz, Player owner, CharacterData target) {
        CharacterData ownerChar = plugin.getCharacterManager().getCharacter(owner.getUniqueId());
        String ownerName = ownerChar != null ? ownerChar.getFirstName() + " " + ownerChar.getLastName() : owner.getName();

        MailItem mail = new MailItem();
        mail.setId(UUID.randomUUID().toString());
        mail.setType("JOB_OFFER");
        mail.setFromPlayerUuid(owner.getUniqueId());
        mail.setTimestamp(System.currentTimeMillis());
        Map<String, String> data = new HashMap<>();
        data.put("businessId", biz.getBusinessId().toString());
        data.put("businessName", biz.getName());
        data.put("payrollRate", String.format("%.2f", biz.getPayrollRate()));
        data.put("ownerName", ownerName);
        data.put("fromName", ownerName);
        mail.setData(data);

        plugin.getCharacterManager().addMailItem(target.getPlayerUuid(), mail);
        owner.sendMessage("\u00a7aJob offer sent to \u00a7e" + target.getFirstName() + " " + target.getLastName() + "\u00a7a!");

        Player targetPlayer = Bukkit.getPlayer(target.getPlayerUuid());
        if (targetPlayer != null) {
            targetPlayer.sendMessage("\u00a7eYou received a job offer from \u00a76" + biz.getName() + "\u00a7e!");
        }

        plugin.getServer().getScheduler().runTask(plugin,
                () -> plugin.getGUIManager().openGUI(new BusinessEmployeesGUI(plugin, businessId), owner));
    }
}
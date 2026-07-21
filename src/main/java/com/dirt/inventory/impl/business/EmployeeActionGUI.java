package com.dirt.inventory.impl.business;

import com.cryptomorin.xseries.XMaterial;
import com.dirt.DirtEconomy;
import com.dirt.data.BusinessData;
import com.dirt.data.BusinessRole;
import com.dirt.data.CharacterData;
import com.dirt.inventory.InventoryButton;
import com.dirt.inventory.InventoryGUI;
import com.dirt.util.ItemUtil;
import com.dirt.util.CurrencyUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.UUID;

public class EmployeeActionGUI extends InventoryGUI {
    private final DirtEconomy plugin;
    private final UUID businessId;
    private final UUID employeeUUID;

    public EmployeeActionGUI(DirtEconomy plugin, UUID businessId, UUID employeeUUID) {
        this.plugin = plugin;
        this.businessId = businessId;
        this.employeeUUID = employeeUUID;
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 27, "\u00a7bEmployee Management");
    }

    @Override
    public void decorate(Player player) {
        fillGlass(27, XMaterial.LIME_STAINED_GLASS_PANE);

        BusinessData biz = plugin.getBusinessManager().loadBusiness(businessId);
        if (biz == null) { super.decorate(player); return; }

        CharacterData empChar = plugin.getCharacterManager().getCharacter(employeeUUID);
        String empName = empChar != null ? empChar.getFirstName() + " " + empChar.getLastName() : "Unknown";
        double currentRate = biz.getEmployeeRates().getOrDefault(employeeUUID, biz.getPayrollRate());

        addButton(4, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.PLAYER_HEAD, "\u00a7f" + empName,
                        "\u00a77Weekly Pay: \u00a7e" + CurrencyUtil.symbol() + String.format("%.2f", currentRate)))
                .consumer(e -> {})
        );

        addButton(11, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.GOLD_NUGGET, "\u00a7eGive Pay Raise",
                        "\u00a77Current rate: \u00a7e" + CurrencyUtil.symbol() + String.format("%.2f", currentRate),
                        "\u00a77Enter the amount to add to their pay."))
                .consumer(e -> handleRaise(biz, (Player) e.getWhoClicked(), currentRate))
        );

        addButton(15, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.RED_WOOL, "\u00a7cFire Employee",
                        "\u00a77Remove " + empName + " from this business."))
                .consumer(e -> handleFire(biz, (Player) e.getWhoClicked(), empName))
        );

        BusinessRole currentRole = biz.getRoleForEmployee(employeeUUID);
        String roleName = currentRole != null ? currentRole.getColorCode() + currentRole.getName() : "\u00a77None";
        addButton(13, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.NAME_TAG, "\u00a7dRole: " + roleName,
                        "\u00a77Click to change role assignment.",
                        "\u00a77Available roles: \u00a7f" + biz.getRoles().size()))
                .consumer(e -> handleRoleChange(biz, (Player) e.getWhoClicked(), empName))
        );

        addButton(22, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.ARROW, "\u00a7cBack"))
                .consumer(e -> plugin.getGUIManager().openGUI(new BusinessEmployeesGUI(plugin, businessId), (Player) e.getWhoClicked()))
        );

        super.decorate(player);
    }

    private void handleRaise(BusinessData biz, Player player, double currentRate) {
        player.closeInventory();
        plugin.getChatInputManager().requestInput(player,
                "\u00a7eEnter the amount to ADD to their weekly pay (current: \u00a7e" + CurrencyUtil.symbol() + String.format("%.2f", currentRate) + "\u00a7e):",
                amtStr -> {
                    double delta;
                    try { delta = Double.parseDouble(amtStr.trim()); } catch (NumberFormatException ex) {
                        player.sendMessage("\u00a7cInvalid amount."); return;
                    }
                    if (delta <= 0) { player.sendMessage("\u00a7cAmount must be greater than zero."); return; }
                    double newRate = currentRate + delta;
                    biz.getEmployeeRates().put(employeeUUID, newRate);
                    plugin.getBusinessManager().saveBusiness(biz);
                    player.sendMessage("\u00a7aPay raise applied. New rate: \u00a7e" + CurrencyUtil.symbol() + String.format("%.2f", newRate));
                    Player emp = Bukkit.getPlayer(employeeUUID);
                    if (emp != null) emp.sendMessage("\u00a7aYou received a pay raise at \u00a76" + biz.getName() + "\u00a7a! New rate: \u00a7e" + CurrencyUtil.symbol() + String.format("%.2f", newRate));
                    plugin.getServer().getScheduler().runTask(plugin,
                            () -> plugin.getGUIManager().openGUI(new BusinessEmployeesGUI(plugin, businessId), player));
                });
    }

    private void handleFire(BusinessData biz, Player player, String empName) {
        biz.getEmployeeUUIDs().remove(employeeUUID);
        biz.getEmployeeRates().remove(employeeUUID);
        for (BusinessRole role : biz.getRoles()) {
            role.getMemberUUIDs().remove(employeeUUID);
        }
        plugin.getBusinessManager().saveBusiness(biz);
        player.sendMessage("\u00a7c" + empName + " has been fired from " + biz.getName() + ".");
        Player emp = Bukkit.getPlayer(employeeUUID);
        if (emp != null) emp.sendMessage("\u00a7cYou have been fired from \u00a76" + biz.getName() + "\u00a7c.");
        plugin.getGUIManager().openGUI(new BusinessEmployeesGUI(plugin, businessId), player);
    }

    private void handleRoleChange(BusinessData biz, Player player, String empName) {
        if (biz.getRoles().isEmpty()) {
            player.sendMessage("\u00a7cNo roles have been created yet. Create roles in the Roles menu.");
            return;
        }
        player.closeInventory();
        StringBuilder msg = new StringBuilder("\u00a7eSelect a role for " + empName + ":\n");
        for (int i = 0; i < biz.getRoles().size(); i++) {
            BusinessRole role = biz.getRoles().get(i);
            msg.append("\u00a7e").append(i + 1).append(". ").append(role.getColorCode()).append(role.getName()).append("\n");
        }
        msg.append("\u00a7e0. Remove from current role\n");
        msg.append("\u00a77Type the number:");
        plugin.getChatInputManager().requestInput(player, msg.toString(), input -> {
            int choice;
            try { choice = Integer.parseInt(input.trim()); } catch (NumberFormatException ex) {
                player.sendMessage("\u00a7cInvalid number."); return;
            }
            if (choice == 0) {
                for (BusinessRole role : biz.getRoles()) {
                    role.getMemberUUIDs().remove(employeeUUID);
                }
                plugin.getBusinessManager().saveBusiness(biz);
                player.sendMessage("\u00a7aRemoved " + empName + " from all roles.");
            } else if (choice > 0 && choice <= biz.getRoles().size()) {
                for (BusinessRole role : biz.getRoles()) {
                    role.getMemberUUIDs().remove(employeeUUID);
                }
                BusinessRole selected = biz.getRoles().get(choice - 1);
                selected.getMemberUUIDs().add(employeeUUID);
                plugin.getBusinessManager().saveBusiness(biz);
                player.sendMessage("\u00a7aAssigned " + empName + " to role " + selected.getColorCode() + selected.getName() + "\u00a7a.");
            } else {
                player.sendMessage("\u00a7cInvalid choice.");
            }
            plugin.getServer().getScheduler().runTask(plugin,
                    () -> plugin.getGUIManager().openGUI(new EmployeeActionGUI(plugin, businessId, employeeUUID), player));
        });
    }
}
package com.mteco.listeners;

import com.mteco.MTeco;
import com.mteco.data.CharacterData;
import com.mteco.inventory.impl.IDViewGUI;
import com.mteco.inventory.impl.MarriageConfirmGUI;
import com.mteco.managers.FamilyManager;
import com.mteco.data.FamilyData;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class IDInteractListener implements Listener {
    private final MTeco plugin;

    public IDInteractListener(MTeco plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        // Only fire once (main hand fires the event twice on some versions)
        if (event.getHand() != EquipmentSlot.HAND) return;

        if (!(event.getRightClicked() instanceof Player target)) return;

        Player viewer = event.getPlayer();
        ItemStack offhand = viewer.getInventory().getItemInOffHand();

        if (offhand.getType() == Material.DIAMOND) {
            handleMarriageRequest(event, viewer, target);
            return;
        }

        if (offhand.getType() != Material.PAPER) return;

        CharacterData data = plugin.getCharacterManager().getCharacter(target.getUniqueId());
        if (data == null) {
            viewer.sendMessage("§c" + target.getName() + " has no active character.");
            return;
        }

        event.setCancelled(true);
        plugin.getGUIManager().openGUI(new IDViewGUI(plugin, data, target.getUniqueId(), false), viewer);
    }

    private void handleMarriageRequest(PlayerInteractEntityEvent event, Player viewer, Player target) {
        if (!plugin.isFamilyEnabled()) {
            viewer.sendMessage("§cThe family system is currently disabled.");
            return;
        }

        CharacterData myData = plugin.getCharacterManager().getCharacter(viewer.getUniqueId());
        if (myData == null) {
            viewer.sendMessage("§cYou do not have an active character.");
            return;
        }

        CharacterData targetData = plugin.getCharacterManager().getCharacter(target.getUniqueId());
        if (targetData == null) {
            viewer.sendMessage("§c" + target.getName() + " does not have an active character.");
            return;
        }

        if (myData.getGender() == null || myData.getGender().equalsIgnoreCase(targetData.getGender())) {
            viewer.sendMessage("§cYou can only propose to a character of the opposite gender.");
            return;
        }

        // Check if viewer is already a spouse in a family
        if (isMarried(myData)) {
            viewer.sendMessage("§cYour character is already married.");
            return;
        }

        // Check if target is already a spouse in a family
        if (isMarried(targetData)) {
            viewer.sendMessage("§c" + targetData.getFirstName() + " " + targetData.getLastName() + " is already married.");
            return;
        }

        event.setCancelled(true);
        plugin.getGUIManager().openGUI(new MarriageConfirmGUI(plugin, target.getUniqueId()), viewer);
    }

    private boolean isMarried(CharacterData data) {
        if (data.getFamilyId() == null) return false;
        // A CHILD role means they're in a family as a child, not as a spouse
        return !"CHILD".equals(data.getFamilyRole());
    }
}
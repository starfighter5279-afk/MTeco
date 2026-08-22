package com.dirt.listeners;

import com.cryptomorin.xseries.XEnchantment;
import com.cryptomorin.xseries.XMaterial;
import com.dirt.DirtEconomy;
import com.dirt.data.PhoneData;
import com.dirt.inventory.impl.phone.PhoneLockscreenGUI;
import com.dirt.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.entity.Player;

import java.util.Arrays;

public class PhoneListener implements Listener {
    private final DirtEconomy plugin;
    private final NamespacedKey phoneKey;

    public PhoneListener(DirtEconomy plugin) {
        this.plugin = plugin;
        this.phoneKey = NamespacedKey.fromString(plugin.getName().toLowerCase(java.util.Locale.ROOT) + ":phone");
        registerRecipe();
    }

    private void registerRecipe() {
        ItemStack phone = createPhoneItem();
        NamespacedKey recipeKey = NamespacedKey.fromString(plugin.getName().toLowerCase(java.util.Locale.ROOT) + ":phone_recipe");
        ShapedRecipe recipe = new ShapedRecipe(recipeKey, phone);
        recipe.shape("GG ", "CC ", "RB ");
        recipe.setIngredient('G', Material.GLASS_PANE);
        recipe.setIngredient('C', Material.COPPER_INGOT);
        recipe.setIngredient('R', Material.REDSTONE);
        recipe.setIngredient('B', Material.STONE_BUTTON);
        Bukkit.addRecipe(recipe);
    }

    public ItemStack createPhoneItem() {
        ItemStack item = XMaterial.matchXMaterial("NETHERITE_INGOT").map(XMaterial::parseItem).orElse(new ItemStack(Material.NETHERITE_INGOT));
        ItemMeta meta = item.getItemMeta();
        ItemUtil.setDisplayName(meta, "\u00a7b\u00a7lPhone");
        ItemUtil.setLore(meta, Arrays.asList("\u00a77Right-click to open your phone"));
        XEnchantment.matchXEnchantment("UNBREAKING").ifPresent(enchantment -> meta.addEnchant(enchantment.getEnchant(), 1, true));
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.getPersistentDataContainer().set(phoneKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public boolean isPhone(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(phoneKey, PersistentDataType.BYTE);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack item = event.getItem();
        if (!isPhone(item)) return;

        event.setCancelled(true);
        Player player = event.getPlayer();

        if (!plugin.getCharacterManager().hasCharacter(player.getUniqueId())) {
            player.sendMessage("\u00a7cYou need a character to use a phone.");
            return;
        }

        PhoneData phone = plugin.getPhoneManager().getPhone(player.getUniqueId());
        if (phone == null || phone.getUsername() == null) {
            plugin.getChatInputManager().requestInput(player,
                    "\u00a7b\u2588\u2588\u2588 Phone Setup \u2588\u2588\u2588\n\u00a7eEnter a unique username for your phone.\n\u00a77This will be visible to other players.\n\u00a77Type your username in chat:",
                    input -> {
                        String username = input.trim();
                        if (username.length() < 3 || username.length() > 16) {
                            player.sendMessage("\u00a7cUsername must be 3-16 characters.");
                            return;
                        }
                        if (!username.matches("[a-zA-Z0-9_]+")) {
                            player.sendMessage("\u00a7cUsername can only contain letters, numbers, and underscores.");
                            return;
                        }
                        if (plugin.getPhoneManager().isUsernameTaken(username)) {
                            player.sendMessage("\u00a7cThat username is already taken. Try a different one and right-click your phone again.");
                            return;
                        }
                        plugin.getPhoneManager().setUsername(player.getUniqueId(), username);
                        player.sendMessage("\u00a7aPhone username set to: \u00a7b" + username);
                        player.sendMessage("\u00a7eRight-click your phone again to open it!");
                    });
            return;
        }

        plugin.getGUIManager().openGUI(new PhoneLockscreenGUI(plugin), player);
    }

    @EventHandler
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        for (ItemStack ingredient : event.getInventory().getMatrix()) {
            if (isPhone(ingredient)) {
                event.getInventory().setResult(null);
                return;
            }
        }
    }

    @EventHandler
    public void onCraft(CraftItemEvent event) {
        for (ItemStack ingredient : event.getInventory().getMatrix()) {
            if (isPhone(ingredient)) {
                event.setCancelled(true);
                return;
            }
        }
    }
}
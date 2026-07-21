package com.dirt.inventory.impl;

import com.cryptomorin.xseries.XMaterial;
import com.dirt.DirtEconomy;
import com.dirt.config.LifeConfig;
import com.dirt.data.CharacterData;
import com.dirt.inventory.InventoryButton;
import com.dirt.inventory.InventoryGUI;
import com.dirt.util.DiscordBotManager;
import com.dirt.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;

public class DeathScreenGUI extends InventoryGUI {
    private final DirtEconomy plugin;
    private final String characterName;
    private final String causeOfDeath;

    public DeathScreenGUI(DirtEconomy plugin, String characterName, String causeOfDeath) {
        this.plugin = plugin;
        this.characterName = characterName;
        this.causeOfDeath = causeOfDeath;
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 54, "§4§l☠ Death Screen");
    }

    @Override
    public void decorate(Player player) {
        LifeConfig cfg = plugin.getLifeConfig();
        boolean tokensEnabled = cfg.isLifeTokensEnabled();
        int playerTokens = plugin.getLifeManager().getTokens(player.getUniqueId());

        for (int i = 0; i < 54; i++) {
            final int slot = i;
            addButton(i, new InventoryButton()
                    .creator(p -> {
                        boolean edge = slot % 9 == 0 || slot % 9 == 8 || slot < 9 || slot >= 45;
                        return edge
                                ? ItemUtil.buildGlassPane(XMaterial.RED_STAINED_GLASS_PANE)
                                : ItemUtil.buildGlassPane(XMaterial.BLACK_STAINED_GLASS_PANE);
                    })
                    .consumer(e -> {})
            );
        }

        addButton(4, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.SKELETON_SKULL,
                        "§4§l☠ " + characterName + " Has Died",
                        "",
                        "§7Cause of Death:",
                        "§c§o" + causeOfDeath,
                        "",
                        "§8Rest in peace..."))
                .consumer(e -> {})
        );

        addButton(19, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.SOUL_LANTERN, "§8§o..."))
                .consumer(e -> {})
        );
        addButton(25, new InventoryButton()
                .creator(p -> ItemUtil.buildItem(XMaterial.SOUL_LANTERN, "§8§o..."))
                .consumer(e -> {})
        );

        if (tokensEnabled) {
            addButton(22, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.SUNFLOWER,
                            "§6§l✦ Life Tokens",
                            "",
                            "§7You have: §e§l" + playerTokens + " §7token" + (playerTokens != 1 ? "s" : ""),
                            "",
                            playerTokens > 0 ? "§aYou can revive this character!" : "§cNo tokens... this life is over."))
                    .consumer(e -> {})
            );
        }

        boolean showRevive = tokensEnabled && playerTokens > 0;
        boolean showGetTokens = tokensEnabled && playerTokens <= 0 && cfg.isGetTokensButtonEnabled();

        if (showRevive) {
            addButton(29, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.RED_WOOL,
                            cfg.getContinueButtonTitle(),
                            "",
                            "§7Your character will be §cpermanently gone§7.",
                            "§7You'll create a brand new character.",
                            "",
                            "§c§lClick to move on..."))
                    .consumer(e -> handleContinueToNewLife((Player) e.getWhoClicked()))
            );

            addButton(33, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.TOTEM_OF_UNDYING,
                            cfg.getReviveButtonTitle(),
                            "",
                            "§7Use §e1 Life Token §7to revive §f" + characterName + "§7!",
                            "§7Keep your character, items, & money.",
                            "",
                            "§a§lClick to revive!"))
                    .consumer(e -> handleUseLifeToken((Player) e.getWhoClicked()))
            );
        } else if (showGetTokens) {
            addButton(29, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.RED_WOOL,
                            cfg.getContinueButtonTitle(),
                            "",
                            "§7Your character will be §cpermanently gone§7.",
                            "§7You'll create a brand new character.",
                            "",
                            "§c§lClick to move on..."))
                    .consumer(e -> handleContinueToNewLife((Player) e.getWhoClicked()))
            );

            addButton(33, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.GOLD_INGOT,
                            cfg.getGetTokensButtonTitle(),
                            "",
                            "§7Get life tokens to revive in the future!",
                            "",
                            "§e§lClick for details"))
                    .consumer(e -> handleGetTokens((Player) e.getWhoClicked()))
            );
        } else {
            addButton(31, new InventoryButton()
                    .creator(p -> ItemUtil.buildItem(XMaterial.RED_WOOL,
                            cfg.getContinueButtonTitle(),
                            "",
                            "§7Your character will be §cpermanently gone§7.",
                            "§7You'll create a brand new character.",
                            "",
                            "§c§lClick to move on..."))
                    .consumer(e -> handleContinueToNewLife((Player) e.getWhoClicked()))
            );
        }

        super.decorate(player);
    }

    private void handleContinueToNewLife(Player player) {
        plugin.getLifeManager().setProcessingDeath(player.getUniqueId());
        plugin.getLifeManager().consumePendingDeathScreen(player.getUniqueId());
        player.closeInventory();

        player.sendMessage("");
        player.sendMessage("§4§m                                          §r");
        player.sendMessage("§4§l  ☠ §c" + characterName + " §7is gone forever...");
        player.sendMessage("");
        player.sendMessage("§e  Right-click any block to place your");
        player.sendMessage("§e  inheritance chest at that location.");
        player.sendMessage("§4§m                                          §r");
        player.sendMessage("");

        plugin.getLocationSelectionManager().requestLocation(player,
                "§eRight-click a block to select the chest location.",
                loc -> {
                    plugin.getLifeManager().clearProcessingDeath(player.getUniqueId());
                    plugin.executeDeath(player, loc);
                });
    }

    private void handleUseLifeToken(Player player) {
        if (!plugin.getLifeManager().useToken(player.getUniqueId())) {
            player.sendMessage("§cYou don't have any life tokens!");
            return;
        }

        plugin.getLifeManager().consumePendingDeathScreen(player.getUniqueId());
        player.closeInventory();

        int remaining = plugin.getLifeManager().getTokens(player.getUniqueId());
        player.sendMessage("");
        player.sendMessage("§a§m                                          §r");
        player.sendMessage("§a§l  ✦ REVIVED! §e" + characterName + " §alives on!");
        player.sendMessage("§7  Used §e1 Life Token §7— §e" + remaining + " §7remaining.");
        player.sendMessage("§a§m                                          §r");
        player.sendMessage("");
    }

    private void handleGetTokens(Player player) {
        LifeConfig cfg = plugin.getLifeConfig();
        String action = cfg.getGetTokensAction();

        switch (action.toLowerCase()) {
            case "link":
                String link = cfg.getGetTokensLink();
                player.sendMessage("");
                player.sendMessage("§e§m                                          §r");
                player.sendMessage("§e§l  ★ §6Get Life Tokens:");
                player.sendMessage("§b  §n" + link);
                player.sendMessage("§e§m                                          §r");
                player.sendMessage("");
                sendDiscordTokenLink(player, link);
                break;

            case "instructions":
                String instructions = cfg.getGetTokensInstructions();
                player.sendMessage("");
                player.sendMessage("§e§m                                          §r");
                player.sendMessage("§e§l  ★ §6How to get Life Tokens:");
                player.sendMessage("§f  " + instructions);
                player.sendMessage("§e§m                                          §r");
                player.sendMessage("");
                break;

            case "grant":
                plugin.getLifeManager().addTokens(player.getUniqueId(), 1);
                player.sendMessage("");
                player.sendMessage("§a§l✦ §eYou received §a§l1 Life Token§e!");
                player.sendMessage("§7You can now revive your character.");
                player.sendMessage("");
                plugin.getGUIManager().openGUI(
                        new DeathScreenGUI(plugin, characterName, causeOfDeath), player);
                break;

            default:
                player.sendMessage("§cThis feature is not configured properly.");
                break;
        }
    }

    private void sendDiscordTokenLink(Player player, String link) {
        DiscordBotManager bot = plugin.getDiscordBotManager();
        if (bot == null || !bot.isConnected()) return;
        bot.sendLifeTokenLink(player.getUniqueId(), characterName, link);
    }

    @Override
    public void onClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();
        if (plugin.getChatInputManager().hasPendingInput(player.getUniqueId())) return;
        if (plugin.getLifeManager().isProcessingDeath(player.getUniqueId())) return;
        if (!plugin.getLifeManager().hasPendingDeathScreen(player.getUniqueId())) return;

        CharacterData data = plugin.getCharacterManager().getCharacter(player.getUniqueId());
        if (data == null) {
            plugin.getLifeManager().consumePendingDeathScreen(player.getUniqueId());
            return;
        }

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (plugin.getLifeManager().hasPendingDeathScreen(player.getUniqueId())
                    && !plugin.getLifeManager().isProcessingDeath(player.getUniqueId())) {
                plugin.getGUIManager().openGUI(
                        new DeathScreenGUI(plugin, characterName, causeOfDeath), player);
            }
        }, 1L);
    }
}
package com.dirt.config;

import com.dirt.DirtEconomy;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class LifeConfig {
    private final DirtEconomy plugin;

    private boolean lifeSystemEnabled;
    private boolean lifeTokensEnabled;

    private String continueButtonTitle;
    private String reviveButtonTitle;
    private String getTokensButtonTitle;

    private boolean getTokensButtonEnabled;
    private String getTokensAction;
    private String getTokensLink;
    private String getTokensInstructions;

    public LifeConfig(DirtEconomy plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        File file = new File(plugin.getDataFolder(), "life.yml");
        if (!file.exists()) {
            plugin.saveResource("life.yml", false);
        }
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);

        lifeSystemEnabled = cfg.getBoolean("life-system", false);
        lifeTokensEnabled = cfg.getBoolean("life-tokens", false);

        continueButtonTitle = cfg.getString("death-screen.continue-button-title", "§c§l☠ Continue To New Life");
        reviveButtonTitle = cfg.getString("death-screen.revive-button-title", "§a§l✦ Use Life Token");
        getTokensButtonTitle = cfg.getString("death-screen.get-tokens-button-title", "§e§l★ Get Life Tokens");

        getTokensButtonEnabled = cfg.getBoolean("death-screen.get-tokens-button.enabled", false);
        getTokensAction = cfg.getString("death-screen.get-tokens-button.action", "link");
        getTokensLink = cfg.getString("death-screen.get-tokens-button.link", "https://example.com/store");
        getTokensInstructions = cfg.getString("death-screen.get-tokens-button.instructions",
                "§ePurchase life tokens at our web store!");
    }

    public boolean isLifeSystemEnabled() { return lifeSystemEnabled; }
    public boolean isLifeTokensEnabled() { return lifeTokensEnabled; }
    public String getContinueButtonTitle() { return continueButtonTitle; }
    public String getReviveButtonTitle() { return reviveButtonTitle; }
    public String getGetTokensButtonTitle() { return getTokensButtonTitle; }
    public boolean isGetTokensButtonEnabled() { return getTokensButtonEnabled; }
    public String getGetTokensAction() { return getTokensAction; }
    public String getGetTokensLink() { return getTokensLink; }
    public String getGetTokensInstructions() { return getTokensInstructions; }
}
package com.dirt;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class Settings {

    private final DirtEconomy plugin;

    // ──── Core (config.yml) ────
    private double nameChangeCost;
    private long deathRespawnDelayTicks;
    private long joinDelayTicks;
    private double dailyLoginReward;
    private boolean familyEnabled;
    private double marriageCost;
    private double divorcePayout;
    private int marriageEligibilityDays;
    private boolean dirtHandsEnabled;

    // ──── Contracts ────
    private boolean contractsEnabled;
    private double paperCopyCost;

    // 
    private boolean discordEnabled;
    private String discordBotToken;
    private String discordGuildId;
    private String discordLinkChannelId;
    private boolean discordRolePermissions;
    private boolean discordDirtcName;

    // ──── Chat Bridge ────
    private boolean discordChatBridgeEnabled;
    private String discordChatBridgeChannelId;

    // ──── Activity Feed ────
    private boolean discordActivityFeedEnabled;
    private String discordActivityFeedChannelId;

    // ──── Auto-Link ────
    private boolean discordAutoLinkEnabled;

    // ──── Nations (nations.yml) ────
    private boolean nationsEnabled;
    private int maxRegionChunks;
    private long taxCollectionIntervalMinutes;
    private double taxBracketLowerMax;
    private double taxBracketMiddleMax;
    private double warCost;
    private int warMaxAttackingRegions;
    private boolean propertyPermissionsEnabled;
    private boolean mintersEnabled;
    private int minterBarProcessingSeconds;
    private int minterNuggetProcessingSeconds;
    private double minterBarVaultAmount;
    private double minterNuggetVaultAmount;
    private boolean enforcersEnabled;
    private boolean lawsEnabled;
    private boolean crimeEnabled;
    private long borderTickRate;
    private int borderViewRadius;
    private boolean mobPermissionsEnabled;
    private boolean playerNationCreation;

    // ──── Business (business.yml) ────
    private boolean businessEnabled;
    private long payrollIntervalMinutes;

    // ──── Shops (shops.yml) ────
    private boolean shopsEnabled;
    private boolean onlineShopsEnabled;
    private int saleHistoryDays;
    private int monthlyProfitDays;

    // ──── GPS (gps.yml) ────
    private boolean gpsEnabled;
    private int gpsParticleIntervalTicks;
    private int gpsParticleDistanceBlocks;
    private double gpsArrivalRadius;
    private int gpsMaxPersonalLocations;
    private int gpsMaxGlobalLocations;
    private boolean gpsBusinessLocationsEnabled;

    // ──── Phone (phone.yml) ────
    private boolean phoneEnabled;
    private int phoneMaxContacts;
    private int phoneMaxConversations;
    private int phoneMaxMessageHistory;
    private boolean phoneTexterEnabled;
    private boolean phoneContactsEnabled;
    private boolean phoneMapsEnabled;
    private boolean phoneShopoholicEnabled;
    private boolean phoneLiferizeEnabled;
    private boolean phoneAppStoreEnabled;

    public Settings(DirtEconomy plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        // ── Core (config.yml) ──
        plugin.reloadConfig();
        FileConfiguration cfg = plugin.getConfig();

        nameChangeCost = cfg.getDouble("character.name-change-cost", 100_000.0);
        deathRespawnDelayTicks = cfg.getLong("character.death-respawn-delay-ticks", 10L);
        joinDelayTicks = cfg.getLong("character.join-delay-ticks", 20L);
        dailyLoginReward = cfg.getDouble("character.daily-login-reward", 500.0);
        familyEnabled = cfg.getBoolean("family.enabled", true);
        marriageCost = cfg.getDouble("family.marriage-cost", 1_000.0);
        divorcePayout = cfg.getDouble("family.divorce-payout", 1_000.0);
        marriageEligibilityDays = cfg.getInt("family.marriage-eligibility-days", 30);
        dirtHandsEnabled = cfg.getBoolean("dirthands.enabled", true);

        // ── Contracts ──
        contractsEnabled = cfg.getBoolean("contracts.enabled", true);
        paperCopyCost = cfg.getDouble("contracts.paper-copy-cost", 500.0);

        // ── Discord (discord.yml) ──
        FileConfiguration discord = loadCustomConfig("discord.yml");
        discordEnabled = discord.getBoolean("enabled", false);
        discordBotToken = discord.getString("bot-token", "");
        discordGuildId = discord.getString("guild-id", "");
        discordLinkChannelId = discord.getString("link-channel-id", "");
        discordRolePermissions = discord.getBoolean("role-discord-permissions", false);
        discordDirtcName = discord.getBoolean("discord-dirtc-name", false);

        // ── Chat Bridge ──
        discordChatBridgeEnabled = discord.getBoolean("chat-bridge.enabled", false);
        discordChatBridgeChannelId = discord.getString("chat-bridge.channel-id", "");

        // ── Activity Feed ──
        discordActivityFeedEnabled = discord.getBoolean("activity-feed.enabled", false);
        discordActivityFeedChannelId = discord.getString("activity-feed.channel-id", "");

        // ── Auto-Link ──
        discordAutoLinkEnabled = discord.getBoolean("auto-link", true);

        // ── Nations (nations.yml) ──
        FileConfiguration nations = loadCustomConfig("nations.yml");
        nationsEnabled = nations.getBoolean("enabled", false);
        maxRegionChunks = nations.getInt("regions.max-chunks", 500);
        taxCollectionIntervalMinutes = nations.getLong("tax.collection-interval-minutes", 60);
        taxBracketLowerMax = nations.getDouble("tax.brackets.lower-class-max", 100_000);
        taxBracketMiddleMax = nations.getDouble("tax.brackets.middle-class-max", 200_000);
        warCost = nations.getDouble("war.cost", 10_000.0);
        warMaxAttackingRegions = nations.getInt("war.max-attacking-regions", 5);
        propertyPermissionsEnabled = nations.getBoolean("property.custom-permissions", true);
        mintersEnabled = nations.getBoolean("minters.enabled", true);
        minterBarProcessingSeconds = nations.getInt("minters.bar-processing-seconds", 60);
        minterNuggetProcessingSeconds = nations.getInt("minters.nugget-processing-seconds", 15);
        minterBarVaultAmount = nations.getDouble("minters.bar-vault-amount", 100.0);
        minterNuggetVaultAmount = nations.getDouble("minters.nugget-vault-amount", 10.0);
        enforcersEnabled = nations.getBoolean("enforcers.enabled", true);
        lawsEnabled = nations.getBoolean("laws.enabled", true);
        crimeEnabled = nations.getBoolean("crime.enabled", true);
        borderTickRate = nations.getLong("border-visualizer.tick-rate", 40L);
        borderViewRadius = nations.getInt("border-visualizer.view-radius-chunks", 5);
        mobPermissionsEnabled = nations.getBoolean("mob-permissions.enabled", false);
        playerNationCreation = nations.getBoolean("player-creation", false);

        // ── Business (business.yml) ──
        FileConfiguration business = loadCustomConfig("business.yml");
        businessEnabled = business.getBoolean("enabled", false);
        payrollIntervalMinutes = business.getLong("payroll-interval-minutes", 10080);

        // ── Shops (shops.yml) ──
        FileConfiguration shops = loadCustomConfig("shops.yml");
        shopsEnabled = shops.getBoolean("enabled", true);
        onlineShopsEnabled = shops.getBoolean("online-shops", true);
        saleHistoryDays = shops.getInt("sale-history-days", 60);
        monthlyProfitDays = shops.getInt("monthly-profit-days", 30);

        // ── GPS (gps.yml) ──
        FileConfiguration gps = loadCustomConfig("gps.yml");
        gpsEnabled = gps.getBoolean("enabled", true);
        gpsParticleIntervalTicks = gps.getInt("particle-interval-ticks", 10);
        gpsParticleDistanceBlocks = gps.getInt("particle-distance-blocks", 30);
        gpsArrivalRadius = gps.getDouble("arrival-radius", 3.0);
        gpsMaxPersonalLocations = gps.getInt("max-personal-locations", 20);
        gpsMaxGlobalLocations = gps.getInt("max-global-locations", 100);
        gpsBusinessLocationsEnabled = gps.getBoolean("business-locations", true);

        // ── Phone (phone.yml) ──
        FileConfiguration phone = loadCustomConfig("phone.yml");
        phoneEnabled = phone.getBoolean("enabled", true);
        phoneMaxContacts = phone.getInt("max-contacts", 50);
        phoneMaxConversations = phone.getInt("max-conversations", 30);
        phoneMaxMessageHistory = phone.getInt("max-message-history", 200);
        phoneTexterEnabled = phone.getBoolean("apps.texter", true);
        phoneContactsEnabled = phone.getBoolean("apps.contacts", true);
        phoneMapsEnabled = phone.getBoolean("apps.maps", true);
        phoneShopoholicEnabled = phone.getBoolean("apps.shopoholic", true);
        phoneLiferizeEnabled = phone.getBoolean("apps.liferize", true);
        phoneAppStoreEnabled = phone.getBoolean("apps.app-store", true);
    }

    private FileConfiguration loadCustomConfig(String filename) {
        File file = new File(plugin.getDataFolder(), filename);
        if (!file.exists()) {
            plugin.saveResource(filename, false);
        }
        return YamlConfiguration.loadConfiguration(file);
    }

    // ──── Core getters ────
    public double getNameChangeCost() { return nameChangeCost; }
    public long getDeathRespawnDelayTicks() { return deathRespawnDelayTicks; }
    public long getJoinDelayTicks() { return joinDelayTicks; }
    public double getDailyLoginReward() { return dailyLoginReward; }
    public boolean isFamilyEnabled() { return familyEnabled; }
    public double getMarriageCost() { return marriageCost; }
    public double getDivorcePayout() { return divorcePayout; }
    public int getMarriageEligibilityDays() { return marriageEligibilityDays; }
    public long getMarriageEligibilityMs() { return (long) marriageEligibilityDays * 24L * 60L * 60L * 1000L; }
    public boolean isDirtHandsEnabled() { return dirtHandsEnabled; }

    // ──── Contracts getters ────
    public boolean isContractsEnabled() { return contractsEnabled; }
    public double getPaperCopyCost() { return paperCopyCost; }

    // ──── Discord getters ────
    public boolean isDiscordEnabled() { return discordEnabled; }
    public String getDiscordBotToken() { return discordBotToken; }
    public String getDiscordGuildId() { return discordGuildId; }
    public String getDiscordLinkChannelId() { return discordLinkChannelId; }
    public boolean isDiscordRolePermissions() { return discordRolePermissions; }
    public boolean isDiscordDirtcName() { return discordDirtcName; }

    // ──── Chat Bridge getters ────
    public boolean isDiscordChatBridgeEnabled() { return discordChatBridgeEnabled; }
    public String getDiscordChatBridgeChannelId() { return discordChatBridgeChannelId; }

    // ──── Activity Feed getters ────
    public boolean isDiscordActivityFeedEnabled() { return discordActivityFeedEnabled; }
    public String getDiscordActivityFeedChannelId() { return discordActivityFeedChannelId; }

    // ──── Auto-Link getters ────
    public boolean isDiscordAutoLinkEnabled() { return discordAutoLinkEnabled; }

    // ──── Nations getters ────
    public boolean isNationsEnabled() { return nationsEnabled; }
    public boolean isCustomGovernmentEnabled() { return plugin.getRolesConfig().isCustomGovernmentEnabled(); }
    public int getMaxCustomRoles() { return plugin.getRolesConfig().getMaxCustomRoles(); }
    public int getMaxRegionChunks() { return maxRegionChunks; }
    public long getTaxCollectionIntervalMinutes() { return taxCollectionIntervalMinutes; }
    public long getTaxCollectionIntervalTicks() { return taxCollectionIntervalMinutes * 60 * 20L; }
    public double getTaxBracketLowerMax() { return taxBracketLowerMax; }
    public double getTaxBracketMiddleMax() { return taxBracketMiddleMax; }
    public double getWarCost() { return warCost; }
    public int getMaxAttackingRegions() { return warMaxAttackingRegions; }
    public boolean isPropertyPermissionsEnabled() { return propertyPermissionsEnabled; }
    public boolean isMintersEnabled() { return mintersEnabled; }
    public int getMinterBarProcessingSeconds() { return minterBarProcessingSeconds; }
    public int getMinterNuggetProcessingSeconds() { return minterNuggetProcessingSeconds; }
    public double getMinterBarVaultAmount() { return minterBarVaultAmount; }
    public double getMinterNuggetVaultAmount() { return minterNuggetVaultAmount; }
    public boolean isEnforcersEnabled() { return enforcersEnabled; }
    public boolean isLawsEnabled() { return lawsEnabled; }
    public boolean isCrimeEnabled() { return crimeEnabled; }
    public long getBorderTickRate() { return borderTickRate; }
    public int getBorderViewRadiusChunks() { return borderViewRadius; }
    public boolean isMobPermissionsEnabled() { return mobPermissionsEnabled; }
    public boolean isPlayerNationCreation() { return playerNationCreation; }

    // ──── Business getters ────
    public boolean isBusinessEnabled() { return businessEnabled; }
    public long getPayrollIntervalMinutes() { return payrollIntervalMinutes; }
    public long getPayrollIntervalTicks() { return payrollIntervalMinutes * 60 * 20L; }

    // ──── Shops getters ────
    public boolean isShopsEnabled() { return shopsEnabled; }
    public boolean isOnlineShopsEnabled() { return onlineShopsEnabled; }
    public int getSaleHistoryDays() { return saleHistoryDays; }
    public long getSaleHistoryMs() { return (long) saleHistoryDays * 24L * 60L * 60L * 1000L; }
    public int getMonthlyProfitDays() { return monthlyProfitDays; }
    public long getMonthlyProfitMs() { return (long) monthlyProfitDays * 24L * 60L * 60L * 1000L; }

    // ──── GPS getters ────
    public boolean isGpsEnabled() { return gpsEnabled; }
    public int getGpsParticleIntervalTicks() { return gpsParticleIntervalTicks; }
    public int getGpsParticleDistanceBlocks() { return gpsParticleDistanceBlocks; }
    public double getGpsArrivalRadius() { return gpsArrivalRadius; }
    public int getGpsMaxPersonalLocations() { return gpsMaxPersonalLocations; }
    public int getGpsMaxGlobalLocations() { return gpsMaxGlobalLocations; }
    public boolean isGpsBusinessLocationsEnabled() { return gpsBusinessLocationsEnabled; }

    // ──── Phone getters ────
    public boolean isPhoneEnabled() { return phoneEnabled; }
    public int getPhoneMaxContacts() { return phoneMaxContacts; }
    public int getPhoneMaxConversations() { return phoneMaxConversations; }
    public int getPhoneMaxMessageHistory() { return phoneMaxMessageHistory; }
    public boolean isPhoneTexterEnabled() { return phoneTexterEnabled; }
    public boolean isPhoneContactsEnabled() { return phoneContactsEnabled; }
    public boolean isPhoneMapsEnabled() { return phoneMapsEnabled; }
    public boolean isPhoneShopoholicEnabled() { return phoneShopoholicEnabled; }
    public boolean isPhoneLiferizeEnabled() { return phoneLiferizeEnabled; }
    public boolean isPhoneAppStoreEnabled() { return phoneAppStoreEnabled; }
}
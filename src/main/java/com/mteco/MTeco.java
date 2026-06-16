package com.mteco;

import com.mteco.commands.MTCommand;
import com.mteco.commands.MTCCommand;
import com.mteco.commands.MTIDCommand;
import com.mteco.commands.MTNCommand;
import com.mteco.commands.MTRCommand;
import com.mteco.commands.MTBCommand;
import com.mteco.commands.MTSCommand;
import com.mteco.commands.HomeCommand;
import com.mteco.config.RolesConfig;
import com.mteco.data.CharacterData;
import com.mteco.data.FamilyData;
import com.mteco.data.NationData;
import com.mteco.data.PropertyData;
import com.mteco.data.BusinessData;
import com.mteco.inventory.gui.GUIListener;
import com.mteco.inventory.gui.GUIManager;
import com.mteco.inventory.impl.CharacterCreationGUI;
import com.mteco.listeners.ChestProtectionListener;
import com.mteco.listeners.CrimeListener;
import com.mteco.listeners.IDInteractListener;
import com.mteco.listeners.JoinListener;
import com.mteco.listeners.LawBookListener;
import com.mteco.listeners.MThandsListener;
import com.mteco.listeners.MTshopsListener;
import com.mteco.listeners.MinterListener;
import com.mteco.listeners.NationProtectionListener;
import com.mteco.listeners.ProximityChatListener;
import com.mteco.listeners.RegionEnterListener;
import com.mteco.managers.BusinessManager;
import com.mteco.managers.CharacterManager;
import com.mteco.managers.ChestProtectionManager;
import com.mteco.managers.FamilyManager;
import com.mteco.managers.IDManager;
import com.mteco.managers.LawCrimeManager;
import com.mteco.managers.MinterManager;
import com.mteco.managers.NationManager;
import com.mteco.managers.ProximityManager;
import com.mteco.managers.ShopManager;
import com.mteco.managers.WarManager;
import com.mteco.managers.CouncilVoteManager;
import com.mteco.managers.ContractManager;
import com.mteco.util.BlueMapHook;
import com.mteco.util.BorderVisualizerManager;
import com.mteco.util.ChatInputManager;
import com.mteco.util.ChunkSelectionManager;
import com.mteco.util.ConfigValidator;
import com.mteco.util.CurrencyUtil;
import com.mteco.util.DataMigrator;
import com.mteco.util.DiscordBotManager;
import com.mteco.util.LocationSelectionManager;
import com.mteco.util.MTNationsHook;
import com.mteco.util.StockroomSelectionManager;
import lombok.Getter;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MTeco extends JavaPlugin {
    @Getter private Economy economy;
    private GUIManager guiManager;

    public GUIManager getGUIManager() {
        return guiManager;
    }

    @Getter private CharacterManager characterManager;
    @Getter private FamilyManager familyManager;
    @Getter private ChestProtectionManager chestProtectionManager;
    @Getter private ChatInputManager chatInputManager;
    @Getter private LocationSelectionManager locationSelectionManager;
    @Getter private IDManager idManager;
    @Getter private MTNationsHook nationsHook;
    @Getter private NationManager nationManager;
    @Getter private WarManager warManager;
    @Getter private ChunkSelectionManager chunkSelectionManager;
    @Getter private BlueMapHook blueMapHook;
    @Getter private BorderVisualizerManager borderVisualizerManager;
    @Getter private BusinessManager businessManager;
    @Getter private ShopManager shopManager;
    @Getter private StockroomSelectionManager stockroomSelectionManager;
    @Getter private MTshopsListener shopsListener;
    @Getter private MinterManager minterManager;
    @Getter private LawCrimeManager lawCrimeManager;
    @Getter private Settings settings;
    @Getter private RolesConfig rolesConfig;
    @Getter private DiscordBotManager discordBotManager;
    @Getter private CouncilVoteManager councilVoteManager;
    @Getter private ContractManager contractManager;
    @Getter private ProximityManager proximityManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        ConfigValidator.validateAll(this);
        DataMigrator.migrate(this);
        this.settings = new Settings(this);
        this.rolesConfig = new RolesConfig(this);

        if (!setupEconomy()) {
            getLogger().severe("Vault or a compatible economy plugin was not found! Disabling MTeco.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        CurrencyUtil.init();

        this.idManager = new IDManager(this);
        this.characterManager = new CharacterManager(this, idManager);
        this.familyManager = new FamilyManager(this);
        this.chestProtectionManager = new ChestProtectionManager(this);
        this.guiManager = new GUIManager(this);
        this.chatInputManager = new ChatInputManager(this);
        this.locationSelectionManager = new LocationSelectionManager();
        this.nationsHook = new MTNationsHook();

        if (isMTNationsEnabled()) {
            this.nationManager = new NationManager(this);
            this.warManager = new WarManager(this);
            this.chunkSelectionManager = new ChunkSelectionManager(this);
            this.borderVisualizerManager = new BorderVisualizerManager(this);
            this.councilVoteManager = new CouncilVoteManager(this);
            getServer().getPluginManager().registerEvents(new NationProtectionListener(this), this);
            getServer().getPluginManager().registerEvents(chunkSelectionManager, this);
            getServer().getPluginManager().registerEvents(new RegionEnterListener(this), this);

            if (settings.isMintersEnabled()) {
                this.minterManager = new MinterManager(this);
                getServer().getPluginManager().registerEvents(new MinterListener(this), this);
            }
            if (settings.isLawsEnabled() || settings.isCrimeEnabled()) {
                this.lawCrimeManager = new LawCrimeManager(this);
            }
            if (settings.isLawsEnabled()) {
                getServer().getPluginManager().registerEvents(new LawBookListener(this), this);
            }
            if (settings.isCrimeEnabled()) {
                getServer().getPluginManager().registerEvents(new CrimeListener(this), this);
            }

            long intervalTicks = settings.getTaxCollectionIntervalTicks();
            getServer().getScheduler().runTaskTimer(this, this::collectTaxes, intervalTicks, intervalTicks);
        }

        if (isMTBusinessEnabled()) {
            this.businessManager = new BusinessManager(this);
            long payrollTicks = settings.getPayrollIntervalTicks();
            getServer().getScheduler().runTaskTimer(this, this::runPayroll, payrollTicks, payrollTicks);
        }

        if (isContractsEnabled()) {
            this.contractManager = new ContractManager(this);
            getServer().getScheduler().runTaskTimer(this, () -> contractManager.checkExpiredContracts(), 24000L, 24000L);
        }

        if (isMTShopsEnabled()) {
            this.shopManager = new ShopManager(this);
            this.shopsListener = new MTshopsListener(this);
            getServer().getPluginManager().registerEvents(shopsListener, this);
            if (isMTBusinessEnabled()) {
                this.stockroomSelectionManager = new StockroomSelectionManager(this);
                getServer().getPluginManager().registerEvents(stockroomSelectionManager, this);
            }
        }

        if (getServer().getPluginManager().getPlugin("BlueMap") != null) {
            this.blueMapHook = new BlueMapHook(this);
        }

        getServer().getPluginManager().registerEvents(new GUIListener(guiManager), this);
        getServer().getPluginManager().registerEvents(new JoinListener(this), this);
        getServer().getPluginManager().registerEvents(new ChestProtectionListener(this), this);
        getServer().getPluginManager().registerEvents(chatInputManager, this);
        getServer().getPluginManager().registerEvents(locationSelectionManager, this);
        getServer().getPluginManager().registerEvents(new IDInteractListener(this), this);

        if (settings.isMthandsEnabled()) {
            getServer().getPluginManager().registerEvents(new MThandsListener(this), this);
        }

        if (settings.isDiscordEnabled()) {
            try {
                this.discordBotManager = new DiscordBotManager(this);
            } catch (NoClassDefFoundError e) {
                getLogger().warning("[Discord] JDA library not found. Discord integration disabled.");
            }
        }

        if (settings.isProximityVoiceEnabled() && discordBotManager != null) {
            this.proximityManager = new ProximityManager(this);
            proximityManager.start();
        }

        if (settings.isProximityTextChatEnabled()) {
            getServer().getPluginManager().registerEvents(new ProximityChatListener(this), this);
        }

        if (settings.isDiscordChatBridgeEnabled() && discordBotManager != null) {
            getServer().getPluginManager().registerEvents(new com.mteco.listeners.ChatBridgeListener(this), this);
        }

        MTCommand mtCommand = new MTCommand(this);
        getCommand("mteco").setExecutor(mtCommand);
        getCommand("mteco").setTabCompleter(mtCommand);

        MTCCommand mtcCommand = new MTCCommand(this);
        getCommand("mtc").setExecutor(mtcCommand);
        getCommand("mtc").setTabCompleter(mtcCommand);

        MTIDCommand mtidCommand = new MTIDCommand(this);
        getCommand("mtid").setExecutor(mtidCommand);
        getCommand("mtid").setTabCompleter(mtidCommand);

        MTNCommand mtnCommand = new MTNCommand(this);
        getCommand("mtn").setExecutor(mtnCommand);
        getCommand("mtn").setTabCompleter(mtnCommand);

        MTRCommand mtrCommand = new MTRCommand(this);
        getCommand("mtr").setExecutor(mtrCommand);
        getCommand("mtr").setTabCompleter(mtrCommand);

        MTBCommand mtbCommand = new MTBCommand(this);
        getCommand("mtb").setExecutor(mtbCommand);
        getCommand("mtb").setTabCompleter(mtbCommand);

        MTSCommand mtsCommand = new MTSCommand(this);
        getCommand("mts").setExecutor(mtsCommand);
        getCommand("mts").setTabCompleter(mtsCommand);

        com.mteco.commands.LawsViewCommand lawsCmd = new com.mteco.commands.LawsViewCommand(this);
        getCommand("laws").setExecutor(lawsCmd);
        getCommand("laws").setTabCompleter(lawsCmd);

        HomeCommand homeCommand = new HomeCommand(this);
        getCommand("sethome").setExecutor(homeCommand);
        getCommand("sethome").setTabCompleter(homeCommand);
        getCommand("home").setExecutor(homeCommand);
        getCommand("home").setTabCompleter(homeCommand);

        getLogger().info("MTeco enabled successfully.");
    }

    @Override
    public void onDisable() {
        if (proximityManager != null) proximityManager.stop();
        if (discordBotManager != null) discordBotManager.shutdown();
        getLogger().info("MTeco disabled.");
    }

    public void reloadData() {
        ConfigValidator.validateAll(this);
        if (characterManager != null) characterManager.reloadCache();
        if (familyManager != null) familyManager.reloadCache();
        if (nationManager != null) nationManager.reloadCache();
        if (warManager != null) warManager.reloadCache();
        if (businessManager != null) businessManager.reloadCache();
        if (shopManager != null) shopManager.reloadCache();
    }

    public boolean isFamilyEnabled() {
        return settings.isFamilyEnabled();
    }

    public boolean isMTNationsEnabled() {
        return settings.isNationsEnabled();
    }

    public boolean isMTBusinessEnabled() {
        return isMTNationsEnabled() && settings.isBusinessEnabled();
    }

    public boolean isMTShopsEnabled() {
        return settings.isShopsEnabled();
    }

    public boolean isOnlineShopsEnabled() {
        return isMTShopsEnabled() && settings.isOnlineShopsEnabled();
    }

    public boolean isContractsEnabled() {
        return settings.isContractsEnabled();
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) return false;
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;
        economy = rsp.getProvider();
        return economy != null;
    }

    public void executeDeath(Player player, Location chestLocation) {
        CharacterData character = characterManager.getCharacter(player.getUniqueId());
        if (character == null) return;

        UUID inheritorUUID = character.getInheritorUuid();

        double balance = economy.getBalance(player);
        if (balance > 0) {
            economy.withdrawPlayer(player, balance);
            if (inheritorUUID != null) {
                economy.depositPlayer(Bukkit.getOfflinePlayer(inheritorUUID), balance);
                Player inheritor = Bukkit.getPlayer(inheritorUUID);
                if (inheritor != null) {
                    inheritor.sendMessage("§e" + character.getFirstName() + " " + character.getLastName()
                            + " has died and left you §a" + CurrencyUtil.symbol() + String.format("%.0f", balance)
                            + "§e and an inheritance chest.");
                }
            }
        }

        if (character.getFamilyId() != null) {
            FamilyData family = familyManager.loadFamily(character.getFamilyId());
            if (family != null) removeFromFamily(player.getUniqueId(), family);
        }

        if (inheritorUUID != null) {
            spawnInheritanceChest(player, chestLocation, inheritorUUID);
        } else {
            player.getInventory().clear();
        }

        characterManager.deleteCharacter(player.getUniqueId());

        if (isMTNationsEnabled()) {
            UUID resolvedInheritor = resolveInheritor(player.getUniqueId());
            if (resolvedInheritor != null) {
                for (PropertyData prop : nationManager.getPropertiesByOwner(player.getUniqueId())) {
                    prop.setOwnerUUID(resolvedInheritor);
                    prop.setForSale(false);
                    nationManager.saveProperty(prop);
                }
            }
        }

        characterManager.addForcedCreation(player.getUniqueId());
        characterManager.initPendingCreation(player.getUniqueId());

        player.sendMessage("§cYour character has died. Please create a new character.");

        getServer().getScheduler().runTaskLater(this, () ->
                guiManager.openGUI(new CharacterCreationGUI(this), player), settings.getDeathRespawnDelayTicks());
    }

    private UUID resolveInheritor(UUID deceasedUUID) {
        CharacterData deceased = characterManager.getCharacter(deceasedUUID);
        if (deceased == null) return null;
        UUID next = deceased.getInheritorUuid();
        while (next != null) {
            if (characterManager.hasCharacter(next)) return next;
            CharacterData nextData = characterManager.getCharacter(next);
            next = nextData != null ? nextData.getInheritorUuid() : null;
        }
        return null;
    }

    private void removeFromFamily(UUID playerUUID, FamilyData family) {
        if (playerUUID.equals(family.getSpouse1())) {
            family.setSpouse1(null);
        } else if (playerUUID.equals(family.getSpouse2())) {
            family.setSpouse2(null);
        } else {
            family.getChildren().remove(playerUUID);
        }

        boolean noSpousesLeft = family.getSpouse1() == null && family.getSpouse2() == null;
        if (noSpousesLeft) {
            List<UUID> remaining = new ArrayList<>(family.getChildren());
            for (UUID memberUUID : remaining) {
                CharacterData memberData = characterManager.getCharacter(memberUUID);
                if (memberData != null) {
                    memberData.setFamilyId(null);
                    characterManager.saveCharacter(memberData);
                }
            }
            familyManager.deleteFamily(family.getFamilyId());
        } else {
            UUID survivingSpouse = family.getSpouse1() != null ? family.getSpouse1() : family.getSpouse2();
            Player spousePlayer = Bukkit.getPlayer(survivingSpouse);
            if (spousePlayer != null) {
                spousePlayer.sendMessage("§cYour spouse has passed away.");
            }
            familyManager.saveFamily(family);
        }
    }

    private void runPayroll() {
        for (BusinessData biz : businessManager.getAllBusinesses()) {
            for (UUID empUUID : new ArrayList<>(biz.getEmployeeUUIDs())) {
                double rate = biz.getEmployeeRates().getOrDefault(empUUID, biz.getPayrollRate());
                if (rate <= 0) continue;
                double balance = businessManager.getTreasuryBalance(biz.getName());
                if (balance < rate) continue;
                businessManager.withdrawFromTreasury(biz.getName(), rate);
                economy.depositPlayer(getServer().getOfflinePlayer(empUUID), rate);
                Player p = getServer().getPlayer(empUUID);
                if (p != null) p.sendMessage("§aYou received your payroll of §e" + CurrencyUtil.symbol() + String.format("%.2f", rate) + "§a from §6" + biz.getName() + "§a.");
            }
        }
    }

    private void collectTaxes() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            NationData nation = nationManager.getNationByMember(player.getUniqueId());
            if (nation == null) continue;
            double balance = economy.getBalance(player);
            double rate;
            if (balance <= settings.getTaxBracketLowerMax()) {
                rate = nation.getLowerClassTaxRate();
            } else if (balance <= settings.getTaxBracketMiddleMax()) {
                rate = nation.getMiddleClassTaxRate();
            } else {
                rate = nation.getUpperClassTaxRate();
            }
            if (rate <= 0) continue;
            double taxAmount = balance * (rate / 100.0);
            economy.withdrawPlayer(player, taxAmount);
            nationManager.depositToTreasury(nation.getName(), taxAmount);
            CharacterData taxChar = characterManager.getCharacter(player.getUniqueId());
            String charName = taxChar != null ? taxChar.getFirstName() + " " + taxChar.getLastName() : player.getName();
            nationManager.logTransaction(nation.getNationId(), "TAX_RECEIVED", taxAmount, "Tax collected from " + charName);
            player.sendMessage("§e" + CurrencyUtil.symbol() + String.format("%.0f", taxAmount)
                    + " has been collected as national tax and deposited into the " + nation.getName() + " treasury.");
        }
    }

    private void spawnInheritanceChest(Player player, Location loc, UUID inheritorUUID) {
        Block block = loc.getBlock();
        block.setType(Material.CHEST);

        Block adjacent = block.getRelative(BlockFace.EAST);
        boolean placedDouble = adjacent.getType() == Material.AIR || adjacent.getType() == Material.CAVE_AIR;
        if (placedDouble) {
            adjacent.setType(Material.CHEST);
        }

        BlockState state = block.getState();
        if (state instanceof Chest chest) {
            Inventory chestInv = chest.getInventory();
            for (ItemStack item : player.getInventory().getContents()) {
                if (item != null && item.getType() != Material.AIR) chestInv.addItem(item);
            }
            for (ItemStack item : player.getInventory().getArmorContents()) {
                if (item != null && item.getType() != Material.AIR) chestInv.addItem(item);
            }
            ItemStack offhand = player.getInventory().getItemInOffHand();
            if (offhand.getType() != Material.AIR) chestInv.addItem(offhand);
        }

        player.getInventory().clear();

        chestProtectionManager.addProtectedChest(loc, inheritorUUID);
        if (placedDouble) {
            chestProtectionManager.addProtectedChest(adjacent.getLocation(), inheritorUUID);
        }

        player.sendMessage("§eAn inheritance chest has been placed at your chosen location for your inheritor.");
    }
}
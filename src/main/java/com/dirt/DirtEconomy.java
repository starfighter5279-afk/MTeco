package com.dirt;

import com.dirt.commands.DirtCommand;
import com.dirt.commands.DirtCCommand;
import com.dirt.commands.DirtIDCommand;
import com.dirt.commands.DirtNCommand;
import com.dirt.commands.DirtRCommand;
import com.dirt.commands.DirtBCommand;
import com.dirt.commands.DirtSCommand;
import com.dirt.commands.HomeCommand;
import com.dirt.commands.GPSCommand;
import com.dirt.commands.GiveCommand;
import com.dirt.config.LifeConfig;
import com.dirt.config.RolesConfig;
import com.dirt.data.CharacterData;
import com.dirt.data.FamilyData;
import com.dirt.data.NationData;
import com.dirt.data.PropertyData;
import com.dirt.data.BusinessData;
import com.dirt.inventory.gui.GUIListener;
import com.dirt.inventory.gui.GUIManager;
import com.dirt.inventory.impl.CharacterCreationGUI;
import com.dirt.listeners.ChestProtectionListener;
import com.dirt.listeners.CrimeListener;
import com.dirt.listeners.DeathListener;
import com.dirt.listeners.FastTravelListener;
import com.dirt.listeners.IDInteractListener;
import com.dirt.listeners.JoinListener;
import com.dirt.listeners.LawBookListener;
import com.dirt.listeners.DirtHandsListener;
import com.dirt.listeners.DirtShopsListener;
import com.dirt.listeners.MinterListener;
import com.dirt.listeners.NationProtectionListener;
import com.dirt.listeners.RegionEnterListener;
import com.dirt.listeners.PhoneListener;
import com.dirt.managers.BusinessManager;
import com.dirt.managers.CharacterManager;
import com.dirt.managers.ChestProtectionManager;
import com.dirt.managers.FamilyManager;
import com.dirt.managers.IDManager;
import com.dirt.managers.LawCrimeManager;
import com.dirt.managers.MinterManager;
import com.dirt.managers.NationManager;
import com.dirt.managers.ShopManager;
import com.dirt.managers.WarManager;
import com.dirt.managers.CouncilVoteManager;
import com.dirt.managers.ContractManager;
import com.dirt.managers.FastTravelManager;
import com.dirt.managers.GPSManager;
import com.dirt.managers.LifeManager;
import com.dirt.managers.PhoneManager;
import com.dirt.util.BlueMapHook;
import com.dirt.util.BorderVisualizerManager;
import com.dirt.util.ChatInputManager;
import com.dirt.util.ChunkSelectionManager;
import com.dirt.util.ConfigValidator;
import com.dirt.util.CurrencyUtil;
import com.dirt.util.DataMigrator;
import com.dirt.util.DiscordBotManager;
import com.dirt.util.LocationSelectionManager;
import com.dirt.util.DirtNationsHook;
import com.dirt.util.StockroomSelectionManager;
import com.dirt.scheduler.PlatformScheduler;
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

public class DirtEconomy extends JavaPlugin {
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
    @Getter private DirtNationsHook nationsHook;
    @Getter private NationManager nationManager;
    @Getter private WarManager warManager;
    @Getter private ChunkSelectionManager chunkSelectionManager;
    @Getter private BlueMapHook blueMapHook;
    @Getter private BorderVisualizerManager borderVisualizerManager;
    @Getter private BusinessManager businessManager;
    @Getter private ShopManager shopManager;
    @Getter private StockroomSelectionManager stockroomSelectionManager;
    @Getter private DirtShopsListener shopsListener;
    @Getter private MinterManager minterManager;
    @Getter private LawCrimeManager lawCrimeManager;
    @Getter private Settings settings;
    @Getter private RolesConfig rolesConfig;
    @Getter private DiscordBotManager discordBotManager;
    @Getter private CouncilVoteManager councilVoteManager;
    @Getter private ContractManager contractManager;
    @Getter private GPSManager gpsManager;
    @Getter private FastTravelManager fastTravelManager;
    @Getter private PhoneManager phoneManager;
    @Getter private PhoneListener phoneListener;
    @Getter private LifeConfig lifeConfig;
    @Getter private LifeManager lifeManager;
    @Getter private PlatformScheduler platformScheduler;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        ConfigValidator.validateAll(this);
        DataMigrator.migrate(this);
        this.settings = new Settings(this);
        this.rolesConfig = new RolesConfig(this);
        this.platformScheduler = new PlatformScheduler(this);

        if (!setupEconomy()) {
            getLogger().severe("Vault or a compatible economy plugin was not found! Disabling DirtEconomy.");
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
        this.fastTravelManager = new FastTravelManager(this);
        this.locationSelectionManager = new LocationSelectionManager();
        this.nationsHook = new DirtNationsHook();

        this.lifeConfig = new LifeConfig(this);
        this.lifeManager = new LifeManager(this);

        if (isDirtNationsEnabled()) {
            this.nationManager = new NationManager(this);
            if (isDirtBusinessEnabled()) {
                this.businessManager = new BusinessManager(this);
            }
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
            platformScheduler.runGlobalTimer(this::collectTaxes, intervalTicks, intervalTicks);
        }

        if (isDirtBusinessEnabled()) {
            long payrollTicks = settings.getPayrollIntervalTicks();
            platformScheduler.runGlobalTimer(this::runPayroll, payrollTicks, payrollTicks);
        }

        if (isContractsEnabled()) {
            this.contractManager = new ContractManager(this);
            platformScheduler.runGlobalTimer(() -> contractManager.checkExpiredContracts(), 24000L, 24000L);
        }

        if (isDirtShopsEnabled()) {
            this.shopManager = new ShopManager(this);
            this.shopsListener = new DirtShopsListener(this);
            getServer().getPluginManager().registerEvents(shopsListener, this);
            if (isDirtBusinessEnabled()) {
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
        getServer().getPluginManager().registerEvents(new FastTravelListener(this), this);

        if (lifeConfig.isLifeSystemEnabled()) {
            getServer().getPluginManager().registerEvents(new DeathListener(this), this);
        }

        if (settings.isDirtHandsEnabled()) {
            getServer().getPluginManager().registerEvents(new DirtHandsListener(this), this);
        }

        if (settings.isDiscordEnabled()) {
            try {
                this.discordBotManager = new DiscordBotManager(this);
            } catch (NoClassDefFoundError e) {
                getLogger().warning("[Discord] JDA library not found. Discord integration disabled.");
            }
        }

        if (settings.isDiscordChatBridgeEnabled() && discordBotManager != null) {
            getServer().getPluginManager().registerEvents(new com.dirt.listeners.ChatBridgeListener(this), this);
        }

        DirtCommand dirtCommand = new DirtCommand(this);
        getCommand("d").setExecutor(dirtCommand);
        getCommand("d").setTabCompleter(dirtCommand);

        DirtCCommand dirtcCommand = new DirtCCommand(this);
        getCommand("dc").setExecutor(dirtcCommand);
        getCommand("dc").setTabCompleter(dirtcCommand);

        DirtIDCommand dirtidCommand = new DirtIDCommand(this);
        getCommand("did").setExecutor(dirtidCommand);
        getCommand("did").setTabCompleter(dirtidCommand);

        DirtNCommand dirtnCommand = new DirtNCommand(this);
        getCommand("dn").setExecutor(dirtnCommand);
        getCommand("dn").setTabCompleter(dirtnCommand);

        DirtRCommand dirtrCommand = new DirtRCommand(this);
        getCommand("dr").setExecutor(dirtrCommand);
        getCommand("dr").setTabCompleter(dirtrCommand);

        DirtBCommand dirtbCommand = new DirtBCommand(this);
        getCommand("db").setExecutor(dirtbCommand);
        getCommand("db").setTabCompleter(dirtbCommand);

        DirtSCommand dirtsCommand = new DirtSCommand(this);
        getCommand("ds").setExecutor(dirtsCommand);
        getCommand("ds").setTabCompleter(dirtsCommand);

        com.dirt.commands.LawsViewCommand lawsCmd = new com.dirt.commands.LawsViewCommand(this);
        getCommand("laws").setExecutor(lawsCmd);
        getCommand("laws").setTabCompleter(lawsCmd);

        HomeCommand homeCommand = new HomeCommand(this);
        getCommand("sethome").setExecutor(homeCommand);
        getCommand("sethome").setTabCompleter(homeCommand);
        getCommand("home").setExecutor(homeCommand);
        getCommand("home").setTabCompleter(homeCommand);

        if (settings.isGpsEnabled()) {
            this.gpsManager = new GPSManager(this);
            GPSCommand gpsCommand = new GPSCommand(this);
            getCommand("gps").setExecutor(gpsCommand);
            getCommand("gps").setTabCompleter(gpsCommand);
            getCommand("setgps").setExecutor(gpsCommand);
            getCommand("setgps").setTabCompleter(gpsCommand);
        }

        GiveCommand giveCommand = new GiveCommand(this);
        getCommand("give").setExecutor(giveCommand);
        getCommand("give").setTabCompleter(giveCommand);

        if (settings.isPhoneEnabled()) {
            this.phoneManager = new PhoneManager(this);
            this.phoneListener = new PhoneListener(this);
            getServer().getPluginManager().registerEvents(phoneListener, this);
        }

        getLogger().info("DirtEconomy enabled successfully.");
    }

    @Override
    public void onDisable() {
        if (platformScheduler != null) platformScheduler.cancelAllTasks();
        if (discordBotManager != null) discordBotManager.shutdown();
        getLogger().info("DirtEconomy disabled.");
    }

    public void reloadData() {
        ConfigValidator.validateAll(this);
        if (characterManager != null) characterManager.reloadCache();
        if (lifeConfig != null) lifeConfig.reload();
        if (familyManager != null) familyManager.reloadCache();
        if (nationManager != null) nationManager.reloadCache();
        if (warManager != null) warManager.reloadCache();
        if (businessManager != null) businessManager.reloadCache();
        if (shopManager != null) shopManager.reloadCache();
        if (gpsManager != null) gpsManager.reloadCache();
        if (phoneManager != null) phoneManager.reloadCache();
    }

    public boolean isFamilyEnabled() {
        return settings.isFamilyEnabled();
    }

    public boolean isDirtNationsEnabled() {
        return settings.isNationsEnabled();
    }

    public boolean isDirtBusinessEnabled() {
        return isDirtNationsEnabled() && settings.isBusinessEnabled();
    }

    public boolean isDirtShopsEnabled() {
        return settings.isShopsEnabled();
    }

    public boolean isOnlineShopsEnabled() {
        return isDirtShopsEnabled() && settings.isOnlineShopsEnabled();
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

        if (isDirtNationsEnabled()) {
            UUID inheritProp = resolveInheritor(player.getUniqueId());
            if (inheritProp != null) {
                for (PropertyData prop : nationManager.getPropertiesByOwner(player.getUniqueId())) {
                    prop.setOwnerUUID(inheritProp);
                    prop.setForSale(false);
                    nationManager.saveProperty(prop);
                }
            }
        }

        characterManager.addForcedCreation(player.getUniqueId());
        characterManager.initPendingCreation(player.getUniqueId());

        player.sendMessage("§cYour character has died. Please create a new character.");

        platformScheduler.runAtEntityLater(player, () ->
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
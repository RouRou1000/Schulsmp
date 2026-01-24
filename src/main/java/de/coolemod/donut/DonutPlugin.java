package de.coolemod.donut;

import de.coolemod.donut.managers.*;
import de.coolemod.donut.listeners.*;
import de.coolemod.donut.commands.*;
import de.coolemod.donut.guis.*;
import de.coolemod.donut.auctionhouse.*;
import de.coolemod.donut.orders.*;
import de.coolemod.donut.systems.*;
import org.bukkit.plugin.java.JavaPlugin;
import de.coolemod.donut.listeners.PlayerInteractListener;

/**
 * Hauptklasse des DonutCore-Plugins
 * Alles ist auf Deutsch kommentiert und konzipiert für Spigot/Paper 1.21.5
 */
public final class DonutPlugin extends JavaPlugin {

    private static DonutPlugin instance;

    // Manager
    private EconomyManager economy;
    private ShardsManager shards;
    private PlayerStatsManager stats;
    private de.coolemod.donut.managers.SidebarManager scoreboardManager;
    private SpawnerManager spawnerManager;
    private CrateManager crateManager;
    private WorthManager worthManager;
    private AuctionHouseManagerNew auctionManager;  // NEU: Neuer Manager
    private OrdersManager ordersManager;  // OLD
    
    // NEW: Komplett neues Auction House System
    private AuctionHouse auctionHouse;
    // NEW: Komplett neues Order System
    private OrderSystem orderSystem;
    // NEW: Combat, Home und TPA System
    private CombatManager combatManager;
    private HomeManager homeManager;
    private HomeGUI homeGUI;
    private TpaManager tpaManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        try {
            // Manager initialisieren
            getLogger().info("Initialisiere Manager...");
            this.economy = new EconomyManager(this);
            this.shards = new ShardsManager(this);
            this.stats = new PlayerStatsManager(this);
            this.scoreboardManager = new de.coolemod.donut.managers.SidebarManager(this);
            this.spawnerManager = new SpawnerManager(this);
            this.crateManager = new CrateManager(this);
            this.worthManager = new WorthManager(this);
            this.auctionManager = new AuctionHouseManagerNew(this);  // NEU
            this.auctionHouse = new AuctionHouse(this);  // NEW: Komplett neues System
            this.ordersManager = new OrdersManager(this);  // OLD
            this.orderSystem = new OrderSystem(this);  // NEW: Komplett neues Order System
            this.combatManager = new CombatManager(this);  // NEW: Combat System
            this.homeManager = new HomeManager(this, combatManager);  // NEW: Home System
            this.homeGUI = new HomeGUI(this, homeManager);  // NEW: Home GUI
            new HomeListener(this, homeManager, homeGUI);  // NEW: Home Listener
            this.tpaManager = new TpaManager(this, combatManager, homeManager);  // NEW: TPA System
            getLogger().info("Manager erfolgreich initialisiert.");

            // Listener registrieren
            getLogger().info("Registriere Listener...");
            getServer().getPluginManager().registerEvents(new PlayerDeathListener(this), this);
            getServer().getPluginManager().registerEvents(new SpawnerBreakListener(this), this);
            getServer().getPluginManager().registerEvents(new InventoryClickListener(this), this);
            getServer().getPluginManager().registerEvents(new InventoryCloseListener(this), this);
            getServer().getPluginManager().registerEvents(new InventoryDragListener(this), this);
            getServer().getPluginManager().registerEvents(new GlassPaneProtectionListener(this), this);
            getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
            getServer().getPluginManager().registerEvents(new PlayerInteractListener(this), this);
            getServer().getPluginManager().registerEvents(new PlayerChatListener(this), this);
            getServer().getPluginManager().registerEvents(new de.coolemod.donut.gui.AnvilInputGUI(this), this);
            // NEW: Komplett neues Auction House System
            getServer().getPluginManager().registerEvents(new AuctionHouseListener(this, auctionHouse), this);
            // NEW: Komplett neues Order System
            getServer().getPluginManager().registerEvents(new OrderListener(this, orderSystem), this);
            // NEW: Komplett neuer Shop mit InventoryHolder
            getServer().getPluginManager().registerEvents(new de.coolemod.donut.listeners.ShopListener_NEW(this), this);
            getLogger().info("Listener erfolgreich registriert.");

            // Commands registrieren
            getLogger().info("Registriere Commands...");
            getCommand("sell").setExecutor(new SellCommand(this));
            getCommand("worth").setExecutor(new WorthCommand(this));
            getCommand("pay").setExecutor(new PayCommand(this));
            getCommand("balance").setExecutor(new BalanceCommand(this));
            getCommand("slayshop").setExecutor(new SlayShopCommand(this));
            getCommand("ah").setExecutor(new AuctionHouseCommand(this, auctionHouse));  // NEW: Neues System
            getCommand("order").setExecutor(new de.coolemod.donut.orders.OrderCommand(this, orderSystem));  // NEW: Neues System
            getCommand("crate").setExecutor(new CrateCommand(this));
            getCommand("crateadmin").setExecutor(new de.coolemod.donut.commands.CrateAdminCommand(this));
            getCommand("shop").setExecutor(new ShopCommand(this));
            getCommand("rtp").setExecutor(new de.coolemod.donut.commands.WarpCommand(this));
            getCommand("menu").setExecutor(new de.coolemod.donut.commands.MenuCommand(this));
            // NEW: Home und TPA Commands
            HomeCommand homeCommand = new HomeCommand(this, homeManager);
            homeCommand.setHomeGUI(homeGUI);  // Set GUI reference
            getCommand("home").setExecutor(homeCommand);
            getCommand("home").setTabCompleter(homeCommand);
            getCommand("homes").setExecutor(homeCommand);  // /homes opens GUI
            TpaCommand tpaCommand = new TpaCommand(this, tpaManager);
            getCommand("tpa").setExecutor(tpaCommand);
            getCommand("tpa").setTabCompleter(tpaCommand);
            getCommand("tpahere").setExecutor(tpaCommand);
            getCommand("tpahere").setTabCompleter(tpaCommand);
            getCommand("tpaccept").setExecutor(tpaCommand);
            getCommand("tpdeny").setExecutor(tpaCommand);

            // TabCompleter
            de.coolemod.donut.commands.GlobalTabCompleter globalTab = new de.coolemod.donut.commands.GlobalTabCompleter();
            getCommand("crate").setTabCompleter(globalTab);
            getCommand("rtp").setTabCompleter(globalTab);
            getLogger().info("Commands erfolgreich registriert.");

            // Scoreboard starten
            getLogger().info("Starte Scoreboard...");
            scoreboardManager.start();
            getLogger().info("Scoreboard erfolgreich gestartet.");

            getLogger().info("DonutCore aktiviert.");
        } catch (Exception e) {
            getLogger().severe("FEHLER beim Laden von DonutCore!");
            getLogger().severe("Error: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @Override
    public void onDisable() {
        // Daten speichern (null-safe)
        if (economy != null) economy.save();
        if (shards != null) shards.save();
        if (stats != null) stats.save();
        if (auctionManager != null) auctionManager.save();
        if (ordersManager != null) ordersManager.save();
        getLogger().info("DonutCore deaktiviert und Daten gespeichert.");
    }

    public static DonutPlugin getInstance() {
        return instance;
    }

    // Getter für Manager
    public EconomyManager getEconomy() { return economy; }
    public ShardsManager getShards() { return shards; }
    public PlayerStatsManager getStats() { return stats; }
    public de.coolemod.donut.managers.SidebarManager getScoreboardManager() { return scoreboardManager; }
    public SpawnerManager getSpawnerManager() { return spawnerManager; }
    public CrateManager getCrateManager() { return crateManager; }
    public WorthManager getWorthManager() { return worthManager; }
    public AuctionHouseManagerNew getAuctionManager() { return auctionManager; }  // NEU
    public OrdersManager getOrdersManager() { return ordersManager; }  // OLD
    public AuctionHouse getAuctionHouse() { return auctionHouse; }  // NEW
    public OrderSystem getOrderSystem() { return orderSystem; }  // NEW
    public CombatManager getCombatManager() { return combatManager; }  // NEW
    public HomeManager getHomeManager() { return homeManager; }  // NEW
    public TpaManager getTpaManager() { return tpaManager; }  // NEW
}

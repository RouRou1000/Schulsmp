package de.coolemod.donut;

import de.coolemod.donut.managers.*;
import de.coolemod.donut.listeners.*;
import de.coolemod.donut.commands.*;
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
    private AuctionHouseManager auctionManager;
    private OrdersManager ordersManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        // Manager initialisieren
        this.economy = new EconomyManager(this);
        this.shards = new ShardsManager(this);
        this.stats = new PlayerStatsManager(this);
        this.scoreboardManager = new de.coolemod.donut.managers.SidebarManager(this);
        this.spawnerManager = new SpawnerManager(this);
        this.crateManager = new CrateManager(this);
        this.worthManager = new WorthManager(this);
        this.auctionManager = new AuctionHouseManager(this);
        this.ordersManager = new OrdersManager(this);

        // Listener registrieren
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

        // Commands registrieren
        getCommand("sell").setExecutor(new SellCommand(this));
        getCommand("worth").setExecutor(new WorthCommand(this));
        getCommand("pay").setExecutor(new PayCommand(this));
        getCommand("balance").setExecutor(new BalanceCommand(this));
        getCommand("slayshop").setExecutor(new SlayShopCommand(this));
        getCommand("ah").setExecutor(new AuctionCommand(this));
        getCommand("order").setExecutor(new OrderCommand(this));
        getCommand("crate").setExecutor(new CrateCommand(this));
        getCommand("crateadmin").setExecutor(new de.coolemod.donut.commands.CrateAdminCommand(this));
        getCommand("shop").setExecutor(new ShopCommand(this));
        getCommand("rtp").setExecutor(new de.coolemod.donut.commands.WarpCommand(this));
        getCommand("menu").setExecutor(new de.coolemod.donut.commands.MenuCommand(this));

        // TabCompleter
        de.coolemod.donut.commands.GlobalTabCompleter globalTab = new de.coolemod.donut.commands.GlobalTabCompleter();
        getCommand("crate").setTabCompleter(globalTab);
        getCommand("rtp").setTabCompleter(globalTab);

        // Scoreboard starten
        scoreboardManager.start();

        getLogger().info("DonutCore aktiviert.");
    }

    @Override
    public void onDisable() {
        // Daten speichern
        economy.save();
        shards.save();
        stats.save();
        auctionManager.save();
        ordersManager.save();
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
    public AuctionHouseManager getAuctionManager() { return auctionManager; }
    public OrdersManager getOrdersManager() { return ordersManager; }
}

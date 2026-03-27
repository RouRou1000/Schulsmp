package de.coolemod.schulcore;

import de.coolemod.schulcore.managers.*;
import de.coolemod.schulcore.listeners.*;
import de.coolemod.schulcore.commands.*;
import de.coolemod.schulcore.gui.*;
import de.coolemod.schulcore.auctionhouse.*;
import de.coolemod.schulcore.orders.*;
import de.coolemod.schulcore.systems.*;
import de.coolemod.schulcore.spawner.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;
import de.coolemod.schulcore.listeners.PlayerInteractListener;

/**
 * Hauptklasse des DonutCore-Plugins
 * Alles ist auf Deutsch kommentiert und konzipiert für Spigot/Paper 1.21.5
 */
public final class SchulCorePlugin extends JavaPlugin {

    private static SchulCorePlugin instance;

    // Manager
    private EconomyManager economy;
    private ShardsManager shards;
    private PlayerStatsManager stats;
    private de.coolemod.schulcore.managers.SidebarManager scoreboardManager;
    private SpawnerManager spawnerManager;
    private CrateManager crateManager;
    private WorthManager worthManager;
    private AuctionHouseManagerNew auctionManager;  // NEU: Neuer Manager
    private OrdersManager ordersManager;  // OLD
    
    // NEW: Komplett neues Auction House System
    private AuctionHouse auctionHouse;
    // NEW: Komplett neues Order System
    private de.coolemod.schulcore.orders.OrderSystem orderSystem;
    // NEW: Combat, Home und TPA System
    private CombatManager combatManager;
    private HomeManager homeManager;
    private HomeGUI homeGUI;
    private TpaManager tpaManager;
    // NEW: Spawner System (DonutSMP-Style)
    private SpawnerSystemManager spawnerSystem;
    // NEW: PacketCheck System
    private de.coolemod.schulcore.listeners.PacketCheckListener packetCheckListener;
    // NEW: Rang-System
    private de.coolemod.schulcore.managers.RankManager rankManager;
    // NEW: Wipe System
    private de.coolemod.schulcore.managers.WipeManager wipeManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        try {
            // Manager initialisieren
            getLogger().info("[SchulCore] Initialisiere Manager...");
            this.economy = new EconomyManager(this);
            this.shards = new ShardsManager(this);
            this.stats = new PlayerStatsManager(this);
            this.scoreboardManager = new de.coolemod.schulcore.managers.SidebarManager(this);
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
            this.spawnerSystem = new SpawnerSystemManager(this);  // NEW: Spawner System
            this.packetCheckListener = new de.coolemod.schulcore.listeners.PacketCheckListener(this);  // NEW: PacketCheck
            this.rankManager = new de.coolemod.schulcore.managers.RankManager(this);  // NEW: Rang-System
            this.wipeManager = new de.coolemod.schulcore.managers.WipeManager(this);  // NEW: Wipe System
            getLogger().info("[SchulCore] Manager erfolgreich initialisiert.");

            // Listener registrieren
            getLogger().info("[SchulCore] Registriere Listener...");
            getServer().getPluginManager().registerEvents(new PlayerDeathListener(this), this);
            getServer().getPluginManager().registerEvents(new SpawnerBreakListener(this), this);
            getServer().getPluginManager().registerEvents(new SpawnerListener(this, spawnerSystem), this);  // NEW: Spawner System
            getServer().getPluginManager().registerEvents(new InventoryClickListener(this), this);
            getServer().getPluginManager().registerEvents(new InventoryCloseListener(this), this);
            getServer().getPluginManager().registerEvents(new InventoryDragListener(this), this);
            getServer().getPluginManager().registerEvents(new GlassPaneProtectionListener(this), this);
            getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
            getServer().getPluginManager().registerEvents(new de.coolemod.schulcore.listeners.PlayerInteractListener(this), this);
            getServer().getPluginManager().registerEvents(new PlayerChatListener(this), this);
            getServer().getPluginManager().registerEvents(new de.coolemod.schulcore.gui.AnvilInputGUI(this), this);
            // NEW: Komplett neues Auction House System
            getServer().getPluginManager().registerEvents(new AuctionHouseListener(this, auctionHouse), this);
            // NEW: Komplett neues Order System
            getServer().getPluginManager().registerEvents(new OrderListener(this, orderSystem), this);
            // NEW: Komplett neuer Shop mit InventoryHolder
            getServer().getPluginManager().registerEvents(new de.coolemod.schulcore.listeners.ShopListener_NEW(this), this);
            if (getServer().getPluginManager().isPluginEnabled("ProtocolLib")) {
                try {
                    getServer().getPluginManager().registerEvents(new WorthLoreListener(this), this);
                } catch (Exception ex) {
                    getLogger().warning("[SchulCore] WorthLoreListener konnte nicht geladen werden: " + ex.getMessage());
                    ex.printStackTrace();
                }
            } else {
                getLogger().warning("[SchulCore] ProtocolLib nicht gefunden. Worth-Anzeige im Inventar ist deaktiviert.");
            }
            // NEW: PacketCheck Listener
            getServer().getPluginManager().registerEvents(packetCheckListener, this);
            getLogger().info("[SchulCore] Listener erfolgreich registriert.");

            // Commands registrieren
            getLogger().info("[SchulCore] Registriere Commands...");
            registerCommand("sell", new SellCommand(this), null);
            registerCommand("worth", new WorthCommand(this), null);
            registerCommand("pay", new PayCommand(this), null);
            registerCommand("balance", new BalanceCommand(this), null);
            registerCommand("slayshop", new SlayShopCommand(this), null);
            registerCommand("ah", new AuctionHouseCommand(this, auctionHouse), null);  // NEW: Neues System
            registerCommand("order", new de.coolemod.schulcore.orders.OrderCommand(this, orderSystem), null);  // NEW: Neues System
            registerCommand("crate", new CrateCommand(this), null);
            registerCommand("crateadmin", new de.coolemod.schulcore.commands.CrateAdminCommand(this), null);
            registerCommand("shop", new ShopCommand(this), null);
            registerCommand("rtp", new de.coolemod.schulcore.commands.WarpCommand(this), null);
            registerCommand("spawn", new de.coolemod.schulcore.commands.SpawnCommand(this), null);
            registerCommand("menu", new de.coolemod.schulcore.commands.MenuCommand(this), null);
            // Debug-Commands
            MoneyGiveCommand moneyCmd = new MoneyGiveCommand(this);
            registerCommand("money", moneyCmd, moneyCmd);
            ShardsGiveCommand shardsCmd = new ShardsGiveCommand(this);
            registerCommand("shards", shardsCmd, shardsCmd);
            // NEW: Home und TPA Commands
            HomeCommand homeCommand = new HomeCommand(this, homeManager);
            homeCommand.setHomeGUI(homeGUI);  // Set GUI reference
            registerCommand("home", homeCommand, homeCommand);
            registerCommand("homes", homeCommand, null);  // /homes opens GUI
            TpaCommand tpaCommand = new TpaCommand(this, tpaManager);
            registerCommand("tpa", tpaCommand, tpaCommand);
            registerCommand("tpahere", tpaCommand, tpaCommand);
            registerCommand("tpaccept", tpaCommand, null);
            registerCommand("tpdeny", tpaCommand, null);

            // TabCompleter
            de.coolemod.schulcore.commands.GlobalTabCompleter globalTab = new de.coolemod.schulcore.commands.GlobalTabCompleter();
            registerTabCompleter("crate", globalTab);
            // NEW: PacketCheck Command
            de.coolemod.schulcore.commands.PacketCheckCommand pcCmd = new de.coolemod.schulcore.commands.PacketCheckCommand(this, packetCheckListener);
            pcCmd.setWipeManager(wipeManager);
            registerCommand("packetcheck", pcCmd, pcCmd);
            // NEW: Wipe Commands
            de.coolemod.schulcore.commands.WipeCommand wipeCmd = new de.coolemod.schulcore.commands.WipeCommand(this, wipeManager);
            registerCommand("wipe", wipeCmd, wipeCmd);
            registerCommand("unwipe", wipeCmd, wipeCmd);
            // NEW: Rang-System
            de.coolemod.schulcore.commands.RankCommand rankCmd = new de.coolemod.schulcore.commands.RankCommand(this, rankManager);
            registerCommand("rank", rankCmd, rankCmd);
            getLogger().info("[SchulCore] Commands erfolgreich registriert.");

            // Scoreboard starten
            getLogger().info("[SchulCore] Starte Scoreboard...");
            scoreboardManager.start();
            getLogger().info("[SchulCore] Scoreboard erfolgreich gestartet.");

            getLogger().info("[SchulCore] SchulCore aktiviert.");
        } catch (Exception e) {
            getLogger().severe("[SchulCore] FEHLER beim Laden von SchulCore!");
            getLogger().severe("[SchulCore] Error: " + e.getMessage());
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
        if (spawnerSystem != null) spawnerSystem.saveData();  // NEW: Spawner System
        if (rankManager != null) rankManager.save();  // NEW: Rang-System
        getLogger().info("[SchulCore] SchulCore deaktiviert und Daten gespeichert.");
    }

    public static SchulCorePlugin getInstance() {
        return instance;
    }

    private void registerCommand(String name, CommandExecutor executor, TabCompleter completer) {
        var command = getCommand(name);
        if (command == null) {
            getLogger().warning("[SchulCore] Command '" + name + "' fehlt in plugin.yml und wurde nicht registriert.");
            return;
        }
        command.setExecutor(executor);
        if (completer != null) {
            command.setTabCompleter(completer);
        }
    }

    private void registerTabCompleter(String name, TabCompleter completer) {
        var command = getCommand(name);
        if (command == null) {
            getLogger().warning("[SchulCore] Command '" + name + "' fehlt in plugin.yml und bekam keinen TabCompleter.");
            return;
        }
        command.setTabCompleter(completer);
    }

    // Getter für Manager
    public EconomyManager getEconomy() { return economy; }
    public ShardsManager getShards() { return shards; }
    public PlayerStatsManager getStats() { return stats; }
    public de.coolemod.schulcore.managers.SidebarManager getScoreboardManager() { return scoreboardManager; }
    public SpawnerManager getSpawnerManager() { return spawnerManager; }
    public CrateManager getCrateManager() { return crateManager; }
    public WorthManager getWorthManager() { return worthManager; }
    public AuctionHouseManagerNew getAuctionManager() { return auctionManager; }  // NEU
    public OrdersManager getOrdersManager() { return ordersManager; }  // OLD
    public AuctionHouse getAuctionHouse() { return auctionHouse; }  // NEW
    public de.coolemod.schulcore.orders.OrderSystem getOrderSystem() { return orderSystem; }  // NEW
    public CombatManager getCombatManager() { return combatManager; }  // NEW
    public HomeManager getHomeManager() { return homeManager; }  // NEW
    public TpaManager getTpaManager() { return tpaManager; }  // NEW
    public SpawnerSystemManager getSpawnerSystem() { return spawnerSystem; }  // NEW: Spawner System
    public de.coolemod.schulcore.managers.RankManager getRankManager() { return rankManager; }  // NEW: Rang-System
    public de.coolemod.schulcore.managers.WipeManager getWipeManager() { return wipeManager; }  // NEW: Wipe System
}

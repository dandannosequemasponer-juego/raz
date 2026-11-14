package com.cytricos.races;

import com.cytricos.races.command.CommandManager;
import com.cytricos.races.gui.*;
import com.cytricos.races.hook.LibsDisguisesHook;
import com.cytricos.races.hook.PlaceholderAPIHook;
import com.cytricos.races.hook.ProtocolLibHook;
import com.cytricos.races.hook.WorldGuardHook;
import com.cytricos.races.listener.*;
import com.cytricos.races.manager.*;
import com.cytricos.races.model.PlayerData;
import com.cytricos.races.task.RelicTask;
import com.cytricos.races.util.ColorUtils;
import net.luckperms.api.LuckPerms;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;

/**
 * Clase principal del plugin CytricosRaces para CYTRICOS RPG.
 * Compatible con Paper 26.2 y Java 25.
 * Lead Developers: Dandan_13 & Wolfit12
 */
public class CytricosRaces extends JavaPlugin {
    private static CytricosRaces instance;

    private ConfigManager configManager;
    private StorageManager storageManager;
    private PlayerDataManager playerDataManager;
    private RaceManager raceManager;
    private CustomItemManager customItemManager;
    private CosmeticsManager cosmeticsManager;
    private WingsManager wingsManager;
    private HeadCosmeticManager headCosmeticManager;
    private QuestManager questManager;
    private RelicManager relicManager;
    private ResourcePackManager resourcePackManager;
    private TransformationManager transformationManager;
    private RelicTask relicTask;

    private PlayerGUI playerGUI;
    private RaceChangeConfirmGUI raceChangeConfirmGUI;
    private MainMenuGUI mainMenuGUI;
    private ProfileGUI profileGUI;
    private AbilityBookGUI abilityBookGUI;
    private SkillTreeGUI skillTreeGUI;
    private QuestsGUI questsGUI;
    private AchievementsGUI achievementsGUI;
    private CosmeticsGUI cosmeticsGUI;
    private CustomItemsGUI customItemsGUI;
    private SettingsGUI settingsGUI;
    private EncyclopediaGUI encyclopediaGUI;
    private AdminGUI adminGUI;

    private Economy vaultEconomy = null;
    private LuckPerms luckPerms = null;
    private ProtocolLibHook protocolLibHook = null;
    private WorldGuardHook worldGuardHook = null;
    private LibsDisguisesHook libsDisguisesHook = null;
    private RaceAbilitiesListener raceAbilitiesListener;

    private final Map<UUID, List<Long>> flightViolations = new ConcurrentHashMap<>();

    @Override
    public void onEnable() {
        instance = this;

        long startTime = System.currentTimeMillis();
        getLogger().info("=========================================");
        getLogger().info("Iniciando CytricosRaces v7.0.0 (Paper 26.2 / Java 25)...");
        getLogger().info("Desarrolladores Líderes: Dandan_13 & Wolfit12");
        getLogger().info("Servidor: CYTRICOS RPG (play.cytricos.com)");
        getLogger().info("=========================================");

        // 1. Inicializar Gestores, Almacenamiento y Hooks Tempranos
        this.configManager = new ConfigManager(this);
        this.configManager.setup();

        this.protocolLibHook = new ProtocolLibHook(this);
        this.storageManager = new StorageManager(this);
        this.storageManager.init();

        this.playerDataManager = new PlayerDataManager(this);
        this.raceManager = new RaceManager(this);
        this.customItemManager = new CustomItemManager(this);
        this.cosmeticsManager = new CosmeticsManager(this);
        this.wingsManager = new WingsManager(this);
        this.headCosmeticManager = new HeadCosmeticManager(this);
        this.questManager = new QuestManager(this);
        this.relicManager = new RelicManager(this);
        this.resourcePackManager = new ResourcePackManager(this);
        this.transformationManager = new TransformationManager(this);
        this.relicTask = new RelicTask(this);

        this.playerGUI = new PlayerGUI(this);
        this.raceChangeConfirmGUI = new RaceChangeConfirmGUI(this);
        this.mainMenuGUI = new MainMenuGUI(this);
        this.profileGUI = new ProfileGUI(this);
        this.abilityBookGUI = new AbilityBookGUI(this);
        this.skillTreeGUI = new SkillTreeGUI(this);
        this.questsGUI = new QuestsGUI(this);
        this.achievementsGUI = new AchievementsGUI(this);
        this.cosmeticsGUI = new CosmeticsGUI(this);
        this.customItemsGUI = new CustomItemsGUI(this);
        this.settingsGUI = new SettingsGUI(this);
        this.encyclopediaGUI = new EncyclopediaGUI(this);
        this.adminGUI = new AdminGUI(this);

        // 2. Cargar razas desde races.yml
        this.raceManager.loadRaces();

        // 3. Hooks a plugins externos
        setupVault();
        setupLuckPerms();
        setupPlaceholderAPI();
        this.worldGuardHook = new WorldGuardHook(this);
        this.libsDisguisesHook = new LibsDisguisesHook(this);

        // 4. Registrar Listeners
        this.raceAbilitiesListener = new RaceAbilitiesListener(this);
        getServer().getPluginManager().registerEvents(new GUIListener(this), this);
        getServer().getPluginManager().registerEvents(this.raceAbilitiesListener, this);
        getServer().getPluginManager().registerEvents(new EnvironmentListener(this), this);
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        getServer().getPluginManager().registerEvents(new CustomItemListener(this), this);
        getServer().getPluginManager().registerEvents(new CustomMobDropListener(this), this);

        // 5. Registrar Comandos y Tab Completion
        CommandManager cmdManager = new CommandManager(this);
        PluginCommand cmd = getCommand("raza");
        if (cmd != null) {
            cmd.setExecutor(cmdManager);
            cmd.setTabCompleter(cmdManager);
        }

        // 6. Iniciar Tareas Periódicas y SHA-1 Asíncrono del Resource Pack
        startAutoSaveTask();
        startFlightHeightCheckTask();
        if (cosmeticsManager != null) cosmeticsManager.startAuraTask();
        if (wingsManager != null) wingsManager.startSyncTask();
        if (headCosmeticManager != null) headCosmeticManager.startSyncTask();
        if (transformationManager != null) transformationManager.startTasks();
        if (relicTask != null) relicTask.start();
        if (resourcePackManager != null) resourcePackManager.initAsyncSha1();

        long diff = System.currentTimeMillis() - startTime;
        getLogger().info("CytricosRaces iniciado satisfactoriamente en " + diff + "ms.");
    }

    @Override
    public void onDisable() {
        getLogger().info("Deshabilitando CytricosRaces...");

        if (transformationManager != null) transformationManager.stopAllTasks();
        if (relicTask != null) relicTask.stop();
        if (cosmeticsManager != null) cosmeticsManager.stopAuraTask();
        if (wingsManager != null) wingsManager.stopSyncTask();
        if (headCosmeticManager != null) headCosmeticManager.stopSyncTask();
        if (protocolLibHook != null) protocolLibHook.unregisterAll();

        if (playerDataManager != null) playerDataManager.saveAll();
        if (storageManager != null) storageManager.close();

        getServer().getScheduler().cancelTasks(this);
        getLogger().info("CytricosRaces deshabilitado con éxito. Datos guardados.");
    }

    private void startAutoSaveTask() {
        int intervalSeconds = getConfig().getInt("general_settings.save_interval_seconds", 300);
        getServer().getScheduler().runTaskTimerAsynchronously(this, () -> playerDataManager.saveAll(),
                intervalSeconds * 20L, intervalSeconds * 20L);
    }

    private void startFlightHeightCheckTask() {
        getServer().getScheduler().runTaskTimer(this, () -> {
            double fairyMax = getConfig().getDouble("flight_settings.fairy_max_height", 16.0);
            double angelMax = getConfig().getDouble("flight_settings.angel_max_height", 28.0);
            long now = System.currentTimeMillis();

            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
                    continue;
                }
                PlayerData data = playerDataManager.get(player.getUniqueId());
                if (data == null || !data.hasAssignedRace()) continue;

                if (data.isFlightBanned()) {
                    if (player.isFlying() || player.getAllowFlight()) {
                        player.setFlying(false);
                        player.setAllowFlight(false);
                    }
                    continue;
                }

                String race = data.getRace().toLowerCase();
                int level = data.getLevel();
                if (data.isTransformed()) continue; // El techo Y en transformación lo gestiona TransformationManager

                double limit = -1;
                if (race.equals("fairy")) {
                    limit = fairyMax;
                } else if (race.equals("angel") && level >= 3) {
                    limit = angelMax;
                }

                if (limit > 0 && player.isFlying()) {
                    int groundY = player.getWorld().getHighestBlockYAt(player.getLocation());
                    double altitude = player.getLocation().getY() - groundY;
                    if (altitude > limit) {
                        Vector v = player.getVelocity();
                        player.setVelocity(new Vector(v.getX(), -1.8, v.getZ()));
                        ColorUtils.sendActionBar(player, "&c&l¡LÍMITE DE ALTITUD (" + (int) limit + "m)! &7Descendiendo forzosamente.");
                        player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 8, 0.3, 0.2, 0.3, 0.05);
                        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.5f);

                        List<Long> strikes = flightViolations.computeIfAbsent(player.getUniqueId(), k -> new CopyOnWriteArrayList<>());
                        strikes.removeIf(t -> (now - t) > 10000L);
                        strikes.add(now);

                        if (strikes.size() >= 3) {
                            strikes.clear();
                            player.setFlying(false);
                            player.setAllowFlight(false);
                            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 100, 0, false, false));

                            long banUntil = now + (5 * 60 * 1000L);
                            data.setFlightBanUntil(banUntil);
                            playerDataManager.saveToFile(data);

                            ColorUtils.sendTitle(player, "&4&l¡VUELO CLAUSURADO!", "&c3 infracciones de altitud. 5m de penalización.", 10, 70, 20);
                            ColorUtils.sendMessage(player, "&c&l[SISTEMA DE VUELO] &7Has sido sancionado con &e5 minutos &7sin capacidad de vuelo por ignorar el límite de altitud.");
                            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.8f, 1.2f);
                        }
                    }
                }
            }
        }, 20L, 10L);
    }

    private void setupVault() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            getLogger().warning("Vault no encontrado. El sistema de pago por economía estará inactivo.");
            return;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            getLogger().warning("No se encontró un proveedor de Economía compatible con Vault.");
            return;
        }
        this.vaultEconomy = rsp.getProvider();
        getLogger().info("Integración con Vault Economy establecida correctamente.");
    }

    private void setupLuckPerms() {
        try {
            RegisteredServiceProvider<LuckPerms> provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
            if (provider != null) {
                this.luckPerms = provider.getProvider();
                getLogger().info("Integración con LuckPerms API 5.4 establecida correctamente.");
            } else {
                getLogger().warning("LuckPerms API no encontrada en los servicios registrados.");
            }
        } catch (Throwable t) {
            getLogger().log(Level.WARNING, "No se pudo enlazar LuckPerms: " + t.getMessage());
        }
    }

    private void setupPlaceholderAPI() {
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PlaceholderAPIHook(this).register();
            getLogger().info("Expansión de PlaceholderAPI (%cytricosraces_*) registrada correctamente.");
        } else {
            getLogger().info("PlaceholderAPI no detectado.");
        }
    }

    public static CytricosRaces getInstance() { return instance; }
    public static Economy getVaultEco() { return instance != null ? instance.vaultEconomy : null; }
    public ConfigManager getConfigManager() { return configManager; }
    public PlayerDataManager getPlayerDataManager() { return playerDataManager; }
    public RaceManager getRaceManager() { return raceManager; }
    public CustomItemManager getCustomItemManager() { return customItemManager; }
    public CosmeticsManager getCosmeticsManager() { return cosmeticsManager; }
    public WingsManager getWingsManager() { return wingsManager; }
    public HeadCosmeticManager getHeadCosmeticManager() { return headCosmeticManager; }
    public QuestManager getQuestManager() { return questManager; }
    public RelicManager getRelicManager() { return relicManager; }
    public ResourcePackManager getResourcePackManager() { return resourcePackManager; }
    public TransformationManager getTransformationManager() { return transformationManager; }
    public RelicTask getRelicTask() { return relicTask; }
    public StorageManager getStorageManager() { return storageManager; }

    public PlayerGUI getPlayerGUI() { return playerGUI; }
    public RaceChangeConfirmGUI getRaceChangeConfirmGUI() { return raceChangeConfirmGUI; }
    public MainMenuGUI getMainMenuGUI() { return mainMenuGUI; }
    public ProfileGUI getProfileGUI() { return profileGUI; }
    public AbilityBookGUI getAbilityBookGUI() { return abilityBookGUI; }
    public SkillTreeGUI getSkillTreeGUI() { return skillTreeGUI; }
    public QuestsGUI getQuestsGUI() { return questsGUI; }
    public AchievementsGUI getAchievementsGUI() { return achievementsGUI; }
    public CosmeticsGUI getCosmeticsGUI() { return cosmeticsGUI; }
    public CustomItemsGUI getCustomItemsGUI() { return customItemsGUI; }
    public SettingsGUI getSettingsGUI() { return settingsGUI; }
    public EncyclopediaGUI getEncyclopediaGUI() { return encyclopediaGUI; }
    public AdminGUI getAdminGUI() { return adminGUI; }

    public Economy getVaultEconomy() { return vaultEconomy; }
    public boolean isVaultAvailable() { return vaultEconomy != null; }
    public LuckPerms getLuckPerms() { return luckPerms; }
    public boolean isLuckPermsAvailable() { return luckPerms != null; }
    public ProtocolLibHook getProtocolLibHook() { return protocolLibHook; }
    public WorldGuardHook getWorldGuardHook() { return worldGuardHook; }
    public LibsDisguisesHook getLibsDisguisesHook() { return libsDisguisesHook; }
    public RaceAbilitiesListener getRaceAbilitiesListener() { return raceAbilitiesListener; }

    public void sendConfiguredResourcePack(Player player) {
        if (resourcePackManager != null) {
            resourcePackManager.sendPack(player, false);
        }
    }
}

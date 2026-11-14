package com.cytricos.races;

import com.cytricos.races.command.CommandManager;
import com.cytricos.races.gui.AdminGUI;
import com.cytricos.races.gui.PlayerGUI;
import com.cytricos.races.hook.PlaceholderAPIHook;
import com.cytricos.races.listener.GUIListener;
import com.cytricos.races.listener.RaceAbilitiesListener;
import com.cytricos.races.manager.*;
import net.luckperms.api.LuckPerms;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

/**
 * Clase principal del plugin CytricosRaces para CYTRICOS RPG.
 * Compatible con Spigot / Paper 1.20.5+ y 1.21 (Java 21).
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
    private QuestManager questManager;

    private PlayerGUI playerGUI;
    private com.cytricos.races.gui.RaceChangeConfirmGUI raceChangeConfirmGUI;
    private com.cytricos.races.gui.MainMenuGUI mainMenuGUI;
    private com.cytricos.races.gui.ProfileGUI profileGUI;
    private com.cytricos.races.gui.AbilityBookGUI abilityBookGUI;
    private com.cytricos.races.gui.SkillTreeGUI skillTreeGUI;
    private com.cytricos.races.gui.QuestsGUI questsGUI;
    private com.cytricos.races.gui.AchievementsGUI achievementsGUI;
    private com.cytricos.races.gui.CosmeticsGUI cosmeticsGUI;
    private com.cytricos.races.gui.SettingsGUI settingsGUI;
    private com.cytricos.races.gui.EncyclopediaGUI encyclopediaGUI;
    private AdminGUI adminGUI;

    private Economy vaultEconomy = null;
    private LuckPerms luckPerms = null;
    private com.cytricos.races.hook.WorldGuardHook worldGuardHook = null;
    private com.cytricos.races.hook.LibsDisguisesHook libsDisguisesHook = null;
    private RaceAbilitiesListener raceAbilitiesListener;

    @Override
    public void onEnable() {
        instance = this;

        long startTime = System.currentTimeMillis();
        getLogger().info("=========================================");
        getLogger().info("Iniciando CytricosRaces v7.0.0...");
        getLogger().info("Desarrolladores Lideres: Dandan_13 & Wolfit12");
        getLogger().info("Servidor: CYTRICOS RPG (play.cytricos.com)");
        getLogger().info("=========================================");

        // 1. Inicializar Gestores y Almacenamiento
        this.configManager = new ConfigManager(this);
        this.configManager.setup();

        this.storageManager = new StorageManager(this);
        this.storageManager.init();

        this.playerDataManager = new PlayerDataManager(this);
        this.raceManager = new RaceManager(this);
        this.customItemManager = new CustomItemManager(this);
        this.cosmeticsManager = new CosmeticsManager(this);
        this.questManager = new QuestManager(this);

        this.playerGUI = new PlayerGUI(this);
        this.raceChangeConfirmGUI = new com.cytricos.races.gui.RaceChangeConfirmGUI(this);
        this.mainMenuGUI = new com.cytricos.races.gui.MainMenuGUI(this);
        this.profileGUI = new com.cytricos.races.gui.ProfileGUI(this);
        this.abilityBookGUI = new com.cytricos.races.gui.AbilityBookGUI(this);
        this.skillTreeGUI = new com.cytricos.races.gui.SkillTreeGUI(this);
        this.questsGUI = new com.cytricos.races.gui.QuestsGUI(this);
        this.achievementsGUI = new com.cytricos.races.gui.AchievementsGUI(this);
        this.cosmeticsGUI = new com.cytricos.races.gui.CosmeticsGUI(this);
        this.settingsGUI = new com.cytricos.races.gui.SettingsGUI(this);
        this.encyclopediaGUI = new com.cytricos.races.gui.EncyclopediaGUI(this);
        this.adminGUI = new AdminGUI(this);

        // 2. Cargar razas desde races.yml
        this.raceManager.loadRaces();

        // 3. Hooks a plugins externos
        setupVault();
        setupLuckPerms();
        setupPlaceholderAPI();
        this.worldGuardHook = new com.cytricos.races.hook.WorldGuardHook(this);
        this.libsDisguisesHook = new com.cytricos.races.hook.LibsDisguisesHook(this);

        // 4. Registrar Listeners
        this.raceAbilitiesListener = new RaceAbilitiesListener(this);
        getServer().getPluginManager().registerEvents(new GUIListener(this), this);
        getServer().getPluginManager().registerEvents(this.raceAbilitiesListener, this);
        getServer().getPluginManager().registerEvents(new com.cytricos.races.listener.EnvironmentListener(this), this);
        getServer().getPluginManager().registerEvents(new com.cytricos.races.listener.ChatListener(this), this);

        // 5. Registrar Comandos y Tab Completion
        CommandManager cmdManager = new CommandManager(this);
        PluginCommand cmd = getCommand("raza");
        if (cmd != null) {
            cmd.setExecutor(cmdManager);
            cmd.setTabCompleter(cmdManager);
        }


        // 6. Iniciar Tareas Periódicas
        startAutoSaveTask();
        startFlightHeightCheckTask();
        if (cosmeticsManager != null) {
            cosmeticsManager.startAuraTask();
        }

        long diff = System.currentTimeMillis() - startTime;
        getLogger().info("CytricosRaces iniciado satisfactoriamente en " + diff + "ms.");
    }

    @Override
    public void onDisable() {
        getLogger().info("Deshabilitando CytricosRaces...");

        if (cosmeticsManager != null) {
            cosmeticsManager.stopAuraTask();
        }

        if (playerDataManager != null) {
            playerDataManager.saveAll();
        }

        if (storageManager != null) {
            storageManager.close();
        }

        getServer().getScheduler().cancelTasks(this);
        getLogger().info("CytricosRaces deshabilitado con exito. Datos salvados.");
    }

    private void startAutoSaveTask() {
        int intervalSeconds = getConfig().getInt("general_settings.save_interval_seconds", 300);
        getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {
            playerDataManager.saveAll();
        }, intervalSeconds * 20L, intervalSeconds * 20L);
    }

    private final java.util.Map<java.util.UUID, java.util.List<Long>> flightViolations = new java.util.concurrent.ConcurrentHashMap<>();

    private void startFlightHeightCheckTask() {
        // Tarea cada 10 ticks (0.5s): Control estricto de altitud para Fairy y Angel con sistema de 3 strikes
        getServer().getScheduler().runTaskTimer(this, () -> {
            double fairyMax = getConfig().getDouble("flight_settings.fairy_max_height", 16.0);
            double angelMax = getConfig().getDouble("flight_settings.angel_max_height", 28.0);
            long now = System.currentTimeMillis();

            for (org.bukkit.entity.Player player : Bukkit.getOnlinePlayers()) {
                if (player.getGameMode() == org.bukkit.GameMode.CREATIVE || player.getGameMode() == org.bukkit.GameMode.SPECTATOR) {
                    continue;
                }
                PlayerData data = playerDataManager.get(player.getUniqueId());
                if (data == null) continue;

                // Si tiene ban de vuelo activo, forzar caída
                if (data.isFlightBanned()) {
                    if (player.isFlying() || player.getAllowFlight()) {
                        player.setFlying(false);
                        player.setAllowFlight(false);
                    }
                    continue;
                }

                String race = data.getRace().toLowerCase();
                int level = data.getLevel();

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
                        // Vector de empuje descendente estricto (-1.8)
                        org.bukkit.util.Vector v = player.getVelocity();
                        player.setVelocity(new org.bukkit.util.Vector(v.getX(), -1.8, v.getZ()));
                        com.cytricos.races.util.ColorUtils.sendActionBar(player, "&c&l¡LÍMITE DE ALTITUD (" + (int)limit + "m)! &7Descendiendo forzosamente.");
                        player.getWorld().spawnParticle(org.bukkit.Particle.CLOUD, player.getLocation(), 8, 0.3, 0.2, 0.3, 0.05);
                        player.getWorld().playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.5f);

                        // Registro de strikes dentro de una ventana de 10 segundos
                        java.util.List<Long> strikes = flightViolations.computeIfAbsent(player.getUniqueId(), k -> new java.util.concurrent.CopyOnWriteArrayList<>());
                        strikes.removeIf(t -> (now - t) > 10000L);
                        strikes.add(now);

                        if (strikes.size() >= 3) {
                            strikes.clear();
                            player.setFlying(false);
                            player.setAllowFlight(false);
                            // 5s de caída lenta para evitar muerte por impacto
                            player.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SLOW_FALLING, 100, 0, false, false));

                            // Penalización persistente de 5 minutos
                            long banUntil = now + (5 * 60 * 1000L);
                            data.setFlightBanUntil(banUntil);
                            playerDataManager.saveToFile(data);

                            com.cytricos.races.util.ColorUtils.sendTitle(player, "&4&l¡VUELO CLAUSURADO!", "&c3 infracciones de altitud. 5m de penalización.", 10, 70, 20);
                            com.cytricos.races.util.ColorUtils.sendMessage(player, "&c&l[SISTEMA DE VUELO] &7Has sido sancionado con &e5 minutos &7sin capacidad de vuelo por ignorar el límite de altitud.");
                            player.getWorld().playSound(player.getLocation(), org.bukkit.Sound.ENTITY_WITHER_SPAWN, 0.8f, 1.2f);
                        }
                    }
                }
            }
        }, 20L, 10L);
    }

    private void setupVault() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            getLogger().warning("Vault no encontrado. El sistema de pago por economia estara inactivo.");
            return;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            getLogger().warning("No se encontro un proveedor de Economia compatible con Vault.");
            return;
        }
        this.vaultEconomy = rsp.getProvider();
        getLogger().info("Integracion con Vault Economy establecida correctamente.");
    }

    private void setupLuckPerms() {
        try {
            RegisteredServiceProvider<LuckPerms> provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
            if (provider != null) {
                this.luckPerms = provider.getProvider();
                getLogger().info("Integracion con LuckPerms API 5.4 establecida correctamente.");
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
            getLogger().info("Expansion de PlaceholderAPI (%cytricosraces_*) registrada correctamente.");
        } else {
            getLogger().info("PlaceholderAPI no detectado. Las variables de chat y scoreboards estaran inactivas.");
        }
    }

    public static CytricosRaces getInstance() {
        return instance;
    }

    public static Economy getVaultEco() {
        return instance != null ? instance.vaultEconomy : null;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }

    public RaceManager getRaceManager() {
        return raceManager;
    }

    public CustomItemManager getCustomItemManager() {
        return customItemManager;
    }

    public PlayerGUI getPlayerGUI() {
        return playerGUI;
    }

    public com.cytricos.races.gui.RaceChangeConfirmGUI getRaceChangeConfirmGUI() {
        return raceChangeConfirmGUI;
    }

    public AdminGUI getAdminGUI() {
        return adminGUI;
    }

    public Economy getVaultEconomy() {
        return vaultEconomy;
    }

    public boolean isVaultAvailable() {
        return vaultEconomy != null;
    }

    public LuckPerms getLuckPerms() {
        return luckPerms;
    }

    public boolean isLuckPermsAvailable() {
        return luckPerms != null;
    }

    public StorageManager getStorageManager() {
        return storageManager;
    }

    public com.cytricos.races.hook.WorldGuardHook getWorldGuardHook() {
        return worldGuardHook;
    }

    public com.cytricos.races.hook.LibsDisguisesHook getLibsDisguisesHook() {
        return libsDisguisesHook;
    }

    public RaceAbilitiesListener getRaceAbilitiesListener() {
        return raceAbilitiesListener;
    }

    public com.cytricos.races.gui.MainMenuGUI getMainMenuGUI() {
        return mainMenuGUI;
    }

    public com.cytricos.races.gui.ProfileGUI getProfileGUI() {
        return profileGUI;
    }

    public com.cytricos.races.gui.AbilityBookGUI getAbilityBookGUI() {
        return abilityBookGUI;
    }

    public com.cytricos.races.gui.SkillTreeGUI getSkillTreeGUI() {
        return skillTreeGUI;
    }

    public com.cytricos.races.gui.QuestsGUI getQuestsGUI() {
        return questsGUI;
    }

    public com.cytricos.races.gui.AchievementsGUI getAchievementsGUI() {
        return achievementsGUI;
    }

    public com.cytricos.races.gui.CosmeticsGUI getCosmeticsGUI() {
        return cosmeticsGUI;
    }

    public com.cytricos.races.gui.SettingsGUI getSettingsGUI() {
        return settingsGUI;
    }

    public com.cytricos.races.gui.EncyclopediaGUI getEncyclopediaGUI() {
        return encyclopediaGUI;
    }

    public CosmeticsManager getCosmeticsManager() {
        return cosmeticsManager;
    }

    public QuestManager getQuestManager() {
        return questManager;
    }
}



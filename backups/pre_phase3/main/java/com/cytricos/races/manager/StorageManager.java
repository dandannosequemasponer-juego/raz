package com.cytricos.races.manager;

import com.cytricos.races.CytricosRaces;
import com.cytricos.races.model.PlayerData;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.Collection;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Gestor unificado de almacenamiento persistente dual (YAML / MySQL) para CYTRICOS RPG.
 * Compatible con HikariCP Connection Pool de alto rendimiento en Paper 1.20.5+ / 1.21.
 * Ofrece persistencia estricta de:
 * - race_id, level, free_change_available
 * - flight_ban_until (castigo persistente de vuelo)
 * - transform_cooldown_until (cooldown de metamorfosis)
 * - active_disguise y transform_active_until (estado de disfraz y nivel 6 infinito)
 *
 * Lead Developers: Dandan_13 & Wolfit12
 */
public class StorageManager {
    private final CytricosRaces plugin;
    private final File dataFolder;
    private StorageType storageType;
    private HikariDataSource dataSource;
    private String tablePrefix;

    public enum StorageType {
        YAML,
        MYSQL
    }

    public StorageManager(CytricosRaces plugin) {
        this.plugin = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "userdata");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
    }

    public void init() {
        String typeStr = plugin.getConfig().getString("storage.type", "YAML").toUpperCase();
        try {
            this.storageType = StorageType.valueOf(typeStr);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Tipo de almacenamiento '" + typeStr + "' no reconocido. Usando YAML por defecto.");
            this.storageType = StorageType.YAML;
        }

        if (this.storageType == StorageType.MYSQL) {
            setupMySQL();
        } else {
            plugin.getLogger().info("Almacenamiento configurado en modo YAML Flatfile (userdata/).");
        }
    }

    private void setupMySQL() {
        try {
            this.tablePrefix = plugin.getConfig().getString("storage.mysql.table_prefix", "cr_");
            String host = plugin.getConfig().getString("storage.mysql.host", "localhost");
            int port = plugin.getConfig().getInt("storage.mysql.port", 3306);
            String database = plugin.getConfig().getString("storage.mysql.database", "cytricos_races");
            String username = plugin.getConfig().getString("storage.mysql.username", "cytricos");
            String password = plugin.getConfig().getString("storage.mysql.password", "password");

            HikariConfig hikari = new HikariConfig();
            hikari.setPoolName("CytricosRaces-HikariPool");
            hikari.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=utf8");
            hikari.setUsername(username);
            hikari.setPassword(password);

            hikari.setMaximumPoolSize(plugin.getConfig().getInt("storage.mysql.pool.maximum_pool_size", 10));
            hikari.setMinimumIdle(plugin.getConfig().getInt("storage.mysql.pool.minimum_idle", 5));
            hikari.setConnectionTimeout(plugin.getConfig().getLong("storage.mysql.pool.connection_timeout", 30000));
            hikari.setIdleTimeout(plugin.getConfig().getLong("storage.mysql.pool.idle_timeout", 600000));
            hikari.setMaxLifetime(plugin.getConfig().getLong("storage.mysql.pool.max_lifetime", 1800000));

            hikari.addDataSourceProperty("cachePrepStmts", "true");
            hikari.addDataSourceProperty("prepStmtCacheSize", "250");
            hikari.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

            this.dataSource = new HikariDataSource(hikari);

            // Crear tabla si no existe con soporte para persistencia de restricciones v7.0
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {
                String createTable = "CREATE TABLE IF NOT EXISTS " + tablePrefix + "player_data (" +
                        "uuid VARCHAR(36) PRIMARY KEY, " +
                        "race VARCHAR(32) NOT NULL, " +
                        "level INT NOT NULL, " +
                        "free_change_available BOOLEAN NOT NULL, " +
                        "last_change_timestamp BIGINT NOT NULL, " +
                        "flight_ban_until BIGINT DEFAULT 0, " +
                        "transform_cooldown_until BIGINT DEFAULT 0, " +
                        "active_disguise BOOLEAN DEFAULT FALSE, " +
                        "transform_active_until BIGINT DEFAULT 0, " +
                        "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";
                stmt.execute(createTable);
            }

            plugin.getLogger().info("Conexion con MySQL establecida exitosamente con HikariCP.");
        } catch (Throwable t) {
            plugin.getLogger().log(Level.SEVERE, "Error al conectar con MySQL. Aplicando fallback a YAML: " + t.getMessage(), t);
            this.storageType = StorageType.YAML;
            if (this.dataSource != null) {
                this.dataSource.close();
                this.dataSource = null;
            }
        }
    }

    public PlayerData loadPlayerData(UUID uuid) {
        if (storageType == StorageType.MYSQL && dataSource != null) {
            String query = "SELECT race, level, free_change_available, last_change_timestamp, flight_ban_until, transform_cooldown_until, active_disguise, transform_active_until " +
                    "FROM " + tablePrefix + "player_data WHERE uuid = ?;";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, uuid.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        String race = rs.getString("race");
                        int level = rs.getInt("level");
                        boolean freeChange = rs.getBoolean("free_change_available");
                        long timestamp = rs.getLong("last_change_timestamp");

                        PlayerData data = new PlayerData(uuid, race, level, freeChange);
                        data.setLastChangeTimestamp(timestamp);
                        data.setFlightBanUntil(rs.getLong("flight_ban_until"));
                        data.setTransformCooldownUntil(rs.getLong("transform_cooldown_until"));
                        data.setActiveDisguise(rs.getBoolean("active_disguise"));
                        data.setTransformActiveUntil(rs.getLong("transform_active_until"));
                        return data;
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Error al cargar datos MySQL para " + uuid + ", intentando YAML fallback.", e);
            }
        }

        return loadFromYaml(uuid);
    }

    public void savePlayerData(PlayerData data) {
        if (data == null) return;

        if (storageType == StorageType.MYSQL && dataSource != null) {
            String upsert = "INSERT INTO " + tablePrefix + "player_data (uuid, race, level, free_change_available, last_change_timestamp, flight_ban_until, transform_cooldown_until, active_disguise, transform_active_until) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE " +
                    "race = VALUES(race), " +
                    "level = VALUES(level), " +
                    "free_change_available = VALUES(free_change_available), " +
                    "last_change_timestamp = VALUES(last_change_timestamp), " +
                    "flight_ban_until = VALUES(flight_ban_until), " +
                    "transform_cooldown_until = VALUES(transform_cooldown_until), " +
                    "active_disguise = VALUES(active_disguise), " +
                    "transform_active_until = VALUES(transform_active_until);";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(upsert)) {
                stmt.setString(1, data.getUuid().toString());
                stmt.setString(2, data.getRace());
                stmt.setInt(3, data.getLevel());
                stmt.setBoolean(4, data.hasFreeRaceChangeAvailable());
                stmt.setLong(5, data.getLastChangeTimestamp());
                stmt.setLong(6, data.getFlightBanUntil());
                stmt.setLong(7, data.getTransformCooldownUntil());
                stmt.setBoolean(8, data.isActiveDisguise());
                stmt.setLong(9, data.getTransformActiveUntil());
                stmt.executeUpdate();
                return;
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Error al guardar datos MySQL para " + data.getUuid() + ", guardando en YAML fallback.", e);
            }
        }

        saveToYaml(data);
    }

    public void saveAll(Collection<PlayerData> dataCollection) {
        if (dataCollection == null || dataCollection.isEmpty()) return;
        for (PlayerData data : dataCollection) {
            savePlayerData(data);
        }
    }

    private PlayerData loadFromYaml(UUID uuid) {
        File file = new File(dataFolder, uuid.toString() + ".yml");
        if (!file.exists()) {
            String defaultRace = plugin.getConfig().getString("general_settings.default_race", "human");
            boolean freeChange = plugin.getConfig().getBoolean("race_change_settings.allow_one_free_change", true);
            PlayerData newData = new PlayerData(uuid, defaultRace, 1, freeChange);
            saveToYaml(newData);
            return newData;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        String race = config.getString("race", config.getString("race_id", "human"));
        int level = config.getInt("level", 1);
        boolean freeChange = config.getBoolean("free_change_available", true);
        PlayerData data = new PlayerData(uuid, race, level, freeChange);
        data.setLastChangeTimestamp(config.getLong("last_change_timestamp", System.currentTimeMillis()));
        data.setFlightBanUntil(config.getLong("flight_ban_until", 0L));
        data.setTransformCooldownUntil(config.getLong("transform_cooldown_until", 0L));
        data.setActiveDisguise(config.getBoolean("active_disguise", false));
        data.setTransformActiveUntil(config.getLong("transform_active_until", 0L));
        data.setRacialResource(config.getDouble("racial_resource", data.getMaxRacialResource()));

        // Sistemas RPG v7.0
        data.setTalentPoints(config.getInt("rpg.talent_points", Math.max(0, level - 1)));
        data.setUnlockedTalentsRaw(config.getStringList("rpg.unlocked_talents"));
        data.setCompletedQuestsRaw(config.getStringList("rpg.completed_quests"));
        if (config.isConfigurationSection("rpg.quest_progress")) {
            org.bukkit.configuration.ConfigurationSection sec = config.getConfigurationSection("rpg.quest_progress");
            if (sec != null) {
                java.util.Map<String, Integer> qp = new java.util.HashMap<>();
                for (String key : sec.getKeys(false)) {
                    qp.put(key, sec.getInt(key, 0));
                }
                data.setQuestProgressMapRaw(qp);
            }
        }
        data.setUnlockedAchievementsRaw(config.getStringList("rpg.unlocked_achievements"));
        data.setSelectedTitle(config.getString("rpg.selected_title", ""));
        data.setSelectedCosmeticAura(config.getString("rpg.cosmetic_aura", "none"));

        // Ajustes
        data.setSettingParticles(config.getBoolean("settings.particles", true));
        data.setSettingSounds(config.getBoolean("settings.sounds", true));
        data.setSettingActionBar(config.getBoolean("settings.action_bar", true));
        data.setSettingCooldownMsgs(config.getBoolean("settings.cooldown_msgs", true));

        // Estadísticas
        data.setAbilitiesUsed(config.getInt("stats.abilities_used", 0));
        data.setQuestsCompletedCount(config.getInt("stats.quests_completed", 0));
        data.setMobsDefeated(config.getInt("stats.mobs_defeated", 0));
        data.setTimePlayedSeconds(config.getLong("stats.time_played_seconds", 0L));

        return data;
    }

    private void saveToYaml(PlayerData data) {
        File file = new File(dataFolder, data.getUuid().toString() + ".yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        config.set("uuid", data.getUuid().toString());
        config.set("race", data.getRace());
        config.set("race_id", data.getRace());
        config.set("level", data.getLevel());
        config.set("free_change_available", data.hasFreeRaceChangeAvailable());
        config.set("last_change_timestamp", data.getLastChangeTimestamp());
        config.set("flight_ban_until", data.getFlightBanUntil());
        config.set("transform_cooldown_until", data.getTransformCooldownUntil());
        config.set("active_disguise", data.isActiveDisguise());
        config.set("transform_active_until", data.getTransformActiveUntil());
        config.set("racial_resource", data.getRacialResource());

        // Sistemas RPG v7.0
        config.set("rpg.talent_points", data.getTalentPoints());
        config.set("rpg.unlocked_talents", new java.util.ArrayList<>(data.getUnlockedTalents()));
        config.set("rpg.completed_quests", new java.util.ArrayList<>(data.getCompletedQuests()));
        for (java.util.Map.Entry<String, Integer> entry : data.getQuestProgressMap().entrySet()) {
            config.set("rpg.quest_progress." + entry.getKey(), entry.getValue());
        }
        config.set("rpg.unlocked_achievements", new java.util.ArrayList<>(data.getUnlockedAchievements()));
        config.set("rpg.selected_title", data.getSelectedTitle());
        config.set("rpg.cosmetic_aura", data.getSelectedCosmeticAura());

        // Ajustes
        config.set("settings.particles", data.isSettingParticles());
        config.set("settings.sounds", data.isSettingSounds());
        config.set("settings.action_bar", data.isSettingActionBar());
        config.set("settings.cooldown_msgs", data.isSettingCooldownMsgs());

        // Estadísticas
        config.set("stats.abilities_used", data.getAbilitiesUsed());
        config.set("stats.quests_completed", data.getQuestsCompletedCount());
        config.set("stats.mobs_defeated", data.getMobsDefeated());
        config.set("stats.time_played_seconds", data.getTimePlayedSeconds());

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Error al guardar userdata YAML para " + data.getUuid(), e);
        }
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            plugin.getLogger().info("Pool de conexiones MySQL (HikariCP) cerrado correctamente.");
        }
    }

    public StorageType getStorageType() {
        return storageType;
    }

    public boolean isMySQL() {
        return storageType == StorageType.MYSQL;
    }
}

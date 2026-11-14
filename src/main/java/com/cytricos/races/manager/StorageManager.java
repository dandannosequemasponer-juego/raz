package com.cytricos.races.manager;

import com.cytricos.races.CytricosRaces;
import com.cytricos.races.model.PlayerData;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;
import java.util.logging.Level;

/**
 * Gestor unificado de almacenamiento persistente dual (YAML / MySQL) para CYTRICOS RPG.
 * Enforce estricto de UTF-8 (useUnicode=true&characterEncoding=UTF-8 en MySQL y InputStreamReader/OutputStreamWriter en YAML).
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
            plugin.getLogger().info("Almacenamiento configurado en modo YAML UTF-8 (userdata/).");
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
            hikari.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database
                    + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&useUnicode=true&characterEncoding=UTF-8");
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
            hikari.addDataSourceProperty("useUnicode", "true");
            hikari.addDataSourceProperty("characterEncoding", "UTF-8");

            this.dataSource = new HikariDataSource(hikari);

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
                        "rpg_payload MEDIUMTEXT NULL, " +
                        "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;";
                stmt.execute(createTable);
                try {
                    stmt.execute("ALTER TABLE " + tablePrefix + "player_data ADD COLUMN rpg_payload MEDIUMTEXT NULL;");
                } catch (SQLException ignored) {
                }
            }

            plugin.getLogger().info("Conexión MySQL (UTF-8mb4) establecida exitosamente con HikariCP.");
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
            String query = "SELECT race, level, free_change_available, last_change_timestamp, flight_ban_until, transform_cooldown_until, active_disguise, transform_active_until, rpg_payload " +
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
                        deserializeRpgPayload(data, rs.getString("rpg_payload"));
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
            String upsert = "INSERT INTO " + tablePrefix + "player_data (uuid, race, level, free_change_available, last_change_timestamp, flight_ban_until, transform_cooldown_until, active_disguise, transform_active_until, rpg_payload) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE " +
                    "race = VALUES(race), " +
                    "level = VALUES(level), " +
                    "free_change_available = VALUES(free_change_available), " +
                    "last_change_timestamp = VALUES(last_change_timestamp), " +
                    "flight_ban_until = VALUES(flight_ban_until), " +
                    "transform_cooldown_until = VALUES(transform_cooldown_until), " +
                    "active_disguise = VALUES(active_disguise), " +
                    "transform_active_until = VALUES(transform_active_until), " +
                    "rpg_payload = VALUES(rpg_payload);";
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
                stmt.setString(10, serializeRpgPayload(data));
                stmt.executeUpdate();
                return;
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Error al guardar datos MySQL para " + data.getUuid() + ", guardando en YAML fallback.", e);
            }
        }

        saveToYaml(data);
    }

    private String serializeRpgPayload(PlayerData data) {
        YamlConfiguration yml = new YamlConfiguration();
        yml.set("racial_resource", data.getRacialResource());
        yml.set("max_racial_resource", data.getMaxRacialResource());
        yml.set("talent_points", data.getTalentPoints());
        yml.set("mob_kills_counter", data.getMobKillsCounter());
        yml.set("unlocked_talents", new ArrayList<>(data.getUnlockedTalents()));
        yml.set("completed_quests", new ArrayList<>(data.getCompletedQuests()));
        for (Map.Entry<String, Integer> entry : data.getQuestProgressMap().entrySet()) {
            yml.set("quest_progress." + entry.getKey(), entry.getValue());
        }
        yml.set("unlocked_achievements", new ArrayList<>(data.getUnlockedAchievements()));
        yml.set("selected_title", data.getSelectedTitle());
        yml.set("cosmetic_aura", data.getSelectedCosmeticAura());
        yml.set("selected_wings", data.getSelectedWings());
        yml.set("selected_head_cosmetic", data.getSelectedHeadCosmetic());
        yml.set("first_person_wings_hidden", data.isFirstPersonWingsHidden());
        yml.set("backpack_b64", data.getBackpackBase64());
        yml.set("unlocked_cosmetics", new ArrayList<>(data.getUnlockedCosmetics()));
        yml.set("settings.particles", data.isSettingParticles());
        yml.set("settings.sounds", data.isSettingSounds());
        yml.set("settings.action_bar", data.isSettingActionBar());
        yml.set("settings.cooldown_msgs", data.isSettingCooldownMsgs());
        yml.set("stats.abilities_used", data.getAbilitiesUsed());
        yml.set("stats.quests_completed", data.getQuestsCompletedCount());
        yml.set("stats.mobs_defeated", data.getMobsDefeated());
        yml.set("stats.time_played_seconds", data.getTimePlayedSeconds());
        return yml.saveToString();
    }

    private void deserializeRpgPayload(PlayerData data, String payload) {
        if (payload == null || payload.isEmpty()) return;
        try {
            YamlConfiguration yml = new YamlConfiguration();
            yml.loadFromString(payload);
            data.setMaxRacialResource(yml.getDouble("max_racial_resource", data.getMaxRacialResource()));
            data.setRacialResource(yml.getDouble("racial_resource", data.getMaxRacialResource()));
            data.setTalentPoints(yml.getInt("talent_points", data.getTalentPoints()));
            data.setMobKillsCounter(yml.getInt("mob_kills_counter", 0));
            data.setUnlockedTalentsRaw(yml.getStringList("unlocked_talents"));
            data.setCompletedQuestsRaw(yml.getStringList("completed_quests"));
            if (yml.isConfigurationSection("quest_progress")) {
                ConfigurationSection sec = yml.getConfigurationSection("quest_progress");
                if (sec != null) {
                    Map<String, Integer> qp = new HashMap<>();
                    for (String k : sec.getKeys(false)) {
                        qp.put(k, sec.getInt(k, 0));
                    }
                    data.setQuestProgressMapRaw(qp);
                }
            }
            data.setUnlockedAchievementsRaw(yml.getStringList("unlocked_achievements"));
            data.setSelectedTitle(yml.getString("selected_title", ""));
            data.setSelectedCosmeticAura(yml.getString("cosmetic_aura", "none"));
            data.setSelectedWings(yml.getString("selected_wings", "none"));
            data.setSelectedHeadCosmetic(yml.getString("selected_head_cosmetic", "none"));
            data.setFirstPersonWingsHidden(yml.getBoolean("first_person_wings_hidden", true));
            data.setBackpackBase64(yml.getString("backpack_b64", ""));
            data.setUnlockedCosmeticsRaw(yml.getStringList("unlocked_cosmetics"));
            data.setSettingParticles(yml.getBoolean("settings.particles", true));
            data.setSettingSounds(yml.getBoolean("settings.sounds", true));
            data.setSettingActionBar(yml.getBoolean("settings.action_bar", true));
            data.setSettingCooldownMsgs(yml.getBoolean("settings.cooldown_msgs", true));
            data.setAbilitiesUsed(yml.getInt("stats.abilities_used", 0));
            data.setQuestsCompletedCount(yml.getInt("stats.quests_completed", 0));
            data.setMobsDefeated(yml.getInt("stats.mobs_defeated", 0));
            data.setTimePlayedSeconds(yml.getLong("stats.time_played_seconds", 0L));
        } catch (Exception ignored) {
        }
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
            String defaultRace = plugin.getConfig().getString("general_settings.default_race", "none");
            boolean freeChange = plugin.getConfig().getBoolean("race_change_settings.allow_one_free_change", true);
            PlayerData newData = new PlayerData(uuid, defaultRace, 1, freeChange);
            saveToYaml(newData);
            return newData;
        }

        YamlConfiguration config = new YamlConfiguration();
        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            config.load(reader);
        } catch (Exception ex) {
            plugin.getLogger().log(Level.SEVERE, "Error cargando userdata UTF-8 para " + uuid, ex);
        }

        String race = config.getString("race", config.getString("race_id", "none"));
        int level = config.getInt("level", 1);
        boolean freeChange = config.getBoolean("free_change_available", true);
        PlayerData data = new PlayerData(uuid, race, level, freeChange);
        data.setLastChangeTimestamp(config.getLong("last_change_timestamp", System.currentTimeMillis()));
        data.setFlightBanUntil(config.getLong("flight_ban_until", 0L));
        data.setTransformCooldownUntil(config.getLong("transform_cooldown_until", 0L));
        data.setActiveDisguise(config.getBoolean("active_disguise", false));
        data.setTransformActiveUntil(config.getLong("transform_active_until", 0L));
        data.setMaxRacialResource(config.getDouble("max_racial_resource", data.getMaxRacialResource()));
        data.setRacialResource(config.getDouble("racial_resource", data.getMaxRacialResource()));

        data.setTalentPoints(config.getInt("rpg.talent_points", 0));
        data.setMobKillsCounter(config.getInt("rpg.mob_kills_counter", 0));
        data.setUnlockedTalentsRaw(config.getStringList("rpg.unlocked_talents"));
        data.setCompletedQuestsRaw(config.getStringList("rpg.completed_quests"));
        if (config.isConfigurationSection("rpg.quest_progress")) {
            ConfigurationSection sec = config.getConfigurationSection("rpg.quest_progress");
            if (sec != null) {
                Map<String, Integer> qp = new HashMap<>();
                for (String key : sec.getKeys(false)) {
                    qp.put(key, sec.getInt(key, 0));
                }
                data.setQuestProgressMapRaw(qp);
            }
        }
        data.setUnlockedAchievementsRaw(config.getStringList("rpg.unlocked_achievements"));
        data.setSelectedTitle(config.getString("rpg.selected_title", ""));
        data.setSelectedCosmeticAura(config.getString("rpg.cosmetic_aura", "none"));
        data.setSelectedWings(config.getString("rpg.selected_wings", "none"));
        data.setSelectedHeadCosmetic(config.getString("rpg.selected_head_cosmetic", "none"));
        data.setFirstPersonWingsHidden(config.getBoolean("rpg.first_person_wings_hidden", true));
        data.setBackpackBase64(config.getString("rpg.backpack_b64", ""));
        data.setUnlockedCosmeticsRaw(config.getStringList("rpg.unlocked_cosmetics"));

        data.setSettingParticles(config.getBoolean("settings.particles", true));
        data.setSettingSounds(config.getBoolean("settings.sounds", true));
        data.setSettingActionBar(config.getBoolean("settings.action_bar", true));
        data.setSettingCooldownMsgs(config.getBoolean("settings.cooldown_msgs", true));

        data.setAbilitiesUsed(config.getInt("stats.abilities_used", 0));
        data.setQuestsCompletedCount(config.getInt("stats.quests_completed", 0));
        data.setMobsDefeated(config.getInt("stats.mobs_defeated", 0));
        data.setTimePlayedSeconds(config.getLong("stats.time_played_seconds", 0L));

        return data;
    }

    private void saveToYaml(PlayerData data) {
        File file = new File(dataFolder, data.getUuid().toString() + ".yml");
        YamlConfiguration config = new YamlConfiguration();

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
        config.set("max_racial_resource", data.getMaxRacialResource());

        config.set("rpg.talent_points", data.getTalentPoints());
        config.set("rpg.mob_kills_counter", data.getMobKillsCounter());
        config.set("rpg.unlocked_talents", new ArrayList<>(data.getUnlockedTalents()));
        config.set("rpg.completed_quests", new ArrayList<>(data.getCompletedQuests()));
        for (Map.Entry<String, Integer> entry : data.getQuestProgressMap().entrySet()) {
            config.set("rpg.quest_progress." + entry.getKey(), entry.getValue());
        }
        config.set("rpg.unlocked_achievements", new ArrayList<>(data.getUnlockedAchievements()));
        config.set("rpg.selected_title", data.getSelectedTitle());
        config.set("rpg.cosmetic_aura", data.getSelectedCosmeticAura());
        config.set("rpg.selected_wings", data.getSelectedWings());
        config.set("rpg.selected_head_cosmetic", data.getSelectedHeadCosmetic());
        config.set("rpg.first_person_wings_hidden", data.isFirstPersonWingsHidden());
        config.set("rpg.backpack_b64", data.getBackpackBase64());
        config.set("rpg.unlocked_cosmetics", new ArrayList<>(data.getUnlockedCosmetics()));

        config.set("settings.particles", data.isSettingParticles());
        config.set("settings.sounds", data.isSettingSounds());
        config.set("settings.action_bar", data.isSettingActionBar());
        config.set("settings.cooldown_msgs", data.isSettingCooldownMsgs());

        config.set("stats.abilities_used", data.getAbilitiesUsed());
        config.set("stats.quests_completed", data.getQuestsCompletedCount());
        config.set("stats.mobs_defeated", data.getMobsDefeated());
        config.set("stats.time_played_seconds", data.getTimePlayedSeconds());

        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
            writer.write(config.saveToString());
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Error al guardar userdata YAML UTF-8 para " + data.getUuid(), e);
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

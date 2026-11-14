package com.cytricos.races.manager;

import com.cytricos.races.CytricosRaces;
import com.cytricos.races.util.ColorUtils;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

/**
 * Gestor de configuraciones YAML (config.yml, races.yml, relics.yml) para CYTRICOS RPG.
 * Enforce estricto de UTF-8 en toda lectura/escritura y recarga en vivo sin reinicios.
 * Lead Developers: Dandan_13 & Wolfit12
 */
public class ConfigManager {
    private final CytricosRaces plugin;
    private FileConfiguration racesConfig = null;
    private File racesConfigFile = null;
    private FileConfiguration relicsConfig = null;
    private File relicsConfigFile = null;

    public ConfigManager(CytricosRaces plugin) {
        this.plugin = plugin;
    }

    public void setup() {
        plugin.saveDefaultConfig();
        saveDefaultRacesConfig();
        saveDefaultRelicsConfig();
        reloadAll();
    }

    public void reloadAll() {
        plugin.reloadConfig();
        reloadRacesConfig();
        reloadRelicsConfig();
    }

    public void reloadRacesConfig() {
        if (racesConfigFile == null) {
            racesConfigFile = new File(plugin.getDataFolder(), "races.yml");
        }
        racesConfig = loadUtf8Yaml(racesConfigFile);

        InputStream defaultStream = plugin.getResource("races.yml");
        if (defaultStream != null) {
            try (InputStreamReader reader = new InputStreamReader(defaultStream, StandardCharsets.UTF_8)) {
                YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(reader);
                racesConfig.setDefaults(defaultConfig);
            } catch (IOException ex) {
                plugin.getLogger().log(Level.WARNING, "No se pudo leer races.yml por defecto en UTF-8", ex);
            }
        }
    }

    public FileConfiguration getRacesConfig() {
        if (racesConfig == null) {
            reloadRacesConfig();
        }
        return racesConfig;
    }

    public void saveRacesConfig() {
        if (racesConfig == null || racesConfigFile == null) {
            return;
        }
        saveUtf8Yaml(racesConfig, racesConfigFile);
    }

    public void saveDefaultRacesConfig() {
        if (racesConfigFile == null) {
            racesConfigFile = new File(plugin.getDataFolder(), "races.yml");
        }
        if (!racesConfigFile.exists()) {
            plugin.saveResource("races.yml", false);
        }
    }

    public void reloadRelicsConfig() {
        if (relicsConfigFile == null) {
            relicsConfigFile = new File(plugin.getDataFolder(), "relics.yml");
        }
        relicsConfig = loadUtf8Yaml(relicsConfigFile);

        InputStream defaultStream = plugin.getResource("relics.yml");
        if (defaultStream != null) {
            try (InputStreamReader reader = new InputStreamReader(defaultStream, StandardCharsets.UTF_8)) {
                YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(reader);
                relicsConfig.setDefaults(defaultConfig);
            } catch (IOException ex) {
                plugin.getLogger().log(Level.WARNING, "No se pudo leer relics.yml por defecto en UTF-8", ex);
            }
        }
    }

    public FileConfiguration getRelicsConfig() {
        if (relicsConfig == null) {
            reloadRelicsConfig();
        }
        return relicsConfig;
    }

    public void saveDefaultRelicsConfig() {
        if (relicsConfigFile == null) {
            relicsConfigFile = new File(plugin.getDataFolder(), "relics.yml");
        }
        if (!relicsConfigFile.exists() && plugin.getResource("relics.yml") != null) {
            plugin.saveResource("relics.yml", false);
        }
    }

    private YamlConfiguration loadUtf8Yaml(File file) {
        YamlConfiguration cfg = new YamlConfiguration();
        if (file == null || !file.exists()) {
            return cfg;
        }
        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            cfg.load(reader);
        } catch (Exception ex) {
            plugin.getLogger().log(Level.SEVERE, "Error cargando YAML UTF-8: " + file.getName(), ex);
        }
        return cfg;
    }

    private void saveUtf8Yaml(FileConfiguration cfg, File file) {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
            writer.write(cfg.saveToString());
        } catch (IOException ex) {
            plugin.getLogger().log(Level.SEVERE, "No se pudo guardar YAML UTF-8 en " + file.getName(), ex);
        }
    }

    public String getMessage(String path, String def) {
        String msg = plugin.getConfig().getString("messages." + path, def);
        return color(msg);
    }

    public static String color(String message) {
        return ColorUtils.color(message);
    }

    public boolean isGlobalChangesEnabled() {
        return plugin.getConfig().getBoolean("race_change_settings.global_changes_enabled", true);
    }

    public void setGlobalChangesEnabled(boolean enabled) {
        plugin.getConfig().set("race_change_settings.global_changes_enabled", enabled);
        plugin.saveConfig();
    }

    public double getRaceChangeCost() {
        return plugin.getConfig().getDouble("race_change_settings.economy_change.cost", 50000.0);
    }

    public void setRaceChangeCost(double cost) {
        plugin.getConfig().set("race_change_settings.economy_change.cost", Math.max(0.0, cost));
        plugin.saveConfig();
    }

    public double getTransformationMaxFlightY() {
        return plugin.getConfig().getDouble("transformations.max-flight-y", 160.0);
    }

    public void setTransformationMaxFlightY(double yLimit) {
        plugin.getConfig().set("transformations.max-flight-y", Math.max(20.0, yLimit));
        plugin.saveConfig();
    }

    public int getTalentKillsPerPoint() {
        return Math.max(1, plugin.getConfig().getInt("talent_points.kills_per_point", 64));
    }

    public void setTalentKillsPerPoint(int kills) {
        plugin.getConfig().set("talent_points.kills_per_point", Math.max(1, kills));
        plugin.saveConfig();
    }

    public double getTalentShardDropChance() {
        return Math.max(0.0, Math.min(1.0, plugin.getConfig().getDouble("talent_shard.drop_chance", 0.03)));
    }

    public void setTalentShardDropChance(double chance) {
        plugin.getConfig().set("talent_shard.drop_chance", Math.max(0.0, Math.min(1.0, chance)));
        plugin.saveConfig();
    }

    public boolean isEncyclopediaUseGlyphs() {
        return plugin.getConfig().getBoolean("encyclopedia.use_glyphs", false);
    }

    public boolean isInjectLuckPermsChatSuffix() {
        return plugin.getConfig().getBoolean("general_settings.inject_luckperms_chat_suffix", false);
    }

    public String getDefaultRaceId() {
        return plugin.getConfig().getString("general_settings.default_race", "none");
    }
}

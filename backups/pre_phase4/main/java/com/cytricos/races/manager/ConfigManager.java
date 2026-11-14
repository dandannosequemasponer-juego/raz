package com.cytricos.races.manager;

import com.cytricos.races.CytricosRaces;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

/**
 * Gestor de configuraciones YAML (config.yml y races.yml) para CYTRICOS RPG.
 * Permite recarga en vivo sin reinicios de servidor.
 * Lead Developers: Dandan_13 & Wolfit12
 */
public class ConfigManager {
    private final CytricosRaces plugin;
    private FileConfiguration racesConfig = null;
    private File racesConfigFile = null;

    public ConfigManager(CytricosRaces plugin) {
        this.plugin = plugin;
    }

    public void setup() {
        plugin.saveDefaultConfig();
        saveDefaultRacesConfig();
    }

    public void reloadAll() {
        plugin.reloadConfig();
        reloadRacesConfig();
    }

    public void reloadRacesConfig() {
        if (racesConfigFile == null) {
            racesConfigFile = new File(plugin.getDataFolder(), "races.yml");
        }
        racesConfig = YamlConfiguration.loadConfiguration(racesConfigFile);

        InputStream defaultStream = plugin.getResource("races.yml");
        if (defaultStream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defaultStream, StandardCharsets.UTF_8));
            racesConfig.setDefaults(defaultConfig);
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
        try {
            getRacesConfig().save(racesConfigFile);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.SEVERE, "No se pudo guardar " + racesConfigFile, ex);
        }
    }

    public void saveDefaultRacesConfig() {
        if (racesConfigFile == null) {
            racesConfigFile = new File(plugin.getDataFolder(), "races.yml");
        }
        if (!racesConfigFile.exists()) {
            plugin.saveResource("races.yml", false);
        }
    }

    public String getMessage(String path, String def) {
        String msg = plugin.getConfig().getString("messages." + path, def);
        return color(msg);
    }

    public static String color(String message) {
        if (message == null) return "";
        return ChatColor.translateAlternateColorCodes('&', message);
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
        plugin.getConfig().set("race_change_settings.economy_change.cost", cost);
        plugin.saveConfig();
    }
}

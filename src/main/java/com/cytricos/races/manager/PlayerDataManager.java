package com.cytricos.races.manager;

import com.cytricos.races.CytricosRaces;
import com.cytricos.races.model.PlayerData;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestor de datos de jugadores en memoria y puente con StorageManager (YAML/MySQL).
 * Lead Developers: Dandan_13 & Wolfit12
 */
public class PlayerDataManager {
    private final CytricosRaces plugin;
    private final Map<UUID, PlayerData> cache = new ConcurrentHashMap<>();

    public PlayerDataManager(CytricosRaces plugin) {
        this.plugin = plugin;
    }

    public PlayerData get(UUID uuid) {
        return cache.computeIfAbsent(uuid, this::loadData);
    }

    public PlayerData loadData(UUID uuid) {
        if (plugin.getStorageManager() != null) {
            return plugin.getStorageManager().loadPlayerData(uuid);
        }
        String defaultRace = plugin.getConfig().getString("general_settings.default_race", "none");
        boolean freeChange = plugin.getConfig().getBoolean("race_change_settings.allow_one_free_change", true);
        return new PlayerData(uuid, defaultRace, 1, freeChange);
    }

    public void save(PlayerData data) {
        if (data == null) return;
        if (plugin.getStorageManager() != null) {
            plugin.getStorageManager().savePlayerData(data);
        }
    }

    public void saveToFile(PlayerData data) {
        save(data);
    }

    public void saveAll() {
        if (plugin.getStorageManager() != null) {
            plugin.getStorageManager().saveAll(cache.values());
        }
    }

    public void unload(UUID uuid) {
        PlayerData data = cache.remove(uuid);
        if (data != null) {
            save(data);
        }
    }

    public void resetPlayer(UUID uuid) {
        PlayerData data = get(uuid);
        String defaultRace = plugin.getConfig().getString("general_settings.default_race", "none");
        data.setRace(defaultRace);
        data.wipeRaceProgress();
        data.setFreeRaceChangeAvailable(true);
        save(data);
    }

    public Map<UUID, PlayerData> getCache() {
        return cache;
    }
}

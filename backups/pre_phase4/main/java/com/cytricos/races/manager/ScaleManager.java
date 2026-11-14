package com.cytricos.races.manager;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Gestor de escalado físico en tiempo real y dimensiones de hitbox (Attribute.GENERIC_SCALE).
 * Totalmente compatible con Paper / Spigot 1.20.5+ y 1.21.
 * Lead Developers: Dandan_13 & Wolfit12
 */
public class ScaleManager {
    private static final Logger LOGGER = Logger.getLogger("CytricosRaces-Scale");

    /**
     * Aplica la escala física y hitbox al jugador según su raza.
     * Ejemplo: 0.50 (Hada), 0.60 (Duende), 0.75 (Enano), 1.00 (Humano), 1.20 (Orco), 1.35 (Gigante).
     *
     * @param player Jugador objetivo
     * @param scaleValue Valor numérico de escala (0.1 a 3.0)
     */
    public static void setPlayerRaceScale(Player player, double scaleValue) {
        if (player == null || !player.isOnline()) return;

        double clampedScale = Math.max(0.1, Math.min(3.0, scaleValue));

        try {
            // Intento 1: Paper / Spigot 1.20.5+ directo via Attribute.GENERIC_SCALE
            Attribute scaleAttr = null;
            try {
                scaleAttr = Attribute.valueOf("GENERIC_SCALE");
            } catch (IllegalArgumentException ignored) {
                // Posible versión 1.21.2+ con nombres de registro
            }

            if (scaleAttr != null) {
                AttributeInstance instance = player.getAttribute(scaleAttr);
                if (instance != null) {
                    instance.setBaseValue(clampedScale);
                    return;
                }
            }

            // Intento 2: Registro de Bukkit via NamespacedKey minecraft:generic.scale
            try {
                Attribute regAttr = Registry.ATTRIBUTE.get(NamespacedKey.minecraft("generic.scale"));
                if (regAttr != null) {
                    AttributeInstance instance = player.getAttribute(regAttr);
                    if (instance != null) {
                        instance.setBaseValue(clampedScale);
                        return;
                    }
                }
            } catch (Throwable ignored) {
                // Fallback silencioso si no está disponible
            }

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "No se pudo aplicar GENERIC_SCALE para " + player.getName() + ": " + e.getMessage());
        }
    }

    /**
     * Obtiene la escala física actual del jugador.
     */
    public static double getPlayerRaceScale(Player player) {
        if (player == null) return 1.0;
        try {
            Attribute scaleAttr = Attribute.valueOf("GENERIC_SCALE");
            AttributeInstance instance = player.getAttribute(scaleAttr);
            if (instance != null) {
                return instance.getBaseValue();
            }
        } catch (Throwable ignored) {}
        return 1.0;
    }
}

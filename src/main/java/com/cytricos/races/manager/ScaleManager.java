package com.cytricos.races.manager;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Gestor de escalado físico en tiempo real y dimensiones de hitbox (Attribute.SCALE).
 * Compatible con Paper 26.2 (Java 25).
 * Lead Developers: Dandan_13 & Wolfit12
 */
public class ScaleManager {
    private static final Logger LOGGER = Logger.getLogger("CytricosRaces-Scale");

    private static Attribute resolveScaleAttribute() {
        try {
            return Attribute.SCALE;
        } catch (Throwable ignored) {
            return Registry.ATTRIBUTE.get(NamespacedKey.minecraft("scale"));
        }
    }

    /**
     * Aplica la escala física y hitbox al jugador según su raza.
     * Ejemplo: 0.50 (Hada), 0.60 (Duende), 0.75 (Enano), 1.00 (Humano), 1.20 (Orco), 1.35 (Gigante).
     */
    public static void setPlayerRaceScale(Player player, double scaleValue) {
        if (player == null || !player.isOnline()) return;

        double clampedScale = Math.max(0.1, Math.min(3.0, scaleValue));

        try {
            Attribute scaleAttr = resolveScaleAttribute();
            if (scaleAttr != null) {
                AttributeInstance instance = player.getAttribute(scaleAttr);
                if (instance != null) {
                    instance.setBaseValue(clampedScale);
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "No se pudo aplicar Attribute.SCALE para " + player.getName() + ": " + e.getMessage());
        }
    }

    /**
     * Obtiene la escala física actual del jugador.
     */
    public static double getPlayerRaceScale(Player player) {
        if (player == null) return 1.0;
        try {
            Attribute scaleAttr = resolveScaleAttribute();
            if (scaleAttr != null) {
                AttributeInstance instance = player.getAttribute(scaleAttr);
                if (instance != null) {
                    return instance.getBaseValue();
                }
            }
        } catch (Throwable ignored) {}
        return 1.0;
    }
}

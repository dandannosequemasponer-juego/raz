package com.cytricos.races.hook;

import com.cytricos.races.CytricosRaces;
import com.cytricos.races.model.PlayerData;
import com.cytricos.races.model.Race;
import com.cytricos.races.model.RaceLevel;
import com.cytricos.races.util.ColorUtils;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Expansión oficial de PlaceholderAPI para CYTRICOS RPG (v7.0).
 * Identificador: %cytricosraces_<variable>%
 *
 * Variables disponibles:
 * - %cytricosraces_race% -> Nombre legible de la raza activa.
 * - %cytricosraces_level% -> Nivel actual (1 al 6).
 * - %cytricosraces_title% -> Subprefijo/Título de chat coloreado del nivel.
 * - %cytricosraces_scale% -> Escala física actual del jugador (Attribute.GENERIC_SCALE).
 * - %cytricosraces_flight_status% -> ALLOWED, CEILING_WARN, o RESTRICTED (MM:SS).
 * - %cytricosraces_cooldown% -> Tiempo restante de cooldown de transformación / habilidad.
 * - %cytricosraces_transform_status% -> HUMAN o TRANSFORMED (XXs / INFINITE).
 *
 * Lead Developers: Dandan_13 & Wolfit12
 */
public class CytricosRacesPAPI extends PlaceholderExpansion {
    private final CytricosRaces plugin;

    public CytricosRacesPAPI(CytricosRaces plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "cytricosraces";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Dandan_13 & Wolfit12";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) return "";

        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        if (data == null) return "";

        String p = params.toLowerCase();

        switch (p) {
            case "race", "raza", "race_display" -> {
                Race race = plugin.getRaceManager().getRace(data.getRace());
                return race != null ? ColorUtils.color(race.getDisplayName()) : "Humano";
            }
            case "race_id", "id" -> {
                return data.getRace();
            }
            case "level", "nivel" -> {
                return String.valueOf(data.getLevel());
            }
            case "level_name", "nombre_nivel" -> {
                Race race = plugin.getRaceManager().getRace(data.getRace());
                if (race != null) {
                    RaceLevel lvl = race.getLevel(data.getLevel());
                    if (lvl != null && lvl.getName() != null) {
                        return ColorUtils.color(lvl.getName());
                    }
                }
                return "Nivel " + data.getLevel();
            }
            case "title", "subprefix", "suffix", "sufijo", "titulo" -> {
                if (!data.getSelectedTitle().isEmpty()) {
                    return ColorUtils.color(data.getSelectedTitle());
                }
                Race race = plugin.getRaceManager().getRace(data.getRace());
                if (race != null) {
                    RaceLevel lvl = race.getLevel(data.getLevel());
                    if (lvl != null && lvl.getSubprefix() != null) {
                        return ColorUtils.color(lvl.getSubprefix());
                    }
                }
                return "";
            }
            case "talent_points", "puntos_talento" -> {
                return String.valueOf(data.getTalentPoints());
            }
            case "talents_count", "talentos_desbloqueados" -> {
                return String.valueOf(data.getUnlockedTalents().size());
            }
            case "quests_completed", "misiones_completadas" -> {
                return String.valueOf(data.getQuestsCompletedCount());
            }
            case "achievements_count", "logros_desbloqueados" -> {
                return String.valueOf(data.getUnlockedAchievements().size());
            }
            case "cosmetic_aura", "aura" -> {
                return data.getSelectedCosmeticAura();
            }
            case "scale", "escala" -> {
                try {
                    AttributeInstance attr = player.getAttribute(Attribute.GENERIC_SCALE);
                    if (attr != null) {
                        return String.format(java.util.Locale.US, "%.2f", attr.getValue());
                    }
                } catch (Throwable ignored) {}
                Race race = plugin.getRaceManager().getRace(data.getRace());
                return race != null ? String.valueOf(race.getScale()) : "1.00";
            }
            case "flight_status", "estado_vuelo" -> {
                if (data.isFlightBanned()) {
                    long sec = data.getFlightBanRemainingSeconds();
                    return "RESTRICTED (" + String.format("%02d:%02d", sec / 60, sec % 60) + ")";
                }
                if (player.isFlying()) {
                    int groundY = player.getWorld().getHighestBlockYAt(player.getLocation());
                    double alt = player.getLocation().getY() - groundY;
                    double max = plugin.getConfig().getDouble("flight_settings." + data.getRace() + "_max_height", 16.0);
                    if (alt > max) {
                        return "CEILING_WARN";
                    }
                    return "ALLOWED";
                }
                return player.getAllowFlight() ? "ALLOWED" : "GROUNDED";
            }
            case "cooldown", "tiempo_cooldown" -> {
                if (data.isTransformOnCooldown()) {
                    return data.getTransformCooldownRemainingSeconds() + "s";
                }
                return "READY";
            }
            case "transform_status", "estado_transformacion" -> {
                if (data.isTransformed()) {
                    long rem = data.getTransformRemainingSeconds();
                    if (rem == -1L) {
                        return "TRANSFORMED (INFINITE)";
                    }
                    return "TRANSFORMED (" + rem + "s)";
                }
                return "HUMAN";
            }
            case "free_change", "cambio_gratis" -> {
                return data.hasFreeRaceChangeAvailable() ? "true" : "false";
            }
            case "resource", "recurso" -> {
                return String.valueOf((int) data.getRacialResource());
            }
            case "resource_max", "recurso_max" -> {
                return String.valueOf((int) data.getMaxRacialResource());
            }
            case "resource_name", "nombre_recurso" -> {
                return data.getResourceName();
            }
            case "experience", "experiencia" -> {
                return String.valueOf(player.getTotalExperience());
            }
            case "next_level", "siguiente_nivel" -> {
                return data.getLevel() < 6 ? String.valueOf(data.getLevel() + 1) : "MAX";
            }
            case "unlocked_abilities", "habilidades_desbloqueadas" -> {
                return String.valueOf(Math.min(6, data.getLevel() * 2));
            }
            default -> {
                return null;
            }
        }
    }
}

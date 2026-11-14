package com.cytricos.races.manager;

import com.cytricos.races.CytricosRaces;
import com.cytricos.races.model.PlayerData;
import com.cytricos.races.util.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestor Unificado de Metamorfosis / Transformación Racial (Fase 7) para CYTRICOS RPG.
 * - Inicio mediante Shift + Clic Derecho una sola vez, manteniendo Shift pulsado continuamente 6.0s.
 * - Cancelación instantánea si el jugador suelta Shift antes de completar los 6.0s.
 * - Duraciones configurables por nivel (Lv1: 20s, Lv2: 35s, Lv3: 50s, Lv4: 75s, Lv5: 110s, Lv6: Infinito).
 * - Límite configurable de altitud aérea en transformación (transformations.max-flight-y) con empuje suave.
 * - Funciona completamente tanto con LibsDisguises como sin él.
 *
 * Lead Developers: Dandan_13 & Wolfit12
 */
public class TransformationManager {
    private static final long CHANNEL_DURATION_MS = 6000L;

    private final CytricosRaces plugin;
    private final Map<UUID, Long> channelingMap = new ConcurrentHashMap<>();
    private BukkitTask channelTask;
    private BukkitTask expirationAndCeilingTask;

    public TransformationManager(CytricosRaces plugin) {
        this.plugin = plugin;
    }

    public void startTasks() {
        stopAllTasks();

        // Tarea de canalización cada 2 ticks (0.1s)
        channelTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long now = System.currentTimeMillis();
            for (Map.Entry<UUID, Long> entry : channelingMap.entrySet()) {
                UUID uuid = entry.getKey();
                Player player = Bukkit.getPlayer(uuid);
                if (player == null || !player.isOnline() || player.isDead()) {
                    channelingMap.remove(uuid);
                    continue;
                }
                if (!player.isSneaking()) {
                    cancelChanneling(uuid, "&c&l[METAMORFOSIS] &7Soltaste Shift antes de tiempo. Canalización cancelada.");
                    continue;
                }

                long elapsed = now - entry.getValue();
                if (elapsed < CHANNEL_DURATION_MS) {
                    double progress = (double) elapsed / CHANNEL_DURATION_MS;
                    int filled = Math.min(12, Math.max(0, (int) Math.round(progress * 12)));
                    String bar = "&6" + "█".repeat(filled) + "&8" + "░".repeat(12 - filled);
                    double remSec = Math.max(0.0, (CHANNEL_DURATION_MS - elapsed) / 1000.0);
                    ColorUtils.sendActionBar(player, "&5&lCanalizando Metamorfosis: &8[" + bar + "&8] &e" + String.format("%.1f", remSec) + "s");
                    PlayerData data = plugin.getPlayerDataManager().get(uuid);
                    if (data == null || data.isSettingParticles()) {
                        player.getWorld().spawnParticle(Particle.PORTAL, player.getLocation().add(0, 1.0, 0), 4, 0.25, 0.35, 0.25, 0.04);
                    }
                    if (elapsed % 1000L < 120L && (data == null || data.isSettingSounds())) {
                        player.playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.35f, 1.0f + (float) (progress * 0.6));
                    }
                } else {
                    channelingMap.remove(uuid);
                    toggleTransformation(player);
                }
            }
        }, 4L, 2L);

        // Tarea de expiración y control de techo Y para transformaciones cada 10 ticks (0.5s)
        expirationAndCeilingTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long now = System.currentTimeMillis();
            double maxFlightY = plugin.getConfigManager().getTransformationMaxFlightY();

            for (Player player : Bukkit.getOnlinePlayers()) {
                PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
                if (data == null || !data.isTransformed()) continue;

                // Verificar expiración temporal (Lv 1..5)
                if (data.getTransformActiveUntil() > 0 && now >= data.getTransformActiveUntil()) {
                    expireTransformation(player, data);
                    continue;
                }

                // Refuerzo de efectos sutiles mientras esté transformado
                if (isAerialForm(data.getRace(), data.getLevel())) {
                    if (player.getGameMode() != GameMode.SPECTATOR && player.getGameMode() != GameMode.CREATIVE) {
                        if (!data.isFlightBanned() && !player.getAllowFlight()) {
                            player.setAllowFlight(true);
                        }
                        if (player.isFlying() && player.getLocation().getY() > maxFlightY) {
                            Vector v = player.getVelocity();
                            player.setVelocity(new Vector(v.getX(), -0.35, v.getZ()));
                            ColorUtils.sendActionBar(player, "&c&l[TECHO DE VUELO] &7Altura máxima de transformación alcanzada (&e" + (int) maxFlightY + "m&7).");
                        }
                    }
                }
            }
        }, 20L, 10L);
    }

    public void stopAllTasks() {
        if (channelTask != null) {
            channelTask.cancel();
            channelTask = null;
        }
        if (expirationAndCeilingTask != null) {
            expirationAndCeilingTask.cancel();
            expirationAndCeilingTask = null;
        }
        channelingMap.clear();
    }

    public boolean isChanneling(UUID uuid) {
        return channelingMap.containsKey(uuid);
    }

    public void startChanneling(Player player) {
        if (player == null || !player.isOnline()) return;
        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        if (data == null || !data.hasAssignedRace()) {
            ColorUtils.sendActionBar(player, "&cPrimero debes elegir una raza con &e/razas&c.");
            return;
        }
        if (channelingMap.containsKey(player.getUniqueId())) {
            return;
        }
        if (!data.isTransformed() && data.isTransformOnCooldown()) {
            long rem = data.getTransformCooldownRemainingSeconds();
            ColorUtils.sendActionBar(player, "&c&l[COOLDOWN] &7Metamorfosis disponible en &e" + rem + "s&7.");
            return;
        }
        channelingMap.put(player.getUniqueId(), System.currentTimeMillis());
        ColorUtils.sendActionBar(player, "&6&l[METAMORFOSIS] &7Mantén &eShift &7pulsado durante &f6.0s &7para completar...");
    }

    public void cancelChanneling(UUID uuid, String actionBarMessage) {
        if (channelingMap.remove(uuid) != null && actionBarMessage != null) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                ColorUtils.sendActionBar(p, actionBarMessage);
                p.playSound(p.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 0.45f, 1.3f);
            }
        }
    }

    public void toggleTransformation(Player player) {
        if (player == null) return;
        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        if (data == null || !data.hasAssignedRace()) {
            ColorUtils.sendMessage(player, "&cDebes seleccionar una raza antes de transformarte.");
            return;
        }

        if (data.isTransformed()) {
            revertTransformation(player, true);
            return;
        }

        if (data.isTransformOnCooldown()) {
            long remaining = data.getTransformCooldownRemainingSeconds();
            ColorUtils.sendActionBar(player, "&c&l[COOLDOWN] &7Transformación disponible en &e" + remaining + "s&7.");
            return;
        }

        int level = Math.max(1, Math.min(6, data.getLevel()));
        int durationSec = getConfiguredDurationSeconds(level);
        long activeUntil = (durationSec < 0) ? -1L : (System.currentTimeMillis() + (durationSec * 1000L));
        String durationText = (durationSec < 0)
                ? "&b∞ Infinita &7(Shift + Clic Derecho 6s o /raza transformar para revertir)"
                : "&aDuración: &e" + durationSec + "s";

        if (plugin.getLibsDisguisesHook() != null && plugin.getLibsDisguisesHook().isAvailable()) {
            plugin.getLibsDisguisesHook().disguiseForRace(player, data.getRace(), level);
        }

        data.setActiveDisguise(true);
        data.setTransformActiveUntil(activeUntil);
        plugin.getPlayerDataManager().saveToFile(data);

        applyTransformationBuffs(player, data.getRace(), level, durationSec);

        if (data.isSettingSounds()) {
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.9f, 0.9f);
        }
        if (data.isSettingParticles()) {
            player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, player.getLocation().add(0, 1, 0), 36, 0.45, 0.65, 0.45, 0.1);
        }
        ColorUtils.sendTitle(player, "&6&l¡METAMORFOSIS ACTIVA!", durationText, 10, 50, 10);
    }

    public void revertTransformation(Player player, boolean applyCooldown) {
        if (player == null) return;
        channelingMap.remove(player.getUniqueId());
        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());

        if (plugin.getLibsDisguisesHook() != null) {
            plugin.getLibsDisguisesHook().undisguise(player);
        }
        if (data != null) {
            boolean wasTransformed = data.isTransformed();
            data.setActiveDisguise(false);
            data.setTransformActiveUntil(0L);
            if (applyCooldown && wasTransformed) {
                data.setTransformCooldownUntil(System.currentTimeMillis() + 15000L);
            }
            plugin.getPlayerDataManager().saveToFile(data);
            plugin.getRaceManager().applyPassives(player, data.getRace(), data.getLevel());

            if (wasTransformed && applyCooldown) {
                ColorUtils.sendTitle(player, "&a&lFORMA ORIGINAL", "&7Has revertido tu apariencia normal.", 10, 35, 10);
                ColorUtils.sendActionBar(player, "&eEnfriamiento de metamorfosis: 15s.");
                if (data.isSettingSounds()) {
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 0.9f, 1.0f);
                }
                if (data.isSettingParticles()) {
                    player.getWorld().spawnParticle(Particle.LARGE_SMOKE, player.getLocation().add(0, 1, 0), 20, 0.3, 0.5, 0.3, 0.05);
                }
            }
        }
    }

    private void expireTransformation(Player player, PlayerData data) {
        if (plugin.getLibsDisguisesHook() != null) {
            plugin.getLibsDisguisesHook().undisguise(player);
        }
        data.setActiveDisguise(false);
        data.setTransformActiveUntil(0L);
        data.setTransformCooldownUntil(System.currentTimeMillis() + 15000L);
        plugin.getPlayerDataManager().saveToFile(data);
        plugin.getRaceManager().applyPassives(player, data.getRace(), data.getLevel());

        ColorUtils.sendMessage(player, "&c&l[METAMORFOSIS] &7Tu transformación ha concluido. Enfriamiento de 15s.");
        ColorUtils.sendActionBar(player, "&eMetamorfosis finalizada (Cooldown: 15s).");
        if (data.isSettingSounds()) {
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BAT_TAKEOFF, 0.9f, 0.8f);
        }
    }

    private int getConfiguredDurationSeconds(int level) {
        int def = switch (level) {
            case 1 -> 20;
            case 2 -> 35;
            case 3 -> 50;
            case 4 -> 75;
            case 5 -> 110;
            default -> -1;
        };
        return plugin.getConfig().getInt("transformations.durations.level_" + level, def);
    }

    public boolean isAerialForm(String raceId, int level) {
        if (raceId == null) return false;
        return switch (raceId.toLowerCase()) {
            case "fairy", "angel", "vampire", "dragonkin", "yokai" -> true;
            default -> false;
        };
    }

    private void applyTransformationBuffs(Player player, String raceId, int level, int durationSec) {
        int ticks = (durationSec < 0) ? 20 * 600 : (durationSec * 20);
        String r = raceId.toLowerCase();
        if (isAerialForm(r, level) && player.getGameMode() != GameMode.SPECTATOR) {
            player.setAllowFlight(true);
            player.setFlying(true);
        }
        switch (r) {
            case "werewolf", "orc", "giant", "demon" -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, ticks, level >= 5 ? 1 : 0, true, false));
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, ticks, 0, true, false));
            }
            case "beastkin", "elf", "goblin" -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, ticks, 1, true, false));
                player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, ticks, 1, true, false));
            }
            case "dwarf" -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, ticks, 1, true, false));
                player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, ticks, 1, true, false));
            }
            case "vampire", "yokai" -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, ticks, 0, true, false));
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, ticks, 1, true, false));
            }
            case "druid", "angel", "fairy" -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, ticks, 0, true, false));
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, ticks, 0, true, false));
            }
            default -> player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, ticks, 0, true, false));
        }
    }

    public void cleanupPlayer(Player player) {
        if (player == null) return;
        channelingMap.remove(player.getUniqueId());
        revertTransformation(player, false);
    }
}

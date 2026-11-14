package com.cytricos.races.listener;

import com.cytricos.races.CytricosRaces;
import com.cytricos.races.model.PlayerData;
import com.cytricos.races.model.Race;
import com.cytricos.races.util.ColorUtils;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Listener central de habilidades, desventajas universales (L1+) y perks de progresión (1 a 6)
 * para las 16 razas de CYTRICOS RPG.
 *
 * REGLA FUNDAMENTAL DE ARQUITECTURA:
 * - Todas las desventajas y debuffs raciales están activos INMEDIATAMENTE desde Nivel 1.
 * - La ascensión de Nivel 1 al 6 ÚNICAMENTE desbloquea Perks positivos, pasivas, habilidades
 *   activas y sufijos superiores de LuckPerms.
 *
 * Lead Developers: Dandan_13 & Wolfit12
 */
public class RaceAbilitiesListener implements Listener {
    private final CytricosRaces plugin;

    // Cooldown maps: UUID -> AbilityName -> ExpirationTimestamp
    private final Map<UUID, Map<String, Long>> cooldowns = new ConcurrentHashMap<>();

    // State maps for temporary transformations/skills
    private final Set<UUID> beastFormActive = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Set<UUID> stoneSkinActive = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Set<UUID> batFormActive = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Set<UUID> unyieldingFuryActive = Collections.newSetFromMap(new ConcurrentHashMap<>());

    // Continuous 6.0s sneak-hold tracking
    private final Map<UUID, Long> sneakStartMap = new ConcurrentHashMap<>();
    private final Map<UUID, Location> sneakLocMap = new ConcurrentHashMap<>();

    public RaceAbilitiesListener(CytricosRaces plugin) {
        this.plugin = plugin;
        startSneakHoldTask();
        startTransformationExpirationTask();
        startResourceRegenTask();
    }

    private void startResourceRegenTask() {
        // Tarea cada 80 ticks (4s): Regeneración pasiva de recurso racial (+3 puntos, +15% con talento surv_regen)
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
                if (data != null && data.getRacialResource() < data.getMaxRacialResource()) {
                    double amount = data.hasTalent("surv_regen") ? 3.45 : 3.0;
                    data.addRacialResource(amount);
                }
            }
        }, 80L, 80L);
    }

    private void startSneakHoldTask() {
        // Tarea cada 2 ticks (0.1s): Monitoreo milimétrico de canalización estática de 6.0s
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long now = System.currentTimeMillis();
            for (Map.Entry<UUID, Long> entry : sneakStartMap.entrySet()) {
                UUID uuid = entry.getKey();
                Player p = Bukkit.getPlayer(uuid);
                if (p == null || !p.isOnline() || p.isDead() || !p.isSneaking()) {
                    sneakStartMap.remove(uuid);
                    sneakLocMap.remove(uuid);
                    continue;
                }

                Location initLoc = sneakLocMap.get(uuid);
                if (initLoc != null && p.getLocation().distanceSquared(initLoc) > 0.25) {
                    sneakStartMap.remove(uuid);
                    sneakLocMap.remove(uuid);
                    ColorUtils.sendActionBar(p, "&c&l¡MOVIMIENTO DETECTADO! &7Canalización interrumpida.");
                    p.playSound(p.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 0.4f, 1.4f);
                    continue;
                }

                long elapsed = now - entry.getValue();
                if (elapsed < 6000L) {
                    double progress = (double) elapsed / 6000.0;
                    int filled = (int) (progress * 10);
                    String bar = "&a" + "█".repeat(Math.max(0, filled)) + "&7" + "░".repeat(Math.max(0, 10 - filled));
                    ColorUtils.sendActionBar(p, "&6&lCanalizando Metamorfosis: &8[" + bar + "&8] &e" + String.format("%.1f", (6000L - elapsed) / 1000.0) + "s");
                    p.getWorld().spawnParticle(Particle.PORTAL, p.getLocation().add(0, 0.8, 0), 3, 0.2, 0.2, 0.2, 0.05);
                    if (elapsed % 1000L < 150L) {
                        p.playSound(p.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.35f, 1.1f + (float) progress * 0.5f);
                    }
                } else {
                    // Completó 6.0 segundos continuos
                    sneakStartMap.remove(uuid);
                    sneakLocMap.remove(uuid);
                    processSneakTransformation(p);
                }
            }
        }, 10L, 2L);
    }

    public void toggleTransformation(Player player) {
        processSneakTransformation(player);
    }

    public void revertTransformation(Player player) {
        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        if (data == null) return;
        if (plugin.getLibsDisguisesHook() != null) {
            plugin.getLibsDisguisesHook().undisguise(player);
        }
        data.setActiveDisguise(false);
        data.setTransformActiveUntil(0);
        data.setTransformCooldownUntil(System.currentTimeMillis() + 15000L);
        plugin.getPlayerDataManager().saveToFile(data);

        ColorUtils.sendTitle(player, "&a&lFORMA ORIGINAL", "&7Has revertido tu apariencia normal.", 10, 40, 10);
        ColorUtils.sendActionBar(player, "&eEnfriamiento de transformación activado: 15s.");
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1.0f, 1.0f);
        player.getWorld().spawnParticle(Particle.LARGE_SMOKE, player.getLocation().add(0, 1, 0), 25, 0.3, 0.5, 0.3, 0.05);
    }

    private void processSneakTransformation(Player player) {
        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        if (data == null) return;

        // Caso 1: Ya está transformado -> Revertir a forma original
        if (data.isTransformed()) {
            revertTransformation(player);
            return;
        }

        // Caso 2: Intento de transformar -> Verificar cooldown
        if (data.isTransformOnCooldown()) {
            long remaining = data.getTransformCooldownRemainingSeconds();
            ColorUtils.sendActionBar(player, "&c&l[COOLDOWN] &7Transformación disponible en: &e" + remaining + "s");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.7f, 1.0f);
            return;
        }

        int level = data.getLevel();
        long activeUntil;
        String durationText;

        // Nivel 6: Duración infinita (infinito hasta que agache 6s de nuevo o use /raza transformar)
        if (level >= 6) {
            activeUntil = -1L;
            durationText = "&b∞ Duración Infinita &7(/raza transformar o Shift 6s para revertir)";
        } else {
            // Niveles 1 al 5: Duración lineal: 15 + (lvl - 1) * 5 segundos
            int durationSec = 15 + (level - 1) * 5;
            activeUntil = System.currentTimeMillis() + (durationSec * 1000L);
            durationText = "&aDuración: &e" + durationSec + "s";
        }

        boolean disguised = false;
        if (plugin.getLibsDisguisesHook() != null) {
            disguised = plugin.getLibsDisguisesHook().disguiseForRace(player, data.getRace(), level);
        }

        if (!disguised) {
            return;
        }

        data.setActiveDisguise(true);
        data.setTransformActiveUntil(activeUntil);
        plugin.getPlayerDataManager().saveToFile(data);

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.85f);
        player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, player.getLocation().add(0, 1, 0), 40, 0.4, 0.6, 0.4, 0.1);
        ColorUtils.sendTitle(player, "&6&l¡TRANSFORMACIÓN ACTIVADA!", durationText, 10, 50, 10);
    }

    private void startTransformationExpirationTask() {
        // Tarea cada 20 ticks (1s): Expiración automática para Nivel 1-5
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long now = System.currentTimeMillis();
            for (Player player : Bukkit.getOnlinePlayers()) {
                PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
                if (data == null || !data.isTransformed()) continue;

                // Nivel 6 tiene -1L (infinita), no expira por tiempo
                if (data.getTransformActiveUntil() > 0 && now >= data.getTransformActiveUntil()) {
                    if (plugin.getLibsDisguisesHook() != null) {
                        plugin.getLibsDisguisesHook().undisguise(player);
                    }
                    data.setActiveDisguise(false);
                    data.setTransformActiveUntil(0);
                    data.setTransformCooldownUntil(now + 15000L);
                    plugin.getPlayerDataManager().saveToFile(data);

                    ColorUtils.sendMessage(player, "&c&l[METAMORFOSIS] &7Tu transformación ha expirado. Entras en enfriamiento de 15s.");
                    ColorUtils.sendActionBar(player, "&eEnfriamiento de transformación activado: 15s.");
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BAT_TAKEOFF, 1.0f, 0.8f);
                    player.getWorld().spawnParticle(Particle.SMOKE, player.getLocation().add(0, 1, 0), 15, 0.2, 0.4, 0.2, 0.05);
                }
            }
        }, 40L, 20L);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        if (event.isSneaking()) {
            PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
            if (data != null) {
                sneakStartMap.put(player.getUniqueId(), System.currentTimeMillis());
                sneakLocMap.put(player.getUniqueId(), player.getLocation());
            }
        } else {
            sneakStartMap.remove(player.getUniqueId());
            sneakLocMap.remove(player.getUniqueId());
        }
    }

    private boolean checkCooldown(Player player, String ability, int seconds) {
        long now = System.currentTimeMillis();
        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        Map<String, Long> pCd = cooldowns.computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>());
        Long expires = pCd.get(ability);
        if (expires != null && now < expires) {
            long remaining = Math.max(1L, (expires - now) / 1000L);
            if (data == null || data.isSettingCooldownMsgs()) {
                player.sendMessage(ColorUtils.color("&c&l[COOLDOWN] &7Espera &e" + remaining + "s &7para reutilizar &6" + ability + "&7."));
            }
            if (data == null || data.isSettingActionBar()) {
                ColorUtils.sendActionBar(player, "&c⏳ " + ability + " disponible en &e" + remaining + "s");
            }
            return false;
        }
        double cdMult = (data != null && data.hasTalent("util_cd")) ? 0.85 : 1.0;
        long effectiveMs = (long) (seconds * 1000L * cdMult);
        pCd.put(ability, now + effectiveMs);

        if (data != null) {
            data.incrementAbilitiesUsed();
        }
        if (plugin.getQuestManager() != null) {
            plugin.getQuestManager().recordAction(player, com.cytricos.races.manager.QuestManager.QuestType.USE_ABILITY, 1);
        }
        return true;
    }

    // =========================================================================
    // 1. CICLO DE VIDA DEL JUGADOR (CONEXIÓN, REAPARICIÓN, SALIDA)
    // =========================================================================

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());

        // Si es nuevo jugador, notificar selección inicial gratuita
        if (data.hasFreeRaceChangeAvailable() && data.getRace().equalsIgnoreCase("human") && data.getLevel() == 1) {
            player.sendMessage(ColorUtils.color("&8=================================================="));
            player.sendMessage(ColorUtils.color("&a&l¡BIENVENIDO A CYTRICOS RPG!"));
            player.sendMessage(ColorUtils.color("&7Dispones de una &eSelección Gratuita de Raza&7."));
            player.sendMessage(ColorUtils.color("&7Escribe &a/razas &7para abrir el menú interactivo."));
            player.sendMessage(ColorUtils.color("&8=================================================="));
        }

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                plugin.getRaceManager().applyPassives(player, data.getRace(), data.getLevel());
            }
        }, 10L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                plugin.getRaceManager().applyPassives(player, data.getRace(), data.getLevel());
            }
        }, 5L);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        beastFormActive.remove(uuid);
        stoneSkinActive.remove(uuid);
        batFormActive.remove(uuid);
        unyieldingFuryActive.remove(uuid);
        cooldowns.remove(uuid);
        if (plugin.getLibsDisguisesHook() != null) {
            plugin.getLibsDisguisesHook().undisguise(player);
        }
        plugin.getPlayerDataManager().unload(uuid);
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        beastFormActive.remove(uuid);
        stoneSkinActive.remove(uuid);
        batFormActive.remove(uuid);
        unyieldingFuryActive.remove(uuid);

        PlayerData data = plugin.getPlayerDataManager().get(uuid);
        if (data != null) {
            if (plugin.getLibsDisguisesHook() != null && data.isTransformed()) {
                plugin.getLibsDisguisesHook().undisguise(player);
            }
            data.setActiveDisguise(false);
            data.setTransformActiveUntil(0);
            plugin.getPlayerDataManager().saveToFile(data);

            if (player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR) {
                String raceId = data.getRace().toLowerCase();
                int level = data.getLevel();
                if (!raceId.equals("fairy") && !(raceId.equals("angel") && level >= 3)) {
                    player.setFlying(false);
                    player.setAllowFlight(false);
                }
            }
        }
    }


    // =========================================================================
    // 2. MOVILIDAD FÍSICA: DOBLE SALTO, ESCALADA, VUELO Y AMBIENTE
    // =========================================================================

    @EventHandler(priority = EventPriority.HIGH)
    public void onToggleFlight(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) return;

        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        if (data == null) return;

        // Si el jugador se encuentra sancionado por exceso de altitud, bloquear vuelo
        if (data.isFlightBanned()) {
            event.setCancelled(true);
            player.setFlying(false);
            player.setAllowFlight(false);
            long rem = data.getFlightBanRemainingSeconds();
            ColorUtils.sendActionBar(player, "&c&l[!] &cVuelo bloqueado por exceso de altitud. Restante: &e" + String.format("%02d:%02d", rem / 60, rem % 60));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.7f, 1.0f);
            return;
        }

        String raceId = data.getRace().toLowerCase();
        int level = data.getLevel();

        // Fairy (L1+) y Angel (L3+): Vuelo permanente permitido
        if (raceId.equals("fairy") || (raceId.equals("angel") && level >= 3)) {
            return;
        }

        // Werewolf Nivel 2+ (Doble Salto de Caza)
        if (raceId.equals("werewolf") && level >= 2) {
            event.setCancelled(true);
            player.setFlying(false);
            player.setAllowFlight(false);
            Vector v = player.getLocation().getDirection().multiply(1.3).setY(0.7);
            player.setVelocity(v);
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WOLF_HOWL, 0.6f, 1.6f);
            player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 10, 0.2, 0.1, 0.2, 0.05);
            return;
        }

        // Beastkin Nivel 4+ (Triple Salto en el aire)
        if (raceId.equals("beastkin") && level >= 4) {
            event.setCancelled(true);
            player.setFlying(false);
            player.setAllowFlight(false);
            Vector v = player.getLocation().getDirection().multiply(1.4).setY(0.75);
            player.setVelocity(v);
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BAT_TAKEOFF, 0.8f, 1.4f);
            player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, player.getLocation(), 5, 0.2, 0.1, 0.2, 0.02);
            return;
        }

        // Cualquier otro intento sin permiso se cancela
        event.setCancelled(true);
        player.setFlying(false);
        player.setAllowFlight(false);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
                event.getFrom().getBlockY() == event.getTo().getBlockY() &&
                event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        Player player = event.getPlayer();
        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        String raceId = data.getRace().toLowerCase();
        int level = data.getLevel();
        Location loc = player.getLocation();
        Block block = loc.getBlock();

        // Re-habilitar doble salto si tocan suelo
        if (player.isOnGround()) {
            if ((raceId.equals("werewolf") && level >= 2) || (raceId.equals("beastkin") && level >= 4)) {
                player.setAllowFlight(true);
            }
        }

        // Beastkin L2+: Escalada de paredes natural (trepar muros sólidos al agacharse/saltar)
        if (raceId.equals("beastkin") && level >= 2 && player.isSneaking()) {
            Block facingBlock = loc.clone().add(loc.getDirection().setY(0).normalize()).getBlock();
            if (facingBlock.getType().isSolid()) {
                player.setVelocity(new Vector(0, 0.25, 0));
                player.setFallDistance(0);
            }
        }

        // Elfo L1+: Velocidad aumentada sobre hojas
        if (raceId.equals("elf")) {
            Block below = loc.clone().subtract(0, 0.5, 0).getBlock();
            if (below.getType().name().contains("LEAVES")) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, 1, false, false));
            }
        }

        // Dragonkin Desventaja (L1+): Slowness III en agua fría o biomas gélidos
        if (raceId.equals("dragonkin")) {
            String biome = block.getBiome().name();
            boolean isCold = biome.contains("SNOW") || biome.contains("FROZEN") || biome.contains("ICE");
            if (isCold || (player.isInWater() && isCold)) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 2, false, false));
            }
        }

        // Druida Desventaja (L1+): Slowness en desiertos sin luz solar directa
        if (raceId.equals("druid")) {
            String biome = block.getBiome().name();
            if ((biome.contains("DESERT") || biome.contains("BADLANDS")) && block.getLightFromSky() < 12) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 0, false, false));
            }
        }

        // Gigante Desventaja (L1+): Lentitud física permanente (Slowness I)
        if (raceId.equals("giant")) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 0, false, false));
        }

        // Gigante L4+: Romper bloques blandos al correr/embestir
        if (raceId.equals("giant") && level >= 4 && player.isSprinting()) {
            Block headBlock = loc.clone().add(loc.getDirection().setY(0).normalize()).add(0, 1, 0).getBlock();
            if (plugin.getWorldGuardHook() == null || plugin.getWorldGuardHook().canBreak(player, headBlock)) {
                if (headBlock.getType() == Material.DIRT || headBlock.getType() == Material.GRASS_BLOCK ||
                        headBlock.getType().name().contains("LEAVES") || headBlock.getType().name().contains("GLASS")) {
                    headBlock.breakNaturally();
                    player.getWorld().spawnParticle(Particle.BLOCK, headBlock.getLocation().add(0.5, 0.5, 0.5), 15, 0.3, 0.3, 0.3, headBlock.getBlockData());
                }
            }
        }


        // Hada Desventaja (L1+): Slowness severo en agua profunda
        if (raceId.equals("fairy") && player.isInWater()) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 2, false, false));
        }
    }

    // =========================================================================
    // 3. DAÑO AMBIENTAL, VULNERABILIDADES (L1+) E INMUNIDADES
    // =========================================================================

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        Race race = plugin.getRaceManager().getRace(data.getRace());
        if (race == null) return;

        String raceId = race.getId().toLowerCase();
        int level = data.getLevel();
        EntityDamageEvent.DamageCause cause = event.getCause();

        // Inmunidad temporal: Unyielding Fury (Orco L6) o Lord of Mist (Vampiro L6)
        if (unyieldingFuryActive.contains(player.getUniqueId())) {
            event.setCancelled(true);
            return;
        }

        // Stone Skin activo de Enano L4: Inmunidad total a proyectiles
        if (stoneSkinActive.contains(player.getUniqueId()) && cause == EntityDamageEvent.DamageCause.PROJECTILE) {
            event.setCancelled(true);
            player.getWorld().playSound(player.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1.0f, 0.8f);
            return;
        }

        // 1. Inmunidades de Fuego y Lava
        if ((cause == EntityDamageEvent.DamageCause.FIRE || cause == EntityDamageEvent.DamageCause.FIRE_TICK || cause == EntityDamageEvent.DamageCause.LAVA)) {
            if (race.hasImmunity("fire") || (raceId.equals("werewolf") && level >= 4)) {
                event.setCancelled(true);
                player.setFireTicks(0);
                return;
            }
            // Vulnerabilidad de Elfo (+15% fuego L1-2, +5% L3-5, 0% L6) y Druida (+25% fuego L1-2, +10% L3-5, 0% L6)
            if (raceId.equals("elf")) {
                double mult = (level >= 6) ? 1.0 : (level >= 3 ? 1.05 : 1.15);
                event.setDamage(event.getDamage() * mult);
            } else if (raceId.equals("druid")) {
                double mult = (level >= 6) ? 1.0 : (level >= 3 ? 1.10 : 1.25);
                event.setDamage(event.getDamage() * mult);
            }
        }

        // 2. Inmunidad a Caída: Hadas (L2+), Ángeles (L2+), o por configuración
        if (cause == EntityDamageEvent.DamageCause.FALL) {
            if (race.hasImmunity("fall_damage") || (raceId.equals("fairy") && level >= 2) || (raceId.equals("angel") && level >= 2)) {
                event.setCancelled(true);
                return;
            }
            // Enano Desventaja: Densidad corporal (L1-2: +20%, L3-4: +10%, L5+: 0% penalización)
            if (raceId.equals("dwarf") && level < 5) {
                double mult = (level >= 3) ? 1.10 : 1.20;
                event.setDamage(event.getDamage() * mult);
            }
        }

        // 3. Inmunidad a Ahogamiento: Vampiro (L1+), Yokai (L4+)
        if (cause == EntityDamageEvent.DamageCause.DROWNING) {
            if (race.hasImmunity("drowning") || raceId.equals("vampire") || (raceId.equals("yokai") && level >= 4)) {
                event.setCancelled(true);
                player.setRemainingAir(player.getMaximumAir());
                return;
            }
        }

        // 4. Inmunidad a Veneno: Druida (L4+), No-Muerto (L1+), Vampiro (L1+)
        if (cause == EntityDamageEvent.DamageCause.POISON) {
            if (race.hasImmunity("poison") || (raceId.equals("druid") && level >= 4) || raceId.equals("undead") || raceId.equals("vampire")) {
                event.setCancelled(true);
                return;
            }
        }

        // 5. Inmunidad a Sofocación en bloques: Yokai (L4+)
        if (cause == EntityDamageEvent.DamageCause.SUFFOCATION && (race.hasImmunity("suffocation") || (raceId.equals("yokai") && level >= 4))) {
            event.setCancelled(true);
            return;
        }

        // 6. Celestial Desventaja: Recibe daño extra en oscuridad total (L1-2: +20%, L3-4: +10%, L5+: 0%)
        if (raceId.equals("angel") && level < 5 && player.getLocation().getBlock().getLightLevel() <= 3) {
            double mult = (level >= 3) ? 1.10 : 1.20;
            event.setDamage(event.getDamage() * mult);
        }

        // 7. Yokai Desventaja: Recibe daño extra de proyectiles (L1-2: +15%, L3-4: +7%, L5+: 0%)
        if (raceId.equals("yokai") && level < 5 && cause == EntityDamageEvent.DamageCause.PROJECTILE) {
            double mult = (level >= 3) ? 1.07 : 1.15;
            event.setDamage(event.getDamage() * mult);
        }

        // 8. Gigante Desventaja: Envergadura vulnerable a flechas (L1-2: +30%, L3-4: +15%, L5+: 0%)
        if (raceId.equals("giant") && level < 5 && cause == EntityDamageEvent.DamageCause.PROJECTILE) {
            double mult = (level >= 3) ? 1.15 : 1.30;
            event.setDamage(event.getDamage() * mult);
        }
    }

    // =========================================================================
    // 4. COMBATE CUERPO A CUERPO, DISTANCIA Y BALANCE PVP
    // =========================================================================

    @EventHandler(priority = EventPriority.HIGH)
    public void onPvPDamage(EntityDamageByEntityEvent event) {
        Player attacker = null;
        Arrow arrow = null;
        if (event.getDamager() instanceof Player p) {
            attacker = p;
        } else if (event.getDamager() instanceof Arrow a && a.getShooter() instanceof Player p) {
            attacker = p;
            arrow = a;
        }

        // Talento de Combate: Golpe Certero (combat_crit: +10% daño crítico contra cualquier objetivo)
        if (attacker != null && event.getEntity() instanceof LivingEntity) {
            PlayerData atkCritData = plugin.getPlayerDataManager().get(attacker.getUniqueId());
            if (atkCritData != null && atkCritData.hasTalent("combat_crit")) {
                boolean isCrit = (arrow != null && arrow.isCritical())
                        || (!attacker.isOnGround() && attacker.getFallDistance() > 0.0F
                        && !attacker.isInWater() && !attacker.isClimbing()
                        && !attacker.hasPotionEffect(PotionEffectType.BLINDNESS));
                if (isCrit) {
                    event.setDamage(event.getDamage() * 1.10);
                }
            }
        }

        // CASO A: VÍCTIMA ES JUGADOR
        if (event.getEntity() instanceof Player victim) {
            PlayerData vicData = plugin.getPlayerDataManager().get(victim.getUniqueId());
            String vicRaceId = vicData.getRace().toLowerCase();
            int vicLevel = vicData.getLevel();

            // Hada L6: Campo de absorción de daño (12b)
            if (vicRaceId.equals("fairy") && vicLevel >= 6) {
                event.setDamage(event.getDamage() * 0.70);
            }

            // Goblin Desventaja: Daño recibido de ataques pesados (hachas, mazas) (L1-2: +25%, L3-5: +10%, L6: 0%)
            if (vicRaceId.equals("goblin") && vicLevel < 6) {
                if (attacker != null) {
                    Material weapon = attacker.getInventory().getItemInMainHand().getType();
                    if (weapon.name().contains("AXE") || weapon.name().contains("MACE")) {
                        double mult = (vicLevel >= 3) ? 1.10 : 1.25;
                        event.setDamage(event.getDamage() * mult);
                    }
                }
            }

            // Orco L6: Furia Inquebrantable (5s invulnerable al caer bajo 20% vida)
            if (vicRaceId.equals("orc") && vicLevel >= 6) {
                if (victim.getHealth() - event.getFinalDamage() <= victim.getMaxHealth() * 0.20) {
                    if (checkCooldown(victim, "FuriaInquebrantable", 180)) {
                        unyieldingFuryActive.add(victim.getUniqueId());
                        victim.sendMessage(ColorUtils.color("&4&l[FURIA]&c ¡Te has vuelto invulnerable por 5 segundos!"));
                        victim.getWorld().spawnParticle(Particle.LAVA, victim.getLocation(), 20, 0.5, 0.5, 0.5, 0.1);
                        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                            unyieldingFuryActive.remove(victim.getUniqueId());
                            victim.sendMessage(ColorUtils.color("&7Tu estado de invulnerabilidad ha finalizado."));
                        }, 100L);
                    }
                }
            }

            // ATACANTE ES JUGADOR
            if (attacker != null) {
                PlayerData atkData = plugin.getPlayerDataManager().get(attacker.getUniqueId());
                String atkRaceId = atkData.getRace().toLowerCase();
                int atkLevel = atkData.getLevel();

                // Werewolf Desventaja: Penalización por armadura pesada (L1-3: -20%, L4-5: -10%, L6: 0% sin penalización)
                if (atkRaceId.equals("werewolf")) {
                    ItemStack chest = attacker.getInventory().getChestplate();
                    if (chest != null && (chest.getType() == Material.DIAMOND_CHESTPLATE || chest.getType() == Material.NETHERITE_CHESTPLATE || chest.getType() == Material.IRON_CHESTPLATE)) {
                        double penalty = (atkLevel >= 6) ? 1.0 : (atkLevel >= 4 ? 0.90 : 0.80);
                        event.setDamage(event.getDamage() * penalty);
                    }
                    // Werewolf L2+: Garras sin armas (+2 corazones / 4 de daño extra)
                    if (atkLevel >= 2 && attacker.getInventory().getItemInMainHand().getType() == Material.AIR) {
                        event.setDamage(event.getDamage() + 4.0);
                        victim.getWorld().spawnParticle(Particle.CRIT, victim.getLocation().add(0, 1, 0), 8, 0.2, 0.2, 0.2, 0.1);
                    }
                    // Werewolf L5+: Sangrado continuo acumulativo
                    if (atkLevel >= 5) {
                        applyBleedEffect(victim, 3);
                    }
                    // Werewolf L6 (Fenrir): Ignora 50% armadura enemiga
                    if (atkLevel >= 6) {
                        double trueDmg = event.getDamage() * 0.50;
                        event.setDamage(event.getDamage() + trueDmg);
                    }
                }

                // Vampire L1+: 35% probabilidad de morder y nutrirse en combate
                if (atkRaceId.equals("vampire") && Math.random() < 0.35) {
                    attacker.setFoodLevel(Math.min(20, attacker.getFoodLevel() + 2));
                    attacker.setSaturation(Math.min(20.0f, attacker.getSaturation() + 2.0f));
                    attacker.getWorld().playSound(attacker.getLocation(), Sound.ENTITY_GENERIC_DRINK, 0.8f, 0.9f);
                    attacker.getWorld().spawnParticle(Particle.DUST, victim.getLocation().add(0, 1, 0), 12, 0.2, 0.3, 0.2, new Particle.DustOptions(Color.RED, 1.3f));
                    ColorUtils.sendActionBar(attacker, "&4&l[MORDISCO VAMPÍRICO] &c+2 Hambre / +2 Saturación absorbida de la víctima");
                }

                // Vampire L3+: Robo de vida del 15%
                if (atkRaceId.equals("vampire") && atkLevel >= 3) {
                    double heal = Math.max(0.5, event.getFinalDamage() * 0.15);
                    attacker.setHealth(Math.min(attacker.getMaxHealth(), attacker.getHealth() + heal));
                    attacker.getWorld().spawnParticle(Particle.HEART, attacker.getLocation().add(0, 1.5, 0), 2, 0.2, 0.2, 0.2, 0.05);
                }

                // Vampire L5+: Drenar hambre/energía
                if (atkRaceId.equals("vampire") && atkLevel >= 5) {
                    victim.setFoodLevel(Math.max(0, victim.getFoodLevel() - 2));
                }

                // Balance PvP: Celestiales causan +30% daño a Vampiros y No-Muertos
                if (atkRaceId.equals("angel") && (vicRaceId.equals("vampire") || vicRaceId.equals("undead"))) {
                    event.setDamage(event.getDamage() * 1.30);
                    victim.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, victim.getLocation().add(0, 1, 0), 10, 0.3, 0.3, 0.3, 0.1);
                }

                // Balance PvP: Orcos y Gigantes rompen escudos y otorgan empuje masivo
                if (atkRaceId.equals("orc") || atkRaceId.equals("giant") || beastFormActive.contains(attacker.getUniqueId())) {
                    if (victim.isBlocking()) {
                        victim.setCooldown(Material.SHIELD, 100); // 5s desactivado
                        victim.sendMessage(ColorUtils.color("&c&l¡Tu escudo fue destrozado por la fuerza brutal de tu rival!"));
                        attacker.sendMessage(ColorUtils.color("&a&l¡Has quebrado el escudo enemigo!"));
                        victim.getWorld().playSound(victim.getLocation(), Sound.ITEM_SHIELD_BREAK, 1.0f, 0.8f);
                    }
                }

                // Orco L1+: Ataques con hacha ignoran 25% armadura
                if (atkRaceId.equals("orc") && attacker.getInventory().getItemInMainHand().getType().name().contains("AXE")) {
                    event.setDamage(event.getDamage() * 1.25);
                    // Orco L2+: Furia sangrienta (+50% daño si está bajo de 30% vida)
                    if (atkLevel >= 2 && attacker.getHealth() <= attacker.getMaxHealth() * 0.30) {
                        event.setDamage(event.getDamage() * 1.50);
                        attacker.getWorld().spawnParticle(Particle.FLAME, attacker.getLocation(), 5, 0.2, 0.2, 0.2, 0.05);
                    }
                }

                // Elfo L1+: +15% daño de arco
                if (atkRaceId.equals("elf") && arrow != null) {
                    event.setDamage(event.getDamage() * 1.15);
                    // Elfo L5+: Flechas ignoran 30% armadura
                    if (atkLevel >= 5) {
                        event.setDamage(event.getDamage() * 1.30);
                    }
                }

                // Elfo L3+: Flechas curativas si disparas a compañeros
                if (atkRaceId.equals("elf") && atkLevel >= 3 && arrow != null && victim != attacker) {
                    if (victim.getScoreboard().getPlayerTeam(victim) != null &&
                            victim.getScoreboard().getPlayerTeam(victim).equals(attacker.getScoreboard().getPlayerTeam(attacker))) {
                        event.setCancelled(true);
                        victim.setHealth(Math.min(victim.getMaxHealth(), victim.getHealth() + 6.0));
                        victim.sendMessage(ColorUtils.color("&a¡Una flecha silvana aliada te ha restaurado 3 corazones!"));
                        victim.getWorld().spawnParticle(Particle.HEART, victim.getLocation().add(0, 1, 0), 4, 0.2, 0.2, 0.2, 0.05);
                        return;
                    }
                }

                // Goblin L3+: Hurto aleatorio seguro con exclusión de armas y netherite
                if (atkRaceId.equals("goblin") && atkLevel >= 3) {
                    performGoblinTheft(attacker, victim, atkLevel);
                }

                // Demonio L1+: Aplica Fuego Abisal
                if (atkRaceId.equals("demon")) {
                    victim.setFireTicks(80);
                    victim.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, victim.getLocation().add(0, 1, 0), 6, 0.2, 0.2, 0.2, 0.05);
                }

                // Undead Desventaja: Recibe daño extra si atacante tiene Smite (L1-3: 2.0x, L4-5: 1.4x, L6 Archilich: 1.0x sin daño extra)
                if (vicRaceId.equals("undead")) {
                    ItemStack weapon = attacker.getInventory().getItemInMainHand();
                    if (weapon.hasItemMeta() && weapon.getItemMeta().hasEnchant(Enchantment.SMITE)) {
                        double mult = (vicLevel >= 6) ? 1.0 : (vicLevel >= 4 ? 1.4 : 2.0);
                        event.setDamage(event.getDamage() * mult);
                        victim.getWorld().playSound(victim.getLocation(), Sound.ENTITY_WITHER_HURT, 1.0f, 1.2f);
                    }
                }

                // Undead L3+: Robar XP e infligir náuseas a mano vacía
                if (atkRaceId.equals("undead") && atkLevel >= 3 && attacker.getInventory().getItemInMainHand().getType() == Material.AIR) {
                    victim.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 100, 1));
                    attacker.giveExp(3);
                }
            }
        }
    }

    private void applyBleedEffect(Player victim, int seconds) {
        new org.bukkit.scheduler.BukkitRunnable() {
            int count = 0;
            @Override
            public void run() {
                if (!victim.isOnline() || victim.isDead() || count >= seconds) {
                    cancel();
                    return;
                }
                victim.damage(1.0);
                victim.getWorld().spawnParticle(Particle.DUST, victim.getLocation().add(0, 1, 0), 10, 0.2, 0.2, 0.2, new Particle.DustOptions(Color.RED, 1.2f));
                count++;
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    private void performGoblinTheft(Player goblin, Player victim, int goblinLevel) {
        // Validación de permisos y modos de juego del objetivo
        if (victim.getGameMode() == GameMode.CREATIVE || victim.getGameMode() == GameMode.SPECTATOR) {
            return;
        }
        if (victim.hasPermission("cytricosraces.theft.exempt") || victim.hasPermission("cytricosraces.admin")) {
            return;
        }
        if (plugin.getWorldGuardHook() != null && !plugin.getWorldGuardHook().canPvp(goblin, victim.getLocation())) {
            return;
        }

        // Probabilidad escalonada: 20% base L3, +5% por nivel hasta 35% en L6
        double chance = 0.20 + (goblinLevel - 3) * 0.05;
        if (Math.random() > chance) {
            return;
        }

        // Cooldown de 45 segundos por hurto
        if (!checkCooldown(goblin, "RoboDuende", 45)) {
            return;
        }

        // Búsqueda de ranura elegible (ranuras 0 a 8 de la hotbar, excluyendo la mano principal activa)
        int heldSlot = victim.getInventory().getHeldItemSlot();
        List<Integer> candidateSlots = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            if (i == heldSlot) continue; // No desarma el arma en uso directo
            ItemStack it = victim.getInventory().getItem(i);
            if (it == null || it.getType() == Material.AIR) continue;
            Material m = it.getType();
            // Filtro de exclusión de seguridad de objetos
            if (m.name().contains("NETHERITE_")) continue;
            if (m == Material.TOTEM_OF_UNDYING) continue;
            if (m.name().contains("SHULKER_BOX")) continue;
            candidateSlots.add(i);
        }

        if (candidateSlots.isEmpty()) {
            return;
        }

        int chosenSlot = candidateSlots.get(new Random().nextInt(candidateSlots.size()));
        ItemStack victimItem = victim.getInventory().getItem(chosenSlot);
        if (victimItem == null || victimItem.getType() == Material.AIR) {
            return;
        }

        // Cantidad a robar: 1 objeto o hasta 4 si es apilable
        int stealQty = (victimItem.getMaxStackSize() > 1 && victimItem.getAmount() > 1)
                ? Math.min(Math.min(4, victimItem.getAmount()), 1 + (goblinLevel - 3))
                : 1;

        ItemStack stolenStack = victimItem.clone();
        stolenStack.setAmount(stealQty);

        // Descontar del inventario de la víctima
        if (victimItem.getAmount() <= stealQty) {
            victim.getInventory().setItem(chosenSlot, null);
        } else {
            victimItem.setAmount(victimItem.getAmount() - stealQty);
            victim.getInventory().setItem(chosenSlot, victimItem);
        }

        // Transferencia segura al duende (si el inventario está lleno, cae a sus pies para evitar pérdida o dupes)
        Map<Integer, ItemStack> overflow = goblin.getInventory().addItem(stolenStack);
        if (!overflow.isEmpty()) {
            for (ItemStack drop : overflow.values()) {
                goblin.getWorld().dropItemNaturally(goblin.getLocation(), drop);
            }
            ColorUtils.sendMessage(goblin, "&6[INVENTARIO LLENO] &7¡El botín robado no cupo en tus bolsillos y cayó a tus pies!");
        }

        String itemName = stolenStack.hasItemMeta() && stolenStack.getItemMeta().hasDisplayName()
                ? stolenStack.getItemMeta().getDisplayName()
                : stolenStack.getType().name();

        ColorUtils.sendMessage(goblin, "&a&l[DUENDE ASTUTO] &7¡Le has sustraído con éxito &e" + stealQty + "x " + itemName + " &7a &f" + victim.getName() + "&7!");
        ColorUtils.sendMessage(victim, "&c&l[¡HURTO SUFRIDO!] &7¡Un duende sigiloso te ha robado &e" + stealQty + "x " + itemName + " &7de tu cinto!");
        goblin.getWorld().playSound(goblin.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.2f, 1.5f);
        victim.getWorld().playSound(victim.getLocation(), Sound.ENTITY_CHICKEN_EGG, 1.0f, 0.6f);
        goblin.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, goblin.getLocation().add(0, 1, 0), 10, 0.3, 0.3, 0.3, 0.05);
    }


    // =========================================================================
    // 5. CONSUMO DE ALIMENTOS, DESVENTAJAS DIETÉTICAS (L1+)
    // =========================================================================

    @EventHandler(priority = EventPriority.HIGH)
    public void onConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        String raceId = data.getRace().toLowerCase();
        Material mat = event.getItem().getType();

        // Werewolf Desventaja (L1+): Comida cocinada causa Náuseas
        if (raceId.equals("werewolf")) {
            if (mat == Material.COOKED_BEEF || mat == Material.COOKED_PORKCHOP || mat == Material.COOKED_MUTTON ||
                    mat == Material.COOKED_CHICKEN || mat == Material.COOKED_COD || mat == Material.COOKED_SALMON) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 160, 1));
                player.sendMessage(ColorUtils.color("&cTu naturaleza salvaje aborrece la carne cocinada."));
            }
        }

        // Vampire Desventaja (L1+): No consume comida sólida. Admite pociones, estofados, miel y CALDO DE SANGRE (Sopa de Remolacha)
        if (raceId.equals("vampire")) {
            if (mat == Material.BEETROOT_SOUP) {
                player.setFoodLevel(Math.min(20, player.getFoodLevel() + 6));
                player.setSaturation(Math.min(20.0f, player.getSaturation() + 6.0f));
                player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 0));
                ColorUtils.sendActionBar(player, "&4&l[CALDO DE SANGRE] &cNutrición vital absorbida (+Regeneración)");
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_DRINK, 1.0f, 0.9f);
                player.getWorld().spawnParticle(Particle.HEART, player.getLocation().add(0, 1.2, 0), 3, 0.2, 0.2, 0.2, 0.05);
                return;
            }

            if (mat != Material.POTION && mat != Material.HONEY_BOTTLE && mat != Material.MUSHROOM_STEW && mat != Material.SUSPICIOUS_STEW) {
                event.setCancelled(true);
                player.sendMessage(ColorUtils.color("&5Como vampiro, solo te nutres de sangre fresca, caldo de remolacha y fluidos místicos."));
            }
        }

        // Druida Cítrico Desventaja (L1+): Rechaza carnes (Nauseas) / Prefiere frutas (saturación x2)
        if (raceId.equals("druid")) {
            if (mat.name().contains("BEEF") || mat.name().contains("PORK") || mat.name().contains("MUTTON") || mat.name().contains("CHICKEN")) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 140, 1));
                player.sendMessage(ColorUtils.color("&eTu esencia botánica rechaza el tejido animal procesado."));
            } else if (mat == Material.APPLE || mat == Material.GOLDEN_APPLE || mat == Material.ENCHANTED_GOLDEN_APPLE ||
                    mat == Material.MELON_SLICE || mat == Material.SWEET_BERRIES || mat == Material.GLOW_BERRIES) {
                player.setSaturation(player.getSaturation() + 5.0f);
                player.sendMessage(ColorUtils.color("&a¡Absorbes nutrición cítrica concentrada!"));
            }
        }

        // No-Muerto L2+: Carne podrida regenera salud (+4 HP / 2 corazones)
        if (raceId.equals("undead") && data.getLevel() >= 2 && mat == Material.ROTTEN_FLESH) {
            player.setHealth(Math.min(player.getMaxHealth(), player.getHealth() + 4.0));
            player.sendMessage(ColorUtils.color("&2¡La descomposición revitaliza tus órganos no-muertos!"));
        }
    }

    // =========================================================================
    // 6. CONTROL DE HAMBRE Y ENERGÍA
    // =========================================================================

    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        String raceId = data.getRace().toLowerCase();

        // No-Muerto (L1+): Barra de hambre congelada permanentemente
        if (raceId.equals("undead")) {
            event.setCancelled(true);
            player.setFoodLevel(20);
        }
    }

    // =========================================================================
    // 7. DISPARO CON ARCO: DESVENTAJAS DE BEASTKIN Y ORCO (L1+)
    // =========================================================================

    @EventHandler
    public void onShootBow(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        String raceId = data.getRace().toLowerCase();

        // Beastkin Desventaja (L1+): Desvío de arcos (solo permite ballestas)
        if (raceId.equals("beastkin")) {
            ItemStack bow = event.getBow();
            if (bow != null && bow.getType() == Material.BOW) {
                event.setCancelled(true);
                player.sendMessage(ColorUtils.color("&6Tu anatomía bestial desvía el arco. &e¡Solo tienes destreza con ballestas!"));
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.7f, 0.8f);
            }
        }

        // Orco Desventaja (L1+): Arcos tienen desviación imprecisa
        if (raceId.equals("orc") && event.getProjectile() instanceof Arrow arrow) {
            Vector spread = new Vector((Math.random() - 0.5) * 0.4, (Math.random() - 0.5) * 0.2, (Math.random() - 0.5) * 0.4);
            arrow.setVelocity(arrow.getVelocity().add(spread));
        }
    }

    // =========================================================================
    // 8. DURABILIDAD DE ÍTEMS Y ARMADURAS
    // =========================================================================

    @EventHandler
    public void onItemDamage(PlayerItemDamageEvent event) {
        Player player = event.getPlayer();
        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        String raceId = data.getRace().toLowerCase();
        int level = data.getLevel();
        Material type = event.getItem().getType();

        // Dragonkin Desventaja (L1+): Armaduras de hierro u oro se rompen 20% más rápido
        if (raceId.equals("dragonkin") && (type.name().contains("IRON_") || type.name().contains("GOLDEN_"))) {
            event.setDamage((int) Math.ceil(event.getDamage() * 1.25));
        }

        // Humano L3+: Doble durabilidad en herramientas/armaduras (50% prob. ignorar daño)
        if (raceId.equals("human") && level >= 3 && Math.random() < 0.50) {
            event.setCancelled(true);
        }

        // Enano L5+: Escudos nunca pierden durabilidad
        if (raceId.equals("dwarf") && level >= 5 && type == Material.SHIELD) {
            event.setCancelled(true);
        }
    }

    // =========================================================================
    // 9. MINERÍA Y DOBLE ORE DROP (ENANO L2+)
    // =========================================================================

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        Block block = event.getBlock();
        String matName = block.getType().name();

        if (matName.contains("_ORE") || matName.contains("GOLD") || matName.contains("DIAMOND") || matName.contains("ANCIENT_DEBRIS")) {
            if (plugin.getQuestManager() != null) {
                plugin.getQuestManager().recordAction(player, com.cytricos.races.manager.QuestManager.QuestType.MINE_BLOCKS, 1);
            }
        }

        if (data != null && data.getRace().equalsIgnoreCase("dwarf") && data.getLevel() >= 2) {
            if (matName.contains("_ORE")) {
                if (Math.random() < 0.15) { // 15% prob. doble mineral
                    block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(block.getType(), 1));
                    player.sendMessage(ColorUtils.color("&6&l[MINERÍA ENANA] &7¡Tu destreza subterránea ha extraído doble mineral!"));
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7f, 1.8f);
                }
            }
        }
    }

    // =========================================================================
    // 10. ACTIVACIÓN DE HABILIDADES RACIALES INTERACTIVAS
    // =========================================================================

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getItem() != null && plugin.getCustomItemManager() != null
                && plugin.getCustomItemManager().isAnyCustomItem(event.getItem())) {
            return;
        }
        Player player = event.getPlayer();
        Action action = event.getAction();
        boolean isRightClick = action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK;

        // Activación por interacción física (Shift + Clic Derecho con mano principal)
        if (player.isSneaking() && isRightClick) {
            triggerActiveAbility(player);
        }
    }

    /**
     * Ejecuta la habilidad activa correspondiente a la raza y nivel del jugador.
     * Invocable tanto por interacción física como por el comando /raza habilidad.
     */
    public void triggerActiveAbility(Player player) {
        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        String raceId = data.getRace().toLowerCase();
        int level = data.getLevel();

        switch (raceId) {
            case "werewolf" -> {
                if (level >= 6) {
                    if (checkCooldown(player, "FormaFenrir", 120)) {
                        beastFormActive.add(player.getUniqueId());
                        if (plugin.getLibsDisguisesHook() != null) {
                            plugin.getLibsDisguisesHook().disguiseAsWolf(player, true);
                        }
                        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 300, 2));
                        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 300, 1));
                        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 300, 0));
                        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WOLF_HOWL, 1.5f, 0.6f);
                        player.getWorld().spawnParticle(Particle.FLAME, player.getLocation(), 30, 0.5, 0.5, 0.5, 0.1);
                        ColorUtils.sendMessage(player, "&4&l[FENRIR] &c¡Has despertado la Forma Bestial Ancestral por 15 segundos!");

                        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                            beastFormActive.remove(player.getUniqueId());
                            if (plugin.getLibsDisguisesHook() != null) {
                                plugin.getLibsDisguisesHook().undisguise(player);
                            }
                            ColorUtils.sendMessage(player, "&7Tu transformación en Fenrir ha concluido.");
                        }, 300L);
                    }
                } else if (level >= 3) {
                    if (checkCooldown(player, "RugidoAterrador", 45)) {
                        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WOLF_GROWL, 1.5f, 0.7f);
                        player.getWorld().spawnParticle(Particle.SONIC_BOOM, player.getLocation().add(0, 1, 0), 1);
                        for (Entity e : player.getNearbyEntities(10, 5, 10)) {
                            if (e instanceof LivingEntity target && target != player) {
                                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 120, 2));
                                ColorUtils.sendMessage(target, "&c&l¡El rugido del alfa te ha paralizado de miedo!");
                            }
                        }
                        ColorUtils.sendMessage(player, "&a&l[HABILIDAD] &7¡Has desatado tu Rugido Aterrador!");
                    }
                } else {
                    ColorUtils.sendMessage(player, "&cRequieres Nivel 3 o superior para desbloquear habilidades activas.");
                }
            }

            case "vampire" -> {
                if (level >= 5) {
                    if (checkCooldown(player, "FormaMurcielago", 60)) {
                        batFormActive.add(player.getUniqueId());
                        if (plugin.getLibsDisguisesHook() != null) {
                            plugin.getLibsDisguisesHook().disguiseAsBat(player);
                        }
                        player.setAllowFlight(true);
                        player.setFlying(true);
                        player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 160, 0));
                        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BAT_TAKEOFF, 1.2f, 0.9f);
                        player.getWorld().spawnParticle(Particle.LARGE_SMOKE, player.getLocation(), 20, 0.3, 0.3, 0.3, 0.05);
                        ColorUtils.sendMessage(player, "&5&l[FORMA QUIRÓPTERA] &d¡Te has transformado en murciélago por 8s!");

                        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                            batFormActive.remove(player.getUniqueId());
                            if (plugin.getLibsDisguisesHook() != null) {
                                plugin.getLibsDisguisesHook().undisguise(player);
                            }
                            if (player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR) {
                                player.setFlying(false);
                                player.setAllowFlight(false);
                            }
                            ColorUtils.sendMessage(player, "&7Tu metamorfosis en murciélago ha finalizado.");
                        }, 160L);
                    }
                } else if (level >= 4) {
                    if (checkCooldown(player, "SaltoSombras", 20)) {
                        Location target = player.getLocation().add(player.getLocation().getDirection().multiply(12));
                        player.teleport(target);
                        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.2f);
                        player.getWorld().spawnParticle(Particle.PORTAL, player.getLocation(), 25, 0.3, 0.5, 0.3, 0.1);
                        ColorUtils.sendMessage(player, "&5&l[HABILIDAD] &7¡Te has desplazado entre las sombras!");
                    }
                } else {
                    ColorUtils.sendMessage(player, "&cRequieres Nivel 4 o superior para desbloquear habilidades activas.");
                }
            }

            case "dragonkin" -> {
                if (level >= 3) {
                    if (checkCooldown(player, "AlientoFuego", 30)) {
                        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.4f);
                        for (int i = 1; i <= 6; i++) {
                            Location cone = player.getEyeLocation().add(player.getLocation().getDirection().multiply(i));
                            player.getWorld().spawnParticle(Particle.FLAME, cone, 15, 0.3, 0.3, 0.3, 0.05);
                            for (Entity e : cone.getWorld().getNearbyEntities(cone, 1.5, 1.5, 1.5)) {
                                if (e instanceof LivingEntity target && target != player) {
                                    if (plugin.getWorldGuardHook() == null || !(target instanceof Player pVic) || plugin.getWorldGuardHook().canPvp(player, pVic.getLocation())) {
                                        target.setFireTicks(100);
                                        target.damage(5.0, player);
                                    }
                                }
                            }
                        }
                        ColorUtils.sendMessage(player, "&c&l[HABILIDAD] &7¡Has desatado tu Aliento de Fuego!");
                    }
                } else {
                    ColorUtils.sendMessage(player, "&cRequieres Nivel 3 para desbloquear Aliento de Fuego.");
                }
            }

            case "orc" -> {
                if (level >= 3) {
                    if (checkCooldown(player, "GritoGuerra", 45)) {
                        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PIGLIN_BRUTE_ANGRY, 1.2f, 0.8f);
                        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 200, 1));
                        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 200, 0));
                        for (Entity e : player.getNearbyEntities(8, 4, 8)) {
                            if (e instanceof Player ally && ally != player) {
                                ally.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 160, 0));
                                ColorUtils.sendMessage(ally, "&4¡El grito de guerra orco te llena de adrenalina combativa!");
                            }
                        }
                        ColorUtils.sendMessage(player, "&4&l[HABILIDAD] &7¡Grito de Guerra activado!");
                    }
                } else {
                    ColorUtils.sendMessage(player, "&cRequieres Nivel 3 para desbloquear Grito de Guerra.");
                }
            }

            case "giant" -> {
                if (level >= 3) {
                    if (checkCooldown(player, "PisotonTitanico", 40)) {
                        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_IRON_GOLEM_ATTACK, 1.5f, 0.6f);
                        player.getWorld().spawnParticle(Particle.EXPLOSION, player.getLocation(), 3);
                        for (Entity e : player.getNearbyEntities(6, 3, 6)) {
                            if (e instanceof LivingEntity target && target != player) {
                                if (plugin.getWorldGuardHook() == null || !(target instanceof Player pVic) || plugin.getWorldGuardHook().canPvp(player, pVic.getLocation())) {
                                    target.setVelocity(new Vector(0, 1.2, 0));
                                    target.damage(6.0, player);
                                    ColorUtils.sendMessage(target, "&c&l¡El pisotón de un gigante te ha lanzado por los aires!");
                                }
                            }
                        }
                        ColorUtils.sendMessage(player, "&6&l[HABILIDAD] &7¡Pisotón Titánico ejecutado!");
                    }
                } else {
                    ColorUtils.sendMessage(player, "&cRequieres Nivel 3 para desbloquear Pisotón Titánico.");
                }
            }

            case "dwarf" -> {
                if (level >= 4) {
                    if (checkCooldown(player, "PielPiedra", 60)) {
                        stoneSkinActive.add(player.getUniqueId());
                        ColorUtils.sendMessage(player, "&e&l[HABILIDAD] &7Piel de Piedra activa: ¡Inmune a flechas durante 6s!");
                        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1.0f, 1.2f);
                        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                            stoneSkinActive.remove(player.getUniqueId());
                            ColorUtils.sendMessage(player, "&7Piel de Piedra ha expirado.");
                        }, 120L);
                    }
                } else {
                    ColorUtils.sendMessage(player, "&cRequieres Nivel 4 para desbloquear Piel de Piedra.");
                }
            }

            case "druid" -> {
                if (level >= 3) {
                    if (checkCooldown(player, "ExplosionVitaminica", 35)) {
                        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_SPLASH_POTION_THROWN, 1.0f, 1.2f);
                        player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation().add(0, 1, 0), 20, 1.5, 0.5, 1.5, 0.1);
                        for (Entity e : player.getNearbyEntities(7, 4, 7)) {
                            if (e instanceof Player ally) {
                                ally.setHealth(Math.min(ally.getMaxHealth(), ally.getHealth() + 8.0));
                                ColorUtils.sendMessage(ally, "&e¡Un rocío de zumo vitamínico te ha curado 4 corazones!");
                            }
                        }
                        player.setHealth(Math.min(player.getMaxHealth(), player.getHealth() + 8.0));
                        ColorUtils.sendMessage(player, "&a&l[HABILIDAD] &7¡Explosión Vitamínica desatada!");
                    }
                } else {
                    ColorUtils.sendMessage(player, "&cRequieres Nivel 3 para desbloquear Explosión Vitamínica.");
                }
            }

            case "yokai" -> {
                if (level >= 3) {
                    if (checkCooldown(player, "PasoEspectral", 40)) {
                        Location targetLoc = player.getLocation().add(player.getLocation().getDirection().multiply(4));
                        if (plugin.getWorldGuardHook() != null && !plugin.getWorldGuardHook().canBuild(player, targetLoc)) {
                            ColorUtils.sendMessage(player, "&cNo puedes atravesar hacia una zona protegida.");
                            return;
                        }
                        player.teleport(targetLoc);
                        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 60, 0));
                        player.getWorld().playSound(player.getLocation(), Sound.PARTICLE_SOUL_ESCAPE, 1.2f, 1.4f);
                        player.getWorld().spawnParticle(Particle.SOUL, player.getLocation(), 20, 0.3, 0.5, 0.3, 0.05);
                        ColorUtils.sendMessage(player, "&3&l[PASO ESPECTRAL] &b¡Te has desfasado atravesando la materia!");
                    }
                } else {
                    ColorUtils.sendMessage(player, "&cRequieres Nivel 3 para desbloquear Paso Espectral.");
                }
            }

            case "fairy" -> {
                if (level >= 3) {
                    if (checkCooldown(player, "PolvoHada", 40)) {
                        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.2f, 1.8f);
                        player.getWorld().spawnParticle(Particle.END_ROD, player.getLocation().add(0, 1, 0), 25, 1.0, 0.5, 1.0, 0.05);
                        for (Entity e : player.getNearbyEntities(6, 4, 6)) {
                            if (e instanceof Player ally && ally != player) {
                                ally.setHealth(Math.min(ally.getMaxHealth(), ally.getHealth() + 6.0));
                                ColorUtils.sendMessage(ally, "&d¡Polvo de hada reluciente te ha curado 3 corazones!");
                            } else if (e instanceof LivingEntity target && target != player) {
                                if (plugin.getWorldGuardHook() == null || !(target instanceof Player pVic) || plugin.getWorldGuardHook().canPvp(player, pVic.getLocation())) {
                                    target.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 60, 0));
                                }
                            }
                        }
                        ColorUtils.sendMessage(player, "&d&l[HABILIDAD] &7¡Polvo de Hada liberado!");
                    }
                } else {
                    ColorUtils.sendMessage(player, "&cRequieres Nivel 3 para desbloquear Polvo de Hada.");
                }
            }

            case "angel" -> {
                if (level >= 4) {
                    if (checkCooldown(player, "JuicioCelestial", 60)) {
                        player.getWorld().playSound(player.getLocation(), Sound.ITEM_TOTEM_USE, 1.0f, 1.6f);
                        player.getWorld().spawnParticle(Particle.FLASH, player.getLocation().add(0, 2, 0), 5);
                        for (Entity e : player.getNearbyEntities(8, 4, 8)) {
                            if (e instanceof Player ally) {
                                ally.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 200, 1));
                                ColorUtils.sendMessage(ally, "&e¡Una bendición angélica te ha concedido escudo divino!");
                            }
                        }
                        ColorUtils.sendMessage(player, "&e&l[HABILIDAD] &7¡Juicio Celestial desatado!");
                    }
                } else {
                    ColorUtils.sendMessage(player, "&cRequieres Nivel 4 para desbloquear Juicio Celestial.");
                }
            }

            case "demon" -> {
                if (level >= 3) {
                    if (checkCooldown(player, "LlamaradaAbisal", 35)) {
                        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1.2f, 0.7f);
                        player.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, player.getLocation().add(0, 1, 0), 30, 1.5, 0.5, 1.5, 0.08);
                        for (Entity e : player.getNearbyEntities(6, 3, 6)) {
                            if (e instanceof LivingEntity target && target != player) {
                                if (plugin.getWorldGuardHook() == null || !(target instanceof Player pVic) || plugin.getWorldGuardHook().canPvp(player, pVic.getLocation())) {
                                    target.setFireTicks(120);
                                    target.damage(4.0, player);
                                }
                            }
                        }
                        ColorUtils.sendMessage(player, "&4&l[HABILIDAD] &7¡Llamarada Abisal activa!");
                    }
                } else {
                    ColorUtils.sendMessage(player, "&cRequieres Nivel 3 para desbloquear Llamarada Abisal.");
                }
            }

            case "goblin" -> {
                if (level >= 4) {
                    if (checkCooldown(player, "BombaHumo", 45)) {
                        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.0f, 0.8f);
                        player.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, player.getLocation(), 40, 2.0, 1.0, 2.0, 0.05);
                        for (Entity e : player.getNearbyEntities(5, 3, 5)) {
                            if (e instanceof LivingEntity target && target != player) {
                                target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 80, 0));
                            }
                        }
                        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 100, 2));
                        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 100, 0));
                        ColorUtils.sendMessage(player, "&2&l[HABILIDAD] &a¡Bomba de humo detonada! Escapas a gran velocidad.");
                    }
                } else {
                    ColorUtils.sendMessage(player, "&cRequieres Nivel 4 para desbloquear Bomba de Humo.");
                }
            }

            case "human" -> {
                if (level >= 2) {
                    if (checkCooldown(player, "SegundoAliento", 50)) {
                        player.setHealth(Math.min(player.getMaxHealth(), player.getHealth() + 6.0));
                        player.setFoodLevel(Math.min(20, player.getFoodLevel() + 6));
                        player.setSaturation(player.getSaturation() + 4.0f);
                        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_BURP, 0.8f, 1.2f);
                        player.getWorld().spawnParticle(Particle.HEART, player.getLocation().add(0, 1.5, 0), 4, 0.2, 0.2, 0.2, 0.05);
                        ColorUtils.sendMessage(player, "&6&l[SEGUNDO ALIENTO] &eHas recuperado vitalidad y nutrición táctica.");
                    }
                } else {
                    ColorUtils.sendMessage(player, "&cRequieres Nivel 2 para Segundo Aliento.");
                }
            }

            case "beastkin" -> {
                if (level >= 3) {
                    if (checkCooldown(player, "SentidoSalvaje", 40)) {
                        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 160, 1));
                        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_OCELOT_DEATH, 0.8f, 1.4f);
                        for (Entity e : player.getNearbyEntities(15, 6, 15)) {
                            if (e instanceof LivingEntity target && target != player) {
                                target.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 160, 0));
                            }
                        }
                        ColorUtils.sendMessage(player, "&6&l[SENTIDO SALVAJE] &eTus instintos rastrean a las presas cercanas.");
                    }
                } else {
                    ColorUtils.sendMessage(player, "&cRequieres Nivel 3 para Sentido Salvaje.");
                }
            }

            case "undead" -> {
                if (level >= 3) {
                    if (checkCooldown(player, "NubePutrida", 45)) {
                        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.7f, 1.6f);
                        player.getWorld().spawnParticle(Particle.SNEEZE, player.getLocation(), 35, 1.5, 0.5, 1.5, 0.05);
                        for (Entity e : player.getNearbyEntities(6, 3, 6)) {
                            if (e instanceof LivingEntity target && target != player) {
                                if (plugin.getWorldGuardHook() == null || !(target instanceof Player pVic) || plugin.getWorldGuardHook().canPvp(player, pVic.getLocation())) {
                                    target.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 100, 0));
                                    target.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 100, 0));
                                }
                            }
                        }
                        ColorUtils.sendMessage(player, "&2&l[NUBE PÚTRIDA] &aEmanas vapores pestilentes.");
                    }
                } else {
                    ColorUtils.sendMessage(player, "&cRequieres Nivel 3 para Nube Pútrida.");
                }
            }

            case "elf" -> {
                if (level >= 3) {
                    if (checkCooldown(player, "RafagaSilvana", 40)) {
                        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 160, 1));
                        Vector dir = player.getLocation().getDirection();
                        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1.2f, 1.3f);
                        for (double offset : new double[]{-0.2, 0.0, 0.2}) {
                            Vector spreadDir = dir.clone().add(new Vector(offset, 0, offset)).normalize();
                            Arrow arrow = player.launchProjectile(Arrow.class, spreadDir.multiply(2.0));
                            arrow.setShooter(player);
                            arrow.setDamage(arrow.getDamage() * 1.2);
                        }
                        ColorUtils.sendMessage(player, "&a&l[RÁFAGA SILVANA] &2¡Disparas una descarga de flechas rápidas!");
                    }
                } else {
                    ColorUtils.sendMessage(player, "&cRequieres Nivel 3 para Ráfaga Silvana.");
                }
            }

            default -> ColorUtils.sendMessage(player, "&7Tu raza no posee habilidades activas.");
        }
    }

    // =========================================================================
    // 11. MUERTE Y RESURRECCIÓN (ARCHILICH UNDEAD L6 & FILACTERIA L4)
    // =========================================================================

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        String raceId = data.getRace().toLowerCase();
        int level = data.getLevel();

        beastFormActive.remove(player.getUniqueId());
        batFormActive.remove(player.getUniqueId());
        stoneSkinActive.remove(player.getUniqueId());
        if (plugin.getLibsDisguisesHook() != null) {
            plugin.getLibsDisguisesHook().undisguise(player);
        }

        // UNDEAD L6 (Archilich): Revivir instantáneamente 1 vez por hora
        if (raceId.equals("undead") && level >= 6) {
            if (checkCooldown(player, "ArchilichRevive", 3600)) {
                event.setKeepInventory(true);
                event.setKeepLevel(true);
                event.getDrops().clear();
                ColorUtils.sendMessage(player, "&2&l[ARCHILICH]&a ¡Tu filacteria inmortal desafía a la muerte! Revives en combate.");
                player.getWorld().playSound(player.getLocation(), Sound.ITEM_TOTEM_USE, 1.0f, 0.8f);
                player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, player.getLocation(), 50, 0.5, 1.0, 0.5, 0.2);

                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    player.spigot().respawn();
                    player.setHealth(player.getMaxHealth() * 0.50);
                    player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 100, 2));
                }, 2L);
                return;
            }
        }

        // UNDEAD L4 (Filacteria Portátil): Conserva 50% de inventario/XP al morir (CD 30m)
        if (raceId.equals("undead") && level >= 4) {
            if (checkCooldown(player, "FilacteriaPortatil", 1800)) {
                event.setKeepLevel(true);
                ColorUtils.sendMessage(player, "&b&l[FILACTERIA]&7 Tu alma retiene tu experiencia y posesiones esenciales.");
            }
        }
    }

    // =========================================================================
    // 12. INTERACCIONES BIOLÓGICAS CON MOBS (EXPANSIÓN RPG v7.0)
    // =========================================================================

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityTarget(EntityTargetLivingEntityEvent event) {
        if (!(event.getTarget() instanceof Player player)) return;
        if (event.getEntity() == null) return;

        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        if (data == null) return;

        String raceId = data.getRace().toLowerCase();
        EntityType entityType = event.getEntity().getType();

        // 1. No-Muerto: Los no-muertos menores (esqueletos, zombis, ahogados, strays, husks) reconocen al no-muerto como aliado
        if (raceId.equals("undead")) {
            if (entityType == EntityType.ZOMBIE || entityType == EntityType.SKELETON ||
                    entityType == EntityType.DROWNED || entityType == EntityType.HUSK ||
                    entityType == EntityType.STRAY || entityType == EntityType.WITHER_SKELETON) {
                // Solo ignora si no fue provocado por daño reciente del jugador
                if (event.getReason() == EntityTargetLivingEntityEvent.TargetReason.CLOSEST_PLAYER ||
                        event.getReason() == EntityTargetLivingEntityEvent.TargetReason.RANDOM_TARGET) {
                    event.setCancelled(true);
                }
            }
        }

        // 2. HombreLobo & Beastkin: Los cánidos y felinos salvajes no atacan a linajes bestiales
        if (raceId.equals("werewolf") || raceId.equals("beastkin")) {
            if (entityType == EntityType.WOLF || entityType == EntityType.FOX || entityType == EntityType.OCELOT) {
                if (event.getReason() != EntityTargetLivingEntityEvent.TargetReason.TARGET_ATTACKED_ENTITY) {
                    event.setCancelled(true);
                }
            }
        }

        // 3. Demonio: Habitantes del inframundo respetan la sangre abisal
        if (raceId.equals("demon")) {
            if (entityType == EntityType.PIGLIN || entityType == EntityType.ZOMBIFIED_PIGLIN || entityType == EntityType.HOGLIN) {
                if (event.getReason() != EntityTargetLivingEntityEvent.TargetReason.TARGET_ATTACKED_ENTITY) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityKill(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;
        PlayerData data = plugin.getPlayerDataManager().get(killer.getUniqueId());
        if (data != null) {
            data.incrementMobsDefeated();
        }
        if (plugin.getQuestManager() != null) {
            plugin.getQuestManager().recordAction(killer, com.cytricos.races.manager.QuestManager.QuestType.KILL_MOBS, 1);
        }
    }
}


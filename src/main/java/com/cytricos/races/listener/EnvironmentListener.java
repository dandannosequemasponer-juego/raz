package com.cytricos.races.listener;

import com.cytricos.races.CytricosRaces;
import com.cytricos.races.model.PlayerData;
import com.cytricos.races.util.ColorUtils;
import org.bukkit.*;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.type.RedstoneLamp;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

/**
 * Motor contextual de entorno, biomas, radiación solar progresiva (Lv 1-6) y pasivas ambientales
 * para las 16 razas de CYTRICOS RPG.
 * Lead Developers: Dandan_13 & Wolfit12
 */
public class EnvironmentListener implements Listener {
    private final CytricosRaces plugin;
    private long solarTickCycle = 0L;

    public EnvironmentListener(CytricosRaces plugin) {
        this.plugin = plugin;
        startEnvironmentTask();
        startSolarExposureTask();
    }

    private void startEnvironmentTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (!player.isValid() || player.isDead()) continue;
                PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
                if (data == null || !data.hasAssignedRace()) continue;

                applyContextualBuffs(player, data.getRace().toLowerCase(), data.getLevel());
            }
        }, 40L, 40L);
    }

    /**
     * Fase 5: Vulnerabilidad Solar Progresiva (Lv 1-6) + Protecciones + Extinción Instantánea (cada 10 ticks = 0.5s).
     */
    private void startSolarExposureTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            solarTickCycle++;
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (!player.isValid() || player.isDead()) continue;
                if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
                    continue;
                }
                PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
                if (data == null || !data.hasAssignedRace()) continue;

                String raceId = data.getRace().toLowerCase();
                int level = data.getLevel();
                Location loc = player.getLocation();
                World world = player.getWorld();

                // Mecánica Beastkin: olfato rastrero al agacharse (partículas hacia entidades en 8 bloques)
                if (raceId.equals("beastkin") && player.isSneaking() && data.isSettingParticles()) {
                    emitBeastkinScent(player);
                }

                // Mecánica Dwarf: hundimiento más rápido y mayor consumo de oxígeno bajo el agua
                if (raceId.equals("dwarf") && player.isInWater()) {
                    Vector vel = player.getVelocity();
                    if (vel.getY() > -0.12) {
                        player.setVelocity(vel.add(new Vector(0, -0.05, 0)));
                    }
                    if (player.getRemainingAir() > 6 && solarTickCycle % 2 == 0) {
                        player.setRemainingAir(Math.max(0, player.getRemainingAir() - 8));
                    }
                }

                // Mecánica Angel: peto pesado desactiva el vuelo
                if (raceId.equals("angel")) {
                    ItemStack chest = player.getInventory().getChestplate();
                    boolean heavy = chest != null && (chest.getType() == Material.IRON_CHESTPLATE
                            || chest.getType() == Material.DIAMOND_CHESTPLATE
                            || chest.getType() == Material.NETHERITE_CHESTPLATE);
                    if (heavy && player.isFlying()) {
                        player.setFlying(false);
                        player.setAllowFlight(false);
                        ColorUtils.sendActionBar(player, "&c&l[ÁNGEL] &7¡Tu armadura pesada impide el vuelo celestial!");
                    }
                }

                // Proximidad a bloques especiales (Redstone / Faros) para Vampiros y Yokai
                if (solarTickCycle % 2 == 0 && (raceId.equals("vampire") || raceId.equals("yokai"))) {
                    checkLightAndBlockAuras(player, raceId, level, loc);
                }

                if (!raceId.equals("vampire") && !raceId.equals("undead")) {
                    continue;
                }

                // Lv 6: 100% Inmune al sol en toda circunstancia
                if (level >= 6) {
                    if (player.getFireTicks() > 0) player.setFireTicks(0);
                    continue;
                }

                boolean isNormalEnv = world.getEnvironment() == World.Environment.NORMAL;
                boolean isDay = world.getTime() >= 0 && world.getTime() <= 12300;
                boolean isStormOrRain = world.hasStorm() || world.isThundering();
                boolean inSafeZone = plugin.getWorldGuardHook() != null && plugin.getWorldGuardHook().isInSafeZone(player, loc);

                // Lluvia ácida / corrosión para Vampiro bajo tormenta descubierta
                if (raceId.equals("vampire") && isStormOrRain && loc.getBlock().getLightFromSky() >= 15 && !inSafeZone) {
                    if (solarTickCycle % 4 == 0 && level < 5) {
                        player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 60, 0, false, false));
                        ColorUtils.sendActionBar(player, "&9&l[TORMENTA] &7La lluvia debilita tu esencia vampírica.");
                    }
                }

                if (!isNormalEnv || !isDay || isStormOrRain || inSafeZone) {
                    if (player.getFireTicks() > 0 && !isInLavaOrFireBlock(player)) {
                        player.setFireTicks(0);
                    }
                    continue;
                }

                // Protecciones estrictas:
                // 1) Cualquier casco equipado
                ItemStack helmet = player.getInventory().getHelmet();
                boolean hasAnyHelmet = helmet != null && helmet.getType() != Material.AIR;

                // 2) Sumergido en agua
                boolean inWater = player.isInWater();

                // 3) Sombra o bajo techo sólido
                boolean inShadeOrUnderRoof = isProtectedByRoofOrShade(world, loc);

                if (hasAnyHelmet || inWater || inShadeOrUnderRoof) {
                    // EXTINCIÓN INMEDIATA EN EL TICK EXACTO DE PROTECCIÓN
                    if (player.getFireTicks() > 0) {
                        player.setFireTicks(0);
                    }
                    if (hasAnyHelmet && solarTickCycle % 6 == 0) {
                        degradeHelmet(player, helmet);
                    }
                    continue;
                }

                // A pleno sol sin protección -> Vulnerabilidad gradual Lv 1-5
                if (level <= 3) {
                    // Lv 1-3: Combustión completa
                    player.setFireTicks(Math.max(player.getFireTicks(), 60));
                    if (solarTickCycle % 2 == 0) {
                        player.damage(1.5);
                        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 50, 1, false, false));
                        player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 50, 0, false, false));
                        ColorUtils.sendActionBar(player, "&4&l¡RADIACIÓN SOLAR! &cCúbrete bajo un techo, agua o ponte un casco.");
                    }
                } else if (level == 4) {
                    // Lv 4: 50% menos de fuego y daño
                    player.setFireTicks(Math.max(player.getFireTicks(), 30));
                    if (solarTickCycle % 2 == 0) {
                        player.damage(0.75);
                        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 0, false, false));
                        ColorUtils.sendActionBar(player, "&6&l[SOL MITIGADO Lv.4] &eSufres quemaduras reducidas al 50%.");
                    }
                } else {
                    // Lv 5: Quemadura mínima
                    player.setFireTicks(Math.max(player.getFireTicks(), 15));
                    if (solarTickCycle % 4 == 0) {
                        player.damage(0.5);
                        ColorUtils.sendActionBar(player, "&e&l[RESISTENCIA SOLAR Lv.5] &7El sol apenas logra abrasar tu piel.");
                    }
                }

                if (solarTickCycle % 2 == 0 && data.isSettingParticles()) {
                    world.spawnParticle(Particle.SMOKE, loc.clone().add(0, 1.0, 0), 5, 0.2, 0.35, 0.2, 0.02);
                }
            }
        }, 20L, 10L);
    }

    private boolean isProtectedByRoofOrShade(World world, Location loc) {
        Block feet = loc.getBlock();
        if (feet.getLightFromSky() < 15) {
            return true;
        }
        int bx = loc.getBlockX();
        int bz = loc.getBlockZ();
        int startY = loc.getBlockY() + 1;
        int highestY = world.getHighestBlockYAt(bx, bz);
        if (highestY >= startY) {
            for (int y = startY; y <= highestY; y++) {
                Material m = world.getBlockAt(bx, y, bz).getType();
                if (m.isOccluding() || m.isSolid() || m.name().contains("LEAVES")
                        || m.name().contains("SLAB") || m.name().contains("STAIRS")) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isInLavaOrFireBlock(Player player) {
        Material m = player.getLocation().getBlock().getType();
        return m == Material.FIRE || m == Material.SOUL_FIRE || m == Material.LAVA || m == Material.CAMPFIRE;
    }

    private void emitBeastkinScent(Player player) {
        Location eye = player.getEyeLocation();
        for (Entity nearby : player.getNearbyEntities(8.0, 5.0, 8.0)) {
            if (nearby instanceof LivingEntity target && target != player) {
                Vector dir = target.getEyeLocation().toVector().subtract(eye.toVector()).normalize();
                Location mark = eye.clone().add(dir.multiply(1.4));
                player.spawnParticle(Particle.END_ROD, mark, 1, 0.05, 0.05, 0.05, 0.0);
                break;
            }
        }
    }

    private void checkLightAndBlockAuras(Player player, String raceId, int level, Location loc) {
        if (level >= 5) return;
        World w = loc.getWorld();
        int cx = loc.getBlockX();
        int cy = loc.getBlockY();
        int cz = loc.getBlockZ();

        int r = raceId.equals("yokai") ? 6 : 4;
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    Block b = w.getBlockAt(cx + dx, cy + dy, cz + dz);
                    Material m = b.getType();
                    if (raceId.equals("vampire")) {
                        boolean litRedstone = m == Material.REDSTONE_BLOCK
                                || (m == Material.REDSTONE_LAMP && b.getBlockData() instanceof RedstoneLamp lamp && lamp.isLit())
                                || m == Material.BEACON;
                        if (litRedstone) {
                            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 50, 1, false, false));
                            ColorUtils.sendActionBar(player, "&c&l[AURA DE REDSTONE] &7La energía brillante entorpece tus pasos.");
                            return;
                        }
                    } else if (raceId.equals("yokai") && m == Material.BEACON) {
                        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 50, 0, false, false));
                        ColorUtils.sendActionBar(player, "&e&l[FARO SAGRADO] &7La luz del Faro nubla tu visión espectral.");
                        return;
                    }
                }
            }
        }
    }

    private void degradeHelmet(Player player, ItemStack helmet) {
        if (helmet.getItemMeta() instanceof Damageable damageable) {
            damageable.setDamage(damageable.getDamage() + 1);
            if (damageable.getDamage() >= helmet.getType().getMaxDurability()) {
                player.getInventory().setHelmet(null);
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
                ColorUtils.sendMessage(player, "&c&l¡Tu casco se ha roto por la intensa radiación solar!");
            } else {
                helmet.setItemMeta(damageable);
            }
        }
    }

    private void applyContextualBuffs(Player player, String raceId, int level) {
        Location loc = player.getLocation();
        World world = player.getWorld();
        long time = world.getTime();
        boolean isNight = time >= 13000 && time <= 23000;
        boolean isDay = time >= 0 && time < 12300;
        boolean isNether = world.getEnvironment() == World.Environment.NETHER;
        Biome biome = loc.getBlock().getBiome();
        String biomeName = biome.name();

        switch (raceId) {
            case "werewolf" -> {
                if (isNight) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60, 1, false, false));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 120, 0, false, false));
                    // Luna llena: frenesí nocturno
                    long dayCount = (world.getFullTime() / 24000L) % 8L;
                    if (dayCount == 0L) {
                        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 60, 0, false, false));
                    }
                }
            }

            case "vampire" -> {
                if (isNight || isNether) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 60, 0, false, false));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 60, 0, false, false));
                }
            }

            case "dragonkin" -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 100, 0, false, false));
                boolean coldBiome = biomeName.contains("SNOW") || biomeName.contains("ICE") || biomeName.contains("FROZEN");
                if (coldBiome && player.isInWater() && level < 6) {
                    int amp = (level >= 4) ? 0 : 2;
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, amp, false, false));
                }
            }

            case "beastkin" -> {
                boolean nature = biomeName.contains("FOREST") || biomeName.contains("TAIGA") || biomeName.contains("SAVANNA");
                if (nature) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60, 0, false, false));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 120, 0, false, false));
                }
            }

            case "elf" -> {
                boolean woods = biomeName.contains("FOREST") || biomeName.contains("JUNGLE") || biomeName.contains("TAIGA");
                if (woods) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60, 0, false, false));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 60, 1, false, false));
                }
            }

            case "fairy" -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 60, 0, false, false));
                if (player.isInWater() && level < 5) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 0, false, false));
                }
            }

            case "orc" -> {
                boolean peaks = biomeName.contains("MOUNTAIN") || biomeName.contains("PEAKS") || biomeName.contains("BADLANDS") || biomeName.contains("WINDSWEPT");
                if (peaks) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 60, 0, false, false));
                }
            }

            case "druid" -> {
                boolean green = biomeName.contains("MEADOW") || biomeName.contains("FLOWER") || biomeName.contains("FOREST") || biomeName.contains("CHERRY");
                if (green && isDay) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 60, 0, false, false));
                    if (player.getFoodLevel() < 20) {
                        player.setFoodLevel(Math.min(20, player.getFoodLevel() + 1));
                    }
                }
                // Aceleración pasiva de cultivos en radio 3x3
                for (int dx = -3; dx <= 3; dx++) {
                    for (int dz = -3; dz <= 3; dz++) {
                        Block b = world.getBlockAt(loc.getBlockX() + dx, loc.getBlockY(), loc.getBlockZ() + dz);
                        if (b.getBlockData() instanceof Ageable age && age.getAge() < age.getMaximumAge() && Math.random() < 0.14) {
                            age.setAge(age.getAge() + 1);
                            b.setBlockData(age);
                        }
                    }
                }
                Material under = loc.clone().subtract(0, 0.5, 0).getBlock().getType();
                if (biomeName.contains("DESERT") || biomeName.contains("BADLANDS")
                        || under == Material.STONE || under == Material.DEEPSLATE) {
                    if (level < 6) {
                        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 0, false, false));
                    }
                }
            }

            case "dwarf" -> {
                if (loc.getY() <= 60) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 80, 1, false, false));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 120, 0, false, false));
                }
            }

            case "demon" -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 100, 0, false, false));
                if (isNether) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 60, 0, false, false));
                }
                if (level < 4 && (player.isInWater() || (world.hasStorm() && loc.getBlock().getLightFromSky() == 15))) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 60, 0, false, false));
                }
            }

            case "yokai" -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 60, 0, false, false));
            }

            case "giant" -> {
                if (level < 6) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 0, false, false));
                }
            }

            case "angel" -> {
                if (isDay && !world.hasStorm() && !isProtectedByRoofOrShade(world, loc)) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 60, 0, false, false));
                }
                // Si tiene vuelo activo en survival, consume algo más de saturación (+20%)
                if (player.isFlying() && player.getFoodLevel() > 4 && Math.random() < 0.15) {
                    player.setExhaustion(player.getExhaustion() + 0.5f);
                }
            }

            case "goblin" -> {
                if (player.getInventory().getItemInOffHand().getType() == Material.AIR) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60, 1, false, false));
                }
                // Inventario lleno -> lentitud
                if (player.getInventory().firstEmpty() == -1 && level < 6) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 0, false, false));
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onExpChange(PlayerExpChangeEvent event) {
        Player player = event.getPlayer();
        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        if (data != null && data.hasAssignedRace() && data.getRace().equalsIgnoreCase("human")) {
            int bonus = (int) Math.ceil(event.getAmount() * 0.10);
            event.setAmount(event.getAmount() + bonus);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onFoodChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        if (data == null || !data.hasAssignedRace()) return;
        if (data.getRace().equalsIgnoreCase("undead")) {
            event.setCancelled(true);
            player.setFoodLevel(20);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPotionEffect(EntityPotionEffectEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        if (data == null || !data.hasAssignedRace()) return;

        PotionEffect newEff = event.getNewEffect();
        if (newEff == null) return;
        String race = data.getRace().toLowerCase();
        PotionEffectType type = newEff.getType();

        // Veneno bloqueado para No-Muerto, Vampiro y Druida L4+
        if (type == PotionEffectType.POISON && (race.equals("undead") || race.equals("vampire")
                || (race.equals("druid") && data.getLevel() >= 4))) {
            event.setCancelled(true);
            return;
        }

        // Orco: Inmune a Lentitud y Debilidad
        if (race.equals("orc") && (type == PotionEffectType.SLOWNESS || type == PotionEffectType.WEAKNESS)) {
            event.setCancelled(true);
            return;
        }

        // Ángel: Inmune a Ceguera, Oscuridad y Daño Instantáneo
        if (race.equals("angel") && (type == PotionEffectType.BLINDNESS || type == PotionEffectType.DARKNESS || type == PotionEffectType.INSTANT_DAMAGE)) {
            event.setCancelled(true);
            return;
        }

        // Hada: 2x duración en pociones benéficas añadidas
        if (race.equals("fairy") && event.getAction() == EntityPotionEffectEvent.Action.ADDED && isPositiveEffect(type)) {
            int doubled = Math.min(20 * 600, newEff.getDuration() * 2);
            if (doubled > newEff.getDuration()) {
                event.setCancelled(true);
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (player.isOnline()) {
                        player.addPotionEffect(new PotionEffect(type, doubled, newEff.getAmplifier(), newEff.isAmbient(), newEff.hasParticles(), newEff.hasIcon()));
                    }
                });
            }
        }
    }

    private boolean isPositiveEffect(PotionEffectType t) {
        return t == PotionEffectType.SPEED || t == PotionEffectType.STRENGTH || t == PotionEffectType.REGENERATION
                || t == PotionEffectType.RESISTANCE || t == PotionEffectType.FIRE_RESISTANCE || t == PotionEffectType.JUMP_BOOST
                || t == PotionEffectType.ABSORPTION || t == PotionEffectType.HASTE || t == PotionEffectType.NIGHT_VISION
                || t == PotionEffectType.WATER_BREATHING || t == PotionEffectType.INVISIBILITY;
    }
}

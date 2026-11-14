package com.cytricos.races.listener;

import com.cytricos.races.CytricosRaces;
import com.cytricos.races.model.PlayerData;
import com.cytricos.races.util.ColorUtils;
import org.bukkit.*;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
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

/**
 * Motor contextual de buffs y debuffs ambientales por bioma, hora del día y exposición solar (v7.0).
 * Incluye el algoritmo estricto de cobertura/sombra y mitigación progresiva de desventajas (Nivel 1 al 6).
 *
 * Algoritmo Solar para Vampiros y No-Muertos:
 * - Chequeo diurno (0 a 12300 ticks)
 * - Chequeo meteorológico (tormentas anulan la radiación solar)
 * - Chequeo de regiones seguras (WorldGuard SafeZone)
 * - Chequeo milimétrico de techo/bloques opacos: si hay bloques sobre el jugador, SE EXTINGUE EL FUEGO INMEDIATAMENTE
 * - Mitigación por nivel:
 *   - L1-2: Combustión directa si no porta casco; el casco degrada durabilidad.
 *   - L3-4: La luz no quema; aplica Slowness II y Weakness I.
 *   - L5: Sin daño ni slowness si lleva casco.
 *   - L6: Inmunidad absoluta al sol (Imperator Sanguis / Archilich).
 *
 * Lead Developers: Dandan_13 & Wolfit12
 */
public class EnvironmentListener implements Listener {
    private final CytricosRaces plugin;

    public EnvironmentListener(CytricosRaces plugin) {
        this.plugin = plugin;
        startEnvironmentTask();
        startSolarExposureTask();
    }

    private void startEnvironmentTask() {
        // Tarea periódica cada 40 ticks (2 segundos) para buffs contextuales por bioma
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (!player.isValid() || player.isDead()) continue;
                PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
                if (data == null) continue;

                applyContextualBuffs(player, data.getRace().toLowerCase(), data.getLevel());
            }
        }, 40L, 40L);
    }

    /**
     * Tarea recurrente cada 20 ticks (1 segundo) para control solar y cobertura.
     * Implementa el algoritmo exacto de detección de sombra, intemperie y mitigación de debuffs.
     */
    private void startSolarExposureTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
                    continue;
                }
                PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
                if (data == null) continue;

                String raceId = data.getRace().toLowerCase();
                int level = data.getLevel();

                if (!raceId.equals("vampire") && !raceId.equals("undead")) {
                    continue;
                }

                World world = player.getWorld();
                // 1. Chequeo de hora diurna: world.getTime() >= 0 && world.getTime() <= 12300
                boolean isDay = world.getTime() >= 0 && world.getTime() <= 12300;
                if (!isDay) continue;

                // 2. Chequeo de tormenta o lluvia en la ubicación
                if (world.hasStorm() || world.isThundering()) {
                    continue;
                }

                Location loc = player.getLocation();

                // 3. Chequeo de Zona Segura (WorldGuard SafeZone)
                if (plugin.getWorldGuardHook() != null && plugin.getWorldGuardHook().isInSafeZone(player, loc)) {
                    continue;
                }

                // 4. Algoritmo de Cobertura y Sombra:
                // Obtener bloque más alto en coordenadas X,Z del jugador
                int highestBlockY = world.getHighestBlockYAt(loc);
                int playerBlockY = loc.getBlockY();
                boolean isUnderCover = false;

                if (highestBlockY > playerBlockY) {
                    // Escaneo vertical de bloques opacos o semisólidos sobre la cabeza del jugador
                    for (int y = playerBlockY + 2; y <= highestBlockY; y++) {
                        Block b = world.getBlockAt(loc.getBlockX(), y, loc.getBlockZ());
                        Material type = b.getType();
                        if (type.isSolid() || type.name().contains("LEAVES") || type.name().contains("GLASS") ||
                                type.name().contains("SLAB") || type.name().contains("STAIRS") || type.name().contains("ROOF")) {
                            isUnderCover = true;
                            break;
                        }
                    }
                }

                // REGLA CRÍTICA: SI EL JUGADOR ESTÁ BAJO TECHO/SOMBRA, EXTINGUIR FUEGO Y CANCELAR DAÑO SOLAR
                if (isUnderCover) {
                    if (player.getFireTicks() > 0) {
                        player.setFireTicks(0);
                    }
                    continue;
                }

                // A pleno sol: verificar equipamiento de casco con la tabla de progresión por nivel
                ItemStack helmet = player.getInventory().getHelmet();
                Material helmetMat = (helmet != null) ? helmet.getType() : Material.AIR;
                boolean hasAllowedHelmet = isHelmetAllowedForVampire(level, helmetMat);

                if (raceId.equals("vampire")) {
                    // Nivel 6 (Imperator Sanguis): Inmunidad total a la luz solar bajo cualquier condición (sin necesidad de casco)
                    if (level >= 6) {
                        if (player.getFireTicks() > 0) player.setFireTicks(0);
                        continue;
                    }

                    // Nivel 1 al 5: Requiere casco aprobado según su nivel
                    if (hasAllowedHelmet) {
                        if (player.getFireTicks() > 0) player.setFireTicks(0);
                        ColorUtils.sendActionBar(player, "&a&l[PROTECCIÓN SOLAR] &7Tu casco absorbe la radiación.");
                        degradeHelmet(player, helmet);
                    } else {
                        // Casco no aprobado o sin casco -> Combustión y daño solar
                        player.setFireTicks(60);
                        player.damage(1.0);
                        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 1, false, false));
                        player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 40, 0, false, false));
                        String warning = (helmetMat == Material.AIR)
                                ? "&4&l¡COMBUSTIÓN SOLAR! &cBusca sombra o colócate un casco adecuado."
                                : "&4&l¡CASCO INSUFICIENTE! &cTu nivel requiere un casco de mayor calibre.";
                        ColorUtils.sendActionBar(player, warning);
                        player.getWorld().spawnParticle(Particle.SMOKE, loc.clone().add(0, 1, 0), 8, 0.2, 0.4, 0.2, 0.02);
                        player.getWorld().playSound(loc, Sound.ENTITY_GENERIC_BURN, 0.7f, 1.0f);
                    }
                } else if (raceId.equals("undead")) {
                    // No-Muerto Nivel 6 (Archilich): Inmune a la combustión solar
                    if (level >= 6) {
                        if (player.getFireTicks() > 0) player.setFireTicks(0);
                        continue;
                    }
                    if (helmetMat != Material.AIR) {
                        if (player.getFireTicks() > 0) player.setFireTicks(0);
                        degradeHelmet(player, helmet);
                    } else {
                        player.setFireTicks(60);
                        player.damage(1.0);
                        ColorUtils.sendActionBar(player, "&4&l¡COMBUSTIÓN CADAVÉRICA! &cTus restos se calcinan bajo el sol.");
                    }
                }
            }
        }, 20L, 20L);
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

    /**
     * Tabla de progresión estricta de cascos aprobados para Vampiros:
     * - Nivel 1: Netherite o Diamante.
     * - Nivel 2: Hierro, Diamante o Netherite.
     * - Nivel 3: Cota de Malla, Hierro, Diamante o Netherite.
     * - Nivel 4: Oro, Cota de Malla, Hierro, Diamante o Netherite.
     * - Nivel 5: Cualquier casco (incluyendo Cuero / Caparazón de Tortuga).
     * - Nivel 6: No requiere casco (Inmunidad absoluta).
     */
    public static boolean isHelmetAllowedForVampire(int level, Material mat) {
        if (level >= 6) return true;
        if (mat == null || mat == Material.AIR) return false;
        String name = mat.name();
        if (!name.endsWith("_HELMET")) return false;

        return switch (level) {
            case 5 -> true; // Cualquier casco
            case 4 -> name.contains("GOLDEN") || name.contains("CHAINMAIL") || name.contains("IRON") || name.contains("DIAMOND") || name.contains("NETHERITE") || name.contains("TURTLE");
            case 3 -> name.contains("CHAINMAIL") || name.contains("IRON") || name.contains("DIAMOND") || name.contains("NETHERITE");
            case 2 -> name.contains("IRON") || name.contains("DIAMOND") || name.contains("NETHERITE");
            case 1 -> name.contains("DIAMOND") || name.contains("NETHERITE");
            default -> false;
        };
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
                // 1. +20% Speed & Night Vision durante la Noche
                if (isNight) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60, 1, false, false));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 100, 0, false, false));
                }
            }

            case "vampire" -> {
                // 2. Fuerza y celeridad de noche o en el Nether
                if (isNight || isNether) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 60, 0, false, false));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 60, 0, false, false));
                }
            }

            case "dragonkin" -> {
                // 3. Resistencia al fuego innata; lentitud mitigada por nivel en agua fría/nieve
                player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 100, 0, false, false));
                boolean coldBiome = biomeName.contains("SNOW") || biomeName.contains("ICE") || biomeName.contains("FROZEN");
                if (coldBiome && player.isInWater()) {
                    // Mitigación progresiva: L1-3 Slowness III, L4-5 Slowness I, L6 Inmune
                    if (level < 6) {
                        int amp = (level >= 4) ? 0 : 2;
                        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, amp, false, false));
                    }
                }
            }

            case "beastkin" -> {
                // 4. +15% Speed & Night Vision en Forest, Taiga y Savanna
                boolean nature = biomeName.contains("FOREST") || biomeName.contains("TAIGA") || biomeName.contains("SAVANNA");
                if (nature) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60, 0, false, false));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 100, 0, false, false));
                }
            }

            case "undead" -> {
                // 5. No-Muerto: barra de hambre congelada (manejado en FoodLevelChangeEvent)
            }

            case "human" -> {
                // 6. EXP aumentado (manejado en PlayerExpChangeEvent)
            }

            case "elf" -> {
                // 7. +15% Speed & Jump Boost II en Forest, Jungle, Taiga
                boolean woods = biomeName.contains("FOREST") || biomeName.contains("JUNGLE") || biomeName.contains("TAIGA");
                if (woods) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60, 0, false, false));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 60, 1, false, false));
                }
            }

            case "fairy" -> {
                // 8. Caída lenta permanente; slowness en agua profunda
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 60, 0, false, false));
                if (player.isInWater() && level < 5) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 0, false, false));
                }
            }

            case "orc" -> {
                // 9. +2 Armor & Knockback Resistance en Mountain / Badlands
                boolean peaks = biomeName.contains("MOUNTAIN") || biomeName.contains("PEAKS") || biomeName.contains("BADLANDS") || biomeName.contains("WINDSWEPT");
                if (peaks) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 60, 0, false, false));
                }
            }

            case "druid" -> {
                // 10. Regeneración & Saturación en praderas floridas / bosques durante el día
                boolean green = biomeName.contains("MEADOW") || biomeName.contains("FLOWER") || biomeName.contains("FOREST") || biomeName.contains("CHERRY");
                if (green && isDay) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 60, 0, false, false));
                    if (player.getFoodLevel() < 20) {
                        player.setFoodLevel(Math.min(20, player.getFoodLevel() + 1));
                    }
                }
            }

            case "dwarf" -> {
                // 11. Haste II & Night Vision bajo Y=60 en cuevas
                if (loc.getY() <= 60) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 80, 1, false, false));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 100, 0, false, false));
                }
            }

            case "demon" -> {
                // 12. Inmunidad a fuego & Fuerza en Nether; debilidad en agua mitigada a L4+
                player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 100, 0, false, false));
                if (isNether) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 60, 0, false, false));
                }
                if (level < 4 && (player.isInWater() || (world.hasStorm() && loc.getBlock().getLightFromSky() == 15))) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 60, 0, false, false));
                }
            }

            case "yokai" -> {
                // 13. Caída lenta pasiva permanente
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 60, 0, false, false));
            }

            case "giant" -> {
                // 14. Slowness I pasiva (mitigada por completo a Nivel 6 - Titán Supremo)
                if (level < 6) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 0, false, false));
                }
            }

            case "angel" -> {
                // 15. Regeneración bajo la luz solar directa
                if (isDay && !world.hasStorm() && loc.getBlock().getLightFromSky() == 15 && world.getHighestBlockYAt(loc) <= loc.getBlockY()) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 60, 0, false, false));
                }
            }

            case "goblin" -> {
                // 16. +25% Speed si la mano secundaria está vacía
                if (player.getInventory().getItemInOffHand().getType() == Material.AIR) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60, 1, false, false));
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onExpChange(PlayerExpChangeEvent event) {
        Player player = event.getPlayer();
        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        // 6. Humano: +10% EXP global de todas las fuentes
        if (data != null && data.getRace().equalsIgnoreCase("human")) {
            int bonus = (int) Math.ceil(event.getAmount() * 0.10);
            event.setAmount(event.getAmount() + bonus);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onFoodChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        // 5. Undead: barra de hambre congelada
        if (data != null && data.getRace().equalsIgnoreCase("undead")) {
            event.setCancelled(true);
            player.setFoodLevel(20);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPotionEffect(EntityPotionEffectEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        if (data == null) return;

        // Inmunidad a veneno para No-Muerto y Demonio
        if (event.getNewEffect() != null && event.getNewEffect().getType() == PotionEffectType.POISON) {
            if (data.getRace().equalsIgnoreCase("undead") || data.getRace().equalsIgnoreCase("vampire")) {
                event.setCancelled(true);
            }
        }
    }
}

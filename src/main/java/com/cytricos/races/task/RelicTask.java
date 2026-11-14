package com.cytricos.races.task;

import com.cytricos.races.CytricosRaces;
import com.cytricos.races.model.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

/**
 * Tarea ligera cada 20 ticks (1s) que evalúa las reliquias equipadas en la mano secundaria (Offhand)
 * y aplica efectos pasivos continuos auténticos a su portador.
 * Lead Developers: Dandan_13 & Wolfit12
 */
public class RelicTask {
    private final CytricosRaces plugin;
    private BukkitTask task;

    public RelicTask(CytricosRaces plugin) {
        this.plugin = plugin;
    }

    public void start() {
        stop();
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tickOffhandRelics, 20L, 20L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void tickOffhandRelics() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.isOnline() || player.isDead()) continue;
            ItemStack offhand = player.getInventory().getItemInOffHand();
            String relicId = plugin.getCustomItemManager().getCustomItemId(offhand);
            if (relicId == null) continue;

            PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
            boolean particles = data == null || data.isSettingParticles();

            switch (relicId.toLowerCase()) {
                case "phoenix_ember_ring" -> {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 60, 0, true, false));
                    if (player.getFireTicks() > 0) {
                        player.setFireTicks(0);
                    }
                    if (particles) {
                        player.getWorld().spawnParticle(Particle.FLAME, player.getLocation().add(0, 1.0, 0), 3, 0.35, 0.35, 0.35, 0.01);
                    }
                }
                case "vampire_blood_chalice" -> {
                    if (data != null && data.getRacialResource() < data.getMaxRacialResource()) {
                        data.addRacialResource(1.5);
                    }
                    if (player.getFoodLevel() < 20 && Math.random() < 0.25) {
                        player.setFoodLevel(Math.min(20, player.getFoodLevel() + 1));
                    }
                }
                case "abyssal_demon_blade" -> {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 50, 0, true, false));
                }
                case "celestial_seraph_staff" -> {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 60, 0, true, false));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 50, 0, true, false));
                }
                case "dwarven_mithril_pickaxe" -> {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 60, 0, true, false));
                    if (player.getLocation().getY() <= 60) {
                        player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 260, 0, true, false));
                    }
                }
                case "fairy_dew_amulet" -> {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 60, 0, true, false));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 60, 0, true, false));
                }
                case "elf_sylvan_bow" -> {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60, 0, true, false));
                }
                case "void_reaper_scythe" -> {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 260, 0, true, false));
                    player.removePotionEffect(PotionEffectType.WITHER);
                }
                case "nature_warden_talisman" -> {
                    if (player.getWorld().getTime() < 12300 && player.getLocation().getBlock().getLightFromSky() >= 12) {
                        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 60, 0, true, false));
                    }
                    // Acelerar sutilmente cultivos en radio 3x3
                    for (int dx = -2; dx <= 2; dx++) {
                        for (int dz = -2; dz <= 2; dz++) {
                            var blk = player.getLocation().clone().add(dx, 0, dz).getBlock();
                            if (blk.getBlockData() instanceof Ageable age && age.getAge() < age.getMaximumAge() && Math.random() < 0.08) {
                                age.setAge(age.getAge() + 1);
                                blk.setBlockData(age);
                            }
                        }
                    }
                }
                case "shadow_cloak_brooch" -> {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 260, 0, true, false));
                    if (player.isSneaking()) {
                        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, 0, true, false));
                    }
                }
                case "dragon_heart_gem" -> {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 60, 0, true, false));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 60, 0, true, false));
                }
                case "cytricos_relic_coin" -> {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.LUCK, 60, 0, true, false));
                }
                default -> {
                }
            }
        }
    }
}

package com.cytricos.races.listener;

import com.cytricos.races.CytricosRaces;
import com.cytricos.races.model.PlayerData;
import com.cytricos.races.util.ColorUtils;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Listener de Objetos Personalizados (PDC) y Alas Cosméticas para CYTRICOS RPG.
 * Maneja habilidades activas/pasivas de reliquias custom, protección contra
 * modificación en yunques y sincronización limpia de entidades de alas.
 *
 * Lead Developers: Dandan_13 & Wolfit12
 */
public class CustomItemListener implements Listener {
    private final CytricosRaces plugin;
    private final Map<UUID, Map<String, Long>> itemCooldowns = new ConcurrentHashMap<>();

    public CustomItemListener(CytricosRaces plugin) {
        this.plugin = plugin;
    }

    private boolean checkItemCooldown(Player player, String itemId, int seconds) {
        long now = System.currentTimeMillis();
        Map<String, Long> map = itemCooldowns.computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>());
        Long expires = map.get(itemId);
        if (expires != null && now < expires) {
            long remaining = Math.max(1, (expires - now) / 1000L);
            ColorUtils.sendActionBar(player, "&c&l[RELIQUIA] &7Recarga restante: &e" + remaining + "s");
            return false;
        }
        map.put(itemId, now + (seconds * 1000L));
        return true;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack item = event.getItem();
        if (item == null || item.getType() == Material.AIR) return;

        String customId = plugin.getCustomItemManager().getCustomItemId(item);
        if (customId == null) return;

        Player player = event.getPlayer();
        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());

        switch (customId) {
            case "vampire_blood_chalice" -> {
                event.setCancelled(true);
                if (!checkItemCooldown(player, customId, 30)) return;
                double maxHp = player.getMaxHealth();
                player.setHealth(Math.min(maxHp, player.getHealth() + 6.0));
                player.setFoodLevel(Math.min(20, player.getFoodLevel() + 6));
                player.setSaturation(Math.min(20f, player.getSaturation() + 6.0f));
                if (data != null) {
                    data.addRacialResource(35.0);
                }
                player.getWorld().spawnParticle(Particle.CRIMSON_SPORE, player.getLocation().add(0, 1, 0), 20, 0.4, 0.5, 0.4, 0.05);
                if (data == null || data.isSettingSounds()) {
                    player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_DRINK, 1.0f, 0.8f);
                }
                ColorUtils.sendActionBar(player, "&4&l[CÁLIZ CARMESÍ] &7Vitalidad y +35 de Recurso restaurados.");
            }
            case "celestial_seraph_staff" -> {
                event.setCancelled(true);
                if (!checkItemCooldown(player, customId, 25)) return;
                for (Entity nearby : player.getNearbyEntities(5.0, 5.0, 5.0)) {
                    if (nearby instanceof Player ally) {
                        ally.setHealth(Math.min(ally.getMaxHealth(), ally.getHealth() + 4.0));
                        ally.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 0));
                        ally.getWorld().spawnParticle(Particle.GLOW, ally.getLocation().add(0, 1, 0), 12, 0.3, 0.5, 0.3, 0.02);
                    }
                }
                player.setHealth(Math.min(player.getMaxHealth(), player.getHealth() + 4.0));
                player.getWorld().spawnParticle(Particle.END_ROD, player.getLocation().add(0, 1.2, 0), 25, 1.2, 0.5, 1.2, 0.05);
                if (data == null || data.isSettingSounds()) {
                    player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.9f, 1.4f);
                }
                ColorUtils.sendActionBar(player, "&f&l[BÁCULO SERAFÍN] &7Pulso curativo emitido (Radio 5m).");
            }
            case "fairy_dew_amulet" -> {
                event.setCancelled(true);
                if (!checkItemCooldown(player, customId, 18)) return;
                Vector boost = player.getLocation().getDirection().multiply(0.55).setY(0.65);
                player.setVelocity(boost);
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 120, 0, false, false));
                player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 80, 0, false, false));
                player.getWorld().spawnParticle(Particle.CHERRY_LEAVES, player.getLocation().add(0, 0.5, 0), 20, 0.5, 0.3, 0.5, 0.05);
                if (data == null || data.isSettingSounds()) {
                    player.playSound(player.getLocation(), Sound.ENTITY_ALLAY_AMBIENT_WITHOUT_ITEM, 1.0f, 1.4f);
                }
                ColorUtils.sendActionBar(player, "&d&l[AMULETO FEÉRICO] &7Impulso aéreo y Caída Lenta activados.");
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        ItemStack hand = attacker.getInventory().getItemInMainHand();
        String customId = plugin.getCustomItemManager().getCustomItemId(hand);
        if ("abyssal_demon_blade".equals(customId)) {
            event.setDamage(event.getDamage() + 1.5);
            event.getEntity().setFireTicks(Math.max(event.getEntity().getFireTicks(), 80));
            event.getEntity().getWorld().spawnParticle(Particle.FLAME, event.getEntity().getLocation().add(0, 1, 0), 10, 0.3, 0.3, 0.3, 0.05);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityShootBow(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player shooter)) return;
        String customId = plugin.getCustomItemManager().getCustomItemId(event.getBow());
        if ("elf_sylvan_bow".equals(customId) && event.getProjectile() instanceof Arrow arrow) {
            arrow.setVelocity(arrow.getVelocity().multiply(1.15));
            shooter.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, shooter.getLocation().add(0, 1.3, 0), 8, 0.3, 0.3, 0.3, 0.02);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack hand = player.getInventory().getItemInMainHand();
        String customId = plugin.getCustomItemManager().getCustomItemId(hand);
        if ("dwarven_mithril_pickaxe".equals(customId)) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 60, 1, false, false));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        ItemStack first = event.getInventory().getItem(0);
        ItemStack second = event.getInventory().getItem(1);
        if (plugin.getCustomItemManager().isAnyCustomItem(first)
                || plugin.getCustomItemManager().isAnyCustomItem(second)
                || plugin.getWingsManager().isWingItem(first)
                || plugin.getWingsManager().isWingItem(second)) {
            // Prevenir alteración o falsificación de ítems PDC en yunques
            event.setResult(null);
        }
    }

    // Sincronización y limpieza segura de Alas Cosméticas (ItemDisplay)
    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            plugin.getWingsManager().refreshPlayerWings(event.getPlayer());
        }, 20L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        plugin.getWingsManager().removeWingDisplay(event.getPlayer());
        itemCooldowns.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        plugin.getWingsManager().removeWingDisplay(event.getEntity());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent event) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            plugin.getWingsManager().refreshPlayerWings(event.getPlayer());
        }, 10L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            plugin.getWingsManager().refreshPlayerWings(event.getPlayer());
        }, 10L);
    }
}

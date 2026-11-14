package com.cytricos.races.listener;

import com.cytricos.races.CytricosRaces;
import com.cytricos.races.manager.CustomItemManager;
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
import org.bukkit.event.inventory.PrepareGrindstoneEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Listener de Reliquias Personalizadas (PDC), Fragmentos de Talento, Cosméticos Craneales Flotantes,
 * limpieza anti-duplicación de Alas/Coronas y entrega automática de Resource Pack al entrar.
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

    private boolean canUseCustomItem(Player player, PlayerData data, String itemId) {
        CustomItemManager.CustomItemSpec spec = plugin.getCustomItemManager().getSpec(itemId);
        if (spec == null) return true;
        if (plugin.getCustomItemManager().meetsItemRequirements(player, data, spec)) {
            return true;
        }
        String raceReq = spec.requiredRace.isEmpty() ? "" : " (" + spec.requiredRace.toUpperCase() + ")";
        ColorUtils.sendActionBar(player, "&c&l[REQUISITO] &7Requiere &eNivel " + spec.requiredLevel + "+" + raceReq + " &7para usar esta reliquia.");
        return false;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack item = event.getItem();
        if (item == null || item.getType() == Material.AIR) return;

        String customId = plugin.getCustomItemManager().getCustomItemId(item);
        if (customId == null) return;

        Player player = event.getPlayer();
        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());

        switch (customId) {
            case "talent_shard" -> {
                event.setCancelled(true);
                if (data == null || !data.hasAssignedRace()) {
                    ColorUtils.sendMessage(player, "&cPrimero debes elegir una raza con &e/razas &cpara absorber este fragmento.");
                    return;
                }
                if (item.getAmount() > 1) {
                    item.setAmount(item.getAmount() - 1);
                } else {
                    player.getInventory().setItemInMainHand(null);
                }
                data.addTalentPoints(1);
                plugin.getPlayerDataManager().saveToFile(data);
                player.getWorld().spawnParticle(Particle.END_ROD, player.getLocation().add(0, 1.0, 0), 20, 0.35, 0.5, 0.35, 0.05);
                if (data.isSettingSounds()) {
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.9f, 1.6f);
                }
                ColorUtils.sendActionBar(player, "&d&l[FRAGMENTO DE TALENTO] &b+1 Punto de Talento Racial obtenido.");
                ColorUtils.sendMessage(player, "&d&l[TALENTOS] &7Has absorbido un Fragmento de Talento Racial (&b+1 Punto&7).");
            }
            case "celestial_crown", "astralis_halo" -> {
                event.setCancelled(true);
                if (plugin.getHeadCosmeticManager() != null) {
                    plugin.getHeadCosmeticManager().toggleHeadCosmetic(player, customId);
                }
            }
            case "vampire_blood_chalice" -> {
                event.setCancelled(true);
                if (!canUseCustomItem(player, data, customId) || !checkItemCooldown(player, customId, 30)) return;
                player.setHealth(Math.min(player.getMaxHealth(), player.getHealth() + 6.0));
                player.setFoodLevel(Math.min(20, player.getFoodLevel() + 6));
                player.setSaturation(Math.min(20f, player.getSaturation() + 6.0f));
                if (data != null) data.addRacialResource(35.0);
                player.getWorld().spawnParticle(Particle.CRIMSON_SPORE, player.getLocation().add(0, 1, 0), 20, 0.4, 0.5, 0.4, 0.05);
                if (data == null || data.isSettingSounds()) {
                    player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_DRINK, 1.0f, 0.8f);
                }
                ColorUtils.sendActionBar(player, "&4&l[CÁLIZ CARMESÍ] &7Vitalidad y +35 de Recurso restaurados.");
            }
            case "celestial_seraph_staff" -> {
                event.setCancelled(true);
                if (!canUseCustomItem(player, data, customId) || !checkItemCooldown(player, customId, 25)) return;
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
                if (!canUseCustomItem(player, data, customId) || !checkItemCooldown(player, customId, 18)) return;
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
            case "void_reaper_scythe" -> {
                event.setCancelled(true);
                if (!canUseCustomItem(player, data, customId) || !checkItemCooldown(player, customId, 16)) return;
                Vector dash = player.getLocation().getDirection().normalize().multiply(1.15);
                dash.setY(Math.max(0.2, Math.min(0.45, dash.getY() + 0.15)));
                player.setVelocity(dash);
                player.getWorld().spawnParticle(Particle.PORTAL, player.getLocation().add(0, 1, 0), 30, 0.4, 0.5, 0.4, 0.1);
                if (data == null || data.isSettingSounds()) {
                    player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.9f, 1.3f);
                }
                ColorUtils.sendActionBar(player, "&3&l[GUADAÑA DEL VACÍO] &7Paso sombrío ejecutado.");
            }
            case "nature_warden_talisman" -> {
                event.setCancelled(true);
                if (!canUseCustomItem(player, data, customId) || !checkItemCooldown(player, customId, 24)) return;
                player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 160, 0, false, false));
                player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 120, 1, false, false));
                player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation().add(0, 1, 0), 20, 0.6, 0.5, 0.6, 0.05);
                if (data == null || data.isSettingSounds()) {
                    player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.1f);
                }
                ColorUtils.sendActionBar(player, "&a&l[GUARDIÁN DEL BOSQUE] &7Bendición natural activa.");
            }
            case "phoenix_ember_ring" -> {
                event.setCancelled(true);
                if (!canUseCustomItem(player, data, customId) || !checkItemCooldown(player, customId, 30)) return;
                player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 200, 0, false, false));
                player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 200, 0, false, false));
                player.getWorld().spawnParticle(Particle.FLAME, player.getLocation().add(0, 1, 0), 28, 0.6, 0.4, 0.6, 0.05);
                if (data == null || data.isSettingSounds()) {
                    player.playSound(player.getLocation(), Sound.ITEM_FIRECHARGE_USE, 0.9f, 1.1f);
                }
                ColorUtils.sendActionBar(player, "&6&l[BRASAS DE FÉNIX] &7Inmunidad ígnea y vigor solar activados.");
            }
            case "shadow_cloak_brooch" -> {
                event.setCancelled(true);
                if (!canUseCustomItem(player, data, customId) || !checkItemCooldown(player, customId, 28)) return;
                player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 100, 0, false, false));
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 100, 1, false, false));
                player.getWorld().spawnParticle(Particle.SMOKE, player.getLocation().add(0, 1, 0), 24, 0.4, 0.5, 0.4, 0.04);
                if (data == null || data.isSettingSounds()) {
                    player.playSound(player.getLocation(), Sound.ENTITY_PHANTOM_FLAP, 0.8f, 1.4f);
                }
                ColorUtils.sendActionBar(player, "&8&l[MANTO SOMBRÍO] &7Velo espectral activado por 5s.");
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        PlayerData data = plugin.getPlayerDataManager().get(attacker.getUniqueId());

        ItemStack mainHand = attacker.getInventory().getItemInMainHand();
        String mainId = plugin.getCustomItemManager().getCustomItemId(mainHand);
        if ("abyssal_demon_blade".equals(mainId) && canUseCustomItem(attacker, data, mainId)) {
            event.setDamage(event.getDamage() + 1.5);
            event.getEntity().setFireTicks(Math.max(event.getEntity().getFireTicks(), 80));
            event.getEntity().getWorld().spawnParticle(Particle.FLAME, event.getEntity().getLocation().add(0, 1, 0), 10, 0.3, 0.3, 0.3, 0.05);
        } else if ("void_reaper_scythe".equals(mainId) && canUseCustomItem(attacker, data, mainId)) {
            event.setDamage(event.getDamage() + 2.0);
            if (event.getEntity() instanceof LivingEntity target) {
                target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 60, 0));
            }
            attacker.setHealth(Math.min(attacker.getMaxHealth(), attacker.getHealth() + 1.0));
            event.getEntity().getWorld().spawnParticle(Particle.PORTAL, event.getEntity().getLocation().add(0, 1, 0), 10, 0.3, 0.4, 0.3, 0.08);
        }

        // Pasivas de Combate desde Mano Secundaria (Offhand Relics)
        ItemStack offHand = attacker.getInventory().getItemInOffHand();
        String offId = plugin.getCustomItemManager().getCustomItemId(offHand);
        if (offId != null) {
            if ("phoenix_ember_ring".equals(offId)) {
                event.setDamage(event.getDamage() + 1.5);
                event.getEntity().setFireTicks(Math.max(event.getEntity().getFireTicks(), 80));
            } else if ("vampire_blood_chalice".equals(offId)) {
                double heal = Math.max(0.5, event.getFinalDamage() * 0.15);
                attacker.setHealth(Math.min(attacker.getMaxHealth(), attacker.getHealth() + heal));
            } else if ("abyssal_demon_blade".equals(offId) && event.getEntity() instanceof LivingEntity target) {
                var eq = target.getEquipment();
                boolean unarmored = eq == null || (eq.getChestplate() == null || eq.getChestplate().getType() == Material.AIR);
                if (unarmored) {
                    event.setDamage(event.getDamage() * 1.10);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityShootBow(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player shooter)) return;
        String customId = plugin.getCustomItemManager().getCustomItemId(event.getBow());
        if ("elf_sylvan_bow".equals(customId) && event.getProjectile() instanceof Arrow arrow) {
            PlayerData data = plugin.getPlayerDataManager().get(shooter.getUniqueId());
            if (canUseCustomItem(shooter, data, customId)) {
                arrow.setVelocity(arrow.getVelocity().multiply(1.15));
                shooter.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, shooter.getLocation().add(0, 1.3, 0), 8, 0.3, 0.3, 0.3, 0.02);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack hand = player.getInventory().getItemInMainHand();
        String customId = plugin.getCustomItemManager().getCustomItemId(hand);
        if ("dwarven_mithril_pickaxe".equals(customId)) {
            PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
            if (plugin.getCustomItemManager().meetsItemRequirements(player, data, plugin.getCustomItemManager().getSpec(customId))) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 60, 1, false, false));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        ItemStack first = event.getInventory().getItem(0);
        ItemStack second = event.getInventory().getItem(1);
        if (isProtectedSpecialItem(first) || isProtectedSpecialItem(second)) {
            event.setResult(null);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        for (ItemStack matrixItem : event.getInventory().getMatrix()) {
            if (isProtectedSpecialItem(matrixItem)) {
                event.getInventory().setResult(null);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepareGrindstone(PrepareGrindstoneEvent event) {
        ItemStack upper = event.getInventory().getItem(0);
        ItemStack lower = event.getInventory().getItem(1);
        if (isProtectedSpecialItem(upper) || isProtectedSpecialItem(lower)) {
            event.setResult(null);
        }
    }

    private boolean isProtectedSpecialItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        return plugin.getCustomItemManager().isAnyCustomItem(item)
                || plugin.getWingsManager().isWingItem(item);
    }

    // Ciclo de vida de Cosméticos 3D, Resource Pack y Bienvenida "Sin Raza"
    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());

        if (data != null && !data.hasAssignedRace()) {
            ColorUtils.sendMessage(player, "&8==================================================");
            ColorUtils.sendMessage(player, "&6&l¡BIENVENIDO A CYTRICOS RPG!");
            ColorUtils.sendMessage(player, "&7Tu estado actual es &fSin Raza &7(sin ventajas ni poderes activos).");
            ColorUtils.sendMessage(player, "&7Usa &a/razas &7para abrir el selector gratuito y escoger tu linaje.");
            ColorUtils.sendMessage(player, "&8==================================================");
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;
            if (data != null) {
                plugin.getRaceManager().applyPassives(player, data.getRace(), data.getLevel());
            }
            if (plugin.getWingsManager() != null) {
                plugin.getWingsManager().refreshPlayerWings(player);
            }
            if (plugin.getHeadCosmeticManager() != null) {
                plugin.getHeadCosmeticManager().refreshPlayerHeadCosmetic(player);
            }
            if (plugin.getResourcePackManager() != null && plugin.getResourcePackManager().isAutoSendEnabled()) {
                plugin.getResourcePackManager().sendPack(player, true);
            }
        }, 15L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        Player p = event.getPlayer();
        if (plugin.getWingsManager() != null) plugin.getWingsManager().cleanupPlayerWings(p);
        if (plugin.getHeadCosmeticManager() != null) plugin.getHeadCosmeticManager().cleanupPlayerHeadCosmetic(p);
        itemCooldowns.remove(p.getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        Player p = event.getEntity();
        if (plugin.getWingsManager() != null) plugin.getWingsManager().cleanupPlayerWings(p);
        if (plugin.getHeadCosmeticManager() != null) plugin.getHeadCosmeticManager().cleanupPlayerHeadCosmetic(p);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent event) {
        Player p = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!p.isOnline()) return;
            if (plugin.getWingsManager() != null) plugin.getWingsManager().refreshPlayerWings(p);
            if (plugin.getHeadCosmeticManager() != null) plugin.getHeadCosmeticManager().refreshPlayerHeadCosmetic(p);
        }, 10L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player p = event.getPlayer();
        if (plugin.getWingsManager() != null) plugin.getWingsManager().cleanupPlayerWings(p);
        if (plugin.getHeadCosmeticManager() != null) plugin.getHeadCosmeticManager().cleanupPlayerHeadCosmetic(p);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!p.isOnline()) return;
            if (plugin.getWingsManager() != null) plugin.getWingsManager().refreshPlayerWings(p);
            if (plugin.getHeadCosmeticManager() != null) plugin.getHeadCosmeticManager().refreshPlayerHeadCosmetic(p);
        }, 10L);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        Player p = event.getPlayer();
        if (event.getTo() == null) return;
        boolean farOrCrossWorld = !event.getFrom().getWorld().equals(event.getTo().getWorld())
                || event.getFrom().distanceSquared(event.getTo()) > 256.0;
        if (!farOrCrossWorld) return;

        if (plugin.getWingsManager() != null) plugin.getWingsManager().cleanupPlayerWings(p);
        if (plugin.getHeadCosmeticManager() != null) plugin.getHeadCosmeticManager().cleanupPlayerHeadCosmetic(p);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!p.isOnline()) return;
            if (plugin.getWingsManager() != null) plugin.getWingsManager().refreshPlayerWings(p);
            if (plugin.getHeadCosmeticManager() != null) plugin.getHeadCosmeticManager().refreshPlayerHeadCosmetic(p);
        }, 8L);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        Player player = event.getPlayer();
        if (event.getNewGameMode() == GameMode.SPECTATOR) {
            if (plugin.getWingsManager() != null) plugin.getWingsManager().cleanupPlayerWings(player);
            if (plugin.getHeadCosmeticManager() != null) plugin.getHeadCosmeticManager().cleanupPlayerHeadCosmetic(player);
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!player.isOnline()) return;
                if (plugin.getWingsManager() != null) plugin.getWingsManager().refreshPlayerWings(player);
                if (plugin.getHeadCosmeticManager() != null) plugin.getHeadCosmeticManager().refreshPlayerHeadCosmetic(player);
            }, 5L);
        }
    }
}

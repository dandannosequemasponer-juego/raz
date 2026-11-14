package com.cytricos.races.listener;

import com.cytricos.races.CytricosRaces;
import com.cytricos.races.manager.QuestManager;
import com.cytricos.races.model.PlayerData;
import com.cytricos.races.model.Race;
import com.cytricos.races.util.ColorUtils;
import io.papermc.paper.event.player.PlayerTradeEvent;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Listener central de habilidades activas (Shift + F / /raza habilidad),
 * canalización de metamorfosis (Shift + Clic Derecho 6s), combate PvP/PvE,
 * bloqueos de equipo, economía en yunque/aldeanos y progresión de talentos para las 16 razas.
 *
 * Lead Developers: Dandan_13 & Wolfit12
 */
public class RaceAbilitiesListener implements Listener {
    private final CytricosRaces plugin;

    private final Map<UUID, Map<String, Long>> cooldowns = new ConcurrentHashMap<>();
    private final Set<UUID> beastFormActive = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Set<UUID> stoneSkinActive = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Set<UUID> unyieldingFuryActive = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Map<UUID, Long> fairyStaticSneakStart = new ConcurrentHashMap<>();
    private final Map<UUID, Location> fairyStaticSneakLoc = new ConcurrentHashMap<>();
    private final Set<UUID> combatHungerActive = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public RaceAbilitiesListener(CytricosRaces plugin) {
        this.plugin = plugin;
        startResourceRegenTask();
        startFairyStealthAndOffhandTask();
    }

    private void startResourceRegenTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
                if (data == null || !data.hasAssignedRace()) continue;
                if (data.getRacialResource() < data.getMaxRacialResource()) {
                    double amount = data.hasTalent("surv_regen") ? 3.45 : 3.0;
                    data.addRacialResource(amount);
                }
            }
        }, 80L, 80L);
    }

    private void startFairyStealthAndOffhandTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long now = System.currentTimeMillis();
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (!player.isOnline() || player.isDead()) continue;
                PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
                if (data == null || !data.hasAssignedRace()) {
                    fairyStaticSneakStart.remove(player.getUniqueId());
                    fairyStaticSneakLoc.remove(player.getUniqueId());
                    continue;
                }

                String race = data.getRace().toLowerCase();

                // Hada: Invisibilidad al permanecer agachada quieto durante 3.0 segundos
                if (race.equals("fairy") && player.isSneaking()) {
                    Location cur = player.getLocation();
                    Location prev = fairyStaticSneakLoc.get(player.getUniqueId());
                    if (prev == null || !prev.getWorld().equals(cur.getWorld()) || prev.distanceSquared(cur) > 0.08) {
                        fairyStaticSneakLoc.put(player.getUniqueId(), cur);
                        fairyStaticSneakStart.put(player.getUniqueId(), now);
                    } else {
                        long start = fairyStaticSneakStart.getOrDefault(player.getUniqueId(), now);
                        if (now - start >= 3000L) {
                            player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 30, 0, true, false));
                        }
                    }
                } else if (race.equals("yokai") && data.getLevel() >= 2 && player.isSneaking()
                        && player.getLocation().getBlock().getLightLevel() <= 7) {
                    // Yokai L2+: Sigilo nocturno/sombra al agacharse
                    player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 30, 0, true, false));
                } else {
                    fairyStaticSneakStart.remove(player.getUniqueId());
                    fairyStaticSneakLoc.remove(player.getUniqueId());
                }

                // Humano: Buffs tácticos según el ítem en Mano Secundaria (Offhand)
                if (race.equals("human")) {
                    Material off = player.getInventory().getItemInOffHand().getType();
                    if (off == Material.SHIELD) {
                        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 35, 0, true, false));
                    } else if (off == Material.TORCH || off == Material.SOUL_TORCH || off == Material.LANTERN) {
                        player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 240, 0, true, false));
                    } else if (off == Material.TOTEM_OF_UNDYING || off == Material.ENCHANTED_BOOK) {
                        player.addPotionEffect(new PotionEffect(PotionEffectType.LUCK, 35, 0, true, false));
                    }
                }

                // Beastkin: Penalización por armadura completa de Netherite
                if (race.equals("beastkin") && isWearingFullNetherite(player)) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 35, 1, true, false));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 35, 0, true, false));
                }
            }
        }, 20L, 10L);
    }

    private boolean isWearingFullNetherite(Player player) {
        var inv = player.getInventory();
        return inv.getHelmet() != null && inv.getHelmet().getType() == Material.NETHERITE_HELMET
                && inv.getChestplate() != null && inv.getChestplate().getType() == Material.NETHERITE_CHESTPLATE
                && inv.getLeggings() != null && inv.getLeggings().getType() == Material.NETHERITE_LEGGINGS
                && inv.getBoots() != null && inv.getBoots().getType() == Material.NETHERITE_BOOTS;
    }

    public void toggleTransformation(Player player) {
        if (plugin.getTransformationManager() != null) {
            plugin.getTransformationManager().toggleTransformation(player);
        }
    }

    public void revertTransformation(Player player) {
        if (plugin.getTransformationManager() != null) {
            plugin.getTransformationManager().revertTransformation(player, true);
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
                ColorUtils.sendMessage(player, "&c&l[COOLDOWN] &7Espera &e" + remaining + "s &7para reutilizar &6" + ability + "&7.");
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
            plugin.getQuestManager().recordAction(player, QuestManager.QuestType.USE_ABILITY, 1);
        }
        return true;
    }

    // =========================================================================
    // 1. ENTRADA DE CANALIZACIÓN (Shift+Clic Derecho) Y HABILIDAD (Shift+F)
    // =========================================================================

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        if (!event.isSneaking()) {
            if (plugin.getTransformationManager() != null && plugin.getTransformationManager().isChanneling(player.getUniqueId())) {
                plugin.getTransformationManager().cancelChanneling(player.getUniqueId(),
                        "&c&l[METAMORFOSIS] &7Canalización cancelada por soltar Agacharse (Shift).");
            }
            fairyStaticSneakStart.remove(player.getUniqueId());
            fairyStaticSneakLoc.remove(player.getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        ItemStack held = event.getItem();
        if (held != null && plugin.getCustomItemManager() != null && plugin.getCustomItemManager().isAnyCustomItem(held)) {
            return;
        }
        Action action = event.getAction();
        boolean isRightClick = action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK;
        if (!isRightClick) return;

        Player player = event.getPlayer();
        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        if (data == null || !data.hasAssignedRace()) return;

        // Fase 7: Shift + Clic Derecho una sola vez inicia la canalización de 6.0s manteniendo Shift
        if (player.isSneaking() && plugin.getTransformationManager() != null) {
            plugin.getTransformationManager().startChanneling(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onSwapHandItems(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        if (!player.isSneaking()) return;
        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        if (data == null || !data.hasAssignedRace()) return;

        event.setCancelled(true);
        triggerActiveAbility(player);
    }

    // =========================================================================
    // 2. CICLO DE VIDA DEL JUGADOR
    // =========================================================================

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline() && data != null) {
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
        unyieldingFuryActive.remove(uuid);
        fairyStaticSneakStart.remove(uuid);
        fairyStaticSneakLoc.remove(uuid);
        combatHungerActive.remove(uuid);
        cooldowns.remove(uuid);
        if (plugin.getTransformationManager() != null) {
            plugin.getTransformationManager().cleanupPlayer(player);
        }
        plugin.getPlayerDataManager().unload(uuid);
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        beastFormActive.remove(uuid);
        stoneSkinActive.remove(uuid);
        unyieldingFuryActive.remove(uuid);
        if (plugin.getTransformationManager() != null) {
            plugin.getTransformationManager().cleanupPlayer(player);
        }
        PlayerData data = plugin.getPlayerDataManager().get(uuid);
        if (data != null) {
            plugin.getRaceManager().applyPassives(player, data.getRace(), data.getLevel());
        }
    }

    // =========================================================================
    // 3. MOVILIDAD, BLOQUEOS DE ARMADURA/ELYTRA/MONTURAS Y TÚNELES GIGANTE
    // =========================================================================

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onToggleFlight(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) return;

        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        if (data == null || !data.hasAssignedRace()) {
            event.setCancelled(true);
            player.setFlying(false);
            player.setAllowFlight(false);
            return;
        }

        if (data.isFlightBanned()) {
            event.setCancelled(true);
            player.setFlying(false);
            player.setAllowFlight(false);
            long rem = data.getFlightBanRemainingSeconds();
            ColorUtils.sendActionBar(player, "&c&l[!] &cVuelo bloqueado por exceso de altitud. Restante: &e"
                    + String.format("%02d:%02d", rem / 60, rem % 60));
            return;
        }

        String raceId = data.getRace().toLowerCase();
        int level = data.getLevel();

        if (raceId.equals("angel")) {
            ItemStack chest = player.getInventory().getChestplate();
            if (chest != null && (chest.getType() == Material.IRON_CHESTPLATE
                    || chest.getType() == Material.DIAMOND_CHESTPLATE
                    || chest.getType() == Material.NETHERITE_CHESTPLATE)) {
                event.setCancelled(true);
                player.setFlying(false);
                player.setAllowFlight(false);
                ColorUtils.sendActionBar(player, "&c&l[ÁNGEL] &7El peso de tu pechera metálica impide el vuelo.");
                return;
            }
        }

        if (raceId.equals("fairy") || (raceId.equals("angel") && level >= 3)
                || (data.isTransformed() && plugin.getTransformationManager() != null
                && plugin.getTransformationManager().isAerialForm(raceId, level))) {
            return;
        }

        if (raceId.equals("werewolf") && level >= 2) {
            event.setCancelled(true);
            player.setFlying(false);
            player.setAllowFlight(false);
            player.setVelocity(player.getLocation().getDirection().multiply(1.3).setY(0.7));
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WOLF_ANGRY_GROWL, 0.6f, 1.6f);
            player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 10, 0.2, 0.1, 0.2, 0.05);
            return;
        }

        if (raceId.equals("beastkin") && level >= 4) {
            event.setCancelled(true);
            player.setFlying(false);
            player.setAllowFlight(false);
            player.setVelocity(player.getLocation().getDirection().multiply(1.4).setY(0.75));
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BAT_TAKEOFF, 0.8f, 1.4f);
            player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, player.getLocation(), 5, 0.2, 0.1, 0.2, 0.02);
            return;
        }

        event.setCancelled(true);
        player.setFlying(false);
        player.setAllowFlight(false);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityToggleGlide(EntityToggleGlideEvent event) {
        if (!(event.getEntity() instanceof Player player) || !event.isGliding()) return;
        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        if (data == null || !data.hasAssignedRace()) return;

        String race = data.getRace().toLowerCase();
        if (race.equals("dwarf") || race.equals("orc")) {
            event.setCancelled(true);
            ColorUtils.sendActionBar(player, "&cTu contextura física no te permite planear con Élitros.");
        } else if (race.equals("dragonkin") && data.getLevel() >= 4
                && player.getInventory().getHelmet() != null && player.getInventory().getHelmet().getType() != Material.AIR) {
            event.setCancelled(true);
            ColorUtils.sendActionBar(player, "&c¡Quítate el casco para poder planear como Draconiano!");
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getTo() == null) return;
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        Player player = event.getPlayer();
        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        if (data == null || !data.hasAssignedRace()) return;

        String raceId = data.getRace().toLowerCase();
        int level = data.getLevel();
        Location loc = event.getTo();
        Block block = loc.getBlock();

        if (player.isOnGround()) {
            if ((raceId.equals("werewolf") && level >= 2) || (raceId.equals("beastkin") && level >= 4)) {
                player.setAllowFlight(true);
            }
        }

        // No-Muerto: nado lento
        if (raceId.equals("undead") && player.isInWater() && level < 6) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 35, 1, true, false));
        }

        // Beastkin L2+: Escalada de paredes
        if (raceId.equals("beastkin") && level >= 2 && player.isSneaking()) {
            Block facingBlock = loc.clone().add(loc.getDirection().setY(0).normalize()).getBlock();
            if (facingBlock.getType().isSolid()) {
                player.setVelocity(new Vector(0, 0.25, 0));
                player.setFallDistance(0);
            }
        }

        // Elfo L1+: Velocidad sobre hojas
        if (raceId.equals("elf")) {
            Block below = loc.clone().subtract(0, 0.5, 0).getBlock();
            if (below.getType().name().contains("LEAVES")) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, 1, false, false));
            }
        }

        // Gigante: bloqueo de túneles de 1x2 y destrucción de bloques blandos al correr en L4+
        if (raceId.equals("giant")) {
            Block fromTwoUp = event.getFrom().clone().add(0, 2, 0).getBlock();
            Block toTwoUp = loc.clone().add(0, 2, 0).getBlock();
            Block toOneUp = loc.clone().add(0, 1, 0).getBlock();
            if (!fromTwoUp.getType().isSolid() && toTwoUp.getType().isSolid() && toOneUp.isPassable()) {
                event.setCancelled(true);
                ColorUtils.sendActionBar(player, "&6&l[GIGANTE] &7Tu estatura colosal no cabe en túneles de 2 bloques de alto.");
                return;
            }
            if (level >= 4 && player.isSprinting()) {
                Block headBlock = loc.clone().add(loc.getDirection().setY(0).normalize()).add(0, 1, 0).getBlock();
                if (plugin.getWorldGuardHook() == null || plugin.getWorldGuardHook().canBreak(player, headBlock)) {
                    if (headBlock.getType() == Material.DIRT || headBlock.getType() == Material.GRASS_BLOCK
                            || headBlock.getType().name().contains("LEAVES") || headBlock.getType().name().contains("GLASS")) {
                        headBlock.breakNaturally();
                        player.getWorld().spawnParticle(Particle.BLOCK, headBlock.getLocation().add(0.5, 0.5, 0.5), 12, 0.3, 0.3, 0.3, headBlock.getBlockData());
                    }
                }
            }
        }

        // Hada en agua
        if (raceId.equals("fairy") && player.isInWater() && level < 5) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 50, 2, false, false));
        }

        // Druida en desierto o piedra
        if (raceId.equals("druid") && level < 6) {
            String biome = block.getBiome().name();
            if ((biome.contains("DESERT") || biome.contains("BADLANDS")) && block.getLightFromSky() < 12) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 50, 0, false, false));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMountEntity(EntityMountEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        if (data != null && data.hasAssignedRace() && data.getRace().equalsIgnoreCase("giant")) {
            event.setCancelled(true);
            ColorUtils.sendActionBar(player, "&c&l[GIGANTE] &7Tu peso colosal impide montar criaturas o botes.");
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onVehicleEnter(VehicleEnterEvent event) {
        if (!(event.getEntered() instanceof Player player)) return;
        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        if (data != null && data.hasAssignedRace() && data.getRace().equalsIgnoreCase("giant")) {
            event.setCancelled(true);
            ColorUtils.sendActionBar(player, "&c&l[GIGANTE] &7No puedes subir a vehículos ni monturas por tu tamaño.");
        }
    }

    // Restricciones de armadura y escudo para Goblin, Fairy, Dwarf y Orc
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryClickArmorAndTrade(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        if (data == null || !data.hasAssignedRace()) return;
        String race = data.getRace().toLowerCase();

        if (event.getSlotType() == InventoryType.SlotType.ARMOR || event.isShiftClick()) {
            ItemStack check = event.isShiftClick() ? event.getCurrentItem() : event.getCursor();
            if (check != null && check.getType() != Material.AIR) {
                Material m = check.getType();
                if (race.equals("goblin") && m.name().startsWith("NETHERITE_") && isArmorPiece(m)) {
                    event.setCancelled(true);
                    ColorUtils.sendActionBar(player, "&c&l[DUENDE] &7La armadura de Netherite es demasiado pesada para ti.");
                    return;
                }
                if (race.equals("fairy") && (m.name().startsWith("IRON_") || m.name().startsWith("DIAMOND_") || m.name().startsWith("NETHERITE_")) && isArmorPiece(m)) {
                    event.setCancelled(true);
                    ColorUtils.sendActionBar(player, "&c&l[HADA] &7Solo puedes vestir armaduras ligeras (Cuero, Oro o Cota).");
                    return;
                }
                if ((race.equals("dwarf") || race.equals("orc")) && m == Material.ELYTRA) {
                    event.setCancelled(true);
                    ColorUtils.sendActionBar(player, "&cTu anatomía impide equipar Élitros.");
                    return;
                }
            }
        }

        // Bloquear escudo en offhand para Hada
        if (race.equals("fairy") && event.getRawSlot() == 45) {
            ItemStack cursor = event.getCursor();
            if (cursor != null && cursor.getType() == Material.SHIELD) {
                event.setCancelled(true);
                ColorUtils.sendActionBar(player, "&c&l[HADA] &7Las hadas no pueden cargar escudos pesados.");
            }
        }
    }

    private boolean isArmorPiece(Material m) {
        String n = m.name();
        return n.endsWith("_HELMET") || n.endsWith("_CHESTPLATE") || n.endsWith("_LEGGINGS") || n.endsWith("_BOOTS");
    }

    // =========================================================================
    // 4. ECONOMÍA, YUNQUE, ENCANTAMIENTO Y ALDEANOS (HUMAN, DWARF, GOBLIN)
    // =========================================================================

    @EventHandler(priority = EventPriority.HIGH)
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        if (!(event.getView().getPlayer() instanceof Player player)) return;
        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        if (data == null || !data.hasAssignedRace()) return;

        String race = data.getRace().toLowerCase();
        if (race.equals("dwarf") || race.equals("goblin")) {
            double factor = race.equals("goblin") ? 0.60 : 0.70; // 40% goblin, 30% dwarf
            int currentCost = event.getView().getRepairCost();
            if (currentCost > 1) {
                event.getView().setRepairCost(Math.max(1, (int) Math.floor(currentCost * factor)));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEnchantItem(EnchantItemEvent event) {
        Player player = event.getEnchanter();
        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        if (data != null && data.hasAssignedRace() && data.getRace().equalsIgnoreCase("goblin")) {
            int newCost = Math.max(1, (int) Math.floor(event.getExpLevelCost() * 0.60));
            event.setExpLevelCost(newCost);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onVillagerTrade(PlayerTradeEvent event) {
        Player player = event.getPlayer();
        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        if (data == null || !data.hasAssignedRace() || !data.getRace().equalsIgnoreCase("human")) return;

        MerchantRecipe recipe = event.getTrade();
        for (ItemStack ing : recipe.getIngredients()) {
            if (ing != null && ing.getType() == Material.EMERALD && ing.getAmount() > 1) {
                int refund = Math.max(1, (int) Math.floor(ing.getAmount() * 0.15));
                plugin.getCustomItemManager().giveItemSafely(player, new ItemStack(Material.EMERALD, refund));
                ColorUtils.sendActionBar(player, "&b&l[COMERCIANTE HUMANO] &7Reembolso diplomático: &a+" + refund + " Esmeralda(s)");
                break;
            }
        }
    }

    // =========================================================================
    // 5. DAÑO AMBIENTAL, REGENERACIÓN, REVIVE ALIADO Y VULNERABILIDADES
    // =========================================================================

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        if (data == null || !data.hasAssignedRace()) return;
        Race race = plugin.getRaceManager().getRace(data.getRace());
        if (race == null) return;

        String raceId = race.getId().toLowerCase();
        int level = data.getLevel();
        EntityDamageEvent.DamageCause cause = event.getCause();

        if (unyieldingFuryActive.contains(player.getUniqueId())) {
            event.setCancelled(true);
            return;
        }

        if (stoneSkinActive.contains(player.getUniqueId()) && cause == EntityDamageEvent.DamageCause.PROJECTILE) {
            event.setCancelled(true);
            player.getWorld().playSound(player.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1.0f, 0.8f);
            return;
        }

        if ((cause == EntityDamageEvent.DamageCause.FIRE || cause == EntityDamageEvent.DamageCause.FIRE_TICK
                || cause == EntityDamageEvent.DamageCause.LAVA)) {
            if (race.hasImmunity("fire") || (raceId.equals("werewolf") && level >= 4)) {
                event.setCancelled(true);
                player.setFireTicks(0);
                return;
            }
            if (raceId.equals("elf")) {
                double mult = (level >= 6) ? 1.0 : (level >= 3 ? 1.05 : 1.15);
                event.setDamage(event.getDamage() * mult);
            } else if (raceId.equals("druid")) {
                double mult = (level >= 6) ? 1.0 : (level >= 3 ? 1.10 : 1.25);
                event.setDamage(event.getDamage() * mult);
            }
        }

        if (cause == EntityDamageEvent.DamageCause.FALL) {
            if (race.hasImmunity("fall_damage") || (raceId.equals("fairy") && level >= 2) || (raceId.equals("angel") && level >= 2)) {
                event.setCancelled(true);
                return;
            }
            if (raceId.equals("dwarf") && level < 5) {
                double mult = (level >= 3) ? 1.10 : 1.20;
                event.setDamage(event.getDamage() * mult);
            }
        }

        if (cause == EntityDamageEvent.DamageCause.DROWNING) {
            if (race.hasImmunity("drowning") || raceId.equals("vampire") || (raceId.equals("yokai") && level >= 4)) {
                event.setCancelled(true);
                player.setRemainingAir(player.getMaximumAir());
                return;
            }
        }

        if (cause == EntityDamageEvent.DamageCause.POISON) {
            if (race.hasImmunity("poison") || (raceId.equals("druid") && level >= 4) || raceId.equals("undead") || raceId.equals("vampire")) {
                event.setCancelled(true);
                return;
            }
        }

        if (cause == EntityDamageEvent.DamageCause.SUFFOCATION && (race.hasImmunity("suffocation") || (raceId.equals("yokai") && level >= 4))) {
            event.setCancelled(true);
            return;
        }

        if (raceId.equals("angel") && level < 5 && player.getLocation().getBlock().getLightLevel() <= 3) {
            double mult = (level >= 3) ? 1.10 : 1.20;
            event.setDamage(event.getDamage() * mult);
        }

        if (raceId.equals("yokai") && level < 5 && cause == EntityDamageEvent.DamageCause.PROJECTILE) {
            double mult = (level >= 3) ? 1.07 : 1.15;
            event.setDamage(event.getDamage() * mult);
        }

        if (raceId.equals("giant") && level < 5 && cause == EntityDamageEvent.DamageCause.PROJECTILE) {
            double mult = (level >= 3) ? 1.15 : 1.30;
            event.setDamage(event.getDamage() * mult);
        }

        // Hada: Resurrección de aliado cercano en peligro mortal (radio 6m, 1h cooldown)
        if (player.getHealth() - event.getFinalDamage() <= 0) {
            for (Entity nearby : player.getNearbyEntities(6.0, 4.0, 6.0)) {
                if (nearby instanceof Player fairy && fairy != player) {
                    PlayerData fData = plugin.getPlayerDataManager().get(fairy.getUniqueId());
                    if (fData != null && fData.hasAssignedRace() && fData.getRace().equalsIgnoreCase("fairy")
                            && fData.getLevel() >= 3 && checkCooldown(fairy, "BendicionResurreccion", 3600)) {
                        event.setCancelled(true);
                        player.setHealth(Math.min(player.getMaxHealth(), 8.0));
                        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 1));
                        player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 120, 1));
                        player.getWorld().playSound(player.getLocation(), Sound.ITEM_TOTEM_USE, 0.9f, 1.4f);
                        player.getWorld().spawnParticle(Particle.END_ROD, player.getLocation().add(0, 1, 0), 30, 0.5, 0.8, 0.5, 0.1);
                        ColorUtils.sendMessage(player, "&d&l[MILAGRO FEÉRICO] &7¡El hada &f" + fairy.getName() + " &7te ha salvado de la muerte!");
                        ColorUtils.sendMessage(fairy, "&d&l[RESURRECCIÓN] &7Has salvado a &f" + player.getName() + " &7con tu gracia feérica.");
                        return;
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityRegainHealth(EntityRegainHealthEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        if (data == null || !data.hasAssignedRace()) return;
        String race = data.getRace().toLowerCase();

        // Hada: x2 curación recibida
        if (race.equals("fairy")) {
            event.setAmount(event.getAmount() * 2.0);
        }
        // Hombre Lobo: pociones curativas reducidas al 50%
        if (race.equals("werewolf") && event.getRegainReason() == EntityRegainHealthEvent.RegainReason.MAGIC) {
            event.setAmount(event.getAmount() * 0.50);
        }
    }

    // =========================================================================
    // 6. COMBATE PVP / PVE Y PASIVAS OFENSIVAS
    // =========================================================================

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCombatDamage(EntityDamageByEntityEvent event) {
        Player attacker = null;
        Arrow arrow = null;
        if (event.getDamager() instanceof Player p) {
            attacker = p;
        } else if (event.getDamager() instanceof Arrow a && a.getShooter() instanceof Player p) {
            attacker = p;
            arrow = a;
        }

        // Hambre de combate para Orco (+30%) y Bestia (+25%)
        if (attacker != null) {
            PlayerData atkPre = plugin.getPlayerDataManager().get(attacker.getUniqueId());
            if (atkPre != null && atkPre.hasAssignedRace()) {
                String ar = atkPre.getRace().toLowerCase();
                if (ar.equals("orc") || ar.equals("beastkin")) {
                    combatHungerActive.add(attacker.getUniqueId());
                }
            }
        }

        // Talento crítico + Humano L6 (+40% daño crítico)
        if (attacker != null && event.getEntity() instanceof LivingEntity) {
            PlayerData atkData = plugin.getPlayerDataManager().get(attacker.getUniqueId());
            if (atkData != null && atkData.hasAssignedRace()) {
                boolean isCrit = (arrow != null && arrow.isCritical())
                        || (!attacker.isOnGround() && attacker.getFallDistance() > 0.0F
                        && !attacker.isInWater() && !attacker.isClimbing()
                        && !attacker.hasPotionEffect(PotionEffectType.BLINDNESS));
                if (isCrit) {
                    if (atkData.hasTalent("combat_crit")) {
                        event.setDamage(event.getDamage() * 1.10);
                    }
                    if (atkData.getRace().equalsIgnoreCase("human") && atkData.getLevel() >= 6) {
                        event.setDamage(event.getDamage() * 1.40);
                        attacker.getWorld().spawnParticle(Particle.CRIT, event.getEntity().getLocation().add(0, 1, 0), 12, 0.3, 0.3, 0.3, 0.1);
                    }
                }

                // Druida: Ácido Cítrico (-20% efectividad de armadura objetivo -> +20% daño neto)
                if (atkData.getRace().equalsIgnoreCase("druid") && atkData.getLevel() >= 2) {
                    event.setDamage(event.getDamage() * 1.20);
                    event.getEntity().getWorld().spawnParticle(Particle.ITEM_SLIME, event.getEntity().getLocation().add(0, 1, 0), 6, 0.25, 0.3, 0.25, 0.02);
                }

                // Dragonkin: Barrido de cola al agacharse y golpear
                if (atkData.getRace().equalsIgnoreCase("dragonkin") && attacker.isSneaking()
                        && checkCooldown(attacker, "BarridoDeCola", 12)) {
                    for (Entity e : attacker.getNearbyEntities(3.5, 2.0, 3.5)) {
                        if (e instanceof LivingEntity nearbyTarget && nearbyTarget != attacker && nearbyTarget != event.getEntity()) {
                            nearbyTarget.damage(4.0, attacker);
                            Vector kb = nearbyTarget.getLocation().toVector().subtract(attacker.getLocation().toVector()).normalize().multiply(0.9).setY(0.35);
                            nearbyTarget.setVelocity(kb);
                        }
                    }
                    attacker.getWorld().playSound(attacker.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 0.9f, 0.8f);
                    ColorUtils.sendActionBar(attacker, "&5&l[DRACONIANO] &7¡Barrido de Cola ejecutado!");
                }

                // Yokai L6: golpes aplican Levitación breve
                if (atkData.getRace().equalsIgnoreCase("yokai") && atkData.getLevel() >= 6
                        && event.getEntity() instanceof LivingEntity livingVic) {
                    livingVic.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 25, 0));
                }
            }
        }

        // Lobos aliados defienden a Bestia cuando es atacada
        if (event.getEntity() instanceof Player victim) {
            PlayerData vicData = plugin.getPlayerDataManager().get(victim.getUniqueId());
            if (vicData != null && vicData.hasAssignedRace()) {
                String vicRace = vicData.getRace().toLowerCase();
                int vicLevel = vicData.getLevel();

                if (vicRace.equals("beastkin") && event.getDamager() instanceof LivingEntity aggressor) {
                    for (Entity nearby : victim.getNearbyEntities(14.0, 6.0, 14.0)) {
                        if (nearby instanceof Wolf wolf) {
                            wolf.setTarget(aggressor);
                        }
                    }
                }

                // Hombre Lobo vulnerabilidad a armas de Hierro (+30% daño recibido)
                if (vicRace.equals("werewolf") && attacker != null) {
                    Material weap = attacker.getInventory().getItemInMainHand().getType();
                    if (weap.name().startsWith("IRON_")) {
                        event.setDamage(event.getDamage() * 1.30);
                        victim.getWorld().spawnParticle(Particle.CRIT, victim.getLocation().add(0, 1, 0), 8, 0.2, 0.3, 0.2, 0.1);
                    }
                }

                // Hada L6: Campo de absorción (-30% daño)
                if (vicRace.equals("fairy") && vicLevel >= 6) {
                    event.setDamage(event.getDamage() * 0.70);
                }

                // Duende vulnerabilidad a hachas y mazas
                if (vicRace.equals("goblin") && vicLevel < 6 && attacker != null) {
                    Material weapon = attacker.getInventory().getItemInMainHand().getType();
                    if (weapon.name().contains("AXE") || weapon.name().contains("MACE")) {
                        double mult = (vicLevel >= 3) ? 1.10 : 1.25;
                        event.setDamage(event.getDamage() * mult);
                    }
                }

                // Orco L6: Furia Inquebrantable
                if (vicRace.equals("orc") && vicLevel >= 6 && victim.getHealth() - event.getFinalDamage() <= victim.getMaxHealth() * 0.20) {
                    if (checkCooldown(victim, "FuriaInquebrantable", 180)) {
                        unyieldingFuryActive.add(victim.getUniqueId());
                        ColorUtils.sendMessage(victim, "&4&l[FURIA] &c¡Te has vuelto invulnerable por 5 segundos!");
                        victim.getWorld().spawnParticle(Particle.LAVA, victim.getLocation(), 20, 0.5, 0.5, 0.5, 0.1);
                        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                            unyieldingFuryActive.remove(victim.getUniqueId());
                            ColorUtils.sendMessage(victim, "&7Tu estado de invulnerabilidad ha finalizado.");
                        }, 100L);
                    }
                }

                // PvP directo Atacante vs Víctima
                if (attacker != null) {
                    PlayerData atkData = plugin.getPlayerDataManager().get(attacker.getUniqueId());
                    if (atkData != null && atkData.hasAssignedRace()) {
                        String atkRace = atkData.getRace().toLowerCase();
                        int atkLevel = atkData.getLevel();

                        if (atkRace.equals("werewolf")) {
                            ItemStack chest = attacker.getInventory().getChestplate();
                            if (chest != null && (chest.getType() == Material.DIAMOND_CHESTPLATE || chest.getType() == Material.NETHERITE_CHESTPLATE || chest.getType() == Material.IRON_CHESTPLATE)) {
                                double penalty = (atkLevel >= 6) ? 1.0 : (atkLevel >= 4 ? 0.90 : 0.80);
                                event.setDamage(event.getDamage() * penalty);
                            }
                            if (atkLevel >= 2 && attacker.getInventory().getItemInMainHand().getType() == Material.AIR) {
                                event.setDamage(event.getDamage() + 4.0);
                                victim.getWorld().spawnParticle(Particle.CRIT, victim.getLocation().add(0, 1, 0), 8, 0.2, 0.2, 0.2, 0.1);
                            }
                            if (atkLevel >= 5) {
                                applyBleedEffect(victim, 3);
                            }
                            if (atkLevel >= 6) {
                                event.setDamage(event.getDamage() * 1.50);
                            }
                        }

                        if (atkRace.equals("vampire") && Math.random() < 0.35) {
                            attacker.setFoodLevel(Math.min(20, attacker.getFoodLevel() + 2));
                            attacker.setSaturation(Math.min(20.0f, attacker.getSaturation() + 2.0f));
                            attacker.getWorld().playSound(attacker.getLocation(), Sound.ENTITY_GENERIC_DRINK, 0.8f, 0.9f);
                            attacker.getWorld().spawnParticle(Particle.DUST, victim.getLocation().add(0, 1, 0), 12, 0.2, 0.3, 0.2, new Particle.DustOptions(Color.RED, 1.3f));
                            ColorUtils.sendActionBar(attacker, "&4&l[MORDISCO VAMPÍRICO] &c+2 Hambre / +2 Saturación");
                        }

                        if (atkRace.equals("vampire") && atkLevel >= 3) {
                            double heal = Math.max(0.5, event.getFinalDamage() * 0.15);
                            attacker.setHealth(Math.min(attacker.getMaxHealth(), attacker.getHealth() + heal));
                        }

                        if (atkRace.equals("vampire") && atkLevel >= 5) {
                            victim.setFoodLevel(Math.max(0, victim.getFoodLevel() - 2));
                        }

                        if (atkRace.equals("angel") && (vicRace.equals("vampire") || vicRace.equals("undead"))) {
                            event.setDamage(event.getDamage() * 1.30);
                            victim.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, victim.getLocation().add(0, 1, 0), 10, 0.3, 0.3, 0.3, 0.1);
                        }

                        // Quiebre de escudo: Gigante/Orco siempre, Demonio 25% probabilidad
                        if (victim.isBlocking()) {
                            boolean breakShield = atkRace.equals("orc") || atkRace.equals("giant")
                                    || beastFormActive.contains(attacker.getUniqueId())
                                    || (atkRace.equals("demon") && Math.random() < 0.25);
                            if (breakShield) {
                                victim.setCooldown(Material.SHIELD, 100);
                                ColorUtils.sendMessage(victim, "&c&l¡Tu escudo fue destrozado en combate!");
                                ColorUtils.sendMessage(attacker, "&a&l¡Has quebrado el escudo enemigo!");
                                victim.getWorld().playSound(victim.getLocation(), Sound.ITEM_SHIELD_BREAK, 1.0f, 0.8f);
                            }
                        }

                        if (atkRace.equals("orc") && attacker.getInventory().getItemInMainHand().getType().name().contains("AXE")) {
                            event.setDamage(event.getDamage() * 1.25);
                            if (atkLevel >= 2 && attacker.getHealth() <= attacker.getMaxHealth() * 0.30) {
                                event.setDamage(event.getDamage() * 1.50);
                                attacker.getWorld().spawnParticle(Particle.FLAME, attacker.getLocation(), 5, 0.2, 0.2, 0.2, 0.05);
                            }
                        }

                        if (atkRace.equals("elf") && arrow != null) {
                            event.setDamage(event.getDamage() * 1.15);
                            if (atkLevel >= 5) {
                                event.setDamage(event.getDamage() * 1.30);
                            }
                        }

                        if (atkRace.equals("elf") && atkLevel >= 3 && arrow != null && victim != attacker) {
                            var team = victim.getScoreboard().getPlayerTeam(victim);
                            if (team != null && team.equals(attacker.getScoreboard().getPlayerTeam(attacker))) {
                                event.setCancelled(true);
                                victim.setHealth(Math.min(victim.getMaxHealth(), victim.getHealth() + 6.0));
                                ColorUtils.sendMessage(victim, "&a¡Una flecha silvana aliada te ha restaurado 3 corazones!");
                                return;
                            }
                        }

                        if (atkRace.equals("goblin") && atkLevel >= 3) {
                            performGoblinTheft(attacker, victim, atkLevel);
                        }

                        if (atkRace.equals("demon")) {
                            victim.setFireTicks(80);
                            victim.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, victim.getLocation().add(0, 1, 0), 6, 0.2, 0.2, 0.2, 0.05);
                        }

                        if ((vicRace.equals("undead") || vicRace.equals("demon"))) {
                            ItemStack weapon = attacker.getInventory().getItemInMainHand();
                            if (weapon.hasItemMeta() && weapon.getItemMeta().hasEnchant(Enchantment.SMITE)) {
                                double mult = vicRace.equals("demon") ? 1.20 : (vicLevel >= 6 ? 1.0 : (vicLevel >= 4 ? 1.4 : 2.0));
                                event.setDamage(event.getDamage() * mult);
                                victim.getWorld().playSound(victim.getLocation(), Sound.ENTITY_WITHER_HURT, 0.9f, 1.2f);
                            }
                        }

                        if (atkRace.equals("undead") && atkLevel >= 3 && attacker.getInventory().getItemInMainHand().getType() == Material.AIR) {
                            victim.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 100, 1));
                            attacker.giveExp(3);
                        }
                    }
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
        if (victim.getGameMode() == GameMode.CREATIVE || victim.getGameMode() == GameMode.SPECTATOR) return;
        if (victim.hasPermission("cytricosraces.theft.exempt") || victim.hasPermission("cytricosraces.admin")) return;
        if (plugin.getWorldGuardHook() != null && !plugin.getWorldGuardHook().canPvp(goblin, victim.getLocation())) return;

        double chance = 0.20 + (goblinLevel - 3) * 0.05;
        if (Math.random() > chance) return;
        if (!checkCooldown(goblin, "RoboDuende", 45)) return;

        int heldSlot = victim.getInventory().getHeldItemSlot();
        List<Integer> candidateSlots = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            if (i == heldSlot) continue;
            ItemStack it = victim.getInventory().getItem(i);
            if (it == null || it.getType() == Material.AIR) continue;
            Material m = it.getType();
            if (m.name().contains("NETHERITE_") || m == Material.TOTEM_OF_UNDYING || m.name().contains("SHULKER_BOX")) continue;
            candidateSlots.add(i);
        }

        if (candidateSlots.isEmpty()) return;

        int chosenSlot = candidateSlots.get(new Random().nextInt(candidateSlots.size()));
        ItemStack victimItem = victim.getInventory().getItem(chosenSlot);
        if (victimItem == null || victimItem.getType() == Material.AIR) return;

        int stealQty = (victimItem.getMaxStackSize() > 1 && victimItem.getAmount() > 1)
                ? Math.min(Math.min(4, victimItem.getAmount()), 1 + (goblinLevel - 3))
                : 1;

        ItemStack stolenStack = victimItem.clone();
        stolenStack.setAmount(stealQty);

        if (victimItem.getAmount() <= stealQty) {
            victim.getInventory().setItem(chosenSlot, null);
        } else {
            victimItem.setAmount(victimItem.getAmount() - stealQty);
            victim.getInventory().setItem(chosenSlot, victimItem);
        }

        plugin.getCustomItemManager().giveItemSafely(goblin, stolenStack);
        String itemName = (stolenStack.hasItemMeta() && stolenStack.getItemMeta().displayName() != null)
                ? ColorUtils.toPlainText(stolenStack.getItemMeta().displayName())
                : stolenStack.getType().name();

        ColorUtils.sendMessage(goblin, "&a&l[DUENDE ASTUTO] &7¡Sustrajiste &e" + stealQty + "x " + itemName + " &7a &f" + victim.getName() + "&7!");
        ColorUtils.sendMessage(victim, "&c&l[¡HURTO SUFRIDO!] &7¡Un duende te ha robado &e" + stealQty + "x " + itemName + "&7!");
        goblin.getWorld().playSound(goblin.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.2f, 1.5f);
        victim.getWorld().playSound(victim.getLocation(), Sound.ENTITY_CHICKEN_EGG, 1.0f, 0.6f);
    }

    // =========================================================================
    // 7. CONSUMIBLES, POCIONES CURATIVAS (UNDEAD) Y HAMBRE DE COMBATE
    // =========================================================================

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        if (data == null || !data.hasAssignedRace()) return;
        String raceId = data.getRace().toLowerCase();
        ItemStack consumed = event.getItem();
        Material mat = consumed.getType();

        // Demonio: pociones curativas drenan saturación
        if (raceId.equals("demon") && mat == Material.POTION
                && consumed.getItemMeta() instanceof org.bukkit.inventory.meta.PotionMeta pm
                && pm.getBasePotionType() != null && pm.getBasePotionType().name().contains("HEAL")) {
            player.setSaturation(0f);
            player.setFoodLevel(Math.max(1, player.getFoodLevel() - 3));
            ColorUtils.sendActionBar(player, "&c&l[DEMONIO] &7La curación sagrada consume tu energía interna.");
        }

        // No-Muerto: curación instantánea causa daño mágico
        if (raceId.equals("undead") && mat == Material.POTION
                && consumed.getItemMeta() instanceof org.bukkit.inventory.meta.PotionMeta pm
                && pm.getBasePotionType() != null && pm.getBasePotionType().name().contains("HEAL")) {
            event.setCancelled(true);
            player.damage(5.0);
            ColorUtils.sendActionBar(player, "&4&l[NO-MUERTO] &c¡La curación mágica quema tus restos!");
            return;
        }

        if (raceId.equals("werewolf")) {
            if (mat == Material.COOKED_BEEF || mat == Material.COOKED_PORKCHOP || mat == Material.COOKED_MUTTON
                    || mat == Material.COOKED_CHICKEN || mat == Material.COOKED_COD || mat == Material.COOKED_SALMON) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 160, 1));
                ColorUtils.sendMessage(player, "&cTu naturaleza salvaje aborrece la carne cocinada.");
            }
        }

        if (raceId.equals("vampire")) {
            if (mat == Material.BEETROOT_SOUP) {
                player.setFoodLevel(Math.min(20, player.getFoodLevel() + 6));
                player.setSaturation(Math.min(20.0f, player.getSaturation() + 6.0f));
                player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 0));
                ColorUtils.sendActionBar(player, "&4&l[CALDO DE SANGRE] &cNutrición vital absorbida (+Regeneración)");
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_DRINK, 1.0f, 0.9f);
                return;
            }
            if (mat != Material.POTION && mat != Material.HONEY_BOTTLE && mat != Material.MUSHROOM_STEW && mat != Material.SUSPICIOUS_STEW) {
                event.setCancelled(true);
                ColorUtils.sendMessage(player, "&5Como vampiro, solo te nutres de sangre fresca, caldo de remolacha y fluidos místicos.");
            }
        }

        if (raceId.equals("druid")) {
            if (mat.name().contains("BEEF") || mat.name().contains("PORK") || mat.name().contains("MUTTON") || mat.name().contains("CHICKEN")) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 140, 1));
                ColorUtils.sendMessage(player, "&eTu esencia botánica rechaza el tejido animal procesado.");
            } else if (mat == Material.APPLE || mat == Material.GOLDEN_APPLE || mat == Material.ENCHANTED_GOLDEN_APPLE
                    || mat == Material.MELON_SLICE || mat == Material.SWEET_BERRIES || mat == Material.GLOW_BERRIES) {
                player.setSaturation(player.getSaturation() + 5.0f);
                ColorUtils.sendMessage(player, "&a¡Absorbes nutrición cítrica concentrada!");
            }
        }

        if (raceId.equals("undead") && data.getLevel() >= 2 && mat == Material.ROTTEN_FLESH) {
            player.setHealth(Math.min(player.getMaxHealth(), player.getHealth() + 4.0));
            player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 200, 0));
            ColorUtils.sendMessage(player, "&2¡La carne podrida revitaliza tus huesos (+Absorción)!");
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPotionSplash(PotionSplashEvent event) {
        if (event.getPotion().getPotionMeta().getBasePotionType() == null) return;
        String baseName = event.getPotion().getPotionMeta().getBasePotionType().name();
        if (!baseName.contains("HEAL")) return;

        for (LivingEntity affected : event.getAffectedEntities()) {
            if (affected instanceof Player p) {
                PlayerData data = plugin.getPlayerDataManager().get(p.getUniqueId());
                if (data != null && data.hasAssignedRace() && data.getRace().equalsIgnoreCase("undead")) {
                    event.setIntensity(p, 0.0);
                    p.damage(4.0);
                    ColorUtils.sendActionBar(p, "&4&l[NO-MUERTO] &c¡La poción de curación daña tu esencia!");
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        if (data == null || !data.hasAssignedRace()) return;
        String race = data.getRace().toLowerCase();

        if (race.equals("undead")) {
            event.setCancelled(true);
            player.setFoodLevel(20);
            return;
        }

        if (event.getFoodLevel() < player.getFoodLevel() && combatHungerActive.remove(player.getUniqueId())) {
            int delta = player.getFoodLevel() - event.getFoodLevel();
            int extra = Math.max(1, (int) Math.round(delta * (race.equals("orc") ? 0.30 : 0.25)));
            event.setFoodLevel(Math.max(0, event.getFoodLevel() - extra));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onShootBow(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        if (data == null || !data.hasAssignedRace()) return;
        String raceId = data.getRace().toLowerCase();

        if (raceId.equals("beastkin")) {
            ItemStack bow = event.getBow();
            if (bow != null && bow.getType() == Material.BOW) {
                event.setCancelled(true);
                ColorUtils.sendMessage(player, "&6Tu anatomía bestial desvía el arco. &e¡Solo tienes destreza con ballestas!");
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.7f, 0.8f);
                return;
            }
        }

        if (raceId.equals("orc") && event.getProjectile() instanceof Arrow arrow) {
            Vector spread = new Vector((Math.random() - 0.5) * 0.4, (Math.random() - 0.5) * 0.2, (Math.random() - 0.5) * 0.4);
            arrow.setVelocity(arrow.getVelocity().add(spread));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onItemDamage(PlayerItemDamageEvent event) {
        Player player = event.getPlayer();
        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        if (data == null || !data.hasAssignedRace()) return;
        String raceId = data.getRace().toLowerCase();
        int level = data.getLevel();
        Material type = event.getItem().getType();

        // Demonio: armadura de oro se desgasta el doble de rápido
        if (raceId.equals("demon") && type.name().startsWith("GOLDEN_") && isArmorPiece(type)) {
            event.setDamage(event.getDamage() * 2);
        }

        // Enano: armaduras duran +50% más
        if (raceId.equals("dwarf") && isArmorPiece(type) && Math.random() < 0.34) {
            event.setCancelled(true);
            return;
        }

        if (raceId.equals("dragonkin") && (type.name().contains("IRON_") || type.name().contains("GOLDEN_"))) {
            event.setDamage((int) Math.ceil(event.getDamage() * 1.25));
        }

        if (raceId.equals("human") && level >= 3 && Math.random() < 0.50) {
            event.setCancelled(true);
        }

        if (raceId.equals("dwarf") && level >= 5 && type == Material.SHIELD) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        if (data == null || !data.hasAssignedRace()) return;
        Block block = event.getBlock();
        String matName = block.getType().name();

        if (matName.contains("_ORE") || matName.contains("GOLD") || matName.contains("DIAMOND") || matName.contains("ANCIENT_DEBRIS")) {
            if (plugin.getQuestManager() != null) {
                plugin.getQuestManager().recordAction(player, QuestManager.QuestType.MINE_BLOCKS, 1);
            }
        }

        if (data.getRace().equalsIgnoreCase("dwarf") && data.getLevel() >= 2 && matName.contains("_ORE")) {
            if (Math.random() < 0.15) {
                block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(block.getType(), 1));
                ColorUtils.sendMessage(player, "&6&l[MINERÍA ENANA] &7¡Tu destreza subterránea extrajo doble mineral!");
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7f, 1.8f);
            }
        }
    }

    // =========================================================================
    // 8. HABILIDADES ACTIVAS DE LAS 16 RAZAS (Shift + F / /raza habilidad)
    // =========================================================================

    public void triggerActiveAbility(Player player) {
        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        if (data == null || !data.hasAssignedRace()) {
            ColorUtils.sendMessage(player, "&cPrimero elige una raza con &e/razas&c.");
            return;
        }
        String raceId = data.getRace().toLowerCase();
        int level = data.getLevel();

        switch (raceId) {
            case "werewolf" -> {
                if (level >= 6) {
                    if (checkCooldown(player, "FormaFenrir", 120)) {
                        beastFormActive.add(player.getUniqueId());
                        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 300, 2));
                        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 300, 1));
                        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 300, 0));
                        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WOLF_ANGRY_GROWL, 1.5f, 0.6f);
                        player.getWorld().spawnParticle(Particle.FLAME, player.getLocation(), 30, 0.5, 0.5, 0.5, 0.1);
                        ColorUtils.sendMessage(player, "&4&l[FENRIR] &c¡Forma Bestial Ancestral activada por 15s!");
                        plugin.getServer().getScheduler().runTaskLater(plugin, () -> beastFormActive.remove(player.getUniqueId()), 300L);
                    }
                } else if (level >= 3) {
                    if (checkCooldown(player, "RugidoAterrador", 45)) {
                        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WOLF_GROWL, 1.5f, 0.7f);
                        player.getWorld().spawnParticle(Particle.SONIC_BOOM, player.getLocation().add(0, 1, 0), 1);
                        for (Entity e : player.getNearbyEntities(10, 5, 10)) {
                            if (e instanceof LivingEntity target && target != player) {
                                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 120, 2));
                            }
                        }
                        ColorUtils.sendMessage(player, "&a&l[HABILIDAD] &7¡Has desatado tu Rugido Aterrador!");
                    }
                } else {
                    ColorUtils.sendMessage(player, "&cRequieres Nivel 3 o superior para desbloquear tu habilidad activa.");
                }
            }

            case "vampire" -> {
                if (level >= 6) {
                    if (checkCooldown(player, "SenorDeLaNiebla", 70)) {
                        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 120, 0));
                        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 120, 2));
                        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BAT_TAKEOFF, 1.2f, 0.7f);
                        player.getWorld().spawnParticle(Particle.LARGE_SMOKE, player.getLocation().add(0, 1, 0), 30, 0.6, 0.5, 0.6, 0.04);
                        ColorUtils.sendMessage(player, "&5&l[SEÑOR DE LA NIEBLA] &d¡Te disuelves en bruma nocturna durante 6s!");
                    }
                } else if (level >= 4) {
                    if (checkCooldown(player, "SaltoSombras", 20)) {
                        Location target = player.getLocation().add(player.getLocation().getDirection().multiply(10));
                        if (target.getBlock().getType().isSolid()) {
                            target = player.getLocation().add(player.getLocation().getDirection().multiply(3));
                        }
                        player.teleport(target);
                        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.2f);
                        player.getWorld().spawnParticle(Particle.PORTAL, player.getLocation(), 25, 0.3, 0.5, 0.3, 0.1);
                        ColorUtils.sendMessage(player, "&5&l[SALTO DE SOMBRAS] &7¡Te has desplazado entre las tinieblas!");
                    }
                } else {
                    ColorUtils.sendMessage(player, "&cRequieres Nivel 4 para Salto de Sombras.");
                }
            }

            case "dragonkin" -> {
                if (level >= 5) {
                    if (checkCooldown(player, "RugidoDeTrueno", 40)) {
                        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.4f, 0.8f);
                        player.getWorld().spawnParticle(Particle.SONIC_BOOM, player.getLocation().add(0, 1.2, 0), 2);
                        for (Entity e : player.getNearbyEntities(8, 4, 8)) {
                            if (e instanceof LivingEntity target && target != player) {
                                if (plugin.getWorldGuardHook() == null || !(target instanceof Player pVic) || plugin.getWorldGuardHook().canPvp(player, pVic.getLocation())) {
                                    target.damage(7.0, player);
                                    target.setVelocity(target.getLocation().toVector().subtract(player.getLocation().toVector()).normalize().multiply(1.2).setY(0.5));
                                }
                            }
                        }
                        ColorUtils.sendMessage(player, "&5&l[RUGIDO DE TRUENO] &d¡Onda expansiva dracónica desatada!");
                    }
                } else if (level >= 3) {
                    if (checkCooldown(player, "AlientoFuego", 30)) {
                        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.4f);
                        for (int i = 1; i <= 6; i++) {
                            Location cone = player.getEyeLocation().add(player.getLocation().getDirection().multiply(i));
                            player.getWorld().spawnParticle(Particle.FLAME, cone, 14, 0.3, 0.3, 0.3, 0.05);
                            for (Entity e : cone.getWorld().getNearbyEntities(cone, 1.5, 1.5, 1.5)) {
                                if (e instanceof LivingEntity target && target != player) {
                                    if (plugin.getWorldGuardHook() == null || !(target instanceof Player pVic) || plugin.getWorldGuardHook().canPvp(player, pVic.getLocation())) {
                                        target.setFireTicks(100);
                                        target.damage(5.0, player);
                                    }
                                }
                            }
                        }
                        ColorUtils.sendMessage(player, "&c&l[ALIENTO DE FUEGO] &7¡Has abrasado el frente con llamas dracónicas!");
                    }
                } else {
                    ColorUtils.sendMessage(player, "&cRequieres Nivel 3 para Aliento de Fuego.");
                }
            }

            case "orc" -> {
                if (level >= 5) {
                    if (checkCooldown(player, "GolpeSismicoMagma", 35)) {
                        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.1f, 0.8f);
                        player.getWorld().spawnParticle(Particle.LAVA, player.getLocation().add(0, 0.5, 0), 25, 2.0, 0.4, 2.0, 0.05);
                        for (Entity e : player.getNearbyEntities(6, 3, 6)) {
                            if (e instanceof LivingEntity target && target != player) {
                                if (plugin.getWorldGuardHook() == null || !(target instanceof Player pVic) || plugin.getWorldGuardHook().canPvp(player, pVic.getLocation())) {
                                    target.damage(7.0, player);
                                    target.setFireTicks(80);
                                    target.setVelocity(new Vector(0, 0.65, 0));
                                }
                            }
                        }
                        ColorUtils.sendMessage(player, "&4&l[GOLPE SÍSMICO EN MAGMA] &c¡Sacudes la tierra con fuego y hacha!");
                    }
                } else if (level >= 3) {
                    if (checkCooldown(player, "GritoGuerra", 45)) {
                        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PIGLIN_BRUTE_ANGRY, 1.2f, 0.8f);
                        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 200, 1));
                        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 200, 0));
                        for (Entity e : player.getNearbyEntities(8, 4, 8)) {
                            if (e instanceof Player ally && ally != player) {
                                ally.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 160, 0));
                            }
                        }
                        ColorUtils.sendMessage(player, "&4&l[GRITO DE GUERRA] &7¡Fuerza y temple de combate activados!");
                    }
                } else {
                    ColorUtils.sendMessage(player, "&cRequieres Nivel 3 para Grito de Guerra.");
                }
            }

            case "giant" -> {
                if (level >= 3) {
                    if (checkCooldown(player, "ImpactoTelurico", 40)) {
                        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_IRON_GOLEM_ATTACK, 1.5f, 0.6f);
                        player.getWorld().spawnParticle(Particle.EXPLOSION, player.getLocation(), 3);
                        double dmg = (level >= 6) ? 8.0 : 6.0;
                        for (Entity e : player.getNearbyEntities(6, 3, 6)) {
                            if (e instanceof LivingEntity target && target != player) {
                                if (plugin.getWorldGuardHook() == null || !(target instanceof Player pVic) || plugin.getWorldGuardHook().canPvp(player, pVic.getLocation())) {
                                    target.setVelocity(new Vector(0, 1.15, 0));
                                    target.damage(dmg, player);
                                }
                            }
                        }
                        ColorUtils.sendMessage(player, "&6&l[IMPACTO TELÚRICO] &7¡Has sacudido el suelo bajo tus pies!");
                    }
                } else {
                    ColorUtils.sendMessage(player, "&cRequieres Nivel 3 para Impacto Telúrico.");
                }
            }

            case "dwarf" -> {
                if (level >= 6) {
                    if (checkCooldown(player, "ColosoDeTitanio", 55)) {
                        stoneSkinActive.add(player.getUniqueId());
                        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 200, 1));
                        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 200, 1));
                        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1.2f, 0.9f);
                        ColorUtils.sendMessage(player, "&6&l[COLOSO DE TITANIO] &e¡Armadura viviente inquebrantable por 10s!");
                        plugin.getServer().getScheduler().runTaskLater(plugin, () -> stoneSkinActive.remove(player.getUniqueId()), 200L);
                    }
                } else if (level >= 4) {
                    if (checkCooldown(player, "PielPiedra", 60)) {
                        stoneSkinActive.add(player.getUniqueId());
                        ColorUtils.sendMessage(player, "&e&l[PIEL DE PIEDRA] &7Inmune a proyectiles durante 6s.");
                        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1.0f, 1.2f);
                        plugin.getServer().getScheduler().runTaskLater(plugin, () -> stoneSkinActive.remove(player.getUniqueId()), 120L);
                    }
                } else {
                    ColorUtils.sendMessage(player, "&cRequieres Nivel 4 para Piel de Piedra.");
                }
            }

            case "druid" -> {
                if (level >= 3) {
                    if (checkCooldown(player, "ExplosionVitaminica", 35)) {
                        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_SPLASH_POTION_THROW, 1.0f, 1.2f);
                        player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation().add(0, 1, 0), 20, 1.5, 0.5, 1.5, 0.1);
                        for (Entity e : player.getNearbyEntities(7, 4, 7)) {
                            if (e instanceof Player ally) {
                                ally.setHealth(Math.min(ally.getMaxHealth(), ally.getHealth() + 8.0));
                            }
                        }
                        player.setHealth(Math.min(player.getMaxHealth(), player.getHealth() + 8.0));
                        player.removePotionEffect(PotionEffectType.POISON);
                        player.removePotionEffect(PotionEffectType.WITHER);
                        ColorUtils.sendMessage(player, "&a&l[EXPLOSIÓN VITAMÍNICA] &7¡Energía botánica curativa desatada!");
                    }
                } else {
                    ColorUtils.sendMessage(player, "&cRequieres Nivel 3 para Explosión Vitamínica.");
                }
            }

            case "yokai" -> {
                if (level >= 3) {
                    if (checkCooldown(player, "PosesionTemporal", 40)) {
                        Location targetLoc = player.getLocation().add(player.getLocation().getDirection().multiply(4));
                        if (plugin.getWorldGuardHook() != null && !plugin.getWorldGuardHook().canBuild(player, targetLoc)) {
                            ColorUtils.sendMessage(player, "&cNo puedes atravesar hacia una zona protegida.");
                            return;
                        }
                        player.teleport(targetLoc);
                        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 60, 0));
                        for (Entity e : player.getNearbyEntities(4, 3, 4)) {
                            if (e instanceof LivingEntity target && target != player) {
                                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 3));
                                target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 60, 1));
                            }
                        }
                        player.getWorld().playSound(player.getLocation(), Sound.PARTICLE_SOUL_ESCAPE, 1.2f, 1.4f);
                        player.getWorld().spawnParticle(Particle.SOUL, player.getLocation(), 20, 0.3, 0.5, 0.3, 0.05);
                        ColorUtils.sendMessage(player, "&5&l[POSESIÓN TEMPORAL] &b¡Desfase espiritual ejecutado!");
                    }
                } else {
                    ColorUtils.sendMessage(player, "&cRequieres Nivel 3 para Posesión Temporal.");
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
                            } else if (e instanceof LivingEntity target && target != player) {
                                if (plugin.getWorldGuardHook() == null || !(target instanceof Player pVic) || plugin.getWorldGuardHook().canPvp(player, pVic.getLocation())) {
                                    target.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 60, 0));
                                }
                            }
                        }
                        ColorUtils.sendMessage(player, "&d&l[POLVO DE HADA] &7¡Aura feérica liberada!");
                    }
                } else {
                    ColorUtils.sendMessage(player, "&cRequieres Nivel 3 para Polvo de Hada.");
                }
            }

            case "angel" -> {
                if (level >= 4) {
                    if (checkCooldown(player, "JuicioCelestial", 60)) {
                        player.getWorld().playSound(player.getLocation(), Sound.ITEM_TOTEM_USE, 1.0f, 1.6f);
                        player.getWorld().spawnParticle(Particle.FLASH, player.getLocation().add(0, 2, 0), 3);
                        player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 200, 1));
                        for (Entity e : player.getNearbyEntities(8, 4, 8)) {
                            if (e instanceof Player ally && ally != player) {
                                ally.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 200, 1));
                            } else if (e instanceof Monster m) {
                                m.damage(6.0, player);
                            }
                        }
                        ColorUtils.sendMessage(player, "&e&l[JUICIO CELESTIAL] &7¡Escudo sagrado y luz purificadora desatados!");
                    }
                } else {
                    ColorUtils.sendMessage(player, "&cRequieres Nivel 4 para Juicio Celestial.");
                }
            }

            case "demon" -> {
                if (level >= 3) {
                    if (checkCooldown(player, "ExplosionDeAzufre", 35)) {
                        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1.2f, 0.7f);
                        player.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, player.getLocation().add(0, 1, 0), 32, 1.5, 0.5, 1.5, 0.08);
                        if (level >= 5) {
                            player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 160, 1));
                        }
                        for (Entity e : player.getNearbyEntities(6, 3, 6)) {
                            if (e instanceof LivingEntity target && target != player) {
                                if (plugin.getWorldGuardHook() == null || !(target instanceof Player pVic) || plugin.getWorldGuardHook().canPvp(player, pVic.getLocation())) {
                                    target.setFireTicks(120);
                                    target.damage(5.0, player);
                                }
                            }
                        }
                        ColorUtils.sendMessage(player, "&4&l[EXPLOSIÓN DE AZUFRE] &c¡Dominio infernal liberado!");
                    }
                } else {
                    ColorUtils.sendMessage(player, "&cRequieres Nivel 3 para Explosión de Azufre.");
                }
            }

            case "goblin" -> {
                if (level >= 4) {
                    if (checkCooldown(player, "BombaHumo", 45)) {
                        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.0f, 0.8f);
                        player.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, player.getLocation(), 35, 2.0, 1.0, 2.0, 0.05);
                        for (Entity e : player.getNearbyEntities(5, 3, 5)) {
                            if (e instanceof LivingEntity target && target != player) {
                                target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 80, 0));
                            }
                        }
                        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 100, 2));
                        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 100, 0));
                        ColorUtils.sendMessage(player, "&2&l[BOMBA DE HUMO] &a¡Escapas envuelto en humo denso!");
                    }
                } else {
                    ColorUtils.sendMessage(player, "&cRequieres Nivel 4 para Bomba de Humo.");
                }
            }

            case "human" -> {
                if (level >= 2) {
                    if (checkCooldown(player, "SobrecargaAdrenalina", 45)) {
                        player.setHealth(Math.min(player.getMaxHealth(), player.getHealth() + 6.0));
                        player.setFoodLevel(Math.min(20, player.getFoodLevel() + 6));
                        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 160, 0));
                        player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 160, 1));
                        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.4f);
                        ColorUtils.sendMessage(player, "&b&l[SOBRECARGA DE ADRENALINA] &e¡Escudo rúnico y vitalidad táctica activados!");
                    }
                } else {
                    ColorUtils.sendMessage(player, "&cRequieres Nivel 2 para Sobrecarga de Adrenalina.");
                }
            }

            case "beastkin" -> {
                if (level >= 6) {
                    if (checkCooldown(player, "BestiaTotemicaSuprema", 65)) {
                        beastFormActive.add(player.getUniqueId());
                        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 240, 1));
                        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 240, 2));
                        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 240, 1));
                        ColorUtils.sendMessage(player, "&6&l[BESTIA TOTÉMICA SUPREMA] &e¡Instinto alfa máximo durante 12s!");
                        plugin.getServer().getScheduler().runTaskLater(plugin, () -> beastFormActive.remove(player.getUniqueId()), 240L);
                    }
                } else if (level >= 3) {
                    if (checkCooldown(player, "SentidoSalvaje", 40)) {
                        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 160, 1));
                        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_OCELOT_DEATH, 0.8f, 1.4f);
                        for (Entity e : player.getNearbyEntities(15, 6, 15)) {
                            if (e instanceof LivingEntity target && target != player) {
                                target.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 160, 0));
                            }
                        }
                        ColorUtils.sendMessage(player, "&6&l[SENTIDO SALVAJE] &e¡Presas marcadas alrededor!");
                    }
                } else {
                    ColorUtils.sendMessage(player, "&cRequieres Nivel 3 para Sentido Salvaje.");
                }
            }

            case "undead" -> {
                if (level >= 3) {
                    if (checkCooldown(player, "NubePutrida", 45)) {
                        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.7f, 1.6f);
                        player.getWorld().spawnParticle(Particle.SNEEZE, player.getLocation(), 30, 1.5, 0.5, 1.5, 0.05);
                        for (Entity e : player.getNearbyEntities(6, 3, 6)) {
                            if (e instanceof LivingEntity target && target != player) {
                                if (plugin.getWorldGuardHook() == null || !(target instanceof Player pVic) || plugin.getWorldGuardHook().canPvp(player, pVic.getLocation())) {
                                    target.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 100, 0));
                                    target.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 100, 0));
                                }
                            }
                        }
                        if (level >= 5) {
                            Zombie ally = (Zombie) player.getWorld().spawnEntity(player.getLocation().add(1, 0, 0), EntityType.ZOMBIE);
                            ally.customName(ColorUtils.component("&2Esbirro de " + player.getName()));
                            Bukkit.getScheduler().runTaskLater(plugin, () -> { if (!ally.isDead()) ally.remove(); }, 300L);
                        }
                        ColorUtils.sendMessage(player, "&2&l[NUBE PÚTRIDA] &a¡Vapores pestilentes liberados!");
                    }
                } else {
                    ColorUtils.sendMessage(player, "&cRequieres Nivel 3 para Nube Pútrida.");
                }
            }

            case "elf" -> {
                if (level >= 3) {
                    if (checkCooldown(player, "RemolinoDeViento", 38)) {
                        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 160, 1));
                        Vector dir = player.getLocation().getDirection();
                        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1.2f, 1.3f);
                        for (double offset : new double[]{-0.2, 0.0, 0.2}) {
                            Vector spreadDir = dir.clone().add(new Vector(offset, 0, offset)).normalize();
                            Arrow arrow = player.launchProjectile(Arrow.class, spreadDir.multiply(2.1));
                            arrow.setShooter(player);
                            arrow.setDamage(arrow.getDamage() * 1.25);
                        }
                        for (Entity e : player.getNearbyEntities(5, 3, 5)) {
                            if (e instanceof LivingEntity target && target != player) {
                                target.setVelocity(target.getLocation().toVector().subtract(player.getLocation().toVector()).normalize().multiply(0.9).setY(0.35));
                            }
                        }
                        ColorUtils.sendMessage(player, "&a&l[REMOLINO DE VIENTO] &2¡Ráfaga silvana y empuje eólico!");
                    }
                } else {
                    ColorUtils.sendMessage(player, "&cRequieres Nivel 3 para Remolino de Viento.");
                }
            }

            default -> ColorUtils.sendMessage(player, "&7Tu raza no posee habilidades activas adicionales.");
        }
    }

    // =========================================================================
    // 9. MUERTE, FILACTERIA / ARCHILICH Y PROGRESIÓN DE PUNTOS DE TALENTO (FASE 9)
    // =========================================================================

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        if (data == null || !data.hasAssignedRace()) return;
        String raceId = data.getRace().toLowerCase();
        int level = data.getLevel();

        beastFormActive.remove(player.getUniqueId());
        stoneSkinActive.remove(player.getUniqueId());
        unyieldingFuryActive.remove(player.getUniqueId());

        if (raceId.equals("undead") && level >= 6 && checkCooldown(player, "ArchilichRevive", 3600)) {
            event.setKeepInventory(true);
            event.setKeepLevel(true);
            event.getDrops().clear();
            ColorUtils.sendMessage(player, "&2&l[ARCHILICH] &a¡Tu filacteria inmortal desafía a la muerte! Revives en combate.");
            player.getWorld().playSound(player.getLocation(), Sound.ITEM_TOTEM_USE, 1.0f, 0.8f);
            player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, player.getLocation(), 50, 0.5, 1.0, 0.5, 0.2);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    player.spigot().respawn();
                    player.setHealth(player.getMaxHealth() * 0.50);
                    player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 100, 2));
                }
            }, 2L);
            return;
        }

        // No-Muerto L4: Filacteria Portátil conserva 50% de los drops + XP
        if (raceId.equals("undead") && level >= 4 && checkCooldown(player, "FilacteriaPortatil", 1800)) {
            event.setKeepLevel(true);
            event.setDroppedExp(0);
            List<ItemStack> drops = event.getDrops();
            List<ItemStack> kept = new ArrayList<>();
            Iterator<ItemStack> it = drops.iterator();
            int idx = 0;
            while (it.hasNext()) {
                ItemStack st = it.next();
                if (idx % 2 == 0) {
                    kept.add(st.clone());
                    it.remove();
                }
                idx++;
            }
            event.getItemsToKeep().addAll(kept);
            ColorUtils.sendMessage(player, "&b&l[FILACTERIA PORTÁTIL] &7Conservas el 50% de tus pertenencias y toda tu XP.");
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityTarget(EntityTargetLivingEntityEvent event) {
        if (!(event.getTarget() instanceof Player player) || event.getEntity() == null) return;
        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        if (data == null || !data.hasAssignedRace()) return;

        String raceId = data.getRace().toLowerCase();
        EntityType entityType = event.getEntity().getType();

        if (raceId.equals("undead") && (entityType == EntityType.ZOMBIE || entityType == EntityType.SKELETON
                || entityType == EntityType.DROWNED || entityType == EntityType.HUSK
                || entityType == EntityType.STRAY || entityType == EntityType.WITHER_SKELETON)) {
            if (event.getReason() == EntityTargetLivingEntityEvent.TargetReason.CLOSEST_PLAYER
                    || event.getReason() == EntityTargetLivingEntityEvent.TargetReason.RANDOM_TARGET) {
                event.setCancelled(true);
            }
        } else if ((raceId.equals("werewolf") || raceId.equals("beastkin"))
                && (entityType == EntityType.WOLF || entityType == EntityType.FOX || entityType == EntityType.OCELOT)) {
            if (event.getReason() != EntityTargetLivingEntityEvent.TargetReason.TARGET_ATTACKED_ENTITY) {
                event.setCancelled(true);
            }
        } else if (raceId.equals("demon") && (entityType == EntityType.PIGLIN || entityType == EntityType.ZOMBIFIED_PIGLIN || entityType == EntityType.HOGLIN)) {
            if (event.getReason() != EntityTargetLivingEntityEvent.TargetReason.TARGET_ATTACKED_ENTITY) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityKill(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;
        PlayerData data = plugin.getPlayerDataManager().get(killer.getUniqueId());
        if (data == null || !data.hasAssignedRace()) return;

        data.incrementMobsDefeated();
        data.incrementMobKillsCounter();

        // Fase 9: Hito de bajas de mobs -> +1 Punto de Talento cada X kills
        int milestone = plugin.getConfigManager().getTalentKillsPerPoint();
        if (data.getMobKillsCounter() >= milestone) {
            data.setMobKillsCounter(data.getMobKillsCounter() - milestone);
            data.addTalentPoints(1);
            plugin.getPlayerDataManager().saveToFile(data);
            ColorUtils.sendMessage(killer, "&b&l[TALENTOS] &7¡Has derrotado &e" + milestone + " &7criaturas y ganado &b+1 Punto de Talento&7!");
            if (data.isSettingSounds()) {
                killer.playSound(killer.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.9f, 1.5f);
            }
        }

        // Fase 9: Probabilidad de caída de Fragmento de Talento Racial
        double shardChance = plugin.getConfigManager().getTalentShardDropChance();
        if (shardChance > 0 && Math.random() < shardChance) {
            ItemStack shard = plugin.getCustomItemManager().createCustomItem("talent_shard");
            if (shard != null) {
                event.getEntity().getWorld().dropItemNaturally(event.getEntity().getLocation(), shard);
            }
        }

        if (plugin.getQuestManager() != null) {
            plugin.getQuestManager().recordAction(killer, QuestManager.QuestType.KILL_MOBS, 1);
        }
    }
}

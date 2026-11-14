package com.cytricos.races.manager;

import com.cytricos.races.CytricosRaces;
import com.cytricos.races.model.PlayerData;
import com.cytricos.races.util.ColorUtils;
import org.bukkit.*;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.EquippableComponent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestor Centralizado de Alas Cosméticas 3D Exclusivas por Raza para CYTRICOS RPG.
 * - 16 pares de alas 100% exclusivas (1 por cada raza, cero alas compartidas).
 * - Validación estricta de raza al equipar y renderizar.
 * - Desequipado y destrucción inmediata al cambiar de raza.
 * - Registro estricto Map<UUID, UUID> + PDC "wing_owner" + barrido de 16 bloques anti-duplicación.
 * - Ocultación en 1ra persona vía ProtocolLib / Paper hideEntity preservando visibilidad multijugador y F5.
 *
 * Lead Developers: Dandan_13 & Wolfit12
 */
public class WingsManager {
    private final CytricosRaces plugin;
    private final NamespacedKey wingKey;
    private final NamespacedKey wingOwnerKey;
    private final Map<String, WingDefinition> registry = new LinkedHashMap<>();
    private final Map<UUID, UUID> primaryEntityMap = new ConcurrentHashMap<>();
    private final Map<UUID, WingRig> activeRigs = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitTask> playerTasks = new ConcurrentHashMap<>();
    private BukkitTask syncTask;
    private long animationTick = 0L;

    public enum WingRarity {
        COMUN("&7Común", "&7"),
        RARO("&9Raro", "&9"),
        EPICO("&5Épico", "&5"),
        LEGENDARIO("&6Legendario", "&6"),
        MITICO("&d&lMítico", "&d");

        public final String label;
        public final String colorCode;

        WingRarity(String label, String colorCode) {
            this.label = label;
            this.colorCode = colorCode;
        }
    }

    public enum WingState {
        IDLE("Reposo"),
        OPENING("Desplegando"),
        CLOSING("Replegando"),
        FLYING("Aleteo"),
        GLIDING("Planeo");

        public final String displayEs;

        WingState(String displayEs) {
            this.displayEs = displayEs;
        }
    }

    public static class WingDefinition {
        public final String id;
        public final String displayName;
        public final String description;
        public final int customModelData;
        public final int leftModelData;
        public final int rightModelData;
        public final Material iconMaterial;
        public final Particle trailParticle;
        public final String targetRace;
        public final String spanishRaceName;
        public final WingRarity rarity;
        public final int requiredLevel;
        public final double unlockCost;
        public final float scale;

        public WingDefinition(String id, String displayName, String description,
                              int customModelData, int leftModelData, int rightModelData,
                              Material iconMaterial, Particle trailParticle,
                              String targetRace, String spanishRaceName,
                              WingRarity rarity, int requiredLevel, double unlockCost, float scale) {
            this.id = id;
            this.displayName = displayName;
            this.description = description;
            this.customModelData = customModelData;
            this.leftModelData = leftModelData;
            this.rightModelData = rightModelData;
            this.iconMaterial = iconMaterial;
            this.trailParticle = trailParticle;
            this.targetRace = targetRace.toLowerCase();
            this.spanishRaceName = spanishRaceName;
            this.rarity = rarity;
            this.requiredLevel = requiredLevel;
            this.unlockCost = unlockCost;
            this.scale = scale;
        }
    }

    private static class WingRig {
        final String wingId;
        final ItemDisplay leftDisplay;
        final ItemDisplay rightDisplay;
        WingState currentState = WingState.OPENING;
        int transitionTicksLeft = 10;
        boolean wasAirborne = false;

        WingRig(String wingId, ItemDisplay leftDisplay, ItemDisplay rightDisplay) {
            this.wingId = wingId;
            this.leftDisplay = leftDisplay;
            this.rightDisplay = rightDisplay;
        }

        boolean isValid() {
            return leftDisplay != null && !leftDisplay.isDead()
                    && rightDisplay != null && !rightDisplay.isDead();
        }
    }

    public WingsManager(CytricosRaces plugin) {
        this.plugin = plugin;
        this.wingKey = new NamespacedKey(plugin, "cytricos_wing_id");
        this.wingOwnerKey = new NamespacedKey(plugin, "wing_owner");
        initExclusiveWings();
    }

    private void initExclusiveWings() {
        register(new WingDefinition("angel_wings", "&f&lAlas de Ángel",
                "Plumas radiantes forjadas con luz celestial exclusiva de Ángel.",
                20001, 20101, 20201, Material.FEATHER, Particle.END_ROD,
                "angel", "Ángel", WingRarity.EPICO, 1, 0.0, 1.18f));

        register(new WingDefinition("demon_wings", "&c&lAlas de Demonio",
                "Alas membranosas negras con vetas ardientes del Averno.",
                20002, 20102, 20202, Material.BLAZE_POWDER, Particle.FLAME,
                "demon", "Demonio", WingRarity.EPICO, 1, 0.0, 1.20f));

        register(new WingDefinition("vampire_wings", "&4&lAlas de Vampiro",
                "Membranas carmesí nocturnas exclusivas de Vampiro.",
                20003, 20103, 20203, Material.REDSTONE, Particle.CRIMSON_SPORE,
                "vampire", "Vampiro", WingRarity.RARO, 1, 0.0, 1.14f));

        register(new WingDefinition("dragonkin_wings", "&5&lAlas de Draconiano",
                "Envergadura escamosa dracónica con espinas de amatista.",
                20004, 20104, 20204, Material.DRAGON_BREATH, Particle.DRAGON_BREATH,
                "dragonkin", "Draconiano", WingRarity.LEGENDARIO, 1, 0.0, 1.32f));

        register(new WingDefinition("fairy_wings", "&d&lAlas de Hada",
                "Alas translúcidas iridiscentes de cuatro pétalos feéricos.",
                20005, 20105, 20205, Material.PRISMARINE_CRYSTALS, Particle.CHERRY_LEAVES,
                "fairy", "Hada", WingRarity.RARO, 1, 0.0, 1.06f));

        register(new WingDefinition("undead_wings", "&8&lAlas de No-Muerto",
                "Sombras óseas y bruma espectral de la cripta.",
                20007, 20107, 20207, Material.BONE, Particle.ASH,
                "undead", "No-Muerto", WingRarity.RARO, 1, 0.0, 1.12f));

        register(new WingDefinition("yokai_wings", "&5&lAlas de Yokai",
                "Llamaradas espirituales flotantes entre dimensiones.",
                20010, 20110, 20210, Material.ECHO_SHARD, Particle.SOUL_FIRE_FLAME,
                "yokai", "Yokai", WingRarity.MITICO, 2, 4000.0, 1.22f));

        register(new WingDefinition("druid_wings", "&a&lAlas de Druida",
                "Hojas esmeralda y lianas vivas del bosque ancestral.",
                20011, 20111, 20211, Material.SPORE_BLOSSOM, Particle.HAPPY_VILLAGER,
                "druid", "Druida", WingRarity.EPICO, 1, 0.0, 1.14f));

        register(new WingDefinition("dwarf_wings", "&6&lAlas de Enano",
                "Estandarte mecánico a vapor forjado con engranajes de mithril.",
                20012, 20112, 20212, Material.RAW_GOLD, Particle.CRIT,
                "dwarf", "Enano", WingRarity.EPICO, 2, 3500.0, 1.10f));

        register(new WingDefinition("human_wings", "&b&lAlas de Humano",
                "Manto estelar de vanguardia tejido con runas tácticas.",
                20013, 20113, 20213, Material.NETHER_STAR, Particle.ENCHANTED_HIT,
                "human", "Humano", WingRarity.RARO, 1, 0.0, 1.12f));

        register(new WingDefinition("elf_wings", "&a&lAlas de Elfo",
                "Plumaje de viento silvano ligero con destellos esmeralda.",
                20014, 20114, 20214, Material.BOW, Particle.GLOW,
                "elf", "Elfo", WingRarity.EPICO, 2, 3000.0, 1.14f));

        register(new WingDefinition("giant_wings", "&7&lAlas de Gigante",
                "Placas monolíticas de piedra rúnica flotantes en la espalda.",
                20015, 20115, 20215, Material.DEEPSLATE, Particle.CLOUD,
                "giant", "Gigante", WingRarity.LEGENDARIO, 3, 6000.0, 1.38f));

        register(new WingDefinition("orc_wings", "&2&lAlas de Orco",
                "Trofeos bélicos de guerra con estandartes carmesí y colmillos.",
                20016, 20116, 20216, Material.IRON_AXE, Particle.SMOKE,
                "orc", "Orco", WingRarity.EPICO, 2, 3500.0, 1.24f));

        register(new WingDefinition("goblin_wings", "&e&lAlas de Duende",
                "Planeador improvisado de chatarra dorada y pólvora ingeniosa.",
                20017, 20117, 20217, Material.GOLD_INGOT, Particle.ELECTRIC_SPARK,
                "goblin", "Duende", WingRarity.RARO, 1, 0.0, 1.04f));

        register(new WingDefinition("beastkin_wings", "&6&lAlas de Bestia",
                "Plumaje de grifo salvaje entrelazado con zarpas totémicas.",
                20018, 20118, 20218, Material.RABBIT_HIDE, Particle.SWEEP_ATTACK,
                "beastkin", "Bestia", WingRarity.EPICO, 2, 3500.0, 1.16f));

        register(new WingDefinition("werewolf_wings", "&8&lAlas de Hombre Lobo",
                "Espinas lunares argentadas cargadas con furia de manada.",
                20019, 20119, 20219, Material.BONE_MEAL, Particle.WAX_OFF,
                "werewolf", "Hombre Lobo", WingRarity.EPICO, 2, 4000.0, 1.20f));
    }

    private void register(WingDefinition def) {
        registry.put(def.id.toLowerCase(), def);
    }

    public Collection<WingDefinition> getAllWings() {
        return Collections.unmodifiableCollection(registry.values());
    }

    public WingDefinition getWing(String id) {
        if (id == null) return null;
        String clean = id.toLowerCase();
        if (registry.containsKey(clean)) {
            return registry.get(clean);
        }
        return switch (clean) {
            case "dragon_wings" -> registry.get("dragonkin_wings");
            case "nature_wings" -> registry.get("druid_wings");
            case "dark_wings", "void_wings" -> registry.get("undead_wings");
            case "phoenix_wings" -> registry.get("demon_wings");
            case "celestial_wings", "dark_angel_wings" -> registry.get("angel_wings");
            default -> null;
        };
    }

    public WingDefinition getWingForRace(String raceId) {
        if (raceId == null) return null;
        for (WingDefinition def : registry.values()) {
            if (def.targetRace.equalsIgnoreCase(raceId)) {
                return def;
            }
        }
        return null;
    }

    public boolean isRaceExclusiveMatch(PlayerData data, WingDefinition def) {
        if (data == null || def == null || !data.hasAssignedRace()) return false;
        return def.targetRace.equalsIgnoreCase(data.getRace());
    }

    public boolean isWingUnlocked(Player player, PlayerData data, WingDefinition def) {
        if (def == null) return false;
        if (!isRaceExclusiveMatch(data, def)) {
            return false;
        }
        if (player != null && (player.hasPermission("cytricosraces.admin")
                || player.hasPermission("cytricosraces.wings." + def.id)
                || player.hasPermission("cytricosraces.wings.*"))) {
            return true;
        }
        if (def.requiredLevel <= 1 && def.unlockCost <= 0.0) {
            return true;
        }
        if (data.hasUnlockedCosmetic("wing:" + def.id) || def.id.equalsIgnoreCase(data.getSelectedWings())) {
            return true;
        }
        return data.getLevel() >= def.requiredLevel;
    }

    public boolean tryUnlockWithVault(Player player, PlayerData data, WingDefinition def) {
        if (player == null || data == null || def == null) return false;
        if (!isRaceExclusiveMatch(data, def)) {
            ColorUtils.sendMessage(player, "&c&l[ALAS] &7Estas alas pertenecen exclusivamente a la raza &e" + def.spanishRaceName + "&7.");
            return false;
        }
        if (isWingUnlocked(player, data, def)) return true;

        if (!plugin.getConfig().getBoolean("cosmetics_settings.wings.allow_vault_unlock", true)) {
            ColorUtils.sendMessage(player, "&cRequiere &eNivel " + def.requiredLevel + "+ &cpara desbloquear estas alas.");
            return false;
        }
        if (!plugin.isVaultAvailable() || plugin.getVaultEconomy() == null || def.unlockCost <= 0.0) {
            ColorUtils.sendMessage(player, "&cRequiere &eNivel " + def.requiredLevel + "+ &cpara desbloquear " + def.displayName + "&c.");
            return false;
        }

        double balance = plugin.getVaultEconomy().getBalance(player);
        if (balance < def.unlockCost) {
            ColorUtils.sendMessage(player, "&cNecesitas &e$" + String.format("%.0f", def.unlockCost)
                    + " &co alcanzar el &eNivel " + def.requiredLevel + " &cpara desbloquear " + def.displayName + "&c.");
            return false;
        }

        plugin.getVaultEconomy().withdrawPlayer(player, def.unlockCost);
        data.unlockCosmetic("wing:" + def.id);
        plugin.getPlayerDataManager().saveToFile(data);
        ColorUtils.sendMessage(player, "&a&l[COSMÉTICOS] &7¡Has desbloqueado " + def.displayName + " &7por &e$" + String.format("%.0f", def.unlockCost) + "&7!");
        return true;
    }

    public ItemStack createWingElytraItem(String wingId) {
        WingDefinition def = getWing(wingId);
        if (def == null) return null;

        ItemStack item = new ItemStack(Material.ELYTRA);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(ColorUtils.component(def.displayName));
            meta.setCustomModelData(def.customModelData);
            try {
                EquippableComponent eq = meta.getEquippable();
                eq.setSlot(EquipmentSlot.CHEST);
                eq.setModel(new NamespacedKey("cytricos", def.id));
                meta.setEquippable(eq);
            } catch (Throwable ignored) {
            }

            meta.lore(ColorUtils.componentList(List.of(
                    "&7" + def.description,
                    "&7Rareza: " + def.rarity.label + " &8| &7Exclusiva de: &e" + def.spanishRaceName,
                    "",
                    "&8Modelo Pack: &fcytricos:" + def.id + " &7(CMD " + def.customModelData + ")",
                    "&6&lCosmético Exclusivo CYTRICOS RPG"
            )));
            meta.getPersistentDataContainer().set(wingKey, PersistentDataType.STRING, def.id);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(meta);
        }
        return item;
    }

    public boolean isWingItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(wingKey, PersistentDataType.STRING);
    }

    public String getWingIdFromItem(ItemStack item) {
        if (!isWingItem(item)) return null;
        return item.getItemMeta().getPersistentDataContainer().get(wingKey, PersistentDataType.STRING);
    }

    private ItemStack createWingPartStack(int customModelData) {
        ItemStack stack = new ItemStack(Material.FEATHER);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setCustomModelData(customModelData);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    public void unequipOnRaceChange(Player player) {
        if (player == null) return;
        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        if (data != null) {
            data.setSelectedWings("none");
            plugin.getPlayerDataManager().saveToFile(data);
        }
        cleanupPlayerWings(player);
    }

    public boolean equipCosmeticWings(Player player, String wingId) {
        cleanupPlayerWings(player);

        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        if (data == null) return false;

        if (wingId == null || wingId.equalsIgnoreCase("none")) {
            data.setSelectedWings("none");
            plugin.getPlayerDataManager().saveToFile(data);
            return true;
        }

        WingDefinition def = getWing(wingId);
        if (def == null) return false;

        if (!isRaceExclusiveMatch(data, def)) {
            ColorUtils.sendMessage(player, "&c&l[ALAS] &7¡No puedes equipar " + def.displayName
                    + "&7! Son exclusivas de la raza &e" + def.spanishRaceName + "&7.");
            data.setSelectedWings("none");
            plugin.getPlayerDataManager().saveToFile(data);
            return false;
        }

        data.setSelectedWings(def.id);
        data.unlockCosmetic("wing:" + def.id);
        plugin.getPlayerDataManager().saveToFile(data);

        if (plugin.getConfig().getBoolean("cosmetics_settings.wings.enabled", true)
                && plugin.getConfig().getBoolean("cosmetics_settings.wings.use_display_entities", true)) {
            spawnWingDisplay(player, def);
        }
        return true;
    }

    public void spawnWingDisplay(Player player, WingDefinition def) {
        if (player == null || !player.isOnline() || player.isDead() || def == null) return;

        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        if (data == null || !isRaceExclusiveMatch(data, def)) {
            cleanupPlayerWings(player);
            if (data != null && !"none".equalsIgnoreCase(data.getSelectedWings())) {
                data.setSelectedWings("none");
                plugin.getPlayerDataManager().saveToFile(data);
            }
            return;
        }

        // Protocolo Obligatorio Anti-Duplicación previo al spawn
        cleanupPlayerWings(player);

        if (player.getGameMode() == GameMode.SPECTATOR || player.isInvisible()) {
            return;
        }

        int interpTicks = Math.max(1, plugin.getConfig().getInt("cosmetics_settings.wings.animation_period_ticks", 2));
        try {
            Location spawnLoc = player.getLocation();
            ItemDisplay leftDisplay = spawnHingeEntity(player, spawnLoc, def.leftModelData, interpTicks);
            ItemDisplay rightDisplay = spawnHingeEntity(player, spawnLoc, def.rightModelData, interpTicks);

            player.addPassenger(leftDisplay);
            player.addPassenger(rightDisplay);

            primaryEntityMap.put(player.getUniqueId(), leftDisplay.getUniqueId());
            WingRig rig = new WingRig(def.id, leftDisplay, rightDisplay);
            rig.currentState = WingState.OPENING;
            rig.transitionTicksLeft = 10;
            activeRigs.put(player.getUniqueId(), rig);

            applyFirstPersonVisibility(player, data, rig);
            applyRigPose(player, def, rig, interpTicks);
        } catch (Throwable t) {
            plugin.getLogger().warning("No se pudo generar rig ItemDisplay para alas de " + player.getName() + ": " + t.getMessage());
        }
    }

    private ItemDisplay spawnHingeEntity(Player owner, Location loc, int modelData, int interpTicks) {
        return owner.getWorld().spawn(loc, ItemDisplay.class, entity -> {
            entity.setPersistent(false);
            entity.setItemStack(createWingPartStack(modelData));
            entity.setBillboard(Display.Billboard.FIXED);
            entity.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
            entity.setInterpolationDelay(0);
            entity.setInterpolationDuration(interpTicks);
            entity.setTeleportDuration(1);
            entity.setViewRange(0.95f);
            entity.getPersistentDataContainer().set(wingOwnerKey, PersistentDataType.STRING, owner.getUniqueId().toString());
        });
    }

    public void applyFirstPersonVisibility(Player player, PlayerData data, WingRig rig) {
        if (player == null || rig == null) return;
        boolean hideFirstPerson = data == null || data.isFirstPersonWingsHidden();

        if (plugin.getProtocolLibHook() != null) {
            if (rig.leftDisplay != null) {
                if (hideFirstPerson) plugin.getProtocolLibHook().registerTrackedEntity(rig.leftDisplay.getEntityId(), player.getUniqueId());
                else plugin.getProtocolLibHook().unregisterTrackedEntity(rig.leftDisplay.getEntityId());
            }
            if (rig.rightDisplay != null) {
                if (hideFirstPerson) plugin.getProtocolLibHook().registerTrackedEntity(rig.rightDisplay.getEntityId(), player.getUniqueId());
                else plugin.getProtocolLibHook().unregisterTrackedEntity(rig.rightDisplay.getEntityId());
            }
        }

        if (hideFirstPerson) {
            if (rig.leftDisplay != null) player.hideEntity(plugin, rig.leftDisplay);
            if (rig.rightDisplay != null) player.hideEntity(plugin, rig.rightDisplay);
        } else {
            if (rig.leftDisplay != null) player.showEntity(plugin, rig.leftDisplay);
            if (rig.rightDisplay != null) player.showEntity(plugin, rig.rightDisplay);
        }
    }

    public boolean toggleFirstPersonVisibility(Player player) {
        if (player == null) return true;
        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        if (data == null) return true;
        boolean next = !data.isFirstPersonWingsHidden();
        data.setFirstPersonWingsHidden(next);
        plugin.getPlayerDataManager().saveToFile(data);
        WingRig rig = activeRigs.get(player.getUniqueId());
        if (rig != null && rig.isValid()) {
            applyFirstPersonVisibility(player, data, rig);
        }
        return next;
    }

    /**
     * Protocolo estricto de limpieza pre-spawn y eventos de desconexión/muerte/cambio de mundo/teletransporte:
     * 1. Cancela cualquier BukkitTask por jugador.
     * 2. Elimina entidades registradas en el mapa Map<UUID, UUID> y WingRig.
     * 3. Escanea 16x16x16 bloques alrededor y elimina cualquier ItemDisplay huérfano con wing_owner == UUID.
     */
    public void cleanupPlayerWings(Player player) {
        if (player == null) return;
        UUID uuid = player.getUniqueId();
        String ownerStr = uuid.toString();

        BukkitTask pTask = playerTasks.remove(uuid);
        if (pTask != null) {
            pTask.cancel();
        }

        if (plugin.getProtocolLibHook() != null) {
            plugin.getProtocolLibHook().unregisterOwner(uuid);
        }

        UUID registeredId = primaryEntityMap.remove(uuid);
        if (registeredId != null) {
            Entity regEnt = Bukkit.getEntity(registeredId);
            if (regEnt != null && !regEnt.isDead()) {
                regEnt.remove();
            }
        }

        WingRig existing = activeRigs.remove(uuid);
        if (existing != null) {
            despawnRig(existing);
        }

        for (Entity passenger : new ArrayList<>(player.getPassengers())) {
            if (passenger instanceof ItemDisplay id && (
                    ownerStr.equals(id.getPersistentDataContainer().get(wingOwnerKey, PersistentDataType.STRING))
                            || !id.isPersistent()
            )) {
                id.remove();
            }
        }

        for (Entity nearby : player.getNearbyEntities(16.0, 16.0, 16.0)) {
            if (nearby instanceof ItemDisplay id) {
                String tagOwner = id.getPersistentDataContainer().get(wingOwnerKey, PersistentDataType.STRING);
                if (ownerStr.equals(tagOwner)) {
                    id.remove();
                }
            }
        }
    }

    public void removeWingDisplay(Player player) {
        cleanupPlayerWings(player);
    }

    public void refreshPlayerWings(Player player) {
        if (player == null || !player.isOnline()) return;
        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        if (data == null || !data.hasAssignedRace()) {
            cleanupPlayerWings(player);
            return;
        }
        String wingId = data.getSelectedWings();
        if (wingId != null && !wingId.equalsIgnoreCase("none")
                && plugin.getConfig().getBoolean("cosmetics_settings.wings.enabled", true)
                && plugin.getConfig().getBoolean("cosmetics_settings.wings.use_display_entities", true)) {
            WingDefinition def = getWing(wingId);
            if (def != null && isRaceExclusiveMatch(data, def)) {
                spawnWingDisplay(player, def);
                return;
            } else if (def != null) {
                data.setSelectedWings("none");
                plugin.getPlayerDataManager().saveToFile(data);
            }
        }
        cleanupPlayerWings(player);
    }

    public void startSyncTask() {
        stopSyncTask();
        purgeOrphanedWingEntities();
        if (!plugin.getConfig().getBoolean("cosmetics_settings.wings.enabled", true)) return;

        int period = Math.max(1, plugin.getConfig().getInt("cosmetics_settings.wings.animation_period_ticks", 2));
        syncTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            animationTick += period;
            boolean globalParticles = plugin.getConfig().getBoolean("cosmetics_settings.wings.particles_enabled", true);

            Iterator<Map.Entry<UUID, WingRig>> it = activeRigs.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<UUID, WingRig> entry = it.next();
                Player player = Bukkit.getPlayer(entry.getKey());
                WingRig rig = entry.getValue();

                if (player == null || !player.isOnline() || player.isDead() || rig == null || !rig.isValid()) {
                    despawnRig(rig);
                    primaryEntityMap.remove(entry.getKey());
                    it.remove();
                    continue;
                }

                PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
                WingDefinition def = getWing(rig.wingId);
                if (def == null || !isRaceExclusiveMatch(data, def)
                        || player.isInvisible() || player.getGameMode() == GameMode.SPECTATOR) {
                    despawnRig(rig);
                    primaryEntityMap.remove(entry.getKey());
                    it.remove();
                    continue;
                }

                if (!ensureMounted(player, rig.leftDisplay) || !ensureMounted(player, rig.rightDisplay)) {
                    despawnRig(rig);
                    primaryEntityMap.remove(entry.getKey());
                    it.remove();
                    continue;
                }

                updateRigState(player, rig, period);
                applyRigPose(player, def, rig, period);

                if (globalParticles && data.isSettingParticles() && def.trailParticle != null) {
                    double chance = (rig.currentState == WingState.FLYING || rig.currentState == WingState.GLIDING) ? 0.55 : 0.16;
                    if (Math.random() < chance) {
                        spawnWingTipParticles(player, def.trailParticle);
                    }
                }
            }
        }, 6L, period);
    }

    private boolean ensureMounted(Player player, ItemDisplay display) {
        if (!display.getWorld().equals(player.getWorld())) {
            return false;
        }
        if (!player.getPassengers().contains(display)) {
            return player.addPassenger(display);
        }
        return true;
    }

    private void updateRigState(Player player, WingRig rig, int elapsedTicks) {
        boolean isGliding = player.isGliding();
        boolean isFlying = player.isFlying();
        boolean isAirborne = isGliding || isFlying || (!player.isOnGround() && player.getVelocity().getY() < -0.15);

        if (isGliding) {
            rig.currentState = WingState.GLIDING;
            rig.wasAirborne = true;
            rig.transitionTicksLeft = 0;
            return;
        }

        if (isFlying || (isAirborne && player.getVelocity().lengthSquared() > 0.03)) {
            if (!rig.wasAirborne && rig.currentState != WingState.OPENING) {
                rig.currentState = WingState.OPENING;
                rig.transitionTicksLeft = 6;
            } else if (rig.transitionTicksLeft > 0) {
                rig.transitionTicksLeft = Math.max(0, rig.transitionTicksLeft - elapsedTicks);
            } else {
                rig.currentState = WingState.FLYING;
            }
            rig.wasAirborne = true;
            return;
        }

        if (rig.wasAirborne) {
            rig.wasAirborne = false;
            rig.currentState = WingState.CLOSING;
            rig.transitionTicksLeft = 10;
            return;
        }

        if (rig.transitionTicksLeft > 0) {
            rig.transitionTicksLeft = Math.max(0, rig.transitionTicksLeft - elapsedTicks);
        } else {
            rig.currentState = WingState.IDLE;
        }
    }

    private void applyRigPose(Player player, WingDefinition def, WingRig rig, int interpTicks) {
        float bodyYaw = player.getLocation().getYaw();
        float bodyYawRad = (float) Math.toRadians(-bodyYaw);
        double timeSec = animationTick / 20.0;

        float flapDeg;
        float pitchDeg;
        float rollDeg;
        float sideSep = 0.14f;
        float backOffset = -0.22f;
        float verticalOffset = player.isSneaking() ? -0.44f : -0.26f;

        switch (rig.currentState) {
            case OPENING -> {
                float progress = 1.0f - Math.min(1.0f, rig.transitionTicksLeft / 10.0f);
                flapDeg = 68.0f - (50.0f * progress);
                pitchDeg = -8.0f * (1.0f - progress);
                rollDeg = 8.0f * progress;
            }
            case CLOSING -> {
                float progress = 1.0f - Math.min(1.0f, rig.transitionTicksLeft / 10.0f);
                flapDeg = 22.0f + (36.0f * progress);
                pitchDeg = 6.0f * progress;
                rollDeg = -4.0f * progress;
            }
            case FLYING -> {
                double beat = Math.sin(timeSec * 8.5);
                double secondary = Math.cos(timeSec * 8.5);
                flapDeg = (float) (32.0 + 24.0 * beat);
                pitchDeg = (float) (12.0 + 10.0 * secondary);
                rollDeg = (float) (8.0 * beat);
            }
            case GLIDING -> {
                double shimmer = Math.sin(timeSec * 5.0);
                flapDeg = (float) (40.0 + 6.0 * shimmer);
                pitchDeg = 32.0f;
                rollDeg = (float) (-6.0 + 3.0 * shimmer);
                verticalOffset = -0.35f;
                backOffset = -0.28f;
            }
            default -> {
                double sway = Math.sin(timeSec * 2.4);
                boolean moving = player.getVelocity().setY(0).lengthSquared() > 0.008;
                double amp = moving ? 6.5 : 3.8;
                flapDeg = (float) (28.0 + amp * sway);
                pitchDeg = (float) (4.0 + 2.5 * Math.cos(timeSec * 2.4));
                rollDeg = (float) (2.5 * sway);
            }
        }

        float flapRad = (float) Math.toRadians(flapDeg);
        float pitchRad = (float) Math.toRadians(pitchDeg);
        float rollRad = (float) Math.toRadians(rollDeg);

        Quaternionf bodyQuat = new Quaternionf().rotateY(bodyYawRad);
        Vector3f scale = new Vector3f(def.scale, def.scale, def.scale);

        Vector3f leftTrans = bodyQuat.transform(new Vector3f(sideSep, verticalOffset, backOffset), new Vector3f());
        Quaternionf leftLocal = new Quaternionf()
                .rotateY(+flapRad)
                .rotateX(pitchRad)
                .rotateZ(+rollRad);
        rig.leftDisplay.setInterpolationDelay(0);
        rig.leftDisplay.setInterpolationDuration(interpTicks);
        rig.leftDisplay.setTransformation(new Transformation(leftTrans, bodyQuat, scale, leftLocal));

        Vector3f rightTrans = bodyQuat.transform(new Vector3f(-sideSep, verticalOffset, backOffset), new Vector3f());
        Quaternionf rightLocal = new Quaternionf()
                .rotateY(-flapRad)
                .rotateX(pitchRad)
                .rotateZ(-rollRad);
        rig.rightDisplay.setInterpolationDelay(0);
        rig.rightDisplay.setInterpolationDuration(interpTicks);
        rig.rightDisplay.setTransformation(new Transformation(rightTrans, bodyQuat, scale, rightLocal));
    }

    private void spawnWingTipParticles(Player player, Particle particle) {
        Location base = player.getLocation().add(0, 1.15, 0);
        double rad = Math.toRadians(base.getYaw());
        double backX = Math.sin(rad) * 0.45;
        double backZ = -Math.cos(rad) * 0.45;
        double side = (Math.random() < 0.5 ? 1.0 : -1.0) * 0.68;
        double sideX = Math.cos(rad) * side;
        double sideZ = Math.sin(rad) * side;

        Location tip = base.add(backX + sideX, 0.15 + Math.random() * 0.35, backZ + sideZ);
        if (particle == Particle.LAVA) {
            player.getWorld().spawnParticle(Particle.FLAME, tip, 1, 0.08, 0.08, 0.08, 0.01);
        } else {
            player.getWorld().spawnParticle(particle, tip, 1, 0.08, 0.08, 0.08, 0.01);
        }
    }

    private void despawnRig(WingRig rig) {
        if (rig == null) return;
        if (rig.leftDisplay != null && !rig.leftDisplay.isDead()) {
            rig.leftDisplay.remove();
        }
        if (rig.rightDisplay != null && !rig.rightDisplay.isDead()) {
            rig.rightDisplay.remove();
        }
    }

    private void purgeOrphanedWingEntities() {
        for (World world : Bukkit.getWorlds()) {
            for (ItemDisplay display : world.getEntitiesByClass(ItemDisplay.class)) {
                if (display.getPersistentDataContainer().has(wingOwnerKey, PersistentDataType.STRING)) {
                    display.remove();
                }
            }
        }
    }

    public void stopSyncTask() {
        if (syncTask != null) {
            syncTask.cancel();
            syncTask = null;
        }
        for (BukkitTask task : playerTasks.values()) {
            task.cancel();
        }
        playerTasks.clear();
        cleanupAllDisplays();
    }

    public void cleanupAllDisplays() {
        for (WingRig rig : activeRigs.values()) {
            despawnRig(rig);
        }
        activeRigs.clear();
        primaryEntityMap.clear();
        purgeOrphanedWingEntities();
    }
}

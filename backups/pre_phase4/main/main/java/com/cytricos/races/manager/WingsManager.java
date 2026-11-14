package com.cytricos.races.manager;

import com.cytricos.races.CytricosRaces;
import com.cytricos.races.model.PlayerData;
import com.cytricos.races.util.ColorUtils;
import org.bukkit.*;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestor Independiente de Alas Cosméticas 3D para CYTRICOS RPG (v7.0).
 * 100% gratuito e independiente (NO requiere ItemsAdder, Oraxen ni mods cliente).
 *
 * Soporta dos mecanismos nativos compatibles con Paper 26.2:
 * 1) Ítems de Élitros Personalizados (ELYTRA con CustomModelData y modelo Blockbench del Resource Pack).
 * 2) Entidades ligeras ItemDisplay (1 sola entidad pasajera no persistente por jugador activo),
 *    sincronizadas con el ángulo corporal del jugador y limpiadas ante cualquier cambio de estado.
 *
 * Limitación técnica documentada: Las animaciones esqueléticas de aleteo por hueso
 * requieren shaders/mods de cliente; este sistema muestra modelos 3D estáticos de alta fidelidad
 * con estelas de partículas suaves y rotación corporal sincronizada.
 *
 * Lead Developers: Dandan_13 & Wolfit12
 */
public class WingsManager {
    private final CytricosRaces plugin;
    private final NamespacedKey wingKey;
    private final Map<String, WingDefinition> registry = new LinkedHashMap<>();
    private final Map<UUID, ItemDisplay> activeDisplays = new ConcurrentHashMap<>();
    private BukkitTask syncTask;

    public static class WingDefinition {
        public final String id;
        public final String displayName;
        public final String description;
        public final int customModelData;
        public final Material iconMaterial;
        public final Particle trailParticle;
        public final String recommendedRace;

        public WingDefinition(String id, String displayName, String description, int customModelData,
                              Material iconMaterial, Particle trailParticle, String recommendedRace) {
            this.id = id;
            this.displayName = displayName;
            this.description = description;
            this.customModelData = customModelData;
            this.iconMaterial = iconMaterial;
            this.trailParticle = trailParticle;
            this.recommendedRace = recommendedRace;
        }
    }

    public WingsManager(CytricosRaces plugin) {
        this.plugin = plugin;
        this.wingKey = new NamespacedKey(plugin, "cytricos_wing_id");
        initWings();
    }

    private void initWings() {
        register(new WingDefinition("angel_wings", "&f&lAlas de Ángel",
                "Plumas radiantes forjadas con luz divina.", 20001, Material.FEATHER, Particle.END_ROD, "angel"));
        register(new WingDefinition("demon_wings", "&c&lAlas de Demonio",
                "Alas membranosas negras con vetas de magma.", 20002, Material.BLAZE_POWDER, Particle.FLAME, "demon"));
        register(new WingDefinition("vampire_wings", "&4&lAlas de Vampiro",
                "Membranas carmesí de señor nocturno.", 20003, Material.REDSTONE, Particle.CRIMSON_SPORE, "vampire"));
        register(new WingDefinition("dragon_wings", "&5&lAlas de Dragón",
                "Envergadura escamosa de dragón ancestral.", 20004, Material.DRAGON_BREATH, Particle.DRAGON_BREATH, "dragonkin"));
        register(new WingDefinition("fairy_wings", "&d&lAlas de Hada",
                "Alas translúcidas iridiscentes con brillo feérico.", 20005, Material.PRISMARINE_CRYSTALS, Particle.CHERRY_LEAVES, "fairy"));
        register(new WingDefinition("phoenix_wings", "&6&lAlas de Fénix",
                "Plumaje ígneo renacido de las cenizas.", 20006, Material.FIRE_CHARGE, Particle.LAVA, "phoenix"));
        register(new WingDefinition("dark_wings", "&8&lAlas Oscuras",
                "Sombras etéreas tejidas con energía del vacío.", 20007, Material.COAL, Particle.SMOKE, "undead"));
        register(new WingDefinition("celestial_wings", "&b&lAlas Celestiales",
                "Alas astrales con destellos de constelaciones.", 20008, Material.NETHER_STAR, Particle.GLOW, "angel"));
    }

    private void register(WingDefinition def) {
        registry.put(def.id.toLowerCase(), def);
    }

    public Collection<WingDefinition> getAllWings() {
        return Collections.unmodifiableCollection(registry.values());
    }

    public WingDefinition getWing(String id) {
        if (id == null) return null;
        return registry.get(id.toLowerCase());
    }

    /**
     * Construye un objeto ELYTRA cosmético con el modelo personalizado del Resource Pack y clave PDC.
     */
    public ItemStack createWingElytraItem(String wingId) {
        WingDefinition def = getWing(wingId);
        if (def == null) return null;

        ItemStack item = new ItemStack(Material.ELYTRA);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(ColorUtils.component(def.displayName));
            meta.setCustomModelData(def.customModelData);
            meta.lore(ColorUtils.componentList(List.of(
                    "&7" + def.description,
                    "",
                    "&8ID Modelo: &fcytricos:" + def.id + " &7(CMD " + def.customModelData + ")",
                    "&6&lCosmético Independiente CYTRICOS RPG",
                    "&7Equípalas en el pecho para planear con estilo."
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

    /**
     * Construye el ItemStack visual utilizado por la entidad ItemDisplay (Material FEATHER + CMD).
     */
    private ItemStack createDisplayModelStack(WingDefinition def) {
        ItemStack stack = new ItemStack(Material.FEATHER);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setCustomModelData(def.customModelData);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    public void equipCosmeticWings(Player player, String wingId) {
        removeWingDisplay(player);

        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        if (data == null) return;

        if (wingId == null || wingId.equalsIgnoreCase("none")) {
            data.setSelectedWings("none");
            plugin.getPlayerDataManager().saveToFile(data);
            return;
        }

        WingDefinition def = getWing(wingId);
        if (def == null) return;

        data.setSelectedWings(def.id);
        plugin.getPlayerDataManager().saveToFile(data);

        if (!plugin.getConfig().getBoolean("cosmetics_settings.wings.enabled", true)) {
            return;
        }
        if (!plugin.getConfig().getBoolean("cosmetics_settings.wings.use_display_entities", true)) {
            return;
        }

        spawnWingDisplay(player, def);
    }

    public void spawnWingDisplay(Player player, WingDefinition def) {
        if (player == null || !player.isOnline() || player.isDead() || def == null) return;
        removeWingDisplay(player);

        if (player.getGameMode() == GameMode.SPECTATOR || player.isInvisible()) {
            return;
        }

        try {
            Location spawnLoc = player.getLocation();
            ItemDisplay display = player.getWorld().spawn(spawnLoc, ItemDisplay.class, entity -> {
                entity.setPersistent(false);
                entity.setItemStack(createDisplayModelStack(def));
                entity.setBillboard(Display.Billboard.FIXED);
                entity.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
                entity.setInterpolationDuration(2);
                entity.setTeleportDuration(1);
                entity.setViewRange(0.75f);

                // Posicionar detrás de la espalda del jugador con rotación inicial
                float yawRad = (float) Math.toRadians(-player.getLocation().getYaw() + 180.0f);
                Quaternionf rot = new Quaternionf().rotateY(yawRad);
                Vector3f translation = new Vector3f(0f, -0.35f, 0f);
                Vector3f scale = new Vector3f(1.25f, 1.25f, 1.25f);
                entity.setTransformation(new Transformation(translation, rot, scale, new Quaternionf()));
            });

            player.addPassenger(display);
            activeDisplays.put(player.getUniqueId(), display);
        } catch (Throwable t) {
            plugin.getLogger().warning("No se pudo generar ItemDisplay para alas de " + player.getName() + ": " + t.getMessage());
        }
    }

    public void removeWingDisplay(Player player) {
        if (player == null) return;
        ItemDisplay existing = activeDisplays.remove(player.getUniqueId());
        if (existing != null && !existing.isDead()) {
            existing.remove();
        }
        // Seguridad adicional: limpiar cualquier ItemDisplay pasajero huérfano del plugin
        for (Entity passenger : player.getPassengers()) {
            if (passenger instanceof ItemDisplay && !passenger.isPersistent()) {
                passenger.remove();
            }
        }
    }

    public void refreshPlayerWings(Player player) {
        if (player == null || !player.isOnline()) return;
        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        if (data == null) return;
        String wingId = data.getSelectedWings();
        if (wingId != null && !wingId.equalsIgnoreCase("none")) {
            WingDefinition def = getWing(wingId);
            if (def != null) {
                spawnWingDisplay(player, def);
            }
        } else {
            removeWingDisplay(player);
        }
    }

    public void startSyncTask() {
        stopSyncTask();
        if (!plugin.getConfig().getBoolean("cosmetics_settings.wings.enabled", true)) return;

        // Tarea compartida ligera cada 4 ticks (0.2s) para rotar las alas con la espalda del jugador
        syncTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            Iterator<Map.Entry<UUID, ItemDisplay>> it = activeDisplays.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<UUID, ItemDisplay> entry = it.next();
                Player player = Bukkit.getPlayer(entry.getKey());
                ItemDisplay display = entry.getValue();

                if (player == null || !player.isOnline() || player.isDead() || display == null || display.isDead()) {
                    if (display != null && !display.isDead()) display.remove();
                    it.remove();
                    continue;
                }

                if (player.isInvisible() || player.getGameMode() == GameMode.SPECTATOR) {
                    display.remove();
                    it.remove();
                    continue;
                }

                // Reacoplar si se desmontó por cambio de mundo o teletransporte
                if (!player.getPassengers().contains(display)) {
                    if (display.getWorld().equals(player.getWorld())) {
                        player.addPassenger(display);
                    } else {
                        display.remove();
                        it.remove();
                        continue;
                    }
                }

                // Sincronizar rotación horizontal con la espalda del jugador
                float bodyYaw = player.getLocation().getYaw();
                float yawRad = (float) Math.toRadians(-bodyYaw + 180.0f);
                Quaternionf rot = new Quaternionf().rotateY(yawRad);
                Vector3f translation = new Vector3f(0f, player.isSneaking() ? -0.50f : -0.35f, 0f);
                Vector3f scale = new Vector3f(1.25f, 1.25f, 1.25f);
                display.setInterpolationDelay(0);
                display.setInterpolationDuration(2);
                display.setTransformation(new Transformation(translation, rot, scale, new Quaternionf()));

                // Partícula sutil de las alas si el jugador activó partículas
                PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
                if (data != null && data.isSettingParticles() && Math.random() < 0.25) {
                    WingDefinition def = getWing(data.getSelectedWings());
                    if (def != null && def.trailParticle != null) {
                        player.getWorld().spawnParticle(def.trailParticle,
                                player.getLocation().add(0, 1.1, 0), 1, 0.35, 0.25, 0.35, 0.01);
                    }
                }
            }
        }, 10L, 4L);
    }

    public void stopSyncTask() {
        if (syncTask != null) {
            syncTask.cancel();
            syncTask = null;
        }
        cleanupAllDisplays();
    }

    public void cleanupAllDisplays() {
        for (ItemDisplay display : activeDisplays.values()) {
            if (display != null && !display.isDead()) {
                display.remove();
            }
        }
        activeDisplays.clear();
    }
}

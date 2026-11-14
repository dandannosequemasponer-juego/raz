package com.cytricos.races.manager;

import com.cytricos.races.CytricosRaces;
import com.cytricos.races.model.PlayerData;
import com.cytricos.races.util.ColorUtils;
import org.bukkit.*;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
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
 * Gestor Independiente de Cosméticos Craneales 3D (Coronas y Aureolas) para CYTRICOS RPG.
 * Fase 3: Completamente desacoplado de la ranura física del casco (player.getInventory().getHelmet()),
 * preservando la armadura equipada y la protección solar real del jugador.
 *
 * Lead Developers: Dandan_13 & Wolfit12
 */
public class HeadCosmeticManager {
    private final CytricosRaces plugin;
    private final NamespacedKey ownerKey;
    private final Map<String, HeadCosmeticDef> registry = new LinkedHashMap<>();
    private final Map<UUID, UUID> activeEntities = new ConcurrentHashMap<>();
    private final Map<UUID, String> activeIds = new ConcurrentHashMap<>();
    private BukkitTask syncTask;
    private long tickCounter = 0L;

    public static class HeadCosmeticDef {
        public final String id;
        public final String displayName;
        public final String description;
        public final Material iconMaterial;
        public final int customModelData;
        public final double heightOffset;
        public final float scale;
        public final boolean rotateSlowly;
        public final WingsManager.WingRarity rarity;
        public final double unlockCost;

        public HeadCosmeticDef(String id, String displayName, String description,
                               Material iconMaterial, int customModelData,
                               double heightOffset, float scale, boolean rotateSlowly,
                               WingsManager.WingRarity rarity, double unlockCost) {
            this.id = id;
            this.displayName = displayName;
            this.description = description;
            this.iconMaterial = iconMaterial;
            this.customModelData = customModelData;
            this.heightOffset = heightOffset;
            this.scale = scale;
            this.rotateSlowly = rotateSlowly;
            this.rarity = rarity;
            this.unlockCost = unlockCost;
        }
    }

    public HeadCosmeticManager(CytricosRaces plugin) {
        this.plugin = plugin;
        this.ownerKey = new NamespacedKey(plugin, "head_cosmetic_owner");
        initRegistry();
    }

    private void initRegistry() {
        register(new HeadCosmeticDef(
                "celestial_crown",
                "&e&lCorona Celestial",
                "Diadema real flotante sobre la cabeza. No ocupa el slot de casco.",
                Material.GOLDEN_HELMET, 21007,
                0.36, 0.72f, false,
                WingsManager.WingRarity.LEGENDARIO, 5000.0
        ));
        register(new HeadCosmeticDef(
                "astralis_halo",
                "&b&lAureola Astral Divina",
                "Anillo luminoso suspendido sobre el jugador. No ocupa el slot de casco.",
                Material.NETHER_STAR, 21014,
                0.50, 0.78f, true,
                WingsManager.WingRarity.MITICO, 8500.0
        ));
    }

    private void register(HeadCosmeticDef def) {
        registry.put(def.id.toLowerCase(), def);
    }

    public Collection<HeadCosmeticDef> getAllHeadCosmetics() {
        return Collections.unmodifiableCollection(registry.values());
    }

    public HeadCosmeticDef getCosmetic(String id) {
        if (id == null) return null;
        return registry.get(id.toLowerCase());
    }

    public boolean isUnlocked(Player player, PlayerData data, HeadCosmeticDef def) {
        if (def == null) return false;
        if (player != null && (player.hasPermission("cytricosraces.admin")
                || player.hasPermission("cytricosraces.headcosmetics." + def.id)
                || player.hasPermission("cytricosraces.cosmetics.*"))) {
            return true;
        }
        if (data == null) return false;
        return data.hasUnlockedCosmetic("head:" + def.id)
                || def.id.equalsIgnoreCase(data.getSelectedHeadCosmetic())
                || data.getLevel() >= 3;
    }

    public boolean tryUnlockWithVault(Player player, PlayerData data, HeadCosmeticDef def) {
        if (player == null || data == null || def == null) return false;
        if (isUnlocked(player, data, def)) return true;

        if (!plugin.isVaultAvailable() || plugin.getVaultEconomy() == null || def.unlockCost <= 0.0) {
            ColorUtils.sendMessage(player, "&cAlcanza el &eNivel 3+ &cpara desbloquear " + def.displayName + "&c.");
            return false;
        }
        double bal = plugin.getVaultEconomy().getBalance(player);
        if (bal < def.unlockCost) {
            ColorUtils.sendMessage(player, "&cNecesitas &e$" + String.format("%.0f", def.unlockCost)
                    + " &co alcanzar &eNivel 3+ &cpara desbloquear " + def.displayName + "&c.");
            return false;
        }
        plugin.getVaultEconomy().withdrawPlayer(player, def.unlockCost);
        data.unlockCosmetic("head:" + def.id);
        plugin.getPlayerDataManager().saveToFile(data);
        ColorUtils.sendMessage(player, "&a&l[COSMÉTICOS] &7¡Desbloqueado " + def.displayName + " &7por &e$" + String.format("%.0f", def.unlockCost) + "&7!");
        return true;
    }

    public boolean equipHeadCosmetic(Player player, String id) {
        if (player == null) return false;
        cleanupPlayerHeadCosmetic(player);

        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        if (data == null) return false;

        if (id == null || id.equalsIgnoreCase("none")) {
            data.setSelectedHeadCosmetic("none");
            plugin.getPlayerDataManager().saveToFile(data);
            return true;
        }

        HeadCosmeticDef def = getCosmetic(id);
        if (def == null) return false;

        data.setSelectedHeadCosmetic(def.id);
        data.unlockCosmetic("head:" + def.id);
        plugin.getPlayerDataManager().saveToFile(data);

        spawnHeadCosmetic(player, def);
        return true;
    }

    public void toggleHeadCosmetic(Player player, String id) {
        if (player == null || id == null) return;
        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        if (data == null) return;

        if (id.equalsIgnoreCase(data.getSelectedHeadCosmetic())) {
            equipHeadCosmetic(player, "none");
            ColorUtils.sendMessage(player, "&e&l[COSMÉTICOS] &7Accesorio craneal desactivado. Tu ranura de casco sigue libre.");
        } else {
            HeadCosmeticDef def = getCosmetic(id);
            if (def != null) {
                equipHeadCosmetic(player, def.id);
                ColorUtils.sendMessage(player, "&a&l[COSMÉTICOS] &7Equipado " + def.displayName + " &7flotante (tu casco real no fue tocado).");
            }
        }
    }

    public void unequipOnRaceChange(Player player) {
        if (player == null) return;
        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        if (data != null) {
            data.setSelectedHeadCosmetic("none");
            plugin.getPlayerDataManager().saveToFile(data);
        }
        cleanupPlayerHeadCosmetic(player);
    }

    public void spawnHeadCosmetic(Player player, HeadCosmeticDef def) {
        if (player == null || !player.isOnline() || player.isDead() || def == null) return;
        cleanupPlayerHeadCosmetic(player);
        if (player.getGameMode() == GameMode.SPECTATOR || player.isInvisible()) return;

        try {
            Location loc = computeTargetLocation(player, def);
            ItemStack displayItem = new ItemStack(Material.CARVED_PUMPKIN);
            ItemMeta meta = displayItem.getItemMeta();
            if (meta != null) {
                meta.setCustomModelData(def.customModelData);
                displayItem.setItemMeta(meta);
            }

            ItemDisplay entity = player.getWorld().spawn(loc, ItemDisplay.class, ed -> {
                ed.setPersistent(false);
                ed.setItemStack(displayItem);
                ed.setBillboard(Display.Billboard.FIXED);
                ed.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.HEAD);
                ed.setInterpolationDelay(0);
                ed.setInterpolationDuration(2);
                ed.setTeleportDuration(2);
                ed.setViewRange(0.90f);
                ed.getPersistentDataContainer().set(ownerKey, PersistentDataType.STRING, player.getUniqueId().toString());
            });

            activeEntities.put(player.getUniqueId(), entity.getUniqueId());
            activeIds.put(player.getUniqueId(), def.id);

            PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
            boolean hideFirstPerson = data == null || data.isFirstPersonWingsHidden();
            if (plugin.getProtocolLibHook() != null) {
                if (hideFirstPerson) plugin.getProtocolLibHook().registerTrackedEntity(entity.getEntityId(), player.getUniqueId());
                else plugin.getProtocolLibHook().unregisterTrackedEntity(entity.getEntityId());
            }
            if (hideFirstPerson) {
                player.hideEntity(plugin, entity);
            } else {
                player.showEntity(plugin, entity);
            }

            applyTransform(player, def, entity);
        } catch (Throwable t) {
            plugin.getLogger().warning("Error generando cosmético craneal para " + player.getName() + ": " + t.getMessage());
        }
    }

    public void refreshPlayerHeadCosmetic(Player player) {
        if (player == null || !player.isOnline()) return;
        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        if (data == null || !data.hasAssignedRace()) {
            cleanupPlayerHeadCosmetic(player);
            return;
        }
        String selected = data.getSelectedHeadCosmetic();
        if (selected != null && !"none".equalsIgnoreCase(selected)) {
            HeadCosmeticDef def = getCosmetic(selected);
            if (def != null) {
                spawnHeadCosmetic(player, def);
                return;
            }
        }
        cleanupPlayerHeadCosmetic(player);
    }

    public void applyFirstPersonState(Player player) {
        if (player == null) return;
        UUID entId = activeEntities.get(player.getUniqueId());
        if (entId == null) return;
        Entity ent = Bukkit.getEntity(entId);
        if (!(ent instanceof ItemDisplay display) || display.isDead()) return;

        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        boolean hide = data == null || data.isFirstPersonWingsHidden();
        if (plugin.getProtocolLibHook() != null) {
            if (hide) plugin.getProtocolLibHook().registerTrackedEntity(display.getEntityId(), player.getUniqueId());
            else plugin.getProtocolLibHook().unregisterTrackedEntity(display.getEntityId());
        }
        if (hide) player.hideEntity(plugin, display);
        else player.showEntity(plugin, display);
    }

    public void cleanupPlayerHeadCosmetic(Player player) {
        if (player == null) return;
        UUID uuid = player.getUniqueId();
        String ownerStr = uuid.toString();

        activeIds.remove(uuid);
        UUID entId = activeEntities.remove(uuid);
        if (entId != null) {
            Entity ent = Bukkit.getEntity(entId);
            if (ent != null && !ent.isDead()) {
                if (plugin.getProtocolLibHook() != null) {
                    plugin.getProtocolLibHook().unregisterTrackedEntity(ent.getEntityId());
                }
                ent.remove();
            }
        }

        for (Entity nearby : player.getNearbyEntities(16.0, 16.0, 16.0)) {
            if (nearby instanceof ItemDisplay id) {
                String tagOwner = id.getPersistentDataContainer().get(ownerKey, PersistentDataType.STRING);
                if (ownerStr.equals(tagOwner)) {
                    id.remove();
                }
            }
        }
    }

    private Location computeTargetLocation(Player player, HeadCosmeticDef def) {
        Location eye = player.getEyeLocation();
        double bob = def.rotateSlowly ? Math.sin(tickCounter * 0.14) * 0.04 : 0.0;
        Location target = eye.clone().add(0, def.heightOffset + bob, 0);
        target.setPitch(0f);
        return target;
    }

    private void applyTransform(Player player, HeadCosmeticDef def, ItemDisplay entity) {
        float yaw = player.getLocation().getYaw();
        float spin = def.rotateSlowly ? (tickCounter * 3.0f) % 360.0f : 0.0f;
        float yawRad = (float) Math.toRadians(-yaw + spin);
        Quaternionf rot = new Quaternionf().rotateY(yawRad);
        Vector3f scale = new Vector3f(def.scale, def.scale, def.scale);
        entity.setInterpolationDelay(0);
        entity.setInterpolationDuration(2);
        entity.setTransformation(new Transformation(new Vector3f(0, 0, 0), rot, scale, new Quaternionf()));
    }

    public void startSyncTask() {
        stopSyncTask();
        purgeOrphanedEntities();

        syncTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            tickCounter++;
            Iterator<Map.Entry<UUID, UUID>> it = activeEntities.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<UUID, UUID> entry = it.next();
                Player player = Bukkit.getPlayer(entry.getKey());
                Entity ent = Bukkit.getEntity(entry.getValue());

                if (player == null || !player.isOnline() || player.isDead()
                        || !(ent instanceof ItemDisplay display) || display.isDead()) {
                    if (ent != null && !ent.isDead()) ent.remove();
                    activeIds.remove(entry.getKey());
                    it.remove();
                    continue;
                }

                if (player.isInvisible() || player.getGameMode() == GameMode.SPECTATOR
                        || !display.getWorld().equals(player.getWorld())) {
                    display.remove();
                    activeIds.remove(entry.getKey());
                    it.remove();
                    continue;
                }

                String cosId = activeIds.get(player.getUniqueId());
                HeadCosmeticDef def = getCosmetic(cosId);
                if (def == null) {
                    display.remove();
                    activeIds.remove(entry.getKey());
                    it.remove();
                    continue;
                }

                Location target = computeTargetLocation(player, def);
                display.teleport(target);
                applyTransform(player, def, display);
            }
        }, 4L, 2L);
    }

    private void purgeOrphanedEntities() {
        for (World world : Bukkit.getWorlds()) {
            for (ItemDisplay display : world.getEntitiesByClass(ItemDisplay.class)) {
                if (display.getPersistentDataContainer().has(ownerKey, PersistentDataType.STRING)) {
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
        cleanupAllDisplays();
    }

    public void cleanupAllDisplays() {
        for (UUID entId : activeEntities.values()) {
            Entity ent = Bukkit.getEntity(entId);
            if (ent != null && !ent.isDead()) {
                ent.remove();
            }
        }
        activeEntities.clear();
        activeIds.clear();
        purgeOrphanedEntities();
    }
}

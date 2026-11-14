package com.cytricos.races.manager;

import com.cytricos.races.CytricosRaces;
import com.cytricos.races.model.PlayerData;
import com.cytricos.races.util.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

/**
 * Gestor eficiente de cosméticos y auras de partículas para CYTRICOS RPG.
 * Incluye catálogo con rareza, requisitos de desbloqueo e integración con Vault,
 * ejecutando una única tarea ligera compartida para todos los jugadores en línea.
 *
 * Lead Developers: Dandan_13 & Wolfit12
 */
public class CosmeticsManager {
    private final CytricosRaces plugin;
    private final Map<String, AuraDefinition> auraRegistry = new LinkedHashMap<>();
    private BukkitTask auraTask;
    private double angle = 0;

    public static class AuraDefinition {
        public final String id;
        public final Material icon;
        public final String displayName;
        public final String description;
        public final WingsManager.WingRarity rarity;
        public final int requiredLevel;
        public final double unlockCost;

        public AuraDefinition(String id, Material icon, String displayName, String description,
                              WingsManager.WingRarity rarity, int requiredLevel, double unlockCost) {
            this.id = id;
            this.icon = icon;
            this.displayName = displayName;
            this.description = description;
            this.rarity = rarity;
            this.requiredLevel = requiredLevel;
            this.unlockCost = unlockCost;
        }
    }

    public CosmeticsManager(CytricosRaces plugin) {
        this.plugin = plugin;
        initAuras();
    }

    private void initAuras() {
        registerAura(new AuraDefinition("none", Material.BARRIER, "&c&lSin Aura",
                "Desactiva el efecto orbital de partículas.", WingsManager.WingRarity.COMUN, 1, 0.0));
        registerAura(new AuraDefinition("flame", Material.BLAZE_POWDER, "&6&lLlama Abisal",
                "Chispas ígneas y humo orbital constante.", WingsManager.WingRarity.RARO, 1, 0.0));
        registerAura(new AuraDefinition("fairy", Material.PRISMARINE_CRYSTALS, "&d&lBrillo Feérico",
                "Polvo mágico resplandeciente del bosque.", WingsManager.WingRarity.RARO, 1, 0.0));
        registerAura(new AuraDefinition("shadow", Material.ECHO_SHARD, "&8&lBruma Umbría",
                "Llama azulada de almas y cenizas espectrales.", WingsManager.WingRarity.EPICO, 2, 2500.0));
        registerAura(new AuraDefinition("holy", Material.NETHER_STAR, "&e&lResplandor Sagrado",
                "Destellos dorados de purificación divina.", WingsManager.WingRarity.LEGENDARIO, 3, 5000.0));
        registerAura(new AuraDefinition("runic", Material.CHERRY_LEAVES, "&5&lChispas Rúnicas",
                "Pétalos ancestrales y motas energéticas del portal.", WingsManager.WingRarity.MITICO, 4, 8500.0));
    }

    private void registerAura(AuraDefinition def) {
        auraRegistry.put(def.id.toLowerCase(), def);
    }

    public Collection<AuraDefinition> getAllAuras() {
        return Collections.unmodifiableCollection(auraRegistry.values());
    }

    public AuraDefinition getAura(String id) {
        if (id == null) return null;
        return auraRegistry.get(id.toLowerCase());
    }

    public boolean isAuraUnlocked(Player player, PlayerData data, AuraDefinition def) {
        if (def == null) return false;
        if ("none".equalsIgnoreCase(def.id)) return true;
        if (player != null && (player.hasPermission("cytricosraces.admin")
                || player.hasPermission("cytricosraces.auras." + def.id)
                || player.hasPermission("cytricosraces.auras.*"))) {
            return true;
        }
        if (def.requiredLevel <= 1 && def.unlockCost <= 0.0) return true;
        if (data == null) return false;
        if (data.hasUnlockedCosmetic("aura:" + def.id) || def.id.equalsIgnoreCase(data.getSelectedCosmeticAura())) {
            return true;
        }
        return data.getLevel() >= def.requiredLevel;
    }

    public boolean tryUnlockAuraWithVault(Player player, PlayerData data, AuraDefinition def) {
        if (player == null || data == null || def == null) return false;
        if (isAuraUnlocked(player, data, def)) return true;

        if (!plugin.getConfig().getBoolean("cosmetics_settings.wings.allow_vault_unlock", true)) {
            ColorUtils.sendMessage(player, "&cRequiere &eNivel " + def.requiredLevel + "+ &cpara desbloquear esta aura.");
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
        data.unlockCosmetic("aura:" + def.id);
        plugin.getPlayerDataManager().saveToFile(data);
        ColorUtils.sendMessage(player, "&a&l[COSMÉTICOS] &7¡Aura desbloqueada: " + def.displayName + " &7por &e$" + String.format("%.0f", def.unlockCost) + "&7!");
        return true;
    }

    public void startAuraTask() {
        if (auraTask != null) {
            auraTask.cancel();
        }
        if (!plugin.getConfig().getBoolean("cosmetics_settings.auras_enabled", true)) {
            return;
        }

        // Tarea cada 10 ticks (0.5s): Reproduce partículas orbitales sutiles
        auraTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            angle += 0.35;
            if (angle > Math.PI * 2) {
                angle = 0;
            }

            for (Player player : Bukkit.getOnlinePlayers()) {
                if (!player.isValid() || player.isDead()) continue;

                PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
                if (data == null) continue;

                // Respetar la preferencia de partículas del jugador
                if (!data.isSettingParticles()) continue;

                String aura = data.getSelectedCosmeticAura();
                if (aura == null || aura.equalsIgnoreCase("none")) continue;

                Location loc = player.getLocation().add(0, 0.9, 0);
                double xOffset = 0.65 * Math.cos(angle);
                double zOffset = 0.65 * Math.sin(angle);
                Location particleLoc = loc.clone().add(xOffset, 0, zOffset);
                Location oppositeLoc = loc.clone().add(-xOffset, 0, -zOffset);

                switch (aura.toLowerCase()) {
                    case "flame", "fuego" -> {
                        player.getWorld().spawnParticle(Particle.FLAME, particleLoc, 1, 0, 0, 0, 0.01);
                        player.getWorld().spawnParticle(Particle.SMOKE, oppositeLoc, 1, 0, 0, 0, 0.01);
                    }
                    case "fairy", "hada" -> {
                        player.getWorld().spawnParticle(Particle.ENCHANTED_HIT, particleLoc, 2, 0.05, 0.05, 0.05, 0.02);
                        player.getWorld().spawnParticle(Particle.END_ROD, oppositeLoc, 1, 0, 0, 0, 0.01);
                    }
                    case "shadow", "sombra" -> {
                        player.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, particleLoc, 1, 0, 0, 0, 0.01);
                        player.getWorld().spawnParticle(Particle.ASH, oppositeLoc, 2, 0.05, 0.05, 0.05, 0.01);
                    }
                    case "holy", "sagrado" -> {
                        player.getWorld().spawnParticle(Particle.WAX_OFF, particleLoc, 2, 0.05, 0.05, 0.05, 0.02);
                        player.getWorld().spawnParticle(Particle.GLOW, oppositeLoc, 1, 0, 0, 0, 0.01);
                    }
                    case "runic", "runico" -> {
                        player.getWorld().spawnParticle(Particle.CHERRY_LEAVES, particleLoc, 1, 0, 0, 0, 0.02);
                        player.getWorld().spawnParticle(Particle.PORTAL, oppositeLoc, 2, 0.05, 0.05, 0.05, 0.02);
                    }
                }
            }
        }, 20L, 10L);
    }

    public void stopAuraTask() {
        if (auraTask != null) {
            auraTask.cancel();
            auraTask = null;
        }
    }
}

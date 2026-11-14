package com.cytricos.races.manager;

import com.cytricos.races.CytricosRaces;
import com.cytricos.races.model.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

/**
 * Gestor eficiente de cosméticos y auras de partículas para CYTRICOS RPG.
 * Utiliza una única tarea asíncrona o sincrónica compartida para todos los jugadores en línea,
 * evitando sobrecarga de memoria o ticks de servidor.
 *
 * Lead Developers: Dandan_13 & Wolfit12
 */
public class CosmeticsManager {
    private final CytricosRaces plugin;
    private BukkitTask auraTask;
    private double angle = 0;

    public CosmeticsManager(CytricosRaces plugin) {
        this.plugin = plugin;
    }

    public void startAuraTask() {
        if (auraTask != null) {
            auraTask.cancel();
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

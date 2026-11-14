package com.cytricos.races.hook;

import com.cytricos.races.CytricosRaces;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

/**
 * Hook oficial para WorldGuard 7.x en Paper 1.20.5+ / 1.21.
 * Valida de forma estricta las banderas de:
 * - PVP (pvp deny)
 * - INVINCIBILITY (invincible allow)
 * - BLOCK_BREAK y BUILD
 *
 * Funciones de Zona Segura (Safezone):
 * - Desactiva la quemadura solar de Vampiros dentro de zonas protegidas.
 * - Suprime el daño pasivo agresivo, robo de ítems y knockbacks.
 * - Deshabilita explosiones o bursts de transformación en zonas seguras.
 *
 * Lead Developers: Dandan_13 & Wolfit12
 */
public class WorldGuardHook {
    private final CytricosRaces plugin;
    private final boolean available;

    public WorldGuardHook(CytricosRaces plugin) {
        this.plugin = plugin;
        boolean wgPresent = false;
        try {
            wgPresent = plugin.getServer().getPluginManager().getPlugin("WorldGuard") != null
                    && Class.forName("com.sk89q.worldguard.WorldGuard") != null;
        } catch (Throwable t) {
            wgPresent = false;
        }
        this.available = wgPresent;
        if (available) {
            plugin.getLogger().info("Integracion con WorldGuard 7.x establecida. Protecciones regionales activas.");
        } else {
            plugin.getLogger().info("WorldGuard no detectado. Las verificaciones regionales operaran en modo permisivo.");
        }
    }

    public boolean isAvailable() {
        return available;
    }

    /**
     * Comprueba si la ubicación del jugador corresponde a una Zona Segura (Safezone):
     * - Bandera PVP denegada (pvp: DENY)
     * - O Bandera INVINCIBILITY permitida (invincible: ALLOW)
     */
    public boolean isInSafeZone(Player player, Location location) {
        if (!available || location == null) return false;
        try {
            com.sk89q.worldguard.LocalPlayer localPlayer = player != null ? WorldGuardPlugin.inst().wrapPlayer(player) : null;
            RegionQuery query = WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery();
            com.sk89q.worldedit.util.Location weLoc = BukkitAdapter.adapt(location);

            // Si PvP está denegado, es zona segura
            boolean pvpAllowed = query.testState(weLoc, localPlayer, Flags.PVP);
            if (!pvpAllowed) return true;

            // Si invencibilidad está permitida, es zona segura
            boolean invincible = query.testState(weLoc, localPlayer, Flags.INVINCIBILITY);
            return invincible;
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * Verifica si el PVP está permitido para un atacante en una ubicación determinada.
     */
    public boolean canPvp(Player attacker, Location location) {
        if (!available || attacker == null || location == null) return true;
        try {
            com.sk89q.worldguard.LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(attacker);
            RegionQuery query = WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery();
            com.sk89q.worldedit.util.Location weLoc = BukkitAdapter.adapt(location);
            return query.testState(weLoc, localPlayer, Flags.PVP);
        } catch (Throwable t) {
            return true;
        }
    }

    /**
     * Verifica si el jugador tiene permiso para romper un bloque específico.
     */
    public boolean canBreak(Player player, Block block) {
        if (!available || player == null || block == null) return true;
        try {
            com.sk89q.worldguard.LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(player);
            RegionQuery query = WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery();
            com.sk89q.worldedit.util.Location weLoc = BukkitAdapter.adapt(block.getLocation());
            return query.testState(weLoc, localPlayer, Flags.BLOCK_BREAK)
                    && query.testState(weLoc, localPlayer, Flags.BUILD);
        } catch (Throwable t) {
            return true;
        }
    }

    /**
     * Verifica si se permite la construcción o modificación en una ubicación.
     */
    public boolean canBuild(Player player, Location location) {
        if (!available || player == null || location == null) return true;
        try {
            com.sk89q.worldguard.LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(player);
            RegionQuery query = WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery();
            com.sk89q.worldedit.util.Location weLoc = BukkitAdapter.adapt(location);
            return query.testState(weLoc, localPlayer, Flags.BUILD);
        } catch (Throwable t) {
            return true;
        }
    }

    /**
     * Comprobación genérica de bandera StateFlag.
     */
    public boolean testFlag(Player player, Location location, StateFlag flag) {
        if (!available || player == null || location == null || flag == null) return true;
        try {
            com.sk89q.worldguard.LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(player);
            RegionQuery query = WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery();
            com.sk89q.worldedit.util.Location weLoc = BukkitAdapter.adapt(location);
            return query.testState(weLoc, localPlayer, flag);
        } catch (Throwable t) {
            return true;
        }
    }
}

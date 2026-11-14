package com.cytricos.races.hook;

import com.cytricos.races.CytricosRaces;
import com.cytricos.races.model.PlayerData;
import com.cytricos.races.util.ColorUtils;
import me.libraryaddict.disguise.DisguiseAPI;
import me.libraryaddict.disguise.disguisetypes.DisguiseType;
import me.libraryaddict.disguise.disguisetypes.MobDisguise;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

/**
 * Hook oficial para LibsDisguises en Paper 1.20.5+ / 1.21.
 * Gestiona metamorfosis raciales por activación de Shift (6 segundos) o comando /raza transformar:
 * - Vampiro (Lvl >= 2) -> BAT (vuelo rápido, hitbox reducida)
 * - Werewolf (Lvl >= 4) -> WOLF (fuerza feral, velocidad) (Lvl 6: POLAR_BEAR - Fenrir)
 * - Fairy (Lvl >= 1) -> ALLAY / BEE (escala 0.50, vuelo y agilidad)
 * - Dragonkin (Lvl >= 4) -> PHANTOM (Lvl 6: ENDER_DRAGON)
 * - Yokai (Lvl >= 3) -> VEX (paso etéreo)
 * - Giant (Lvl >= 3) -> GIANT (envergadura colosal)
 * - Orc (Lvl >= 3) -> IRON_GOLEM (resistencia pétrea)
 * - Goblin (Lvl >= 3) -> ZOMBIE (pequeño/sigiloso)
 * - Demon (Lvl >= 3) -> WITHER_SKELETON (fuego abisal)
 * - Beastkin (Lvl >= 3) -> FOX (agilidad animal)
 * - Undead (Lvl >= 3) -> WITHER_SKELETON / SKELETON
 * - Angel (Lvl >= 3) -> VEX
 *
 * Oculta el renderizado de armadura para evitar glitches visuales de armadura flotante.
 * Mantiene la hotbar e interacción de ítems 100% funcionales.
 * Lead Developers: Dandan_13 & Wolfit12
 */
public class LibsDisguisesHook {
    private final CytricosRaces plugin;
    private final boolean available;

    public LibsDisguisesHook(CytricosRaces plugin) {
        this.plugin = plugin;
        boolean present = false;
        try {
            present = plugin.getServer().getPluginManager().getPlugin("LibsDisguises") != null
                    && Class.forName("me.libraryaddict.disguise.DisguiseAPI") != null;
        } catch (Throwable t) {
            present = false;
        }
        this.available = present;
        if (available) {
            plugin.getLogger().info("Integracion con LibsDisguises establecida. Metamorfosis visual activa.");
        } else {
            plugin.getLogger().info("LibsDisguises no detectado. Las transformaciones operaran con efectos visuales y particulas.");
        }
    }

    public boolean isAvailable() {
        return available;
    }

    /**
     * Aplica la metamorfosis correspondiente a la raza y nivel del jugador.
     * Retorna false si el nivel del jugador no cumple el umbral mínimo de metamorfosis.
     */
    public boolean disguiseForRace(Player player, String raceId, int level) {
        if (!available || player == null || raceId == null) return false;
        try {
            DisguiseType type;
            switch (raceId.toLowerCase()) {
                case "vampire" -> {
                    if (level < 2) {
                        ColorUtils.sendMessage(player, "&cRequieres Nivel 2 o superior como Vampiro para transformarte en Murciélago.");
                        return false;
                    }
                    type = DisguiseType.BAT;
                }
                case "werewolf" -> {
                    if (level < 4) {
                        ColorUtils.sendMessage(player, "&cRequieres Nivel 4 o superior como Hombre Lobo para despertar la Forma Bestial.");
                        return false;
                    }
                    type = (level >= 6) ? DisguiseType.POLAR_BEAR : DisguiseType.WOLF;
                }
                case "fairy" -> {
                    try {
                        type = DisguiseType.valueOf("ALLAY");
                    } catch (Throwable t) {
                        type = DisguiseType.BEE;
                    }
                }
                case "dragonkin" -> {
                    if (level < 4) {
                        ColorUtils.sendMessage(player, "&cRequieres Nivel 4 o superior para manifestar el aspecto dracónico.");
                        return false;
                    }
                    type = (level >= 6) ? DisguiseType.ENDER_DRAGON : DisguiseType.PHANTOM;
                }
                case "yokai" -> {
                    if (level < 3) {
                        ColorUtils.sendMessage(player, "&cRequieres Nivel 3 o superior para asumir forma espectral.");
                        return false;
                    }
                    type = DisguiseType.VEX;
                }
                case "giant" -> {
                    if (level < 3) {
                        ColorUtils.sendMessage(player, "&cRequieres Nivel 3 o superior para la forma titánica.");
                        return false;
                    }
                    type = DisguiseType.GIANT;
                }
                case "orc" -> {
                    if (level < 3) {
                        ColorUtils.sendMessage(player, "&cRequieres Nivel 3 o superior para la forma de coloso de guerra.");
                        return false;
                    }
                    type = DisguiseType.IRON_GOLEM;
                }
                case "goblin" -> {
                    if (level < 3) {
                        ColorUtils.sendMessage(player, "&cRequieres Nivel 3 o superior para la forma de duende sigiloso.");
                        return false;
                    }
                    type = DisguiseType.ZOMBIE;
                }
                case "demon" -> {
                    if (level < 3) {
                        ColorUtils.sendMessage(player, "&cRequieres Nivel 3 o superior para la forma demoníaca abisal.");
                        return false;
                    }
                    type = DisguiseType.WITHER_SKELETON;
                }
                case "beastkin" -> {
                    if (level < 3) {
                        ColorUtils.sendMessage(player, "&cRequieres Nivel 3 o superior para la forma feral rápida.");
                        return false;
                    }
                    type = DisguiseType.FOX;
                }
                case "undead" -> {
                    if (level < 3) {
                        ColorUtils.sendMessage(player, "&cRequieres Nivel 3 o superior para la forma cadavérica ancestral.");
                        return false;
                    }
                    type = (level >= 6) ? DisguiseType.WITHER_SKELETON : DisguiseType.SKELETON;
                }
                case "angel" -> {
                    if (level < 3) {
                        ColorUtils.sendMessage(player, "&cRequieres Nivel 3 o superior para la forma seráfica.");
                        return false;
                    }
                    type = DisguiseType.VEX;
                }
                case "druid" -> {
                    if (level < 3) {
                        ColorUtils.sendMessage(player, "&cRequieres Nivel 3 o superior para el avatar botánico.");
                        return false;
                    }
                    type = DisguiseType.IRON_GOLEM;
                }
                case "dwarf" -> {
                    if (level < 4) {
                        ColorUtils.sendMessage(player, "&cRequieres Nivel 4 o superior para la forma de enano acorazado.");
                        return false;
                    }
                    type = DisguiseType.PIGLIN;
                }
                case "elf" -> {
                    if (level < 4) {
                        ColorUtils.sendMessage(player, "&cRequieres Nivel 4 o superior para el espíritu silvano.");
                        return false;
                    }
                    type = DisguiseType.STRAY;
                }
                default -> type = DisguiseType.WOLF;
            }

            MobDisguise mobDisguise = new MobDisguise(type);
            mobDisguise.setViewSelfDisguise(true);
            mobDisguise.setHideArmorFromSelf(true);

            // Ocultar armadura en el watcher para evitar armaduras flotantes
            try {
                mobDisguise.getWatcher().setArmorVisible(false);
            } catch (Throwable ignored) {}
            try {
                mobDisguise.getWatcher().setArmor(new org.bukkit.inventory.ItemStack[4]);
            } catch (Throwable ignored) {}

            DisguiseAPI.disguiseToAll(player, mobDisguise);

            // Conceder vuelo dinámico en murciélago o hada
            if (type == DisguiseType.BAT || type.name().equals("ALLAY") || type == DisguiseType.BEE) {
                player.setAllowFlight(true);
                player.setFlying(true);
            }

            return true;
        } catch (Throwable t) {
            plugin.getLogger().warning("Error al aplicar metamorfosis a " + player.getName() + ": " + t.getMessage());
            return false;
        }
    }

    public boolean disguiseAsBat(Player player) {
        return disguiseForRace(player, "vampire", 2);
    }

    public boolean disguiseAsWolf(Player player, boolean angry) {
        return disguiseForRace(player, "werewolf", 4);
    }

    public boolean disguiseAsPolarBear(Player player) {
        return disguiseForRace(player, "werewolf", 6);
    }

    public void undisguise(Player player) {
        if (!available || player == null) return;
        try {
            if (DisguiseAPI.isDisguised(player)) {
                DisguiseAPI.undisguiseToAll(player);
            }
            PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
            if (data != null) {
                String race = data.getRace().toLowerCase();
                if (!race.equals("fairy") && !(race.equals("angel") && data.getLevel() >= 3)) {
                    if (player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR) {
                        player.setFlying(false);
                        player.setAllowFlight(false);
                    }
                }
            }
        } catch (Throwable ignored) {}
    }

    public boolean isDisguised(Player player) {
        if (!available || player == null) return false;
        try {
            return DisguiseAPI.isDisguised(player);
        } catch (Throwable t) {
            return false;
        }
    }
}

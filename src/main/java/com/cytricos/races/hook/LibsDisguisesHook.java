package com.cytricos.races.hook;

import com.cytricos.races.CytricosRaces;
import com.cytricos.races.model.PlayerData;
import me.libraryaddict.disguise.DisguiseAPI;
import me.libraryaddict.disguise.disguisetypes.DisguiseType;
import me.libraryaddict.disguise.disguisetypes.MobDisguise;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

/**
 * Hook oficial para LibsDisguises en Paper 26.2.
 * Soporta todas las 16 razas desde Nivel 1 y degrada con elegancia si LibsDisguises no está instalado.
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
            plugin.getLogger().info("Integración con LibsDisguises establecida. Metamorfosis visual activa.");
        } else {
            plugin.getLogger().info("LibsDisguises no detectado. Metamorfosis operará con atributos, vuelo y partículas.");
        }
    }

    public boolean isAvailable() {
        return available;
    }

    public boolean isAerialRaceForm(String raceId, int level) {
        if (raceId == null) return false;
        return switch (raceId.toLowerCase()) {
            case "vampire", "fairy", "angel", "dragonkin", "yokai" -> true;
            default -> false;
        };
    }

    public boolean disguiseForRace(Player player, String raceId, int level) {
        if (!available || player == null || raceId == null || raceId.equalsIgnoreCase("none")) {
            return false;
        }
        try {
            DisguiseType type = switch (raceId.toLowerCase()) {
                case "vampire" -> DisguiseType.BAT;
                case "werewolf" -> (level >= 6) ? DisguiseType.POLAR_BEAR : DisguiseType.WOLF;
                case "fairy" -> resolveAllayOrBee();
                case "dragonkin" -> (level >= 6) ? DisguiseType.ENDER_DRAGON : DisguiseType.PHANTOM;
                case "yokai", "angel" -> DisguiseType.VEX;
                case "giant" -> DisguiseType.GIANT;
                case "orc", "druid" -> DisguiseType.IRON_GOLEM;
                case "goblin" -> DisguiseType.ZOMBIE;
                case "demon" -> DisguiseType.WITHER_SKELETON;
                case "beastkin" -> DisguiseType.FOX;
                case "undead" -> (level >= 6) ? DisguiseType.WITHER_SKELETON : DisguiseType.SKELETON;
                case "dwarf" -> DisguiseType.PIGLIN;
                case "elf" -> DisguiseType.STRAY;
                case "human" -> DisguiseType.IRON_GOLEM;
                default -> DisguiseType.WOLF;
            };

            MobDisguise mobDisguise = new MobDisguise(type);
            mobDisguise.setViewSelfDisguise(true);
            mobDisguise.setHideArmorFromSelf(true);

            try {
                mobDisguise.getWatcher().setArmorVisible(false);
            } catch (Throwable ignored) {}
            try {
                mobDisguise.getWatcher().setArmor(new org.bukkit.inventory.ItemStack[4]);
            } catch (Throwable ignored) {}

            DisguiseAPI.disguiseToAll(player, mobDisguise);
            return true;
        } catch (Throwable t) {
            plugin.getLogger().warning("Error al aplicar disfraz LibsDisguises a " + player.getName() + ": " + t.getMessage());
            return false;
        }
    }

    private DisguiseType resolveAllayOrBee() {
        try {
            return DisguiseType.valueOf("ALLAY");
        } catch (Throwable t) {
            return DisguiseType.BEE;
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

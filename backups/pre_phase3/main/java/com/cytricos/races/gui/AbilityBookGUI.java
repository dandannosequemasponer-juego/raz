package com.cytricos.races.gui;

import com.cytricos.races.CytricosRaces;
import com.cytricos.races.model.PlayerData;
import com.cytricos.races.model.Race;
import com.cytricos.races.util.ColorUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Libro y Grimorio de Habilidades Raciales para CYTRICOS RPG.
 * Clasifica y muestra todas las habilidades pasivas, activas,
 * de combate, vuelo y defensivas con requisitos y tiempos de recarga.
 *
 * Lead Developers: Dandan_13 & Wolfit12
 */
public class AbilityBookGUI {
    public static final String TITLE = ColorUtils.color("&8[&eCYTRICOS&8] &0Libro de Habilidades");
    public static final Component TITLE_COMPONENT = ColorUtils.component("&8[&eCYTRICOS&8] &0Libro de Habilidades");

    private final CytricosRaces plugin;

    public AbilityBookGUI(CytricosRaces plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 45, TITLE_COMPONENT);

        ItemStack bg = PlayerGUI.createGuiItem(Material.BLACK_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 45; i++) {
            inv.setItem(i, bg);
        }

        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        Race race = plugin.getRaceManager().getRace(data.getRace());
        String raceId = data.getRace().toLowerCase();
        int level = data.getLevel();

        // Slot 4: Cabecera informativa
        inv.setItem(4, PlayerGUI.createGuiItem(Material.ENCHANTED_BOOK, "&d&lGrimorio de Habilidades", List.of(
                "&7Raza: &f" + (race != null ? race.getDisplayName() : "Humano"),
                "&7Nivel: &6Nivel " + level + " / 6",
                "&7Recurso: &b" + (int) data.getRacialResource() + " &8(" + data.getResourceName() + ")",
                "",
                "&7Las habilidades activas se ejecutan con &eShift + F",
                "&7o mediante el comando &e/raza habilidad&7."
        )));

        // Slot 19: Habilidad Pasiva 1 (Fisiología de Raza)
        List<String> pass1Lore = new ArrayList<>();
        pass1Lore.add("&7Categoría: &bPasiva Continua");
        pass1Lore.add("&7Escala física natural de &e" + (race != null ? race.getScale() : 1.0) + "x&7.");
        pass1Lore.add("&7Modifica tu tamaño, altura de paso y alcance de hitbox.");
        pass1Lore.add("");
        pass1Lore.add("&a✔ Siempre activa e innata.");
        inv.setItem(19, PlayerGUI.createGuiItem(Material.FEATHER, "&a&lFisiología y Escala", pass1Lore));

        // Slot 20: Habilidad Pasiva 2 (Inmunidades o Adaptaciones)
        List<String> pass2Lore = new ArrayList<>();
        pass2Lore.add("&7Categoría: &bPasiva de Supervivencia");
        if (race != null && !race.getImmunities().isEmpty()) {
            pass2Lore.add("&7Inmunidades nativas: &f" + String.join(", ", race.getImmunities()));
        } else {
            pass2Lore.add("&7Adaptabilidad fisiológica mejorada.");
        }
        pass2Lore.add("");
        pass2Lore.add("&a✔ Desbloqueada desde Nivel 1.");
        inv.setItem(20, PlayerGUI.createGuiItem(Material.SHIELD, "&a&lInmunidad Elemental", pass2Lore));

        // Slot 21: Habilidad Activa Principal (Shift + F / /raza habilidad)
        List<String> actLore = new ArrayList<>();
        actLore.add("&7Categoría: &6Activa Principal");
        actLore.add("&7Activación: &eShift + F &7o &e/raza habilidad");
        actLore.add("&7Coste de Recurso: &b25-35 " + data.getResourceName());
        actLore.add("");
        switch (raceId) {
            case "vampire" -> {
                actLore.add("&fBruma Vampírica: Te envuelve en humo, otorgando");
                actLore.add("&fvelocidad, sigilo e invisibilidad temporal.");
            }
            case "werewolf" -> {
                actLore.add("&fAullido Feral: Entra en frenesí salvaje aumentando");
                actLore.add("&fel daño y la velocidad de movimiento.");
            }
            case "fairy" -> {
                actLore.add("&fPolvo Sanador: Cura a aliados cercanos y concede");
                actLore.add("&fregeneración de salud con partículas feéricas.");
            }
            case "demon" -> {
                actLore.add("&fLlamarada Abisal: Incinera a enemigos cercanos e");
                actLore.add("&fignora el daño de fuego y lava.");
            }
            case "angel" -> {
                actLore.add("&fJuicio Celestial: Emite un destello sagrado que daña");
                actLore.add("&fno-muertos y bendice a los justos.");
            }
            case "goblin" -> {
                actLore.add("&fPaso Sombrío y Hurto: Intenta hurtar un objeto");
                actLore.add("&fde un adversario sin ser detectado.");
            }
            case "human" -> {
                actLore.add("&fDespliegue Táctico: Abre tu mochila de campaña");
                actLore.add("&fy acelera tu velocidad de reacción.");
            }
            default -> {
                actLore.add("&fPoder Racial: Desata la energía inherente de tu raza");
                actLore.add("&fpara obtener ventajas tácticas inmediatas.");
            }
        }
        actLore.add("");
        if (data.isTransformOnCooldown()) {
            actLore.add("&c⏳ En recarga: &e" + data.getTransformCooldownRemainingSeconds() + "s restantes.");
        } else {
            actLore.add("&a✔ Lista para usar.");
        }
        inv.setItem(21, PlayerGUI.createGuiItem(Material.BLAZE_POWDER, "&6&lHabilidad Activa Primaria", actLore));

        // Slot 22: Polimorfia y Metamorfosis (/raza transformar)
        boolean hasTransform = raceId.equals("werewolf") || raceId.equals("vampire") || raceId.equals("yokai");
        List<String> morphLore = new ArrayList<>();
        morphLore.add("&7Categoría: &5Metamorfosis");
        morphLore.add("&7Comando: &e/raza transformar");
        if (hasTransform) {
            morphLore.add("&7Permite alternar entre tu forma humanoid y tu");
            morphLore.add("&7forma salvaje mitológica mediante LibsDisguises.");
            morphLore.add("");
            if (data.isTransformed()) {
                morphLore.add("&a✔ Metamorfosis activa actualmente.");
            } else {
                morphLore.add("&e▶ Disponible para activar.");
            }
        } else {
            morphLore.add("&cTu raza no posee habilidades de metamorfosis.");
        }
        inv.setItem(22, PlayerGUI.createGuiItem(Material.DRAGON_HEAD, "&5&lMetamorfosis Feral", morphLore));

        // Slot 23: Habilidad de Movilidad / Vuelo
        boolean hasFlight = raceId.equals("fairy") || (raceId.equals("angel") && level >= 3);
        List<String> flightLore = new ArrayList<>();
        flightLore.add("&7Categoría: &fMovilidad Aérea");
        if (hasFlight) {
            double ceiling = plugin.getConfig().getDouble("flight_settings." + raceId + "_max_height", raceId.equals("fairy") ? 16.0 : 28.0);
            flightLore.add("&7Vuelo sostenido en el aire mediante doble salto.");
            flightLore.add("&7Techo de altitud seguro: &e" + (int) ceiling + " bloques&7.");
            flightLore.add("&c¡Superar el techo activa el sistema de 3 strikes!");
            flightLore.add("");
            if (data.isFlightBanned()) {
                flightLore.add("&4✖ Vuelo inhabilitado por sanción: &e" + data.getFlightBanRemainingSeconds() + "s.");
            } else {
                flightLore.add("&a✔ Vuelo operativo.");
            }
        } else {
            flightLore.add("&7Habilidad de desplazamiento terrestre y salto ágil.");
            flightLore.add("&7Multiplicador de velocidad: &b" + (int)((race != null ? race.getBaseSpeedMultiplier() : 1.0) * 100) + "%&7.");
        }
        inv.setItem(23, PlayerGUI.createGuiItem(Material.ELYTRA, "&f&lMovilidad y Vuelo", flightLore));

        // Slot 24: Habilidad Defensiva
        List<String> defLore = new ArrayList<>();
        defLore.add("&7Categoría: &3Defensiva / Mitigación");
        defLore.add("&7Reduce el retroceso recibido y protege contra");
        defLore.add("&7embestidas críticas de adversarios.");
        defLore.add("");
        defLore.add("&a✔ Bonificación pasiva de armadura natural.");
        inv.setItem(24, PlayerGUI.createGuiItem(Material.TURTLE_HELMET, "&3&lFortaleza Defensiva", defLore));

        // Slot 36: Volver
        inv.setItem(36, PlayerGUI.createGuiItem(Material.ARROW, "&eVolver al Menú Principal", List.of("&7Regresa al hub principal.")));

        // Slot 44: Cerrar
        inv.setItem(44, PlayerGUI.createGuiItem(Material.BARRIER, "&c&lCerrar", null));

        player.openInventory(inv);
    }
}

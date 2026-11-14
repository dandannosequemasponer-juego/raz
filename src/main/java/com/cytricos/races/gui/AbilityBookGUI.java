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
 * Cubre las 16 razas con controles actualizados:
 * - Habilidad Activa: Shift + F (o /raza habilidad)
 * - Metamorfosis Universal: Shift + Clic Derecho manteniendo Shift 6.0s (o /raza transformar)
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
        boolean hasRace = data != null && data.hasAssignedRace();
        Race race = hasRace ? plugin.getRaceManager().getRace(data.getRace()) : null;
        String raceId = hasRace ? data.getRace().toLowerCase() : "none";
        int level = hasRace ? data.getLevel() : 0;

        // Slot 4: Cabecera informativa
        inv.setItem(4, PlayerGUI.createGuiItem(Material.ENCHANTED_BOOK, "&d&lGrimorio de Habilidades", List.of(
                "&7Raza: &f" + (race != null ? race.getDisplayName() : "Sin Raza"),
                "&7Nivel: &6" + (hasRace ? ("Nivel " + level + " / 6") : "Sin Nivel"),
                "&7Recurso: &b" + (int) data.getRacialResource() + " &8(" + data.getResourceName() + ")",
                "",
                "&7Habilidad Activa: &eShift + F &7o &e/raza habilidad",
                "&7Metamorfosis (6.0s): &5Shift + Clic Derecho &7o &e/raza transformar"
        )));

        // Slot 19: Pasiva de Fisiología y Escala
        List<String> pass1Lore = new ArrayList<>();
        pass1Lore.add("&7Categoría: &bPasiva Continua");
        pass1Lore.add("&7Escala física natural: &e" + (race != null ? race.getScale() : 1.0) + "x&7.");
        pass1Lore.add("&7Alcance, vida base y velocidad adaptados a tu raza.");
        pass1Lore.add("");
        pass1Lore.add(hasRace ? "&a✔ Activa permanentemente." : "&c✖ Elige una raza en /razas primero.");
        inv.setItem(19, PlayerGUI.createGuiItem(Material.FEATHER, "&a&lFisiología y Escala", pass1Lore));

        // Slot 20: Pasiva Elemental y Entorno
        List<String> pass2Lore = new ArrayList<>();
        pass2Lore.add("&7Categoría: &bPasiva de Supervivencia");
        if (race != null && !race.getImmunities().isEmpty()) {
            pass2Lore.add("&7Inmunidades nativas: &f" + String.join(", ", race.getImmunities()));
        } else {
            pass2Lore.add("&7Adaptabilidad fisiológica y biomas predilectos.");
        }
        pass2Lore.add("");
        pass2Lore.add(hasRace ? "&a✔ Desbloqueada desde Nivel 1." : "&c✖ Elige una raza en /razas primero.");
        inv.setItem(20, PlayerGUI.createGuiItem(Material.SHIELD, "&a&lInmunidad y Entorno", pass2Lore));

        // Slot 21: Habilidad Activa Principal (Shift + F)
        List<String> actLore = new ArrayList<>();
        actLore.add("&7Categoría: &6Activa Principal");
        actLore.add("&7Activación: &eShift + F &7o &e/raza habilidad");
        actLore.add("&7Clic aquí: &eEjecutar habilidad ahora");
        actLore.add("");
        switch (raceId) {
            case "dwarf" -> {
                actLore.add("&fFirmeza Sísmica (N3+): &7Inmunidad temporal a");
                actLore.add("&7proyectiles y onda expansiva subterránea.");
            }
            case "human" -> {
                actLore.add("&fSobrecarga de Adrenalina (N2+): &7Curación,");
                actLore.add("&7absorción táctica y velocidad. (&e/raza mochila &7en N4+)");
            }
            case "elf" -> {
                actLore.add("&fPaso del Viento (N3+): &7Impulso ágil, visión");
                actLore.add("&7nocturna y mejora de combate con arcos.");
            }
            case "giant" -> {
                actLore.add("&fImpacto Telúrico (N3+): &7Golpe sísmico que");
                actLore.add("&7lanza por los aires y aturde a enemigos cercanos.");
            }
            case "orc" -> {
                actLore.add("&fGrito de Guerra / Furia (N3+): &7Aumenta fuerza");
                actLore.add("&7y regeneración; en N6 evita morir en combate.");
            }
            case "goblin" -> {
                actLore.add("&fBomba de Humo y Hurto (N4+): &7Ciega enemigos");
                actLore.add("&7cercanos y otorga sigilo e invisibilidad breve.");
            }
            case "demon" -> {
                actLore.add("&fExplosión de Azufre (N3+): &7Incendia rivales");
                actLore.add("&7en área y potencia el daño cuerpo a cuerpo.");
            }
            case "angel" -> {
                actLore.add("&fJuicio Celestial (N4+): &7Otorga absorción sagrada");
                actLore.add("&7a aliados cercanos y purifica monstruos.");
            }
            case "dragonkin" -> {
                actLore.add("&fAliento Dracónico (N3+): &7Proyecta fuego y");
                actLore.add("&7empuje en cono frente a ti.");
            }
            case "beastkin" -> {
                actLore.add("&fSentido Salvaje / Bestia Totémica (N3+): &7Revela");
                actLore.add("&7objetivos cercanos y desata agilidad feral.");
            }
            case "werewolf" -> {
                actLore.add("&fAullido Alfa (N3+): &7Aumenta fuerza, velocidad");
                actLore.add("&7y otorga visión de caza a ti y tus lobos.");
            }
            case "vampire" -> {
                actLore.add("&fBruma Vampírica (N3+): &7Te vuelve invisible en");
                actLore.add("&7niebla carmesí con desplazamiento veloz.");
            }
            case "undead" -> {
                actLore.add("&fNube Necrótica (N3+): &7Desata miasma marchita");
                actLore.add("&7contra enemigos y regenera tu vitalidad ósea.");
            }
            case "druid" -> {
                actLore.add("&fEsporas Ácidas / Armonía (N3+): &7Envenena rivales");
                actLore.add("&7y regenera salud sobre hierba o naturaleza.");
            }
            case "fairy" -> {
                actLore.add("&fPolvo de Hada (N3+): &7Sana aliados cercanos y");
                actLore.add("&7eleva a hostiles con polvo luminoso.");
            }
            case "yokai" -> {
                actLore.add("&fPosesión Espectral (N3+): &7Invisibilidad e");
                actLore.add("&7intangibilidad breve atravesando la batalla.");
            }
            default -> actLore.add("&7Elige una raza en &e/razas &7para desbloquear habilidades.");
        }
        inv.setItem(21, PlayerGUI.createGuiItem(Material.BLAZE_POWDER, "&6&lHabilidad Activa Primaria", actLore));

        // Slot 22: Metamorfosis Universal (Shift + Clic Derecho 6.0s)
        List<String> morphLore = new ArrayList<>();
        morphLore.add("&7Categoría: &5Metamorfosis Racial (16 Razas)");
        morphLore.add("&7Activación: &eShift + Clic Derecho (mantener 6.0s)");
        morphLore.add("&7Comando rápido: &e/raza transformar");
        morphLore.add("&7Duración: &fN1:20s | N2:35s | N3:50s | N4:75s | N5:110s | &bN6:∞");
        morphLore.add("");
        if (!hasRace) {
            morphLore.add("&c✖ Debes elegir una raza primero.");
        } else if (data.isTransformed()) {
            morphLore.add("&a✔ Metamorfosis ACTIVA ahora mismo.");
            morphLore.add("&e▶ Clic para revertir transformación.");
        } else if (data.isTransformOnCooldown()) {
            morphLore.add("&c⏳ En enfriamiento: &e" + data.getTransformCooldownRemainingSeconds() + "s");
        } else {
            morphLore.add("&a✔ Disponible (requiere canalización de 6.0s).");
        }
        inv.setItem(22, PlayerGUI.createGuiItem(Material.DRAGON_HEAD, "&5&lMetamorfosis de Linaje", morphLore));

        // Slot 23: Movilidad / Vuelo
        boolean hasFlight = raceId.equals("fairy") || (raceId.equals("angel") && level >= 3);
        List<String> flightLore = new ArrayList<>();
        flightLore.add("&7Categoría: &fMovilidad Aérea");
        if (hasFlight) {
            double ceiling = plugin.getConfig().getDouble("flight_settings." + raceId + "_max_height", raceId.equals("fairy") ? 16.0 : 28.0);
            flightLore.add("&7Vuelo sostenido mediante doble salto.");
            flightLore.add("&7Techo seguro sobre terreno: &e" + (int) ceiling + " bloques&7.");
            flightLore.add("&7En metamorfosis aérea: techo Y=" + (int) plugin.getConfigManager().getTransformationMaxFlightY());
            flightLore.add("");
            if (data.isFlightBanned()) {
                flightLore.add("&4✖ Vuelo clausurado: &e" + data.getFlightBanRemainingSeconds() + "s");
            } else {
                flightLore.add("&a✔ Vuelo operativo.");
            }
        } else {
            flightLore.add("&7Desplazamiento terrestre adaptado a tu raza.");
            flightLore.add("&7Velocidad base: &b" + (int)((race != null ? race.getBaseSpeedMultiplier() : 1.0) * 100) + "%&7.");
        }
        inv.setItem(23, PlayerGUI.createGuiItem(Material.ELYTRA, "&f&lMovilidad y Vuelo", flightLore));

        // Slot 24: Habilidad Defensiva / Vulnerabilidad Solar
        List<String> defLore = new ArrayList<>();
        defLore.add("&7Categoría: &3Defensiva y Resiliencia");
        if (raceId.equals("vampire") || raceId.equals("undead")) {
            defLore.add("&7Vulnerabilidad Solar Gradual (N1-N3 fuerte,");
            defLore.add("&7N4 50%, N5 mínima, &aN6 inmune total al sol&7).");
            defLore.add("&7Protecciones: Cualquier casco, techo, agua o lluvia.");
        } else {
            defLore.add("&7Mitigación física y adaptación elemental de tu raza.");
        }
        defLore.add("");
        defLore.add("&a✔ Gestionado automáticamente en combate.");
        inv.setItem(24, PlayerGUI.createGuiItem(Material.TURTLE_HELMET, "&3&lResiliencia y Defensas", defLore));

        inv.setItem(36, PlayerGUI.createGuiItem(Material.ARROW, "&eVolver al Menú Principal", List.of("&7Regresa al hub principal.")));
        inv.setItem(44, PlayerGUI.createGuiItem(Material.BARRIER, "&c&lCerrar", null));

        player.openInventory(inv);
    }
}

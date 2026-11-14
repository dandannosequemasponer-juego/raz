package com.cytricos.races.gui;

import com.cytricos.races.CytricosRaces;
import com.cytricos.races.model.Race;
import com.cytricos.races.model.RaceLevel;
import com.cytricos.races.util.ColorUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Enciclopedia Interactiva de Razas (Fase 8) para CYTRICOS RPG.
 * Genera y abre un libro escrito real (Material.WRITTEN_BOOK) con Adventure API:
 * - Tabla de contenidos interactiva (clic para saltar directamente a cada raza).
 * - Ficha completa de cada una de las 16 razas (pasivas, activas, desventajas y rangos 1-6).
 * - Soporte de glifos configurables en config.yml (encyclopedia.use_glyphs).
 *
 * Lead Developers: Dandan_13 & Wolfit12
 */
public class EncyclopediaGUI {
    private final CytricosRaces plugin;

    public EncyclopediaGUI(CytricosRaces plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        if (player == null || !player.isOnline()) return;

        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();
        if (meta == null) return;

        meta.setTitle(ColorUtils.color("&6Códice de Razas CYTRICOS"));
        meta.setAuthor("CYTRICOS RPG");

        List<Race> sortedRaces = new ArrayList<>(plugin.getRaceManager().getAllRaces());
        sortedRaces.sort(Comparator.comparingInt(Race::getSlot));

        boolean useGlyphs = plugin.getConfigManager().isEncyclopediaUseGlyphs();
        List<Component> pages = new ArrayList<>();

        // Páginas 1 y 2: Tabla de Contenidos interactiva (8 razas por página)
        int firstRacePage = 3;
        pages.add(buildTocPage(sortedRaces, 0, 8, firstRacePage, useGlyphs, true));
        pages.add(buildTocPage(sortedRaces, 8, sortedRaces.size(), firstRacePage, useGlyphs, false));

        // Páginas 3..34: 2 páginas detalladas por cada una de las 16 razas
        for (Race race : sortedRaces) {
            pages.add(buildRaceOverviewPage(race, useGlyphs));
            pages.add(buildRaceProgressionPage(race));
        }

        meta.pages(pages);
        book.setItemMeta(meta);

        player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1.0f, 1.05f);
        player.openBook(book);
    }

    private Component buildTocPage(List<Race> races, int from, int to, int firstRacePage, boolean useGlyphs, boolean firstPage) {
        Component page = Component.empty();
        if (firstPage) {
            page = page.append(Component.text("══ CÓDICE DE RAZAS ══\n", NamedTextColor.DARK_PURPLE, TextDecoration.BOLD))
                    .append(Component.text("Haz clic en cualquier raza para ir a su ficha:\n\n", NamedTextColor.DARK_GRAY));
        } else {
            page = page.append(Component.text("══ ÍNDICE (Cont.) ══\n", NamedTextColor.DARK_PURPLE, TextDecoration.BOLD))
                    .append(Component.text("Selecciona un linaje:\n\n", NamedTextColor.DARK_GRAY));
        }

        for (int i = from; i < to && i < races.size(); i++) {
            Race r = races.get(i);
            int targetPage = firstRacePage + (i * 2);
            String plainName = ColorUtils.strip(r.getDisplayName());
            String prefix = useGlyphs ? (glyphForRace(r.getId()) + " ") : "• ";

            Component entry = Component.text(prefix + plainName, NamedTextColor.DARK_BLUE, TextDecoration.BOLD)
                    .clickEvent(ClickEvent.changePage(targetPage))
                    .hoverEvent(HoverEvent.showText(
                            Component.text("Clic para abrir ", NamedTextColor.YELLOW)
                                    .append(Component.text(plainName, NamedTextColor.GOLD, TextDecoration.BOLD))
                                    .append(Component.text(" (Pág. " + targetPage + ")", NamedTextColor.GRAY))
                    ));
            page = page.append(entry).append(Component.text("\n"));
        }

        page = page.append(Component.text("\nControles Clave:\n", NamedTextColor.DARK_GRAY, TextDecoration.BOLD))
                .append(Component.text("Shift+F: ", NamedTextColor.GOLD)).append(Component.text("Habilidad\n", NamedTextColor.BLACK))
                .append(Component.text("Shift+Der(6s): ", NamedTextColor.GOLD)).append(Component.text("Mutación", NamedTextColor.BLACK));
        return page;
    }

    private Component buildRaceOverviewPage(Race race, boolean useGlyphs) {
        String plainName = ColorUtils.strip(race.getDisplayName());
        String glyph = useGlyphs ? (glyphForRace(race.getId()) + " ") : "";

        Component page = Component.empty()
                .append(Component.text(glyph + plainName.toUpperCase() + "\n", NamedTextColor.DARK_RED, TextDecoration.BOLD))
                .append(Component.text("◀ Volver al Índice\n", NamedTextColor.DARK_GRAY)
                        .clickEvent(ClickEvent.changePage(1))
                        .hoverEvent(HoverEvent.showText(Component.text("Ir a la página 1", NamedTextColor.YELLOW))))
                .append(Component.text("\nEscala: ", NamedTextColor.DARK_GRAY))
                .append(Component.text(race.getScale() + "x", NamedTextColor.DARK_GREEN, TextDecoration.BOLD))
                .append(Component.text(" | Vida: ", NamedTextColor.DARK_GRAY))
                .append(Component.text((int) (race.getBaseHealthHearts() * 2) + " HP\n", NamedTextColor.RED, TextDecoration.BOLD))
                .append(Component.text("Vel: ", NamedTextColor.DARK_GRAY))
                .append(Component.text((int) (race.getBaseSpeedMultiplier() * 100) + "%", NamedTextColor.DARK_AQUA))
                .append(Component.text(" | Daño: ", NamedTextColor.DARK_GRAY))
                .append(Component.text((int) (race.getBaseDamageMultiplier() * 100) + "%\n\n", NamedTextColor.DARK_PURPLE));

        String desc = race.getDescription();
        if (desc != null && !desc.isBlank()) {
            page = page.append(Component.text(trimMax(desc, 95) + "\n\n", NamedTextColor.BLACK));
        }

        if (!race.getImmunities().isEmpty()) {
            page = page.append(Component.text("Inmunidades:\n", NamedTextColor.DARK_GREEN, TextDecoration.BOLD))
                    .append(Component.text("+ " + trimMax(String.join(", ", race.getImmunities()), 56) + "\n", NamedTextColor.DARK_GREEN));
        }

        if (!race.getVulnerabilities().isEmpty()) {
            page = page.append(Component.text("Desventajas:\n", NamedTextColor.DARK_RED, TextDecoration.BOLD))
                    .append(Component.text("- " + trimMax(String.join(", ", race.getVulnerabilities()), 56) + "\n", NamedTextColor.RED));
        }

        return page;
    }

    private Component buildRaceProgressionPage(Race race) {
        Component page = Component.text("PROGRESIÓN Y RANGOS\n", NamedTextColor.DARK_BLUE, TextDecoration.BOLD)
                .append(Component.text("◀ Volver al Índice\n\n", NamedTextColor.DARK_GRAY)
                        .clickEvent(ClickEvent.changePage(1)));

        for (int lvl = 1; lvl <= 6; lvl++) {
            RaceLevel rl = race.getLevel(lvl);
            if (rl == null) continue;
            page = page.append(Component.text("N" + lvl + " " + trimMax(rl.getName(), 16) + ": ", NamedTextColor.GOLD, TextDecoration.BOLD))
                    .append(Component.text(trimMax(rl.getPerks(), 44) + "\n", NamedTextColor.BLACK));
        }
        return page;
    }

    private String trimMax(String text, int max) {
        if (text == null) return "";
        String clean = ColorUtils.strip(text).replaceAll("\\s+", " ").trim();
        if (clean.length() <= max) return clean;
        return clean.substring(0, Math.max(0, max - 1)) + "…";
    }

    private String glyphForRace(String id) {
        if (id == null) return "✦";
        return switch (id.toLowerCase()) {
            case "dwarf" -> "⚒";
            case "human" -> "⚔";
            case "elf" -> "➶";
            case "giant" -> "⛰";
            case "orc" -> "☠";
            case "goblin" -> "⚙";
            case "demon" -> "🔥";
            case "angel" -> "✧";
            case "dragonkin" -> "🐉";
            case "beastkin" -> "🐾";
            case "werewolf" -> "🐺";
            case "vampire" -> "🦇";
            case "undead" -> "💀";
            case "druid" -> "🌿";
            case "fairy" -> "✨";
            case "yokai" -> "👁";
            default -> "✦";
        };
    }
}

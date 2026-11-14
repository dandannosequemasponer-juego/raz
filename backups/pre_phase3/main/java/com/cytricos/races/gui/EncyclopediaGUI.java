package com.cytricos.races.gui;

import com.cytricos.races.CytricosRaces;
import com.cytricos.races.model.Race;
import com.cytricos.races.util.ColorUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Enciclopedia y Códice Histórico de Razas para CYTRICOS RPG.
 * Proporciona lectura profunda sobre el trasfondo, biomas predilectos,
 * escalas físicas, fisiología y afinidades elementales de las 16 razas.
 *
 * Lead Developers: Dandan_13 & Wolfit12
 */
public class EncyclopediaGUI {
    public static final String TITLE = ColorUtils.color("&8[&eCYTRICOS&8] &0Enciclopedia de Razas");
    public static final Component TITLE_COMPONENT = ColorUtils.component("&8[&eCYTRICOS&8] &0Enciclopedia de Razas");

    private final CytricosRaces plugin;

    public EncyclopediaGUI(CytricosRaces plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 45, TITLE_COMPONENT);

        ItemStack bg = PlayerGUI.createGuiItem(Material.BLACK_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 45; i++) {
            inv.setItem(i, bg);
        }

        // Cargar las 16 razas con descripciones profundas
        for (Race race : plugin.getRaceManager().getAllRaces()) {
            ItemStack icon = createCodexItem(race);
            if (race.getSlot() >= 0 && race.getSlot() < 45) {
                inv.setItem(race.getSlot(), icon);
            }
        }

        // Slot 36: Volver
        inv.setItem(36, PlayerGUI.createGuiItem(Material.ARROW, "&eVolver al Menú Principal", List.of("&7Regresa al hub principal.")));

        // Slot 44: Cerrar
        inv.setItem(44, PlayerGUI.createGuiItem(Material.BARRIER, "&c&lCerrar", null));

        player.openInventory(inv);
    }

    private ItemStack createCodexItem(Race race) {
        List<String> lore = new ArrayList<>();
        lore.add("&7" + race.getDescription());
        lore.add("");
        lore.add("&6&lFisiología y Biología:");
        lore.add("&7- Escala Hitbox: &e" + race.getScale() + "x");
        lore.add("&7- Vida Base: &c" + (race.getBaseHealthHearts() * 2) + " HP");
        lore.add("&7- Modificador Daño: &d" + (int)(race.getBaseDamageMultiplier() * 100) + "%");
        lore.add("&7- Velocidad: &b" + (int)(race.getBaseSpeedMultiplier() * 100) + "%");
        lore.add("");
        lore.add("&9&lAfinidades e Inmunidades:");
        if (!race.getImmunities().isEmpty()) {
            lore.add("&7- " + String.join(", ", race.getImmunities()));
        } else {
            lore.add("&7- Adaptación neutral sin inmunidades elementales.");
        }
        lore.add("");
        lore.add("&4&lDebilidades Históricas:");
        if (!race.getVulnerabilities().isEmpty()) {
            for (String v : race.getVulnerabilities()) {
                lore.add("&c- " + v);
            }
        } else {
            lore.add("&7- Sin debilidades severas registradas.");
        }
        lore.add("");
        lore.add("&e▶ Clic para escuchar el eco ancestral de esta raza.");

        return PlayerGUI.createGuiItem(race.getMaterial(), "&e&l" + race.getDisplayName(), lore);
    }

    public void handleClick(Player player, int slot) {
        for (Race r : plugin.getRaceManager().getAllRaces()) {
            if (r.getSlot() == slot) {
                player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.9f, 1.0f);
                ColorUtils.sendActionBar(player, "&6Consultando Códice: &e" + r.getDisplayName());
                return;
            }
        }
    }
}

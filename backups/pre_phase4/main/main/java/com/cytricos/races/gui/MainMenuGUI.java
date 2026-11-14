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
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Hub Principal Premium RPG para CYTRICOS RPG.
 * GUI de 45 casillas con estética profesional, secciones temáticas,
 * indicadores de estado, accesos directos y navegación completa.
 *
 * Lead Developers: Dandan_13 & Wolfit12
 */
public class MainMenuGUI {
    public static final String TITLE = ColorUtils.color("&8[&eCYTRICOS&8] &0Menú Principal RPG");
    public static final Component TITLE_COMPONENT = ColorUtils.component("&8[&eCYTRICOS&8] &0Menú Principal RPG");

    private final CytricosRaces plugin;

    public MainMenuGUI(CytricosRaces plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 45, TITLE_COMPONENT);

        // Fondo: paneles de cristal negro en todas las casillas
        ItemStack bg = PlayerGUI.createGuiItem(Material.BLACK_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 45; i++) {
            inv.setItem(i, bg);
        }

        // Borde decorativo temático en bordes
        ItemStack border = PlayerGUI.createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ", null);
        int[] borderSlots = {0, 1, 2, 3, 5, 6, 7, 8, 9, 17, 27, 35, 36, 37, 38, 39, 41, 42, 43, 44};
        for (int s : borderSlots) {
            inv.setItem(s, border);
        }

        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        Race race = plugin.getRaceManager().getRace(data.getRace());
        String raceName = race != null ? race.getDisplayName() : "&fHumano";

        // Slot 4: Cabeza del jugador con estado en tiempo real
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(player);
            meta.displayName(ColorUtils.component("&e&l" + player.getName() + " &7- &f" + raceName));
            List<String> lore = new ArrayList<>();
            lore.add("&7Raza: " + raceName);
            lore.add("&7Nivel Racial: &6Nivel " + data.getLevel() + " / 6");
            lore.add("&7Escala Hitbox: &a" + (race != null ? race.getScale() : 1.0) + "x");
            lore.add("&7Recurso: &b" + (int) data.getRacialResource() + "&7/&b" + (int) data.getMaxRacialResource() + " &8(" + data.getResourceName() + ")");
            if (!data.getSelectedTitle().isEmpty()) {
                lore.add("&7Título: &6" + data.getSelectedTitle());
            }
            lore.add("&7Puntos de Talento: &e" + data.getTalentPoints());
            lore.add("");
            lore.add("&8IP: PLAY.CYTRICOS.COM");
            meta.lore(ColorUtils.componentList(lore));
            skull.setItemMeta(meta);
        }
        inv.setItem(4, skull);

        // Fila 2 y 3: Módulos RPG principales
        // Slot 19: Mi Raza & Progresión de 6 Niveles
        inv.setItem(19, PlayerGUI.createGuiItem(Material.WRITABLE_BOOK, "&a&lMi Raza y Progresión", List.of(
                "&7Inspecciona los 6 niveles de tu raza actual,",
                "&7ventajas activas, desventajas y requisitos",
                "&7para ascender hacia la cumbre de poder.",
                "",
                "&e▶ Clic para abrir el árbol de 6 niveles."
        )));

        // Slot 20: Elegir o Cambiar de Raza
        inv.setItem(20, PlayerGUI.createGuiItem(Material.NETHER_STAR, "&b&lElegir / Cambiar Raza", List.of(
                "&7Explora las 16 razas disponibles en",
                "&7CYTRICOS RPG y reencarna utilizando",
                "&7cambio gratuito, dinero Vault o Esencia.",
                "",
                "&a▶ Clic para abrir la cuadrícula de 45 casillas."
        )));

        // Slot 21: Libro de Habilidades
        inv.setItem(21, PlayerGUI.createGuiItem(Material.ENCHANTED_BOOK, "&d&lLibro de Habilidades", List.of(
                "&7Consulta todas tus habilidades pasivas,",
                "&7activas, de combate, vuelo y estados",
                "&7con sus tiempos de recarga en tiempo real.",
                "",
                "&e▶ Clic para abrir el grimorio de habilidades."
        )));

        // Slot 22: Perfil RPG y Estadísticas
        inv.setItem(22, PlayerGUI.createGuiItem(Material.TOTEM_OF_UNDYING, "&6&lMi Perfil RPG", List.of(
                "&7Resumen completo de estadísticas físicas,",
                "&7atributos de combate, vida máxima,",
                "&7bonos pasivos y medidores de aventura.",
                "",
                "&e▶ Clic para ver tu ficha de aventurero."
        )));

        // Slot 23: Árbol de Talentos Raciales
        inv.setItem(23, PlayerGUI.createGuiItem(Material.OAK_SAPLING, "&2&lÁrbol de Talentos", List.of(
                "&7Invierte tus Puntos de Talento en ramas",
                "&7de Combate, Supervivencia y Utilidad",
                "&7para personalizar el estilo de tu raza.",
                "",
                "&7Puntos Disponibles: &e" + data.getTalentPoints() + " Pts",
                "",
                "&e▶ Clic para acceder al árbol de mejoras."
        )));

        // Slot 24: Misiones Raciales (Quests)
        inv.setItem(24, PlayerGUI.createGuiItem(Material.FILLED_MAP, "&e&lMisiones Raciales", List.of(
                "&7Cumple desafíos específicos adaptados a",
                "&7la identidad y naturaleza de tu raza.",
                "&7Gana dinero Vault, experiencia y títulos.",
                "",
                "&7Misiones Completadas: &a" + data.getQuestsCompletedCount(),
                "",
                "&e▶ Clic para revisar tus misiones activas."
        )));

        // Slot 25: Logros y Títulos Cosméticos
        inv.setItem(25, PlayerGUI.createGuiItem(Material.GOLDEN_HELMET, "&6&lLogros y Títulos", List.of(
                "&7Consigue hazañas legendarias y equipa",
                "&7títulos cosméticos exclusivos para lucir",
                "&7en tu nombre de chat y perfil.",
                "",
                "&7Logros Desbloqueados: &a" + data.getUnlockedAchievements().size(),
                "",
                "&e▶ Clic para gestionar tus títulos y logros."
        )));

        // Fila 4: Utilidades y Configuración
        // Slot 29: Enciclopedia de Razas
        inv.setItem(29, PlayerGUI.createGuiItem(Material.KNOWLEDGE_BOOK, "&3&lEnciclopedia de Razas", List.of(
                "&7Guía informativa profunda sobre el lore,",
                "&7origen, hábitats y mecánicas de cada raza.",
                "",
                "&e▶ Clic para consultar la enciclopedia."
        )));

        // Slot 31: Cosméticos, Alas 3D y Auras
        inv.setItem(31, PlayerGUI.createGuiItem(Material.AMETHYST_SHARD, "&d&lAuras, Alas 3D y Cosméticos", List.of(
                "&7Equipa auras de partículas sutiles, 8 modelos",
                "&7exclusivos de Alas 3D y explora reliquias custom.",
                "",
                "&7Aura Activa: &e" + data.getSelectedCosmeticAura().toUpperCase(),
                "&7Alas Activas: &b" + data.getSelectedWings().toUpperCase(),
                "",
                "&e▶ Clic para abrir el centro cosmético."
        )));

        // Slot 33: Configuración Personal
        inv.setItem(33, PlayerGUI.createGuiItem(Material.COMPARATOR, "&f&lConfiguración Personal", List.of(
                "&7Personaliza la visibilidad de partículas,",
                "&7sonidos de habilidad y mensajes en ActionBar.",
                "",
                "&e▶ Clic para ajustar tus preferencias."
        )));

        // Slot 40: Cerrar Menú
        inv.setItem(40, PlayerGUI.createGuiItem(Material.BARRIER, "&c&lCerrar Menú", List.of("&7Cierra esta interfaz.")));

        player.openInventory(inv);
    }
}

package com.cytricos.races.gui;

import com.cytricos.races.CytricosRaces;
import com.cytricos.races.model.PlayerData;
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
 * Menú de Configuración y Ajustes Personales para CYTRICOS RPG.
 * Permite al jugador alternar opciones de confort visual y sonoro
 * (partículas, sonidos, actionbar y mensajes de cooldown) de manera persistente.
 *
 * Lead Developers: Dandan_13 & Wolfit12
 */
public class SettingsGUI {
    public static final String TITLE = ColorUtils.color("&8[&eCYTRICOS&8] &0Ajustes Personales");
    public static final Component TITLE_COMPONENT = ColorUtils.component("&8[&eCYTRICOS&8] &0Ajustes Personales");

    private final CytricosRaces plugin;

    public SettingsGUI(CytricosRaces plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE_COMPONENT);

        ItemStack bg = PlayerGUI.createGuiItem(Material.BLACK_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, bg);
        }

        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());

        // Slot 4: Cabecera
        inv.setItem(4, PlayerGUI.createGuiItem(Material.COMPARATOR, "&e&lPreferencias Personales", List.of(
                "&7Configura cómo se presentan las notificaciones,",
                "&7partículas y efectos de sonido en tu cliente.",
                "&7Los cambios se guardan de forma instantánea y persistente."
        )));

        // Slot 10: Partículas Cosméticas
        inv.setItem(10, createToggleItem(Material.FIREWORK_STAR, "&dPartículas y Auras",
                "Controla la emisión de partículas orbitales cosméticas.", data.isSettingParticles()));

        // Slot 12: Sonidos de Habilidad
        inv.setItem(12, createToggleItem(Material.NOTE_BLOCK, "&aEfectos de Sonido",
                "Controla las reproducciones de audio de habilidades y subidas de nivel.", data.isSettingSounds()));

        // Slot 14: Avisos en Barra de Acción (ActionBar)
        inv.setItem(14, createToggleItem(Material.GOLDEN_SWORD, "&6Avisos de Combate en ActionBar",
                "Muestra recargas y advertencias de vuelo sobre la barra de vida.", data.isSettingActionBar()));

        // Slot 16: Notificaciones de Cooldown en Chat
        inv.setItem(16, createToggleItem(Material.CLOCK, "&bMensajes de Cooldown en Chat",
                "Envía alertas de recarga al intentar usar habilidades.", data.isSettingCooldownMsgs()));

        // Slot 18: Volver
        inv.setItem(18, PlayerGUI.createGuiItem(Material.ARROW, "&eVolver al Menú Principal", List.of("&7Regresa al hub principal.")));

        // Slot 26: Cerrar
        inv.setItem(26, PlayerGUI.createGuiItem(Material.BARRIER, "&c&lCerrar", null));

        player.openInventory(inv);
    }

    private ItemStack createToggleItem(Material mat, String title, String desc, boolean enabled) {
        List<String> lore = new ArrayList<>();
        lore.add("&7" + desc);
        lore.add("");
        lore.add("&7Estado actual: " + (enabled ? "&a&lACTIVADO" : "&c&lDESACTIVADO"));
        lore.add("");
        lore.add("&e▶ Clic para alternar (" + (enabled ? "Desactivar" : "Activar") + ").");
        return PlayerGUI.createGuiItem(enabled ? Material.LIME_DYE : Material.GRAY_DYE, (enabled ? "&a" : "&7") + title, lore);
    }

    public void handleClick(Player player, int slot) {
        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        if (data == null) return;

        boolean changed = false;
        switch (slot) {
            case 10 -> {
                data.setSettingParticles(!data.isSettingParticles());
                changed = true;
            }
            case 12 -> {
                data.setSettingSounds(!data.isSettingSounds());
                changed = true;
            }
            case 14 -> {
                data.setSettingActionBar(!data.isSettingActionBar());
                changed = true;
            }
            case 16 -> {
                data.setSettingCooldownMsgs(!data.isSettingCooldownMsgs());
                changed = true;
            }
        }

        if (changed) {
            plugin.getPlayerDataManager().saveToFile(data);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.2f);
            open(player);
        }
    }
}

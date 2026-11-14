package com.cytricos.races.gui;

import com.cytricos.races.CytricosRaces;
import com.cytricos.races.model.Race;
import com.cytricos.races.model.RaceLevel;
import com.cytricos.races.util.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Gestor de GUIs Administrativas para Admins y Owners en CYTRICOS RPG.
 * Soporta edición por arrastre (Drag & Drop) de slots 0-44, ajuste de variables globales y niveles.
 * Lead Developers: Dandan_13 & Wolfit12
 */
public class AdminGUI {
    public static final String ADMIN_MAIN_TITLE = "§c§lAdmin Menú de Razas (Editor)";
    public static final String RACE_VARS_TITLE_PREFIX = "§c§lVariables: ";
    public static final String LEVEL_SELECT_TITLE_PREFIX = "§c§lNiveles de ";
    public static final String LEVEL_EDIT_TITLE_PREFIX = "§c§lEditando ";
    public static final String CHANGE_PANEL_TITLE = "§6§lGestión de Cambios de Raza";

    private final CytricosRaces plugin;

    public AdminGUI(CytricosRaces plugin) {
        this.plugin = plugin;
    }

    /**
     * Menú principal de administración de 45 slots con soporte para Drag & Drop y edición táctil.
     */
    public void openAdminMainMenu(Player admin) {
        Inventory inv = Bukkit.createInventory(null, 45, ADMIN_MAIN_TITLE);

        ItemStack bg = PlayerGUI.createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 45; i++) {
            inv.setItem(i, bg);
        }

        // Colocar las razas en sus slots
        for (Race race : plugin.getRaceManager().getAllRaces()) {
            ItemStack icon = createAdminRaceIcon(race);
            if (race.getSlot() >= 0 && race.getSlot() < 45) {
                inv.setItem(race.getSlot(), icon);
            }
        }

        // Slot 40: Panel de Gestión de Cambio de Raza (Lingote de Oro)
        List<String> changeLore = new ArrayList<>();
        changeLore.add(ChatColor.GRAY + "Ajustar precio de Vault ($)");
        changeLore.add(ChatColor.GRAY + "Inyectar nuevo Token de Reencarnación");
        changeLore.add(ChatColor.GRAY + "Gestionar tokens gratuitos por jugador");
        ItemStack goldIngot = PlayerGUI.createGuiItem(Material.GOLD_INGOT, ChatColor.GOLD + "§lGestión de Cambio de Raza", changeLore);
        inv.setItem(40, goldIngot);

        // Slot 44: Toggle de Pausa Global de Cambios
        boolean active = plugin.getConfigManager().isGlobalChangesEnabled();
        Material toggleMat = active ? Material.EMERALD_BLOCK : Material.REDSTONE_BLOCK;
        String toggleName = active ? ChatColor.GREEN + "§lCAMBIOS HABILITADOS" : ChatColor.RED + "§lSISTEMA PAUSADO GLOBALMENTE";
        List<String> toggleLore = List.of(
                ChatColor.GRAY + "Estado actual del sistema de cambios.",
                ChatColor.YELLOW + "Clic para conmutar (/raza admin cambios parar|iniciar)."
        );
        inv.setItem(44, PlayerGUI.createGuiItem(toggleMat, toggleName, toggleLore));

        admin.openInventory(inv);
    }

    private ItemStack createAdminRaceIcon(Race race) {
        ItemStack item = new ItemStack(race.getMaterial());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtils.color(race.getDisplayName()));
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "ID de Raza: " + ChatColor.WHITE + race.getId());
            lore.add(ChatColor.GRAY + "Slot Actual: " + ChatColor.AQUA + race.getSlot());
            lore.add(ChatColor.GRAY + "Escala Física: " + ChatColor.YELLOW + race.getScale() + "x");
            lore.add(ChatColor.GRAY + "Vida: " + ChatColor.RED + (race.getBaseHealthHearts() * 2) + " HP");
            lore.add("");
            lore.add(ChatColor.YELLOW + "▶ Clic Izquierdo: " + ChatColor.WHITE + "Ver/Editar 6 Niveles");
            lore.add(ChatColor.GOLD + "▶ Shift + Clic Izquierdo: " + ChatColor.WHITE + "Variables Globales (Escala/Atributos)");
            lore.add(ChatColor.AQUA + "▶ Arrastrar y Soltar: " + ChatColor.WHITE + "Mover a otro slot (0-44)");
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Submenú de variables globales de raza (escala +/- 0.05, vida, velocidad, daño, inmunidades).
     */
    public void openRaceVariablesEditor(Player admin, Race race) {
        Inventory inv = Bukkit.createInventory(null, 27, RACE_VARS_TITLE_PREFIX + race.getId());

        ItemStack bg = PlayerGUI.createGuiItem(Material.BLACK_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, bg);
        }

        // Slot 10: Escala -0.05 (Redstone)
        inv.setItem(10, PlayerGUI.createGuiItem(Material.REDSTONE_BLOCK, ChatColor.RED + "§l-0.05 Escala Física",
                List.of(ChatColor.GRAY + "Reduce la escala y hitbox del jugador.")));

        // Slot 11: Info de Escala actual (Cuarzo)
        inv.setItem(11, PlayerGUI.createGuiItem(Material.QUARTZ_BLOCK, ChatColor.YELLOW + "§lEscala Actual: " + race.getScale() + "x",
                List.of(ChatColor.GRAY + "Afecta a Attribute.GENERIC_SCALE en vivo.")));

        // Slot 12: Escala +0.05 (Emerald)
        inv.setItem(12, PlayerGUI.createGuiItem(Material.EMERALD_BLOCK, ChatColor.GREEN + "§l+0.05 Escala Física",
                List.of(ChatColor.GRAY + "Aumenta la escala y hitbox del jugador.")));

        // Slot 14: Vida Base (Golden Apple)
        inv.setItem(14, PlayerGUI.createGuiItem(Material.GOLDEN_APPLE, ChatColor.RED + "Vida Base: " + (race.getBaseHealthHearts() * 2) + " HP",
                List.of(ChatColor.GRAY + "Clic izquierdo: +1 corazón", ChatColor.GRAY + "Clic derecho: -1 corazón")));

        // Slot 15: Velocidad de movimiento (Pluma)
        inv.setItem(15, PlayerGUI.createGuiItem(Material.FEATHER, ChatColor.AQUA + "Velocidad: " + (int)(race.getBaseSpeedMultiplier() * 100) + "%",
                List.of(ChatColor.GRAY + "Clic izquierdo: +5%", ChatColor.GRAY + "Clic derecho: -5%")));

        // Slot 16: Multiplicador de Daño (Espada)
        inv.setItem(16, PlayerGUI.createGuiItem(Material.IRON_SWORD, ChatColor.LIGHT_PURPLE + "Daño Base: " + (int)(race.getBaseDamageMultiplier() * 100) + "%",
                List.of(ChatColor.GRAY + "Clic izquierdo: +5%", ChatColor.GRAY + "Clic derecho: -5%")));

        // Slot 18: Volver
        inv.setItem(18, PlayerGUI.createGuiItem(Material.ARROW, ChatColor.YELLOW + "Volver al Menú Admin", null));

        admin.openInventory(inv);
    }

    /**
     * Submenú de selección de los 6 niveles para edición.
     */
    public void openLevelSelectMenu(Player admin, Race race) {
        Inventory inv = Bukkit.createInventory(null, 27, LEVEL_SELECT_TITLE_PREFIX + race.getId());

        ItemStack bg = PlayerGUI.createGuiItem(Material.BLACK_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, bg);
        }

        for (int lvl = 1; lvl <= 6; lvl++) {
            int slot = 9 + lvl;
            RaceLevel rLevel = race.getLevel(lvl);
            String title = ChatColor.GOLD + "Nivel " + lvl + (rLevel != null ? ": " + rLevel.getName() : "");
            List<String> lore = new ArrayList<>();
            if (rLevel != null) {
                lore.add(ChatColor.GRAY + "Sufijo de Chat: " + ColorUtils.color(rLevel.getSubprefix()));
                lore.add(ChatColor.GRAY + "Coste Vault: $" + String.format("%.0f", rLevel.getCostMoney()));
                lore.add(ChatColor.GRAY + "Requisito: " + rLevel.getRequiredItemAmount() + "x " + rLevel.getRequiredItem().name());
            }
            lore.add("");
            lore.add(ChatColor.YELLOW + "▶ Clic para editar requisitos y coste");
            inv.setItem(slot, PlayerGUI.createGuiItem(Material.PLAYER_HEAD, title, lore));
        }

        inv.setItem(18, PlayerGUI.createGuiItem(Material.ARROW, ChatColor.YELLOW + "Volver", null));
        admin.openInventory(inv);
    }

    /**
     * Submenú de edición específica de un nivel.
     */
    public void openLevelEditor(Player admin, Race race, int level) {
        RaceLevel rLevel = race.getLevel(level);
        if (rLevel == null) return;

        Inventory inv = Bukkit.createInventory(null, 27, LEVEL_EDIT_TITLE_PREFIX + race.getId() + " Lvl " + level);

        ItemStack bg = PlayerGUI.createGuiItem(Material.BLACK_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, bg);
        }

        // Slot 10: Coste Vault -$1,000
        inv.setItem(10, PlayerGUI.createGuiItem(Material.REDSTONE, ChatColor.RED + "-$1,000 Coste Vault", null));

        // Slot 11: Coste Actual (Gold Ingot)
        inv.setItem(11, PlayerGUI.createGuiItem(Material.GOLD_INGOT, ChatColor.GOLD + "Coste Actual: $" + String.format("%.0f", rLevel.getCostMoney()),
                List.of(ChatColor.GRAY + "Precio en Vault para ascender a este nivel.")));

        // Slot 12: Coste Vault +$1,000
        inv.setItem(12, PlayerGUI.createGuiItem(Material.EMERALD, ChatColor.GREEN + "+$1,000 Coste Vault", null));

        // Slot 13: Coste Vault +$10,000
        inv.setItem(13, PlayerGUI.createGuiItem(Material.EMERALD_BLOCK, ChatColor.GREEN + "+$10,000 Coste Vault", null));

        // Slot 15: Ítem requerido actual
        Material reqMat = rLevel.getRequiredItem() == Material.AIR ? Material.BARRIER : rLevel.getRequiredItem();
        inv.setItem(15, PlayerGUI.createGuiItem(reqMat, ChatColor.YELLOW + "Requisito: " + rLevel.getRequiredItemAmount() + "x " + reqMat.name(),
                List.of(ChatColor.GRAY + "Pon un ítem en tu mano y haz clic", ChatColor.GRAY + "para reemplazar el material requerido.")));

        // Slot 18: Volver
        inv.setItem(18, PlayerGUI.createGuiItem(Material.ARROW, ChatColor.YELLOW + "Volver", null));

        admin.openInventory(inv);
    }

    /**
     * Panel de Gestión de Cambio de Raza (Admins y Owners).
     */
    public void openChangeManagementPanel(Player admin) {
        Inventory inv = Bukkit.createInventory(null, 27, CHANGE_PANEL_TITLE);

        ItemStack bg = PlayerGUI.createGuiItem(Material.BLACK_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, bg);
        }

        double cost = plugin.getConfigManager().getRaceChangeCost();

        // Slot 10: -$5,000
        inv.setItem(10, PlayerGUI.createGuiItem(Material.REDSTONE, ChatColor.RED + "-$5,000 Coste Cambio", null));

        // Slot 11: Coste Vault Actual
        inv.setItem(11, PlayerGUI.createGuiItem(Material.GOLD_BLOCK, ChatColor.GOLD + "Coste Cambio: $" + String.format("%.0f", cost),
                List.of(ChatColor.GRAY + "Precio por cambiar de raza tras consumir", ChatColor.GRAY + "el intento gratuito inicial.")));

        // Slot 12: +$5,000
        inv.setItem(12, PlayerGUI.createGuiItem(Material.EMERALD, ChatColor.GREEN + "+$5,000 Coste Cambio", null));

        // Slot 14: Definir ítem en mano como Token Oficial
        inv.setItem(14, PlayerGUI.createGuiItem(Material.NETHER_STAR, ChatColor.AQUA + "§lRegistrar Ítem en Mano como Token",
                List.of(ChatColor.GRAY + "Guarda el nombre, lore y CustomModelData", ChatColor.GRAY + "del ítem sostenido en mano principal.")));

        // Slot 15: Dar Token al Admin
        inv.setItem(15, PlayerGUI.createGuiItem(Material.CHEST, ChatColor.GREEN + "§lObtener Esencia de Reencarnación",
                List.of(ChatColor.GRAY + "Te otorga 1x del ítem oficial en tu inventario.")));

        // Slot 18: Volver
        inv.setItem(18, PlayerGUI.createGuiItem(Material.ARROW, ChatColor.YELLOW + "Volver", null));

        admin.openInventory(inv);
    }
}

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
 * Soporta edición de slots, variables globales, niveles, techo Y de transformación
 * y tasa de bajas por punto de talento.
 *
 * Lead Developers: Dandan_13 & Wolfit12
 */
public class AdminGUI {
    public static final String ADMIN_MAIN_TITLE = "§c§lAdmin Menú de Razas (Editor)";
    public static final String RACE_VARS_TITLE_PREFIX = "§c§lVariables: ";
    public static final String LEVEL_SELECT_TITLE_PREFIX = "§c§lNiveles de ";
    public static final String LEVEL_EDIT_TITLE_PREFIX = "§c§lEditando ";
    public static final String CHANGE_PANEL_TITLE = "§6§lGestión de Cambios y Reglas";

    private final CytricosRaces plugin;

    public AdminGUI(CytricosRaces plugin) {
        this.plugin = plugin;
    }

    public void openAdminMainMenu(Player admin) {
        Inventory inv = Bukkit.createInventory(null, 45, ADMIN_MAIN_TITLE);

        ItemStack bg = PlayerGUI.createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 45; i++) {
            inv.setItem(i, bg);
        }

        for (Race race : plugin.getRaceManager().getAllRaces()) {
            ItemStack icon = createAdminRaceIcon(race);
            if (race.getSlot() >= 0 && race.getSlot() < 45) {
                inv.setItem(race.getSlot(), icon);
            }
        }

        List<String> changeLore = new ArrayList<>();
        changeLore.add(ChatColor.GRAY + "Ajustar precio de Vault ($)");
        changeLore.add(ChatColor.GRAY + "Ajustar Techo Y de transformación y Bajas/Talento");
        changeLore.add(ChatColor.GRAY + "Registrar y entregar Token de Reencarnación");
        ItemStack goldIngot = PlayerGUI.createGuiItem(Material.GOLD_INGOT, ChatColor.GOLD + "§lGestión de Cambios y Reglas", changeLore);
        inv.setItem(40, goldIngot);

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
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    public void openRaceVariablesEditor(Player admin, Race race) {
        Inventory inv = Bukkit.createInventory(null, 27, RACE_VARS_TITLE_PREFIX + race.getId());

        ItemStack bg = PlayerGUI.createGuiItem(Material.BLACK_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, bg);
        }

        inv.setItem(10, PlayerGUI.createGuiItem(Material.REDSTONE_BLOCK, ChatColor.RED + "§l-0.05 Escala Física",
                List.of(ChatColor.GRAY + "Reduce la escala y hitbox del jugador.")));

        inv.setItem(11, PlayerGUI.createGuiItem(Material.QUARTZ_BLOCK, ChatColor.YELLOW + "§lEscala Actual: " + race.getScale() + "x",
                List.of(ChatColor.GRAY + "Afecta a Attribute.SCALE en vivo.")));

        inv.setItem(12, PlayerGUI.createGuiItem(Material.EMERALD_BLOCK, ChatColor.GREEN + "§l+0.05 Escala Física",
                List.of(ChatColor.GRAY + "Aumenta la escala y hitbox del jugador.")));

        inv.setItem(14, PlayerGUI.createGuiItem(Material.GOLDEN_APPLE, ChatColor.RED + "Vida Base: " + (race.getBaseHealthHearts() * 2) + " HP",
                List.of(ChatColor.GRAY + "Clic izquierdo: +1 corazón", ChatColor.GRAY + "Clic derecho: -1 corazón")));

        inv.setItem(15, PlayerGUI.createGuiItem(Material.FEATHER, ChatColor.AQUA + "Velocidad: " + (int)(race.getBaseSpeedMultiplier() * 100) + "%",
                List.of(ChatColor.GRAY + "Clic izquierdo: +5%", ChatColor.GRAY + "Clic derecho: -5%")));

        inv.setItem(16, PlayerGUI.createGuiItem(Material.IRON_SWORD, ChatColor.LIGHT_PURPLE + "Daño Base: " + (int)(race.getBaseDamageMultiplier() * 100) + "%",
                List.of(ChatColor.GRAY + "Clic izquierdo: +5%", ChatColor.GRAY + "Clic derecho: -5%")));

        inv.setItem(18, PlayerGUI.createGuiItem(Material.ARROW, ChatColor.YELLOW + "Volver al Menú Admin", null));

        admin.openInventory(inv);
    }

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

    public void openLevelEditor(Player admin, Race race, int level) {
        RaceLevel rLevel = race.getLevel(level);
        if (rLevel == null) return;

        Inventory inv = Bukkit.createInventory(null, 27, LEVEL_EDIT_TITLE_PREFIX + race.getId() + " Lvl " + level);

        ItemStack bg = PlayerGUI.createGuiItem(Material.BLACK_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, bg);
        }

        inv.setItem(10, PlayerGUI.createGuiItem(Material.REDSTONE, ChatColor.RED + "-$1,000 Coste Vault", null));
        inv.setItem(11, PlayerGUI.createGuiItem(Material.GOLD_INGOT, ChatColor.GOLD + "Coste Actual: $" + String.format("%.0f", rLevel.getCostMoney()),
                List.of(ChatColor.GRAY + "Precio en Vault para ascender a este nivel.")));
        inv.setItem(12, PlayerGUI.createGuiItem(Material.EMERALD, ChatColor.GREEN + "+$1,000 Coste Vault", null));
        inv.setItem(13, PlayerGUI.createGuiItem(Material.EMERALD_BLOCK, ChatColor.GREEN + "+$10,000 Coste Vault", null));

        Material reqMat = rLevel.getRequiredItem() == Material.AIR ? Material.BARRIER : rLevel.getRequiredItem();
        inv.setItem(15, PlayerGUI.createGuiItem(reqMat, ChatColor.YELLOW + "Requisito: " + rLevel.getRequiredItemAmount() + "x " + reqMat.name(),
                List.of(ChatColor.GRAY + "Pon un ítem en tu mano y haz clic", ChatColor.GRAY + "para reemplazar el material requerido.")));

        inv.setItem(18, PlayerGUI.createGuiItem(Material.ARROW, ChatColor.YELLOW + "Volver", null));

        admin.openInventory(inv);
    }

    public void openChangeManagementPanel(Player admin) {
        Inventory inv = Bukkit.createInventory(null, 36, CHANGE_PANEL_TITLE);

        ItemStack bg = PlayerGUI.createGuiItem(Material.BLACK_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 36; i++) {
            inv.setItem(i, bg);
        }

        double cost = plugin.getConfigManager().getRaceChangeCost();
        double maxFlightY = plugin.getConfigManager().getTransformationMaxFlightY();
        int killsPerPoint = plugin.getConfigManager().getTalentKillsPerPoint();

        // Fila 1: Coste de Cambio y Token de Reencarnación
        inv.setItem(10, PlayerGUI.createGuiItem(Material.REDSTONE, ChatColor.RED + "-$5,000 Coste Cambio", null));
        inv.setItem(11, PlayerGUI.createGuiItem(Material.GOLD_BLOCK, ChatColor.GOLD + "Coste Cambio: $" + String.format("%.0f", cost),
                List.of(ChatColor.GRAY + "Precio por cambiar de raza tras consumir", ChatColor.GRAY + "el intento gratuito inicial.")));
        inv.setItem(12, PlayerGUI.createGuiItem(Material.EMERALD, ChatColor.GREEN + "+$5,000 Coste Cambio", null));

        inv.setItem(14, PlayerGUI.createGuiItem(Material.NETHER_STAR, ChatColor.AQUA + "§lRegistrar Ítem en Mano como Token",
                List.of(ChatColor.GRAY + "Guarda el nombre, lore y CustomModelData", ChatColor.GRAY + "del ítem sostenido en mano principal.")));
        inv.setItem(15, PlayerGUI.createGuiItem(Material.CHEST, ChatColor.GREEN + "§lObtener Esencia de Reencarnación",
                List.of(ChatColor.GRAY + "Te otorga 1x del ítem oficial en tu inventario.")));

        // Fila 2: Techo de Vuelo de Transformación (Y) y Bajas por Punto de Talento
        inv.setItem(19, PlayerGUI.createGuiItem(Material.REDSTONE, ChatColor.RED + "-10m Techo Vuelo Transf.", null));
        inv.setItem(20, PlayerGUI.createGuiItem(Material.FEATHER, ChatColor.AQUA + "Techo Y Transformación: " + (int) maxFlightY,
                List.of(ChatColor.GRAY + "Altura Y máxima al volar transformado", ChatColor.GRAY + "(config.yml: transformations.max-flight-y).")));
        inv.setItem(21, PlayerGUI.createGuiItem(Material.EMERALD, ChatColor.GREEN + "+10m Techo Vuelo Transf.", null));

        inv.setItem(23, PlayerGUI.createGuiItem(Material.REDSTONE, ChatColor.RED + "-8 Bajas por Talento", null));
        inv.setItem(24, PlayerGUI.createGuiItem(Material.EXPERIENCE_BOTTLE, ChatColor.LIGHT_PURPLE + "Bajas / Talento: " + killsPerPoint,
                List.of(ChatColor.GRAY + "Enemigos necesarios para ganar", ChatColor.GRAY + "+1 Punto de Talento automático.")));
        inv.setItem(25, PlayerGUI.createGuiItem(Material.EMERALD, ChatColor.GREEN + "+8 Bajas por Talento", null));

        // Fila 3: Volver
        inv.setItem(27, PlayerGUI.createGuiItem(Material.ARROW, ChatColor.YELLOW + "Volver", null));

        admin.openInventory(inv);
    }
}

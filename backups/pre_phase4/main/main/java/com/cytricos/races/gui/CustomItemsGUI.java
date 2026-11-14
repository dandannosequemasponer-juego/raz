package com.cytricos.races.gui;

import com.cytricos.races.CytricosRaces;
import com.cytricos.races.manager.CustomItemManager;
import com.cytricos.races.manager.WingsManager;
import com.cytricos.races.util.ColorUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Catálogo Interactivo de Reliquias, Armas, Armaduras y Alas Físicas (Elytra)
 * del Resource Pack independiente de CYTRICOS RPG.
 *
 * Lead Developers: Dandan_13 & Wolfit12
 */
public class CustomItemsGUI {
    public static final String TITLE = ColorUtils.color("&8[&eCYTRICOS&8] &0Catálogo de Objetos Custom");
    public static final Component TITLE_COMPONENT = ColorUtils.component("&8[&eCYTRICOS&8] &0Catálogo de Objetos Custom");

    private final CytricosRaces plugin;

    public CustomItemsGUI(CytricosRaces plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE_COMPONENT);

        ItemStack bg = PlayerGUI.createGuiItem(Material.BLACK_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, bg);
        }

        boolean isAdmin = player.hasPermission("cytricosraces.admin");

        inv.setItem(4, PlayerGUI.createGuiItem(Material.ANVIL, "&6&lArmería y Reliquias Personalizadas", List.of(
                "&7Todos los objetos poseen modelos 3D reales en el",
                "&7Resource Pack oficial y clave PDC anti-falsificación.",
                "",
                isAdmin ? "&a▶ Modo Admin: Clic para recibir el objeto." : "&e▶ Modo Consulta: Inspección de reliquias."
        )));

        // Fila 2 (Slots 10-16) y Fila 3 (Slots 19-20): Custom Items + Esencia
        int[] itemSlots = {10, 11, 12, 13, 14, 15, 16, 19, 20};
        List<ItemStack> displayItems = new ArrayList<>();
        displayItems.add(plugin.getCustomItemManager().createChangeTokenItem());
        for (CustomItemManager.CustomItemSpec spec : plugin.getCustomItemManager().getAllCustomItems()) {
            displayItems.add(plugin.getCustomItemManager().createCustomItem(spec.id));
        }

        for (int i = 0; i < itemSlots.length && i < displayItems.length; i++) {
            ItemStack item = displayItems.get(i);
            if (item != null) {
                inv.setItem(itemSlots[i], appendHint(item, isAdmin));
            }
        }

        // Fila 4 (Slots 28-35): 8 Élitros Personalizados
        int[] wingSlots = {28, 29, 30, 31, 32, 33, 34, 37};
        int wIdx = 0;
        for (WingsManager.WingDefinition def : plugin.getWingsManager().getAllWings()) {
            if (wIdx >= wingSlots.length) break;
            ItemStack elytra = plugin.getWingsManager().createWingElytraItem(def.id);
            if (elytra != null) {
                inv.setItem(wingSlots[wIdx], appendHint(elytra, isAdmin));
            }
            wIdx++;
        }

        inv.setItem(45, PlayerGUI.createGuiItem(Material.ARROW, "&eVolver a Cosméticos", List.of("&7Regresa al menú de cosméticos y alas.")));
        inv.setItem(53, PlayerGUI.createGuiItem(Material.BARRIER, "&c&lCerrar", null));

        player.openInventory(inv);
    }

    private ItemStack appendHint(ItemStack base, boolean isAdmin) {
        ItemStack clone = base.clone();
        ItemMeta meta = clone.getItemMeta();
        if (meta != null) {
            List<Component> lore = meta.lore() != null ? new ArrayList<>(meta.lore()) : new ArrayList<>();
            lore.add(Component.empty());
            lore.add(ColorUtils.component(isAdmin ? "&a▶ Clic (Admin) para obtener en inventario." : "&7✔ Objeto verificable en el servidor."));
            meta.lore(lore);
            clone.setItemMeta(meta);
        }
        return clone;
    }

    public void handleClick(Player player, int slot) {
        if (!player.hasPermission("cytricosraces.admin")) {
            ColorUtils.sendActionBar(player, "&7Solo administradores pueden generar objetos desde el catálogo.");
            return;
        }

        int[] itemSlots = {10, 11, 12, 13, 14, 15, 16, 19, 20};
        if (slot == 10) {
            player.getInventory().addItem(plugin.getCustomItemManager().createChangeTokenItem());
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.2f);
            ColorUtils.sendMessage(player, "&aRecibido: &6Esencia de Reencarnación Ancestral");
            return;
        }
        List<CustomItemManager.CustomItemSpec> specs = new ArrayList<>(plugin.getCustomItemManager().getAllCustomItems());
        for (int i = 1; i < itemSlots.length && (i - 1) < specs.size(); i++) {
            if (itemSlots[i] == slot) {
                CustomItemManager.CustomItemSpec spec = specs.get(i - 1);
                ItemStack stack = plugin.getCustomItemManager().createCustomItem(spec.id);
                if (stack != null) {
                    player.getInventory().addItem(stack);
                    player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.2f);
                    ColorUtils.sendMessage(player, "&aRecibido: " + spec.displayName);
                }
                return;
            }
        }

        int[] wingSlots = {28, 29, 30, 31, 32, 33, 34, 37};
        List<WingsManager.WingDefinition> wings = new ArrayList<>(plugin.getWingsManager().getAllWings());
        for (int i = 0; i < wingSlots.length && i < wings.size(); i++) {
            if (wingSlots[i] == slot) {
                WingsManager.WingDefinition def = wings.get(i);
                ItemStack elytra = plugin.getWingsManager().createWingElytraItem(def.id);
                if (elytra != null) {
                    player.getInventory().addItem(elytra);
                    player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.2f);
                    ColorUtils.sendMessage(player, "&aÉlitros personalizados recibidos: " + def.displayName);
                }
                return;
            }
        }
    }
}

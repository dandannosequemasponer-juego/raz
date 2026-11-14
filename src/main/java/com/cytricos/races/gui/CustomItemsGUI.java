package com.cytricos.races.gui;

import com.cytricos.races.CytricosRaces;
import com.cytricos.races.manager.CustomItemManager;
import com.cytricos.races.manager.WingsManager;
import com.cytricos.races.model.PlayerData;
import com.cytricos.races.util.ColorUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Catálogo Interactivo Paginado de Reliquias, Armas, Cosméticos 3D, Coleccionables
 * y Élitros con Modelos Blockbench del Resource Pack oficial de CYTRICOS RPG.
 *
 * Lead Developers: Dandan_13 & Wolfit12
 */
public class CustomItemsGUI {
    public static final String TITLE = ColorUtils.color("&8[&eCYTRICOS&8] &0Catálogo de Objetos Custom");
    public static final Component TITLE_COMPONENT = ColorUtils.component("&8[&eCYTRICOS&8] &0Catálogo de Objetos Custom");

    private static final int[] GRID_SLOTS = {
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    };

    private enum FilterMode {
        ALL("Todos los Objetos"),
        FUNCIONAL("Funcionales (Armas y Amuletos)"),
        COSMETICO("Cosméticos 3D"),
        COLECCIONABLE("Coleccionables y Tokens"),
        WINGS("Élitros Físicos 3D");

        final String label;

        FilterMode(String label) {
            this.label = label;
        }
    }

    private static class ViewState {
        FilterMode filter = FilterMode.ALL;
        int page = 0;
    }

    private record CatalogEntry(String kind, String id, ItemStack item) {}

    private final CytricosRaces plugin;
    private final Map<UUID, ViewState> viewStates = new ConcurrentHashMap<>();

    public CustomItemsGUI(CytricosRaces plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        ViewState state = viewStates.computeIfAbsent(player.getUniqueId(), k -> new ViewState());
        render(player, state);
    }

    private void render(Player player, ViewState state) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE_COMPONENT);

        ItemStack border = PlayerGUI.createGuiItem(Material.BLACK_STAINED_GLASS_PANE, " ", null);
        ItemStack subtle = PlayerGUI.createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 54; i++) {
            int row = i / 9;
            inv.setItem(i, (row == 0 || row == 5) ? border : subtle);
        }

        boolean isAdmin = player.hasPermission("cytricosraces.admin");
        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());

        // Filtros superiores (Slots 1, 2, 3, 5, 6) + Cabecera (Slot 4)
        inv.setItem(1, filterIcon(Material.CHEST, "&f&lTodos", state.filter == FilterMode.ALL,
                "Explora todas las reliquias, tokens y élitros 3D."));
        inv.setItem(2, filterIcon(Material.NETHERITE_SWORD, "&a&lFuncionales", state.filter == FilterMode.FUNCIONAL,
                "Armas, herramientas y amuletos con poderes activos."));
        inv.setItem(3, filterIcon(Material.CARVED_PUMPKIN, "&b&lCosméticos 3D", state.filter == FilterMode.COSMETICO,
                "Coronas y aureolas Blockbench equipables."));

        inv.setItem(4, PlayerGUI.createGuiItem(Material.ANVIL, "&6&lArmería y Reliquias CYTRICOS", List.of(
                "&7Filtro actual: &e" + state.filter.label,
                "&7Tu Nivel Racial: &aNivel " + (data != null ? data.getLevel() : 1) + "/6",
                "&7Seguridad: &fPDC + UUID anti-duplicación",
                "",
                isAdmin ? "&a▶ Modo Admin: Clic para obtener un ejemplar único."
                        : "&e▶ Modo Catálogo: Inspecciona rareza y requisitos."
        )));

        inv.setItem(5, filterIcon(Material.GOLD_NUGGET, "&e&lColeccionables", state.filter == FilterMode.COLECCIONABLE,
                "Trofeos, monedas antiguas y Esencia de Reencarnación."));
        inv.setItem(6, filterIcon(Material.ELYTRA, "&d&lÉlitros 3D", state.filter == FilterMode.WINGS,
                "Las 16 alas físicas exclusivas de cada raza."));

        List<CatalogEntry> entries = buildEntries(player, data, state.filter, isAdmin);
        int pageSize = GRID_SLOTS.length;
        int totalPages = Math.max(1, (int) Math.ceil(entries.size() / (double) pageSize));
        if (state.page >= totalPages) state.page = totalPages - 1;
        if (state.page < 0) state.page = 0;

        int start = state.page * pageSize;
        for (int i = 0; i < GRID_SLOTS.length; i++) {
            int dataIdx = start + i;
            if (dataIdx < entries.size()) {
                inv.setItem(GRID_SLOTS[i], entries.get(dataIdx).item());
            } else {
                inv.setItem(GRID_SLOTS[i], null);
            }
        }

        inv.setItem(45, PlayerGUI.createGuiItem(Material.ARROW, "&eVolver a Cosméticos",
                List.of("&7Regresa al menú de alas y auras.")));

        if (state.page > 0) {
            inv.setItem(47, PlayerGUI.createGuiItem(Material.SPECTRAL_ARROW, "&a◀ Página Anterior",
                    List.of("&7Ir a la página " + state.page + " de " + totalPages)));
        }

        inv.setItem(49, PlayerGUI.createGuiItem(Material.PAPER, "&b&lPágina " + (state.page + 1) + " de " + totalPages,
                List.of("&7Total mostrado: &e" + entries.size() + " artículos.")));

        if (state.page + 1 < totalPages) {
            inv.setItem(51, PlayerGUI.createGuiItem(Material.SPECTRAL_ARROW, "&aPágina Siguiente ▶",
                    List.of("&7Ir a la página " + (state.page + 2) + " de " + totalPages)));
        }

        inv.setItem(53, PlayerGUI.createGuiItem(Material.BARRIER, "&c&lCerrar", null));

        player.openInventory(inv);
    }

    private ItemStack filterIcon(Material mat, String title, boolean active, String desc) {
        return PlayerGUI.createGuiItem(mat, (active ? "&a★ " : "") + title, List.of(
                "&7" + desc,
                "",
                active ? "&a✔ Filtro activo" : "&e▶ Clic para filtrar"
        ));
    }

    private List<CatalogEntry> buildEntries(Player player, PlayerData data, FilterMode filter, boolean isAdmin) {
        List<CatalogEntry> entries = new ArrayList<>();

        if (filter == FilterMode.ALL || filter == FilterMode.COLECCIONABLE) {
            ItemStack token = plugin.getCustomItemManager().createChangeTokenItem();
            if (token != null) {
                entries.add(new CatalogEntry("TOKEN", "reincarnation_essence",
                        appendHint(token, isAdmin, true, "&a✔ Token administrativo transferible.")));
            }
        }

        if (filter == FilterMode.ALL || filter == FilterMode.FUNCIONAL
                || filter == FilterMode.COSMETICO || filter == FilterMode.COLECCIONABLE) {
            for (CustomItemManager.CustomItemSpec spec : plugin.getCustomItemManager().getAllCustomItems()) {
                if (filter == FilterMode.FUNCIONAL && spec.category != CustomItemManager.ItemCategory.FUNCIONAL) continue;
                if (filter == FilterMode.COSMETICO && spec.category != CustomItemManager.ItemCategory.COSMETICO) continue;
                if (filter == FilterMode.COLECCIONABLE && spec.category != CustomItemManager.ItemCategory.COLECCIONABLE) continue;

                ItemStack stack = plugin.getCustomItemManager().createCustomItem(spec.id);
                if (stack != null) {
                    boolean meets = plugin.getCustomItemManager().meetsItemRequirements(player, data, spec);
                    String statusLine = meets
                            ? "&a✔ Cumples el requisito de uso (Nivel " + spec.requiredLevel + "+)."
                            : "&c✖ Bloqueado para uso (Requiere Nivel " + spec.requiredLevel + "+).";
                    entries.add(new CatalogEntry("ITEM", spec.id, appendHint(stack, isAdmin, meets, statusLine)));
                }
            }
        }

        if (filter == FilterMode.ALL || filter == FilterMode.WINGS) {
            for (WingsManager.WingDefinition def : plugin.getWingsManager().getAllWings()) {
                ItemStack elytra = plugin.getWingsManager().createWingElytraItem(def.id);
                if (elytra != null) {
                    boolean unlocked = plugin.getWingsManager().isWingUnlocked(player, data, def);
                    String statusLine = unlocked
                            ? "&a✔ Alas desbloqueadas en tu colección."
                            : "&eℹ Desbloquea estas alas desde el menú de Cosméticos.";
                    entries.add(new CatalogEntry("WING", def.id, appendHint(elytra, isAdmin, unlocked, statusLine)));
                }
            }
        }

        return entries;
    }

    private ItemStack appendHint(ItemStack base, boolean isAdmin, boolean unlocked, String statusLine) {
        ItemStack clone = base.clone();
        ItemMeta meta = clone.getItemMeta();
        if (meta != null) {
            List<Component> lore = meta.lore() != null ? new ArrayList<>(meta.lore()) : new ArrayList<>();
            lore.add(Component.empty());
            lore.add(ColorUtils.component(statusLine));
            lore.add(ColorUtils.component(isAdmin
                    ? "&a▶ Clic (Admin) para recibir en tu inventario."
                    : (unlocked ? "&7✔ Verificado con clave PDC oficial." : "&8🔒 Inspección informativa.")));
            meta.lore(lore);
            clone.setItemMeta(meta);
        }
        return clone;
    }

    public void handleClick(Player player, int slot) {
        ViewState state = viewStates.computeIfAbsent(player.getUniqueId(), k -> new ViewState());

        if (slot == 1) { state.filter = FilterMode.ALL; state.page = 0; render(player, state); return; }
        if (slot == 2) { state.filter = FilterMode.FUNCIONAL; state.page = 0; render(player, state); return; }
        if (slot == 3) { state.filter = FilterMode.COSMETICO; state.page = 0; render(player, state); return; }
        if (slot == 5) { state.filter = FilterMode.COLECCIONABLE; state.page = 0; render(player, state); return; }
        if (slot == 6) { state.filter = FilterMode.WINGS; state.page = 0; render(player, state); return; }

        if (slot == 47 && state.page > 0) {
            state.page--;
            player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.9f, 1.0f);
            render(player, state);
            return;
        }
        if (slot == 51) {
            state.page++;
            player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.9f, 1.1f);
            render(player, state);
            return;
        }

        int gridIdx = -1;
        for (int i = 0; i < GRID_SLOTS.length; i++) {
            if (GRID_SLOTS[i] == slot) {
                gridIdx = i;
                break;
            }
        }
        if (gridIdx == -1) return;

        if (!player.hasPermission("cytricosraces.admin")) {
            ColorUtils.sendActionBar(player, "&eModo catálogo: solo administradores pueden generar reliquias aquí.");
            return;
        }

        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        List<CatalogEntry> entries = buildEntries(player, data, state.filter, true);
        int absIdx = (state.page * GRID_SLOTS.length) + gridIdx;
        if (absIdx >= entries.size()) return;

        CatalogEntry selected = entries.get(absIdx);
        switch (selected.kind()) {
            case "TOKEN" -> {
                plugin.getCustomItemManager().giveItemSafely(player, plugin.getCustomItemManager().createChangeTokenItem());
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.2f);
                ColorUtils.sendMessage(player, "&aRecibido: &6Esencia de Reencarnación Ancestral");
            }
            case "ITEM" -> {
                ItemStack item = plugin.getCustomItemManager().createCustomItem(selected.id());
                if (item != null) {
                    plugin.getCustomItemManager().giveItemSafely(player, item);
                    player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.2f);
                    CustomItemManager.CustomItemSpec spec = plugin.getCustomItemManager().getSpec(selected.id());
                    ColorUtils.sendMessage(player, "&aRecibido: " + (spec != null ? spec.displayName : selected.id()));
                }
            }
            case "WING" -> {
                ItemStack elytra = plugin.getWingsManager().createWingElytraItem(selected.id());
                if (elytra != null) {
                    plugin.getCustomItemManager().giveItemSafely(player, elytra);
                    player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.2f);
                    WingsManager.WingDefinition def = plugin.getWingsManager().getWing(selected.id());
                    ColorUtils.sendMessage(player, "&aÉlitros personalizados recibidos: " + (def != null ? def.displayName : selected.id()));
                }
            }
        }
    }
}

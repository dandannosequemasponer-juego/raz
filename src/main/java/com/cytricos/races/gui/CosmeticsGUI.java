package com.cytricos.races.gui;

import com.cytricos.races.CytricosRaces;
import com.cytricos.races.manager.CosmeticsManager;
import com.cytricos.races.manager.HeadCosmeticManager;
import com.cytricos.races.manager.WingsManager;
import com.cytricos.races.model.PlayerData;
import com.cytricos.races.util.ColorUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Menú de Cosméticos, 16 Alas 3D Exclusivas, Coronas/Aureolas Flotantes y Auras para CYTRICOS RPG.
 * - Alas 3D exclusivas de raza (cero compartidas).
 * - Coronas y Aureolas flotantes desacopladas del slot de casco físico.
 * - Botón de alternancia de visibilidad en 1ra persona (ocultas en 1ra persona vs visibles en F5/terceros).
 *
 * Lead Developers: Dandan_13 & Wolfit12
 */
public class CosmeticsGUI {
    public static final String TITLE = ColorUtils.color("&8[&eCYTRICOS&8] &0Cosméticos y Alas 3D");
    public static final Component TITLE_COMPONENT = ColorUtils.component("&8[&eCYTRICOS&8] &0Cosméticos y Alas 3D");

    private static final int[] GRID_SLOTS = {
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    };

    private enum Tab {
        WINGS,
        HEAD_COSMETICS,
        AURAS
    }

    private static class ViewState {
        Tab tab = Tab.WINGS;
        int page = 0;
        boolean onlyUnlocked = false;
    }

    private final CytricosRaces plugin;
    private final Map<UUID, ViewState> viewStates = new ConcurrentHashMap<>();

    public CosmeticsGUI(CytricosRaces plugin) {
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

        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        String activeAura = data != null ? data.getSelectedCosmeticAura().toLowerCase() : "none";
        String activeWings = data != null ? data.getSelectedWings().toLowerCase() : "none";
        String activeHead = data != null ? data.getSelectedHeadCosmetic().toLowerCase() : "none";
        boolean hideFirstPerson = data == null || data.isFirstPersonWingsHidden();

        List<WingsManager.WingDefinition> allWings = new ArrayList<>(plugin.getWingsManager().getAllWings());
        List<HeadCosmeticManager.HeadCosmeticDef> allHeads = new ArrayList<>(plugin.getHeadCosmeticManager().getAllHeadCosmetics());
        List<CosmeticsManager.AuraDefinition> allAuras = new ArrayList<>(plugin.getCosmeticsManager().getAllAuras());

        // Barra superior (0-8)
        inv.setItem(1, PlayerGUI.createGuiItem(Material.ELYTRA,
                (state.tab == Tab.WINGS ? "&a▶ " : "&7") + "&b&lAlas 3D (16 Razas)",
                List.of(
                        "&716 pares exclusivos de cada raza.",
                        "&cSolo puedes equipar las alas de tu raza activa.",
                        "",
                        state.tab == Tab.WINGS ? "&a✔ Pestaña activa" : "&e▶ Clic para ver alas 3D"
                )));

        inv.setItem(2, PlayerGUI.createGuiItem(Material.GOLDEN_HELMET,
                (state.tab == Tab.HEAD_COSMETICS ? "&a▶ " : "&7") + "&e&lCoronas y Aureolas 3D",
                List.of(
                        "&7Accesorios flotantes sobre tu cabeza.",
                        "&a✔ No ocupan ni reemplazan tu casco real.",
                        "",
                        state.tab == Tab.HEAD_COSMETICS ? "&a✔ Pestaña activa" : "&e▶ Clic para ver accesorios de cabeza"
                )));

        inv.setItem(3, PlayerGUI.createGuiItem(Material.COMPASS,
                "&e&lFiltro de Colección",
                List.of(
                        "&7Mostrando: " + (state.onlyUnlocked ? "&aSolo Desbloqueados" : "&fToda la Colección"),
                        "",
                        "&e▶ Clic para alternar filtro"
                )));

        inv.setItem(4, PlayerGUI.createGuiItem(Material.AMETHYST_SHARD, "&d&lCentro Cosmético", List.of(
                "&7Alas: &b" + ("none".equals(activeWings) ? "Ninguna" : activeWings.toUpperCase()),
                "&7Cabeza: &e" + ("none".equals(activeHead) ? "Ninguna" : activeHead.toUpperCase()),
                "&7Aura: &6" + ("none".equals(activeAura) ? "Ninguna" : activeAura.toUpperCase()),
                "&7Raza: &f" + (data != null && data.hasAssignedRace() ? data.getRace().toUpperCase() : "Sin Raza")
        )));

        String unequipLabel = switch (state.tab) {
            case WINGS -> "&c&lDesequipar Alas";
            case HEAD_COSMETICS -> "&c&lDesequipar Corona / Aureola";
            case AURAS -> "&c&lDesequipar Aura";
        };
        inv.setItem(5, PlayerGUI.createGuiItem(Material.RED_DYE, unequipLabel,
                List.of("&7Retira el cosmético activo de esta categoría.")));

        inv.setItem(6, PlayerGUI.createGuiItem(Material.BLAZE_POWDER,
                (state.tab == Tab.AURAS ? "&a▶ " : "&7") + "&6&lAuras de Partículas",
                List.of(
                        "&7Efectos orbitales ligeros y configurables.",
                        "",
                        state.tab == Tab.AURAS ? "&a✔ Pestaña activa" : "&e▶ Clic para ver auras"
                )));

        inv.setItem(8, PlayerGUI.createGuiItem(hideFirstPerson ? Material.ENDER_EYE : Material.SPYGLASS,
                "&b&lVista en 1ra Persona: " + (hideFirstPerson ? "&aOCULTA" : "&eVISIBLE"),
                List.of(
                        "&7Controla si ves tus propias alas y coronas",
                        "&7mientras juegas en primera persona.",
                        "&7(En F5 y otros jugadores siempre son visibles).",
                        "",
                        "&e▶ Clic para alternar visibilidad en 1ra persona"
                )));

        // Contenido paginado (14 slots)
        List<ItemStack> entries = switch (state.tab) {
            case WINGS -> buildWingIcons(player, data, allWings, activeWings, state.onlyUnlocked);
            case HEAD_COSMETICS -> buildHeadIcons(player, data, allHeads, activeHead, state.onlyUnlocked);
            case AURAS -> buildAuraIcons(player, data, allAuras, activeAura, state.onlyUnlocked);
        };

        int pageSize = GRID_SLOTS.length;
        int totalPages = Math.max(1, (int) Math.ceil(entries.size() / (double) pageSize));
        if (state.page >= totalPages) state.page = totalPages - 1;
        if (state.page < 0) state.page = 0;

        int start = state.page * pageSize;
        for (int i = 0; i < GRID_SLOTS.length; i++) {
            int dataIdx = start + i;
            if (dataIdx < entries.size()) {
                inv.setItem(GRID_SLOTS[i], entries.get(dataIdx));
            } else {
                inv.setItem(GRID_SLOTS[i], null);
            }
        }

        inv.setItem(45, PlayerGUI.createGuiItem(Material.ARROW, "&eVolver al Menú Principal",
                List.of("&7Regresa al hub principal de razas.")));

        if (state.page > 0) {
            inv.setItem(47, PlayerGUI.createGuiItem(Material.SPECTRAL_ARROW, "&a◀ Página Anterior",
                    List.of("&7Ir a la página " + state.page + " de " + totalPages)));
        }

        inv.setItem(49, PlayerGUI.createGuiItem(Material.NETHERITE_SWORD, "&6&lCatálogo de Reliquias y Objetos",
                List.of("&7Explora reliquias, armas, fragmentos", "&7y élitros físicos 3D del Resource Pack.")));

        inv.setItem(50, PlayerGUI.createGuiItem(Material.PAINTING, "&a&lInstalar Resource Pack Oficial",
                List.of("&7Envía o actualiza el Resource Pack oficial", "&7con modelos 3D de alas, coronas y reliquias.")));

        if (state.page + 1 < totalPages) {
            inv.setItem(51, PlayerGUI.createGuiItem(Material.SPECTRAL_ARROW, "&aPágina Siguiente ▶",
                    List.of("&7Ir a la página " + (state.page + 2) + " de " + totalPages)));
        }

        inv.setItem(53, PlayerGUI.createGuiItem(Material.BARRIER, "&c&lCerrar", null));

        player.openInventory(inv);
    }

    private List<ItemStack> buildWingIcons(Player player, PlayerData data, List<WingsManager.WingDefinition> wings,
                                           String activeWings, boolean onlyUnlocked) {
        List<ItemStack> list = new ArrayList<>();
        boolean isAdmin = player.hasPermission("cytricosraces.admin");

        for (WingsManager.WingDefinition def : wings) {
            boolean matchesRace = plugin.getWingsManager().isRaceExclusiveMatch(data, def);
            boolean unlocked = plugin.getWingsManager().isWingUnlocked(player, data, def);
            if (onlyUnlocked && !unlocked) continue;

            boolean equipped = activeWings.equalsIgnoreCase(def.id);
            List<String> lore = new ArrayList<>();
            lore.add("&7" + def.description);
            lore.add("&7Rareza: " + def.rarity.label + " &8| &7Exclusiva de: &e" + def.spanishRaceName);
            lore.add("&8Modelo 3D: &fcytricos:" + def.id + " &7(CMD " + def.customModelData + ")");
            lore.add("");

            if (!matchesRace) {
                lore.add("&c✖ BLOQUEADA PARA TU RAZA");
                lore.add("&7Solo la raza &e" + def.spanishRaceName + " &7puede usar estas alas.");
            } else if (equipped) {
                lore.add("&a✔ ALAS EQUIPADAS ACTUALMENTE");
                lore.add("&c▶ Clic izquierdo para desequipar.");
            } else if (unlocked) {
                lore.add("&a✔ DISPONIBLE EN TU COLECCIÓN");
                lore.add("&e▶ Clic izquierdo para equipar alas animadas.");
            } else {
                lore.add("&c✖ COSMÉTICO BLOQUEADO");
                if (def.unlockCost > 0 && plugin.getConfig().getBoolean("cosmetics_settings.wings.allow_vault_unlock", true)) {
                    lore.add("&6▶ Clic izquierdo para desbloquear por &e$" + String.format("%.0f", def.unlockCost)
                            + " &6(o Nivel " + def.requiredLevel + "+).");
                } else {
                    lore.add("&7Alcanza el &eNivel " + def.requiredLevel + " &7para desbloquear.");
                }
            }
            if (isAdmin) {
                lore.add("&b▶ Clic derecho (Admin): Recibir Élitros 3D físicos.");
            }

            ItemStack icon = new ItemStack(def.iconMaterial);
            ItemMeta meta = icon.getItemMeta();
            if (meta != null) {
                String prefix = equipped ? "&a★ " : (unlocked ? "" : "&8🔒 ");
                meta.displayName(ColorUtils.component(prefix + def.displayName));
                meta.setCustomModelData(def.customModelData);
                meta.lore(ColorUtils.componentList(lore));
                meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
                icon.setItemMeta(meta);
            }
            list.add(icon);
        }
        return list;
    }

    private List<ItemStack> buildHeadIcons(Player player, PlayerData data,
                                           List<HeadCosmeticManager.HeadCosmeticDef> heads,
                                           String activeHead, boolean onlyUnlocked) {
        List<ItemStack> list = new ArrayList<>();
        for (HeadCosmeticManager.HeadCosmeticDef def : heads) {
            boolean unlocked = plugin.getHeadCosmeticManager().isUnlocked(player, data, def);
            if (onlyUnlocked && !unlocked) continue;

            boolean equipped = activeHead.equalsIgnoreCase(def.id);
            List<String> lore = new ArrayList<>();
            lore.add("&7" + def.description);
            lore.add("&7Rareza: " + def.rarity.label);
            lore.add("&a✔ Flota sobre tu cabeza sin ocupar el slot de casco.");
            lore.add("");
            if (equipped) {
                lore.add("&a✔ ACCESORIO EQUIPADO ACTUALMENTE");
                lore.add("&c▶ Clic izquierdo para retirar.");
            } else if (unlocked) {
                lore.add("&a✔ DISPONIBLE EN TU COLECCIÓN");
                lore.add("&e▶ Clic izquierdo para equipar.");
            } else {
                lore.add("&c✖ Requiere Nivel 3+ o $" + String.format("%.0f", def.unlockCost));
                lore.add("&6▶ Clic izquierdo para desbloquear por $" + String.format("%.0f", def.unlockCost));
            }
            ItemStack icon = new ItemStack(def.iconMaterial);
            ItemMeta meta = icon.getItemMeta();
            if (meta != null) {
                meta.displayName(ColorUtils.component((equipped ? "&a★ " : (unlocked ? "" : "&8🔒 ")) + def.displayName));
                meta.setCustomModelData(def.customModelData);
                meta.lore(ColorUtils.componentList(lore));
                meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
                icon.setItemMeta(meta);
            }
            list.add(icon);
        }
        return list;
    }

    private List<ItemStack> buildAuraIcons(Player player, PlayerData data, List<CosmeticsManager.AuraDefinition> auras,
                                           String activeAura, boolean onlyUnlocked) {
        List<ItemStack> list = new ArrayList<>();
        for (CosmeticsManager.AuraDefinition def : auras) {
            boolean unlocked = plugin.getCosmeticsManager().isAuraUnlocked(player, data, def);
            if (onlyUnlocked && !unlocked) continue;

            boolean equipped = activeAura.equalsIgnoreCase(def.id);
            List<String> lore = new ArrayList<>();
            lore.add("&7" + def.description);
            if (!"none".equalsIgnoreCase(def.id)) {
                lore.add("&7Rareza: " + def.rarity.label);
                lore.add("&7Requisito: " + (unlocked
                        ? "&a✔ Desbloqueada"
                        : "&c✖ Nivel " + def.requiredLevel + "+ o $" + String.format("%.0f", def.unlockCost)));
            }
            lore.add("");
            if (equipped) {
                lore.add("&a✔ AURA EQUIPADA ACTUALMENTE");
            } else if (unlocked) {
                lore.add("&e▶ Clic para equipar este efecto orbital.");
            } else {
                lore.add("&6▶ Clic para desbloquear por &e$" + String.format("%.0f", def.unlockCost) + "&6.");
            }

            list.add(PlayerGUI.createGuiItem(def.icon, (equipped ? "&a★ " : (unlocked ? "" : "&8🔒 ")) + def.displayName, lore));
        }
        return list;
    }

    public void handleClick(Player player, int slot, boolean isRightClick) {
        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        if (data == null) return;
        ViewState state = viewStates.computeIfAbsent(player.getUniqueId(), k -> new ViewState());

        if (slot == 1) {
            state.tab = Tab.WINGS;
            state.page = 0;
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.2f);
            render(player, state);
            return;
        }
        if (slot == 2) {
            state.tab = Tab.HEAD_COSMETICS;
            state.page = 0;
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.2f);
            render(player, state);
            return;
        }
        if (slot == 6) {
            state.tab = Tab.AURAS;
            state.page = 0;
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.2f);
            render(player, state);
            return;
        }
        if (slot == 3) {
            state.onlyUnlocked = !state.onlyUnlocked;
            state.page = 0;
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.0f);
            render(player, state);
            return;
        }
        if (slot == 5) {
            switch (state.tab) {
                case WINGS -> {
                    plugin.getWingsManager().equipCosmeticWings(player, "none");
                    ColorUtils.sendMessage(player, "&7Alas cosméticas desactivadas.");
                }
                case HEAD_COSMETICS -> {
                    plugin.getHeadCosmeticManager().equipHeadCosmetic(player, "none");
                    ColorUtils.sendMessage(player, "&7Accesorio craneal desactivado.");
                }
                case AURAS -> {
                    data.setSelectedCosmeticAura("none");
                    plugin.getPlayerDataManager().saveToFile(data);
                    ColorUtils.sendMessage(player, "&7Aura cosmética desactivada.");
                }
            }
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 0.8f);
            render(player, state);
            return;
        }
        if (slot == 8) {
            boolean nowHidden = plugin.getWingsManager().toggleFirstPersonVisibility(player);
            if (plugin.getHeadCosmeticManager() != null) {
                plugin.getHeadCosmeticManager().applyFirstPersonState(player);
            }
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.1f);
            ColorUtils.sendMessage(player, nowHidden
                    ? "&b&l[COSMÉTICOS] &7Vista en 1ra persona: &aOculta &7(sigue visible en F5 y para otros)."
                    : "&b&l[COSMÉTICOS] &7Vista en 1ra persona: &eVisible&7.");
            render(player, state);
            return;
        }
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
        if (slot == 49) {
            plugin.getCustomItemsGUI().open(player);
            return;
        }
        if (slot == 50) {
            player.closeInventory();
            plugin.sendConfiguredResourcePack(player);
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

        int absoluteIdx = (state.page * GRID_SLOTS.length) + gridIdx;

        switch (state.tab) {
            case WINGS -> {
                List<WingsManager.WingDefinition> filtered = new ArrayList<>();
                for (WingsManager.WingDefinition w : plugin.getWingsManager().getAllWings()) {
                    if (!state.onlyUnlocked || plugin.getWingsManager().isWingUnlocked(player, data, w)) {
                        filtered.add(w);
                    }
                }
                if (absoluteIdx >= filtered.size()) return;

                WingsManager.WingDefinition def = filtered.get(absoluteIdx);
                if (isRightClick && player.hasPermission("cytricosraces.admin")) {
                    ItemStack elytra = plugin.getWingsManager().createWingElytraItem(def.id);
                    if (elytra != null) {
                        plugin.getCustomItemManager().giveItemSafely(player, elytra);
                        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.2f);
                        ColorUtils.sendMessage(player, "&aÉlitros personalizados entregados: " + def.displayName);
                    }
                    return;
                }

                if (!plugin.getWingsManager().isRaceExclusiveMatch(data, def)) {
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 1.0f);
                    ColorUtils.sendMessage(player, "&cEstas alas pertenecen exclusivamente a la raza &e" + def.spanishRaceName + "&c.");
                    return;
                }

                if (!plugin.getWingsManager().isWingUnlocked(player, data, def)) {
                    if (!plugin.getWingsManager().tryUnlockWithVault(player, data, def)) {
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 1.0f);
                        return;
                    }
                }

                if (data.getSelectedWings().equalsIgnoreCase(def.id)) {
                    plugin.getWingsManager().equipCosmeticWings(player, "none");
                    ColorUtils.sendMessage(player, "&7Has retirado tus alas cosméticas.");
                } else if (plugin.getWingsManager().equipCosmeticWings(player, def.id)) {
                    player.playSound(player.getLocation(), Sound.ENTITY_PHANTOM_FLAP, 1.0f, 1.2f);
                    ColorUtils.sendMessage(player, "&aAlas cosméticas animadas equipadas: " + def.displayName);
                }
                render(player, state);
            }
            case HEAD_COSMETICS -> {
                List<HeadCosmeticManager.HeadCosmeticDef> filtered = new ArrayList<>();
                for (HeadCosmeticManager.HeadCosmeticDef h : plugin.getHeadCosmeticManager().getAllHeadCosmetics()) {
                    if (!state.onlyUnlocked || plugin.getHeadCosmeticManager().isUnlocked(player, data, h)) {
                        filtered.add(h);
                    }
                }
                if (absoluteIdx >= filtered.size()) return;

                HeadCosmeticManager.HeadCosmeticDef def = filtered.get(absoluteIdx);
                if (!plugin.getHeadCosmeticManager().isUnlocked(player, data, def)) {
                    if (!plugin.getHeadCosmeticManager().tryUnlockWithVault(player, data, def)) {
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 1.0f);
                        return;
                    }
                }

                plugin.getHeadCosmeticManager().toggleHeadCosmetic(player, def.id);
                player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_GOLD, 1.0f, 1.2f);
                render(player, state);
            }
            case AURAS -> {
                List<CosmeticsManager.AuraDefinition> filtered = new ArrayList<>();
                for (CosmeticsManager.AuraDefinition a : plugin.getCosmeticsManager().getAllAuras()) {
                    if (!state.onlyUnlocked || plugin.getCosmeticsManager().isAuraUnlocked(player, data, a)) {
                        filtered.add(a);
                    }
                }
                if (absoluteIdx >= filtered.size()) return;

                CosmeticsManager.AuraDefinition def = filtered.get(absoluteIdx);
                if (!plugin.getCosmeticsManager().isAuraUnlocked(player, data, def)) {
                    if (!plugin.getCosmeticsManager().tryUnlockAuraWithVault(player, data, def)) {
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 1.0f);
                        return;
                    }
                }

                data.setSelectedCosmeticAura(def.id);
                plugin.getPlayerDataManager().saveToFile(data);
                player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.2f);
                if (!"none".equalsIgnoreCase(def.id)) {
                    player.getWorld().spawnParticle(Particle.GLOW, player.getLocation().add(0, 1.2, 0), 10, 0.4, 0.4, 0.4, 0.05);
                    ColorUtils.sendMessage(player, "&aAura cosmética equipada: " + def.displayName);
                } else {
                    ColorUtils.sendMessage(player, "&7Aura cosmética desactivada.");
                }
                render(player, state);
            }
        }
    }
}

package com.cytricos.races.gui;

import com.cytricos.races.CytricosRaces;
import com.cytricos.races.model.PlayerData;
import com.cytricos.races.model.Race;
import com.cytricos.races.model.RaceLevel;
import com.cytricos.races.util.ColorUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Interfaz gráfica de selección de razas y progresión de niveles para CYTRICOS RPG.
 * GUI de 5 filas (45 slots) con fondo de paneles de cristal negro.
 *
 * Mapeo Oficial de Slots (45 slots, 0 a 44):
 * Fila 0 (0-8): 3 Enano (anvil), 4 Humano (player_head), 5 Elfo (bow)
 * Fila 1 (9-17): 11 Gigante (mace), 12 Orco (piglin_head), 13 Duende (green_bundle), 14 Demonio (end_crystal), 15 Ángel (nether_star)
 * Fila 2 (18-26): 19 Dragonkin (dragon_head), 20 Beastkin (totem_of_undying), 21 Werewolf (wolf_spawn_egg), 22 Vampire (redstone), 23 Undead (wither_skeleton_skull), 24 Druid (golden_apple), 25 Fairy (prismarine_crystals)
 * Fila 3 (27-35): 31 Yokai (soul_lantern)
 * Fila 4 (36-44): 36 Datos Jugador (player_head), 44 Botón Confirmar / Cambiar Raza (lime_wool)
 *
 * REGLAS V7.0:
 * - Al hacer clic en el Slot 44 (Lana Verde), NUNCA cambia la raza de inmediato: abre RaceChangeConfirmGUI.
 * - Al hacer clic en otra raza, abre RaceChangeConfirmGUI con la raza preseleccionada.
 * - En la GUI de progresión: El Nivel 1 muestra claramente las desventajas con viñetas (&c- ...).
 *   Los niveles 2 al 6 OCULTAN COMPLETAMENTE el bloque de desventajas y muestran solo ventajas, estadísticas y habilidades.
 *
 * Lead Developers: Dandan_13 & Wolfit12
 */
public class PlayerGUI {
    public static final String SELECTION_TITLE = ColorUtils.color("&8[&eCYTRICOS&8] &0Selección de Raza");
    public static final Component SELECTION_TITLE_COMPONENT = ColorUtils.component("&8[&eCYTRICOS&8] &0Selección de Raza");

    public static final String CONFIRM_TITLE = RaceChangeConfirmGUI.TITLE_LEGACY;
    public static final String PROGRESS_TITLE_PREFIX = ColorUtils.color("&8[&eCYTRICOS&8] &0Progresión: ");

    private final CytricosRaces plugin;

    public PlayerGUI(CytricosRaces plugin) {
        this.plugin = plugin;
    }

    public void openConfirmationMenu(Player player, String targetRaceId) {
        plugin.getRaceChangeConfirmGUI().open(player, targetRaceId);
    }

    /**
     * Despliega la GUI interactiva principal de 45 casillas (5 filas).
     */
    public void openSelectionMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 45, SELECTION_TITLE_COMPONENT);

        // Fondo: paneles de cristal negro en todas las casillas no asignadas
        ItemStack bg = createGuiItem(Material.BLACK_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 45; i++) {
            inv.setItem(i, bg);
        }

        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        String currentRaceId = data.getRace();

        // Cargar las 16 razas en sus slots correspondientes
        for (Race race : plugin.getRaceManager().getAllRaces()) {
            boolean isCurrent = race.getId().equalsIgnoreCase(currentRaceId);
            ItemStack icon = createRaceIcon(race, isCurrent, data.getLevel());
            if (race.getSlot() >= 0 && race.getSlot() < 45) {
                inv.setItem(race.getSlot(), icon);
            }
        }

        // Slot 36: Cabeza del jugador con datos de raza
        ItemStack playerHead = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) playerHead.getItemMeta();
        if (skullMeta != null) {
            skullMeta.setOwningPlayer(player);
            skullMeta.displayName(ColorUtils.component("&e&lDatos de tu Raza"));
            List<String> lore = new ArrayList<>();
            lore.add("&7Jugador: &b" + player.getName());
            Race curRace = plugin.getRaceManager().getRace(currentRaceId);
            String raceDisplay = curRace != null ? curRace.getDisplayName() : "&fNinguna";
            lore.add("&7Raza actual: " + raceDisplay);
            lore.add("&7Nivel actual: &6Nivel " + data.getLevel() + " / 6");
            lore.add("&7Escala física: &a" + (curRace != null ? curRace.getScale() : 1.0) + "x");
            lore.add("&7Cambio gratis: " + (data.hasFreeRaceChangeAvailable() ? "&aDisponible" : "&cAgotado"));
            if (data.isFlightBanned()) {
                long sec = data.getFlightBanRemainingSeconds();
                lore.add("&c&lVuelo restringido: &e" + String.format("%02d:%02d", sec / 60, sec % 60));
            }
            if (plugin.isVaultAvailable()) {
                lore.add("&7Dinero en Vault: &e$" + String.format("%.0f", plugin.getVaultEconomy().getBalance(player)));
            }
            lore.add("");
            lore.add("&8IP: PLAY.CYTRICOS.COM");
            skullMeta.lore(ColorUtils.componentList(lore));
            for (ItemFlag flag : ItemFlag.values()) {
                skullMeta.addItemFlags(flag);
            }
            playerHead.setItemMeta(skullMeta);
        }
        inv.setItem(36, playerHead);

        // Slot 40: Botón de regreso al Hub Principal RPG
        inv.setItem(40, createGuiItem(Material.COMPASS, "&6&lMenú Principal RPG", List.of(
                "&7Abre el panel completo de tu personaje:",
                "&7Perfil, Misiones, Talentos, Logros y Alas.",
                "",
                "&e▶ Clic para abrir el Menú Principal."
        )));

        // Slot 44: Botón de confirmación / cambio de raza (lime_wool)
        List<String> confirmLore = new ArrayList<>();
        confirmLore.add("&7Haz clic aquí para abrir el menú");
        confirmLore.add("&7de pago y confirmación de cambio.");
        confirmLore.add("");
        if (data.hasFreeRaceChangeAvailable()) {
            confirmLore.add("&a✔ Dispones de 1 Cambio Gratuito");
        } else {
            double cost = plugin.getConfigManager().getRaceChangeCost();
            confirmLore.add("&fCoste Vault: &e$" + String.format("%.0f", cost));
            confirmLore.add("&fO entrega: &6Esencia de Reencarnación");
        }
        confirmLore.add("");
        confirmLore.add("&e▶ Clic para abrir el submenú de confirmación.");
        ItemStack confirmBtn = createGuiItem(Material.LIME_WOOL, "&a&lCambiar raza", confirmLore);
        inv.setItem(44, confirmBtn);

        player.openInventory(inv);
    }

    private ItemStack createRaceIcon(Race race, boolean isCurrent, int playerLevel) {
        ItemStack item = new ItemStack(race.getMaterial());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(ColorUtils.component(race.getDisplayName()));
            List<String> lore = new ArrayList<>();

            if (isCurrent) {
                lore.add("&a&l[TU RAZA ACTUAL - NIVEL " + playerLevel + "]");
            }
            lore.add("&7Escala Física: &e" + race.getScale() + "x");
            lore.add("&7Vida Base: &c" + (race.getBaseHealthHearts() * 2) + " HP &7(" + race.getBaseHealthHearts() + " corazones)");
            lore.add("&7Velocidad: &b" + (int)(race.getBaseSpeedMultiplier() * 100) + "%");
            lore.add("&7Daño Base: &d" + (int)(race.getBaseDamageMultiplier() * 100) + "%");

            if (!race.getImmunities().isEmpty()) {
                lore.add("&9Inmunidades: &7" + String.join(", ", race.getImmunities()));
            }

            // Sección de Desventajas en el ícono principal
            if (!race.getVulnerabilities().isEmpty()) {
                lore.add("&4Desventajas nativas (L1+):");
                for (String vuln : race.getVulnerabilities()) {
                    lore.add("&c- " + vuln);
                }
            }

            lore.add("");
            if (isCurrent) {
                lore.add("&e▶ Clic para ver tus 6 Niveles y ascender");
            } else {
                lore.add("&a▶ Clic para previsualizar y cambiar a esta raza");
            }

            meta.lore(ColorUtils.componentList(lore));
            for (ItemFlag flag : ItemFlag.values()) {
                meta.addItemFlags(flag);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Abre el submenú de progresión de los 6 niveles de la raza activa.
     * CUMPLE LA REGLA V7.0:
     * - Nivel 1: Muestra claramente las desventajas con viñetas (&c- ...).
     * - Niveles 2 al 6: OCULTAN COMPLETAMENTE el bloque de desventajas y muestran solo ventajas, estadísticas y habilidades.
     */
    public void openRaceProgressMenu(Player player, Race race) {
        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        Inventory inv = Bukkit.createInventory(null, 27, ColorUtils.component(PROGRESS_TITLE_PREFIX + ColorUtils.strip(race.getDisplayName())));

        ItemStack bg = createGuiItem(Material.BLACK_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, bg);
        }

        // Slots 10 a 15: Niveles 1 al 6
        for (int lvl = 1; lvl <= 6; lvl++) {
            int slot = 9 + lvl; // 10, 11, 12, 13, 14, 15
            RaceLevel rLevel = race.getLevel(lvl);
            boolean unlocked = lvl <= data.getLevel();
            boolean isCurrent = lvl == data.getLevel();
            boolean isNext = lvl == data.getLevel() + 1;

            Material mat = unlocked ? Material.LIME_STAINED_GLASS_PANE : (isNext ? Material.YELLOW_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE);
            String title = (unlocked ? "&a" : (isNext ? "&e" : "&7")) + "Nivel " + lvl + (rLevel != null ? ": " + rLevel.getName() : "");

            List<String> lore = new ArrayList<>();
            if (rLevel != null) {
                lore.add("&7Sufijo de Chat: " + rLevel.getSubprefix());
                lore.add("");
                lore.add("&b&lVentajas y Habilidades:");
                for (String perkLine : rLevel.getPerks().split("[,\\.]")) {
                    String p = perkLine.trim();
                    if (!p.isEmpty()) {
                        lore.add("&f✔ " + p);
                    }
                }

                // REGLA CRÍTICA V7.0:
                // Solo Nivel 1 muestra desventajas. Niveles 2 al 6 ocultan completamente el bloque.
                if (lvl == 1) {
                    lore.add("");
                    lore.add("&c&lDesventajas nativas:");
                    if (rLevel.getDisadvantages() != null && !rLevel.getDisadvantages().isEmpty()) {
                        for (String disLine : rLevel.getDisadvantages().split("[,\\.]")) {
                            String d = disLine.trim();
                            if (!d.isEmpty()) {
                                lore.add("&c- " + d);
                            }
                        }
                    } else if (!race.getVulnerabilities().isEmpty()) {
                        for (String v : race.getVulnerabilities()) {
                            lore.add("&c- " + v);
                        }
                    }
                }

                if (lvl > 1) {
                    lore.add("");
                    lore.add("&6&lRequisitos de Ascenso:");
                    if (rLevel.getRequiredItem() != Material.AIR && rLevel.getRequiredItemAmount() > 0) {
                        lore.add("&7- Material: &f" + rLevel.getRequiredItemAmount() + "x " + rLevel.getRequiredItem().name());
                    }
                    if (rLevel.getCostMoney() > 0) {
                        lore.add("&7- Dinero Vault: &e$" + String.format("%.0f", rLevel.getCostMoney()));
                    }
                }
            }

            lore.add("");
            if (isCurrent) {
                lore.add("&a✔ TU NIVEL ACTUAL");
            } else if (unlocked) {
                lore.add("&2✔ Nivel dominado");
            } else if (isNext) {
                lore.add("&e▶ ¡Siguiente nivel a desbloquear!");
            } else {
                lore.add("&c✖ Bloqueado");
            }

            inv.setItem(slot, createGuiItem(mat, title, lore));
        }

        // Slot 22: Botón de ascender nivel o botón de reencarnar
        boolean isOwnRace = race.getId().equalsIgnoreCase(data.getRace());
        if (isOwnRace) {
            if (data.getLevel() < 6) {
                List<String> ascendLore = new ArrayList<>();
                RaceLevel nextLvl = race.getLevel(data.getLevel() + 1);
                if (nextLvl != null) {
                    ascendLore.add("&7Ascenso hacia: &6Nivel " + (data.getLevel() + 1) + " (" + nextLvl.getName() + ")");
                    if (nextLvl.getRequiredItem() != Material.AIR && nextLvl.getRequiredItemAmount() > 0) {
                        ascendLore.add("&7Material: &f" + nextLvl.getRequiredItemAmount() + "x " + nextLvl.getRequiredItem().name());
                    }
                    if (nextLvl.getCostMoney() > 0) {
                        ascendLore.add("&7Coste: &e$" + String.format("%.0f", nextLvl.getCostMoney()));
                    }
                }
                ascendLore.add("");
                ascendLore.add("&e▶ Haz clic para consumir requisitos y subir de nivel");
                inv.setItem(22, createGuiItem(Material.EMERALD_BLOCK, "&a&lSUBIR DE NIVEL", ascendLore));
            } else {
                inv.setItem(22, createGuiItem(Material.NETHER_STAR, "&6&lNIVEL MÁXIMO ALCANZADO", List.of("&7Has alcanzado la cúspide legendaria de tu raza.")));
            }
        } else {
            List<String> inspectLore = new ArrayList<>();
            inspectLore.add("&7Estás inspeccionando las estadísticas de &e" + race.getDisplayName() + "&7.");
            inspectLore.add("&7Para reencarnar en esta raza, haz clic aquí");
            inspectLore.add("&7para acceder al menú de pago y confirmación.");
            inspectLore.add("");
            inspectLore.add("&a▶ Clic para abrir el menú de pago para esta raza.");
            inv.setItem(22, createGuiItem(Material.LIME_WOOL, "&a&lELEGIR ESTA RAZA (PAGO)", inspectLore));
        }

        // Slot 18: Volver al menú principal
        inv.setItem(18, createGuiItem(Material.ARROW, "&eVolver al Menú Principal", null));

        player.openInventory(inv);
    }

    public static ItemStack createGuiItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(ColorUtils.component(name));
            if (lore != null) {
                meta.lore(ColorUtils.componentList(lore));
            }
            for (ItemFlag flag : ItemFlag.values()) {
                meta.addItemFlags(flag);
            }
            item.setItemMeta(meta);
        }
        return item;
    }
}

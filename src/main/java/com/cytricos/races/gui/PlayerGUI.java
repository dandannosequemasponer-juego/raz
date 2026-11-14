package com.cytricos.races.gui;

import com.cytricos.races.CytricosRaces;
import com.cytricos.races.model.PlayerData;
import com.cytricos.races.model.Race;
import com.cytricos.races.model.RaceLevel;
import com.cytricos.races.util.ColorUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
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
 * GUI de 5 filas (45 slots) con soporte para estado "Sin Raza", confirmación segura
 * y visualización de Nivel 1 (con desventajas) vs Niveles 2-6 (sin desventajas).
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

    public void openSelectionMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 45, SELECTION_TITLE_COMPONENT);

        ItemStack bg = createGuiItem(Material.BLACK_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 45; i++) {
            inv.setItem(i, bg);
        }

        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        boolean hasRace = data != null && data.hasAssignedRace();
        String currentRaceId = hasRace ? data.getRace() : "none";

        for (Race race : plugin.getRaceManager().getAllRaces()) {
            boolean isCurrent = hasRace && race.getId().equalsIgnoreCase(currentRaceId);
            ItemStack icon = createRaceIcon(race, isCurrent, hasRace ? data.getLevel() : 0, hasRace);
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
            Race curRace = hasRace ? plugin.getRaceManager().getRace(currentRaceId) : null;
            String raceDisplay = curRace != null ? curRace.getDisplayName() : "&fSin Raza";
            lore.add("&7Raza actual: " + raceDisplay);
            lore.add("&7Nivel actual: &6" + (hasRace ? ("Nivel " + data.getLevel() + " / 6") : "Sin Nivel"));
            lore.add("&7Escala física: &a" + (curRace != null ? curRace.getScale() : 1.0) + "x");
            if (!hasRace) {
                lore.add("&7Primera elección: &a¡Gratuita!");
            } else {
                lore.add("&7Cambio gratis: " + (data.hasFreeRaceChangeAvailable() ? "&aDisponible" : "&cAgotado"));
            }
            if (data != null && data.isFlightBanned()) {
                long sec = data.getFlightBanRemainingSeconds();
                lore.add("&c&lVuelo restringido: &e" + String.format("%02d:%02d", sec / 60, sec % 60));
            }
            if (plugin.isVaultAvailable() && plugin.getVaultEconomy() != null) {
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

        // Slot 40: Menú Principal RPG
        inv.setItem(40, createGuiItem(Material.COMPASS, "&6&lMenú Principal RPG", List.of(
                "&7Abre el panel completo de tu personaje:",
                "&7Perfil, Misiones, Talentos, Logros y Alas.",
                "",
                "&e▶ Clic para abrir el Menú Principal."
        )));

        // Slot 44: Botón de confirmación / cambio de raza
        List<String> confirmLore = new ArrayList<>();
        confirmLore.add("&7Haz clic aquí para abrir el menú");
        confirmLore.add("&7de pago y confirmación de cambio.");
        confirmLore.add("");
        if (!hasRace) {
            confirmLore.add("&a✔ Tu primera selección de raza es Gratuita");
        } else if (data.hasFreeRaceChangeAvailable()) {
            confirmLore.add("&a✔ Dispones de 1 Cambio Gratuito");
        } else {
            double cost = plugin.getConfigManager().getRaceChangeCost();
            confirmLore.add("&fCoste Vault: &e$" + String.format("%.0f", cost));
            confirmLore.add("&fO entrega: &6Esencia de Reencarnación");
        }
        confirmLore.add("");
        confirmLore.add("&e▶ Clic para abrir el submenú de confirmación.");
        inv.setItem(44, createGuiItem(Material.LIME_WOOL, hasRace ? "&a&lCambiar raza" : "&a&lConfirmar raza inicial", confirmLore));

        player.openInventory(inv);
    }

    private ItemStack createRaceIcon(Race race, boolean isCurrent, int playerLevel, boolean playerHasRace) {
        ItemStack item = new ItemStack(race.getMaterial());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(ColorUtils.component(race.getDisplayName()));
            List<String> lore = new ArrayList<>();

            if (isCurrent) {
                lore.add("&a&l[TU RAZA ACTUAL - NIVEL " + playerLevel + "]");
            }
            lore.add("&7Escala Física: &e" + race.getScale() + "x");
            lore.add("&7Vida Base: &c" + (int)(race.getBaseHealthHearts() * 2) + " HP &7(" + race.getBaseHealthHearts() + " corazones)");
            lore.add("&7Velocidad: &b" + (int)(race.getBaseSpeedMultiplier() * 100) + "%");
            lore.add("&7Daño Base: &d" + (int)(race.getBaseDamageMultiplier() * 100) + "%");

            if (!race.getImmunities().isEmpty()) {
                lore.add("&9Inmunidades: &7" + String.join(", ", race.getImmunities()));
            }

            if (!race.getVulnerabilities().isEmpty()) {
                lore.add("&4Desventajas nativas (N1+):");
                for (String vuln : race.getVulnerabilities()) {
                    lore.add("&c- " + vuln);
                }
            }

            lore.add("");
            if (isCurrent) {
                lore.add("&e▶ Clic para ver tus 6 Niveles y ascender");
            } else if (!playerHasRace) {
                lore.add("&e▶ Clic izquierdo: Ver sus 6 Niveles");
                lore.add("&a▶ Clic derecho: Elegir esta raza (Gratis)");
            } else {
                lore.add("&e▶ Clic izquierdo: Inspeccionar 6 Niveles");
                lore.add("&a▶ Clic derecho: Ir directo a Confirmar Cambio");
            }

            meta.lore(ColorUtils.componentList(lore));
            for (ItemFlag flag : ItemFlag.values()) {
                meta.addItemFlags(flag);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    public void openRaceProgressMenu(Player player, Race race) {
        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        boolean hasRace = data != null && data.hasAssignedRace();
        Inventory inv = Bukkit.createInventory(null, 27, ColorUtils.component(PROGRESS_TITLE_PREFIX + ColorUtils.strip(race.getDisplayName())));

        ItemStack bg = createGuiItem(Material.BLACK_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, bg);
        }

        boolean isOwnRace = hasRace && race.getId().equalsIgnoreCase(data.getRace());
        int activeLevel = isOwnRace ? data.getLevel() : 0;

        for (int lvl = 1; lvl <= 6; lvl++) {
            int slot = 9 + lvl;
            RaceLevel rLevel = race.getLevel(lvl);
            boolean unlocked = isOwnRace && lvl <= activeLevel;
            boolean isCurrent = isOwnRace && lvl == activeLevel;
            boolean isNext = isOwnRace && lvl == activeLevel + 1;

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
            inspectLore.add(hasRace
                    ? "&7Para reencarnar en esta raza, haz clic aquí para confirmar."
                    : "&a¡Tu primera elección de raza es completamente gratuita!");
            inspectLore.add("");
            inspectLore.add("&a▶ Clic para abrir el menú de confirmación.");
            inv.setItem(22, createGuiItem(Material.LIME_WOOL, hasRace ? "&a&lELEGIR ESTA RAZA (CONFIRMAR)" : "&a&lELEGIR ESTA RAZA (GRATIS)", inspectLore));
        }

        inv.setItem(18, createGuiItem(Material.ARROW, "&eVolver a Selección de Razas", null));

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

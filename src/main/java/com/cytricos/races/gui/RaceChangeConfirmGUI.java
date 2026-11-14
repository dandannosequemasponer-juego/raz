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
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Submenú interactivo de Confirmación de Cambio de Raza (Vault / Esencia / Gratis) para CYTRICOS RPG.
 * Advierte explícitamente del reinicio completo de progreso al cambiar de raza y soporta
 * la elección inicial gratuita desde el estado "Sin Raza".
 *
 * Lead Developers: Dandan_13 & Wolfit12
 */
public class RaceChangeConfirmGUI {
    public static final String TITLE_LEGACY = ColorUtils.color("&8[&eCYTRICOS&8] &0Confirmar Cambio");
    public static final Component TITLE_COMPONENT = ColorUtils.component("&8[&eCYTRICOS&8] &0Confirmar Cambio");

    private final CytricosRaces plugin;
    private final Map<UUID, String> targetRaceMap = new ConcurrentHashMap<>();

    public RaceChangeConfirmGUI(CytricosRaces plugin) {
        this.plugin = plugin;
    }

    public String getTargetRace(UUID uuid) {
        return targetRaceMap.get(uuid);
    }

    public void clearTargetRace(UUID uuid) {
        targetRaceMap.remove(uuid);
    }

    public void open(Player player, String targetRaceId) {
        if (targetRaceId != null && !targetRaceId.equalsIgnoreCase("none")) {
            targetRaceMap.put(player.getUniqueId(), targetRaceId);
        } else {
            targetRaceId = targetRaceMap.get(player.getUniqueId());
        }
        Inventory inv = Bukkit.createInventory(null, 27, TITLE_COMPONENT);

        ItemStack bg = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta bgMeta = bg.getItemMeta();
        if (bgMeta != null) {
            bgMeta.displayName(ColorUtils.component(" "));
            bg.setItemMeta(bgMeta);
        }
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, bg);
        }

        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        boolean unassigned = !data.hasAssignedRace();
        Race curRace = unassigned ? null : plugin.getRaceManager().getRace(data.getRace());
        String currentRaceLabel = curRace != null ? curRace.getDisplayName() : "&fSin Raza";

        double balance = plugin.isVaultAvailable() ? plugin.getVaultEconomy().getBalance(player) : 0.0;
        double cost = plugin.getConfigManager().getRaceChangeCost();
        boolean hasTokenItem = plugin.getCustomItemManager().hasChangeTokenItem(player);
        boolean hasFree = data.hasFreeRaceChangeAvailable() || unassigned;
        boolean bypass = player.hasPermission("cytricosraces.admin.bypasscost");

        Race targetRace = targetRaceId != null ? plugin.getRaceManager().getRace(targetRaceId) : null;
        String targetName = targetRace != null ? targetRace.getDisplayName() : "&eNueva Raza a Elegir";

        // SLOT 11: Pago por Economía Vault
        ItemStack ecoItem = new ItemStack(Material.LIME_WOOL);
        ItemMeta ecoMeta = ecoItem.getItemMeta();
        if (ecoMeta != null) {
            ecoMeta.displayName(ColorUtils.component("&a&lPAGO POR ECONOMÍA VAULT"));
            List<String> lore = new ArrayList<>();
            lore.add("&7Paga la tasa requerida para moldear");
            lore.add("&7tu linaje y reencarnar en el servidor.");
            lore.add("");
            lore.add("&fCoste del Cambio: &e$" + String.format("%.0f", cost));
            lore.add("&fTu Balance Actual: " + (balance >= cost || bypass ? "&a$" + String.format("%.0f", balance) : "&c$" + String.format("%.0f", balance)));
            if (!unassigned) {
                appendWipeWarning(lore);
            }
            lore.add("");
            if (bypass) {
                lore.add("&d&l[ADMIN] &d¡Dispones de bypass de coste de cambio!");
                lore.add("&e▶ Clic para confirmar cambio gratuito administrativo.");
            } else if (balance >= cost) {
                lore.add("&a✔ Fondos suficientes disponibles.");
                lore.add("&e▶ Clic para pagar $" + String.format("%.0f", cost) + " y confirmar.");
            } else {
                lore.add("&c✘ Fondos insuficientes en tu cuenta.");
            }
            ecoMeta.lore(ColorUtils.componentList(lore));
            ecoItem.setItemMeta(ecoMeta);
        }
        inv.setItem(11, ecoItem);

        // SLOT 13: Resumen de Estado / Selección Inicial o Cambio Gratuito
        ItemStack statusItem = new ItemStack(hasFree ? Material.NETHER_STAR : Material.PAPER);
        ItemMeta statusMeta = statusItem.getItemMeta();
        if (statusMeta != null) {
            statusMeta.displayName(ColorUtils.component(unassigned ? "&a&l¡ELECCIÓN DE LINAJE INICIAL!" : "&6&lRESUMEN Y CAMBIO GRATUITO"));
            List<String> lore = new ArrayList<>();
            lore.add("&7Raza Actual: " + currentRaceLabel + (unassigned ? "" : " &7(Nivel " + data.getLevel() + ")"));
            lore.add("&7Raza Destino: &a" + targetName);
            lore.add("");
            lore.add("&7Cambio Gratuito Único: " + (data.hasFreeRaceChangeAvailable() ? "&aDISPONIBLE" : "&cAGOTADO"));
            lore.add("&7Tu Dinero en Vault: &e$" + String.format("%.0f", balance));
            lore.add("&7Esencia en Mochila: " + (hasTokenItem ? "&aDISPONIBLE" : "&cNO DISPONIBLE"));
            if (!unassigned && hasFree) {
                appendWipeWarning(lore);
            }
            lore.add("");
            if (unassigned) {
                lore.add("&a&l¡SELECCIÓN INICIAL GRATUITA!");
                lore.add("&e▶ Clic aquí para elegir tu primera raza sin coste.");
            } else if (data.hasFreeRaceChangeAvailable()) {
                lore.add("&a&l¡TIENES TU CAMBIO GRATUITO!");
                lore.add("&e▶ Clic aquí para usar tu Cambio Gratuito Único.");
            } else {
                lore.add("&7Selecciona uno de los métodos de pago laterales.");
            }
            statusMeta.lore(ColorUtils.componentList(lore));
            statusItem.setItemMeta(statusMeta);
        }
        inv.setItem(13, statusItem);

        // SLOT 15: Pago por Esencia de Reencarnación
        ItemStack tokenSlotItem = new ItemStack(Material.CHEST);
        ItemMeta tokenMeta = tokenSlotItem.getItemMeta();
        if (tokenMeta != null) {
            tokenMeta.displayName(ColorUtils.component("&6&lENTREGA DE ÍTEM ESPECIAL"));
            List<String> lore = new ArrayList<>();
            lore.add("&7Entrega una &6Esencia de Reencarnación Ancestral");
            lore.add("&7obtenible en cofres legendarios, eventos o tienda.");
            lore.add("");
            lore.add("&fEstado en Inventario: " + (hasTokenItem ? "&aDISPONIBLE EN MOCHILA" : "&cNO ENCONTRADO"));
            if (!unassigned) {
                appendWipeWarning(lore);
            }
            lore.add("");
            if (hasTokenItem || bypass) {
                lore.add("&a✔ Ítem listo para ser entregado.");
                lore.add("&e▶ Clic para consumir 1x Esencia y confirmar.");
            } else {
                lore.add("&c✘ No portas el objeto requerido en tu inventario.");
            }
            tokenMeta.lore(ColorUtils.componentList(lore));
            tokenSlotItem.setItemMeta(tokenMeta);
        }
        inv.setItem(15, tokenSlotItem);

        // SLOT 22: Cancelar / Regresar
        ItemStack cancelItem = new ItemStack(Material.RED_WOOL);
        ItemMeta cancelMeta = cancelItem.getItemMeta();
        if (cancelMeta != null) {
            cancelMeta.displayName(ColorUtils.component("&c&lCANCELAR Y REGRESAR"));
            List<String> lore = new ArrayList<>();
            lore.add("&7Cancela la operación y regresa al menú");
            lore.add("&7principal de razas sin ningún cargo.");
            lore.add("");
            lore.add("&e▶ Clic para volver a /razas.");
            cancelMeta.lore(ColorUtils.componentList(lore));
            cancelItem.setItemMeta(cancelMeta);
        }
        inv.setItem(22, cancelItem);

        player.openInventory(inv);
    }

    private void appendWipeWarning(List<String> lore) {
        lore.add("");
        lore.add("&c&l⚠ REINICIO TOTAL AL CAMBIAR DE RAZA:");
        lore.add("&7• Nivel de raza reiniciado a &fNivel 1");
        lore.add("&7• Experiencia vanilla reiniciada a &f0");
        lore.add("&7• Puntos y Árbol de Talentos a &f0");
        lore.add("&7• Misiones, Alas y Coronas retiradas");
    }
}

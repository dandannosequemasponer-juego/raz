package com.cytricos.races.gui;

import com.cytricos.races.CytricosRaces;
import com.cytricos.races.model.ChangeMethod;
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

/**
 * Submenú interactivo de Confirmación y Doble Pago (Vault / Token Ítem Especial) para CYTRICOS RPG.
 * Tamaño: 27 Slots (3 Filas).
 *
 * Mapeo oficial:
 * - Slot 11: Pago por Economía Vault (Lana Verde / Bloque de Esmeralda)
 * - Slot 13: Resumen de Estado del Jugador (Papel / Lingote de Oro)
 * - Slot 15: Pago por Ítem Especial / Token de Reencarnación (Cofre / Estrella del Nether)
 * - Slot 22: Botón de Cancelar / Regresar al Menú Principal (Lana Roja / Barrera)
 *
 * Lead Developers: Dandan_13 & Wolfit12
 */
public class RaceChangeConfirmGUI {
    public static final String TITLE_LEGACY = ColorUtils.color("&8[&eCYTRICOS&8] &0Confirmar Cambio");
    public static final Component TITLE_COMPONENT = ColorUtils.component("&8[&eCYTRICOS&8] &0Confirmar Cambio");

    private final CytricosRaces plugin;
    private final java.util.Map<java.util.UUID, String> targetRaceMap = new java.util.concurrent.ConcurrentHashMap<>();

    public RaceChangeConfirmGUI(CytricosRaces plugin) {
        this.plugin = plugin;
    }

    public String getTargetRace(java.util.UUID uuid) {
        return targetRaceMap.get(uuid);
    }

    public void clearTargetRace(java.util.UUID uuid) {
        targetRaceMap.remove(uuid);
    }

    /**
     * Abre el submenú de confirmación de cambio de raza para un jugador.
     * @param player Jugador que solicita el cambio
     * @param targetRaceId ID de la nueva raza deseada (o null si proviene del slot 44 de /razas)
     */
    public void open(Player player, String targetRaceId) {
        if (targetRaceId != null) {
            targetRaceMap.put(player.getUniqueId(), targetRaceId);
        } else {
            targetRaceId = targetRaceMap.get(player.getUniqueId());
        }
        Inventory inv = Bukkit.createInventory(null, 27, TITLE_COMPONENT);

        // Fondo de paneles de cristal negro
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
        double balance = plugin.isVaultAvailable() ? plugin.getVaultEconomy().getBalance(player) : 0.0;
        double cost = plugin.getConfigManager().getRaceChangeCost();
        boolean hasTokenItem = plugin.getCustomItemManager().hasChangeTokenItem(player);
        boolean hasFree = data.hasFreeRaceChangeAvailable();
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
            lore.add("");
            if (bypass) {
                lore.add("&d&l[ADMIN] &d¡Dispones de bypass de coste de cambio!");
                lore.add("&e▶ Clic para confirmar cambio gratuito administrativo.");
            } else if (balance >= cost) {
                lore.add("&a✔ Fondos suficientes disponibles.");
                lore.add("&e▶ Clic para pagar $" + String.format("%.0f", cost) + " y reiniciar a Nivel 1.");
            } else {
                lore.add("&c✘ Fondos insuficientes en tu cuenta.");
            }
            ecoMeta.lore(ColorUtils.componentList(lore));
            ecoItem.setItemMeta(ecoMeta);
        }
        inv.setItem(11, ecoItem);

        // SLOT 13: Resumen de Estado del Jugador / Token Gratuito
        ItemStack statusItem = new ItemStack(hasFree ? Material.NETHER_STAR : Material.PAPER);
        ItemMeta statusMeta = statusItem.getItemMeta();
        if (statusMeta != null) {
            statusMeta.displayName(ColorUtils.component("&6&lRESUMEN DE ESTADO RACIAL"));
            List<String> lore = new ArrayList<>();
            lore.add("&7Raza Actual: &e" + data.getRace().toUpperCase() + " &7(Nivel " + data.getLevel() + ")");
            lore.add("&7Raza Destino: &a" + targetName);
            lore.add("");
            lore.add("&7Cambio Gratuito Único: " + (hasFree ? "&aDISPONIBLE" : "&cAGOTADO"));
            lore.add("&7Tu Dinero en Vault: &e$" + String.format("%.0f", balance));
            lore.add("&7Esencia en Mochila: " + (hasTokenItem ? "&aDISPONIBLE" : "&cNO DISPONIBLE"));
            lore.add("");
            if (hasFree) {
                lore.add("&a&l¡TIENES TU CAMBIO GRATUITO!");
                lore.add("&e▶ Haz clic en este ítem para usar tu Token Gratuito.");
            } else {
                lore.add("&7Selecciona uno de los métodos de pago laterales.");
            }
            statusMeta.lore(ColorUtils.componentList(lore));
            statusItem.setItemMeta(statusMeta);
        }
        inv.setItem(13, statusItem);

        // SLOT 15: Pago por Ítem Especial / Token de Reencarnación
        ItemStack tokenSlotItem = new ItemStack(Material.CHEST);
        ItemMeta tokenMeta = tokenSlotItem.getItemMeta();
        if (tokenMeta != null) {
            tokenMeta.displayName(ColorUtils.component("&6&lENTREGA DE ÍTEM ESPECIAL"));
            List<String> lore = new ArrayList<>();
            lore.add("&7Entrega una &6Esencia de Reencarnación Ancestral");
            lore.add("&7obtenible en cofres legendarios, eventos o tienda.");
            lore.add("");
            lore.add("&fEstado en Inventario: " + (hasTokenItem ? "&aDISPONIBLE EN MOCHILA" : "&cNO ENCONTRADO"));
            lore.add("");
            if (hasTokenItem || bypass) {
                lore.add("&a✔ Ítem listo para ser entregado.");
                lore.add("&e▶ Clic para consumir 1x Ítem y reiniciar a Nivel 1.");
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
}

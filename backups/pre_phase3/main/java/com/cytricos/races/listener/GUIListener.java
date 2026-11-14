package com.cytricos.races.listener;

import com.cytricos.races.CytricosRaces;
import com.cytricos.races.gui.AdminGUI;
import com.cytricos.races.gui.PlayerGUI;
import com.cytricos.races.model.ChangeMethod;
import com.cytricos.races.model.PlayerData;
import com.cytricos.races.model.Race;
import com.cytricos.races.model.RaceLevel;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * Listener de interacción para todas las GUIs (Jugadores y Administración).
 * Procesa clics, cancelaciones, Drag & Drop de slots y ascensos.
 * Lead Developers: Dandan_13 & Wolfit12
 */
public class GUIListener implements Listener {
    private final CytricosRaces plugin;

    public GUIListener(CytricosRaces plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        String title = event.getView().getTitle();
        int rawSlot = event.getRawSlot();

        // 1. Menú Principal de Selección de Razas (/razas o /raza gui)
        if (title.equals(PlayerGUI.SELECTION_TITLE)) {
            event.setCancelled(true);
            if (rawSlot < 0 || rawSlot >= 45) return;

            PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());

            if (rawSlot == 36) {
                // Cabeza del jugador: sonido informativo
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7f, 1.2f);
                return;
            }

            if (rawSlot == 44) {
                // Botón Cambiar Raza: abrir submenú interactivo de pago y confirmación
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.0f);
                plugin.getRaceChangeConfirmGUI().open(player, null);
                return;
            }

            Race clickedRace = plugin.getRaceManager().getRaceBySlot(rawSlot);
            if (clickedRace != null) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.2f);
                // Al hacer clic en cualquier raza (propia o ajena), SIEMPRE abre el menú
                // de información y niveles para inspección, SIN cambiar de raza de forma no autorizada.
                plugin.getPlayerGUI().openRaceProgressMenu(player, clickedRace);
            }
            return;
        }

        // 2. Submenú de Confirmación y Doble Pago (Vault / Ítem Especial / Token Gratuito)
        if (title.contains("Confirmar Cambio")) {
            event.setCancelled(true);
            if (rawSlot < 0 || rawSlot >= 27) return;

            String targetRaceId = plugin.getRaceChangeConfirmGUI().getTargetRace(player.getUniqueId());

            if (rawSlot == 11) {
                // Slot 11: Pago por Economía Vault
                if (targetRaceId == null) {
                    player.sendMessage(ColorUtils.color("&cPor favor selecciona una raza destino en el menú principal."));
                    plugin.getPlayerGUI().openSelectionMenu(player);
                    return;
                }
                boolean success = plugin.getRaceManager().processRaceChange(player, targetRaceId, ChangeMethod.ECONOMY);
                if (success) {
                    player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                    plugin.getRaceChangeConfirmGUI().clearTargetRace(player.getUniqueId());
                    player.closeInventory();
                } else {
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                }
            } else if (rawSlot == 13) {
                // Slot 13: Resumen / Token Gratuito
                PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
                if (data.hasFreeRaceChangeAvailable()) {
                    if (targetRaceId == null) {
                        player.sendMessage(ColorUtils.color("&cPor favor selecciona una raza destino en el menú principal."));
                        plugin.getPlayerGUI().openSelectionMenu(player);
                        return;
                    }
                    boolean success = plugin.getRaceManager().processRaceChange(player, targetRaceId, ChangeMethod.FREE);
                    if (success) {
                        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                        plugin.getRaceChangeConfirmGUI().clearTargetRace(player.getUniqueId());
                        player.closeInventory();
                    } else {
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    }
                } else {
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7f, 1.2f);
                }
            } else if (rawSlot == 15) {
                // Slot 15: Pago por Ítem Especial (Esencia de Reencarnación)
                if (targetRaceId == null) {
                    player.sendMessage(ColorUtils.color("&cPor favor selecciona una raza destino en el menú principal."));
                    plugin.getPlayerGUI().openSelectionMenu(player);
                    return;
                }
                boolean success = plugin.getRaceManager().processRaceChange(player, targetRaceId, ChangeMethod.SPECIAL_ITEM);
                if (success) {
                    player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                    plugin.getRaceChangeConfirmGUI().clearTargetRace(player.getUniqueId());
                    player.closeInventory();
                } else {
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                }
            } else if (rawSlot == 22) {
                // Slot 22: Cancelar y regresar al menú principal
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 0.8f);
                plugin.getRaceChangeConfirmGUI().clearTargetRace(player.getUniqueId());
                plugin.getPlayerGUI().openSelectionMenu(player);
            }
            return;
        }

        // 3. Menú de Progreso de Niveles (1 al 6)
        if (title.startsWith(PlayerGUI.PROGRESS_TITLE_PREFIX)) {
            event.setCancelled(true);
            PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
            String raceNameFromTitle = title.substring(PlayerGUI.PROGRESS_TITLE_PREFIX.length()).trim();
            Race viewedRace = plugin.getRaceManager().resolveRace(raceNameFromTitle);
            if (viewedRace == null) {
                viewedRace = plugin.getRaceManager().getRace(data.getRace());
            }

            if (rawSlot == 18) {
                // Volver
                plugin.getPlayerGUI().openSelectionMenu(player);
                return;
            }

            if (rawSlot == 22 && viewedRace != null) {
                if (viewedRace.getId().equalsIgnoreCase(data.getRace())) {
                    // Botón Subir de Nivel en la propia raza
                    boolean upgraded = plugin.getRaceManager().processLevelUp(player);
                    if (upgraded) {
                        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
                        plugin.getPlayerGUI().openRaceProgressMenu(player, viewedRace);
                    } else {
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 1.0f);
                    }
                } else {
                    // Botón Reencarnar en raza inspeccionada -> Abrir submenú de pago y confirmación
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.0f);
                    plugin.getRaceChangeConfirmGUI().open(player, viewedRace.getId());
                }
            }
            return;
        }

        // 4. Menú Principal de Administración (Admin GUI con Drag & Drop y edición)
        if (title.equals(AdminGUI.ADMIN_MAIN_TITLE)) {
            if (!player.hasPermission("cytricosraces.admin")) {
                event.setCancelled(true);
                player.closeInventory();
                return;
            }

            if (rawSlot >= 45) return; // Permitir interacción con inventario propio

            // Slot 40: Gestión de Cambio de Raza
            if (rawSlot == 40) {
                event.setCancelled(true);
                plugin.getAdminGUI().openChangeManagementPanel(player);
                return;
            }

            // Slot 44: Toggle de Pausa Global
            if (rawSlot == 44) {
                event.setCancelled(true);
                boolean current = plugin.getConfigManager().isGlobalChangesEnabled();
                plugin.getConfigManager().setGlobalChangesEnabled(!current);
                player.sendMessage(ChatColor.YELLOW + "Pausa global de cambios: " + (!current ? ChatColor.GREEN + "HABILITADO" : ChatColor.RED + "PAUSADO"));
                plugin.getAdminGUI().openAdminMainMenu(player);
                return;
            }

            Race race = plugin.getRaceManager().getRaceBySlot(rawSlot);
            if (race != null) {
                // Shift + Click -> Variables Globales
                if (event.isShiftClick()) {
                    event.setCancelled(true);
                    plugin.getAdminGUI().openRaceVariablesEditor(player, race);
                    return;
                }

                // Clic simple izquierdo -> Editor de niveles
                if (event.getClick() == ClickType.LEFT) {
                    event.setCancelled(true);
                    plugin.getAdminGUI().openLevelSelectMenu(player, race);
                    return;
                }
            }
            return;
        }

        // 5. Editor de Variables Globales de Raza
        if (title.startsWith(AdminGUI.RACE_VARS_TITLE_PREFIX)) {
            event.setCancelled(true);
            String raceId = title.substring(AdminGUI.RACE_VARS_TITLE_PREFIX.length()).trim();
            Race race = plugin.getRaceManager().getRace(raceId);
            if (race == null) return;

            switch (rawSlot) {
                case 10 -> { // -0.05 Escala
                    race.setScale(race.getScale() - 0.05);
                    plugin.getRaceManager().saveRaceParameter(race);
                    player.sendMessage(ChatColor.GREEN + "Nueva escala para " + race.getId() + ": " + race.getScale());
                    plugin.getAdminGUI().openRaceVariablesEditor(player, race);
                }
                case 12 -> { // +0.05 Escala
                    race.setScale(race.getScale() + 0.05);
                    plugin.getRaceManager().saveRaceParameter(race);
                    player.sendMessage(ChatColor.GREEN + "Nueva escala para " + race.getId() + ": " + race.getScale());
                    plugin.getAdminGUI().openRaceVariablesEditor(player, race);
                }
                case 14 -> { // Vida Base (+/- 1 corazon)
                    double delta = event.getClick() == ClickType.RIGHT ? -1.0 : 1.0;
                    race.setBaseHealthHearts(Math.max(2.0, Math.min(30.0, race.getBaseHealthHearts() + delta)));
                    plugin.getRaceManager().saveRaceParameter(race);
                    plugin.getAdminGUI().openRaceVariablesEditor(player, race);
                }
                case 15 -> { // Velocidad Base (+/- 5%)
                    double delta = event.getClick() == ClickType.RIGHT ? -0.05 : 0.05;
                    race.setBaseSpeedMultiplier(Math.max(0.4, Math.min(2.5, race.getBaseSpeedMultiplier() + delta)));
                    plugin.getRaceManager().saveRaceParameter(race);
                    plugin.getAdminGUI().openRaceVariablesEditor(player, race);
                }
                case 16 -> { // Daño Base (+/- 5%)
                    double delta = event.getClick() == ClickType.RIGHT ? -0.05 : 0.05;
                    race.setBaseDamageMultiplier(Math.max(0.4, Math.min(3.0, race.getBaseDamageMultiplier() + delta)));
                    plugin.getRaceManager().saveRaceParameter(race);
                    plugin.getAdminGUI().openRaceVariablesEditor(player, race);
                }
                case 18 -> plugin.getAdminGUI().openAdminMainMenu(player);
            }
            return;
        }

        // 6. Selector de 6 Niveles en Menú Admin
        if (title.startsWith(AdminGUI.LEVEL_SELECT_TITLE_PREFIX)) {
            event.setCancelled(true);
            String raceId = title.substring(AdminGUI.LEVEL_SELECT_TITLE_PREFIX.length()).trim();
            Race race = plugin.getRaceManager().getRace(raceId);
            if (race == null) return;

            if (rawSlot >= 10 && rawSlot <= 15) {
                int level = rawSlot - 9;
                plugin.getAdminGUI().openLevelEditor(player, race, level);
            } else if (rawSlot == 18) {
                plugin.getAdminGUI().openAdminMainMenu(player);
            }
            return;
        }

        // 7. Editor Específico de Nivel
        if (title.startsWith(AdminGUI.LEVEL_EDIT_TITLE_PREFIX)) {
            event.setCancelled(true);
            String[] parts = title.replace(AdminGUI.LEVEL_EDIT_TITLE_PREFIX, "").trim().split(" Lvl ");
            if (parts.length != 2) return;
            String raceId = parts[0];
            int lvl = Integer.parseInt(parts[1]);
            Race race = plugin.getRaceManager().getRace(raceId);
            if (race == null) return;
            RaceLevel rLevel = race.getLevel(lvl);
            if (rLevel == null) return;

            switch (rawSlot) {
                case 10 -> { // -$1,000 Vault
                    rLevel.setCostMoney(Math.max(0, rLevel.getCostMoney() - 1000));
                    saveLevelToYaml(race, rLevel);
                    plugin.getAdminGUI().openLevelEditor(player, race, lvl);
                }
                case 12 -> { // +$1,000 Vault
                    rLevel.setCostMoney(rLevel.getCostMoney() + 1000);
                    saveLevelToYaml(race, rLevel);
                    plugin.getAdminGUI().openLevelEditor(player, race, lvl);
                }
                case 13 -> { // +$10,000 Vault
                    rLevel.setCostMoney(rLevel.getCostMoney() + 10000);
                    saveLevelToYaml(race, rLevel);
                    plugin.getAdminGUI().openLevelEditor(player, race, lvl);
                }
                case 15 -> { // Inyectar ítem en mano
                    ItemStack hand = player.getInventory().getItemInMainHand();
                    if (hand.getType() != Material.AIR) {
                        rLevel.setRequiredItem(hand.getType());
                        rLevel.setRequiredItemAmount(hand.getAmount());
                        saveLevelToYaml(race, rLevel);
                        player.sendMessage(ChatColor.GREEN + "Requisito actualizado a: " + hand.getAmount() + "x " + hand.getType().name());
                        plugin.getAdminGUI().openLevelEditor(player, race, lvl);
                    } else {
                        player.sendMessage(ChatColor.RED + "Sostén un ítem en mano para asignarlo como requisito.");
                    }
                }
                case 18 -> plugin.getAdminGUI().openLevelSelectMenu(player, race);
            }
            return;
        }

        // 8. Panel de Gestión de Cambio de Raza
        if (title.equals(AdminGUI.CHANGE_PANEL_TITLE)) {
            event.setCancelled(true);
            double cost = plugin.getConfigManager().getRaceChangeCost();
            switch (rawSlot) {
                case 10 -> { // -$5,000
                    plugin.getConfigManager().setRaceChangeCost(Math.max(0, cost - 5000));
                    plugin.getAdminGUI().openChangeManagementPanel(player);
                }
                case 12 -> { // +$5,000
                    plugin.getConfigManager().setRaceChangeCost(cost + 5000);
                    plugin.getAdminGUI().openChangeManagementPanel(player);
                }
                case 14 -> { // Inyectar ítem sostenido como Token oficial
                    plugin.getCustomItemManager().setHeldItemAsToken(player);
                }
                case 15 -> { // Dar token al admin
                    player.getInventory().addItem(plugin.getCustomItemManager().createChangeTokenItem());
                    player.sendMessage(ChatColor.GREEN + "Se ha agregado la Esencia de Reencarnación a tu inventario.");
                }
                case 18 -> plugin.getAdminGUI().openAdminMainMenu(player);
            }
        }
    }

    private void saveLevelToYaml(Race race, RaceLevel level) {
        String path = "races." + race.getId() + ".levels." + level.getLevel() + ".";
        plugin.getConfigManager().getRacesConfig().set(path + "cost_money", level.getCostMoney());
        plugin.getConfigManager().getRacesConfig().set(path + "required_item", level.getRequiredItem().name());
        plugin.getConfigManager().getRacesConfig().set(path + "required_item_amount", level.getRequiredItemAmount());
        plugin.getConfigManager().saveRacesConfig();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        String title = event.getView().getTitle();

        // Evitar que muevan ítems en menús de jugadores
        if (title.equals(PlayerGUI.SELECTION_TITLE) ||
                title.equals(PlayerGUI.CONFIRM_TITLE) ||
                title.startsWith(PlayerGUI.PROGRESS_TITLE_PREFIX) ||
                title.startsWith(AdminGUI.RACE_VARS_TITLE_PREFIX) ||
                title.startsWith(AdminGUI.LEVEL_SELECT_TITLE_PREFIX) ||
                title.startsWith(AdminGUI.LEVEL_EDIT_TITLE_PREFIX) ||
                title.equals(AdminGUI.CHANGE_PANEL_TITLE)) {
            event.setCancelled(true);
        }
    }
}

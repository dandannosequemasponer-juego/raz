package com.cytricos.races.listener;

import com.cytricos.races.CytricosRaces;
import com.cytricos.races.gui.*;
import com.cytricos.races.model.ChangeMethod;
import com.cytricos.races.model.PlayerData;
import com.cytricos.races.model.Race;
import com.cytricos.races.model.RaceLevel;
import com.cytricos.races.util.ColorUtils;
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
import org.bukkit.inventory.ItemStack;

/**
 * Listener central de interacción para todas las GUIs RPG y de Administración de CYTRICOS RPG.
 * Previene movimientos, robos, duplicaciones y maneja navegación completa en español.
 *
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
        boolean isRightClick = event.getClick() == ClickType.RIGHT || event.getClick() == ClickType.SHIFT_RIGHT;

        // 0. Hub Principal RPG (/raza o /raza menu)
        if (title.equals(MainMenuGUI.TITLE)) {
            event.setCancelled(true);
            if (rawSlot < 0 || rawSlot >= 45) return;

            PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
            Race currentRace = plugin.getRaceManager().getRace(data.getRace());
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.1f);

            switch (rawSlot) {
                case 19 -> {
                    if (currentRace != null) {
                        plugin.getPlayerGUI().openRaceProgressMenu(player, currentRace);
                    } else {
                        plugin.getPlayerGUI().openSelectionMenu(player);
                    }
                }
                case 20 -> plugin.getPlayerGUI().openSelectionMenu(player);
                case 21 -> plugin.getAbilityBookGUI().open(player);
                case 22 -> plugin.getProfileGUI().open(player);
                case 23 -> plugin.getSkillTreeGUI().open(player);
                case 24 -> plugin.getQuestsGUI().open(player);
                case 25 -> plugin.getAchievementsGUI().open(player);
                case 29 -> plugin.getEncyclopediaGUI().open(player);
                case 31 -> plugin.getCosmeticsGUI().open(player);
                case 33 -> plugin.getSettingsGUI().open(player);
                case 40 -> player.closeInventory();
            }
            return;
        }

        // 0.1 Perfil RPG del Jugador
        if (title.equals(ProfileGUI.TITLE)) {
            event.setCancelled(true);
            if (rawSlot == 36) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.0f);
                plugin.getMainMenuGUI().open(player);
            } else if (rawSlot == 44) {
                player.closeInventory();
            }
            return;
        }

        // 0.2 Libro de Habilidades
        if (title.equals(AbilityBookGUI.TITLE)) {
            event.setCancelled(true);
            if (rawSlot == 21) {
                player.closeInventory();
                if (plugin.getRaceAbilitiesListener() != null) {
                    plugin.getRaceAbilitiesListener().triggerActiveAbility(player);
                }
            } else if (rawSlot == 22) {
                player.closeInventory();
                if (plugin.getRaceAbilitiesListener() != null) {
                    plugin.getRaceAbilitiesListener().toggleTransformation(player);
                }
            } else if (rawSlot == 36) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.0f);
                plugin.getMainMenuGUI().open(player);
            } else if (rawSlot == 44) {
                player.closeInventory();
            }
            return;
        }

        // 0.3 Árbol de Talentos
        if (title.equals(SkillTreeGUI.TITLE)) {
            event.setCancelled(true);
            if (rawSlot == 36) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.0f);
                plugin.getMainMenuGUI().open(player);
            } else if (rawSlot == 44) {
                player.closeInventory();
            } else if (rawSlot >= 0 && rawSlot < 45) {
                plugin.getSkillTreeGUI().handleClick(player, rawSlot, isRightClick);
            }
            return;
        }

        // 0.4 Misiones Raciales
        if (title.equals(QuestsGUI.TITLE)) {
            event.setCancelled(true);
            if (rawSlot == 36) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.0f);
                plugin.getMainMenuGUI().open(player);
            } else if (rawSlot == 44) {
                player.closeInventory();
            } else if (rawSlot >= 0 && rawSlot < 45) {
                plugin.getQuestsGUI().handleClick(player, rawSlot);
            }
            return;
        }

        // 0.5 Logros y Títulos
        if (title.equals(AchievementsGUI.TITLE)) {
            event.setCancelled(true);
            if (rawSlot == 36) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.0f);
                plugin.getMainMenuGUI().open(player);
            } else if (rawSlot == 44) {
                player.closeInventory();
            } else if (rawSlot >= 0 && rawSlot < 45) {
                plugin.getAchievementsGUI().handleClick(player, rawSlot);
            }
            return;
        }

        // 0.6 Cosméticos y Alas 3D (54 slots)
        if (title.equals(CosmeticsGUI.TITLE)) {
            event.setCancelled(true);
            if (rawSlot == 45) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.0f);
                plugin.getMainMenuGUI().open(player);
            } else if (rawSlot == 53) {
                player.closeInventory();
            } else if (rawSlot >= 0 && rawSlot < 54) {
                plugin.getCosmeticsGUI().handleClick(player, rawSlot, isRightClick);
            }
            return;
        }

        // 0.7 Catálogo de Objetos Personalizados (54 slots)
        if (title.equals(CustomItemsGUI.TITLE)) {
            event.setCancelled(true);
            if (rawSlot == 45) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.0f);
                plugin.getCosmeticsGUI().open(player);
            } else if (rawSlot == 53) {
                player.closeInventory();
            } else if (rawSlot >= 0 && rawSlot < 54) {
                plugin.getCustomItemsGUI().handleClick(player, rawSlot);
            }
            return;
        }

        // 0.8 Ajustes Personales (27 slots)
        if (title.equals(SettingsGUI.TITLE)) {
            event.setCancelled(true);
            if (rawSlot == 18) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.0f);
                plugin.getMainMenuGUI().open(player);
            } else if (rawSlot == 26) {
                player.closeInventory();
            } else if (rawSlot >= 0 && rawSlot < 27) {
                plugin.getSettingsGUI().handleClick(player, rawSlot);
            }
            return;
        }

        // 0.9 Enciclopedia de Razas (45 slots)
        if (title.equals(EncyclopediaGUI.TITLE)) {
            event.setCancelled(true);
            if (rawSlot == 36) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.0f);
                plugin.getMainMenuGUI().open(player);
            } else if (rawSlot == 44) {
                player.closeInventory();
            } else if (rawSlot >= 0 && rawSlot < 45) {
                plugin.getEncyclopediaGUI().handleClick(player, rawSlot);
            }
            return;
        }

        // 1. Menú de Selección de Razas (45 slots)
        if (title.equals(PlayerGUI.SELECTION_TITLE)) {
            event.setCancelled(true);
            if (rawSlot < 0 || rawSlot >= 45) return;

            if (rawSlot == 36) {
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7f, 1.2f);
                plugin.getProfileGUI().open(player);
                return;
            }

            if (rawSlot == 40) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.0f);
                plugin.getMainMenuGUI().open(player);
                return;
            }

            if (rawSlot == 44) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.0f);
                plugin.getRaceChangeConfirmGUI().open(player, null);
                return;
            }

            Race clickedRace = plugin.getRaceManager().getRaceBySlot(rawSlot);
            if (clickedRace != null) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.2f);
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
                plugin.getPlayerGUI().openSelectionMenu(player);
                return;
            }

            if (rawSlot == 22 && viewedRace != null) {
                if (viewedRace.getId().equalsIgnoreCase(data.getRace())) {
                    boolean upgraded = plugin.getRaceManager().processLevelUp(player);
                    if (upgraded) {
                        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
                        plugin.getPlayerGUI().openRaceProgressMenu(player, viewedRace);
                    } else {
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 1.0f);
                    }
                } else {
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.0f);
                    plugin.getRaceChangeConfirmGUI().open(player, viewedRace.getId());
                }
            }
            return;
        }

        // 4. Menú Principal de Administración (Admin GUI)
        if (title.equals(AdminGUI.ADMIN_MAIN_TITLE)) {
            event.setCancelled(true);
            if (!player.hasPermission("cytricosraces.admin")) {
                player.closeInventory();
                return;
            }

            if (rawSlot >= 45) return;

            if (rawSlot == 40) {
                plugin.getAdminGUI().openChangeManagementPanel(player);
                return;
            }

            if (rawSlot == 44) {
                boolean current = plugin.getConfigManager().isGlobalChangesEnabled();
                plugin.getConfigManager().setGlobalChangesEnabled(!current);
                player.sendMessage(ChatColor.YELLOW + "Pausa global de cambios: " + (!current ? ChatColor.GREEN + "HABILITADO" : ChatColor.RED + "PAUSADO"));
                plugin.getAdminGUI().openAdminMainMenu(player);
                return;
            }

            Race race = plugin.getRaceManager().getRaceBySlot(rawSlot);
            if (race != null) {
                if (event.isShiftClick()) {
                    plugin.getAdminGUI().openRaceVariablesEditor(player, race);
                    return;
                }
                if (event.getClick() == ClickType.LEFT) {
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
                case 10 -> {
                    race.setScale(race.getScale() - 0.05);
                    plugin.getRaceManager().saveRaceParameter(race);
                    player.sendMessage(ChatColor.GREEN + "Nueva escala para " + race.getId() + ": " + race.getScale());
                    plugin.getAdminGUI().openRaceVariablesEditor(player, race);
                }
                case 12 -> {
                    race.setScale(race.getScale() + 0.05);
                    plugin.getRaceManager().saveRaceParameter(race);
                    player.sendMessage(ChatColor.GREEN + "Nueva escala para " + race.getId() + ": " + race.getScale());
                    plugin.getAdminGUI().openRaceVariablesEditor(player, race);
                }
                case 14 -> {
                    double delta = event.getClick() == ClickType.RIGHT ? -1.0 : 1.0;
                    race.setBaseHealthHearts(Math.max(2.0, Math.min(30.0, race.getBaseHealthHearts() + delta)));
                    plugin.getRaceManager().saveRaceParameter(race);
                    plugin.getAdminGUI().openRaceVariablesEditor(player, race);
                }
                case 15 -> {
                    double delta = event.getClick() == ClickType.RIGHT ? -0.05 : 0.05;
                    race.setBaseSpeedMultiplier(Math.max(0.4, Math.min(2.5, race.getBaseSpeedMultiplier() + delta)));
                    plugin.getRaceManager().saveRaceParameter(race);
                    plugin.getAdminGUI().openRaceVariablesEditor(player, race);
                }
                case 16 -> {
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
                case 10 -> {
                    rLevel.setCostMoney(Math.max(0, rLevel.getCostMoney() - 1000));
                    saveLevelToYaml(race, rLevel);
                    plugin.getAdminGUI().openLevelEditor(player, race, lvl);
                }
                case 12 -> {
                    rLevel.setCostMoney(rLevel.getCostMoney() + 1000);
                    saveLevelToYaml(race, rLevel);
                    plugin.getAdminGUI().openLevelEditor(player, race, lvl);
                }
                case 13 -> {
                    rLevel.setCostMoney(rLevel.getCostMoney() + 10000);
                    saveLevelToYaml(race, rLevel);
                    plugin.getAdminGUI().openLevelEditor(player, race, lvl);
                }
                case 15 -> {
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
                case 10 -> {
                    plugin.getConfigManager().setRaceChangeCost(Math.max(0, cost - 5000));
                    plugin.getAdminGUI().openChangeManagementPanel(player);
                }
                case 12 -> {
                    plugin.getConfigManager().setRaceChangeCost(cost + 5000);
                    plugin.getAdminGUI().openChangeManagementPanel(player);
                }
                case 14 -> plugin.getCustomItemManager().setHeldItemAsToken(player);
                case 15 -> {
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
        if (isPluginGui(title)) {
            event.setCancelled(true);
        }
    }

    private boolean isPluginGui(String title) {
        return title.equals(MainMenuGUI.TITLE) ||
                title.equals(ProfileGUI.TITLE) ||
                title.equals(AbilityBookGUI.TITLE) ||
                title.equals(SkillTreeGUI.TITLE) ||
                title.equals(QuestsGUI.TITLE) ||
                title.equals(AchievementsGUI.TITLE) ||
                title.equals(CosmeticsGUI.TITLE) ||
                title.equals(CustomItemsGUI.TITLE) ||
                title.equals(SettingsGUI.TITLE) ||
                title.equals(EncyclopediaGUI.TITLE) ||
                title.equals(PlayerGUI.SELECTION_TITLE) ||
                title.equals(PlayerGUI.CONFIRM_TITLE) ||
                title.contains("Confirmar Cambio") ||
                title.startsWith(PlayerGUI.PROGRESS_TITLE_PREFIX) ||
                title.equals(AdminGUI.ADMIN_MAIN_TITLE) ||
                title.startsWith(AdminGUI.RACE_VARS_TITLE_PREFIX) ||
                title.startsWith(AdminGUI.LEVEL_SELECT_TITLE_PREFIX) ||
                title.startsWith(AdminGUI.LEVEL_EDIT_TITLE_PREFIX) ||
                title.equals(AdminGUI.CHANGE_PANEL_TITLE);
    }
}

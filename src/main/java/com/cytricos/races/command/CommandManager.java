package com.cytricos.races.command;

import com.cytricos.races.CytricosRaces;
import com.cytricos.races.listener.GUIListener;
import com.cytricos.races.model.ChangeMethod;
import com.cytricos.races.model.PlayerData;
import com.cytricos.races.model.Race;
import com.cytricos.races.model.RaceLevel;
import com.cytricos.races.util.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Gestor completo de comandos 100% en español para CYTRICOS RPG.
 * Incluye comandos de jugador, mochila táctica persistente, libro/enciclopedia,
 * visibilidad en 1ra persona de alas, entrega administrativa (/raza dar y /raza admin)
 * y configuración en vivo de límites de vuelo y puntos de talento.
 *
 * Lead Developers: Dandan_13 & Wolfit12
 */
public class CommandManager implements CommandExecutor, TabCompleter {
    private final CytricosRaces plugin;

    public CommandManager(CytricosRaces plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (label.equalsIgnoreCase("razas") && args.length == 0) {
            if (sender instanceof Player p) {
                plugin.getPlayerGUI().openSelectionMenu(p);
            } else {
                ColorUtils.sendMessage(sender, "&cComando solo disponible para jugadores.");
            }
            return true;
        }

        if (args.length == 0) {
            if (sender instanceof Player p) {
                plugin.getMainMenuGUI().open(p);
            } else {
                sendHelp(sender);
            }
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "menu" -> {
                if (sender instanceof Player p) plugin.getMainMenuGUI().open(p);
                else ColorUtils.sendMessage(sender, "&cSolo jugadores.");
            }
            case "gui", "elegir", "razas" -> {
                if (sender instanceof Player p) plugin.getPlayerGUI().openSelectionMenu(p);
                else ColorUtils.sendMessage(sender, "&cSolo jugadores.");
            }
            case "confirmar" -> {
                if (sender instanceof Player p) {
                    PlayerData data = plugin.getPlayerDataManager().get(p.getUniqueId());
                    plugin.getPlayerGUI().openConfirmationMenu(p, data != null ? data.getRace() : null);
                }
            }
            case "info" -> handleInfo(sender, args);
            case "estadisticas", "stats" -> handleStats(sender, args);
            case "perfil", "profile" -> {
                if (args.length == 1 && sender instanceof Player p) plugin.getProfileGUI().open(p);
                else handleProfile(sender, args);
            }
            case "habilidades", "grimorio" -> {
                if (sender instanceof Player p) plugin.getAbilityBookGUI().open(p);
                else ColorUtils.sendMessage(sender, "&cSolo jugadores.");
            }
            case "talentos", "arbol" -> {
                if (sender instanceof Player p) plugin.getSkillTreeGUI().open(p);
                else ColorUtils.sendMessage(sender, "&cSolo jugadores.");
            }
            case "misiones", "quests" -> {
                if (sender instanceof Player p) plugin.getQuestsGUI().open(p);
                else ColorUtils.sendMessage(sender, "&cSolo jugadores.");
            }
            case "logros", "titulos" -> {
                if (sender instanceof Player p) plugin.getAchievementsGUI().open(p);
                else ColorUtils.sendMessage(sender, "&cSolo jugadores.");
            }
            case "cosmeticos", "alas", "auras" -> {
                if (sender instanceof Player p) {
                    if (args.length >= 2 && args[1].equalsIgnoreCase("vista")) {
                        boolean hidden = plugin.getWingsManager().toggleFirstPersonVisibility(p);
                        if (plugin.getHeadCosmeticManager() != null) {
                            plugin.getHeadCosmeticManager().applyFirstPersonState(p);
                        }
                        ColorUtils.sendMessage(p, hidden
                                ? "&b&l[ALAS] &7Visibilidad en 1ra persona: &aOculta &7(visible en F5 y multijugador)."
                                : "&b&l[ALAS] &7Visibilidad en 1ra persona: &eVisible&7.");
                    } else {
                        plugin.getCosmeticsGUI().open(p);
                    }
                } else {
                    ColorUtils.sendMessage(sender, "&cSolo jugadores.");
                }
            }
            case "items", "reliquias" -> {
                if (sender instanceof Player p) plugin.getCustomItemsGUI().open(p);
                else ColorUtils.sendMessage(sender, "&cSolo jugadores.");
            }
            case "ajustes", "config" -> {
                if (sender instanceof Player p) plugin.getSettingsGUI().open(p);
                else ColorUtils.sendMessage(sender, "&cSolo jugadores.");
            }
            case "enciclopedia", "codice" -> {
                if (sender instanceof Player p) plugin.getEncyclopediaGUI().open(p);
                else ColorUtils.sendMessage(sender, "&cSolo jugadores.");
            }
            case "pack", "resourcepack" -> {
                if (sender instanceof Player p) plugin.sendConfiguredResourcePack(p);
                else ColorUtils.sendMessage(sender, "&cSolo jugadores.");
            }
            case "subirnivel" -> handleLevelUp(sender);
            case "habilidad", "skill" -> handleAbility(sender);
            case "mochila", "backpack" -> handleBackpack(sender);
            case "guia", "guide" -> handleGuide(sender);
            case "previsualizar" -> handlePreview(sender, args);
            case "cambiarraza" -> handleRaceChange(sender, args);
            case "estado" -> handleStatus(sender);
            case "transformar", "transform", "polimorfia" -> handleTransformToggle(sender);
            case "dar" -> handleGiveSubcommand(sender, args);
            case "ayuda" -> sendHelp(sender);
            case "admin" -> handleAdmin(sender, args);
            case "creador" -> handleCreator(sender, args);
            default -> sendHelp(sender);
        }

        return true;
    }

    /**
     * Subcomando administrativo unificado: /raza dar <reliquia|alas|token> <jugador> [id]
     */
    private void handleGiveSubcommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("cytricosraces.admin")) {
            ColorUtils.sendMessage(sender, "&cNo tienes permiso para usar este comando.");
            return;
        }
        if (args.length < 3) {
            ColorUtils.sendMessage(sender, "&eUso: /raza dar <reliquia|alas|token> <jugador> [id]");
            return;
        }
        String category = args[1].toLowerCase();
        Player target = Bukkit.getPlayer(args[2]);
        if (target == null) {
            ColorUtils.sendMessage(sender, "&cJugador no encontrado o desconectado.");
            return;
        }

        switch (category) {
            case "token", "esencia" -> {
                ItemStack token = plugin.getCustomItemManager().createChangeTokenItem();
                plugin.getCustomItemManager().giveItemSafely(target, token);
                ColorUtils.sendMessage(sender, "&aEntregada Esencia de Reencarnación a &e" + target.getName() + "&a.");
            }
            case "reliquia", "item" -> {
                if (args.length < 4) {
                    ColorUtils.sendMessage(sender, "&eUso: /raza dar reliquia <jugador> <id_reliquia>");
                    return;
                }
                ItemStack stack = plugin.getCustomItemManager().createCustomItem(args[3]);
                if (stack == null) {
                    ColorUtils.sendMessage(sender, "&cReliquia u objeto no encontrado: &e" + args[3]);
                    return;
                }
                plugin.getCustomItemManager().giveItemSafely(target, stack);
                ColorUtils.sendMessage(sender, "&aReliquia '&e" + args[3] + "&a' entregada a &e" + target.getName() + "&a.");
            }
            case "alas" -> {
                if (args.length < 4) {
                    ColorUtils.sendMessage(sender, "&eUso: /raza dar alas <jugador> <id_alas>");
                    return;
                }
                ItemStack elytra = plugin.getWingsManager().createWingElytraItem(args[3]);
                if (elytra == null) {
                    ColorUtils.sendMessage(sender, "&cAlas no encontradas: &e" + args[3]);
                    return;
                }
                plugin.getCustomItemManager().giveItemSafely(target, elytra);
                ColorUtils.sendMessage(sender, "&aÉlitros '&e" + args[3] + "&a' entregados a &e" + target.getName() + "&a.");
            }
            default -> ColorUtils.sendMessage(sender, "&eUso: /raza dar <reliquia|alas|token> <jugador> [id]");
        }
    }

    private void handleInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            ColorUtils.sendMessage(sender, "&eUso: /raza info <raza>");
            return;
        }
        Race race = plugin.getRaceManager().resolveRace(args[1]);
        if (race == null) {
            ColorUtils.sendMessage(sender, "&cLa raza '" + args[1] + "' no existe.");
            return;
        }

        ColorUtils.sendMessage(sender, "&8========================================");
        ColorUtils.sendMessage(sender, "&6&lRAZA: " + race.getDisplayName());
        ColorUtils.sendMessage(sender, "&7Escala Física: &e" + race.getScale() + "x");
        ColorUtils.sendMessage(sender, "&7Vida Base: &c" + (int)(race.getBaseHealthHearts() * 2) + " HP");
        ColorUtils.sendMessage(sender, "&7Velocidad: &b" + (int)(race.getBaseSpeedMultiplier() * 100) + "%");
        ColorUtils.sendMessage(sender, "&7Multiplicador de Daño: &d" + (int)(race.getBaseDamageMultiplier() * 100) + "%");
        ColorUtils.sendMessage(sender, "&e--- Progresión de 6 Niveles ---");
        for (int i = 1; i <= 6; i++) {
            RaceLevel lvl = race.getLevel(i);
            if (lvl != null) {
                ColorUtils.sendMessage(sender, "&6Nvl " + i + " " + lvl.getName() + " &7-> &f" + lvl.getPerks());
            }
        }
        ColorUtils.sendMessage(sender, "&8========================================");
    }

    private void handleStats(CommandSender sender, String[] args) {
        Player target;
        if (args.length > 1 && sender.hasPermission("cytricosraces.admin")) {
            target = Bukkit.getPlayer(args[1]);
        } else if (sender instanceof Player p) {
            target = p;
        } else {
            ColorUtils.sendMessage(sender, "&cEspecifica un jugador: /raza stats <jugador>");
            return;
        }

        if (target == null) {
            ColorUtils.sendMessage(sender, "&cJugador no encontrado.");
            return;
        }

        PlayerData data = plugin.getPlayerDataManager().get(target.getUniqueId());
        boolean hasRace = data != null && data.hasAssignedRace();
        Race race = hasRace ? plugin.getRaceManager().getRace(data.getRace()) : null;
        String raceName = race != null ? race.getDisplayName() : "&fSin Raza";

        ColorUtils.sendMessage(sender, "&8====== [ ESTADÍSTICAS RACIALES ] ======");
        ColorUtils.sendMessage(sender, "&eJugador: &f" + target.getName());
        ColorUtils.sendMessage(sender, "&eRaza Activa: " + raceName);
        ColorUtils.sendMessage(sender, "&eNivel Racial: &6" + (hasRace ? (data.getLevel() + " / 6") : "Sin Nivel"));
        ColorUtils.sendMessage(sender, "&eEscala Hitbox: &a" + (race != null ? race.getScale() : 1.0) + "x");
        ColorUtils.sendMessage(sender, "&eVida Máxima: &c" + (int)target.getMaxHealth() + " HP");
        ColorUtils.sendMessage(sender, "&eCambio Gratuito Único: " + (data != null && data.hasFreeRaceChangeAvailable() ? "&aDisponible" : "&cConsumido"));
        ColorUtils.sendMessage(sender, "&8=======================================");
    }

    private void handleLevelUp(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            ColorUtils.sendMessage(sender, "&cComando solo disponible para jugadores.");
            return;
        }
        plugin.getRaceManager().processLevelUp(player);
    }

    private void handleAbility(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            ColorUtils.sendMessage(sender, "&cComando solo disponible para jugadores.");
            return;
        }
        if (plugin.getRaceAbilitiesListener() != null) {
            plugin.getRaceAbilitiesListener().triggerActiveAbility(player);
        }
    }

    private void handleBackpack(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            ColorUtils.sendMessage(sender, "&cComando solo disponible para jugadores.");
            return;
        }
        GUIListener.openPersistentBackpack(plugin, player);
    }

    private void handlePreview(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            ColorUtils.sendMessage(sender, "&cSolo jugadores.");
            return;
        }
        if (args.length < 2) {
            ColorUtils.sendMessage(player, "&eUso: /raza previsualizar <raza>");
            return;
        }
        Race race = plugin.getRaceManager().resolveRace(args[1]);
        if (race == null) {
            ColorUtils.sendMessage(player, "&cRaza no encontrada.");
            return;
        }
        ColorUtils.sendMessage(player, "&6&l[PREVISUALIZACIÓN] &7Raza: " + race.getDisplayName() + " &7- Escala: &e" + race.getScale() + "x");
        plugin.getPlayerGUI().openConfirmationMenu(player, race.getId());
    }

    private void handleRaceChange(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            ColorUtils.sendMessage(sender, "&cSolo jugadores.");
            return;
        }
        if (args.length < 2) {
            ColorUtils.sendMessage(player, "&eUso: /raza cambiarraza <raza> [gratis|dinero|objeto]");
            return;
        }

        Race resolved = plugin.getRaceManager().resolveRace(args[1]);
        if (resolved == null) {
            ColorUtils.sendMessage(player, "&cRaza '" + args[1] + "' no encontrada.");
            return;
        }
        String raceId = resolved.getId();
        ChangeMethod method = null;
        if (args.length >= 3) {
            method = ChangeMethod.fromString(args[2]);
        }

        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        if (method == null) {
            if (!data.hasAssignedRace() || data.hasFreeRaceChangeAvailable()) method = ChangeMethod.FREE;
            else if (plugin.getCustomItemManager().hasChangeTokenItem(player)) method = ChangeMethod.SPECIAL_ITEM;
            else method = ChangeMethod.ECONOMY;
        }

        plugin.getRaceManager().processRaceChange(player, raceId, method);
    }

    private void handleTransformToggle(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            ColorUtils.sendMessage(sender, "&cComando solo disponible para jugadores.");
            return;
        }
        if (plugin.getRaceAbilitiesListener() != null) {
            plugin.getRaceAbilitiesListener().toggleTransformation(player);
        }
    }

    private void handleStatus(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            ColorUtils.sendMessage(sender, "&cSolo jugadores.");
            return;
        }

        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        double cost = plugin.getConfigManager().getRaceChangeCost();
        boolean hasToken = plugin.getCustomItemManager().hasChangeTokenItem(player);

        ColorUtils.sendMessage(player, "&8====== [ ESTADO DE CAMBIO DE RAZA ] ======");
        ColorUtils.sendMessage(player, "&eSistema Global: " + (plugin.getConfigManager().isGlobalChangesEnabled() ? "&aActivo" : "&cPausado"));
        ColorUtils.sendMessage(player, "&eCambio Gratuito Único: " + (data.hasFreeRaceChangeAvailable() ? "&aDisponible" : "&cConsumido"));
        ColorUtils.sendMessage(player, "&ePrecio en Dinero Vault: &6$" + String.format("%.0f", cost));
        ColorUtils.sendMessage(player, "&eEsencia de Reencarnación en Mochila: " + (hasToken ? "&aEn posesión" : "&cNo"));
        ColorUtils.sendMessage(player, "&8==========================================");
    }

    private void handleAdmin(CommandSender sender, String[] args) {
        if (!sender.hasPermission("cytricosraces.admin")) {
            ColorUtils.sendMessage(sender, "&cNo tienes permiso para usar comandos administrativos.");
            return;
        }

        if (args.length < 2 || args[1].equalsIgnoreCase("ayuda")) {
            sendAdminHelp(sender);
            return;
        }

        String action = args[1].toLowerCase();

        switch (action) {
            case "menu", "gui" -> {
                if (sender instanceof Player p) plugin.getAdminGUI().openAdminMainMenu(p);
                else ColorUtils.sendMessage(sender, "&cSolo jugadores.");
            }
            case "cambios" -> {
                if (args.length < 3) {
                    ColorUtils.sendMessage(sender, "&eUso: /raza admin cambios parar | iniciar");
                    return;
                }
                if (args[2].equalsIgnoreCase("parar")) {
                    plugin.getConfigManager().setGlobalChangesEnabled(false);
                    ColorUtils.sendMessage(sender, "&c&l[CYTRICOS] &7El sistema de cambios de raza ha sido &cPAUSADO&7 globalmente.");
                } else if (args[2].equalsIgnoreCase("iniciar")) {
                    plugin.getConfigManager().setGlobalChangesEnabled(true);
                    ColorUtils.sendMessage(sender, "&a&l[CYTRICOS] &7El sistema de cambios de raza ha sido &aACTIVADO&7 globalmente.");
                }
            }
            case "editar" -> {
                if (args.length < 3 || !(sender instanceof Player p)) {
                    ColorUtils.sendMessage(sender, "&eUso: /raza admin editar <raza>");
                    return;
                }
                Race race = plugin.getRaceManager().resolveRace(args[2]);
                if (race != null) plugin.getAdminGUI().openRaceVariablesEditor(p, race);
                else ColorUtils.sendMessage(sender, "&cRaza no encontrada.");
            }
            case "editarnivel" -> {
                if (args.length < 4 || !(sender instanceof Player p)) {
                    ColorUtils.sendMessage(sender, "&eUso: /raza admin editarnivel <raza> <1-6>");
                    return;
                }
                Race race = plugin.getRaceManager().resolveRace(args[2]);
                try {
                    int lvl = Integer.parseInt(args[3]);
                    if (race != null && lvl >= 1 && lvl <= 6) {
                        plugin.getAdminGUI().openLevelEditor(p, race, lvl);
                    } else {
                        ColorUtils.sendMessage(sender, "&cNivel o raza inválidos.");
                    }
                } catch (NumberFormatException e) {
                    ColorUtils.sendMessage(sender, "&cNivel debe ser un número entre 1 y 6.");
                }
            }
            case "establecerjugador" -> {
                if (args.length < 4) {
                    ColorUtils.sendMessage(sender, "&eUso: /raza admin establecerjugador <jugador> <raza|none> [nivel]");
                    return;
                }
                Player target = Bukkit.getPlayer(args[2]);
                if (target == null) {
                    ColorUtils.sendMessage(sender, "&cJugador desconectado o no encontrado.");
                    return;
                }
                if (plugin.getTransformationManager() != null) {
                    plugin.getTransformationManager().revertTransformation(target, false);
                }
                if (plugin.getWingsManager() != null) {
                    plugin.getWingsManager().unequipOnRaceChange(target);
                }
                if (plugin.getHeadCosmeticManager() != null) {
                    plugin.getHeadCosmeticManager().unequipOnRaceChange(target);
                }

                if (args[3].equalsIgnoreCase("none") || args[3].equalsIgnoreCase("sinraza")) {
                    PlayerData data = plugin.getPlayerDataManager().get(target.getUniqueId());
                    data.setRace("none");
                    data.wipeRaceProgress();
                    plugin.getPlayerDataManager().saveToFile(data);
                    plugin.getRaceManager().applyPassives(target, "none", 1);
                    ColorUtils.sendMessage(sender, "&aJugador &e" + target.getName() + " &aestablecido a &fSin Raza&a.");
                    return;
                }

                Race race = plugin.getRaceManager().resolveRace(args[3]);
                if (race == null) {
                    ColorUtils.sendMessage(sender, "&cRaza no encontrada.");
                    return;
                }
                int lvl = 1;
                if (args.length >= 5) {
                    try { lvl = Math.max(1, Math.min(6, Integer.parseInt(args[4]))); } catch (NumberFormatException ignored) {}
                }
                PlayerData data = plugin.getPlayerDataManager().get(target.getUniqueId());
                data.setRace(race.getId());
                data.setLevelRaw(lvl);
                plugin.getPlayerDataManager().saveToFile(data);
                plugin.getRaceManager().applyPassives(target, race.getId(), lvl);
                ColorUtils.sendMessage(sender, "&aJugador &e" + target.getName() + " &aasignado a " + race.getDisplayName() + " &aNivel " + lvl);
            }
            case "reiniciar" -> {
                if (args.length < 3) {
                    ColorUtils.sendMessage(sender, "&eUso: /raza admin reiniciar <jugador>");
                    return;
                }
                Player target = Bukkit.getPlayer(args[2]);
                if (target != null) {
                    if (plugin.getTransformationManager() != null) plugin.getTransformationManager().revertTransformation(target, false);
                    if (plugin.getWingsManager() != null) plugin.getWingsManager().unequipOnRaceChange(target);
                    if (plugin.getHeadCosmeticManager() != null) plugin.getHeadCosmeticManager().unequipOnRaceChange(target);
                    plugin.getPlayerDataManager().resetPlayer(target.getUniqueId());
                    target.setLevel(0);
                    target.setExp(0.0f);
                    target.setTotalExperience(0);
                    PlayerData d = plugin.getPlayerDataManager().get(target.getUniqueId());
                    plugin.getRaceManager().applyPassives(target, d.getRace(), 1);
                    ColorUtils.sendMessage(sender, "&aProgreso racial reiniciado a Sin Raza para &e" + target.getName());
                } else {
                    ColorUtils.sendMessage(sender, "&cJugador no encontrado.");
                }
            }
            case "inspeccionar" -> {
                if (args.length < 3) {
                    ColorUtils.sendMessage(sender, "&eUso: /raza admin inspeccionar <jugador>");
                    return;
                }
                Player target = Bukkit.getPlayer(args[2]);
                if (target != null) handleStats(sender, new String[]{"stats", target.getName()});
            }
            case "darobjetocambio" -> {
                if (args.length < 3) {
                    ColorUtils.sendMessage(sender, "&eUso: /raza admin darobjetocambio <jugador> [cantidad]");
                    return;
                }
                Player target = Bukkit.getPlayer(args[2]);
                int amount = 1;
                if (args.length >= 4) {
                    try { amount = Integer.parseInt(args[3]); } catch (NumberFormatException ignored) {}
                }
                if (target != null) {
                    ItemStack token = plugin.getCustomItemManager().createChangeTokenItem();
                    token.setAmount(amount);
                    plugin.getCustomItemManager().giveItemSafely(target, token);
                    ColorUtils.sendMessage(sender, "&aEntregado " + amount + "x Esencia de Reencarnación a &e" + target.getName());
                }
            }
            case "establecerslot" -> {
                if (args.length < 4) {
                    ColorUtils.sendMessage(sender, "&eUso: /raza admin establecerslot <raza> <slot_0-44>");
                    return;
                }
                try {
                    int slot = Integer.parseInt(args[3]);
                    Race targetRace = plugin.getRaceManager().resolveRace(args[2]);
                    String raceId = targetRace != null ? targetRace.getId() : args[2];
                    plugin.getRaceManager().setRaceSlot(raceId, slot);
                    ColorUtils.sendMessage(sender, "&aSlot de &e" + raceId + " &aactualizado a &e" + slot);
                } catch (NumberFormatException e) {
                    ColorUtils.sendMessage(sender, "&cEl slot debe ser un entero entre 0 y 44.");
                }
            }
            case "recargar" -> {
                plugin.getConfigManager().reloadAll();
                plugin.getRaceManager().loadRaces();
                if (plugin.getRelicManager() != null) plugin.getRelicManager().loadRelics();
                if (plugin.getResourcePackManager() != null) plugin.getResourcePackManager().initAsyncSha1();
                ColorUtils.sendMessage(sender, "&a&l[CYTRICOS] &7Configuraciones, races.yml, relics.yml y Resource Pack recargados.");
            }
            case "establecerpreciocambio" -> {
                if (args.length < 3) {
                    ColorUtils.sendMessage(sender, "&eUso: /raza admin establecerpreciocambio <monto>");
                    return;
                }
                try {
                    double amount = Double.parseDouble(args[2]);
                    plugin.getConfigManager().setRaceChangeCost(amount);
                    ColorUtils.sendMessage(sender, "&aPrecio de cambio Vault actualizado a: &e$" + String.format("%.0f", amount));
                } catch (NumberFormatException e) {
                    ColorUtils.sendMessage(sender, "&cMonto inválido.");
                }
            }
            case "maxvuelotransform" -> {
                if (args.length < 3) {
                    ColorUtils.sendMessage(sender, "&eUso: /raza admin maxvuelotransform <alturaY>");
                    return;
                }
                try {
                    double y = Double.parseDouble(args[2]);
                    plugin.getConfigManager().setTransformationMaxFlightY(y);
                    ColorUtils.sendMessage(sender, "&aAltura máxima Y de vuelo en metamorfosis establecida a: &e" + (int) y);
                } catch (NumberFormatException e) {
                    ColorUtils.sendMessage(sender, "&cAltura numérica inválida.");
                }
            }
            case "killsportalento" -> {
                if (args.length < 3) {
                    ColorUtils.sendMessage(sender, "&eUso: /raza admin killsportalento <cantidad>");
                    return;
                }
                try {
                    int kills = Integer.parseInt(args[2]);
                    plugin.getConfigManager().setTalentKillsPerPoint(kills);
                    ColorUtils.sendMessage(sender, "&aBajas necesarias por punto de talento establecidas a: &e" + kills);
                } catch (NumberFormatException e) {
                    ColorUtils.sendMessage(sender, "&cCantidad entera inválida.");
                }
            }
            case "establecerobjetocambio" -> {
                if (sender instanceof Player p) plugin.getCustomItemManager().setHeldItemAsToken(p);
            }
            case "concedercambiogratis" -> {
                if (args.length < 3) {
                    ColorUtils.sendMessage(sender, "&eUso: /raza admin concedercambiogratis <jugador>");
                    return;
                }
                Player target = Bukkit.getPlayer(args[2]);
                if (target != null) {
                    PlayerData d = plugin.getPlayerDataManager().get(target.getUniqueId());
                    d.setFreeRaceChangeAvailable(true);
                    plugin.getPlayerDataManager().saveToFile(d);
                    ColorUtils.sendMessage(sender, "&aCambio gratuito otorgado a &e" + target.getName());
                }
            }
            case "revocarcambiogratis" -> {
                if (args.length < 3) {
                    ColorUtils.sendMessage(sender, "&eUso: /raza admin revocarcambiogratis <jugador>");
                    return;
                }
                Player target = Bukkit.getPlayer(args[2]);
                if (target != null) {
                    PlayerData d = plugin.getPlayerDataManager().get(target.getUniqueId());
                    d.setFreeRaceChangeAvailable(false);
                    plugin.getPlayerDataManager().saveToFile(d);
                    ColorUtils.sendMessage(sender, "&cCambio gratuito revocado para &e" + target.getName());
                }
            }
            case "estableceralturavuelo" -> {
                if (args.length < 4) {
                    ColorUtils.sendMessage(sender, "&eUso: /raza admin estableceralturavuelo <fairy|angel> <altura>");
                    return;
                }
                String targetRace = args[2].toLowerCase();
                try {
                    double height = Double.parseDouble(args[3]);
                    if (targetRace.equals("fairy") || targetRace.equals("hada")) {
                        plugin.getConfig().set("flight_settings.fairy_max_height", height);
                        plugin.saveConfig();
                        ColorUtils.sendMessage(sender, "&aLímite de vuelo para Hada establecido a: &e" + height + " bloques.");
                    } else if (targetRace.equals("angel") || targetRace.equals("ángel")) {
                        plugin.getConfig().set("flight_settings.angel_max_height", height);
                        plugin.saveConfig();
                        ColorUtils.sendMessage(sender, "&aLímite de vuelo para Ángel establecido a: &e" + height + " bloques.");
                    } else {
                        ColorUtils.sendMessage(sender, "&cRaza con vuelo inválida. Usa 'fairy' o 'angel'.");
                    }
                } catch (NumberFormatException e) {
                    ColorUtils.sendMessage(sender, "&cLa altura debe ser un valor numérico.");
                }
            }
            case "perdonarvuelo" -> handlePardonFlight(sender, args);
            case "diagnostico", "diagnostic" -> handleDiagnostic(sender);
            case "inspeccionarcooldowns" -> handleInspectCooldowns(sender, args);
            case "daritem", "darreliquia" -> handleAdminGiveItem(sender, args);
            case "daralas" -> handleAdminGiveWings(sender, args);
            default -> sendAdminHelp(sender);
        }
    }

    private void handleProfile(CommandSender sender, String[] args) {
        Player target;
        if (args.length > 1 && sender.hasPermission("cytricosraces.admin")) {
            target = Bukkit.getPlayer(args[1]);
        } else if (sender instanceof Player p) {
            target = p;
        } else {
            ColorUtils.sendMessage(sender, "&cEspecifica un jugador: /raza perfil <jugador>");
            return;
        }

        if (target == null) {
            ColorUtils.sendMessage(sender, "&cJugador no encontrado.");
            return;
        }

        PlayerData data = plugin.getPlayerDataManager().get(target.getUniqueId());
        boolean hasRace = data != null && data.hasAssignedRace();
        Race race = hasRace ? plugin.getRaceManager().getRace(data.getRace()) : null;
        String raceName = race != null ? race.getDisplayName() : "&fSin Raza";

        ColorUtils.sendMessage(sender, "&8================ [ PERFIL RPG RACIAL ] ================");
        ColorUtils.sendMessage(sender, "&ePersonaje: &f" + target.getName() + " &7(" + target.getUniqueId() + ")");
        ColorUtils.sendMessage(sender, "&eLinaje Activo: " + raceName);
        ColorUtils.sendMessage(sender, "&eNivel de Ascensión: &6" + (hasRace ? (data.getLevel() + " / 6") : "Sin Nivel"));
        ColorUtils.sendMessage(sender, "&eRecurso Racial (" + data.getResourceName() + "): &a" + (int)data.getRacialResource() + " / " + (int)data.getMaxRacialResource());
        ColorUtils.sendMessage(sender, "&eEscala Física: &b" + (race != null ? race.getScale() : 1.0) + "x");
        ColorUtils.sendMessage(sender, "&ePuntos de Talento: &e" + data.getTalentPoints() + " &7(" + data.getMobKillsCounter() + "/" + plugin.getConfigManager().getTalentKillsPerPoint() + " bajas)");
        ColorUtils.sendMessage(sender, "&eMetamorfosis: " + (data.isTransformed() ? "&dActiva" : "&7Forma Base"));
        ColorUtils.sendMessage(sender, "&8=======================================================");
    }

    private void handleDiagnostic(CommandSender sender) {
        Runtime rt = Runtime.getRuntime();
        long maxMem = rt.maxMemory() / 1024 / 1024;
        long freeMem = rt.freeMemory() / 1024 / 1024;
        long usedMem = (rt.totalMemory() - rt.freeMemory()) / 1024 / 1024;

        ColorUtils.sendMessage(sender, "&8============= [ DIAGNÓSTICO TÉCNICO CYTRICOS ] =============");
        ColorUtils.sendMessage(sender, "&ePlugin: &aCytricosRaces v" + plugin.getDescription().getVersion() + " (Paper 26.2 / Java 25)");
        ColorUtils.sendMessage(sender, "&eAlmacenamiento: &b" + (plugin.getStorageManager() != null ? plugin.getStorageManager().getStorageType().name() : "YAML Flatfile"));
        ColorUtils.sendMessage(sender, "&eJugadores en Caché: &f" + plugin.getPlayerDataManager().getCache().size() + " cargados");
        ColorUtils.sendMessage(sender, "&eMemoria JVM: &d" + usedMem + "MB usados / " + maxMem + "MB máx (Libre: " + freeMem + "MB)");
        ColorUtils.sendMessage(sender, "&eIntegraciones:");
        ColorUtils.sendMessage(sender, "&7 - Vault: " + (plugin.isVaultAvailable() ? "&aConectado" : "&cDesconectado"));
        ColorUtils.sendMessage(sender, "&7 - LuckPerms: " + (plugin.isLuckPermsAvailable() ? "&aConectado" : "&eNo detectado"));
        ColorUtils.sendMessage(sender, "&7 - PlaceholderAPI: " + (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI") ? "&aRegistrado" : "&eNo instalado"));
        ColorUtils.sendMessage(sender, "&7 - ProtocolLib: " + (plugin.getProtocolLibHook() != null && plugin.getProtocolLibHook().isAvailable() ? "&aOcultación 1ra Persona" : "&ePaper hideEntity"));
        ColorUtils.sendMessage(sender, "&7 - WorldGuard: " + (plugin.getWorldGuardHook() != null ? "&aProtecciones Activas" : "&7No detectado"));
        ColorUtils.sendMessage(sender, "&7 - LibsDisguises: " + (plugin.getLibsDisguisesHook() != null && plugin.getLibsDisguisesHook().isAvailable() ? "&aPolimorfia Lista" : "&eNo detectado"));
        ColorUtils.sendMessage(sender, "&8============================================================");
    }

    private void handleAdminGiveItem(CommandSender sender, String[] args) {
        if (args.length < 4) {
            ColorUtils.sendMessage(sender, "&eUso: /raza admin darreliquia <jugador> <id_objeto>");
            return;
        }
        Player target = Bukkit.getPlayer(args[2]);
        if (target == null) {
            ColorUtils.sendMessage(sender, "&cJugador no encontrado.");
            return;
        }
        ItemStack stack = plugin.getCustomItemManager().createCustomItem(args[3]);
        if (stack == null) {
            ColorUtils.sendMessage(sender, "&cReliquia u objeto no encontrado: " + args[3]);
            return;
        }
        plugin.getCustomItemManager().giveItemSafely(target, stack);
        ColorUtils.sendMessage(sender, "&aEntregado objeto '&e" + args[3] + "&a' a &e" + target.getName() + "&a.");
    }

    private void handleAdminGiveWings(CommandSender sender, String[] args) {
        if (args.length < 4) {
            ColorUtils.sendMessage(sender, "&eUso: /raza admin daralas <jugador> <id_alas>");
            return;
        }
        Player target = Bukkit.getPlayer(args[2]);
        if (target == null) {
            ColorUtils.sendMessage(sender, "&cJugador no encontrado.");
            return;
        }
        ItemStack elytra = plugin.getWingsManager().createWingElytraItem(args[3]);
        if (elytra == null) {
            ColorUtils.sendMessage(sender, "&cAlas no encontradas: " + args[3]);
            return;
        }
        plugin.getCustomItemManager().giveItemSafely(target, elytra);
        ColorUtils.sendMessage(sender, "&aEntregados élitros '&e" + args[3] + "&a' a &e" + target.getName() + "&a.");
    }

    private void handleInspectCooldowns(CommandSender sender, String[] args) {
        if (args.length < 3) {
            ColorUtils.sendMessage(sender, "&eUso: /raza admin inspeccionarcooldowns <jugador>");
            return;
        }
        Player target = Bukkit.getPlayer(args[2]);
        if (target == null) {
            ColorUtils.sendMessage(sender, "&cJugador no encontrado.");
            return;
        }
        PlayerData data = plugin.getPlayerDataManager().get(target.getUniqueId());
        ColorUtils.sendMessage(sender, "&8====== [ COOLDOWNS DE " + target.getName().toUpperCase() + " ] ======");
        ColorUtils.sendMessage(sender, "&eTransformación / Metamorfosis: " + (data.isTransformOnCooldown() ? "&c" + data.getTransformCooldownRemainingSeconds() + "s" : "&aDisponible"));
        ColorUtils.sendMessage(sender, "&eSanción de Vuelo: " + (data.isFlightBanned() ? "&c" + data.getFlightBanRemainingSeconds() + "s" : "&aSin sanción"));
        ColorUtils.sendMessage(sender, "&eRecurso (" + data.getResourceName() + "): &6" + (int)data.getRacialResource() + " / " + (int)data.getMaxRacialResource());
        ColorUtils.sendMessage(sender, "&8=================================================");
    }

    private void handlePardonFlight(CommandSender sender, String[] args) {
        if (args.length < 3) {
            ColorUtils.sendMessage(sender, "&eUso: /raza admin perdonarvuelo <jugador>");
            return;
        }
        Player target = Bukkit.getPlayer(args[2]);
        if (target != null) {
            PlayerData data = plugin.getPlayerDataManager().get(target.getUniqueId());
            data.setFlightBanUntil(0);
            plugin.getPlayerDataManager().saveToFile(data);
            target.setAllowFlight(true);
            ColorUtils.sendMessage(sender, "&aSe ha restablecido el vuelo y perdonado la sanción para &e" + target.getName());
            ColorUtils.sendMessage(target, "&a&l[SISTEMA DE VUELO] &7La administración ha perdonado tu sanción de vuelo.");
        } else {
            ColorUtils.sendMessage(sender, "&cJugador no encontrado o desconectado.");
        }
    }

    private void handleGuide(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            ColorUtils.sendMessage(sender, "&cComando solo disponible para jugadores.");
            return;
        }

        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();
        if (meta != null) {
            meta.setTitle(ColorUtils.color("&6&lGuía de Razas CYTRICOS"));
            meta.setAuthor("CYTRICOS RPG");

            meta.addPage(ColorUtils.color("&8====== &6CYTRICOS RPG &8======\n\n" +
                    "&0Bienvenido a la &lGuía Oficial de Razas&0.\n\n" +
                    "&0Contamos con &616 razas únicas&0 y 6 niveles de evolución.\n\n" +
                    "&8Usa &2/razas &8o &2/raza enciclopedia&8."));

            meta.addPage(ColorUtils.color("&8==== &bCONTROLES CLAVE &8====\n\n" +
                    "&0• &lShift + F&0: Activa tu habilidad racial primaria.\n\n" +
                    "&0• &lShift + Clic Der. (6.0s)&0: Canaliza tu metamorfosis sin soltar Shift.\n\n" +
                    "&0• &l/raza alas vista&0: Oculta tus alas en 1ra persona."));

            meta.addPage(ColorUtils.color("&8==== &6PROGRESIÓN &8====\n\n" +
                    "&0• Gana puntos de talento cada nivel, derrotando criaturas o usando &5Fragmentos de Talento&0.\n\n" +
                    "&0• Cambiar de raza reinicia nivel, XP, talentos, misiones y alas."));

            book.setItemMeta(meta);
        }

        plugin.getCustomItemManager().giveItemSafely(player, book);
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1.0f, 1.0f);
        ColorUtils.sendMessage(player, "&a&l[GUÍA] &7Se ha entregado la &6Guía Oficial de Razas &7a tu inventario.");
    }

    private void handleCreator(CommandSender sender, String[] args) {
        if (!sender.hasPermission("cytricosraces.creator")) {
            ColorUtils.sendMessage(sender, "&cNo tienes permiso de creador.");
            return;
        }
        if (args.length < 4 || !args[1].equalsIgnoreCase("establecerescala")) {
            ColorUtils.sendMessage(sender, "&eUso: /raza creador establecerescala <raza> <valor>");
            return;
        }
        Race race = plugin.getRaceManager().resolveRace(args[2]);
        if (race != null) {
            try {
                double scale = Double.parseDouble(args[3]);
                race.setScale(scale);
                plugin.getRaceManager().saveRaceParameter(race);
                ColorUtils.sendMessage(sender, "&aEscala de " + race.getId() + " establecida a: &e" + race.getScale());
            } catch (NumberFormatException e) {
                ColorUtils.sendMessage(sender, "&cValor de escala numérico inválido.");
            }
        }
    }

    private void sendHelp(CommandSender sender) {
        ColorUtils.sendMessage(sender, "&8================ [ AYUDA DE RAZAS RPG ] ================");
        ColorUtils.sendMessage(sender, "&e/raza (o /raza menu) &7- Abre el Hub Principal RPG completo.");
        ColorUtils.sendMessage(sender, "&e/razas (o /raza elegir) &7- Abre la cuadrícula de selección de 16 razas.");
        ColorUtils.sendMessage(sender, "&e/raza perfil | habilidades &7- Ficha RPG y Libro de Habilidades.");
        ColorUtils.sendMessage(sender, "&e/raza talentos | misiones | logros &7- Talentos, misiones y títulos.");
        ColorUtils.sendMessage(sender, "&e/raza cosmeticos | alas vista | pack &7- Alas 3D, coronas y pack.");
        ColorUtils.sendMessage(sender, "&e/raza items | ajustes | enciclopedia &7- Reliquias, ajustes y códice.");
        ColorUtils.sendMessage(sender, "&e/raza habilidad | transformar | mochila &7- Poderes y mochila táctica.");
        ColorUtils.sendMessage(sender, "&e/raza info [raza] | subirnivel &7- Info de raza y ascenso de nivel.");
        if (sender.hasPermission("cytricosraces.admin")) {
            ColorUtils.sendMessage(sender, "&c/raza admin ayuda | /raza dar &7- Comandos administrativos.");
        }
        ColorUtils.sendMessage(sender, "&8=======================================================");
    }

    private void sendAdminHelp(CommandSender sender) {
        ColorUtils.sendMessage(sender, "&8==================== [ CYTRICOS RPG - AYUDA ADMIN ] ====================");
        ColorUtils.sendMessage(sender, "&e/raza admin menu | diagnostico &7- Panel GUI admin y diagnóstico técnico.");
        ColorUtils.sendMessage(sender, "&e/raza dar <reliquia|alas|token> <jug> [id] &7- Entrega directa de ítems.");
        ColorUtils.sendMessage(sender, "&e/raza admin darreliquia <jug> <id> | daralas <jug> <id> &7- Alias directos.");
        ColorUtils.sendMessage(sender, "&e/raza admin maxvuelotransform <Y> | killsportalento <n> &7- Reglas en vivo.");
        ColorUtils.sendMessage(sender, "&e/raza admin cambios parar | iniciar &7- Congela/reanuda cambios de raza.");
        ColorUtils.sendMessage(sender, "&e/raza admin editar <raza> | editarnivel <raza> <1-6> &7- Edita parámetros.");
        ColorUtils.sendMessage(sender, "&e/raza admin estableceralturavuelo <fairy|angel> <alt> &7- Techo de vuelo.");
        ColorUtils.sendMessage(sender, "&e/raza admin perdonarvuelo <jugador> &7- Restablece el vuelo a un jugador.");
        ColorUtils.sendMessage(sender, "&e/raza admin establecerpreciocambio <monto> &7- Precio Vault de cambio.");
        ColorUtils.sendMessage(sender, "&e/raza admin darobjetocambio <jugador> &7- Entrega Esencia de Reencarnación.");
        ColorUtils.sendMessage(sender, "&e/raza admin establecerjugador <jug> <raza|none> [lvl] &7- Forzar raza.");
        ColorUtils.sendMessage(sender, "&e/raza admin reiniciar <jugador> | recargar &7- Reset a Sin Raza / recarga.");
        ColorUtils.sendMessage(sender, "&8========================================================================");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            List<String> list = new ArrayList<>(List.of(
                    "menu", "gui", "elegir", "guia", "habilidad", "habilidades", "mochila",
                    "transformar", "info", "perfil", "talentos", "misiones", "logros",
                    "cosmeticos", "alas", "items", "reliquias", "ajustes", "enciclopedia", "pack",
                    "estadisticas", "subirnivel", "previsualizar", "cambiarraza", "estado", "ayuda"));
            if (sender.hasPermission("cytricosraces.admin")) {
                list.add("admin");
                list.add("dar");
            }
            if (sender.hasPermission("cytricosraces.creator")) {
                list.add("creador");
            }
            return list.stream().filter(s -> s.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        }

        if (args.length == 2 && (args[0].equalsIgnoreCase("alas") || args[0].equalsIgnoreCase("cosmeticos"))) {
            return List.of("vista").stream().filter(s -> s.startsWith(args[1].toLowerCase())).collect(Collectors.toList());
        }

        if (args.length == 2 && (args[0].equalsIgnoreCase("perfil") || args[0].equalsIgnoreCase("estadisticas"))) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase())).collect(Collectors.toList());
        }

        if (args.length == 2 && (args[0].equalsIgnoreCase("previsualizar") || args[0].equalsIgnoreCase("cambiarraza") || args[0].equalsIgnoreCase("info"))) {
            return collectRaceSuggestions(args[1]);
        }

        if (args[0].equalsIgnoreCase("dar") && sender.hasPermission("cytricosraces.admin")) {
            if (args.length == 2) {
                return List.of("reliquia", "alas", "token").stream().filter(s -> s.startsWith(args[1].toLowerCase())).collect(Collectors.toList());
            }
            if (args.length == 3) {
                return Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(n -> n.toLowerCase().startsWith(args[2].toLowerCase())).collect(Collectors.toList());
            }
            if (args.length == 4) {
                if (args[1].equalsIgnoreCase("reliquia") || args[1].equalsIgnoreCase("item")) {
                    List<String> ids = new ArrayList<>();
                    plugin.getCustomItemManager().getAllCustomItems().forEach(s -> ids.add(s.id));
                    return ids.stream().filter(id -> id.startsWith(args[3].toLowerCase())).collect(Collectors.toList());
                }
                if (args[1].equalsIgnoreCase("alas")) {
                    List<String> ids = new ArrayList<>();
                    plugin.getWingsManager().getAllWings().forEach(w -> ids.add(w.id));
                    return ids.stream().filter(id -> id.startsWith(args[3].toLowerCase())).collect(Collectors.toList());
                }
            }
        }

        if (args[0].equalsIgnoreCase("admin") && sender.hasPermission("cytricosraces.admin")) {
            if (args.length == 2) {
                return List.of("ayuda", "menu", "diagnostico", "inspeccionarcooldowns", "darreliquia", "daritem", "daralas",
                                "maxvuelotransform", "killsportalento", "cambios", "editar", "editarnivel",
                                "estableceralturavuelo", "perdonarvuelo", "establecerjugador", "reiniciar", "inspeccionar",
                                "darobjetocambio", "establecerslot", "recargar", "establecerpreciocambio",
                                "establecerobjetocambio", "concedercambiogratis", "revocarcambiogratis")
                        .stream().filter(s -> s.startsWith(args[1].toLowerCase())).collect(Collectors.toList());
            }
            if (args.length == 3 && (args[1].equalsIgnoreCase("editar") || args[1].equalsIgnoreCase("editarnivel") || args[1].equalsIgnoreCase("establecerslot"))) {
                return collectRaceSuggestions(args[2]);
            }
            if (args.length == 4 && (args[1].equalsIgnoreCase("daritem") || args[1].equalsIgnoreCase("darreliquia"))) {
                List<String> ids = new ArrayList<>();
                ids.add("reincarnation_essence");
                plugin.getCustomItemManager().getAllCustomItems().forEach(s -> ids.add(s.id));
                return ids.stream().filter(id -> id.startsWith(args[3].toLowerCase())).collect(Collectors.toList());
            }
            if (args.length == 4 && args[1].equalsIgnoreCase("daralas")) {
                List<String> ids = new ArrayList<>();
                plugin.getWingsManager().getAllWings().forEach(w -> ids.add(w.id));
                return ids.stream().filter(id -> id.startsWith(args[3].toLowerCase())).collect(Collectors.toList());
            }
            if (args.length == 4 && args[1].equalsIgnoreCase("establecerjugador")) {
                List<String> list = collectRaceSuggestions(args[3]);
                if ("none".startsWith(args[3].toLowerCase())) list.add("none");
                return list;
            }
            if (args.length == 5 && args[1].equalsIgnoreCase("establecerjugador")) {
                return List.of("1", "2", "3", "4", "5", "6").stream().filter(s -> s.startsWith(args[4])).collect(Collectors.toList());
            }
            if (args.length == 3 && args[1].equalsIgnoreCase("estableceralturavuelo")) {
                return List.of("fairy", "angel", "hada", "ángel").stream().filter(s -> s.startsWith(args[2].toLowerCase())).collect(Collectors.toList());
            }
            if (args.length == 3 && args[1].equalsIgnoreCase("cambios")) {
                return List.of("parar", "iniciar");
            }
            if (args.length == 3 && (args[1].equalsIgnoreCase("perdonarvuelo") || args[1].equalsIgnoreCase("inspeccionar")
                    || args[1].equalsIgnoreCase("inspeccionarcooldowns") || args[1].equalsIgnoreCase("reiniciar")
                    || args[1].equalsIgnoreCase("concedercambiogratis") || args[1].equalsIgnoreCase("revocarcambiogratis")
                    || args[1].equalsIgnoreCase("darobjetocambio") || args[1].equalsIgnoreCase("establecerjugador")
                    || args[1].equalsIgnoreCase("daritem") || args[1].equalsIgnoreCase("darreliquia")
                    || args[1].equalsIgnoreCase("daralas"))) {
                return Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(n -> n.toLowerCase().startsWith(args[2].toLowerCase())).collect(Collectors.toList());
            }
        }

        return completions;
    }

    private List<String> collectRaceSuggestions(String prefix) {
        Set<String> raceNames = new LinkedHashSet<>();
        for (Race r : plugin.getRaceManager().getAllRaces()) {
            raceNames.add(r.getId());
            raceNames.add(ColorUtils.strip(r.getDisplayName()).toLowerCase());
        }
        return raceNames.stream().filter(id -> id.startsWith(prefix.toLowerCase())).collect(Collectors.toList());
    }
}

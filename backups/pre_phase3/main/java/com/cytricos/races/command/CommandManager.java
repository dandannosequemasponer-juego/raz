package com.cytricos.races.command;

import com.cytricos.races.CytricosRaces;
import com.cytricos.races.model.ChangeMethod;
import com.cytricos.races.model.PlayerData;
import com.cytricos.races.model.Race;
import com.cytricos.races.model.RaceLevel;
import com.cytricos.races.util.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Gestor completo de comandos 100% en español para CYTRICOS RPG.
 * Maneja comandos de Jugador (/raza gui, info, stats, subirnivel...)
 * y de Administración (/raza admin cambios, menu, editar, darobjetocambio...).
 * Lead Developers: Dandan_13 & Wolfit12
 */
public class CommandManager implements CommandExecutor, TabCompleter {
    private final CytricosRaces plugin;

    public CommandManager(CytricosRaces plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Ejecución de /razas directamente abre el menú
        if (label.equalsIgnoreCase("razas") && args.length == 0) {
            if (sender instanceof Player p) {
                plugin.getPlayerGUI().openSelectionMenu(p);
            } else {
                sender.sendMessage(ChatColor.RED + "Comando solo disponible para jugadores.");
            }
            return true;
        }

        if (args.length == 0) {
            if (sender instanceof Player p) {
                plugin.getPlayerGUI().openSelectionMenu(p);
            } else {
                sendHelp(sender);
            }
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "gui", "menu" -> {
                if (sender instanceof Player p) {
                    plugin.getPlayerGUI().openSelectionMenu(p);
                } else {
                    sender.sendMessage(ChatColor.RED + "Solo jugadores.");
                }
            }
            case "confirmar" -> {
                if (sender instanceof Player p) {
                    PlayerData data = plugin.getPlayerDataManager().get(p.getUniqueId());
                    plugin.getPlayerGUI().openConfirmationMenu(p, data.getRace());
                }
            }
            case "info" -> handleInfo(sender, args);
            case "estadisticas", "stats" -> handleStats(sender, args);
            case "perfil", "profile" -> handleProfile(sender, args);
            case "subirnivel" -> handleLevelUp(sender);
            case "habilidad", "skill" -> handleAbility(sender);
            case "mochila", "backpack" -> handleBackpack(sender);
            case "guia", "guide" -> handleGuide(sender);
            case "previsualizar" -> handlePreview(sender, args);
            case "cambiarraza" -> handleRaceChange(sender, args);
            case "estado" -> handleStatus(sender);
            case "transformar", "transform", "polimorfia" -> handleTransformToggle(sender);
            case "ayuda" -> sendHelp(sender);
            case "admin" -> handleAdmin(sender, args);
            case "creador" -> handleCreator(sender, args);
            default -> sendHelp(sender);
        }

        return true;
    }


    private void handleInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.YELLOW + "Uso: /raza info <raza>");
            return;
        }
        Race race = plugin.getRaceManager().resolveRace(args[1]);
        if (race == null) {
            sender.sendMessage(ChatColor.RED + "La raza '" + args[1] + "' no existe.");
            return;
        }

        sender.sendMessage(ChatColor.DARK_GRAY + "========================================");
        sender.sendMessage(ChatColor.GOLD + "§lRAZA: " + ChatColor.translateAlternateColorCodes('&', race.getDisplayName()));
        sender.sendMessage(ChatColor.GRAY + "Escala Física: " + ChatColor.YELLOW + race.getScale() + "x");
        sender.sendMessage(ChatColor.GRAY + "Vida Base: " + ChatColor.RED + (race.getBaseHealthHearts() * 2) + " HP");
        sender.sendMessage(ChatColor.GRAY + "Velocidad: " + ChatColor.AQUA + (int)(race.getBaseSpeedMultiplier() * 100) + "%");
        sender.sendMessage(ChatColor.GRAY + "Multiplicador de Daño: " + ChatColor.LIGHT_PURPLE + (int)(race.getBaseDamageMultiplier() * 100) + "%");
        sender.sendMessage(ChatColor.YELLOW + "--- Progresión de 6 Niveles ---");
        for (int i = 1; i <= 6; i++) {
            RaceLevel lvl = race.getLevel(i);
            if (lvl != null) {
                sender.sendMessage(ChatColor.GOLD + "Lvl " + i + " " + lvl.getName() + ChatColor.GRAY + " -> " + lvl.getPerks());
            }
        }
        sender.sendMessage(ChatColor.DARK_GRAY + "========================================");
    }

    private void handleStats(CommandSender sender, String[] args) {
        Player target;
        if (args.length > 1 && sender.hasPermission("cytricosraces.admin")) {
            target = Bukkit.getPlayer(args[1]);
        } else if (sender instanceof Player p) {
            target = p;
        } else {
            sender.sendMessage(ChatColor.RED + "Especifica un jugador: /raza stats <jugador>");
            return;
        }

        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Jugador no encontrado.");
            return;
        }

        PlayerData data = plugin.getPlayerDataManager().get(target.getUniqueId());
        Race race = plugin.getRaceManager().getRace(data.getRace());
        String raceName = race != null ? race.getDisplayName() : "Ninguna";

        sender.sendMessage(ChatColor.DARK_GRAY + "====== [ ESTADÍSTICAS RACIALES ] ======");
        sender.sendMessage(ChatColor.YELLOW + "Jugador: " + ChatColor.WHITE + target.getName());
        sender.sendMessage(ChatColor.YELLOW + "Raza Activa: " + ChatColor.translateAlternateColorCodes('&', raceName));
        sender.sendMessage(ChatColor.YELLOW + "Nivel Racial: " + ChatColor.GOLD + data.getLevel() + " / 6");
        sender.sendMessage(ChatColor.YELLOW + "Escala Hitbox: " + ChatColor.GREEN + (race != null ? race.getScale() : 1.0) + "x");
        sender.sendMessage(ChatColor.YELLOW + "Vida Máxima: " + ChatColor.RED + (int)target.getMaxHealth() + " HP");
        sender.sendMessage(ChatColor.YELLOW + "Cambio Gratuito Único: " + (data.hasFreeRaceChangeAvailable() ? ChatColor.GREEN + "Disponible" : ChatColor.RED + "Consumido"));
        sender.sendMessage(ChatColor.DARK_GRAY + "=======================================");
    }

    private void handleLevelUp(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Comando solo disponible para jugadores.");
            return;
        }
        plugin.getRaceManager().processLevelUp(player);
    }

    private void handleAbility(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Comando solo disponible para jugadores.");
            return;
        }
        if (plugin.getRaceAbilitiesListener() != null) {
            plugin.getRaceAbilitiesListener().triggerActiveAbility(player);
        }
    }

    private void handleBackpack(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Comando solo disponible para jugadores.");
            return;
        }
        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        if (!data.getRace().equalsIgnoreCase("human") || data.getLevel() < 4) {
            ColorUtils.sendMessage(player, "&cLa mochila táctica es una ventaja exclusiva para Humanos de Nivel 4 o superior.");
            return;
        }
        org.bukkit.inventory.Inventory backpack = Bukkit.createInventory(player, 9, ColorUtils.component("&8[&6CYTRICOS&8] &7Mochila Táctica"));
        player.openInventory(backpack);
        player.getWorld().playSound(player.getLocation(), org.bukkit.Sound.ITEM_ARMOR_EQUIP_LEATHER, 1.0f, 1.0f);
        ColorUtils.sendMessage(player, "&6&l[MOCHILA] &7Mochila táctica desplegada (9 casillas).");
    }

    private void handlePreview(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Solo jugadores.");
            return;
        }
        if (args.length < 2) {
            player.sendMessage(ChatColor.YELLOW + "Uso: /raza previsualizar <raza>");
            return;
        }
        Race race = plugin.getRaceManager().resolveRace(args[1]);
        if (race == null) {
            player.sendMessage(ChatColor.RED + "Raza no encontrada.");
            return;
        }
        player.sendMessage(ChatColor.GOLD + "§l[PREVIEW] §7Raza: " + race.getDisplayName() + " §7- Escala: §e" + race.getScale() + "x");
        plugin.getPlayerGUI().openConfirmationMenu(player, race.getId());
    }

    private void handleRaceChange(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Solo jugadores.");
            return;
        }
        if (args.length < 2) {
            player.sendMessage(ChatColor.YELLOW + "Uso: /raza cambiarraza <raza> [gratis|dinero|objeto]");
            return;
        }

        Race resolved = plugin.getRaceManager().resolveRace(args[1]);
        if (resolved == null) {
            player.sendMessage(ChatColor.RED + "Raza '" + args[1] + "' no encontrada.");
            return;
        }
        String raceId = resolved.getId();
        ChangeMethod method = null;
        if (args.length >= 3) {
            method = ChangeMethod.fromString(args[2]);
        }

        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        if (method == null) {
            if (data.hasFreeRaceChangeAvailable()) method = ChangeMethod.FREE;
            else if (plugin.getCustomItemManager().hasChangeTokenItem(player)) method = ChangeMethod.SPECIAL_ITEM;
            else method = ChangeMethod.ECONOMY;
        }

        plugin.getRaceManager().processRaceChange(player, raceId, method);
    }

    private void handleTransformToggle(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Comando solo disponible para jugadores.");
            return;
        }
        if (plugin.getRaceAbilitiesListener() != null) {
            plugin.getRaceAbilitiesListener().toggleTransformation(player);
        }
    }

    private void handleStatus(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Solo jugadores.");
            return;
        }

        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        double cost = plugin.getConfigManager().getRaceChangeCost();
        boolean hasToken = plugin.getCustomItemManager().hasChangeTokenItem(player);

        player.sendMessage(ChatColor.DARK_GRAY + "====== [ ESTADO DE CAMBIO DE RAZA ] ======");
        player.sendMessage(ChatColor.YELLOW + "Sistema Global: " + (plugin.getConfigManager().isGlobalChangesEnabled() ? ChatColor.GREEN + "Activo" : ChatColor.RED + "Pausado"));
        player.sendMessage(ChatColor.YELLOW + "Cambio Gratuito Único: " + (data.hasFreeRaceChangeAvailable() ? ChatColor.GREEN + "Disponible" : ChatColor.RED + "Consumido"));
        player.sendMessage(ChatColor.YELLOW + "Precio en Dinero Vault: " + ChatColor.GOLD + "$" + String.format("%.0f", cost));
        player.sendMessage(ChatColor.YELLOW + "Esencia de Reencarnación en Mochila: " + (hasToken ? ChatColor.GREEN + "En posesión" : ChatColor.RED + "No"));
        player.sendMessage(ChatColor.DARK_GRAY + "==========================================");
    }

    private void handleAdmin(CommandSender sender, String[] args) {
        if (!sender.hasPermission("cytricosraces.admin")) {
            sender.sendMessage(ChatColor.RED + "No tienes permiso para usar comandos administrativos.");
            return;
        }

        if (args.length < 2 || args[1].equalsIgnoreCase("ayuda")) {
            sendAdminHelp(sender);
            return;
        }

        String action = args[1].toLowerCase();

        switch (action) {
            case "menu", "gui" -> {
                if (sender instanceof Player p) {
                    plugin.getAdminGUI().openAdminMainMenu(p);
                } else {
                    sender.sendMessage(ChatColor.RED + "Solo jugadores.");
                }
            }
            case "cambios" -> {
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.YELLOW + "Uso: /raza admin cambios parar | iniciar");
                    return;
                }
                if (args[2].equalsIgnoreCase("parar")) {
                    plugin.getConfigManager().setGlobalChangesEnabled(false);
                    sender.sendMessage(ChatColor.RED + "§l[CYTRICOS] §7El sistema de cambios de raza ha sido §cPAUSADO§7 globalmente.");
                } else if (args[2].equalsIgnoreCase("iniciar")) {
                    plugin.getConfigManager().setGlobalChangesEnabled(true);
                    sender.sendMessage(ChatColor.GREEN + "§l[CYTRICOS] §7El sistema de cambios de raza ha sido §aACTIVADO§7 globalmente.");
                }
            }
            case "editar" -> {
                if (args.length < 3 || !(sender instanceof Player p)) {
                    sender.sendMessage(ChatColor.YELLOW + "Uso: /raza admin editar <raza>");
                    return;
                }
                Race race = plugin.getRaceManager().resolveRace(args[2]);
                if (race != null) {
                    plugin.getAdminGUI().openRaceVariablesEditor(p, race);
                } else {
                    sender.sendMessage(ChatColor.RED + "Raza no encontrada.");
                }
            }
            case "editarnivel" -> {
                if (args.length < 4 || !(sender instanceof Player p)) {
                    sender.sendMessage(ChatColor.YELLOW + "Uso: /raza admin editarnivel <raza> <1-6>");
                    return;
                }
                Race race = plugin.getRaceManager().resolveRace(args[2]);
                try {
                    int lvl = Integer.parseInt(args[3]);
                    if (race != null && lvl >= 1 && lvl <= 6) {
                        plugin.getAdminGUI().openLevelEditor(p, race, lvl);
                    } else {
                        sender.sendMessage(ChatColor.RED + "Nivel o raza inválidos.");
                    }
                } catch (NumberFormatException e) {
                    sender.sendMessage(ChatColor.RED + "Nivel debe ser un número entre 1 y 6.");
                }
            }
            case "establecerjugador" -> {
                if (args.length < 4) {
                    sender.sendMessage(ChatColor.YELLOW + "Uso: /raza admin establecerjugador <jugador> <raza> [nivel]");
                    return;
                }
                Player target = Bukkit.getPlayer(args[2]);
                if (target == null) {
                    sender.sendMessage(ChatColor.RED + "Jugador desconectado o no encontrado.");
                    return;
                }
                Race race = plugin.getRaceManager().resolveRace(args[3]);
                if (race == null) {
                    sender.sendMessage(ChatColor.RED + "Raza no encontrada.");
                    return;
                }
                int lvl = 1;
                if (args.length >= 5) {
                    try {
                        lvl = Integer.parseInt(args[4]);
                    } catch (NumberFormatException ignored) {}
                }
                PlayerData data = plugin.getPlayerDataManager().get(target.getUniqueId());
                data.setRace(race.getId());
                data.setLevel(lvl);
                plugin.getPlayerDataManager().saveToFile(data);
                plugin.getRaceManager().applyPassives(target, race.getId(), lvl);
                sender.sendMessage(ChatColor.GREEN + "Jugador " + target.getName() + " asignado a " + race.getDisplayName() + " Nivel " + lvl);
            }
            case "reiniciar" -> {
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.YELLOW + "Uso: /raza admin reiniciar <jugador>");
                    return;
                }
                Player target = Bukkit.getPlayer(args[2]);
                if (target != null) {
                    plugin.getPlayerDataManager().resetPlayer(target.getUniqueId());
                    PlayerData d = plugin.getPlayerDataManager().get(target.getUniqueId());
                    plugin.getRaceManager().applyPassives(target, d.getRace(), 1);
                    sender.sendMessage(ChatColor.GREEN + "Progreso racial reiniciado para " + target.getName());
                } else {
                    sender.sendMessage(ChatColor.RED + "Jugador no encontrado.");
                }
            }
            case "inspeccionar" -> {
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.YELLOW + "Uso: /raza admin inspeccionar <jugador>");
                    return;
                }
                Player target = Bukkit.getPlayer(args[2]);
                if (target != null) {
                    handleStats(sender, new String[]{"stats", target.getName()});
                }
            }
            case "darobjetocambio" -> {
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.YELLOW + "Uso: /raza admin darobjetocambio <jugador> [cantidad]");
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
                    target.getInventory().addItem(token);
                    sender.sendMessage(ChatColor.GREEN + "Entregado " + amount + "x Token de Reencarnación a " + target.getName());
                }
            }
            case "establecerslot" -> {
                if (args.length < 4) {
                    sender.sendMessage(ChatColor.YELLOW + "Uso: /raza admin establecerslot <raza> <slot_0-44>");
                    return;
                }
                try {
                    int slot = Integer.parseInt(args[3]);
                    Race targetRace = plugin.getRaceManager().resolveRace(args[2]);
                    String raceId = targetRace != null ? targetRace.getId() : args[2];
                    plugin.getRaceManager().setRaceSlot(raceId, slot);
                    sender.sendMessage(ChatColor.GREEN + "Slot de " + raceId + " actualizado a " + slot);
                } catch (NumberFormatException e) {
                    sender.sendMessage(ChatColor.RED + "El slot debe ser un entero entre 0 y 44.");
                }
            }
            case "recargar" -> {
                plugin.getConfigManager().reloadAll();
                plugin.getRaceManager().loadRaces();
                sender.sendMessage(ChatColor.GREEN + "§l[CYTRICOS] §7Configuraciones, races.yml y cachés recargados.");
            }
            case "establecerpreciocambio" -> {
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.YELLOW + "Uso: /raza admin establecerpreciocambio <monto>");
                    return;
                }
                try {
                    double amount = Double.parseDouble(args[2]);
                    plugin.getConfigManager().setRaceChangeCost(amount);
                    sender.sendMessage(ChatColor.GREEN + "Precio de cambio Vault actualizado a: $" + String.format("%.0f", amount));
                } catch (NumberFormatException e) {
                    sender.sendMessage(ChatColor.RED + "Monto inválido.");
                }
            }
            case "establecerobjetocambio" -> {
                if (sender instanceof Player p) {
                    plugin.getCustomItemManager().setHeldItemAsToken(p);
                }
            }
            case "concedercambiogratis" -> {
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.YELLOW + "Uso: /raza admin concedercambiogratis <jugador>");
                    return;
                }
                Player target = Bukkit.getPlayer(args[2]);
                if (target != null) {
                    PlayerData d = plugin.getPlayerDataManager().get(target.getUniqueId());
                    d.setFreeRaceChangeAvailable(true);
                    plugin.getPlayerDataManager().saveToFile(d);
                    sender.sendMessage(ChatColor.GREEN + "Cambio gratuito otorgado a " + target.getName());
                }
            }
            case "revocarcambiogratis" -> {
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.YELLOW + "Uso: /raza admin revocarcambiogratis <jugador>");
                    return;
                }
                Player target = Bukkit.getPlayer(args[2]);
                if (target != null) {
                    PlayerData d = plugin.getPlayerDataManager().get(target.getUniqueId());
                    d.setFreeRaceChangeAvailable(false);
                    plugin.getPlayerDataManager().saveToFile(d);
                    sender.sendMessage(ChatColor.RED + "Cambio gratuito revocado para " + target.getName());
                }
            }
            case "estableceralturavuelo" -> {
                if (args.length < 4) {
                    sender.sendMessage(ChatColor.YELLOW + "Uso: /raza admin estableceralturavuelo <fairy|angel> <altura>");
                    return;
                }
                String targetRace = args[2].toLowerCase();
                try {
                    double height = Double.parseDouble(args[3]);
                    if (targetRace.equals("fairy") || targetRace.equals("hada")) {
                        plugin.getConfig().set("flight_settings.fairy_max_height", height);
                        plugin.saveConfig();
                        sender.sendMessage(ChatColor.GREEN + "Límite de vuelo para Hada establecido a: " + height + " bloques.");
                    } else if (targetRace.equals("angel")) {
                        plugin.getConfig().set("flight_settings.angel_max_height", height);
                        plugin.saveConfig();
                        sender.sendMessage(ChatColor.GREEN + "Límite de vuelo para Ángel establecido a: " + height + " bloques.");
                    } else {
                        sender.sendMessage(ChatColor.RED + "Raza con vuelo inválida. Usa 'fairy' o 'angel'.");
                    }
                } catch (NumberFormatException e) {
                    sender.sendMessage(ChatColor.RED + "La altura debe ser un valor numérico.");
                }
            }
            case "perdonarvuelo" -> handlePardonFlight(sender, args);
            case "diagnostico", "diagnostic" -> handleDiagnostic(sender);
            case "inspeccionarcooldowns" -> handleInspectCooldowns(sender, args);
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
            sender.sendMessage(ChatColor.RED + "Especifica un jugador: /raza perfil <jugador>");
            return;
        }

        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Jugador no encontrado.");
            return;
        }

        PlayerData data = plugin.getPlayerDataManager().get(target.getUniqueId());
        Race race = plugin.getRaceManager().getRace(data.getRace());
        String raceName = race != null ? race.getDisplayName() : "Humano";

        sender.sendMessage(ChatColor.DARK_GRAY + "================ [ PERFIL RPG RACIAL ] ================");
        sender.sendMessage(ChatColor.YELLOW + "Personaje: " + ChatColor.WHITE + target.getName() + ChatColor.GRAY + " (" + target.getUniqueId() + ")");
        sender.sendMessage(ChatColor.YELLOW + "Linaje Activo: " + ChatColor.translateAlternateColorCodes('&', raceName));
        sender.sendMessage(ChatColor.YELLOW + "Nivel de Ascensión: " + ChatColor.GOLD + data.getLevel() + " / 6");
        sender.sendMessage(ChatColor.YELLOW + "Recurso Racial (" + data.getResourceName() + "): " + ChatColor.GREEN + (int)data.getRacialResource() + " / " + (int)data.getMaxRacialResource());
        sender.sendMessage(ChatColor.YELLOW + "Escala Física (Hitbox): " + ChatColor.AQUA + (race != null ? race.getScale() : 1.0) + "x");
        sender.sendMessage(ChatColor.YELLOW + "Experiencia Acumulada: " + ChatColor.LIGHT_PURPLE + target.getTotalExperience() + " EXP");
        sender.sendMessage(ChatColor.YELLOW + "Estado de Vuelo: " + (data.isFlightBanned() ? ChatColor.RED + "Sancionado (" + data.getFlightBanRemainingSeconds() + "s)" : ChatColor.GREEN + "Normal"));
        sender.sendMessage(ChatColor.YELLOW + "Metamorfosis / Disfraz: " + (data.isTransformed() ? ChatColor.LIGHT_PURPLE + "Activa" : ChatColor.GRAY + "Forma Humana"));
        sender.sendMessage(ChatColor.DARK_GRAY + "=======================================================");
    }

    private void handleDiagnostic(CommandSender sender) {
        Runtime rt = Runtime.getRuntime();
        long maxMem = rt.maxMemory() / 1024 / 1024;
        long freeMem = rt.freeMemory() / 1024 / 1024;
        long usedMem = (rt.totalMemory() - rt.freeMemory()) / 1024 / 1024;

        sender.sendMessage(ChatColor.DARK_GRAY + "============= [ DIAGNÓSTICO TÉCNICO CYTRICOS ] =============");
        sender.sendMessage(ChatColor.YELLOW + "Plugin: " + ChatColor.GREEN + "CytricosRaces v" + plugin.getDescription().getVersion() + " (Paper 26.2 / Java 25)");
        sender.sendMessage(ChatColor.YELLOW + "Almacenamiento: " + ChatColor.AQUA + (plugin.getStorageManager() != null ? plugin.getStorageManager().getStorageType().name() : "YAML Flatfile"));
        sender.sendMessage(ChatColor.YELLOW + "Jugadores en Caché: " + ChatColor.WHITE + plugin.getPlayerDataManager().getCache().size() + " cargados");
        sender.sendMessage(ChatColor.YELLOW + "Memoria JVM: " + ChatColor.LIGHT_PURPLE + usedMem + "MB usados / " + maxMem + "MB máx (Libre: " + freeMem + "MB)");
        sender.sendMessage(ChatColor.YELLOW + "Integraciones:");
        sender.sendMessage(ChatColor.GRAY + " - Vault: " + (plugin.getVaultHook() != null && plugin.getVaultHook().hasEconomy() ? ChatColor.GREEN + "Conectado" : ChatColor.RED + "Desconectado"));
        sender.sendMessage(ChatColor.GRAY + " - LuckPerms: " + (plugin.getLuckPermsHook() != null ? ChatColor.GREEN + "Conectado" : ChatColor.YELLOW + "No detectado"));
        sender.sendMessage(ChatColor.GRAY + " - PlaceholderAPI: " + (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI") ? ChatColor.GREEN + "Registrado" : ChatColor.YELLOW + "No instalado"));
        sender.sendMessage(ChatColor.GRAY + " - WorldGuard: " + (plugin.getWorldGuardHook() != null ? ChatColor.GREEN + "Protecciones Activas" : ChatColor.GRAY + "No detectado"));
        sender.sendMessage(ChatColor.GRAY + " - LibsDisguises: " + (plugin.getLibsDisguisesHook() != null && plugin.getLibsDisguisesHook().isEnabled() ? ChatColor.GREEN + "Polimorfia Lista" : ChatColor.YELLOW + "No detectado"));
        sender.sendMessage(ChatColor.DARK_GRAY + "============================================================");
    }

    private void handleInspectCooldowns(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.YELLOW + "Uso: /raza admin inspeccionarcooldowns <jugador>");
            return;
        }
        Player target = Bukkit.getPlayer(args[2]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Jugador no encontrado.");
            return;
        }
        PlayerData data = plugin.getPlayerDataManager().get(target.getUniqueId());
        sender.sendMessage(ChatColor.DARK_GRAY + "====== [ COOLDOWNS DE " + target.getName().toUpperCase() + " ] ======");
        sender.sendMessage(ChatColor.YELLOW + "Transformación / Metamorfosis: " + (data.isTransformOnCooldown() ? ChatColor.RED + data.getTransformCooldownRemainingSeconds() + "s" : ChatColor.GREEN + "Disponible"));
        sender.sendMessage(ChatColor.YELLOW + "Sanción de Vuelo: " + (data.isFlightBanned() ? ChatColor.RED + data.getFlightBanRemainingSeconds() + "s" : ChatColor.GREEN + "Sin sanción"));
        sender.sendMessage(ChatColor.YELLOW + "Recurso (" + data.getResourceName() + "): " + ChatColor.GOLD + (int)data.getRacialResource() + " / " + (int)data.getMaxRacialResource());
        sender.sendMessage(ChatColor.DARK_GRAY + "=================================================");
    }

    private void handlePardonFlight(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.YELLOW + "Uso: /raza admin perdonarvuelo <jugador>");
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
            sender.sendMessage(ChatColor.RED + "Jugador no encontrado o desconectado.");
        }
    }

    private void handleGuide(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Comando solo disponible para jugadores.");
            return;
        }

        ItemStack book = new ItemStack(org.bukkit.Material.WRITTEN_BOOK);
        org.bukkit.inventory.meta.BookMeta meta = (org.bukkit.inventory.meta.BookMeta) book.getItemMeta();
        if (meta != null) {
            meta.setTitle(ColorUtils.color("&6&lGuía de Razas CYTRICOS"));
            meta.setAuthor("CYTRICOS RPG");

            meta.addPage(ColorUtils.color("&8====== &6CYTRICOS RPG &8======\n\n" +
                    "&0Bienvenido a la &lGuía Oficial de Razas v7.0&0.\n\n" +
                    "&7El servidor cuenta con &616 razas únicas&7, cada una con escalado físico, pasivas, desventajas nativas y 6 niveles de progresión.\n\n" +
                    "&8Comando: &2/razas"));

            meta.addPage(ColorUtils.color("&8==== &bMETAMORFOSIS &8====\n\n" +
                    "&0Para activar tu &bdisfraz racial místico&0:\n\n" +
                    "&0Mantén presionado &lSHIFT (Agacharse)&0 de forma continua e inmóvil por &66.0 segundos&0.\n\n" +
                    "&7- N1 a N5: 15s a 35s.\n" +
                    "&7- N6: &b∞ Duración Infinita&7.\n" +
                    "&7- CD al revertir: 15s."));

            meta.addPage(ColorUtils.color("&8==== &eLÍMITE DE VUELO &8====\n\n" +
                    "&0El vuelo en &eHadas&0 y &eÁngeles&0 está estrictamente regulado:\n\n" +
                    "&0- &aHada&0: Techo de &e16m&0 sobre el terreno.\n" +
                    "&0- &6Ángel (N3+)&0: Techo de &e28m&0.\n\n" +
                    "&c&lPenalización:\n" +
                    "&7Superar el límite 3 veces en 10s clausura el vuelo por &c5 minutos persistentes&7."));

            meta.addPage(ColorUtils.color("&8==== &6PROGRESIÓN RACIAL &8====\n\n" +
                    "&0Cada raza cuenta con &66 niveles&0:\n\n" +
                    "&c- Nivel 1: &0Todas las desventajas están activas desde el inicio.\n" +
                    "&a- Niveles 2 al 6: &0Desbloquean nuevas ventajas, habilidades activas, atributos y sufijos de chat.\n\n" +
                    "&8Asciende en: &e/razas"));

            book.setItemMeta(meta);
        }

        player.getInventory().addItem(book);
        player.getWorld().playSound(player.getLocation(), org.bukkit.Sound.ITEM_BOOK_PAGE_TURN, 1.0f, 1.0f);
        ColorUtils.sendMessage(player, "&a&l[GUÍA] &7Se ha entregado la &6Guía Oficial de Razas &7a tu inventario.");
    }

    private void handleCreator(CommandSender sender, String[] args) {
        if (!sender.hasPermission("cytricosraces.creator")) {
            sender.sendMessage(ChatColor.RED + "No tienes permiso de creador.");
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.YELLOW + "Uso: /raza creador <crear|eliminar|establecerescala|estableceratributo|agregarpasiva|establecerrequisito>");
            return;
        }

        String action = args[1].toLowerCase();
        switch (action) {
            case "establecerescala" -> {
                if (args.length < 4) {
                    sender.sendMessage(ChatColor.YELLOW + "Uso: /raza creador establecerescala <raza> <valor>");
                    return;
                }
                Race race = plugin.getRaceManager().getRace(args[2]);
                if (race != null) {
                    try {
                        double scale = Double.parseDouble(args[3]);
                        race.setScale(scale);
                        plugin.getRaceManager().saveRaceParameter(race);
                        sender.sendMessage(ChatColor.GREEN + "Escala de " + race.getId() + " establecida a: " + race.getScale());
                    } catch (NumberFormatException e) {
                        sender.sendMessage(ChatColor.RED + "Valor de escala numérico inválido.");
                    }
                }
            }
            default -> sender.sendMessage(ChatColor.YELLOW + "Comando de creador recibido.");
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.DARK_GRAY + "================ [ AYUDA DE RAZAS ] ================");
        sender.sendMessage(ChatColor.YELLOW + "/razas (o /raza gui) " + ChatColor.GRAY + "- Abre el menú de selección de razas.");
        sender.sendMessage(ChatColor.YELLOW + "/raza guia " + ChatColor.GRAY + "- Recibe el libro guía oficial de razas.");
        sender.sendMessage(ChatColor.YELLOW + "/raza confirmar " + ChatColor.GRAY + "- Abre menú interactivo de confirmación.");
        sender.sendMessage(ChatColor.YELLOW + "/raza habilidad " + ChatColor.GRAY + "- Desata la habilidad activa de tu linaje racial.");
        sender.sendMessage(ChatColor.YELLOW + "/raza mochila " + ChatColor.GRAY + "- Abre tu mochila táctica (Humanos Nivel 4+).");
        sender.sendMessage(ChatColor.YELLOW + "/raza info [raza] " + ChatColor.GRAY + "- Muestra información de una raza y sus 6 niveles.");
        sender.sendMessage(ChatColor.YELLOW + "/raza estadisticas " + ChatColor.GRAY + "- Muestra tus estadísticas raciales activas.");
        sender.sendMessage(ChatColor.YELLOW + "/raza subirnivel " + ChatColor.GRAY + "- Asciende al siguiente nivel de tu raza.");
        sender.sendMessage(ChatColor.YELLOW + "/raza transformar " + ChatColor.GRAY + "- Alterna tu metamorfosis / polimorfia racial.");
        sender.sendMessage(ChatColor.YELLOW + "/raza cambiarraza <raza> [metodo] " + ChatColor.GRAY + "- Solicita cambio de raza.");
        sender.sendMessage(ChatColor.YELLOW + "/raza estado " + ChatColor.GRAY + "- Consulta estado de tokens y costes.");
        if (sender.hasPermission("cytricosraces.admin")) {
            sender.sendMessage(ChatColor.RED + "/raza admin ayuda " + ChatColor.GRAY + "- Compendio de comandos de administración.");
        }
        sender.sendMessage(ChatColor.DARK_GRAY + "===================================================");
    }

    private void sendAdminHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.DARK_GRAY + "==================== [ CYTRICOS RPG - AYUDA ADMIN ] ====================");
        sender.sendMessage(ChatColor.YELLOW + "/raza admin cambios parar " + ChatColor.GRAY + "- Congela el sistema de cambios de raza globalmente.");
        sender.sendMessage(ChatColor.YELLOW + "/raza admin cambios iniciar " + ChatColor.GRAY + "- Reanuda la posibilidad de cambiar de raza.");
        sender.sendMessage(ChatColor.YELLOW + "/raza admin menu " + ChatColor.GRAY + "- Abre la GUI administrativa de arrastrar slots y edición.");
        sender.sendMessage(ChatColor.YELLOW + "/raza admin editar <raza> " + ChatColor.GRAY + "- Submenú de atributos, pasivas y hitbox de la raza.");
        sender.sendMessage(ChatColor.YELLOW + "/raza admin editarnivel <raza> <1-6> " + ChatColor.GRAY + "- Configura requisitos y costes por nivel.");
        sender.sendMessage(ChatColor.YELLOW + "/raza admin estableceralturavuelo <fairy|angel> <alt> " + ChatColor.GRAY + "- Configura el techo de vuelo.");
        sender.sendMessage(ChatColor.YELLOW + "/raza admin perdonarvuelo <jugador> " + ChatColor.GRAY + "- Restablece el vuelo y perdona la sanción a un jugador.");
        sender.sendMessage(ChatColor.YELLOW + "/raza admin establecerpreciocambio <monto> " + ChatColor.GRAY + "- Define el valor $X del cambio por Vault.");
        sender.sendMessage(ChatColor.YELLOW + "/raza admin establecerobjetocambio " + ChatColor.GRAY + "- Registra el ítem de la mano como Token Especial.");
        sender.sendMessage(ChatColor.YELLOW + "/raza admin darobjetocambio <jugador> " + ChatColor.GRAY + "- Entrega el Token especial a un jugador.");
        sender.sendMessage(ChatColor.YELLOW + "/raza admin concedercambiogratis <jugador> " + ChatColor.GRAY + "- Otorga 1 cambio gratuito a un jugador.");
        sender.sendMessage(ChatColor.YELLOW + "/raza admin revocarcambiogratis <jugador> " + ChatColor.GRAY + "- Cancela el cambio gratuito a un jugador.");
        sender.sendMessage(ChatColor.YELLOW + "/raza admin establecerjugador <jugador> <raza> " + ChatColor.GRAY + "- Forzar raza y nivel a un jugador.");
        sender.sendMessage(ChatColor.YELLOW + "/raza admin reiniciar <jugador> " + ChatColor.GRAY + "- Reinicia por completo los datos raciales.");
        sender.sendMessage(ChatColor.YELLOW + "/raza admin inspeccionar <jugador> " + ChatColor.GRAY + "- GUI con historial y estado técnico del jugador.");
        sender.sendMessage(ChatColor.YELLOW + "/raza admin recargar " + ChatColor.GRAY + "- Recarga configuraciones y lenguaje en vivo.");
        sender.sendMessage(ChatColor.DARK_GRAY + "========================================================================");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            List<String> list = new ArrayList<>(List.of("gui", "guia", "guide", "habilidad", "mochila", "transformar", "info", "perfil", "estadisticas", "subirnivel", "previsualizar", "cambiarraza", "estado", "ayuda"));
            if (sender.hasPermission("cytricosraces.admin")) {
                list.add("admin");
            }
            if (sender.hasPermission("cytricosraces.creator")) {
                list.add("creador");
            }
            return list.stream().filter(s -> s.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        }

        if (args.length == 2 && (args[0].equalsIgnoreCase("perfil") || args[0].equalsIgnoreCase("estadisticas") || args[0].equalsIgnoreCase("info"))) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase())).collect(Collectors.toList());
        }

        if (args.length == 2 && (args[0].equalsIgnoreCase("previsualizar") || args[0].equalsIgnoreCase("cambiarraza"))) {
            Set<String> raceNames = new LinkedHashSet<>();
            for (Race r : plugin.getRaceManager().getAllRaces()) {
                raceNames.add(r.getId());
                raceNames.add(ChatColor.stripColor(ColorUtils.color(r.getDisplayName())).toLowerCase());
            }
            return raceNames.stream().filter(id -> id.startsWith(args[1].toLowerCase())).collect(Collectors.toList());
        }

        if (args[0].equalsIgnoreCase("admin") && sender.hasPermission("cytricosraces.admin")) {
            if (args.length == 2) {
                return List.of("ayuda", "menu", "diagnostico", "inspeccionarcooldowns", "cambios", "editar", "editarnivel", "estableceralturavuelo", "perdonarvuelo", "establecerjugador", "reiniciar", "inspeccionar", "darobjetocambio", "establecerslot", "recargar", "establecerpreciocambio", "establecerobjetocambio", "concedercambiogratis", "revocarcambiogratis")
                        .stream().filter(s -> s.startsWith(args[1].toLowerCase())).collect(Collectors.toList());
            }
            if (args.length == 3 && (args[1].equalsIgnoreCase("editar") || args[1].equalsIgnoreCase("editarnivel") || args[1].equalsIgnoreCase("establecerslot"))) {
                Set<String> raceNames = new LinkedHashSet<>();
                for (Race r : plugin.getRaceManager().getAllRaces()) {
                    raceNames.add(r.getId());
                    raceNames.add(ChatColor.stripColor(ColorUtils.color(r.getDisplayName())).toLowerCase());
                }
                return raceNames.stream().filter(id -> id.startsWith(args[2].toLowerCase())).collect(Collectors.toList());
            }
            if (args.length == 4 && args[1].equalsIgnoreCase("establecerjugador")) {
                Set<String> raceNames = new LinkedHashSet<>();
                for (Race r : plugin.getRaceManager().getAllRaces()) {
                    raceNames.add(r.getId());
                    raceNames.add(ChatColor.stripColor(ColorUtils.color(r.getDisplayName())).toLowerCase());
                }
                return raceNames.stream().filter(id -> id.startsWith(args[3].toLowerCase())).collect(Collectors.toList());
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
            if (args.length == 3 && (args[1].equalsIgnoreCase("perdonarvuelo") || args[1].equalsIgnoreCase("inspeccionar") || args[1].equalsIgnoreCase("inspeccionarcooldowns") || args[1].equalsIgnoreCase("reiniciar") || args[1].equalsIgnoreCase("concedercambiogratis") || args[1].equalsIgnoreCase("revocarcambiogratis") || args[1].equalsIgnoreCase("darobjetocambio") || args[1].equalsIgnoreCase("establecerjugador"))) {
                return Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(n -> n.toLowerCase().startsWith(args[2].toLowerCase())).collect(Collectors.toList());
            }
        }

        return completions;
    }
}


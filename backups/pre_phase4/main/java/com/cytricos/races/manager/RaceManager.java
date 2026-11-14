package com.cytricos.races.manager;

import com.cytricos.races.CytricosRaces;
import com.cytricos.races.model.ChangeMethod;
import com.cytricos.races.model.PlayerData;
import com.cytricos.races.model.Race;
import com.cytricos.races.model.RaceLevel;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.types.PrefixNode;
import net.luckperms.api.node.types.SuffixNode;
import com.cytricos.races.util.ColorUtils;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Gestor central de razas, progresion de 6 niveles, atributos fisicos y pasivas para CYTRICOS RPG.
 * Lead Developers: Dandan_13 & Wolfit12
 */
public class RaceManager {
    private final CytricosRaces plugin;
    private final Map<String, Race> races = new ConcurrentHashMap<>();
    private final Map<Integer, Race> slotMap = new ConcurrentHashMap<>();

    public RaceManager(CytricosRaces plugin) {
        this.plugin = plugin;
    }

    public void loadRaces() {
        races.clear();
        slotMap.clear();

        FileConfiguration config = plugin.getConfigManager().getRacesConfig();
        ConfigurationSection section = config.getConfigurationSection("races");
        if (section == null) {
            plugin.getLogger().warning("No se encontró la sección 'races' en races.yml.");
            return;
        }

        for (String id : section.getKeys(false)) {
            ConfigurationSection raceSec = section.getConfigurationSection(id);
            if (raceSec == null) continue;

            String displayName = raceSec.getString("display_name", id);
            String matName = raceSec.getString("material", "STONE");
            Material mat = Material.matchMaterial(matName);
            if (mat == null) mat = Material.STONE;

            int slot = raceSec.getInt("slot", 0);
            double scale = raceSec.getDouble("scale", 1.0);
            double baseHealth = raceSec.getDouble("base_health_hearts", 10.0);
            double baseSpeed = raceSec.getDouble("base_speed_mult", 1.0);
            double baseDamage = raceSec.getDouble("base_damage_mult", 1.0);

            Race race = new Race(id, displayName, mat, slot, scale, baseHealth, baseSpeed, baseDamage);

            for (String imm : raceSec.getStringList("immunities")) {
                race.addImmunity(imm);
            }
            for (String vuln : raceSec.getStringList("vulnerabilities")) {
                race.addVulnerability(vuln);
            }

            ConfigurationSection levelsSec = raceSec.getConfigurationSection("levels");
            if (levelsSec != null) {
                for (String lvlKey : levelsSec.getKeys(false)) {
                    try {
                        int lvl = Integer.parseInt(lvlKey);
                        ConfigurationSection lSec = levelsSec.getConfigurationSection(lvlKey);
                        if (lSec != null) {
                            String lName = lSec.getString("name", "Nivel " + lvl);
                            String subprefix = lSec.getString("subprefix", "&8[&7Nivel " + lvl + "&8]");
                            String perks = lSec.getString("perks", "");
                            String disadvantages = lSec.getString("disadvantages", "");
                            String reqItemName = lSec.getString("required_item", "AIR");
                            Material reqItem = Material.matchMaterial(reqItemName);
                            if (reqItem == null) reqItem = Material.AIR;
                            int reqAmount = lSec.getInt("required_item_amount", 0);
                            double costMoney = lSec.getDouble("cost_money", 0.0);

                            RaceLevel raceLevel = new RaceLevel(lvl, lName, subprefix, perks, disadvantages, reqItem, reqAmount, costMoney);
                            race.addLevel(raceLevel);
                        }
                    } catch (NumberFormatException ignored) {}
                }
            }

            races.put(id.toLowerCase(), race);
            slotMap.put(slot, race);
        }

        plugin.getLogger().info("Se cargaron " + races.size() + " razas exitosamente.");
    }

    public Collection<Race> getAllRaces() {
        return Collections.unmodifiableCollection(races.values());
    }

    public Race getRace(String id) {
        if (id == null) return null;
        return races.get(id.toLowerCase());
    }

    /**
     * Resuelve una raza admitiendo nombres en español (con o sin tildes) o IDs internos en inglés.
     * Ejemplo: "vampiro" -> "vampire", "enano" -> "dwarf", "duende" -> "goblin", "ángel" -> "angel".
     */
    public Race resolveRace(String query) {
        if (query == null || query.trim().isEmpty()) return null;
        String clean = query.trim().toLowerCase()
                .replace("á", "a")
                .replace("é", "e")
                .replace("í", "i")
                .replace("ó", "o")
                .replace("ú", "u")
                .replace(" ", "")
                .replace("-", "")
                .replace("_", "");

        // 1. Coincidencia directa por ID
        Race direct = races.get(clean);
        if (direct != null) return direct;

        // 2. Mapeo de alias en español
        String mappedId = switch (clean) {
            case "vampiro" -> "vampire";
            case "enano" -> "dwarf";
            case "humano" -> "human";
            case "elfo" -> "elf";
            case "gigante" -> "giant";
            case "orco" -> "orc";
            case "duende", "goblin" -> "goblin";
            case "demonio", "demonkin", "oni" -> "demon";
            case "angel", "celestial", "angelkin" -> "angel";
            case "dragon", "dragonkin", "lagarto", "draco" -> "dragonkin";
            case "bestia", "beastkin", "demihumano" -> "beastkin";
            case "hombrelobo", "lobo", "licantropo", "werewolf" -> "werewolf";
            case "zombi", "nomuerto", "muerto", "undead", "esqueleto" -> "undead";
            case "druida", "druidacitrico", "citrico", "planta" -> "druid";
            case "hada", "fairy" -> "fairy";
            case "espiritu", "yokai", "anima" -> "yokai";
            default -> null;
        };

        if (mappedId != null) {
            Race r = races.get(mappedId);
            if (r != null) return r;
        }

        // 3. Búsqueda por coincidencia en display_name
        for (Race race : races.values()) {
            String strippedDisplay = ColorUtils.strip(race.getDisplayName()).toLowerCase()
                    .replace("á", "a").replace("é", "e").replace("í", "i").replace("ó", "o").replace("ú", "u")
                    .replace(" ", "").replace("-", "").replace("_", "").replace("/", "");
            if (strippedDisplay.contains(clean) || clean.contains(strippedDisplay)) {
                return race;
            }
        }

        return null;
    }

    public Race getRaceBySlot(int slot) {
        return slotMap.get(slot);
    }

    public void setRaceSlot(String raceId, int newSlot) {
        Race race = getRace(raceId);
        if (race == null || newSlot < 0 || newSlot > 44) return;

        // Remover del slot anterior
        slotMap.remove(race.getSlot());

        // Si ya había una raza en el nuevo slot, intercambiar o desplazar
        Race existing = slotMap.get(newSlot);
        if (existing != null) {
            existing.setSlot(race.getSlot());
            slotMap.put(existing.getSlot(), existing);
            plugin.getConfigManager().getRacesConfig().set("races." + existing.getId() + ".slot", existing.getSlot());
        }

        race.setSlot(newSlot);
        slotMap.put(newSlot, race);
        plugin.getConfigManager().getRacesConfig().set("races." + race.getId() + ".slot", newSlot);
        plugin.getConfigManager().saveRacesConfig();
    }

    public void saveRaceParameter(Race race) {
        if (race == null) return;
        FileConfiguration cfg = plugin.getConfigManager().getRacesConfig();
        String path = "races." + race.getId() + ".";
        cfg.set(path + "display_name", race.getDisplayName().replace('§', '&'));
        cfg.set(path + "scale", race.getScale());
        cfg.set(path + "base_health_hearts", race.getBaseHealthHearts());
        cfg.set(path + "base_speed_mult", race.getBaseSpeedMultiplier());
        cfg.set(path + "base_damage_mult", race.getBaseDamageMultiplier());
        cfg.set(path + "immunities", new ArrayList<>(race.getImmunities()));
        cfg.set(path + "vulnerabilities", new ArrayList<>(race.getVulnerabilities()));
        plugin.getConfigManager().saveRacesConfig();
    }

    /**
     * Aplica atributos pasivos, velocidad, vida, efectos de poción, escalado y sufijo LuckPerms.
     * Respeta la regla de desventajas activas desde Nivel 1 y Perks incrementales por nivel.
     */
    public void applyPassives(Player player, String raceId, int level) {
        if (player == null || !player.isOnline()) return;
        Race race = getRace(raceId);
        if (race == null) return;

        // 1. Escala física y hitbox (Attribute.GENERIC_SCALE) con progresión especial por nivel
        double effectiveScale = race.getScale();
        if (race.getId().equalsIgnoreCase("giant")) {
            if (level <= 2) effectiveScale = 1.15;
            else if (level <= 4) effectiveScale = 1.25;
            else effectiveScale = 1.35;
        }
        ScaleManager.setPlayerRaceScale(player, effectiveScale);

        // 2. Vida máxima dinámica
        try {
            AttributeInstance healthAttr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            if (healthAttr != null) {
                double targetHealth = race.getBaseHealthHearts() * 2.0;
                // Perks de salud por ascensión
                if (race.getId().equalsIgnoreCase("dwarf") && level >= 2) targetHealth += 6.0; // +3 corazones
                if (race.getId().equalsIgnoreCase("giant")) {
                    if (level >= 6) targetHealth += 24.0; // +12 corazones
                    else if (level >= 4) targetHealth += 16.0; // +8 corazones
                    else if (level >= 2) targetHealth += 8.0; // +4 corazones
                }
                if (race.getId().equalsIgnoreCase("dragonkin")) {
                    if (level >= 6) targetHealth += 16.0; // +8 corazones
                }
                if (race.getId().equalsIgnoreCase("demon") && level >= 2) targetHealth += 4.0; // +2 corazones

                // Talento Supervivencia: Piel de Hierro (+2 HP)
                PlayerData pData = plugin.getPlayerDataManager().get(player.getUniqueId());
                if (pData != null && pData.hasTalent("surv_health")) {
                    targetHealth += 2.0;
                }

                healthAttr.setBaseValue(targetHealth);
                if (player.getHealth() > targetHealth) {
                    player.setHealth(targetHealth);
                }
            }
        } catch (Throwable ignored) {}

        // 3. Velocidad de movimiento base
        try {
            AttributeInstance speedAttr = player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
            if (speedAttr != null) {
                double speedMult = race.getBaseSpeedMultiplier();
                if (race.getId().equalsIgnoreCase("werewolf") && level >= 1) speedMult *= 1.15;
                if (race.getId().equalsIgnoreCase("beastkin") && level >= 4) speedMult *= 1.30;
                if (race.getId().equalsIgnoreCase("fairy") && level >= 5) speedMult *= 1.40;

                PlayerData pData = plugin.getPlayerDataManager().get(player.getUniqueId());
                if (pData != null && pData.hasTalent("util_speed")) {
                    speedMult *= 1.05;
                }

                speedAttr.setBaseValue(0.10 * speedMult);
            }
        } catch (Throwable ignored) {}

        // 4. Daño de ataque base
        try {
            AttributeInstance attackAttr = player.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
            if (attackAttr != null) {
                double dmgMult = race.getBaseDamageMultiplier();
                if (race.getId().equalsIgnoreCase("human") && level >= 5) dmgMult *= 1.15;

                PlayerData pData = plugin.getPlayerDataManager().get(player.getUniqueId());
                if (pData != null && pData.hasTalent("combat_dmg")) {
                    dmgMult *= 1.05;
                }

                attackAttr.setBaseValue(1.0 * dmgMult);
            }
        } catch (Throwable ignored) {}

        // 5. Permiso de vuelo (Fairy L1+ pasivo, Angel L3+ pasivo)
        if (race.getId().equalsIgnoreCase("fairy") || (race.getId().equalsIgnoreCase("angel") && level >= 3)) {
            player.setAllowFlight(true);
        } else if (player.getGameMode() == GameMode.SURVIVAL || player.getGameMode() == GameMode.ADVENTURE) {
            player.setAllowFlight(false);
            player.setFlying(false);
        }

        // 6. Aplicar Chat SUFFIX en LuckPerms (sin tocar grupo ni prefijos primarios)
        applyLuckPermsSuffix(player, race, level);

        // 7. Efectos pasivos de raza
        applyRacialPotionBuffs(player, race, level);
    }

    private void applyRacialPotionBuffs(Player player, Race race, int level) {
        // Limpiar efectos pasivos controlados
        player.removePotionEffect(PotionEffectType.FIRE_RESISTANCE);
        player.removePotionEffect(PotionEffectType.NIGHT_VISION);
        player.removePotionEffect(PotionEffectType.SLOW_FALLING);

        String id = race.getId();
        if (id.equals("demon") || id.equals("dragonkin")) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, 0, false, false));
        }
        if (id.equals("elf") && level >= 2) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 0, false, false));
        }
        if (id.equals("dwarf") && player.getLocation().getY() <= 60) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 400, 0, false, false));
            player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 400, 1, false, false));
        }
        if (id.equals("yokai")) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, Integer.MAX_VALUE, 0, false, false));
        }
    }

    /**
     * Inyecta la etiqueta racial exclusivamente como un Chat SUFFIX en LuckPerms (SuffixNode).
     * Formato resultante en chat: [Rango] [NombreJugador] [SufijoRaza] : Mensaje
     * Limpia sufijos raciales anteriores con alta prioridad e inyecta el sufijo dinámico.
     */
    public void applyLuckPermsSuffix(Player player, Race race, int level) {
        if (!plugin.isLuckPermsAvailable() || player == null || race == null) return;

        RaceLevel rLevel = race.getLevel(level);
        if (rLevel == null || rLevel.getSubprefix() == null) return;

        try {
            LuckPerms lp = plugin.getLuckPerms();
            lp.getUserManager().modifyUser(player.getUniqueId(), user -> {
                // Limpiar cualquier sufijo o prefijo racial previo de prioridad >= 50
                user.data().clear(node -> (node instanceof SuffixNode && ((SuffixNode) node).getPriority() >= 50)
                        || (node instanceof PrefixNode && ((PrefixNode) node).getPriority() >= 50));

                // Inyectar nuevo SUFIJO con prioridad 100 y espacio inicial
                // Formato resultante: [Rank] [PlayerName] [RaceSuffix]
                String coloredSuffix = " " + ColorUtils.color(rLevel.getSubprefix());
                SuffixNode node = SuffixNode.builder(coloredSuffix, 100).build();
                user.data().add(node);
            });
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error al inyectar sufijo LuckPerms para " + player.getName(), e);
        }
    }

    /**
     * Procesa la solicitud de cambio de raza aplicando la jerarquía de 4 capas:
     * 0. Pausa global
     * 1. Token gratuito de cambio único
     * 2. Pago Vault
     * 3. Consumo de Ítem Especial (Esencia de Reencarnación Ancestral)
     */
    public boolean processRaceChange(Player player, String newRaceId, ChangeMethod method) {
        // 0. Verificación de Pausa Global (/raza admin cambios parar | iniciar)
        if (!plugin.getConfigManager().isGlobalChangesEnabled()) {
            player.sendMessage(plugin.getConfigManager().getMessage("changes_paused",
                    "&c&l[CYTRICOS] &7Los cambios de raza están pausados actualmente por la administración."));
            return false;
        }

        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        if (data.getRace().equalsIgnoreCase(newRaceId)) {
            player.sendMessage(plugin.getConfigManager().getMessage("already_in_race",
                    "&cYa perteneces a esta raza."));
            return false;
        }

        Race targetRace = getRace(newRaceId);
        if (targetRace == null) {
            player.sendMessage(ChatColor.RED + "La raza seleccionada no existe.");
            return false;
        }

        boolean authorized = false;

        // 1. Método Gratuito (1 Sola vez por jugador)
        if (method == ChangeMethod.FREE && data.hasFreeRaceChangeAvailable()) {
            data.setFreeRaceChangeAvailable(false); // Consumir token único
            authorized = true;
            player.sendMessage(plugin.getConfigManager().getMessage("free_change_used",
                    "&a&l[CYTRICOS] &7¡Has utilizado tu cambio de raza gratuito!"));
        }
        // 2. Método Economía Vault
        else if (method == ChangeMethod.ECONOMY) {
            double cost = plugin.getConfigManager().getRaceChangeCost();
            if (plugin.isVaultAvailable() && plugin.getVaultEconomy().getBalance(player) >= cost) {
                plugin.getVaultEconomy().withdrawPlayer(player, cost);
                authorized = true;
                String msg = plugin.getConfigManager().getMessage("paid_change_success",
                        "&a&l[CYTRICOS] &7Cambio de raza realizado con éxito por &e$%cost%&7.")
                        .replace("%cost%", String.format("%.0f", cost));
                player.sendMessage(msg);
            } else {
                player.sendMessage(ChatColor.RED + "No tienes suficiente dinero ($" + String.format("%.0f", cost) + ") para cambiar de raza.");
                return false;
            }
        }
        // 3. Método Objeto Especial con Lore Custom
        else if (method == ChangeMethod.SPECIAL_ITEM) {
            if (plugin.getCustomItemManager().hasChangeTokenItem(player)) {
                plugin.getCustomItemManager().consumeChangeTokenItem(player, 1);
                authorized = true;
                player.sendMessage(plugin.getConfigManager().getMessage("item_change_success",
                        "&a&l[CYTRICOS] &7Has entregado tu &6Esencia de Reencarnación Ancestral &7y cambiado de raza."));
            } else {
                player.sendMessage(ChatColor.RED + "No tienes la Esencia de Reencarnación Ancestral en tu inventario.");
                return false;
            }
        }

        if (authorized) {
            // Reset seguro de progreso de raza previa a Nivel 1 (sin explotar puntos de talento)
            data.setRace(targetRace.getId());
            data.setLevel(1);
            data.resetTalents();
            data.setTalentPoints(0);
            data.setMaxRacialResource(100.0);
            data.setRacialResource(100.0);
            data.setLastChangeTimestamp(System.currentTimeMillis());
            data.unlockAchievement("reincarnation");
            if (!targetRace.getId().equalsIgnoreCase("human")) {
                data.unlockAchievement("first_race");
            }
            plugin.getPlayerDataManager().saveToFile(data);

            // Actualizar atributos, escala, pasivas y alas
            applyPassives(player, targetRace.getId(), 1);
            if (plugin.getWingsManager() != null) {
                plugin.getWingsManager().refreshPlayerWings(player);
            }

            if (data.isSettingSounds()) {
                player.playSound(player.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.1f);
            }
            if (data.isSettingParticles()) {
                player.getWorld().spawnParticle(org.bukkit.Particle.TOTEM_OF_UNDYING, player.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.1);
            }
            ColorUtils.sendTitle(player, "&6&l¡REENCARNACIÓN COMPLETADA!", "&eAhora eres: " + targetRace.getDisplayName(), 10, 60, 20);

            String msg = plugin.getConfigManager().getMessage("race_changed",
                    "&e&l[CYTRICOS] &7Tu raza ha sido cambiada a: &a%race%")
                    .replace("%race%", targetRace.getDisplayName());
            player.sendMessage(msg);
            return true;
        }

        player.sendMessage(plugin.getConfigManager().getMessage("no_funds_or_item",
                "&c&l[CYTRICOS] &7No cumples los requisitos para cambiar de raza."));
        return false;
    }

    /**
     * Procesa la ascensión al siguiente nivel de la raza actual del jugador.
     */
    public boolean processLevelUp(Player player) {
        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        Race race = getRace(data.getRace());
        if (race == null) return false;

        int currentLvl = data.getLevel();
        if (currentLvl >= 6) {
            player.sendMessage(plugin.getConfigManager().getMessage("level_max",
                    "&e&l[CYTRICOS] &7Ya te encuentras en el nivel máximo legendario."));
            return false;
        }

        int nextLvl = currentLvl + 1;
        RaceLevel targetLevel = race.getLevel(nextLvl);
        if (targetLevel == null) {
            player.sendMessage(ChatColor.RED + "Error: Datos de nivel no configurados.");
            return false;
        }

        // Verificar dinero Vault
        if (targetLevel.getCostMoney() > 0 && plugin.isVaultAvailable()) {
            if (plugin.getVaultEconomy().getBalance(player) < targetLevel.getCostMoney()) {
                String msg = plugin.getConfigManager().getMessage("insufficient_reqs",
                        "&c&l[CYTRICOS] &7No cumples con los requisitos de materiales o dinero ($%cost%) para ascender.")
                        .replace("%cost%", String.format("%.0f", targetLevel.getCostMoney()));
                player.sendMessage(msg);
                return false;
            }
        }

        // Verificar ítems requeridos en inventario
        Material reqMat = targetLevel.getRequiredItem();
        int reqAmount = targetLevel.getRequiredItemAmount();
        if (reqMat != null && reqMat != Material.AIR && reqAmount > 0) {
            if (!hasItem(player, reqMat, reqAmount)) {
                player.sendMessage(ChatColor.RED + "Te faltan materiales: " + reqAmount + "x " + reqMat.name());
                return false;
            }
        }

        // Consumir dinero
        if (targetLevel.getCostMoney() > 0 && plugin.isVaultAvailable()) {
            plugin.getVaultEconomy().withdrawPlayer(player, targetLevel.getCostMoney());
        }

        // Consumir ítems
        if (reqMat != null && reqMat != Material.AIR && reqAmount > 0) {
            consumeItem(player, reqMat, reqAmount);
        }

        // Promover
        data.setLevel(nextLvl);
        data.setMaxRacialResource(100.0 + (nextLvl - 1) * 20.0);
        data.setRacialResource(data.getMaxRacialResource());
        if (nextLvl >= 3) data.unlockAchievement("level_3");
        if (nextLvl >= 6) data.unlockAchievement("level_6");
        plugin.getPlayerDataManager().saveToFile(data);
        applyPassives(player, race.getId(), nextLvl);

        // Feedback visual y sonoro de subida de nivel
        if (data.isSettingSounds()) {
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.25f);
        }
        if (data.isSettingParticles()) {
            player.getWorld().spawnParticle(org.bukkit.Particle.END_ROD, player.getLocation().add(0, 1, 0), 25, 0.5, 0.6, 0.5, 0.08);
        }
        ColorUtils.sendTitle(player, "&a&l¡ASCENSO NIVEL " + nextLvl + "!", "&e" + targetLevel.getName() + " &7(+1 Punto de Talento)", 10, 60, 20);

        String msg = plugin.getConfigManager().getMessage("level_up_success",
                "&a&l[CYTRICOS] &7¡Has ascendido al Nivel %level% de %race%!")
                .replace("%level%", String.valueOf(nextLvl))
                .replace("%race%", race.getDisplayName());
        player.sendMessage(msg);
        if (targetLevel.getPerks() != null && !targetLevel.getPerks().isEmpty()) {
            ColorUtils.sendMessage(player, "&e&l[NUEVOS PODERES] &f" + targetLevel.getPerks());
        }
        return true;
    }

    private boolean hasItem(Player player, Material mat, int amount) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == mat) {
                count += item.getAmount();
                if (count >= amount) return true;
            }
        }
        return false;
    }

    private void consumeItem(Player player, Material mat, int amount) {
        int left = amount;
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item != null && item.getType() == mat) {
                if (item.getAmount() <= left) {
                    left -= item.getAmount();
                    player.getInventory().setItem(i, null);
                } else {
                    item.setAmount(item.getAmount() - left);
                    left = 0;
                }
                if (left <= 0) break;
            }
        }
        player.updateInventory();
    }
}

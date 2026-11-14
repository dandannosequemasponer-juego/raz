package com.cytricos.races.manager;

import com.cytricos.races.CytricosRaces;
import com.cytricos.races.model.ChangeMethod;
import com.cytricos.races.model.PlayerData;
import com.cytricos.races.model.Race;
import com.cytricos.races.model.RaceLevel;
import com.cytricos.races.util.ColorUtils;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.node.types.PrefixNode;
import net.luckperms.api.node.types.SuffixNode;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
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
 * Gestor central de razas, progresión de 6 niveles, atributos físicos, wipe completo en cambio de raza
 * y compatibilidad con estado inicial "Sin Raza" ("none") para CYTRICOS RPG.
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
            race.setDescription(raceSec.getString("description", ""));

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
        if (id == null || id.equalsIgnoreCase("none")) return null;
        return races.get(id.toLowerCase());
    }

    public Race resolveRace(String query) {
        if (query == null || query.trim().isEmpty()) return null;
        String clean = query.trim().toLowerCase()
                .replace("á", "a")
                .replace("é", "e")
                .replace("í", "i")
                .replace("ó", "o")
                .replace("ú", "u")
                .replace("ñ", "n")
                .replace(" ", "")
                .replace("-", "")
                .replace("_", "");

        Race direct = races.get(clean);
        if (direct != null) return direct;

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
            case "dragon", "dragonkin", "draconiano", "lagarto", "draco" -> "dragonkin";
            case "bestia", "beastkin", "semihumano", "demihumano", "bestiasemihumano" -> "beastkin";
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

        slotMap.remove(race.getSlot());

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
        cfg.set(path + "display_name", race.getDisplayName());
        cfg.set(path + "scale", race.getScale());
        cfg.set(path + "base_health_hearts", race.getBaseHealthHearts());
        cfg.set(path + "base_speed_mult", race.getBaseSpeedMultiplier());
        cfg.set(path + "base_damage_mult", race.getBaseDamageMultiplier());
        cfg.set(path + "immunities", new ArrayList<>(race.getImmunities()));
        cfg.set(path + "vulnerabilities", new ArrayList<>(race.getVulnerabilities()));
        plugin.getConfigManager().saveRacesConfig();
    }

    public void resetToUnassignedPassives(Player player) {
        if (player == null || !player.isOnline()) return;
        ScaleManager.setPlayerRaceScale(player, 1.0);
        setAttrBase(player, Attribute.MAX_HEALTH, 20.0);
        if (player.getHealth() > 20.0) player.setHealth(20.0);
        setAttrBase(player, Attribute.MOVEMENT_SPEED, 0.10);
        setAttrBase(player, Attribute.ATTACK_DAMAGE, 1.0);
        setAttrBase(player, Attribute.ARMOR, 0.0);
        setAttrBase(player, Attribute.KNOCKBACK_RESISTANCE, 0.0);
        setAttrBase(player, Attribute.JUMP_STRENGTH, 0.42);
        setReach(player, 3.0, 4.5);

        if (player.getGameMode() == GameMode.SURVIVAL || player.getGameMode() == GameMode.ADVENTURE) {
            player.setAllowFlight(false);
            player.setFlying(false);
        }
        player.removePotionEffect(PotionEffectType.FIRE_RESISTANCE);
        player.removePotionEffect(PotionEffectType.NIGHT_VISION);
        player.removePotionEffect(PotionEffectType.SLOW_FALLING);
    }

    public void applyPassives(Player player, String raceId, int level) {
        if (player == null || !player.isOnline()) return;
        if (raceId == null || raceId.equalsIgnoreCase("none")) {
            resetToUnassignedPassives(player);
            return;
        }
        Race race = getRace(raceId);
        if (race == null) {
            resetToUnassignedPassives(player);
            return;
        }

        String id = race.getId().toLowerCase();

        // 1. Escala física (Attribute.SCALE)
        double effectiveScale = race.getScale();
        if (id.equals("giant")) {
            if (level <= 2) effectiveScale = 1.15;
            else if (level <= 4) effectiveScale = 1.25;
            else effectiveScale = 1.35;
        }
        ScaleManager.setPlayerRaceScale(player, effectiveScale);

        // 2. Salud Máxima
        double targetHealth = race.getBaseHealthHearts() * 2.0;
        if (id.equals("dwarf") && level >= 2) targetHealth += 6.0;
        if (id.equals("giant")) {
            if (level >= 6) targetHealth += 24.0;
            else if (level >= 4) targetHealth += 16.0;
            else if (level >= 2) targetHealth += 8.0;
        }
        if (id.equals("dragonkin") && level >= 6) targetHealth += 16.0;
        if (id.equals("demon") && level >= 2) targetHealth += 4.0;

        PlayerData pData = plugin.getPlayerDataManager().get(player.getUniqueId());
        if (pData != null && pData.hasTalent("surv_health")) {
            targetHealth += 2.0;
        }
        setAttrBase(player, Attribute.MAX_HEALTH, targetHealth);
        if (player.getHealth() > targetHealth) {
            player.setHealth(targetHealth);
        }

        // 3. Velocidad base
        double speedMult = race.getBaseSpeedMultiplier();
        if (id.equals("werewolf")) speedMult *= 1.15;
        if (id.equals("beastkin") && level >= 4) speedMult *= 1.30;
        if (id.equals("fairy") && level >= 5) speedMult *= 1.40;
        if (pData != null && pData.hasTalent("util_speed")) speedMult *= 1.05;
        setAttrBase(player, Attribute.MOVEMENT_SPEED, 0.10 * speedMult);

        // 4. Daño base
        double dmgMult = race.getBaseDamageMultiplier();
        if (id.equals("human") && level >= 5) dmgMult *= 1.15;
        if (pData != null && pData.hasTalent("combat_dmg")) dmgMult *= 1.05;
        setAttrBase(player, Attribute.ATTACK_DAMAGE, 1.0 * dmgMult);

        // 5. Armadura natural (Dragonkin L5+: +4 puntos de armadura)
        double naturalArmor = (id.equals("dragonkin") && level >= 5) ? 4.0 : 0.0;
        setAttrBase(player, Attribute.ARMOR, naturalArmor);

        // 6. Resistencia al empuje (Dwarf L4+: 100% inmune)
        double kbRes = (id.equals("dwarf") && level >= 4) ? 1.0 : 0.0;
        setAttrBase(player, Attribute.KNOCKBACK_RESISTANCE, kbRes);

        // 7. Salto (Dwarf limitado a ~1 bloque = 0.36; resto 0.42)
        double jumpStrength = id.equals("dwarf") ? 0.36 : 0.42;
        setAttrBase(player, Attribute.JUMP_STRENGTH, jumpStrength);

        // 8. Alcance de ataque/bloques (Goblin -30%, Giant +1..2 bloques)
        double entityReach = 3.0;
        double blockReach = 4.5;
        if (id.equals("goblin")) {
            entityReach = 3.0 * 0.70;
            blockReach = 4.5 * 0.70;
        } else if (id.equals("giant")) {
            double extra = level >= 5 ? 1.5 : 1.0;
            entityReach = 3.0 + extra;
            blockReach = 4.5 + extra;
        }
        setReach(player, entityReach, blockReach);

        // 9. Permiso de vuelo (Fairy L1+ y Angel L3+ sin peto pesado)
        boolean angelHeavyChest = false;
        if (id.equals("angel")) {
            ItemStack chest = player.getInventory().getChestplate();
            if (chest != null && (chest.getType() == Material.IRON_CHESTPLATE
                    || chest.getType() == Material.DIAMOND_CHESTPLATE
                    || chest.getType() == Material.NETHERITE_CHESTPLATE)) {
                angelHeavyChest = true;
            }
        }

        boolean allowFlight = id.equals("fairy") || (id.equals("angel") && level >= 3 && !angelHeavyChest)
                || (pData != null && pData.isTransformed() && plugin.getTransformationManager() != null
                && plugin.getTransformationManager().isAerialForm(id, level));

        if (allowFlight && (pData == null || !pData.isFlightBanned())) {
            player.setAllowFlight(true);
        } else if (player.getGameMode() == GameMode.SURVIVAL || player.getGameMode() == GameMode.ADVENTURE) {
            player.setAllowFlight(false);
            player.setFlying(false);
        }

        // 10. Sufijo opcional LuckPerms (desactivado por defecto para no contaminar Tablist)
        if (plugin.getConfigManager().isInjectLuckPermsChatSuffix()) {
            applyLuckPermsSuffix(player, race, level);
        }

        // 11. Efectos pasivos de poción
        applyRacialPotionBuffs(player, race, level);
    }

    private void setAttrBase(Player player, Attribute attr, double value) {
        try {
            AttributeInstance inst = player.getAttribute(attr);
            if (inst != null) {
                inst.setBaseValue(value);
            }
        } catch (Throwable ignored) {}
    }

    private void setReach(Player player, double entityReach, double blockReach) {
        try {
            Attribute entAttr = Registry.ATTRIBUTE.get(NamespacedKey.minecraft("entity_interaction_range"));
            if (entAttr != null) {
                AttributeInstance inst = player.getAttribute(entAttr);
                if (inst != null) inst.setBaseValue(entityReach);
            }
            Attribute blkAttr = Registry.ATTRIBUTE.get(NamespacedKey.minecraft("block_interaction_range"));
            if (blkAttr != null) {
                AttributeInstance inst = player.getAttribute(blkAttr);
                if (inst != null) inst.setBaseValue(blockReach);
            }
        } catch (Throwable ignored) {}
    }

    private void applyRacialPotionBuffs(Player player, Race race, int level) {
        player.removePotionEffect(PotionEffectType.FIRE_RESISTANCE);
        player.removePotionEffect(PotionEffectType.NIGHT_VISION);
        player.removePotionEffect(PotionEffectType.SLOW_FALLING);

        String id = race.getId().toLowerCase();
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

    public void applyLuckPermsSuffix(Player player, Race race, int level) {
        if (!plugin.isLuckPermsAvailable() || player == null || race == null) return;

        RaceLevel rLevel = race.getLevel(level);
        if (rLevel == null || rLevel.getSubprefix() == null) return;

        try {
            LuckPerms lp = plugin.getLuckPerms();
            lp.getUserManager().modifyUser(player.getUniqueId(), user -> {
                user.data().clear(node -> (node instanceof SuffixNode && ((SuffixNode) node).getPriority() >= 50)
                        || (node instanceof PrefixNode && ((PrefixNode) node).getPriority() >= 50));

                String coloredSuffix = " " + ColorUtils.color(rLevel.getSubprefix());
                SuffixNode node = SuffixNode.builder(coloredSuffix, 100).build();
                user.data().add(node);
            });
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error al inyectar sufijo LuckPerms para " + player.getName(), e);
        }
    }

    public boolean processRaceChange(Player player, String newRaceId, ChangeMethod method) {
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
            ColorUtils.sendMessage(player, "&cLa raza seleccionada no existe.");
            return false;
        }

        boolean authorized = false;
        boolean wasUnassigned = !data.hasAssignedRace();

        if (method == ChangeMethod.FREE && (data.hasFreeRaceChangeAvailable() || wasUnassigned)) {
            if (!wasUnassigned) {
                data.setFreeRaceChangeAvailable(false);
            }
            authorized = true;
            if (wasUnassigned) {
                ColorUtils.sendMessage(player, "&a&l[CYTRICOS] &7¡Has elegido tu linaje inicial!");
            } else {
                player.sendMessage(plugin.getConfigManager().getMessage("free_change_used",
                        "&a&l[CYTRICOS] &7¡Has utilizado tu cambio de raza gratuito!"));
            }
        } else if (method == ChangeMethod.ECONOMY) {
            double cost = plugin.getConfigManager().getRaceChangeCost();
            if (plugin.isVaultAvailable() && plugin.getVaultEconomy().getBalance(player) >= cost) {
                plugin.getVaultEconomy().withdrawPlayer(player, cost);
                authorized = true;
                String msg = plugin.getConfigManager().getMessage("paid_change_success",
                        "&a&l[CYTRICOS] &7Cambio de raza realizado con éxito por &e$%cost%&7.")
                        .replace("%cost%", String.format("%.0f", cost));
                player.sendMessage(msg);
            } else {
                ColorUtils.sendMessage(player, "&cNo tienes suficiente dinero ($" + String.format("%.0f", cost) + ") para cambiar de raza.");
                return false;
            }
        } else if (method == ChangeMethod.SPECIAL_ITEM) {
            if (plugin.getCustomItemManager().hasChangeTokenItem(player)) {
                plugin.getCustomItemManager().consumeChangeTokenItem(player, 1);
                authorized = true;
                player.sendMessage(plugin.getConfigManager().getMessage("item_change_success",
                        "&a&l[CYTRICOS] &7Has entregado tu &6Esencia de Reencarnación Ancestral &7y cambiado de raza."));
            } else {
                ColorUtils.sendMessage(player, "&cNo tienes la Esencia de Reencarnación Ancestral en tu inventario.");
                return false;
            }
        }

        if (authorized) {
            // Desactivar transformación, alas y accesorios craneales previos antes del cambio
            if (plugin.getTransformationManager() != null) {
                plugin.getTransformationManager().revertTransformation(player, false);
            }
            if (plugin.getWingsManager() != null) {
                plugin.getWingsManager().unequipOnRaceChange(player);
            }
            if (plugin.getHeadCosmeticManager() != null) {
                plugin.getHeadCosmeticManager().unequipOnRaceChange(player);
            }

            // Limpieza total obligatoria (Fase 4): Nivel 1, 0 XP, 0 Talentos, 0 Misiones
            data.setRace(targetRace.getId());
            data.wipeRaceProgress();
            data.setLastChangeTimestamp(System.currentTimeMillis());
            data.unlockAchievement("reincarnation");
            data.unlockAchievement("first_race");

            player.setLevel(0);
            player.setExp(0.0f);
            player.setTotalExperience(0);

            plugin.getPlayerDataManager().saveToFile(data);

            applyPassives(player, targetRace.getId(), 1);

            if (data.isSettingSounds()) {
                player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.1f);
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

    public boolean processLevelUp(Player player) {
        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        if (data == null || !data.hasAssignedRace()) {
            ColorUtils.sendMessage(player, "&cPrimero debes elegir una raza con &e/razas&c.");
            return false;
        }
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
            ColorUtils.sendMessage(player, "&cError: Datos de nivel no configurados.");
            return false;
        }

        if (targetLevel.getCostMoney() > 0 && plugin.isVaultAvailable()) {
            if (plugin.getVaultEconomy().getBalance(player) < targetLevel.getCostMoney()) {
                String msg = plugin.getConfigManager().getMessage("insufficient_reqs",
                        "&c&l[CYTRICOS] &7No cumples con los requisitos de materiales o dinero ($%cost%) para ascender.")
                        .replace("%cost%", String.format("%.0f", targetLevel.getCostMoney()));
                player.sendMessage(msg);
                return false;
            }
        }

        Material reqMat = targetLevel.getRequiredItem();
        int reqAmount = targetLevel.getRequiredItemAmount();
        if (reqMat != null && reqMat != Material.AIR && reqAmount > 0) {
            if (!hasItem(player, reqMat, reqAmount)) {
                ColorUtils.sendMessage(player, "&cTe faltan materiales: &e" + reqAmount + "x " + reqMat.name());
                return false;
            }
        }

        if (targetLevel.getCostMoney() > 0 && plugin.isVaultAvailable()) {
            plugin.getVaultEconomy().withdrawPlayer(player, targetLevel.getCostMoney());
        }

        if (reqMat != null && reqMat != Material.AIR && reqAmount > 0) {
            consumeItem(player, reqMat, reqAmount);
        }

        data.setLevel(nextLvl);
        data.setMaxRacialResource(100.0 + (nextLvl - 1) * 20.0);
        data.setRacialResource(data.getMaxRacialResource());
        if (nextLvl >= 3) data.unlockAchievement("level_3");
        if (nextLvl >= 6) data.unlockAchievement("level_6");
        plugin.getPlayerDataManager().saveToFile(data);
        applyPassives(player, race.getId(), nextLvl);

        if (data.isSettingSounds()) {
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.25f);
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

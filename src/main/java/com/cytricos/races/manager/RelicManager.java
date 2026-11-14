package com.cytricos.races.manager;

import com.cytricos.races.CytricosRaces;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;

import java.util.*;

/**
 * Gestor de configuración para pasivas de Mano Secundaria (Offhand) y reglas de botín
 * de Reliquias desde relics.yml para jefes personalizados o mobs del reino.
 * Lead Developers: Dandan_13 & Wolfit12
 */
public class RelicManager {
    private final CytricosRaces plugin;
    private final Map<String, RelicBehavior> relicBehaviors = new LinkedHashMap<>();

    public static class DropRule {
        public final String customMobId;
        public final String displayNameContains;
        public final EntityType entityType;
        public final double dropChance;

        public DropRule(String customMobId, String displayNameContains, EntityType entityType, double dropChance) {
            this.customMobId = customMobId == null ? "" : customMobId.trim().toLowerCase();
            this.displayNameContains = displayNameContains == null ? "" : displayNameContains.trim().toLowerCase();
            this.entityType = entityType;
            this.dropChance = Math.max(0.0, Math.min(1.0, dropChance));
        }
    }

    public static class RelicBehavior {
        public final String relicId;
        public final String offhandDescription;
        public final List<DropRule> dropRules;

        public RelicBehavior(String relicId, String offhandDescription, List<DropRule> dropRules) {
            this.relicId = relicId;
            this.offhandDescription = offhandDescription;
            this.dropRules = dropRules;
        }
    }

    public RelicManager(CytricosRaces plugin) {
        this.plugin = plugin;
        loadRelics();
    }

    public void loadRelics() {
        relicBehaviors.clear();
        FileConfiguration cfg = plugin.getConfigManager().getRelicsConfig();
        if (cfg == null) return;
        ConfigurationSection root = cfg.getConfigurationSection("relics");
        if (root == null) return;

        for (String relicId : root.getKeys(false)) {
            ConfigurationSection sec = root.getConfigurationSection(relicId);
            if (sec == null) continue;

            String offhandDesc = sec.getString("offhand_passive", "");
            List<DropRule> rules = new ArrayList<>();

            List<Map<?, ?>> rawRules = sec.getMapList("drop_rules");
            for (Map<?, ?> map : rawRules) {
                Object mobObj = map.get("custom_mob_id");
                String customMobId = mobObj != null ? String.valueOf(mobObj) : "";
                Object nameObj = map.get("display_name_contains");
                String nameContains = nameObj != null ? String.valueOf(nameObj) : "";
                Object typeObj = map.get("entity_type");
                EntityType eType = null;
                if (typeObj != null && !String.valueOf(typeObj).isBlank()) {
                    try {
                        eType = EntityType.valueOf(String.valueOf(typeObj).trim().toUpperCase());
                    } catch (IllegalArgumentException ignored) {
                    }
                }
                double chance = 0.0;
                Object chanceObj = map.get("drop_chance");
                if (chanceObj instanceof Number n) {
                    chance = n.doubleValue();
                } else if (chanceObj != null) {
                    try {
                        chance = Double.parseDouble(String.valueOf(chanceObj));
                    } catch (NumberFormatException ignored) {
                    }
                }
                if (chance > 0.0) {
                    rules.add(new DropRule(customMobId, nameContains, eType, chance));
                }
            }

            relicBehaviors.put(relicId.toLowerCase(), new RelicBehavior(relicId, offhandDesc, rules));
        }

        plugin.getLogger().info("Cargadas " + relicBehaviors.size() + " configuraciones de reliquias desde relics.yml.");
    }

    public RelicBehavior getRelicBehavior(String relicId) {
        if (relicId == null) return null;
        return relicBehaviors.get(relicId.toLowerCase());
    }

    public Collection<RelicBehavior> getAllRelics() {
        return Collections.unmodifiableCollection(relicBehaviors.values());
    }
}

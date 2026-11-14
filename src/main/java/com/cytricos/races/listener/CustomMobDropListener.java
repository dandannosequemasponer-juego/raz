package com.cytricos.races.listener;

import com.cytricos.races.CytricosRaces;
import com.cytricos.races.manager.RelicManager;
import com.cytricos.races.util.ColorUtils;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

/**
 * Listener extensible para botín de Reliquias Personalizadas desde relics.yml.
 * Evalúa coincidencias por PDC custom_mob_id, display_name_contains y entity_type con drop_chance.
 * Lead Developers: Dandan_13 & Wolfit12
 */
public class CustomMobDropListener implements Listener {
    private final CytricosRaces plugin;
    private final NamespacedKey cytricosMobKey;
    private final NamespacedKey genericMobKey;

    public CustomMobDropListener(CytricosRaces plugin) {
        this.plugin = plugin;
        this.cytricosMobKey = new NamespacedKey(plugin, "custom_mob_id");
        this.genericMobKey = NamespacedKey.minecraft("custom_mob_id");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        if (plugin.getRelicManager() == null) return;
        LivingEntity entity = event.getEntity();
        Player killer = entity.getKiller();
        if (killer == null) return;

        String entityCustomName = "";
        if (entity.customName() != null) {
            entityCustomName = ColorUtils.toPlainText(entity.customName()).toLowerCase();
        }
        String pdcMobId = extractCustomMobId(entity);
        EntityType type = entity.getType();

        for (RelicManager.RelicBehavior relic : plugin.getRelicManager().getAllRelics()) {
            for (RelicManager.DropRule rule : relic.dropRules) {
                if (matchesRule(entity, pdcMobId, entityCustomName, type, rule)) {
                    if (Math.random() <= rule.dropChance) {
                        ItemStack stack = plugin.getCustomItemManager().createCustomItem(relic.relicId);
                        if (stack != null) {
                            event.getDrops().add(stack);
                            ColorUtils.sendMessage(killer, "&6&l[BOTÍN DE RELIQUIA] &7¡Ha caído una reliquia: "
                                    + plugin.getCustomItemManager().getSpec(relic.relicId).displayName + "&7!");
                            break;
                        }
                    }
                }
            }
        }
    }

    private String extractCustomMobId(LivingEntity entity) {
        PersistentDataContainer pdc = entity.getPersistentDataContainer();
        String val = pdc.get(cytricosMobKey, PersistentDataType.STRING);
        if (val != null && !val.isBlank()) return val.trim().toLowerCase();
        val = pdc.get(genericMobKey, PersistentDataType.STRING);
        if (val != null && !val.isBlank()) return val.trim().toLowerCase();
        for (String tag : entity.getScoreboardTags()) {
            if (tag.startsWith("mob:") || tag.startsWith("cytricos:")) {
                return tag.substring(tag.indexOf(':') + 1).trim().toLowerCase();
            }
        }
        return "";
    }

    private boolean matchesRule(LivingEntity entity, String pdcMobId, String customNameLower,
                                EntityType type, RelicManager.DropRule rule) {
        boolean customIdMatches = !rule.customMobId.isEmpty()
                && (!pdcMobId.isEmpty() && pdcMobId.equalsIgnoreCase(rule.customMobId)
                || entity.getScoreboardTags().stream().anyMatch(t -> t.equalsIgnoreCase(rule.customMobId)));

        boolean nameMatches = !rule.displayNameContains.isEmpty()
                && !customNameLower.isEmpty()
                && customNameLower.contains(rule.displayNameContains);

        boolean typeMatches = rule.entityType != null && type == rule.entityType;

        if (customIdMatches) {
            return true;
        }
        if (!rule.displayNameContains.isEmpty()) {
            return nameMatches && (rule.entityType == null || typeMatches);
        }
        // Para jefes vanilla nativos (Wither, Ender Dragon, Warden, Elder Guardian) sin customName ni PDC
        return typeMatches && (type == EntityType.WITHER || type == EntityType.ENDER_DRAGON
                || type == EntityType.WARDEN || type == EntityType.ELDER_GUARDIAN || rule.customMobId.isEmpty());
    }
}

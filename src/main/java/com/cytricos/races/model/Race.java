package com.cytricos.races.model;

import org.bukkit.Material;
import java.util.*;

/**
 * Representa una de las 16 razas completas de CYTRICOS RPG.
 * Incluye traductores de inmunidades y vulnerabilidades al español para GUIs y Códice.
 * Lead Developers: Dandan_13 & Wolfit12
 */
public class Race {
    private final String id;
    private String displayName;
    private Material material;
    private int slot;
    private double scale;
    private double baseHealthHearts;
    private double baseSpeedMultiplier;
    private double baseDamageMultiplier;
    private String description = "";
    private final Set<String> immunities = new HashSet<>();
    private final Set<String> vulnerabilities = new HashSet<>();
    private final Map<Integer, RaceLevel> levels = new HashMap<>();

    public Race(String id, String displayName, Material material, int slot, double scale,
                double baseHealthHearts, double baseSpeedMultiplier, double baseDamageMultiplier) {
        this.id = id.toLowerCase();
        this.displayName = displayName;
        this.material = material;
        this.slot = slot;
        this.scale = scale;
        this.baseHealthHearts = baseHealthHearts;
        this.baseSpeedMultiplier = baseSpeedMultiplier;
        this.baseDamageMultiplier = baseDamageMultiplier;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDescription() {
        if (description != null && !description.isBlank()) return description;
        RaceLevel l1 = levels.get(1);
        return l1 != null && l1.getPerks() != null ? l1.getPerks() : "Linaje ancestral de CYTRICOS RPG.";
    }

    public void setDescription(String description) {
        this.description = description != null ? description : "";
    }

    public Material getMaterial() {
        return material;
    }

    public void setMaterial(Material material) {
        this.material = material;
    }

    public int getSlot() {
        return slot;
    }

    public void setSlot(int slot) {
        this.slot = slot;
    }

    public double getScale() {
        return scale;
    }

    public void setScale(double scale) {
        this.scale = Math.max(0.1, Math.min(3.0, Math.round(scale * 100.0) / 100.0));
    }

    public double getBaseHealthHearts() {
        return baseHealthHearts;
    }

    public void setBaseHealthHearts(double baseHealthHearts) {
        this.baseHealthHearts = baseHealthHearts;
    }

    public double getBaseSpeedMultiplier() {
        return baseSpeedMultiplier;
    }

    public void setBaseSpeedMultiplier(double baseSpeedMultiplier) {
        this.baseSpeedMultiplier = baseSpeedMultiplier;
    }

    public double getBaseDamageMultiplier() {
        return baseDamageMultiplier;
    }

    public void setBaseDamageMultiplier(double baseDamageMultiplier) {
        this.baseDamageMultiplier = baseDamageMultiplier;
    }

    public Set<String> getImmunities() {
        return immunities;
    }

    public boolean hasImmunity(String immunity) {
        return immunities.contains(immunity.toLowerCase());
    }

    public void addImmunity(String immunity) {
        immunities.add(immunity.toLowerCase());
    }

    public void removeImmunity(String immunity) {
        immunities.remove(immunity.toLowerCase());
    }

    public Set<String> getVulnerabilities() {
        return vulnerabilities;
    }

    public boolean hasVulnerability(String vuln) {
        return vulnerabilities.contains(vuln.toLowerCase());
    }

    public void addVulnerability(String vuln) {
        vulnerabilities.add(vuln.toLowerCase());
    }

    public void removeVulnerability(String vuln) {
        vulnerabilities.remove(vuln.toLowerCase());
    }

    public Map<Integer, RaceLevel> getLevels() {
        return levels;
    }

    public RaceLevel getLevel(int level) {
        return levels.get(level);
    }

    public void addLevel(RaceLevel level) {
        levels.put(level.getLevel(), level);
    }

    public List<String> getImmunitiesSpanish() {
        List<String> list = new ArrayList<>();
        for (String key : immunities) {
            list.add(translateTrait(key));
        }
        return list;
    }

    public List<String> getVulnerabilitiesSpanish() {
        List<String> list = new ArrayList<>();
        for (String key : vulnerabilities) {
            list.add(translateTrait(key));
        }
        return list;
    }

    public static String translateTrait(String key) {
        if (key == null) return "";
        return switch (key.toLowerCase()) {
            case "knockback" -> "Empuje físico";
            case "fire" -> "Fuego";
            case "lava" -> "Lava";
            case "fall_damage" -> "Daño por caída";
            case "poison" -> "Veneno";
            case "drowning" -> "Ahogamiento";
            case "suffocation" -> "Sofocación";
            case "instant_damage" -> "Daño instantáneo";
            case "slowness" -> "Lentitud";
            case "weakness" -> "Debilidad";
            case "short_jump" -> "Salto corto (-1.5b)";
            case "drowning_speed" -> "Hundimiento rápido en agua";
            case "low_health" -> "Vida máxima reducida";
            case "fire_damage" -> "Vulnerabilidad al fuego";
            case "arrow_damage" -> "Vulnerabilidad a proyectiles";
            case "tunnel_restriction" -> "Restricción en túneles 1x2";
            case "hunger_drain" -> "Consumo acelerado de hambre";
            case "bow_restriction" -> "Imprecisión o bloqueo con arcos";
            case "melee_reach" -> "Alcance cuerpo a cuerpo reducido";
            case "water_weakness" -> "Debilidad en agua y lluvia";
            case "holy_vulnerability" -> "Vulnerabilidad a daño sagrado/Castigo";
            case "darkness_vulnerability" -> "Vulnerabilidad en oscuridad total";
            case "dark_potions" -> "Rechazo a pociones oscuras";
            case "cold_slowness" -> "Lentitud en biomas gélidos";
            case "armor_fragility" -> "Mayor desgaste de armaduras ligeras";
            case "energy_vulnerability" -> "Vulnerabilidad a magia y energía";
            case "silver_damage" -> "Vulnerabilidad a armas de hierro/plata";
            case "cooked_food_nausea" -> "Náuseas con comida cocinada";
            case "sunlight_burn" -> "Combustión bajo luz solar directa";
            case "rain_corrosion" -> "Corrosión mística bajo la lluvia";
            case "healing_potion_damage" -> "Las pociones curativas causan daño";
            case "smite_weakness" -> "Vulnerabilidad severa a Castigo (Smite)";
            case "meat_nausea" -> "Náuseas al consumir carne";
            case "desert_slowness" -> "Lentitud en desiertos y oscuridad";
            case "water_slowness" -> "Lentitud en agua profunda";
            case "beacon_blindness" -> "Ceguera cerca de fanales";
            default -> key;
        };
    }
}

package com.cytricos.races.model;

import org.bukkit.Material;
import java.util.*;

/**
 * Representa una de las 16 razas completas de CYTRICOS RPG.
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
}

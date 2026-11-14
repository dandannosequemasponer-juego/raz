package com.cytricos.races.model;

import org.bukkit.Material;

/**
 * Representa uno de los 6 niveles de progresión de una raza en CYTRICOS RPG.
 * Contiene subprefijo LuckPerms, perks, desventajas, costo Vault y material requerido.
 * Lead Developers: Dandan_13 & Wolfit12
 */
public class RaceLevel {
    private final int level;
    private String name;
    private String subprefix;
    private String perks;
    private String disadvantages;
    private Material requiredItem;
    private int requiredItemAmount;
    private double costMoney;

    public RaceLevel(int level, String name, String subprefix, String perks,
                     String disadvantages, Material requiredItem,
                     int requiredItemAmount, double costMoney) {
        this.level = level;
        this.name = name;
        this.subprefix = subprefix;
        this.perks = perks;
        this.disadvantages = disadvantages;
        this.requiredItem = requiredItem;
        this.requiredItemAmount = requiredItemAmount;
        this.costMoney = costMoney;
    }

    public int getLevel() {
        return level;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSubprefix() {
        return subprefix;
    }

    public void setSubprefix(String subprefix) {
        this.subprefix = subprefix;
    }

    public String getPerks() {
        return perks;
    }

    public void setPerks(String perks) {
        this.perks = perks;
    }

    public String getDisadvantages() {
        return disadvantages;
    }

    public void setDisadvantages(String disadvantages) {
        this.disadvantages = disadvantages;
    }

    public Material getRequiredItem() {
        return requiredItem;
    }

    public void setRequiredItem(Material requiredItem) {
        this.requiredItem = requiredItem;
    }

    public int getRequiredItemAmount() {
        return requiredItemAmount;
    }

    public void setRequiredItemAmount(int requiredItemAmount) {
        this.requiredItemAmount = requiredItemAmount;
    }

    public double getCostMoney() {
        return costMoney;
    }

    public void setCostMoney(double costMoney) {
        this.costMoney = costMoney;
    }
}

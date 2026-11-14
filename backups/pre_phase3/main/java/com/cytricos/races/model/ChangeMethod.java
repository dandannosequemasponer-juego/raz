package com.cytricos.races.model;

/**
 * Métodos autorizados para efectuar el cambio de raza en CYTRICOS RPG.
 * Lead Developers: Dandan_13 & Wolfit12
 */
public enum ChangeMethod {
    FREE,
    ECONOMY,
    SPECIAL_ITEM;

    public static ChangeMethod fromString(String text) {
        if (text == null) return null;
        return switch (text.toLowerCase()) {
            case "free", "gratis" -> FREE;
            case "money", "dinero", "economy", "vault" -> ECONOMY;
            case "item", "objeto", "token" -> SPECIAL_ITEM;
            default -> null;
        };
    }
}

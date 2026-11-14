package com.cytricos.races.gui;

import com.cytricos.races.CytricosRaces;
import com.cytricos.races.model.PlayerData;
import com.cytricos.races.model.Race;
import com.cytricos.races.model.RaceLevel;
import com.cytricos.races.util.ColorUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Menú de Perfil RPG del Jugador para CYTRICOS RPG.
 * Despliega estadísticas vitales, barra de recurso racial, bonos,
 * desventajas y compatibilidad con estado "Sin Raza".
 *
 * Lead Developers: Dandan_13 & Wolfit12
 */
public class ProfileGUI {
    public static final String TITLE = ColorUtils.color("&8[&eCYTRICOS&8] &0Perfil RPG del Jugador");
    public static final Component TITLE_COMPONENT = ColorUtils.component("&8[&eCYTRICOS&8] &0Perfil RPG del Jugador");

    private final CytricosRaces plugin;

    public ProfileGUI(CytricosRaces plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 45, TITLE_COMPONENT);

        ItemStack bg = PlayerGUI.createGuiItem(Material.BLACK_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 45; i++) {
            inv.setItem(i, bg);
        }

        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        boolean hasRace = data != null && data.hasAssignedRace();
        Race race = hasRace ? plugin.getRaceManager().getRace(data.getRace()) : null;
        String raceName = race != null ? race.getDisplayName() : "&fSin Raza";

        // Slot 13: Cabeza de Perfil Central
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(player);
            meta.displayName(ColorUtils.component("&6&l" + player.getName()));
            List<String> lore = new ArrayList<>();
            lore.add("&7Raza: " + raceName);
            lore.add("&7Nivel Racial: &6" + (hasRace ? ("Nivel " + data.getLevel() + " / 6") : "Sin Nivel"));
            if (!data.getSelectedTitle().isEmpty()) {
                lore.add("&7Título Honorífico: &e" + data.getSelectedTitle());
            }
            lore.add("&7Cambio Gratuito Único: " + (data.hasFreeRaceChangeAvailable() ? "&aDisponible" : "&cAgotado"));
            lore.add("");
            lore.add("&8CYTRICOS RPG - FICHA DE AVENTURERO");
            meta.lore(ColorUtils.componentList(lore));
            skull.setItemMeta(meta);
        }
        inv.setItem(13, skull);

        // Slot 20: Atributos Físicos de Combate
        double maxHealth = player.getMaxHealth();
        double scale = 1.0;
        try {
            AttributeInstance attr = player.getAttribute(Attribute.SCALE);
            if (attr != null) scale = attr.getValue();
        } catch (Throwable ignored) {
            if (race != null) scale = race.getScale();
        }
        List<String> attrLore = new ArrayList<>();
        attrLore.add("&7Vida Máxima: &c" + (int) maxHealth + " HP &7(" + (int)(maxHealth / 2) + " corazones)");
        attrLore.add("&7Multiplicador Daño: &e" + (int)((race != null ? race.getBaseDamageMultiplier() : 1.0) * 100) + "%");
        attrLore.add("&7Velocidad Terrestre: &b" + (int)((race != null ? race.getBaseSpeedMultiplier() : 1.0) * 100) + "%");
        attrLore.add("&7Escala Física: &a" + String.format(java.util.Locale.US, "%.2f", scale) + "x");
        if (data.hasTalent("combat_dmg")) attrLore.add("&2✔ Bono Talento: +5% Daño Extra");
        if (data.hasTalent("surv_health")) attrLore.add("&2✔ Bono Talento: +2 HP Máximo");
        if (data.hasTalent("util_speed")) attrLore.add("&2✔ Bono Talento: +5% Velocidad");
        inv.setItem(20, PlayerGUI.createGuiItem(Material.DIAMOND_SWORD, "&c&lAtributos de Combate", attrLore));

        // Slot 21: Medidor de Recurso Racial
        double curRes = data.getRacialResource();
        double maxRes = data.getMaxRacialResource();
        double percent = maxRes > 0 ? (curRes / maxRes) : 0;
        int filled = (int) Math.round(percent * 10);
        StringBuilder bar = new StringBuilder("&a[");
        for (int i = 0; i < 10; i++) {
            bar.append(i < filled ? "■" : "&7■");
        }
        bar.append("&a]");

        List<String> resLore = new ArrayList<>();
        resLore.add("&7Recurso de Raza: &b" + data.getResourceName());
        resLore.add("&7Cantidad: &e" + (int) curRes + " &7/ &e" + (int) maxRes);
        resLore.add("&7Barra: " + bar + " &f" + (int)(percent * 100) + "%");
        resLore.add("");
        resLore.add("&7Regeneración Pasiva: &a+3 unidades cada 4s");
        resLore.add("&7Se consume al utilizar habilidades activas.");
        inv.setItem(21, PlayerGUI.createGuiItem(Material.REDSTONE, "&b&l" + data.getResourceName(), resLore));

        // Slot 22: Ventajas Desbloqueadas
        List<String> perksLore = new ArrayList<>();
        if (race != null) {
            for (int lvl = 1; lvl <= data.getLevel(); lvl++) {
                RaceLevel rLvl = race.getLevel(lvl);
                if (rLvl != null) {
                    perksLore.add("&6Nvl " + lvl + " (" + rLvl.getName() + "): &f" + rLvl.getPerks());
                }
            }
        }
        if (perksLore.isEmpty()) {
            perksLore.add("&7Sin ventajas activas (Sin Raza).");
        }
        inv.setItem(22, PlayerGUI.createGuiItem(Material.NETHER_STAR, "&e&lVentajas de Raza Activas", perksLore));

        // Slot 23: Desventajas y Vulnerabilidades
        List<String> vulnLore = new ArrayList<>();
        if (race != null && !race.getVulnerabilities().isEmpty()) {
            for (String v : race.getVulnerabilities()) {
                vulnLore.add("&c- " + v);
            }
        } else if (!hasRace) {
            vulnLore.add("&7Elige una raza en &e/razas &7para despertar tu linaje.");
        } else {
            vulnLore.add("&aTu raza no posee debilidades elementales severas.");
        }
        inv.setItem(23, PlayerGUI.createGuiItem(Material.LAVA_BUCKET, "&4&lDesventajas Nativas", vulnLore));

        // Slot 24: Estadísticas de Aventura
        List<String> statsLore = new ArrayList<>();
        statsLore.add("&7Habilidades Ejecutadas: &e" + data.getAbilitiesUsed());
        statsLore.add("&7Misiones Completadas: &a" + data.getQuestsCompletedCount());
        statsLore.add("&7Enemigos Derrotados: &c" + data.getMobsDefeated());
        statsLore.add("&7Bajas hacia Talento: &d" + data.getMobKillsCounter() + " / " + plugin.getConfigManager().getTalentKillsPerPoint());
        statsLore.add("&7Logros Desbloqueados: &6" + data.getUnlockedAchievements().size());
        statsLore.add("&7Puntos de Talento: &b" + data.getTalentPoints());
        inv.setItem(24, PlayerGUI.createGuiItem(Material.COMPASS, "&6&lEstadísticas de Aventura", statsLore));

        inv.setItem(36, PlayerGUI.createGuiItem(Material.ARROW, "&eVolver al Menú Principal", List.of("&7Regresa al hub principal.")));
        inv.setItem(44, PlayerGUI.createGuiItem(Material.BARRIER, "&c&lCerrar", null));

        player.openInventory(inv);
    }
}

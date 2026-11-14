package com.cytricos.races.gui;

import com.cytricos.races.CytricosRaces;
import com.cytricos.races.model.PlayerData;
import com.cytricos.races.util.ColorUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Árbol de Talentos y Mejoras Raciales para CYTRICOS RPG.
 * Sistema no intrusivo y balanceado de 3 ramas (Combate, Supervivencia, Utilidad)
 * con inversión segura de Puntos de Talento, sin duplicación ni exploits.
 *
 * Lead Developers: Dandan_13 & Wolfit12
 */
public class SkillTreeGUI {
    public static final String TITLE = ColorUtils.color("&8[&eCYTRICOS&8] &0Árbol de Talentos");
    public static final Component TITLE_COMPONENT = ColorUtils.component("&8[&eCYTRICOS&8] &0Árbol de Talentos");

    private final CytricosRaces plugin;

    public SkillTreeGUI(CytricosRaces plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 45, TITLE_COMPONENT);

        ItemStack bg = PlayerGUI.createGuiItem(Material.BLACK_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 45; i++) {
            inv.setItem(i, bg);
        }

        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());

        // Slot 4: Cabecera con Puntos Disponibles y Reset
        List<String> headLore = new ArrayList<>();
        headLore.add("&7Invierte tus puntos en ramas especializadas.");
        headLore.add("&7Puntos Disponibles: &e" + data.getTalentPoints() + " Pts");
        headLore.add("&7Talentos Desbloqueados: &a" + data.getUnlockedTalents().size() + " / 6");
        headLore.add("");
        headLore.add("&c▶ Clic derecho para reiniciar talentos (Reembolso completo).");
        inv.setItem(4, PlayerGUI.createGuiItem(Material.BEACON, "&e&lPuntos de Talento Racial", headLore));

        // Cabeceras de Ramas
        inv.setItem(10, PlayerGUI.createGuiItem(Material.IRON_SWORD, "&c&lRAMA DE COMBATE", List.of("&7Potenciación de daño e impacto ofensivo.")));
        inv.setItem(12, PlayerGUI.createGuiItem(Material.GOLDEN_APPLE, "&a&lRAMA DE SUPERVIVENCIA", List.of("&7Resistencia física, salud y regeneración.")));
        inv.setItem(14, PlayerGUI.createGuiItem(Material.FEATHER, "&b&lRAMA DE UTILIDAD", List.of("&7Velocidad, enfriamientos y energía.")));

        // RAMA 1: COMBATE
        // Slot 19: Furia Ofensiva (combat_dmg)
        inv.setItem(19, createTalentItem(data, "combat_dmg", Material.BLAZE_POWDER, "&c1. Furia Ofensiva",
                List.of("&7Aumenta tu daño base un &a+5% extra&7.", "&7Afecta ataques físicos y habilidades."), null));

        // Slot 28: Golpe Certero (combat_crit)
        inv.setItem(28, createTalentItem(data, "combat_crit", Material.NETHERITE_SWORD, "&c2. Golpe Certero",
                List.of("&7Incrementa el daño crítico un &a+10%&7.", "&7Requiere Furia Ofensiva previa."), "combat_dmg"));

        // RAMA 2: SUPERVIVENCIA
        // Slot 21: Piel de Hierro (surv_health)
        inv.setItem(21, createTalentItem(data, "surv_health", Material.SHIELD, "&a1. Piel de Hierro",
                List.of("&7Aumenta tu vida máxima en &a+2 HP (1 corazón)&7.", "&7Se acumula con la vida de tu raza."), null));

        // Slot 30: Regeneración Acelerada (surv_regen)
        inv.setItem(30, createTalentItem(data, "surv_regen", Material.REDSTONE_BLOCK, "&a2. Vigor Acelerado",
                List.of("&7Aumenta la regeneración de tu recurso racial", "&7en un &a+15% por intervalo&7.", "&7Requiere Piel de Hierro previa."), "surv_health"));

        // RAMA 3: UTILIDAD
        // Slot 23: Agilidad Primordial (util_speed)
        inv.setItem(23, createTalentItem(data, "util_speed", Material.SUGAR, "&b1. Agilidad Primordial",
                List.of("&7Otorga un &a+5% de velocidad terrestre extra&7.", "&7Mejora la evasión y respuesta táctica."), null));

        // Slot 32: Maestría de Enfriamiento (util_cd)
        inv.setItem(32, createTalentItem(data, "util_cd", Material.CLOCK, "&b2. Enfoque Místico",
                List.of("&7Reduce el tiempo de recarga de tu habilidad", "&7activa principal en un &a-15%&7.", "&7Requiere Agilidad Primordial previa."), "util_speed"));

        // Slot 36: Volver
        inv.setItem(36, PlayerGUI.createGuiItem(Material.ARROW, "&eVolver al Menú Principal", List.of("&7Regresa al hub principal.")));

        // Slot 44: Cerrar
        inv.setItem(44, PlayerGUI.createGuiItem(Material.BARRIER, "&c&lCerrar", null));

        player.openInventory(inv);
    }

    private ItemStack createTalentItem(PlayerData data, String talentId, Material baseMat, String name, List<String> desc, String requiredPrerequisite) {
        boolean unlocked = data.hasTalent(talentId);
        boolean prereqMet = requiredPrerequisite == null || data.hasTalent(requiredPrerequisite);
        boolean canBuy = !unlocked && prereqMet && data.getTalentPoints() > 0;

        Material iconMat;
        String prefix;
        if (unlocked) {
            iconMat = Material.LIME_DYE;
            prefix = "&a✔ ";
        } else if (canBuy) {
            iconMat = Material.YELLOW_DYE;
            prefix = "&e▶ ";
        } else {
            iconMat = Material.GRAY_DYE;
            prefix = "&7✖ ";
        }

        List<String> lore = new ArrayList<>(desc);
        lore.add("");
        if (unlocked) {
            lore.add("&a✔ TALENTO DESBLOQUEADO Y ACTIVO");
        } else if (canBuy) {
            lore.add("&e▶ Clic para desbloquear (Coste: 1 Punto)");
        } else if (!prereqMet) {
            lore.add("&c✖ Requiere desbloquear el talento anterior");
        } else {
            lore.add("&c✖ Puntos insuficientes (Requiere 1 Punto)");
        }

        return PlayerGUI.createGuiItem(iconMat, prefix + name, lore);
    }

    public void handleClick(Player player, int slot, boolean isRightClick) {
        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        if (data == null) return;

        if (slot == 4 && isRightClick) {
            // Reiniciar talentos
            int refunded = data.getUnlockedTalents().size();
            if (refunded == 0) {
                ColorUtils.sendMessage(player, "&cNo tienes talentos asignados para reiniciar.");
                return;
            }
            data.resetTalents();
            plugin.getPlayerDataManager().saveToFile(data);
            plugin.getRaceManager().applyPassives(player, data.getRace(), data.getLevel());
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 0.8f, 1.2f);
            ColorUtils.sendMessage(player, "&a&l[CYTRICOS RPG] &7Talentos reiniciados con éxito. Se reembolsaron &e" + refunded + " Puntos de Talento&7.");
            open(player);
            return;
        }

        String targetTalent = null;
        String prereq = null;
        switch (slot) {
            case 19 -> targetTalent = "combat_dmg";
            case 28 -> { targetTalent = "combat_crit"; prereq = "combat_dmg"; }
            case 21 -> targetTalent = "surv_health";
            case 30 -> { targetTalent = "surv_regen"; prereq = "surv_health"; }
            case 23 -> targetTalent = "util_speed";
            case 32 -> { targetTalent = "util_cd"; prereq = "util_speed"; }
        }

        if (targetTalent != null) {
            if (data.hasTalent(targetTalent)) {
                ColorUtils.sendMessage(player, "&aEste talento ya se encuentra desbloqueado.");
                return;
            }
            if (prereq != null && !data.hasTalent(prereq)) {
                ColorUtils.sendMessage(player, "&cDebes desbloquear el talento previo antes de adquirir este.");
                return;
            }
            if (data.getTalentPoints() <= 0) {
                ColorUtils.sendMessage(player, "&cNo tienes Puntos de Talento disponibles. Sube de nivel o completa misiones.");
                return;
            }

            // Desbloquear
            if (data.unlockTalent(targetTalent)) {
                plugin.getPlayerDataManager().saveToFile(data);
                plugin.getRaceManager().applyPassives(player, data.getRace(), data.getLevel());
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.3f);
                ColorUtils.sendMessage(player, "&a&l[TALENTO DESBLOQUEADO] &7Has adquirido una nueva mejora racial.");
                open(player);
            }
        }
    }
}

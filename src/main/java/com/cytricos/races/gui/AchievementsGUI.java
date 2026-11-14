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
 * Menú de Logros y Títulos Cosméticos para CYTRICOS RPG.
 * Permite visualizar hitos legendarios y equipar títulos de honor
 * para lucir en el chat, perfil y la variable %cytricosraces_title%.
 *
 * Lead Developers: Dandan_13 & Wolfit12
 */
public class AchievementsGUI {
    public static final String TITLE = ColorUtils.color("&8[&eCYTRICOS&8] &0Logros y Títulos");
    public static final Component TITLE_COMPONENT = ColorUtils.component("&8[&eCYTRICOS&8] &0Logros y Títulos");

    private final CytricosRaces plugin;

    public static class AchievementEntry {
        public final String id;
        public final String name;
        public final String desc;
        public final String titleReward;
        public final Material icon;

        public AchievementEntry(String id, String name, String desc, String titleReward, Material icon) {
            this.id = id;
            this.name = name;
            this.desc = desc;
            this.titleReward = titleReward;
            this.icon = icon;
        }
    }

    private final List<AchievementEntry> achievements = List.of(
            new AchievementEntry("first_race", "El Despertar", "Elige tu primera raza en CYTRICOS RPG.", "&b[Novicio]", Material.IRON_HELMET),
            new AchievementEntry("level_3", "Heredero del Linaje", "Alcanza el Nivel 3 en cualquier raza.", "&a[Adepto]", Material.GOLDEN_HELMET),
            new AchievementEntry("level_6", "Cúspide de Poder", "Alcanza el Nivel 6 legendario.", "&6&l[Leyenda Viviente]", Material.NETHERITE_HELMET),
            new AchievementEntry("ability_master", "Maestro de Habilidades", "Ejecuta 25 habilidades raciales activas.", "&d[Conjurador]", Material.ENCHANTED_BOOK),
            new AchievementEntry("hunter", "Cazador Implacable", "Derrota a 50 enemigos usando poderes raciales.", "&c[Cazador]", Material.DIAMOND_SWORD),
            new AchievementEntry("quest_master", "Pionero de Cytricos", "Completa 3 misiones raciales.", "&e[Pionero]", Material.FILLED_MAP),
            new AchievementEntry("reincarnation", "Ciclo Eterno", "Reencarna mediante cambio de raza.", "&5[Reencarnado]", Material.ENDER_EYE),
            new AchievementEntry("flight_master", "Señor de los Cielos", "Domina el vuelo sin recibir sanciones.", "&f[Alado]", Material.FEATHER),
            new AchievementEntry("night_walker", "Sombra Nocturna", "Sobrevive a la noche en forma salvaje.", "&8[Caminante Sombrío]", Material.WITHER_SKELETON_SKULL),
            new AchievementEntry("titan_might", "Poder Descomunal", "Alcanza escala y vitalidad titánica.", "&4[Titánico]", Material.ANVIL)
    );

    public AchievementsGUI(CytricosRaces plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 45, TITLE_COMPONENT);

        ItemStack bg = PlayerGUI.createGuiItem(Material.BLACK_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 45; i++) {
            inv.setItem(i, bg);
        }

        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());

        // Comprobación automática de logros pasivos
        checkAutoAchievements(player, data);

        // Slot 4: Cabecera con título activo
        List<String> headLore = new ArrayList<>();
        headLore.add("&7Título Cosmético Equipado: " + (data.getSelectedTitle().isEmpty() ? "&fNinguno" : "&e" + data.getSelectedTitle()));
        headLore.add("&7Logros Desbloqueados: &a" + data.getUnlockedAchievements().size() + " / " + achievements.size());
        headLore.add("");
        headLore.add("&7Haz clic en un logro desbloqueado para equipar");
        headLore.add("&7su título cosmético para el chat y perfil.");
        inv.setItem(4, PlayerGUI.createGuiItem(Material.GOLDEN_CHESTPLATE, "&6&lTítulo de Honor Activo", headLore));

        // Slots para logros (10 casillas: 19 a 23 y 28 a 32)
        int[] slots = {19, 20, 21, 22, 23, 28, 29, 30, 31, 32};
        for (int i = 0; i < achievements.size() && i < slots.length; i++) {
            AchievementEntry a = achievements.get(i);
            int slot = slots[i];
            boolean unlocked = data.hasAchievement(a.id);
            boolean isEquipped = a.titleReward != null && a.titleReward.equalsIgnoreCase(data.getSelectedTitle());

            Material itemMat = unlocked ? a.icon : Material.GRAY_DYE;
            String prefix = unlocked ? (isEquipped ? "&6★ &lEQUIPADO: " : "&a✔ &lDESBLOQUEADO: ") : "&7✖ &lBLOQUEADO: ";

            List<String> lore = new ArrayList<>();
            lore.add("&7" + a.desc);
            lore.add("");
            lore.add("&6Recompensa de Título: &f" + a.titleReward);
            lore.add("");
            if (isEquipped) {
                lore.add("&a✔ Este título está activo actualmente.");
                lore.add("&e▶ Clic para desequipar título.");
            } else if (unlocked) {
                lore.add("&e▶ Clic para equipar este título cosmético.");
            } else {
                lore.add("&c✖ Completa el requisito para desbloquear.");
            }

            inv.setItem(slot, PlayerGUI.createGuiItem(itemMat, prefix + a.name, lore));
        }

        // Slot 36: Volver
        inv.setItem(36, PlayerGUI.createGuiItem(Material.ARROW, "&eVolver al Menú Principal", List.of("&7Regresa al hub principal.")));

        // Slot 44: Cerrar
        inv.setItem(44, PlayerGUI.createGuiItem(Material.BARRIER, "&c&lCerrar", null));

        player.openInventory(inv);
    }

    private void checkAutoAchievements(Player player, PlayerData data) {
        boolean changed = false;
        if (data.hasAssignedRace() && !data.hasAchievement("first_race")) {
            data.unlockAchievement("first_race");
            changed = true;
        }
        if (data.getLevel() >= 3 && !data.hasAchievement("level_3")) {
            data.unlockAchievement("level_3");
            changed = true;
        }
        if (data.getLevel() >= 6 && !data.hasAchievement("level_6")) {
            data.unlockAchievement("level_6");
            changed = true;
        }
        if (data.getAbilitiesUsed() >= 25 && !data.hasAchievement("ability_master")) {
            data.unlockAchievement("ability_master");
            changed = true;
        }
        if (data.getMobsDefeated() >= 50 && !data.hasAchievement("hunter")) {
            data.unlockAchievement("hunter");
            changed = true;
        }
        if (data.getQuestsCompletedCount() >= 3 && !data.hasAchievement("quest_master")) {
            data.unlockAchievement("quest_master");
            changed = true;
        }
        if (changed) {
            plugin.getPlayerDataManager().saveToFile(data);
        }
    }

    public void handleClick(Player player, int slot) {
        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        if (data == null) return;

        int[] slots = {19, 20, 21, 22, 23, 28, 29, 30, 31, 32};
        for (int i = 0; i < achievements.size() && i < slots.length; i++) {
            if (slots[i] == slot) {
                AchievementEntry a = achievements.get(i);
                if (!data.hasAchievement(a.id)) {
                    ColorUtils.sendMessage(player, "&cNo has desbloqueado este logro aún.");
                    return;
                }

                if (a.titleReward != null) {
                    if (a.titleReward.equalsIgnoreCase(data.getSelectedTitle())) {
                        data.setSelectedTitle("");
                        ColorUtils.sendMessage(player, "&7Has desequipado tu título cosmético.");
                    } else {
                        data.setSelectedTitle(a.titleReward);
                        ColorUtils.sendMessage(player, "&aHas equipado el título: " + a.titleReward);
                    }
                    plugin.getPlayerDataManager().saveToFile(data);
                    player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_GOLD, 1.0f, 1.2f);
                    open(player);
                }
                return;
            }
        }
    }
}

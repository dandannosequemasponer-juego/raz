package com.cytricos.races.gui;

import com.cytricos.races.CytricosRaces;
import com.cytricos.races.manager.QuestManager;
import com.cytricos.races.model.PlayerData;
import com.cytricos.races.util.ColorUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Menú interactivo de Misiones Raciales para CYTRICOS RPG.
 * Despliega misiones temáticas por raza, barras de progreso visuales
 * y botones de reclamo de recompensas sin duplicación.
 *
 * Lead Developers: Dandan_13 & Wolfit12
 */
public class QuestsGUI {
    public static final String TITLE = ColorUtils.color("&8[&eCYTRICOS&8] &0Misiones Raciales");
    public static final Component TITLE_COMPONENT = ColorUtils.component("&8[&eCYTRICOS&8] &0Misiones Raciales");

    private final CytricosRaces plugin;

    public QuestsGUI(CytricosRaces plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 45, TITLE_COMPONENT);

        ItemStack bg = PlayerGUI.createGuiItem(Material.BLACK_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 45; i++) {
            inv.setItem(i, bg);
        }

        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        List<QuestManager.RacialQuest> quests = plugin.getQuestManager().getQuestsForRace(data.getRace());

        // Slot 4: Cabecera informativa
        inv.setItem(4, PlayerGUI.createGuiItem(Material.MAP, "&e&lMisiones de Linaje Racial", List.of(
                "&7Raza: &f" + data.getRace().toUpperCase(),
                "&7Misiones Completadas: &a" + data.getQuestsCompletedCount(),
                "",
                "&7Cumple los objetivos en el mundo para reclamar",
                "&7dinero Vault, Puntos de Talento y Títulos cosméticos."
        )));

        // Slots de misiones: 20, 21, 22, 23, 24
        int[] slots = {20, 21, 22, 23, 24};
        for (int i = 0; i < quests.size() && i < slots.length; i++) {
            QuestManager.RacialQuest q = quests.get(i);
            int slot = slots[i];
            boolean completed = data.isQuestCompleted(q.getId());
            int progress = Math.min(q.getTargetAmount(), data.getQuestProgress(q.getId()));
            boolean readyToClaim = !completed && progress >= q.getTargetAmount();

            Material iconMat;
            String statusPrefix;
            if (completed) {
                iconMat = Material.BOOK;
                statusPrefix = "&a✔ &lCOMPLETADA: ";
            } else if (readyToClaim) {
                iconMat = Material.ENCHANTED_GOLDEN_APPLE;
                statusPrefix = "&e★ &l¡RECLAMAR!: ";
            } else {
                iconMat = Material.WRITABLE_BOOK;
                statusPrefix = "&7⌛ &lEN CURSO: ";
            }

            // Barra visual de progreso
            int filled = q.getTargetAmount() > 0 ? (int) Math.round(((double) progress / q.getTargetAmount()) * 10) : 0;
            StringBuilder bar = new StringBuilder("&a[");
            for (int b = 0; b < 10; b++) {
                if (b < filled) bar.append("■");
                else bar.append("&7■");
            }
            bar.append("&a]");

            List<String> lore = new ArrayList<>();
            lore.add("&7" + q.getDescription());
            lore.add("");
            lore.add("&7Progreso: " + bar + " &f" + progress + " &7/ &f" + q.getTargetAmount());
            lore.add("");
            lore.add("&6&lRecompensas:");
            if (q.getRewardMoney() > 0) {
                lore.add("&7- Dinero Vault: &e$" + String.format("%.0f", q.getRewardMoney()));
            }
            if (q.getRewardTalentPoints() > 0) {
                lore.add("&7- Puntos de Talento: &b+" + q.getRewardTalentPoints() + " Pts");
            }
            if (q.getRewardTitle() != null && !q.getRewardTitle().isEmpty()) {
                lore.add("&7- Título Cosmético: " + q.getRewardTitle());
            }
            lore.add("");
            if (completed) {
                lore.add("&2✔ Recompensa ya reclamada.");
            } else if (readyToClaim) {
                lore.add("&e▶ ¡Clic aquí para reclamar tu recompensa!");
            } else {
                lore.add("&7▶ Continúa jugando para avanzar en este objetivo.");
            }

            inv.setItem(slot, PlayerGUI.createGuiItem(iconMat, statusPrefix + "&f" + q.getName(), lore));
        }

        // Slot 36: Volver
        inv.setItem(36, PlayerGUI.createGuiItem(Material.ARROW, "&eVolver al Menú Principal", List.of("&7Regresa al hub principal.")));

        // Slot 44: Cerrar
        inv.setItem(44, PlayerGUI.createGuiItem(Material.BARRIER, "&c&lCerrar", null));

        player.openInventory(inv);
    }

    public void handleClick(Player player, int slot) {
        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        if (data == null) return;

        List<QuestManager.RacialQuest> quests = plugin.getQuestManager().getQuestsForRace(data.getRace());
        int[] slots = {20, 21, 22, 23, 24};
        for (int i = 0; i < quests.size() && i < slots.length; i++) {
            if (slots[i] == slot) {
                QuestManager.RacialQuest q = quests.get(i);
                if (plugin.getQuestManager().claimReward(player, q.getId())) {
                    open(player);
                }
                return;
            }
        }
    }
}

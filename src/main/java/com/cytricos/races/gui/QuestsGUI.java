package com.cytricos.races.gui;

import com.cytricos.races.CytricosRaces;
import com.cytricos.races.manager.QuestManager;
import com.cytricos.races.model.PlayerData;
import com.cytricos.races.model.Race;
import com.cytricos.races.util.ColorUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Menú interactivo de 54 casillas para Misiones Raciales (Fase 10) en CYTRICOS RPG.
 * - Columna 4 completa (4, 13, 22, 31, 40, 49) con divisor vertical de cristal teñido.
 * - Panel izquierdo (cols 0-3): listado seleccionable de misiones de la raza activa.
 * - Panel derecho (cols 5-8): cabecera, descripción, barra de progreso en vivo, recompensas y botón de reclamo.
 *
 * Lead Developers: Dandan_13 & Wolfit12
 */
public class QuestsGUI {
    public static final String TITLE = ColorUtils.color("&8[&eCYTRICOS&8] &0Misiones Raciales");
    public static final Component TITLE_COMPONENT = ColorUtils.component("&8[&eCYTRICOS&8] &0Misiones Raciales");

    private static final int[] LEFT_QUEST_SLOTS = {10, 11, 12, 19, 20, 21, 28, 29, 30};
    private static final int[] DIVIDER_SLOTS = {4, 13, 22, 31, 40, 49};

    private final CytricosRaces plugin;
    private final Map<UUID, String> selectedQuestMap = new ConcurrentHashMap<>();

    public QuestsGUI(CytricosRaces plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE_COMPONENT);

        ItemStack bg = PlayerGUI.createGuiItem(Material.BLACK_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, bg);
        }

        // Divisor vertical estricto en la columna 4 (4, 13, 22, 31, 40, 49)
        ItemStack divider = PlayerGUI.createGuiItem(Material.YELLOW_STAINED_GLASS_PANE, "&8┃", null);
        ItemStack dividerSub = PlayerGUI.createGuiItem(Material.GRAY_STAINED_GLASS_PANE, "&8┃", null);
        for (int slot : DIVIDER_SLOTS) {
            inv.setItem(slot, (slot == 4 || slot == 49) ? divider : dividerSub);
        }

        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        boolean hasRace = data != null && data.hasAssignedRace();
        Race race = hasRace ? plugin.getRaceManager().getRace(data.getRace()) : null;
        String raceDisplay = race != null ? race.getDisplayName() : "&fSin Raza";

        // Cabecera izquierda (Slot 1)
        inv.setItem(1, PlayerGUI.createGuiItem(Material.MAP, "&e&lMisiones de Linaje", List.of(
                "&7Raza Activa: " + raceDisplay,
                "&7Completadas: &a" + (data != null ? data.getQuestsCompletedCount() : 0),
                "",
                "&7Haz clic en una misión a la izquierda",
                "&7para inspeccionar sus detalles y reclamar."
        )));

        if (!hasRace) {
            inv.setItem(20, PlayerGUI.createGuiItem(Material.BARRIER, "&c&lSin Raza Asignada", List.of(
                    "&7Aún no perteneces a ningún linaje.",
                    "&7Usa &e/razas &7para elegir tu raza inicial",
                    "&7y desbloquear tus misiones raciales."
            )));
            inv.setItem(24, PlayerGUI.createGuiItem(Material.NETHER_STAR, "&e&lElige tu Raza", List.of(
                    "&7Haz clic para abrir el selector de 16 razas."
            )));
            inv.setItem(45, PlayerGUI.createGuiItem(Material.ARROW, "&eVolver al Menú Principal", List.of("&7Regresa al hub principal.")));
            inv.setItem(53, PlayerGUI.createGuiItem(Material.BARRIER, "&c&lCerrar", null));
            player.openInventory(inv);
            return;
        }

        List<QuestManager.RacialQuest> quests = plugin.getQuestManager().getQuestsForRace(data.getRace());
        String selectedId = selectedQuestMap.get(player.getUniqueId());
        QuestManager.RacialQuest selected = null;
        for (QuestManager.RacialQuest q : quests) {
            if (q.getId().equalsIgnoreCase(selectedId)) {
                selected = q;
                break;
            }
        }
        if (selected == null && !quests.isEmpty()) {
            selected = quests.get(0);
            selectedQuestMap.put(player.getUniqueId(), selected.getId());
        }

        // Renderizar misiones en el panel izquierdo
        for (int i = 0; i < quests.size() && i < LEFT_QUEST_SLOTS.length; i++) {
            QuestManager.RacialQuest q = quests.get(i);
            int slot = LEFT_QUEST_SLOTS[i];
            boolean completed = data.isQuestCompleted(q.getId());
            int progress = Math.min(q.getTargetAmount(), data.getQuestProgress(q.getId()));
            boolean readyToClaim = !completed && progress >= q.getTargetAmount();
            boolean isSelected = selected != null && selected.getId().equalsIgnoreCase(q.getId());

            Material iconMat = completed ? Material.BOOK : (readyToClaim ? Material.ENCHANTED_GOLDEN_APPLE : Material.WRITABLE_BOOK);
            String stateTag = completed ? "&a✔ COMPLETADA" : (readyToClaim ? "&e★ ¡LISTA PARA RECLAMAR!" : "&7⌛ EN CURSO");
            String selPrefix = isSelected ? "&6▶ " : "";

            List<String> lore = new ArrayList<>();
            lore.add("&7Estado: " + stateTag);
            lore.add("&7Progreso: &f" + progress + " &7/ &f" + q.getTargetAmount());
            lore.add("");
            lore.add(isSelected ? "&a✔ Seleccionada en el panel derecho." : "&e▶ Clic izquierdo para inspeccionar.");
            if (readyToClaim) {
                lore.add("&6▶ Clic derecho para reclamar al instante.");
            }

            inv.setItem(slot, PlayerGUI.createGuiItem(iconMat, selPrefix + "&f&l" + q.getName(), lore));
        }

        // Renderizar detalle en el panel derecho (cols 5-8)
        if (selected != null) {
            boolean completed = data.isQuestCompleted(selected.getId());
            int progress = Math.min(selected.getTargetAmount(), data.getQuestProgress(selected.getId()));
            boolean ready = !completed && progress >= selected.getTargetAmount();

            // Slot 15: Título y Categoría
            inv.setItem(15, PlayerGUI.createGuiItem(Material.NAME_TAG, "&6&l" + selected.getName(), List.of(
                    "&7Linaje: " + raceDisplay,
                    "&7Tipo de Desafío: &b" + translateQuestType(selected.getType()),
                    "&7Estado Actual: " + (completed ? "&a✔ Completada y Reclamada" : (ready ? "&e★ Lista para Reclamar" : "&7En Progreso"))
            )));

            // Slot 24: Objetivos + Barra de progreso en vivo
            String bar = buildProgressBar(progress, selected.getTargetAmount(), 14);
            int pct = selected.getTargetAmount() > 0 ? (int) Math.round((progress * 100.0) / selected.getTargetAmount()) : 100;
            inv.setItem(24, PlayerGUI.createGuiItem(Material.FILLED_MAP, "&e&lObjetivo y Progreso", List.of(
                    "&f" + selected.getDescription(),
                    "",
                    "&7Avance: " + bar + " &f" + pct + "%",
                    "&7Contador: &e" + progress + " &7/ &e" + selected.getTargetAmount()
            )));

            // Slot 33: Recompensas
            List<String> rewLore = new ArrayList<>();
            if (selected.getRewardMoney() > 0) {
                rewLore.add("&7• Economía Vault: &e$" + String.format("%.0f", selected.getRewardMoney()));
            }
            if (selected.getRewardTalentPoints() > 0) {
                rewLore.add("&7• Puntos de Talento: &b+" + selected.getRewardTalentPoints() + " Pts");
            }
            if (selected.getRewardTitle() != null && !selected.getRewardTitle().isEmpty()) {
                rewLore.add("&7• Título Cosmético: " + selected.getRewardTitle());
            }
            rewLore.add("");
            rewLore.add("&8Las recompensas se otorgan una única vez.");
            inv.setItem(33, PlayerGUI.createGuiItem(Material.CHEST, "&6&lRecompensas de Misión", rewLore));

            // Slot 42: Botón de Reclamo
            if (completed) {
                inv.setItem(42, PlayerGUI.createGuiItem(Material.BOOK, "&2&lRECOMPENSA RECLAMADA", List.of(
                        "&7Ya cobraste todas las recompensas",
                        "&7correspondientes a esta misión."
                )));
            } else if (ready) {
                inv.setItem(42, PlayerGUI.createGuiItem(Material.EMERALD_BLOCK, "&a&l¡RECLAMAR RECOMPENSA!", List.of(
                        "&7¡Has cumplido el 100% del objetivo!",
                        "&e▶ Clic aquí para recibir tus premios."
                )));
            } else {
                inv.setItem(42, PlayerGUI.createGuiItem(Material.CLOCK, "&7&lOBJETIVO INCOMPLETO", List.of(
                        "&7Aún te faltan &e" + (selected.getTargetAmount() - progress) + " &7acciones",
                        "&7para poder reclamar esta recompensa."
                )));
            }
        }

        // Navegación inferior (Slots 45 y 53)
        inv.setItem(45, PlayerGUI.createGuiItem(Material.ARROW, "&eVolver al Menú Principal", List.of("&7Regresa al hub principal.")));
        inv.setItem(53, PlayerGUI.createGuiItem(Material.BARRIER, "&c&lCerrar", null));

        player.openInventory(inv);
    }

    public void handleClick(Player player, int slot, boolean isRightClick) {
        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        if (data == null) return;

        if (!data.hasAssignedRace()) {
            if (slot == 24) {
                plugin.getPlayerGUI().openSelectionMenu(player);
            }
            return;
        }

        List<QuestManager.RacialQuest> quests = plugin.getQuestManager().getQuestsForRace(data.getRace());

        // Clic en el panel izquierdo: seleccionar o reclamar con clic derecho
        for (int i = 0; i < quests.size() && i < LEFT_QUEST_SLOTS.length; i++) {
            if (LEFT_QUEST_SLOTS[i] == slot) {
                QuestManager.RacialQuest q = quests.get(i);
                selectedQuestMap.put(player.getUniqueId(), q.getId());
                if (isRightClick && plugin.getQuestManager().claimReward(player, q.getId())) {
                    open(player);
                    return;
                }
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.15f);
                open(player);
                return;
            }
        }

        // Clic en botón de reclamo (Slot 42)
        if (slot == 42) {
            String selId = selectedQuestMap.get(player.getUniqueId());
            if (selId != null && plugin.getQuestManager().claimReward(player, selId)) {
                open(player);
            } else {
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.7f, 1.0f);
            }
        }
    }

    private String buildProgressBar(int current, int target, int segments) {
        int filled = target > 0 ? (int) Math.round(((double) current / target) * segments) : segments;
        filled = Math.max(0, Math.min(segments, filled));
        return "&a[" + "■".repeat(filled) + "&7" + "■".repeat(segments - filled) + "&a]";
    }

    private String translateQuestType(QuestManager.QuestType type) {
        return switch (type) {
            case KILL_MOBS -> "Combate y Cacería";
            case MINE_BLOCKS -> "Minería y Extracción";
            case USE_ABILITY -> "Maestría de Habilidades";
            case COLLECT_ITEM -> "Recolección y Cosecha";
            case SURVIVE_TIME -> "Supervivencia";
        };
    }
}

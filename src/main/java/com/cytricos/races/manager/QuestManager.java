package com.cytricos.races.manager;

import com.cytricos.races.CytricosRaces;
import com.cytricos.races.model.PlayerData;
import com.cytricos.races.util.ColorUtils;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Gestor oficial de misiones raciales (Quests) para CYTRICOS RPG.
 * Maneja misiones exclusivas de las 16 razas con progreso persistente,
 * recompensas en economía Vault, puntos de talento y títulos cosméticos.
 *
 * Lead Developers: Dandan_13 & Wolfit12
 */
public class QuestManager {
    private final CytricosRaces plugin;
    private final Map<String, RacialQuest> registeredQuests = new LinkedHashMap<>();

    public enum QuestType {
        KILL_MOBS,
        MINE_BLOCKS,
        USE_ABILITY,
        COLLECT_ITEM,
        SURVIVE_TIME
    }

    public static class RacialQuest {
        private final String id;
        private final String raceId;
        private final String name;
        private final String description;
        private final QuestType type;
        private final int targetAmount;
        private final double rewardMoney;
        private final int rewardTalentPoints;
        private final String rewardTitle;

        public RacialQuest(String id, String raceId, String name, String description, QuestType type, int targetAmount, double rewardMoney, int rewardTalentPoints, String rewardTitle) {
            this.id = id;
            this.raceId = raceId.toLowerCase();
            this.name = name;
            this.description = description;
            this.type = type;
            this.targetAmount = targetAmount;
            this.rewardMoney = rewardMoney;
            this.rewardTalentPoints = rewardTalentPoints;
            this.rewardTitle = rewardTitle;
        }

        public String getId() { return id; }
        public String getRaceId() { return raceId; }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public QuestType getType() { return type; }
        public int getTargetAmount() { return targetAmount; }
        public double getRewardMoney() { return rewardMoney; }
        public int getRewardTalentPoints() { return rewardTalentPoints; }
        public String getRewardTitle() { return rewardTitle; }
    }

    public QuestManager(CytricosRaces plugin) {
        this.plugin = plugin;
        initDefaultQuests();
    }

    private void initDefaultQuests() {
        // Vampiro
        register(new RacialQuest("vamp_hunt", "vampire", "Noche de Caza", "Elimina a 15 criaturas hostiles en la oscuridad.", QuestType.KILL_MOBS, 15, 250.0, 1, "&4[Depredador Nocturno]"));
        register(new RacialQuest("vamp_feed", "vampire", "Sed de Sangre", "Usa habilidades vampíricas 10 veces.", QuestType.USE_ABILITY, 10, 150.0, 1, null));

        // Hombre Lobo
        register(new RacialQuest("were_fury", "werewolf", "Furia Feral", "Activa tu metamorfosis o furia en combate 8 veces.", QuestType.USE_ABILITY, 8, 200.0, 1, "&c[Alfa del Bosque]"));
        register(new RacialQuest("were_hunt", "werewolf", "Cacería de Manada", "Derrota a 20 enemigos bajo la luz de la luna.", QuestType.KILL_MOBS, 20, 300.0, 1, null));

        // Hada
        register(new RacialQuest("fairy_flowers", "fairy", "Esencia Floral", "Recolecta o planta 16 flores o brotes sagrados.", QuestType.COLLECT_ITEM, 16, 150.0, 1, "&d[Guardián Floral]"));
        register(new RacialQuest("fairy_wings", "fairy", "Danza Aérea", "Usa tus habilidades feéricas 12 veces.", QuestType.USE_ABILITY, 12, 200.0, 1, null));

        // Enano
        register(new RacialQuest("dwarf_ores", "dwarf", "Vetas Profundas", "Pica 32 minerales en las cavernas subterráneas.", QuestType.MINE_BLOCKS, 32, 350.0, 1, "&6[Señor de la Forja]"));
        register(new RacialQuest("dwarf_anvil", "dwarf", "Temple de Piedra", "Activa tu firmeza sísmica 5 veces.", QuestType.USE_ABILITY, 5, 150.0, 1, null));

        // Demonio
        register(new RacialQuest("demon_nether", "demon", "Ira del Averno", "Elimina a 15 criaturas hostiles con furia infernal.", QuestType.KILL_MOBS, 15, 300.0, 1, "&c[Príncipe Abisal]"));
        register(new RacialQuest("demon_fire", "demon", "Pira Infernal", "Desata tu fuego abisal en batalla 10 veces.", QuestType.USE_ABILITY, 10, 200.0, 1, null));

        // Ángel
        register(new RacialQuest("angel_blessing", "angel", "Vigilia Celestial", "Derrota a 15 monstruos nocturnos con juicio divino.", QuestType.KILL_MOBS, 15, 250.0, 1, "&f[Serafín Sagrado]"));
        register(new RacialQuest("angel_flight", "angel", "Gracia Alada", "Usa bendiciones celestiales 8 veces.", QuestType.USE_ABILITY, 8, 200.0, 1, null));

        // Elfo
        register(new RacialQuest("elf_archery", "elf", "Tiro Silvano", "Acaba con 15 enemigos en el campo de batalla.", QuestType.KILL_MOBS, 15, 250.0, 1, "&a[Ojo de Halcón]"));
        register(new RacialQuest("elf_agility", "elf", "Paso Etéreo", "Usa tu remolino y agilidad élfica 10 veces.", QuestType.USE_ABILITY, 10, 150.0, 1, null));

        // Duende
        register(new RacialQuest("goblin_theft", "goblin", "Manos Ligeras", "Usa tus artimañas de duende 5 veces.", QuestType.USE_ABILITY, 5, 200.0, 1, "&e[Pícaro Astuto]"));
        register(new RacialQuest("goblin_gold", "goblin", "Codicia Brillante", "Pica 20 bloques o menas valiosas.", QuestType.MINE_BLOCKS, 20, 300.0, 1, null));

        // Humano
        register(new RacialQuest("human_survive", "human", "Voluntad Inquebrantable", "Elimina a 20 criaturas hostiles de cualquier clase.", QuestType.KILL_MOBS, 20, 200.0, 1, "&b[Campeón Mortal]"));
        register(new RacialQuest("human_tactics", "human", "Mente Estratégica", "Usa tus técnicas tácticas 10 veces.", QuestType.USE_ABILITY, 10, 150.0, 1, null));

        // Draconiano
        register(new RacialQuest("dragon_might", "dragonkin", "Rugido Primigenio", "Derrota a 20 enemigos con fuerza dracónica.", QuestType.KILL_MOBS, 20, 350.0, 1, "&4[Furia de Dragón]"));
        register(new RacialQuest("dragon_roar", "dragonkin", "Aliento Ancestral", "Libera tu poder dracónico 8 veces.", QuestType.USE_ABILITY, 8, 220.0, 1, null));

        // Orco
        register(new RacialQuest("orc_smash", "orc", "Ira Demoledora", "Elimina a 20 adversarios en combate cuerpo a cuerpo.", QuestType.KILL_MOBS, 20, 250.0, 1, "&2[Caudillo de Guerra]"));
        register(new RacialQuest("orc_quake", "orc", "Aplastamiento Sísmico", "Desata tu golpe sísmico con hacha 8 veces.", QuestType.USE_ABILITY, 8, 200.0, 1, null));

        // Gigante
        register(new RacialQuest("giant_titans", "giant", "Firmeza Titánica", "Derrota a 15 enemigos soportando el embate.", QuestType.KILL_MOBS, 15, 250.0, 1, "&6[Titán Indomable]"));
        register(new RacialQuest("giant_stomp", "giant", "Impacto Colosal", "Ejecuta tu impacto telúrico 8 veces.", QuestType.USE_ABILITY, 8, 220.0, 1, null));

        // No-Muerto
        register(new RacialQuest("undead_crypt", "undead", "Señor de la Cripta", "Derrota a 15 criaturas desafiando a la muerte.", QuestType.KILL_MOBS, 15, 200.0, 1, "&8[Heraldo de la Muerte]"));
        register(new RacialQuest("undead_cloud", "undead", "Nube Necrótica", "Libera miasmas o invocaciones 8 veces.", QuestType.USE_ABILITY, 8, 200.0, 1, null));

        // Yokai
        register(new RacialQuest("yokai_spirit", "yokai", "Celo Espiritual", "Ejecuta 10 manifestaciones de energía espiritual.", QuestType.USE_ABILITY, 10, 200.0, 1, "&5[Espíritu Ancestral]"));
        register(new RacialQuest("yokai_hunt", "yokai", "Velo Espectral", "Elimina a 15 entidades desde las sombras.", QuestType.KILL_MOBS, 15, 250.0, 1, null));

        // Druida
        register(new RacialQuest("druid_harmony", "druid", "Armonía Botánica", "Recolecta o cultiva 24 plantas y frutos.", QuestType.COLLECT_ITEM, 24, 200.0, 1, "&a[Archidruida]"));
        register(new RacialQuest("druid_acid", "druid", "Savia Ácida", "Desata poderes druídicos 10 veces.", QuestType.USE_ABILITY, 10, 200.0, 1, null));

        // Bestia / Semihumano
        register(new RacialQuest("beastkin_prowl", "beastkin", "Instinto Feral", "Derrota a 15 monstruos usando tu agilidad bestial.", QuestType.KILL_MOBS, 15, 200.0, 1, "&6[Garra Veloz]"));
        register(new RacialQuest("beastkin_totem", "beastkin", "Despertar Totémico", "Invoca tu espíritu totémico 8 veces.", QuestType.USE_ABILITY, 8, 200.0, 1, null));
    }

    private void register(RacialQuest quest) {
        registeredQuests.put(quest.getId().toLowerCase(), quest);
    }

    public List<RacialQuest> getQuestsForRace(String raceId) {
        if (raceId == null || raceId.equalsIgnoreCase("none") || raceId.isBlank()) {
            return Collections.emptyList();
        }
        List<RacialQuest> list = new ArrayList<>();
        String r = raceId.toLowerCase();
        for (RacialQuest q : registeredQuests.values()) {
            if (q.getRaceId().equalsIgnoreCase(r)) {
                list.add(q);
            }
        }
        return list;
    }

    public RacialQuest getQuest(String questId) {
        if (questId == null) return null;
        return registeredQuests.get(questId.toLowerCase());
    }

    public void recordAction(Player player, QuestType type, int amount) {
        if (player == null) return;
        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        if (data == null || !data.hasAssignedRace()) return;

        List<RacialQuest> quests = getQuestsForRace(data.getRace());
        for (RacialQuest q : quests) {
            if (q.getType() == type && !data.isQuestCompleted(q.getId())) {
                int before = data.getQuestProgress(q.getId());
                if (before >= q.getTargetAmount()) continue;
                data.incrementQuestProgress(q.getId(), amount);
                int prog = data.getQuestProgress(q.getId());
                if (prog >= q.getTargetAmount()) {
                    ColorUtils.sendActionBar(player, "&a&l¡MISIÓN COMPLETADA! &e" + q.getName() + " &7- Abre /raza misiones para reclamar.");
                    if (data.isSettingSounds()) {
                        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.4f);
                    }
                }
            }
        }
    }

    public boolean claimReward(Player player, String questId) {
        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        if (data == null || !data.hasAssignedRace()) return false;

        RacialQuest quest = getQuest(questId);
        if (quest == null || !quest.getRaceId().equalsIgnoreCase(data.getRace())) return false;

        if (data.isQuestCompleted(quest.getId())) {
            ColorUtils.sendMessage(player, "&cYa has reclamado la recompensa de esta misión.");
            return false;
        }

        int progress = data.getQuestProgress(quest.getId());
        if (progress < quest.getTargetAmount()) {
            ColorUtils.sendMessage(player, "&cNo has completado el objetivo aún (&e" + progress + "&c/&e" + quest.getTargetAmount() + "&c).");
            return false;
        }

        data.completeQuest(quest.getId());

        if (quest.getRewardMoney() > 0 && plugin.isVaultAvailable()) {
            plugin.getVaultEconomy().depositPlayer(player, quest.getRewardMoney());
        }

        if (quest.getRewardTalentPoints() > 0) {
            data.addTalentPoints(quest.getRewardTalentPoints());
        }

        if (quest.getRewardTitle() != null && !quest.getRewardTitle().isEmpty()) {
            data.unlockAchievement("quest_" + quest.getId());
            ColorUtils.sendMessage(player, "&6¡Has desbloqueado el título cosmético: " + quest.getRewardTitle() + "&6!");
        }

        plugin.getPlayerDataManager().saveToFile(data);

        if (data.isSettingSounds()) {
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.2f);
        }
        ColorUtils.sendTitle(player, "&a&l¡MISIÓN CUMPLIDA!", "&e" + quest.getName(), 10, 50, 20);
        ColorUtils.sendMessage(player, "&a&l[CYTRICOS RPG] &7Recompensa reclamada: &e$" + String.format("%.0f", quest.getRewardMoney())
                + " &7+ &b" + quest.getRewardTalentPoints() + " Puntos de Talento&7.");

        return true;
    }
}

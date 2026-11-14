package com.cytricos.races.model;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Almacena el estado persistente, nivel racial, árbol de talentos, misiones,
 * logros, cosméticos y preferencias de un jugador en CYTRICOS RPG (v7.0).
 *
 * Lead Developers: Dandan_13 & Wolfit12
 */
public class PlayerData {
    private final UUID uuid;
    private String race;
    private int level;
    private boolean freeRaceChangeAvailable;
    private long lastChangeTimestamp;

    // Persistencia v7.0
    private long flightBanUntil;
    private long transformCooldownUntil;
    private boolean activeDisguise;
    private long transformActiveUntil; // -1 = infinito (Nivel 6)

    // Sistema de Recursos Raciales (Expansión RPG v7.0)
    private double racialResource;
    private double maxRacialResource;

    // Estados volátiles
    private boolean beastFormActive;
    private boolean intangibilityActive;

    // 1. Árbol de Talentos y Mejoras Raciales (Skill Tree)
    private int talentPoints;
    private final Set<String> unlockedTalents;

    // 2. Misiones Raciales (Quests)
    private final Set<String> completedQuests;
    private final Map<String, Integer> questProgress;

    // 3. Logros y Títulos Cosméticos (Achievements & Titles)
    private final Set<String> unlockedAchievements;
    private String selectedTitle;

    // 4. Cosméticos y Auras de Partículas
    private String selectedCosmeticAura;

    // 5. Ajustes Personales del Jugador (Settings)
    private boolean settingParticles;
    private boolean settingSounds;
    private boolean settingActionBar;
    private boolean settingCooldownMsgs;

    // 6. Estadísticas Raciales Medibles
    private int abilitiesUsed;
    private int questsCompletedCount;
    private int mobsDefeated;
    private long timePlayedSeconds;

    public PlayerData(UUID uuid, String race, int level, boolean freeRaceChangeAvailable) {
        this.uuid = uuid;
        this.race = race != null ? race.toLowerCase() : "human";
        this.level = Math.max(1, Math.min(6, level));
        this.freeRaceChangeAvailable = freeRaceChangeAvailable;
        this.lastChangeTimestamp = System.currentTimeMillis();
        this.flightBanUntil = 0L;
        this.transformCooldownUntil = 0L;
        this.activeDisguise = false;
        this.transformActiveUntil = 0L;
        this.maxRacialResource = 100.0 + (this.level - 1) * 20.0;
        this.racialResource = this.maxRacialResource;
        this.beastFormActive = false;
        this.intangibilityActive = false;

        // Nuevos sistemas RPG
        this.talentPoints = Math.max(0, this.level - 1);
        this.unlockedTalents = Collections.newSetFromMap(new ConcurrentHashMap<>());
        this.completedQuests = Collections.newSetFromMap(new ConcurrentHashMap<>());
        this.questProgress = new ConcurrentHashMap<>();
        this.unlockedAchievements = Collections.newSetFromMap(new ConcurrentHashMap<>());
        this.selectedTitle = "";
        this.selectedCosmeticAura = "none";

        this.settingParticles = true;
        this.settingSounds = true;
        this.settingActionBar = true;
        this.settingCooldownMsgs = true;

        this.abilitiesUsed = 0;
        this.questsCompletedCount = 0;
        this.mobsDefeated = 0;
        this.timePlayedSeconds = 0L;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getRace() {
        return race;
    }

    public void setRace(String race) {
        this.race = race != null ? race.toLowerCase() : "human";
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        int oldLevel = this.level;
        this.level = Math.max(1, Math.min(6, level));
        if (this.level > oldLevel) {
            // Otorgar puntos de talento al subir de nivel
            int pointsEarned = this.level - oldLevel;
            this.talentPoints += pointsEarned;
        }
    }

    public boolean hasFreeRaceChangeAvailable() {
        return freeRaceChangeAvailable;
    }

    public void setFreeRaceChangeAvailable(boolean freeRaceChangeAvailable) {
        this.freeRaceChangeAvailable = freeRaceChangeAvailable;
    }

    public long getLastChangeTimestamp() {
        return lastChangeTimestamp;
    }

    public void setLastChangeTimestamp(long lastChangeTimestamp) {
        this.lastChangeTimestamp = lastChangeTimestamp;
    }

    public long getFlightBanUntil() {
        return flightBanUntil;
    }

    public void setFlightBanUntil(long flightBanUntil) {
        this.flightBanUntil = flightBanUntil;
    }

    public boolean isFlightBanned() {
        return System.currentTimeMillis() < flightBanUntil;
    }

    public long getFlightBanRemainingSeconds() {
        long diff = flightBanUntil - System.currentTimeMillis();
        return diff > 0 ? diff / 1000L : 0L;
    }

    public void clearFlightBan() {
        this.flightBanUntil = 0L;
    }

    public long getTransformCooldownUntil() {
        return transformCooldownUntil;
    }

    public void setTransformCooldownUntil(long transformCooldownUntil) {
        this.transformCooldownUntil = transformCooldownUntil;
    }

    public boolean isTransformOnCooldown() {
        return System.currentTimeMillis() < transformCooldownUntil;
    }

    public long getTransformCooldownRemainingSeconds() {
        long diff = transformCooldownUntil - System.currentTimeMillis();
        return diff > 0 ? diff / 1000L : 0L;
    }

    public boolean isActiveDisguise() {
        return activeDisguise;
    }

    public void setActiveDisguise(boolean activeDisguise) {
        this.activeDisguise = activeDisguise;
    }

    public long getTransformActiveUntil() {
        return transformActiveUntil;
    }

    public void setTransformActiveUntil(long transformActiveUntil) {
        this.transformActiveUntil = transformActiveUntil;
    }

    public boolean isTransformed() {
        if (!activeDisguise) return false;
        if (transformActiveUntil == -1L) return true; // Infinito (Nivel 6)
        return System.currentTimeMillis() < transformActiveUntil;
    }

    public long getTransformRemainingSeconds() {
        if (!activeDisguise) return 0L;
        if (transformActiveUntil == -1L) return -1L; // Infinito
        long diff = transformActiveUntil - System.currentTimeMillis();
        return diff > 0 ? diff / 1000L : 0L;
    }

    public boolean isBeastFormActive() {
        return beastFormActive;
    }

    public void setBeastFormActive(boolean beastFormActive) {
        this.beastFormActive = beastFormActive;
    }

    public boolean isIntangibilityActive() {
        return intangibilityActive;
    }

    public void setIntangibilityActive(boolean intangibilityActive) {
        this.intangibilityActive = intangibilityActive;
    }

    // Métodos del Sistema de Recursos Raciales
    public double getRacialResource() {
        return racialResource;
    }

    public void setRacialResource(double racialResource) {
        this.racialResource = Math.max(0.0, Math.min(this.maxRacialResource, racialResource));
    }

    public double getMaxRacialResource() {
        return maxRacialResource;
    }

    public void setMaxRacialResource(double maxRacialResource) {
        this.maxRacialResource = Math.max(10.0, maxRacialResource);
        if (this.racialResource > this.maxRacialResource) {
            this.racialResource = this.maxRacialResource;
        }
    }

    public boolean consumeRacialResource(double amount) {
        if (this.racialResource >= amount) {
            this.racialResource -= amount;
            return true;
        }
        return false;
    }

    public void addRacialResource(double amount) {
        this.racialResource = Math.min(this.maxRacialResource, this.racialResource + amount);
    }

    public String getResourceName() {
        return switch (race.toLowerCase()) {
            case "vampire" -> "Esencia de Sangre";
            case "werewolf" -> "Furia Feral";
            case "fairy" -> "Polvo Feérico";
            case "druid" -> "Maná Botánico";
            case "dwarf" -> "Temple de Piedra";
            case "demon" -> "Fuego Abisal";
            case "angel" -> "Gracia Divina";
            case "dragonkin" -> "Fuerza Dracónica";
            case "beastkin" -> "Instinto Salvaje";
            case "goblin" -> "Astucia de Duende";
            case "orc" -> "Ira Berserker";
            case "giant" -> "Vigor Titánico";
            case "elf" -> "Concentración Silvana";
            case "undead" -> "Poder Necrótico";
            case "yokai" -> "Energía Espiritual";
            default -> "Adrenalina Táctica";
        };
    }

    // Talentos Raciales (Skill Tree)
    public int getTalentPoints() {
        return talentPoints;
    }

    public void setTalentPoints(int talentPoints) {
        this.talentPoints = Math.max(0, talentPoints);
    }

    public void addTalentPoints(int points) {
        this.talentPoints += points;
    }

    public boolean hasTalent(String talentId) {
        return unlockedTalents.contains(talentId.toLowerCase());
    }

    public boolean unlockTalent(String talentId) {
        if (talentPoints > 0 && !hasTalent(talentId)) {
            unlockedTalents.add(talentId.toLowerCase());
            talentPoints--;
            return true;
        }
        return false;
    }

    public Set<String> getUnlockedTalents() {
        return Collections.unmodifiableSet(unlockedTalents);
    }

    public void setUnlockedTalentsRaw(Collection<String> talents) {
        unlockedTalents.clear();
        if (talents != null) {
            for (String t : talents) {
                unlockedTalents.add(t.toLowerCase());
            }
        }
    }

    public void resetTalents() {
        int refunded = unlockedTalents.size();
        unlockedTalents.clear();
        this.talentPoints += refunded;
    }

    // Misiones Raciales (Quests)
    public boolean isQuestCompleted(String questId) {
        return completedQuests.contains(questId.toLowerCase());
    }

    public void completeQuest(String questId) {
        completedQuests.add(questId.toLowerCase());
        this.questsCompletedCount++;
    }

    public Set<String> getCompletedQuests() {
        return Collections.unmodifiableSet(completedQuests);
    }

    public void setCompletedQuestsRaw(Collection<String> quests) {
        completedQuests.clear();
        if (quests != null) {
            for (String q : quests) {
                completedQuests.add(q.toLowerCase());
            }
        }
    }

    public int getQuestProgress(String questId) {
        return questProgress.getOrDefault(questId.toLowerCase(), 0);
    }

    public void setQuestProgress(String questId, int progress) {
        questProgress.put(questId.toLowerCase(), progress);
    }

    public void incrementQuestProgress(String questId, int amount) {
        int current = getQuestProgress(questId);
        questProgress.put(questId.toLowerCase(), current + amount);
    }

    public Map<String, Integer> getQuestProgressMap() {
        return Collections.unmodifiableMap(questProgress);
    }

    public void setQuestProgressMapRaw(Map<String, Integer> map) {
        questProgress.clear();
        if (map != null) {
            for (Map.Entry<String, Integer> entry : map.entrySet()) {
                questProgress.put(entry.getKey().toLowerCase(), entry.getValue());
            }
        }
    }

    // Logros y Títulos
    public boolean hasAchievement(String achievementId) {
        return unlockedAchievements.contains(achievementId.toLowerCase());
    }

    public void unlockAchievement(String achievementId) {
        unlockedAchievements.add(achievementId.toLowerCase());
    }

    public Set<String> getUnlockedAchievements() {
        return Collections.unmodifiableSet(unlockedAchievements);
    }

    public void setUnlockedAchievementsRaw(Collection<String> achievements) {
        unlockedAchievements.clear();
        if (achievements != null) {
            for (String a : achievements) {
                unlockedAchievements.add(a.toLowerCase());
            }
        }
    }

    public String getSelectedTitle() {
        return selectedTitle != null ? selectedTitle : "";
    }

    public void setSelectedTitle(String selectedTitle) {
        this.selectedTitle = selectedTitle != null ? selectedTitle : "";
    }

    // Cosméticos
    public String getSelectedCosmeticAura() {
        return selectedCosmeticAura != null ? selectedCosmeticAura : "none";
    }

    public void setSelectedCosmeticAura(String selectedCosmeticAura) {
        this.selectedCosmeticAura = selectedCosmeticAura != null ? selectedCosmeticAura : "none";
    }

    // Ajustes Personales (Settings)
    public boolean isSettingParticles() {
        return settingParticles;
    }

    public void setSettingParticles(boolean settingParticles) {
        this.settingParticles = settingParticles;
    }

    public boolean isSettingSounds() {
        return settingSounds;
    }

    public void setSettingSounds(boolean settingSounds) {
        this.settingSounds = settingSounds;
    }

    public boolean isSettingActionBar() {
        return settingActionBar;
    }

    public void setSettingActionBar(boolean settingActionBar) {
        this.settingActionBar = settingActionBar;
    }

    public boolean isSettingCooldownMsgs() {
        return settingCooldownMsgs;
    }

    public void setSettingCooldownMsgs(boolean settingCooldownMsgs) {
        this.settingCooldownMsgs = settingCooldownMsgs;
    }

    // Estadísticas
    public int getAbilitiesUsed() {
        return abilitiesUsed;
    }

    public void incrementAbilitiesUsed() {
        this.abilitiesUsed++;
    }

    public void setAbilitiesUsed(int abilitiesUsed) {
        this.abilitiesUsed = abilitiesUsed;
    }

    public int getQuestsCompletedCount() {
        return questsCompletedCount;
    }

    public void setQuestsCompletedCount(int questsCompletedCount) {
        this.questsCompletedCount = questsCompletedCount;
    }

    public int getMobsDefeated() {
        return mobsDefeated;
    }

    public void incrementMobsDefeated() {
        this.mobsDefeated++;
    }

    public void setMobsDefeated(int mobsDefeated) {
        this.mobsDefeated = mobsDefeated;
    }

    public long getTimePlayedSeconds() {
        return timePlayedSeconds;
    }

    public void addTimePlayed(long seconds) {
        this.timePlayedSeconds += seconds;
    }

    public void setTimePlayedSeconds(long timePlayedSeconds) {
        this.timePlayedSeconds = timePlayedSeconds;
    }
}

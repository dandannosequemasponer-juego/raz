package com.cytricos.races.model;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Almacena el estado persistente, nivel racial, árbol de talentos, misiones,
 * logros, cosméticos, mochila táctica y preferencias de un jugador en CYTRICOS RPG (v7.0).
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

    // Sistema de Recursos Raciales
    private double racialResource;
    private double maxRacialResource;

    // Estados volátiles
    private boolean beastFormActive;
    private boolean intangibilityActive;

    // 1. Árbol de Talentos y Mejoras Raciales (Skill Tree)
    private int talentPoints;
    private final Set<String> unlockedTalents;
    private int mobKillsCounter;

    // 2. Misiones Raciales (Quests)
    private final Set<String> completedQuests;
    private final Map<String, Integer> questProgress;

    // 3. Logros y Títulos Cosméticos (Achievements & Titles)
    private final Set<String> unlockedAchievements;
    private String selectedTitle;

    // 4. Cosméticos, Alas 3D, Cosméticos de Cabeza y Auras
    private String selectedCosmeticAura;
    private String selectedWings;
    private String selectedHeadCosmetic;
    private boolean firstPersonWingsHidden;
    private final Set<String> unlockedCosmetics;

    // 5. Mochila Táctica Persistente de Humano (Base64 / YAML)
    private String backpackBase64;

    // 6. Ajustes Personales del Jugador (Settings)
    private boolean settingParticles;
    private boolean settingSounds;
    private boolean settingActionBar;
    private boolean settingCooldownMsgs;

    // 7. Estadísticas Raciales Medibles
    private int abilitiesUsed;
    private int questsCompletedCount;
    private int mobsDefeated;
    private long timePlayedSeconds;

    public PlayerData(UUID uuid, String race, int level, boolean freeRaceChangeAvailable) {
        this.uuid = uuid;
        this.race = (race != null && !race.isBlank()) ? race.toLowerCase() : "none";
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

        this.talentPoints = 0;
        this.mobKillsCounter = 0;
        this.unlockedTalents = Collections.newSetFromMap(new ConcurrentHashMap<>());
        this.completedQuests = Collections.newSetFromMap(new ConcurrentHashMap<>());
        this.questProgress = new ConcurrentHashMap<>();
        this.unlockedAchievements = Collections.newSetFromMap(new ConcurrentHashMap<>());
        this.selectedTitle = "";
        this.selectedCosmeticAura = "none";
        this.selectedWings = "none";
        this.selectedHeadCosmetic = "none";
        this.firstPersonWingsHidden = true;
        this.unlockedCosmetics = Collections.newSetFromMap(new ConcurrentHashMap<>());
        this.backpackBase64 = "";

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

    public boolean hasAssignedRace() {
        return race != null && !race.isBlank() && !race.equalsIgnoreCase("none");
    }

    public void setRace(String race) {
        this.race = (race != null && !race.isBlank()) ? race.toLowerCase() : "none";
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        int oldLevel = this.level;
        this.level = Math.max(1, Math.min(6, level));
        if (this.level > oldLevel && hasAssignedRace()) {
            int pointsEarned = this.level - oldLevel;
            this.talentPoints += pointsEarned;
        }
    }

    public void setLevelRaw(int level) {
        this.level = Math.max(1, Math.min(6, level));
    }

    /**
     * Reinicia por completo el progreso racial al confirmar un cambio de raza:
     * Nivel 1, 0 puntos de talento, talentos bloqueados y misiones raciales limpias.
     */
    public void wipeRaceProgress() {
        this.level = 1;
        this.talentPoints = 0;
        this.mobKillsCounter = 0;
        this.unlockedTalents.clear();
        this.completedQuests.clear();
        this.questProgress.clear();
        this.questsCompletedCount = 0;
        this.maxRacialResource = 100.0;
        this.racialResource = 100.0;
        this.activeDisguise = false;
        this.transformActiveUntil = 0L;
        this.transformCooldownUntil = 0L;
        this.beastFormActive = false;
        this.intangibilityActive = false;
        this.selectedWings = "none";
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
        if (transformActiveUntil == -1L) return true;
        return System.currentTimeMillis() < transformActiveUntil;
    }

    public long getTransformRemainingSeconds() {
        if (!activeDisguise) return 0L;
        if (transformActiveUntil == -1L) return -1L;
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
        if (!hasAssignedRace()) return "Energía Latente";
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

    // Talentos Raciales y Contador de Bajas
    public int getTalentPoints() {
        return talentPoints;
    }

    public void setTalentPoints(int talentPoints) {
        this.talentPoints = Math.max(0, talentPoints);
    }

    public void addTalentPoints(int points) {
        this.talentPoints = Math.max(0, this.talentPoints + points);
    }

    public int getMobKillsCounter() {
        return mobKillsCounter;
    }

    public void setMobKillsCounter(int mobKillsCounter) {
        this.mobKillsCounter = Math.max(0, mobKillsCounter);
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
        if (completedQuests.add(questId.toLowerCase())) {
            this.questsCompletedCount++;
        }
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

    // Cosméticos, Alas y Cabeza
    public String getSelectedCosmeticAura() {
        return selectedCosmeticAura != null ? selectedCosmeticAura : "none";
    }

    public void setSelectedCosmeticAura(String selectedCosmeticAura) {
        this.selectedCosmeticAura = selectedCosmeticAura != null ? selectedCosmeticAura : "none";
    }

    public String getSelectedWings() {
        return selectedWings != null ? selectedWings : "none";
    }

    public void setSelectedWings(String selectedWings) {
        this.selectedWings = selectedWings != null ? selectedWings.toLowerCase() : "none";
    }

    public String getSelectedHeadCosmetic() {
        return selectedHeadCosmetic != null ? selectedHeadCosmetic : "none";
    }

    public void setSelectedHeadCosmetic(String selectedHeadCosmetic) {
        this.selectedHeadCosmetic = selectedHeadCosmetic != null ? selectedHeadCosmetic.toLowerCase() : "none";
    }

    public boolean isFirstPersonWingsHidden() {
        return firstPersonWingsHidden;
    }

    public void setFirstPersonWingsHidden(boolean firstPersonWingsHidden) {
        this.firstPersonWingsHidden = firstPersonWingsHidden;
    }

    public boolean hasUnlockedCosmetic(String cosmeticId) {
        if (cosmeticId == null) return false;
        return unlockedCosmetics.contains(cosmeticId.toLowerCase());
    }

    public void unlockCosmetic(String cosmeticId) {
        if (cosmeticId != null && !cosmeticId.isBlank()) {
            unlockedCosmetics.add(cosmeticId.toLowerCase());
        }
    }

    public Set<String> getUnlockedCosmetics() {
        return Collections.unmodifiableSet(unlockedCosmetics);
    }

    public void setUnlockedCosmeticsRaw(Collection<String> cosmetics) {
        unlockedCosmetics.clear();
        if (cosmetics != null) {
            for (String c : cosmetics) {
                if (c != null && !c.isBlank()) {
                    unlockedCosmetics.add(c.toLowerCase());
                }
            }
        }
    }

    // Mochila Táctica Persistente
    public String getBackpackBase64() {
        return backpackBase64 != null ? backpackBase64 : "";
    }

    public void setBackpackBase64(String backpackBase64) {
        this.backpackBase64 = backpackBase64 != null ? backpackBase64 : "";
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

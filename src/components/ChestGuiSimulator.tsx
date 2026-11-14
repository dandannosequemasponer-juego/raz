import React, { useState } from 'react';
import { INITIAL_RACES_DATA } from '../data/pluginCodeData';
import { MinecraftItemIcon } from './MinecraftItemIcon';
import { MinecraftTooltip } from './MinecraftTooltip';
import { Check, X, ShieldAlert, Sparkles, Volume2, ArrowLeft } from 'lucide-react';

interface ChestGuiSimulatorProps {
  playerRace: string;
  setPlayerRace: (raceId: string) => void;
  playerLevel: number;
  setPlayerLevel: (lvl: number) => void;
  freeChangeAvailable: boolean;
  setFreeChangeAvailable: (avail: boolean) => void;
  vaultBalance: number;
  setVaultBalance: (balance: number) => void;
  hasSpecialToken: boolean;
  setHasSpecialToken: (has: boolean) => void;
  globalChangesEnabled: boolean;
  racesSlots: Record<string, number>;
  onLogMessage: (msg: string) => void;
}

export const ChestGuiSimulator: React.FC<ChestGuiSimulatorProps> = ({
  playerRace,
  setPlayerRace,
  playerLevel,
  setPlayerLevel,
  freeChangeAvailable,
  setFreeChangeAvailable,
  vaultBalance,
  setVaultBalance,
  hasSpecialToken,
  setHasSpecialToken,
  globalChangesEnabled,
  racesSlots,
  onLogMessage
}) => {
  const [hoveredSlot, setHoveredSlot] = useState<number | null>(null);
  const [activeModal, setActiveModal] = useState<'none' | 'confirm' | 'progress'>('none');
  const [targetRaceId, setTargetRaceId] = useState<string | null>(null);

  // Simple Web Audio feedback synthesizer for classic Minecraft clicks & bells
  const playSound = (type: 'click' | 'levelup' | 'error' | 'success') => {
    try {
      const ctx = new (window.AudioContext || (window as unknown as { webkitAudioContext: typeof AudioContext }).webkitAudioContext)();
      const osc = ctx.createOscillator();
      const gain = ctx.createGain();
      osc.connect(gain);
      gain.connect(ctx.destination);

      const now = ctx.currentTime;
      if (type === 'click') {
        osc.frequency.setValueAtTime(800, now);
        osc.frequency.exponentialRampToValueAtTime(400, now + 0.05);
        gain.gain.setValueAtTime(0.2, now);
        gain.gain.linearRampToValueAtTime(0.01, now + 0.05);
        osc.start(now);
        osc.stop(now + 0.05);
      } else if (type === 'success') {
        osc.frequency.setValueAtTime(523.25, now); // C5
        osc.frequency.setValueAtTime(659.25, now + 0.1); // E5
        osc.frequency.setValueAtTime(783.99, now + 0.2); // G5
        gain.gain.setValueAtTime(0.3, now);
        gain.gain.linearRampToValueAtTime(0.01, now + 0.35);
        osc.start(now);
        osc.stop(now + 0.35);
      } else if (type === 'levelup') {
        osc.frequency.setValueAtTime(440, now);
        osc.frequency.exponentialRampToValueAtTime(880, now + 0.25);
        gain.gain.setValueAtTime(0.35, now);
        gain.gain.linearRampToValueAtTime(0.01, now + 0.3);
        osc.start(now);
        osc.stop(now + 0.3);
      } else if (type === 'error') {
        osc.frequency.setValueAtTime(200, now);
        osc.frequency.linearRampToValueAtTime(120, now + 0.15);
        gain.gain.setValueAtTime(0.3, now);
        gain.gain.linearRampToValueAtTime(0.01, now + 0.15);
        osc.start(now);
        osc.stop(now + 0.15);
      }
    } catch {
      // Audio fallback
    }
  };

  const getRaceBySlot = (slot: number) => {
    return INITIAL_RACES_DATA.find(r => (racesSlots[r.id] ?? r.slot) === slot);
  };

  const currentRaceObj = INITIAL_RACES_DATA.find(r => r.id === playerRace) || INITIAL_RACES_DATA[1];
  const targetRaceObj = INITIAL_RACES_DATA.find(r => r.id === targetRaceId);

  const handleSlotClick = (slot: number) => {
    playSound('click');

    // Slot 36: Player data head
    if (slot === 36) {
      onLogMessage(`§8[§eCYTRICOS§8] §7Estadísticas de §aDandan_13§7: Raza §e${currentRaceObj.name} §7(Nivel §6${playerLevel}§7), Escala §b${currentRaceObj.scale}x§7.`);
      return;
    }

    // Slot 44: Confirm button
    if (slot === 44) {
      onLogMessage("§8[§eCYTRICOS§8] §7Haz clic en una raza de la cuadrícula para abrir la confirmación.");
      return;
    }

    const clickedRace = getRaceBySlot(slot);
    if (!clickedRace) return;

    if (clickedRace.id === playerRace) {
      // Clic en su propia raza activa -> abrir progreso de 6 niveles
      setActiveModal('progress');
    } else {
      // Clic en otra raza -> abrir confirmación interactiva
      setTargetRaceId(clickedRace.id);
      setActiveModal('confirm');
    }
  };

  const handleConfirmChange = () => {
    if (!globalChangesEnabled) {
      playSound('error');
      onLogMessage("§c§l[CYTRICOS] §7Los cambios de raza están pausados actualmente por la administración.");
      setActiveModal('none');
      return;
    }

    if (!targetRaceObj) return;

    // Check layer hierarchy
    if (freeChangeAvailable) {
      setFreeChangeAvailable(false);
      setPlayerRace(targetRaceObj.id);
      setPlayerLevel(1);
      playSound('success');
      onLogMessage("§a§l[CYTRICOS] §7¡Has utilizado tu cambio de raza gratuito único!");
      onLogMessage(`§e§l[CYTRICOS] §7Tu raza ha sido cambiada a: §a${targetRaceObj.name.toUpperCase()} (Nivel 1)`);
    } else if (hasSpecialToken) {
      setHasSpecialToken(false);
      setPlayerRace(targetRaceObj.id);
      setPlayerLevel(1);
      playSound('success');
      onLogMessage("§a§l[CYTRICOS] §7Has entregado tu §6Esencia de Reencarnación Ancestral §7y cambiado de raza.");
      onLogMessage(`§e§l[CYTRICOS] §7Tu raza ha sido cambiada a: §a${targetRaceObj.name.toUpperCase()} (Nivel 1)`);
    } else if (vaultBalance >= 50000) {
      setVaultBalance(vaultBalance - 50000);
      setPlayerRace(targetRaceObj.id);
      setPlayerLevel(1);
      playSound('success');
      onLogMessage("§a§l[CYTRICOS] §7Cambio de raza realizado con éxito por §e$50,000§7.");
      onLogMessage(`§e§l[CYTRICOS] §7Tu raza ha sido cambiada a: §a${targetRaceObj.name.toUpperCase()} (Nivel 1)`);
    } else {
      playSound('error');
      onLogMessage("§c§l[CYTRICOS] §7No posees el cambio gratuito, ni el dinero suficiente ($50,000), ni la Esencia Ancestral.");
    }

    setActiveModal('none');
  };

  const handleLevelUp = () => {
    if (playerLevel >= 6) {
      playSound('error');
      onLogMessage("§e§l[CYTRICOS] §7Ya te encuentras en el nivel máximo legendario.");
      return;
    }

    const nextLvl = currentRaceObj.levels[playerLevel];
    if (vaultBalance >= nextLvl.cost) {
      setVaultBalance(vaultBalance - nextLvl.cost);
      setPlayerLevel(playerLevel + 1);
      playSound('levelup');
      onLogMessage(`§a§l[CYTRICOS] §7¡Has ascendido al Nivel ${playerLevel + 1} (${nextLvl.name}) de ${currentRaceObj.name}!`);
    } else {
      playSound('error');
      onLogMessage(`§c§l[CYTRICOS] §7No tienes suficiente dinero ($${nextLvl.cost.toLocaleString()}) para ascender.`);
    }
  };

  return (
    <div className="relative flex flex-col items-center">
      {/* HUD Info Banner */}
      <div className="w-full max-w-4xl mb-4 bg-slate-900/90 border border-slate-800 rounded-xl p-3 flex flex-wrap items-center justify-between gap-3 text-xs">
        <div className="flex items-center gap-4">
          <div className="flex items-center gap-2">
            <span className="text-slate-400">Jugador:</span>
            <span className="font-semibold text-emerald-400">Dandan_13</span>
          </div>
          <span className="text-slate-700">|</span>
          <div className="flex items-center gap-2">
            <span className="text-slate-400">Raza Activa:</span>
            <span className="font-semibold text-amber-300">{currentRaceObj.name} (Lvl {playerLevel})</span>
          </div>
          <span className="text-slate-700">|</span>
          <div className="flex items-center gap-2">
            <span className="text-slate-400">Escala (Hitbox):</span>
            <span className="font-semibold text-cyan-300">{currentRaceObj.scale}x</span>
          </div>
        </div>

        <div className="flex items-center gap-3">
          <div className="px-2.5 py-1 bg-slate-800/80 rounded border border-slate-700 text-slate-300">
            Vault: <span className="text-yellow-400 font-bold">${vaultBalance.toLocaleString()}</span>
          </div>
          <div className={`px-2.5 py-1 rounded border text-[11px] ${
            freeChangeAvailable
              ? 'bg-emerald-950/60 border-emerald-700 text-emerald-300'
              : 'bg-slate-800 border-slate-700 text-slate-400'
          }`}>
            Token Gratis: {freeChangeAvailable ? 'Disponible (1x)' : 'Consumido'}
          </div>
          <div className={`px-2.5 py-1 rounded border text-[11px] ${
            hasSpecialToken
              ? 'bg-purple-950/60 border-purple-700 text-purple-300'
              : 'bg-slate-800 border-slate-700 text-slate-400'
          }`}>
            Esencia Reencarnación: {hasSpecialToken ? 'En Mano' : 'No'}
          </div>
        </div>
      </div>

      {/* MINECRAFT CHEST CONTAINER (Pixel-Accurate to screenshots!) */}
      <div className="relative bg-[#c6c6c6] p-4 rounded-lg shadow-2xl border-4 border-[#373737] border-t-[#ffffff] border-l-[#ffffff]">
        {/* Chest Header */}
        <div className="flex items-center justify-between pb-3 px-1">
          <div className="flex items-center gap-2">
            <span className="font-mc text-lg font-bold text-[#373737] tracking-wider">
              Cofre grande
            </span>
            <span className="text-[11px] font-mc text-[#606060]">
              (/razas · 45 slots)
            </span>
          </div>

          <div className="flex items-center gap-2">
            {!globalChangesEnabled && (
              <span className="font-mc text-xs bg-red-800 text-red-100 px-2 py-0.5 rounded flex items-center gap-1 font-bold">
                <ShieldAlert className="w-3.5 h-3.5" />
                CAMBIOS PAUSADOS
              </span>
            )}
            <span className="font-mc text-xs text-[#555555]">
              CYTRICOS RPG
            </span>
          </div>
        </div>

        {/* 45-Slot Grid (5 Rows x 9 Columns) */}
        <div className="grid grid-cols-9 gap-1 bg-[#8b8b8b] p-1.5 rounded border-2 border-[#373737] border-b-[#ffffff] border-r-[#ffffff]">
          {Array.from({ length: 45 }).map((_, slotIdx) => {
            const race = getRaceBySlot(slotIdx);
            const isCurrentRace = race?.id === playerRace;
            const isPlayerHead = slotIdx === 36;
            const isConfirmButton = slotIdx === 44;

            let itemMaterial = 'black_stained_glass_pane';
            let itemTitle = ' ';
            let itemLore: string[] = [];
            let itemScale: number | undefined = undefined;

            if (race) {
              itemMaterial = race.material;
              itemTitle = race.name;
              itemScale = race.scale;
              itemLore = [
                isCurrentRace ? '§a§l[TU RAZA ACTUAL - NIVEL ' + playerLevel + ']' : '§7Haz clic para seleccionar',
                `§7Escala Física: §e${race.scale}x`,
                `§7Vida Base: §c${race.baseHealth * 2} HP`,
                `§7Velocidad: §b${Math.round(race.speed * 100)}%`,
                `§7Daño Base: §d${Math.round(race.damage * 100)}%`,
                isCurrentRace ? '§e▶ Clic para ver tus 6 Niveles y ascender' : '§a▶ Clic para reencarnar en esta raza'
              ];
            } else if (isPlayerHead) {
              itemMaterial = 'player_head';
              itemTitle = 'Cabeza del jugador datos raza';
              itemLore = [
                '§7Jugador: §bDandan_13',
                `§7Raza actual: §e${currentRaceObj.name}`,
                `§7Nivel actual: §6Nivel ${playerLevel}`,
                `§7Escala física: §a${currentRaceObj.scale}x`,
                `§7Cambio gratis: ${freeChangeAvailable ? '§aDisponible' : '§cConsumido'}`,
                `§7Dinero Vault: §e$${vaultBalance.toLocaleString()}`,
                '§8IP: PLAY.CYTRICOS.COM'
              ];
            } else if (isConfirmButton) {
              itemMaterial = 'lime_wool';
              itemTitle = 'Cambiar raza';
              itemLore = [
                '§7Haz clic en una raza para',
                '§7previsualizar y confirmar.',
                freeChangeAvailable ? '§a✔ 1x Cambio Gratuito Disponible' : '§eCoste de cambio: $50,000'
              ];
            }

            return (
              <div
                key={slotIdx}
                onMouseEnter={() => setHoveredSlot(slotIdx)}
                onMouseLeave={() => setHoveredSlot(null)}
                onClick={() => handleSlotClick(slotIdx)}
                className={`relative w-11 h-11 md:w-12 md:h-12 flex items-center justify-center cursor-pointer transition-transform ${
                  isCurrentRace
                    ? 'ring-2 ring-emerald-400 bg-emerald-950/40 rounded'
                    : 'hover:bg-white/10'
                } bg-[#8b8b8b] border-2 border-[#373737] border-t-[#373737] border-l-[#373737] border-b-[#ffffff] border-r-[#ffffff] active:border-t-[#ffffff] active:border-l-[#ffffff] active:border-b-[#373737] active:border-r-[#373737]`}
              >
                {/* Slot index indicator for development reference */}
                <span className="absolute top-0.5 left-0.5 text-[8px] font-mc text-black/40 pointer-events-none">
                  {slotIdx}
                </span>

                {/* Item Graphics */}
                <MinecraftItemIcon
                  material={itemMaterial}
                  size={32}
                  glow={isCurrentRace || (race && race.scale >= 1.2)}
                />

                {/* Current level badge for active race */}
                {isCurrentRace && (
                  <span className="absolute bottom-0 right-0.5 text-[10px] font-mc font-bold text-emerald-300 drop-shadow">
                    L{playerLevel}
                  </span>
                )}

                {/* In-Game Tooltip on Hover */}
                {hoveredSlot === slotIdx && (itemTitle !== ' ' || race || isPlayerHead || isConfirmButton) && (
                  <div className="absolute left-1/2 -translate-x-1/2 bottom-full mb-2 pointer-events-none z-50">
                    <MinecraftTooltip
                      title={itemTitle}
                      material={itemMaterial}
                      componentsCount={isPlayerHead ? 16 : isConfirmButton ? 13 : 14}
                      scale={itemScale}
                      lore={itemLore}
                      isCurrentRace={isCurrentRace}
                    />
                  </div>
                )}
              </div>
            );
          })}
        </div>

        {/* Inventory Subtitle Bar */}
        <div className="pt-3 flex justify-between items-center text-xs font-mc text-[#373737]">
          <span>Inventario</span>
          <span className="text-[10px] text-[#666666]">Spigot / Paper 1.20.5+ · Attribute.GENERIC_SCALE</span>
        </div>
      </div>

      {/* MODAL 1: CONFIRMATION GUI (Lana Verde vs Lana Roja) */}
      {activeModal === 'confirm' && targetRaceObj && (
        <div className="fixed inset-0 bg-black/80 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-[#181a20] border-2 border-slate-700 w-full max-w-md rounded-xl p-6 shadow-2xl text-center animate-in zoom-in-95">
            <div className="flex items-center justify-between pb-3 border-b border-slate-800 mb-4">
              <span className="font-mc text-sm font-bold text-amber-400">
                Confirmación de Reencarnación
              </span>
              <button
                onClick={() => setActiveModal('none')}
                className="text-slate-400 hover:text-white"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="flex flex-col items-center gap-3 my-4">
              <div className="p-3 bg-slate-900 rounded-lg border border-slate-700 shadow-inner">
                <MinecraftItemIcon material={targetRaceObj.material} size={48} glow />
              </div>
              <h3 className="text-xl font-bold text-white font-mc">
                {targetRaceObj.name}
              </h3>
              <p className="text-xs text-slate-300 max-w-sm">
                {targetRaceObj.description}
              </p>
            </div>

            {/* Warning Lore Banner */}
            <div className="bg-red-950/50 border border-red-700/60 p-3 rounded-lg text-left text-xs mb-5 space-y-1.5">
              <div className="text-red-400 font-bold flex items-center gap-1.5">
                <ShieldAlert className="w-4 h-4 shrink-0" />
                ADVERTENCIA DE PROGRESIÓN OBLIGATORIA:
              </div>
              <p className="text-red-200">
                • Tu nivel se reiniciará a <strong>Nivel 1</strong>.
              </p>
              <p className="text-red-200">
                • Se revocarán los subprefijos y pasivas de tu raza anterior.
              </p>
              <div className="pt-2 border-t border-red-900/40 text-amber-300 font-semibold">
                Método autorizado: {
                  freeChangeAvailable
                    ? '✔ Cambio Gratuito Único (Token 1x)'
                    : hasSpecialToken
                    ? '✔ Esencia de Reencarnación Ancestral'
                    : '✔ Dinero Vault ($50,000)'
                }
              </div>
            </div>

            {/* 3-Row Mini Chest Confirmation Buttons */}
            <div className="flex justify-center items-center gap-6">
              {/* Confirm (Slot 11 Lime Wool) */}
              <button
                onClick={handleConfirmChange}
                className="flex items-center gap-2 px-5 py-2.5 bg-emerald-600 hover:bg-emerald-500 active:scale-95 text-white font-mc font-bold rounded-lg shadow-lg transition-all"
              >
                <Check className="w-4 h-4" />
                <span>CONFIRMAR (Lana Verde)</span>
              </button>

              {/* Cancel (Slot 15 Red Wool) */}
              <button
                onClick={() => {
                  playSound('click');
                  setActiveModal('none');
                }}
                className="flex items-center gap-2 px-5 py-2.5 bg-red-700 hover:bg-red-600 active:scale-95 text-white font-mc font-bold rounded-lg shadow-lg transition-all"
              >
                <X className="w-4 h-4" />
                <span>CANCELAR (Lana Roja)</span>
              </button>
            </div>
          </div>
        </div>
      )}

      {/* MODAL 2: 6-LEVEL PROGRESSION & ASCENSION */}
      {activeModal === 'progress' && (
        <div className="fixed inset-0 bg-black/85 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-[#181a20] border-2 border-slate-700 w-full max-w-2xl rounded-xl p-6 shadow-2xl animate-in zoom-in-95">
            <div className="flex items-center justify-between pb-3 border-b border-slate-800 mb-4">
              <div className="flex items-center gap-2">
                <button
                  onClick={() => setActiveModal('none')}
                  className="p-1 hover:bg-slate-800 rounded text-slate-400 hover:text-white"
                >
                  <ArrowLeft className="w-4 h-4" />
                </button>
                <span className="font-mc text-sm font-bold text-amber-400">
                  Progreso de {currentRaceObj.name} (6 Niveles)
                </span>
              </div>
              <span className="text-xs font-mc text-slate-400">
                Nivel Actual: <strong className="text-emerald-400">{playerLevel} / 6</strong>
              </span>
            </div>

            {/* 6 Levels Grid */}
            <div className="grid grid-cols-2 md:grid-cols-3 gap-3 my-4">
              {currentRaceObj.levels.map((lvl) => {
                const isCurrent = lvl.lvl === playerLevel;
                const isUnlocked = lvl.lvl <= playerLevel;
                const isNext = lvl.lvl === playerLevel + 1;

                return (
                  <div
                    key={lvl.lvl}
                    className={`p-3 rounded-lg border text-left flex flex-col justify-between ${
                      isCurrent
                        ? 'bg-emerald-950/40 border-emerald-500 shadow-md ring-1 ring-emerald-500'
                        : isUnlocked
                        ? 'bg-slate-900 border-slate-700'
                        : isNext
                        ? 'bg-amber-950/30 border-amber-600/60'
                        : 'bg-slate-900/40 border-slate-800 opacity-60'
                    }`}
                  >
                    <div>
                      <div className="flex items-center justify-between mb-1.5">
                        <span className="font-mc text-xs font-bold text-white">
                          Nivel {lvl.lvl}: {lvl.name}
                        </span>
                        {isCurrent && (
                          <span className="text-[10px] bg-emerald-700 text-white px-1.5 py-0.2 rounded font-bold">
                            ACTIVO
                          </span>
                        )}
                      </div>

                      <div className="text-[10px] font-mc text-amber-300 bg-black/40 px-1.5 py-0.5 rounded border border-white/5 mb-2">
                        Prefijo: {lvl.subprefix}
                      </div>

                      <p className="text-[11px] text-slate-300 leading-snug mb-2">
                        {lvl.perks}
                      </p>
                    </div>

                    <div className="text-[10px] text-slate-400 border-t border-slate-800 pt-1.5">
                      Req: <span className="text-yellow-300">{lvl.req}</span>
                    </div>
                  </div>
                );
              })}
            </div>

            {/* Ascension Action Button */}
            <div className="flex items-center justify-between pt-4 border-t border-slate-800">
              <div className="text-xs text-slate-400">
                {playerLevel < 6 ? (
                  <span>
                    Siguiente Nivel: <strong className="text-yellow-400">${currentRaceObj.levels[playerLevel].cost.toLocaleString()}</strong>
                  </span>
                ) : (
                  <span className="text-emerald-400 font-bold">¡Has alcanzado la cúspide racial!</span>
                )}
              </div>

              {playerLevel < 6 ? (
                <button
                  onClick={handleLevelUp}
                  className="px-5 py-2.5 bg-emerald-600 hover:bg-emerald-500 text-white font-mc font-bold rounded-lg shadow-lg flex items-center gap-2 text-xs transition-all active:scale-95"
                >
                  <Sparkles className="w-4 h-4" />
                  SUBIR DE NIVEL (/raza subirnivel)
                </button>
              ) : (
                <div className="px-4 py-2 bg-slate-800 text-slate-400 rounded text-xs font-mc">
                  Nivel Máximo Dominado
                </div>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

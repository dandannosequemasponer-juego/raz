import React, { useState } from 'react';
import { INITIAL_RACES_DATA } from '../data/pluginCodeData';
import { MinecraftItemIcon } from './MinecraftItemIcon';
import { Shield, Sparkles, Sliders, Move, DollarSign, Power, RefreshCw, Key } from 'lucide-react';

interface AdminSlotEditorProps {
  racesSlots: Record<string, number>;
  setRacesSlots: React.Dispatch<React.SetStateAction<Record<string, number>>>;
  globalChangesEnabled: boolean;
  setGlobalChangesEnabled: (val: boolean) => void;
  vaultChangePrice: number;
  setVaultChangePrice: (price: number) => void;
  onLogMessage: (msg: string) => void;
}

export const AdminSlotEditor: React.FC<AdminSlotEditorProps> = ({
  racesSlots,
  setRacesSlots,
  globalChangesEnabled,
  setGlobalChangesEnabled,
  vaultChangePrice,
  setVaultChangePrice,
  onLogMessage
}) => {
  const [selectedRaceId, setSelectedRaceId] = useState<string>('werewolf');
  const [draggedRaceId, setDraggedRaceId] = useState<string | null>(null);
  const [racesState, setRacesState] = useState(INITIAL_RACES_DATA);

  const selectedRace = racesState.find(r => r.id === selectedRaceId) || racesState[0];

  const handleDragStart = (e: React.DragEvent, raceId: string) => {
    setDraggedRaceId(raceId);
    e.dataTransfer.setData('text/plain', raceId);
  };

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault();
  };

  const handleDrop = (e: React.DragEvent, targetSlot: number) => {
    e.preventDefault();
    if (!draggedRaceId) return;

    // Check if another race occupies this slot
    const currentOccupant = Object.entries(racesSlots).find(([_, slot]) => slot === targetSlot);

    setRacesSlots(prev => {
      const updated = { ...prev };
      const oldSlot = prev[draggedRaceId];

      if (currentOccupant) {
        // Swap slots
        updated[currentOccupant[0]] = oldSlot;
      }

      updated[draggedRaceId] = targetSlot;
      return updated;
    });

    onLogMessage(`§c§l[ADMIN] §7Slot de §e${draggedRaceId} §7reasignado dinámicamente a casilla §a${targetSlot} §7(actualizado en races.yml).`);
    setDraggedRaceId(null);
  };

  const adjustScale = (delta: number) => {
    const newScale = Math.max(0.2, Math.min(2.5, Math.round((selectedRace.scale + delta) * 100) / 100));
    setRacesState(prev => prev.map(r => r.id === selectedRaceId ? { ...r, scale: newScale } : r));
    onLogMessage(`§c§l[ADMIN] §7Escala física (Attribute.GENERIC_SCALE) de §e${selectedRace.name} §7ajustada a §a${newScale.toFixed(2)}x`);
  };

  const adjustHealth = (delta: number) => {
    setRacesState(prev => prev.map(r => r.id === selectedRaceId ? { ...r, baseHealth: Math.max(2, r.baseHealth + delta) } : r));
    onLogMessage(`§c§l[ADMIN] §7Vida base de §e${selectedRace.name} §7actualizada.`);
  };

  const toggleImmunity = (imm: string) => {
    setRacesState(prev => prev.map(r => {
      if (r.id !== selectedRaceId) return r;
      const has = r.immunities.includes(imm);
      return {
        ...r,
        immunities: has ? r.immunities.filter(i => i !== imm) : [...r.immunities, imm]
      };
    }));
    onLogMessage(`§c§l[ADMIN] §7Inmunidad §e${imm} §7conmutada para §a${selectedRace.name}`);
  };

  return (
    <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 w-full max-w-6xl">
      {/* LEFT: 45-Slot Drag & Drop Admin Grid */}
      <div className="lg:col-span-7 flex flex-col items-center">
        <div className="w-full bg-slate-900/90 border border-slate-800 rounded-xl p-4 shadow-xl">
          <div className="flex items-center justify-between pb-3 border-b border-slate-800 mb-4">
            <div className="flex items-center gap-2">
              <Move className="w-4 h-4 text-cyan-400" />
              <h3 className="font-mc font-bold text-sm text-white">
                Editor Posicional de Slots (0 a 44)
              </h3>
            </div>
            <span className="text-xs text-slate-400">
              Arrastra o haz clic para seleccionar
            </span>
          </div>

          {/* 45 Slot Interactive Container */}
          <div className="grid grid-cols-9 gap-1.5 bg-[#1b1e26] p-3 rounded-lg border border-slate-700">
            {Array.from({ length: 45 }).map((_, slotIdx) => {
              const raceId = Object.keys(racesSlots).find(id => racesSlots[id] === slotIdx);
              const race = racesState.find(r => r.id === raceId);
              const isSelected = race?.id === selectedRaceId;

              return (
                <div
                  key={slotIdx}
                  onDragOver={handleDragOver}
                  onDrop={(e) => handleDrop(e, slotIdx)}
                  onClick={() => race && setSelectedRaceId(race.id)}
                  className={`relative w-11 h-11 md:w-12 md:h-12 flex items-center justify-center rounded transition-all cursor-pointer ${
                    isSelected
                      ? 'bg-amber-500/20 ring-2 ring-amber-400'
                      : race
                      ? 'bg-slate-800/80 hover:bg-slate-700 border border-slate-600'
                      : 'bg-black/40 hover:bg-white/5 border border-white/5'
                  }`}
                  draggable={!!race}
                  onDragStart={(e) => race && handleDragStart(e, race.id)}
                >
                  <span className="absolute top-0.5 left-0.5 text-[8px] font-mc text-slate-500 pointer-events-none">
                    {slotIdx}
                  </span>

                  {race ? (
                    <div className="flex flex-col items-center justify-center">
                      <MinecraftItemIcon material={race.material} size={28} />
                      <span className="text-[8px] font-mc text-slate-300 truncate max-w-[40px] text-center leading-none mt-0.5">
                        {race.name.split(' ')[0]}
                      </span>
                    </div>
                  ) : (
                    <span className="text-[10px] text-slate-600 font-mono">·</span>
                  )}
                </div>
              );
            })}
          </div>

          <p className="mt-3 text-xs text-slate-400 text-center">
            💡 Los cambios se persisten inmediatamente en <code className="text-cyan-300">races.yml</code> sin necesidad de reiniciar el servidor.
          </p>
        </div>
      </div>

      {/* RIGHT: Selected Race Parameters Submenu & Global Switches */}
      <div className="lg:col-span-5 space-y-4">
        {/* Global Pause Switch */}
        <div className="bg-slate-900/90 border border-slate-800 rounded-xl p-4 shadow-xl">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <Power className={`w-4 h-4 ${globalChangesEnabled ? 'text-emerald-400' : 'text-red-400'}`} />
              <span className="font-mc text-xs font-bold text-white">
                Sistema Global de Cambios
              </span>
            </div>
            <button
              onClick={() => {
                const next = !globalChangesEnabled;
                setGlobalChangesEnabled(next);
                onLogMessage(`§c§l[ADMIN] §7Pausa global: ${next ? '§aREANUDADO (/raza admin cambios iniciar)' : '§cPAUSADO (/raza admin cambios parar)'}`);
              }}
              className={`px-3 py-1.5 rounded-lg font-mc text-xs font-bold transition-all ${
                globalChangesEnabled
                  ? 'bg-emerald-600 hover:bg-emerald-500 text-white'
                  : 'bg-red-600 hover:bg-red-500 text-white animate-pulse'
              }`}
            >
              {globalChangesEnabled ? 'HABILITADO' : 'PAUSADO'}
            </button>
          </div>
        </div>

        {/* Selected Race Live Parameter Submenu */}
        <div className="bg-slate-900/90 border border-slate-800 rounded-xl p-5 shadow-xl space-y-4">
          <div className="flex items-center justify-between pb-3 border-b border-slate-800">
            <div className="flex items-center gap-3">
              <div className="p-2 bg-slate-800 rounded-lg border border-slate-700">
                <MinecraftItemIcon material={selectedRace.material} size={32} />
              </div>
              <div>
                <h4 className="font-mc text-sm font-bold text-amber-300">
                  {selectedRace.name}
                </h4>
                <p className="text-[11px] text-slate-400">
                  Slot: <span className="text-cyan-300 font-bold">{racesSlots[selectedRace.id]}</span>
                </p>
              </div>
            </div>

            <span className="text-[11px] font-mc text-slate-400 bg-slate-800 px-2 py-1 rounded">
              ID: {selectedRace.id}
            </span>
          </div>

          {/* Scale Tuning (+/- 0.05 live!) */}
          <div>
            <div className="flex justify-between items-center mb-1 text-xs">
              <span className="text-slate-300 font-medium">Escala Física & Hitbox:</span>
              <span className="font-mc font-bold text-cyan-400">{selectedRace.scale.toFixed(2)}x</span>
            </div>
            <div className="flex items-center gap-2">
              <button
                onClick={() => adjustScale(-0.05)}
                className="px-3 py-1.5 bg-red-950/80 hover:bg-red-900 border border-red-700 text-red-200 rounded font-mc text-xs font-bold"
              >
                [-0.05]
              </button>
              <div className="flex-1 bg-slate-800 h-2.5 rounded-full overflow-hidden">
                <div
                  className="bg-cyan-500 h-full transition-all"
                  style={{ width: `${(selectedRace.scale / 2.0) * 100}%` }}
                />
              </div>
              <button
                onClick={() => adjustScale(0.05)}
                className="px-3 py-1.5 bg-emerald-950/80 hover:bg-emerald-900 border border-emerald-700 text-emerald-200 rounded font-mc text-xs font-bold"
              >
                [+0.05]
              </button>
            </div>
          </div>

          {/* Health Hearts */}
          <div>
            <div className="flex justify-between items-center mb-1 text-xs">
              <span className="text-slate-300 font-medium">Vida Máxima Base:</span>
              <span className="font-mc font-bold text-red-400">{selectedRace.baseHealth * 2} HP ({selectedRace.baseHealth} corazones)</span>
            </div>
            <div className="flex items-center gap-2">
              <button
                onClick={() => adjustHealth(-1)}
                className="px-3 py-1 bg-slate-800 hover:bg-slate-700 border border-slate-700 text-slate-200 rounded text-xs font-mc"
              >
                -1 Corazón
              </button>
              <button
                onClick={() => adjustHealth(1)}
                className="px-3 py-1 bg-slate-800 hover:bg-slate-700 border border-slate-700 text-slate-200 rounded text-xs font-mc"
              >
                +1 Corazón
              </button>
            </div>
          </div>

          {/* Immunities Toggles */}
          <div>
            <span className="block text-xs text-slate-300 font-medium mb-1.5">Inmunidades Raciales:</span>
            <div className="flex flex-wrap gap-2">
              {['fire', 'poison', 'fall_damage', 'drowning', 'knockback'].map(imm => {
                const active = selectedRace.immunities.includes(imm);
                return (
                  <button
                    key={imm}
                    onClick={() => toggleImmunity(imm)}
                    className={`px-2.5 py-1 rounded text-xs font-mc font-medium border transition-all ${
                      active
                        ? 'bg-blue-600/80 border-blue-400 text-white'
                        : 'bg-slate-800 border-slate-700 text-slate-400 hover:text-white'
                    }`}
                  >
                    {imm === 'fall_damage' ? 'Caída' : imm.charAt(0).toUpperCase() + imm.slice(1)}: {active ? 'ON' : 'OFF'}
                  </button>
                );
              })}
            </div>
          </div>
        </div>

        {/* Vault Price & Token Admin Panel */}
        <div className="bg-slate-900/90 border border-slate-800 rounded-xl p-4 shadow-xl space-y-3">
          <div className="flex items-center gap-2 text-xs font-bold font-mc text-amber-400">
            <DollarSign className="w-4 h-4" />
            <span>Ajuste de Precio de Vault para Cambio</span>
          </div>

          <div className="flex items-center justify-between text-xs">
            <span className="text-slate-400">Precio actual por cambio:</span>
            <span className="font-mc font-bold text-yellow-300">${vaultChangePrice.toLocaleString()}</span>
          </div>

          <div className="flex items-center gap-2">
            <button
              onClick={() => {
                setVaultChangePrice(Math.max(0, vaultChangePrice - 5000));
                onLogMessage(`§c§l[ADMIN] §7Precio de cambio Vault ajustado a $${(vaultChangePrice - 5000).toLocaleString()}`);
              }}
              className="flex-1 py-1 bg-slate-800 hover:bg-slate-700 rounded border border-slate-700 text-xs font-mc text-slate-200"
            >
              -$5,000
            </button>
            <button
              onClick={() => {
                setVaultChangePrice(vaultChangePrice + 5000);
                onLogMessage(`§c§l[ADMIN] §7Precio de cambio Vault ajustado a $${(vaultChangePrice + 5000).toLocaleString()}`);
              }}
              className="flex-1 py-1 bg-slate-800 hover:bg-slate-700 rounded border border-slate-700 text-xs font-mc text-slate-200"
            >
              +$5,000
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

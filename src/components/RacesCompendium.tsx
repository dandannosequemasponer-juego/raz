import React, { useState } from 'react';
import { INITIAL_RACES_DATA } from '../data/pluginCodeData';
import { MinecraftItemIcon } from './MinecraftItemIcon';
import { Shield, Swords, Sparkles, AlertTriangle, Search, Heart, Zap, Crosshair } from 'lucide-react';

export const RacesCompendium: React.FC = () => {
  const [searchTerm, setSearchTerm] = useState('');
  const [scaleFilter, setScaleFilter] = useState<'all' | 'small' | 'standard' | 'large'>('all');

  const filteredRaces = INITIAL_RACES_DATA.filter(r => {
    const matchesSearch = r.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
                          r.description.toLowerCase().includes(searchTerm.toLowerCase()) ||
                          r.id.toLowerCase().includes(searchTerm.toLowerCase());

    const matchesScale =
      scaleFilter === 'all' ? true :
      scaleFilter === 'small' ? r.scale < 1.0 :
      scaleFilter === 'standard' ? r.scale === 1.0 :
      r.scale > 1.0;

    return matchesSearch && matchesScale;
  });

  return (
    <div className="w-full max-w-6xl space-y-8">
      {/* Search & Scale Filter Toolbar */}
      <div className="bg-slate-900 border border-slate-800 rounded-xl p-4 shadow-xl flex flex-wrap items-center justify-between gap-4">
        <div className="relative flex-1 min-w-[260px]">
          <Search className="w-4 h-4 absolute left-3 top-1/2 -translate-y-1/2 text-slate-500" />
          <input
            type="text"
            placeholder="Buscar raza por nombre, ventajas o desventajas..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="w-full bg-slate-950 border border-slate-700 rounded-lg pl-9 pr-3 py-2 text-xs font-mc text-white focus:outline-none focus:border-cyan-500 placeholder:text-slate-600"
          />
        </div>

        <div className="flex items-center gap-1.5 font-mc text-xs">
          <span className="text-slate-400 mr-1 text-[11px]">Escala Física:</span>
          {(['all', 'small', 'standard', 'large'] as const).map(f => (
            <button
              key={f}
              onClick={() => setScaleFilter(f)}
              className={`px-3 py-1.5 rounded-lg border transition-all ${
                scaleFilter === f
                  ? 'bg-emerald-950 border-emerald-600 text-emerald-300 font-bold'
                  : 'bg-slate-800/80 border-slate-700 text-slate-400 hover:text-white'
              }`}
            >
              {f === 'all' ? 'Todas (16)' :
               f === 'small' ? 'Pequeñas (<1.0x)' :
               f === 'standard' ? 'Estándar (1.0x)' : 'Gigantes (>1.0x)'}
            </button>
          ))}
        </div>
      </div>

      {/* 16 Races Grid Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
        {filteredRaces.map((race) => (
          <div
            key={race.id}
            className="bg-slate-900/90 border border-slate-800 rounded-xl p-5 shadow-xl flex flex-col justify-between hover:border-slate-700 transition-all"
          >
            <div>
              {/* Header */}
              <div className="flex items-start justify-between gap-3 mb-3">
                <div className="flex items-center gap-3">
                  <div className="p-2.5 bg-slate-950 rounded-xl border border-slate-800 shadow-inner">
                    <MinecraftItemIcon material={race.material} size={36} glow />
                  </div>
                  <div>
                    <h4 className="font-mc text-base font-bold text-white flex items-center gap-2">
                      <span>{race.name}</span>
                      <span className="text-xs text-slate-500 font-normal">({race.id})</span>
                    </h4>
                    <p className="text-xs text-slate-400 mt-0.5 line-clamp-2">
                      {race.description}
                    </p>
                  </div>
                </div>

                <div className="text-right shrink-0 flex flex-col items-end gap-1">
                  <span className="inline-block px-2 py-0.5 bg-amber-950/80 border border-amber-700 text-amber-300 font-mc text-xs font-bold rounded">
                    Slot {race.slot}
                  </span>
                  <span className="inline-block px-2 py-0.5 bg-cyan-950/80 border border-cyan-700 text-cyan-300 font-mc text-xs font-bold rounded">
                    {race.scale.toFixed(2)}x Escala
                  </span>
                </div>
              </div>

              {/* Combat & Physical Attributes */}
              <div className="grid grid-cols-3 gap-2 bg-slate-950/80 p-2.5 rounded-lg border border-slate-800/80 mb-3 text-center">
                <div className="flex items-center justify-center gap-1.5 text-xs">
                  <Heart className="w-3.5 h-3.5 text-red-400" />
                  <span className="font-mc text-slate-300 font-medium">{race.baseHealth * 2} HP</span>
                </div>
                <div className="flex items-center justify-center gap-1.5 text-xs">
                  <Zap className="w-3.5 h-3.5 text-cyan-400" />
                  <span className="font-mc text-slate-300 font-medium">{Math.round(race.speed * 100)}% Vel</span>
                </div>
                <div className="flex items-center justify-center gap-1.5 text-xs">
                  <Swords className="w-3.5 h-3.5 text-amber-400" />
                  <span className="font-mc text-slate-300 font-medium">{Math.round(race.damage * 100)}% Daño</span>
                </div>
              </div>

              {/* Immunities & Vulnerabilities */}
              <div className="flex flex-wrap gap-1.5 mb-3 text-[11px] font-mc">
                {race.immunities.map(imm => (
                  <span key={imm} className="px-2 py-0.5 bg-blue-950/60 border border-blue-800 text-blue-300 rounded">
                    🛡 Inmune: {imm}
                  </span>
                ))}
                {race.vulnerabilities.map(vuln => (
                  <span key={vuln} className="px-2 py-0.5 bg-red-950/50 border border-red-800 text-red-300 rounded">
                    ⚠ Debilidad: {vuln}
                  </span>
                ))}
              </div>

              {/* 6 Levels Progression Summary */}
              <div className="space-y-1.5 border-t border-slate-800/80 pt-3">
                <span className="block text-[11px] font-mc font-bold text-slate-400 uppercase tracking-wider">
                  Progresión de 6 Niveles (Subprefijos LuckPerms)
                </span>
                <div className="grid grid-cols-2 sm:grid-cols-3 gap-1.5 text-[10px] font-mc">
                  {race.levels.map(l => (
                    <div key={l.lvl} className="p-1.5 bg-slate-950/60 rounded border border-slate-800 text-left">
                      <div className="font-bold text-amber-400">
                        L{l.lvl} {l.name}
                      </div>
                      <div className="text-slate-400 text-[9px] truncate" title={l.subprefix}>
                        {l.subprefix}
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          </div>
        ))}
      </div>

      {/* Central PvP Counterplay Matrix (Documentación Técnica de Razas v7.0) */}
      <div className="bg-slate-900/90 border border-slate-800 rounded-xl p-6 shadow-2xl">
        <div className="flex items-center gap-2 mb-4 pb-3 border-b border-slate-800">
          <Swords className="w-5 h-5 text-red-400" />
          <h3 className="font-mc text-base font-bold text-white">
            Matriz Central de Contrajuego y Balance PvP (CYTRICOS RPG)
          </h3>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-xs font-mc text-slate-300">
          <div className="p-4 bg-slate-950/80 rounded-lg border border-slate-800 space-y-1.5">
            <h5 className="font-bold text-amber-400 flex items-center gap-1.5">
              <span>Vampiros vs. Celestiales</span>
            </h5>
            <p className="text-slate-300">
              Los <strong>Celestiales</strong> causan un <strong>+30% de daño extra</strong> a Vampiros y No-Muertos, pero son más vulnerables a los proyectiles y flechas lejanas (+20% daño recibido).
            </p>
          </div>

          <div className="p-4 bg-slate-950/80 rounded-lg border border-slate-800 space-y-1.5">
            <h5 className="font-bold text-amber-400 flex items-center gap-1.5">
              <span>Orcos &amp; Gigantes vs. Asesinos Ágiles</span>
            </h5>
            <p className="text-slate-300">
              Los <strong>Orcos y Gigantes</strong> rompen escudos y provocan un retroceso masivo, pero sufren ante la alta velocidad, evasión y ataques aéreos de Hadas y Elfos.
            </p>
          </div>

          <div className="p-4 bg-slate-950/80 rounded-lg border border-slate-800 space-y-1.5">
            <h5 className="font-bold text-amber-400 flex items-center gap-1.5">
              <span>Espíritu / Yokai (Intangibilidad Espectral)</span>
            </h5>
            <p className="text-slate-300">
              Su <strong>Paso Espectral de 3 segundos</strong> permite escapar de combos y atravesar estructuras, pero no pueden infligir daño ni atacar a otros mientras atraviesan bloques.
            </p>
          </div>

          <div className="p-4 bg-slate-950/80 rounded-lg border border-slate-800 space-y-1.5">
            <h5 className="font-bold text-amber-400 flex items-center gap-1.5">
              <span>Druidas Cítricos (Soporte Esencial)</span>
            </h5>
            <p className="text-slate-300">
              Clase insignia de CYTRICOS: limpian venenos del equipo, otorgan sanación continua mediante zumos cítricos y ralentizan áreas enemigas con crecimiento de espinas.
            </p>
          </div>
        </div>
      </div>
    </div>
  );
};

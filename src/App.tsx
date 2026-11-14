/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React, { useState } from 'react';
import { ChestGuiSimulator } from './components/ChestGuiSimulator';
import { AdminSlotEditor } from './components/AdminSlotEditor';
import { CommandConsole } from './components/CommandConsole';
import { CodeExplorer } from './components/CodeExplorer';
import { RacesCompendium } from './components/RacesCompendium';
import { INITIAL_RACES_DATA } from './data/pluginCodeData';
import { exportPluginZip } from './utils/zipExporter';
import { Download, Sparkles, Server, Terminal, Shield, BookOpen, Layers } from 'lucide-react';

export default function App() {
  const [activeTab, setActiveTab] = useState<'gui' | 'admin' | 'console' | 'code' | 'compendium'>('gui');
  const [downloadingZip, setDownloadingZip] = useState(false);

  // Shared RPG Player & Server State
  const [playerRace, setPlayerRace] = useState<string>('werewolf');
  const [playerLevel, setPlayerLevel] = useState<number>(1);
  const [freeChangeAvailable, setFreeChangeAvailable] = useState<boolean>(true);
  const [vaultBalance, setVaultBalance] = useState<number>(799000);
  const [hasSpecialToken, setHasSpecialToken] = useState<boolean>(true);
  const [globalChangesEnabled, setGlobalChangesEnabled] = useState<boolean>(true);

  // Dynamic slot positioning (Slot 0-44)
  const [racesSlots, setRacesSlots] = useState<Record<string, number>>(() => {
    const map: Record<string, number> = {};
    INITIAL_RACES_DATA.forEach(r => {
      map[r.id] = r.slot;
    });
    return map;
  });

  // Minecraft In-Game Chat Logs
  const [logs, setLogs] = useState<string[]>([
    "§8[§eCYTRICOS§8] §aIniciando CytricosRaces v7.0.0-RELEASE (Paper 1.20.5+ / Java 21)...",
    "§8[§eCYTRICOS§8] §7Desarrolladores Líderes: §eDandan_13 §7& §eWolfit12",
    "§8[§eCYTRICOS§8] §7Integración con §bVault Economy§7 y §bLuckPerms API 5.4§7 establecida.",
    "§8[§eCYTRICOS§8] §7Se cargaron §a16 razas§7 dinámicas con escalado físico §eAttribute.GENERIC_SCALE§7.",
    "§8[§eCYTRICOS§8] §7Escribe §e/razas§7 o §e/raza ayuda§7 para comenzar."
  ]);

  const addLogMessage = (msg: string) => {
    setLogs(prev => [...prev, msg]);
  };

  const handleCommandExecution = (cmdStr: string) => {
    const trimmed = cmdStr.trim();
    addLogMessage(`§7> §f${trimmed}`);

    const parts = trimmed.replace('/', '').split(' ');
    const main = parts[0]?.toLowerCase();
    const sub = parts[1]?.toLowerCase();

    if (main === 'razas' || (main === 'raza' && (sub === 'gui' || sub === 'menu' || !sub))) {
      setActiveTab('gui');
      addLogMessage("§8[§eCYTRICOS§8] §7Abriendo menú de selección de razas de 45 slots...");
      return;
    }

    if (main === 'raza') {
      switch (sub) {
        case 'estadisticas':
        case 'stats': {
          const race = INITIAL_RACES_DATA.find(r => r.id === playerRace);
          addLogMessage("§8====== [ §eESTADÍSTICAS RACIALES §8] ======");
          addLogMessage(`§eJugador: §fDandan_13`);
          addLogMessage(`§eRaza Activa: §a${race?.name || playerRace} §7(Nivel §6${playerLevel}§7)`);
          addLogMessage(`§eEscala Física: §b${race?.scale || 1.0}x`);
          addLogMessage(`§eVida Máxima: §c${(race?.baseHealth || 10) * 2} HP`);
          addLogMessage(`§eCambio Gratuito Único: ${freeChangeAvailable ? '§aDisponible' : '§cConsumido'}`);
          addLogMessage(`§eDinero en Vault: §e$${vaultBalance.toLocaleString()}`);
          addLogMessage("§8=======================================");
          break;
        }

        case 'subirnivel': {
          const race = INITIAL_RACES_DATA.find(r => r.id === playerRace);
          if (playerLevel >= 6) {
            addLogMessage("§e§l[CYTRICOS] §7Ya te encuentras en el nivel máximo legendario.");
          } else if (race) {
            const nextLvl = race.levels[playerLevel];
            if (vaultBalance >= nextLvl.cost) {
              setVaultBalance(vaultBalance - nextLvl.cost);
              setPlayerLevel(playerLevel + 1);
              addLogMessage(`§a§l[CYTRICOS] §7¡Has ascendido al Nivel ${playerLevel + 1} (${nextLvl.name}) de ${race.name}!`);
            } else {
              addLogMessage(`§c§l[CYTRICOS] §7No cumples los requisitos de dinero ($${nextLvl.cost.toLocaleString()}).`);
            }
          }
          break;
        }

        case 'info': {
          const raceId = parts[2]?.toLowerCase();
          const target = INITIAL_RACES_DATA.find(r => r.id === raceId || r.name.toLowerCase().includes(raceId));
          if (target) {
            addLogMessage(`§8====== [ §6RAZA: ${target.name.toUpperCase()} §8] ======`);
            addLogMessage(`§7${target.description}`);
            addLogMessage(`§7Escala: §e${target.scale}x §7| Vida: §c${target.baseHealth * 2} HP §7| Vel: §b${Math.round(target.speed * 100)}%`);
            addLogMessage(`§7Subprefijo Nivel 1: §8[§7${target.levels[0].name}§8] §7-> Nivel 6: §e${target.levels[5].subprefix}`);
          } else {
            addLogMessage("§cRaza no encontrada. Razas disponibles: werewolf, vampire, elf, orc, goblin, giant, dwarf, druid...");
          }
          break;
        }

        case 'cambiarraza': {
          const raceId = parts[2]?.toLowerCase();
          const target = INITIAL_RACES_DATA.find(r => r.id === raceId || r.name.toLowerCase().includes(raceId));
          if (!target) {
            addLogMessage("§cUso: /raza cambiarraza <raza> [gratis|dinero|objeto]");
            return;
          }
          if (target.id === playerRace) {
            addLogMessage("§cYa perteneces a esta raza.");
            return;
          }
          if (!globalChangesEnabled) {
            addLogMessage("§c§l[CYTRICOS] §7Los cambios de raza están pausados actualmente por la administración.");
            return;
          }

          if (freeChangeAvailable) {
            setFreeChangeAvailable(false);
            setPlayerRace(target.id);
            setPlayerLevel(1);
            addLogMessage("§a§l[CYTRICOS] §7¡Has utilizado tu cambio de raza gratuito único!");
            addLogMessage(`§e§l[CYTRICOS] §7Tu raza ha sido cambiada a: §a${target.name.toUpperCase()} (Nivel 1)`);
          } else if (hasSpecialToken) {
            setHasSpecialToken(false);
            setPlayerRace(target.id);
            setPlayerLevel(1);
            addLogMessage("§a§l[CYTRICOS] §7Has entregado tu §6Esencia de Reencarnación Ancestral §7y cambiado de raza.");
            addLogMessage(`§e§l[CYTRICOS] §7Tu raza ha sido cambiada a: §a${target.name.toUpperCase()} (Nivel 1)`);
          } else if (vaultBalance >= 50000) {
            setVaultBalance(vaultBalance - 50000);
            setPlayerRace(target.id);
            setPlayerLevel(1);
            addLogMessage("§a§l[CYTRICOS] §7Cambio de raza realizado con éxito por §e$50,000§7.");
            addLogMessage(`§e§l[CYTRICOS] §7Tu raza ha sido cambiada a: §a${target.name.toUpperCase()} (Nivel 1)`);
          } else {
            addLogMessage("§c§l[CYTRICOS] §7No posees fondos suficientes ni el ítem de reencarnación.");
          }
          break;
        }

        case 'estado': {
          addLogMessage("§8====== [ §eESTADO DE CAMBIO DE RAZA §8] ======");
          addLogMessage(`§eSistema Global: ${globalChangesEnabled ? '§aActivo' : '§cPausado'}`);
          addLogMessage(`§eCambio Gratuito Único: ${freeChangeAvailable ? '§aDisponible' : '§cConsumido'}`);
          addLogMessage(`§ePrecio en Vault: §6$50,000`);
          addLogMessage(`§eEsencia en Mochila: ${hasSpecialToken ? '§aEn posesión' : '§cNo'}`);
          addLogMessage("§8==========================================");
          break;
        }

        case 'ayuda': {
          addLogMessage("§8================ [ §eAYUDA DE RAZAS §8] ================");
          addLogMessage("§e/razas §7- Abre el menú interactivo de selección.");
          addLogMessage("§e/raza confirmar §7- Abre confirmación con lana verde y roja.");
          addLogMessage("§e/raza info [raza] §7- Muestra detalles y 6 niveles.");
          addLogMessage("§e/raza estadisticas §7- Consulta tus atributos raciales.");
          addLogMessage("§e/raza subirnivel §7- Asciende de nivel en tu raza actual.");
          addLogMessage("§e/raza cambiarraza <raza> §7- Solicita cambio de raza.");
          addLogMessage("§e/raza estado §7- Muestra disponibilidad de cambio.");
          addLogMessage("§c/raza admin ayuda §7- Compendio de administración.");
          addLogMessage("§8===================================================");
          break;
        }

        case 'admin': {
          const action = parts[2]?.toLowerCase();
          if (action === 'cambios') {
            const mode = parts[3]?.toLowerCase();
            if (mode === 'parar') {
              setGlobalChangesEnabled(false);
              addLogMessage("§c§l[CYTRICOS] §7El sistema de cambios de raza ha sido §cPAUSADO§7 globalmente.");
            } else if (mode === 'iniciar') {
              setGlobalChangesEnabled(true);
              addLogMessage("§a§l[CYTRICOS] §7El sistema de cambios de raza ha sido §aACTIVADO§7 globalmente.");
            } else {
              addLogMessage("§cUso: /raza admin cambios parar | iniciar");
            }
          } else if (action === 'menu' || action === 'gui') {
            setActiveTab('admin');
            addLogMessage("§c§l[ADMIN] §7Abriendo panel de administración y reubicación de slots...");
          } else if (action === 'recargar') {
            addLogMessage("§a§l[CYTRICOS] §7Configuraciones, races.yml y cachés recargados.");
          } else if (action === 'darobjetocambio') {
            setHasSpecialToken(true);
            addLogMessage("§a§l[ADMIN] §7Entregada Esencia de Reencarnación Ancestral a Dandan_13.");
          } else {
            addLogMessage("§8==================== [ §cAYUDA ADMIN §8] ====================");
            addLogMessage("§e/raza admin cambios parar|iniciar §7- Pausa o reanuda cambios.");
            addLogMessage("§e/raza admin menu §7- Editor de slots (0-44) por Drag & Drop.");
            addLogMessage("§e/raza admin editar <raza> §7- Variables globales y escala.");
            addLogMessage("§e/raza admin darobjetocambio <jugador> §7- Da ítem especial.");
            addLogMessage("§e/raza admin recargar §7- Recarga en vivo.");
            addLogMessage("§8============================================================");
          }
          break;
        }

        default:
          addLogMessage("§cComando desconocido. Escribe /raza ayuda.");
      }
    } else {
      addLogMessage("§cComando no reconocido. Prueba /razas");
    }
  };

  const handleDownloadZip = async () => {
    try {
      setDownloadingZip(true);
      const zipBlob = await exportPluginZip();
      const url = URL.createObjectURL(zipBlob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `CytricosRaces-v7.0.0-SourceCode.zip`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      URL.revokeObjectURL(url);
    } catch (err) {
      console.error(err);
    } finally {
      setDownloadingZip(false);
    }
  };

  return (
    <div className="min-h-screen bg-[#0c0e12] text-slate-100 flex flex-col font-sans">
      {/* 1. TOP BAR CONTRACT (Single-line Brand - 4/5 Nav Links - 1 Primary Action) */}
      <header className="sticky top-0 z-40 bg-[#0f1218]/95 backdrop-blur border-b border-slate-800 px-6 py-3 flex items-center justify-between gap-8">
        {/* Zone 1: Brand Wordmark */}
        <div className="flex items-center gap-3 shrink-0">
          <div className="w-8 h-8 rounded-lg bg-emerald-600/20 border border-emerald-500/40 flex items-center justify-center">
            <Shield className="w-4 h-4 text-emerald-400" />
          </div>
          <span className="font-mc text-sm font-bold tracking-tight text-white whitespace-nowrap">
            CYTRICOS RPG <span className="text-emerald-400">· CytricosRaces v7.0</span>
          </span>
        </div>

        {/* Zone 2: Navigation Links */}
        <nav className="hidden md:flex items-center gap-6 text-xs font-mc text-slate-300">
          <button
            onClick={() => setActiveTab('gui')}
            className={`transition-colors whitespace-nowrap hover:text-white pb-0.5 ${
              activeTab === 'gui' ? 'text-emerald-400 border-b-2 border-emerald-400 font-bold' : ''
            }`}
          >
            Simulador GUI
          </button>
          <button
            onClick={() => setActiveTab('admin')}
            className={`transition-colors whitespace-nowrap hover:text-white pb-0.5 ${
              activeTab === 'admin' ? 'text-amber-400 border-b-2 border-amber-400 font-bold' : ''
            }`}
          >
            Editor Admin
          </button>
          <button
            onClick={() => setActiveTab('console')}
            className={`transition-colors whitespace-nowrap hover:text-white pb-0.5 ${
              activeTab === 'console' ? 'text-cyan-400 border-b-2 border-cyan-400 font-bold' : ''
            }`}
          >
            Consola
          </button>
          <button
            onClick={() => setActiveTab('code')}
            className={`transition-colors whitespace-nowrap hover:text-white pb-0.5 ${
              activeTab === 'code' ? 'text-purple-400 border-b-2 border-purple-400 font-bold' : ''
            }`}
          >
            Código Fuente
          </button>
          <button
            onClick={() => setActiveTab('compendium')}
            className={`transition-colors whitespace-nowrap hover:text-white pb-0.5 ${
              activeTab === 'compendium' ? 'text-red-400 border-b-2 border-red-400 font-bold' : ''
            }`}
          >
            Compendio &amp; PvP
          </button>
        </nav>

        {/* Zone 3: 1 Primary Action */}
        <div className="flex items-center gap-3 shrink-0">
          <button
            onClick={handleDownloadZip}
            disabled={downloadingZip}
            className="px-4 py-2 text-xs font-mc font-bold text-white bg-emerald-600 hover:bg-emerald-500 rounded-lg shadow-lg flex items-center gap-2 transition-all active:scale-95 whitespace-nowrap"
          >
            <Download className="w-3.5 h-3.5" />
            <span>{downloadingZip ? 'Generando...' : 'Descargar Plugin ZIP'}</span>
          </button>
        </div>
      </header>

      {/* 2. MAIN APPLICATION CONTENT */}
      <main className="flex-1 max-w-7xl w-full mx-auto p-4 sm:p-6 lg:p-8 flex flex-col items-center">
        {/* Hero Section & Server Badge */}
        <div className="w-full text-center mb-8">
          <div className="inline-flex items-center gap-2 px-3 py-1 bg-slate-900 border border-slate-800 rounded-full text-xs font-mc text-slate-400 mb-3">
            <span className="w-2 h-2 rounded-full bg-emerald-400 animate-ping" />
            <span>Paper 26.2 · Java 25 · Attribute.GENERIC_SCALE · Resource Pack 3D</span>
            <span className="text-slate-600">·</span>
            <span className="text-emerald-400">play.cytricos.com</span>
          </div>

          <h1 className="text-2xl sm:text-4xl font-bold tracking-tight text-white mb-2 font-mc">
            CytricosRaces <span className="text-emerald-400">Plugin Suite</span>
          </h1>

          <p className="text-sm text-slate-400 max-w-2xl mx-auto leading-relaxed">
            Código fuente completo y entorno interactivo para el sistema de 16 razas de CYTRICOS RPG.
            Desarrollado para <strong>Dandan_13</strong> y <strong>Wolfit12</strong> con escalado de hitbox real y subprefijos LuckPerms.
          </p>
        </div>

        {/* Dynamic Tab Views */}
        {activeTab === 'gui' && (
          <div className="w-full flex flex-col items-center animate-in fade-in duration-200">
            <ChestGuiSimulator
              playerRace={playerRace}
              setPlayerRace={setPlayerRace}
              playerLevel={playerLevel}
              setPlayerLevel={setPlayerLevel}
              freeChangeAvailable={freeChangeAvailable}
              setFreeChangeAvailable={setFreeChangeAvailable}
              vaultBalance={vaultBalance}
              setVaultBalance={setVaultBalance}
              hasSpecialToken={hasSpecialToken}
              setHasSpecialToken={setHasSpecialToken}
              globalChangesEnabled={globalChangesEnabled}
              racesSlots={racesSlots}
              onLogMessage={addLogMessage}
            />
          </div>
        )}

        {activeTab === 'admin' && (
          <div className="w-full flex flex-col items-center animate-in fade-in duration-200">
            <AdminSlotEditor
              racesSlots={racesSlots}
              setRacesSlots={setRacesSlots}
              globalChangesEnabled={globalChangesEnabled}
              setGlobalChangesEnabled={setGlobalChangesEnabled}
              vaultChangePrice={50000}
              setVaultChangePrice={() => {}}
              onLogMessage={addLogMessage}
            />
          </div>
        )}

        {activeTab === 'console' && (
          <div className="w-full flex flex-col items-center animate-in fade-in duration-200">
            <CommandConsole
              logs={logs}
              onExecuteCommand={handleCommandExecution}
              onClearLogs={() => setLogs([])}
            />
          </div>
        )}

        {activeTab === 'code' && (
          <div className="w-full flex flex-col items-center animate-in fade-in duration-200">
            <CodeExplorer />
          </div>
        )}

        {activeTab === 'compendium' && (
          <div className="w-full flex flex-col items-center animate-in fade-in duration-200">
            <RacesCompendium />
          </div>
        )}
      </main>

      {/* 3. CLEAN FOOTER */}
      <footer className="bg-[#0a0c10] border-t border-slate-900 py-6 px-6 text-center text-xs font-mc text-slate-500">
        <div className="max-w-7xl mx-auto flex flex-col sm:flex-row items-center justify-between gap-3">
          <div>
            CYTRICOS RPG · Lead Developers: <span className="text-slate-300">Dandan_13</span> &amp; <span className="text-slate-300">Wolfit12</span>
          </div>
          <div>
            IP: <span className="text-emerald-400">PLAY.CYTRICOS.COM</span> · Maven Release v7.0.0
          </div>
        </div>
      </footer>
    </div>
  );
}

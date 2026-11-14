import React, { useState } from 'react';
import { Terminal, Send, Trash2, HelpCircle } from 'lucide-react';

interface CommandConsoleProps {
  logs: string[];
  onExecuteCommand: (cmd: string) => void;
  onClearLogs: () => void;
}

export const CommandConsole: React.FC<CommandConsoleProps> = ({
  logs,
  onExecuteCommand,
  onClearLogs
}) => {
  const [inputVal, setInputVal] = useState('');

  const QUICK_COMMANDS = [
    { label: "/razas", desc: "Abre GUI principal" },
    { label: "/raza estadisticas", desc: "Ver stats activas" },
    { label: "/raza subirnivel", desc: "Ascender 1 nivel" },
    { label: "/raza info werewolf", desc: "Info de Hombre Lobo" },
    { label: "/raza info vampire", desc: "Info de Vampiro" },
    { label: "/raza info giant", desc: "Info de Gigante" },
    { label: "/raza estado", desc: "Estado de tokens y costos" },
    { label: "/raza ayuda", desc: "Guía de jugador" },
    { label: "/raza admin ayuda", desc: "Compendio de administración" },
    { label: "/raza admin cambios parar", desc: "Pausar cambios globalmente" },
    { label: "/raza admin cambios iniciar", desc: "Reanudar cambios" },
    { label: "/raza admin recargar", desc: "Recargar configs en vivo" },
    { label: "/raza admin darobjetocambio Dandan_13", desc: "Entregar Esencia Ancestral" }
  ];

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!inputVal.trim()) return;
    onExecuteCommand(inputVal.trim());
    setInputVal('');
  };

  // Convert Minecraft color codes (§0-9, §a-f, §l, etc.) into styled spans
  const formatMinecraftColor = (text: string) => {
    // Split by § code
    const parts = text.split(/(§[0-9a-fk-or])/gi);
    let currentColor = 'text-[#aaaaaa]';
    let isBold = false;

    return parts.map((part, i) => {
      if (part.startsWith('§')) {
        const code = part.charAt(1).toLowerCase();
        switch (code) {
          case '0': currentColor = 'text-[#000000]'; break;
          case '1': currentColor = 'text-[#0000aa]'; break;
          case '2': currentColor = 'text-[#00aa00]'; break;
          case '3': currentColor = 'text-[#00aaaa]'; break;
          case '4': currentColor = 'text-[#aa0000]'; break;
          case '5': currentColor = 'text-[#aa00aa]'; break;
          case '6': currentColor = 'text-[#ffaa00]'; break;
          case '7': currentColor = 'text-[#aaaaaa]'; break;
          case '8': currentColor = 'text-[#555555]'; break;
          case '9': currentColor = 'text-[#5555ff]'; break;
          case 'a': currentColor = 'text-[#55ff55]'; break;
          case 'b': currentColor = 'text-[#55ffff]'; break;
          case 'c': currentColor = 'text-[#ff5555]'; break;
          case 'd': currentColor = 'text-[#ff55ff]'; break;
          case 'e': currentColor = 'text-[#ffff55]'; break;
          case 'f': currentColor = 'text-[#ffffff]'; break;
          case 'l': isBold = true; break;
          case 'r': currentColor = 'text-[#aaaaaa]'; isBold = false; break;
        }
        return null;
      }
      return (
        <span key={i} className={`${currentColor} ${isBold ? 'font-bold' : ''}`}>
          {part}
        </span>
      );
    });
  };

  return (
    <div className="w-full max-w-4xl bg-slate-900 border border-slate-800 rounded-xl shadow-2xl overflow-hidden flex flex-col h-[520px]">
      {/* Console Header */}
      <div className="bg-slate-950 px-4 py-3 border-b border-slate-800 flex items-center justify-between">
        <div className="flex items-center gap-2">
          <Terminal className="w-4 h-4 text-emerald-400" />
          <span className="font-mc text-xs font-bold text-white tracking-wide">
            Consola del Servidor · CYTRICOS RPG Terminal
          </span>
        </div>
        <div className="flex items-center gap-2">
          <button
            onClick={onClearLogs}
            className="flex items-center gap-1 px-2.5 py-1 text-[11px] font-mc text-slate-400 hover:text-white bg-slate-800 hover:bg-slate-700 rounded transition-colors"
          >
            <Trash2 className="w-3 h-3" />
            Limpiar
          </button>
        </div>
      </div>

      {/* Quick Action Chips */}
      <div className="bg-slate-950/60 p-2.5 border-b border-slate-800/80 flex flex-wrap gap-1.5 overflow-x-auto">
        {QUICK_COMMANDS.map((qc) => (
          <button
            key={qc.label}
            onClick={() => onExecuteCommand(qc.label)}
            title={qc.desc}
            className="px-2.5 py-1 rounded bg-slate-800/90 hover:bg-slate-700 border border-slate-700 text-[11px] font-mc text-cyan-300 hover:text-white transition-all whitespace-nowrap"
          >
            {qc.label}
          </button>
        ))}
      </div>

      {/* Output Terminal Stream */}
      <div className="flex-1 p-4 overflow-y-auto bg-[#0a0c10] font-mc text-xs leading-relaxed space-y-1 select-text">
        {logs.map((log, index) => (
          <div key={index} className="flex items-start gap-2">
            <span className="text-slate-600 select-none text-[10px] shrink-0 pt-0.5">
              {new Date().toLocaleTimeString()}
            </span>
            <div className="break-all whitespace-pre-wrap">
              {formatMinecraftColor(log)}
            </div>
          </div>
        ))}
      </div>

      {/* Input Field */}
      <form onSubmit={handleSubmit} className="p-3 bg-slate-950 border-t border-slate-800 flex items-center gap-2">
        <div className="relative flex-1">
          <span className="absolute left-3 top-1/2 -translate-y-1/2 font-mc text-slate-500 text-xs">
            &gt;
          </span>
          <input
            type="text"
            value={inputVal}
            onChange={(e) => setInputVal(e.target.value)}
            placeholder="Escribe un comando (/raza info elf, /raza admin ayuda, /raza subirnivel)..."
            className="w-full bg-slate-900 border border-slate-700 rounded-lg pl-7 pr-3 py-2 text-xs font-mc text-white focus:outline-none focus:border-emerald-500 placeholder:text-slate-600"
          />
        </div>
        <button
          type="submit"
          className="px-4 py-2 bg-emerald-600 hover:bg-emerald-500 text-white font-mc font-bold text-xs rounded-lg transition-colors flex items-center gap-1.5"
        >
          <Send className="w-3.5 h-3.5" />
          <span>Ejecutar</span>
        </button>
      </form>
    </div>
  );
};

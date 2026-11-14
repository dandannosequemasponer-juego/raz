import React from 'react';

interface MinecraftTooltipProps {
  title: string;
  material: string;
  componentsCount?: number;
  scale?: number;
  lore?: string[];
  isCurrentRace?: boolean;
}

export const MinecraftTooltip: React.FC<MinecraftTooltipProps> = ({
  title,
  material,
  componentsCount = 14,
  scale,
  lore = [],
  isCurrentRace = false
}) => {
  return (
    <div className="z-50 pointer-events-none min-w-[240px] max-w-[320px] bg-[#100010]/95 border-2 border-[#2b005e] shadow-2xl p-2.5 rounded text-left font-mc text-xs leading-relaxed animate-in fade-in zoom-in-95 duration-100 select-none">
      {/* Title */}
      <div className="font-bold text-base mb-1 tracking-wide flex items-center justify-between gap-2">
        <span className={isCurrentRace ? "text-emerald-400" : "text-amber-300"}>
          {title.replace(/§[0-9a-fk-or]/gi, '')}
        </span>
        {scale && (
          <span className="text-[10px] text-cyan-300 bg-[#191932] px-1.5 py-0.5 rounded border border-cyan-800">
            {scale.toFixed(2)}x
          </span>
        )}
      </div>

      {/* Minecraft Material Identifier */}
      <div className="text-[#8e8e8e] text-[11px] font-normal mb-1.5 flex items-center gap-1.5">
        <span className="text-[#55ffff]">minecraft:{material}</span>
      </div>

      {/* Lore Lines */}
      {lore.length > 0 && (
        <div className="space-y-0.5 border-t border-purple-900/40 pt-1.5 text-[#aaaaaa]">
          {lore.map((line, idx) => {
            const cleanLine = line.replace(/§[0-9a-fk-or]/gi, '');
            const isWarn = cleanLine.includes('⚠') || cleanLine.toLowerCase().includes('desventaja') || cleanLine.toLowerCase().includes('advertencia');
            const isBuff = cleanLine.includes('✔') || cleanLine.includes('+') || cleanLine.toLowerCase().includes('ventaja');
            const isAction = cleanLine.includes('▶');

            return (
              <div
                key={idx}
                className={
                  isWarn
                    ? "text-[#ff5555] font-medium"
                    : isBuff
                    ? "text-[#55ff55]"
                    : isAction
                    ? "text-[#ffff55] font-semibold pt-1"
                    : "text-[#d0d0d0]"
                }
              >
                {cleanLine}
              </div>
            );
          })}
        </div>
      )}

      {/* Component Count Indicator (Faithful to 1.20.5+ screenshots!) */}
      <div className="mt-2 text-[10px] text-[#555555] border-t border-purple-900/30 pt-1 flex justify-between items-center">
        <span>{componentsCount} componente(s)</span>
        <span className="text-[9px] text-[#00aaaa]">CYTRICOS RPG</span>
      </div>
    </div>
  );
};

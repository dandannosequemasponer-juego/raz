import React from 'react';

interface MinecraftItemIconProps {
  material: string;
  size?: number;
  className?: string;
  glow?: boolean;
}

export const MinecraftItemIcon: React.FC<MinecraftItemIconProps> = ({
  material,
  size = 32,
  className = "",
  glow = false
}) => {
  const normMat = material.toLowerCase().replace('minecraft:', '');

  const renderGraphic = () => {
    switch (normMat) {
      case 'anvil':
        return (
          <svg viewBox="0 0 32 32" className="w-full h-full drop-shadow">
            <rect x="6" y="6" width="20" height="7" fill="#4a4f56" rx="1"/>
            <rect x="8" y="7" width="16" height="3" fill="#6e757f"/>
            <rect x="12" y="13" width="8" height="8" fill="#36393e"/>
            <rect x="8" y="21" width="16" height="6" fill="#4a4f56" rx="1"/>
            <rect x="9" y="22" width="14" height="2" fill="#6e757f"/>
          </svg>
        );

      case 'player_head':
        return (
          <svg viewBox="0 0 32 32" className="w-full h-full drop-shadow">
            <rect x="6" y="6" width="20" height="20" fill="#b98561" rx="2"/>
            {/* Hair */}
            <rect x="6" y="6" width="20" height="6" fill="#4a301e"/>
            <rect x="6" y="12" width="4" height="4" fill="#4a301e"/>
            <rect x="22" y="12" width="4" height="4" fill="#4a301e"/>
            {/* Eyes */}
            <rect x="9" y="15" width="4" height="2" fill="#ffffff"/>
            <rect x="11" y="15" width="2" height="2" fill="#3b5998"/>
            <rect x="19" y="15" width="4" height="2" fill="#ffffff"/>
            <rect x="19" y="15" width="2" height="2" fill="#3b5998"/>
            {/* Nose & Mouth */}
            <rect x="14" y="18" width="4" height="2" fill="#99603f"/>
            <rect x="13" y="22" width="6" height="2" fill="#633722"/>
          </svg>
        );

      case 'bow':
        return (
          <svg viewBox="0 0 32 32" className="w-full h-full drop-shadow">
            <path d="M7 25 C14 24, 24 14, 25 7" stroke="#7a5330" strokeWidth="3" fill="none" strokeLinecap="round"/>
            <path d="M9 23 C15 22, 22 15, 23 9" stroke="#9e7046" strokeWidth="1" fill="none"/>
            <line x1="7" y1="25" x2="25" y2="7" stroke="#eaeaea" strokeWidth="1.5" strokeDasharray="1 1"/>
          </svg>
        );

      case 'mace':
        return (
          <svg viewBox="0 0 32 32" className="w-full h-full drop-shadow">
            {/* Handle */}
            <line x1="7" y1="25" x2="19" y2="13" stroke="#8c6239" strokeWidth="3" strokeLinecap="round"/>
            <line x1="8" y1="24" x2="18" y2="14" stroke="#c49a6c" strokeWidth="1"/>
            {/* Heavy Iron Mace Head */}
            <rect x="17" y="5" width="10" height="10" fill="#6c757d" transform="rotate(45 22 10)" rx="1"/>
            <rect x="18" y="6" width="8" height="8" fill="#adb5bd" transform="rotate(45 22 10)"/>
            {/* Studs */}
            <circle cx="22" cy="10" r="2" fill="#343a40"/>
          </svg>
        );

      case 'piglin_head':
        return (
          <svg viewBox="0 0 32 32" className="w-full h-full drop-shadow">
            <rect x="6" y="7" width="20" height="18" fill="#d99b79" rx="2"/>
            {/* Snout */}
            <rect x="11" y="15" width="10" height="7" fill="#bc7758" rx="1"/>
            <rect x="13" y="17" width="2" height="3" fill="#663c2c"/>
            <rect x="17" y="17" width="2" height="3" fill="#663c2c"/>
            {/* Tusks */}
            <polygon points="10,19 11,19 10.5,15" fill="#f8f9fa"/>
            <polygon points="21,19 22,19 21.5,15" fill="#f8f9fa"/>
            {/* Eyes */}
            <rect x="8" y="11" width="4" height="2" fill="#ffffff"/>
            <rect x="9" y="11" width="2" height="2" fill="#440000"/>
            <rect x="20" y="11" width="4" height="2" fill="#ffffff"/>
            <rect x="21" y="11" width="2" height="2" fill="#440000"/>
          </svg>
        );

      case 'green_bundle':
        return (
          <svg viewBox="0 0 32 32" className="w-full h-full drop-shadow">
            {/* Sack body */}
            <path d="M9 13 C9 9, 23 9, 23 13 C26 18, 26 27, 21 28 C16 29, 11 28, 8 26 C6 21, 6 16, 9 13 Z" fill="#437332"/>
            <path d="M11 14 C12 11, 20 11, 21 14 C23 18, 23 25, 19 26 C15 27, 12 26, 10 24 Z" fill="#589643"/>
            {/* Tie string */}
            <rect x="13" y="10" width="6" height="3" fill="#d4af37" rx="1"/>
          </svg>
        );

      case 'end_crystal':
        return (
          <svg viewBox="0 0 32 32" className="w-full h-full drop-shadow">
            {/* Base bedrock */}
            <rect x="9" y="24" width="14" height="4" fill="#3a3a3a"/>
            {/* Outer glass crystal */}
            <polygon points="16,4 26,14 16,24 6,14" fill="#d68fff" fillOpacity="0.4" stroke="#ff85ea" strokeWidth="1.5"/>
            {/* Inner fiery core */}
            <polygon points="16,8 22,14 16,20 10,14" fill="#ff44aa" stroke="#ffffff" strokeWidth="1"/>
            <circle cx="16" cy="14" r="2.5" fill="#ffff88"/>
          </svg>
        );

      case 'nether_star':
        return (
          <svg viewBox="0 0 32 32" className="w-full h-full drop-shadow">
            {/* Radiant glowing star */}
            <path d="M16 3 L19 12 L28 12 L21 17 L24 26 L16 20 L8 26 L11 17 L4 12 L13 12 Z" fill="#f0f8ff" stroke="#a0e0ff" strokeWidth="1.5"/>
            <path d="M16 6 L18 13 L24 13 L19 17 L21 23 L16 19 L11 23 L13 17 L8 13 L14 13 Z" fill="#ffffff"/>
            <circle cx="16" cy="15" r="3" fill="#ffffaa"/>
          </svg>
        );

      case 'dragon_head':
        return (
          <svg viewBox="0 0 32 32" className="w-full h-full drop-shadow">
            <rect x="6" y="8" width="20" height="16" fill="#1b1c1e" rx="2"/>
            {/* Snout */}
            <rect x="6" y="14" width="20" height="8" fill="#121314"/>
            {/* Purple Eyes */}
            <rect x="9" y="11" width="4" height="2" fill="#d84aff"/>
            <rect x="19" y="11" width="4" height="2" fill="#d84aff"/>
            {/* Nostrils */}
            <rect x="10" y="19" width="2" height="2" fill="#2d2f33"/>
            <rect x="20" y="19" width="2" height="2" fill="#2d2f33"/>
          </svg>
        );

      case 'totem_of_undying':
        return (
          <svg viewBox="0 0 32 32" className="w-full h-full drop-shadow">
            {/* Golden wings */}
            <path d="M5 12 C10 10, 22 10, 27 12 C25 18, 20 18, 16 16 C12 18, 7 18, 5 12 Z" fill="#e5a93b"/>
            {/* Head and eyes */}
            <rect x="11" y="6" width="10" height="9" fill="#f7c844" rx="2"/>
            <rect x="13" y="9" width="2" height="2" fill="#2cd47f"/>
            <rect x="17" y="9" width="2" height="2" fill="#2cd47f"/>
            {/* Torso */}
            <rect x="13" y="15" width="6" height="11" fill="#e5a93b" rx="1"/>
            <circle cx="16" cy="20" r="1.5" fill="#2cd47f"/>
          </svg>
        );

      case 'wolf_spawn_egg':
        return (
          <svg viewBox="0 0 32 32" className="w-full h-full drop-shadow">
            <path d="M16 5 C9 5, 8 16, 8 20 C8 25, 11 27, 16 27 C21 27, 24 25, 24 20 C24 16, 23 5, 16 5 Z" fill="#d7d3c8"/>
            {/* Darker wolf spots */}
            <circle cx="13" cy="14" r="2.5" fill="#887e74"/>
            <circle cx="18" cy="19" r="2" fill="#887e74"/>
            <circle cx="15" cy="23" r="1.5" fill="#887e74"/>
            <circle cx="19" cy="11" r="1.5" fill="#887e74"/>
          </svg>
        );

      case 'redstone':
        return (
          <svg viewBox="0 0 32 32" className="w-full h-full drop-shadow">
            <circle cx="16" cy="17" r="5" fill="#e61919"/>
            <circle cx="12" cy="20" r="3.5" fill="#ba1111"/>
            <circle cx="20" cy="19" r="4" fill="#ff3838"/>
            <circle cx="15" cy="12" r="3" fill="#ff4d4d"/>
            <circle cx="17" cy="16" r="1.5" fill="#ffffff" opacity="0.6"/>
          </svg>
        );

      case 'wither_skeleton_skull':
        return (
          <svg viewBox="0 0 32 32" className="w-full h-full drop-shadow">
            <rect x="7" y="7" width="18" height="18" fill="#1e1e24" rx="2"/>
            {/* Hollow eye sockets */}
            <rect x="10" y="12" width="4" height="4" fill="#000000"/>
            <rect x="18" y="12" width="4" height="4" fill="#000000"/>
            {/* Nose cavity */}
            <rect x="15" y="17" width="2" height="3" fill="#000000"/>
            {/* Teeth */}
            <rect x="11" y="21" width="10" height="2" fill="#3a3a42"/>
          </svg>
        );

      case 'golden_apple':
        return (
          <svg viewBox="0 0 32 32" className="w-full h-full drop-shadow">
            {/* Apple body */}
            <path d="M16 10 C13 7, 7 8, 7 15 C7 23, 13 27, 16 27 C19 27, 25 23, 25 15 C25 8, 19 7, 16 10 Z" fill="#ffd700"/>
            <path d="M16 11 C14 8, 9 9, 9 15 C9 21, 14 25, 16 25 C18 25, 23 21, 23 15 C23 9, 18 8, 16 11 Z" fill="#ffeb3b"/>
            {/* Leaf and stem */}
            <path d="M16 9 C16 6, 17 4, 18 3" stroke="#8d6e63" strokeWidth="2" strokeLinecap="round" fill="none"/>
            <path d="M16 6 C14 4, 11 5, 12 7 C14 7, 15 6, 16 6 Z" fill="#7cb342"/>
          </svg>
        );

      case 'prismarine_crystals':
        return (
          <svg viewBox="0 0 32 32" className="w-full h-full drop-shadow">
            <polygon points="12,5 18,9 14,24 8,20" fill="#44c7b5"/>
            <polygon points="18,9 25,13 21,27 14,24" fill="#66ebd8"/>
            <polygon points="14,24 21,27 17,29 10,26" fill="#2d9688"/>
          </svg>
        );

      case 'soul_lantern':
        return (
          <svg viewBox="0 0 32 32" className="w-full h-full drop-shadow">
            {/* Ring */}
            <circle cx="16" cy="6" r="3" stroke="#484f59" strokeWidth="2" fill="none"/>
            {/* Lantern frame */}
            <rect x="10" y="9" width="12" height="4" fill="#373c43" rx="1"/>
            <rect x="9" y="13" width="14" height="12" fill="#1c242b"/>
            {/* Soul flame */}
            <path d="M16 15 C13 18, 13 22, 16 23 C19 22, 19 18, 16 15 Z" fill="#00d4ff"/>
            <circle cx="16" cy="19" r="2" fill="#ffffff"/>
            <rect x="9" y="25" width="14" height="3" fill="#373c43"/>
          </svg>
        );

      case 'lime_wool':
        return (
          <svg viewBox="0 0 32 32" className="w-full h-full drop-shadow">
            <rect x="6" y="6" width="20" height="20" fill="#70b91b" rx="2"/>
            <rect x="8" y="8" width="16" height="16" fill="#88d825" stroke="#5da012" strokeWidth="1"/>
            <path d="M10 16 L14 20 L22 11" stroke="#ffffff" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" fill="none"/>
          </svg>
        );

      case 'red_wool':
        return (
          <svg viewBox="0 0 32 32" className="w-full h-full drop-shadow">
            <rect x="6" y="6" width="20" height="20" fill="#a81a1a" rx="2"/>
            <rect x="8" y="8" width="16" height="16" fill="#c42828" stroke="#871212" strokeWidth="1"/>
            <line x1="11" y1="11" x2="21" y2="21" stroke="#ffffff" strokeWidth="2.5" strokeLinecap="round"/>
            <line x1="21" y1="11" x2="11" y2="21" stroke="#ffffff" strokeWidth="2.5" strokeLinecap="round"/>
          </svg>
        );

      case 'gold_ingot':
        return (
          <svg viewBox="0 0 32 32" className="w-full h-full drop-shadow">
            <polygon points="7,19 13,11 25,11 19,19" fill="#ffd700"/>
            <polygon points="19,19 25,11 27,15 21,23" fill="#d4af37"/>
            <polygon points="7,19 19,19 21,23 9,23" fill="#e6c200"/>
          </svg>
        );

      case 'black_stained_glass_pane':
      default:
        return (
          <div className="w-full h-full bg-[#181a20]/60 rounded border border-white/5 flex items-center justify-center">
            <div className="w-1.5 h-1.5 rounded-full bg-white/10" />
          </div>
        );
    }
  };

  return (
    <div
      style={{ width: size, height: size }}
      className={`relative inline-flex items-center justify-center select-none ${className} ${glow ? 'animate-pulse' : ''}`}
    >
      {renderGraphic()}
    </div>
  );
};

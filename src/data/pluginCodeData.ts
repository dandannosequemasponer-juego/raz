export interface CodeFile {
  path: string;
  name: string;
  category: 'maven' | 'config' | 'source' | 'ci';
  language: 'java' | 'yaml' | 'xml';
  description: string;
  content: string;
}

export const PLUGIN_METADATA = {
  name: "CytricosRaces",
  version: "7.0.0-RELEASE",
  leadDevelopers: ["Dandan_13", "Wolfit12"],
  server: "CYTRICOS RPG",
  ip: "PLAY.CYTRICOS.COM",
  javaVersion: "Java 21",
  targetApi: "Paper / Spigot 1.20.5+ / 1.21",
  dependencies: [
    { name: "Paper API", version: "1.20.5-R0.1-SNAPSHOT / 1.21", note: "Attribute.GENERIC_SCALE hitbox & visual scaling" },
    { name: "Vault API", version: "1.7", note: "Sistema de economía y costo de reencarnación" },
    { name: "LuckPerms API", version: "5.4", note: "Inyección de Chat Suffix (SuffixNode) para rangos raciales" },
    { name: "PlaceholderAPI", version: "2.11.6", note: "Variables %cytricosraces_*% para TAB, Scoreboard y Chat" },
    { name: "ProtocolLib & LibsDisguises", version: "10.0.44", note: "Transformaciones y metamorfosis raciales (Murciélago, Fenrir)" }
  ]
};

export const INITIAL_RACES_DATA = [
  {
    id: "dwarf",
    name: "Enano",
    displayName: "&6&lEnano",
    slot: 3,
    material: "anvil",
    scale: 0.75,
    baseHealth: 13.0,
    speed: 0.85,
    damage: 1.15,
    immunities: ["knockback"],
    vulnerabilities: ["short_jump", "drowning_speed"],
    description: "Habitantes de las profundidades de colosal resistencia física, maestros forjadores y litomantes.",
    levels: [
      { lvl: 1, name: "Picapedrero", subprefix: "&8[&7Picapedrero&8]", perks: "Hitbox reducida (0.75). Visión nocturna bajo Y=60. Minería II.", req: "64x Hierro Bruto + $1,000", cost: 1000 },
      { lvl: 2, name: "Minero", subprefix: "&8[&7Minero&8]", perks: "15% prob. doble mineral. +3 corazones. Armaduras duran +50%.", req: "32x Piedra Forjada + $5,000", cost: 5000 },
      { lvl: 3, name: "Forjador", subprefix: "&8[&6&lForjador&8]", perks: "Descuento 30% en yunque y reparación.", req: "16x Hierro Rúnico + $15,000", cost: 15000 },
      { lvl: 4, name: "Bastión", subprefix: "&8[&eBastión&8]", perks: "Inmunidad a empuje. Piel de Piedra (6s inmune a flechas).", req: "1x Núcleo Montaña + $45,000", cost: 45000 },
      { lvl: 5, name: "Mountain Lord", subprefix: "&8[&8Mountain Lord&8]", perks: "Escudos irrompibles. Picos ignoran 30% armadura.", req: "3x Núcleo Terrestre + $100,000", cost: 100000 },
      { lvl: 6, name: "King of Deeps", subprefix: "&8[&6&lKING OF DEEPS&8]", perks: "Coloso de Titanio (15s inmune fuego/lava/expl, +40% daño).", req: "1x Netherite Ingot (Nivel Máximo)", cost: 0 }
    ]
  },
  {
    id: "human",
    name: "Humano",
    displayName: "&e&lHumano",
    slot: 4,
    material: "player_head",
    scale: 1.00,
    baseHealth: 10.0,
    speed: 1.00,
    damage: 1.00,
    immunities: [],
    vulnerabilities: [],
    description: "Maestros de la versatilidad, la adaptación táctica, la forja rúnica y el comercio.",
    levels: [
      { lvl: 1, name: "Aprendiz", subprefix: "&8[&7Aprendiz&8]", perks: "+10% XP global en todas las actividades. Adaptación rápida.", req: "32x Trigo + $500", cost: 500 },
      { lvl: 2, name: "Artesano", subprefix: "&8[&7Artesano&8]", perks: "Sin penalizaciones de equipo; -15% precios en tiendas.", req: "16x Lingote Hierro + $2,500", cost: 2500 },
      { lvl: 3, name: "Maestro", subprefix: "&8[&9Maestro&8]", perks: "Doble durabilidad en herramientas y armaduras crafteadas.", req: "8x Diamante + $10,000", cost: 10000 },
      { lvl: 4, name: "Vanguardia", subprefix: "&8[&9Vanguardia&8]", perks: "Mochila táctica (9 slots extras) y offhand activa con buffs.", req: "1x Esmeralda Pulida + $30,000", cost: 30000 },
      { lvl: 5, name: "Campeón", subprefix: "&8[&3Campeón&8]", perks: "+15% daño general y +10% resistencia a todo impacto.", req: "3x Bloque Oro + $75,000", cost: 75000 },
      { lvl: 6, name: "Leyenda Rúnica", subprefix: "&8[&6LEYENDA RÚNICA&8]", perks: "Runas de transporte rápido y +40% daño crítico.", req: "1x Netherite Ingot (Nivel Máximo)", cost: 0 }
    ]
  },
  {
    id: "elf",
    name: "Elfo",
    displayName: "&a&lElfo",
    slot: 5,
    material: "bow",
    scale: 1.00,
    baseHealth: 9.0,
    speed: 1.10,
    damage: 1.15,
    immunities: [],
    vulnerabilities: ["low_health", "fire_damage"],
    description: "Humanoides de orejas largas, agilidad impecable, maestros de la arquería y destreza silvana.",
    levels: [
      { lvl: 1, name: "Scout", subprefix: "&8[&aScout&8]", perks: "+15% daño con arcos y velocidad aumentada sobre hojas.", req: "32x Plumas + $1,000", cost: 1000 },
      { lvl: 2, name: "Cazador", subprefix: "&8[&aCazador&8]", perks: "Visión nocturna permanente y salto aumentado (1.5b).", req: "16x Madera Savia + $5,000", cost: 5000 },
      { lvl: 3, name: "Archienchanter", subprefix: "&8[&aArchienchanter&8]", perks: "Flechas curativas para aliados e imbuición de viento.", req: "8x Cristal Esmeralda + $15,000", cost: 15000 },
      { lvl: 4, name: "Guardián Silvano", subprefix: "&8[&2GuardianSilvano&8]", perks: "Levitación controlada 10s y sigilo en bosques.", req: "1x Lágrima de Bosque + $45,000", cost: 45000 },
      { lvl: 5, name: "Señor del Bosque", subprefix: "&8[&2SeñorBosque&8]", perks: "Flechas ignoran 30% de armadura enemiga.", req: "3x Corazón Silvano + $100,000", cost: 100000 },
      { lvl: 6, name: "Señor Silvano", subprefix: "&8[&2SEÑOR SILVANO&8]", perks: "Escudo eólico defensivo y ráfagas de 3 flechas.", req: "1x Netherite Ingot (Nivel Máximo)", cost: 0 }
    ]
  },
  {
    id: "giant",
    name: "Gigante",
    displayName: "&6&lGigante",
    slot: 11,
    material: "mace",
    scale: 1.35,
    baseHealth: 16.0,
    speed: 0.75,
    damage: 1.40,
    immunities: ["knockback"],
    vulnerabilities: ["arrow_damage", "tunnel_restriction"],
    description: "Estatura monumental, colosal resistencia física, potencia de impacto y lentitud.",
    levels: [
      { lvl: 1, name: "Alto", subprefix: "&8[&7Alto&8]", perks: "Alcance de golpe aumentado (+1b). Hitbox (1.15).", req: "64x Piedra + $1,000", cost: 1000 },
      { lvl: 2, name: "Fuerza", subprefix: "&8[&6Fuerza&8]", perks: "+4 de vida extra. Ignora 15% empuje enemigo.", req: "32x Hierro + $5,000", cost: 5000 },
      { lvl: 3, name: "Titán", subprefix: "&8[&c&lTitán&8]", perks: "Hitbox (1.25). Pisotón Titánico: Eleva enemigos 5b.", req: "16x Bloque Diamante + $15,000", cost: 15000 },
      { lvl: 4, name: "Coloso", subprefix: "&8[&6&lColoso&8]", perks: "+8 de vida extra. Rompe bloques blandos al embestir.", req: "1x Corazón Roca + $45,000", cost: 45000 },
      { lvl: 5, name: "Goliath", subprefix: "&8[&4&lGoliath&8]", perks: "Hitbox (1.35). Inmunidad total a stun y empuje 10s.", req: "3x Núcleo Gigante + $100,000", cost: 100000 },
      { lvl: 6, name: "Titan Supreme", subprefix: "&8[&c&kI&6&lTITAN SUPREME&c&kI&8]", perks: "Coloso Ancestral: +12 vida y potencia descomunal.", req: "1x Netherite Ingot (Nivel Máximo)", cost: 0 }
    ]
  },
  {
    id: "orc",
    name: "Orco",
    displayName: "&4&lOrco",
    slot: 12,
    material: "piglin_head",
    scale: 1.20,
    baseHealth: 14.0,
    speed: 0.90,
    damage: 1.25,
    immunities: ["slowness", "weakness"],
    vulnerabilities: ["hunger_drain", "bow_restriction"],
    description: "Seres de piel verde y colmillos prominentes. Destacan por brutalidad física y maestría con hachas.",
    levels: [
      { lvl: 1, name: "Bruto", subprefix: "&8[&2Bruto&8]", perks: "+4 corazones base. Ataques con hacha ignoran 25% armadura.", req: "64x Hueso + $1,000", cost: 1000 },
      { lvl: 2, name: "Destructor", subprefix: "&8[&2Destructor&8]", perks: "Furia Sangrienta (+50% daño físico al estar bajo de vida).", req: "32x Lingote Hierro + $5,000", cost: 5000 },
      { lvl: 3, name: "Guerrero", subprefix: "&8[&4Guerrero&8]", perks: "Grito de Guerra: Incrementa daño y resistencia a aliados 8b.", req: "16x Bloque Carbón + $15,000", cost: 15000 },
      { lvl: 4, name: "Behemoth", subprefix: "&8[&cBehemoth&8]", perks: "Golpe Sísmico: onda expansiva de empuje a 5b.", req: "1x Corazón Furia + $45,000", cost: 45000 },
      { lvl: 5, name: "Señor de Guerra", subprefix: "&8[&4SeñorDeGuerra&8]", perks: "Inmune a lentitud y debilidad. +25% daño hachas.", req: "3x Núcleo Furia + $100,000", cost: 100000 },
      { lvl: 6, name: "Señor de la Guerra", subprefix: "&8[&4SEÑOR DE LA GUERRA&8]", perks: "Furia Inquebrantable (5s invulnerable al caer bajo 20% vida).", req: "1x Netherite Ingot (Nivel Máximo)", cost: 0 }
    ]
  },
  {
    id: "goblin",
    name: "Duende / Goblin",
    displayName: "&a&lDuende / Goblin",
    slot: 13,
    material: "green_bundle",
    scale: 0.60,
    baseHealth: 7.0,
    speed: 1.25,
    damage: 0.90,
    immunities: [],
    vulnerabilities: ["low_health", "melee_reach"],
    description: "Seres diminutos de piel verde, extremadamente astutos, expertos en trampas, forja y sigilo.",
    levels: [
      { lvl: 1, name: "Pilluelo", subprefix: "&8[&aPilluelo&8]", perks: "Hitbox reducida (0.60), extremadamente difícil de golpear.", req: "32x Pepitas Oro + $1,000", cost: 1000 },
      { lvl: 2, name: "Trampero", subprefix: "&8[&aTrampero&8]", perks: "Colocación instantánea de trampas y 20% desc. en recetas.", req: "16x Redstone + $5,000", cost: 5000 },
      { lvl: 3, name: "Astuto", subprefix: "&8[&aAstuto&8]", perks: "15% probabilidad de robar ítems de la hotbar enemiga.", req: "8x Lingote Oro + $15,000", cost: 15000 },
      { lvl: 4, name: "Saqueador", subprefix: "&8[&2Saqueador&8]", perks: "+25% velocidad al llevar la mano secundaria libre.", req: "1x Sacos de Tesoro + $45,000", cost: 45000 },
      { lvl: 5, name: "Artesano", subprefix: "&8[&aArtesano&8]", perks: "Descuento 40% en forja, encantamientos y crafteos.", req: "3x Pepitas Esmeralda + $100,000", cost: 100000 },
      { lvl: 6, name: "Reduende", subprefix: "&8[&2REDUENDE&8]", perks: "Modo Escurridizo (Invisibilidad y velocidad 5s). Trampa.", req: "1x Netherite Ingot (Nivel Máximo)", cost: 0 }
    ]
  },
  {
    id: "demon",
    name: "Demonkin / Demonio / Oni",
    displayName: "&c&lDemonkin / Demonio / Oni",
    slot: 14,
    material: "end_crystal",
    scale: 1.10,
    baseHealth: 12.0,
    speed: 1.05,
    damage: 1.20,
    immunities: ["fire", "lava"],
    vulnerabilities: ["water_weakness", "holy_vulnerability"],
    description: "Criaturas del inframundo con cuernos frontales, resistencia al fuego y corrupción abisal.",
    levels: [
      { lvl: 1, name: "Imp", subprefix: "&8[&cImp&8]", perks: "Resistencia al Fuego I. Golpes aplican Fuego Abisal.", req: "32x Cuarzo + $1,000", cost: 1000 },
      { lvl: 2, name: "Cornudo", subprefix: "&8[&cCornudo&8]", perks: "+2 corazones vida. Embestida frontal con cuernos.", req: "16x Vara Blaze + $5,000", cost: 5000 },
      { lvl: 3, name: "Devastador", subprefix: "&8[&4Devastador&8]", perks: "Ataques rompen escudos un 25%. Furia abisal.", req: "8x Crema Magma + $15,000", cost: 15000 },
      { lvl: 4, name: "Oni", subprefix: "&8[&c&lOni&8]", perks: "Hitbox 1.10. Explosión de Azufre en área 4b.", req: "1x Alma Nether + $45,000", cost: 45000 },
      { lvl: 5, name: "Abyssal", subprefix: "&8[&4&lAbyssal&8]", perks: "Aura de Corrupción (fuego DoT continuo a 3b).", req: "3x Núcleos Lava + $100,000", cost: 100000 },
      { lvl: 6, name: "Demon King", subprefix: "&8[&4&kI&c&lDEMON KING&4&kI&8]", perks: "Convierte suelo en lava 5s. Dominio Infernal.", req: "1x Netherite Ingot (Nivel Máximo)", cost: 0 }
    ]
  },
  {
    id: "angel",
    name: "Angelkin / Celestial",
    displayName: "&f&lAngelkin / Celestial",
    slot: 15,
    material: "nether_star",
    scale: 1.00,
    baseHealth: 10.0,
    speed: 1.10,
    damage: 1.10,
    immunities: ["fall_damage"],
    vulnerabilities: ["darkness_vulnerability", "dark_potions"],
    description: "Seres alados luminosos dedicados a la sanación, protección radiante y vuelo.",
    levels: [
      { lvl: 1, name: "Creyente", subprefix: "&8[&fCreyente&8]", perks: "Halo luminoso, Regeneración I bajo la luz solar.", req: "32x Plumas + $1,000", cost: 1000 },
      { lvl: 2, name: "Querubín", subprefix: "&8[&fQuerubín&8]", perks: "Inmune a caída. Despliegue de plumas protectoras.", req: "16x Cristal Luz + $5,000", cost: 5000 },
      { lvl: 3, name: "Serafín", subprefix: "&8[&fSerafín&8]", perks: "Despliega Alas: Vuelo continuo sin necesidad de Elytra.", req: "8x Esencia Celestial + $15,000", cost: 15000 },
      { lvl: 4, name: "Arcángel", subprefix: "&8[&eArcángel&8]", perks: "Aura Radiante: cura continua a aliados en radio 6b.", req: "1x Sol Fulgurante + $45,000", cost: 45000 },
      { lvl: 5, name: "Santo", subprefix: "&8[&fSanto&8]", perks: "Escudo de Luz (absorbe hasta 6 corazones entrantes).", req: "3x Núcleo Radiante + $100,000", cost: 100000 },
      { lvl: 6, name: "Arcángel Supremo", subprefix: "&8[&e&lARCANGEL SUPREMO&8]", perks: "Inmunidad temporal + Sanación total instantánea. Juicio.", req: "1x Netherite Ingot (Nivel Máximo)", cost: 0 }
    ]
  },
  {
    id: "dragonkin",
    name: "Dragonkin / Lagarto",
    displayName: "&2&lDragonkin / Lagarto",
    slot: 19,
    material: "dragon_head",
    scale: 1.10,
    baseHealth: 12.0,
    speed: 0.95,
    damage: 1.15,
    immunities: ["fire", "lava"],
    vulnerabilities: ["cold_slowness", "armor_fragility"],
    description: "Descendientes de dragones con cuernos, escamas duras y aliento elemental.",
    levels: [
      { lvl: 1, name: "Escama", subprefix: "&8[&2Escama&8]", perks: "+2 corazones de vida. Resistencia innata al fuego y lava.", req: "32x Escama Dragón + $1,000", cost: 1000 },
      { lvl: 2, name: "Guerrero Draco", subprefix: "&8[&2GuerreroDraco&8]", perks: "Cola defensiva (empuje frontal hacia atrás a enemigos).", req: "16x Ala Dragón + $5,000", cost: 5000 },
      { lvl: 3, name: "Señor Draco", subprefix: "&8[&c&lSeñor Draco&2]", perks: "Activa: 'Aliento de Fuego' (cono de llamas continuo 4s).", req: "8x Núcleo Magma + $15,000", cost: 15000 },
      { lvl: 4, name: "Archidraco", subprefix: "&8[&6Archidraco&8]", perks: "Alas dracónicas (planeo avanzado y saltos elevados).", req: "1x Aliento Ender + $45,000", cost: 45000 },
      { lvl: 5, name: "Wyrm Master", subprefix: "&8[&6Wyrm Master&8]", perks: "Escamas pasivas (+4 armadura). Inmune a empuje.", req: "3x Escama Vacío + $100,000", cost: 100000 },
      { lvl: 6, name: "Draconis Rex", subprefix: "&8[&4&kI&6&lDRACONIS REX&4&kI&8]", perks: "+8 corazones extra, inmune explosiones, rugido rompe escudos.", req: "1x Netherite Ingot (Nivel Máximo)", cost: 0 }
    ]
  },
  {
    id: "beastkin",
    name: "Beastkin / Demihumano",
    displayName: "&6&lBeastkin / Demihumano",
    slot: 20,
    material: "totem_of_undying",
    scale: 1.00,
    baseHealth: 10.0,
    speed: 1.15,
    damage: 1.10,
    immunities: [],
    vulnerabilities: ["bow_restriction", "energy_vulnerability"],
    description: "Híbridos animales (Zorro, Tigre, Oso) expertos en agilidad, rastreo y supervivencia.",
    levels: [
      { lvl: 1, name: "Bestia", subprefix: "&8[&7Bestia&8]", perks: "Olfato desarrollado (detecta partículas agachados 8b).", req: "32x Piel Salvaje + $1,000", cost: 1000 },
      { lvl: 2, name: "Depredador", subprefix: "&8[&7Depredador&8]", perks: "Escalada de paredes natural (trepa muros como escaleras).", req: "16x Garra Oso + $5,000", cost: 5000 },
      { lvl: 3, name: "Coloso", subprefix: "&8[&6Coloso&8]", perks: "Especialización: Oso (+vida), Tigre (+daño), Zorro (+evasión).", req: "8x Garra Tigre + $15,000", cost: 15000 },
      { lvl: 4, name: "Monarca Feral", subprefix: "&8[&aMonarcaFeral&8]", perks: "Animales salvajes no atacan y combaten a su lado.", req: "1x Tótem Inmortal + $45,000", cost: 45000 },
      { lvl: 5, name: "Apex Beast", subprefix: "&8[&aApex Beast&8]", perks: "+30% velocidad carrera. Triple salto en aire.", req: "3x Corazón Alfa + $100,000", cost: 100000 },
      { lvl: 6, name: "Overlord", subprefix: "&8[&2OVERLORD&8]", perks: "Bestia Totémica Suprema (45s): Fuerza física, Regen II.", req: "1x Netherite Ingot (Nivel Máximo)", cost: 0 }
    ]
  },
  {
    id: "werewolf",
    name: "Hombre Lobo",
    displayName: "&c&lHombre Lobo",
    slot: 21,
    material: "wolf_spawn_egg",
    scale: 1.00,
    baseHealth: 10.0,
    speed: 1.15,
    damage: 1.10,
    immunities: [],
    vulnerabilities: ["silver_damage", "cooked_food_nausea"],
    description: "Humanos con capacidad de transformación antropomórfica. Bestias que dependen de velocidad y furia.",
    levels: [
      { lvl: 1, name: "Cachorro", subprefix: "&8[&7Cachorro&8]", perks: "+15% vel. movimiento, come carne cruda sin náuseas.", req: "32x Cordero + $1,000", cost: 1000 },
      { lvl: 2, name: "Acechador", subprefix: "&8[&7Acechador&8]", perks: "Doble salto (parkour), garras afiladas (+2 corazones sin armas).", req: "64x Piel de Lobo + $5,000", cost: 5000 },
      { lvl: 3, name: "Alfa", subprefix: "&8[&4Alfa&8]", perks: "Activa: 'Rugido Aterrador' (Lentitud 10b). Regen nocturna.", req: "16x Plata Pura + $15,000", cost: 15000 },
      { lvl: 4, name: "Bestia", subprefix: "&8[&c&lBestia&8]", perks: "Transformación Bestia (30s): Inmune fuego, rompe escudos.", req: "1x Estrella Nether + $45,000", cost: 45000 },
      { lvl: 5, name: "Horror", subprefix: "&8[&4&lHorror&8]", perks: "Ataques aplican sangrado acumulativo. Inmunidad a fuego.", req: "3x Corazón Dragón Menor + $100,000", cost: 100000 },
      { lvl: 6, name: "Fenrir", subprefix: "&8[&4&k||&c&lFENRIR&4&k||&8]", perks: "Licántropo Supremo. Ignora 50% armadura. Visión térmica 20b.", req: "1x Netherite Ingot (Nivel Máximo)", cost: 0 }
    ]
  },
  {
    id: "vampire",
    name: "Vampiro",
    displayName: "&5&lVampiro",
    slot: 22,
    material: "redstone",
    scale: 1.00,
    baseHealth: 10.0,
    speed: 1.05,
    damage: 1.05,
    immunities: ["poison", "drowning"],
    vulnerabilities: ["sunlight_burn", "rain_corrosion"],
    description: "Criaturas de la noche con orejas puntiagudas y colmillos. Consumen fuerza vital y aborrecen el sol.",
    levels: [
      { lvl: 1, name: "Fledgling", subprefix: "&8[&5Fledgling&8]", perks: "Inmune a veneno y ahogamiento. Se cura al eliminar mobs.", req: "64x Sangre Mob + $1,000", cost: 1000 },
      { lvl: 2, name: "Nocturno", subprefix: "&8[&5Nocturno&8]", perks: "Transformación en Murciélago (vuelo rápido, 2 corazones).", req: "32x Cristal Oscuro + $5,000", cost: 5000 },
      { lvl: 3, name: "Lord", subprefix: "&8[&5&lLord&5]", perks: "Murciélago Gigante. Robo de vida pasivo 15%.", req: "16x Cáliz Sangre + $15,000", cost: 15000 },
      { lvl: 4, name: "Drácula", subprefix: "&8[&4Drácula&5]", perks: "Inmunidad parcial a sol. Teletransporte a sombras 15b.", req: "1x Cáliz Ancestral + $45,000", cost: 45000 },
      { lvl: 5, name: "Nosferatu", subprefix: "&8[&5Nosferatu&8]", perks: "Drena estamina/energía enemiga. Regeneración en oscuridad.", req: "3x Lágrima Oscura + $100,000", cost: 100000 },
      { lvl: 6, name: "Imperator Sanguis", subprefix: "&8[&4&lIMPERATOR SANGUIS&8]", perks: "Inmunidad total a sol. Señor de la Niebla (invulnerable 8s).", req: "1x Netherite Ingot (Nivel Máximo)", cost: 0 }
    ]
  },
  {
    id: "undead",
    name: "Zombi / No-Muerto",
    displayName: "&2&lZombi / No-Muerto",
    slot: 23,
    material: "wither_skeleton_skull",
    scale: 1.00,
    baseHealth: 10.0,
    speed: 1.00,
    damage: 1.05,
    immunities: ["poison", "instant_damage"],
    vulnerabilities: ["healing_potion_damage", "smite_weakness"],
    description: "Cadáveres reanimados, resistentes al dolor y fatiga. Pociones dañinas los curan y la sanación daña.",
    levels: [
      { lvl: 1, name: "Caminante", subprefix: "&8[&7Caminante&8]", perks: "Barra de hambre congelada. Inmune a veneno y daño.", req: "64x Podredumbre + $1,000", cost: 1000 },
      { lvl: 2, name: "Devorador", subprefix: "&8[&7Devorador&8]", perks: "Absorción de vida al consumir carne podrida fresca.", req: "32x Carne Maldita + $5,000", cost: 5000 },
      { lvl: 3, name: "Señor Zombi", subprefix: "&8[&2Señor Zombi&8]", perks: "Ataque mano vacía roba XP. Activa: Ejército de la Tumba.", req: "16x Esencia Alma + $15,000", cost: 15000 },
      { lvl: 4, name: "Lich", subprefix: "&8[&bLich&8]", perks: "Filacteria Portátil (al morir conserva 50% inventario/XP).", req: "1x Alma Enfrascada + $45,000", cost: 45000 },
      { lvl: 5, name: "Necrolord", subprefix: "&8[&3Necrolord&8]", perks: "Aura de descomposición pasiva (Lentitud y hambre 4b).", req: "3x Núcleo Cripta + $100,000", cost: 100000 },
      { lvl: 6, name: "Archilich", subprefix: "&8[&2ARCHILICH&8]", perks: "Revive instantáneamente al morir con 50% vida (1h CD).", req: "1x Netherite Ingot (Nivel Máximo)", cost: 0 }
    ]
  },
  {
    id: "druid",
    name: "Druida Cítrico / Planta",
    displayName: "&e&lDruida Cítrico / Planta",
    slot: 24,
    material: "golden_apple",
    scale: 1.00,
    baseHealth: 10.0,
    speed: 1.00,
    damage: 1.00,
    immunities: ["poison"],
    vulnerabilities: ["meat_nausea", "desert_slowness"],
    description: "Clase insignia de CYTRICOS. Seres de esencia vegetal capaces de sanar, aplicar ácidos y explotar zumos.",
    levels: [
      { lvl: 1, name: "Brote", subprefix: "&8[&eBrote&8]", perks: "Doble nivel saturación frutas. Rechaza carne (náuseas).", req: "32x Manzanas + $1,000", cost: 1000 },
      { lvl: 2, name: "Arboleda", subprefix: "&8[&eArboleda&8]", perks: "Crecimiento cultivos +50% rápido. Daño veneno reflejado.", req: "16x Naranjas Místicas + $5,000", cost: 5000 },
      { lvl: 3, name: "Archidruida", subprefix: "&8[&a&lArchidruida&e]", perks: "Explosión Vitamínica (cura 4 corazones en área a aliados).", req: "8x Esencia Cítrica + $15,000", cost: 15000 },
      { lvl: 4, name: "Cytrico", subprefix: "&8[&eCYTRICO&8]", perks: "Inmune a veneno. Efecto Ácido Cítrico (-20% defensa objetivo).", req: "1x Semilla Ancestral + $45,000", cost: 45000 },
      { lvl: 5, name: "Citrus Overlord", subprefix: "&8[&aCitrus Overlord&8]", perks: "Fertilidad natural 16b. Regeneración acelerada bajo sol.", req: "3x Núcleo Frutal + $100,000", cost: 100000 },
      { lvl: 6, name: "Master Deity", subprefix: "&8[&e&lMASTER DEITY&8]", perks: "Formación Frutal (Bombas de Zumo continuas sin cooldown).", req: "1x Netherite Ingot (Nivel Máximo)", cost: 0 }
    ]
  },
  {
    id: "fairy",
    name: "Hada",
    displayName: "&d&lHada",
    slot: 25,
    material: "prismarine_crystals",
    scale: 0.50,
    baseHealth: 6.0,
    speed: 1.25,
    damage: 0.75,
    immunities: ["fall_damage"],
    vulnerabilities: ["low_health", "water_slowness"],
    description: "Seres diminutos, etéreos, dotados de alas de insecto y capacidad de vuelo ilimitado.",
    levels: [
      { lvl: 1, name: "Chispa", subprefix: "&8[&dChispa&8]", perks: "Vuelo libre pasivo, Hitbox muy reducida (0.50).", req: "32x Polvo Brillante + $1,000", cost: 1000 },
      { lvl: 2, name: "Lucérnaga", subprefix: "&8[&dLucérnaga&8]", perks: "Inmune a caída. Invisibilidad estática al agacharse 3s.", req: "16x Ala Mariposa + $5,000", cost: 5000 },
      { lvl: 3, name: "Esencia", subprefix: "&8[&dEsencia&8]", perks: "Efectividad sanación duplicada. Revive aliados en combate.", req: "8x Esencia Hada + $15,000", cost: 15000 },
      { lvl: 4, name: "Ninfa", subprefix: "&8[&5Ninfa&8]", perks: "Aura radiante pasiva de regeneración en área para equipo.", req: "1x Lágrima Estelar + $45,000", cost: 45000 },
      { lvl: 5, name: "Espíritu Feérico", subprefix: "&8[&5EspírituFeérico&8]", perks: "+40% velocidad de vuelo. Pociones duran el doble.", req: "3x Núcleo Luz + $100,000", cost: 100000 },
      { lvl: 6, name: "Reina Feérica", subprefix: "&8[&5REINA FEÉRICA&8]", perks: "Campo de absorción de daño masivo para clan 12b.", req: "1x Netherite Ingot (Nivel Máximo)", cost: 0 }
    ]
  },
  {
    id: "yokai",
    name: "Espíritu / Yokai",
    displayName: "&b&lEspíritu / Yokai",
    slot: 31,
    material: "soul_lantern",
    scale: 1.00,
    baseHealth: 8.0,
    speed: 1.10,
    damage: 1.00,
    immunities: ["drowning", "suffocation", "slowness"],
    vulnerabilities: ["low_health", "beacon_blindness"],
    description: "Seres etéreos expertos en intangibilidad y manipulación del plano de las almas.",
    levels: [
      { lvl: 1, name: "Ánima", subprefix: "&8[&bÁnima&8]", perks: "Caída Lenta I pasiva. Inmune a trampas de lentitud.", req: "32x Membrana Phantom + $1,000", cost: 1000 },
      { lvl: 2, name: "Espectro", subprefix: "&8[&bEspectro&8]", perks: "Invisibilidad breve (2s) al agacharse en la oscuridad.", req: "16x Polvo Alma + $5,000", cost: 5000 },
      { lvl: 3, name: "Yokai", subprefix: "&8[&3&lYokai&8]", perks: "Paso Espectral: Atraviesa bloques durante 3s (CD: 20 min).", req: "8x Cristal Alma + $15,000", cost: 15000 },
      { lvl: 4, name: "Sombra", subprefix: "&8[&b&lSombra&8]", perks: "Inmunidad al daño por ahogamiento y suffocación.", req: "1x Esencia Vacío + $45,000", cost: 45000 },
      { lvl: 5, name: "Astral Lord", subprefix: "&8[&3&lAstral Lord&8]", perks: "Posesión Temporal: Inmoviliza al objetivo reflejando daño.", req: "3x Lágrimas Ánima + $100,000", cost: 100000 },
      { lvl: 6, name: "Yokai Overlord", subprefix: "&8[&3&kI&b&lYOKAI OVERLORD&3&kI&8]", perks: "Vuelo etéreo flotante sin Elytra. Levita al recibir golpes.", req: "1x Netherite Ingot (Nivel Máximo)", cost: 0 }
    ]
  }
];

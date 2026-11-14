# EXHAUSTIVE SPECIFICATION PROMPT: Paper 1.20.5+ "CytricosRaces" Complete RPG Plugin Implementation

Act as a Principal Minecraft Paper/Spigot Java Architect. You are tasked with writing the full, non-truncated, production-ready codebase for the RPG plugin `CytricosRaces` targeting **Paper 1.20.5+ (Java 21)**. 

The plugin must implement all specifications from the **CYTRICOS RPG Technical Documentation v7.0**, fixing all current runtime bugs (unparsed color codes, unassigned abilities, non-scaling drawbacks), while fully integrating with **WorldGuard**, **LibsDisguises**, **PlaceholderAPI**, **Vault**, and **LuckPerms**, featuring dual **YAML / MySQL** data persistence, high-performance particle & sound engines, level-locked drawbacks, altitude-restricted flight, and accurate Adventure API text formatting.

---

### SECTION 1: CORE ARCHITECTURE & EXTERNAL PLUGIN INTEGRATIONS

1. **Paper 1.20.5+ Adventure API Standard (Strict Color & Formatting Engine):**
   - **ZERO RAW COLOR CODES:** Never output unparsed raw strings like `&d&lHada` or `&5&lVampiro` directly into GUIs, Chat, or ActionBars.
   - All text output, item names, lores, GUI titles, and chat messages MUST use **Adventure API `net.kyori.adventure.text.Component`**.
   - Implement `ColorUtils.java` using `LegacyComponentSerializer.legacyAmpersand().deserialize(text)` for 100% translation of `&` legacy color codes across all interfaces.

2. **Paper Native Attributes (`Attribute.GENERIC_SCALE`):**
   - Use Paper 1.20.5+ native attributes for physical scaling (`Attribute.GENERIC_SCALE`), max health (`GENERIC_MAX_HEALTH`), attack damage (`GENERIC_ATTACK_DAMAGE`), movement speed (`GENERIC_MOVEMENT_SPEED`), and entity interaction range (`GENERIC_ENTITY_INTERACTION_RANGE`).
   - Update scaling dynamically upon race selection, level-up, or active transformation.

3. **WorldGuard Region & Protection API Hook (`WorldGuardHook.java`):**
   - Before executing ANY harmful ability, block destruction (e.g., Giant, Demon, Druid), block phasing (Yokai), AOE shockwaves, or Goblin item theft, query the WorldGuard RegionContainer.
   - Verify `Flags.PVP` is ALLOWED for entity damage/theft, and `Flags.BLOCK_BREAK` / `BUILD` is ALLOWED before breaking or modifying any world blocks. If denied, cancel the action gracefully.

4. **LibsDisguises API Hook (`LibsDisguisesHook.java`):**
   - **Vampire Bat Form (Level 2/3):** Apply `MobDisguise(DisguiseType.BAT)` to the player.
   - **Werewolf Beast Form (Level 4/6):** Apply `MobDisguise(DisguiseType.WOLF)` or `DisguiseType.POLAR_BEAR` (scaled).
   - Automatically undisguise when transformation timers expire or when entering prohibited environments (e.g., water/sunlight).

5. **PlaceholderAPI (PAPI) & TAB Integration (`PlaceholderAPIHook.java`):**
   - Register the PAPI expansion `%cytricosraces_...%` with placeholders for TAB Scoreboard / Nametags:
     - `%cytricosraces_race%`: Color-formatted display name of active race.
     - `%cytricosraces_level%`: Current level integer (1-6).
     - `%cytricosraces_prefix%`: Colorized subprefix (e.g., `&8[&5&lLord&5]`).
     - `%cytricosraces_scale%`: Current entity scale float value.
     - `%cytricosraces_flight_height%`: Max allowed flight altitude above ground.
     - `%cytricosraces_free_change%`: Availability status (`True`/`False`).

6. **Dual Data Persistence Engine (`StorageManager.java`):**
   - Provide a unified storage system with a toggle in `config.yml` (`storage.type: YAML` or `MYSQL`).
   - Allow flatfile YAML editing for easy manual configuration changes, with automated SQL connection pooling (HikariCP) if MySQL is enabled.

---

### SECTION 2: ALL 16 RACES - COMPLETE 6-LEVEL MATRIX & MECHANIC SPECIFICATIONS

#### 1. WEREWOLF (`werewolf`) - Base Scale: 1.00
- **Icon in GUI:** `minecraft:wolf_spawn_egg`
- **Level 1 (`&8[&7Cachorro&8]`):** +15% movement speed. Raw meat grants extra saturation without nausea/poison. Cooked meat grants Nausea I (10s). Heavy armor imposes -20% damage penalty. Req: 32x Mutton + $1,000.
- **Level 2 (`&8[&7Acechador&8]`):** Double jump boost. Bare-handed claws (+2 hearts damage). Full moon triggers frenzy particle effects (`Particle.REDSTONE` red). Req: 64x Wolf Pelt + $5,000.
- **Level 3 (`&8[&4Alfa&8]`):** Active "Terrifying Roar" (applies Slowness II to targets within 10b for 5s). Night regeneration I. Takes +30% damage from silver-named weapons. Req: 16x Pure Silver + $15,000.
- **Level 4 (`&8[&c&IBestia&8]`):** Beast Transformation active (30s): increases scale to 1.35, applies Strength II, Speed II, shield break, LibsDisguises Wolf disguise. Sprinting consumes double hunger. Req: 1x Nether Star + $45,000.
- **Level 5 (`&8[&4&IHorror&8]`):** Attacks apply 3-stack Bleed effect (DoT every 2s). Fire immunity. Healing potions 50% effective. Req: 3x Minor Dragon Heart + $100,000.
- **Level 6 (`&8[&4&k||&c&IFENRIR&4&k||&8]`):** Ignores 50% enemy armor. Thermal vision (Glowing effect on targets within 20b). Spectral Claw attack.

#### 2. VAMPIRE (`vampire`) - Base Scale: 1.00
- **Icon in GUI:** `minecraft:redstone`
- **Level 1 (`&8[&5Fledgling&8]`):** Poison & Drowning immunity. Heals on mob kills. Cannot consume solid food. **Sunlight Burn Engine:** Burns under direct sky light = 15 when daytime (`0-12000 ticks`) and not raining. Instant safety when stepping under shade (`highestBlockY <= playerY`). Req: 64x Mob Blood + $1,000.
- **Level 2 (`&8[&7Nocturno&8]`):** Bat Transformation active (LibsDisguises Bat, scale 0.3, fast flight, HP reduced to 2 hearts). Rain exposure causes corrosion damage. Req: 32x Dark Crystal + $5,000.
- **Level 3 (`&8[&5&lLord&5]`):** Giant Bat form. 15% passive lifesteal on melee hit. Partial sun resistance (requires helmet). Shadow Teleportation (15b dash). Req: 16x Blood Chalice + $15,000.
- **Level 4 (`&8[&4Drácula&5]`):** Double damage from fire. Req: 1x Ancestral Chalice + $45,000.
- **Level 5 (`&8[&5Nosferatu&8]`):** Drains target hunger on hit. Accelerated night regen. Redstone/Beacon lit blocks reduce speed by 40%. Req: 3x Dark Tear + $100,000.
- **Level 6 (`&8[&4&IIMPERATOR SANGUIS&8]`):** **100% Sunlight Immunity** (completely suppresses sunlight burn listener). Mist Form (8s invulnerability and untargetable).

#### 3. DRAGONKIN (`dragonkin`) - Base Scale: 1.10
- **Icon in GUI:** `minecraft:dragon_head`
- **Level 1 (`&8[&2Escama&8]`):** +2 base hearts. Fire and Lava immunity. Slowness III while swimming in cold water/biomes. Req: 32x Dragon Scale + $1,000.
- **Level 2 (`&8[&7GuerreroDraco&8]`):** Defensive Tail Whip (crouching knocks back frontal attackers). Heavy armor breaks 10% faster. Req: 16x Dragon Wing + $5,000.
- **Level 3 (`&8[&c&lSeñor Draco&2]`):** Active "Fire Breath" (continuous 4s cone of flame in facing direction, `Particle.DRAGON_BREATH`). Req: 8x Magma Core + $15,000.
- **Level 4 (`&8[&6Archidraco&8]`):** Draconic Wings (gliding and high leap). Helmet cannot be worn while flying. Req: 1x Ender Breath + $45,000.
- **Level 5 (`&8[&6Wyrm Master&8]`):** Passive Dragon Scales (+4 natural armor). Knockback immunity. Req: 3x Void Scale + $100,000.
- **Level 6 (`&8[&4&kl&6&IDRACONIS REX&4&kl&8]`):** +8 extra hearts. Explosion immunity. Shield-breaking roar. Plasma Sphere projectile.

#### 4. DEMIHUMAN / BEASTKIN (`beastkin`) - Base Scale: 1.00
- **Icon in GUI:** `minecraft:totem_of_undying`
- **Level 1 (`&8[&7Bestia&8]`):** Scent Tracking (spawns particles over crouched targets within 8b). Bow penalty (deflects regular bows, crossbows permitted). Req: 32x Wild Pelt + $1,000.
- **Level 2 (`&8[&7Depredador&8]`):** Wall Climbing (climbs vertical blocks like ladders). Hunger drops 25% faster. Req: 16x Bear Claw + $5,000.
- **Level 3 (`&8[&6Coloso&8]`):** Path Specialization: Bear (+HP), Tiger (+Damage), Fox (+Evasion). Villager trading 30% pricier. Req: 8x Tiger Claw + $15,000.
- **Level 4 (`&8[&aMonarcaFeral&8]`):** +15% damage received from magical/energy attacks. Req: 1x Totem of Undying + $45,000.
- **Level 5 (`&8[&aApex Beast&8]`):** +30% sprint speed. Triple jump. Cannot equip full Netherite without losing agility. Req: 3x Alpha Heart + $100,000.
- **Level 6 (`&8[&2OVERLORD&8]`):** Wild beasts are non-hostile and defend player. Totemic Beast Form (45s burst of Strength, Regen II).

#### 5. UNDEAD / ZOMBIE (`undead`) - Base Scale: 1.00
- **Icon in GUI:** `minecraft:wither_skeleton_skull`
- **Level 1 (`&8[&7Caminante&8]`):** Frozen hunger bar. Poison and Instant Damage immunity. Healing potions inflict direct damage. Req: 64x Rotten Flesh + $1,000.
- **Level 2 (`&8[&7Devorador&8]`):** Consuming rotten flesh grants absorption hearts and instant health. Swim speed -30%. Req: 32x Cursed Flesh + $5,000.
- **Level 3 (`&8[&2SeñorZombi&8]`):** Bare-handed hit drains target EXP or applies Nausea. Active "Army of the Grave" (summons 2 mini-zombies for 20s). Takes 2x damage from Smite. Req: 16x Soul Essence + $15,000.
- **Level 4 (`&8[&bLich&8]`):** Portable Phylactery (retains 50% EXP/Inventory on death, 30m CD). Takes +25% damage from sacred arrows. Req: 1x Bottled Soul + $45,000.
- **Level 5 (`&8[&3Necrolord&8]`):** Passive Decay Aura (applies Slowness and Hunger to targets within 4b). Can only eat rotten flesh or splash dark potions. Req: 3x Crypt Core + $100,000.
- **Level 6 (`&8[&2ARCHILICH&8]`):** Auto-resurrect on death with 50% HP (1h CD). Complete Smite immunity.

#### 6. HUMAN (`human`) - Base Scale: 1.00
- **Icon in GUI:** `minecraft:player_head`
- **Level 1 (`&8[&7Aprendiz&8]`):** +10% global EXP multiplier. Req: 32x Wheat + $500.
- **Level 2 (`&8[&7Artesano&8]`):** No equipment restrictions. 15% discount at server shops. Req: 16x Iron Ingot + $2,500.
- **Level 3 (`&8[&9Maestro&8]`):** Double durability on all self-crafted tools and armor. Req: 8x Diamond + $10,000.
- **Level 4 (`&8[&9Vanguardia&8]`):** Tactical Backpack (opens dynamic 9-slot inventory via command `/raza mochila`). Active offhand item buffs. Req: 1x Polished Emerald + $30,000.
- **Level 5 (`&8[&3Campeón&8]`):** +15% physical damage, +10% damage resistance. Req: 3x Gold Block + $75,000.
- **Level 6 (`&8[&6LEYENDA RÚNICA&8]`):** Fast-travel runic markers. +40% critical damage boost. Adrenaline Overcharge and Runic Shield.

#### 7. ELF (`elf`) - Base Scale: 1.00
- **Icon in GUI:** `minecraft:bow`
- **Level 1 (`&8[&aScout&8]`):** +15% bow damage. Speed boosted on leaves/wood. Base health penalty (-10%). Req: 32x Feathers + $1,000.
- **Level 2 (`&8[&7Cazador&8]`):** Permanent Night Vision. Jump height boosted (1.5b). Melee damage weakness. Req: 16x Sap Wood + $5,000.
- **Level 3 (`&8[&aArchienchanter&8]`):** Healing arrows (shots heal allies). Wind bow imbuement. Req: 8x Emerald Crystal + $15,000.
- **Level 4 (`&8[&2GuardianSilvano&8]`):** Controlled levitation (10s). Forest stealth. Heavy armor cancels levitation. Req: 1x Forest Tear + $45,000.
- **Level 5 (`&8[&2Señor Bosque&8]`):** Arrows ignore 30% of target armor. Takes +15% extra fire damage. Req: 3x Silvan Heart + $100,000.
- **Level 6 (`&8[&2SEÑOR SILVANO&8]`):** Defensive Wind Shield. 3-arrow volley burst. Homing Arrow shot.

#### 8. FAIRY (`fairy`) - Base Scale: 0.50
- **Icon in GUI:** `minecraft:prismarine_crystals`
- **Level 1 (`&8[&dChispa&8]`):** Permanent scale 0.50. Passive Flight. Base max health reduced to 6 hearts. **Height Limit System:** Enforces configurable `max_flight_height` above terrain. Req: 32x Glowstone Dust + $1,000.
- **Level 2 (`&8[&7Lucérnaga&8]`):** Fall damage immunity. Static invisibility while crouched (3s). Req: 16x Butterfly Wing + $5,000.
- **Level 3 (`&8[&dEsencia&8]`):** Double healing potion effectiveness. Ally revival mechanic (1h CD). Cannot equip shields or heavy chestplates. Req: 8x Fairy Essence + $15,000.
- **Level 4 (`&8[&5Ninfa&8]`):** Radiant AOE healing aura for party members. Melee attack strength -25%. Req: 1x Stellar Tear + $45,000.
- **Level 5 (`&8[&5Espíritu Feérico&8]`):** +40% flight speed. Potion duration doubled. Severe Slowness in deep water. Req: 3x Light Core + $100,000.
- **Level 6 (`&8[&5REINA FEÉRICA&8]`):** Clan damage absorption field (12b). Confusion Dust and Illusionary Mirage skills.

#### 9. ORC (`orc`) - Base Scale: 1.20
- **Icon in GUI:** `minecraft:piglin_head`
- **Level 1 (`&8[&2Bruto&8]`):** +4 base hearts. Axe attacks ignore 25% target armor. -10% base movement speed. Req: 64x Bones + $1,000.
- **Level 2 (`&8[&7Destructor&8]`):** "Bloody Fury" (+50% physical damage when HP drops below 30%). Hitbox increased (1.20). Req: 32x Iron Ingot + $5,000.
- **Level 3 (`&8[&4Guerrero&8]`):** War Cry (grants Strength and Resistance to allies within 8b). Cannot use bows accurately. Req: 16x Coal Block + $15,000.
- **Level 4 (`&8[&cBehemoth&8]`):** Seismic Axe Stomp (generates 5b knockback shockwave). Hunger drains 30% faster in combat. Req: 1x Fury Heart + $45,000.
- **Level 5 (`&8[&4SeñorDeGuerra&8]`):** Immunity to Slowness and Weakness. +25% additional axe damage. Cannot equip Elytra. Req: 3x Fury Core + $100,000.
- **Level 6 (`&8[&4SEÑOR DE LA GUERRA&8]`):** "Unyielding Fury" (5s absolute invulnerability when HP drops below 20%). Magma Imbuement and Ash Shield.

#### 10. CITRUS DRUID (`druid`) - Base Scale: 1.00
- **Icon in GUI:** `minecraft:golden_apple`
- **Level 1 (`&8[&eBrote&8]`):** Double saturation from fruit. Eating meat grants Nausea. Crop growth speed +50% in 5b radius. Reflected poison damage. Slowness in deserts/darkness. Req: 32x Apples + $1,000.
- **Level 2 (`&8[&7Arboleda&8]`):** -10% speed on stone/cobblestone. Req: 16x Mystic Oranges + $5,000.
- **Level 3 (`&8[&a&lArchidruida&e]`):** "Vitamin Explosion" (heals 4 hearts to nearby allies). Takes +25% damage from fire. Req: 8x Citrus Essence + $15,000.
- **Level 4 (`&8[&eCYTRICO&8]`):** Poison immunity. "Citrus Acid" hit effect (-20% target defense). Rapid regen under direct sunlight. Hydration system (requires drinking water/juice bottles). Req: 1x Ancestral Seed + $45,000.
- **Level 5 (`&8[&aCitrus Overlord&8]`):** Heavy armor reduces healing by 30%. Req: 3x Fruit Core + $100,000.
- **Level 6 (`&8[&e&IMASTER DEITY&8]`):** Fruit Bomb form (unleashes gaseous juice bombs, thorn growth).

#### 11. DWARF (`dwarf`) - Base Scale: 0.75
- **Icon in GUI:** `minecraft:anvil`
- **Level 1 (`&8[&7Picapedrero&8]`):** Scale 0.75. Permanent Night Vision below Y=60. Haste II in caves. -15% movement speed. Req: 64x Raw Iron + $1,000.
- **Level 2 (`&8[&7Minero&8]`):** 15% chance for double ore drops. +3 base hearts. Armor durability lasts +50% longer. Jump height reduced. Req: 32x Smooth Stone + $5,000.
- **Level 3 (`&8[&6&IForjador&8]`):** 30% discount on anvil combination and repair costs. Sinks rapidly in water and consumes double oxygen. Req: 16x Runic Iron + $15,000.
- **Level 4 (`&8[&eBastión&8]`):** Full Knockback immunity. Active "Stone Skin" (6s arrow immunity). Takes +20% fall damage. Req: 1x Mountain Core + $45,000.
- **Level 5 (`&8[&8Mountain Lord&8]`):** Shields suffer zero durability loss. Hammers/Picks ignore 30% armor. Cannot fly with Elytra. Req: 3x Earth Core + $100,000.
- **Level 6 (`&8[&6&IKING OF DEEPS&8]`):** "Titanium Colossus" (15s immunity to fire/lava/explosions, +40% damage). Mountain Fury.

#### 12. DEMON / ONI (`demon`) - Base Scale: 1.10
- **Icon in GUI:** `minecraft:end_crystal`
- **Level 1 (`&8[&cImp&8]`):** Fire Resistance I. Physical attacks ignite targets with Abyssal Fire. Water/Rain exposure applies Weakness I. Req: 32x Quartz + $1,000.
- **Level 2 (`&8[&cCornudo&8]`):** +2 base hearts. Horn Charge dash. Gold armor breaks 2x faster. Req: 16x Blaze Rod + $5,000.
- **Level 3 (`&8[&4Devastador&8]`):** Physical attacks break shields by 25%. Takes +20% damage from holy/smite weapons. Req: 8x Magma Cream + $15,000.
- **Level 4 (`&8[&c&lOni&8]`):** Scale 1.10. Active "Sulfur Explosion" (4b radius AOE). Healing potions drain saturation. Req: 1x Nether Soul + $45,000.
- **Level 5 (`&8[&4&lAbyssal&8]`):** Corruption Aura (continuous DoT fire to enemies within 3b). Slowness I in deep water. Req: 3x Lava Core + $100,000.
- **Level 6 (`&8[&4&kl&c&IDEMON KING&4&kl&8]`):** Complete Fire/Lava immunity. Converts surrounding floor to netherrack/lava for 5s. Infernal Domain.

#### 13. YOKAI / PHANTOM SPIRIT (`yokai`) - Base Scale: 1.00
- **Icon in GUI:** `minecraft:soul_lantern`
- **Level 1 (`&8[&bÁnima&8]`):** Passive Slow Falling I. Slowness trap immunity. Base HP penalty (-20%). Req: 32x Phantom Membrane + $1,000.
- **Level 2 (`&8[&bEspectro&8]`):** Invisibility while crouched in dark areas (2s). Takes +15% projectile damage. Req: 16x Soul Dust + $5,000.
- **Level 3 (`&8[&3&lYokai&8]`):** **Spectral Step (Block Phasing):** Active ability allowing player to walk through solid blocks for 3s (20m CD). **Restriction:** CANNOT deal damage or attack entities while inside blocks. Direct sunlight instantly cancels phasing and ejects player to nearest open space. Req: 8x Soul Crystal + $15,000.
- **Level 4 (`&8[&b&lSombra&8]`):** Drowning and Suffocation immunity. Cannot wear Iron or Netherite armor. Req: 1x Void Essence + $45,000.
- **Level 5 (`&8[&3&IAstral Lord&8]`):** Temporal Possession (freezes target and reflects incoming damage). Bright beacon light causes Blindness I. Req: 3x Anima Tear + $100,000.
- **Level 6 (`&8[&3&kl&b&IYOKAI OVERLORD&3&kl&8]`):** Ethereal floating flight without Elytra. Shadow Veil and Astral Displacement.

#### 14. GIANT (`giant`) - Base Scale: 1.35
- **Icon in GUI:** `minecraft:mace`
- **Level 1 (`&8[&7Alto&8]`):** Scale 1.15. Extended melee reach (+1 block). Cannot fit into 1x2 tunnels. Req: 64x Stone + $1,000.
- **Level 2 (`&8[&6Fuerza&8]`):** +4 base hearts. Ignores 15% knockback. Permanent Slowness I. Fast hunger drain. Req: 32x Iron + $5,000.
- **Level 3 (`&8[&c&lTitán&8]`):** Scale 1.25. Titanic Stomp (launches targets 5 blocks upward). Base speed -25%. Req: 16x Diamond Block + $15,000.
- **Level 4 (`&8[&6&lColoso&8]`):** +8 base hearts. Breaks weak blocks (leaves, glass) when sprinting. Takes +30% ranged arrow damage. Req: 1x Rock Heart + $45,000.
- **Level 5 (`&8[&4&lGoliath&8]`):** Scale 1.35. Full immunity to stun and knockback for 10s. Cannot enter boats or mount horses. Req: 3x Giant Core + $100,000.
- **Level 6 (`&8[&c&kl&6&ITITAN SUPREME&c&kl&8]`):** Ancestral Colossus Form (+12 extra hearts, massive impact force). Telluric Impact and Rock Wall.

#### 15. ANGEL / CELESTIAL (`angel`) - Base Scale: 1.00
- **Icon in GUI:** `minecraft:nether_star`
- **Level 1 (`&8[&fCreyente&8]`):** Radiant halo light. Daytime Regen I. Ranged dark projectile weakness (+10%). Req: 32x Feathers + $1,000.
- **Level 2 (`&8[&7Querubín&8]`):** Fall damage immunity. Feather Shield. Takes +20% damage in pitch darkness. Req: 16x Light Crystal + $5,000.
- **Level 3 (`&8[&fSerafín&8]`):** **Deploy Wings Flight:** Enables controlled flight without Elytra. **Enforces Height Limit System** (`max_flight_height`). Heavy chestplates disabled during flight. Req: 8x Celestial Essence + $15,000.
- **Level 4 (`&8[&eArcángel&8]`):** Radiant Aura (heals allies within 6b radius). Increased hunger drain while flying. Req: 1x Radiant Sun + $45,000.
- **Level 5 (`&8[&fSanto&8]`):** Shield of Light (absorbs up to 6 hearts incoming damage). Cannot consume dark potions. Req: 3x Radiant Core + $100,000.
- **Level 6 (`&8[&e&IARCANGEL SUPREMO&8]`):** Temporal invulnerability + full instant heal. Radiant Judgment and Divine Blessing.

#### 16. GOBLIN / DUENDE (`goblin`) - Base Scale: 0.60
- **Icon in GUI:** `minecraft:green_bundle`
- **Level 1 (`&8[&aPilluelo&8]`):** Fixed scale 0.60. Melee attack reach reduced by 30%. Gold Nodule crafting discount. Req: 32x Gold Nuggets + $1,000.
- **Level 2 (`&8[&7Trampero&8]`):** Instant trap placement. 20% discount on crafting recipes. Base HP reduced to 7 hearts. Req: 16x Redstone + $5,000.
- **Level 3 (`&8[&aAstuto&8]`):** **Hotbar Item Theft Engine:** In `EntityDamageByEntityEvent`, when Goblin (Level >= 3) attacks a player: check WorldGuard PvP flag, roll 15% probability (`ThreadLocalRandom.current().nextDouble() <= 0.15`). Randomly select an item slot (0-8) from victim's main Hotbar and transfer it to Goblin's inventory. Play `Sound.ENTITY_ITEM_PICKUP`, spawn `Particle.VILLAGER_HAPPY`, and send ActionBar alert to both players. Cannot wear Netherite armor. Req: 8x Gold Ingot + $15,000.
- **Level 4 (`&8[&2Saqueador&8]`):** +25% movement speed when offhand is empty. Takes +25% damage from heavy attacks. Req: 1x Treasure Sack + $45,000.
- **Level 5 (`&8[&aArtesano&8]`):** 40% discount on anvil combination, enchanting, and crafting. Slowness when inventory is full. Req: 3x Emerald Nuggets + $100,000.
- **Level 6 (`&8[&2REDUENDE&8]`):** Slip Mode (5s total invisibility + speed burst). Redstone Trap and Ambush.

---

### SECTION 3: SPECIFIC MECHANIC IMPLEMENTATION LOGIC

1. **Fairy & Angel Height Flight Enforcement Task:**
   - Execute a periodic `BukkitRunnable` (every 10 ticks) checking all flying players.
   - Calculate distance: `double heightAboveGround = playerLoc.getY() - world.getHighestBlockYAt(playerLoc);`
   - If `heightAboveGround > max_flight_height` (configured per race/level in `races.yml`), gently push player downward (`player.setVelocity(new Vector(0, -0.3, 0))`), play `Sound.BLOCK_NOTE_BLOCK_BASS`, spawn `Particle.END_ROD`, and display ActionBar: `§c¡Altura máxima de vuelo alcanzada!`.

2. **Vampire Sunlight & Shade Burn Engine:**
   - Run a `BukkitRunnable` every 20 ticks (1 second).
   - Target burns ONLY if ALL conditions are met:
     1. Player is a Vampire (Levels 1 to 5). **Level 6 (Imperator Sanguis) HAS 100% SUNLIGHT IMMUNITY** and completely bypasses this task.
     2. World time is daytime (`time >= 0 && time <= 12000`) and world is NOT raining.
     3. Direct sky light level at player position is 15 (`block.getLightFromSky() == 15`).
     4. Player is NOT under shade (`world.getHighestBlockYAt(playerLoc) <= playerLoc.getBlockY()`).
   - If player moves under opaque blocks, instantly halt burning damage.

3. **Goblin Hotbar Theft Listener:**
   - In `EntityDamageByEntityEvent`: check if attacker is Goblin Level >= 3, victim is Player, and WorldGuard PvP flag is ALLOWED.
   - Roll 15% chance (`ThreadLocalRandom.current().nextDouble() <= 0.15`).
   - Scan victim's hotbar (slots 0 to 8). Collect non-empty slot indices. Select one randomly.
   - Move ItemStack to Goblin's inventory (or drop at feet if full).
   - Play `Sound.ENTITY_ITEM_PICKUP`, trigger `Particle.VILLAGER_HAPPY`, and output ActionBars to both players using Adventure API.

4. **Yokai Spectral Step (Block Phasing):**
   - Command `/raza habilidad` triggers 3s phase mode.
   - Check WorldGuard build/pvp flags. Allow walking through solid blocks.
   - **Anti-Exploit:** Cancel damage dealing while inside blocks (`EntityDamageByEntityEvent` cancelled). Direct sunlight exposure instantly terminates phasing and teleports player to highest solid Y.

---

### SECTION 4: INTERACTIVE GUI & ADMIN ENGINE SYSTEM

1. **Player Selection & Level View GUI (`PlayerGUI.java`):**
   - 5 rows (45 slots) filled with `black_stained_glass_pane` borders.
   - Dynamic race icons based on `races.yml` materials.
   - **Confirmation GUI:** Triggered when clicking a new race. Slot 44/11 (Lime Wool) confirms selection (applies Vault cost, resets to Level 1, updates scale), Slot 15 (Red Wool) cancels operation.
   - **Level Progress GUI:** Clicking current active race displays the 6-level progression tree, requirements, and current level status.

2. **Admin Interactive GUI (`AdminGUI.java` - `/raza admin menu`):**
   - **Drag & Drop Slot Reordering:** Drag race icons across slots 0-44 in GUI to instantly update and persist positions in `races.yml`.
   - **Shift-Left-Click Submenu:** Adjust global variables (interactive buttons for Scale `+0.05` / `-0.05`, base HP, movement speed, and passive toggles).
   - **Level Edit Submenu:** Configure item requirements, Vault money costs, flight altitude limits (`max_flight_height`), and LuckPerms prefix/suffix per level.
   - **Race Change Management Panel (`/raza admin changemenu`):** Edit Vault cost ($X), set hand item as Token Special Change item (custom lore, display name, CustomModelData).

---

### SECTION 5: COMMAND MATRIX & PLACEHOLDERAPI EXPANSION

1. **Player Commands (100% Spanish Chat/GUI Output):**
   - `/razas` or `/raza gui` - Opens main race selection GUI.
   - `/raza subirnivel` - Level up checking item requirements & Vault cost.
   - `/raza info [raza]` - Displays race stats and level details.
   - `/raza stats` - Displays player's current race, level, scale, base HP.
   - `/raza cambiarraza <raza> [metodo]` - Requests race change (gratis, dinero, objeto).
   - `/raza estado` - Displays player race change token status.

2. **Admin Commands (Staff):**
   - `/raza admin menu` - Opens interactive Admin GUI.
   - `/raza admin cambios parar|iniciar` - Globally pauses or resumes race changes.
   - `/raza admin estableceralturavuelo <raza> <nivel> <bloques>` - Set max flight height limit.
   - `/raza admin establecerpreciocambio <monto>` - Sets Vault price for race change.
   - `/raza admin establecerobjetocambio` - Sets item in hand as Special Change Token.
   - `/raza admin darobjetocambio <jugador> [cantidad]` - Gives Special Token to player.
   - `/raza admin concedercambiogratis <jugador>` - Grants 1 free race change attempt.
   - `/raza admin revocarcambiogratis <jugador>` - Revokes free race change attempt.
   - `/raza admin recargar` - Live reloads `config.yml` and `races.yml`.

---

### REQUIRED CODE DELIVERABLES

Generate complete, fully written, un-truncated Java code for:
1. `Main.java` (Plugin lifecycle, Vault/PAPI/WorldGuard/LibsDisguises hooks, flight & sun task loops).
2. `ColorUtils.java` (Adventure API Component serializer for `&` legacy codes).
3. `StorageManager.java`, `PlayerDataManager.java`, `PlayerData.java` (Dual YAML/MySQL persistence).
4. `RaceManager.java` & `ScaleManager.java` (Paper `Attribute.GENERIC_SCALE` injection).
5. `WorldGuardHook.java` & `LibsDisguisesHook.java` (Region protection & entity transformation wrappers).
6. `PlaceholderAPIHook.java` (PAPI expansion for TAB scoreboard/nametags).
7. `PlayerGUI.java` & `AdminGUI.java` (Interactive menus with Drag & Drop saving).
8. `GUIListener.java` (Inventory clicks, drag events, confirmation workflow).
9. `RaceAbilitiesListener.java` (Goblin theft, Vampire sun burn + shade logic, Yokai block phasing, height-restricted flight loop, visual particles and audio engine).
10. `CommandManager.java` (Full command tree with TabCompleter).
11. Default `config.yml` and `races.yml`.
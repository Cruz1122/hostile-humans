# Large visual showcase for squad combat, retreat protection, isolation and cohesion.
# Run with: /function hostile_humans:debug/squad_showcase
kill @e[tag=hh_squad_showcase]

# Stable viewing conditions.
gamerule doMobSpawning false
gamerule mobGriefing true
gamerule doDaylightCycle false
time set day
weather clear

# Shared 81x61 arena and perimeter.
fill ~-40 ~-1 ~-30 ~40 ~-1 ~30 minecraft:smooth_stone
fill ~-40 ~ ~-30 ~40 ~6 ~-30 minecraft:deepslate_bricks
fill ~-40 ~ ~30 ~40 ~6 ~30 minecraft:deepslate_bricks
fill ~-40 ~ ~-29 ~-40 ~6 ~29 minecraft:deepslate_bricks
fill ~40 ~ ~-29 ~40 ~6 ~29 minecraft:deepslate_bricks

# Central observation platform.
fill ~-4 ~7 ~-4 ~4 ~7 ~4 minecraft:tinted_glass
fill ~-1 ~8 ~-1 ~1 ~9 ~1 minecraft:air

# Colored zones: west = squad fight, east = protect, north = cohesion.
fill ~-37 ~-1 ~-12 ~-8 ~-1 ~12 minecraft:red_concrete
fill ~8 ~-1 ~-12 ~37 ~-1 ~12 minecraft:orange_concrete
fill ~-16 ~-1 ~16 ~16 ~-1 ~27 minecraft:green_concrete

# Low separators keep each behavior readable without creating a giant maze.
fill ~-5 ~ ~-14 ~-5 ~3 ~14 minecraft:deepslate_bricks
fill ~5 ~ ~-14 ~5 ~3 ~14 minecraft:deepslate_bricks
fill ~-18 ~ ~14 ~18 ~2 ~14 minecraft:deepslate_bricks

# Floating labels.
summon minecraft:text_display ~-22 ~5 ~-12 {text:"{\"text\":\"1. SHARED AGGRO - 4v4\",\"color\":\"gold\",\"bold\":true}",billboard:"center",background:1073741824,Tags:["hh_squad_showcase"]}
summon minecraft:text_display ~22 ~5 ~-12 {text:"{\"text\":\"2. PROTECT RETREATING ALLY\",\"color\":\"yellow\",\"bold\":true}",billboard:"center",background:1073741824,Tags:["hh_squad_showcase"]}
summon minecraft:text_display ~ ~5 ~25 {text:"{\"text\":\"3. IDLE COHESION / REJOIN\",\"color\":\"green\",\"bold\":true}",billboard:"center",background:1073741824,Tags:["hh_squad_showcase"]}

# -----------------------------------------------------------------------------
# Zone 1: Hispanic squad versus International squad.
# One forced damage event starts local shared aggro; tiers remain dataset-driven.
# -----------------------------------------------------------------------------
summon hostile_humans:human_tier1 ~-34 ~ ~-6 {PersonaId:"coldified",SquadId:[I;286331153,286331153,286331153,286331153],PersistenceRequired:1b,Tags:["hh_squad_showcase","hh_showcase_fight","hh_showcase_hispanic"]}
summon hostile_humans:human_tier1 ~-34 ~ ~-2 {PersonaId:"elrichmc",SquadId:[I;286331153,286331153,286331153,286331153],PersistenceRequired:1b,Tags:["hh_squad_showcase","hh_showcase_fight","hh_showcase_hispanic"]}
summon hostile_humans:human_tier1 ~-34 ~ ~2 {PersonaId:"spreendmc",SquadId:[I;286331153,286331153,286331153,286331153],PersistenceRequired:1b,Tags:["hh_squad_showcase","hh_showcase_fight","hh_showcase_hispanic"]}
summon hostile_humans:human_tier1 ~-34 ~ ~6 {PersonaId:"elrubius",SquadId:[I;286331153,286331153,286331153,286331153],PersistenceRequired:1b,Tags:["hh_squad_showcase","hh_showcase_fight","hh_showcase_hispanic"]}
summon hostile_humans:human_tier1 ~-12 ~ ~-6 {PersonaId:"purpled",SquadId:[I;572662306,572662306,572662306,572662306],PersistenceRequired:1b,Tags:["hh_squad_showcase","hh_showcase_fight","hh_showcase_international"]}
summon hostile_humans:human_tier1 ~-12 ~ ~-2 {PersonaId:"dream",SquadId:[I;572662306,572662306,572662306,572662306],PersistenceRequired:1b,Tags:["hh_squad_showcase","hh_showcase_fight","hh_showcase_international"]}
summon hostile_humans:human_tier1 ~-12 ~ ~2 {PersonaId:"sapnap",SquadId:[I;572662306,572662306,572662306,572662306],PersistenceRequired:1b,Tags:["hh_squad_showcase","hh_showcase_fight","hh_showcase_international"]}
summon hostile_humans:human_tier1 ~-12 ~ ~6 {PersonaId:"tommyinnit",SquadId:[I;572662306,572662306,572662306,572662306],PersistenceRequired:1b,Tags:["hh_squad_showcase","hh_showcase_fight","hh_showcase_international"]}

# Same-faction Humans in a separate squad, isolated behind glass: no instant squad telepathy.
fill ~-38 ~ ~9 ~-29 ~3 ~9 minecraft:tinted_glass
fill ~-38 ~ ~13 ~-29 ~3 ~13 minecraft:tinted_glass
fill ~-38 ~ ~10 ~-38 ~3 ~12 minecraft:tinted_glass
fill ~-29 ~ ~10 ~-29 ~3 ~12 minecraft:tinted_glass
summon hostile_humans:human_tier1 ~-36 ~ ~11 {PersonaId:"shadoune666",SquadId:[I;1145324612,1145324612,1145324612,1145324612],PersistenceRequired:1b,Tags:["hh_squad_showcase","hh_showcase_observer"]}
summon hostile_humans:human_tier1 ~-32 ~ ~11 {PersonaId:"farfadox",SquadId:[I;1145324612,1145324612,1145324612,1145324612],PersistenceRequired:1b,Tags:["hh_squad_showcase","hh_showcase_observer"]}

# -----------------------------------------------------------------------------
# Zone 2: TheFocus starts critical. Antfrost damages him while three healthy
# allies share his squad. TheFocus keeps retreat/heal priority; defenders react.
# -----------------------------------------------------------------------------
summon hostile_humans:human_tier1 ~12 ~ ~ {PersonaId:"thefocus",SquadId:[I;858993459,858993459,858993459,858993459],Health:2.0f,PersistenceRequired:1b,Tags:["hh_squad_showcase","hh_showcase_protect","hh_showcase_wounded"]}
summon hostile_humans:human_tier1 ~17 ~ ~-6 {PersonaId:"goncho",SquadId:[I;858993459,858993459,858993459,858993459],PersistenceRequired:1b,Tags:["hh_squad_showcase","hh_showcase_protect","hh_showcase_defender"]}
summon hostile_humans:human_tier1 ~17 ~ ~ {PersonaId:"killercreeper55",SquadId:[I;858993459,858993459,858993459,858993459],PersistenceRequired:1b,Tags:["hh_squad_showcase","hh_showcase_protect","hh_showcase_defender"]}
summon hostile_humans:human_tier1 ~17 ~ ~6 {PersonaId:"serpias",SquadId:[I;858993459,858993459,858993459,858993459],PersistenceRequired:1b,Tags:["hh_squad_showcase","hh_showcase_protect","hh_showcase_defender"]}
summon hostile_humans:human_tier1 ~31 ~ ~ {PersonaId:"antfrost",PersistenceRequired:1b,Tags:["hh_squad_showcase","hh_showcase_protect","hh_showcase_attacker"]}

# -----------------------------------------------------------------------------
# Zone 3: Three members start together; a fourth begins 24 blocks away but still
# inside the bounded 28-block squad query and should navigate back while idle.
# -----------------------------------------------------------------------------
summon hostile_humans:human_tier1 ~-12 ~ ~20 {PersonaId:"amilcar",SquadId:[I;1431655765,1431655765,1431655765,1431655765],PersistenceRequired:1b,Tags:["hh_squad_showcase","hh_showcase_cohesion"]}
summon hostile_humans:human_tier1 ~-10 ~ ~22 {PersonaId:"aquino",SquadId:[I;1431655765,1431655765,1431655765,1431655765],PersistenceRequired:1b,Tags:["hh_squad_showcase","hh_showcase_cohesion"]}
summon hostile_humans:human_tier1 ~-8 ~ ~20 {PersonaId:"carola",SquadId:[I;1431655765,1431655765,1431655765,1431655765],PersistenceRequired:1b,Tags:["hh_squad_showcase","hh_showcase_cohesion"]}
summon hostile_humans:human_tier1 ~12 ~ ~21 {PersonaId:"arigameplays",SquadId:[I;1431655765,1431655765,1431655765,1431655765],PersistenceRequired:1b,Tags:["hh_squad_showcase","hh_showcase_cohesion","hh_showcase_rejoiner"]}

# Visual markers and short spawn protection.
effect give @e[tag=hh_squad_showcase,type=hostile_humans:human_tier1] minecraft:glowing 300 0 true
effect give @e[tag=hh_showcase_fight] minecraft:resistance 5 4 true

# Trigger the two combat demonstrations after all entities exist.
damage @e[tag=hh_showcase_hispanic,limit=1,sort=nearest] 1 minecraft:mob_attack by @e[tag=hh_showcase_international,limit=1,sort=nearest]
damage @e[tag=hh_showcase_wounded,limit=1] 0.5 minecraft:mob_attack by @e[tag=hh_showcase_attacker,limit=1]

# Move the executor only after all relative arena coordinates have been resolved.
tp @s ~ ~8 ~

tellraw @s {"text":"[HH SHOWCASE] West/red: shared aggro 4v4. Glass pen: same faction, different squad, no instant sharing.","color":"gold"}
tellraw @s {"text":"East/orange: critical ally protection. North/green: isolated member rejoins its idle squad.","color":"yellow"}
tellraw @s {"text":"For deterministic retreat, set run_away_middle_fight_chance=1.0 in hostile_humans.toml before launching the world.","color":"gray"}
tellraw @s {"text":"Reset everything with /function hostile_humans:debug/squad_showcase","color":"aqua"}

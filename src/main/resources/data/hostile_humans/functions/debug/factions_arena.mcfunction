# Large three-faction arena. Run with: /function hostile_humans:debug/factions_arena
# It is idempotent and removes only entities owned by this scenario.
kill @e[tag=hh_factions_arena]

# Create a 51x51 combat floor around the executor's current position.
fill ~-25 ~-1 ~-25 ~25 ~-1 ~25 minecraft:polished_deepslate
fill ~-25 ~ ~-25 ~25 ~8 ~25 minecraft:air
fill ~-25 ~ ~-25 ~25 ~7 ~-25 minecraft:deepslate_bricks
fill ~-25 ~ ~25 ~25 ~7 ~25 minecraft:deepslate_bricks
fill ~-25 ~ ~-24 ~-25 ~7 ~24 minecraft:deepslate_bricks
fill ~25 ~ ~-24 ~25 ~7 ~24 minecraft:deepslate_bricks

# Observation platform above the center.
fill ~-4 ~5 ~-4 ~4 ~5 ~4 minecraft:tinted_glass
setblock ~ ~5 ~ minecraft:air
fill ~-1 ~6 ~-1 ~1 ~7 ~1 minecraft:air
tp @s ~ ~6 ~

# Visual faction markers: Hispanic west, International east, Legends north.
fill ~-22 ~ ~-9 ~-14 ~ ~9 minecraft:red_concrete
fill ~14 ~ ~-9 ~22 ~ ~9 minecraft:blue_concrete
fill ~-9 ~ ~14 ~9 ~ ~22 minecraft:purple_concrete
fill ~-13 ~ ~-13 ~13 ~ ~13 minecraft:smooth_stone

# Low cover keeps the battle readable without isolating factions.
fill ~-7 ~ ~-2 ~-5 ~2 ~2 minecraft:stone_bricks
fill ~5 ~ ~-2 ~7 ~2 ~2 minecraft:stone_bricks
fill ~-2 ~ ~5 ~2 ~2 ~7 minecraft:stone_bricks
fill ~-2 ~ ~-7 ~2 ~2 ~-5 minecraft:stone_bricks

gamerule doMobSpawning false

# HISPANIC_CREATORS: mixed T1-T5 roster on the west side.
summon hostile_humans:human_tier1 ~-19 ~1 ~-6 {PersonaId:"coldified",PersistenceRequired:1b,Tags:["hh_factions_arena","hh_arena_hispanic"]}
summon hostile_humans:human_tier1 ~-19 ~1 ~-2 {PersonaId:"shadoune666",PersistenceRequired:1b,Tags:["hh_factions_arena","hh_arena_hispanic"]}
summon hostile_humans:human_tier1 ~-19 ~1 ~2 {PersonaId:"elrichmc",PersistenceRequired:1b,Tags:["hh_factions_arena","hh_arena_hispanic"]}
summon hostile_humans:human_tier1 ~-19 ~1 ~6 {PersonaId:"elrubius",PersistenceRequired:1b,Tags:["hh_factions_arena","hh_arena_hispanic"]}
summon hostile_humans:human_tier1 ~-15 ~1 ~-4 {PersonaId:"arigameplays",PersistenceRequired:1b,Tags:["hh_factions_arena","hh_arena_hispanic"]}
summon hostile_humans:human_tier1 ~-15 ~1 ~4 {PersonaId:"carola",PersistenceRequired:1b,Tags:["hh_factions_arena","hh_arena_hispanic"]}

# INTERNATIONAL_CREATORS: mixed T1-T5 roster on the east side.
summon hostile_humans:human_tier1 ~19 ~1 ~-6 {PersonaId:"technoblade",PersistenceRequired:1b,Tags:["hh_factions_arena","hh_arena_international"]}
summon hostile_humans:human_tier1 ~19 ~1 ~-2 {PersonaId:"clownpierce",PersistenceRequired:1b,Tags:["hh_factions_arena","hh_arena_international"]}
summon hostile_humans:human_tier1 ~19 ~1 ~2 {PersonaId:"dream",PersistenceRequired:1b,Tags:["hh_factions_arena","hh_arena_international"]}
summon hostile_humans:human_tier1 ~19 ~1 ~6 {PersonaId:"antvenom",PersistenceRequired:1b,Tags:["hh_factions_arena","hh_arena_international"]}
summon hostile_humans:human_tier1 ~15 ~1 ~-4 {PersonaId:"ranboo",PersistenceRequired:1b,Tags:["hh_factions_arena","hh_arena_international"]}
summon hostile_humans:human_tier1 ~15 ~1 ~4 {PersonaId:"skeppy",PersistenceRequired:1b,Tags:["hh_factions_arena","hh_arena_international"]}

# MINECRAFT_LEGENDS: all bundled skins visible from the north camp.
summon hostile_humans:human_tier1 ~-8 ~1 ~19 {PersonaId:"entity303",PersistenceRequired:1b,Tags:["hh_factions_arena","hh_arena_legends"]}
summon hostile_humans:human_tier1 ~-4 ~1 ~19 {PersonaId:"error422",PersistenceRequired:1b,Tags:["hh_factions_arena","hh_arena_legends"]}
summon hostile_humans:human_tier1 ~ ~1 ~19 {PersonaId:"herobrine",PersistenceRequired:1b,Tags:["hh_factions_arena","hh_arena_legends"]}
summon hostile_humans:human_tier1 ~4 ~1 ~19 {PersonaId:"null",PersistenceRequired:1b,Tags:["hh_factions_arena","hh_arena_legends"]}
summon hostile_humans:human_tier1 ~8 ~1 ~19 {PersonaId:"whiteeyes",PersistenceRequired:1b,Tags:["hh_factions_arena","hh_arena_legends"]}
summon hostile_humans:human_tier1 ~-8 ~1 ~15 {PersonaId:"farlandsman",PersistenceRequired:1b,Tags:["hh_factions_arena","hh_arena_legends"]}
summon hostile_humans:human_tier1 ~-4 ~1 ~15 {PersonaId:"giantalex",PersistenceRequired:1b,Tags:["hh_factions_arena","hh_arena_legends"]}
summon hostile_humans:human_tier1 ~ ~1 ~15 {PersonaId:"greensteve",PersistenceRequired:1b,Tags:["hh_factions_arena","hh_arena_legends"]}
summon hostile_humans:human_tier1 ~4 ~1 ~15 {PersonaId:"alex",PersistenceRequired:1b,Tags:["hh_factions_arena","hh_arena_legends"]}
summon hostile_humans:human_tier1 ~8 ~1 ~15 {PersonaId:"steve",PersistenceRequired:1b,Tags:["hh_factions_arena","hh_arena_legends"]}

effect give @e[tag=hh_factions_arena] minecraft:glowing 300 0 true
effect give @e[tag=hh_factions_arena] minecraft:resistance 8 4 true

tellraw @s {"text":"[HH ARENA] West/red: Hispanic allies | East/blue: International allies | North/purple: Legends allies","color":"gold"}
tellraw @s {"text":"All three factions are mutually hostile. The ten bundled legend skins are present.","color":"aqua"}
tellraw @s {"text":"Reset with /function hostile_humans:debug/factions_arena","color":"gray"}

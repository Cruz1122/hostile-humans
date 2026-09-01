# Large, readable sandbox for observing camps, missions, survival and combat.
# Run once in a fresh open area with: /function hostile_humans:debug/mega_scenario
# The three camps are created through the real operator command, while all
# humans, inventories, enemies and terrain are placed by this scenario.

# Remove only entities from an earlier run of this scenario.
kill @e[tag=hh_mega_scenario]

# Freeze the test conditions so the player can observe several in-game minutes.
gamerule doMobSpawning false
gamerule doDaylightCycle false
gamerule mobGriefing true
gamerule keepInventory true
time set noon
weather clear

# Clear a bounded 85x85x18 observation area in safe-sized fill batches.
fill ~-42 ~-1 ~-42 ~42 ~1 ~42 minecraft:air
fill ~-42 ~2 ~-42 ~42 ~4 ~42 minecraft:air
fill ~-42 ~5 ~-42 ~42 ~7 ~42 minecraft:air
fill ~-42 ~8 ~-42 ~42 ~10 ~42 minecraft:air
fill ~-42 ~11 ~-42 ~42 ~13 ~42 minecraft:air
fill ~-42 ~14 ~-42 ~42 ~16 ~42 minecraft:air
fill ~-42 ~-1 ~-42 ~42 ~-1 ~42 minecraft:grass_block

# Roads, a shallow stream and a central ruined settlement.
fill ~-42 ~ ~-1 ~42 ~ ~1 minecraft:gravel
fill ~-1 ~ ~-42 ~1 ~ ~42 minecraft:gravel
fill ~-14 ~ ~14 ~14 ~ ~16 minecraft:water
fill ~-2 ~ ~14 ~2 ~ ~16 minecraft:stone_bricks
fill ~-7 ~ ~-7 ~7 ~2 ~-7 minecraft:stone_bricks
fill ~-7 ~ ~7 ~7 ~2 ~7 minecraft:stone_bricks
fill ~-7 ~ ~-6 ~-7 ~2 ~6 minecraft:stone_bricks
fill ~7 ~ ~-6 ~7 ~2 ~6 minecraft:stone_bricks
fill ~-7 ~ ~-7 ~-4 ~2 ~-7 minecraft:air
fill ~4 ~ ~7 ~7 ~2 ~7 minecraft:air
fill ~-7 ~ ~-1 ~-7 ~1 ~1 minecraft:air
fill ~7 ~ ~-1 ~7 ~1 ~1 minecraft:air
setblock ~4 ~ ~4 minecraft:chest[facing=north]
setblock ~-4 ~ ~4 minecraft:barrel[facing=south]
item replace block ~4 ~ ~4 container.0 with minecraft:iron_ingot 8
item replace block ~4 ~ ~4 container.1 with minecraft:gold_ingot 4
item replace block ~4 ~ ~4 container.2 with minecraft:arrow 32
item replace block ~4 ~ ~4 container.3 with minecraft:cooked_beef 16
item replace block ~-4 ~ ~4 container.0 with minecraft:oak_log 16
item replace block ~-4 ~ ~4 container.1 with minecraft:cobblestone 32

# Northwest quarry and southeast wood line give the survival goals visible work.
fill ~-28 ~ ~16 ~-16 ~ ~25 minecraft:stone
fill ~-28 ~1 ~16 ~-16 ~2 ~25 minecraft:air
setblock ~-24 ~ ~19 minecraft:coal_ore
setblock ~-20 ~ ~21 minecraft:iron_ore
setblock ~-18 ~ ~18 minecraft:coal_ore
fill ~18 ~ ~18 ~28 ~ ~28 minecraft:coarse_dirt
fill ~20 ~ ~20 ~20 ~3 ~20 minecraft:oak_log
fill ~25 ~ ~22 ~25 ~4 ~22 minecraft:oak_log
fill ~21 ~4 ~20 ~19 ~6 ~22 minecraft:oak_leaves
fill ~26 ~5 ~22 ~24 ~7 ~24 minecraft:oak_leaves

# Red Hispanic camp, blue International camp and purple Legends camp markers.
fill ~-35 ~ ~-5 ~-25 ~ ~-5 minecraft:red_wool
fill ~-35 ~ ~5 ~-25 ~ ~5 minecraft:red_wool
fill ~-35 ~ ~-4 ~-35 ~ ~4 minecraft:red_wool
fill ~-25 ~ ~-4 ~-25 ~ ~4 minecraft:red_wool
fill ~25 ~ ~-5 ~35 ~ ~-5 minecraft:blue_wool
fill ~25 ~ ~5 ~35 ~ ~5 minecraft:blue_wool
fill ~25 ~ ~-4 ~25 ~ ~4 minecraft:blue_wool
fill ~35 ~ ~-4 ~35 ~ ~4 minecraft:blue_wool
fill ~-5 ~ ~-39 ~5 ~ ~-39 minecraft:purple_wool
fill ~-5 ~ ~-29 ~5 ~ ~-29 minecraft:purple_wool
fill ~-5 ~ ~-38 ~-5 ~ ~-30 minecraft:purple_wool
fill ~5 ~ ~-38 ~5 ~ ~-30 minecraft:purple_wool

# Hispanic squad: food, wood and basic tools make its camp visibly useful.
summon hostile_humans:human_tier1 ~-33 ~ ~-2 {PersonaId:"coldified",SquadId:[I;10101,10101,10101,10101],PersistenceRequired:1b,HandItems:[{id:"minecraft:stone_sword",Count:1b},{id:"minecraft:shield",Count:1b}],Inventory:[{Slot:20b,id:"minecraft:cooked_beef",Count:2b},{Slot:21b,id:"minecraft:oak_planks",Count:16b}],Tags:["hh_mega_scenario","hh_mega_hispanic"]}
summon hostile_humans:human_tier1 ~-32 ~ ~2 {PersonaId:"shadoune666",SquadId:[I;10101,10101,10101,10101],PersistenceRequired:1b,HandItems:[{id:"minecraft:stone_pickaxe",Count:1b},{}],Inventory:[{Slot:20b,id:"minecraft:cooked_beef",Count:2b},{Slot:21b,id:"minecraft:stick",Count:8b}],Tags:["hh_mega_scenario","hh_mega_hispanic"]}
summon hostile_humans:human_tier1 ~-28 ~ ~-2 {PersonaId:"elrichmc",SquadId:[I;10101,10101,10101,10101],PersistenceRequired:1b,HandItems:[{id:"minecraft:stone_axe",Count:1b},{}],Inventory:[{Slot:20b,id:"minecraft:cooked_beef",Count:2b}],Tags:["hh_mega_scenario","hh_mega_hispanic"]}
summon hostile_humans:human_tier1 ~-27 ~ ~2 {PersonaId:"elrubius",SquadId:[I;10101,10101,10101,10101],PersistenceRequired:1b,HandItems:[{id:"minecraft:bow",Count:1b},{}],Inventory:[{Slot:20b,id:"minecraft:arrow",Count:16b},{Slot:21b,id:"minecraft:cooked_beef",Count:2b}],Tags:["hh_mega_scenario","hh_mega_hispanic"]}

# International squad: iron weapons and arrows create a stronger patrol.
summon hostile_humans:human_tier2 ~27 ~ ~-2 {PersonaId:"technoblade",SquadId:[I;20202,20202,20202,20202],PersistenceRequired:1b,HandItems:[{id:"minecraft:iron_sword",Count:1b},{id:"minecraft:shield",Count:1b}],Inventory:[{Slot:20b,id:"minecraft:cooked_beef",Count:4b}],Tags:["hh_mega_scenario","hh_mega_international"]}
summon hostile_humans:human_tier2 ~28 ~ ~2 {PersonaId:"clownpierce",SquadId:[I;20202,20202,20202,20202],PersistenceRequired:1b,HandItems:[{id:"minecraft:iron_axe",Count:1b},{}],Inventory:[{Slot:20b,id:"minecraft:cooked_beef",Count:4b}],Tags:["hh_mega_scenario","hh_mega_international"]}
summon hostile_humans:human_tier1 ~32 ~ ~-2 {PersonaId:"dream",SquadId:[I;20202,20202,20202,20202],PersistenceRequired:1b,HandItems:[{id:"minecraft:bow",Count:1b},{}],Inventory:[{Slot:20b,id:"minecraft:arrow",Count:24b},{Slot:21b,id:"minecraft:cooked_beef",Count:4b}],Tags:["hh_mega_scenario","hh_mega_international"]}
summon hostile_humans:human_tier1 ~33 ~ ~2 {PersonaId:"antvenom",SquadId:[I;20202,20202,20202,20202],PersistenceRequired:1b,HandItems:[{id:"minecraft:stone_sword",Count:1b},{}],Inventory:[{Slot:20b,id:"minecraft:cooked_beef",Count:4b}],Tags:["hh_mega_scenario","hh_mega_international"]}

# Legends squad: a compact enemy camp with valuable supplies for raids.
summon hostile_humans:human_tier2 ~-2 ~ ~-37 {PersonaId:"entity303",SquadId:[I;30303,30303,30303,30303],PersistenceRequired:1b,HandItems:[{id:"minecraft:iron_sword",Count:1b},{id:"minecraft:shield",Count:1b}],Inventory:[{Slot:20b,id:"minecraft:cooked_beef",Count:3b}],Tags:["hh_mega_scenario","hh_mega_legends"]}
summon hostile_humans:human_tier1 ~2 ~ ~-37 {PersonaId:"error422",SquadId:[I;30303,30303,30303,30303],PersistenceRequired:1b,HandItems:[{id:"minecraft:iron_pickaxe",Count:1b},{}],Inventory:[{Slot:20b,id:"minecraft:cooked_beef",Count:3b}],Tags:["hh_mega_scenario","hh_mega_legends"]}
summon hostile_humans:human_tier2 ~-2 ~ ~-32 {PersonaId:"herobrine",SquadId:[I;30303,30303,30303,30303],PersistenceRequired:1b,HandItems:[{id:"minecraft:iron_axe",Count:1b},{}],Inventory:[{Slot:20b,id:"minecraft:cooked_beef",Count:3b}],Tags:["hh_mega_scenario","hh_mega_legends"]}
summon hostile_humans:human_tier1 ~2 ~ ~-32 {PersonaId:"null",SquadId:[I;30303,30303,30303,30303],PersistenceRequired:1b,HandItems:[{id:"minecraft:bow",Count:1b},{}],Inventory:[{Slot:20b,id:"minecraft:arrow",Count:20b},{Slot:21b,id:"minecraft:cooked_beef",Count:3b}],Tags:["hh_mega_scenario","hh_mega_legends"]}

# Hostile pressure around the settlement and real prey near the wood line.
summon minecraft:zombie ~-10 ~ ~-10 {PersistenceRequired:1b,Tags:["hh_mega_scenario","hh_mega_threat"]}
summon minecraft:zombie ~10 ~ ~-10 {PersistenceRequired:1b,Tags:["hh_mega_scenario","hh_mega_threat"]}
summon minecraft:zombie ~-10 ~ ~10 {PersistenceRequired:1b,Tags:["hh_mega_scenario","hh_mega_threat"]}
summon minecraft:zombie ~10 ~ ~10 {PersistenceRequired:1b,Tags:["hh_mega_scenario","hh_mega_threat"]}
summon minecraft:skeleton ~-13 ~ ~0 {PersistenceRequired:1b,Tags:["hh_mega_scenario","hh_mega_threat"]}
summon minecraft:skeleton ~13 ~ ~0 {PersistenceRequired:1b,Tags:["hh_mega_scenario","hh_mega_threat"]}
summon minecraft:cow ~22 ~ ~25 {PersistenceRequired:1b,Tags:["hh_mega_scenario","hh_mega_prey"]}
summon minecraft:cow ~24 ~ ~25 {PersistenceRequired:1b,Tags:["hh_mega_scenario","hh_mega_prey"]}
summon minecraft:chicken ~26 ~ ~26 {PersistenceRequired:1b,Tags:["hh_mega_scenario","hh_mega_prey"]}
summon minecraft:chicken ~28 ~ ~26 {PersistenceRequired:1b,Tags:["hh_mega_scenario","hh_mega_prey"]}
effect give @e[tag=hh_mega_scenario] minecraft:glowing 1200 0 true

# Create the three real persistent camp records and assign their nearby squads.
execute positioned ~-30 ~ ~ run hostilehumans camp
execute positioned ~30 ~ ~ run hostilehumans camp
execute positioned ~ ~ ~-34 run hostilehumans camp

# Add visible, faction-specific reserves to the real camp chests.
item replace block ~-29 ~ ~ container.0 with minecraft:cooked_beef 24
item replace block ~-29 ~ ~ container.1 with minecraft:oak_planks 32
item replace block ~-29 ~ ~ container.2 with minecraft:coal 16
item replace block ~31 ~ ~ container.0 with minecraft:iron_ingot 16
item replace block ~31 ~ ~ container.1 with minecraft:arrow 64
item replace block ~31 ~ ~ container.2 with minecraft:cooked_beef 24
item replace block ~1 ~ ~-34 container.0 with minecraft:gold_ingot 16
item replace block ~1 ~ ~-34 container.1 with minecraft:diamond 3
item replace block ~1 ~ ~-34 container.2 with minecraft:cooked_beef 24

# Raise an observation tower and enter spectator mode above the whole settlement.
fill ~-3 ~ ~-3 ~3 ~5 ~3 minecraft:oak_planks
fill ~-2 ~6 ~-2 ~2 ~6 ~2 minecraft:tinted_glass
fill ~-1 ~7 ~-1 ~1 ~9 ~1 minecraft:air
tp @s ~ ~10 ~
gamemode spectator @s

tellraw @s {"text":"[HH MEGA] Escenario listo: 3 campamentos, 12 humanos, patrullas hostiles, cantera, bosque, presa y asentamiento central.","color":"gold"}
tellraw @s {"text":"[HH MEGA] Rojo oeste=Hispanic | Azul este=International | Morado norte=Legends | Observas desde la torre central.","color":"aqua"}
tellraw @s {"text":"[HH MEGA] Expedición manual: execute positioned ~-30 ~-10 ~ run hostilehumans expedition", "color":"yellow"}
tellraw @s {"text":"[HH MEGA] Raid manual: execute positioned ~-30 ~-10 ~ run hostilehumans raid", "color":"red"}
tellraw @s {"text":"[HH MEGA] Inspección: hostilehumans inspect @e[tag=hh_mega_scenario,limit=1,sort=nearest] | Estado: /function hostile_humans:debug/mega_scenario_status", "color":"gray"}
tellraw @s {"text":"[HH MEGA] Deja pasar 2-5 minutos para observar agrupación, combate, saqueo, retorno y cooldowns.","color":"green"}

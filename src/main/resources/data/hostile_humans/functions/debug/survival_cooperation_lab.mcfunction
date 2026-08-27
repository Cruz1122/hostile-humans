# Wide cooperation lab: material sharing, claims, station reuse and natural division of work.
# Run with: /function hostile_humans:debug/survival_cooperation_lab
schedule clear hostile_humans:debug/survival_interruptions_start
kill @e[tag=hh_survival_integrated]
kill @e[tag=hh_survival_early]
kill @e[tag=hh_survival_iron]
kill @e[tag=hh_survival_food]
kill @e[tag=hh_survival_ranged]
kill @e[tag=hh_survival_gapple]
gamerule doMobSpawning false
gamerule mobGriefing true
gamerule doDaylightCycle false
time set day

fill ~-34 ~-1 ~-22 ~34 ~-1 ~22 minecraft:smooth_stone
fill ~-34 ~ ~-22 ~34 ~8 ~-8 minecraft:air
fill ~-34 ~ ~-7 ~34 ~8 ~7 minecraft:air
fill ~-34 ~ ~8 ~34 ~8 ~22 minecraft:air
fill ~-34 ~ ~-22 ~34 ~5 ~-22 minecraft:deepslate_bricks
fill ~-34 ~ ~22 ~34 ~5 ~22 minecraft:deepslate_bricks
fill ~-34 ~ ~-21 ~-34 ~5 ~21 minecraft:deepslate_bricks
fill ~34 ~ ~-21 ~34 ~5 ~21 minecraft:deepslate_bricks

# Four open zones with no walls: opportunity, not assigned professions.
# North-west wood zone.
fill ~-27 ~ ~-16 ~-27 ~4 ~-16 minecraft:oak_log
fill ~-30 ~4 ~-19 ~-24 ~6 ~-13 minecraft:oak_leaves
fill ~-20 ~ ~-14 ~-20 ~4 ~-14 minecraft:oak_log
fill ~-23 ~4 ~-17 ~-17 ~6 ~-11 minecraft:oak_leaves
# North-east food/ammo zone.
summon minecraft:chicken ~22 ~ ~-15 {PersistenceRequired:1b,Tags:["hh_survival_integrated","hh_lab_food"]}
summon minecraft:chicken ~27 ~ ~-11 {PersistenceRequired:1b,Tags:["hh_survival_integrated","hh_lab_food"]}
summon minecraft:cow ~19 ~ ~-9 {PersistenceRequired:1b,Tags:["hh_survival_integrated","hh_lab_food"]}
summon minecraft:pig ~28 ~ ~-17 {PersistenceRequired:1b,Tags:["hh_survival_integrated","hh_lab_food"]}
# South-west ore zone: duplicate visible blocks expose resource claims.
fill ~-30 ~ ~9 ~-16 ~2 ~19 minecraft:stone
setblock ~-27 ~1 ~12 minecraft:coal_ore
setblock ~-23 ~1 ~12 minecraft:iron_ore
setblock ~-20 ~1 ~15 minecraft:iron_ore
setblock ~-18 ~1 ~18 minecraft:iron_ore
# South-east ranged/gold opportunity.
fill ~16 ~ ~9 ~30 ~2 ~19 minecraft:stone
setblock ~19 ~1 ~12 minecraft:gravel
setblock ~23 ~1 ~15 minecraft:gold_ore
setblock ~27 ~1 ~17 minecraft:gold_ore

# Shared central workshop: exactly one table and one furnace encourage reuse and furnace claims.
fill ~-4 ~ ~-4 ~4 ~ ~4 minecraft:oak_planks
setblock ~-1 ~1 ~ minecraft:crafting_table
setblock ~1 ~1 ~ minecraft:furnace
setblock ~ ~1 ~2 minecraft:chest{Items:[{Slot:0b,id:"minecraft:string",Count:12b},{Slot:1b,id:"minecraft:flint",Count:8b},{Slot:2b,id:"minecraft:apple",Count:4b}]}

# Materials are intentionally concentrated. Nearby members must transfer physically.
summon hostile_humans:human_tier1 ~-6 ~ ~-2 {PersonaId:"amilcar",SquadId:[I;1819044972,1819044972,1819044972,1819044972],PersistenceRequired:1b,HandItems:[{id:"minecraft:stone_pickaxe",Count:1b},{}],Inventory:[{Slot:20b,id:"minecraft:oak_planks",Count:24b},{Slot:21b,id:"minecraft:stick",Count:12b}],Tags:["hh_survival_integrated","hh_lab_squad","hh_lab_wood_rich"]}
summon hostile_humans:human_tier1 ~-3 ~ ~-5 {PersonaId:"aquino",SquadId:[I;1819044972,1819044972,1819044972,1819044972],PersistenceRequired:1b,HandItems:[{id:"minecraft:stone_pickaxe",Count:1b},{}],Inventory:[{Slot:20b,id:"minecraft:coal",Count:8b}],Tags:["hh_survival_integrated","hh_lab_squad","hh_lab_fuel_rich"]}
summon hostile_humans:human_tier1 ~3 ~ ~-5 {PersonaId:"carola",SquadId:[I;1819044972,1819044972,1819044972,1819044972],PersistenceRequired:1b,HandItems:[{id:"minecraft:stone_sword",Count:1b},{}],Inventory:[{Slot:20b,id:"minecraft:iron_ingot",Count:8b}],Tags:["hh_survival_integrated","hh_lab_squad","hh_lab_iron_rich"]}
summon hostile_humans:human_tier1 ~6 ~ ~-2 {PersonaId:"arigameplays",SquadId:[I;1819044972,1819044972,1819044972,1819044972],PersistenceRequired:1b,HandItems:[{},{}],ArmorItems:[{},{},{},{}],Tags:["hh_survival_integrated","hh_lab_squad","hh_lab_receiver"]}

summon minecraft:text_display ~-24 ~7 ~-14 {text:'{"text":"WOOD OPPORTUNITY","color":"green","bold":true}',billboard:"center",background:1073741824,Tags:["hh_survival_integrated"]}
summon minecraft:text_display ~24 ~7 ~-13 {text:'{"text":"FOOD + FEATHERS","color":"yellow","bold":true}',billboard:"center",background:1073741824,Tags:["hh_survival_integrated"]}
summon minecraft:text_display ~-23 ~7 ~15 {text:'{"text":"CLAIMED ORES","color":"gray","bold":true}',billboard:"center",background:1073741824,Tags:["hh_survival_integrated"]}
summon minecraft:text_display ~ ~7 ~ {text:'{"text":"SHARED WORKSHOP","color":"aqua","bold":true}',billboard:"center",background:1073741824,Tags:["hh_survival_integrated"]}
effect give @e[tag=hh_lab_squad] minecraft:glowing 1800 0 true
tp @s ~ ~9 ~
gamemode spectator @s
tellraw @s {"text":"[HH SURVIVAL] Cooperation lab ready: four needs, concentrated inventories and one reusable workshop.","color":"gold"}
tellraw @s {"text":"Watch for exact nearby sharing, different resource claims, one furnace user at a time and no duplicate stations.","color":"aqua"}

# Broad integrated surface scenario: early progression, hunting, cooking, sharing and exploration.
# Run with: /function hostile_humans:debug/survival_overworld_expedition
schedule clear hostile_humans:debug/survival_interruptions_start
kill @e[tag=hh_survival_integrated]
kill @e[tag=hh_survival_early]
kill @e[tag=hh_survival_iron]
kill @e[tag=hh_survival_food]
kill @e[tag=hh_survival_gapple]
gamerule doMobSpawning false
gamerule mobGriefing true
gamerule doDaylightCycle false
time set day
weather clear

# Reset a 73x49 expedition field in safe slices.
fill ~-36 ~-1 ~-24 ~36 ~-1 ~24 minecraft:grass_block
fill ~-36 ~ ~-24 ~36 ~7 ~-9 minecraft:air
fill ~-36 ~ ~-8 ~36 ~7 ~7 minecraft:air
fill ~-36 ~ ~8 ~36 ~7 ~24 minecraft:air
fill ~-36 ~ ~-24 ~36 ~4 ~-24 minecraft:stone_bricks
fill ~-36 ~ ~24 ~36 ~4 ~24 minecraft:stone_bricks
fill ~-36 ~ ~-23 ~-36 ~4 ~23 minecraft:stone_bricks
fill ~36 ~ ~-23 ~36 ~4 ~23 minecraft:stone_bricks

# West woodland: enough logs for useful progression, not an infinite forest.
fill ~-12 ~ ~-2 ~-12 ~3 ~-2 minecraft:oak_log
fill ~-14 ~3 ~-4 ~-10 ~5 ~ minecraft:oak_leaves
fill ~-29 ~ ~-15 ~-29 ~4 ~-15 minecraft:oak_log
fill ~-32 ~4 ~-18 ~-26 ~6 ~-12 minecraft:oak_leaves
fill ~-23 ~ ~-19 ~-23 ~5 ~-19 minecraft:oak_log
fill ~-26 ~5 ~-22 ~-20 ~7 ~-16 minecraft:oak_leaves
fill ~-27 ~ ~-5 ~-27 ~4 ~-5 minecraft:dark_oak_log
fill ~-30 ~4 ~-8 ~-24 ~6 ~-2 minecraft:dark_oak_leaves

# South quarry and exposed coal. All resource faces remain visible.
fill ~-17 ~ ~12 ~-6 ~2 ~21 minecraft:stone
setblock ~-15 ~1 ~14 minecraft:coal_ore
setblock ~-12 ~1 ~18 minecraft:coal_ore
setblock ~-8 ~1 ~14 minecraft:iron_ore
setblock ~-7 ~1 ~17 minecraft:iron_ore
setblock ~-10 ~2 ~20 minecraft:gravel

# East ridge requires exploration but only normal flat navigation.
fill ~16 ~ ~-18 ~31 ~2 ~-8 minecraft:stone
setblock ~18 ~1 ~-15 minecraft:coal_ore
setblock ~23 ~1 ~-12 minecraft:iron_ore
setblock ~27 ~1 ~-16 minecraft:iron_ore
setblock ~30 ~1 ~-10 minecraft:gold_ore

# Animal meadow: livestock provides food.
summon minecraft:cow ~-2 ~ ~-5 {PersistenceRequired:1b,Tags:["hh_survival_integrated","hh_surface_food"]}
summon minecraft:cow ~3 ~ ~-8 {PersistenceRequired:1b,Tags:["hh_survival_integrated","hh_surface_food"]}
summon minecraft:pig ~5 ~ ~-3 {PersistenceRequired:1b,Tags:["hh_survival_integrated","hh_surface_food"]}
summon minecraft:sheep ~9 ~ ~-7 {PersistenceRequired:1b,Tags:["hh_survival_integrated","hh_surface_food"]}
summon minecraft:chicken ~1 ~ ~-11 {PersistenceRequired:1b,Tags:["hh_survival_integrated","hh_surface_food"]}
summon minecraft:chicken ~7 ~ ~-12 {PersistenceRequired:1b,Tags:["hh_survival_integrated","hh_surface_food"]}

# Four under-equipped squad members. One has a small coal reserve to expose natural sharing.
summon hostile_humans:human_tier1 ~-8 ~ ~ {PersonaId:"coldified",SquadId:[I;1785358954,1785358954,1785358954,1785358954],PersistenceRequired:1b,HandItems:[{},{}],ArmorItems:[{},{},{},{}],Tags:["hh_survival_integrated","hh_surface_squad"]}
summon hostile_humans:human_tier1 ~-5 ~ ~2 {PersonaId:"elrichmc",SquadId:[I;1785358954,1785358954,1785358954,1785358954],PersistenceRequired:1b,HandItems:[{},{}],ArmorItems:[{},{},{},{}],Tags:["hh_survival_integrated","hh_surface_squad"]}
summon hostile_humans:human_tier1 ~-5 ~ ~-2 {PersonaId:"spreendmc",SquadId:[I;1785358954,1785358954,1785358954,1785358954],PersistenceRequired:1b,HandItems:[{},{}],ArmorItems:[{},{},{},{}],Inventory:[{Slot:20b,id:"minecraft:coal",Count:4b}],Tags:["hh_survival_integrated","hh_surface_squad","hh_surface_supplier"]}
summon hostile_humans:human_tier1 ~-2 ~ ~ {PersonaId:"elrubius",SquadId:[I;1785358954,1785358954,1785358954,1785358954],PersistenceRequired:1b,HandItems:[{},{}],ArmorItems:[{},{},{},{}],Tags:["hh_survival_integrated","hh_surface_squad"]}

# Observation platform and labels.
fill ~-5 ~8 ~-4 ~5 ~8 ~4 minecraft:tinted_glass
summon minecraft:text_display ~-25 ~8 ~-12 {text:'{"text":"WOOD + APPLES","color":"green","bold":true}',billboard:"center",background:1073741824,Tags:["hh_survival_integrated"]}
summon minecraft:text_display ~-11 ~7 ~17 {text:'{"text":"STONE / COAL / IRON","color":"gray","bold":true}',billboard:"center",background:1073741824,Tags:["hh_survival_integrated"]}
summon minecraft:text_display ~24 ~7 ~-13 {text:'{"text":"EXPLORATION RIDGE","color":"gold","bold":true}',billboard:"center",background:1073741824,Tags:["hh_survival_integrated"]}
summon minecraft:text_display ~4 ~6 ~-9 {text:'{"text":"FOOD","color":"yellow","bold":true}',billboard:"center",background:1073741824,Tags:["hh_survival_integrated"]}
effect give @e[tag=hh_surface_squad] minecraft:glowing 1800 0 true
tp @s ~ ~9 ~
gamemode spectator @s
tellraw @s {"text":"[HH SURVIVAL] Overworld expedition ready: observe wood -> stone tools -> hunt -> furnace -> iron progression.","color":"gold"}
tellraw @s {"text":"Expected: parallel work, physical drops, supplier sharing, reusable stations, bounded gathering and eastward exploration.","color":"aqua"}
tellraw @s {"text":"Reset with /function hostile_humans:debug/survival_overworld_expedition","color":"gray"}

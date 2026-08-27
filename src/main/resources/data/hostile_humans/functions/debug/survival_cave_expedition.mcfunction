# Integrated underground scenario: cave-height exploration, exposed/hidden ores, tool gating and upgrades.
# Run with: /function hostile_humans:debug/survival_cave_expedition
schedule clear hostile_humans:debug/survival_interruptions_start
kill @e[tag=hh_survival_integrated]
kill @e[tag=hh_survival_early]
kill @e[tag=hh_survival_iron]
kill @e[tag=hh_survival_food]
kill @e[tag=hh_survival_ranged]
kill @e[tag=hh_survival_gapple]
gamerule doMobSpawning false
gamerule mobGriefing true

# Build a sealed 65x31 cave around the executor. Corridors are broad and normally navigable.
fill ~-32 ~-2 ~-15 ~32 ~7 ~15 minecraft:deepslate
fill ~-29 ~-1 ~-5 ~29 ~4 ~5 minecraft:air
fill ~-24 ~-1 ~-12 ~-10 ~4 ~12 minecraft:air
fill ~10 ~-1 ~-12 ~24 ~4 ~12 minecraft:air
fill ~-6 ~-1 ~-13 ~6 ~4 ~13 minecraft:air
fill ~-29 ~-1 ~-2 ~-29 ~3 ~2 minecraft:air
fill ~29 ~-1 ~-2 ~29 ~3 ~2 minecraft:air
fill ~-29 ~-2 ~-5 ~29 ~-2 ~5 minecraft:deepslate
fill ~-24 ~-2 ~-12 ~-10 ~-2 ~12 minecraft:deepslate
fill ~10 ~-2 ~-12 ~24 ~-2 ~12 minecraft:deepslate
fill ~-6 ~-2 ~-13 ~6 ~-2 ~13 minecraft:deepslate

# Early chamber: coal/iron and an existing shared furnace opportunity.
setblock ~-20 ~ ~-8 minecraft:coal_ore
setblock ~-18 ~1 ~-10 minecraft:coal_ore
setblock ~-15 ~ ~-8 minecraft:deepslate_iron_ore
setblock ~-13 ~1 ~-10 minecraft:deepslate_iron_ore
setblock ~-18 ~ ~8 minecraft:gravel
setblock ~-14 ~ ~9 minecraft:furnace
setblock ~-12 ~ ~9 minecraft:crafting_table

# Deep chamber: exposed gold and diamond. Diamond must wait for an iron pickaxe.
setblock ~15 ~ ~-9 minecraft:deepslate_gold_ore
setblock ~18 ~1 ~-10 minecraft:deepslate_gold_ore
setblock ~20 ~ ~8 minecraft:deepslate_diamond_ore
setblock ~22 ~1 ~10 minecraft:deepslate_diamond_ore
setblock ~16 ~ ~10 minecraft:deepslate_iron_ore

# Explicit no-xray controls: fully encased ores beside the navigable central tunnel.
fill ~2 ~-1 ~-1 ~4 ~1 ~1 minecraft:deepslate
setblock ~3 ~ ~ minecraft:deepslate_diamond_ore
fill ~-4 ~-1 ~-1 ~-2 ~1 ~1 minecraft:deepslate
setblock ~-3 ~ ~ minecraft:deepslate_gold_ore

# Squad begins with stone-tier access and concentrated supplies, but no iron gear.
summon hostile_humans:human_tier1 ~-25 ~ ~ {PersonaId:"shadoune666",SquadId:[I;1802201963,1802201963,1802201963,1802201963],PersistenceRequired:1b,HandItems:[{id:"minecraft:stone_pickaxe",Count:1b},{}],ArmorItems:[{},{},{},{}],Inventory:[{Slot:20b,id:"minecraft:cooked_beef",Count:4b}],Tags:["hh_survival_integrated","hh_cave_squad"]}
summon hostile_humans:human_tier1 ~-22 ~ ~2 {PersonaId:"farfadox",SquadId:[I;1802201963,1802201963,1802201963,1802201963],PersistenceRequired:1b,HandItems:[{id:"minecraft:stone_pickaxe",Count:1b},{}],ArmorItems:[{},{},{},{}],Inventory:[{Slot:20b,id:"minecraft:oak_planks",Count:12b},{Slot:21b,id:"minecraft:stick",Count:8b}],Tags:["hh_survival_integrated","hh_cave_squad","hh_cave_wood_supplier"]}
summon hostile_humans:human_tier1 ~-22 ~ ~-2 {PersonaId:"goncho",SquadId:[I;1802201963,1802201963,1802201963,1802201963],PersistenceRequired:1b,HandItems:[{id:"minecraft:stone_sword",Count:1b},{}],ArmorItems:[{},{},{},{}],Inventory:[{Slot:20b,id:"minecraft:coal",Count:6b}],Tags:["hh_survival_integrated","hh_cave_squad","hh_cave_fuel_supplier"]}

# Labels are inside the ceiling so they remain readable in spectator mode.
summon minecraft:text_display ~-17 ~5 ~-9 {text:'{"text":"EARLY: COAL + IRON","color":"yellow","bold":true}',billboard:"center",background:1073741824,Tags:["hh_survival_integrated"]}
summon minecraft:text_display ~ ~5 ~6 {text:'{"text":"HIDDEN ORES: MUST IGNORE","color":"red","bold":true}',billboard:"center",background:1073741824,Tags:["hh_survival_integrated"]}
summon minecraft:text_display ~19 ~5 ~9 {text:'{"text":"DEEP: GOLD + DIAMOND","color":"aqua","bold":true}',billboard:"center",background:1073741824,Tags:["hh_survival_integrated"]}
effect give @e[tag=hh_cave_squad] minecraft:glowing 1800 0 true
tp @s ~ ~6 ~
gamemode spectator @s
tellraw @s {"text":"[HH SURVIVAL] Cave expedition ready: no surface Y targets, exposed-only ores and iron-pick diamond gating.","color":"gold"}
tellraw @s {"text":"Expected: share wood/fuel, mine visible iron, smelt in real time, craft iron pick, then explore the deep chamber.","color":"aqua"}
tellraw @s {"text":"The central encased gold/diamond controls must remain untouched.","color":"red"}

# Run with /function hostile_humans:debug/survival_early
kill @e[tag=hh_survival_early]
gamerule mobGriefing true
gamerule doMobSpawning false
fill ~-12 ~-1 ~-10 ~12 ~-1 ~10 minecraft:grass_block
fill ~-12 ~ ~-10 ~12 ~8 ~10 minecraft:air
fill ~5 ~ ~-2 ~5 ~4 ~-2 minecraft:oak_log
fill ~3 ~4 ~-4 ~7 ~6 ~ minecraft:oak_leaves
fill ~-1 ~ ~4 ~3 ~2 ~6 minecraft:stone
setblock ~1 ~1 ~5 minecraft:coal_ore
summon minecraft:cow ~-5 ~ ~4 {PersistenceRequired:1b,Tags:["hh_survival_early"]}
summon minecraft:chicken ~-3 ~ ~5 {PersistenceRequired:1b,Tags:["hh_survival_early"]}
summon hostile_humans:human_tier1 ~-5 ~ ~-2 {PersonaId:"coldified",SquadId:[I;16909060,16909060,16909060,16909060],PersistenceRequired:1b,HandItems:[{},{}],ArmorItems:[{},{},{},{}],Tags:["hh_survival_early"]}
summon hostile_humans:human_tier1 ~-7 ~ ~ {PersonaId:"elrichmc",SquadId:[I;16909060,16909060,16909060,16909060],PersistenceRequired:1b,HandItems:[{},{}],ArmorItems:[{},{},{},{}],Tags:["hh_survival_early"]}
effect give @e[tag=hh_survival_early,type=hostile_humans:human_tier1] minecraft:glowing 300 0 true
tellraw @s {"text":"[HH] Survival early: observe wood, basic recipes, food and furnace progression.","color":"green"}

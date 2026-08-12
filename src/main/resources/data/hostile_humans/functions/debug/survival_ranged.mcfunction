# Run with /function hostile_humans:debug/survival_ranged
kill @e[tag=hh_survival_ranged]
gamerule mobGriefing true
fill ~-8 ~-1 ~-6 ~8 ~-1 ~6 minecraft:smooth_stone
fill ~-8 ~ ~-6 ~8 ~5 ~6 minecraft:air
setblock ~2 ~ ~ minecraft:crafting_table
summon hostile_humans:human_tier1 ~-3 ~ ~ {PersonaId:"goncho",SquadId:[I;320083222,320083222,320083222,320083222],PersistenceRequired:1b,HandItems:[{id:"minecraft:iron_sword",Count:1b},{}],Inventory:[{Slot:20b,id:"minecraft:string",Count:3b},{Slot:21b,id:"minecraft:oak_planks",Count:8b},{Slot:22b,id:"minecraft:flint",Count:8b},{Slot:23b,id:"minecraft:feather",Count:8b}],Tags:["hh_survival_ranged"]}
effect give @e[tag=hh_survival_ranged] minecraft:glowing 300 0 true
tellraw @s {"text":"[HH] Survival ranged: loaded recipes should produce one bow and a bounded arrow stock.","color":"green"}

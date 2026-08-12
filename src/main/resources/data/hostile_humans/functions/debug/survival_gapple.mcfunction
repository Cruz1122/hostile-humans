# Run with /function hostile_humans:debug/survival_gapple
kill @e[tag=hh_survival_gapple]
gamerule mobGriefing true
fill ~-8 ~-1 ~-6 ~8 ~-1 ~6 minecraft:smooth_stone
fill ~-8 ~ ~-6 ~8 ~5 ~6 minecraft:air
setblock ~2 ~ ~ minecraft:crafting_table
summon hostile_humans:human_tier1 ~-3 ~ ~ {PersonaId:"killercreeper55",SquadId:[I;336926231,336926231,336926231,336926231],PersistenceRequired:1b,HandItems:[{id:"minecraft:diamond_sword",Count:1b},{}],ArmorItems:[{id:"minecraft:diamond_boots",Count:1b},{id:"minecraft:diamond_leggings",Count:1b},{id:"minecraft:diamond_chestplate",Count:1b},{id:"minecraft:diamond_helmet",Count:1b}],Inventory:[{Slot:20b,id:"minecraft:apple",Count:3b},{Slot:21b,id:"minecraft:gold_ingot",Count:24b},{Slot:22b,id:"minecraft:cooked_beef",Count:16b},{Slot:23b,id:"minecraft:diamond_pickaxe",Count:1b},{Slot:24b,id:"minecraft:diamond_axe",Count:1b}],Tags:["hh_survival_gapple"]}
effect give @e[tag=hh_survival_gapple] minecraft:glowing 300 0 true
tellraw @s {"text":"[HH] Survival gapple: craft only the useful stock target and retain excess gold.","color":"green"}

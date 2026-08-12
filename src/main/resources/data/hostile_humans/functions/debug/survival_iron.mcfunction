# Run with /function hostile_humans:debug/survival_iron
kill @e[tag=hh_survival_iron]
gamerule mobGriefing true
fill ~-10 ~-1 ~-8 ~10 ~-1 ~8 minecraft:stone
fill ~-10 ~ ~-8 ~10 ~6 ~8 minecraft:air
setblock ~4 ~ ~ minecraft:iron_ore
setblock ~5 ~ ~ minecraft:iron_ore
setblock ~4 ~1 ~1 minecraft:coal_ore
setblock ~ ~ ~4 minecraft:furnace
summon hostile_humans:human_tier1 ~-5 ~ ~ {PersonaId:"spreendmc",SquadId:[I;286397204,286397204,286397204,286397204],PersistenceRequired:1b,HandItems:[{id:"minecraft:stone_pickaxe",Count:1b},{}],Inventory:[{Slot:20b,id:"minecraft:cobblestone",Count:8b}],Tags:["hh_survival_iron"]}
effect give @e[tag=hh_survival_iron] minecraft:glowing 300 0 true
tellraw @s {"text":"[HH] Survival iron: mine exposed iron, smelt in the real furnace, then upgrade gear.","color":"green"}

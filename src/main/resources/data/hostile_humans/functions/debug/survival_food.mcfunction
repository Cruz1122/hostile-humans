# Run with /function hostile_humans:debug/survival_food
kill @e[tag=hh_survival_food]
gamerule mobGriefing true
fill ~-10 ~-1 ~-8 ~10 ~-1 ~8 minecraft:grass_block
fill ~-10 ~ ~-8 ~10 ~5 ~8 minecraft:air
setblock ~ ~ ~4 minecraft:furnace
summon minecraft:cow ~4 ~ ~ {PersistenceRequired:1b,Tags:["hh_survival_food"]}
summon minecraft:pig ~5 ~ ~2 {PersistenceRequired:1b,Tags:["hh_survival_food"]}
summon minecraft:chicken ~3 ~ ~-2 {PersistenceRequired:1b,Tags:["hh_survival_food"]}
summon hostile_humans:human_tier1 ~-5 ~ ~ {PersonaId:"elrubius",SquadId:[I;303240213,303240213,303240213,303240213],PersistenceRequired:1b,Inventory:[{Slot:20b,id:"minecraft:coal",Count:2b}],Tags:["hh_survival_food"]}
effect give @e[tag=hh_survival_food] minecraft:glowing 300 0 true
tellraw @s {"text":"[HH] Survival food: hunt, collect vanilla drops, cook and retrieve food.","color":"green"}

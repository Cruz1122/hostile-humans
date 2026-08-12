# Minimal hidden chest scenario. Re-running it cleans only this scenario.
kill @e[tag=hh_chest_hidden]
gamerule doMobSpawning false
time set day
fill ~-3 ~ ~-3 ~8 ~4 ~5 minecraft:air
fill ~-3 ~-1 ~-3 ~8 ~-1 ~5 minecraft:polished_deepslate
setblock ~4 ~ ~ minecraft:chest
item replace block ~4 ~ ~ container.0 with minecraft:diamond_sword
fill ~3 ~ ~-1 ~3 ~2 ~1 minecraft:stone
summon hostile_humans:human_tier1 ~1 ~ ~ {PersistenceRequired:1b,CustomName:'{"text":"HH_CHEST_HIDDEN"}',CustomNameVisible:1b,Tags:["hh_chest_hidden"]}
effect give @e[tag=hh_chest_hidden] minecraft:glowing 300 0 true
tellraw @s {"text":"[HH CHEST] Hidden chest ready: the stone wall should prevent detection.","color":"yellow"}

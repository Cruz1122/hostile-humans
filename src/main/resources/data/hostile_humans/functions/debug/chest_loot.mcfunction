# Minimal visible chest-looting scenario. Re-running it cleans only this scenario.
kill @e[tag=hh_chest_loot]
gamerule doMobSpawning false
time set day
fill ~-3 ~ ~-3 ~8 ~4 ~5 minecraft:air
fill ~-3 ~-1 ~-3 ~8 ~-1 ~5 minecraft:polished_deepslate
setblock ~3 ~ ~ minecraft:chest
item replace block ~3 ~ ~ container.0 with minecraft:diamond_sword
item replace block ~3 ~ ~ container.1 with minecraft:bread 8
item replace block ~3 ~ ~ container.2 with minecraft:cobblestone 16
item replace block ~3 ~ ~ container.3 with minecraft:iron_ingot
summon hostile_humans:human_tier1 ~1 ~ ~ {PersistenceRequired:1b,CustomName:'{"text":"HH_CHEST_LOOT"}',CustomNameVisible:1b,Tags:["hh_chest_loot"]}
effect give @e[tag=hh_chest_loot] minecraft:glowing 300 0 true
tellraw @s {"text":"[HH CHEST] Visible chest ready: useful items should move, iron ingot should remain.","color":"green"}

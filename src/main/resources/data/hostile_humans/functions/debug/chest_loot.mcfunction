# Open, spacious visible chest-looting scenario. Re-running it cleans the arena.
kill @e[tag=hh_chest_loot]
gamerule doMobSpawning false
time set day
fill ~-12 ~ ~-12 ~12 ~8 ~12 minecraft:air
fill ~-12 ~-1 ~-12 ~12 ~-1 ~12 minecraft:polished_deepslate
setblock ~5 ~ ~ minecraft:chest
item replace block ~5 ~ ~ container.0 with minecraft:diamond_sword
item replace block ~5 ~ ~ container.1 with minecraft:bread 8
item replace block ~5 ~ ~ container.2 with minecraft:cobblestone 16
item replace block ~5 ~ ~ container.3 with minecraft:iron_ingot
item replace block ~5 ~ ~ container.4 with minecraft:diamond_chestplate
item replace block ~5 ~ ~ container.5 with minecraft:diamond_leggings
item replace block ~5 ~ ~ container.6 with minecraft:diamond_boots
item replace block ~5 ~ ~ container.7 with minecraft:diamond_helmet
item replace block ~5 ~ ~ container.8 with minecraft:shield
item replace block ~5 ~ ~ container.9 with minecraft:totem_of_undying
summon hostile_humans:human_tier1 ~-1 ~ ~ {PersistenceRequired:1b,CustomName:'{"text":"HH_CHEST_LOOT"}',CustomNameVisible:1b,Tags:["hh_chest_loot"]}
effect give @e[tag=hh_chest_loot] minecraft:glowing 300 0 true
team remove hh_chest_debug
team add hh_chest_debug
team modify hh_chest_debug friendlyFire false
team join hh_chest_debug @s
team join hh_chest_debug @e[tag=hh_chest_loot]
tellraw @s {"text":"[HH CHEST] Watch the glowing human see the chest, walk to it, open it for a few seconds, and take the useful stacks one by one.","color":"green"}
tellraw @s {"text":"[HH CHEST] Weapons, food, blocks, armor, shield and totem should leave gradually; the human should equip the armor. The iron ingot should remain.","color":"yellow"}

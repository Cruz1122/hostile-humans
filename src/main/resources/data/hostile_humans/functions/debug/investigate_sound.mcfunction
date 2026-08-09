# Reset only the debug human used by this scenario.
kill @e[tag=hh_debug]

# Build two small rooms separated by a solid wall with a side passage.
tp @s 8 81 2 270 0
fill -2 80 -4 10 80 4 minecraft:polished_deepslate
fill -2 81 -4 10 84 4 minecraft:air
fill 4 81 -4 4 83 4 minecraft:stone
fill 4 81 4 4 83 4 minecraft:air
setblock 4 81 2 minecraft:gold_block
gamerule doMobSpawning false

# Spawn one human in the opposite room. The wall blocks direct sight of the player.
summon hostile_humans:human_tier1 0 81 -2 {PersistenceRequired:1b,CustomName:'{"text":"HH_SOUND_DEBUG"}',CustomNameVisible:1b,Tags:["hh_debug"]}
effect give @e[tag=hh_debug] minecraft:glowing 300 0 true
give @s minecraft:diamond_pickaxe 1

# The player breaks the gold block at 4 81 2 to produce the investigation stimulus.
tellraw @s {"text":"[HH SOUND DEBUG] Rompe el bloque de oro de la pared; el NPC debe investigar el sonido","color":"yellow"}
execute if entity @e[tag=hh_debug,limit=1] unless entity @e[tag=hh_debug,limit=2] run tellraw @s {"text":"[HH SOUND DEBUG] Escenario listo: un humano hh_debug","color":"green"}
execute unless entity @e[tag=hh_debug,limit=1] run tellraw @s {"text":"[HH SOUND DEBUG] No se encontró el humano","color":"red"}
execute if entity @e[tag=hh_debug,limit=2] run tellraw @s {"text":"[HH SOUND DEBUG] Hay varias entidades hh_debug","color":"red"}

# Reset the isolated debug area and build a four-door house.
kill @e[tag=hh_debug]
gamerule doMobSpawning false
time set day
weather clear
tp @s 2 81 -13 0 0
execute positioned 0 81 0 run kill @e[type=!minecraft:player,distance=..24]

# Clear and floor a large observation area.
fill -13 81 -13 13 86 13 minecraft:air
fill -13 80 -13 13 80 13 minecraft:polished_deepslate

# Build a sealed house around the NPC.
fill -6 81 -6 6 84 -6 minecraft:stone
fill -6 81 6 6 84 6 minecraft:stone
fill -6 81 -6 -6 84 6 minecraft:stone
fill 6 81 -6 6 84 6 minecraft:stone
fill -6 85 -6 6 85 6 minecraft:stone

# Four closed doors: north, south, west and east.
setblock 0 81 -6 minecraft:spruce_door[facing=north,half=lower,hinge=left,open=false,powered=false]
setblock 0 82 -6 minecraft:spruce_door[facing=north,half=upper,hinge=left,open=false,powered=false]
setblock 0 81 6 minecraft:spruce_door[facing=south,half=lower,hinge=left,open=false,powered=false]
setblock 0 82 6 minecraft:spruce_door[facing=south,half=upper,hinge=left,open=false,powered=false]
setblock -6 81 0 minecraft:spruce_door[facing=west,half=lower,hinge=left,open=false,powered=false]
setblock -6 82 0 minecraft:spruce_door[facing=west,half=upper,hinge=left,open=false,powered=false]
setblock 6 81 0 minecraft:spruce_door[facing=east,half=lower,hinge=left,open=false,powered=false]
setblock 6 82 0 minecraft:spruce_door[facing=east,half=upper,hinge=left,open=false,powered=false]

# Break one gold block outside the house; do not break a door.
setblock 2 81 -11 minecraft:gold_block
setblock -2 81 11 minecraft:gold_block
setblock -11 81 -2 minecraft:gold_block
setblock 11 81 2 minecraft:gold_block

summon hostile_humans:human_tier1 0 81 0 {PersistenceRequired:1b,CustomName:'{"text":"HH_SOUND_DEBUG"}',CustomNameVisible:1b,Tags:["hh_debug"]}
effect give @e[tag=hh_debug] minecraft:glowing 300 0 true
give @s minecraft:diamond_pickaxe 1

tellraw @s {"text":"[HH SOUND DEBUG] Rompe UN BLOQUE DE ORO exterior, no una puerta.","color":"yellow","bold":true}
tellraw @s {"text":"[HH SOUND DEBUG] El NPC debe abrir y cruzar la puerta de esa dirección.","color":"yellow"}
tellraw @s {"text":"Norte: 2 81 -11 | Sur: -2 81 11 | Oeste: -11 81 -2 | Este: 11 81 2","color":"gray"}
execute if entity @e[tag=hh_debug,limit=1] unless entity @e[tag=hh_debug,limit=2] run tellraw @s {"text":"[HH SOUND DEBUG] Escenario listo: un humano a 11 bloques de cada estímulo","color":"green"}
execute unless entity @e[tag=hh_debug,limit=1] run tellraw @s {"text":"[HH SOUND DEBUG] No se encontró el humano","color":"red"}

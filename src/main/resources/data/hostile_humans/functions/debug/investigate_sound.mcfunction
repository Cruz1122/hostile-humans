# Reset the debug human and build a four-door house.
kill @e[tag=hh_debug]
gamerule doMobSpawning false
tp @s 0 81 -7 0 0

fill -6 80 -6 6 80 6 minecraft:polished_deepslate
fill -5 81 -5 5 84 5 minecraft:air
fill -4 81 -4 4 83 -4 minecraft:stone
fill -4 81 4 4 83 4 minecraft:stone
fill -4 81 -4 -4 83 4 minecraft:stone
fill 4 81 -4 4 83 4 minecraft:stone
fill -4 84 -4 4 84 4 minecraft:stone

# Four closed doors: north, south, west and east.
setblock 0 81 -4 minecraft:oak_door[facing=north,half=lower,hinge=left,open=false,powered=false]
setblock 0 82 -4 minecraft:oak_door[facing=north,half=upper,hinge=left,open=false,powered=false]
setblock 0 81 4 minecraft:oak_door[facing=south,half=lower,hinge=left,open=false,powered=false]
setblock 0 82 4 minecraft:oak_door[facing=south,half=upper,hinge=left,open=false,powered=false]
setblock -4 81 0 minecraft:oak_door[facing=west,half=lower,hinge=left,open=false,powered=false]
setblock -4 82 0 minecraft:oak_door[facing=west,half=upper,hinge=left,open=false,powered=false]
setblock 4 81 0 minecraft:oak_door[facing=east,half=lower,hinge=left,open=false,powered=false]
setblock 4 82 0 minecraft:oak_door[facing=east,half=upper,hinge=left,open=false,powered=false]

summon hostile_humans:human_tier1 0 81 0 {PersistenceRequired:1b,CustomName:'{"text":"HH_SOUND_DEBUG"}',CustomNameVisible:1b,Tags:["hh_debug"]}
effect give @e[tag=hh_debug] minecraft:glowing 300 0 true
give @s minecraft:diamond_axe 1

tellraw @s {"text":"[HH SOUND DEBUG] Casa lista: 4 puertas cerradas (norte, sur, este, oeste). Rompe una puerta; el NPC debe investigar esa dirección.","color":"yellow"}
tellraw @s {"text":"[HH SOUND DEBUG] Norte: z=-4 | Sur: z=4 | Oeste: x=-4 | Este: x=4","color":"gray"}
execute if entity @e[tag=hh_debug,limit=1] unless entity @e[tag=hh_debug,limit=2] run tellraw @s {"text":"[HH SOUND DEBUG] Escenario listo: un humano hh_debug dentro de la casa","color":"green"}
execute unless entity @e[tag=hh_debug,limit=1] run tellraw @s {"text":"[HH SOUND DEBUG] No se encontró el humano","color":"red"}

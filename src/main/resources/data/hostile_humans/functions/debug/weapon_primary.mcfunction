# Deterministic primary-melee inspection: sword must win over a picked-up tool.
kill @e[tag=hh_weapon_primary]
gamerule doMobSpawning false
time set day
fill ~-3 ~ ~-3 ~3 ~4 ~3 minecraft:air
fill ~-3 ~-1 ~-3 ~3 ~-1 ~3 minecraft:polished_deepslate
summon hostile_humans:human_tier1 ~ ~ ~ {PersistenceRequired:1b,CustomName:'{"text":"HH_WEAPON_PRIMARY"}',CustomNameVisible:1b,Tags:["hh_weapon_primary"]}
item replace entity @e[tag=hh_weapon_primary,limit=1] weapon.mainhand with minecraft:iron_sword
summon minecraft:item ~1 ~ ~ {Item:{id:"minecraft:iron_pickaxe",Count:1b},Tags:["hh_weapon_primary"]}
effect give @e[tag=hh_weapon_primary] minecraft:glowing 300 0 true
tellraw @s {"text":"[HH WEAPON] Primaria preparada: la espada debe conservar la mano principal frente al pico.","color":"green"}

# Complete manual arena for the tactical equipment feature.
# Run this function from a creative player in a test world.
kill @e[tag=hh_full_debug]
gamerule doMobSpawning false
gamerule mobGriefing true
time set day
tp @s -24 80 0 90 0
fill -24 80 -6 24 85 6 minecraft:air
fill -24 79 -2 24 79 2 minecraft:polished_deepslate

# Four isolated stations. Rebuilding the arena clears every block previously
# placed by the cobweb test before the walls and floor are recreated.
fill -24 80 -2 24 84 -2 minecraft:glass
fill -24 80 2 24 84 2 minecraft:glass
fill -21 80 -2 -21 84 2 minecraft:glass
fill -15 80 -2 -15 84 2 minecraft:glass
fill -9 80 -2 -9 84 2 minecraft:glass
fill -3 80 -2 -3 84 2 minecraft:glass
fill 3 80 -2 3 84 2 minecraft:glass
fill 9 80 -2 9 84 2 minecraft:glass
fill 15 80 -2 15 84 2 minecraft:glass
fill 21 80 -2 21 84 2 minecraft:glass

# Station 1: a primary sword must remain equipped when a tool is nearby.
summon hostile_humans:human_tier1 -18 80 0 {PersistenceRequired:1b,CustomName:'{"text":"HH_PRIMARY"}',CustomNameVisible:1b,Tags:["hh_full_debug","hh_full_primary"]}
item replace entity @e[tag=hh_full_primary,limit=1] weapon.mainhand with minecraft:iron_sword
summon minecraft:item -17 80 0 {Item:{id:"minecraft:iron_pickaxe",Count:1b},Tags:["hh_full_debug","hh_full_primary"]}

# Station 2: a fallback tool must be equipped when no sword or axe exists.
summon hostile_humans:human_tier1 -12 80 0 {PersistenceRequired:1b,CustomName:'{"text":"HH_FALLBACK"}',CustomNameVisible:1b,Tags:["hh_full_debug","hh_full_fallback"]}
item replace entity @e[tag=hh_full_fallback,limit=1] weapon.mainhand with minecraft:air
summon minecraft:item -11 80 0 {Item:{id:"minecraft:iron_pickaxe",Count:1b},Tags:["hh_full_debug","hh_full_fallback"]}

# Station 3: picking up a primary weapon must upgrade a fallback tool.
summon hostile_humans:human_tier1 -6 80 0 {PersistenceRequired:1b,CustomName:'{"text":"HH_UPGRADE"}',CustomNameVisible:1b,Tags:["hh_full_debug","hh_full_upgrade"]}
item replace entity @e[tag=hh_full_upgrade,limit=1] weapon.mainhand with minecraft:iron_pickaxe
summon minecraft:item -5 80 0 {Item:{id:"minecraft:diamond_sword",Count:1b},Tags:["hh_full_debug","hh_full_upgrade"]}

# Station 4: damage the human or let the zombie approach to exercise retreat webs.
summon hostile_humans:human_tier1 0 80 0 {Health:2.0f,PersistenceRequired:1b,CustomName:'{"text":"HH_COBWEB"}',CustomNameVisible:1b,Tags:["hh_full_debug","hh_full_cobweb"]}
summon minecraft:item 1 80 0 {Item:{id:"minecraft:cobweb",Count:3b},Tags:["hh_full_debug","hh_full_cobweb"]}
summon minecraft:zombie 2 80 0 {NoAI:1b,PersistenceRequired:1b,Tags:["hh_full_debug","hh_full_cobweb"]}

effect give @e[tag=hh_full_debug,type=hostile_humans:human_tier1] minecraft:glowing 600 0 true
effect give @e[tag=hh_full_debug,type=minecraft:zombie] minecraft:glowing 600 0 true
tellraw @s {"text":"[HH DEBUG] Arena táctica creada: estaciones separadas en X=-18,-12,-6,0.","color":"green"}
tellraw @s {"text":"[1] PRIMARY: sword > pickaxe | [2] FALLBACK: pickaxe | [3] UPGRADE: diamond sword | [4] COBWEB: retreat, cooldown y quota.","color":"yellow"}
tellraw @s {"text":"Teletranspórtate a -18, -12, -6 o 0 para probar cada cámara individualmente. En la 4, el humano empieza con poca vida.","color":"gray"}
tellraw @s {"text":"Para forzar la retirada, ataca HH_COBWEB estando dentro de su cámara; el zombie queda quieto como amenaza visible.","color":"gray"}
tellraw @s {"text":"Para una prueba determinista, usa run_away_middle_fight_chance=1.0 en hostile_humans-common.toml y reinicia el servidor.","color":"gold"}
tellraw @s {"text":"Para reiniciar: function hostile_humans:debug/all_tactical_equipment", "color":"aqua"}

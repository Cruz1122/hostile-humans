# Manual in-game arena for tactical ender-pearl and water-bucket behavior.
# Run from an empty test area as a creative operator. Re-running this function
# removes only tagged entities, but rebuilds the bounded arena volume.
execute if entity @e[tag=hh_utility_arena,limit=1] run tp @s ~ ~-10 ~16
kill @e[tag=hh_utility_arena]
gamerule doMobSpawning false
gamerule mobGriefing true
time set day

# Clear and rebuild one bounded arena around the command executor.
fill ~-24 ~-1 ~-10 ~24 ~22 ~22 minecraft:air
fill ~-24 ~-1 ~-10 ~24 ~-1 ~22 minecraft:polished_deepslate
fill ~-24 ~ ~-10 ~24 ~6 ~-10 minecraft:glass
fill ~-24 ~ ~22 ~24 ~6 ~22 minecraft:glass
fill ~-24 ~ ~-10 ~-24 ~6 ~22 minecraft:glass
fill ~24 ~ ~-10 ~24 ~6 ~22 minecraft:glass

# Water rescue room: x=-22..-8, z=-8..8.
fill ~-22 ~ ~-8 ~-22 ~6 ~8 minecraft:glass
fill ~-8 ~ ~-8 ~-8 ~6 ~8 minecraft:glass
fill ~-22 ~ ~-8 ~-8 ~6 ~-8 minecraft:glass
fill ~-22 ~ ~8 ~-8 ~6 ~8 minecraft:glass
fill ~-22 ~6 ~-8 ~-8 ~6 ~8 minecraft:glass

# Offensive pearl room: x=2..22, z=-8..8.
fill ~2 ~ ~-8 ~2 ~6 ~8 minecraft:glass
fill ~22 ~ ~-8 ~22 ~6 ~8 minecraft:glass
fill ~2 ~ ~-8 ~22 ~6 ~-8 minecraft:glass
fill ~2 ~ ~8 ~22 ~6 ~8 minecraft:glass
fill ~2 ~6 ~-8 ~22 ~6 ~8 minecraft:glass

# Defensive pearl room: x=2..22, z=10..20.
fill ~2 ~ ~10 ~2 ~6 ~20 minecraft:glass
fill ~22 ~ ~10 ~22 ~6 ~20 minecraft:glass
fill ~2 ~ ~10 ~22 ~6 ~10 minecraft:glass
fill ~2 ~ ~20 ~22 ~6 ~20 minecraft:glass
fill ~2 ~6 ~10 ~22 ~6 ~20 minecraft:glass

# Informational signs are inside the rooms so the station purpose remains
# visible while observing from the raised viewing position.
setblock ~-21 ~ ~-7 minecraft:oak_wall_sign[facing=south]{front_text:{messages:['{"text":"WATER RESCUE"}','{"text":"fire + bucket"}','{"text":"watch placement"}','{"text":"then recovery"}']}}
setblock ~3 ~ ~-7 minecraft:oak_wall_sign[facing=south]{front_text:{messages:['{"text":"OFFENSIVE PEARL"}','{"text":"12 blocks"}','{"text":"one target"}','{"text":"one throw"}']}}
setblock ~3 ~ ~11 minecraft:oak_wall_sign[facing=south]{front_text:{messages:['{"text":"DEFENSIVE PEARL"}','{"text":"low health"}','{"text":"threat nearby"}','{"text":"watch escape"}']}}

# Station 1: the Human starts burning with a water bucket in its off hand.
# It should place water beside itself, clear the fire, wait for the recovery
# delay, then remove only the source that it placed.
summon hostile_humans:human_tier1 ~-15 ~ ~0 {NoAI:1b,Fire:100s,PersistenceRequired:1b,Glowing:1b,CustomName:'{"text":"HH_WATER_RESCUE"}',CustomNameVisible:1b,Tags:["hh_utility_arena","hh_utility_water"]}
item replace entity @e[tag=hh_utility_water,limit=1] weapon.offhand with minecraft:water_bucket
setblock ~-14 ~ ~0 minecraft:fire

# Station 2: the Human has a visible hostile target at twelve blocks, inside
# the offensive range. The target is frozen and invulnerable for observation.
summon hostile_humans:human_tier1 ~6 ~ ~-1 {PersistenceRequired:1b,Glowing:1b,CustomName:'{"text":"HH_OFFENSIVE_PEARL"}',CustomNameVisible:1b,Tags:["hh_utility_arena","hh_utility_offensive"]}
item replace entity @e[tag=hh_utility_offensive,limit=1] weapon.offhand with minecraft:ender_pearl 2
summon minecraft:zombie ~18 ~ ~-1 {PersistenceRequired:1b,Silent:1b,Glowing:1b,CustomName:'{"text":"PEARL_TARGET"}',CustomNameVisible:1b,Attributes:[{Name:"generic.max_health",Base:100.0d},{Name:"generic.movement_speed",Base:0.0d},{Name:"generic.attack_damage",Base:0.0d}],Health:100.0f,Tags:["hh_utility_arena","hh_utility_offensive_target"]}

# Station 3: the Human is deliberately low-health and has a close threat on
# its right. Its defensive destination is the open space to the left.
summon hostile_humans:human_tier1 ~14 ~ ~15 {Health:8.0f,PersistenceRequired:1b,Glowing:1b,CustomName:'{"text":"HH_DEFENSIVE_PEARL"}',CustomNameVisible:1b,Attributes:[{Name:"generic.max_health",Base:40.0d}],Tags:["hh_utility_arena","hh_utility_defensive"]}
item replace entity @e[tag=hh_utility_defensive,limit=1] weapon.offhand with minecraft:ender_pearl
summon minecraft:zombie ~15 ~ ~15 {PersistenceRequired:1b,Silent:1b,Glowing:1b,CustomName:'{"text":"DEFENSIVE_THREAT"}',CustomNameVisible:1b,Attributes:[{Name:"generic.movement_speed",Base:0.0d},{Name:"generic.attack_damage",Base:1.0d},{Name:"generic.max_health",Base:100.0d}],Health:100.0f,Tags:["hh_utility_arena","hh_utility_defensive_target"]}

# Human spawn initialization may assign a persona name during summon. Apply
# the station labels after all spawn-time initialization has completed.
data merge entity @e[tag=hh_utility_water,limit=1] {CustomName:'{"text":"HH_WATER_RESCUE"}',CustomNameVisible:1b,Glowing:1b}
data merge entity @e[tag=hh_utility_offensive,limit=1] {CustomName:'{"text":"HH_OFFENSIVE_PEARL"}',CustomNameVisible:1b,Glowing:1b}
data merge entity @e[tag=hh_utility_defensive,limit=1] {CustomName:'{"text":"HH_DEFENSIVE_PEARL"}',CustomNameVisible:1b,Glowing:1b}

# Move only the executing player to an elevated viewpoint outside the arena.
tp @s ~ ~10 ~-16 0 30
effect give @s minecraft:night_vision 1200 0 true
tellraw @s {"text":"[HH UTILITY] Arena creada. Mira desde arriba: WATER a la izquierda, OFFENSIVE al frente y DEFENSIVE arriba/derecha.","color":"green"}
tellraw @s {"text":"[WATER] HH_WATER_RESCUE debe apagar el fuego, convertir la cubeta en bucket, y recuperar únicamente su fuente después de unos 2 segundos.","color":"aqua"}
tellraw @s {"text":"[OFFENSIVE] HH_OFFENSIVE_PEARL tiene 2 perlas y un objetivo a 12 bloques: debe lanzar una y quedar con 1.","color":"yellow"}
tellraw @s {"text":"[DEFENSIVE] HH_DEFENSIVE_PEARL empieza con poca vida y una amenaza a la derecha: debe lanzar una perla alejándose hacia la izquierda y activar cooldown.","color":"gold"}
tellraw @s {"text":"Observa los cambios de posición y las manos. Para inspeccionar inventario: /hostilehumans inspect @e[tag=hh_utility_water,limit=1]", "color":"gray"}
tellraw @s {"text":"Para reiniciar: vuelve a ejecutar /function hostile_humans:debug/tactical_utility_arena. Usa un área de pruebas vacía: la arena reconstruye su volumen delimitado.","color":"red"}

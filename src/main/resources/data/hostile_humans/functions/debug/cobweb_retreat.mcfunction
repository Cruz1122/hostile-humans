# Small retreat corridor. Only scenario-owned entities are removed.
kill @e[tag=hh_cobweb_debug]
gamerule doMobSpawning false
gamerule mobGriefing true
time set day
fill ~-4 ~ ~-2 ~4 ~3 ~2 minecraft:air
fill ~-4 ~-1 ~-2 ~4 ~-1 ~2 minecraft:stone
fill ~-4 ~ ~-2 ~-4 ~2 ~2 minecraft:stone
fill ~4 ~ ~-2 ~4 ~2 ~2 minecraft:stone
summon hostile_humans:human_tier1 ~ ~ ~ {PersistenceRequired:1b,CustomName:'{"text":"HH_COBWEB_RETREAT"}',CustomNameVisible:1b,Tags:["hh_cobweb_debug"]}
summon minecraft:item ~1 ~ ~ {Item:{id:"minecraft:cobweb",Count:3b},Tags:["hh_cobweb_debug"]}
summon minecraft:creeper ~-2 ~ ~ {PersistenceRequired:1b,NoAI:1b,Tags:["hh_cobweb_debug"]}
effect give @e[tag=hh_cobweb_debug,type=hostile_humans:human_tier1] minecraft:glowing 300 0 true
tellraw @s {"text":"[HH COBWEB] Corredor listo: baja la vida del humano o acerca un perseguidor y observa retirada, consumo, cooldown y límite.","color":"yellow"}
tellraw @s {"text":"[HH COBWEB] Repite la función para limpiar únicamente este escenario.","color":"gray"}

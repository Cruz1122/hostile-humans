# Remove only entities owned by this scenario.
kill @e[tag=hh_debug]

# Prepare a small platform and move only the command executor.
tp @s -12 80 0 90 0
fill 10 80 -2 14 84 2 minecraft:air
fill 10 79 -2 14 79 2 minecraft:polished_deepslate
gamerule doMobSpawning false

# Give the player the minimum equipment needed to inspect the entity.
give @s minecraft:stone_sword 1
give @s minecraft:shield 1

# Spawn exactly one tier-one human and mark it as scenario-owned.
summon hostile_humans:human_tier1 12 80 0 {PersistenceRequired:1b,CustomName:'{"text":"HH_DEBUG"}',CustomNameVisible:1b,Tags:["hh_debug"]}
effect give @e[tag=hh_debug] minecraft:glowing 300 0 true

# Report success only when the restrictive selector finds exactly one entity.
execute if entity @e[tag=hh_debug,limit=1] unless entity @e[tag=hh_debug,limit=2] run tellraw @s {"text":"[HH DEBUG] Exactamente un humano hh_debug","color":"green"}
execute unless entity @e[tag=hh_debug,limit=1] run tellraw @s {"text":"[HH DEBUG] No se encontró ningún humano hh_debug","color":"red"}
execute if entity @e[tag=hh_debug,limit=2] run tellraw @s {"text":"[HH DEBUG] Se encontraron varias entidades hh_debug","color":"red"}

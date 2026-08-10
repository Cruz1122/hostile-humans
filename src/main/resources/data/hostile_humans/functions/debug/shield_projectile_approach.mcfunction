kill @e[tag=hh_projectile_debug]
gamerule doMobSpawning false
summon hostile_humans:human_tier1 ~ ~ ~ {PersistenceRequired:1b,CustomName:'{"text":"HH_PROJECTILE"}',CustomNameVisible:1b,Tags:["hh_projectile_debug"]}
item replace entity @e[tag=hh_projectile_debug,type=hostile_humans:human_tier1,limit=1] weapon.offhand with minecraft:shield
effect give @e[tag=hh_projectile_debug] minecraft:glowing 1200 0 true
tellraw @s {"text":"[HH DEBUG] Projectile scenario ready. Fire from the front, then from behind or away from the human.","color":"gold"}

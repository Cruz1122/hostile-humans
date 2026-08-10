kill @e[tag=hh_shield_low_health]
gamerule doMobSpawning false
summon hostile_humans:human_tier1 ~ ~ ~ {Health:3.0f,PersistenceRequired:1b,CustomName:'{"text":"HH_SHIELD_LOW_HEALTH"}',CustomNameVisible:1b,Tags:["hh_shield_low_health"]}
item replace entity @e[tag=hh_shield_low_health,type=hostile_humans:human_tier1,limit=1] weapon.offhand with minecraft:shield
effect give @e[tag=hh_shield_low_health] minecraft:glowing 1200 0 true
tellraw @s {"text":"[HH DEBUG] Low-health scenario: the human should prioritize retreat/healing over shield tactics.","color":"gold"}

# Observable shield duel: the human can only react to the visible player.
kill @e[tag=hh_shield_debug]
gamerule doMobSpawning false
gamerule mobGriefing true
time set day
fill ~-6 ~ ~-3 ~6 ~4 ~3 minecraft:air
fill ~-6 ~-1 ~-3 ~6 ~-1 ~3 minecraft:polished_deepslate
summon hostile_humans:human_tier1 ~ ~ ~ {PersistenceRequired:1b,CustomName:'{"text":"HH_SHIELD"}',CustomNameVisible:1b,Tags:["hh_shield_debug"]}
item replace entity @e[tag=hh_shield_debug,type=hostile_humans:human_tier1,limit=1] weapon.mainhand with minecraft:iron_sword
item replace entity @e[tag=hh_shield_debug,type=hostile_humans:human_tier1,limit=1] weapon.offhand with minecraft:shield
effect give @e[tag=hh_shield_debug] minecraft:glowing 1200 0 true
give @s minecraft:iron_sword 1
give @s minecraft:iron_axe 1
tellraw @s {"text":"[HH DEBUG] Shield duel ready. Show sword/axe only when visible; use a wall to test no-omniscience.","color":"gold"}

kill @e[tag=hh_mine_debug]
gamerule doMobSpawning false
gamerule mobGriefing true
fill ~-3 ~ ~-2 ~6 ~4 ~2 minecraft:air
fill ~-3 ~-1 ~-2 ~6 ~-1 ~2 minecraft:stone
fill ~2 ~ ~-2 ~2 ~2 ~2 minecraft:oak_planks
summon hostile_humans:human_tier1 ~ ~ ~ {PersistenceRequired:1b,CustomName:'{"text":"HH_MINE"}',CustomNameVisible:1b,Tags:["hh_mine_debug"]}
item replace entity @e[tag=hh_mine_debug,limit=1] weapon.mainhand with minecraft:iron_pickaxe
effect give @e[tag=hh_mine_debug] minecraft:glowing 600 0 true
tellraw @s {"text":"[HH NAV] Mining arena lista: pared fina, pico real y navegación al otro lado.","color":"green"}

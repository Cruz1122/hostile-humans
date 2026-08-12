kill @e[tag=hh_bridge_debug]
gamerule doMobSpawning false
gamerule mobGriefing true
fill ~-3 ~ ~-2 ~8 ~4 ~2 minecraft:air
fill ~-3 ~-1 ~-2 ~1 ~-1 ~2 minecraft:stone
fill ~5 ~-1 ~-2 ~8 ~-1 ~2 minecraft:stone
summon hostile_humans:human_tier1 ~ ~ ~ {PersistenceRequired:1b,CustomName:'{"text":"HH_BRIDGE"}',CustomNameVisible:1b,Tags:["hh_bridge_debug"]}
item replace entity @e[tag=hh_bridge_debug,limit=1] inventory.0 with minecraft:cobblestone 6
effect give @e[tag=hh_bridge_debug] minecraft:glowing 600 0 true
tellraw @s {"text":"[HH NAV] Bridge arena lista: hueco corto y landing segura preparados.","color":"green"}

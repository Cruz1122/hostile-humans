kill @e[tag=hh_mine_tools_debug]
gamerule doMobSpawning false
gamerule mobGriefing true
fill ~-3 ~ ~-2 ~6 ~4 ~2 minecraft:air
fill ~-3 ~-1 ~-2 ~6 ~-1 ~2 minecraft:stone
fill ~2 ~ ~-2 ~2 ~2 ~2 minecraft:stone
summon hostile_humans:human_tier1 ~ ~ ~ {PersistenceRequired:1b,CustomName:'{"text":"HH_MINING_TOOLS"}',CustomNameVisible:1b,Tags:["hh_mine_tools_debug"]}
item replace entity @e[tag=hh_mine_tools_debug,limit=1] weapon.mainhand with minecraft:iron_pickaxe
item replace entity @e[tag=hh_mine_tools_debug,limit=1] inventory.0 with minecraft:iron_shovel
effect give @e[tag=hh_mine_tools_debug] minecraft:glowing 600 0 true
tellraw @s {"text":"[HH NAV] Tool selection lista: observa la herramienta efectiva durante la rotura.","color":"green"}

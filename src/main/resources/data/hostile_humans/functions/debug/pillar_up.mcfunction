kill @e[tag=hh_pillar_debug]
gamerule doMobSpawning false
gamerule mobGriefing true
fill ~-4 ~ ~-3 ~8 ~6 ~3 minecraft:air
fill ~-4 ~-1 ~-3 ~8 ~-1 ~3 minecraft:stone
fill ~4 ~2 ~-3 ~8 ~2 ~3 minecraft:stone
summon hostile_humans:human_tier1 ~ ~ ~ {PersistenceRequired:1b,CustomName:'{"text":"HH_PILLAR"}',CustomNameVisible:1b,Tags:["hh_pillar_debug"]}
item replace entity @e[tag=hh_pillar_debug,limit=1] inventory.0 with minecraft:cobblestone 6
effect give @e[tag=hh_pillar_debug] minecraft:glowing 600 0 true
tellraw @s {"text":"[HH NAV] Pillar arena lista: el NPC tiene seis bloques y una plataforma elevada.","color":"green"}

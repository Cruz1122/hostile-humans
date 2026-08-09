# Upgrade inspection: a fallback tool is replaced after the sword is collected.
kill @e[tag=hh_weapon_upgrade]
gamerule doMobSpawning false
time set day
fill ~-3 ~ ~-3 ~3 ~4 ~3 minecraft:air
fill ~-3 ~-1 ~-3 ~3 ~-1 ~3 minecraft:polished_deepslate
summon hostile_humans:human_tier1 ~ ~ ~ {PersistenceRequired:1b,CustomName:'{"text":"HH_WEAPON_UPGRADE"}',CustomNameVisible:1b,Tags:["hh_weapon_upgrade"]}
item replace entity @e[tag=hh_weapon_upgrade,limit=1] weapon.mainhand with minecraft:iron_pickaxe
summon minecraft:item ~1 ~ ~ {Item:{id:"minecraft:diamond_sword",Count:1b},Tags:["hh_weapon_upgrade"]}
effect give @e[tag=hh_weapon_upgrade] minecraft:glowing 300 0 true
tellraw @s {"text":"[HH WEAPON] Upgrade preparado: recoge la espada y observa la reevaluación sin oscilaciones.","color":"green"}

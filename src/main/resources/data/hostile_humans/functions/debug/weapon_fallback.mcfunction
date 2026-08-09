# Deterministic fallback inspection: no sword or axe is present.
kill @e[tag=hh_weapon_fallback]
gamerule doMobSpawning false
time set day
fill ~-3 ~ ~-3 ~3 ~4 ~3 minecraft:air
fill ~-3 ~-1 ~-3 ~3 ~-1 ~3 minecraft:polished_deepslate
summon hostile_humans:human_tier1 ~ ~ ~ {PersistenceRequired:1b,CustomName:'{"text":"HH_WEAPON_FALLBACK"}',CustomNameVisible:1b,Tags:["hh_weapon_fallback"]}
item replace entity @e[tag=hh_weapon_fallback,limit=1] weapon.mainhand with minecraft:air
summon minecraft:item ~1 ~ ~ {Item:{id:"minecraft:iron_pickaxe",Count:1b},Tags:["hh_weapon_fallback"]}
effect give @e[tag=hh_weapon_fallback] minecraft:glowing 300 0 true
tellraw @s {"text":"[HH WEAPON] Fallback preparado: el NPC debe recoger y equipar el pico.","color":"green"}

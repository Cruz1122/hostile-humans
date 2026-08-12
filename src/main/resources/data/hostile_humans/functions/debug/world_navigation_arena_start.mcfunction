# Open the bridge gap and add one persistent target to each isolated lane.
# Remove leftovers defensively in case this function was invoked manually.
kill @e[tag=hh_world_nav_debug,type=minecraft:zombie]
execute at @e[type=minecraft:marker,tag=hh_world_nav_anchor,limit=1] run fill ~-1 ~ ~ ~-1 ~2 ~ minecraft:air
execute at @e[type=minecraft:marker,tag=hh_world_nav_anchor,limit=1] run summon minecraft:zombie ~-2 ~3 ~-12 {PersistenceRequired:1b,NoAI:1b,NoGravity:1b,Invulnerable:1b,Silent:1b,Tags:["hh_world_nav_debug","hh_pillar_target"]}
execute at @e[type=minecraft:marker,tag=hh_world_nav_anchor,limit=1] run summon minecraft:zombie ~3 ~ ~ {PersistenceRequired:1b,NoAI:1b,NoGravity:1b,Invulnerable:1b,Silent:1b,Tags:["hh_world_nav_debug","hh_bridge_target"]}
execute at @e[type=minecraft:marker,tag=hh_world_nav_anchor,limit=1] run summon minecraft:zombie ~1 ~ ~12 {PersistenceRequired:1b,NoAI:1b,NoGravity:1b,Invulnerable:1b,Silent:1b,Tags:["hh_world_nav_debug","hh_mine_target"]}
effect give @e[type=minecraft:zombie,tag=hh_world_nav_debug] minecraft:glowing 1200 0 true
tellraw @a {"text":"[HH NAV] Suministros recogidos; los tres casos ya están activos.","color":"green"}

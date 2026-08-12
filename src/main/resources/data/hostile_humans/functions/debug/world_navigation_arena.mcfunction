# Integrated manual arena for pillaring, bridging and progressive obstacle mining.
# Run with: /function hostile_humans:debug/world_navigation_arena
# This reset is intentionally idempotent: it also cancels delayed runs from a
# previous arena so old targets cannot be spawned into the new one.
schedule clear hostile_humans:debug/world_navigation_arena_wait
schedule clear hostile_humans:debug/world_navigation_arena_start
kill @e[tag=hh_world_nav_debug]
gamerule doMobSpawning false
gamerule mobGriefing true

# Clear the build area before creating the anchor. The previous order erased
# the marker itself, leaving the delayed activation with no origin.
fill ~-12 ~-1 ~-15 ~8 ~6 ~15 minecraft:air
# Keep one stable origin for the delayed activation function.
summon minecraft:marker ~ ~ ~ {Tags:["hh_world_nav_debug","hh_world_nav_anchor"]}

# Pillar lane (z=-12): a closed corridor ending at a three-block-high ledge.
fill ~-10 ~-1 ~-13 ~0 ~-1 ~-11 minecraft:stone
fill ~-10 ~ ~-13 ~0 ~5 ~-13 minecraft:bedrock
fill ~-10 ~ ~-11 ~0 ~5 ~-11 minecraft:bedrock
fill ~-10 ~ ~-12 ~-10 ~5 ~-12 minecraft:bedrock
fill ~-3 ~ ~-12 ~0 ~2 ~-12 minecraft:stone
summon hostile_humans:human_tier1 ~-7 ~ ~-12 {PersistenceRequired:1b,CustomName:'{"text":"HH_NAV_PILLAR"}',CustomNameVisible:1b,Tags:["hh_world_nav_debug","hh_pillar_debug"]}
summon minecraft:item ~-7 ~ ~-12 {PickupDelay:0s,Item:{id:"minecraft:cobblestone",Count:6b},Tags:["hh_world_nav_debug","hh_nav_supply"]}

# Bridge lane (z=0): a temporary barrier wall keeps the human beside a real three-block gap.
fill ~-8 ~-1 ~-1 ~-2 ~-1 ~1 minecraft:stone
fill ~2 ~-1 ~-1 ~6 ~-1 ~1 minecraft:stone
fill ~-1 ~ ~ ~-1 ~2 ~ minecraft:barrier
fill ~-8 ~ ~-1 ~6 ~3 ~-1 minecraft:bedrock
fill ~-8 ~ ~1 ~6 ~3 ~1 minecraft:bedrock
fill ~-8 ~ ~ ~-8 ~3 ~ minecraft:bedrock
fill ~6 ~ ~ ~6 ~3 ~ minecraft:bedrock
summon hostile_humans:human_tier1 ~-2 ~ ~ {PersistenceRequired:1b,CustomName:'{"text":"HH_NAV_BRIDGE"}',CustomNameVisible:1b,Tags:["hh_world_nav_debug","hh_bridge_debug"]}
summon minecraft:item ~-2 ~ ~ {PickupDelay:0s,Item:{id:"minecraft:cobblestone",Count:6b},Tags:["hh_world_nav_debug","hh_nav_supply"]}

# Mining lane (z=12): a two-block-high breakable wall in a sealed corridor.
fill ~-8 ~-1 ~11 ~4 ~-1 ~13 minecraft:stone
fill ~-8 ~ ~11 ~4 ~3 ~11 minecraft:bedrock
fill ~-8 ~ ~13 ~4 ~3 ~13 minecraft:bedrock
fill ~-8 ~ ~12 ~-8 ~3 ~12 minecraft:bedrock
fill ~4 ~ ~12 ~4 ~3 ~12 minecraft:bedrock
fill ~-1 ~ ~12 ~-1 ~1 ~12 minecraft:stone
summon hostile_humans:human_tier1 ~-2 ~ ~12 {PersistenceRequired:1b,CustomName:'{"text":"HH_NAV_MINE"}',CustomNameVisible:1b,Tags:["hh_world_nav_debug","hh_mine_debug"]}
item replace entity @e[type=hostile_humans:human_tier1,tag=hh_mine_debug,limit=1,sort=nearest] weapon.mainhand with minecraft:iron_pickaxe

effect give @e[type=hostile_humans:human_tier1,tag=hh_world_nav_debug] minecraft:glowing 1200 0 true
# PickUpLoot runs every 30 ticks; activate after that window without waiting
# forever for a stale or unreachable item entity.
schedule function hostile_humans:debug/world_navigation_arena_start 50t replace
tp @s ~-1 ~4 ~-18 0 20
gamemode spectator @s
tellraw @s {"text":"[HH NAV] Arena preparada; esperando que los humanos recojan sus suministros...","color":"yellow"}
tellraw @s {"text":"Izquierda al fondo: pillar | Centro: bridge | Derecha al fondo: mine","color":"gray"}
tellraw @s {"text":"Reset: /function hostile_humans:debug/world_navigation_arena","color":"gray"}

# Reproducible end-to-end survival progression laboratory.
# Run in-game with:
#   /function hostile_humans:debug/survival_full_flow
#
# One empty human must bootstrap itself from wood through diamond gear. The
# arena also provides food, fuel, and a real furnace/crafting table. Keep this
# scenario separate from the
# focused iron lab when diagnosing one specific failure.

schedule clear hostile_humans:debug/survival_interruptions_start
kill @e[tag=hh_survival_full_flow]
kill @e[tag=hh_survival_progression_lab]
# Killing the previous worker drops its equipped gear without preserving the
# worker tag. Clear those stale drops before spawning the new empty worker.
kill @e[type=minecraft:item,distance=..16]
gamerule doMobSpawning false
gamerule mobGriefing true
gamerule doDaylightCycle false
time set day
weather clear

# Flat bounded arena, centred on the command executor.
fill ~-12 ~-1 ~-12 ~12 ~-1 ~12 minecraft:grass_block
fill ~-12 ~ ~-12 ~12 ~8 ~12 minecraft:air
fill ~-12 ~ ~-12 ~12 ~4 ~-12 minecraft:deepslate_bricks
fill ~-12 ~ ~12 ~12 ~4 ~12 minecraft:deepslate_bricks
fill ~-12 ~ ~-11 ~-12 ~4 ~11 minecraft:deepslate_bricks
fill ~12 ~ ~-11 ~12 ~4 ~11 minecraft:deepslate_bricks

# Real stations: the human must walk to them and use their inventories.
setblock ~0 ~ ~2 minecraft:crafting_table
setblock ~1 ~ ~2 minecraft:furnace

# Woodland bootstrap: six exposed logs, with leaves as an optional apple
# source. No tools or wood are placed in the human's inventory.
fill ~-9 ~ ~-1 ~-9 ~5 ~-1 minecraft:oak_log
fill ~-11 ~4 ~-3 ~-7 ~6 ~1 minecraft:oak_leaves

# Stone stage: enough exposed stone for a pickaxe, axe and sword.
fill ~-6 ~ ~4 ~-3 ~1 ~6 minecraft:stone

# Fuel and iron are separate targets so the scanner must gather both before
# the furnace can produce the iron gear gate.
setblock ~3 ~1 ~5 minecraft:coal_ore
setblock ~4 ~1 ~5 minecraft:coal_ore
setblock ~6 ~1 ~4 minecraft:iron_ore
setblock ~7 ~1 ~4 minecraft:iron_ore
setblock ~8 ~1 ~4 minecraft:iron_ore
setblock ~9 ~1 ~4 minecraft:iron_ore

# Gold and diamond stages. The diamond patch is deliberately large enough for
# the current one-human policy: diamond pickaxe/axe/sword plus armor.
fill ~6 ~ ~-9 ~11 ~1 ~-6 minecraft:stone
setblock ~6 ~ ~-9 minecraft:gold_ore
setblock ~7 ~ ~-9 minecraft:gold_ore
setblock ~8 ~ ~-9 minecraft:gold_ore
setblock ~9 ~ ~-9 minecraft:gold_ore
setblock ~10 ~ ~-9 minecraft:gold_ore
setblock ~11 ~ ~-9 minecraft:gold_ore
setblock ~6 ~1 ~-9 minecraft:gold_ore
setblock ~7 ~1 ~-9 minecraft:gold_ore
fill ~6 ~ ~-7 ~11 ~1 ~-4 minecraft:diamond_ore
fill ~6 ~2 ~-7 ~11 ~2 ~-4 minecraft:diamond_ore
fill ~6 ~3 ~-7 ~11 ~3 ~-4 minecraft:diamond_ore

# Hunting targets.
summon minecraft:cow ~-4 ~ ~-6 {PersistenceRequired:1b,Tags:["hh_survival_full_flow","hh_full_flow_food"]}
summon minecraft:pig ~-2 ~ ~-7 {PersistenceRequired:1b,Tags:["hh_survival_full_flow","hh_full_flow_food"]}
summon minecraft:chicken ~-6 ~ ~-7 {PersistenceRequired:1b,Tags:["hh_survival_full_flow","hh_full_flow_food"]}

# The debug tag disables generated combat loadouts, enables persistent
# SurvivalTrace logs, and shows the live survival state over the worker.
summon hostile_humans:human_tier1 ~0 ~ ~0 {PersistenceRequired:1b,Tags:["hh_survival_full_flow","hh_full_flow_worker","hh_survival_debug"]}
effect give @e[tag=hh_full_flow_worker] minecraft:glowing 7200 0 true

# Observation platform. The worker's custom name is the live checkpoint.
fill ~-4 ~8 ~-4 ~4 ~8 ~4 minecraft:tinted_glass

tp @s ~ ~10 ~
gamemode spectator @s
tellraw @s {"text":"[HH SURVIVAL] Full-flow lab ready: one empty human, complete bootstrap to diamond gear.","color":"gold"}
tellraw @s {"text":"Observe: wood/crafting -> stone tools -> food/fuel/iron -> furnace -> iron pickaxe + shield -> diamond gear.","color":"aqua"}
tellraw @s {"text":"Technical trace: run/logs/latest.log (search for [SurvivalTrace]).","color":"yellow"}
tellraw @s {"text":"Optional combat interruption: /function hostile_humans:debug/survival_full_flow_wave", "color":"red"}
tellraw @s {"text":"Reset: /function hostile_humans:debug/survival_full_flow", "color":"gray"}

# Reproducible end-to-end survival progression laboratory.
# Run in-game with:
#   /function hostile_humans:debug/survival_full_flow
#
# One empty human must bootstrap itself from wood through diamond gear. The
# arena also provides food, fuel, a real furnace/crafting table, loot pickup,
# bow materials, and a gravel target. Keep this scenario separate from the
# focused iron lab when diagnosing one specific failure.

schedule clear hostile_humans:debug/survival_interruptions_start
kill @e[tag=hh_survival_full_flow]
kill @e[tag=hh_survival_progression_lab]
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
fill ~-9 ~ ~-1 ~-9 ~5 ~-1 minecraft:oak_log

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

# Hunting and secondary resource targets.
setblock ~-5 ~1 ~-7 minecraft:gravel
summon minecraft:cow ~-4 ~ ~-6 {PersistenceRequired:1b,Tags:["hh_survival_full_flow","hh_full_flow_food"]}
summon minecraft:pig ~-2 ~ ~-7 {PersistenceRequired:1b,Tags:["hh_survival_full_flow","hh_full_flow_food"]}
summon minecraft:chicken ~-6 ~ ~-7 {PersistenceRequired:1b,Tags:["hh_survival_full_flow","hh_full_flow_food"]}

# Physical drops exercise the normal loot pickup path. They are deliberately
# not inserted into HumanData by summon NBT.
summon minecraft:item ~0 ~0.2 ~0 {Item:{id:"minecraft:apple",Count:1b},PickupDelay:0s,Tags:["hh_survival_full_flow"]}
summon minecraft:item ~0 ~0.2 ~0 {Item:{id:"minecraft:string",Count:3b},PickupDelay:0s,Tags:["hh_survival_full_flow"]}
summon minecraft:item ~0 ~0.2 ~0 {Item:{id:"minecraft:feather",Count:6b},PickupDelay:0s,Tags:["hh_survival_full_flow"]}
summon minecraft:item ~0 ~0.2 ~0 {Item:{id:"minecraft:flint",Count:6b},PickupDelay:0s,Tags:["hh_survival_full_flow"]}

summon hostile_humans:human_tier1 ~0 ~ ~0 {PersonaId:"elrichmc",PersistenceRequired:1b,Tags:["hh_survival_full_flow","hh_full_flow_worker"]}
effect give @e[tag=hh_full_flow_worker] minecraft:glowing 7200 0 true

# Observation platform and explicit checkpoints.
fill ~-4 ~8 ~-4 ~4 ~8 ~4 minecraft:tinted_glass
summon minecraft:text_display ~-9 ~7 ~-1 {text:'{"text":"1 WOOD -> BASIC TOOLS","color":"green","bold":true}',billboard:"center",background:1073741824,Tags:["hh_survival_full_flow"]}
summon minecraft:text_display ~-5 ~4 ~5 {text:'{"text":"2 STONE + COAL + IRON","color":"gray","bold":true}',billboard:"center",background:1073741824,Tags:["hh_survival_full_flow"]}
summon minecraft:text_display ~0 ~4 ~2 {text:'{"text":"3 FURNACE + CRAFTING","color":"aqua","bold":true}',billboard:"center",background:1073741824,Tags:["hh_survival_full_flow"]}
summon minecraft:text_display ~9 ~5 ~-7 {text:'{"text":"4 GOLD + DIAMOND GEAR","color":"aqua","bold":true}',billboard:"center",background:1073741824,Tags:["hh_survival_full_flow"]}
summon minecraft:text_display ~-4 ~4 ~-7 {text:'{"text":"FOOD / LOOT / BOW MATERIALS","color":"yellow","bold":true}',billboard:"center",background:1073741824,Tags:["hh_survival_full_flow"]}

tp @s ~ ~10 ~
gamemode spectator @s
tellraw @s {"text":"[HH SURVIVAL] Full-flow lab ready: one empty human, complete bootstrap to diamond gear.","color":"gold"}
tellraw @s {"text":"Observe: loot pickup -> wood/crafting -> stone tools -> food/fuel/iron -> furnace -> iron pickaxe + shield -> diamond gear.","color":"aqua"}
tellraw @s {"text":"Optional combat interruption: /function hostile_humans:debug/survival_full_flow_wave", "color":"red"}
tellraw @s {"text":"Reset: /function hostile_humans:debug/survival_full_flow", "color":"gray"}

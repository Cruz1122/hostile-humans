# Isolated iron-progression lab.
# Run in-game with:
#   /function hostile_humans:debug/survival_progression_lab
#
# This deliberately contains ONE human. Do not add the stone/diamond stages
# until this iron path is observable and correct.

schedule clear hostile_humans:debug/survival_interruptions_start
kill @e[tag=hh_survival_progression_lab]
gamerule doMobSpawning false
gamerule mobGriefing true
gamerule doDaylightCycle false
time set day

# Flat, unobstructed test cell. The human starts at (0, 0, 0) relative to the
# command executor; every relevant target is within normal interaction range.
fill ~-8 ~-1 ~-6 ~8 ~-1 ~6 minecraft:smooth_stone
fill ~-8 ~ ~-6 ~8 ~5 ~6 minecraft:air
fill ~-8 ~ ~-6 ~8 ~ ~-6 minecraft:deepslate_bricks
fill ~-8 ~ ~6 ~8 ~ ~6 minecraft:deepslate_bricks
fill ~-8 ~ ~-6 ~-8 ~ ~6 minecraft:deepslate_bricks
fill ~8 ~ ~-6 ~8 ~ ~6 minecraft:deepslate_bricks

# The only workstation. It is beside the spawn point and must be used for
# smelting raw iron before the iron pickaxe and shield can be crafted.
setblock ~-2 ~ ~1 minecraft:crafting_table
setblock ~-1 ~ ~1 minecraft:furnace

# Four hidden iron ores. The deepslate wall is the blocker: direct x-ray
# should select only the ore blocks, never mine a tunnel through this wall.
fill ~2 ~ ~-1 ~2 ~3 ~1 minecraft:deepslate
setblock ~2 ~1 ~-1 minecraft:deepslate_iron_ore
setblock ~2 ~1 ~0 minecraft:deepslate_iron_ore
setblock ~2 ~1 ~1 minecraft:deepslate_iron_ore
setblock ~2 ~2 ~0 minecraft:deepslate_iron_ore

# Fuel target: visible coal ore, intentionally separate from the deepslate
# wall. After raw iron is collected, the fuel deficit makes this the next
# mining target before the furnace operation starts.
setblock ~1 ~1 ~-2 minecraft:coal_ore

# Initial state:
# - stone pickaxe, axe and sword: no stone-stage work should be needed;
# - no iron ingots, pickaxe or shield: exactly four iron ingots are required;
# - dropped sticks/planks/tools/food are collected immediately after spawn;
# - coal must be mined from the visible coal ore above, then used as furnace fuel.
summon hostile_humans:human_tier1 ~0 ~ ~0 {PersonaId:"elrichmc",PersistenceRequired:1b,HandItems:[{id:"minecraft:stone_pickaxe",Count:1b},{}],Tags:["hh_survival_progression_lab","hh_lab_iron_stage"]}
summon minecraft:item ~0 ~0.2 ~0 {Item:{id:"minecraft:stone_axe",Count:1b},PickupDelay:0s,Tags:["hh_survival_progression_lab"]}
summon minecraft:item ~0 ~0.2 ~0 {Item:{id:"minecraft:stone_sword",Count:1b},PickupDelay:0s,Tags:["hh_survival_progression_lab"]}
summon minecraft:item ~0 ~0.2 ~0 {Item:{id:"minecraft:stick",Count:16b},PickupDelay:0s,Tags:["hh_survival_progression_lab"]}
summon minecraft:item ~0 ~0.2 ~0 {Item:{id:"minecraft:oak_planks",Count:16b},PickupDelay:0s,Tags:["hh_survival_progression_lab"]}
summon minecraft:item ~0 ~0.2 ~0 {Item:{id:"minecraft:cooked_beef",Count:8b},PickupDelay:0s,Tags:["hh_survival_progression_lab"]}
effect give @e[tag=hh_lab_iron_stage] minecraft:glowing 3600 0 true

tp @s ~0 ~6 ~-5
gamemode spectator @s
tellraw @s {"text":"[HH SURVIVAL] One-human iron test ready.","color":"gold"}
tellraw @s {"text":"Expected order: mine exactly four hidden iron ores -> insert raw iron + coal into the nearby furnace -> retrieve ingots -> craft iron pickaxe and shield.","color":"aqua"}
tellraw @s {"text":"Wrong result: axe on ore, surrounding deepslate chosen before available ore, or no crafting after smelting.","color":"red"}

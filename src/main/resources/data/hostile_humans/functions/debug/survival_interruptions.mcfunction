# Integrated preemption scenario: progression interrupted by combat/retreat, then resumed.
# Run with: /function hostile_humans:debug/survival_interruptions
schedule clear hostile_humans:debug/survival_interruptions_start
kill @e[tag=hh_survival_integrated]
kill @e[tag=hh_survival_early]
kill @e[tag=hh_survival_iron]
kill @e[tag=hh_survival_food]
kill @e[tag=hh_survival_gapple]
gamerule doMobSpawning false
gamerule mobGriefing true
gamerule doDaylightCycle false
time set day

fill ~-30 ~-1 ~-15 ~30 ~-1 ~15 minecraft:grass_block
fill ~-30 ~ ~-15 ~30 ~7 ~15 minecraft:air
fill ~-30 ~ ~-15 ~30 ~5 ~-15 minecraft:bedrock
fill ~-30 ~ ~15 ~30 ~5 ~15 minecraft:bedrock
fill ~-30 ~ ~-14 ~-30 ~5 ~14 minecraft:bedrock
fill ~30 ~ ~-14 ~30 ~5 ~14 minecraft:bedrock

# Progression resources around the squad.
fill ~-20 ~ ~-9 ~-20 ~4 ~-9 minecraft:oak_log
fill ~-23 ~4 ~-12 ~-17 ~6 ~-6 minecraft:oak_leaves
fill ~-8 ~ ~7 ~1 ~2 ~12 minecraft:stone
setblock ~-5 ~1 ~9 minecraft:coal_ore
setblock ~-1 ~1 ~10 minecraft:iron_ore
setblock ~5 ~ ~8 minecraft:furnace
setblock ~7 ~ ~8 minecraft:crafting_table
summon minecraft:cow ~-12 ~ ~8 {PersistenceRequired:1b,Tags:["hh_survival_integrated"]}
summon minecraft:chicken ~-15 ~ ~6 {PersistenceRequired:1b,Tags:["hh_survival_integrated"]}

# Anchor drives a delayed, reproducible attack after progression has visibly begun.
summon minecraft:marker ~ ~ ~ {Tags:["hh_survival_integrated","hh_survival_interrupt_anchor"]}
summon hostile_humans:human_tier1 ~-10 ~ ~ {PersonaId:"thefocus",SquadId:[I;1835887981,1835887981,1835887981,1835887981],PersistenceRequired:1b,HandItems:[{id:"minecraft:stone_pickaxe",Count:1b},{}],Inventory:[{Slot:20b,id:"minecraft:coal",Count:3b}],Tags:["hh_survival_integrated","hh_interrupt_squad","hh_interrupt_worker"]}
summon hostile_humans:human_tier1 ~-7 ~ ~-2 {PersonaId:"serpias",SquadId:[I;1835887981,1835887981,1835887981,1835887981],PersistenceRequired:1b,HandItems:[{id:"minecraft:stone_sword",Count:1b},{}],Tags:["hh_survival_integrated","hh_interrupt_squad","hh_interrupt_guard"]}
summon hostile_humans:human_tier1 ~-7 ~ ~2 {PersonaId:"killercreeper55",SquadId:[I;1835887981,1835887981,1835887981,1835887981],PersistenceRequired:1b,HandItems:[{id:"minecraft:stone_axe",Count:1b},{}],Tags:["hh_survival_integrated","hh_interrupt_squad","hh_interrupt_guard"]}

summon minecraft:text_display ~-14 ~7 ~ {text:'{"text":"PHASE 1: PROGRESSION","color":"green","bold":true}',billboard:"center",background:1073741824,Tags:["hh_survival_integrated"]}
summon minecraft:text_display ~14 ~7 ~ {text:'{"text":"PHASE 2: HOSTILE WAVE","color":"red","bold":true}',billboard:"center",background:1073741824,Tags:["hh_survival_integrated"]}
effect give @e[tag=hh_interrupt_squad] minecraft:glowing 1800 0 true
schedule function hostile_humans:debug/survival_interruptions_start 160t replace
tp @s ~ ~9 ~
gamemode spectator @s
tellraw @s {"text":"[HH SURVIVAL] Interruption arena ready. A hostile wave begins in 8 seconds.","color":"gold"}
tellraw @s {"text":"Expected: abandon mining/crafting immediately, share aggro/protect retreat, then resume progression after combat.","color":"aqua"}
tellraw @s {"text":"Resetting this scenario cancels the scheduled wave.","color":"gray"}

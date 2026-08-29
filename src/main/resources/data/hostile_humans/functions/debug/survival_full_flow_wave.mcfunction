# Optional combat interruption for survival_full_flow.
# Run after the worker has started progressing.
kill @e[tag=hh_full_flow_hostile]
summon minecraft:zombie ~10 ~ ~2 {PersistenceRequired:1b,HandItems:[{id:"minecraft:iron_sword",Count:1b},{}],Tags:["hh_survival_full_flow","hh_full_flow_hostile"]}
summon minecraft:skeleton ~11 ~ ~-2 {PersistenceRequired:1b,Tags:["hh_survival_full_flow","hh_full_flow_hostile"]}
effect give @e[tag=hh_full_flow_hostile] minecraft:glowing 600 0 true
tellraw @a[distance=..80] {"text":"[HH SURVIVAL] Hostile interruption active: progression should yield to combat, then resume.","color":"red"}

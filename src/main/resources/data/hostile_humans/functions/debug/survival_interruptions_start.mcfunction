# Delayed phase for survival_interruptions. Executes at the stable marker origin.
execute at @e[type=minecraft:marker,tag=hh_survival_interrupt_anchor,limit=1] run summon minecraft:zombie ~14 ~ ~-3 {PersistenceRequired:1b,HandItems:[{id:"minecraft:iron_sword",Count:1b},{}],Tags:["hh_survival_integrated","hh_interrupt_hostile"]}
execute at @e[type=minecraft:marker,tag=hh_survival_interrupt_anchor,limit=1] run summon minecraft:zombie ~16 ~ ~ {PersistenceRequired:1b,HandItems:[{id:"minecraft:iron_axe",Count:1b},{}],Tags:["hh_survival_integrated","hh_interrupt_hostile"]}
execute at @e[type=minecraft:marker,tag=hh_survival_interrupt_anchor,limit=1] run summon minecraft:skeleton ~14 ~ ~3 {PersistenceRequired:1b,Tags:["hh_survival_integrated","hh_interrupt_hostile"]}
effect give @e[tag=hh_interrupt_hostile] minecraft:glowing 600 0 true
execute at @e[type=minecraft:marker,tag=hh_survival_interrupt_anchor,limit=1] run damage @e[tag=hh_interrupt_worker,limit=1,sort=nearest] 4 minecraft:mob_attack by @e[tag=hh_interrupt_hostile,type=minecraft:zombie,limit=1,sort=nearest]
execute at @e[type=minecraft:marker,tag=hh_survival_interrupt_anchor,limit=1] run tellraw @a[distance=..80] {"text":"[HH SURVIVAL] Hostile wave active: progression should now be preempted by combat/retreat.","color":"red"}

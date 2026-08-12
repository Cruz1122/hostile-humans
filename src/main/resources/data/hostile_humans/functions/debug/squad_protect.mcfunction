# Low-health retreat protection. Run with: /function hostile_humans:debug/squad_protect
kill @e[tag=hh_squad_protect]
fill ~-10 ~-1 ~-7 ~10 ~-1 ~7 minecraft:smooth_stone
fill ~-10 ~ ~-7 ~10 ~5 ~7 minecraft:air
gamerule doMobSpawning false

summon hostile_humans:human_tier1 ~-5 ~ ~ {PersonaId:"elrubius",SquadId:[I;858993459,858993459,858993459,858993459],Health:2.0f,PersistenceRequired:1b,Tags:["hh_squad_protect","hh_protected"]}
summon hostile_humans:human_tier1 ~-3 ~ ~-3 {PersonaId:"coldified",SquadId:[I;858993459,858993459,858993459,858993459],PersistenceRequired:1b,Tags:["hh_squad_protect","hh_defender"]}
summon hostile_humans:human_tier1 ~-3 ~ ~ {PersonaId:"elrichmc",SquadId:[I;858993459,858993459,858993459,858993459],PersistenceRequired:1b,Tags:["hh_squad_protect","hh_defender"]}
summon hostile_humans:human_tier1 ~-3 ~ ~3 {PersonaId:"spreendmc",SquadId:[I;858993459,858993459,858993459,858993459],PersistenceRequired:1b,Tags:["hh_squad_protect","hh_defender"]}
summon hostile_humans:human_tier1 ~2 ~ ~ {PersonaId:"dream",PersistenceRequired:1b,Tags:["hh_squad_protect","hh_attacker"]}

effect give @e[tag=hh_squad_protect] minecraft:glowing 300 0 true
effect give @e[tag=hh_squad_protect] minecraft:resistance 4 4 true
damage @e[tag=hh_protected,limit=1] 1 minecraft:mob_attack by @e[tag=hh_attacker,limit=1]
tellraw @s {"text":"[HH PROTECT] ElRubius starts at 2 HP. Dream's damage alerts defenders when retreat activates.","color":"gold"}
tellraw @s {"text":"Retreat chance uses the current server config; set run_away_middle_fight_chance=1.0 for a deterministic manual run.","color":"gray"}

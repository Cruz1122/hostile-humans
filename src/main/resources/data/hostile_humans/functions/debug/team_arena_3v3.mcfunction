function hostile_humans:debug/team_arena_reset
summon hostile_humans:human_roamer ~-15 ~1 ~-3 {PersistenceRequired:1b,HumanTeam:"arena_red",Tags:["hh_team_arena","hh_team_red"]}
summon hostile_humans:human_roamer ~-15 ~1 ~ {PersistenceRequired:1b,HumanTeam:"arena_red",Tags:["hh_team_arena","hh_team_red"]}
summon hostile_humans:human_roamer ~-15 ~1 ~3 {PersistenceRequired:1b,HumanTeam:"arena_red",Tags:["hh_team_arena","hh_team_red"]}
summon hostile_humans:human_roamer ~15 ~1 ~-3 {PersistenceRequired:1b,HumanTeam:"arena_blue",Tags:["hh_team_arena","hh_team_blue"]}
summon hostile_humans:human_roamer ~15 ~1 ~ {PersistenceRequired:1b,HumanTeam:"arena_blue",Tags:["hh_team_arena","hh_team_blue"]}
summon hostile_humans:human_roamer ~15 ~1 ~3 {PersistenceRequired:1b,HumanTeam:"arena_blue",Tags:["hh_team_arena","hh_team_blue"]}
effect give @e[tag=hh_team_arena] minecraft:glowing 600 0 true
tellraw @s {"text":"[HH TEAM ARENA] 3v3 iniciado. Observa coordinacion de escuadra, navegacion, loot y combate con rolls independientes.","color":"green"}

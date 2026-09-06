function hostile_humans:debug/team_arena_reset
summon hostile_humans:human_roamer ~-15 ~1 ~-2 {PersistenceRequired:1b,HumanTeam:"arena_red",Tags:["hh_team_arena","hh_team_red"]}
summon hostile_humans:human_roamer ~-15 ~1 ~2 {PersistenceRequired:1b,HumanTeam:"arena_red",Tags:["hh_team_arena","hh_team_red"]}
summon hostile_humans:human_roamer ~15 ~1 ~-2 {PersistenceRequired:1b,HumanTeam:"arena_blue",Tags:["hh_team_arena","hh_team_blue"]}
summon hostile_humans:human_roamer ~15 ~1 ~2 {PersistenceRequired:1b,HumanTeam:"arena_blue",Tags:["hh_team_arena","hh_team_blue"]}
effect give @e[tag=hh_team_arena] minecraft:glowing 600 0 true
tellraw @s {"text":"[HH TEAM ARENA] 2v2 iniciado. Los cuatro NPCs tienen rolls independientes de persona, tier y equipamiento.","color":"blue"}

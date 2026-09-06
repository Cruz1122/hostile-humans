function hostile_humans:debug/team_arena_reset
summon hostile_humans:human_roamer ~-15 ~1 ~ {PersistenceRequired:1b,HumanTeam:"arena_red",Tags:["hh_team_arena","hh_team_red"]}
summon hostile_humans:human_roamer ~15 ~1 ~ {PersistenceRequired:1b,HumanTeam:"arena_blue",Tags:["hh_team_arena","hh_team_blue"]}
effect give @e[tag=hh_team_arena] minecraft:glowing 600 0 true
tellraw @s {"text":"[HH TEAM ARENA] 1v1 iniciado. Personas, tier de combate y equipamiento se generan individualmente; observa cover, puertas, fuego y cobwebs.","color":"red"}

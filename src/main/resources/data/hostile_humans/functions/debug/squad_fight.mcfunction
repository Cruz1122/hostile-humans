# Two local squads using the runtime persona tiers. Run with: /function hostile_humans:debug/squad_fight
kill @e[tag=hh_squad_fight]
fill ~-12 ~-1 ~-8 ~12 ~-1 ~8 minecraft:smooth_stone
fill ~-12 ~ ~-8 ~12 ~5 ~8 minecraft:air
gamerule doMobSpawning false

# Hispanic squad: Coldified T1, ElRichMC T3, Spreen T3, ElRubius T4.
summon hostile_humans:human_tier1 ~-8 ~ ~-3 {PersonaId:"coldified",SquadId:[I;286331153,286331153,286331153,286331153],PersistenceRequired:1b,Tags:["hh_squad_fight","hh_squad_hispanic"]}
summon hostile_humans:human_tier1 ~-8 ~ ~-1 {PersonaId:"elrichmc",SquadId:[I;286331153,286331153,286331153,286331153],PersistenceRequired:1b,Tags:["hh_squad_fight","hh_squad_hispanic"]}
summon hostile_humans:human_tier1 ~-8 ~ ~1 {PersonaId:"spreendmc",SquadId:[I;286331153,286331153,286331153,286331153],PersistenceRequired:1b,Tags:["hh_squad_fight","hh_squad_hispanic"]}
summon hostile_humans:human_tier1 ~-8 ~ ~3 {PersonaId:"elrubius",SquadId:[I;286331153,286331153,286331153,286331153],PersistenceRequired:1b,Tags:["hh_squad_fight","hh_squad_hispanic"]}

# International squad: Purpled T2, Dream T3, Sapnap T3, TommyInnit T4.
summon hostile_humans:human_tier1 ~8 ~ ~-3 {PersonaId:"purpled",SquadId:[I;572662306,572662306,572662306,572662306],PersistenceRequired:1b,Tags:["hh_squad_fight","hh_squad_international"]}
summon hostile_humans:human_tier1 ~8 ~ ~-1 {PersonaId:"dream",SquadId:[I;572662306,572662306,572662306,572662306],PersistenceRequired:1b,Tags:["hh_squad_fight","hh_squad_international"]}
summon hostile_humans:human_tier1 ~8 ~ ~1 {PersonaId:"sapnap",SquadId:[I;572662306,572662306,572662306,572662306],PersistenceRequired:1b,Tags:["hh_squad_fight","hh_squad_international"]}
summon hostile_humans:human_tier1 ~8 ~ ~3 {PersonaId:"tommyinnit",SquadId:[I;572662306,572662306,572662306,572662306],PersistenceRequired:1b,Tags:["hh_squad_fight","hh_squad_international"]}

effect give @e[tag=hh_squad_fight] minecraft:glowing 300 0 true
effect give @e[tag=hh_squad_fight] minecraft:resistance 5 4 true
damage @e[tag=hh_squad_hispanic,limit=1,sort=nearest] 1 minecraft:mob_attack by @e[tag=hh_squad_international,limit=1,sort=nearest]
tellraw @s {"text":"[HH SQUAD] Hispanic west vs International east. Runtime dataset tiers are preserved.","color":"gold"}

# Build the reusable 1v1/2v2/3v3 NPC combat coliseum.
# Run as an operator from a clear area:
# /function hostile_humans:debug/team_arena
# The three buttons on the north control wall reset the arena and start a mode.
schedule clear hostile_humans:debug/team_arena
schedule clear hostile_humans:debug/team_arena_1v1
schedule clear hostile_humans:debug/team_arena_2v2
schedule clear hostile_humans:debug/team_arena_3v3
kill @e[tag=hh_team_arena]
gamerule doMobSpawning false
gamerule mobGriefing true
time set day

# Build an elevated viewing gallery directly over the north entrance.
fill ~-24 ~-1 ~-14 ~24 ~8 ~14 minecraft:air
fill ~-24 ~-1 ~-14 ~24 ~-1 ~14 minecraft:polished_deepslate
fill ~-24 ~ ~-14 ~24 ~7 ~-14 minecraft:deepslate_bricks
fill ~-24 ~ ~14 ~24 ~7 ~14 minecraft:deepslate_bricks
fill ~-24 ~ ~-13 ~-24 ~7 ~13 minecraft:deepslate_bricks
fill ~24 ~ ~-13 ~24 ~7 ~13 minecraft:deepslate_bricks
fill ~-23 ~ ~-13 ~23 ~6 ~13 minecraft:air

fill ~-8 ~7 ~-25 ~8 ~7 ~-22 minecraft:polished_deepslate
fill ~-8 ~8 ~-25 ~8 ~8 ~-25 minecraft:iron_bars
setblock ~-4 ~8 ~-24 minecraft:command_block[facing=up]{Command:"execute positioned ~ ~-8 ~24 run function hostile_humans:debug/team_arena_1v1",auto:0b,TrackOutput:0b}
setblock ~ ~8 ~-24 minecraft:command_block[facing=up]{Command:"execute positioned ~ ~-8 ~24 run function hostile_humans:debug/team_arena_2v2",auto:0b,TrackOutput:0b}
setblock ~4 ~8 ~-24 minecraft:command_block[facing=up]{Command:"execute positioned ~ ~-8 ~24 run function hostile_humans:debug/team_arena_3v3",auto:0b,TrackOutput:0b}
setblock ~-4 ~9 ~-24 minecraft:stone_button[face=floor,facing=north,powered=false]
setblock ~ ~9 ~-24 minecraft:stone_button[face=floor,facing=north,powered=false]
setblock ~4 ~9 ~-24 minecraft:stone_button[face=floor,facing=north,powered=false]
setblock ~-5 ~10 ~-24 minecraft:oak_wall_sign[facing=north]{front_text:{messages:['{"text":"1v1","color":"red","bold":true}','{"text":"DUEL"}','{"text":"reset + start"}','{"text":""}']}}
setblock ~-1 ~10 ~-24 minecraft:oak_wall_sign[facing=north]{front_text:{messages:['{"text":"2v2","color":"blue","bold":true}','{"text":"TEAM FIGHT"}','{"text":"reset + start"}','{"text":""}']}}
setblock ~3 ~10 ~-24 minecraft:oak_wall_sign[facing=north]{front_text:{messages:['{"text":"3v3","color":"green","bold":true}','{"text":"SQUAD FIGHT"}','{"text":"reset + start"}','{"text":""}']}}

function hostile_humans:debug/team_arena_1v1
tp @s ~ ~15 ~-24 0 25
gamemode creative @s
effect give @s minecraft:night_vision 1200 0 true
tellraw @s {"text":"[HH TEAM ARENA] Coliseo preparado en creativo. Botones al norte: 1v1, 2v2 y 3v3; cada uno reconstruye y reinicia el combate.","color":"gold"}

# Original settlement gallery. Run once in a fresh open area with operator permissions.
# Each template is a complete multi-house settlement; the command immediately
# initializes it through the same camp/NPC pipeline used after chunk generation.
kill @e[tag=hh_settlement_gallery]
gamerule doMobSpawning false
gamerule doDaylightCycle false
gamerule mobGriefing true
gamerule keepInventory true
time set noon
weather clear

# Place and initialize five original settlement variants in a readable row.
execute positioned ~ ~ ~ run hostilehumans settlement plains
execute positioned ~64 ~ ~ run hostilehumans settlement taiga
execute positioned ~128 ~ ~ run hostilehumans settlement desert
execute positioned ~192 ~ ~ run hostilehumans settlement savanna
execute positioned ~256 ~ ~ run hostilehumans settlement snow

# Observe the full row from above its center.
tp @s ~152 ~24 ~24
gamemode spectator @s
tellraw @s {"text":"[HH SETTLEMENT GALLERY] Five original settlements placed: plains, taiga, desert, savanna and snow.","color":"gold"}
tellraw @s {"text":"[HH SETTLEMENT GALLERY] Each one has six houses, a plaza, real storage stations and a generated Human squad.","color":"aqua"}
tellraw @s {"text":"[HH SETTLEMENT GALLERY] Use hostilehumans inspect @e[type=hostile_humans:human_roamer,limit=1,sort=nearest] to inspect a Human.","color":"gray"}
tellraw @s {"text":"[HH SETTLEMENT GALLERY] This gallery is for visual feedback; natural worldgen happens only in new chunks.","color":"green"}

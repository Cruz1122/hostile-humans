# Remove scenario entities and restore the global debug gamerules.
# CampSavedData records are persistent; use this only after inspecting the run.
kill @e[tag=hh_mega_scenario]
gamerule doMobSpawning true
gamerule doDaylightCycle true
gamerule mobGriefing true
gamerule keepInventory false
gamemode survival @s
tellraw @s {"text":"[HH MEGA] Entidades eliminadas y gamerules restauradas. Los registros persistentes de campamento permanecen en el mundo.","color":"yellow"}

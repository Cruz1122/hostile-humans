# Run with: /function hostile_humans:debug/camp_basic
# Creates a small physical camp around the executing player and assigns the nearest Human.
gamerule mobGriefing true
hostilehumans camp
tellraw @s {"text":"[HH CAMP] Physical chest, furnace, crafting table and campfire created.","color":"gold"}

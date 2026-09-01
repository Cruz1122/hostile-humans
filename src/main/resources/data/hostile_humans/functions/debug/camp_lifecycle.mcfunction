# Run with: /function hostile_humans:debug/camp_lifecycle
# Use the three commands separately to observe each phase without a global scan.
gamerule mobGriefing true
hostilehumans camp
tellraw @s {"text":"[HH LIFECYCLE] Camp ready. Run debug/expedition, then debug/raid after creating an enemy camp.","color":"gold"}

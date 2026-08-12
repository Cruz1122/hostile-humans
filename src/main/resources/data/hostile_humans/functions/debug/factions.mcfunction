# Reset only entities created by this scenario.
kill @e[tag=hh_factions_debug]

# Prepare a compact arena around the executing player.
fill ~-8 ~-1 ~-8 ~8 ~-1 ~8 minecraft:polished_deepslate
fill ~-8 ~ ~-8 ~8 ~5 ~8 minecraft:air
gamerule doMobSpawning false

# The CSV is authoritative: Coldified T1, ElRubius T4, Dream T3, Herobrine T1.
summon hostile_humans:human_tier1 ~-5 ~ ~-2 {PersonaId:"coldified",PersistenceRequired:1b,Tags:["hh_factions_debug","hh_coldified"]}
summon hostile_humans:human_tier1 ~-5 ~ ~2 {PersonaId:"elrubius",CombatSkillTier:4,PersistenceRequired:1b,Tags:["hh_factions_debug","hh_elrubius"]}
summon hostile_humans:human_tier1 ~5 ~ ~-2 {PersonaId:"dream",CombatSkillTier:3,PersistenceRequired:1b,Tags:["hh_factions_debug","hh_dream"]}
summon hostile_humans:human_tier1 ~5 ~ ~2 {PersonaId:"herobrine",CombatSkillTier:1,PersistenceRequired:1b,Tags:["hh_factions_debug","hh_herobrine"]}

# Deliberate duplicate attempt: this entity must remain generic because Coldified is reserved.
summon hostile_humans:human_tier1 ~ ~ ~6 {PersonaId:"coldified",PersistenceRequired:1b,Tags:["hh_factions_debug","hh_duplicate_attempt"]}
effect give @e[tag=hh_factions_debug] minecraft:glowing 300 0 true

tellraw @s {"text":"[HH] factions: Coldified + ElRubius allied; Dream and Herobrine hostile; duplicate Coldified must be generic.","color":"aqua"}

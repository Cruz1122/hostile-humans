# Ten isolated glass rooms: five exact competitive tiers and five mechanics.
# Run as a creative operator in a test world. Re-running removes only this arena.
kill @e[tag=hh_shield_arena]
gamerule doMobSpawning false
gamerule mobGriefing true
time set day
tp @s -42 81 -12 90 0
fill -38 80 -10 38 86 10 minecraft:air
fill -38 79 -10 38 79 10 minecraft:polished_deepslate
fill -38 86 -10 38 86 10 minecraft:glass
fill -38 80 -10 -38 85 10 minecraft:glass
fill 38 80 -10 38 85 10 minecraft:glass
fill -38 80 -10 38 85 -10 minecraft:glass
fill -38 80 10 38 85 10 minecraft:glass

# Room boundaries: five rooms per row, with glass walls and a glass roof.
fill -35 80 -8 -35 85 8 minecraft:glass
fill -21 80 -8 -21 85 8 minecraft:glass
fill -7 80 -8 -7 85 8 minecraft:glass
fill 7 80 -8 7 85 8 minecraft:glass
fill 21 80 -8 21 85 8 minecraft:glass
fill 35 80 -8 35 85 8 minecraft:glass
fill -35 80 -8 35 85 -8 minecraft:glass
fill -35 80 0 35 85 0 minecraft:glass
fill -35 80 8 35 85 8 minecraft:glass

# T5 -> T1. CombatSkillTier is an explicit debug-only NBT override.
summon hostile_humans:human_tier1 -28 80 -4 {PersistenceRequired:1b,CombatSkillTier:5,CustomName:'{"text":"T5 NOVATO"}',CustomNameVisible:1b,Tags:["hh_shield_arena","hh_arena_t5"]}
summon hostile_humans:human_tier1 -14 80 -4 {PersistenceRequired:1b,CombatSkillTier:4,CustomName:'{"text":"T4"}',CustomNameVisible:1b,Tags:["hh_shield_arena","hh_arena_t4"]}
summon hostile_humans:human_tier1 0 80 -4 {PersistenceRequired:1b,CombatSkillTier:3,CustomName:'{"text":"T3 BASE"}',CustomNameVisible:1b,Tags:["hh_shield_arena","hh_arena_t3"]}
summon hostile_humans:human_tier2 14 80 -4 {PersistenceRequired:1b,CombatSkillTier:2,CustomName:'{"text":"T2"}',CustomNameVisible:1b,Tags:["hh_shield_arena","hh_arena_t2"]}
summon hostile_humans:human_tier2 28 80 -4 {PersistenceRequired:1b,CombatSkillTier:1,CustomName:'{"text":"T1 VETERANO"}',CustomNameVisible:1b,Tags:["hh_shield_arena","hh_arena_t1"]}

# Mechanics row: all use T3 so only the tested mechanic changes.
summon hostile_humans:human_tier1 -28 80 4 {PersistenceRequired:1b,CombatSkillTier:3,CustomName:'{"text":"SWORD BLOCK"}',CustomNameVisible:1b,Tags:["hh_shield_arena","hh_arena_sword"]}
summon hostile_humans:human_tier1 -14 80 4 {PersistenceRequired:1b,CombatSkillTier:3,CustomName:'{"text":"AXE RESPONSE"}',CustomNameVisible:1b,Tags:["hh_shield_arena","hh_arena_axe"]}
summon hostile_humans:human_tier1 0 80 4 {PersistenceRequired:1b,CombatSkillTier:3,CustomName:'{"text":"BREAK SHIELD"}',CustomNameVisible:1b,Tags:["hh_shield_arena","hh_arena_break"]}
summon hostile_humans:human_tier1 14 80 4 {PersistenceRequired:1b,CombatSkillTier:3,CustomName:'{"text":"PROJECTILE"}',CustomNameVisible:1b,Tags:["hh_shield_arena","hh_arena_projectile"]}
summon hostile_humans:human_tier1 28 80 4 {PersistenceRequired:1b,CombatSkillTier:3,Health:3.0f,CustomName:'{"text":"HEAL / RETREAT"}',CustomNameVisible:1b,Tags:["hh_shield_arena","hh_arena_heal"]}

# Deterministic starting equipment.
item replace entity @e[tag=hh_shield_arena,type=hostile_humans:human_tier1] weapon.offhand with minecraft:shield
item replace entity @e[tag=hh_shield_arena,type=hostile_humans:human_tier2] weapon.offhand with minecraft:shield
item replace entity @e[tag=hh_shield_arena,tag=hh_arena_sword,limit=1] weapon.mainhand with minecraft:iron_sword
item replace entity @e[tag=hh_shield_arena,tag=hh_arena_axe,limit=1] weapon.mainhand with minecraft:iron_sword
item replace entity @e[tag=hh_shield_arena,tag=hh_arena_break,limit=1] weapon.mainhand with minecraft:iron_sword
item replace entity @e[tag=hh_shield_arena,tag=hh_arena_projectile,limit=1] weapon.mainhand with minecraft:iron_sword
item replace entity @e[tag=hh_shield_arena,tag=hh_arena_heal,limit=1] weapon.mainhand with minecraft:iron_sword
item replace entity @e[tag=hh_shield_arena,tag=hh_arena_t1,limit=1] weapon.mainhand with minecraft:diamond_sword
item replace entity @e[tag=hh_shield_arena,tag=hh_arena_t2,limit=1] weapon.mainhand with minecraft:diamond_axe
item replace entity @e[tag=hh_shield_arena,tag=hh_arena_break,limit=1] weapon.mainhand with minecraft:diamond_axe
item replace entity @e[tag=hh_shield_arena,tag=hh_arena_projectile,limit=1] weapon.mainhand with minecraft:bow
item replace entity @e[tag=hh_shield_arena] armor.head with minecraft:iron_helmet
item replace entity @e[tag=hh_shield_arena] armor.chest with minecraft:iron_chestplate
item replace entity @e[tag=hh_shield_arena] armor.legs with minecraft:iron_leggings
item replace entity @e[tag=hh_shield_arena] armor.feet with minecraft:iron_boots
item replace entity @e[tag=hh_shield_arena,tag=hh_arena_t1,limit=1] armor.head with minecraft:diamond_helmet
item replace entity @e[tag=hh_shield_arena,tag=hh_arena_t1,limit=1] armor.chest with minecraft:diamond_chestplate
item replace entity @e[tag=hh_shield_arena,tag=hh_arena_t1,limit=1] armor.legs with minecraft:diamond_leggings
item replace entity @e[tag=hh_shield_arena,tag=hh_arena_t1,limit=1] armor.feet with minecraft:diamond_boots
effect give @e[tag=hh_shield_arena] minecraft:glowing 1200 0 true

# Explanatory signs inside every room.
setblock -34 80 -7 minecraft:oak_wall_sign[facing=south]{front_text:{messages:['{"text":"TIERS"}','{"text":"T5 T4 T3"}','{"text":"T2 T1"}','{"text":"reaction"}']}}
setblock -20 80 -7 minecraft:oak_wall_sign[facing=south]{front_text:{messages:['{"text":"T4"}','{"text":"bloqueo"}','{"text":"corto"}','{"text":"y errores"}']}}
setblock -6 80 -7 minecraft:oak_wall_sign[facing=south]{front_text:{messages:['{"text":"T3"}','{"text":"perfil"}','{"text":"base"}','{"text":"comparar"}']}}
setblock 8 80 -7 minecraft:oak_wall_sign[facing=south]{front_text:{messages:['{"text":"T2"}','{"text":"bloqueo"}','{"text":"sin orbitar"}','{"text":"visible"}']}}
setblock 22 80 -7 minecraft:oak_wall_sign[facing=south]{front_text:{messages:['{"text":"T1"}','{"text":"reaccion"}','{"text":"rapida"}','{"text":"no perfecta"}']}}

setblock -34 80 7 minecraft:oak_wall_sign[facing=north]{front_text:{messages:['{"text":"SWORD"}','{"text":"ataca"}','{"text":"frontal"}','{"text":"durabilidad"}']}}
setblock -20 80 7 minecraft:oak_wall_sign[facing=north]{front_text:{messages:['{"text":"AXE"}','{"text":"muestra"}','{"text":"hacha"}','{"text":"sigue bloqueo"}']}}
setblock -6 80 7 minecraft:oak_wall_sign[facing=north]{front_text:{messages:['{"text":"BREAK"}','{"text":"levanta"}','{"text":"escudo"}','{"text":"axe swap"}']}}
setblock 8 80 7 minecraft:oak_wall_sign[facing=north]{front_text:{messages:['{"text":"ARROW"}','{"text":"dispara"}','{"text":"frontal"}','{"text":"y trasero"}']}}
setblock 22 80 7 minecraft:oak_wall_sign[facing=north]{front_text:{messages:['{"text":"LOW HP"}','{"text":"golpea"}','{"text":"observa"}','{"text":"retirada"}']}}

tellraw @s {"text":"[HH DEBUG] Arena creada: 5 habitaciones de T5 a T1 y 5 habitaciones de mecánicas. Cada NPC está encerrado.","color":"green"}
tellraw @s {"text":"[HH DEBUG] Rompe una pared de cristal solo en la sala que pruebes; los carteles indican qué observar.","color":"gold"}
tellraw @s {"text":"[HH DEBUG] Tiers: T5 comete más errores; T1 reacciona antes, pero nunca conoce cooldowns ni inventario oculto.","color":"yellow"}

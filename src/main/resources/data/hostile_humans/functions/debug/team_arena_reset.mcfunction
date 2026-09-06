# Shared reset/build step for the detailed coliseum.
# The arena floor is one block below the origin; NPC spawn pads are at y=1.
# The control gallery is at z=-28 and is intentionally outside this volume.
kill @e[tag=hh_team_arena]
fill ~-25 ~-2 ~-22 ~25 ~12 ~22 minecraft:air
fill ~-25 ~-2 ~-22 ~25 ~-2 ~22 minecraft:deepslate
fill ~-23 ~-1 ~-20 ~23 ~-1 ~20 minecraft:smooth_quartz

# Octagonal coliseum perimeter: no solid block is placed near the spawn pads.
fill ~-20 ~ ~-20 ~20 ~8 ~-20 minecraft:cut_sandstone
fill ~-20 ~ ~20 ~20 ~8 ~20 minecraft:cut_sandstone
fill ~-20 ~ ~-19 ~-20 ~8 ~19 minecraft:cut_sandstone
fill ~20 ~ ~-19 ~20 ~8 ~19 minecraft:cut_sandstone
fill ~-16 ~ ~-16 ~16 ~6 ~-16 minecraft:cut_sandstone
fill ~-16 ~ ~16 ~16 ~6 ~16 minecraft:cut_sandstone
fill ~-16 ~ ~-15 ~-16 ~6 ~15 minecraft:cut_sandstone
fill ~16 ~ ~-15 ~16 ~6 ~15 minecraft:cut_sandstone
fill ~-15 ~ ~-15 ~15 ~5 ~-15 minecraft:sandstone
fill ~-15 ~ ~15 ~15 ~5 ~15 minecraft:sandstone
fill ~-15 ~ ~-14 ~-15 ~5 ~14 minecraft:sandstone
fill ~15 ~ ~-14 ~15 ~5 ~14 minecraft:sandstone

# Three spectator/seat terraces on each side.
fill ~-19 ~ ~-13 ~-17 ~1 ~13 minecraft:red_sandstone
fill ~-18 ~2 ~-12 ~-16 ~3 ~12 minecraft:red_sandstone
fill ~17 ~ ~-13 ~19 ~1 ~13 minecraft:blue_concrete
fill ~16 ~2 ~-12 ~18 ~3 ~12 minecraft:blue_concrete
fill ~-13 ~ ~17 ~13 ~1 ~19 minecraft:gold_block
fill ~-12 ~2 ~16 ~12 ~3 ~18 minecraft:gold_block

# Entrance arches and corner towers.
fill ~-4 ~ ~-20 ~4 ~5 ~-20 minecraft:air
fill ~-5 ~ ~-21 ~-5 ~6 ~-20 minecraft:chiseled_sandstone
fill ~5 ~ ~-21 ~5 ~6 ~-20 minecraft:chiseled_sandstone
fill ~-4 ~6 ~-21 ~4 ~7 ~-20 minecraft:chiseled_sandstone
fill ~-21 ~ ~-21 ~-18 ~10 ~-18 minecraft:polished_blackstone_bricks
fill ~18 ~ ~-21 ~21 ~10 ~-18 minecraft:polished_blackstone_bricks
fill ~-21 ~ ~18 ~-18 ~10 ~21 minecraft:polished_blackstone_bricks
fill ~18 ~ ~18 ~21 ~10 ~21 minecraft:polished_blackstone_bricks
fill ~-20 ~10 ~-20 ~-19 ~12 ~-19 minecraft:lantern
fill ~19 ~10 ~-20 ~20 ~12 ~-19 minecraft:lantern
fill ~-20 ~10 ~19 ~-19 ~12 ~20 minecraft:lantern
fill ~19 ~10 ~19 ~20 ~12 ~20 minecraft:lantern

# Battle floor details: central dais, four pillars, low cover and two lanes.
fill ~-5 ~ ~-5 ~5 ~1 ~5 minecraft:polished_blackstone
fill ~-3 ~2 ~-3 ~3 ~3 ~3 minecraft:quartz_block
fill ~-2 ~4 ~-2 ~2 ~4 ~2 minecraft:gold_block
fill ~-12 ~ ~-10 ~-9 ~4 ~-7 minecraft:stone_bricks
fill ~9 ~ ~7 ~12 ~4 ~10 minecraft:stone_bricks
fill ~-12 ~ ~7 ~-9 ~2 ~10 minecraft:stone_bricks
fill ~9 ~ ~-10 ~12 ~2 ~-7 minecraft:stone_bricks
fill ~-2 ~ ~-13 ~2 ~2 ~-11 minecraft:polished_deepslate
fill ~-2 ~ ~11 ~2 ~2 ~13 minecraft:polished_deepslate

# Tactical stations, deliberately away from both spawn pads.
fill ~-9 ~ ~-2 ~-6 ~ ~2 minecraft:gravel
fill ~6 ~ ~-2 ~9 ~ ~2 minecraft:gravel
fill ~-13 ~ ~-4 ~-11 ~ ~-4 minecraft:cobweb
fill ~11 ~ ~4 ~13 ~ ~4 minecraft:cobweb
setblock ~-8 ~ ~-8 minecraft:fire
setblock ~8 ~ ~8 minecraft:fire
setblock ~-8 ~ ~8 minecraft:oak_door[facing=east,half=lower,hinge=left,open=false]
setblock ~-8 ~1 ~8 minecraft:oak_door[facing=east,half=upper,hinge=left,open=false]
setblock ~-9 ~ ~8 minecraft:stone_button[face=wall,facing=east,powered=false]
setblock ~8 ~ ~-8 minecraft:oak_door[facing=west,half=lower,hinge=right,open=false]
setblock ~8 ~1 ~-8 minecraft:oak_door[facing=west,half=upper,hinge=right,open=false]
setblock ~9 ~ ~-8 minecraft:stone_button[face=wall,facing=west,powered=false]

# Safe, open spawn pads. Clearing them last prevents collision with scenery.
fill ~-18 ~ ~-4 ~-12 ~4 ~4 minecraft:air
fill ~12 ~ ~-4 ~18 ~4 ~4 minecraft:air
fill ~-18 ~-1 ~-4 ~-12 ~-1 ~4 minecraft:red_concrete
fill ~12 ~-1 ~-4 ~18 ~-1 ~4 minecraft:blue_concrete
fill ~-17 ~ ~-3 ~-13 ~ ~3 minecraft:gold_block
fill ~13 ~ ~-3 ~17 ~ ~3 minecraft:gold_block

setblock ~-22 ~1 ~-12 minecraft:oak_wall_sign[facing=east]{front_text:{messages:['{"text":"COLISEUM"}','{"text":"COVER"}','{"text":"FIRE"}','{"text":"COBWEB"}']}}
setblock ~8 ~1 ~-12 minecraft:oak_wall_sign[facing=west]{front_text:{messages:['{"text":"TACTICAL"}','{"text":"DOORS"}','{"text":"CENTER DAIS"}','{"text":"LOOT"}']}}
setblock ~-2 ~ ~0 minecraft:chest[facing=south]{Items:[{Slot:0b,id:"minecraft:bread",Count:8b},{Slot:1b,id:"minecraft:cobblestone",Count:16b},{Slot:2b,id:"minecraft:shield",Count:1b}]}
setblock ~2 ~ ~0 minecraft:chest[facing=south]{Items:[{Slot:0b,id:"minecraft:arrow",Count:16b},{Slot:1b,id:"minecraft:golden_apple",Count:1b},{Slot:2b,id:"minecraft:ender_pearl",Count:2b}]}

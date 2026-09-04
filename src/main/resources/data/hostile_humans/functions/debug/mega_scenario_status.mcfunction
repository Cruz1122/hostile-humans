# Status helper for the original settlement gallery.
execute if entity @e[tag=hh_settlement_gallery] run tellraw @s {"text":"[HH SETTLEMENT GALLERY] Gallery squads are active. Follow them in spectator mode and inspect their inventories.","color":"green"}
execute unless entity @e[tag=hh_settlement_gallery] run tellraw @s {"text":"[HH SETTLEMENT GALLERY] No gallery entities are active. Execute debug/settlement_gallery once in a fresh area.","color":"red"}

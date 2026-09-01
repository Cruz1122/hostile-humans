# Status helper for the large observation scenario.
execute if entity @e[tag=hh_mega_scenario] run tellraw @s {"text":"[HH MEGA] Entidades del escenario activas. Usa spectator para seguir cada grupo y hostilehumans inspect para revisar inventarios.","color":"green"}
execute unless entity @e[tag=hh_mega_scenario] run tellraw @s {"text":"[HH MEGA] No hay entidades activas. Ejecuta debug/mega_scenario para crear el escenario.","color":"red"}
execute if entity @e[tag=hh_mega_hispanic] run tellraw @s {"text":"[HH MEGA] Squad rojo activo en el campamento oeste.","color":"red"}
execute if entity @e[tag=hh_mega_international] run tellraw @s {"text":"[HH MEGA] Squad azul activo en el campamento este.","color":"blue"}
execute if entity @e[tag=hh_mega_legends] run tellraw @s {"text":"[HH MEGA] Squad morado activo en el campamento norte.","color":"light_purple"}

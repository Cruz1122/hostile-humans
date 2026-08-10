function hostile_humans:debug/shield_sword_duel
item replace entity @s weapon.mainhand with minecraft:iron_axe
tellraw @s {"text":"[HH DEBUG] Shield-break scenario: raise a shield, then let the human see the axe and observe its temporary disabler choice.","color":"gold"}

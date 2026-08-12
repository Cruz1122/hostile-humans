# Chest looting debug scenario

Run `/function hostile_humans:debug/chest_loot` in a test world.

The command clears a spacious 25x9x25 area around the player, leaves an open
polished-deepslate floor directly below the arena, places one chest five blocks
east, and summons a glowing persistent human one block west. Keep the camera above the arena so
the path and chest remain visible.

## Expected result

1. The human notices the visible chest and walks toward it without breaking or
   placing blocks.
2. The chest visibly opens and remains open for a few seconds while useful
   stacks are transferred one at a time.
3. The diamond sword, bread, cobblestone, armor, shield, and totem move to the
   human. The armor is equipped immediately. The iron ingot remains because it
   is not useful to the current loadout policy.
4. The chest retains any item that does not fit in the human inventory.
5. The player and test human are placed on a temporary no-friendly-fire team,
   preventing the observation scenario from immediately becoming combat.

The glowing name `HH_CHEST_LOOT` identifies the test human. Re-run the command
to reset the arena. The hidden-wall counterpart remains available as
`/function hostile_humans:debug/chest_loot_hidden` for the negative visibility
case.

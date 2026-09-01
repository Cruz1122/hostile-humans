# Tactical utility in-game scenario

Run this from a creative operator in an empty test area:

```text
/function hostile_humans:debug/tactical_utility_arena
```

The function disables natural mob spawning, enables `mobGriefing`, builds a
bounded glass arena, and teleports only the executing player to a raised
viewpoint. Every spawned entity uses the `hh_utility_arena` tag, so rerunning
the function from the viewpoint removes the previous NPCs, zombies, and
remaining entities before rebuilding the arena at the same origin.

## What to watch in-game

### 1. `HH_WATER_RESCUE`

- Located in the left glass room.
- Starts on fire with a water bucket in the off hand and remains stationary so
  the recovery step can be observed without the NPC wandering away.
- Places water beside itself and extinguishes the fire.
- The held item changes from `water_bucket` to `bucket`.
- After the configured recovery delay, it removes only the water source it
  placed and restores the water bucket.
- It must not remove unrelated water sources; this arena intentionally has no
  unrelated source in that room so the owned-source behavior is easy to see.

Inspect its persistent inventory with:

```text
/hostilehumans inspect @e[tag=hh_utility_water,limit=1]
```

### 2. `HH_OFFENSIVE_PEARL`

- Located in the front-right glass room.
- Has two ender pearls in the off hand.
- `PEARL_TARGET` is a visible, stationary zombie twelve blocks away. It is not
  invulnerable, because Minecraft's targeting rules treat invulnerable mobs as
  invalid combat targets.
- The Human should throw one pearl toward the target because the distance is
  inside the configured offensive range.
- The pearl count should go from two to one, and one projectile should be
  visible in flight.
- The cooldown prevents an immediate second throw.

### 3. `HH_DEFENSIVE_PEARL`

- Located in the upper-right glass room.
- Starts at eight health out of forty with one ender pearl.
- `DEFENSIVE_THREAT` is a visible zombie immediately to its right. It has zero
  movement speed but a small attack value, so it can hit the Human and trigger
  the real attacker/flee path without killing the fixture.
- The Human should recognize the low-health threat and throw away from it,
  toward the left side of the room.
- The pearl is consumed and the entity receives an ender-pearl cooldown.

## Useful observation commands

```text
/hostilehumans inspect @e[tag=hh_utility_water,limit=1]
/hostilehumans inspect @e[tag=hh_utility_offensive,limit=1]
/hostilehumans inspect @e[tag=hh_utility_defensive,limit=1]
/gamerule mobGriefing false
```

The last command is a negative check: after setting `mobGriefing` to false,
rerun the arena and verify that the water station does not place water or
consume its bucket. Restore it before the normal observation:

```text
/gamerule mobGriefing true
```

## Reset and cleanup

Run the setup function again to reset the scenario:

```text
/function hostile_humans:debug/tactical_utility_arena
```

When finished, restore the global test-world rule if needed:

```text
/gamerule doMobSpawning true
/gamerule mobGriefing true
```

The scenario is qualitative. The GameTest remains the deterministic contract
for exact inventory counts, NBT persistence, cooldown values, and source-block
ownership.

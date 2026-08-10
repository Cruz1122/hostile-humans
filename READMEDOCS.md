# Human Gear JSON System

## Tactical equipment

### Observable shield combat

Shield tactics run on the logical server and use only signals that a human can
observe: visible held items, blocking state, an already-started swing, visible
projectiles, distance, orientation, velocity and line of sight. They do not read
the player's attack cooldown, hidden inventory, future item selection or future
trajectory.

The tactical controller uses `ShieldState` and `CombatTactic` instead of a
permanent `shouldBlock` flag. It keeps short commitment windows, uses the normal
`LivingEntity.startUsingItem(OFF_HAND)` / `stopUsingItem()` path, and lets Forge
handle shield direction, durability, sounds and synchronization. A non-player
human has its own server-side shield-disable deadline because vanilla player
cooldowns are not automatically available to mobs.

A visible weapon capable of disabling the shield is treated as an observable
melee threat, not as a reason to lower the shield preemptively. The human keeps
blocking through its wind-up; only a connected hit can start shield disablement.

Competitive combat skill is represented independently from the loadout tier as
T1 through T5, where T1 is strongest. The assignment is stable per entity and
varies slightly around the loadout baseline; it is never rerolled every tick.
All combat tiers use the player's three-block melee reach. Their attack
cooldowns are scaled by skill tier so every tier can chain repeated melee hits;
T1 chains fastest while T5 remains more aggressive than the previous baseline.

Server settings:

```toml
[tacticalEquipment]
enableShieldTactics = true
enableProjectileBlocking = true
enableShieldBreaking = true
```

Manual shield scenarios:

- `function hostile_humans:debug/shield_sword_duel`
- `function hostile_humans:debug/shield_axe_counter`
- `function hostile_humans:debug/shield_break_enemy`
- `function hostile_humans:debug/shield_projectile_approach`
- `function hostile_humans:debug/shield_low_health`
- `function hostile_humans:debug/shield_t1_duel`
- `function hostile_humans:debug/shield_tactics_arena` creates ten separate glass
  rooms: T5, T4, T3, T2, T1, frontal sword blocking, visible axe response,
  shield breaking, projectile defense and low-health retreat/healing. Every room
  contains a sign with the specific action to perform and the expected behavior.

These are qualitative scenarios. Use walls and visible hand changes to verify
that the human does not react to hidden information.

Humans use a deterministic melee policy when they enter close combat or finish
collecting an item. Primary melee items are selected before fallback tools.
Primary items include swords and axes through the `hostile_humans:primary_melee_weapons`
item tag. Pickaxes, shovels and hoes are available only through
`hostile_humans:fallback_melee_tools`, and are never preferred over a usable
primary item. The `hostile_humans:never_use_as_melee_weapon` tag can veto an item.
Modpacks can extend these tags with a datapack without changing Java code.

The score within a category combines material quality, effective main-hand attack damage,
applicable offensive enchantment damage, remaining durability and a near-break
penalty. Category always dominates the score. Ties keep the current item, then
use inventory slot and registry ID as stable tie-breakers. Selection is not
performed every tick: it is marked dirty after pickup, loadout generation,
loading, combat transitions and weapon breakage.

Humans continue to receive audible game-event alerts, including visible player
movement, but use a custom listener so vanilla vibration particles are not
rendered for the listener.

Server interaction settings are written to `hostile_humans-server.toml`:

```toml
[tacticalEquipment]
enableFallbackToolWeapons = true
enableCobwebPlacement = true
cobwebCooldownTicks = 80
maxCobwebsPerCombat = 2
cobwebPlacementReach = 2.5
```

During an existing retreat, a human may place only `minecraft:cobweb` in a
small deterministic candidate set behind its route. It requires a real cobweb
stack, a living nearby threat, a loaded chunk, a replaceable supported block,
no self/target intersection, no nearby equivalent cobweb, available cooldown,
and remaining combat quota. Placement is server-side, consumes exactly one
stack item only after success, respects Forge placement cancellation and
`mobGriefing`, and does not load chunks, break blocks, or place in BlockEntities.

Manual scenarios:

- `function hostile_humans:debug/all_tactical_equipment` creates one arena with
  all four stations below. It is the recommended manual smoke test.
- `function hostile_humans:debug/weapon_primary`
- `function hostile_humans:debug/weapon_fallback`
- `function hostile_humans:debug/weapon_upgrade`
- `function hostile_humans:debug/cobweb_retreat`

### Complete manual smoke test

Run the following command as a creative operator in a test world:

```text
/function hostile_humans:debug/all_tactical_equipment
```

The arena uses four labeled, glowing humans in separate glass chambers:

1. `HH_PRIMARY` must keep the iron sword instead of the nearby iron pickaxe.
2. `HH_FALLBACK` must collect and equip the iron pickaxe.
3. `HH_UPGRADE` must replace the iron pickaxe with the diamond sword after pickup.
4. `HH_COBWEB` starts at low health. Attack it inside its chamber so it enters
   retreat, then it must collect cobwebs, place webs only while retreating,
   consume one item per successful placement, respect the cooldown and stop at
   the combat quota.

The scenario disables natural mob spawning and enables `mobGriefing`. Re-running
the same function removes only entities tagged `hh_full_debug`, clears the
previously placed blocks and rebuilds all four chambers. Use these positions to
test stations individually: `X=-18` (primary), `X=-12` (fallback), `X=-6`
(upgrade), and `X=0` (cobweb). For the cobweb station, attack the low-health
human inside its chamber and observe the entity with the `HH_COBWEB` name and glowing effect. The three
single-purpose functions remain available when an isolated reproduction is
needed.

Nearby item pickup is checked twice per second, and cobweb placement tries
several safe positions behind the retreating human when the ideal position is
blocked.

Flee selection is intentionally probabilistic in normal gameplay. For a
deterministic manual cobweb test, set `run_away_middle_fight_chance = 1.0` in
`config/hostile_humans-common.toml` and restart the server before running the
scenario. This does not change the server tactical-equipment settings.

GameTests are grouped in `tacticalEquipment` and `tacticalCobweb` batches and
cover primary-over-fallback selection, fallback use, pickup upgrade, invalid
items, stable ties, successful placement and consumption, empty inventory,
`mobGriefing`, support validation and cooldown.

Pillaring, bridging, barricades, mining and general block construction remain
future work. A possible future boundary is `TacticalWorldActionController`
with separate `PillarUpAction` and `BridgeGapAction` implementations.

  ## Overview

  The mod now supports datapack-driven human gear loadouts.

  Instead of hardcoding human armor and weapons in Java arrays, the mod reads JSON files from:

  data/hostile_humans/human_loadouts/

  Supported default files are:

  data/hostile_humans/human_loadouts/tier1.json
  data/hostile_humans/human_loadouts/tier2.json
  data/hostile_humans/human_loadouts/roamer.json

  These files control what each human tier can spawn with.

  ## Why this exists

  This system makes it easier for players and pack makers to:

  - add support for weapons and armor from other mods
  - rebalance human loadouts without changing code
  - override default human gear with datapacks

  ## How loadouts are selected

  Each tier file can define:

  - mainhand
  - ranged_mainhand
  - offhand
  - inventory
  - bonus_mainhand
  - armor_sets
  - rules

  The mod uses weighted random rolls from these pools during human spawn.

  ## File Format

  ### Top-level structure

  {
    "schema_version": 1,
    "rules": {
      "enchant_chance": 0.3,
      "damage_percent_min": 0.0,
      "damage_percent_max": 0.85
    },
    "mainhand": [],
    "ranged_mainhand": [],
    "offhand": {
      "chance": 0.2,
      "entries": []
    },
    "inventory": [],
    "bonus_mainhand": {
      "chance": 0.0,
      "entries": []
    },
    "armor_sets": []
  }

  ## Field Reference

  ### schema_version

  "schema_version": 1

  Current version of the gear schema.

  ### rules

  "rules": {
    "enchant_chance": 0.3,
    "damage_percent_min": 0.0,
    "damage_percent_max": 0.85
  }

  Meaning:

  - enchant_chance: chance for spawned gear to receive enchantments
  - damage_percent_min: minimum durability damage percent applied to spawned gear
  - damage_percent_max: maximum durability damage percent applied to spawned gear

  ### mainhand

  Weighted pool for normal main-hand weapons.

  Example:

  "mainhand": [
    { "item": "minecraft:iron_sword", "weight": 2 },
    { "item": "minecraft:stone_sword", "weight": 2 },
    { "item": "minecraft:crossbow", "weight": 1 },
    { "item": "minecraft:bow", "weight": 1 }
  ]

  ### ranged_mainhand

  Used when a human is forced to spawn with a ranged weapon.

  Example:

  "ranged_mainhand": [
    { "item": "minecraft:crossbow", "weight": 1 },
    { "item": "minecraft:bow", "weight": 1 }
  ]

  ### offhand

  Controls optional offhand gear.

  Example:

  "offhand": {
    "chance": 0.5,
    "entries": [
      { "item": "minecraft:shield", "weight": 85 },
      { "item": "minecraft:totem_of_undying", "weight": 15 }
    ]
  }

  Meaning:

  - chance: chance to roll an offhand item at all
  - entries: weighted item pool if the roll succeeds

  ### inventory

  Extra inventory items, mainly used for backup weapons.

  Example:

  "inventory": [
    { "item": "minecraft:iron_sword", "weight": 1 },
    { "item": "minecraft:stone_sword", "weight": 1 }
  ]

  ### bonus_mainhand

  Optional rare override for the main hand after the normal roll.

  Example:

  "bonus_mainhand": {
    "chance": 0.05,
    "entries": [
      { "item": "minecraft:trident", "weight": 1 }
    ]
  }

  ### armor_sets

  Weighted pool of full armor sets.

  Example:

  "armor_sets": [
    {
      "weight": 1,
      "head": "minecraft:iron_helmet",
      "chest": "minecraft:iron_chestplate",
      "legs": "minecraft:iron_leggings",
      "feet": "minecraft:iron_boots"
    },
    {
      "weight": 1,
      "head": "minecraft:diamond_helmet",
      "chest": "minecraft:diamond_chestplate",
      "legs": "minecraft:diamond_leggings",
      "feet": "minecraft:diamond_boots"
    }
  ]

  ## Item Entry Format

  Basic item entry:

  {
    "item": "minecraft:diamond_sword",
    "weight": 3
  }

  Optional mod-gated entry:

  {
    "item": "some_mod:my_weapon",
    "weight": 2,
    "requires_mod": "some_mod"
  }

  Meaning:

  - item: item id
  - weight: weighted random chance
  - requires_mod: optional mod id, only used if that mod is loaded

  ## Example Tier 2 File

  {
    "schema_version": 1,
    "rules": {
      "enchant_chance": 1.0,
      "damage_percent_min": 0.0,
      "damage_percent_max": 0.85
    },
    "mainhand": [
      { "item": "minecraft:diamond_sword", "weight": 3 },
      { "item": "minecraft:diamond_axe", "weight": 2 },
      { "item": "minecraft:crossbow", "weight": 1 },
      { "item": "minecraft:bow", "weight": 1 }
    ],
    "ranged_mainhand": [
      { "item": "minecraft:crossbow", "weight": 1 },
      { "item": "minecraft:bow", "weight": 1 }
    ],
    "offhand": {
      "chance": 0.5,
      "entries": [
        { "item": "minecraft:shield", "weight": 85 },
        { "item": "minecraft:totem_of_undying", "weight": 15 }
      ]
    },
    "inventory": [
      { "item": "minecraft:iron_sword", "weight": 1 },
      { "item": "minecraft:stone_sword", "weight": 1 }
    ],
    "bonus_mainhand": {
      "chance": 0.05,
      "entries": [
        { "item": "minecraft:trident", "weight": 1 }
      ]
    },
    "armor_sets": [
      {
        "weight": 1,
        "head": "minecraft:iron_helmet",
        "chest": "minecraft:iron_chestplate",
        "legs": "minecraft:iron_leggings",
        "feet": "minecraft:iron_boots"
      },
      {
        "weight": 1,
        "head": "minecraft:diamond_helmet",
        "chest": "minecraft:diamond_chestplate",
        "legs": "minecraft:diamond_leggings",
        "feet": "minecraft:diamond_boots"
      }
    ]
  }

  ## Datapack Override Behavior

  If a datapack provides one of these files:

  - tier1.json
  - tier2.json
  - roamer.json

  then that datapack version is what the mod uses for that tier.

  This means players can replace the default tier loadout by shipping a datapack with the same file path.

  ## Current Limitations

  This first version supports:

  - exact item ids
  - weighted item pools
  - optional requires_mod
  - weighted armor sets
  - basic gear damage + enchant rules

  It does not currently support:

  - item tags
  - per-slot armor pools outside armor sets
  - merge/append behavior between datapacks
  - custom NBT/components in entries
  - advanced conditions beyond requires_mod

  ## Best Practice For Modpack Authors

  - use exact item ids for stable control
  - use requires_mod for optional compat entries
  - keep one loadout file per tier
  - if you want different styles, distribute different datapacks

  ## Summary

  This system exists so players and pack makers can fully control human spawn gear from JSON instead of code.

  The three tier files are the main extension points:

  - tier1.json
  - tier2.json
  - roamer.json

  If needed later, the schema can be expanded to support tags, custom components, and more advanced gear rules.

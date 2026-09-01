# Testing

Hostile Humans uses Forge GameTests on a dedicated server. The current source
contains domain-scoped `@GameTest` declarations. Always trust the
non-zero expected-test count printed by Forge for the effective run count.

## Fast feedback loop

Compile Java and resources first:

```bash
./gradlew compileJava processResources
```

Then run only the affected scopes in one server startup:

```bash
./gradlew runGameTestServer -PgameTestFilter=worldnavigation
./gradlew runGameTestServer -PgameTestFilter=persona,squad,shield
./gradlew runGameTestServer -PgameTestFilter=utility
```

Available scopes:

| Scope | Main coverage |
|---|---|
| `smoke` | entity registration and lifecycle |
| `persona` | persona dataset, factions, reservation, NBT |
| `squad` | squad targeting, alerts, retreat, memory |
| `equipment` | weapon selection, pickup, upgrades |
| `chestloot` | chest discovery, extraction, equipment, no-loss behavior |
| `cobweb` | tactical cobweb placement and limits |
| `shield` | combat tiers, shield state, weapon switching, critical damage |
| `utility` | offensive/defensive ender pearls and water-bucket rescue/recovery |
| `sound` | sound events, investigation, visible targeting |
| `worldnavigation` | pillar, bridge, mining, controller integration |
| `survival` | needs, resources, loot, crafting, hunting, furnace, sharing |
| `worldprogression` | persistent gear milestones, spawn contexts, natural Human initialization and bounds |
| `loadout` | seeded procedural quality, material locks, age scaling, armor, enchantments and rare utility |
| `deathloot` | current inventory/equipment conservation, NBT preservation, Vanishing and XP rewards |

Unknown scopes fail during Gradle configuration. Always confirm that the GameTest summary reports a non-zero expected test count.

`gameTestFilter` limits the GameTest Java classes compiled into the development run. It does not change production classes. Running without the property restores and compiles every GameTest class.

## Impact guide

- `entity/ai/survival/**` and survival recipes: `survival`.
- `entity/ai/action/**`: `worldnavigation`; add `cobweb` for cobweb/shared placement changes.
- `EnderPearlAction`, `WaterBucketAction`, `TacticalUtilityController`: `utility`; run the full suite because `Human` integration and configuration are shared.
- `entity/ai/squad/**` and squad goals: `squad`.
- `entity/ai/combat/**`, `MeleeAttackGoal`, shields: `shield`.
- `InvestigateSoundGoal` and game-event handling: `sound`.
- `ItemLootGoal`, `PickUpLoot`, `HumanLootPolicy`: `equipment,survival`.
- `ChestLootGoal`: `chestloot`.
- `MeleeWeaponSelector`, melee item tags, equipment reevaluation: `equipment,shield`.
- `persona/**` and `personas.json`: `persona,squad`.
- Shared entity/data/configuration, registries, Mixins, templates, Gradle, or uncertain impact: run the full suite.
- World progression or natural spawning: `worldprogression`; run the full suite because configuration, entity lifecycle and dimensions are shared.
- Death drops, XP, loadout balance or loot tables: `deathloot,loadout`; run the full suite because the vanilla death pipeline and economy are shared.

## Final gate

Focused tests are feedback, not final validation. After they pass, run the complete suite:

```bash
./gradlew runGameTestServer
```

Do not accept a zero-test run, a flaky rerun, or focused-only execution as a substitute for a green full suite.

Rendering, animation quality, and natural-looking movement remain manual checks.

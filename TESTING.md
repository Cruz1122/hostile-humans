# Testing

Hostile Humans uses Forge GameTests on a dedicated server. The suite contains 100 tests in 10 domain scopes.

## Fast feedback loop

Compile Java and resources first:

```bash
./gradlew compileJava processResources
```

Then run only the affected scopes in one server startup:

```bash
./gradlew runGameTestServer -PgameTestFilter=worldnavigation
./gradlew runGameTestServer -PgameTestFilter=persona,squad,shield
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
| `sound` | sound events, investigation, visible targeting |
| `worldnavigation` | pillar, bridge, mining, controller integration |
| `survival` | needs, resources, loot, crafting, hunting, furnace, sharing |

Unknown scopes fail during Gradle configuration. Always confirm that the GameTest summary reports a non-zero expected test count.

`gameTestFilter` limits the GameTest Java classes compiled into the development run. It does not change production classes. Running without the property restores and compiles every GameTest class.

## Impact guide

- `entity/ai/survival/**` and survival recipes: `survival`.
- `entity/ai/action/**`: `worldnavigation`; add `cobweb` for cobweb/shared placement changes.
- `entity/ai/squad/**` and squad goals: `squad`.
- `entity/ai/combat/**`, `MeleeAttackGoal`, shields: `shield`.
- `InvestigateSoundGoal` and game-event handling: `sound`.
- `ItemLootGoal`, `PickUpLoot`, `HumanLootPolicy`: `equipment,survival`.
- `ChestLootGoal`: `chestloot`.
- `MeleeWeaponSelector`, melee item tags, equipment reevaluation: `equipment,shield`.
- `persona/**` and `personas.json`: `persona,squad`.
- Shared entity/data/configuration, registries, Mixins, templates, Gradle, or uncertain impact: run the full suite.

## Final gate

Focused tests are feedback, not final validation. After they pass, run the complete suite:

```bash
./gradlew runGameTestServer
```

Do not accept a zero-test run, a flaky rerun, or focused-only execution as a substitute for a green full suite.

Rendering, animation quality, and natural-looking movement remain manual checks.

---
name: forge-gametest-debug-workflow
description: Use when developing or debugging Minecraft Forge mods. Builds minimal reproducible scenarios, GameTests, and targeted debugging workflows before changing behavior. Avoids manual repetition, oversized test arenas, speculative fixes, and permanent debug noise.
compatibility: opencode
metadata:
  category: minecraft-development
  risk: medium
---

# forge-gametest-debug-workflow

## Purpose

Establish a reproducible development and debugging workflow for Minecraft Forge mods using:

- Minimal manual scenarios.
- Deterministic GameTests.
- Targeted breakpoints only when a test or scenario fails.
- HotSwap for safe method-body changes.
- Runtime logs and transformed Mixin inspection when necessary.

The goal is not to eliminate every Minecraft restart. The goal is to minimize manual reproduction and isolate failures quickly.

## Activation triggers

Use this skill when:

- Creating or debugging a Minecraft Forge mod.
- Adding AI, entities, pathfinding, combat, navigation, spawning, sounds, inventories, or world interaction.
- The user repeatedly restarts Minecraft to test changes.
- A behavior must be reproduced consistently.
- A bug happens only under a specific in-game setup.
- The task involves GameTests, `runClient`, `runGameTestServer`, Mixins, or HotSwap.
- The user asks for a debug environment or test scenario.

## Core principles

### Reproduce before changing

Do not modify implementation code until the failure or expected behavior can be reproduced.

Create the smallest scenario that demonstrates exactly one behavior.

Bad:

- Large arena containing doors, mobs, traps, loot, caves, water, and several NPCs.
- One scenario used to test every subsystem.
- Manual setup repeated after every run.

Good:

- One NPC.
- One stimulus.
- One expected outcome.
- Known coordinates.
- Known initial state.
- Resettable with one command.

### Scenarios and GameTests serve different purposes

Manual scenarios validate qualitative behavior:

- Does movement look natural?
- Does the NPC hesitate correctly?
- Does it expose information it should not know?
- Does the interaction feel believable?

GameTests validate deterministic properties:

- Entity exists after 20 ticks.
- Entity moves within a target radius.
- Door becomes open.
- Item enters an inventory.
- Forbidden block remains intact.
- Target is cleared after a timeout.

Do not expect a GameTest to validate whether AI “looks human”.

### Breakpoints are reactive

Do not make breakpoint configuration the main deliverable.

Use breakpoints only after:

1. A scenario reproduces the issue.
2. A GameTest captures the expected property when possible.
3. The failure is not obvious from logs or assertions.

Place breakpoints in the smallest relevant decision point:

- `canUse`
- `canContinueToUse`
- `start`
- `tick`
- `stop`
- `setTarget`
- `hurt`
- path creation
- entity removal
- state transitions

Avoid unconditional breakpoints in methods executed every tick.

### Inspect real repository state

Never assume:

- `mod_id`
- entity IDs
- registry names
- package names
- main mod class
- Forge version
- mappings
- Gradle tasks
- GameTest namespace
- class names
- Mixin configuration

Read them from the repository.

Registry IDs must be derived from actual registration strings, not constant names.

## Workflow

### Phase 1: Establish the baseline

Verify:

- Correct Java version.
- Gradle Wrapper works.
- Project compiles.
- `runClient` starts.
- Mod loads.
- Target entity can be spawned.
- Runtime resources are copied correctly.
- Relevant logs are available.
- Git working tree is understood before changes.

Do not refactor the mod during baseline setup.

### Phase 2: Create a minimal manual scenario

Create an idempotent function under:

```text
src/main/resources/data/<real_mod_id>/functions/debug/
```

Naming examples:

```text
spawn.mcfunction
investigate_sound.mcfunction
melee_combat.mcfunction
pathfinding.mcfunction
open_door.mcfunction
retreat.mcfunction
loot.mcfunction
underground_ambush.mcfunction
```

Each scenario should:

1. Remove only entities created by that scenario.
2. Use a unique debug tag.
3. Prepare a small safe area.
4. Teleport only the executing player when appropriate.
5. Spawn the minimum required entities.
6. Apply glowing or visible naming when useful.
7. Disable unrelated random behavior where possible.
8. Print a diagnostic success or failure message.
9. Be safe to execute repeatedly.
10. Avoid modifying unrelated world areas.

Use `@s` for the command executor unless all players are intentionally targeted.

Do not use broad selectors such as `@e` without a type, tag, distance, or other restrictive filter.

### Phase 3: Create a smoke GameTest

The first GameTest should test infrastructure, not complex AI.

Minimum smoke test:

1. Create the registered entity.
2. Place it inside the template.
3. Add it to the server level.
4. Wait 20–40 ticks.
5. Assert that it exists.
6. Assert that it is alive.
7. Assert that it has not been removed.
8. Complete successfully.

Do not start with combat, sound tracking, pathfinding, or multi-entity behavior.

### Phase 4: Add behavior-specific GameTests

Each GameTest should have:

- One behavior.
- One main assertion.
- Deterministic initial state.
- Explicit timeout.
- Clear failure message.
- No dependency on random natural spawning.
- No dependency on player input.
- No unnecessary client rendering.

Examples:

#### Sound investigation

Given:

- NPC at position A.
- Sound stimulus at position B.

Assert:

- NPC approaches within a defined radius of B within a fixed tick budget.

#### Door interaction

Given:

- NPC and target separated by a closed door.

Assert:

- Door opens or NPC reaches the target side within a fixed tick budget.

#### Retreat

Given:

- NPC below a configured health threshold.
- Threat nearby.
- Escape path available.

Assert:

- Distance from the threat increases after a fixed number of ticks.

#### Looting

Given:

- Item entity near NPC.

Assert:

- Item is removed from the world and appears in the NPC inventory.

#### Controlled block breaking

Given:

- Allowed block between NPC and target.
- Forbidden block beside it.

Assert:

- Allowed block is removed.
- Forbidden block remains.

### Phase 5: Debug only the failing behavior

When a GameTest fails:

1. Run the relevant test in Debug.
2. Place breakpoints in the specific Goal or policy involved.
3. Filter by debug tag, entity ID, test name, or server-side state.
4. Prefer logpoints in frequently executed methods.
5. Verify `level().isClientSide == false` for server decisions.
6. Inspect current target, navigation path, state, cooldowns, and memory.
7. Remove temporary breakpoints and debug code after fixing the issue.

### Phase 6: Validate and commit

Run:

- Relevant focused tests.
- Full GameTest suite.
- Java compilation.
- Resource processing.
- Client smoke launch when the change affects rendering or synchronization.

Review:

```text
git status
git diff
git diff --cached
```

Do not commit:

```text
run/
build/
.gradle/
.idea/
*.log
```

unless repository policy explicitly requires specific IDE configuration files.

## HotSwap policy

HotSwap is appropriate for changes inside existing method bodies, such as:

- Conditions.
- Numeric thresholds.
- Calculations.
- Logging.
- Return values.
- Branch ordering.

Assume a restart is required for:

- New or removed fields.
- New or removed methods.
- Signature changes.
- Constructor changes.
- Registry changes.
- Mixin changes.
- Access Transformer changes.
- Gradle changes.
- Static initialization changes.
- Resource registrations.

Do not claim HotSwap succeeded without observing the changed behavior or log in the active JVM.

## Mixin debugging

Before adding debug properties, inspect existing Gradle and Mixin configuration.

Do not duplicate:

```text
mixin.debug.export
mixin.debug.verbose
```

When export is enabled, inspect:

```text
run/.mixin.out/classes/
```

Use transformed classes to verify whether injections, redirects, accessors, and overwritten methods were applied as expected.

Do not edit Mixins merely because a failure exists. Establish evidence that the transformed class is related to the failure.

## Resource reload policy

For resource changes:

1. Save the source file.
2. Run the repository’s correct resource-processing task.
3. Verify the processed file under `build/resources/main` or the actual runtime output.
4. Run `/reload` for server data.
5. Use `F3 + T` for client resources where applicable.
6. Re-execute the scenario.

Do not assume IntelliJ copied a modified `.mcfunction` merely because the source file was saved.

## Linux command policy

For Arch Linux and Arch-based systems:

- Explain what every command does.
- Do not use `sudo` unless system-level package or configuration changes are required.
- Prefer repository-local Gradle Wrapper commands.
- Never recommend installing global Gradle when `./gradlew` exists.
- Provide official documentation for Forge, Gradle, IntelliJ, Java, Mixin, or Arch packages when relevant.

## Evidence requirements

When inspecting or modifying a repository, report:

- Exact file path.
- Actual class or method name.
- Relevant code fragment.
- Command executed.
- Result obtained.
- Whether a conclusion is verified or only a hypothesis.

Do not state that something works unless it was compiled, executed, tested, or directly verified.

## Non-goals

This skill must not:

- Refactor unrelated code.
- Rename the mod.
- Rename packages.
- Change Forge or Minecraft versions.
- Upgrade Gradle without necessity.
- Add new AI before the baseline is stable.
- Build a huge generic test arena.
- Create dozens of speculative tests.
- Leave permanent debug output in tick loops.
- Replace deterministic code with an LLM.
- Treat manual observation as an automated assertion.

## Completion criteria

A debugging baseline is complete when:

- `runClient` starts successfully.
- The target entity spawns in a minimal scenario.
- The scenario is idempotent.
- Resource changes reach the runtime.
- A smoke GameTest compiles.
- `runGameTestServer` executes the smoke test successfully.
- The GameTest server can be launched in Debug when needed.
- One targeted breakpoint has been verified.
- Temporary debug output has been removed.
- Changes are isolated in a clean commit.
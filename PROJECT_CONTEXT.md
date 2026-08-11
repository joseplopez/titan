# Titan's Heart - Project Context

## Project Overview
An idle/incremental tapper for Android where the player chips **Crystal Shards** off a sleeping **stone titan**, recruits **Sprites** to automate mining, and explores the depths of the Titan's Heart through **The Descent**.

## Core Mechanics (Implemented)

### Combat & Collection
- **Manual Tapping:** Deals damage and spawns shards exactly at the click point.
- **Precision Cracks:** Weak points (red circles) spawn on the Titan; tapping them deals **5x damage**.
- **Strikers (DPS):** Automated blue motes in orbit that fire arcane bolts toward the Heart. Impact triggers a shard burst.
- **Gatherers (CPS):** Green motes that orbit the golden shard pile at the bottom to bank shards automatically.
- **Thorns (Advanced CPS):** Yellow/Amber motes that zip around the pile to provide massive collection boosts.
- **Shard Pile:** Uncollected shards physically accumulate at the base of the Titan.

### Progressive Layers: "The Descent"
The prestige loop is framed as breaking through geode layers of the Titan's Heart.
1. **The Amber Shell (Layer 1):** Standard stone layer.
2. **The Frozen Marrow (Layer 2):** Icy theme. **Twist:** Brittle-resistance (80% damage reduction without Frost Sprites).
3. **The Ember Core (Layer 3):** Fiery theme. **Twist:** HP Regeneration (requires sustained DPS threshold).
4. **The Verdant Hollow (Layer 4):** Nature/Root theme.

### Meta-Progression: The Constellation
A three-tab permanent talent system fueled by **Starlight**:
- **Might Tree:** Damage, Crack power (Unlocked by default).
- **Craft Tree:** Economy, Cost reduction (Unlocks at Layer 2).
- **Wild Tree:** Capacity, Elemental potency (Unlocks at Layer 3).

## Technical Architecture
- **Language/Framework:** Kotlin, Jetpack Compose.
- **UI Architecture:** MVVM (ViewModel + StateFlow).
- **Persistence:** `androidx.datastore` with `kotlinx-serialization` for full game state.
- **Visuals:** High-precision coordinate system centered on the Heart for all particles/projectiles.
- **Events:** `SharedFlow` based event system for visual impact triggers (Striker hits, Shard collection).

## Monetization (Scaffolded)
- **Boost Mechanic:** Watch a (simulated) ad for 2X Power for 1 minute.
- **Store:** IAP placeholder for "Remove Ads" and permanent boost.

## Project Structure
- `com.centelles.titan.logic`: Game engine, State, and Layer definitions.
- `com.centelles.titan.ui`: Compose screens, including the dynamic `TitanScreen` and `ConstellationScreen`.
- `com.centelles.titan.ui.components`: Custom Material 3 "Arcane" UI components.
- `com.centelles.titan.data`: Repository and DataStore persistence logic.
- `com.centelles.titan.audio`: Sound management (scaffolded).

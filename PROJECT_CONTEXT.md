# Titan's Heart - Project Context

## Project Overview
An idle/incremental tapper for Android where the player chips **Crystal Shards** off a sleeping **stone titan**, recruits **Sprites** to automate mining, and explores the depths of the Titan's Heart through **The Descent**.

## Lore & Story
Before the world had a name, a Titan walked it. It did not wage war, nor seek dominion. It simply slept—curled beneath the ancient mountains, dreaming a dream so vast that stars formed in its wake. 

Over eons, its dreaming heart crystallized into a core of living crystal, humming with the memory of everything it had ever seen. While kingdoms rose and fell upon its shoulders, the Titan remained undisturbed. In time, the last of the old fey folk—small, curious, and utterly fearless creatures who call themselves **Sprites**—found the crack in the stone that led to its heart.

The Sprites do not know if waking the Titan will end the world or begin a new one. They intend to find out.

## Core Mechanics (Implemented)

### Combat & Collection
- **Manual Tapping:** Deals damage and spawns shards exactly at the click point.
- **Precision Cracks:** Weak points (red circles) spawn on the Titan; tapping them deals **5x damage**.
- **Stage Progression:** A "Stage X" display tracks progress through the current layer; defeating a Titan advances the stage and increases HP scaling.
- **Strikers (DPS):** Blue orbital motes that fire arcane bolts toward the Heart. 
- **Gatherers (CPS):** Green motes that patrol the ground area to collect shards. Now feature multi-wave harmonic movement.
- **Thorns (Advanced CPS):** Red motes with organic, non-repetitive patrol paths and dynamic acceleration. They surgically "harvest" landed shards on contact.
- **Elemental Pulses:** As you recruit **Ember** and **Frost** sprites, small randomized fire (red) and ice (blue) energy pulses appear inside the Titan's heart.
- **Shard Ground Area:** A fixed-height rectangular area at the screen's base where shards physically land and accumulate. Uses an entity-based system for individual cleanup.

### Progressive Layers: "The Descent"
The prestige loop is framed as breaking through geode layers of the Titan's Heart.
1. **The Amber Shell (Layer 1):** Standard stone layer.
2. **The Frozen Marrow (Layer 2):** Icy theme with a deep indigo blue core for high contrast. **Twist:** Brittle-resistance (80% damage reduction without Frost Sprites).
3. **The Ember Core (Layer 3):** Fiery theme. **Twist:** HP Regeneration.
4. **The Verdant Hollow (Layer 4):** Nature/Root theme.

### Meta-Progression: The Constellation
A three-tab permanent talent system fueled by **Starlight**:
- **Might Tree:** Damage, Crack power.
- **Craft Tree:** Economy, Cost reduction (Unlocks at Layer 2).
- **Wild Tree:** Capacity, Elemental potency (Unlocks at Layer 3).

## Technical Architecture
- **Language/Framework:** Kotlin, Jetpack Compose.
- **UI Architecture:** MVVM (ViewModel + StateFlow).
- **Persistence:** `androidx.datastore` with `kotlinx-serialization`.
- **Physics & Animation:** 
    - Entity-based feedback system for particles and damage numbers.
    - Multi-wave harmonic sine system for organic sprite movement.
    - Synchronized collision engine between falling particles and patrolling sprites.
- **Internationalization:** Full string extraction into `strings.xml` for localization readiness.
- **Events:** `SharedFlow` based event system for visual impact triggers.

## Monetization (Scaffolded)
- **Boost Mechanic:** 2X Power boost (simulated ad-supported).
- **Store:** IAP placeholder for "Remove Ads" and permanent rewards.

## Project Structure
- `com.centelles.titan.logic`: Game engine, State, and Layer definitions.
- `com.centelles.titan.ui`: Compose screens (`TitanScreen`, `ConstellationScreen`, `UpgradesScreen`).
- `com.centelles.titan.ui.components`: Custom Material 3 "Arcane" UI components.
- `com.centelles.titan.data`: Repository and DataStore persistence logic.

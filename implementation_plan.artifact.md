# Titan: Global Growth & Feature Roadmap

This document outlines the strategic feature additions for **Titan** based on top-performing Idle RPG/Clicker games in the Asian markets (China, Japan, South Korea) for 2024–2025.

---

## 🚀 Strategic Goals
1.  **Retention (D1/D7)**: Give players a reason to return every day.
2.  **Monetization**: Increase the value of Rewarded Ads and potential IAPs.
3.  **Engagement**: Make "active" play (tapping) more rewarding and less of a chore.

---

## 📋 Feature List & Implementation Plans

### 1. Titan’s Blessing (Offline Progress)
*   **Concept**: Reward players for the time they were away.
*   **Priority**: High (Essential for Retention)
*   **Implementation Plan**:
    *   **Logic**: Save `System.currentTimeMillis()` in `GameState` whenever the app closes or saves.
    *   **Calculation**: On app launch: `offline_seconds = (now - last_save) / 1000`.
    *   **Reward**: `shards_earned = offline_seconds * total_cps * offline_multiplier`.
    *   **UI**: A custom "Arcane Panel" popup showing the earned shards with a "Claim" and a "Double with Ad" button.

### 2. The Lucky Shard (Active Interruption)
*   **Concept**: A rare golden shard that spawns randomly to reward active players.
*   **Priority**: High (Increases "Juice" & Fun)
*   **Implementation Plan**:
    *   **Trigger**: A 1% chance on every tap to spawn a `LuckyShard`.
    *   **Behavior**: A golden sprite that floats across the screen for 5 seconds.
    *   **Reward**: Tapping it triggers "Arcane Frenzy" (5x Damage for 15 seconds) or a massive shard windfall.
    *   **UI**: Use a unique gold particle effect and a screen-edge "Frenzy" glow.

### 3. Fey Quests (Daily Goals)
*   **Concept**: Daily tasks that reset every 24 hours.
*   **Priority**: Medium
*   **Implementation Plan**:
    *   **Tasks**: Examples: "Collect 10M Shards," "Tap 1,000 times," "Recruit 10 Strikers."
    *   **Logic**: A simple list in the `UpgradesScreen`. Check progress against current session stats.
    *   **Reward**: Small amounts of **Starlight** (the most valuable currency).

### 4. Spirit Harmony (Synergy Bonuses)
*   **Concept**: Bonuses for maintaining specific ratios of Sprites.
*   **Priority**: Medium (Adds Strategy)
*   **Implementation Plan**:
    *   **Logic**: In `GameState`, calculate "Harmony Levels."
    *   **Example**: 10 Strikers + 10 Gatherers = "Working Class Harmony" (+10% Shard value).
    *   **UI**: Add a "Harmony" indicator in the Spirit Recruitment box.

### 5. Global Descent Leaderboard
*   **Concept**: Show players where they rank against the world.
*   **Priority**: Medium (Uses existing Firebase)
*   **Implementation Plan**:
    *   **Logic**: Use **Firebase Realtime Database** or **Firestore** to push `deepestLayerReached` whenever a user performs a Descent.
    *   **UI**: A simple "Rankings" button in the Constellation screen.

---

## 🛠 Technical Requirements

| Component | Requirement |
| :--- | :--- |
| **Storage** | Update `GameState` and `Room` database to include `lastSaveTimestamp`. |
| **Analytics** | Log `lucky_shard_tapped` and `quest_completed` events. |
| **UI** | New custom `Dialog` components for Offline Progress and Quests. |

---

## 📈 Next Steps Recommendation

1.  **Phase 1**: Implement **Offline Progress**. It provides the immediate dopamine hit needed for Day 2 retention.
2.  **Phase 2**: Add the **Lucky Shard**. It makes the main game screen feel alive and reactive.
3.  **Phase 3**: Global Leaderboards to build community and competition.

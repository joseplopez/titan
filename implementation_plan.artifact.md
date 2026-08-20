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
*   **Priority**: High (Essential for Retention)
*   **Implementation Plan**:
    *   **Logic**: Save `System.currentTimeMillis()` in `GameState` whenever the app closes or saves.
    *   **Calculation**: `offline_seconds = (now - last_save) / 1000`.
    *   **Reward**: `shards_earned = offline_seconds * total_cps * offline_multiplier`.
    *   **UI**: "Arcane Panel" popup with "Claim" and "Double with Ad".

### 2. The Lucky Shard (Active Interruption)
*   **Priority**: High (Increases "Juice")
*   **Implementation Plan**:
    *   **Trigger**: 1% chance per tap to spawn a `LuckyShard`.
    *   **Behavior**: Golden sprite floats for 5s.
    *   **Reward**: "Arcane Frenzy" (5x Damage for 15s) or shard windfall.

### 3. Fey Quests (Daily Goals)
*   **Priority**: Medium
*   **Tasks**: "Collect 10M Shards," "Tap 1,000 times," "Recruit 10 Strikers."
*   **Reward**: **Starlight** (Premium Currency).

### 4. Spirit Harmony (Synergy Bonuses)
*   **Priority**: Medium
*   **Logic**: 10 Strikers + 10 Gatherers = "+10% Shard value."
*   **UI**: "Harmony" indicator in recruitment box.

### 5. Global Descent Leaderboard
*   **Priority**: Medium
*   **Tech**: Firebase Realtime Database to push `deepestLayerReached`.

### 6. Descent Progression Expansion
*   **New Spirits**: Unlock a unique spirit for every Descent level.
*   **Layer Handicaps**: Add more layers with specific damage handicaps/modifiers.
*   **UI**: Describe layer-specific handicaps in the Descent UI.

### 7. Statistics Screen
*   **Concept**: Detailed breakdown of DPS/CPS calculations.
*   **UI**: Modal triggered by clicking the DPS/CPS values.

---

## 🛠 Technical Requirements

| Component | Requirement |
| :--- | :--- |
| **Storage** | Update `GameState` and `Room` to include `lastSaveTimestamp`. |
| **Analytics** | Log `lucky_shard_tapped` and `quest_completed` events. |
| **UI** | New `Dialog` components for Offline Progress, Quests, and Statistics. |

---

## 📈 Next Steps Recommendation

1.  **Phase 1**: Implement **Offline Progress**. It provides the immediate dopamine hit needed for Day 2 retention.
2.  **Phase 2**: Add the **Lucky Shard**. It makes the main game screen feel alive and reactive.
3.  **Phase 3**: Global Leaderboards to build community and competition.

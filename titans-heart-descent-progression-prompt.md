# Titan's Heart — Deep Progression: "The Descent" System
### Follow-up prompt for Gemini Code Assist (Android Studio)

This replaces the current flat Rebirth → flat talent-list loop with a structured long-term progression system. It builds on `PROJECT_CONTEXT.md` — read that first for terminology (Shards, Sprites, DPS/CPS, Cracks, Awakenings, Starlight).

**Problem being solved:** Right now Rebirth is a discretionary reset button available almost immediately, feeding into one undifferentiated upgrade list. There's no reason *not* to rebirth constantly, no sense of going somewhere, and no long-term structure. This prompt fixes that with (1) a narrative/mechanical reframe of what rebirth *is*, (2) a hard gate on when it's available, and (3) a real branching meta-progression system to spend Starlight on.

---

## 1. Reframe: Rebirth → "The Descent"

**Concept:** The Titan's Heart isn't one crystal — it's a geode with layers. You're not resetting progress each rebirth, you're **cracking fully through the current layer and descending into the next one**, which is harder but was always there waiting.

- Rename the mechanic from "Rebirth" to **"The Descent"** throughout UI/code (keep `Starlight` as the currency name — that still fits: light glimpsed as you break through each layer).
- Add a `currentLayer: Int` (starts at 1) to game state, persisted permanently (not reset by anything).
- Each layer has its own name, flavor text, visual palette shift, and difficulty multiplier. Suggested first few:

| Layer | Name | Flavor | Visual shift |
|---|---|---|---|
| 1 | The Amber Shell | Sun-warmed outer stone, the part the world above has always known | Current palette (as-is) |
| 2 | The Frozen Marrow | Colder, older — the Titan's dreams turn to ice here | Shift accents toward pale blue/white, crack-glow cools |
| 3 | The Ember Core | Something is still burning at its center, after all this time | Shift accents toward deep red/orange, cracks glow hot |
| 4 | The Verdant Hollow | Root-systems from the world above have grown all the way down, undisturbed for millennia | Green/gold accent shift, organic crack patterns |
| 5+ | (design as you go) | — | — |

- Each layer should introduce **one new mechanical wrinkle**, not just bigger numbers — e.g.:
  - Layer 2 (Frozen Marrow): parts of the Titan are "brittle-resistant," dealing reduced damage unless you have Frost Sprites active — pushes the player toward using the elemental system you already built, rather than pure numeric scaling.
  - Layer 3 (Ember Core): the Heart slowly regenerates HP unless sustained DPS is kept above a threshold — rewards active building rather than one big burst.
  - Keep later layers' twists as open design TODOs; don't over-specify past layer 3 yet.

---

## 2. Gating: when can you Descend?

- **Hard gate (per your call):** the Descend action is only available after fully clearing the current layer's **final Awakening stage** (i.e., you must reach the last stage of Layer N before Layer N+1 unlocks) — no early-exit prestige button sitting there tempting you from minute one.
- Until that point, the Descend UI element should be visibly present but locked (e.g., grayed out with "Reach Stage X to Descend" text) so the player always knows it's the long-term goal, not hidden.
- On Descend: Starlight earned scales off total lifetime Shard/damage progress *within that layer's run*, and the next run starts at Layer N+1 (harder base Titan HP/resistances) — Shards, Sprites, Groves, and current-run Awakening stage reset as before, but `currentLayer` and all Constellation talents persist.

---

## 3. The Constellation: three unlockable talent trees

Replace the current single flat upgrade-style talent list with three distinct permanent trees, spent using Starlight, each unlocked progressively:

| Tree | Theme | Unlocks |
|---|---|---|
| **Might** | Damage — click power, crit/Crack frequency, Striker effectiveness | Available from the very first Descent |
| **Craft** | Economy — Shard yield, Gatherer effectiveness, cheaper buildings, faster Sprite recruitment cost scaling | Unlocks after your **1st** Descent (reaching Layer 2) |
| **Wild** | Sprites & elements — elemental sprite potency, population cap bonuses, faster elemental unlock costs | Unlocks after your **2nd** Descent (reaching Layer 3) |

**Structure per tree:**
- 10–15 nodes each to start (expandable later), laid out as a small branching path rather than a flat list — a few nodes should be **choice nodes** (pick one of two mutually exclusive upgrades) so players make identity-defining decisions, not just "buy everything eventually."
- Visually: a simple node-and-connector graph (Canvas-drawn lines between circular/hex nodes, using the existing arcane design tokens — locked nodes dim, available nodes glow `SpectralCyan`, purchased nodes fill solid).
- Each tree screen reachable from a "Constellation" tab, separate from the in-run Upgrades & Store screen (in-run upgrades reset on Descend; Constellation nodes never do — keep these two systems visually distinct so the player never confuses temporary vs. permanent power).

---

## 4. Story reveal per Descent

- On completing each Descent (arriving at a new layer for the first time), show a short full-screen narrative beat — reuse the Story Intro screen's presentation style (fade-in text over the parallax starfield, "Continue" button) but keep it brief: 1–2 short paragraphs per layer, not a full replay of the opening story.
- Suggested tone continuation from the original intro: each layer reveals a little more about *why* the Titan sleeps and what the Sprites are actually going to find at the center. Keep the ending ambiguous/open until you decide how deep the game actually goes.
- Persist a flag per layer (`hasSeenLayerIntro[layerNumber]`) so these don't replay on normal app relaunch, only the first time a layer is reached — add a "Story Log" screen where previously-seen layer reveals can be reread, so players who blow through a Descent don't lose the payoff.

---

## 5. Data model changes (guidance for Gemini)

- Add `currentLayer: Int` and `deepestLayerReached: Int` to permanent save state.
- Restructure Starlight-spend data from a flat list into three keyed node-graphs (`mightTree`, `craftTree`, `wildTree`), each node with `id`, `cost`, `prerequisiteIds`, `isChoiceGroup` (optional group id for mutually-exclusive nodes), and `purchased: Boolean`.
- Layer definitions (`name`, `flavorText`, `baseHpMultiplier`, `paletteShift`, `mechanicalTwist`) should live in a simple data table/list, not hardcoded per-screen, so adding Layer 6, 7, 8... later is just adding a row.
- The "Descend" button's enabled/locked state should derive from `currentAwakeningStage >= finalStageOfCurrentLayer`, not a separate flag that could drift out of sync.

---

## 6. Suggested build order

1. Data model: `currentLayer`, layer definitions table, restructure talent data into three trees.
2. Gate the Descend button correctly (locked until final stage of current layer); rename UI copy from "Rebirth" to "Descend."
3. Build the Constellation screen(s) — node-graph UI for one tree first (Might), verify purchase/prerequisite logic, then duplicate pattern for Craft and Wild with their unlock gates.
4. Wire layer transition on Descend: increment `currentLayer`, apply new layer's base multiplier/mechanical twist, trigger the layer story-reveal screen.
5. Implement the first mechanical twist (Layer 2 elemental-resistance) as the template for future layers.
6. Add the Story Log screen for rereading past layer reveals.

---

## 7. Open decisions to confirm as you go
- Exact Starlight-earned formula per layer (recommend scaling with both total run damage *and* a layer-depth multiplier, so deeper layers are worth proportionally more, reinforcing "go deeper" over "grind layer 1 forever").
- Final node counts and specific choice-node pairs per tree (design/balance pass once the graph UI itself works).
- How far the layer list is planned/authored in advance vs. designed incrementally as you build (recommend fully designing layers 1–5 now, leaving room to add more later without a rework).

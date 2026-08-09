# Project Context: Titan's Heart

**Purpose of this file:** Quick-reference context for Gemini Code Assist. Keep this open/attached alongside the codebase so Gemini understands the game's concept, current state, and conventions before making changes — it is *not* a build prompt itself, just grounding.

---

## 1. What this game is

**Titan's Heart** is an Android incremental/idle tapper. Inspired mechanically by *(the) Gnorp Apologue* (Steam), reskinned into an original fantasy setting — no copied names, art, or text, only the underlying loop structure.

**One-line pitch:** The player chips Crystal Shards off a sleeping stone Titan, recruits Sprites to automate mining and collecting, unlocks elemental Sprite types and buildings, and periodically triggers a Rebirth (prestige) that resets the run but grants permanent talents.

**Explicitly NOT included:** offline/idle progress accrual (game only advances while foregrounded and active).

---

## 2. Core loop & terminology

| Term | Meaning |
|---|---|
| **Titan's Heart** | The tap target — a stone titan's crystal core. Has HP per stage. |
| **Crystal Shards** | Main currency, knocked loose by hits, must be collected (swept or auto-gathered). |
| **Sprites** | Worker units, recruited with Shards. Two roles: **Striker** (adds DPS) and **Gatherer** (adds collection rate/CPS). |
| **DPS** | Damage per second — how fast the Heart's HP drops. |
| **CPS** | Collection rate — how fast loose Shards get banked. These two must stay balanced (Gnorp-derived tension): too much DPS piles up uncollected shards on the ground; too much CPS leaves Gatherers idle waiting on Strikers. |
| **Cracks** | Randomly-spawning weakspots on the Titan; tapping one within its lifetime window deals bonus/crit damage. |
| **Groves** | Buildings that raise the Sprite population cap. |
| **Elemental Sprites** | Later-unlock variants — Ember (damage/burn), Frost (slow/brittle amplify), Thorn (collection radius/speed). Unlocked via the Enchanted Garden. |
| **Titan Awakenings** | Stage milestones (~8–10 total). HP pool jumps, new buildings/sprites unlock, Titan's appearance evolves. |
| **Rebirth** | Manual prestige trigger. Resets the run (Shards, Sprites, Groves, Heart HP) but grants **Starlight** based on total run progress. |
| **Starlight** | Permanent meta-currency from Rebirth, spent on the **Constellation** talent tree (persists across all runs). |

---

## 3. Tech stack & architecture

- **Language:** Kotlin
- **UI:** Jetpack Compose (Material 3, but heavily reskinned — see design tokens below, do not rely on default Material look)
- **Architecture:** MVVM — ViewModel holds game state as `StateFlow`; Compose UI observes it; a coroutine-based game loop (~10–20 ticks/sec) drives passive DPS/CPS while the app is foregrounded.
- **Persistence:** Room or DataStore — stores current run state, permanent Constellation/Starlight progress, and settings (haptics, SFX/music volume, reduced motion, `hasSeenIntro` flag). No elapsed-time-since-close calculation on load (no offline progress).
- **Numbers:** Values escalate fast — use `Double` or a big-number formatter (avoid Int/Long overflow) with a K/M/B/e-notation display utility.
- **Monetization:** Google Mobile Ads SDK (rewarded + interstitial, frequency-capped, never gating core progress) + Google Play Billing (remove-ads, cosmetics, currency bundles, starter pack). Nothing should be pay-gated that blocks full play.
- **Assets:** Procedural only — everything is drawn in Compose Canvas (vector paths, gradients, radial glows). No bundled image/PNG assets. Fonts pulled via the Compose downloadable Google Fonts provider at runtime rather than bundled files.

---

## 4. Design tokens (visual identity — apply consistently to any new UI)

**Palette — "Cool & Mystical / Arcane"**
| Token | Hex | Use |
|---|---|---|
| `VoidIndigo` | `#140F26` | Base background |
| `ArcanePurple` | `#4B2E83` | Panels, card surfaces |
| `MysticBlue` | `#2B4A8F` | Secondary surfaces, borders |
| `SpectralCyan` | `#5EE7FF` | Primary glow/accent — cracks, energy, sprite trails |
| `EmberGold` | `#FFD166` | Currency, rewards, highlights |
| `CrackMagma` | `#FF4D6D` | Weakspot cracks, danger/urgency |
| `MoonMist` | `#E8E3F7` | Primary text on dark surfaces |

**Typography:** Display face **Cinzel** (titles, stage names, story text, used sparingly) + body/UI face **Inter** or Roboto (numbers, buttons — legibility-first since this is a numbers-heavy game). Tabular/monospace figures for live-ticking stat counters (Shards, DPS, CPS) so digits don't jitter.

**Signature element:** The Titan is Canvas-drawn (angular faceted silhouette, not a plain circle), with animated glowing crack-veins whose density scales continuously with HP loss. A slow-drifting multi-layer parallax starfield sits behind it. HUD elements are semi-transparent glassy panels with a thin glowing border ("arcane panel" style), not default Material cards/buttons.

---

## 5. Story / narrative frame

Fantasy premise: an ancient Titan has slept beneath the world since before recorded history; its dreaming heart crystallized into the crystal core the player mines. Sprites are the last of the old fey folk, small and fearless, who found their way to its heart and now work to (maybe) wake it. Full intro copy lives in the UI polish prompt (see Section 7) and is shown on a dedicated first-launch Story Intro screen, replayable from a menu.

---

## 6. Current implementation status

As of the last check-in:
- ✅ Core loop implemented and functionally working: tap-to-damage, Shard spawning/sweeping, Striker/Gatherer recruitment, DPS/CPS tracking, basic Upgrades & Store screen (Buildings, Titan Upgrades, Enchanted Garden unlocks), Remove Ads IAP entry, rewarded-ad power boost button.
- ⚠️ Visual layer is currently placeholder/default Material styling (flat gray circle for the Titan, plain dots for Cracks, stock buttons/cards) — a full visual + animation + story-intro pass has been scoped (see Section 7) but may or may not be implemented yet depending on when you're reading this file.
- ❓ Not yet confirmed/implemented as of this doc: Rebirth/Constellation talent tree, elemental Sprite variants (Ember/Frost/Thorn) beyond Thorn appearing in the store, Titan Awakening stage-jump content, sound system, haptics.

**When picking up work, check actual code state rather than assuming the above — this section reflects the plan, not necessarily every line already written.**

---

## 7. Related prompt documents

This file is a summary; these two documents contain the full detailed specs and should be referenced for implementation specifics:

1. **`titans-heart-gemini-prompt.md`** — original game design + build prompt (full system breakdown: Heart, Shards, Sprites, Buildings, Awakenings, Rebirth, technical requirements, phased build order).
2. **`titans-heart-ui-polish-prompt.md`** — visual/animation/story-intro pass (design tokens, Canvas redesign of the Titan, particles/juice, haptics, sound hooks, full Story Intro screen copy and sequencing).

---

## 8. Conventions to keep in mind when generating new code

- Keep game balance/logic in the ViewModel/domain layer, not inline in composables.
- Any new UI should reuse the shared "arcane panel" style and design tokens above rather than default Material components.
- No offline-progress math — game state simply freezes on background/close and resumes as-is.
- No image asset imports — new visuals should be Canvas/vector-drawn.
- Respect the no-pay-gate rule: new monetization hooks should be optional boosts/cosmetics, never required progress gates.

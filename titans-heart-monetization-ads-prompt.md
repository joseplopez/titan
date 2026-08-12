# Titan's Heart — Monetization: Rewarded Ads Only
### Follow-up prompt for Gemini Code Assist (Android Studio)

**Model:** 100% ad-monetized via Google AdMob **rewarded ads only** — no banners, no interstitials, no IAP of any kind. Every ad placement is opt-in (player taps a button to watch), grants a real gameplay advantage, and respects a per-placement cooldown so it can't be chain-watched to trivialize progress. Read `PROJECT_CONTEXT.md` and the Descent-progression prompt first for terminology (Shards, Starlight, Constellation, Descend).

---

## 1. Placements

| # | Placement | What it grants | Where it appears |
|---|---|---|---|
| 1 | **Watch ad for Shards** | Instant Shard reward, scaled to current progress | Upgrades & Store screen |
| 2 | **Watch ad to free-level an Upgrade or Constellation node** | Instantly grants the next level of a chosen Upgrade (in-run) *or* a chosen unlocked Constellation node (permanent) — player's choice of target, no currency spent | Upgrades & Store screen, and Constellation screen |
| 3 | **Watch ad for a temporary DPS/CPS multiplier** | Timed boost (e.g., 2x for 3 minutes) | Upgrades & Store screen |
| 5 | **Watch ad to boost this Descend's Starlight reward** | Increases the Starlight earned from the *current* Descend by a fixed percentage (e.g., +50%) | Descend confirmation screen — **only unlocked starting after the player's first Descend** (i.e., not shown/available the very first time, available from the second Descend onward) |

---

## 2. Reward scaling (must scale with progress, not be flat)

Flat rewards are the main way ad-monetized incrementals feel exploitative early and useless late. Scale each reward off the player's *current* production rather than a fixed number or a hand-authored per-stage table:

- **Shard reward (#1):** grant an amount equivalent to a fixed window of the player's current CPS — e.g., **~90-120 seconds worth of current Shard income** (`reward = currentCPS * 100`, tune the multiplier). This automatically stays proportionally meaningful at Stage 1 and Stage 8 alike without needing manual per-stage tuning. Floor it with a small minimum so it's not worthless at CPS 0-1 early on.
- **Free upgrade/node level (#2):** this is inherently self-scaling — it just grants "the next level" of whatever the player selects, at whatever that level currently costs to reach. No separate formula needed, but consider limiting selection to levels/nodes the player could otherwise almost-afford (see UX note below) rather than letting it jump arbitrarily far ahead.
- **DPS/CPS multiplier (#3):** a flat multiplier (e.g., 2x) and duration are fine here since it scales naturally with whatever the player's current DPS/CPS already is — no separate scaling formula needed.
- **Starlight boost (#5):** percentage-based (e.g., +50%) applied to the already-calculated Descend reward, so it scales with however much Starlight that run earned.

---

## 3. Cooldowns

Each of the 5 placements gets its **own independent cooldown timer** (recommend starting around 3-5 minutes each, tune by playtesting) — watching one doesn't affect the others' availability.

- Store last-watched timestamp per placement in local persisted state (DataStore/Room, same layer as other save data) — survives app restart, so players can't bypass cooldowns by force-closing the app.
- While on cooldown, the button should visibly show remaining time (e.g., a small countdown or disabled/greyed state with "Ready in 2:14") rather than just hiding — keeps the placement visible as a thing to look forward to.

---

## 4. UX / selection details

- **Placement #2 (free level-up)** needs a lightweight specific button per upgrade, on the left button for each specific upgrade cost place the button for free unlock
- **Placement #5 (Starlight boost)** should appear as an explicit optional step on the Descend confirmation flow — e.g., after confirming "Descend," before the transition plays, show "Watch an ad to boost this run's Starlight by 50%?" with a skip option — never block or delay the Descend itself if the player declines.
- All reward buttons should clearly telegraph what they grant *before* the ad plays (e.g., "Watch Ad: +1,240 Shards" with the actual computed number shown up front, not a surprise after watching).

---

## 5. AdMob technical implementation notes

- Use the Google Mobile Ads SDK's `RewardedAd` class exclusively — do not integrate banner or interstitial ad formats at all.
- **Only grant the reward inside the `onUserEarnedReward` callback**, never optimistically before the ad finishes — this is both a Play Store policy requirement and prevents reward exploits from partial ad views.
- Preload the next rewarded ad instance as soon as the current one is consumed/dismissed, so there's minimal wait when the player taps a placement button.
- Handle ad-load failures gracefully: if a rewarded ad fails to load, the placement button should show an "unavailable" or disabled state (e.g., "No ad available — try again soon") rather than crash or silently do nothing when tapped.
- Use AdMob's official test ad unit IDs during development; do not ship a build with test IDs — flag this clearly as a pre-release checklist item.
- Since there's no IAP, you don't need Play Billing Library at all for this pass — keep the dependency footprint minimal.

---

## 6. Suggested build order

1. Wire up the AdMob SDK and a single generic `RewardedAdManager` (load/show/callback handling, reusable across all 5 placements) — verify one placement (start with #1, Shards) works end-to-end with a test ad unit.
2. Add the cooldown persistence layer (per-placement last-watched timestamp + countdown UI) using #1 as the template.
3. Duplicate the pattern for #3 (DPS/CPS multiplier) — these are mechanically similar to #1.
4. Build #2 (free level-up) including the target-picker UX.
5. Build #5 (Starlight boost) last, including the first-Descend gating check and its placement in the Descend confirmation flow.

---

## 7. Other monetization ideas (not requested, worth knowing about for later)

Not included in this pass, but common low-friction rewarded-ad patterns worth keeping in your back pocket if you ever want to expand: a once-per-day larger "daily bonus" ad with a bigger flat reward (drives daily-return habit), and a "double this reward" follow-up offer immediately after certain rewards (watch a second ad to 2x what you just earned). Neither is needed for launch — flagging only so you're aware they exist as options later.

---

## 8. Open decisions to confirm as you go
- Exact cooldown durations and multiplier/percentage values per placement (start with the numbers suggested above, tune from real playtesting/analytics once live).
- Exact Shard-reward multiplier constant (the `* 100` suggestion in Section 2) — validate against real CPS numbers once the balance pass from the earlier fixes prompt is in and pacing feels right.

# Chronos HSC: Smart Study Engine

An offline-first, relational Android study engine built with **Kotlin**, **Jetpack Compose (Material 3)**, **Room SQLite (`ForeignKey.CASCADE`)**, and **Jetpack Glance AppWidget**, tailored specifically for HSC Science students (`Physics`, `Chemistry`, `Higher Math`, `Biology`, and `ICT`).

---

## Key Architectural Features

1. **Parent-Child Relational Chapter Architecture (`StudyTopicChain` -> `DailyTaskSlice`):**
   - Multi-day syllabus chapters are stored as a cohesive parent `StudyTopicChain` with auto-generated daily `DailyTaskSlice` child records (`4-1`, `4-2`, `4-3`, `4-4`) linked via `ForeignKey.CASCADE`.
2. **Full-Topic Cascade Delete Engine & Single-Slice Re-Indexer:**
   - **Delete Entire Topic Chain:** Executes a single `@Transaction` cascade delete (`dao.deleteTopicChainById(chainId)`), wiping the parent topic AND all past, today's, and future slices plus any scheduled Ebbinghaus retention echoes—complete with 1-tap `Undo` restoration.
   - **Delete Only This Slice & Re-Index:** Removes only the selected slice and pulls subsequent slices back by 1 day (`4-3` becomes `4-2`).
3. **1-Tap Domino / Auto-Shift Reschedule Engine:**
   - Detects overdue incomplete slices (`scheduledDateEpochDay < today`) and shifts the entire chain's remaining incomplete slices forward by the exact day delta so missed slices never pile up on the same day.
   - Includes a per-card **Push Domino (`+1d`)** button on every active slice.
4. **Chapter "Boss Fights", Sub-Counter Chip Damage & Overdue Purple Shield:**
   - Every active chapter is rendered as a segmented Boss Health Bar (`totalDays * 100 HP`).
   - Tapping the on-card problem sub-counter (`[ 2 / 5 Problems + ]`) deals chip damage to the active segment, animates the card's fill gradient, triggers haptic feedback, and awards `+10 XP`.
   - Overdue slices add a pulsing `+10% Purple Shield` to the Boss Bar that shatters when a slice is completed.
5. **Energy-State Filtering ("Battery Mode"):**
   - Switch between **⚡ 100% High Focus**, **🔋 50% Medium**, and **🧟 15% Zombie Mode** (which hides heavy problem-solving tasks behind a protective brain footer and offers 1-tap conversion into 15-minute light reviews).
6. **Task Roulette & 25-Minute Micro-Sprint:**
   - Physics-decelerated slot wheel that locks onto a random incomplete slice and opens a **60-second acceptance window** for a **2x XP Multiplier** alongside a built-in **25-minute countdown timer**.
7. **Spaced Repetition Auto-Spawner (Ebbinghaus Retention Echo):**
   - Completing the final slice of any chain launches the **Boss Conquered Dialog**, spawning 15-minute review echoes at `+2/+5/+14d` (*Brutal*), `+3/+7/+21d` (*Medium*), or `+5/+14/+30d` (*Easy*) linked to the same `chainId`.
8. **Swipe-to-Slash Katana Gesture, 12-Week Effort Heatmap & Glance Home Widget:**
   - Custom horizontal katana slash gesture with `Canvas` particle burst, a 7x12 GitHub-style weighted effort heatmap (`(Slices * 25) + (Problems * 5)`), and an interactive Jetpack Glance home-screen widget (`ChronosStudyWidget`).

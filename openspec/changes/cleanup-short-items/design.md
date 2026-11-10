# Design

## Context

- `Activities.removeActivityAndCloseGap` already merges same-activity neighbours for a single deleted item; `TimeTrackingItemQueries.getAdjacentItems` defines touching neighbours (`prev.end == item.start`).
- `ItemPersister.persist` (via `InsertHelper`) rewrites the whole file and **truncates or drops existing items that overlap** the inserted one; `replace` = `delete` + `persist`. The order in which changes are written therefore matters.
- `SubmitStatusTracker` keys items by `activity|start|end`; any modified item is a new key, so only *original* items need the lock check.
- The day header is `newDayNode` in `TimeTrackingItemCellWithActions`, shown on the cell for `lastItemOfDay` (list is newest first). Cell actions call back via `ActionsHandler`.
- `ReportConfig.roundDurationsTo` is injectable (`ConfigModule.provideReportConfig`). `DurationRounder` rounds *durations* with half-even ties, so it is not reusable for snapping *timestamps* with ties-up.
- Requirements: see `specs/activities/short-item-cleanup/spec.md` and the `submit/item-locking` delta.

## Goals / Non-Goals

**Goals:**
- Pure, deterministic planning logic that can be exhaustively unit-tested without UI or file I/O.
- Preview shows exactly what will be written.
- Application of a plan is all-or-nothing with respect to the lock check.

**Non-Goals:**
- Undo (the existing backup service remains the safety net).
- CLI command for the cleanup (can reuse the planner later).
- Multi-day or range cleanup.

## Decisions

### D1: Split into a pure planner and an applying command
`ShortItemCleanupPlanner` (package `org.stt.command`) takes `(dayItems: List<TimeTrackingItem>, day: LocalDate, interval: Duration, isLocked: (TimeTrackingItem) -> Boolean)` and returns a `CleanupPlan` = list of `CleanupChange`:
- `Removed(original)`
- `Changed(original, updated)`

`Activities.applyCleanup(plan)` (new `CommandHandler` method) persists it. The controller builds the plan, shows it in the preview, and passes the *same* plan to `applyCleanup` on confirm.
*Alternative:* one `CleanUpDay(date)` command that plans and writes in one go — rejected because the preview must show the exact change set, and a pure function is far easier to test with theories.

### D2: Planner works on a mutable working copy, emits net changes
The planner keeps a sorted working list of slots `(original, current?)`. Each step mutates `current` (or sets it to `null` for removal). Iteration is by index over the working list in start order; an item is re-checked for shortness using its *current* duration when reached (single pass, earlier slots never revisited). At the end, slots are diffed: `current == null` → `Removed`, `current != original` → `Changed`, otherwise nothing. Merges are expressed as: previous neighbour `Changed` to the full span, short item and next neighbour `Removed`.
*Alternative:* emitting per-step operations — rejected; the same neighbour can change several times, the preview should show only before → final.

### D3: Neighbour eligibility in one predicate
A neighbour (the adjacent slot in the working list, whose current item is non-null) is *eligible* when: it touches the short item exactly, it starts on `day`, and `isLocked(original)` is false. Ineligible neighbours are treated as gaps. The isolated-deletion rule distinguishes "no touching neighbour at all" (delete) from "touching but locked" (keep).

### D4: Grid snapping
`snap(t) = dayStart + round((t - dayStart) / x) * x`, ties up, where `dayStart` is midnight of `day` and computed in milliseconds (like `DurationRounder`). The midpoint is `start + (end - start) / 2`. The result is clamped to `[prev.start, next.end ?: +inf]`; if it equals a bound, that neighbour becomes zero-length and is removed. Implemented as a small private helper in the planner (not in `DurationRounder`, whose tie semantics differ for durations).

### D5: Write order in `applyCleanup`
1. Call `requireNotSubmitted` on every `original` in the plan **before** writing anything (all-or-nothing lock check; covers items submitted between preview and confirm).
2. `persister.delete(original)` for every change (removed and changed).
3. `persister.persist(updated)` for every `Changed`, ascending by start.
4. Publish `ItemDeleted` for removals and `ItemReplaced(original, updated)` for changes.

Deleting first guarantees `InsertHelper` never truncates an item that is part of the plan; the final layout has no overlaps by construction, so persisting does not touch other items.
*Alternative:* using `persister.replace` per change — rejected, intermediate states overlap and `InsertHelper` would cut items that are still to be processed.

### D6: Ongoing item
An ongoing item (`end == null`) is never short. As next neighbour its outer bound is unbounded; merging into it yields `previous.withPendingEnd()`; absorbing moves its start earlier via `withStart`.

### D7: UI wiring
- `TimeTrackingItemCellWithActions`: add a small `FramelessButton` with a broom/compress glyph and tooltip to `newDayNode`; on action call `actionsHandler.cleanUpDay(item.start.toLocalDate())`.
- `ActivitiesController.cleanUpDay(date)`: query all items of the day via `queries.queryItems(Criteria().withStartBetween(...))` (unfiltered), run the planner with `reportConfig.roundDurationsTo` and `submitStatusTracker::isSubmitted`; empty plan → `NotifyUser`/info; otherwise `sttOptionDialogs.showCleanupPreviewDialog(plan)` and on `PERFORM_ACTION` call `activities.applyCleanup(plan)`; catch `IllegalStateException` → existing `activities.submit.itemLocked` warning.
- `STTOptionDialogs.showCleanupPreviewDialog`: same structure as `showItemCoversOtherItemsDialog` — a list with one row per change ("removed" or "before → after" times plus activity), Apply / Cancel.
- New keys in `Application.properties`: tooltip, dialog title/text, "nothing to clean up", row labels.

## Risks / Trade-offs

- [Non-atomic file writes: an I/O failure mid-apply leaves a partially applied plan] → lock check happens before any write; backup service exists; plan sizes are one day.
- [Single-pass order dependence: a long neighbour can drop below `x` and stay if it lies before the current item] → accepted by spec (sequential, no revisit); user can click again for a second pass.
- [Each delete/persist rewrites the whole file: O(changes × file size)] → acceptable for one day of changes; can batch later if needed.
- [Stale plan if items change between preview and confirm] → lock check covers submits; other concurrent edits are not expected in a single-user desktop app; `delete` of a no-longer-existing line is a no-op.
- [Snapping can move time up to x/2 from a long neighbour into another] → intended by spec; preview makes it visible.

# Proposal

## Why

Fast start/stop tracking produces many tiny fragments (a 2-minute "mail" between two tasks, an accidental switch that was corrected a minute later). They clutter the day, distort rounded reports and must currently be cleaned up one by one via delete/edit. A one-click per-day cleanup keeps the low-friction workflow while producing a tidy day that lines up with the report rounding grid.

## What Changes

- New cleanup button on each day header (the calendar bar) in the Activities list. It acts on all items starting on that day, independent of the current search filter.
- An item counts as *short* when its duration is less than `ReportConfig.roundDurationsTo` (default 5 min). The same value `x` is the grid for snapping split points.
- Short items are processed one by one, earliest first, in a single pass:
  - Both touching neighbours have the same activity → the neighbours are merged over the short item.
  - Both touching neighbours differ → the short item's time is distributed to both; the split point is the short item's midpoint rounded to the nearest full `x` (ties round up), clamped to the neighbours' outer bounds.
  - Only one touching neighbour → that neighbour absorbs the full short item (no snapping).
  - No touching neighbour → the short item is deleted.
- Submitted (locked) items are never modified and behave like a gap; an otherwise isolated short item next to a submitted item is kept. The ongoing item is never removed but may absorb time. Breaks get no special treatment.
- Before anything is written, a preview dialog lists all planned changes (removed / merged / resized items, before → after); changes are applied only on confirmation.
- The `Activities` command handler gains a cleanup operation that applies a planned change set while honouring item locking.

## Capabilities

### New Capabilities
- `activities/short-item-cleanup`: Per-day removal of short items by merging or redistributing their time to adjacent items, including the planning rules, exclusions, preview/confirmation and the UI entry point on the day header.

### Modified Capabilities
- `submit/item-locking`: The command-handler guard requirement is extended to the new day-cleanup operation (it must never modify submitted items).

## Impact

- `org.stt.command`: new pure planner (e.g. `ShortItemCleanupPlanner`) producing a list of changes; new `CommandHandler`/`Activities` operation to apply them and publish `ItemDeleted` / `ItemReplaced` / `ItemInserted` events.
- `org.stt.gui.jfx`: `TimeTrackingItemCellWithActions` (button in the day header, new `ActionsHandler` callback), `ActivitiesController` (handler, preview flow), `STTOptionDialogs` (preview dialog), `Application.properties` (labels/tooltips).
- Config: reads existing `ReportConfig.roundDurationsTo`; no new settings, no file-format change.
- Tests: planner unit tests (theories over the rule matrix), `Activities` apply tests including locking.

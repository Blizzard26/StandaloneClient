# Tasks

## 1. Cleanup planner (pure logic)

- [ ] 1.1 Add `CleanupChange` (`Removed`, `Changed`) and `CleanupPlan` model types with KDoc in `org.stt.command`; verify `./gradlew compileKotlin` succeeds
- [ ] 1.2 Implement `ShortItemCleanupPlanner` working-copy loop (D2), short-item check, neighbour eligibility (D3: touching, same day, not locked), and isolated-item deletion vs. keep-next-to-locked; verify with `ShortItemCleanupPlannerTest` scenarios for Short Item Definition, Adjacency (incl. previous-day item), Isolated, Submitted exclusions
- [ ] 1.3 Implement merge of same-activity neighbours incl. merge into ongoing item (D6); verify planner tests for "Same activity on both sides" and "Merge into ongoing item"
- [ ] 1.4 Implement grid snapping helper (D4: nearest multiple of x from midnight, ties up, clamp, zero-length neighbour removal) and different-activity distribution; verify a `@Theory`-based test over the spec examples (inside grid, outside grid, tie, clamp-to-zero) passes
- [ ] 1.5 Implement single-touching-neighbour absorption (no snapping) incl. ongoing next neighbour; verify planner tests "Only previous neighbour touches" / "Only next neighbour touches"
- [ ] 1.6 Verify sequential processing and net-change diffing with planner tests "Short item grows above interval", "Run of short items collapses", breaks handled like other items, and an empty plan for a clean day; `./gradlew test --tests '*ShortItemCleanupPlannerTest'` green

## 2. Applying a plan (command side)

- [ ] 2.1 Add `applyCleanup(plan: CleanupPlan)` to `CommandHandler` and implement in `Activities` per D5 (lock check on all originals first, delete all originals, persist updated items ascending, publish `ItemDeleted`/`ItemReplaced`); update any other `CommandHandler` implementations/mocks; verify `./gradlew compileKotlin compileTestKotlin`
- [ ] 2.2 Add `Activities` tests (in `ActivitiesTest`) with a real `STTItemPersister` over an in-memory/temporary file: merge, distribute, absorb and delete plans produce exactly the expected file content and events; verify tests pass
- [ ] 2.3 Add `Activities` test "Block day cleanup touching submitted items": plan contains an item marked submitted → `IllegalStateException`, file unchanged; verify test passes

## 3. UI: day header button and preview

- [ ] 3.1 Add `cleanUpDay(date: LocalDate)` to `TimeTrackingItemCellWithActions.ActionsHandler` and a cleanup `FramelessButton` with glyph and tooltip in the day header (`createDateSubheader`); verify `TimeTrackingItemCellTest` still passes and a new test asserts the header button invokes `cleanUpDay` with the item's date
- [ ] 3.2 Add `showCleanupPreviewDialog(plan)` to `STTOptionDialogs` (list of removed / before → after rows, Apply/Cancel) and new keys in `Application.properties`; verify the app starts with `./gradlew run` and the dialog renders for a day with short items
- [ ] 3.3 Implement `ActivitiesController.cleanUpDay(date)` (unfiltered day query, planner with `ReportConfig.roundDurationsTo` and `submitStatusTracker::isSubmitted`, "nothing to clean up" notification, preview, `applyCleanup` on confirm, locked-item warning on `IllegalStateException`); verify `ActivitiesControllerTest` covers confirm → `applyCleanup` called, cancel → not called, empty plan → no dialog
- [ ] 3.4 Update `doc/ui-architecture.md` (Activities tab table: day-header cleanup button and preview dialog); verify the doc mentions the button, the dialog and `roundDurationsTo` as interval

## 4. Integration check

- [ ] 4.1 Run `./gradlew build` and a manual check in `./gradlew run`: create a day with short items (merge, distribute, isolated, submitted neighbour), click the day-header button, confirm the preview matches the result in the list and the Report tab

## Workflow follow-up

- Archive the change with `/opsx:archive` after review, syncing `activities/short-item-cleanup` and the `submit/item-locking` delta into the main specs.

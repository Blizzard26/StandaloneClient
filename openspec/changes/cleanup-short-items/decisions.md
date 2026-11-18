# Implementation Decisions (unattended apply run)

Questions that came up while implementing `cleanup-short-items`, answered with the help of an advisor subagent. Each entry lists the options considered and the decision taken.

## Q1: Split bound when the next neighbour is the ongoing item

**Question:** Design D6 calls the ongoing item's outer bound "unbounded". If the short item sits right before an ongoing item that started recently, the snapped split can land after the ongoing item's start, possibly in the future (e.g. x = 5, short B 09:33-09:34, ongoing C since 09:34 → midpoint 09:33:30 snaps to 09:35, after "now").

**Options:**
1. For an ongoing next neighbour, clamp the split to at most its current start, so an ongoing item can only grow earlier and never shrink.
2. Pass `now` into the planner and clamp the split to at most `now`.
3. For an ongoing next neighbour, skip snapping and let it absorb the whole short item.
4. Exclude short items directly before the ongoing item from cleanup.

**Decision:** Option 1, as the advisor recommended. It can never produce a future start, it keeps the planner pure (no clock), and it keeps the snapping behaviour. Options 2–4 add a clock dependency, drop the snapping, or leave fragments behind. The spec ("Distribute Time Between Different Neighbours") and design D6 were updated to match.

## Q2: Visual verification of the preview dialog (task 3.2) in an unattended run

**Question:** Task 3.2 requires checking that the app starts and the dialog renders. No screen is available in this run.

**Options:**
a) Smoke-start `./gradlew run`, check the logs and mark 3.2 done without having seen the dialog.
b) Smoke-start, but leave 3.2 unchecked.
c) Mark the implementation part done and split the visual check into a new unchecked task 3.2a.

**Decision:** (c), as the advisor recommended. The checklist stays accurate: the code is done and tested through a mocked dialog, but the visual check is still open. The smoke start was also skipped. The app resolves its data directory from the `HOME` environment variable, and the sandbox refused to override `HOME`, so starting it would have used the user's real `.stt` data without supervision.

## Q3: Final manual check (task 4.1)

**Question:** Should task 4.1 (build plus a manual GUI walkthrough) be marked done based on build and automated tests?

**Options:** a) leave it unchecked and report the manual check as pending; b) mark it done.

**Decision:** (a), as the advisor recommended. The automated tests cover the planner rules, applying a plan (file content and events), the lock check, the controller flow (confirm / cancel / nothing to do) and the header button. Comparing the preview with the list and the Report tab is still left to the user.

## Q4: Pre-existing failing test `ItemAndDateValidatorTest.shouldReturnFalseWhenTimeIsInFutureAndNoEarlierItem`

**Question:** This test uses `now().plusHours(1)` and fails when run after 23:00 because the time crosses midnight. That makes `./gradlew build` red tonight. Should this change fix it?

**Options:** a) leave it and report it (out of scope); b) fix it in this change.

**Decision:** (a), as the advisor recommended. It is unrelated to this change and keeps the diff focused. The fix is small (use a fixed time or a clock that does not cross midnight) and belongs in a separate `fix:` commit.

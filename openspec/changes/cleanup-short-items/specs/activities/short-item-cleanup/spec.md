# Spec Delta

## Purpose

Lets the user remove short time-tracking fragments of a single day in one step by merging them into, or redistributing their time to, the adjacent items, so the day lines up with the report rounding grid.

All examples below assume the cleanup interval `x` = 5 minutes and items on the same day.

## ADDED Requirements

### Requirement: Day Cleanup Entry Point
The Activities list SHALL show a cleanup button on every day header. Activating it SHALL plan a cleanup for all items whose start lies on that day, regardless of any active search filter.

#### Scenario: Cleanup covers the whole day despite filter
- **WHEN** the search filter shows only some items of 2026-11-10 and the user clicks the cleanup button on that day's header
- **THEN** the cleanup considers every item starting on 2026-11-10, including those hidden by the filter

#### Scenario: Other days untouched
- **WHEN** the user cleans up 2026-11-10
- **THEN** no item starting on another day is created, modified or deleted

### Requirement: Cleanup Interval
The cleanup interval `x` SHALL be the configured report rounding interval (`roundDurationsTo`, default 5 minutes). It defines both which items are short and the grid that split points snap to.

#### Scenario: Interval follows report rounding setting
- **WHEN** the report rounding interval is configured to 15 minutes
- **THEN** items shorter than 15 minutes are short and split points snap to full quarter hours

### Requirement: Short Item Definition
An item SHALL be short when it has an end and its duration is strictly less than `x`. The ongoing item (no end) SHALL never be short.

#### Scenario: Item below interval is short
- **WHEN** an item lasts from 09:32 to 09:35
- **THEN** it is short

#### Scenario: Item equal to interval is not short
- **WHEN** an item lasts from 09:30 to 09:35
- **THEN** it is not short and is kept

#### Scenario: Ongoing item is never removed
- **WHEN** the ongoing item started 2 minutes ago
- **THEN** it is not short and is not removed

### Requirement: Adjacency
A neighbour SHALL count as touching only if it starts on the cleaned-up day and ends exactly when the short item starts (previous) or starts exactly when the short item ends (next). Items separated by a gap SHALL NOT be touching.

#### Scenario: Gap breaks adjacency
- **WHEN** A ends at 09:30 and the short item starts at 09:31
- **THEN** A is not a touching neighbour of the short item

#### Scenario: Item from previous day is not a neighbour
- **WHEN** the first item of 2026-11-10 is short and the item before it started on 2026-11-09 and ends exactly at its start
- **THEN** the item from 2026-11-09 is not a touching neighbour and is unchanged

### Requirement: Merge Neighbours With Same Activity
When both touching neighbours of a short item have the same activity, the cleanup SHALL replace the three items by one item with that activity spanning from the previous neighbour's start to the next neighbour's end.

#### Scenario: Same activity on both sides
- **WHEN** the day contains A 09:00-09:32, B 09:32-09:35, A 09:35-10:00
- **THEN** the result is A 09:00-10:00

#### Scenario: Merge into ongoing item
- **WHEN** the day contains A 09:00-09:32, B 09:32-09:35 and an ongoing A started at 09:35
- **THEN** the result is one ongoing A started at 09:00

### Requirement: Distribute Time Between Different Neighbours
When both touching neighbours exist and have different activities, the cleanup SHALL remove the short item and set the boundary between the neighbours to the short item's midpoint rounded to the nearest multiple of `x` (exact ties round up), clamped between the previous neighbour's start and the next neighbour's end.

#### Scenario: Split snaps to grid point inside the short item
- **WHEN** the day contains A 09:00-09:32, B 09:32-09:36, C 09:36-10:00
- **THEN** the result is A 09:00-09:35, C 09:35-10:00

#### Scenario: Split snaps outside the short item
- **WHEN** the day contains A 09:00-09:31, B 09:31-09:33, C 09:33-10:00
- **THEN** the result is A 09:00-09:30, C 09:30-10:00

#### Scenario: Exact tie rounds up
- **WHEN** the day contains A 09:00-09:31, B 09:31-09:34, C 09:34-10:00 (midpoint 09:32:30)
- **THEN** the result is A 09:00-09:35, C 09:35-10:00

#### Scenario: Neighbour shrunk to zero length is removed
- **WHEN** clamping places the boundary exactly on the previous neighbour's start or the next neighbour's end
- **THEN** the neighbour that would have zero length is removed and the other neighbour covers the whole span

### Requirement: Single Touching Neighbour Absorbs
When exactly one neighbour of a short item is touching and modifiable, that neighbour SHALL be extended to cover the short item completely, without snapping.

#### Scenario: Only previous neighbour touches
- **WHEN** the day contains A 09:00-09:30, B 09:30-09:33 and the next item starts at 09:40
- **THEN** the result is A 09:00-09:33 and the 09:40 item is unchanged

#### Scenario: Only next neighbour touches
- **WHEN** the previous item ends at 09:20 and the day contains B 09:30-09:33, C 09:33-10:00
- **THEN** the result is C 09:30-10:00

### Requirement: Isolated Short Items Are Deleted
A short item with no touching neighbour SHALL be deleted.

#### Scenario: Gaps on both sides
- **WHEN** the day contains B 09:30-09:33 with gaps before and after
- **THEN** B is deleted

### Requirement: Sequential Processing
Short items SHALL be processed one at a time in ascending start order within a single pass. Each item SHALL be evaluated with its duration as changed by earlier steps; an item that has grown to at least `x` SHALL be kept. Items before the current one SHALL NOT be revisited.

#### Scenario: Short item grows above interval
- **WHEN** the day contains A 09:00-09:20, B 09:20-09:22, C 09:22-09:25, D 09:25-10:00
- **THEN** B is split at 09:20 so C becomes 09:20-09:25, C is no longer short, and the result is A 09:00-09:20, C 09:20-09:25, D 09:25-10:00

#### Scenario: Run of short items collapses
- **WHEN** the day contains A 09:00-09:10, B 09:10-09:12, C 09:12-09:14, D 09:14-10:00
- **THEN** B is split at 09:10 so C becomes 09:10-09:14, C is then split at 09:10, and the result is A 09:00-09:10, D 09:10-10:00

### Requirement: Submitted Items Are Excluded
The cleanup SHALL NOT modify or delete any submitted item. A short submitted item SHALL be skipped. A submitted neighbour SHALL be treated as not touching, except that a short item whose only touching neighbours are submitted SHALL be kept instead of deleted.

#### Scenario: Submitted neighbour treated as gap
- **WHEN** the day contains submitted A 09:00-09:30, B 09:30-09:33, C 09:33-10:00
- **THEN** A is unchanged and the result is A 09:00-09:30, C 09:30-10:00

#### Scenario: Short item between submitted items kept
- **WHEN** both touching neighbours of a short item are submitted
- **THEN** the short item and both neighbours are unchanged

#### Scenario: Submitted short item kept
- **WHEN** a short item is submitted
- **THEN** it is unchanged

### Requirement: Breaks Have No Special Treatment
Items whose activity is a configured break activity SHALL be handled exactly like any other item.

#### Scenario: Short break removed
- **WHEN** the day contains A 09:00-09:30, pause 09:30-09:32, C 09:32-10:00
- **THEN** the pause is removed like any other short item

### Requirement: Preview And Confirmation
Before changing anything, the system SHALL show a preview listing every planned change (removed items and items with their before and after times). Changes SHALL be persisted only when the user confirms; cancelling SHALL leave all items unchanged.

#### Scenario: User confirms
- **WHEN** the preview lists planned changes and the user confirms
- **THEN** exactly the previewed changes are persisted and the Activities list and reports reflect them

#### Scenario: User cancels
- **WHEN** the user cancels the preview
- **THEN** no item is created, modified or deleted

#### Scenario: Nothing to clean up
- **WHEN** the day contains no short item that can be removed
- **THEN** the user is informed that there is nothing to clean up and no item is changed

package org.stt.command

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.experimental.theories.DataPoints
import org.junit.experimental.theories.Theories
import org.junit.experimental.theories.Theory
import org.junit.runner.RunWith
import org.stt.model.TimeTrackingItem
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Tests for [ShortItemCleanupPlanner], following the scenarios of the `activities/short-item-cleanup` spec
 * (interval x = 5 minutes, day 2026-11-10).
 *
 * Verified concerns:
 * - short item definition (strictly below x, ongoing item never short) and the empty plan for clean days
 * - adjacency: gaps and items from other days are not touching neighbours
 * - merging same-activity neighbours, also into the ongoing item
 * - distributing time between different neighbours with grid snapping (inside/outside the short item, ties
 *   round up, clamping, zero-length neighbours removed, ongoing neighbour never starts later)
 * - single neighbour absorption, deletion of isolated short items
 * - sequential, earliest-first processing with net change diffing
 * - submitted (locked) items are never changed, breaks are handled like any other item
 */
@RunWith(Theories::class)
class ShortItemCleanupPlannerTest {
    private val sut = ShortItemCleanupPlanner()

    @Test
    fun shouldReturnEmptyPlanWhenNoItemIsShort() {
        // GIVEN
        val items = listOf(item("A", "09:00", "09:30"), item("B", "09:30", "09:35"), ongoing("C", "09:35"))

        // WHEN
        val result = cleanUp(items)

        // THEN
        assertThat(result).isEqualTo(items)
        assertThat(plan(items).isEmpty()).isTrue()
    }

    @Test
    fun shouldNotRemoveOngoingItemEvenIfItStartedRecently() {
        // GIVEN
        val items = listOf(item("A", "09:00", "09:30"), ongoing("B", "09:30"))

        // WHEN
        val result = cleanUp(items)

        // THEN
        assertThat(result).isEqualTo(items)
    }

    @Test
    fun shouldMergeNeighboursWithSameActivity() {
        // GIVEN
        val items = listOf(item("A", "09:00", "09:32"), item("B", "09:32", "09:35"), item("A", "09:35", "10:00"))

        // WHEN
        val result = cleanUp(items)

        // THEN
        assertThat(result).containsExactly(item("A", "09:00", "10:00"))
    }

    @Test
    fun shouldMergeIntoOngoingItem() {
        // GIVEN
        val items = listOf(item("A", "09:00", "09:32"), item("B", "09:32", "09:35"), ongoing("A", "09:35"))

        // WHEN
        val result = cleanUp(items)

        // THEN
        assertThat(result).containsExactly(ongoing("A", "09:00"))
    }

    @Test
    fun shouldExpressMergeAsChangeOfPreviousAndRemovalOfOthers() {
        // GIVEN
        val first = item("A", "09:00", "09:32")
        val short = item("B", "09:32", "09:35")
        val last = item("A", "09:35", "10:00")

        // WHEN
        val result = plan(listOf(first, short, last))

        // THEN
        assertThat(result.changes).containsExactly(
                CleanupChange.Changed(first, item("A", "09:00", "10:00")),
                CleanupChange.Removed(short),
                CleanupChange.Removed(last))
    }

    @Test
    fun shouldSplitAtGridPointInsideShortItem() {
        // GIVEN
        val items = listOf(item("A", "09:00", "09:32"), item("B", "09:32", "09:36"), item("C", "09:36", "10:00"))

        // WHEN
        val result = cleanUp(items)

        // THEN
        assertThat(result).containsExactly(item("A", "09:00", "09:35"), item("C", "09:35", "10:00"))
    }

    @Test
    fun shouldSplitAtGridPointOutsideShortItem() {
        // GIVEN
        val items = listOf(item("A", "09:00", "09:31"), item("B", "09:31", "09:33"), item("C", "09:33", "10:00"))

        // WHEN
        val result = cleanUp(items)

        // THEN
        assertThat(result).containsExactly(item("A", "09:00", "09:30"), item("C", "09:30", "10:00"))
    }

    @Test
    fun shouldRoundUpOnExactTie() {
        // GIVEN
        val items = listOf(item("A", "09:00", "09:31"), item("B", "09:31", "09:34"), item("C", "09:34", "10:00"))

        // WHEN
        val result = cleanUp(items)

        // THEN
        assertThat(result).containsExactly(item("A", "09:00", "09:35"), item("C", "09:35", "10:00"))
    }

    @Test
    fun shouldNeverMoveStartOfOngoingNeighbourLater() {
        // GIVEN
        val items = listOf(item("A", "09:00", "09:33"), item("B", "09:33", "09:34"), ongoing("C", "09:34"))

        // WHEN
        val result = cleanUp(items)

        // THEN
        assertThat(result).containsExactly(item("A", "09:00", "09:34"), ongoing("C", "09:34"))
    }

    @Test
    fun shouldRemoveNextNeighbourShrunkToZeroLength() {
        // GIVEN
        val items = listOf(item("A", "09:00", "09:33"), item("B", "09:33", "09:34"), item("C", "09:34", "09:34:30"))

        // WHEN
        val result = cleanUp(items)

        // THEN
        assertThat(result).containsExactly(item("A", "09:00", "09:34:30"))
    }

    @Test
    fun shouldLetOnlyTouchingPreviousNeighbourAbsorb() {
        // GIVEN
        val items = listOf(item("A", "09:00", "09:30"), item("B", "09:30", "09:33"), item("C", "09:40", "10:00"))

        // WHEN
        val result = cleanUp(items)

        // THEN
        assertThat(result).containsExactly(item("A", "09:00", "09:33"), item("C", "09:40", "10:00"))
    }

    @Test
    fun shouldLetOnlyTouchingNextNeighbourAbsorb() {
        // GIVEN
        val items = listOf(item("A", "09:00", "09:20"), item("B", "09:30", "09:33"), item("C", "09:33", "10:00"))

        // WHEN
        val result = cleanUp(items)

        // THEN
        assertThat(result).containsExactly(item("A", "09:00", "09:20"), item("C", "09:30", "10:00"))
    }

    @Test
    fun shouldLetOngoingNextNeighbourAbsorb() {
        // GIVEN
        val items = listOf(item("B", "09:30", "09:33"), ongoing("C", "09:33"))

        // WHEN
        val result = cleanUp(items)

        // THEN
        assertThat(result).containsExactly(ongoing("C", "09:30"))
    }

    @Test
    fun shouldDeleteIsolatedShortItem() {
        // GIVEN
        val items = listOf(item("A", "09:00", "09:20"), item("B", "09:30", "09:33"), item("C", "09:40", "10:00"))

        // WHEN
        val result = cleanUp(items)

        // THEN
        assertThat(result).containsExactly(item("A", "09:00", "09:20"), item("C", "09:40", "10:00"))
    }

    @Test
    fun shouldNotUseItemOfPreviousDayAsNeighbour() {
        // GIVEN
        val fromPreviousDay = TimeTrackingItem("A", DAY.minusDays(1).atTime(23, 0), DAY.atStartOfDay())
        val short = item("B", "00:00", "00:02")
        val next = item("C", "00:10", "01:00")

        // WHEN
        val result = plan(listOf(fromPreviousDay, short, next))

        // THEN
        assertThat(result.changes).containsExactly(CleanupChange.Removed(short))
    }

    @Test
    fun shouldKeepItemThatGrewAboveInterval() {
        // GIVEN
        val items = listOf(item("A", "09:00", "09:20"), item("B", "09:20", "09:22"), item("C", "09:22", "09:25"),
                item("D", "09:25", "10:00"))

        // WHEN
        val result = cleanUp(items)

        // THEN
        assertThat(result).containsExactly(item("A", "09:00", "09:20"), item("C", "09:20", "09:25"),
                item("D", "09:25", "10:00"))
    }

    @Test
    fun shouldCollapseRunOfShortItems() {
        // GIVEN
        val items = listOf(item("A", "09:00", "09:10"), item("B", "09:10", "09:12"), item("C", "09:12", "09:14"),
                item("D", "09:14", "10:00"))

        // WHEN
        val result = cleanUp(items)

        // THEN
        assertThat(result).containsExactly(item("A", "09:00", "09:10"), item("D", "09:10", "10:00"))
    }

    @Test
    fun shouldReportOnlyNetChanges() {
        // GIVEN
        val a = item("A", "09:00", "09:10")
        val b = item("B", "09:10", "09:12")
        val c = item("C", "09:12", "09:14")
        val d = item("D", "09:14", "10:00")

        // WHEN
        val result = plan(listOf(a, b, c, d))

        // THEN
        assertThat(result.changes).containsExactly(
                CleanupChange.Removed(b),
                CleanupChange.Removed(c),
                CleanupChange.Changed(d, item("D", "09:10", "10:00")))
    }

    @Test
    fun shouldTreatSubmittedNeighbourAsGap() {
        // GIVEN
        val submitted = item("A", "09:00", "09:30")
        val items = listOf(submitted, item("B", "09:30", "09:33"), item("C", "09:33", "10:00"))

        // WHEN
        val result = cleanUp(items, setOf(submitted))

        // THEN
        assertThat(result).containsExactly(submitted, item("C", "09:30", "10:00"))
    }

    @Test
    fun shouldKeepShortItemBetweenSubmittedItems() {
        // GIVEN
        val first = item("A", "09:00", "09:30")
        val last = item("C", "09:33", "10:00")
        val items = listOf(first, item("B", "09:30", "09:33"), last)

        // WHEN
        val result = plan(items, setOf(first, last))

        // THEN
        assertThat(result.isEmpty()).isTrue()
    }

    @Test
    fun shouldKeepShortItemNextToSubmittedItemAndGap() {
        // GIVEN
        val submitted = item("A", "09:00", "09:30")
        val items = listOf(submitted, item("B", "09:30", "09:33"), item("C", "09:40", "10:00"))

        // WHEN
        val result = plan(items, setOf(submitted))

        // THEN
        assertThat(result.isEmpty()).isTrue()
    }

    @Test
    fun shouldKeepSubmittedShortItem() {
        // GIVEN
        val submitted = item("B", "09:30", "09:33")
        val items = listOf(item("A", "09:00", "09:30"), submitted, item("C", "09:33", "10:00"))

        // WHEN
        val result = plan(items, setOf(submitted))

        // THEN
        assertThat(result.isEmpty()).isTrue()
    }

    @Test
    fun shouldTreatBreaksLikeOtherItems() {
        // GIVEN
        val items = listOf(item("A", "09:00", "09:30"), item("pause", "09:30", "09:32"), item("C", "09:32", "10:00"))

        // WHEN
        val result = cleanUp(items)

        // THEN
        assertThat(result).containsExactly(item("A", "09:00", "09:30"), item("C", "09:30", "10:00"))
    }

    @Test
    fun shouldIgnoreItemsOfOtherDays() {
        // GIVEN
        val otherDay = TimeTrackingItem("X", DAY.plusDays(1).atTime(9, 0), DAY.plusDays(1).atTime(9, 1))

        // WHEN
        val result = plan(listOf(otherDay))

        // THEN
        assertThat(result.isEmpty()).isTrue()
    }

    @Theory
    fun shouldSnapToNearestGridPoint(case: SnapCase) {
        // GIVEN
        val time = DAY.atTime(LocalTime.parse(case.time))

        // WHEN
        val result = ShortItemCleanupPlanner.snapToGrid(time, DAY, INTERVAL)

        // THEN
        assertThat(result).isEqualTo(DAY.atTime(LocalTime.parse(case.expected)))
    }

    private fun plan(items: List<TimeTrackingItem>, locked: Set<TimeTrackingItem> = emptySet()) =
            sut.plan(items, DAY, INTERVAL) { locked.contains(it) }

    private fun cleanUp(items: List<TimeTrackingItem>, locked: Set<TimeTrackingItem> = emptySet()): List<TimeTrackingItem> {
        val changes = plan(items, locked).changes
        val removed = changes.map { it.original }.toSet()
        val updated = changes.filterIsInstance<CleanupChange.Changed>().map { it.updated }
        return (items.filter { !removed.contains(it) } + updated).sortedBy { it.start }
    }

    companion object {
        private val DAY: LocalDate = LocalDate.of(2026, 11, 10)
        private val INTERVAL: Duration = Duration.ofMinutes(5)

        @JvmField
        @field:DataPoints
        val snapCases = arrayOf(
                SnapCase("09:34", "09:35"),
                SnapCase("09:32", "09:30"),
                SnapCase("09:32:30", "09:35"),
                SnapCase("09:32:29", "09:30"),
                SnapCase("09:35", "09:35"),
                SnapCase("00:00", "00:00"),
                SnapCase("23:57", "23:55"))

        private fun at(time: String): LocalDateTime = DAY.atTime(LocalTime.parse(time))

        private fun item(activity: String, start: String, end: String) = TimeTrackingItem(activity, at(start), at(end))

        private fun ongoing(activity: String, start: String) = TimeTrackingItem(activity, at(start))
    }
}

/** Input time and expected snapped time for [ShortItemCleanupPlannerTest.shouldSnapToNearestGridPoint]. */
data class SnapCase(val time: String, val expected: String)

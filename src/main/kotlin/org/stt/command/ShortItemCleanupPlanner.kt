package org.stt.command

import org.stt.model.TimeTrackingItem
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject

/**
 * Plans the removal of short items of a single day without touching any persistence.
 *
 * An item is short when it has an end and lasts strictly less than the given interval `x`. Short items are
 * processed one by one in ascending start order (single pass), each evaluated with its current duration:
 *
 * - both touching neighbours have the same activity: they are merged over the short item,
 * - both touching neighbours differ: the short item's time is split between them at its midpoint rounded to
 *   the nearest multiple of `x` (counted from midnight, ties round up), clamped to the neighbours' bounds,
 * - exactly one touching neighbour: it absorbs the whole short item,
 * - no touching neighbour at all: the short item is removed.
 *
 * Neighbours must start on the same day and must not be locked (submitted); locked neighbours are treated like
 * gaps, except that a short item that only touches locked neighbours is kept. Locked short items are skipped.
 * The ongoing item is never short and never starts later than it currently does.
 *
 * The result is the net change per original item (before -> final), see [CleanupPlan].
 */
class ShortItemCleanupPlanner @Inject constructor() {

    private class Slot(val original: TimeTrackingItem) {
        var current: TimeTrackingItem? = original
    }

    fun plan(dayItems: Collection<TimeTrackingItem>,
             day: LocalDate,
             interval: Duration,
             isLocked: (TimeTrackingItem) -> Boolean): CleanupPlan {
        if (interval <= Duration.ZERO) {
            return CleanupPlan(emptyList())
        }
        val slots = dayItems
                .filter { it.start.toLocalDate() == day }
                .sortedBy { it.start }
                .map { Slot(it) }

        for (index in slots.indices) {
            val slot = slots[index]
            val item = slot.current ?: continue
            if (isLocked(slot.original) || !isShort(item, interval)) continue
            processShortItem(slots, index, item, day, interval, isLocked)
        }

        val changes = slots.mapNotNull { slot ->
            val current = slot.current
            when {
                current == null -> CleanupChange.Removed(slot.original)
                current != slot.original -> CleanupChange.Changed(slot.original, current)
                else -> null
            }
        }
        return CleanupPlan(changes)
    }

    private fun processShortItem(slots: List<Slot>, index: Int, item: TimeTrackingItem, day: LocalDate,
                                 interval: Duration, isLocked: (TimeTrackingItem) -> Boolean) {
        val slot = slots[index]
        val previous = (index - 1 downTo 0).map { slots[it] }.firstOrNull { it.current != null }
                ?.takeIf { it.current!!.end == item.start }
        val next = (index + 1 until slots.size).map { slots[it] }.firstOrNull { it.current != null }
                ?.takeIf { it.current!!.start == item.end }
        val modifiablePrevious = previous?.takeIf { !isLocked(it.original) }
        val modifiableNext = next?.takeIf { !isLocked(it.original) }

        when {
            modifiablePrevious != null && modifiableNext != null -> {
                if (modifiablePrevious.current!!.activity == modifiableNext.current!!.activity) {
                    merge(modifiablePrevious, slot, modifiableNext)
                } else {
                    distribute(modifiablePrevious, slot, modifiableNext, item, day, interval)
                }
            }
            modifiablePrevious != null -> {
                modifiablePrevious.current = modifiablePrevious.current!!.withEnd(item.end!!)
                slot.current = null
            }
            modifiableNext != null -> {
                modifiableNext.current = modifiableNext.current!!.withStart(item.start)
                slot.current = null
            }
            previous == null && next == null -> slot.current = null
            else -> {
                // only locked neighbours touch: keep the short item
            }
        }
    }

    private fun merge(previous: Slot, slot: Slot, next: Slot) {
        val previousItem = previous.current!!
        val nextEnd = next.current!!.end
        previous.current = if (nextEnd == null) previousItem.withPendingEnd() else previousItem.withEnd(nextEnd)
        slot.current = null
        next.current = null
    }

    private fun distribute(previous: Slot, slot: Slot, next: Slot, item: TimeTrackingItem, day: LocalDate,
                           interval: Duration) {
        val previousItem = previous.current!!
        val nextItem = next.current!!
        val itemEnd = item.end!!
        val midpoint = item.start.plus(Duration.between(item.start, itemEnd).dividedBy(2))
        val lowerBound = previousItem.start
        val upperBound = nextItem.end ?: nextItem.start
        val split = minOf(maxOf(snapToGrid(midpoint, day, interval), lowerBound), upperBound)

        slot.current = null
        when {
            split == lowerBound -> {
                previous.current = null
                next.current = nextItem.withStart(lowerBound)
            }
            split == upperBound && nextItem.end != null -> {
                next.current = null
                previous.current = previousItem.withEnd(upperBound)
            }
            else -> {
                previous.current = previousItem.withEnd(split)
                next.current = nextItem.withStart(split)
            }
        }
    }

    private fun isShort(item: TimeTrackingItem, interval: Duration): Boolean {
        val end = item.end ?: return false
        return Duration.between(item.start, end) < interval
    }

    companion object {
        /**
         * Rounds [time] to the nearest multiple of [interval] counted from the start of [day]; exact ties round up.
         */
        internal fun snapToGrid(time: LocalDateTime, day: LocalDate, interval: Duration): LocalDateTime {
            val dayStart = day.atStartOfDay()
            val intervalMillis = interval.toMillis()
            val offsetMillis = Duration.between(dayStart, time).toMillis()
            var segments = Math.floorDiv(offsetMillis, intervalMillis)
            val remainder = Math.floorMod(offsetMillis, intervalMillis)
            if (remainder * 2 >= intervalMillis) {
                segments++
            }
            return dayStart.plus(Duration.ofMillis(segments * intervalMillis))
        }
    }
}

package org.stt.command

import org.junit.Before
import org.junit.Test
import org.mockito.BDDMockito.given
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.MockitoAnnotations
import net.engio.mbassy.bus.MBassador
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.mockito.BDDMockito.willThrow
import org.stt.Matchers
import org.stt.Matchers.any
import org.stt.model.ItemDeleted
import org.stt.model.ItemReplaced
import org.stt.persistence.stt.STTItemPersister
import org.stt.persistence.stt.STTItemReader
import java.io.Reader
import java.io.StringReader
import java.io.StringWriter
import java.io.Writer
import javax.inject.Provider
import org.stt.model.TimeTrackingItem
import org.stt.persistence.ItemPersister
import org.stt.query.TimeTrackingItemQueries
import org.stt.submit.SubmitStatusTracker
import java.time.LocalDateTime
import java.util.*
import java.util.stream.Stream

class ActivitiesTest {
    private var sut: Activities? = null
    @Mock
    private lateinit var persister: ItemPersister
    @Mock
    private lateinit var queries: TimeTrackingItemQueries

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        sut = Activities(persister, queries, Optional.empty(), Mockito.mock(SubmitStatusTracker::class.java))
    }

    @Test
    fun shouldFillGapOnDeleteIfPreviousAndNextMatch() {
        // GIVEN
        val before = TimeTrackingItem("expected",
                LocalDateTime.of(2000, 10, 10, 10, 9),
                LocalDateTime.of(2000, 10, 10, 10, 10))
        val itemToDelete = TimeTrackingItem("toDelete",
                LocalDateTime.of(2000, 10, 10, 10, 10),
                LocalDateTime.of(2000, 10, 10, 10, 12))
        val after = TimeTrackingItem("expected",
                LocalDateTime.of(2000, 10, 10, 10, 12),
                LocalDateTime.of(2000, 10, 10, 10, 13))
        val expected = TimeTrackingItem("expected",
                LocalDateTime.of(2000, 10, 10, 10, 9),
                LocalDateTime.of(2000, 10, 10, 10, 13))

        given<TimeTrackingItemQueries.AdjacentItems>(queries.getAdjacentItems(itemToDelete)).willReturn(TimeTrackingItemQueries.AdjacentItems(before, after))

        val removeActivity = RemoveActivity(itemToDelete)

        // WHEN
        sut!!.removeActivityAndCloseGap(removeActivity)

        // THEN
        verify<ItemPersister>(persister).persist(expected)
        verifyNoMoreInteractions(persister)
    }

    @Test
    fun shouldNotFillGapOnDeleteIfPreviousAndNextDiffer() {
        // GIVEN
        val before = TimeTrackingItem("before",
                LocalDateTime.of(2000, 10, 10, 10, 9),
                LocalDateTime.of(2000, 10, 10, 10, 10))
        val itemToDelete = TimeTrackingItem("toDelete",
                LocalDateTime.of(2000, 10, 10, 10, 10),
                LocalDateTime.of(2000, 10, 10, 10, 12))
        val after = TimeTrackingItem("after",
                LocalDateTime.of(2000, 10, 10, 10, 12),
                LocalDateTime.of(2000, 10, 10, 10, 13))

        given<TimeTrackingItemQueries.AdjacentItems>(queries.getAdjacentItems(itemToDelete)).willReturn(TimeTrackingItemQueries.AdjacentItems(before, after))

        val removeActivity = RemoveActivity(itemToDelete)

        // WHEN
        sut!!.removeActivityAndCloseGap(removeActivity)

        // THEN
        verify<ItemPersister>(persister).delete(itemToDelete)
        verifyNoMoreInteractions(persister)
    }

    @Test
    fun shouldFillGapOnDeletedIsOngoing() {
        // GIVEN
        val before = TimeTrackingItem("expected",
                LocalDateTime.of(2000, 10, 10, 10, 9),
                LocalDateTime.of(2000, 10, 10, 10, 10))
        val itemToDelete = TimeTrackingItem("toDelete",
                LocalDateTime.of(2000, 10, 10, 10, 10))
        val expected = TimeTrackingItem("expected",
                LocalDateTime.of(2000, 10, 10, 10, 9))

        given<TimeTrackingItemQueries.AdjacentItems>(queries.getAdjacentItems(itemToDelete)).willReturn(TimeTrackingItemQueries.AdjacentItems(before, null))

        val removeActivity = RemoveActivity(itemToDelete)

        // WHEN
        sut!!.removeActivityAndCloseGap(removeActivity)

        // THEN
        verify<ItemPersister>(persister).persist(expected)
        verifyNoMoreInteractions(persister)
    }

    @Test
    fun shouldNotFillGapIfPreviousItemStartsOnDifferentDay() {
        // GIVEN
        val before = TimeTrackingItem("expected",
                LocalDateTime.of(2000, 10, 9, 10, 9),
                LocalDateTime.of(2000, 10, 10, 10, 10))
        val itemToDelete = TimeTrackingItem("toDelete",
                LocalDateTime.of(2000, 10, 10, 10, 10))

        given(queries.getAdjacentItems(itemToDelete)).willReturn(TimeTrackingItemQueries.AdjacentItems(before, null))

        val removeActivity = RemoveActivity(itemToDelete)

        // WHEN
        sut!!.removeActivityAndCloseGap(removeActivity)

        // THEN
        verify<ItemPersister>(persister).delete(itemToDelete)
        verifyNoMoreInteractions(persister)
    }


    @Test
    fun shouldDeletedWithoutPrevious() {
        // GIVEN
        val itemToDelete = TimeTrackingItem("toDelete",
                LocalDateTime.of(2000, 10, 10, 10, 10),
                LocalDateTime.of(2000, 10, 10, 10, 12))
        val after = TimeTrackingItem("after",
                LocalDateTime.of(2000, 10, 10, 10, 12),
                LocalDateTime.of(2000, 10, 10, 10, 13))

        given<TimeTrackingItemQueries.AdjacentItems>(queries.getAdjacentItems(itemToDelete)).willReturn(TimeTrackingItemQueries.AdjacentItems(null, after))

        val removeActivity = RemoveActivity(itemToDelete)

        // WHEN
        sut!!.removeActivityAndCloseGap(removeActivity)

        // THEN
        verify<ItemPersister>(persister).delete(itemToDelete)
        verifyNoMoreInteractions(persister)
    }

    @Test
    fun shouldEndOngoingItemIfItemWithSameActivityAndStartIsGiven() {
        // GIVEN
        val ongoing = TimeTrackingItem("ongoing",
                LocalDateTime.of(2000, 10, 10, 10, 10))
        val expected = TimeTrackingItem("ongoing",
                LocalDateTime.of(2000, 10, 10, 10, 12),
                LocalDateTime.of(2000, 10, 10, 10, 13))

        val newActivity = NewActivity(expected)

        given(queries.queryItems(any())).willReturn(Stream.of(ongoing))
        given<TimeTrackingItem>(queries.lastItem).willReturn(ongoing)

        // WHEN
        sut!!.addNewActivity(newActivity)

        // THEN
        verify<ItemPersister>(persister).replace(ongoing, expected)
        verifyNoMoreInteractions(persister)
    }

    @Test
    fun shouldNotEndOngoingItemIfItemWithDifferentActivityWithSameStartIsGiven() {
        // GIVEN
        val expected = TimeTrackingItem("different",
                LocalDateTime.of(2000, 10, 10, 10, 12),
                LocalDateTime.of(2000, 10, 10, 10, 13))

        val newActivity = NewActivity(expected)

        given(queries.queryItems(any())).willReturn(Stream.empty())
        given<TimeTrackingItem>(queries.lastItem).willReturn(expected)

        // WHEN
        sut!!.addNewActivity(newActivity)

        // THEN
        verify<ItemPersister>(persister).persist(expected)
        verifyNoMoreInteractions(persister)
    }

    @Test
    fun shouldApplyMergePlan() {
        // GIVEN
        val first = item("A", 9, 0, 9, 32)
        val short = item("B", 9, 32, 9, 35)
        val last = item("A", 9, 35, 10, 0)
        val merged = item("A", 9, 0, 10, 0)
        val cleanup = CleanupFixture(first, short, last)

        // WHEN
        cleanup.sut.applyCleanup(CleanupPlan(listOf(
                CleanupChange.Changed(first, merged), CleanupChange.Removed(short), CleanupChange.Removed(last))))

        // THEN
        assertThat(cleanup.items()).containsExactly(merged)
        verifyReplaced(cleanup.bus, first, merged)
        verifyDeleted(cleanup.bus, short)
        verifyDeleted(cleanup.bus, last)
    }

    @Test
    fun shouldApplyDistributePlan() {
        // GIVEN
        val first = item("A", 9, 0, 9, 32)
        val short = item("B", 9, 32, 9, 36)
        val last = item("C", 9, 36, 10, 0)
        val other = item("D", 10, 0, 11, 0)
        val newFirst = item("A", 9, 0, 9, 35)
        val newLast = item("C", 9, 35, 10, 0)
        val cleanup = CleanupFixture(first, short, last, other)

        // WHEN
        cleanup.sut.applyCleanup(CleanupPlan(listOf(
                CleanupChange.Changed(first, newFirst), CleanupChange.Removed(short), CleanupChange.Changed(last, newLast))))

        // THEN
        assertThat(cleanup.items()).containsExactly(newFirst, newLast, other)
        verifyReplaced(cleanup.bus, first, newFirst)
        verifyDeleted(cleanup.bus, short)
        verifyReplaced(cleanup.bus, last, newLast)
    }

    @Test
    fun shouldApplyAbsorbAndDeletePlan() {
        // GIVEN
        val first = item("A", 9, 0, 9, 30)
        val absorbed = item("B", 9, 30, 9, 33)
        val isolated = item("C", 9, 40, 9, 42)
        val ongoing = TimeTrackingItem("D", LocalDateTime.of(2026, 11, 10, 9, 50))
        val newFirst = item("A", 9, 0, 9, 33)
        val cleanup = CleanupFixture(first, absorbed, isolated, ongoing)

        // WHEN
        cleanup.sut.applyCleanup(CleanupPlan(listOf(
                CleanupChange.Changed(first, newFirst), CleanupChange.Removed(absorbed), CleanupChange.Removed(isolated))))

        // THEN
        assertThat(cleanup.items()).containsExactly(newFirst, ongoing)
    }

    @Test
    fun shouldBlockDayCleanupTouchingSubmittedItems() {
        // GIVEN
        val first = item("A", 9, 0, 9, 30)
        val submitted = item("B", 9, 30, 9, 33)
        val cleanup = CleanupFixture(first, submitted)
        willThrow(IllegalStateException::class.java).given(cleanup.tracker).requireNotSubmitted(submitted)
        val before = cleanup.content

        // WHEN / THEN
        assertThatThrownBy {
            cleanup.sut.applyCleanup(CleanupPlan(listOf(
                    CleanupChange.Changed(first, item("A", 9, 0, 9, 33)), CleanupChange.Removed(submitted))))
        }.isInstanceOf(IllegalStateException::class.java)
        assertThat(cleanup.content).isEqualTo(before)
        verifyNoMoreInteractions(cleanup.bus)
    }

    private fun verifyReplaced(bus: MBassador<Any>, before: TimeTrackingItem, after: TimeTrackingItem) {
        verify(bus).publish(Matchers.argThat<Any> { it is ItemReplaced && it.beforeUpdate == before && it.afterUpdate == after })
    }

    private fun verifyDeleted(bus: MBassador<Any>, item: TimeTrackingItem) {
        verify(bus).publish(Matchers.argThat<Any> { it is ItemDeleted && it.deletedItem == item })
    }

    private fun item(activity: String, startHour: Int, startMinute: Int, endHour: Int, endMinute: Int) =
            TimeTrackingItem(activity, LocalDateTime.of(2026, 11, 10, startHour, startMinute),
                    LocalDateTime.of(2026, 11, 10, endHour, endMinute))

    /** [Activities] backed by a real [STTItemPersister] writing to an in-memory string. */
    private class CleanupFixture(vararg initialItems: TimeTrackingItem) {
        var content = ""
        val bus: MBassador<Any> = Mockito.mock(MBassador::class.java) as MBassador<Any>
        val tracker: SubmitStatusTracker = Mockito.mock(SubmitStatusTracker::class.java)
        private val persister = STTItemPersister(Provider<Reader> { StringReader(content) }, Provider<Writer> {
            object : StringWriter() {
                override fun close() {
                    content = toString()
                    super.close()
                }
            }
        })
        val sut = Activities(persister, Mockito.mock(TimeTrackingItemQueries::class.java), Optional.of(bus), tracker)

        init {
            initialItems.forEach { persister.persist(it) }
        }

        fun items(): List<TimeTrackingItem> {
            val reader = STTItemReader(StringReader(content))
            return generateSequence { reader.read() }.toList()
        }
    }
}
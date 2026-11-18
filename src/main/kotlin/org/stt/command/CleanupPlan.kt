package org.stt.command

import org.stt.model.TimeTrackingItem

/**
 * A single net change produced by [ShortItemCleanupPlanner]: either an item is removed completely
 * or it is replaced by an updated version (different start and/or end).
 *
 * [original] always refers to the item as it is currently persisted, so it can be deleted and checked
 * against the submit lock before anything is written.
 */
sealed class CleanupChange {
    abstract val original: TimeTrackingItem

    /** The [original] item is deleted without replacement. */
    data class Removed(override val original: TimeTrackingItem) : CleanupChange()

    /** The [original] item is replaced by [updated]. */
    data class Changed(override val original: TimeTrackingItem, val updated: TimeTrackingItem) : CleanupChange()
}

/**
 * The complete, ordered (by start of the original item) set of changes a short item cleanup of one day
 * would perform. The same plan is shown in the preview and later applied via [CommandHandler.applyCleanup].
 */
data class CleanupPlan(val changes: List<CleanupChange>) {
    /** True when the cleanup would not change anything. */
    fun isEmpty() = changes.isEmpty()
}

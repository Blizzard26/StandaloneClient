package org.stt.persistence.stt;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;

import org.stt.model.TimeTrackingItem;

import com.google.common.base.Optional;
import org.stt.persistence.ItemReader;
import org.stt.persistence.ItemWriter;

class InsertHelper {
	private final ItemReader reader;
	private final ItemWriter writer;
	private final TimeTrackingItem itemToInsert;

	public InsertHelper(ItemReader reader, ItemWriter writer,
			TimeTrackingItem itemToInsert) {
		this.itemToInsert = checkNotNull(itemToInsert);
		this.writer = checkNotNull(writer);
		this.reader = checkNotNull(reader);
	}

	public void performInsert() throws IOException {
		Optional<TimeTrackingItem> lastReadItem = copyAllNonIntersectingItemsBeforeItemToInsert();
		adjustEndOfLastItemReadAndWrite(lastReadItem);
		writer.write(itemToInsert);
		lastReadItem = skipAllItemsCompletlyCoveredByItemToInsert(lastReadItem);
		adjustStartOfLastItemReadAndWrite(lastReadItem);
		copyRemainingItems();
	}

	private void adjustStartOfLastItemReadAndWrite(
			Optional<TimeTrackingItem> lastReadItem) throws IOException {
		if (lastReadItem.isPresent()
				&& endOfReadItemIsAfterEndOfItemToInsert(lastReadItem.get())) {
			TimeTrackingItem itemAfterItemToInsert = lastReadItem.get()
					.withStart(itemToInsert.getEnd().get());
			writer.write(itemAfterItemToInsert);
		}
	}

	private void adjustEndOfLastItemReadAndWrite(
			Optional<TimeTrackingItem> lastReadItem) throws IOException {
		if (lastReadItem.isPresent()
				&& startOfReadItemIsBeforeStartOfItemToInsert(lastReadItem
						.get())) {
			TimeTrackingItem itemBeforeItemToInsert = lastReadItem.get()
					.withEnd(itemToInsert.getStart());
			writer.write(itemBeforeItemToInsert);
		}
	}

	private void copyRemainingItems() throws IOException {
		Optional<TimeTrackingItem> lastReadItem;
		while ((lastReadItem = reader.read()).isPresent()) {
			writer.write(lastReadItem.get());
		}
	}

	private Optional<TimeTrackingItem> skipAllItemsCompletlyCoveredByItemToInsert(
			Optional<TimeTrackingItem> lastReadItem) {
		while (lastReadItem.isPresent()
				&& !endOfReadItemIsAfterEndOfItemToInsert(lastReadItem.get())) {
			lastReadItem = reader.read();
		}
		return lastReadItem;
	}

	private Optional<TimeTrackingItem> copyAllNonIntersectingItemsBeforeItemToInsert()
			throws IOException {
		Optional<TimeTrackingItem> lastReadItem;
		while ((lastReadItem = reader.read()).isPresent()
				&& endOfReadItemIsBeforeOrEqualToStartOfItemToInsert(lastReadItem
						.get())) {
			writer.write(lastReadItem.get());
		}
		return lastReadItem;
	}

	private boolean startOfReadItemIsBeforeStartOfItemToInsert(
			TimeTrackingItem lastReadItem) {
		return lastReadItem.getStart().isBefore(itemToInsert.getStart());
	}

	private boolean endOfReadItemIsAfterEndOfItemToInsert(
			TimeTrackingItem lastReadItem) {
		if (!itemToInsert.getEnd().isPresent()) {
			return false;
		}
		if (!lastReadItem.getEnd().isPresent()) {
			return true;
		}
		return lastReadItem.getEnd().get().isAfter(itemToInsert.getEnd().get());
	}

	private boolean endOfReadItemIsBeforeOrEqualToStartOfItemToInsert(
			TimeTrackingItem lastReadItem) {
		return lastReadItem.getEnd().isPresent()
				&& !lastReadItem.getEnd().get()
						.isAfter(itemToInsert.getStart());
	}
}

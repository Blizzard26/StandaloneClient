package org.stt.command;

import org.joda.time.DateTime;
import org.stt.model.TimeTrackingItem;
import org.stt.persistence.ItemPersister;
import org.stt.query.TimeTrackingItemQueries;

import com.google.common.base.Optional;

public class NewItemCommandTextItem implements CommandTextItem {

	private TimeTrackingItem timeTrackingItem;

	public NewItemCommandTextItem(String comment, DateTime start, DateTime end) {
		this.timeTrackingItem = new TimeTrackingItem(comment, start, end);
	}

	public NewItemCommandTextItem(String comment, DateTime start) {
		this.timeTrackingItem = new TimeTrackingItem(comment, start);
	}

	@Override
	public Optional<Command> getCommand(ItemPersister persister, TimeTrackingItemQueries timeTrackingItemQueries) {
		NewItemCommand command = new NewItemCommand(persister, timeTrackingItem);
		return Optional.<Command>of(command);
	}

}

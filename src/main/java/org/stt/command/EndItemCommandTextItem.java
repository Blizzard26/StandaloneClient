package org.stt.command;

import org.joda.time.DateTime;
import org.stt.model.TimeTrackingItem;
import org.stt.persistence.ItemPersister;
import org.stt.query.TimeTrackingItemQueries;

import com.google.common.base.Optional;

public class EndItemCommandTextItem implements CommandTextItem {

	private Optional<DateTime> date;
	private boolean resume;

	public EndItemCommandTextItem(DateTime date) {
		this.date = Optional.fromNullable(date);
		this.resume = false;
	}
	
	public EndItemCommandTextItem(DateTime date, boolean resume) {
		this.resume = resume;
		this.date = Optional.fromNullable(date);
	}

	@Override
	public Optional<Command> getCommand(ItemPersister persister, TimeTrackingItemQueries timeTrackingItemQueries) {
		if (resume == true)
		{
			return resumePreviousItemCommand(persister, timeTrackingItemQueries);
		}
		else
		{
			return endCurrentItemCommand(persister, timeTrackingItemQueries);
		}
	}

	private Optional<Command> resumePreviousItemCommand(ItemPersister persister,
			TimeTrackingItemQueries timeTrackingItemQueries) {
		Optional<TimeTrackingItem> currentTimeTrackingitem = timeTrackingItemQueries.getCurrentTimeTrackingitem();
		if (currentTimeTrackingitem.isPresent()) {
			Optional<TimeTrackingItem> previousTimeTrackingItem = timeTrackingItemQueries.getPreviousTimeTrackingItem(currentTimeTrackingitem.get());
			
			if (previousTimeTrackingItem.isPresent())
			{
				TimeTrackingItem previousItem = previousTimeTrackingItem.get();
				TimeTrackingItem resumeItem = new TimeTrackingItem(previousItem.getComment().get(), date.or(DateTime.now()));
				
				return Optional.<Command>of(new ResumeCommand(persister, resumeItem));
			}
		}
		return Optional.absent();
	}

	private Optional<Command> endCurrentItemCommand(ItemPersister persister, TimeTrackingItemQueries timeTrackingItemQueries) {
		Optional<TimeTrackingItem> currentTimeTrackingitem = timeTrackingItemQueries.getCurrentTimeTrackingitem();
		if (currentTimeTrackingitem.isPresent()) {
			TimeTrackingItem unfinisheditem = currentTimeTrackingitem.get();
			TimeTrackingItem nowFinishedItem = unfinisheditem.withEnd(date.or(DateTime.now()));
			return Optional.<Command>of(new EndCurrentItemCommand(persister, unfinisheditem, nowFinishedItem));
		}
		return Optional.absent();
	}

}

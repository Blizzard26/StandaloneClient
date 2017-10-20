package org.stt.event;
 

import static com.google.common.base.Preconditions.checkNotNull;

import org.stt.Service;
import org.stt.model.CurrentItemChanged;
import org.stt.model.FileChanged;
import org.stt.model.ItemModified;
import org.stt.model.TimeTrackingItem;
import org.stt.query.TimeTrackingItemQueries;

import com.google.common.base.Optional;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;

public class CurrentItemChangeListenerService implements Service {

	private EventBus eventBus;
	private TimeTrackingItemQueries timeTrackingItemQueries;
	private Optional<TimeTrackingItem> currentItem;

	@Inject
	public CurrentItemChangeListenerService(EventBus eventBus, TimeTrackingItemQueries timeTrackingItemQueries) {
		this.eventBus = checkNotNull(eventBus);
		this.timeTrackingItemQueries = checkNotNull(timeTrackingItemQueries);
	}
	
	@Override
	public void start() throws Exception {
		eventBus.register(this);
	}

	@Override
	public void stop() {
		eventBus.unregister(this);	
	}
	
	@Subscribe
	public void onFileChange(FileChanged event)
	{
		checkActiveItem();
	}
	
	@Subscribe
	public void itemChanged(ItemModified itemModified)
	{
		checkActiveItem();
	}
	
	private void checkActiveItem() {
		Optional<TimeTrackingItem> currentTimeTrackingitem = timeTrackingItemQueries.getCurrentTimeTrackingitem();

		if (!currentTimeTrackingitem.equals(currentItem)) {
			eventBus.post(new CurrentItemChanged(currentTimeTrackingitem));
			currentItem = currentTimeTrackingitem;
		}
		
	}

}

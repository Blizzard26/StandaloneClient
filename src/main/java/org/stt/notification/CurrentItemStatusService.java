package org.stt.notification;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ResourceBundle;

import org.stt.Service;
import org.stt.command.CommandParser;
import org.stt.gui.Notification;
import org.stt.model.FileChanged;
import org.stt.model.TimeTrackingItem;
import org.stt.query.TimeTrackingItemQueries;

import com.google.common.base.Optional;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

public class CurrentItemStatusService  implements Service{

	private Notification notification;
	private Optional<TimeTrackingItem> activeItem;
	private TimeTrackingItemQueries searcher;
	private EventBus eventBus;
	private ResourceBundle i18n;

	public CurrentItemStatusService(ResourceBundle i18n, EventBus eventBus,
			TimeTrackingItemQueries searcher, Notification notification) {
		this.i18n = checkNotNull(i18n);
		this.eventBus = checkNotNull(eventBus);
		this.searcher = checkNotNull(searcher);
		this.notification = checkNotNull(notification);
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
	public void onFileChanged(FileChanged event) {
		Optional<TimeTrackingItem> currentTimeTrackingitem = searcher.getCurrentTimeTrackingitem();

		if (currentTimeTrackingitem.isPresent() && !currentTimeTrackingitem.equals(activeItem)) {
			TimeTrackingItem timeTrackingItem = currentTimeTrackingitem.get();
			StringBuilder text = new StringBuilder();
			text.append(i18n.getString("onItem")).append(" ")
					.append(CommandParser.itemToCommand(timeTrackingItem));
			
			notification.setStatus(text.toString());
			notification.info(text.toString());
			
		}
		
		activeItem = currentTimeTrackingitem;

	}
	
}

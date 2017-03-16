package org.stt.notification;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ResourceBundle;

import org.stt.Service;
import org.stt.command.CommandParser;
import org.stt.gui.Notification;
import org.stt.model.CurrentItemChanged;
import org.stt.model.TimeTrackingItem;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

public class CurrentItemStatusService  implements Service{

	private Notification notification;
	private EventBus eventBus;
	private ResourceBundle i18n;

	public CurrentItemStatusService(ResourceBundle i18n, EventBus eventBus,
			Notification notification) {
		this.i18n = checkNotNull(i18n);
		this.eventBus = checkNotNull(eventBus);
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
	public void onCurrentItemChanged(CurrentItemChanged event)
	{
		TimeTrackingItem currentItem = event.getCurrentItem();
		
		StringBuilder text = new StringBuilder();
		text.append(i18n.getString("onItem")).append(" ")
				.append(CommandParser.itemToCommand(currentItem));
		
		String message = text.toString();
		notification.setStatus(message);
		notification.info(message);
	}

}

package org.stt.notification;

import java.util.ResourceBundle;

import org.stt.query.TimeTrackingItemQueries;

import com.google.common.eventbus.EventBus;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;

public class StatusNotificationServiceModule extends AbstractModule {

	@Override
	protected void configure() {
		//
	}
	
	@Provides
	public CurrentItemStatusService provideCurrentItemStatusService(ResourceBundle i18n,
			EventBus eventBus, Notification notification, 
			TimeTrackingItemQueries timeTrackingItemQueries)
	{
		return new CurrentItemStatusService(i18n, eventBus, notification);
	}

}

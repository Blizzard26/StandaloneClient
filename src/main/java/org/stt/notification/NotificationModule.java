package org.stt.notification;

import org.stt.gui.systemtray.SystemTrayIcon;

import com.google.inject.AbstractModule;

public class NotificationModule extends AbstractModule {

	//private static final Logger LOG = Logger.getLogger(NotificationModule.class.getName());

	@Override
	protected void configure() {
		 bind(Notification.class).to(SystemTrayIcon.class);
	}

}

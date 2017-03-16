package org.stt.gui;

import java.util.logging.Logger;

import com.google.inject.AbstractModule;

public class SystemTrayNotificationModule extends AbstractModule {

	private static final Logger LOG = Logger.getLogger(SystemTrayNotificationModule.class
            .getName());

	@Override
	protected void configure() {
		 bind(Notification.class).to(SystemTrayIcon.class);
	     //bind(SystemTrayIcon.class).to(SystemTrayIcon.class);
		
	}

}

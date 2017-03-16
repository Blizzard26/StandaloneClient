package org.stt.gui.systemtray;

import java.util.ResourceBundle;
import java.util.logging.Logger;

import org.stt.Configuration;
import org.stt.gui.Notification;
import org.stt.gui.jfx.LogWorkWindowBuilder;

import com.google.common.eventbus.EventBus;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;

public class SystemTrayModule extends AbstractModule {

	private static final Logger LOG = Logger.getLogger(SystemTrayModule.class
            .getName());
	private SystemTrayIcon trayIcon;

	@Override
	protected void configure() {
		 bind(Notification.class).to(SystemTrayIcon.class);
	     //bind(SystemTrayIcon.class).to(SystemTrayIcon.class);
		
	}
	
	@Provides
	public SystemTrayIcon provideSystemTrayIcon(ResourceBundle i18n, Configuration configuration, LogWorkWindowBuilder logWorkWindowBuilder, EventBus eventBus)
	{
		if (trayIcon == null)
		{
			trayIcon = new SystemTrayIcon(i18n, configuration, logWorkWindowBuilder, eventBus);
		}
		return trayIcon;
	}

}

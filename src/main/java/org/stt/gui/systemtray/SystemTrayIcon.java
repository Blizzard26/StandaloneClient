package org.stt.gui.systemtray;

import static com.google.common.base.Preconditions.checkNotNull;

import java.awt.SystemTray;
import java.awt.TrayIcon.MessageType;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import org.stt.Configuration;
import org.stt.event.ShuttingDown;
import org.stt.gui.Notification;
import org.stt.gui.jfx.ApplicationControl;
import org.stt.gui.jfx.LogWorkWindowBuilder;
import org.stt.gui.jfx.STTApplication;

import com.google.common.eventbus.EventBus;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Rectangle2D;
import javafx.stage.Stage;

@Singleton
public class SystemTrayIcon implements Notification {

	private Logger LOG = Logger.getLogger(SystemTrayIcon.class.getName());
	
	private Stage primaryStage;
	private java.awt.TrayIcon trayIcon;
	private EventBus eventBus;


	private LogWorkWindowBuilder logWorkWindowBuilder;
	
	private Configuration configuration;

	private ResourceBundle i18n;

	private ApplicationControl application;

	@Inject
	public SystemTrayIcon(ResourceBundle i18n, 
			Configuration configuration,
			ApplicationControl application,
			LogWorkWindowBuilder logWorkWindowBuilder,
			EventBus eventBus) {
		this.i18n = checkNotNull(i18n);
		this.configuration = checkNotNull(configuration);
		this.application = checkNotNull(application);
		this.logWorkWindowBuilder = checkNotNull(logWorkWindowBuilder);
		this.eventBus = checkNotNull(eventBus);
	}
	

	public void start(Stage primaryStage) {
		this.primaryStage = checkNotNull(primaryStage);
		
		if (!SystemTray.isSupported())
			return;
		
		
		try {
            // ensure awt toolkit is initialized.
            java.awt.Toolkit.getDefaultToolkit();

            // app requires system tray support, just exit if there is no support.
            if (!java.awt.SystemTray.isSupported()) {
                LOG.warning("No system tray support.");
                return;
            }

            // set up a system tray icon.
            java.awt.SystemTray tray = java.awt.SystemTray.getSystemTray();
            final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
            URL imageLoc = contextClassLoader.getResource("Logo.png");
            java.awt.Image image = ImageIO.read(imageLoc);
            trayIcon = new java.awt.TrayIcon(image);
            trayIcon.setImageAutoSize(true);
            setStatus(null);

            // if the user double-clicks on the tray icon, show the main app stage.
//            trayIcon.addActionListener(event -> {
//	        	toggleStage();
//            });
            trayIcon.addMouseListener(new MouseListener() {
				
				@Override
				public void mouseReleased(MouseEvent e) {
					// 
				}
				
				@Override
				public void mousePressed(MouseEvent e) {
					// 					
				}
				
				@Override
				public void mouseExited(MouseEvent e) {
					// 					
				}
				
				@Override
				public void mouseEntered(MouseEvent e) {
					// 
				}
				
				@Override
				public void mouseClicked(MouseEvent e) {
					if (e.getClickCount() == 1 && e.getButton() == MouseEvent.BUTTON1)
					{	
						showLogWorkWindow();
					} 
//					else if (e.getClickCount() == 1 && e.getButton() == MouseEvent.BUTTON2)
//					{
//						
//					}					
					else if (e.getClickCount() > 1 && e.getButton() == MouseEvent.BUTTON1)
					{
						toggleStage();
					}
				}
			});

            // if the user selects the default menu item (which includes the app name), 
            // show the main app stage.
            java.awt.MenuItem openItem = new java.awt.MenuItem(i18n.getString("window.title"));
            openItem.addActionListener(event -> {
            		Platform.runLater(this::showMainWindow);
            });
            

            // the convention for tray icons seems to be to set the default icon for opening
            // the application stage in a bold font.
            java.awt.Font defaultFont = java.awt.Font.decode(null);
            java.awt.Font boldFont = defaultFont.deriveFont(java.awt.Font.BOLD);
            openItem.setFont(boldFont);

            java.awt.MenuItem logItem = new java.awt.MenuItem(i18n.getString("logWork"));
            logItem.addActionListener(event -> Platform.runLater(this::showLogWorkWindow));

            
            // to really exit the application, the user must go to the system tray icon
            // and select the exit option, this will shutdown JavaFX and remove the
            // tray icon (removing the tray icon will also shut down AWT).
            java.awt.MenuItem exitItem = new java.awt.MenuItem(i18n.getString("exit"));
            exitItem.addActionListener(event -> {
                //notificationTimer.cancel();
            	tray.remove(trayIcon);
            	
            	Platform.runLater(() -> {
            		exit();
            	});

            });

            // setup the popup menu for the application.
            final java.awt.PopupMenu popup = new java.awt.PopupMenu();
            popup.add(openItem);
            popup.add(logItem);
            popup.addSeparator();
            popup.add(exitItem);
            trayIcon.setPopupMenu(popup);


            // add the application tray icon to the system tray.
            tray.add(trayIcon);
            
            if (configuration.getMinimizedToTray())
            {            
            	Platform.setImplicitExit(false);
	            primaryStage.iconifiedProperty().addListener(new ChangeListener<Boolean>() {
	
					@Override
					public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
						if (newValue.booleanValue() == true)
						{
		    				hideMainWindow();
						}
					}
				});
            }
            

        } catch (java.awt.AWTException | IOException e) {
            LOG.log(Level.SEVERE, "Unable to init system tray", e);
        }
	}


	private void toggleStage() {
		try {
			if (primaryStage.isShowing())
				Platform.runLater(this::hideMainWindow);
			else
				Platform.runLater(this::showMainWindow);
		} catch (Exception e) {
			LOG.log(Level.SEVERE, "Exception while executing TrayIcon action", e);
		}
	}

	private void exit() {
		try {
			primaryStage.close();
			Platform.exit();
		} finally {
			eventBus.post(new ShuttingDown());
		}
	}

	private void showLogWorkWindow() {
		
		Platform.runLater(new Runnable() {
			
			@Override
			public void run() {
				try {
					logWorkWindowBuilder.setupStage();
				} catch (IOException e) {
					LOG.log(Level.SEVERE, "Exception while creating LogWorkWindow", e);
				}
			}
		});
		
	}

	private void showMainWindow() {
		application.show();
	}
	
	private void hideMainWindow() {
		application.minimizeToTray();
	}


	@Override
	public void error(String errorMessage) {
		displayMessage(errorMessage, MessageType.ERROR);
	}


	@Override
	public void warning(String warningMessage) {
		displayMessage(warningMessage, MessageType.WARNING);
	}


	@Override
	public void info(String infoMessage) {
		displayMessage(infoMessage, MessageType.INFO);
	}
	
	public void displayMessage(String message, MessageType messageType) {
		if (trayIcon != null) {
			trayIcon.displayMessage(i18n.getString("window.title"), message, messageType);
		}
	}

	@Override
	public void setStatus(String statusMessage) {
		if (trayIcon != null)
		{
			StringBuilder s = new StringBuilder();
			s.append(i18n.getString("window.title"));
			
			if (statusMessage != null && !statusMessage.isEmpty())
			{
				s.append(System.lineSeparator()).append(statusMessage);
			}
			trayIcon.setToolTip(s.toString());
		}
	}




}

package org.stt.gui;

import static com.google.common.base.Preconditions.checkNotNull;

import java.awt.SystemTray;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import org.stt.Configuration;
import org.stt.command.CommandParser;
import org.stt.event.ShuttingDown;
import org.stt.gui.jfx.LogWorkWindowBuilder;
import org.stt.model.FileChanged;
import org.stt.model.TimeTrackingItem;
import org.stt.query.TimeTrackingItemQueries;

import com.google.common.base.Optional;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.stage.Stage;

public class SystemTrayIcon {

	private Logger LOG = Logger.getLogger(SystemTrayIcon.class.getName());
	
	private Stage primaryStage;
	private java.awt.TrayIcon trayIcon;
	private EventBus eventBus;


	private LogWorkWindowBuilder logWorkWindowBuilder;

	private TimeTrackingItemQueries searcher;

	private Configuration configuration;

	private Optional<TimeTrackingItem> activeItem = Optional.<TimeTrackingItem> absent();

	@Inject
	public SystemTrayIcon(Configuration configuration,
			EventBus eventBus,
			TimeTrackingItemQueries searcher,
			LogWorkWindowBuilder logWorkWindowBuilder) {
		this.configuration = checkNotNull(configuration);
		this.eventBus = checkNotNull(eventBus);	
		this.searcher = checkNotNull(searcher);
		this.logWorkWindowBuilder = checkNotNull(logWorkWindowBuilder);
	}
	

	public void start(Stage primaryStage) {
		this.primaryStage = checkNotNull(primaryStage);
		
		if (!SystemTray.isSupported())
			return;
		
		
		eventBus.register(this);
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
            trayIcon.setToolTip("SimpleTimeTracking");

            // if the user double-clicks on the tray icon, show the main app stage.
            trayIcon.addActionListener(event -> {
	        	if (primaryStage.isShowing())
	        		Platform.runLater(() -> primaryStage.setIconified(true));
	        	else
	        		Platform.runLater(this::showStage);
            });
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
				}
			});

            // if the user selects the default menu item (which includes the app name), 
            // show the main app stage.
            java.awt.MenuItem openItem = new java.awt.MenuItem("SimpleTimeTracking");
            openItem.addActionListener(event -> {
            		Platform.runLater(this::showStage);
            });
            

            // the convention for tray icons seems to be to set the default icon for opening
            // the application stage in a bold font.
            java.awt.Font defaultFont = java.awt.Font.decode(null);
            java.awt.Font boldFont = defaultFont.deriveFont(java.awt.Font.BOLD);
            openItem.setFont(boldFont);

            java.awt.MenuItem logItem = new java.awt.MenuItem("Log Work");
            logItem.addActionListener(event -> Platform.runLater(this::showLogWorkWindow));

            
            // to really exit the application, the user must go to the system tray icon
            // and select the exit option, this will shutdown JavaFX and remove the
            // tray icon (removing the tray icon will also shut down AWT).
            java.awt.MenuItem exitItem = new java.awt.MenuItem("Exit");
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
		    				primaryStage.hide();
						}
					}
				});
            }
            

        } catch (java.awt.AWTException | IOException e) {
            LOG.log(Level.SEVERE, "Unable to init system tray", e);
        }
	}

    @Subscribe
    public void onFileChanged(FileChanged event) {
        Optional<TimeTrackingItem> currentTimeTrackingitem = searcher.getCurrentTimeTrackingitem();
        
        if (currentTimeTrackingitem.isPresent() && !currentTimeTrackingitem.equals(activeItem))
        {
        	TimeTrackingItem timeTrackingItem = currentTimeTrackingitem.get();
        	StringBuilder stringBuilder = new StringBuilder();
			stringBuilder.append("on ").append(CommandParser.itemToCommand(timeTrackingItem));
			trayIcon.displayMessage("SimpleTimeTracking", stringBuilder.toString(), 
        			java.awt.TrayIcon.MessageType.INFO); 
        }
        activeItem = currentTimeTrackingitem;
        

    }
	
	private void exit() {
		Platform.setImplicitExit(true);
		primaryStage.close();
		Platform.exit();
		eventBus.post(new ShuttingDown());
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

	private void showStage() {
		if (primaryStage != null) {
			primaryStage.show();
			primaryStage.setIconified(false);
			primaryStage.toFront();
		}
	}




}

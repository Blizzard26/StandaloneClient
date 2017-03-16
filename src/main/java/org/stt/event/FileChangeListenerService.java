package org.stt.event;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.stt.Service;
import org.stt.model.FileChanged;
import org.stt.persistence.DatabaseFile;

import com.google.common.eventbus.EventBus;
import com.google.inject.Inject;

import javafx.application.Platform;

public class FileChangeListenerService implements Service
{
	
	Logger LOG = Logger.getLogger(FileChangeListenerService.class.getName());

	private EventBus eventBus;
	private WatchService watchService;

	private Path databaseFile;

	class WatchHandler extends Thread
	{
		Logger LOG = Logger.getLogger(WatchHandler.class.getName());
		
		private Path fileToWatch;
		private WatchService watchService;

		public WatchHandler(Path fileToWatch, WatchService watchService) {
			this.fileToWatch = checkNotNull(fileToWatch);
			this.watchService = checkNotNull(watchService);
		}
		
		@Override
		public void run()
		{

			try {
				Path parentDir = fileToWatch.getParent();
				if (parentDir == null)
					return;
				parentDir.register(watchService, 
						StandardWatchEventKinds.ENTRY_CREATE,
			            StandardWatchEventKinds.ENTRY_DELETE, 
			            StandardWatchEventKinds.ENTRY_MODIFY);
				
				while (!Thread.interrupted()) {
					try {
						WatchKey watchKey = watchService.take();
						List<WatchEvent<?>> pollEvents = watchKey.pollEvents();
						for (WatchEvent<?> event : pollEvents)
						{
							Path changedFile = (Path) event.context();
							
							if (fileToWatch.endsWith(changedFile))
							{
								Platform.runLater(new Runnable() {
									
									@Override
									public void run() {
										notifyFileChanged(changedFile);
									}

									
								});
								
							}
						}
						watchKey.reset();
					} catch (InterruptedException | ClosedWatchServiceException e) {
						Thread.currentThread().interrupt();
					}
				}
			} catch (Exception e) {
				// Ignore
				LOG.log(Level.SEVERE, "Exception in WatchHandler", e);
			}
		}
	}
	

	@Inject
	public FileChangeListenerService(@DatabaseFile File file, EventBus eventBus) {
		this.databaseFile = checkNotNull(file).toPath();
		this.eventBus = checkNotNull(eventBus);
		LOG.info("Starting FileChangeListenerService for file "+this.databaseFile);
	}
	
	@Override
	public void start() throws Exception {
		watchService = FileSystems.getDefault().newWatchService();
		
		WatchHandler handler = new WatchHandler(databaseFile, watchService);
		handler.start();
	}

	@Override
	public void stop() {
		try {
			if (watchService != null)
				watchService.close();
		} catch (IOException e) {
			LOG.log(Level.SEVERE, "Exception while closing watch service", e);
		}
		watchService = null;
	}
	
	protected void notifyFileChanged(Path changedFile) {
		eventBus.post(new FileChanged(changedFile));
	}
}

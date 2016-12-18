package org.stt.persistence.db;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.stt.model.TimeTrackingItem;
import org.stt.persistence.ItemReader;
import org.stt.persistence.db.h2.H2DBStorage;

import com.google.common.base.Optional;
import com.google.inject.Inject;

public class DBItemReader implements ItemReader {

	private static final Logger LOG = Logger.getLogger(ItemReader.class
            .getName());
	
	private H2DBStorage dbStorage;

	private Iterator<TimeTrackingItem> itemIter;
	

	@Inject
	public DBItemReader(H2DBStorage dbStorage)  {
		this.dbStorage = dbStorage;
	}

	@Override
	public void close()  {
		itemIter = null;
	}

	@Override
	public Optional<TimeTrackingItem> read() {
		
		if (itemIter == null)
		{
			try {
				itemIter = this.dbStorage.getAllItems().iterator();
			} catch (SQLException e) {
				LOG.log(Level.SEVERE, "SQL Exception while reading items", e);
				return Optional.<TimeTrackingItem>absent();
			}
		}
		
		if (itemIter.hasNext())
		{
			return Optional.of(itemIter.next());
		}
				
		return Optional.<TimeTrackingItem>absent();
	}

}

package org.stt.persistence.db;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.logging.Logger;

import org.stt.model.TimeTrackingItem;
import org.stt.persistence.ItemReader;

import com.google.common.base.Optional;
import com.google.inject.Inject;

public class DBItemReader implements ItemReader {

	 private static final Logger LOG = Logger.getLogger(ItemReader.class
	            .getName());
	
	private DBStorage dbStorage;

	private Iterator<TimeTrackingItem> itemIter;
	

	@Inject
	public DBItemReader(DBStorage dbStorage)  {
		this.dbStorage = dbStorage;
	}

	@Override
	public void close()  {
		
	}

	@Override
	public Optional<TimeTrackingItem> read() {
		
		if (itemIter == null)
		{
			try {
				itemIter = this.dbStorage.getAllItems().iterator();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		if (itemIter.hasNext())
		{
			return Optional.of(itemIter.next());
		}
				
		return Optional.<TimeTrackingItem>absent();
	}

}

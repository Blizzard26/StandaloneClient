package org.stt.persistence.db;

import java.io.IOException;
import java.sql.SQLException;

import org.stt.model.TimeTrackingItem;
import org.stt.persistence.ItemWriter;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class DBItemWriter implements ItemWriter {
	
	
	private DBStorage dbStorage;

	@Inject
	public DBItemWriter(DBStorage dbStorage) {
		this.dbStorage = dbStorage;
	}

	@Override
	public void close() throws IOException {

	}

	@Override
	public void write(TimeTrackingItem item) throws IOException {
		Preconditions.checkNotNull(item);
		try {
			dbStorage.insertItemInDB(item);
		} catch (SQLException e) {
			throw new IOException(e);
		}
	}
	

}

package org.stt.persistence.db;

import java.io.IOException;
import java.sql.SQLException;

import org.stt.model.TimeTrackingItem;
import org.stt.persistence.ItemWriter;
import org.stt.persistence.db.h2.H2DBStorage;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class DBItemWriter implements ItemWriter {
	
	
	private H2DBStorage dbStorage;

	@Inject
	public DBItemWriter(H2DBStorage dbStorage) {
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

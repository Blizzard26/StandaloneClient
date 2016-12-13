package org.stt.persistence.h2;

import java.io.IOException;

import org.stt.model.TimeTrackingItem;
import org.stt.persistence.ItemPersister;

import com.google.inject.Inject;

public class H2ItemPersister implements ItemPersister {

	private H2ConnectionProvider connectionProvider;

	@Inject
	public H2ItemPersister(@H2DBConnection H2ConnectionProvider connectionProvider) {
		this.connectionProvider = connectionProvider;
	}
	
	@Override
	public void close() throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void insert(TimeTrackingItem item) throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void replace(TimeTrackingItem item, TimeTrackingItem with) throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void delete(TimeTrackingItem item) throws IOException {
		// TODO Auto-generated method stub
		
	}

}

package org.stt.persistence.db;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import org.stt.model.TimeTrackingItem;
import org.stt.persistence.ItemPersister;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class DBItemPersister implements ItemPersister {

	private final DBStorage dbStorage;

	@Inject
	public DBItemPersister(DBStorage dbStorage) throws SQLException {
		this.dbStorage = dbStorage;
	}

	@Override
	public void close() throws IOException {
		
	}

	@Override
	public void insert(TimeTrackingItem item) throws IOException {
		Preconditions.checkNotNull(item);

		try {
			List<TimeTrackingItem> overlappingItems = this.dbStorage.getTimeTrackingItemsInRange(item.getStart(), item.getEnd());
			
			for (TimeTrackingItem other : overlappingItems)
			{
				if (item.getStart().isBefore(other.getStart()))
				{
					
					//    |------
					// |--------- item
					if (!item.getEnd().isPresent() // Inserted item has no end
					
					//   |----|
					// |---------| item
							|| (other.getEnd().isPresent() 
									&& item.getEnd().get().isAfter(other.getEnd().get())))
					{
						this.dbStorage.deleteItemInDB(other);
					}

					//   |-----------
					// |---------| item
					else if (item.getEnd().isPresent() 
							&& (!other.getEnd().isPresent() 
									|| other.getEnd().get().isAfter(item.getEnd().get())))
					{
						TimeTrackingItem newItem;
						if (other.getEnd().isPresent())
						{
							newItem = new TimeTrackingItem(other.getComment().orNull(), 
									item.getEnd().get(), 
									other.getEnd().get());
						}
						else
						{
							newItem = new TimeTrackingItem(other.getComment().orNull(), 
									item.getEnd().get());
						}
						replace(other, newItem);
					}
				}
				else
				{
					// |----
					// |----
					
					
					// |---------|
					//   |-----| item
					if (item.getEnd().isPresent() 
							&& (!other.getEnd().isPresent() || item.getEnd().get().isBefore(other.getEnd().get())))
					{
						// Split old item
						TimeTrackingItem first = new TimeTrackingItem(other.getComment().orNull(), other.getStart(), item.getStart());
						TimeTrackingItem second;
						if (other.getEnd().isPresent())
						{
							second = new TimeTrackingItem(other.getComment().orNull(), item.getEnd().get(), other.getEnd().get());
						}
						else
						{
							second = new TimeTrackingItem(other.getComment().orNull(), item.getEnd().get());
						}
						this.dbStorage.deleteItemInDB(other);
						this.dbStorage.insertItemInDB(first);
						this.dbStorage.insertItemInDB(second);
					}
					
					// |----|
					//   |----- item
					TimeTrackingItem newItem = new TimeTrackingItem(other.getComment().orNull(), other.getStart(), item.getStart());
					replace(other, newItem);
					
				}
				
			}
			this.dbStorage.insertItemInDB(item);
		} catch (SQLException e) {
			throw new IOException(e);
		}

		
	}

	

	@Override
	public void replace(TimeTrackingItem item, TimeTrackingItem with) throws IOException {
		Preconditions.checkNotNull(item);
		Preconditions.checkNotNull(with);
		try {
			this.dbStorage.deleteItemInDB(item);
			this.dbStorage.insertItemInDB(with);
		} catch (SQLException e) {
			throw new IOException(e);
		}
	}

	@Override
	public void delete(TimeTrackingItem item) throws IOException {
		Preconditions.checkNotNull(item);

		try {
			this.dbStorage.deleteItemInDB(item);
		} catch (SQLException e) {
			throw new IOException(e);
		}
	}


}

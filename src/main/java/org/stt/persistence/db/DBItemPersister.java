package org.stt.persistence.db;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import org.joda.time.DateTime;
import org.stt.model.TimeTrackingItem;
import org.stt.persistence.ItemPersister;

import com.google.common.base.Optional;
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
			DateTime start = item.getStart();
			Optional<DateTime> itemEnd = item.getEnd();
			List<TimeTrackingItem> overlappingItems = this.dbStorage.getTimeTrackingItemsInRange(start, itemEnd);
			
			for (TimeTrackingItem other : overlappingItems) {
				DateTime otherStart = other.getStart();
				
				if (start.isBefore(otherStart) || start.isEqual(otherStart)) {

					if (!itemEnd.isPresent()) // Inserted item has no end
					{
						// |------ other
						// |--------- item
						this.dbStorage.deleteItemInDB(other);
					} else { // itemEnd.isPresent
						DateTime end = itemEnd.get();

						if (other.getEnd().isPresent()) {
							DateTime otherEnd = other.getEnd().get();

							if (otherEnd.isAfter(end)) {
								// |--------| other
								// |-----|    item
								TimeTrackingItem newItem = new TimeTrackingItem(other.getComment().orNull(), end,
											otherEnd); 
								replace(other, newItem);
							} else { // (end.isAfter(otherEnd) || end.isEqual(otherEnd))
								// |----| other
								// |---------| item
								this.dbStorage.deleteItemInDB(other);
							}
						} else { // !other.getEnd().isPresent()
							// |--------- other
							// |------| item
							TimeTrackingItem newItem = new TimeTrackingItem(other.getComment().orNull(), end);
							replace(other, newItem);
						}
					}
				} else { // start.isAfter(other.getStart())
					if (itemEnd.isPresent()) {

						DateTime end = itemEnd.get();

						if (!other.getEnd().isPresent()) {
							// |---------- other
							//   |-----| item

							// Split old item
							TimeTrackingItem first = new TimeTrackingItem(other.getComment().orNull(), otherStart,
									start);
							TimeTrackingItem second = new TimeTrackingItem(other.getComment().orNull(), end);

							this.dbStorage.deleteItemInDB(other);
							this.dbStorage.insertItemInDB(first);
							this.dbStorage.insertItemInDB(second);
						} else { // other.getEnd().isPresent()

							DateTime otherEnd = other.getEnd().get();

							if (end.isBefore(other.getEnd().get())) {

								// |----------| other
								//   |-----| item

								// Split old item
								TimeTrackingItem first = new TimeTrackingItem(other.getComment().orNull(),
										otherStart, start);
								TimeTrackingItem second = new TimeTrackingItem(other.getComment().orNull(), end,
										otherEnd);
								this.dbStorage.deleteItemInDB(other);
								this.dbStorage.insertItemInDB(first);
								this.dbStorage.insertItemInDB(second);
							} else {

								// |----|
								//   |-----| item
								TimeTrackingItem newItem = new TimeTrackingItem(other.getComment().orNull(),
										otherStart, start);
								replace(other, newItem);
							}
						}
					} else { // !itemEnd.isPresent()

						// |----|
						//   |----- item
						TimeTrackingItem newItem = new TimeTrackingItem(other.getComment().orNull(), otherStart,
								start);
						replace(other, newItem);
					}
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

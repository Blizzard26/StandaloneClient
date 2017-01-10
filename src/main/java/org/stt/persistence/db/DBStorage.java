package org.stt.persistence.db;

import java.sql.SQLException;
import java.util.List;

import org.joda.time.DateTime;
import org.stt.model.TimeTrackingItem;
import org.stt.query.TimeTrackingItemQueries;

import com.google.common.base.Optional;

public interface DBStorage extends TimeTrackingItemQueries {

	List<TimeTrackingItem> getItemsInRange(Optional<DateTime> start, Optional<DateTime> end) throws SQLException;

	List<TimeTrackingItem> getAllItems() throws SQLException;

	void insertItemInDB(TimeTrackingItem item) throws SQLException;

	void deleteItemInDB(TimeTrackingItem item) throws SQLException;
	
	void startTransaction();
	
	void endTransaction() ;

	void rollback() ;
	

}
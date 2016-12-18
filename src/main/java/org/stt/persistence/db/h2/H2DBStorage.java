package org.stt.persistence.db.h2;

import static org.jooq.impl.DSL.field;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.joda.time.DateTime;
import org.jooq.Condition;
import org.jooq.ConnectionProvider;
import org.jooq.Converter;
import org.jooq.DDLQuery;
import org.jooq.DSLContext;
import org.jooq.DataType;
import org.jooq.Delete;
import org.jooq.Field;
import org.jooq.InsertValuesStep4;
import org.jooq.Record;
import org.jooq.Record4;
import org.jooq.RecordMapper;
import org.jooq.ResultQuery;
import org.jooq.SQLDialect;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.stt.model.TimeTrackingItem;

import com.google.common.base.Optional;
import com.google.inject.Inject;

public class H2DBStorage {
	
	public static final class TimeTrackingItemMapper
			implements RecordMapper<Record4<DateTime, DateTime, String, Boolean>, TimeTrackingItem> {
		@Override
		public TimeTrackingItem map(Record4<DateTime, DateTime, String, Boolean> record) {
			DateTime start = record.get(COLUMN_START);
			
			DateTime end = record.get(COLUMN_END);
			
			String comment = record.get(COLUMN_COMMENT);
			
			//Boolean logged = record.get(COLUMN_LOGGED);
			
			return new TimeTrackingItem(comment, start, Optional.fromNullable(end));
		}
	}

	public static final class DateTimeConverter implements Converter<Timestamp, DateTime> {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		@Override
		public DateTime from(Timestamp databaseObject) {
			return databaseObject == null ? null : new DateTime(databaseObject.getTime());
		}

		@Override
		public Timestamp to(DateTime userObject) {
			return userObject == null ? null : new Timestamp(userObject.getMillis());
		}

		@Override
		public Class<Timestamp> fromType() {
			return Timestamp.class;
		}

		@Override
		public Class<DateTime> toType() {
			return DateTime.class;
		}
	}


	protected static final Logger LOG = Logger.getLogger(H2DBStorage.class.getName());
	
	private static final String ITEMS_TABLE_NAME = "TimeTrackingItems";
	private static final String COLUMN_NAME_START = "START_TIME";
	private static final String COLUMN_NAME_END = "END_TIME";
	private static final String COLUMN_NAME_COMMENT = "COMMENT";
	private static final String COLUMN_NAME_LOGGED = "LOGGED";

	public static final DataType<DateTime> DATE_TIME = SQLDataType.TIMESTAMP.asConvertedDataType(new DateTimeConverter());
	
	public static final Table<Record> ITEMS_TABLE = DSL.table(ITEMS_TABLE_NAME);
	public static final Field<DateTime> COLUMN_START = DSL.field(COLUMN_NAME_START, DATE_TIME);
	public static final Field<DateTime> COLUMN_END = DSL.field(COLUMN_NAME_END, DATE_TIME);
	public static final Field<String> COLUMN_COMMENT = DSL.field(COLUMN_NAME_COMMENT, SQLDataType.VARCHAR);
	public static final Field<Boolean> COLUMN_LOGGED = DSL.field(COLUMN_NAME_LOGGED, SQLDataType.BOOLEAN);
	
	private ConnectionProvider connectionProvider;
	private boolean dbInitialized = false;

	@Inject public H2DBStorage(ConnectionProvider connectionProvider) throws SQLException
	{
		this.connectionProvider = connectionProvider;	
		init();
	}
	
	public void init() throws SQLException {
		if (!dbInitialized) {
			try (DSLContext context = DSL.using(connectionProvider, SQLDialect.H2))
			{
				DDLQuery createTableStmt = context.createTableIfNotExists(ITEMS_TABLE).columns(COLUMN_START, COLUMN_END, COLUMN_COMMENT, COLUMN_LOGGED);
				
				createTableStmt.execute();
				dbInitialized = true;
			}
		}
	}
	
	public List<TimeTrackingItem> getTimeTrackingItemsInRange(DateTime start, Optional<DateTime> end) throws SQLException
	{
		try (DSLContext context = DSL.using(connectionProvider, SQLDialect.H2))
		{

			// |---|---|---|---
			//   |-----------| (start, end)
			
			// |---|---|---|---
			//   |------------- (start, end)
	
			
			Condition c = field(COLUMN_NAME_END).ge(start).or(field(COLUMN_NAME_END).isNull());
			if (end.isPresent())
			{
				c = c.and(field(COLUMN_NAME_START).le(end.get()));
			}
			
			ResultQuery<Record4<DateTime, DateTime, String, Boolean>> sql = context
					.select(COLUMN_START, 
							COLUMN_END, 
							COLUMN_COMMENT,
							COLUMN_LOGGED)
					.from(ITEMS_TABLE).
					where(c).
					orderBy(field(COLUMN_NAME_START).asc());
			
			
			LOG.log(Level.FINEST, "Executing SQL: "+sql.getSQL());
			
	
			List<TimeTrackingItem> items = sql.fetch(new TimeTrackingItemMapper());
			return items;
		}
	}

	
	
	public List<TimeTrackingItem> getAllItems() throws SQLException
	{
		try (DSLContext context = DSL.using(connectionProvider, SQLDialect.H2))
		{
			ResultQuery<Record4<DateTime, DateTime, String, Boolean>> sql = context
					.select(COLUMN_START, 
							COLUMN_END, 
							COLUMN_COMMENT,
							COLUMN_LOGGED)
					.from(ITEMS_TABLE).
					orderBy(field(COLUMN_NAME_START).asc());
			
			
			LOG.log(Level.FINEST, "Executing SQL: "+sql.getSQL());
			
		
			List<TimeTrackingItem> items = sql.fetch(new TimeTrackingItemMapper());
			return items;
		}
	}
	
	public void insertItemInDB(TimeTrackingItem item) throws SQLException {
		try (DSLContext context = DSL.using(connectionProvider, SQLDialect.H2))
		{
			InsertValuesStep4<Record,DateTime,DateTime,String,Boolean> insertStmt = context.insertInto(ITEMS_TABLE, COLUMN_START, COLUMN_END, COLUMN_COMMENT, COLUMN_LOGGED).values(item.getStart(), item.getEnd().orNull(), item.getComment().orNull(), false);
			
			insertStmt.execute();
		}
	}
	

	public void deleteItemInDB(TimeTrackingItem item) throws SQLException {
		try (DSLContext context = DSL.using(connectionProvider, SQLDialect.H2))
		{
			Delete<Record> deleteStmt = context.deleteFrom(ITEMS_TABLE).where(
					COLUMN_START.eq(item.getStart()).and(
							item.getEnd().isPresent() ? COLUMN_END.eq(item.getEnd().get()) : COLUMN_END.isNull())
					.and(item.getComment().isPresent() ? COLUMN_COMMENT.eq(item.getComment().get()) : COLUMN_COMMENT.isNull()));
			
			deleteStmt.execute();
		}
	}

}

package org.stt.persistence.db.h2;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.RuntimeErrorException;

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
import org.jooq.SelectLimitStep;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.stt.model.TimeTrackingItem;
import org.stt.persistence.db.DBStorage;
import org.stt.query.DNFClause;
import org.stt.query.DNFClauseMatcher;

import com.google.common.base.Optional;
import com.google.inject.Inject;

public class H2DBStorage implements DBStorage {
	
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
			return databaseObject == null ? null : new DateTime(roundToSecond(databaseObject.getTime()));
		}

		@Override
		public Timestamp to(DateTime userObject) {
			return userObject == null ? null : new Timestamp(roundToSecond(userObject.getMillis()));
		}

		private long roundToSecond(long millis) {
			return (millis / 1000) * 1000;
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

	private Connection txConnection;

	@Inject public H2DBStorage(ConnectionProvider connectionProvider) throws SQLException
	{
		this.connectionProvider = connectionProvider;	
		init();
	}
	

	private DSLContext getDSLContext() {
		return DSL.using(connectionProvider, SQLDialect.H2);
	}
	
	public void init() {
		if (!dbInitialized) {
			try (DSLContext context = getDSLContext())
			{
				DDLQuery createTableStmt = context.createTableIfNotExists(ITEMS_TABLE).columns(COLUMN_START, COLUMN_END, COLUMN_COMMENT, COLUMN_LOGGED);
				
				createTableStmt.execute();
				dbInitialized = true;
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see org.stt.persistence.db.h2.DBStorage#getTimeTrackingItemsInRange(org.joda.time.DateTime, com.google.common.base.Optional)
	 */
	@Override
	public List<TimeTrackingItem> getItemsInRange(Optional<DateTime> start, 
			Optional<DateTime> end) 
	{
		// |---|---|---|---
		//   |-----------| (start, end)
		
		// |---|---|---|---
		//   |------------- (start, end)

		Condition c = DSL.trueCondition();
		if (start.isPresent())
		{
			c = c.and(COLUMN_END.ge(start.get()).or(COLUMN_END.isNull()));
		}
		if (end.isPresent())
		{
			c = c.and(COLUMN_START.le(end.get()));
		}
		
		return getItemsByCondition(c);
	}

	private List<TimeTrackingItem> getItemsByCondition(Condition c) {

		ResultQuery<Record4<DateTime, DateTime, String, Boolean>> sql = DSL
				.select(COLUMN_START, 
						COLUMN_END, 
						COLUMN_COMMENT,
						COLUMN_LOGGED)
				.from(ITEMS_TABLE)
				.where(c)
				.orderBy(COLUMN_START.asc());
		
		
		LOG.log(Level.FINEST, "Executing SQL: "+sql.getSQL());
		
		try (DSLContext context = getDSLContext())
		{				
			List<TimeTrackingItem> items = context.fetch(sql).map(new TimeTrackingItemMapper());
			return items;
		}
	}

	
	
	/* (non-Javadoc)
	 * @see org.stt.persistence.db.h2.DBStorage#getAllItems()
	 */
	@Override
	public List<TimeTrackingItem> getAllItems()
	{
		ResultQuery<Record4<DateTime, DateTime, String, Boolean>> sql = DSL
				.select(COLUMN_START, 
						COLUMN_END, 
						COLUMN_COMMENT,
						COLUMN_LOGGED)
				.from(ITEMS_TABLE)
				.orderBy(COLUMN_START.asc());
		
		
		LOG.log(Level.FINEST, "Executing SQL: "+sql.getSQL());
		
		try (DSLContext context = getDSLContext())
		{				
			List<TimeTrackingItem> items = context.fetch(sql).map(new TimeTrackingItemMapper());
			return items;
		}
	}
	
	/* (non-Javadoc)
	 * @see org.stt.persistence.db.h2.DBStorage#insertItemInDB(org.stt.model.TimeTrackingItem)
	 */
	@Override
	public void insertItemInDB(TimeTrackingItem item) throws SQLException {
		try (DSLContext context = getDSLContext())
		{
			InsertValuesStep4<Record,DateTime,DateTime,String,Boolean> insertStmt = context.insertInto(ITEMS_TABLE, COLUMN_START, COLUMN_END, COLUMN_COMMENT, COLUMN_LOGGED).values(item.getStart(), item.getEnd().orNull(), item.getComment().orNull(), false);
			
			insertStmt.execute();
		}
	}
	

	/* (non-Javadoc)
	 * @see org.stt.persistence.db.h2.DBStorage#deleteItemInDB(org.stt.model.TimeTrackingItem)
	 */
	@Override
	public void deleteItemInDB(TimeTrackingItem item) throws SQLException {
		try (DSLContext context = getDSLContext())
		{
			Condition condition = COLUMN_START.eq(item.getStart())
					.and(item.getEnd().isPresent() ? COLUMN_END.eq(item.getEnd().get()) : COLUMN_END.isNull())
					.and(item.getComment().isPresent() ? COLUMN_COMMENT.eq(item.getComment().get()) : COLUMN_COMMENT.isNull());
			
			Delete<Record> deleteStmt = context.deleteFrom(ITEMS_TABLE).where(
					condition);
			
			deleteStmt.execute();
		}
	}

	@Override
	public Optional<TimeTrackingItem> getCurrentTimeTrackingitem() {
		Optional<TimeTrackingItem> latestTimeTrackingitem = getLatestTimeTrackingitem();
		
		return latestTimeTrackingitem.isPresent() && !latestTimeTrackingitem.get().getEnd().isPresent() ? 
				latestTimeTrackingitem : Optional.<TimeTrackingItem>absent();
	}

	@Override
	public Optional<TimeTrackingItem> getLatestTimeTrackingitem() {
		ResultQuery<Record4<DateTime, DateTime, String, Boolean>> sql = DSL
				.select(COLUMN_START, 
						COLUMN_END, 
						COLUMN_COMMENT,
						COLUMN_LOGGED)
				.from(ITEMS_TABLE)
				.orderBy(COLUMN_START.desc()).limit(1);
		
		
		LOG.log(Level.FINEST, "Executing SQL: "+sql.getSQL());
		
		try (DSLContext context = getDSLContext())
		{			
			List<TimeTrackingItem> items = context.fetch(sql).map(new TimeTrackingItemMapper());
			return items.size() > 0 ? Optional.of(items.get(0)) : Optional.absent();
		}
	}


	@Override
	public Optional<TimeTrackingItem> getPreviousTimeTrackingItem(TimeTrackingItem item) {
		
		ResultQuery<Record4<DateTime, DateTime, String, Boolean>> sql = DSL
				.select(COLUMN_START, 
						COLUMN_END, 
						COLUMN_COMMENT,
						COLUMN_LOGGED)
				.from(ITEMS_TABLE)
				.where(COLUMN_START.lessThan(item.getStart()))
				.orderBy(COLUMN_START.desc()).limit(1);
		
		LOG.log(Level.FINEST, "Executing SQL: "+sql.getSQL());
		
		try (DSLContext context = getDSLContext())
		{			
			List<TimeTrackingItem> items = context.fetch(sql).map(new TimeTrackingItemMapper());
			return items.size() > 0 ? Optional.of(items.get(0)) : Optional.absent();
		}
	}

	@Override
	public Optional<TimeTrackingItem> getNextTimeTrackingTime(TimeTrackingItem item) {
		ResultQuery<Record4<DateTime, DateTime, String, Boolean>> sql = DSL
				.select(COLUMN_START, 
						COLUMN_END, 
						COLUMN_COMMENT,
						COLUMN_LOGGED)
				.from(ITEMS_TABLE)
				.where(COLUMN_START.greaterThan(item.getStart()))
				.orderBy(COLUMN_START.asc()).limit(1);
		
		LOG.log(Level.FINEST, "Executing SQL: "+sql.getSQL());
		
		try (DSLContext context = getDSLContext())
		{				
	
			List<TimeTrackingItem> items = context.fetch(sql).map(new TimeTrackingItemMapper());
			return items.size() > 0 ? Optional.of(items.get(0)) : Optional.absent();
		}
	}

	@Override
	public Collection<DateTime> getAllTrackedDays() {
		Collection<DateTime> result = new ArrayList<>();

		DateTime lastDay = null;
		for (TimeTrackingItem item : getAllItems()) {
			DateTime currentDay = item.getStart()
					.withTimeAtStartOfDay();
			if (lastDay == null || !lastDay.equals(currentDay)) {
				result.add(currentDay);
				lastDay = currentDay;
			}
		}
		return result;
	}

	@Override
	public Collection<TimeTrackingItem> queryFirstNItems(Optional<DateTime> start, Optional<DateTime> end,
			Optional<Integer> maxItems) {
		Condition c = DSL.trueCondition();
		if (start.isPresent())
		{
			c = c.and(COLUMN_START.greaterOrEqual(start.get()));
		}
		if (end.isPresent())
		{
			c = c.and(COLUMN_END.lessOrEqual(end.get()));
		}		
		
		ResultQuery<Record4<DateTime, DateTime, String, Boolean>> sql = DSL
				.select(COLUMN_START, 
						COLUMN_END, 
						COLUMN_COMMENT,
						COLUMN_LOGGED)
				.from(ITEMS_TABLE)
				.where(c)
				.orderBy(COLUMN_START.asc());
		
		if (maxItems.isPresent())
		{
			sql = ((SelectLimitStep<Record4<DateTime, DateTime, String, Boolean>>)sql).limit(maxItems.get());
		}
		
		LOG.log(Level.FINEST, "Executing SQL: "+sql.getSQL());
		
		try (DSLContext context = getDSLContext())
		{			
			List<TimeTrackingItem> items = context.fetch(sql).map(new TimeTrackingItemMapper());
			return items;
		}
	}

	@Override
	public Collection<TimeTrackingItem> queryItems(DNFClause dnfClause) {
        Collection<TimeTrackingItem> result = new ArrayList<>();
		DNFClauseMatcher DNFClauseMatcher = new DNFClauseMatcher(dnfClause);
	    for (TimeTrackingItem item : getAllItems()) {
			if (DNFClauseMatcher.matches(item)) {
				result.add(item);
			}
	    }
        return result;
	}

	@Override
	public Collection<TimeTrackingItem> queryAllItems() {
		return getAllItems();
	}


	@Override
	public void startTransaction() {
		if (txConnection == null)
		{
			txConnection = connectionProvider.acquire();
			try {
				txConnection.setAutoCommit(false);
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}
		}
	}


	@Override
	public void endTransaction()  {
		if (txConnection != null)
		{
			try {
				txConnection.commit();
				txConnection.setAutoCommit(true);
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}
			connectionProvider.release(txConnection);
		}
		txConnection = null;
	}


	@Override
	public void rollback()   {
		if (txConnection != null)
			try {
				txConnection.rollback();
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}
	}

}

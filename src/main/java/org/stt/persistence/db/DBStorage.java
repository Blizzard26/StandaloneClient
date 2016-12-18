package org.stt.persistence.db;

import static org.stt.persistence.db.DBUtil.transform;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.joda.time.DateTime;
import org.stt.model.TimeTrackingItem;

import com.google.common.base.Optional;
import com.google.inject.Inject;

public class DBStorage {
	
	protected static final Logger LOG = Logger.getLogger(DBStorage.class.getName());
	
	public static final int INDEX_LOGGED = 4;
	public static final int INDEX_COMMENT = 3;
	public static final int INDEX_END = 2;
	public static final int INDEX_START = 1;

	public static final String ITEMS_TABLE_NAME = "TimeTrackingItems";

	public static final String COLUMN_NAME_START = "START_TIME";
	public static final String COLUMN_NAME_END = "END_TIME";
	public static final String COLUMN_NAME_COMMENT = "COMMENT";
	public static final String COLUMN_NAME_LOGGED = "LOGGED";

	public static final String CREATE_STATEMENT = "CREATE TABLE IF NOT EXISTS " + ITEMS_TABLE_NAME + " " + " ("
			+ COLUMN_NAME_START + " TIMESTAMP NOT NULL, " 
			+ COLUMN_NAME_END + " TIMESTAMP, " 
			+ COLUMN_NAME_COMMENT + " VARCHAR(255), " 
			+ COLUMN_NAME_LOGGED + " INT);";

	public static final String INSERT_STATEMENT = "INSERT INTO " + ITEMS_TABLE_NAME + " " + "(" + COLUMN_NAME_START
			+ ", " + COLUMN_NAME_END + ", " + COLUMN_NAME_COMMENT + ", " + COLUMN_NAME_LOGGED + ") "
			+ "VALUES (?, ?, ?, ?)";

	public static final String SELECT_QUERY = "SELECT " + COLUMN_NAME_START + ", " + COLUMN_NAME_END + ", "
			+ COLUMN_NAME_COMMENT + ", " + COLUMN_NAME_LOGGED + " " + "FROM " + ITEMS_TABLE_NAME + " "
			+ "ORDER BY " + COLUMN_NAME_START + " ASC";
	
	private DBConnectionProvider connectionProvider;
	private boolean dbInitialized = false;

	@Inject public DBStorage(DBConnectionProvider connectionProvider) throws SQLException
	{
		this.connectionProvider = connectionProvider;	
		init();
	}
	
	public void init() throws SQLException {
		if (!dbInitialized) {

			Connection connection = connectionProvider.getConnection();
			try {
				try (Statement statement = connection.createStatement())
				{

					statement.execute(CREATE_STATEMENT);
					dbInitialized = true;
				}
			} finally {
				connectionProvider.releaseConnection(connection);
			}
		}

	}

	public List<TimeTrackingItem> getTimeTrackingItemsInRange(DateTime start, Optional<DateTime> end) throws SQLException {
		PreparedStatementBuilder sql = new PreparedStatementBuilder();
		sql.append("SELECT ");
		sql.append(COLUMN_NAME_START).append(", ");
		sql.append(COLUMN_NAME_END).append(", ");
		sql.append(COLUMN_NAME_COMMENT).append(", ");
		sql.append(COLUMN_NAME_LOGGED);
		sql.append(" FROM ").append(ITEMS_TABLE_NAME);
		sql.append(" WHERE (");

		// Start between start and end
		
		// |---|---|---|---
		//   |-----------| (start, end)
		
		// |---|---|---|---
		//   |------------- (start, end)

		sql.append("((").append(COLUMN_NAME_END).append(" > ").addParameter(start).append(")");
		sql.append(" OR ").append(COLUMN_NAME_END).append(" IS NULL) ");

		if (end.isPresent()) {
			sql.append(" AND (").append(COLUMN_NAME_START).append(" < ").addParameter(end.get()).append(")");
		}

		sql.append(" ) ");
		sql.append(" ORDER BY ").append(COLUMN_NAME_START).append(" ASC ");
		
		LOG.log(Level.FINEST, sql.toString());

		Connection connection = connectionProvider.getConnection();
		try 
		{
			try (PreparedStatement prepareStatement = sql.prepareStatement(connection)) {
				try (ResultSet resultSet = prepareStatement.executeQuery())
				{
					return readTimeTrackingItems(resultSet);
				}
				
			}
		}
		finally 
		{
			connectionProvider.releaseConnection(connection);
		}
	}

	
	public List<TimeTrackingItem> getAllItems() throws SQLException
	{
		Connection connection = connectionProvider.getConnection();
		try 
		{
		
			try (Statement statement = connection.createStatement())
			{
				try (ResultSet resultSet = statement.executeQuery(SELECT_QUERY))
				{
					return readTimeTrackingItems(resultSet);
				}
			}
		} 
		finally
		{
			connectionProvider.releaseConnection(connection);
		}
	}
	
	protected List<TimeTrackingItem> readTimeTrackingItems(ResultSet resultSet) throws SQLException {
		List<TimeTrackingItem> items = new ArrayList<TimeTrackingItem>();
		while (resultSet.next())
		{
			TimeTrackingItem timeTrackingItem = readTimeTrackingItem(resultSet);
			
			
			items.add(timeTrackingItem);
		}
		
		return items;
	}

	protected TimeTrackingItem readTimeTrackingItem(ResultSet resultSet) throws SQLException {
		DateTime start;
		DateTime end = null;
		String comment;
		boolean logged;
		
		Timestamp startTimestamp = resultSet.getTimestamp(INDEX_START);
		
		start = transform(startTimestamp);
		
		Timestamp endTimestamp = resultSet.getTimestamp(INDEX_END);
		if (!resultSet.wasNull())
		{
			end = transform(endTimestamp);
		}
		
		
		comment = resultSet.getString(INDEX_COMMENT);
		logged = resultSet.getBoolean(INDEX_LOGGED);
		
		TimeTrackingItem timeTrackingItem;
		if (end != null)
		{
			timeTrackingItem = new TimeTrackingItem(comment, start, end);
		}
		else
		{
			timeTrackingItem = new TimeTrackingItem(comment, start);
		}
		return timeTrackingItem;
	}



	public void insertItemInDB(TimeTrackingItem item) throws SQLException {
		//System.out.println("Insert: "+item);
		Connection connection = connectionProvider.getConnection();
		try {

			try (PreparedStatement prepareStatement = connection.prepareStatement(INSERT_STATEMENT)) {
				prepareStatement.setTimestamp(INDEX_START, transform(item.getStart()));
				if (item.getEnd().isPresent()) {
					prepareStatement.setTimestamp(INDEX_END, transform(item.getEnd().get()));
				} else {
					prepareStatement.setNull(INDEX_END, java.sql.Types.INTEGER);
				}

				prepareStatement.setString(INDEX_COMMENT, item.getComment().orNull());
				prepareStatement.setBoolean(INDEX_LOGGED, false);

				prepareStatement.execute();
			}
			connection.commit();

		}
		finally
		{
			connectionProvider.releaseConnection(connection);
		}
	}
	

	public void deleteItemInDB(TimeTrackingItem item) throws SQLException {
		//System.out.println("Delete: "+item);
		Connection connection = connectionProvider.getConnection();
		try {
			PreparedStatementBuilder sql = new PreparedStatementBuilder();
			sql.append("DELETE FROM ");
			sql.append(ITEMS_TABLE_NAME);
			sql.append(" WHERE ");
			sql.append(COLUMN_NAME_START).append("=").addParameter(item.getStart());
			sql.append(" AND ");
			sql.append(COLUMN_NAME_END);
			if (item.getEnd().isPresent()) {
				sql.append("=").addParameter(item.getEnd().get());
			} else {
				sql.append(" is NULL");
			}
			sql.append(" AND ");
			sql.append(COLUMN_NAME_COMMENT);
			if (item.getComment().isPresent()) {
				sql.append("='").append(item.getComment().get()).append("' ");
			} else {
				sql.append(" is NULL");
			}
			
			LOG.log(Level.FINEST, sql.toString());
			
			try (PreparedStatement stmt = sql.prepareStatement(connection)) {
				stmt.execute();
			}
			connection.commit();
		} 
		finally
		{
			connectionProvider.releaseConnection(connection);
		}
	}

}

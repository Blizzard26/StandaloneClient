package org.stt.persistence.h2;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.joda.time.DateTime;
import org.stt.model.TimeTrackingItem;
import org.stt.persistence.ItemReader;

import com.google.common.base.Optional;
import com.google.inject.Inject;

import static org.stt.persistence.h2.H2Constants.*;

public class H2ItemReader implements ItemReader {

	 private static final Logger LOG = Logger.getLogger(ItemReader.class
	            .getName());
	 
	 
	
	private H2ConnectionProvider connectionProvider;
	private Connection connection;
	
	private ResultSet resultSet;

	@Inject
	public H2ItemReader(@H2DBConnection H2ConnectionProvider connectionProvider) throws ClassNotFoundException, SQLException {
		this.connectionProvider = checkNotNull(connectionProvider);
		this.connection = this.connectionProvider.get();
		
		Statement statement = connection.createStatement();
		resultSet = statement.executeQuery(SELECT_QUERY);
	}

	@Override
	public void close() throws IOException {
		try {
			connection.close();
		} catch (SQLException e) {
			throw new IOException(e);
		}
	}

	@Override
	public Optional<TimeTrackingItem> read() {
		
		try {
			if (resultSet.next())
			{
				DateTime start;
				DateTime end = null;
				String comment;
				boolean logged;
				
				long startLong = resultSet.getLong(INDEX_START);
				
				start = new DateTime(startLong);
				
				long endLong = resultSet.getLong(INDEX_END);
				if (!resultSet.wasNull())
				{
					end = new DateTime(endLong);
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
				
				return Optional.of(timeTrackingItem);
			}
		} catch (SQLException e) {
			LOG.log(Level.SEVERE, "SQL Exception readeing result set", e);
		}
		
		return Optional.<TimeTrackingItem>absent();
	}

}

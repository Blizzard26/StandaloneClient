package org.stt.persistence.h2;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.stt.persistence.h2.H2Constants.*;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.stt.model.TimeTrackingItem;
import org.stt.persistence.ItemWriter;

import com.google.inject.Inject;

public class H2ItemWriter implements ItemWriter {
	
	

	private H2ConnectionProvider connectionProvider;
	private Connection connection;

	@Inject
	public H2ItemWriter(@H2DBConnection H2ConnectionProvider connectionProvider) throws ClassNotFoundException, SQLException {
		this.connectionProvider = checkNotNull(connectionProvider);
		this.connection = this.connectionProvider.get();
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
	public void write(TimeTrackingItem item) throws IOException {
		try {
						
			PreparedStatement prepareStatement = connection.prepareStatement(H2Constants.INSERT_STATEMENT);
			
			prepareStatement.setLong(INDEX_START, item.getStart().getMillis());
			if (item.getEnd().isPresent())
			{
				prepareStatement.setLong(INDEX_END, item.getEnd().get().getMillis());
			}
			else
			{
				prepareStatement.setNull(INDEX_END, java.sql.Types.INTEGER);
			}
			
			prepareStatement.setString(INDEX_COMMENT, item.getComment().orNull());
			prepareStatement.setBoolean(INDEX_LOGGED, false);
			
			prepareStatement.execute();
			
			prepareStatement.close();
			
		} catch (SQLException e) {
			throw new IOException(e);
		}
	}
	

}

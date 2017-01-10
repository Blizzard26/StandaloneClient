package org.stt.persistence.db.h2;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.h2.api.ErrorCode;
import org.h2.mvstore.FileStore;
import org.jooq.ConnectionProvider;
import org.jooq.exception.DataAccessException;

public class H2ConnectionProvider implements ConnectionProvider {

	
	private static final Logger LOG = Logger.getLogger(H2ConnectionProvider.class.getName());
	
	private final H2Configuration configuration;
	private Connection connection;
	private int openConnectionCount;
	
	public H2ConnectionProvider(H2Configuration config) throws ClassNotFoundException
	{
		configuration = config;
		Class.forName("org.h2.Driver");
	}

	public synchronized Connection acquire() throws DataAccessException {
		boolean connectionValid;
		try {
			connectionValid = connection != null && connection.isValid(5);
		} catch (SQLException e) {
			LOG.log(Level.WARNING, "Exception determining connection state", e);
			connectionValid = false;
		}

		if (!connectionValid) {
			if (connection != null) {
				try {
					connection.close();
				} catch (SQLException e) {
					LOG.log(Level.WARNING, "Unable to close invalid connection", e);
				}
				connection = null;
			}

			int i = 0;
			
			while (connection == null)
			{
				try {
					connection = DriverManager.getConnection("jdbc:h2:" + configuration.getDatabase(),
							configuration.getUserName(), configuration.getPassword());
				} catch (SQLException e) {
					if (e.getErrorCode() == ErrorCode.DATABASE_ALREADY_OPEN_1 && i++ < 5)
					{
						LOG.warning("Connection already open. " + i + " try. Retrying in 1 second.");
						try {
							this.wait(1000);
						} catch (InterruptedException e1) {
							break;
						}
					}
					else
					{
						throw new DataAccessException("SQL Exception opening new connection", e);
					}
				}
			}

			openConnectionCount = 0;
		}

		openConnectionCount++;

		return connection;

	}
	
	public synchronized void release(Connection connection) throws DataAccessException
	{
		try {
			if (this.connection != connection)
				return;
			
			--openConnectionCount;
			if (openConnectionCount <= 0 && connection != null)
			{
				
				connection.close();
				
				connection = null;
				openConnectionCount = 0;
			}
		} catch (SQLException e) {
			throw new DataAccessException("SQL Exception closing connection", e);
		}
	}

	public int getOpenConnectionCount() {
		return openConnectionCount;
	}

}

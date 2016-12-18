package org.stt.persistence.db.h2;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.jooq.ConnectionProvider;
import org.jooq.exception.DataAccessException;

public class H2ConnectionProvider implements ConnectionProvider {

	
	private final H2Configuration configuration;
	private Connection connection;
	private int openConnectionCount;
	
	public H2ConnectionProvider(H2Configuration config) throws ClassNotFoundException
	{
		configuration = config;
		Class.forName("org.h2.Driver");
	}

	public synchronized Connection acquire() throws DataAccessException {
		try {
			if (connection == null || connection.isClosed()) 
			{
				
			    connection = DriverManager.getConnection("jdbc:h2:"+configuration.getDatabase(), 
			    		configuration.getUserName(), 
			    		configuration.getPassword() );
			    
			   
			    openConnectionCount = 0;
			}
			
			openConnectionCount++;

	        return connection;
		} catch (SQLException e) {
			throw new DataAccessException("SQL Exception opening new connection", e);
		}

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

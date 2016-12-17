package org.stt.persistence.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class H2ConnectionProvider  {

	
	private final H2Configuration configuration;
	private Connection connection;
	private int openConnectionCount;
	
	public H2ConnectionProvider(H2Configuration config) throws ClassNotFoundException
	{
		configuration = config;
		Class.forName("org.h2.Driver");
	}

	public synchronized Connection getConnection() throws SQLException {
		if (connection == null || connection.isClosed()) 
		{
			
	        connection = DriverManager.getConnection("jdbc:h2:"+configuration.getDatabase(), 
	        		configuration.getUserName(), 
	        		configuration.getPassword() );
	        
	       
	        openConnectionCount = 0;
		}
		
		openConnectionCount++;

        return connection;
	}
	
	public synchronized void releaseConnection(Connection connection) throws SQLException
	{
		if (this.connection != connection)
			return;
		
		--openConnectionCount;
		if (openConnectionCount <= 0 && connection != null)
		{
			connection.close();
			connection = null;
			openConnectionCount = 0;
		}
	}

	public int getConnectionCount() {
		return openConnectionCount;
	}

}

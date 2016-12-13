package org.stt.persistence.h2;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import com.google.inject.Inject;

public class H2ConnectionProvider  {

	
	private boolean dbInitialized = false;
	private final H2Configuration configuration;
	
	public H2ConnectionProvider(H2Configuration config)
	{
		configuration = config;
		
	}

	public Connection get() throws ClassNotFoundException, SQLException {
		Class.forName("org.h2.Driver");
        Connection con = DriverManager.getConnection("jdbc:h2:"+configuration.getDatabase(), 
        		configuration.getUserName(), 
        		configuration.getPassword() );
        
        if (!dbInitialized)
        {
	        Statement statement = con.createStatement();
	        try {
				
				statement.execute(H2Constants.CREATE_STATEMENT);
				dbInitialized = true;
			} finally 
	        {
				statement.close();
	        }
        }
        

        return con;
	}

}

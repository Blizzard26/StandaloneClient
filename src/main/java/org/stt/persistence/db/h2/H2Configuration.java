package org.stt.persistence.db.h2;

import java.io.File;
import java.io.IOException;

import org.h2.engine.Constants;
import org.stt.Configuration;

public class H2Configuration {
	
	public static final String H2_DATABASE_EXTENSION = Constants.SUFFIX_MV_FILE;
	
	private Configuration mainConfiguration;

	public H2Configuration(Configuration configuration)
	{
		this.mainConfiguration = configuration;
	}

	public String getDatabase() {
        String databaseName = mainConfiguration.getPropertiesReplaced("h2_db", "$HOME$/stt");
        File databaseBaseFile = new File(databaseName);
        
        // File extension is automatically added by H2
        File databaseFile = new File(databaseBaseFile.getAbsolutePath() + H2_DATABASE_EXTENSION);
        
        if (!databaseFile.exists() || !databaseFile.isFile())
        {
        	try {
				databaseFile.createNewFile();
			} catch (IOException e) {
				throw new RuntimeException("Unable to create database file", e);
			}
        }
        
        
        return databaseBaseFile.getAbsolutePath();
	}

	public String getUserName() {
		return mainConfiguration.getPropertiesReplaced("h2_username", "h2");
	}

	public String getPassword() {
		return mainConfiguration.getPropertiesReplaced("h2_password", "password");
	}

	public Configuration getBaseConfiguration() {
		return mainConfiguration;
	}

}

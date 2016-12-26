package org.stt.persistence.db.h2;

import java.io.File;
import java.io.IOException;

import org.stt.Configuration;

public class H2Configuration {
	
	private Configuration mainConfiguration;

	public H2Configuration(Configuration configuration)
	{
		this.mainConfiguration = configuration;
		
	}

	public String getDatabase() {
        String sttFileString = mainConfiguration.getPropertiesReplaced("h2_db", "$HOME$/stt.h2.db");
        File sttFile = new File(sttFileString);
        try {
            sttFile.createNewFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return sttFile.getAbsolutePath();
	}

	public String getUserName() {
		return mainConfiguration.getPropertiesReplaced("h2_username", "h2");
	}

	public String getPassword() {
		return mainConfiguration.getPropertiesReplaced("h2_password", "password");
	}

}

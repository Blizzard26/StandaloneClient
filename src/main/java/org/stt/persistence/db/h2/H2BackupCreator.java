package org.stt.persistence.db.h2;

import java.io.File;

import org.stt.persistence.BackupCreator;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * creates backups of the database file in configurable intervals and locations.
 * Optionally deletes old backup files if configured.
 */
@Singleton
public class H2BackupCreator extends BackupCreator {

	
	
	private H2Configuration h2Configuration;

	@Inject
	public H2BackupCreator(H2Configuration configuration) {
		super(configuration);
		this.h2Configuration = configuration;
	}

	@Override
	protected File getFileToBackup() {
		String databaseFile = h2Configuration.getDatabase();
		return new File(databaseFile + H2Configuration.H2_DATABASE_EXTENSION);
	}
}

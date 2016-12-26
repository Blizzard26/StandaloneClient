package org.stt.persistence.stt;

import java.io.File;

import org.stt.Configuration;
import org.stt.persistence.BackupCreator;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * creates backups of the .stt file in configurable intervals and locations.
 * Optionally deletes old backup files if configured.
 */
@Singleton
public class STTBackupCreator extends BackupCreator {

	@Inject
	public STTBackupCreator(Configuration configuration) {
		super(configuration);
	}

	@Override
	protected File getFileToBackup() {
		File toBackupFile = configuration.getSttFile();
		return toBackupFile;
	}
}

package org.stt.model;

import java.nio.file.Path;

import static com.google.common.base.Preconditions.checkNotNull;

public class FileChanged {

	public final Path file;

	public FileChanged(Path file) {
		this.file = checkNotNull(file);
	}

}

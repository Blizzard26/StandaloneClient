package org.stt.persistence.db;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.sql.SQLException;

import org.stt.persistence.ItemPersister;
import org.stt.persistence.ItemReader;
import org.stt.persistence.ItemReaderProvider;
import org.stt.persistence.ItemWriter;
import org.stt.persistence.PreCachingItemReaderProvider;
import org.stt.persistence.stt.STTFile;
import org.stt.query.DefaultTimeTrackingItemQueries;
import org.stt.query.TimeTrackingItemQueries;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;

public class H2PersistenceModule extends AbstractModule {

	@Override
	protected void configure() {
        bind(ItemReader.class).to(DBItemReader.class);
        bind(ItemWriter.class).to(DBItemWriter.class);
        bind(ItemReaderProvider.class).to(PreCachingItemReaderProvider.class);
        bind(TimeTrackingItemQueries.class).to(DefaultTimeTrackingItemQueries.class);
        bind(ItemPersister.class).to(DBItemPersister.class);
	}

	@Provides
	public H2ConnectionProvider getDBConnection() throws SQLException, ClassNotFoundException
	{
		return new H2ConnectionProvider(null); // FIXME

	}

}

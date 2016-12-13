package org.stt.persistence.h2;

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
        bind(ItemReader.class).to(H2ItemReader.class);
        bind(ItemWriter.class).to(H2ItemWriter.class);
        bind(ItemReaderProvider.class).to(PreCachingItemReaderProvider.class);
        bind(TimeTrackingItemQueries.class).to(DefaultTimeTrackingItemQueries.class);
        bind(ItemPersister.class).to(H2ItemPersister.class);
	}

	@Provides @H2DBConnection
	public H2ConnectionProvider getDBConnection() throws SQLException, ClassNotFoundException
	{
		return new H2ConnectionProvider(null); // FIXME

	}

}

package org.stt.persistence.db.h2;

import java.io.File;
import java.sql.SQLException;

import org.jooq.ConnectionProvider;
import org.stt.Configuration;
import org.stt.persistence.DatabaseFile;
import org.stt.persistence.ItemPersister;
import org.stt.persistence.ItemReader;
import org.stt.persistence.ItemReaderProvider;
import org.stt.persistence.ItemWriter;
import org.stt.persistence.PreCachingItemReaderProvider;
import org.stt.persistence.db.DBItemPersister;
import org.stt.persistence.db.DBItemReader;
import org.stt.persistence.db.DBItemWriter;
import org.stt.persistence.db.DBStorage;
import org.stt.persistence.stt.STTFile;
import org.stt.query.TimeTrackingItemQueries;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

public class H2PersistenceModule extends AbstractModule {

	@Override
	protected void configure() {
        bind(ItemReader.class).to(DBItemReader.class);
        bind(ItemWriter.class).to(DBItemWriter.class);
        bind(ItemReaderProvider.class).to(PreCachingItemReaderProvider.class);
        bind(TimeTrackingItemQueries.class).to(DBStorage.class);
        bind(ItemPersister.class).to(DBItemPersister.class);
        bind(Configuration.class).to(H2Configuration.class);
	}

	@Provides
	@Inject public ConnectionProvider getConnectionProvider(H2Configuration config) throws SQLException, ClassNotFoundException
	{
		return new H2ConnectionProvider(config);

	}
	
	@Provides
	@Singleton
	@Inject public DBStorage getDBStorage(ConnectionProvider connectionProvider) throws SQLException
	{
		return new H2DBStorage(connectionProvider);
	}

    @Provides @Named("uncached")
    ItemReaderProvider directReaderProvider(final Provider<ItemReader> readerProvider) {
        return new ItemReaderProvider() {
            @Override
            public ItemReader provideReader() {
                return readerProvider.get();
            }
        };
    }
    

}

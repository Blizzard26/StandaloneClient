package org.stt.persistence.db;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import org.hamcrest.Matchers;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.stt.model.TimeTrackingItem;
import org.stt.persistence.db.h2.H2Configuration;
import org.stt.persistence.db.h2.H2ConnectionProvider;
import org.stt.persistence.db.h2.H2DBStorage;


public class DBItemWriterTest {

	private H2ConnectionProvider connectionProvider;
	private DBItemWriter sut;
	
	@Mock
	H2Configuration configuration;
	private DBStorage dbStorage;
	private Connection connection;
	
	@Before
	public void setUp() throws ClassNotFoundException, SQLException {
		MockitoAnnotations.initMocks(this);
		
		given(configuration.getDatabase()).willReturn("mem:H2ItemWriterTest");
		given(configuration.getUserName()).willReturn("test");
		given(configuration.getPassword()).willReturn("");
		
		
		this.connectionProvider = new H2ConnectionProvider(configuration);
		connection = connectionProvider.acquire();
		
		this.dbStorage = new H2DBStorage(connectionProvider);
		
		sut = new DBItemWriter(dbStorage);
	}
	
	@After
	public void tearDown() throws IOException, SQLException {
		sut.close();
		
		connectionProvider.release(connection);
		
		Assume.assumeThat(connectionProvider.getOpenConnectionCount(), is(0));
	}

	@Test
	public void shouldWriteItemToDB() throws IOException, ClassNotFoundException, SQLException {
		// GIVEN
		TimeTrackingItem item = new TimeTrackingItem("Test", new DateTime(7500000L));
		
		// WHEN
		sut.write(item );
		
		// THEN
		assertThat(dbStorage.getAllItems(), Matchers.contains(item));
	}
	
	

}

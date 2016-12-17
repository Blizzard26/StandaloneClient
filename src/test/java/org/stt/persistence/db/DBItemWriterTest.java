package org.stt.persistence.db;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.stt.model.TimeTrackingItem;


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
		connection = connectionProvider.getConnection();
		
		this.dbStorage = new DBStorage(connectionProvider);
		
		sut = new DBItemWriter(dbStorage);
	}
	
	@After
	public void tearDown() throws IOException, SQLException {
		sut.close();
		
		connectionProvider.releaseConnection(connection);
		
		Assume.assumeThat(connectionProvider.getConnectionCount(), is(0));
	}

	@Test
	public void shouldWriteItemToDB() throws IOException, ClassNotFoundException, SQLException {
		// GIVEN
		TimeTrackingItem item = new TimeTrackingItem("Test", new DateTime(7500000L));
		
		// WHEN
		sut.write(item );
		
		// THEN
		try (Statement statement = connection.createStatement()) {
			try (ResultSet resultSet = statement.executeQuery(DBStorage.SELECT_QUERY))
			{
				assertThat(resultSet.next(), is(true));
				assertThat(resultSet.getTimestamp(DBStorage.INDEX_START), is(new Timestamp(7500000L)));
				assertThat(resultSet.getTimestamp(DBStorage.INDEX_END), nullValue());
				assertThat(resultSet.wasNull(), is(true));
				assertThat(resultSet.getString(DBStorage.INDEX_COMMENT), is("Test"));
				
				assertThat(resultSet.next(), is(false));
			}
		}

	}
	
	

}

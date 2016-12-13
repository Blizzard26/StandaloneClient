package org.stt.persistence.h2;

import static org.mockito.BDDMockito.*;
import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.stt.model.TimeTrackingItem;


public class H2ItemWriterTest {

	private H2ConnectionProvider connectionProvider;
	private H2ItemWriter sut;
	
	@Mock
	H2Configuration configuration;
	
	@Before
	public void setUp() throws ClassNotFoundException, SQLException {
		MockitoAnnotations.initMocks(this);
		
		given(configuration.getDatabase()).willReturn("mem:H2ItemWriterTest");
		given(configuration.getUserName()).willReturn("test");
		given(configuration.getPassword()).willReturn("");
		
		
		this.connectionProvider = new H2ConnectionProvider(configuration);
		
		sut = new H2ItemWriter(connectionProvider);
	}
	
	@After
	public void tearDown() throws IOException {
		sut.close();
	}

	@Test
	public void test() throws IOException, ClassNotFoundException, SQLException {
		// GIVEN
		TimeTrackingItem item = new TimeTrackingItem("Test", new DateTime(7500000L));
		
		// WHEN
		sut.write(item );
		
		// THEN
		Connection connection = connectionProvider.get();
		
		try {
			Statement statement = connection.createStatement();
			try {
				ResultSet resultSet = statement.executeQuery(H2Constants.SELECT_QUERY);
				assertThat(resultSet.next(), is(true));
				assertThat(resultSet.getLong(H2Constants.INDEX_START), is(7500000L));
				assertThat(resultSet.getLong(H2Constants.INDEX_END), is(0L));
				assertThat(resultSet.wasNull(), is(true));
				assertThat(resultSet.getString(H2Constants.INDEX_COMMENT), is("Test"));
				
				assertThat(resultSet.next(), is(false));
				
			} finally {
				statement.close();
			}
		} finally {
			connection.close();
		}
	}

}

package org.stt.persistence.db;

import static org.hamcrest.CoreMatchers.is;
import static org.mockito.BDDMockito.given;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import org.hamcrest.Matchers;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.stt.model.TimeTrackingItem;
import org.stt.persistence.db.h2.H2Configuration;
import org.stt.persistence.db.h2.H2ConnectionProvider;
import org.stt.persistence.db.h2.H2DBStorage;

import com.google.common.base.Optional;

public class DBItemReaderTest {

	private H2ConnectionProvider connectionProvider;
	private DBItemReader sut;
	
	@Mock
	H2Configuration configuration;
	private H2DBStorage dbStorage;
	private Connection connection;
	
	@Before
	public void setUp() throws ClassNotFoundException, SQLException {
		MockitoAnnotations.initMocks(this);
		
		given(configuration.getDatabase()).willReturn("mem:H2ItemReaderTest");
		given(configuration.getUserName()).willReturn("test");
		given(configuration.getPassword()).willReturn("");
		
		
		this.connectionProvider = new H2ConnectionProvider(configuration);
		connection = connectionProvider.acquire();
		
		this.dbStorage = new H2DBStorage(connectionProvider);
		
		sut = new DBItemReader(dbStorage);
	}
	
	@After
	public void tearDown() throws IOException, SQLException {
		sut.close();
		
		connectionProvider.release(connection);
		
		Assume.assumeThat(connectionProvider.getOpenConnectionCount(), is(0));
	}
	
	@Test
	public void multiLineCommentGetsImportedCorrectly() throws ClassNotFoundException, SQLException {

		// GIVEN
		dbStorage.insertItemInDB(new TimeTrackingItem("this is\n a multiline\r string\r\n with different separators", new DateTime(2012,10,10,22,00,00), Optional.fromNullable(new DateTime(2012,11,10,22,00,01))));

		// WHEN
		Optional<TimeTrackingItem> readItem = sut.read();

		// THEN
		Assert.assertEquals(
				"this is\n a multiline\r string\r\n with different separators",
				readItem.get().getComment().get());
	}


	@Test
	public void onlyStartTimeGiven() throws ClassNotFoundException, SQLException {

		// GIVEN
		dbStorage.insertItemInDB(new TimeTrackingItem(null, new DateTime(2012,10,10,22,00,00), Optional.fromNullable(null)));

		// WHEN
		Optional<TimeTrackingItem> readItem = sut.read();

		// THEN
		DateTime time = new DateTime(2012, 10, 10, 22, 00, 00);
		Assert.assertThat(time, Matchers.equalTo(readItem.get().getStart()));
	}

	@Test
	public void startTimeAndCommentGiven() throws ClassNotFoundException, SQLException {

		// GIVEN
		dbStorage.insertItemInDB(new TimeTrackingItem("the long comment", new DateTime(2012,10,10,22,00,00), Optional.fromNullable(null)));

		// WHEN
		Optional<TimeTrackingItem> readItem = sut.read();

		// THEN
		DateTime time = new DateTime(2012, 10, 10, 22, 00, 00);
		Assert.assertThat(time, Matchers.equalTo(readItem.get().getStart()));
		Assert.assertThat("the long comment",
				Matchers.equalTo(readItem.get().getComment().get()));
	}


}

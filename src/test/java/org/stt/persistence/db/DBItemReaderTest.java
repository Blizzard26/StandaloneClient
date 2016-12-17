package org.stt.persistence.db;

import static org.hamcrest.CoreMatchers.is;
import static org.mockito.BDDMockito.given;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
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

import com.google.common.base.Optional;

public class DBItemReaderTest {

	private H2ConnectionProvider connectionProvider;
	private DBItemReader sut;
	
	@Mock
	H2Configuration configuration;
	private DBStorage dbStorage;
	private Connection connection;
	
	@Before
	public void setUp() throws ClassNotFoundException, SQLException {
		MockitoAnnotations.initMocks(this);
		
		given(configuration.getDatabase()).willReturn("mem:H2ItemReaderTest");
		given(configuration.getUserName()).willReturn("test");
		given(configuration.getPassword()).willReturn("");
		
		
		this.connectionProvider = new H2ConnectionProvider(configuration);
		connection = connectionProvider.getConnection();
		
		this.dbStorage = new DBStorage(connectionProvider);
		
		sut = new DBItemReader(dbStorage);
	}
	
	@After
	public void tearDown() throws IOException, SQLException {
		sut.close();
		
		connectionProvider.releaseConnection(connection);
		
		Assume.assumeThat(connectionProvider.getConnectionCount(), is(0));
	}
	
	@Test
	public void multiLineCommentGetsImportedCorrectly() throws ClassNotFoundException, SQLException {

		// GIVEN
		givenItem(
				new DateTime(2012,10,10,22,00,00), new DateTime(2012,11,10,22,00,01), 
				"this is\n a multiline\r string\r\n with different separators");

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
		givenItem(new DateTime(2012,10,10,22,00,00), null, null);

		// WHEN
		Optional<TimeTrackingItem> readItem = sut.read();

		// THEN
		DateTime time = new DateTime(2012, 10, 10, 22, 00, 00);
		Assert.assertThat(time, Matchers.equalTo(readItem.get().getStart()));
	}

	@Test
	public void startTimeAndCommentGiven() throws ClassNotFoundException, SQLException {

		// GIVEN
		givenItem(
				new DateTime(2012,10,10,22,00,00), null, "the long comment");

		// WHEN
		Optional<TimeTrackingItem> readItem = sut.read();

		// THEN
		DateTime time = new DateTime(2012, 10, 10, 22, 00, 00);
		Assert.assertThat(time, Matchers.equalTo(readItem.get().getStart()));
		Assert.assertThat("the long comment",
				Matchers.equalTo(readItem.get().getComment().get()));
	}
	
	private void givenItem(DateTime startDate, DateTime endDate, String comment)
			throws ClassNotFoundException, SQLException {

		try (PreparedStatement preparedStatement = connection.prepareStatement(DBStorage.INSERT_STATEMENT))
		{

			preparedStatement.setTimestamp(DBStorage.INDEX_START, DBUtil.transform(startDate));
			if (endDate != null) {
				preparedStatement.setTimestamp(DBStorage.INDEX_END, DBUtil.transform(endDate));
			} else {
				preparedStatement.setNull(DBStorage.INDEX_END, java.sql.Types.BIGINT);
			}
	
			if (comment != null) {
				preparedStatement.setString(DBStorage.INDEX_COMMENT, comment);
			} else {
				preparedStatement.setNull(DBStorage.INDEX_COMMENT, java.sql.Types.VARCHAR);
			}
	
			preparedStatement.setNull(DBStorage.INDEX_LOGGED, java.sql.Types.INTEGER);
			preparedStatement.executeUpdate();
		}
		
	}


}

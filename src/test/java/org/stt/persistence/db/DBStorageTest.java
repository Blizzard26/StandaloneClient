package org.stt.persistence.db;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeNoException;
import static org.junit.Assume.assumeThat;
import static org.mockito.BDDMockito.given;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.hamcrest.Matchers;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.stt.model.TimeTrackingItem;

import com.google.common.base.Optional;

public class DBStorageTest {
	
	private H2ConnectionProvider connectionProvider;
	
	@Mock
	H2Configuration configuration;

	private DBStorage sut;

	private Connection connection;

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		
		given(configuration.getDatabase()).willReturn("mem:H2ItemWriterTest");
		given(configuration.getUserName()).willReturn("test");
		given(configuration.getPassword()).willReturn("");
		
		
		this.connectionProvider = new H2ConnectionProvider(configuration);
		connection = connectionProvider.getConnection();
		
		this.sut = new DBStorage(connectionProvider);
	}

	@After
	public void tearDown() throws Exception {

		connectionProvider.releaseConnection(connection);
		
		assumeThat(connectionProvider.getConnectionCount(), is(0));
	}

	@Test(expected = NullPointerException.class)
    public void writeNullObjectFails() throws SQLException {

        // WHEN
        sut.insertItemInDB(null);

        // THEN
        // Exception expected
    }

    @Test
    public void shouldInsertItem() throws SQLException {

        // GIVEN
        TimeTrackingItem theItem = new TimeTrackingItem("the comment",
                DateTime.now());

        // WHEN
        sut.insertItemInDB(theItem);

        // THEN
        assertThatDBContains(theItem);
    }
    
    @Test
    public void writeStartSucceeds() throws SQLException {

        // GIVEN
        DateTime theTime = new DateTime(2011, 10, 12, 13, 14, 15);
        TimeTrackingItem theItem = new TimeTrackingItem(null, theTime);

        // WHEN
        sut.insertItemInDB(theItem);

        // THEN
        assertThatDBContains(theItem);
    }

    @Test
    public void writeEndSucceeds() throws SQLException {

        // GIVEN
        TimeTrackingItem theItem = new TimeTrackingItem(null, new DateTime(2011, 10, 12, 13, 14, 15), new DateTime(2012, 10, 12, 13, 14, 15));

        // WHEN
        sut.insertItemInDB(theItem);

        // THEN
        assertThatDBContains(theItem);
    }

    @Test
    public void writeCompleteEntrySucceeds() throws SQLException {

        // GIVEN
        TimeTrackingItem theItem = new TimeTrackingItem("the comment", new DateTime(2011, 10, 12, 13, 14, 15),
                new DateTime(2012, 10, 12, 13, 14, 15));

        // WHEN
        sut.insertItemInDB(theItem);

        // THEN
        assertThatDBContains(theItem);
    }

    @Test
    public void writeMultiLineEntrySucceeds() throws SQLException {

        // GIVEN
        TimeTrackingItem theItem = new TimeTrackingItem(
                "this is\n a multiline\r string\r\n with different separators",
                DateTime.now());

        // WHEN
        sut.insertItemInDB(theItem);

        // THEN
        assertThatDBContains(theItem);
    }

    @Test
    public void writeMultipleItems() throws SQLException {

        // GIVEN
        TimeTrackingItem theItem = new TimeTrackingItem("testitem",
                new DateTime(2011, 10, 10, 11, 12, 13), new DateTime(2014, 10, 10, 11, 12, 13));
        TimeTrackingItem theItem2 = new TimeTrackingItem("testitem",
                new DateTime(2014, 10, 10, 11, 12, 13));
        

        // when
        sut.insertItemInDB(theItem);
        sut.insertItemInDB(theItem2);
        
        
        // then
        assertThatDBContains(theItem, theItem2);
    }
    
    @Test
    public void shouldWriteItemsWithMultipleWhitespaces() throws SQLException {
        // GIVEN
        TimeTrackingItem theItem = new TimeTrackingItem("item with 2  spaces",
                new DateTime(2011, 10, 10, 11, 12, 13));

        // when
        sut.insertItemInDB(theItem);

        // then
        assertThatDBContains(theItem);
    }

    @Test
    public void itemsCanBeDeleted() throws SQLException {

        // GIVEN
        DateTime endOfFirstItem = new DateTime(2014, 10, 10, 11, 12, 13);
		TimeTrackingItem theItem = new TimeTrackingItem("testitem",
                new DateTime(2011, 10, 10, 11, 12, 13),
                endOfFirstItem);
        TimeTrackingItem theItem2 = new TimeTrackingItem("testitem",
                endOfFirstItem);
        sut.insertItemInDB(theItem);
        sut.insertItemInDB(theItem2);
        

        // when
        sut.deleteItemInDB(theItem2);

        // then
        assertThatDBContains(theItem);
    }

	@Test
	public void multiLineCommentGetsImportedCorrectly() throws SQLException {

		// GIVEN
		DateTime startDate = new DateTime(2012,10,10,22,00,00);
		DateTime endDate = new DateTime(2012,11,10,22,00,01);
		sut.insertItemInDB(new TimeTrackingItem("this is\n a multiline\r string\r\n with different separators", startDate, endDate));

		// WHEN
		List<TimeTrackingItem> readItem = sut.getAllItems();

		// THEN
		assertThat(readItem.size(), is(1));
		TimeTrackingItem item = readItem.get(0);
		assertThat(item.getComment(), is(Optional.of("this is\n a multiline\r string\r\n with different separators")));
	}

	@Test
	public void onlyStartTimeGiven() throws SQLException {

		// GIVEN
		DateTime startDate = new DateTime(2012,10,10,22,00,00);
		sut.insertItemInDB(new TimeTrackingItem(null, startDate));

		// WHEN
		List<TimeTrackingItem> readItem = sut.getAllItems();

		// THEN
		assertThat(readItem.size(), is(1));
		TimeTrackingItem item = readItem.get(0);
		
		DateTime time = new DateTime(2012, 10, 10, 22, 00, 00);
		Assert.assertThat(time, Matchers.equalTo(item.getStart()));
	}

	@Test
	public void startTimeAndCommentGiven() throws ClassNotFoundException, SQLException {

		// GIVEN
		DateTime startDate = new DateTime(2012,10,10,22,00,00);
		sut.insertItemInDB(new TimeTrackingItem("the long comment", startDate));

		// WHEN
		List<TimeTrackingItem> readItem = sut.getAllItems();

		// THEN
		assertThat(readItem.size(), is(1));
		TimeTrackingItem item = readItem.get(0);
		
		DateTime time = new DateTime(2012, 10, 10, 22, 00, 00);
		Assert.assertThat(time, Matchers.equalTo(item.getStart()));
		Assert.assertThat("the long comment",
				Matchers.equalTo(item.getComment().get()));
	}

	@Test
	public void getSingleItem() throws SQLException
	{
		// GIVEN
		TimeTrackingItem theItem = new TimeTrackingItem("item", new DateTime(2016, 12, 17, 14, 0, 0), 
				new DateTime(2016, 12, 17, 16, 0, 0));
		sut.insertItemInDB(theItem);
		
		// WHEN
		List<TimeTrackingItem> items = sut.getAllItems();
		
		// THEN
		assertThat(items, Matchers.contains(theItem));
	}
	
	@Test
	public void getMultipleItems() throws SQLException
	{	
		// GIVEN
		TimeTrackingItem first = new TimeTrackingItem("item", new DateTime(2016, 12, 17, 14, 0, 0), 
				new DateTime(2016, 12, 17, 16, 0, 0));
		sut.insertItemInDB(first);
		TimeTrackingItem second = new TimeTrackingItem("item 2", new DateTime(2016, 12, 17, 16, 0, 0));
		sut.insertItemInDB(second);
		
		// WHEN
		List<TimeTrackingItem> items = sut.getAllItems();
		
		// THEN
		assertThat(items, Matchers.contains(first, second));
	}
	
	@Test
	public void getMultipleItemsOrdered() throws SQLException
	{
		// GIVEN
		TimeTrackingItem first = new TimeTrackingItem("item", new DateTime(2016, 12, 17, 14, 0, 0), 
				new DateTime(2016, 12, 17, 16, 0, 0));
		TimeTrackingItem second = new TimeTrackingItem("item 2", new DateTime(2016, 12, 17, 16, 0, 0));
		
		sut.insertItemInDB(second);
		sut.insertItemInDB(first);
		
		// WHEN
		List<TimeTrackingItem> items = sut.getAllItems();
		
		// THEN
		assertThat(items, Matchers.contains(first, second));
	}
	
	@Test
	public void getRangeOpenEnd() throws SQLException
	{
		// |------|
		//     |-----
		
		// GIVEN
		TimeTrackingItem theItem = new TimeTrackingItem("item", 
				new DateTime(2016, 12, 17, 14, 0, 0), 
				new DateTime(2016, 12, 17, 18, 0, 0));
		sut.insertItemInDB(theItem);
		
		// WHEN
		List<TimeTrackingItem> items = sut.getTimeTrackingItemsInRange(new DateTime(2016, 12, 17, 16 ,0, 0), 
				Optional.<DateTime>absent());
		
		// THEN
		assertThat(items, contains(theItem));
	}
	
	@Test
	public void getRangeWithEnd() throws SQLException
	{
		// |------|
		//   |--|
		
		// GIVEN
		TimeTrackingItem theItem = new TimeTrackingItem("item", 
				new DateTime(2016, 12, 17, 14, 0, 0), 
				new DateTime(2016, 12, 17, 20, 0, 0));
		sut.insertItemInDB(theItem);
		
		// WHEN
		List<TimeTrackingItem> items = sut.getTimeTrackingItemsInRange(new DateTime(2016, 12, 17, 16 ,0, 0), 
				Optional.of(new DateTime(2016, 12, 17, 18, 0, 0)));
		
		// THEN
		assertThat(items, contains(theItem));
	}
	
	@Test
	public void getRangeWithStartBeforeItem() throws SQLException
	{
		//    |------|
		//  |-----|
		
		// GIVEN
		TimeTrackingItem theItem = new TimeTrackingItem("item", 
				new DateTime(2016, 12, 17, 14, 0, 0), 
				new DateTime(2016, 12, 17, 18, 0, 0));
		sut.insertItemInDB(theItem);
		
		// WHEN
		List<TimeTrackingItem> items = sut.getTimeTrackingItemsInRange(new DateTime(2016, 12, 17, 13 ,0, 0), 
				Optional.of(new DateTime(2016, 12, 17, 17, 0, 0)));
		
		// THEN
		assertThat(items, contains(theItem));
	}
	
	@Test
	public void getRangeWithStartAfterItem() throws SQLException
	{
		// |------|
		//   |-------|
		
		// GIVEN
		TimeTrackingItem theItem = new TimeTrackingItem("item", 
				new DateTime(2016, 12, 17, 14, 0, 0), 
				new DateTime(2016, 12, 17, 18, 0, 0));
		sut.insertItemInDB(theItem);
		
		// WHEN
		List<TimeTrackingItem> items = sut.getTimeTrackingItemsInRange(new DateTime(2016, 12, 17, 16 ,0, 0), 
				Optional.of(new DateTime(2016, 12, 17, 20, 0, 0)));
		
		// THEN
		assertThat(items, contains(theItem));
	}
	
	@Test
	public void getRangeWithMultiple() throws SQLException
	{
		// |----|---|---|
		//   |--------|
		
		// GIVEN
		TimeTrackingItem first = new TimeTrackingItem("item", 
				new DateTime(2016, 12, 17, 9, 0, 0), 
				new DateTime(2016, 12, 17, 11, 0, 0));
		TimeTrackingItem second = new TimeTrackingItem("item 2", 
				new DateTime(2016, 12, 17, 11, 0, 0), 
				new DateTime(2016, 12, 17, 13, 0, 0));
		TimeTrackingItem third = new TimeTrackingItem("item 3", 
				new DateTime(2016, 12, 17, 13, 0, 0), 
				new DateTime(2016, 12, 17, 16, 0, 0));
		
		sut.insertItemInDB(first);
		sut.insertItemInDB(second);
		sut.insertItemInDB(third);
		
		// WHEN
		List<TimeTrackingItem> items = sut.getTimeTrackingItemsInRange(new DateTime(2016, 12, 17, 10 ,0, 0), 
				Optional.of(new DateTime(2016, 12, 17, 15, 0, 0)));
		
		// THEN
		assertThat(items, contains(first, second, third));
	}
	
	private void assertThatDBContains(TimeTrackingItem... items) {
		Arrays.sort(items, new Comparator<TimeTrackingItem>() {

			@Override
			public int compare(TimeTrackingItem o1, TimeTrackingItem o2) {
				return o1.getStart().compareTo(o2.getStart());
			}
		});
		
			try (Statement statement = connection.createStatement())
			{
				
				try (ResultSet resultSet = statement.executeQuery(DBStorage.SELECT_QUERY))
				{
					for (TimeTrackingItem item : items)
					{
						assertThat("Missing item: " + item, resultSet.next(), is(true));
						assertThat(resultSet.getTimestamp(DBStorage.INDEX_START), is(DBUtil.transform(item.getStart())));
						if (item.getEnd().isPresent())
						{
							assertThat(resultSet.getTimestamp(DBStorage.INDEX_END), is(DBUtil.transform(item.getEnd().get())));
						}
						else
						{
							resultSet.getTimestamp(DBStorage.INDEX_END);
							assertThat(resultSet.wasNull(), is(true));
						}
						
						if (item.getComment().isPresent())
						{
							assertThat(resultSet.getString(DBStorage.INDEX_COMMENT), is(item.getComment().get()));
						}
						else
						{
							resultSet.getString(DBStorage.INDEX_COMMENT);
							assertThat(resultSet.wasNull(), is(true));
						}
					}
					
					assertThat("DB contains more items than expected!", resultSet.next(), is(false));
					
				}
			} catch (SQLException e) {
				assumeNoException(e);
			}
	}

	
}

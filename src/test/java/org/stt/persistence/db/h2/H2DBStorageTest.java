package org.stt.persistence.db.h2;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeThat;
import static org.mockito.BDDMockito.given;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.hamcrest.Matchers;
import org.joda.time.DateTime;
import org.jooq.DSLContext;
import org.jooq.Record4;
import org.jooq.ResultQuery;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.stt.model.TimeTrackingItem;
import org.stt.persistence.db.DBStorage;

import com.google.common.base.Optional;

public class H2DBStorageTest {
	
	private H2ConnectionProvider connectionProvider;
	
	@Mock
	H2Configuration configuration;

	private DBStorage sut;

	private Connection connection;

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		
		given(configuration.getDatabase()).willReturn("mem:");
		given(configuration.getUserName()).willReturn("test");
		given(configuration.getPassword()).willReturn("");
		
		
		this.connectionProvider = new H2ConnectionProvider(configuration);
		connection = connectionProvider.acquire();
		
		this.sut = new H2DBStorage(connectionProvider);
	}

	@After
	public void tearDown() throws Exception {

		connectionProvider.release(connection);
		
		assumeThat(connectionProvider.getOpenConnectionCount(), is(0));
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
                DateTime.now().withMillisOfSecond(0));

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
                DateTime.now().withMillisOfSecond(0));

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
		List<TimeTrackingItem> items = sut.getItemsInRange(Optional.of(new DateTime(2016, 12, 17, 16 ,0, 0)), 
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
		List<TimeTrackingItem> items = sut.getItemsInRange(Optional.of(new DateTime(2016, 12, 17, 16 ,0, 0)), 
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
		List<TimeTrackingItem> items = sut.getItemsInRange(Optional.of(new DateTime(2016, 12, 17, 13 ,0, 0)), 
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
		List<TimeTrackingItem> items = sut.getItemsInRange(Optional.of(new DateTime(2016, 12, 17, 16 ,0, 0)), 
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
		List<TimeTrackingItem> items = sut.getItemsInRange(Optional.of(new DateTime(2016, 12, 17, 10 ,0, 0)), 
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
		
		try (DSLContext context = DSL.using(connectionProvider, SQLDialect.H2))
		{
			ResultQuery<Record4<DateTime, DateTime, String, Boolean>> sql = context
					.select(H2DBStorage.COLUMN_START, 
							H2DBStorage.COLUMN_END, 
							H2DBStorage.COLUMN_COMMENT,
							H2DBStorage.COLUMN_LOGGED)
					.from(H2DBStorage.ITEMS_TABLE).
					orderBy(H2DBStorage.COLUMN_START.asc());
		
			List<Record4<DateTime,DateTime,String,Boolean>> result = sql.fetch();
			
			assertThat("Size missmatch", items.length, is(result.size()));
						
			for (int i = 0; i < items.length; i++) {
				TimeTrackingItem item = items[i];
				Record4<DateTime, DateTime, String, Boolean> record = result.get(i);
				assertThat(record.get(H2DBStorage.COLUMN_START), is(item.getStart()));
				assertThat(record.get(H2DBStorage.COLUMN_END), is(item.getEnd().orNull()));
				assertThat(record.get(H2DBStorage.COLUMN_COMMENT), is(item.getComment().orNull()));
			}
			
		}
	}

	
}

package org.stt.persistence.db;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeNoException;
import static org.junit.Assume.assumeThat;
import static org.mockito.BDDMockito.given;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Comparator;

import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.stt.model.TimeTrackingItem;

import com.google.common.base.Optional;

@RunWith(Theories.class)
public class DBItemPersisterTest {
	
    @DataPoints
    public static DateTime[] sampleDateTimes = new DateTime[]{
            new DateTime(2011, 10, 10, 11, 12, 13),
            new DateTime(2010, 10, 10, 11, 12, 13),
            new DateTime(2012, 10, 10, 11, 12, 13)};

	private H2ConnectionProvider connectionProvider;
	private DBItemPersister sut;
	
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
		
		sut = new DBItemPersister(dbStorage);
	}
	
	@After
	public void tearDown() throws IOException, SQLException {
		sut.close();
		
		connectionProvider.releaseConnection(connection);
		
		assumeThat(connectionProvider.getConnectionCount(), is(0));
	}
	
	@Test(expected = NullPointerException.class)
    public void writeNullObjectFails() throws IOException {

        // WHEN
        sut.insert(null);

        // THEN
        // Exception expected
    }

    @Test
    public void writeCommentSucceeds() throws IOException {

        // GIVEN
        TimeTrackingItem theItem = new TimeTrackingItem("the comment",
                DateTime.now());

        // WHEN
        sut.insert(theItem);

        // THEN
        assertThatDBContains(theItem);
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

    @Test
    public void writeStartSucceeds() throws IOException {

        // GIVEN
        DateTime theTime = new DateTime(2011, 10, 12, 13, 14, 15);
        TimeTrackingItem theItem = new TimeTrackingItem(null, theTime);

        // WHEN
        sut.insert(theItem);

        // THEN
        assertThatDBContains(theItem);
    }

    @Test
    public void writeEndSucceeds() throws IOException {

        // GIVEN
        DateTime start = new DateTime(2011, 10, 12, 13, 14, 15);
        DateTime end = new DateTime(2012, 10, 12, 13, 14, 15);

        TimeTrackingItem theItem = new TimeTrackingItem(null, start, end);

        // WHEN
        sut.insert(theItem);

        // THEN
        assertThatDBContains(theItem);
    }

    @Test
    public void writeCompleteEntrySucceeds() throws IOException {

        // GIVEN
        DateTime start = new DateTime(2011, 10, 12, 13, 14, 15);

        DateTime end = new DateTime(2012, 10, 12, 13, 14, 15);
        TimeTrackingItem theItem = new TimeTrackingItem("the comment", start,
                end);

        // WHEN
        sut.insert(theItem);

        // THEN
        assertThatDBContains(theItem);
    }

    @Test
    public void itemsCanBeDeleted() throws IOException {

        // GIVEN
        TimeTrackingItem theItem = new TimeTrackingItem("testitem",
                new DateTime(2011, 10, 10, 11, 12, 13));
        TimeTrackingItem theItem2 = new TimeTrackingItem("testitem",
                new DateTime(2014, 10, 10, 11, 12, 13));
        sut.insert(theItem);
        sut.insert(theItem2);
        

        // when
        sut.delete(theItem2);

        // then
        TimeTrackingItem expectedItem = new TimeTrackingItem("testitem",
                new DateTime(2011, 10, 10, 11, 12, 13),
                new DateTime(2014, 10, 10, 11, 12, 13));
        assertThatDBContains(expectedItem);
    }

    @Test
    public void itemCanBeReplaced() throws IOException {

        // GIVEN
        TimeTrackingItem theItem = new TimeTrackingItem("testitem",
                new DateTime(2011, 10, 10, 11, 12, 13));
        TimeTrackingItem theItem2 = new TimeTrackingItem("testitem",
                DateTime.now());
        sut.insert(theItem2);

        // when
        sut.replace(theItem2, theItem);

        // then
        assertThatDBContains(theItem);
    }


    @Theory
    public void shouldRemoveCoveredTimeIntervalsIfNewItemHasNoEnd(
            DateTime startOfNewItem) throws IOException {
        DateTime startOfExistingItem = new DateTime(2011, 10, 10, 11, 12, 13);

        assumeFalse(startOfNewItem.isAfter(startOfExistingItem));
        // GIVEN
        TimeTrackingItem existingItem = new TimeTrackingItem("testitem",
                startOfExistingItem);
        sut.insert(existingItem);

        TimeTrackingItem newItem = new TimeTrackingItem("testitem2",
                startOfExistingItem);

        // WHEN
        sut.insert(newItem);

        // THEN
        assertThatDBContains(newItem);
    }

    @Test
    public void shouldSetEndTimeIfNewItemIsStarted() throws IOException {
        // GIVEN
        DateTime startOfExistingItem = new DateTime(2011, 10, 10, 11, 12, 13);
		TimeTrackingItem existingItem = new TimeTrackingItem("testitem",
                startOfExistingItem);
        sut.insert(existingItem);

        DateTime startOfNewItem = new DateTime(2011, 10, 10, 11, 12, 14);
		TimeTrackingItem newItem = new TimeTrackingItem("testitem2",
                startOfNewItem);

        // WHEN
        // |------
        //    |---
        sut.insert(newItem);

        // THEN
        assertThatDBContains(new TimeTrackingItem("testitem", startOfExistingItem, startOfNewItem), 
        		newItem);

    }

    @Theory
    public void shouldRemoveCoveredTimeIntervalsIfCoveredByNewItem(
            DateTime startOfNewItem) throws IOException {
        DateTime startOfExistingItem = new DateTime(2011, 10, 10, 11, 12, 13);
        DateTime endOfNewItem = new DateTime(2020, 10, 10, 11, 12, 13);

        Assume.assumeFalse(startOfNewItem.isAfter(startOfExistingItem));
        // GIVEN
        TimeTrackingItem existingItem = new TimeTrackingItem("existing item",
                startOfExistingItem, endOfNewItem);
        sut.insert(existingItem);

        TimeTrackingItem newItem = new TimeTrackingItem("new item",
                startOfExistingItem, endOfNewItem);

        // WHEN
        sut.insert(newItem);

        // THEN
        assertThatDBContains(newItem);
    }

    @Theory
    public void shouldSplitOverlappingTimeIntervalWithEndIfNewItemEndsBefore(
            DateTime startOfNewItem) throws IOException {
        DateTime startOfExistingItem = new DateTime(2011, 10, 10, 11, 12, 13);
        DateTime endOfNewItem = new DateTime(2020, 10, 10, 11, 12, 13);
        DateTime endOfExistingItem = endOfNewItem.plusMinutes(1);

        Assume.assumeFalse(startOfNewItem.isAfter(startOfExistingItem));
        // GIVEN
        TimeTrackingItem existingItem = new TimeTrackingItem("existing item",
                startOfExistingItem, endOfExistingItem);
        sut.insert(existingItem);

        TimeTrackingItem newItem = new TimeTrackingItem("new item",
                startOfExistingItem, endOfNewItem);

        // WHEN
        sut.insert(newItem);

        // THEN
        TimeTrackingItem splitItem = new TimeTrackingItem("existing item", endOfNewItem, endOfExistingItem);
		assertThatDBContains(newItem, splitItem);
    }

    @Theory
    public void shouldSplitOverlappingTimeIntervalWithoutEndIfNewItemEndsBefore(
            DateTime startOfNewItem) throws IOException {
        DateTime startOfExistingItem = new DateTime(2011, 10, 10, 11, 12, 13);
        DateTime endOfNewItem = new DateTime(2020, 10, 10, 11, 12, 13);

        Assume.assumeFalse(startOfNewItem.isAfter(startOfExistingItem));
        // GIVEN
        TimeTrackingItem existingItem = new TimeTrackingItem("existing item",
                startOfExistingItem);
        sut.insert(existingItem);

        TimeTrackingItem newItem = new TimeTrackingItem("new item",
                startOfExistingItem, endOfNewItem);

        // WHEN
        sut.insert(newItem);

        // THEN
        assertThatDBContains(new TimeTrackingItem("new item", startOfExistingItem, endOfNewItem),
        		new TimeTrackingItem("existing item", endOfNewItem));
    }

//    @Test
//    public void shouldChangeEndOfIntervalBeforeRemoveOverlappingIntervalAndChangeStartOfIntervalAfter()
//            throws IOException {
//        // GIVEN
//        TimeTrackingItem itemBeforeBefore = new TimeTrackingItem(
//                "Item before before", new DateTime(2010, 10, 10, 11, 12, 13),
//                new DateTime(2010, 10, 10, 11, 14, 13));
//        sut.insert(itemBeforeBefore);
//        TimeTrackingItem itemBefore = new TimeTrackingItem("Item before",
//                new DateTime(2020, 10, 10, 11, 12, 13), new DateTime(2020, 10,
//                10, 11, 14, 13));
//        sut.insert(itemBefore);
//        TimeTrackingItem overlappedItem = new TimeTrackingItem(
//                "Overlapped item", new DateTime(2020, 10, 10, 11, 14, 13),
//                new DateTime(2020, 10, 10, 11, 15, 13));
//        sut.insert(overlappedItem);
//        TimeTrackingItem itemAfter = new TimeTrackingItem("Item after",
//                new DateTime(2020, 10, 10, 11, 15, 13), new DateTime(2020, 10,
//                10, 11, 17, 13));
//        sut.insert(itemAfter);
//        TimeTrackingItem itemAfterAfter = new TimeTrackingItem(
//                "Item even after", new DateTime(2020, 10, 10, 11, 17, 13),
//                new DateTime(2020, 10, 10, 11, 19, 13));
//        sut.insert(itemAfterAfter);
//
//        TimeTrackingItem newItem = new TimeTrackingItem("new item",
//                new DateTime(2020, 10, 10, 11, 13, 13), new DateTime(2020, 10,
//                10, 11, 16, 13));
//
//        // WHEN
//        sut.insert(newItem);
//
//        // THEN
//
//        Assert.assertThat(
//                stringWriter.toString(),
//                is("2010-10-10_11:12:13 2010-10-10_11:14:13 Item before before"
//                        + LINE_SEPERATOR
//                        + "2020-10-10_11:12:13 2020-10-10_11:13:13 Item before"
//                        + LINE_SEPERATOR
//                        + "2020-10-10_11:13:13 2020-10-10_11:16:13 new item"
//                        + LINE_SEPERATOR
//                        + "2020-10-10_11:16:13 2020-10-10_11:17:13 Item after"
//                        + LINE_SEPERATOR
//                        + "2020-10-10_11:17:13 2020-10-10_11:19:13 Item even after"
//                        + LINE_SEPERATOR));
//    }

    @Test
    public void shouldSplitCoveringExistingItem() throws IOException {
        // GIVEN
        DateTime startOfExistingItem = new DateTime(2012, 1, 1, 10, 0, 0);
		DateTime endOfExistingItem = new DateTime(2012, 1, 1, 13, 0, 0);
		TimeTrackingItem coveringItem = new TimeTrackingItem("covering",
                startOfExistingItem, endOfExistingItem);
		
        sut.insert(coveringItem);
        DateTime startOfNewItem = new DateTime(2012, 1, 1, 11, 0, 0);
		DateTime endOfNewItem = new DateTime(2012, 1, 1, 12, 0, 0);
		TimeTrackingItem coveredItem = new TimeTrackingItem("newItem",
                startOfNewItem, endOfNewItem);
        sut.insert(coveredItem);
        // WHEN

        // THEN
        printDBContent();
        assertThatDBContains(
        		new TimeTrackingItem("covering", startOfExistingItem, startOfNewItem),
        		new TimeTrackingItem("newItem", startOfNewItem, endOfNewItem),
        		new TimeTrackingItem("covering", endOfNewItem, endOfExistingItem));
    }

    private void printDBContent() {
		try (DBItemReader reader = new DBItemReader(dbStorage)) {
			Optional<TimeTrackingItem> item = reader.read();
			while (item.isPresent()) {
				System.out.println(item.get());
				item = reader.read();
			}
		}
    }

	@Test
    public void shouldNotChangeOldNonOverlappingItem() throws IOException {
        // GIVEN
        DateTime startOfExistingItem = new DateTime(2010, 10, 10, 11, 12, 13);
		DateTime endOfExistingItem = new DateTime(2010, 10, 10, 11, 14, 13);
		TimeTrackingItem oldItem = new TimeTrackingItem("old item",
                startOfExistingItem, endOfExistingItem);
        sut.insert(oldItem);

        DateTime startOfNewItem = new DateTime(2011, 10, 10, 11, 12, 13);
		TimeTrackingItem newItem = new TimeTrackingItem("old item",
                startOfNewItem);

        // WHEN
        sut.insert(newItem);

        // THEN
        assertThatDBContains(oldItem, newItem);
    }

//    private void assertThatFileMatches(String... lines) {
//        StringBuilder expectedText = new StringBuilder();
//        for (String line : lines) {
//            expectedText.append(line).append(LINE_SEPERATOR);
//        }
//        assertThat(stringWriter.toString(), is(expectedText.toString()));
//    }
	
}

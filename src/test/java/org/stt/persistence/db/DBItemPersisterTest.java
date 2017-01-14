package org.stt.persistence.db;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;

import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.stt.model.TimeTrackingItem;
import org.stt.persistence.db.h2.H2Configuration;
import org.stt.persistence.db.h2.H2ConnectionProvider;

import com.google.common.base.Optional;



public class DBItemPersisterTest {


	private H2ConnectionProvider connectionProvider;
	private DBItemPersister sut;

	@Mock
	H2Configuration configuration;

	@Mock
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

		//this.dbStorage = new H2DBStorage(connectionProvider);

		sut = new DBItemPersister(dbStorage);
	}

	@After
	public void tearDown() throws IOException, SQLException {
		sut.close();

		connectionProvider.release(connection);

		assumeThat(connectionProvider.getOpenConnectionCount(), is(0));
	}

	@Test(expected = NullPointerException.class)
	public void writeNullObjectFails() throws IOException {

		// WHEN
		sut.insert(null);

		// THEN
		// Exception expected
		verifyNoMoreInteractions(dbStorage);
	}

	@Test
	public void writeCommentSucceeds() throws IOException, SQLException {

		// GIVEN
		given(dbStorage.getItemsInRange(any(), any())).willReturn(Collections.emptyList());
		
		DateTime startDate = DateTime.now().withMillisOfSecond(0);
		TimeTrackingItem theItem = new TimeTrackingItem("the comment", startDate);

		// WHEN
		sut.insert(theItem);
		
		// THEN
		verify(dbStorage).getItemsInRange(Optional.of(startDate), Optional.absent());
		verify(dbStorage).insertItemInDB(theItem);
		verify(dbStorage).startTransaction();
		verify(dbStorage).endTransaction();
		verifyNoMoreInteractions(dbStorage);
	}

	@Test
	public void writeStartSucceeds() throws IOException, SQLException {

		// GIVEN
		given(dbStorage.getItemsInRange(any(), any())).willReturn(Collections.emptyList());
		
		DateTime theTime = new DateTime(2011, 10, 12, 13, 14, 15);
		TimeTrackingItem theItem = new TimeTrackingItem(null, theTime);
		
		// WHEN
		sut.insert(theItem);
		
		// THEN
		verify(dbStorage).getItemsInRange(Optional.of(theTime), Optional.absent());
		verify(dbStorage).insertItemInDB(theItem);
		verify(dbStorage).startTransaction();
		verify(dbStorage).endTransaction();
		verifyNoMoreInteractions(dbStorage);
	}

	@Test
	public void writeEndSucceeds() throws IOException, SQLException {

		// GIVEN
		given(dbStorage.getItemsInRange(any(), any())).willReturn(Collections.emptyList());
		
		DateTime start = new DateTime(2011, 10, 12, 13, 14, 15);
		DateTime end = new DateTime(2012, 10, 12, 13, 14, 15);

		TimeTrackingItem theItem = new TimeTrackingItem(null, start, end);

		// WHEN
		sut.insert(theItem);
		
		// THEN
		verify(dbStorage).getItemsInRange(Optional.of(start), Optional.of(end));
		verify(dbStorage).insertItemInDB(theItem);
		verify(dbStorage).startTransaction();
		verify(dbStorage).endTransaction();
		verifyNoMoreInteractions(dbStorage);
	}

	@Test
	public void writeCompleteEntrySucceeds() throws IOException, SQLException {

		// GIVEN
		given(dbStorage.getItemsInRange(any(), any())).willReturn(Collections.emptyList());
		
		DateTime start = new DateTime(2011, 10, 12, 13, 14, 15);
		DateTime end = new DateTime(2012, 10, 12, 13, 14, 15);
		TimeTrackingItem theItem = new TimeTrackingItem("the comment", start, end);

		// WHEN
		sut.insert(theItem);
		
		// THEN
		verify(dbStorage).getItemsInRange(Optional.of(start), Optional.of(end));
		verify(dbStorage).insertItemInDB(theItem);
		verify(dbStorage).startTransaction();
		verify(dbStorage).endTransaction();
		verifyNoMoreInteractions(dbStorage);
	}

	@Test
	public void itemsCanBeDeleted() throws IOException, SQLException {

		// GIVEN
		TimeTrackingItem theItem = new TimeTrackingItem("testitem", new DateTime(2014, 10, 10, 11, 12, 13));

		// WHEN
		sut.delete(theItem);

		// THEN
		verify(dbStorage).deleteItemInDB(theItem);
		verify(dbStorage).startTransaction();
		verify(dbStorage).endTransaction();
		verifyNoMoreInteractions(dbStorage);
	}

	@Test
	public void itemCanBeReplaced() throws IOException, SQLException {

		// GIVEN
		TimeTrackingItem theItem = new TimeTrackingItem("testitem", new DateTime(2011, 10, 10, 11, 12, 13));
		TimeTrackingItem theItem2 = new TimeTrackingItem("testitem", DateTime.now());
		//sut.insert(theItem2);

		// WHEN
		sut.replace(theItem2, theItem);
		
		// THEN
		verify(dbStorage).deleteItemInDB(theItem2);
		verify(dbStorage).insertItemInDB(theItem);
		verify(dbStorage).startTransaction();
		verify(dbStorage).endTransaction();
		verifyNoMoreInteractions(dbStorage);
	}

	@Test
	public void shouldRemoveCoveredTimeIntervalsIfNewItemHasNoEnd() throws IOException, SQLException {
		DateTime startOfExistingItem = new DateTime(2011, 10, 10, 11, 12, 13);

		// GIVEN
		TimeTrackingItem existingItem = new TimeTrackingItem("testitem", startOfExistingItem);
		
		given(dbStorage.getItemsInRange(any(), any())).willReturn(Collections.singletonList(existingItem));

		TimeTrackingItem newItem = new TimeTrackingItem("testitem2", startOfExistingItem);

		// WHEN
		sut.insert(newItem);
		
		// THEN
		verify(dbStorage).getItemsInRange(Optional.of(startOfExistingItem), Optional.absent());
		verify(dbStorage).deleteItemInDB(existingItem);
		verify(dbStorage).insertItemInDB(newItem);
		verify(dbStorage).startTransaction();
		verify(dbStorage).endTransaction();
		verifyNoMoreInteractions(dbStorage);
	}
	
	@Test
	public void shouldRemoveCoveredTimeItem() throws IOException, SQLException {
		DateTime startOfExistingItem1 = new DateTime(2011, 10, 10, 16, 14, 00);
		DateTime endOfExistingItem1 = new DateTime(2011, 10, 10, 17, 00, 00);
		DateTime startOfExistingItem2 = endOfExistingItem1;
		DateTime endOfExistingItem2 = new DateTime(2011, 10, 10, 17, 03, 00);
		DateTime startOfExistingItem3 = endOfExistingItem2;

		// GIVEN
		TimeTrackingItem existingItem1 = new TimeTrackingItem("existingItem1", startOfExistingItem1, endOfExistingItem1);
		TimeTrackingItem existingItem2 = new TimeTrackingItem("existingItem2", startOfExistingItem2, endOfExistingItem2);
		TimeTrackingItem existingItem3 = new TimeTrackingItem("existingItem3", startOfExistingItem3);
		
		given(dbStorage.getItemsInRange(any(), any())).willReturn(Arrays.asList(existingItem1, existingItem2, existingItem3));

		TimeTrackingItem newItem = new TimeTrackingItem("testitem", startOfExistingItem2, endOfExistingItem2);

		// WHEN
		sut.insert(newItem);
		
		// THEN
		verify(dbStorage).getItemsInRange(Optional.of(startOfExistingItem2), Optional.of(endOfExistingItem2));
		verify(dbStorage).deleteItemInDB(existingItem2);
		verify(dbStorage).insertItemInDB(newItem);
		verify(dbStorage).startTransaction();
		verify(dbStorage).endTransaction();
		verifyNoMoreInteractions(dbStorage);
	}

	@Test
	public void shouldSetEndTimeIfNewItemIsStarted() throws IOException, SQLException {
		// GIVEN
		DateTime startOfExistingItem = new DateTime(2011, 10, 10, 11, 12, 13);
		TimeTrackingItem existingItem = new TimeTrackingItem("testitem", startOfExistingItem);
		
		given(dbStorage.getItemsInRange(any(), any())).willReturn(Collections.singletonList(existingItem));

		DateTime startOfNewItem = new DateTime(2011, 10, 10, 11, 12, 14);
		TimeTrackingItem newItem = new TimeTrackingItem("testitem2", startOfNewItem);

		// WHEN
		// |------
		// |---
		sut.insert(newItem);

		// THEN
		verify(dbStorage).getItemsInRange(Optional.of(startOfNewItem), Optional.absent());
		verify(dbStorage).deleteItemInDB(existingItem);
		verify(dbStorage).insertItemInDB(new TimeTrackingItem("testitem", startOfExistingItem, startOfNewItem));
		verify(dbStorage).insertItemInDB(newItem);
		verify(dbStorage, times(2)).startTransaction();
		verify(dbStorage, times(2)).endTransaction();
		verifyNoMoreInteractions(dbStorage);
	}

	@Test
	public void shouldRemoveCoveredTimeIntervalsIfCoveredByNewItem() throws IOException, SQLException {
		DateTime startOfExistingItem = new DateTime(2011, 10, 10, 11, 12, 13);
		DateTime endOfNewItem = new DateTime(2020, 10, 10, 11, 12, 13);

		// GIVEN
		TimeTrackingItem existingItem = new TimeTrackingItem("existing item", startOfExistingItem, endOfNewItem);
		
		given(dbStorage.getItemsInRange(any(), any())).willReturn(Collections.singletonList(existingItem));

		TimeTrackingItem newItem = new TimeTrackingItem("new item", startOfExistingItem, endOfNewItem);

		// WHEN
		sut.insert(newItem);
		
		// THEN
		verify(dbStorage).getItemsInRange(Optional.of(startOfExistingItem), Optional.of(endOfNewItem));
		verify(dbStorage).deleteItemInDB(existingItem);
		verify(dbStorage).insertItemInDB(newItem);
		verify(dbStorage).startTransaction();
		verify(dbStorage).endTransaction();
		verifyNoMoreInteractions(dbStorage);
	}
	
	@Test
	public void shouldReduceItemIfCoveredToEnd() throws IOException, SQLException {
	
		DateTime startOfExistingItem = new DateTime(2011, 10, 10, 11, 10, 13);
		DateTime endOfExistingItem = new DateTime(2011, 10, 10, 11, 13, 13);
		DateTime startOfNewItem = new DateTime(2011, 10, 10, 11, 12, 00);
		DateTime endOfNewItem = endOfExistingItem;

		// GIVEN
		TimeTrackingItem existingItem = new TimeTrackingItem("existing item", startOfExistingItem, endOfExistingItem);
		
		given(dbStorage.getItemsInRange(any(), any())).willReturn(Collections.singletonList(existingItem));

		TimeTrackingItem newItem = new TimeTrackingItem("new item", startOfNewItem, endOfNewItem);

		// WHEN
		sut.insert(newItem);
		
		// THEN
		verify(dbStorage).getItemsInRange(Optional.of(startOfNewItem), Optional.of(endOfNewItem));
		verify(dbStorage).deleteItemInDB(existingItem);
		verify(dbStorage).insertItemInDB(new TimeTrackingItem("existing item", startOfExistingItem, startOfNewItem));
		verify(dbStorage).insertItemInDB(newItem);
		verify(dbStorage, times(2)).startTransaction();
		verify(dbStorage, times(2)).endTransaction();
		verifyNoMoreInteractions(dbStorage);
	}

	@Test
	public void shouldSplitOverlappingTimeIntervalWithEndIfNewItemEndsBefore()
			throws IOException, SQLException {
		DateTime startOfExistingItem = new DateTime(2011, 10, 10, 11, 12, 13);
		DateTime endOfNewItem = new DateTime(2020, 10, 10, 11, 12, 13);
		DateTime endOfExistingItem = endOfNewItem.plusMinutes(1);

		
		// GIVEN
		TimeTrackingItem existingItem = new TimeTrackingItem("existing item", startOfExistingItem, endOfExistingItem);
		
		given(dbStorage.getItemsInRange(any(), any())).willReturn(Collections.singletonList(existingItem));

		TimeTrackingItem newItem = new TimeTrackingItem("new item", startOfExistingItem, endOfNewItem);

		// WHEN
		sut.insert(newItem);

		// THEN
		TimeTrackingItem splitItem = new TimeTrackingItem("existing item", endOfNewItem, endOfExistingItem);
		
		verify(dbStorage).getItemsInRange(Optional.of(startOfExistingItem), Optional.of(endOfNewItem));
		verify(dbStorage).deleteItemInDB(existingItem);
		verify(dbStorage).insertItemInDB(splitItem);
		verify(dbStorage).insertItemInDB(newItem);
		verify(dbStorage, times(2)).startTransaction();
		verify(dbStorage, times(2)).endTransaction();
		verifyNoMoreInteractions(dbStorage);
	}

	@Test
	public void shouldSplitOverlappingTimeIntervalWithoutEndIfNewItemEndsBefore()
			throws IOException, SQLException {
		DateTime startOfExistingItem = new DateTime(2011, 10, 10, 11, 12, 13);
		DateTime endOfNewItem = new DateTime(2020, 10, 10, 11, 12, 13);

		
		// GIVEN
		TimeTrackingItem existingItem = new TimeTrackingItem("existing item", startOfExistingItem);
		
		given(dbStorage.getItemsInRange(any(), any())).willReturn(Collections.singletonList(existingItem));

		TimeTrackingItem newItem = new TimeTrackingItem("new item", startOfExistingItem, endOfNewItem);

		// WHEN
		sut.insert(newItem);
		
		// THEN
		verify(dbStorage).getItemsInRange(Optional.of(startOfExistingItem), Optional.of(endOfNewItem));
		verify(dbStorage).deleteItemInDB(existingItem);
		verify(dbStorage).insertItemInDB(new TimeTrackingItem("existing item", endOfNewItem));
		verify(dbStorage).insertItemInDB(new TimeTrackingItem("new item", startOfExistingItem, endOfNewItem));
		verify(dbStorage, times(2)).startTransaction();
		verify(dbStorage, times(2)).endTransaction();
		verifyNoMoreInteractions(dbStorage);
	}

	@Test
	public void shouldChangeEndOfIntervalBeforeRemoveOverlappingIntervalAndChangeStartOfIntervalAfter()
			throws IOException, SQLException {
		// GIVEN
//		TimeTrackingItem itemBeforeBefore = new TimeTrackingItem("Item before before",
//				new DateTime(2010, 10, 10, 11, 12, 13), new DateTime(2010, 10, 10, 11, 14, 13));
		//sut.insert(itemBeforeBefore);
		TimeTrackingItem itemBefore = new TimeTrackingItem("Item before", new DateTime(2020, 10, 10, 11, 12, 13),
				new DateTime(2020, 10, 10, 11, 14, 13));
		//sut.insert(itemBefore);
		TimeTrackingItem overlappedItem = new TimeTrackingItem("Overlapped item",
				new DateTime(2020, 10, 10, 11, 14, 13), new DateTime(2020, 10, 10, 11, 15, 13));
		//sut.insert(overlappedItem);
		TimeTrackingItem itemAfter = new TimeTrackingItem("Item after", new DateTime(2020, 10, 10, 11, 15, 13),
				new DateTime(2020, 10, 10, 11, 17, 13));
		//sut.insert(itemAfter);
//		TimeTrackingItem itemAfterAfter = new TimeTrackingItem("Item even after",
//				new DateTime(2020, 10, 10, 11, 17, 13), new DateTime(2020, 10, 10, 11, 19, 13));
		//sut.insert(itemAfterAfter);

		given(dbStorage.getItemsInRange(any(), any())).willReturn(Arrays.asList(itemBefore, overlappedItem, itemAfter));
		
		DateTime startOfNewItem = new DateTime(2020, 10, 10, 11, 13, 13);
		DateTime endOfNewItem = new DateTime(2020, 10, 10, 11, 16, 13);
		TimeTrackingItem newItem = new TimeTrackingItem("new item", startOfNewItem,
				endOfNewItem);

		// WHEN
		sut.insert(newItem);

		// THEN
		
		verify(dbStorage).getItemsInRange(Optional.of(startOfNewItem), Optional.of(endOfNewItem));
		verify(dbStorage).deleteItemInDB(itemBefore);
		verify(dbStorage).insertItemInDB(new TimeTrackingItem("Item before", new DateTime(2020, 10, 10, 11, 12, 13),
						startOfNewItem));
		verify(dbStorage).deleteItemInDB(overlappedItem);
		verify(dbStorage).deleteItemInDB(itemAfter);
		verify(dbStorage).insertItemInDB(new TimeTrackingItem("Item after", endOfNewItem,
						new DateTime(2020, 10, 10, 11, 17, 13)));
		verify(dbStorage).insertItemInDB(newItem);
		verify(dbStorage, times(3)).startTransaction();
		verify(dbStorage, times(3)).endTransaction();
		verifyNoMoreInteractions(dbStorage);
	}

	@Test
	public void shouldSplitCoveringExistingItem() throws IOException, SQLException {
		// GIVEN
		DateTime startOfExistingItem = new DateTime(2012, 1, 1, 10, 0, 0);
		DateTime endOfExistingItem = new DateTime(2012, 1, 1, 13, 0, 0);
		TimeTrackingItem coveringItem = new TimeTrackingItem("covering", startOfExistingItem, endOfExistingItem);

		DateTime startOfNewItem = new DateTime(2012, 1, 1, 11, 0, 0);
		DateTime endOfNewItem = new DateTime(2012, 1, 1, 12, 0, 0);
		
		given(dbStorage.getItemsInRange(Optional.of(startOfNewItem), Optional.of(endOfNewItem))).willReturn(Collections.singletonList(coveringItem));
		
		// WHEN
		TimeTrackingItem coveredItem = new TimeTrackingItem("newItem", startOfNewItem, endOfNewItem);
		sut.insert(coveredItem);
		
		// THEN		
		verify(dbStorage).getItemsInRange(Optional.of(startOfNewItem), Optional.of(endOfNewItem));
		verify(dbStorage).deleteItemInDB(coveringItem);
		verify(dbStorage).insertItemInDB(new TimeTrackingItem("covering", startOfExistingItem, startOfNewItem));
		verify(dbStorage).insertItemInDB(new TimeTrackingItem("newItem", startOfNewItem, endOfNewItem));
		verify(dbStorage).insertItemInDB(new TimeTrackingItem("covering", endOfNewItem, endOfExistingItem));
		verify(dbStorage).startTransaction();
		verify(dbStorage).endTransaction();
		verifyNoMoreInteractions(dbStorage);
	}
	
	@Test
	public void shouldEndExistingItem() throws IOException, SQLException {
		// GIVEN
		DateTime startOfExistingItem = new DateTime(2012, 1, 1, 10, 0, 0);
		DateTime endOfExistingItem = new DateTime(2012, 1, 1, 13, 0, 0);
		TimeTrackingItem coveringItem = new TimeTrackingItem("covering", startOfExistingItem, endOfExistingItem);

		DateTime startOfNewItem = new DateTime(2012, 1, 1, 11, 0, 0);
		
		given(dbStorage.getItemsInRange(Optional.of(startOfNewItem), Optional.absent())).willReturn(Collections.singletonList(coveringItem));
		
		// WHEN
		TimeTrackingItem coveredItem = new TimeTrackingItem("newItem", startOfNewItem);
		sut.insert(coveredItem);
		
		// THEN		
		verify(dbStorage).getItemsInRange(Optional.of(startOfNewItem), Optional.absent());
		verify(dbStorage).deleteItemInDB(coveringItem);
		verify(dbStorage).insertItemInDB(new TimeTrackingItem("covering", startOfExistingItem, startOfNewItem));
		verify(dbStorage).insertItemInDB(new TimeTrackingItem("newItem", startOfNewItem));
		verify(dbStorage, times(2)).startTransaction();
		verify(dbStorage, times(2)).endTransaction();
		verifyNoMoreInteractions(dbStorage);
	}
	
	@Test
	public void shouldNotChangeUncoveredItem() throws IOException, SQLException {
		// GIVEN
		DateTime startOfExistingItem = new DateTime(2012, 1, 1, 10, 0, 0);
		DateTime endOfExistingItem = new DateTime(2012, 1, 1, 13, 0, 0);
		TimeTrackingItem coveringItem = new TimeTrackingItem("covering", startOfExistingItem, endOfExistingItem);

		DateTime startOfNewItem = endOfExistingItem;
		DateTime endOfNewItem = new DateTime(2012, 1, 1, 14, 0, 0);
		
		given(dbStorage.getItemsInRange(Optional.of(startOfNewItem), Optional.of(endOfNewItem))).willReturn(Collections.singletonList(coveringItem));
		
		// WHEN
		TimeTrackingItem newItem = new TimeTrackingItem("newItem", startOfNewItem, endOfNewItem);
		sut.insert(newItem);
		
		// THEN		
		verify(dbStorage).getItemsInRange(Optional.of(startOfNewItem), Optional.of(endOfNewItem));
		verify(dbStorage).insertItemInDB(new TimeTrackingItem("newItem", startOfNewItem, endOfNewItem));
		verify(dbStorage).startTransaction();
		verify(dbStorage).endTransaction();
		verifyNoMoreInteractions(dbStorage);
	}
	
	@Test
	public void shouldSplitItemWithoutEnd() throws IOException, SQLException {
		// GIVEN
		DateTime startOfExistingItem = new DateTime(2012, 1, 1, 10, 0, 0);
		TimeTrackingItem coveringItem = new TimeTrackingItem("covering", startOfExistingItem);

		DateTime startOfNewItem = new DateTime(2012, 1, 1, 11, 0, 0);
		DateTime endOfNewItem = new DateTime(2012, 1, 1, 12, 0, 0);
		
		given(dbStorage.getItemsInRange(Optional.of(startOfNewItem), Optional.of(endOfNewItem))).willReturn(Collections.singletonList(coveringItem));
		
		// WHEN
		TimeTrackingItem coveredItem = new TimeTrackingItem("newItem", startOfNewItem, endOfNewItem);
		sut.insert(coveredItem);
		
		// THEN		
		verify(dbStorage).getItemsInRange(Optional.of(startOfNewItem), Optional.of(endOfNewItem));
		verify(dbStorage).deleteItemInDB(coveringItem);
		verify(dbStorage).insertItemInDB(new TimeTrackingItem("covering", startOfExistingItem, startOfNewItem));
		verify(dbStorage).insertItemInDB(new TimeTrackingItem("newItem", startOfNewItem, endOfNewItem));
		verify(dbStorage).insertItemInDB(new TimeTrackingItem("covering", endOfNewItem));
		verify(dbStorage).startTransaction();
		verify(dbStorage).endTransaction();
		verifyNoMoreInteractions(dbStorage);
	}

	@Test
	public void shouldNotChangeOldNonOverlappingItem() throws IOException, SQLException {
		// GIVEN
		DateTime startOfExistingItem = new DateTime(2010, 10, 10, 11, 12, 13);
		DateTime endOfExistingItem = new DateTime(2010, 10, 10, 11, 14, 13);
		TimeTrackingItem oldItem = new TimeTrackingItem("old item", startOfExistingItem, endOfExistingItem);

		given(dbStorage.getItemsInRange(Optional.of(endOfExistingItem), Optional.absent())).willReturn(Collections.singletonList(oldItem));
		TimeTrackingItem newItem = new TimeTrackingItem("new item", endOfExistingItem);

		// WHEN
		sut.insert(newItem);
		
		// THEN
		verify(dbStorage).getItemsInRange(Optional.of(endOfExistingItem), Optional.absent());
		verify(dbStorage).insertItemInDB(newItem);
		verify(dbStorage).startTransaction();
		verify(dbStorage).endTransaction();
		verifyNoMoreInteractions(dbStorage);
	}
	
	@Test
	public void shouldRollbackOnFail() throws SQLException, IOException
	{
		// GIVEN
		DateTime startOfNewItem = new DateTime(2010, 10, 10, 11, 14, 13);

		given(dbStorage.getItemsInRange(Optional.of(startOfNewItem), Optional.absent())).willThrow(new SQLException());
		TimeTrackingItem newItem = new TimeTrackingItem("new item", startOfNewItem);

		// WHEN
		try {
			sut.insert(newItem);
			fail("Should throw exception");
		} catch (Exception e) {
			// Nothing to do
		}
		
		// THEN
		
		verify(dbStorage).getItemsInRange(Optional.of(startOfNewItem), Optional.absent());
		verify(dbStorage).startTransaction();
		verify(dbStorage).rollback();
		verify(dbStorage).endTransaction();
		verifyNoMoreInteractions(dbStorage);
	}

}

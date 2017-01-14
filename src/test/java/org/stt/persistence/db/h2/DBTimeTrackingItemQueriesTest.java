package org.stt.persistence.db.h2;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeThat;
import static org.mockito.BDDMockito.given;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.hamcrest.collection.IsCollectionWithSize;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.LocalDate;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.experimental.theories.suppliers.TestedOn;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.stt.model.TimeTrackingItem;
import org.stt.query.DNFClause;
import org.stt.query.TimeTrackingItemQueries;

import com.google.common.base.Optional;

@RunWith(Theories.class)
public class DBTimeTrackingItemQueriesTest {

	private H2ConnectionProvider connectionProvider;
	
	@Mock
	H2Configuration configuration;
	
	private Connection connection;
	
    private TimeTrackingItemQueries sut;
    
    private H2DBStorage dbStorage;
    
    

    @Before
    public void setup() throws ClassNotFoundException, SQLException {
		MockitoAnnotations.initMocks(this);
		
		given(configuration.getDatabase()).willReturn("mem:");
		given(configuration.getUserName()).willReturn("test");
		given(configuration.getPassword()).willReturn("");
		
		this.connectionProvider = new H2ConnectionProvider(configuration);
		connection = connectionProvider.acquire();
        
		dbStorage = new H2DBStorage(connectionProvider);
		sut = dbStorage;
    }
    
	@After
	public void tearDown() throws Exception {
		connectionProvider.release(connection);
		
		assumeThat(connectionProvider.getOpenConnectionCount(), is(0));
	}
	
    private void givenDBContains(TimeTrackingItem... items) throws SQLException {
		for (TimeTrackingItem item : items)
		{
			dbStorage.insertItemInDB(item);
		}
	}

    @Test
    public void shouldFindFirstItemsWithinInterval() throws SQLException {
        // GIVEN
        DateTime startOfRequest = new DateTime(2000, 1, 1, 1, 0);
        DateTime end = new DateTime(2000, 1, 1, 9, 1);
        TimeTrackingItem[] timeTrackingItems = givenOneTTIPerHourStartingWith(new DateTime(2000, 1, 1, 0, 0), 10);

        // WHEN
        Collection<TimeTrackingItem> result = sut.queryFirstNItems(Optional.of(startOfRequest), Optional.of(end), Optional.of(2));

        // THEN
        assertThat(result, CoreMatchers.<Collection<TimeTrackingItem>>is(Arrays.asList(timeTrackingItems[1], timeTrackingItems[2])));
    }

    @Test
    public void shouldFindAllItemsWithinInterval() throws SQLException {
        // GIVEN
        DateTime startOfRequest = new DateTime(2000, 1, 1, 1, 0);
        DateTime end = new DateTime(2000, 1, 1, 5, 1);
        TimeTrackingItem[] timeTrackingItems = givenOneTTIPerHourStartingWith(new DateTime(2000, 1, 1, 0, 0), 6);

        // WHEN
        Collection<TimeTrackingItem> result = sut.queryFirstNItems(Optional.of(startOfRequest), Optional.of(end), Optional.<Integer>absent());

        // THEN
        assertThat(result, CoreMatchers.<Collection<TimeTrackingItem>>is(Arrays.asList(timeTrackingItems[1], timeTrackingItems[2], timeTrackingItems[3],
                timeTrackingItems[4])));
    }

    private TimeTrackingItem[] givenOneTTIPerHourStartingWith(DateTime start, int amount) throws SQLException {
        TimeTrackingItem[] items = new TimeTrackingItem[amount];
        DateTime lastTime = start;
        for (int i = 0; i < items.length; i++) {
            DateTime next = lastTime.plusHours(1);
            items[i] = new TimeTrackingItem("", lastTime, next);
            lastTime = next;
        }
        givenDBContains(items);
        return items;
    }

    @Test
    public void shouldNotFindCurrentItemIfNoneCanBeRead() {
        // GIVEN
        //given(reader.read()).willReturn(Optional.<TimeTrackingItem>absent());

        // WHEN
        Optional<TimeTrackingItem> result = sut.getCurrentTimeTrackingitem();

        // THEN
        assertThat(result, is(Optional.<TimeTrackingItem>absent()));
    }

    @Test
    public void shouldFindCurrentItem() throws SQLException {
        // GIVEN
        TimeTrackingItem unfinishedItem = new TimeTrackingItem(null,
                DateTime.now());
        givenDBContains(unfinishedItem);
        
        // WHEN
        Optional<TimeTrackingItem> result = sut.getCurrentTimeTrackingitem();

        // THEN
        assertNotNull(result);
        assertThat(result.get(), is(unfinishedItem));
    }



	@Test
    public void shouldNotFindCurrentItemIfLastOneIsFinished() throws SQLException {
        // GIVEN
        TimeTrackingItem unfinishedItem = new TimeTrackingItem(null,
                DateTime.now(), DateTime.now().plusMillis(1));
        givenDBContains(unfinishedItem);

        // WHEN
        Optional<TimeTrackingItem> result = sut.getCurrentTimeTrackingitem();

        // THEN
        assertThat(result, is(Optional.<TimeTrackingItem>absent()));
    }

    
    @Test
    public void shouldFindPreviousItem() throws SQLException
    {
        // GIVEN
    	TimeTrackingItem previousItem = new TimeTrackingItem(null, DateTime.now().minusMinutes(5), DateTime.now());
        TimeTrackingItem unfinishedItem = new TimeTrackingItem(null,
                DateTime.now(), DateTime.now().plusMillis(1));;
        givenDBContains(previousItem, unfinishedItem);

        // WHEN
        Optional<TimeTrackingItem> result = sut.getPreviousTimeTrackingItem(unfinishedItem);

        // THEN
        assertNotNull(result);
        assertThat(result.get(), is(previousItem));
    }
    
    @Test
    public void shouldNotFindPreviousItem() throws SQLException
    {
        // GIVEN
        TimeTrackingItem unfinishedItem = new TimeTrackingItem(null,
                DateTime.now(), DateTime.now().plusMillis(1));
        givenDBContains(unfinishedItem);

        // WHEN
        Optional<TimeTrackingItem> result = sut.getPreviousTimeTrackingItem(unfinishedItem);

        // THEN
        assertThat(result, is(Optional.<TimeTrackingItem>absent()));
    }


    @Test
    public void shouldOnlyFindCurrentItem() throws SQLException {
        // GIVEN
        DateTime endOfFinishedItem = DateTime.now().withMillisOfSecond(0).plusSeconds(10);
		TimeTrackingItem finishedItem = new TimeTrackingItem(null,
                DateTime.now().withMillisOfSecond(0), endOfFinishedItem);
        TimeTrackingItem unfinishedItem = new TimeTrackingItem(null,
                endOfFinishedItem);
        givenDBContains(finishedItem, unfinishedItem);

        // WHEN
        Optional<TimeTrackingItem> result = sut.getCurrentTimeTrackingitem();

        // THEN
        assertNotNull(result);
        assertTrue(result.isPresent());
        assertThat(result.get(), is(unfinishedItem));
    }
    
    @Test
    public void allTrackedDaysShouldNotReturnSameDateTimeTwice() throws SQLException {
        // GIVEN
        DateTime dateTimes[] = {new DateTime(2000, 1, 1, 0, 0, 0),
                new DateTime(2000, 1, 1, 0, 0, 0)};

        givenReaderReturnsTrackingTimesForStartDates(dateTimes);

        // WHEN
        Collection<DateTime> result = sut.getAllTrackedDays();

        // THEN
        assertNotNull(result);
        
        DateTime last = null;
        for (DateTime current : result) {
            assertThat(last, anyOf(nullValue(), not(is(current))));
            last = current;
        }
    }

    @Test
    public void allTrackedDaysShouldBeAtStartOfDay() throws SQLException {
        // GIVEN
        DateTime dateTimes[] = {new DateTime(2000, 1, 1, 3, 2, 7),
                new DateTime(2010, 1, 1, 11, 12, 13)};

        givenReaderReturnsTrackingTimesForStartDates(dateTimes);

        // WHEN
        Collection<DateTime> result = sut.getAllTrackedDays();

        // THEN
        assertNotNull(result);
        for (DateTime time : result) {
            assertThat(time, is(time.withTimeAtStartOfDay()));
        }
    }

    @Theory
    public void allTrackedDaysShouldReturnADayPerDay(@TestedOn(ints = {0, 1,
            3, 10}) int days) throws SQLException {
        // GIVEN
        Collection<DateTime> timesForItems = new ArrayList<>();
        DateTime timeForItem = new DateTime(2000, 1, 1, 3, 2, 7);
        for (int i = 0; i < days; i++) {
            timesForItems.add(timeForItem);
            timeForItem = timeForItem.plusDays(1);
        }
        givenReaderReturnsTrackingTimesForStartDates(timesForItems
                .toArray(new DateTime[timesForItems.size()]));

        // WHEN
        Collection<DateTime> result = sut.getAllTrackedDays();

        // THEN
        assertNotNull(result);
        assertThat(result, IsCollectionWithSize.hasSize(days));
        Iterator<DateTime> resultIt = result.iterator();
        Iterator<DateTime> timesForItemsIt = timesForItems.iterator();
        while (resultIt.hasNext() || timesForItemsIt.hasNext()) {
            DateTime trackedDay = resultIt.next();
            DateTime trackedItem = timesForItemsIt.next();
            assertThat(trackedDay, is(trackedItem.withTimeAtStartOfDay()));
        }
    }

    private void givenReaderReturnsTrackingTimesForStartDates(
            DateTime[] dateTimes) throws SQLException {
        TimeTrackingItem[] items = new TimeTrackingItem[dateTimes.length];
        for (int i = 0; i < dateTimes.length; i++) {
            items[i] = new TimeTrackingItem(null, dateTimes[i]);
        }
        givenDBContains(items);
    }

    @Test
    public void shouldReturnItemsWithinInterval() throws SQLException {
        // GIVEN
        Interval queryInterval = new Interval(5000, 10000);
        givenReaderReturnsTrackingTimesForStartDates(new DateTime[]{new DateTime(1000), new DateTime(5000), new DateTime(10000), new DateTime(15000)});

        DNFClause dnfClause = new DNFClause();
        dnfClause.withStartBetween(queryInterval);
        // WHEN
        Collection<TimeTrackingItem> result = sut.queryItems(dnfClause);

        // THEN
        assertNotNull(result);
        assertThat(mapItemToStartDateTime(result), Matchers.<Collection<DateTime>>is(Arrays.asList(new DateTime[]{new DateTime(5000)})));
    }

    @Test
    public void shouldReturnItemsWithStartBefore() throws SQLException {
        // GIVEN
        givenReaderReturnsTrackingTimesForStartDates(new DateTime[]{new DateTime(1000), new DateTime(5000), new DateTime(10000), new DateTime(15000)});

        DNFClause dnfClause = new DNFClause();
        dnfClause.withStartBefore(new DateTime(5000));

        // WHEN
        Collection<TimeTrackingItem> result = sut.queryItems(dnfClause);

        // THEN
        assertNotNull(result);
        assertThat(mapItemToStartDateTime(result), Matchers.<Collection<DateTime>>is(Arrays.asList(new DateTime[]{new DateTime(1000)})));
    }

    @Test
    public void shouldReturnItemsWithStartNotBefore() throws SQLException {
        // GIVEN
        givenReaderReturnsTrackingTimesForStartDates(new DateTime[]{new DateTime(1000), new DateTime(5000), new DateTime(10000), new DateTime(15000)});

        DNFClause dnfClause = new DNFClause();
        dnfClause.withStartNotBefore(new DateTime(10000));

        // WHEN
        Collection<TimeTrackingItem> result = sut.queryItems(dnfClause);

        // THEN
        assertNotNull(result);
        assertThat(mapItemToStartDateTime(result), Matchers.<Collection<DateTime>>is(Arrays.asList(new DateTime[]{new DateTime(10000), new DateTime(15000)})));
    }

    @Test
    public void shouldReturnItemWithEndNotAfter() throws SQLException {
        // GIVEN
        TimeTrackingItem expectedResult = new TimeTrackingItem(null, new DateTime(8000), new DateTime(10000));
        givenDBContains(expectedResult, new TimeTrackingItem(null, new DateTime(10000), new DateTime(12000)));
        DNFClause DNFClause = new DNFClause();
        DNFClause.withEndNotAfter(new DateTime(10000));

        // WHEN
        Collection<TimeTrackingItem> result = sut.queryItems(DNFClause);

        // THEN
        assertThat(result, CoreMatchers.<Collection<TimeTrackingItem>>is(Collections.singletonList(expectedResult)));
    }

    @Test
    public void shouldReturnItemWithEndBefore() throws SQLException {
        // GIVEN
        TimeTrackingItem expectedResult = new TimeTrackingItem(null, new DateTime(800), new DateTime(999));
        givenDBContains(expectedResult, new TimeTrackingItem(null, new DateTime(800), new DateTime(1000)));
        DNFClause dnfClause = new DNFClause();
        dnfClause.withEndBefore(new DateTime(1000));

        // WHEN
        Collection<TimeTrackingItem> result = sut.queryItems(dnfClause);

        // THEN
        assertThat(result, CoreMatchers.<Collection<TimeTrackingItem>>is(Collections.singletonList(expectedResult)));
    }

    @Test
    public void shouldReturnItemOnDay() throws SQLException {
    	assumeThat(connectionProvider.getOpenConnectionCount(), is(Matchers.greaterThanOrEqualTo(1)));
    	
        // GIVEN
        TimeTrackingItem expectedResult = new TimeTrackingItem(null, new DateTime(2015, 1, 3, 1, 1), new DateTime(2015, 1, 3, 3, 3));
        givenDBContains(expectedResult, new TimeTrackingItem(null, new DateTime(800), new DateTime(1000)));
        DNFClause dnfClause = new DNFClause();
        dnfClause.withPeriodAtDay(new LocalDate(2015, 1, 3));

        // WHEN
        Collection<TimeTrackingItem> result = sut.queryItems(dnfClause);


        // THEN
        assertThat(result, CoreMatchers.<Collection<TimeTrackingItem>>is(Collections.singletonList(expectedResult)));
    }


    private Collection<DateTime> mapItemToStartDateTime(Collection<TimeTrackingItem> items) {
        ArrayList<DateTime> result = new ArrayList<>();
        for (TimeTrackingItem item: items) {
            result.add(item.getStart());
        }
        return result;
    }
}

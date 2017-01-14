package org.stt.model;

import org.joda.time.DateTime;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

import java.util.Optional;

public class TimeTrackingItemTest {

	@Test
	public void shouldCreateItemWithoutEnd() {
		// GIVEN

		// WHEN
		TimeTrackingItem sut = new TimeTrackingItem(null, DateTime.now());

		// THEN
		assertThat(sut.getEnd().isPresent(), is(false));
	}

	@Test
	public void shouldCreateItemWithStartIfEndIsMissing() {
		// GIVEN

		// WHEN
		TimeTrackingItem sut = new TimeTrackingItem(null, DateTime.now());

		// THEN
		assertThat(sut.getStart(), notNullValue());
	}

	@Test
	public void shouldCreateItemWithStart() {
		// GIVEN

		// WHEN
		TimeTrackingItem sut = new TimeTrackingItem(null, DateTime.now(),
				DateTime.now().plusMinutes(1));

		// THEN
		assertThat(sut.getStart(), notNullValue());
	}

	@Test
	public void withEndShouldCreateNewItem() {
		// GIVEN
		TimeTrackingItem sut = new TimeTrackingItem(null, DateTime.now());

		// WHEN
		DateTime newEndTime = DateTime.now().withMillisOfSecond(0).plusMinutes(2);
		TimeTrackingItem newItem = sut.withEnd(newEndTime);

		// THEN
		assertThat(newItem, not(is(sut)));
		assertThat(sut.getEnd().isPresent(), is(false));
		assertThat(newItem.getEnd().get(), is(newEndTime));
	}

	@Test
	public void withStartShouldCreateNewItem() {
		// GIVEN
		TimeTrackingItem sut = new TimeTrackingItem(null, DateTime.now());

		// WHEN
		DateTime newStartTime = DateTime.now().withMillisOfSecond(0).plusMinutes(2);
		TimeTrackingItem newItem = sut.withStart(newStartTime);

		// THEN
		assertThat(newItem, not(is(sut)));
		assertThat(newItem.getStart(), is(newStartTime));

	}
	
	@Test
	public void shouldRoundStartToNearestSecond()
	{
		// GIVEN
		
		// WHEN
		TimeTrackingItem sut = new TimeTrackingItem(null, new DateTime(2012, 10, 13, 10, 12, 33, 231));
		
		// THEN
		assertThat(sut.getStart(), is(equalTo(new DateTime(2012, 10, 13, 10, 12, 33))));
	}
	
	@Test
	public void shouldRoundEndToNearestSecond()
	{
		// GIVEN
		
		// WHEN
		TimeTrackingItem sut = new TimeTrackingItem(null, new DateTime(2012, 10, 13, 10, 12), new DateTime(2012, 10, 13, 11, 12, 43, 173));
		
		// THEN
		assertThat(sut.getEnd().get(), is(equalTo(new DateTime(2012, 10, 13, 11, 12, 43))));
	}
}

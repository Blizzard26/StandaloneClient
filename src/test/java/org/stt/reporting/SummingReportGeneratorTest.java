package org.stt.reporting;

import java.util.List;

import org.hamcrest.Matchers;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.stt.model.ReportingItem;
import org.stt.model.TimeTrackingItem;
import org.stt.persistence.ItemReader;

import com.google.common.base.Optional;

public class SummingReportGeneratorTest {

	@Test
	@SuppressWarnings("unchecked")
	public void groupingByCommentWorks() {

		// GIVEN
		TimeTrackingItem expectedItem = new TimeTrackingItem("first comment",
				new DateTime(2012, 12, 12, 14, 14, 14), new DateTime(2012, 12,
						12, 14, 15, 14));
		TimeTrackingItem expectedItem2 = new TimeTrackingItem("first comment",
				new DateTime(2012, 12, 12, 14, 15, 14), new DateTime(2012, 12,
						12, 14, 15, 16));
		TimeTrackingItem expectedItem3 = new TimeTrackingItem("first comment?",
				new DateTime(2012, 12, 12, 14, 15, 14), new DateTime(2012, 12,
						12, 14, 15, 17));

		ItemReader reader = Mockito.mock(ItemReader.class);
		Mockito.when(reader.read()).thenReturn(Optional.of(expectedItem),
				Optional.of(expectedItem2), Optional.of(expectedItem3),
				Optional.<TimeTrackingItem> absent());

		SummingReportGenerator generator = new SummingReportGenerator(reader);

		// WHEN
		List<ReportingItem> report = generator.report();

		// THEN
		Assert.assertThat(report, Matchers.containsInAnyOrder(
				new ReportingItem(new Duration(60 * 1000 + 2 * 1000),
						"first comment"), new ReportingItem(new Duration(
						3 * 1000), "first comment?")));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void nullCommentsGetHandledWell() {

		// GIVEN
		TimeTrackingItem expectedItem = new TimeTrackingItem(null,
				new DateTime(2012, 12, 12, 14, 14, 14), new DateTime(2012, 12,
						12, 14, 15, 14));
		TimeTrackingItem expectedItem2 = new TimeTrackingItem(null,
				new DateTime(2012, 12, 12, 14, 15, 14), new DateTime(2012, 12,
						12, 14, 15, 16));

		ItemReader reader = Mockito.mock(ItemReader.class);
		Mockito.when(reader.read()).thenReturn(Optional.of(expectedItem),
				Optional.of(expectedItem2),
				Optional.<TimeTrackingItem> absent());

		SummingReportGenerator generator = new SummingReportGenerator(reader);

		// WHEN
		List<ReportingItem> report = generator.report();

		// THEN
		Assert.assertThat(report, Matchers
				.containsInAnyOrder(new ReportingItem(new Duration(
						60 * 1000 + 2 * 1000), "")));
	}
}
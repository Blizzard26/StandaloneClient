package org.stt.importer;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.is;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;

import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;
import org.stt.model.TimeTrackingItem;

@RunWith(Theories.class)
public class STTItemExporterTest {

	private static final String LINE_SEPERATOR = System
			.getProperty("line.separator");
	private StringWriter stringWriter;
	private STTItemExporter sut;

	@DataPoints
	public static DateTime[] sampleDateTimes = new DateTime[] {
			new DateTime(2011, 10, 10, 11, 12, 13),
			new DateTime(2010, 10, 10, 11, 12, 13),
			new DateTime(2012, 10, 10, 11, 12, 13) };

	@Before
	public void setUp() {
		stringWriter = new StringWriter();

		StreamResourceProvider provider = new StreamResourceProvider() {

			@Override
			public Writer provideTruncatingWriter() throws IOException {
				stringWriter = new StringWriter();
				return stringWriter;
			}

			@Override
			public Reader provideReader() throws FileNotFoundException {
				return new StringReader(stringWriter.toString());
			}

			@Override
			public Writer provideAppendingWriter() throws IOException {
				return stringWriter;
			}

			@Override
			public void close() {
			}
		};

		sut = new STTItemExporter(provider);
	}

	@Test(expected = NullPointerException.class)
	public void writeNullObjectFails() throws IOException {

		// WHEN
		sut.write(null);

		// THEN
		// Exception expected
	}

	@Test
	public void writeCommentSucceeds() throws IOException {

		// GIVEN
		TimeTrackingItem theItem = new TimeTrackingItem("the comment",
				DateTime.now());

		// WHEN
		sut.write(theItem);

		// THEN
		Assert.assertThat(stringWriter.toString(),
				containsString("the comment"));
	}

	@Test
	public void writeStartSucceeds() throws IOException {

		// GIVEN
		DateTime theTime = new DateTime(2011, 10, 12, 13, 14, 15);
		TimeTrackingItem theItem = new TimeTrackingItem(null, theTime);

		// WHEN
		sut.write(theItem);

		// THEN
		Assert.assertThat(stringWriter.toString(),
				containsString("2011-10-12_13:14:15"));
	}

	@Test
	public void writeEndSucceeds() throws IOException {

		// GIVEN
		DateTime start = new DateTime(2011, 10, 12, 13, 14, 15);
		DateTime end = new DateTime(2012, 10, 12, 13, 14, 15);

		TimeTrackingItem theItem = new TimeTrackingItem(null, start, end);

		// WHEN
		sut.write(theItem);

		// THEN
		Assert.assertThat(stringWriter.toString(),
				containsString("2012-10-12_13:14:15"));
	}

	@Test
	public void writeCompleteEntrySucceeds() throws IOException {

		// GIVEN
		DateTime start = new DateTime(2011, 10, 12, 13, 14, 15);

		DateTime end = new DateTime(2012, 10, 12, 13, 14, 15);
		TimeTrackingItem theItem = new TimeTrackingItem("the comment", start,
				end);

		// WHEN
		sut.write(theItem);

		// THEN
		Assert.assertThat(
				stringWriter.toString(),
				containsString("2011-10-12_13:14:15 2012-10-12_13:14:15 the comment"));
	}

	@Test
	public void writeMultiLineEntrySucceeds() throws IOException {

		// GIVEN
		TimeTrackingItem theItem = new TimeTrackingItem(
				"this is\n a multiline\r string\r\n with different separators",
				DateTime.now());

		// WHEN
		sut.write(theItem);

		// THEN
		Assert.assertThat(
				stringWriter.toString(),
				endsWith("this is\\n a multiline\\r string\\r\\n with different separators"
						+ LINE_SEPERATOR));

	}

	@Test
	public void itemsCanBeDeleted() throws IOException {

		// GIVEN
		TimeTrackingItem theItem = new TimeTrackingItem("testitem",
				new DateTime(2011, 10, 10, 11, 12, 13));
		TimeTrackingItem theItem2 = new TimeTrackingItem("testitem",
				new DateTime(2014, 10, 10, 11, 12, 13));
		sut.write(theItem);
		sut.write(theItem2);

		// when
		sut.delete(theItem2);

		// then
		Assert.assertThat(stringWriter.toString(),
				is("2011-10-10_11:12:13 2014-10-10_11:12:13 testitem"
						+ LINE_SEPERATOR));
	}

	@Test
	public void itemCanBeReplaced() throws IOException {

		// GIVEN
		TimeTrackingItem theItem = new TimeTrackingItem("testitem",
				new DateTime(2011, 10, 10, 11, 12, 13));
		TimeTrackingItem theItem2 = new TimeTrackingItem("testitem",
				DateTime.now());
		sut.write(theItem2);

		// when
		sut.replace(theItem2, theItem);

		// then
		Assert.assertThat(stringWriter.toString(),
				is("2011-10-10_11:12:13 testitem" + LINE_SEPERATOR));
	}

	@Test
	public void shouldWriteItemsWithMultipleWhitespaces() throws IOException {
		// GIVEN
		TimeTrackingItem theItem = new TimeTrackingItem("item with 2  spaces",
				new DateTime(2011, 10, 10, 11, 12, 13));

		// when
		sut.write(theItem);

		// then
		Assert.assertThat(stringWriter.toString(),
				is("2011-10-10_11:12:13 item with 2  spaces" + LINE_SEPERATOR));
	}

	@Theory
	public void shouldRemoveCoveredTimeIntervalsIfNewItemHasNoEnd(
			DateTime startOfNewItem) throws IOException {
		DateTime startOfExistingItem = new DateTime(2011, 10, 10, 11, 12, 13);

		Assume.assumeFalse(startOfNewItem.isAfter(startOfExistingItem));
		// GIVEN
		TimeTrackingItem existingItem = new TimeTrackingItem("testitem",
				startOfExistingItem);
		sut.write(existingItem);

		TimeTrackingItem newItem = new TimeTrackingItem("testitem2",
				startOfExistingItem);

		// WHEN
		sut.write(newItem);

		// THEN
		Assert.assertThat(stringWriter.toString(),
				is("2011-10-10_11:12:13 testitem2" + LINE_SEPERATOR));
	}

	@Test
	public void shouldSetEndTimeIfNewItemIsStarted() throws IOException {
		// GIVEN
		TimeTrackingItem existingItem = new TimeTrackingItem("testitem",
				new DateTime(2011, 10, 10, 11, 12, 13));
		sut.write(existingItem);

		TimeTrackingItem newItem = new TimeTrackingItem("testitem2",
				new DateTime(2011, 10, 10, 11, 12, 14));

		// WHEN
		sut.write(newItem);

		// THEN
		Assert.assertThat(stringWriter.toString(),
				is("2011-10-10_11:12:13 2011-10-10_11:12:14 testitem"
						+ LINE_SEPERATOR + "2011-10-10_11:12:14 testitem2"
						+ LINE_SEPERATOR));

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
		sut.write(existingItem);

		TimeTrackingItem newItem = new TimeTrackingItem("new item",
				startOfExistingItem, endOfNewItem);

		// WHEN
		sut.write(newItem);

		// THEN
		Assert.assertThat(stringWriter.toString(),
				is("2011-10-10_11:12:13 2020-10-10_11:12:13 new item"
						+ LINE_SEPERATOR));
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
		sut.write(existingItem);

		TimeTrackingItem newItem = new TimeTrackingItem("new item",
				startOfExistingItem, endOfNewItem);

		// WHEN
		sut.write(newItem);

		// THEN
		Assert.assertThat(
				stringWriter.toString(),
				is("2011-10-10_11:12:13 2020-10-10_11:12:13 new item"
						+ LINE_SEPERATOR
						+ "2020-10-10_11:12:13 2020-10-10_11:13:13 existing item"
						+ LINE_SEPERATOR));
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
		sut.write(existingItem);

		TimeTrackingItem newItem = new TimeTrackingItem("new item",
				startOfExistingItem, endOfNewItem);

		// WHEN
		sut.write(newItem);

		// THEN
		Assert.assertThat(stringWriter.toString(),
				is("2011-10-10_11:12:13 2020-10-10_11:12:13 new item"
						+ LINE_SEPERATOR + "2020-10-10_11:12:13 existing item"
						+ LINE_SEPERATOR));
	}

	@Test
	public void shouldChangeEndOfIntervalBeforeRemoveOverlappingIntervalAndChangeStartOfIntervalAfter()
			throws IOException {
		// GIVEN
		TimeTrackingItem itemBefore = new TimeTrackingItem("Item before",
				new DateTime(2020, 10, 10, 11, 12, 13), new DateTime(2020, 10,
						10, 11, 14, 13));
		sut.write(itemBefore);
		TimeTrackingItem overlappedItem = new TimeTrackingItem(
				"Overlapped item", new DateTime(2020, 10, 10, 11, 14, 13),
				new DateTime(2020, 10, 10, 11, 15, 13));
		sut.write(overlappedItem);
		TimeTrackingItem itemAfter = new TimeTrackingItem("Item after",
				new DateTime(2020, 10, 10, 11, 15, 13), new DateTime(2020, 10,
						10, 11, 17, 13));
		sut.write(itemAfter);
		TimeTrackingItem itemAfterAfter = new TimeTrackingItem(
				"Item even after", new DateTime(2020, 10, 10, 11, 17, 13),
				new DateTime(2020, 10, 10, 11, 19, 13));
		sut.write(itemAfterAfter);

		TimeTrackingItem newItem = new TimeTrackingItem("new item",
				new DateTime(2020, 10, 10, 11, 13, 13), new DateTime(2020, 10,
						10, 11, 16, 13));

		// WHEN
		sut.write(newItem);

		// THEN

		Assert.assertThat(
				stringWriter.toString(),
				is("2020-10-10_11:12:13 2020-10-10_11:13:13 Item before"
						+ LINE_SEPERATOR
						+ "2020-10-10_11:13:13 2020-10-10_11:16:13 new item"
						+ LINE_SEPERATOR
						+ "2020-10-10_11:16:13 2020-10-10_11:17:13 Item after"
						+ LINE_SEPERATOR
						+ "2020-10-10_11:17:13 2020-10-10_11:19:13 Item even after"
						+ LINE_SEPERATOR));
	}
}

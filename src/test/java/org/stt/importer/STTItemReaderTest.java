package org.stt.importer;

import com.google.common.base.Optional;
import org.hamcrest.Matchers;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Test;
import org.stt.model.TimeTrackingItem;
import org.stt.persistence.ItemReader;
import org.stt.persistence.stt.STTItemReader;

import java.io.StringReader;

public class STTItemReaderTest {

	@Test
	public void multiLineCommentGetsImportedCorrectly() {

		// GIVEN
		StringReader stringReader = new StringReader(
				"2012-10-10_22:00:00 2012-11-10_22:00:01 this is\\n a multiline\\r string\\r\\n with different separators");
		ItemReader theReader = new STTItemReader(stringReader);

		// WHEN
		Optional<TimeTrackingItem> readItem = theReader.read();

		// THEN
		Assert.assertEquals(
				"this is\n a multiline\r string\r\n with different separators",
				readItem.get().getComment().get());
	}

	@Test
	public void onlyStartTimeGiven() {

		// GIVEN
		StringReader stringReader = new StringReader("2012-10-10_22:00:00");
		ItemReader theReader = new STTItemReader(stringReader);

		// WHEN
		Optional<TimeTrackingItem> readItem = theReader.read();

		// THEN
		DateTime time = new DateTime(2012, 10, 10, 22, 00, 00);
		Assert.assertThat(time, Matchers.equalTo(readItem.get().getStart()));
	}

	@Test
	public void startTimeAndCommentGiven() {

		// GIVEN
		StringReader stringReader = new StringReader(
				"2012-10-10_22:00:00 the long comment");
		ItemReader theReader = new STTItemReader(stringReader);

		// WHEN
		Optional<TimeTrackingItem> readItem = theReader.read();

		// THEN
		DateTime time = new DateTime(2012, 10, 10, 22, 00, 00);
		Assert.assertThat(time, Matchers.equalTo(readItem.get().getStart()));
		Assert.assertThat("the long comment",
				Matchers.equalTo(readItem.get().getComment().get()));
	}
}

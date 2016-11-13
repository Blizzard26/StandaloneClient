package org.stt.text;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.stt.Configuration;
import org.stt.ItemReaderTestHelper;
import org.stt.model.TimeTrackingItem;
import org.stt.persistence.ItemReader;
import static org.mockito.BDDMockito.*;

public class SplitItemGrouperTest {
	
	@Mock
	Configuration configuration;
	
	private SplitItemGrouper sut;

	@Mock
	private ItemReader itemReader;

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		given(configuration.getSplitItemGrouperRegularExpression()).willReturn("\\s(:|\\||-)\\s");
		sut = new SplitItemGrouper(configuration);
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void shouldMatchSimpleGroup() {
		// GIVEN

		// WHEN
		List<String> result = sut.getGroupsOf("test");

		// THEN
		assertThat(result, is(Arrays.asList("test")));
	}

	@Test
	public void shouldMatchStringWithDash() {
		// GIVEN

		// WHEN
		String completeText = "test with - dash string";
		List<String> result = sut.getGroupsOf(completeText);

		// THEN
		assertThat(result, is(Arrays.asList("test with", "dash string")));
	}
	
	@Test
	public void shouldMatchStringWithPipe() {
		// GIVEN

		// WHEN
		String completeText = "test with | pipe string";
		List<String> result = sut.getGroupsOf(completeText);

		// THEN
		assertThat(result, is(Arrays.asList("test with", "pipe string")));
	}
	
	
	@Test
	public void shouldMatchStringWithColon() {
		// GIVEN

		// WHEN
		String completeText = "test with : colon string";
		List<String> result = sut.getGroupsOf(completeText);

		// THEN
		assertThat(result, is(Arrays.asList("test with", "colon string")));
	}
	
	@Test
	public void shouldHandleGroupsWithLongerTextThanGivenComment() {
		// GIVEN
		givenReaderReturnsItemsWithComment("test");
		sut.scanForGroups(itemReader);

		// WHEN
		List<String> result = sut.getGroupsOf("t");

		// THEN
		assertThat(result, is(Arrays.asList("t")));
	}

	@Test
	public void shouldFindGroupsWithSpaces() {
		// GIVEN
		String firstComment = "group subgroup -  one";
		givenReaderReturnsItemsWithComment(firstComment, "group subgroup | two");
		sut.scanForGroups(itemReader);

		// WHEN
		List<String> result = sut.getGroupsOf(firstComment);

		// THEN
		assertThat(result, is(Arrays.asList("group subgroup", "one")));

	}

	@Test
	public void shouldFindSubGroups() {
		// GIVEN
		String firstComment = "group : subgroup - one";
		String thirdComment = "group - subgroup2 one";
		givenReaderReturnsItemsWithComment(firstComment, "group - subgroup | two",
				thirdComment);
		sut.scanForGroups(itemReader);

		// WHEN
		List<String> withThreeGroups = sut.getGroupsOf(firstComment);
		List<String> withTwoGroups = sut.getGroupsOf(thirdComment);

		// THEN
		assertThat(withThreeGroups,
				is(Arrays.asList("group", "subgroup", "one")));
		assertThat(withTwoGroups, is(Arrays.asList("group", "subgroup2 one")));
	}

	@Test
	public void shouldFindLongestCommonPrefix() {
		// GIVEN
		String firstComment = "group - one";
		givenReaderReturnsItemsWithComment(firstComment, "group | two");
		sut.scanForGroups(itemReader);

		// WHEN
		List<String> groups = sut.getGroupsOf(firstComment);

		// THEN
		assertThat(groups, is(Arrays.asList("group", "one")));

	}

	@Test
	public void shouldFindGroups() {
		// GIVEN
		String firstComment = "group";
		givenReaderReturnsItemsWithComment(firstComment, firstComment);
		sut.scanForGroups(itemReader);

		// WHEN
		List<String> groups = sut.getGroupsOf(firstComment);

		// THEN
		assertThat(groups, is(Arrays.asList(firstComment)));
	}

	@Test
	public void shouldCutGroupAtShorterItem()
	{
		// GIVEN
		sut.learnGroup("aaaa");
		sut.learnGroup("aaaa - bbbb");
		sut.learnGroup("aaaa - bbbb - cccc");

		// WHEN
		List<String> result = sut.getGroupsOf("aaaa - bbbb | cccc - dddd");

		// THEN
		assertThat(result, is(Arrays.asList("aaaa", "bbbb", "cccc", "dddd")));
	}

	private TimeTrackingItem[] givenReaderReturnsItemsWithComment(
			String... comments) {
		TimeTrackingItem items[] = new TimeTrackingItem[comments.length];
		for (int i = 0; i < comments.length; i++) {
			items[i] = new TimeTrackingItem(comments[i], DateTime.now());
		}
		ItemReaderTestHelper.givenReaderReturns(itemReader, items);
		return items;
	}
}

package org.stt.text;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.given;

import java.util.Collection;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.stt.connector.jira.JiraConnector;
import org.stt.gui.jfx.text.AutoComplete.Match;
import org.stt.text.JiraExpansionProvider;

import com.atlassian.jira.rest.client.api.domain.Issue;

public class JiraExpansionProviderTest {
	
	@Mock
	JiraConnector jiraConnector;
	
	@Mock
	Issue issue;

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		
		given(jiraConnector.getIssue("JRA-7")).willReturn(issue);
		given(issue.getDescription()).willReturn("BDA-7: Testing Issue");
		
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testGetMatches() {
		JiraExpansionProvider sut = new JiraExpansionProvider(jiraConnector);
		
		String text = "Testing JRA-7 replacement";
		Collection<Match> matches = sut.getMatches(text, 13);
		
		assertEquals(1, matches.size());
		
		Match match = matches.iterator().next();
		
		assertEquals("Testing BDA-7: Testing Issue replacement", match.apply(text));
	}

}

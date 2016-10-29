package org.stt.text;

import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.given;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.stt.connector.jira.JiraConnector;

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
		given(issue.getSummary()).willReturn("Testing Issue");
		
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testGetPossibleExpansions() {
		JiraExpansionProvider sut = new JiraExpansionProvider(jiraConnector);
		
		List<String> matches = sut.getPossibleExpansions("JRA-7");
		
		assertEquals(1, matches.size());
		
		assertEquals(": Testing Issue", matches.get(0));
	}

}

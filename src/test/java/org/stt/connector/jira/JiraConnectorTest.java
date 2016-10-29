package org.stt.connector.jira;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;

import java.net.URI;
import java.util.Collection;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.stt.Configuration;

import com.atlassian.jira.rest.client.api.domain.Issue;

public class JiraConnectorTest {

	@Mock
	private Configuration configuration;
	
	private JiraConnector sut;
	
	@Before
	public void setUp() throws Exception {
		
		MockitoAnnotations.initMocks(this);
		
		given(configuration.getJiraURI()).willReturn(new URI("https://jira.atlassian.com/"));
		given(configuration.getJiraUserName()).willReturn("");
		
		sut = new JiraConnector(configuration);
	}

	@After
	public void tearDown() throws Exception {
		sut.stop();
	}

	@Test
	public void testConnection() throws Exception {		
		sut.start();
		
		Set<String> prefixes = sut.getProjectNames();
		
		assertNotNull(prefixes);
		//System.out.println(prefixes);
	}
	
	@Test
	public void testGetIssue() throws Exception
	{
		sut.start();
		
		Issue issue = sut.getIssue("JRA-1");
		
		assertEquals("JRA-1", issue.getKey());
	}
	
}

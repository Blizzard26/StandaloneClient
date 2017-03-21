package org.stt.connector.jira;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.stt.config.JiraConfig;

import net.rcarz.jiraclient.ICredentials;
import net.rcarz.jiraclient.Issue;
import net.rcarz.jiraclient.JiraClient;
import net.rcarz.jiraclient.Project;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;

public class JiraConnectorTest {

	private JiraConnector sut;

	private JiraConfig configuration = new JiraConfig();
	
	@Mock
	ICredentials credentials;
	
	@Mock
	JiraClient jiraClient;
	
	@Before
	public void setUp() throws Exception {
		
		MockitoAnnotations.initMocks(this);

		configuration.setJiraURI(new URI("https://jira.atlassian.com/"));
		configuration.setJiraUsername("");		
		
		sut = Mockito.spy(new JiraConnector(configuration));
		doReturn(credentials).when(sut).createCredentials(eq(configuration));
		doReturn(jiraClient).when(sut).createJiraClient(any(URI.class), eq(credentials));
	}

	@After
	public void tearDown() throws Exception {
		sut.stop();
	}

	@Test
	public void testConnection() throws Exception {		
		// GIVEN
		sut.start();
		
		Project p1 = Mockito.mock(Project.class);
		given(p1.getKey()).willReturn("Project1Key");
		Project p2 = Mockito.mock(Project.class);
		given(p2.getKey()).willReturn("Project2Key");
		
		List<Project> projectList = new ArrayList<>();
		projectList.add(p1);
		projectList.add(p2);
		
		given(jiraClient.getProjects()).willReturn(projectList);
		
		// WHEN
		Set<String> prefixes = sut.getProjectNames();
		
		// THEN
		assertNotNull(prefixes);
		assertEquals(2, prefixes.size());
		assertTrue(prefixes.contains("Project1Key"));
		assertTrue(prefixes.contains("Project2Key"));
	}
	
	@Test
	public void testGetIssue() throws Exception
	{
		// GIVEN
		sut.start();
		
		Project project = Mockito.mock(Project.class);
		given(project.getKey()).willReturn("JRA");
		
		List<Project> projectList = new ArrayList<>();
		projectList.add(project);
		
		given(jiraClient.getProjects()).willReturn(projectList);
		
		Issue issueInject = Mockito.mock(Issue.class);
		
		given(jiraClient.getIssue("JRA-1")).willReturn(issueInject);
		
		// WHEN
		Optional<Issue> issue = sut.getIssue("JRA-1");
		
		// THEN
		assertTrue(issue.isPresent());
		assertEquals(issueInject, issue.get());
	}
	
}

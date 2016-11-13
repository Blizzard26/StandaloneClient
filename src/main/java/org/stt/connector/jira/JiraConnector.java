package org.stt.connector.jira;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.stt.Configuration;
import org.stt.Service;

import com.atlassian.jira.rest.client.api.AuthenticationHandler;
import com.atlassian.jira.rest.client.api.IssueRestClient;
import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.api.JiraRestClientFactory;
import com.atlassian.jira.rest.client.api.ProjectRestClient;
import com.atlassian.jira.rest.client.api.RestClientException;
import com.atlassian.jira.rest.client.api.SearchRestClient;
import com.atlassian.jira.rest.client.api.domain.BasicProject;
import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.api.domain.SearchResult;
import com.atlassian.jira.rest.client.auth.AnonymousAuthenticationHandler;
import com.atlassian.jira.rest.client.auth.BasicHttpAuthenticationHandler;
import com.atlassian.jira.rest.client.internal.async.AsynchronousJiraRestClientFactory;
import com.atlassian.util.concurrent.Promise;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class JiraConnector implements Service {
	
	private static final Logger LOG = Logger.getLogger(JiraConnector.class.getName());
	

	JiraRestClient client = null;
	private Set<String> projectsCache;
	private Configuration configuration;
	private JiraRestClientFactory factory;
	
	@Inject public JiraConnector(Configuration configuration) {
		this(configuration, new AsynchronousJiraRestClientFactory());
	}
	
	public JiraConnector(Configuration configuration, JiraRestClientFactory factory)
	{
		this.configuration = configuration;
		this.factory = factory;
	}


	@Override
	public void start() throws Exception {

		URI jiraURI = configuration.getJiraURI();
		
		if (jiraURI != null)
		{
			AuthenticationHandler authenticationHandler;
			
			String jiraUserName = configuration.getJiraUserName();
			if (jiraUserName != null && jiraUserName.length() > 0)
			{
				authenticationHandler = new BasicHttpAuthenticationHandler(jiraUserName, configuration.getJiraPassword());
			}
			else
			{
				authenticationHandler = new AnonymousAuthenticationHandler();
			}
			
			client = factory.create(jiraURI, authenticationHandler);
		}
	}

	@Override
	public void stop() {
		try {
			if (client != null)
				client.close();
			client = null;
		} catch (IOException e) {
			LOG.log(Level.WARNING, "Exception while closing client connection", e);
		}
	}
	
	public Issue getIssue(String issueKey)
	{	
		if (client != null)
		{
			try 
			{
				IssueRestClient issueClient = client.getIssueClient();
				Promise<Issue> issue = issueClient.getIssue(issueKey);
				
				Issue jiraIssue = issue.get(5000, TimeUnit.MILLISECONDS);
				
				return jiraIssue;
			} 
			catch (InterruptedException e) 
			{
				LOG.log(Level.WARNING, "Exception while retrieving issue", e);
				return null;
			} catch (ExecutionException e) {
				//LOG.log(Level.WARNING, "Exception while retrieving issue", e);
				return null;
			} catch (RestClientException e) {
				//LOG.log(Level.WARNING, "Exception while retrieving issue", e);
				return null;
			} catch (TimeoutException e) {
				LOG.log(Level.WARNING, "Timeout while retrieving issue", e);
				return null;
			}
		}
		else
		{
			return null;
		}
	}
	
	public Collection<Issue> getIssues(String issueKeyPrefix) 
	{
		List<Issue> resultList = new ArrayList<>();
		
		if (client != null)
		{
			int index = issueKeyPrefix.lastIndexOf('-');
			
			// Extract the project key
			String projectKey;
			if (index > 0)
			{
				projectKey = issueKeyPrefix.substring(0, index);
			}
			else
			{
				projectKey = issueKeyPrefix;
			}
			
			// Check if the given project key belongs to an existing project
			if (!getProjectNames().contains(projectKey))
			{
				return Collections.emptyList();
			}
			
			// Get all issue of the project
			SearchRestClient searchClient = client.getSearchClient();
			Promise<SearchResult> searchResultPromise = searchClient.searchJql("project=\"" + projectKey + "\"", Integer.MAX_VALUE, 0, null);
			
			SearchResult searchResult;
			try 
			{
				searchResult = searchResultPromise.get(30, TimeUnit.SECONDS);
			} 
			catch (InterruptedException | ExecutionException | TimeoutException e) 
			{
				LOG.log(Level.WARNING, "Exception while retrieving issues", e);
				return Collections.emptyList();
			}
			
			Iterable<Issue> issues = searchResult.getIssues();
			
			// Select all issues which start with the given prefix
			
			for (Issue issue : issues)
			{
				if (issue.getKey().startsWith(issueKeyPrefix))
				{
					resultList.add(issue);
				}
			}
		}
		
		return resultList;
	}
	
	public Set<String> getProjectNames()
	{
		if (projectsCache == null)
		{
			projectsCache = internalGetProjectNames();
		}
		return projectsCache;
	}

	private Set<String> internalGetProjectNames() 
	{
		Set<String> projects = new HashSet<>();
		if (client != null)
		{
			ProjectRestClient projectClient = client.getProjectClient();
			
			Promise<Iterable<BasicProject>> projectsPromise = projectClient.getAllProjects();
			
			Iterable<BasicProject> projectsIterable;
			try 
			{
				projectsIterable = projectsPromise.get(5000, TimeUnit.MILLISECONDS);
			} 
			catch (InterruptedException | ExecutionException | TimeoutException e) 
			{
				LOG.log(Level.WARNING, "Exception while retrieving projects", e);
				return Collections.emptySet();
			}
			
			for (BasicProject project : projectsIterable)
			{
				projects.add(project.getKey());
			}
		}
		return projects;
	}

}

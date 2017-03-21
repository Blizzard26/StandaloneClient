package org.stt.connector.jira;

import static java.util.Objects.requireNonNull;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.stt.Service;
import org.stt.config.JiraConfig;

import net.rcarz.jiraclient.BasicCredentials;
import net.rcarz.jiraclient.ICredentials;
import net.rcarz.jiraclient.Issue;
import net.rcarz.jiraclient.Issue.SearchResult;
import net.rcarz.jiraclient.JiraClient;
import net.rcarz.jiraclient.JiraException;
import net.rcarz.jiraclient.Project;


@Singleton
public class JiraConnector implements Service {

    private static final Logger LOG = Logger.getLogger(JiraConnector.class.getName());


    JiraClient client = null;
    private Set<String> projectsCache;
    private JiraConfig configuration;

    @Inject
    public JiraConnector(JiraConfig configuration) {
        this.configuration = requireNonNull(configuration);
    }


    @Override
    public void start() throws Exception {

        URI jiraURI = configuration.getJiraURI();

        if (jiraURI != null) {
        	ICredentials credetials = createCredentials(configuration);

			client = createJiraClient(jiraURI, credetials);
        }
    }


	protected JiraClient createJiraClient(URI jiraURI, ICredentials credetials) throws JiraException {
		return new JiraClient(jiraURI.toString(), credetials);
	}


	protected ICredentials createCredentials(JiraConfig configuration) {
		ICredentials credetials;

		String jiraUserName = configuration.getJiraUsername();
		if (jiraUserName != null && jiraUserName.length() > 0) {
			credetials = new BasicCredentials(jiraUserName, configuration.getJiraPassword());
		} else {
		    //authenticationHandler = new AnonymousAuthenticationHandler();
			// fixme
			credetials = null;
		}
		return credetials;
	}

    @Override
    public void stop() {
        client = null;
    }

    public Optional<Issue> getIssue(String issueKey) throws JiraConnectorException {
        if (client != null) {
            String projectKey = getProjectKey(issueKey);

            // Check if the given project key belongs to an existing project
            if (!projectExists(projectKey)) {
                return Optional.empty();
            }

            try {
            	Issue jiraIssue = client.getIssue(issueKey);

                return Optional.of(jiraIssue);
            } catch (JiraException e) {
                handleJiraException(e);
                return Optional.empty();
            }
        } else {
            return Optional.empty();
        }
    }

    private void handleJiraException(JiraException e) throws JiraConnectorException {
        LOG.log(Level.WARNING, "RestClientException while retrieving issue", e);
        // TODO: How to determine the kind of issue?
        throw new JiraConnectorException("Jira Connector returned exception!", e);
    }

    public Collection<Issue> getIssues(String issueKeyPrefix) throws JiraConnectorException {
        List<Issue> resultList = new ArrayList<>();

        if (client != null) {
            String projectKey = getProjectKey(issueKeyPrefix);

            // Check if the given project key belongs to an existing project
            if (!projectExists(projectKey)) {
                return Collections.emptyList();
            }

            // Get all issue of the project
            SearchResult searchResult;
            try {
				searchResult = client.searchIssues("project=\"" + projectKey + "\"");
            } catch (JiraException e) {
                handleJiraException(e);
                return Collections.emptyList();
            }

            Iterable<Issue> issues = searchResult.issues;

            // Select all issues which start with the given prefix

            for (Issue issue : issues) {
                if (issue.getKey().startsWith(issueKeyPrefix)) {
                    resultList.add(issue);
                }
            }
        }

        return resultList;
    }

    private boolean projectExists(String projectKey) throws JiraConnectorException {
        return getProjectNames().contains(projectKey);
    }

    private String getProjectKey(String issueKey) {
        int index = issueKey.lastIndexOf('-');

        // Extract the project key
        String projectKey;
        if (index > 0) {
            projectKey = issueKey.substring(0, index);
        } else {
            projectKey = issueKey;
        }
        return projectKey;
    }

    public Set<String> getProjectNames() throws JiraConnectorException {
        if (projectsCache == null) {
            projectsCache = internalGetProjectNames();
        }
        return projectsCache;
    }

    private Set<String> internalGetProjectNames() throws JiraConnectorException {
        Set<String> resultSet = new HashSet<>();
        if (client != null) {
            List<Project> projects;
			

            try {
            	projects = client.getProjects();
            } catch (JiraException e) {
                handleJiraException(e);
                return Collections.emptySet();
            }

            for (Project project : projects) {
                resultSet.add(project.getKey());
            }
        }
        return resultSet;
    }

}

package org.stt.text;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import org.stt.connector.jira.JiraConnector;
import org.stt.connector.jira.JiraConnectorException;

import com.atlassian.jira.rest.client.api.domain.Issue;
import com.google.inject.Inject;

public class JiraExpansionProvider implements ExpansionProvider {

    private static final Logger LOG = Logger.getLogger(JiraExpansionProvider.class
            .getName());
	
	private JiraConnector jiraConnector;

	@Inject
	public JiraExpansionProvider(JiraConnector connector) {
		this.jiraConnector = checkNotNull(connector);
	}
	
	@Override
	public List<String> getPossibleExpansions(String text) {
		String queryText = text.trim();
		int spaceIndex = queryText.lastIndexOf(' ');
		if (spaceIndex > 0)
		{
			queryText = text.substring(spaceIndex, text.length()).trim();
		}
		
		Issue issue;
		try {
			issue = jiraConnector.getIssue(queryText);
		} catch (JiraConnectorException e) {
			LOG.severe(e.getLocalizedMessage());
			return Collections.emptyList();
		}
		
		List<String> expansions = new ArrayList<>();
		if (issue != null)
		{
			expansions.add(": " + issue.getSummary());
		}
		
		return expansions;
	}

}

package org.stt.text;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.stt.connector.jira.JiraConnector;
import org.stt.connector.jira.JiraConnectorException;
import org.stt.gui.Notification;

import com.atlassian.jira.rest.client.api.domain.Issue;
import com.google.common.base.Optional;
import com.google.inject.Inject;

public class JiraExpansionProvider implements ExpansionProvider {

	private JiraConnector jiraConnector;
	private Notification notification;

	@Inject
	public JiraExpansionProvider(JiraConnector connector, Notification notification) {
		this.jiraConnector = checkNotNull(connector);
		this.notification = checkNotNull(notification);
	}
	
	@Override
	public List<String> getPossibleExpansions(String text) {
		String queryText = text.trim();
		int spaceIndex = queryText.lastIndexOf(' ');
		if (spaceIndex > 0)
		{
			queryText = text.substring(spaceIndex, text.length()).trim();
		}
		
		Optional<Issue> issue;
		try {
			issue = jiraConnector.getIssue(queryText);
		} catch (JiraConnectorException e) {
			notification.error(e.getLocalizedMessage());
			return Collections.emptyList();
		}
		
		List<String> expansions = new ArrayList<>();
		if (issue.isPresent())
		{
			expansions.add(": " + issue.get().getSummary());
		}
		
		return expansions;
	}

}

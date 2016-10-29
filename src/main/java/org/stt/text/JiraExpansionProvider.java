package org.stt.text;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.List;

import org.stt.connector.jira.JiraConnector;

import com.atlassian.jira.rest.client.api.domain.Issue;
import com.google.inject.Inject;

public class JiraExpansionProvider implements ExpansionProvider {

	private JiraConnector jiraConnector;

	@Inject
	public JiraExpansionProvider(JiraConnector connector) {
		this.jiraConnector = checkNotNull(connector);
	}
	
	@Override
	public List<String> getPossibleExpansions(String text) {
		Issue issue = jiraConnector.getIssue(text);
		
		List<String> expansions = new ArrayList<>();
		if (issue != null)
		{
			expansions.add(": " + issue.getSummary());
		}
		
		return expansions;
	}

}

package org.stt.text;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;

import javax.inject.Inject;

import org.stt.connector.jira.JiraConnector;
import org.stt.connector.jira.JiraConnectorException;

import net.rcarz.jiraclient.Issue;


public class JiraExpansionProvider implements ExpansionProvider {

    private static final Logger LOG = Logger.getLogger(JiraExpansionProvider.class
            .getName());
	
	private JiraConnector jiraConnector;

	@Inject
	public JiraExpansionProvider(JiraConnector connector) {
		this.jiraConnector = Objects.requireNonNull(connector);
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
			LOG.severe(e.getLocalizedMessage());
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

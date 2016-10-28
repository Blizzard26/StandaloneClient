package org.stt.text;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.stt.gui.jfx.text.AutoComplete.Match;
import org.stt.connector.jira.JiraConnector;
import org.stt.gui.jfx.text.AutoCompletionProvider;

import com.atlassian.jira.rest.client.api.domain.Issue;
import com.google.inject.Inject;

public class JiraExpansionProvider implements AutoCompletionProvider, ExpansionProvider {

	private JiraConnector jiraConnector;

	@Inject
	public JiraExpansionProvider(JiraConnector connector) {
		this.jiraConnector = checkNotNull(connector);
	}
	
	@Override
	public Collection<Match> getMatches(String text, int caretPos) {
		int startIndex = text.lastIndexOf(' ', caretPos-1);
		
		if (startIndex < 0)
			startIndex = 0;
		else
			startIndex++;
		
		Set<Match> matches = new HashSet<>();
		
		String searchString = text.substring(startIndex, caretPos);
		
		if (searchString.length() > 0)
		{
			try {
				Issue issue = jiraConnector.getIssue(searchString);
				if (issue != null)
				{
					matches.add(new Match(1, ": " + issue.getSummary(), startIndex, caretPos));
				}
			} catch (Exception e) {
				// TODO log
				e.printStackTrace();
			}
		}
		
		return matches;
	}

	@Override
	public List<String> getPossibleExpansions(String text) {
		Collection<Match> matches = getMatches(text, text.length());
		
		List<String> expansions = new ArrayList<String>();
		for (Match match : matches)
		{
			expansions.add(match.apply(text));
		}
		
		return expansions;
	}

}

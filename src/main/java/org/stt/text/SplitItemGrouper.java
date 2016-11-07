package org.stt.text;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.TreeMap;

import org.stt.model.TimeTrackingItem;
import org.stt.persistence.ItemReader;

import com.google.common.base.Optional;

public class SplitItemGrouper implements ItemGrouper, ExpansionProvider {

	private static final String SPLIT_REGEX = ":|\\||-";
	
	private final NavigableMap<String, Integer> set = new TreeMap<>();

	@Override
	public List<String> getPossibleExpansions(String text) {
		checkNotNull(text);
		
		String[] split = text.split(SPLIT_REGEX);
		String grp = split[split.length - 1].trim();
		
		Entry<String, Integer> nextKey = set.ceilingEntry(grp);
		
		String group = nextKey.getKey();
		if (group.startsWith(grp))
		{
			group = group.substring(grp.length(), group.length());
			return Arrays.asList(group);
		}
		
		return Collections.emptyList();
	}

	@Override
	public List<String> getGroupsOf(String text) {
		String[] split = text.split(SPLIT_REGEX);
		List<String> groups = new ArrayList<String>();
		for (String s : split)
		{
			String group = s.trim();
			groups.add(group);
			learnGroup(group);
		}
		
		return groups;
	}

	public void learnGroup(String group) {
		Integer count = set.get(group);
		if (count == null)
		{
			count = Integer.valueOf(1);
		}
		else
		{
			count = Integer.valueOf(count.intValue() + 1);
		}
		set.put(group, count);
	}

	public void scanForGroups(ItemReader itemReader) {
		checkNotNull(itemReader);

		Optional<TimeTrackingItem> item;
		while ((item = itemReader.read()).isPresent()) {
			Optional<String> optComment = item.get().getComment();
			if (optComment.isPresent()) {
				String comment = optComment.get();
				getGroupsOf(comment);
			}
		}
	}

}

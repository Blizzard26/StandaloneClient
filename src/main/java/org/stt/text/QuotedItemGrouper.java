package org.stt.text;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Matches groups of quoted text. The quotes can be any non-alphabetic
 * characters.
 * 
 */
public class QuotedItemGrouper implements ItemGrouper {

	@Override
	public List<String> getGroupsOf(String text) {
        Objects.requireNonNull(text);
        List<String> groups = new ArrayList<>();
		int index = 0;
		while (index < text.length()) {
			char delimiter = text.charAt(index);
			if (!Character.isWhitespace(delimiter)) {

				if (!Character.isAlphabetic(delimiter)) {
					int endOfQuote = text.indexOf(delimiter, index + 1);
					if (endOfQuote > 0) {
						groups.add(text.substring(index + 1, endOfQuote));
						index = endOfQuote;
					} else {
						groups.add(text.substring(index));
						index = text.length();
					}
				} else {
					groups.add(text.substring(index));
					index = text.length();
				}
			}
			index++;
		}
		return groups;
	}
}

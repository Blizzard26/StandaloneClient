package org.stt.search;

import java.util.List;

public interface ExpansionProvider {
	List<String> getPossibleExpansions(String text);
}

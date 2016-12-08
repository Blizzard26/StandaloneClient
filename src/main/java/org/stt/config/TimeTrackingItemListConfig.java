package org.stt.config;

import static java.util.Arrays.asList;

import java.util.List;

public class TimeTrackingItemListConfig implements Config {
	private boolean filterDuplicatesWhenSearching = false;
    private boolean askBeforeDeleting = false;
	private List<String> groupColors;
	private String breakTimeColor;

    /**
	 * When true, only distinct comments will be shown in the result list.
	 */
	public boolean isFilterDuplicatesWhenSearching() {
		return filterDuplicatesWhenSearching;
	}

	public void setFilterDuplicatesWhenSearching(
			boolean filterDuplicatesWhenSearching) {
		this.filterDuplicatesWhenSearching = filterDuplicatesWhenSearching;
	}

    public boolean isAskBeforeDeleting() {
        return askBeforeDeleting;
    }

    public void setAskBeforeDeleting(boolean askBeforeDeleting) {
        this.askBeforeDeleting = askBeforeDeleting;
    }

    public void setGroupColors(List<String> groupColors) {
        this.groupColors = groupColors;
    }
    
    public void setBreakTimeColor(String breakTimeColor) {
    	this.breakTimeColor = breakTimeColor;
    }

    public List<String> getGroupColors() {
        return groupColors;
    }
    
    public String getBreakTimeColor() {
		return breakTimeColor;
	}

    @Override
    public void applyDefaults() {
        if (groupColors == null)  {
            groupColors = asList("BLUE", "DARKCYAN", "GREEN", "DARKGREEN", "BROWN");
        }
        
        if (breakTimeColor == null) {
        	breakTimeColor = "RED";
        }
        	
    }
}

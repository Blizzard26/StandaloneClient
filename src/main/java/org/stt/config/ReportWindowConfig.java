package org.stt.config;

import java.util.List;

import static java.util.Arrays.asList;

/**
 * Created by dante on 01.12.14.
 */
public class ReportWindowConfig implements Config {
    private boolean groupItems = true;
    private List<String> groupColors;
	private String breakTimeColor;

    public boolean isGroupItems() {
        return groupItems;
    }

    public void setGroupItems(boolean groupItems) {
        this.groupItems = groupItems;
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

    @Override
    public void applyDefaults() {
        if (groupColors == null)  {
            groupColors = asList("BLUE", "DARKCYAN", "GREEN", "DARKGREEN", "BROWN");
        }
        
        if (breakTimeColor == null) {
        	breakTimeColor = "RED";
        }
        	
    }

	public String getBreakTimeColor() {
		return breakTimeColor;
	}
}

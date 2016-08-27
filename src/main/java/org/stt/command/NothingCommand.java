package org.stt.command;

import org.stt.model.TimeTrackingItem;

import com.google.common.base.Optional;

/**
 * Created by dante on 14.03.15.
 */
public class NothingCommand implements Command {
    public static final Command INSTANCE = new NothingCommand();

    private NothingCommand() {}

    @Override
    public void execute() {

    }
    
    public Optional<TimeTrackingItem> getItem()
    {
    	return Optional.<TimeTrackingItem>absent();
    }
}

package org.stt.command;

import org.stt.model.TimeTrackingItem;

import com.google.common.base.Optional;

/**
 * Created by dante on 14.03.15.
 */
public interface Command {
    void execute();
    
    public Optional<TimeTrackingItem> getItem();
}

package org.stt.command;

import org.stt.persistence.ItemPersister;
import org.stt.query.TimeTrackingItemQueries;

import com.google.common.base.Optional;

public interface CommandTextItem {

	Optional<Command> getCommand(ItemPersister persister, TimeTrackingItemQueries timeTrackingItemQueries);

}

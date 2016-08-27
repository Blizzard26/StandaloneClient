package org.stt.command;

import org.stt.model.TimeTrackingItem;
import org.stt.persistence.ItemPersister;

import com.google.common.base.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by dante on 15.03.15.
 */
public abstract  class PersistingCommand implements Command {
    protected final ItemPersister persister;

    protected PersistingCommand(ItemPersister persister) {
        this.persister = checkNotNull(persister);
    }
    
    
}

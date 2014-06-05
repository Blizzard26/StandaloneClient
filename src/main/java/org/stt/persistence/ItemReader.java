package org.stt.persistence;

import java.io.Closeable;
import java.io.IOException;

import org.stt.model.TimeTrackingItem;

import com.google.common.base.Optional;

/**
 */
public interface ItemReader extends Closeable {
	/**
	 * Reads an item, if available.
	 * 
	 * @return An {@link Optional} of the {@link TimeTrackingItem} or absent if
	 *         none is available
	 * @throws IOException
	 */
	Optional<TimeTrackingItem> read();
}

package org.stt.model;

import com.google.common.base.Optional;

public class CurrentItemChanged {

	private Optional<TimeTrackingItem> currentItem;

	public CurrentItemChanged(Optional<TimeTrackingItem> currentItem) {
		this.currentItem = currentItem;
	}

	public Optional<TimeTrackingItem> getCurrentItem() {
		return currentItem;
	}

}

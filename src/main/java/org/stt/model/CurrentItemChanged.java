package org.stt.model;

public class CurrentItemChanged {

	private TimeTrackingItem currentItem;

	public CurrentItemChanged(TimeTrackingItem currentItem) {
		this.currentItem = currentItem;
	}

	public TimeTrackingItem getCurrentItem() {
		return currentItem;
	}

}

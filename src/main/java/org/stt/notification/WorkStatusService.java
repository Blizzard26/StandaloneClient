package org.stt.notification;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ResourceBundle;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Period;
import org.stt.Service;
import org.stt.event.TimePassedEvent;
import org.stt.gui.Notification;
import org.stt.model.CurrentItemChanged;
import org.stt.model.FileChanged;
import org.stt.model.ItemModified;
import org.stt.query.TimeTrackingItemQueries;
import org.stt.query.WorkTimeQueries;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

public class WorkStatusService implements Service {

	private EventBus eventBus;
	private Notification notification;
	private ResourceBundle i18n;
	private DateTime lastWeekWorktimeNotification;
	private DateTime lastDailyWorktimeNotification;
	private WorkTimeQueries workTimeQueries;
	private Duration remainingWorktimeToday;
	private Duration remainingWorktimeWeek;
	private DateTime lastUpdate;

	public WorkStatusService(ResourceBundle i18n, EventBus eventBus, WorkTimeQueries workTimeQueries,
			TimeTrackingItemQueries searcher, Notification notification) {
		this.i18n = checkNotNull(i18n);
		this.eventBus = checkNotNull(eventBus);
		this.workTimeQueries = checkNotNull(workTimeQueries);
		this.notification = checkNotNull(notification);
	}

	@Override
	public void start() throws Exception {
		eventBus.register(this);

	}

	@Override
	public void stop() {
		eventBus.unregister(this);
	}

	@Subscribe
	public void onCurrentItemChanged(CurrentItemChanged event)
	{
		updateWorktime();
		checkWorktime();
	}
	
	@Subscribe
	public void onFileChanged(FileChanged event) {
		updateWorktime();
		checkWorktime();
	}
	
	@Subscribe
	public void updateOnModification(ItemModified event) {
		updateWorktime();
		checkWorktime();
	}


	@Subscribe
	public void timePassed(TimePassedEvent event) {
		timeElapsed();
		checkWorktime();	
	}

	private void updateWorktime() {
		remainingWorktimeToday = workTimeQueries.queryRemainingWorktimeToday();
		remainingWorktimeWeek = workTimeQueries.queryRemainingWorktimeWeek();
		lastUpdate = DateTime.now();
	}

	
	private void timeElapsed() {
		if (lastUpdate == null)
			updateWorktime();
		
		DateTime now = DateTime.now();
		Duration elapsed = new Period(lastUpdate, now).toStandardDuration();
		lastUpdate = now;
		
		remainingWorktimeToday = remainingWorktimeToday.minus(elapsed);		
		remainingWorktimeWeek = remainingWorktimeWeek.minus(elapsed);
	}
	

	private void checkWorktime() {
		if (durationLessOrEqualZero(remainingWorktimeToday)) {
			if (!today(lastDailyWorktimeNotification)) {
				notification.info(i18n.getString("achievement.worktimeTodayReached"));
				lastDailyWorktimeNotification = DateTime.now();
			}
		}
		else
		{
			lastDailyWorktimeNotification = null;
		}

		if (durationLessOrEqualZero(remainingWorktimeWeek)) {
			if (!today(lastWeekWorktimeNotification)) {
				notification.info(i18n.getString("achievement.worktimeWeekReached"));
				lastWeekWorktimeNotification = DateTime.now();
			}
		}
		else
		{
			lastWeekWorktimeNotification = null;
		}
	}

	private boolean durationLessOrEqualZero(Duration duration) {
		return duration.isEqual(Duration.ZERO) || duration.isShorterThan(Duration.ZERO);
	}

	private boolean today(DateTime dateTime) 
	{
		DateTime now = DateTime.now();
		return dateTime != null && dateTime.getDayOfYear() == now.getDayOfYear() && dateTime.getYear() == now.getYear();
	}

}

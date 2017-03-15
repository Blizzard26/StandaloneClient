package org.stt.query;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.joda.time.*;
import org.stt.text.ItemCategorizer;
import org.stt.model.TimeTrackingItem;
import org.stt.reporting.WorkingtimeItemProvider;

import java.util.logging.Logger;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by dante on 29.03.15.
 */
@Singleton
public class WorkTimeQueries {
    private static final Logger LOG = Logger.getLogger(WorkTimeQueries.class.getName());
    private WorkingtimeItemProvider workingtimeItemProvider;
    private ItemCategorizer itemCategorizer;
    private TimeTrackingItemQueries timeTrackingItemQueries;

    @Inject
    public WorkTimeQueries(WorkingtimeItemProvider workingtimeItemProvider, ItemCategorizer itemCategorizer,
                           TimeTrackingItemQueries timeTrackingItemQueries) {
        this.workingtimeItemProvider = checkNotNull(workingtimeItemProvider);
        this.itemCategorizer = checkNotNull(itemCategorizer);
        this.timeTrackingItemQueries = checkNotNull(timeTrackingItemQueries);
    }

    public Duration queryRemainingWorktimeToday() {
        DateTime now = new DateTime();
        LocalDate today = now.toLocalDate();
        Duration workedTime = queryWorktime(today.toInterval().withEnd(now));
        Duration remainingDuration = workingtimeItemProvider.getWorkingTimeFor(today).getMin().minus(workedTime);
        if (remainingDuration.isShorterThan(Duration.ZERO)) {
            remainingDuration = Duration.ZERO;
        }
        return remainingDuration;
    }
    
    public Duration queryRemainingWorktimeWeek() {
    	DateTime now = DateTime.now();
    	LocalDate today = now.toLocalDate();
    	LocalDate startOfWeek = today.withDayOfWeek(0);
    	LocalDate endOfWeek = startOfWeek.plusWeeks(1);
    	
    	// Get time worked so far this week
    	Duration workedTime = queryWorktime(endOfWeek.toInterval().withEnd(now));
    	
    	// Calculated working time of week
    	Duration workingtime = Duration.ZERO;
    	LocalDate day = startOfWeek;
    	while (day.isBefore(endOfWeek))
    	{
    		workingtime.plus(workingtimeItemProvider.getWorkingTimeFor(day).getMin());
    		day = day.plusDays(1);
    	}
    	
    	// Determine remaining duration
    	Duration remainingDuration = workingtime.minus(workedTime);
    	if (remainingDuration.isShorterThan(Duration.ZERO))
    	{
    		remainingDuration = Duration.ZERO;
    	}
    	
    	return remainingDuration;
    }

    public Duration queryWeekWorktime() {
        DateTime now = new DateTime();
        LocalDate monday = now.toLocalDate().withDayOfWeek(DateTimeConstants.MONDAY);
        Interval currentWeek = monday.toInterval().withEnd(now);
        return queryWorktime(currentWeek);
    }

    public Duration queryWorktime(Interval interval) {
        Duration workedTime = Duration.ZERO;
        DNFClause dnfClause = new DNFClause();
        dnfClause.withStartNotBefore(interval.getStart());
        dnfClause.withStartBefore(interval.getEnd());
        for (TimeTrackingItem item: timeTrackingItemQueries.queryItems(dnfClause)) {
            if (itemCategorizer.getCategory(item.getComment().or("")) == ItemCategorizer.ItemCategory.WORKTIME) {
                DateTime end = item.getEnd().or(interval.getEnd());
                workedTime = workedTime.plus(new Duration(item.getStart(), end));
            }
        }
        return workedTime;
    }
}

package org.stt.query;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.joda.time.DateTime;
import org.stt.model.TimeTrackingItem;
import org.stt.persistence.IOUtil;
import org.stt.persistence.ItemReader;
import org.stt.persistence.ItemReaderProvider;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class DefaultTimeTrackingItemQueries implements TimeTrackingItemQueries {

	private final ItemReaderProvider provider;

	/**
	 * @param provider
	 *            where to search for items
	 */
	@Inject
	public DefaultTimeTrackingItemQueries(ItemReaderProvider provider) {
		this.provider = checkNotNull(provider);
	}

	@Override
	public Optional<TimeTrackingItem> getCurrentTimeTrackingitem() {
		Optional<TimeTrackingItem> latestTimeTrackingitem = getLatestTimeTrackingitem();
		
		return latestTimeTrackingitem.isPresent() && !latestTimeTrackingitem.get().getEnd().isPresent() ? 
				latestTimeTrackingitem : Optional.<TimeTrackingItem>absent();
		
	}
	
	@Override
	public Optional<TimeTrackingItem> getLatestTimeTrackingitem() {
		try (ItemReader reader = provider.provideReader()) {
			Optional<TimeTrackingItem> item;
			TimeTrackingItem currentItem = null;
			while ((item = reader.read()).isPresent()) {
				currentItem = item.get();
			}
			return currentItem == null ? Optional.<TimeTrackingItem> absent() : Optional.of(currentItem);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public Optional<TimeTrackingItem> getPreviousTimeTrackingItem(TimeTrackingItem item) {
		try (ItemReader reader = provider.provideReader()) {
			Optional<TimeTrackingItem> currentItem;
			TimeTrackingItem previousItem = null;
			while ((currentItem = reader.read()).isPresent()) {
				if (item.equals(currentItem.get()))
				{
					// Found current item
					return previousItem != null ? Optional.of(previousItem)
							 : Optional.<TimeTrackingItem> absent();
				}
				else
				{
					// Current item is not the one we're looking for, remember previous one
					previousItem = currentItem.get();
				}
			}
			return Optional.<TimeTrackingItem> absent();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public Optional<TimeTrackingItem> getNextTimeTrackingTime(TimeTrackingItem item) {
		try (ItemReader reader = provider.provideReader()) {
			Optional<TimeTrackingItem> currentItem;
			while ((currentItem = reader.read()).isPresent()) {
				if (item.equals(currentItem.get()))
				{
					return reader.read();
				}
			}
			return Optional.<TimeTrackingItem> absent();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Collection<DateTime> getAllTrackedDays() {
		Collection<DateTime> result = new ArrayList<>();
		try (ItemReader reader = provider.provideReader()) {
			Optional<TimeTrackingItem> item;
			DateTime lastDay = null;
			while ((item = reader.read()).isPresent()) {
				DateTime currentDay = item.get().getStart()
						.withTimeAtStartOfDay();
				if (lastDay == null || !lastDay.equals(currentDay)) {
					result.add(currentDay);
					lastDay = currentDay;
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return result;
	}

	@Override
	public Collection<TimeTrackingItem> queryFirstNItems(Optional<DateTime> start, Optional<DateTime> end, Optional<Integer> maxItems) {
		List<TimeTrackingItem> result = new ArrayList<>();
		try (ItemReader reader = provider.provideReader()) {
			Optional<TimeTrackingItem> read;
			while ((!maxItems.isPresent() || result.size() < maxItems.get()) && (read = reader.read()).isPresent()) {
				TimeTrackingItem item = read.get();
				boolean afterStart = !start.isPresent() || !item.getStart().isBefore(start.get());
                boolean itemDoesntEndAfterQuery = !end.isPresent() || (item.getEnd().isPresent() && !item.getEnd().get().isAfter(end.get()));
                boolean beforeEnd = !end.isPresent() || itemDoesntEndAfterQuery;
				if (afterStart && beforeEnd) {
					result.add(item);
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return result;
	}

    @Override
    public Collection<TimeTrackingItem> queryItems(DNFClause dnfClause) {
        Collection<TimeTrackingItem> result = new ArrayList<>();
        try (ItemReader reader = provider.provideReader()) {
			DNFClauseMatcher DNFClauseMatcher = new DNFClauseMatcher(dnfClause);
			Optional<TimeTrackingItem> read;
            while ((read = reader.read()).isPresent()) {
                TimeTrackingItem item = read.get();
				if (DNFClauseMatcher.matches(item)) {
					result.add(item);
				}
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    @Override
    public Collection<TimeTrackingItem> queryAllItems() {
        try {
            return IOUtil.readAll(provider.provideReader());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }




}

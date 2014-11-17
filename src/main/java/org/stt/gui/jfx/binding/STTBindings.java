package org.stt.gui.jfx.binding;

import javafx.beans.binding.StringBinding;
import javafx.beans.value.ObservableStringValue;
import javafx.beans.value.ObservableValue;
import org.joda.time.Duration;
import org.stt.time.DateTimeHelper;

/**
 *
 * @author dante
 */
public class STTBindings {

	public static ObservableStringValue formattedDuration(final ObservableValue<Duration> duration) {
		return new StringBinding() {
			{
				bind(duration);
			}

			@Override
			protected String computeValue() {
				return DateTimeHelper.FORMATTER_PERIOD_HHh_MMm_SSs.print(duration.getValue().toPeriod());
			}
		};
	}
}

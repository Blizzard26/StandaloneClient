package org.stt.gui.jfx;

import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Period;
import org.stt.event.TimePassedEvent;
import org.stt.model.CurrentItemChanged;
import org.stt.model.FileChanged;
import org.stt.model.ItemModified;
import org.stt.gui.jfx.binding.STTBindings;
import org.stt.query.WorkTimeQueries;

import java.util.ResourceBundle;
import java.util.logging.Logger;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by dante on 29.03.15.
 */
@Singleton
public class WorktimePaneBuilder implements AdditionalPaneBuilder {
    private static final Logger LOG = Logger.getLogger(WorktimePaneBuilder.class.getName());
    private final ResourceBundle i18n;
    private SimpleObjectProperty<Duration> remainingWorktime = new SimpleObjectProperty<>();
    private SimpleObjectProperty<Duration> weekWorktime = new SimpleObjectProperty<>();
    private WorkTimeQueries workTimeQueries;
	private DateTime lastUpdate;
	private boolean activeItem = true;

    @Inject
    public WorktimePaneBuilder(ResourceBundle i18n, WorkTimeQueries workTimeQueries) {
        this.i18n = checkNotNull(i18n);
        this.workTimeQueries = checkNotNull(workTimeQueries);
    }

    @Subscribe
    public void updateOnModification(ItemModified event) {
        updateItems();
    }
    
    @Subscribe
    public void updateOnFileChanged(FileChanged event) {
    	updateItems();
    }

	@Subscribe 
	public void onCurrentItemChanged(CurrentItemChanged event)
	{
		activeItem = event.getCurrentItem().isPresent();
		updateItems();
	}
    
    @Subscribe
    public void timePassed(TimePassedEvent event) {
        timeElapsed();
    }

    private void updateItems() {
        LOG.finest("Updating remaining worktime");
        remainingWorktime.setValue(workTimeQueries.queryRemainingWorktimeToday());
        weekWorktime.setValue(workTimeQueries.queryWeekWorktime());
        lastUpdate = DateTime.now();
    }
    
    private void timeElapsed() {
    	if (!activeItem)
    	{
    		lastUpdate = null;
    		return;
    	}
    	
    	if (lastUpdate == null)
    		updateItems();
		
		DateTime now = DateTime.now();
		Duration elapsed = new Period(lastUpdate, now).toStandardDuration();
		lastUpdate = now;
		
		Duration remainingWorktimeToday = remainingWorktime.get();
		Duration worktimeWeek = weekWorktime.get();
		
		remainingWorktimeToday = remainingWorktimeToday.minus(elapsed);		
		worktimeWeek = worktimeWeek.plus(elapsed);
		
		remainingWorktime.set(remainingWorktimeToday);
		weekWorktime.set(worktimeWeek);
    }

    @Override
    public Pane build() {
        updateItems();
        FlowPane flowPane = new FlowPane();
        flowPane.setHgap(20);
        ObservableList<Node> elements = flowPane.getChildren();
        Label remainingWorktimeToday = new Label();
        remainingWorktimeToday.textProperty().bind(STTBindings.formattedDuration(remainingWorktime));
        Label weekWorktimeLabel = new Label();
        weekWorktimeLabel.textProperty().bind(STTBindings.formattedDuration(weekWorktime));

        elements.add(hbox(4, new Label(i18n.getString("remainingWorktimeToday")), remainingWorktimeToday));
        elements.add(hbox(4, new Label(i18n.getString("weekWorktime")), weekWorktimeLabel));

        return flowPane;
    }

    private HBox hbox(double spacing, Node... nodes) {
        HBox hBox = new HBox(spacing);
        hBox.getChildren().addAll(nodes);
        return hBox;
    }
}

package org.stt.gui.jfx;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;
import java.util.ResourceBundle;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.stt.config.TimeTrackingItemListConfig;
import org.stt.model.TimeTrackingItem;
import org.stt.model.TimeTrackingItemFilter;
import org.stt.text.ItemCategorizer.ItemCategory;
import org.stt.text.ItemGrouper;
import org.stt.text.WorktimeCategorizer;

import javafx.beans.binding.ObjectBinding;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.Separator;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;
import javafx.scene.text.TextFlow;
import javafx.scene.text.Text;

class TimeTrackingItemCell extends ListCell<TimeTrackingItem> {

	private final HBox cellPane = new HBox(10);

//	private final Label labelForComment = new Label();
	
	private final TextFlow flowPaneForComment = new TextFlow();

	private final HBox timePane = new HBox();

	private final Label labelForStart = new Label();

	private final Label labelForEnd = new Label();

	final Button editButton;

	final Button continueButton;

	final Button deleteButton;

	private final ImageView fromToImageView;

	private final ImageView runningImageView;

	private final TimeTrackingItemFilter firstItemOfTheDayFilter;
	private final BorderPane firstDayPane;

	private final Separator separator;
	
	private final WorktimeCategorizer worktimeCategorizer;
	private final ItemGrouper itemGrouper;
    private final TimeTrackingItemListConfig config;
    
	private final Color[] groupColors;
	private final Color breakTimeColor;

	public TimeTrackingItemCell(Builder builder) {
		ResourceBundle localization = builder.resourceBundle;
		this.editButton = new ImageButton(checkNotNull(builder.editImage));
		this.continueButton = new ImageButton(
				checkNotNull(builder.continueImage));
		this.deleteButton = new ImageButton(checkNotNull(builder.deleteImage));
		setupTooltips(localization);
		this.fromToImageView = new ImageView(checkNotNull(builder.fromToImage));
		this.runningImageView = new ImageView(
				checkNotNull(builder.runningImage));
		this.firstItemOfTheDayFilter = checkNotNull(builder.firstItemOfTheDayFilter);
		this.worktimeCategorizer = checkNotNull(builder.workitemCategorizer);
		this.itemGrouper = checkNotNull(builder.itemGrouper);
		this.config = checkNotNull(builder.timeTrackingItemListConfig);
		
		String breakColorString = config.getBreakTimeColor();
        breakTimeColor = Color.web(breakColorString);
        
        List<String> colorStrings = config.getGroupColors();
        groupColors = new Color[colorStrings.size()];
        for (int i = 0; i < colorStrings.size(); i++) {
            groupColors[i] = Color.web(colorStrings.get(i));
        }

		final ContinueActionHandler continueActionHandler = checkNotNull(builder.continueActionHandler);
		final EditActionHandler editActionHandler = checkNotNull(builder.editActionHandler);
		final DeleteActionHandler deleteActionHandler = checkNotNull(builder.deleteActionHandler);

		setupListenersForCallbacks(continueActionHandler, editActionHandler,
				deleteActionHandler);

		HBox actionsPane = new HBox();
		actionsPane.getChildren().addAll(deleteButton, continueButton,
				editButton);

		Pane space = new Pane();
		HBox.setHgrow(space, Priority.ALWAYS);
//		labelForComment.setWrapText(true);
//		labelForComment.setPrefWidth(350);
		flowPaneForComment.setPrefWidth(350);

		timePane.setPrefWidth(250);
		timePane.setSpacing(5);
		timePane.setAlignment(Pos.CENTER_LEFT);

//		cellPane.getChildren().addAll(actionsPane, labelForComment, space,
//				timePane);
		cellPane.getChildren().addAll(actionsPane, flowPaneForComment, space,
				timePane);
		cellPane.setAlignment(Pos.CENTER_LEFT);
		actionsPane.setAlignment(Pos.CENTER_LEFT);

		firstDayPane = new BorderPane();
		separator = new Separator();

		setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
	}

	protected void setupTooltips(ResourceBundle localization) {
		editButton.setTooltip(new Tooltip(localization
				.getString("itemList.edit")));
		continueButton.setTooltip(new Tooltip(localization
				.getString("itemList.continue")));
		deleteButton.setTooltip(new Tooltip(localization
				.getString("itemList.delete")));
	}

	private void setupListenersForCallbacks(
			final ContinueActionHandler continueActionHandler,
			final EditActionHandler editActionHandler,
			final DeleteActionHandler deleteActionHandler) {
		continueButton.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				continueActionHandler.continueItem(getItem());
			}
		});

		editButton.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent arg0) {
				editActionHandler.edit(getItem());
			}
		});

		deleteButton.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent arg0) {
				deleteActionHandler.delete(getItem());
			}
		});
	}

	@Override
	protected void updateItem(TimeTrackingItem item, boolean empty) {
		super.updateItem(item, empty);
		if (empty) {
			setGraphic(null);
		} else {
			applyLabelForComment();
			setupTimePane();
			if (firstItemOfTheDayFilter.filter(item)) {
				setupFirstDayPane();
				setGraphic(firstDayPane);
			} else {
				setupCellPane();
				setGraphic(cellPane);
			}
		}
	}

	private void setupCellPane() {
		firstDayPane.setCenter(null);
		firstDayPane.setBottom(null);
	}

	private void setupFirstDayPane() {
		firstDayPane.setCenter(cellPane);
		firstDayPane.setBottom(separator);
	}

	private void applyLabelForComment() {
//		if (getItem().getComment().isPresent()) {
//			labelForComment.setText(getItem().getComment().get());
//		} else {
//			labelForComment.setText("");
//		}
		
		ObservableList<Node> flowPaneChildren = flowPaneForComment.getChildren();
        flowPaneChildren.clear();
		if (getItem().getComment().isPresent()) {
			String item = getItem().getComment().get();
			ItemCategory category = worktimeCategorizer.getCategory(item);
			switch (category) {
			case BREAK:
				colorizeBreakTime(item, flowPaneChildren);
				break;
			case WORKTIME:
			default:
				colorizeGroups(item, flowPaneChildren);
				break;
			}
           
        }

	}
	
	private void colorizeBreakTime(String item, ObservableList<Node> flowPaneChildren) {
		final Text partLabel = new Text(item);
		Color color = breakTimeColor;
		
//		Color selected = color.deriveColor(0, 1, 3, 1);
//		BooleanBinding selectedRow = Bindings.equal(tableForReport.getSelectionModel().selectedIndexProperty(),
//				indexProperty());
//		ObjectBinding<Color> colorObjectBinding = new When(selectedRow).then(selected).otherwise(color);
		
		ObjectBinding<Color> colorObjectBinding = new ObjectBinding<Color>() {

			@Override
			protected Color computeValue() {
				return color;
			}
		};
		partLabel.fillProperty().bind(colorObjectBinding);
		flowPaneChildren.add(partLabel);
	}

	private void colorizeGroups(String item, ObservableList<Node> flowPaneChildren) {
		final List<String> itemGroups = itemGrouper.getGroupsOf(item);
		int index = 0;
		int lastIndex = 0;
		
		for (int i = 0; i < itemGroups.size(); i++) {
			String partToShow;
			String part = itemGroups.get(i);
			index = item.indexOf(part, lastIndex) + part.length();
			if (index == -1)
				index = item.length();
			
			partToShow = item.substring(lastIndex, index);
			lastIndex = index;
			
			final Text partLabel = new Text(partToShow);
			if (i < groupColors.length) {
				Color color = groupColors[i];
//				Color selected = color.deriveColor(0, 1, 3, 1);
//				BooleanBinding selectedRow = Bindings
//						.equal(tableForReport.getSelectionModel().selectedIndexProperty(), indexProperty());
//				ObjectBinding<Color> colorObjectBinding = new When(selectedRow).then(selected).otherwise(color);
				ObjectBinding<Color> colorObjectBinding = new ObjectBinding<Color>() {

					@Override
					protected Color computeValue() {
						return color;
					}
				};
				partLabel.fillProperty().bind(colorObjectBinding);
			}
			flowPaneChildren.add(partLabel);
		}
	}


	private void setupTimePane() {
		DateTimeFormatter dateTimeFormatter = DateTimeFormat.shortDateTime();
		labelForStart.setText(dateTimeFormatter.print(getItem().getStart()));

		if (!getItem().getEnd().isPresent()) {
			timePane.getChildren().setAll(labelForStart, runningImageView);
		} else {
			labelForEnd.setText(dateTimeFormatter.print(getItem().getEnd()
					.get()));
			timePane.getChildren().setAll(labelForStart, fromToImageView,
					labelForEnd);
		}
	}

	public interface ContinueActionHandler {

		void continueItem(TimeTrackingItem item);
	}

	public interface EditActionHandler {

		void edit(TimeTrackingItem item);
	}

	public interface DeleteActionHandler {

		void delete(TimeTrackingItem item);
	}

	public static class Builder {

		private TimeTrackingItemListConfig timeTrackingItemListConfig;
		private ItemGrouper itemGrouper;
		private WorktimeCategorizer workitemCategorizer;
		private ContinueActionHandler continueActionHandler;
		private DeleteActionHandler deleteActionHandler;
		private EditActionHandler editActionHandler;
		private TimeTrackingItemFilter firstItemOfTheDayFilter;
		private ResourceBundle resourceBundle;
		private Image editImage;
		private Image deleteImage;
		private Image continueImage;
		private Image fromToImage;
		private Image runningImage;

		public Builder editImage(Image editImage) {
			this.editImage = checkNotNull(editImage);
			return this;
		}

		public Builder continueImage(Image continueImage) {
			this.continueImage = checkNotNull(continueImage);
			return this;
		}

		public Builder deleteImage(Image deleteImage) {
			this.deleteImage = checkNotNull(deleteImage);
			return this;
		}

		public Builder fromToImage(Image fromToImage) {
			this.fromToImage = checkNotNull(fromToImage);
			return this;
		}

		public Builder runningImage(Image runningImage) {
			this.runningImage = checkNotNull(runningImage);
			return this;
		}

		public Builder continueActionHandler(ContinueActionHandler handler) {
			this.continueActionHandler = handler;
			return this;
		}

		public Builder deleteActionHandler(DeleteActionHandler handler) {
			this.deleteActionHandler = handler;
			return this;
		}

		public Builder editActionHandler(EditActionHandler handler) {
			this.editActionHandler = handler;
			return this;
		}

		public Builder firstItemOfTheDayFilter(
				TimeTrackingItemFilter firstItemOfTheDayFilter) {
			this.firstItemOfTheDayFilter = checkNotNull(firstItemOfTheDayFilter);
			return this;
		}

		public Builder resourceBundle(ResourceBundle resourceBundle) {
			this.resourceBundle = checkNotNull(resourceBundle);
			return this;
		}

		public Builder itemGrouper(ItemGrouper itemGrouper) {
			this.itemGrouper = checkNotNull(itemGrouper);
			return this;
		}

		public Builder workitemCategorizer(WorktimeCategorizer workitemCategorizer) {
			this.workitemCategorizer = checkNotNull(workitemCategorizer);
			return this;
		}

		public Builder timeTrackingItemListConfig(TimeTrackingItemListConfig timeTrackingItemListConfig) {
			this.timeTrackingItemListConfig = checkNotNull(timeTrackingItemListConfig);
			return this;
		}
	}
}

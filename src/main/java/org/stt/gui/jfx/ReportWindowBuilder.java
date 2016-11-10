package org.stt.gui.jfx;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.stt.time.DateTimeHelper.FORMATTER_PERIOD_HHh_MMm_SSs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;
import org.stt.config.ReportWindowConfig;
import org.stt.gui.jfx.binding.ReportBinding;
import org.stt.gui.jfx.binding.STTBindings;
import org.stt.model.ReportingItem;
import org.stt.persistence.ItemReaderProvider;
import org.stt.query.TimeTrackingItemQueries;
import org.stt.reporting.SummingReportGenerator.Report;
import org.stt.text.ItemGrouper;
import org.stt.time.DateTimeHelper;
import org.stt.time.DurationRounder;

import com.google.common.base.Joiner;
import com.google.inject.Inject;
import com.google.inject.Provider;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.ListBinding;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.binding.StringBinding;
import javafx.beans.binding.When;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableStringValue;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableColumn.CellDataFeatures;
import javafx.scene.control.TreeTableColumn.SortType;
import javafx.scene.control.TreeTablePosition;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.cell.TreeItemPropertyValueFactory;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Callback;
import javafx.util.StringConverter;

public class ReportWindowBuilder {
    private final ItemReaderProvider readerProvider;
    private final TimeTrackingItemQueries timeTrackingItemQueries;

    private final Provider<Stage> stageProvider;
    private final DurationRounder rounder;
    private final ItemGrouper itemGrouper;
    private final Color[] groupColors;
    private ReportWindowConfig config;


    @Inject
    ReportWindowBuilder(Provider<Stage> stageProvider,
                               ItemReaderProvider readerProvider, TimeTrackingItemQueries searcher,
                               DurationRounder rounder, ItemGrouper itemGrouper, ReportWindowConfig config) {
        this.config = checkNotNull(config);
        this.stageProvider = checkNotNull(stageProvider);
        this.timeTrackingItemQueries = checkNotNull(searcher);
        this.readerProvider = checkNotNull(readerProvider);
        this.rounder = checkNotNull(rounder);
        this.itemGrouper = checkNotNull(itemGrouper);

        List<String> colorStrings = config.getGroupColors();
        groupColors = new Color[colorStrings.size()];
        for (int i = 0; i < colorStrings.size(); i++) {
            groupColors[i] = Color.web(colorStrings.get(i));
        }
    }

    public void setupStage() throws IOException {
        try {
			Stage stage = stageProvider.get();

			ReportWindowController controller = new ReportWindowController(stage);

			ResourceBundle localization = ResourceBundle
			        .getBundle("org.stt.gui.Application");
			FXMLLoader loader = new FXMLLoader(getClass().getResource(
			        "/org/stt/gui/jfx/ReportWindow.fxml"), localization);
			loader.setController(controller);
			loader.load();

			stage.show();
		} catch (Exception e) {
			e.printStackTrace();
		}
    }

    private ObservableValue<Report> createReportModel(
            final ObservableValue<DateTime> selectedDateTime) {
        ObservableValue<DateTime> nextDay = new ObjectBinding<DateTime>() {
            @Override
            protected DateTime computeValue() {
                return selectedDateTime.getValue() != null ? selectedDateTime
                        .getValue().plusDays(1) : null;
            }

            {
                bind(selectedDateTime);
            }


        };
        return new ReportBinding(selectedDateTime, nextDay, readerProvider);
    }

    private ListBinding<ListItem> createReportingItemsListModel(
            final ObservableValue<Report> report) {
        return new ListBinding<ListItem>() {
            @Override
            protected ObservableList<ListItem> computeValue() {
                List<ReportingItem> reportingItems = report.getValue()
                        .getReportingItems();
                List<ListItem> resultList = new ArrayList<>();
                for (ReportingItem reportItem : reportingItems) {
                    resultList.add(new ListItem(reportItem.getComment(),
                            reportItem.getDuration(), rounder
                            .roundDuration(reportItem.getDuration())));
                }
                return FXCollections.observableArrayList(resultList);
            }

            {
                super.bind(report);
            }


        };
    }

    public static class ListItem {

        private final String comment;
        private final Duration duration;
        private final Duration roundedDuration;
		private boolean logged;

        public ListItem(String comment, Duration duration,
                        Duration roundedDuration) {
        	this.logged = false;
            this.comment = comment;
            this.duration = duration;
            this.roundedDuration = roundedDuration;
        }

        public String getComment() {
            return comment;
        }

        public Duration getDuration() {
            return duration;
        }

        public Duration getRoundedDuration() {
            return roundedDuration;
        }
        
        public boolean isLogged()
        {
        	return logged;
        }
        
        public void setLogged(boolean logged)
        {
        	this.logged = logged;
        }
    }

    public class ReportWindowController {

        private final Stage stage;
        @FXML
        private TreeTableColumn<ListItem, String> columnForRoundedDuration;
        @FXML
        private TreeTableColumn<ListItem, String> columnForDuration;
        @FXML
        private TreeTableColumn<ListItem, String> columnForComment;
        @FXML
        private TreeTableView<ListItem> treeForReport;
        @FXML
        private FlowPane reportControlsPane;
        @FXML
        private BorderPane borderPane;
        @FXML
        private Label startOfReport;
        @FXML
        private Label endOfReport;
        @FXML
        private Label uncoveredTime;
        @FXML
        private Label roundedDurationSum;
        @FXML
        private Label pauseSum;

        public ReportWindowController(Stage stage) {
            this.stage = checkNotNull(stage);
        }

        @FXML
        public void closeWindow() {
            stage.close();
        }

        @FXML
        public void initialize() {
            final ObservableValue<DateTime> selectedDateTimeProperty = addComboBoxForDateTimeSelectionAndReturnSelectedDateTimeProperty();
            final ObservableValue<Report> reportModel = createReportModel(selectedDateTimeProperty);
            final StringBinding startBinding = createBindingForStartOfReport(reportModel);
            final StringBinding endBinding = createBindingForEndOfReport(reportModel);
            final ObjectBinding<Duration> uncoveredTimeBinding = createBindingForUncoveredTimeOfReport(reportModel);
            ObservableStringValue formattedUncoveredTimeBinding = STTBindings
                    .formattedDuration(uncoveredTimeBinding);
            ObjectBinding<Color> uncoveredTimeTextFillBinding = new When(
                    uncoveredTimeBinding.isEqualTo(Duration.ZERO)).then(
                    Color.BLACK).otherwise(Color.RED);

            startOfReport.textProperty().bind(startBinding);
            endOfReport.textProperty().bind(endBinding);
            uncoveredTime.textFillProperty().bind(uncoveredTimeTextFillBinding);
            uncoveredTime.textProperty().bind(formattedUncoveredTimeBinding);
            startOfReport.setOnMouseClicked(new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent event) {
                    setClipboard(startBinding.get());
                }
            });
            endOfReport.setOnMouseClicked(new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent event) {
                    setClipboard(endBinding.get());
                }
            });

            ListBinding<ListItem> reportListModel = createReportingItemsListModel(reportModel);
            //tableForReport.setItems(reportListModel);
            //tableForReport.getSelectionModel().setCellSelectionEnabled(true);
            TreeItem<ListItem> root = new TreeItem<ListItem>(new ListItem("", Duration.ZERO, Duration.ZERO))
    		{

				@Override
				public boolean isLeaf() {
					// TODO Auto-generated method stub
					return super.isLeaf();
				}

				@Override
				public ObservableList<TreeItem<ListItem>> getChildren() {
					// TODO Auto-generated method stub
					return super.getChildren();
				}
    	
    		};
			treeForReport.setRoot(root);
			treeForReport.getSelectionModel().setCellSelectionEnabled(true);

            roundedDurationSum
                    .textProperty()
                    .bind(STTBindings
                            .formattedDuration(createBindingForRoundedDurationSum(reportListModel)));
            
            pauseSum.textProperty()
		            .bind(STTBindings
		                    .formattedDuration(createBindingForPauseSum(reportModel)));

            setRoundedDurationColumnCellFactoryToConvertDurationToString();
            setDurationColumnCellFactoryToConvertDurationToString();
            setCommentColumnCellFactory();

            presetSortingToAscendingCommentColumn();

            addSelectionToClipboardListenerToTableForReport();

            addSceneToStageAndSetStageToModal();

            columnForComment.prefWidthProperty().bind(
                    treeForReport.widthProperty().subtract(
                            columnForRoundedDuration.widthProperty().add(
                                    columnForDuration.widthProperty())));
        }

        private ObservableValue<Duration> createBindingForPauseSum(final ObservableValue<Report> reportModel) {
        	return new ObjectBinding<Duration>() {
                @Override
                protected Duration computeValue() {
                	return reportModel.getValue().getPauseTime();
                }

                {
                    bind(reportModel);
                }


            };
		}

		private ObservableValue<Duration> createBindingForRoundedDurationSum(
                final ListBinding<ListItem> items) {
            return new ObjectBinding<Duration>() {
                @Override
                protected Duration computeValue() {
                    Duration duration = Duration.ZERO;
                    for (ListItem item : items) {
                        duration = duration.plus(item.roundedDuration);
                    }
                    return duration;
                }

                {
                    bind(items);
                }


            };
        }

        private ObjectBinding<Duration> createBindingForUncoveredTimeOfReport(
                final ObservableValue<Report> reportModel) {
            return new ObjectBinding<Duration>() {
                @Override
                protected Duration computeValue() {
                    return reportModel.getValue().getUncoveredDuration();
                }

                {
                    bind(reportModel);
                }


            };
        }

        private StringBinding createBindingForEndOfReport(
                final ObservableValue<Report> reportModel) {
            return new StringBinding() {
                @Override
                protected String computeValue() {
                    return DateTimeHelper.DATE_TIME_FORMATTER_HH_MM_SS
                            .print(reportModel.getValue().getEnd());
                }

                {
                    bind(reportModel);
                }


            };
        }

        private StringBinding createBindingForStartOfReport(
                final ObservableValue<Report> reportModel) {
            return new StringBinding() {
                @Override
                protected String computeValue() {
                    return DateTimeHelper.DATE_TIME_FORMATTER_HH_MM_SS
                            .print(reportModel.getValue().getStart());
                }

                {
                    bind(reportModel);
                }


            };
        }

        private void addSceneToStageAndSetStageToModal() {
            Scene scene = new Scene(borderPane);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initStyle(StageStyle.UTILITY);
            stage.setScene(scene);

            scene.setOnKeyPressed(new EventHandler<KeyEvent>() {

                @Override
                public void handle(KeyEvent event) {
                    if (KeyCode.ESCAPE.equals(event.getCode())) {
                        event.consume();
                        stage.close();
                    }
                }
            });
        }

        private void presetSortingToAscendingCommentColumn() {
            columnForComment.setSortType(SortType.ASCENDING);
            treeForReport.getSortOrder().add(columnForComment);
        }

        private void setCommentColumnCellFactory() {
        	columnForComment
        			.setCellValueFactory(new TreeItemPropertyValueFactory<>(
        					"comment"));
            if (config.isGroupItems()) {
                setItemGroupingCellFactory();
            }
        }

        private void setItemGroupingCellFactory() {
            columnForComment.setCellFactory(new Callback<TreeTableColumn<ListItem,String>, TreeTableCell<ListItem,String>>() {
				@Override
				public TreeTableCell<ListItem, String> call(TreeTableColumn<ListItem, String> param) {
					return new CommentTableCell();
				}
			});
        }

        private void addSelectionToClipboardListenerToTableForReport() {
            treeForReport.getSelectionModel().getSelectedCells()
                    .addListener(new ListChangeListener<TreeTablePosition<ListItem, ?>>()
            		{

						@Override
						public void onChanged(
								javafx.collections.ListChangeListener.Change<? extends TreeTablePosition<ListItem, ?>> change) {
							ObservableList<? extends TreeTablePosition<ListItem, ?>> selectedPositions = change
                                    .getList();
                            setClipboardIfExactlyOneItemWasSelected(selectedPositions);
						}
                    
                        private void setClipboardIfExactlyOneItemWasSelected(
                                ObservableList<? extends TreeTablePosition<ListItem, ?>> selectedPositions) {
                            if (selectedPositions.size() == 1) {
                                TreeTablePosition<ListItem, ?> position = selectedPositions
                                        .get(0);
                                TreeItem<ListItem> treeItem = treeForReport.getTreeItem(position.getRow());
                                ListItem listItem = treeItem.getValue();
                                if (position.getTableColumn() == columnForRoundedDuration) {
                                    setClipBoard(listItem.getRoundedDuration());
                                } else if (position.getTableColumn() == columnForDuration) {
                                    setClipBoard(listItem.getDuration());
                                } else if (position.getTableColumn() == columnForComment) {
                                    setClipboard(listItem.getComment());
                                }
                            }
                        }

                    });
        }

        private void setClipboard(String comment) {
            ClipboardContent content = new ClipboardContent();
            content.putString(comment);
            setClipboardContentTo(content);
        }

        private void setClipBoard(Duration duration) {
            PeriodFormatter formatter = new PeriodFormatterBuilder()
                    .printZeroIfSupported().minimumPrintedDigits(2)
                    .appendHours().appendSeparator(":").appendMinutes()
                    .toFormatter();
            setClipboard(formatter.print(duration.toPeriod()));
        }

        private void setClipboardContentTo(ClipboardContent content) {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            clipboard.setContent(content);
        }

        private void setDurationColumnCellFactoryToConvertDurationToString() {
            columnForDuration
                    .setCellValueFactory(new TreeItemPropertyValueFactory<ListItem, String>(
                            "duration") {
                        @Override
                        public ObservableValue<String> call(
                                CellDataFeatures<ListItem, String> cellDataFeatures) {
                            TreeItem<ListItem> treeItem = cellDataFeatures.getValue();
							ListItem value = treeItem.getValue();
							
							String duration = "";
							if (value != null)
							{
								duration = FORMATTER_PERIOD_HHh_MMm_SSs
                                    .print(value
                                            .getDuration().toPeriod());
							}
                            return new SimpleStringProperty(duration);
                        }
                    });
        }

        private void setRoundedDurationColumnCellFactoryToConvertDurationToString() {
            columnForRoundedDuration
                    .setCellValueFactory(new TreeItemPropertyValueFactory<ListItem, String>(
                            "roundedDuration") {
                        @Override
                        public ObservableValue<String> call(
                                CellDataFeatures<ListItem, String> cellDataFeatures) {
                            TreeItem<ListItem> treeItem = cellDataFeatures.getValue();
							ListItem value = treeItem.getValue();
							String duration = "";
							if (value != null)
							{
								duration = FORMATTER_PERIOD_HHh_MMm_SSs
	                                    .print(value
	                                            .getRoundedDuration().toPeriod());
							}
                            return new SimpleStringProperty(duration);
                        }
                    });
        }

        private ObservableValue<DateTime> addComboBoxForDateTimeSelectionAndReturnSelectedDateTimeProperty() {
            final ComboBox<DateTime> comboBox = new ComboBox<>();
            ObservableList<DateTime> availableDays = FXCollections
                    .observableArrayList(timeTrackingItemQueries.getAllTrackedDays());
            Collections.reverse(availableDays);
            comboBox.setItems(availableDays);
            if (!availableDays.isEmpty()) {
                comboBox.getSelectionModel().select(0);
            }
            comboBox.setConverter(new StringConverter<DateTime>() {
                @Override
                public String toString(DateTime dateTime) {
                    return DateTimeFormat.shortDate().print(dateTime);
                }

                @Override
                public DateTime fromString(String arg0) {
                    throw new UnsupportedOperationException();
                }
            });
            reportControlsPane.getChildren().add(comboBox);

            return comboBox.getSelectionModel().selectedItemProperty();
        }

        private class CommentTableCell extends TreeTableCell<ListItem, String> {
            private FlowPane flowPane = new FlowPane();

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                ObservableList<Node> flowPaneChildren = flowPane.getChildren();
                flowPaneChildren.clear();
                if (!empty) {
                    final List<String> itemGroups = itemGrouper.getGroupsOf(item);
                    for (int i = 0; i < itemGroups.size(); i++) {
                        String partToShow;
                        String part = itemGroups.get(i);
                        if (i > 0) {
                            partToShow = " " + part;
                        } else {
                            partToShow = part;
                        }
                        final Label partLabel = new Label(partToShow);
                        addClickListener(itemGroups, partLabel, i);
                        if (i < groupColors.length) {
                            Color color = groupColors[i];
                            Color selected = color.deriveColor(0, 1, 3, 1);
                            BooleanBinding selectedRow = Bindings.equal(treeForReport.getSelectionModel().selectedIndexProperty(), indexProperty());
                            ObjectBinding<Color> colorObjectBinding = new When(selectedRow).then(selected).otherwise(color);
                            partLabel.textFillProperty().bind(colorObjectBinding);
                        }
                        flowPaneChildren.add(partLabel);
                    }
                }
                setGraphic(flowPane);
            }

            private void addClickListener(final List<String> itemGroups, Label partLabel, final int fromIndex) {
                partLabel.setOnMouseClicked(new EventHandler<MouseEvent>() {
                    @Override
                    public void handle(MouseEvent event) {
                        String commentRemainder = Joiner.on(" ").join(itemGroups.subList(fromIndex, itemGroups.size()));
                        setClipboard(commentRemainder);
                    }
                });
            }
        }
    }
}

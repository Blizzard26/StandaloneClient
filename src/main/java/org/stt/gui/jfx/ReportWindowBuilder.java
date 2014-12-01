package org.stt.gui.jfx;

import com.google.common.base.Joiner;
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
import javafx.scene.control.*;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableColumn.SortType;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Callback;
import javafx.util.StringConverter;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;
import org.stt.Factory;
import org.stt.gui.jfx.binding.ReportBinding;
import org.stt.gui.jfx.binding.STTBindings;
import org.stt.model.ReportingItem;
import org.stt.persistence.ItemReaderProvider;
import org.stt.reporting.ItemGrouper;
import org.stt.reporting.SummingReportGenerator.Report;
import org.stt.searching.ItemSearcher;
import org.stt.time.DateTimeHelper;
import org.stt.time.DurationRounder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.stt.time.DateTimeHelper.FORMATTER_PERIOD_HHh_MMm_SSs;

public class ReportWindowBuilder {
    private static final Paint[] GROUP_FILL = {Color.BLUE, Color.DARKCYAN, Color.GREEN, Color.DARKGREEN, Color.BROWN};

    private final ItemReaderProvider readerProvider;
    private final ItemSearcher itemSearcher;

    private final Factory<Stage> stageFactory;
    private final DurationRounder rounder;
    private final ItemGrouper itemGrouper;
    private boolean groupItems;

    public ReportWindowBuilder(Factory<Stage> stageFactory,
                               ItemReaderProvider readerProvider, ItemSearcher searcher,
                               DurationRounder rounder, ItemGrouper itemGrouper, boolean groupItems) {
        this.groupItems = groupItems;
        this.stageFactory = checkNotNull(stageFactory);
        this.itemSearcher = checkNotNull(searcher);
        this.readerProvider = checkNotNull(readerProvider);
        this.rounder = checkNotNull(rounder);
        this.itemGrouper = checkNotNull(itemGrouper);
    }

    public void setupStage() throws IOException {
        Stage stage = stageFactory.create();

        ReportWindowController controller = new ReportWindowController(stage);

        ResourceBundle localization = ResourceBundle
                .getBundle("org.stt.gui.Application");
        FXMLLoader loader = new FXMLLoader(getClass().getResource(
                "/org/stt/gui/jfx/ReportWindow.fxml"), localization);
        loader.setController(controller);
        loader.load();

        stage.show();
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

        public ListItem(String comment, Duration duration,
                        Duration roundedDuration) {
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
    }

    public class ReportWindowController {

        private final Stage stage;
        @FXML
        private TableColumn<ListItem, String> columnForRoundedDuration;
        @FXML
        private TableColumn<ListItem, String> columnForDuration;
        @FXML
        private TableColumn<ListItem, String> columnForComment;
        @FXML
        private TableView<ListItem> tableForReport;
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
            StringBinding startBinding = createBindingForStartOfReport(reportModel);
            StringBinding endBinding = createBindingForEndOfReport(reportModel);
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

            ListBinding<ListItem> reportListModel = createReportingItemsListModel(reportModel);
            tableForReport.setItems(reportListModel);

            roundedDurationSum
                    .textProperty()
                    .bind(STTBindings
                            .formattedDuration(createBindingForRoundedDurationSum(reportListModel)));

            setRoundedDurationColumnCellFactoryToConvertDurationToString();
            setDurationColumnCellFactoryToConvertDurationToString();
            setCommentColumnCellFactory();

            presetSortingToAscendingCommentColumn();

            addSelectionToClipboardListenerToTableForReport();

            addSceneToStageAndSetStageToModal();

            columnForComment.prefWidthProperty().bind(
                    tableForReport.widthProperty().subtract(
                            columnForRoundedDuration.widthProperty().add(
                                    columnForDuration.widthProperty())));
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
            StringBinding endBinding = new StringBinding() {
                @Override
                protected String computeValue() {
                    return DateTimeHelper.DATE_TIME_FORMATTER_HH_MM_SS
                            .print(reportModel.getValue().getEnd());
                }

                {
                    bind(reportModel);
                }


            };
            return endBinding;
        }

        private StringBinding createBindingForStartOfReport(
                final ObservableValue<Report> reportModel) {
            StringBinding startBinding = new StringBinding() {
                @Override
                protected String computeValue() {
                    return DateTimeHelper.DATE_TIME_FORMATTER_HH_MM_SS
                            .print(reportModel.getValue().getStart());
                }

                {
                    bind(reportModel);
                }


            };
            return startBinding;
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
            tableForReport.getSortOrder().add(columnForComment);
        }

        private void setCommentColumnCellFactory() {
            columnForComment
                    .setCellValueFactory(new PropertyValueFactory<ListItem, String>(
                            "comment"));
            if (groupItems) {
                setItemGroupingCellFactory();
            }
        }

        private void setItemGroupingCellFactory() {
            columnForComment.setCellFactory(new Callback<TableColumn<ListItem, String>, TableCell<ListItem, String>>() {
                @Override
                public TableCell<ListItem, String> call(TableColumn<ListItem, String> param) {
                    return new TableCell<ListItem, String>() {
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
                                    final int finalIndex = i;
                                    final Label partLabel = new Label(partToShow);
                                    partLabel.setOnMouseClicked(new EventHandler<MouseEvent>() {
                                        @Override
                                        public void handle(MouseEvent event) {
                                            String commentRemainder = Joiner.on(" ").join(itemGroups.subList(finalIndex, itemGroups.size()));
                                            setClipboard(commentRemainder);
                                        }
                                    });
                                    if (i < GROUP_FILL.length) {
                                        partLabel.setTextFill(GROUP_FILL[i]);
                                    }
                                    flowPaneChildren.add(partLabel);
                                }
                            }
                            setGraphic(flowPane);
                        }
                    };
                }
            });
        }

        @SuppressWarnings("rawtypes")
        private void addSelectionToClipboardListenerToTableForReport() {
            tableForReport.getSelectionModel().getSelectedCells()
                    .addListener(new ListChangeListener<TablePosition>() {

                        @Override
                        public void onChanged(
                                javafx.collections.ListChangeListener.Change<? extends TablePosition> change) {
                            ObservableList<? extends TablePosition> selectedPositions = change
                                    .getList();
                            setClipboardIfExactlyOneItemWasSelected(selectedPositions);
                        }

                        private void setClipboardIfExactlyOneItemWasSelected(
                                ObservableList<? extends TablePosition> selectedPositions) {
                            if (selectedPositions.size() == 1) {
                                TablePosition position = selectedPositions
                                        .get(0);
                                ListItem listItem = tableForReport.getItems()
                                        .get(position.getRow());
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
            ClipboardContent content = new ClipboardContent();
            PeriodFormatter formatter = new PeriodFormatterBuilder()
                    .printZeroIfSupported().minimumPrintedDigits(2)
                    .appendHours().appendSeparator(":").appendMinutes()
                    .toFormatter();
            content.putString(formatter.print(duration.toPeriod()));
            setClipboardContentTo(content);
        }

        private void setClipboardContentTo(ClipboardContent content) {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            clipboard.setContent(content);
        }

        private void setDurationColumnCellFactoryToConvertDurationToString() {
            columnForDuration
                    .setCellValueFactory(new PropertyValueFactory<ListItem, String>(
                            "duration") {
                        @Override
                        public ObservableValue<String> call(
                                CellDataFeatures<ListItem, String> cellDataFeatures) {
                            String duration = FORMATTER_PERIOD_HHh_MMm_SSs
                                    .print(cellDataFeatures.getValue()
                                            .getDuration().toPeriod());
                            return new SimpleStringProperty(duration);
                        }
                    });
        }

        private void setRoundedDurationColumnCellFactoryToConvertDurationToString() {
            columnForRoundedDuration
                    .setCellValueFactory(new PropertyValueFactory<ListItem, String>(
                            "roundedDuration") {
                        @Override
                        public ObservableValue<String> call(
                                CellDataFeatures<ListItem, String> cellDataFeatures) {
                            String duration = FORMATTER_PERIOD_HHh_MMm_SSs
                                    .print(cellDataFeatures.getValue()
                                            .getRoundedDuration().toPeriod());
                            return new SimpleStringProperty(duration);
                        }
                    });
        }

        private ObservableValue<DateTime> addComboBoxForDateTimeSelectionAndReturnSelectedDateTimeProperty() {
            final ComboBox<DateTime> comboBox = new ComboBox<>();
            ObservableList<DateTime> availableDays = FXCollections
                    .observableArrayList(itemSearcher.getAllTrackedDays());
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
    }
}

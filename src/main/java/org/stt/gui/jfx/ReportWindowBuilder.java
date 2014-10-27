package org.stt.gui.jfx;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.util.Collections;
import java.util.ResourceBundle;

import javafx.beans.binding.ListBinding;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.binding.StringBinding;
import javafx.beans.binding.When;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableColumn.SortType;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.StringConverter;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;
import org.stt.Factory;
import org.stt.filter.StartDateReaderFilter;
import org.stt.model.ReportingItem;
import org.stt.persistence.ItemReader;
import org.stt.persistence.ItemReaderProvider;
import org.stt.reporting.SummingReportGenerator;
import org.stt.reporting.SummingReportGenerator.Report;
import org.stt.searching.ItemSearcher;

public class ReportWindowBuilder {
	private final ItemReaderProvider readerProvider;
	private final ItemSearcher itemSearcher;

	private final PeriodFormatter hmsPeriodFormatter = new PeriodFormatterBuilder()
			.printZeroAlways().minimumPrintedDigits(2).appendHours()
			.appendSuffix("h").appendSeparator(":").appendMinutes()
			.appendSuffix("m").appendSeparator(":").appendSeconds()
			.appendSuffix("s").toFormatter();

	private final DateTimeFormatter hmsDateFormat = DateTimeFormat
			.forPattern("HH:mm:ss");

	private final Factory<Stage> stageFactory;

	public ReportWindowBuilder(Factory<Stage> stageFactory,
			ItemReaderProvider readerProvider, ItemSearcher searcher) {
		this.stageFactory = checkNotNull(stageFactory);
		this.itemSearcher = checkNotNull(searcher);
		this.readerProvider = checkNotNull(readerProvider);
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
		return new ObjectBinding<Report>() {
			{
				super.bind(selectedDateTime);
			}

			@Override
			protected Report computeValue() {
				Report report;
				if (selectedDateTime.getValue() != null) {
					DateTime startOfDay = selectedDateTime.getValue();
					DateTime nextDay = startOfDay.plusDays(1);
					report = createSummaryReportFor(startOfDay, nextDay);
				} else {
					report = new Report(
							Collections.<ReportingItem> emptyList(), null,
							null, Duration.ZERO);
				}
				return report;
			}

		};
	}

	private ListBinding<ReportingItem> createReportingItemsListModel(
			final ObservableValue<Report> report) {
		return new ListBinding<ReportingItem>() {
			{
				super.bind(report);
			}

			@Override
			protected ObservableList<ReportingItem> computeValue() {
				return FXCollections.observableArrayList(report.getValue()
						.getReportingItems());
			}

		};
	}

	private Report createSummaryReportFor(DateTime startOfDay, DateTime nextDay) {
		try (ItemReader itemReader = readerProvider.provideReader();
				StartDateReaderFilter filter = new StartDateReaderFilter(
						itemReader, startOfDay, nextDay)) {
			SummingReportGenerator reportGenerator = new SummingReportGenerator(
					filter);
			return reportGenerator.createReport();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public class ReportWindowController {
		@FXML
		private TableColumn<ReportingItem, String> columnForDuration;

		@FXML
		private TableColumn<ReportingItem, String> columnForComment;

		@FXML
		private TableView<ReportingItem> tableForReport;

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

		private final Stage stage;

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
			StringBinding formattedUncoveredTimeBinding = new StringBinding() {
				{
					bind(uncoveredTimeBinding);
				}

				@Override
				protected String computeValue() {
					return hmsPeriodFormatter.print(uncoveredTimeBinding.get()
							.toPeriod());
				}
			};
			ObjectBinding<Color> uncoveredTimeTextFillBinding = new When(
					uncoveredTimeBinding.isEqualTo(Duration.ZERO)).then(
					Color.BLACK).otherwise(Color.RED);

			startOfReport.textProperty().bind(startBinding);
			endOfReport.textProperty().bind(endBinding);
			uncoveredTime.textFillProperty().bind(uncoveredTimeTextFillBinding);
			uncoveredTime.textProperty().bind(formattedUncoveredTimeBinding);

			ListBinding<ReportingItem> reportListModel = createReportingItemsListModel(reportModel);
			tableForReport.setItems(reportListModel);

			setDurationColumnCellFactoryToConvertDurationToString();
			setCommentColumnCellFactory();

			presetSortingToAscendingCommentColumn();

			addSelectionToClipboardListenerToTableForReport();

			addSceneToStageAndSetStageToModal();
		}

		private ObjectBinding<Duration> createBindingForUncoveredTimeOfReport(
				final ObservableValue<Report> reportModel) {
			return new ObjectBinding<Duration>() {
				{
					bind(reportModel);
				}

				@Override
				protected Duration computeValue() {
					return reportModel.getValue().getUncoveredDuration();
				}
			};
		}

		private StringBinding createBindingForEndOfReport(
				final ObservableValue<Report> reportModel) {
			StringBinding endBinding = new StringBinding() {
				{
					bind(reportModel);
				}

				@Override
				protected String computeValue() {
					return hmsDateFormat.print(reportModel.getValue().getEnd());
				}
			};
			return endBinding;
		}

		private StringBinding createBindingForStartOfReport(
				final ObservableValue<Report> reportModel) {
			StringBinding startBinding = new StringBinding() {
				{
					bind(reportModel);
				}

				@Override
				protected String computeValue() {
					return hmsDateFormat.print(reportModel.getValue()
							.getStart());
				}
			};
			return startBinding;
		}

		private void addSceneToStageAndSetStageToModal() {
			Scene scene = new Scene(borderPane);
			stage.initModality(Modality.APPLICATION_MODAL);
			stage.initStyle(StageStyle.UTILITY);
			stage.setScene(scene);
		}

		private void presetSortingToAscendingCommentColumn() {
			columnForComment.setSortType(SortType.ASCENDING);
			tableForReport.getSortOrder().add(columnForComment);
		}

		private void setCommentColumnCellFactory() {
			columnForComment
					.setCellValueFactory(new PropertyValueFactory<ReportingItem, String>(
							"comment"));
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
								ReportingItem reportingItem = tableForReport
										.getItems().get(position.getRow());
								if (position.getTableColumn() == columnForDuration) {
									setClipBoard(reportingItem.getDuration());
								} else {
									setClipboard(reportingItem.getComment());
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
					.printZeroIfSupported().appendHours().appendSeparator(":")
					.appendMinutes().toFormatter();
			content.putString(formatter.print(duration.toPeriod()));
			setClipboardContentTo(content);
		}

		private void setClipboardContentTo(ClipboardContent content) {
			Clipboard clipboard = Clipboard.getSystemClipboard();
			clipboard.setContent(content);
		}

		private void setDurationColumnCellFactoryToConvertDurationToString() {
			columnForDuration
					.setCellValueFactory(new PropertyValueFactory<ReportingItem, String>(
							"duration") {
						@Override
						public ObservableValue<String> call(
								CellDataFeatures<ReportingItem, String> cellDataFeatures) {
							String duration = hmsPeriodFormatter
									.print(cellDataFeatures.getValue()
											.getDuration().toPeriod());
							return new SimpleStringProperty(duration);
						}
					});
		}

		private ObservableValue<DateTime> addComboBoxForDateTimeSelectionAndReturnSelectedDateTimeProperty() {
			final ComboBox<DateTime> comboBox = new ComboBox<DateTime>();
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

package org.stt.gui.jfx;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.stt.time.DateTimeHelper.FORMATTER_PERIOD_HHh_MMm_SSs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;
import org.stt.config.ReportWindowConfig;
import org.stt.gui.jfx.ReportWindowBuilder.ReportItem;
import org.stt.gui.jfx.binding.ReportBinding;
import org.stt.gui.jfx.binding.STTBindings;
import org.stt.model.ReportingItem;
import org.stt.persistence.ItemReaderProvider;
import org.stt.query.TimeTrackingItemQueries;
import org.stt.reporting.SummingReportGenerator.Report;
import org.stt.text.ItemCategorizer.ItemCategory;
import org.stt.text.ItemGrouper;
import org.stt.text.WorktimeCategorizer;
import org.stt.time.DateTimeHelper;
import org.stt.time.DurationRounder;

import com.google.common.base.Joiner;
import com.google.inject.Inject;
import com.google.inject.Provider;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.binding.StringBinding;
import javafx.beans.binding.When;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableStringValue;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.CheckBoxTreeItem;
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
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.FlowPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Shape;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
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
    private final WorktimeCategorizer worktimeCategorizer;
    private final Color[] groupColors;
    private ReportWindowConfig config;
	private Color breakTimeColor;


    @Inject
    ReportWindowBuilder(Provider<Stage> stageProvider,
                               ItemReaderProvider readerProvider, TimeTrackingItemQueries searcher,
                               DurationRounder rounder, ItemGrouper itemGrouper, WorktimeCategorizer worktimeCategorizer, ReportWindowConfig config) {
        this.config = checkNotNull(config);
        this.stageProvider = checkNotNull(stageProvider);
        this.timeTrackingItemQueries = checkNotNull(searcher);
        this.readerProvider = checkNotNull(readerProvider);
        this.rounder = checkNotNull(rounder);
        this.itemGrouper = checkNotNull(itemGrouper);
        this.worktimeCategorizer = checkNotNull(worktimeCategorizer);

        String breakColorString = this.config.getBreakTimeColor();
        breakTimeColor = Color.web(breakColorString);
        
        List<String> colorStrings = this.config.getGroupColors();
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

    private ObjectBinding<TreeItem<ReportItem>> createReportingTreeModel(ObservableValue<Report> reportModel) {
    	
    	ObjectBinding<TreeItem<ReportItem>> objectBinding = new ObjectBinding<TreeItem<ReportItem>>() {

			@Override
			protected TreeItem<ReportItem> computeValue() {
		    	ReportItem rootItem = new ReportItem();
		    	
		    	for (ReportingItem item : reportModel.getValue().getReportingItems())
		    	{
		    		List<String> groups = itemGrouper.getGroupsOf(item.getComment());
		    		
		    		ReportItem reportItem = rootItem;
		    		for (int i = 0; i < groups.size() -1; i++) {
						String group = groups.get(i);
						reportItem = reportItem.getOrCreateChild(group);
					}
		    		
		    		reportItem.addLeaf(new ReportItem(item.getComment(), item.getDuration(), rounder.roundDuration(item.getDuration())));
		    		
		    	}
		    	
				return new TreeItemExtension(rootItem);
			}
			
			{
				bind(reportModel);
			}
    		
    	};
    	
    	return objectBinding;
	}

	private final class TreeItemExtension extends TreeItem<ReportItem> {
		
		public TreeItemExtension(ReportItem item) {
			super(item);			
			List<TreeItem<ReportItem>> items = new ArrayList<>();
			for (ReportItem i : getValue().getChildern())
			{
				items.add(new TreeItemExtension(i));
			}
			getChildren().setAll(items);
			setExpanded(true);
			
		}
	}

	public static class ReportItem {

        private final String comment;
        private final Duration duration;
        private final Duration roundedDuration;
		private boolean logged;
		
		private final Map<String, ReportItem> children = new HashMap<>();
		private final List<ReportItem> leafs = new ArrayList<>();

		public ReportItem() {
			this("", Duration.ZERO, Duration.ZERO);
		}
		
		
        public ReportItem(String comment, Duration duration,
                        Duration roundedDuration) {
        	this.logged = false;
            this.comment = comment;
            this.duration = duration;
            this.roundedDuration = roundedDuration;
        }

        public void addLeaf(ReportItem reportItem) {
			leafs.add(reportItem);
		}

		public ReportItem getOrCreateChild(String group) {
			ReportItem item = children.get(group);
			if (item == null)
			{
				item = new ReportItem(group, Duration.ZERO, Duration.ZERO);
				children.put(group, item);
			}
			return item;
		}
		
		public List<ReportItem> getChildern()
		{
			List<ReportItem> l = new ArrayList<>();
			
			l.addAll(children.values());
			l.addAll(leafs);
			
			Collections.sort(l, new Comparator<ReportItem>() {
				@Override
				public int compare(ReportItem o1, ReportItem o2) {
					return o1.getComment().compareTo(o2.getComment());
				}
			});
			
			return l;
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
        
        public String toString()
        {
        	StringBuilder s = new StringBuilder();
        	
        	s.append("'" + comment + "'-[");
        	
        	String sep = "";
        	for (ReportItem item : getChildern())
        	{
        		s.append(sep);
        		sep = ", ";
        		s.append(item);
        	}
        	s.append("]");
        	return s.toString();
        }
    }

    public class ReportWindowController {

        private final Stage stage;
        @FXML
        private TreeTableColumn<ReportItem, String> columnForRoundedDuration;
        @FXML
        private TreeTableColumn<ReportItem, String> columnForDuration;
        @FXML
        private TreeTableColumn<ReportItem, String> columnForComment;
        
        @FXML
        private TreeTableColumn<ReportItem, Boolean> columnForLogged;
        
        @FXML
        private TreeTableView<ReportItem> treeForReport;
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
                    setCommentToClipboard(startBinding.get());
                }
            });
            endOfReport.setOnMouseClicked(new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent event) {
                    setCommentToClipboard(endBinding.get());
                }
            });

            ObjectBinding<TreeItem<ReportItem>> root = createReportingTreeModel(reportModel);
			//treeForReport.setRoot(root);
			treeForReport.rootProperty().bind(root);
			treeForReport.getSelectionModel().setCellSelectionEnabled(false);
			
			//treeForReport.setShowRoot(true);

            roundedDurationSum
                    .textProperty()
                    .bind(STTBindings
                            .formattedDuration(createBindingForRoundedDurationSum(reportModel)));
            
            pauseSum.textProperty()
		            .bind(STTBindings
		                    .formattedDuration(createBindingForPauseSum(reportModel)));

            setRoundedDurationColumnCellFactoryToConvertDurationToString();
            setDurationColumnCellFactoryToConvertDurationToString();
            setLoggedColumnCellFactory();
            setCommentColumnCellFactory();

            presetSortingToAscendingCommentColumn();

            addSelectionToClipboardListenerToTableForReport();

            addSceneToStageAndSetStageToModal();

            columnForComment.prefWidthProperty().bind(
                    treeForReport.widthProperty().subtract(
                            columnForRoundedDuration.widthProperty().add(
                                    columnForDuration.widthProperty().add(columnForLogged.widthProperty()).add(10))));
            columnForComment.setMinWidth(300);
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
                final ObservableValue<Report> reportModel) {
            return new ObjectBinding<Duration>() {
                @Override
                protected Duration computeValue() {
                    Duration duration = Duration.ZERO;
                    for (ReportingItem item : reportModel.getValue().getReportingItems()) {
                        duration = duration.plus(rounder.roundDuration(item.getDuration()));
                    }
                    return duration;
                }

                {
                    bind(reportModel);
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
            columnForComment.setCellFactory(new Callback<TreeTableColumn<ReportItem,String>, TreeTableCell<ReportItem,String>>() {
				@Override
				public TreeTableCell<ReportItem, String> call(TreeTableColumn<ReportItem, String> param) {
					return new CommentTableCell();
				}
			});
            
        }

        private void addSelectionToClipboardListenerToTableForReport() {
            treeForReport.getSelectionModel().getSelectedCells()
                    .addListener(new ListChangeListener<TreeTablePosition<ReportItem, ?>>()
            		{

						@Override
						public void onChanged(
								javafx.collections.ListChangeListener.Change<? extends TreeTablePosition<ReportItem, ?>> change) {
							ObservableList<? extends TreeTablePosition<ReportItem, ?>> selectedPositions = change
                                    .getList();
                            setClipboardIfExactlyOneItemWasSelected(selectedPositions);
						}
                    
                        private void setClipboardIfExactlyOneItemWasSelected(
                                ObservableList<? extends TreeTablePosition<ReportItem, ?>> selectedPositions) {
                            if (selectedPositions.size() == 1) {
                                TreeTablePosition<ReportItem, ?> position = selectedPositions
                                        .get(0);
                                TreeItem<ReportItem> treeItem = treeForReport.getTreeItem(position.getRow());
                                ReportItem listItem = treeItem.getValue();
                                if (position.getTableColumn() == columnForRoundedDuration) {
                                    setDurationToClipboard(listItem.getRoundedDuration());
                                } else if (position.getTableColumn() == columnForDuration) {
                                    setDurationToClipboard(listItem.getDuration());
                                } else if (position.getTableColumn() == columnForComment) {
                                    setCommentToClipboard(listItem.getComment());
                                }
                            }
                        }

                    });
        }

        private void setCommentToClipboard(String comment) {
            ClipboardContent content = new ClipboardContent();
            content.putString(comment);
            setClipboardContentTo(content);
        }

        private void setDurationToClipboard(Duration duration) {
            PeriodFormatter formatter = new PeriodFormatterBuilder()
                    .printZeroIfSupported().minimumPrintedDigits(2)
                    .appendHours().appendSeparator(":").appendMinutes()
                    .toFormatter();
            setCommentToClipboard(formatter.print(duration.toPeriod()));
        }

        private void setClipboardContentTo(ClipboardContent content) {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            clipboard.setContent(content);
        }

        private void setDurationColumnCellFactoryToConvertDurationToString() {
            columnForDuration
                    .setCellValueFactory(new TreeItemPropertyValueFactory<ReportItem, String>(
                            "duration") {
                        @Override
                        public ObservableValue<String> call(
                                CellDataFeatures<ReportItem, String> cellDataFeatures) {
                            TreeItem<ReportItem> treeItem = cellDataFeatures.getValue();
							ReportItem value = treeItem.getValue();
							
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
        
		private void setLoggedColumnCellFactory() {
			// TODO
		}

        private void setRoundedDurationColumnCellFactoryToConvertDurationToString() {
            columnForRoundedDuration
                    .setCellValueFactory(new TreeItemPropertyValueFactory<ReportItem, String>(
                            "roundedDuration") {
                        @Override
                        public ObservableValue<String> call(
                                CellDataFeatures<ReportItem, String> cellDataFeatures) {
                            TreeItem<ReportItem> treeItem = cellDataFeatures.getValue();
							ReportItem value = treeItem.getValue();
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

        private class CommentTableCell extends TreeTableCell<ReportItem, String> {
            private TextFlow flowPane = new TextFlow() {

            	// Bugfix for wrong behavior when adding Text to TextFlow
            	// see JDK-8124129
				@Override
				protected double computePrefHeight(double width) {
					if (width < USE_COMPUTED_SIZE)
					{
						CommentTableCell parent = (CommentTableCell) getParent();
						
						width = parent.computePrefWidth(-1);
					    Insets insets = parent.getInsets();
					    
					    width -= insets.getLeft() + insets.getRight();
					}
					
					return super.computePrefHeight(width);
				}

            	
            };

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                System.out.println();
                ObservableList<Node> flowPaneChildren = flowPane.getChildren();
                flowPaneChildren.clear();
				if (!empty) {
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
						
                setGraphic(flowPane);
            }

			private void colorizeBreakTime(String item, ObservableList<Node> flowPaneChildren) {
				final Label partLabel = new Label(item);
				addClickListener(Arrays.asList(item), partLabel, 0);
				Color color = breakTimeColor;
				Color selected = color.deriveColor(0, 1, 3, 1);
				BooleanBinding selectedRow = Bindings.equal(treeForReport.getSelectionModel().selectedIndexProperty(),
						indexProperty());
				ObjectBinding<Color> colorObjectBinding = new When(selectedRow).then(selected).otherwise(color);
				partLabel.textFillProperty().bind(colorObjectBinding);
				flowPaneChildren.add(partLabel);
			}

			private void colorizeGroups(String item, ObservableList<Node> flowPaneChildren) {
				final List<String> itemGroups = itemGrouper.getGroupsOf(item);
				int index = 0;
				int lastIndex = 0;
				
				BooleanBinding selectedRow = Bindings
						.equal(treeForReport.getSelectionModel().selectedIndexProperty(), indexProperty());
				
				for (int i = 0; i < itemGroups.size(); i++) {
					String partToShow;
					String part = itemGroups.get(i);
					index = item.indexOf(part, lastIndex) + part.length();
					if (index == -1)
						index = item.length();
					
					partToShow = item.substring(lastIndex, index);
					lastIndex = index;
					
					final Text partLabel = new Text(partToShow);
					//partLabel.setStyle("-fx-border-color: black");
					addClickListener(itemGroups, partLabel, i);
					if (i < groupColors.length) {
						Color color = groupColors[i];
						Color selected = color.deriveColor(0, 1, 3, 1);

						ObjectBinding<Color> colorObjectBinding = new When(selectedRow).then(selected).otherwise(color);
						partLabel.fillProperty().bind(colorObjectBinding);
					}
					flowPaneChildren.add(partLabel);
				}
			}

            private void addClickListener(final List<String> itemGroups, Node partLabel, final int fromIndex) {
                partLabel.setOnMouseClicked(new EventHandler<MouseEvent>() {
                    @Override
                    public void handle(MouseEvent event) {
                        String commentRemainder = Joiner.on(" ").join(itemGroups.subList(fromIndex, itemGroups.size()));
                        setCommentToClipboard(commentRemainder);
                    }
                });
            }
        }
    }
}

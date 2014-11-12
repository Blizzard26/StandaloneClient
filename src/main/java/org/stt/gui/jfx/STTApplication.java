package org.stt.gui.jfx;

import com.google.common.base.Optional;
import static com.google.common.base.Preconditions.checkNotNull;
import com.sun.javafx.application.PlatformImpl;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.binding.SetBinding;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.stt.CommandHandler;
import org.stt.gui.jfx.ResultViewConfigurer.Callback;
import org.stt.model.TimeTrackingItem;
import org.stt.model.TimeTrackingItemFilter;
import org.stt.persistence.IOUtil;
import org.stt.persistence.ItemReader;
import org.stt.persistence.ItemReaderProvider;
import org.stt.searching.ExpansionProvider;

public class STTApplication implements Callback {

	private static final Logger LOG = Logger.getLogger(STTApplication.class
			.getName());

	private final ExecutorService executorService;
	private final CommandHandler commandHandler;
	private final ItemReaderProvider historySourceProvider;
	private final ReportWindowBuilder reportWindowBuilder;
	private final ExpansionProvider expansionProvider;

	final ObservableList<TimeTrackingItem> allItems = FXCollections
			.observableArrayList();
	ListProperty<TimeTrackingItem> filteredList = new SimpleListProperty<TimeTrackingItem>(FXCollections
			.<TimeTrackingItem>observableArrayList());
	final StringProperty currentCommand = new SimpleStringProperty("");
	final IntegerProperty commandCaretPosition = new SimpleIntegerProperty();
	Property<TimeTrackingItem> selectedItem = new SimpleObjectProperty<>();

	ViewAdapter viewAdapter;

	private STTApplication(Builder builder) {
		this.expansionProvider = checkNotNull(builder.expansionProvider);
		this.reportWindowBuilder = checkNotNull(builder.reportWindowBuilder);
		this.commandHandler = checkNotNull(builder.commandHandler);
		this.historySourceProvider = checkNotNull(builder.historySourceProvider);
		this.executorService = checkNotNull(builder.executorService);

		ResultViewConfigurer resultViewConfigurer = new ResultViewConfigurer();
		resultViewConfigurer.configure(filteredList, selectedItem, allItems,
				currentCommand, STTApplication.this);

	}

	void readHistoryFrom(final ItemReader reader) {
		executorService.execute(new FutureTask<Void>(new Runnable() {

			@Override
			public void run() {
				Collection<TimeTrackingItem> allItems;
				try {
					allItems = IOUtil.readAll(reader);
				} catch (IOException ex) {
					LOG.log(Level.SEVERE, null, ex);
					throw new RuntimeException(ex);
				}
				viewAdapter.updateAllItems(allItems);
			}
		}, null));
	}

	@Override
	public void textOfSelectedItem(String textToSet) {
		setCommandText(textToSet);
		viewAdapter.requestFocusOnCommandText();
	}

	private void setCommandText(String textToSet) {
		currentCommand.set(textToSet);
		commandCaretPosition.set(currentCommand.get().length());
	}

	void expandCurrentCommand() {
		int caretPosition = commandCaretPosition.get();
		String textToExpand = currentCommand.get().substring(0, caretPosition);
		List<String> expansions = expansionProvider
				.getPossibleExpansions(textToExpand);
		if (!expansions.isEmpty()) {
			String maxExpansion = expansions.get(0);
			for (String exp : expansions) {
				maxExpansion = commonPrefix(maxExpansion, exp);
			}
			String tail = currentCommand.get().substring(caretPosition);
			String expandedText = textToExpand + maxExpansion;
			currentCommand.set(expandedText + tail);
			commandCaretPosition.set(expandedText.length());
		}
	}

	String commonPrefix(String a, String b) {
		for (int i = 0; i < a.length() && i < b.length(); i++) {
			if (a.charAt(i) != b.charAt(i)) {
				return a.substring(0, i);
			}
		}
		return a;
	}

	protected Optional<TimeTrackingItem> executeCommand() {
		final String text = currentCommand.get();
		if (!text.trim().isEmpty()) {
			Optional<TimeTrackingItem> result = commandHandler.executeCommand(text);
			clearCommand();
			return result;
		}
		return Optional.<TimeTrackingItem>absent();
	}

	private void clearCommand() {
		currentCommand.set("");
	}

	public void show(Stage stage) {
		viewAdapter = new ViewAdapter(stage);
		viewAdapter.show();
	}

	public void start(final Stage stage) {
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				try {
					show(stage);
					readHistoryFrom(historySourceProvider.provideReader());
				} catch (Exception e) {
					LOG.log(Level.SEVERE, "Couldn't start", e);
				}
			}
		});
	}

	@Override
	public void continueItem(TimeTrackingItem item) {
		commandHandler.resumeGivenItem(item);
		viewAdapter.shutdown();
	}

	@Override
	public void edit(TimeTrackingItem item) {
		setCommandText(commandHandler.itemToCommand(item));
	}

	@Override
	public void delete(TimeTrackingItem item) {
		try {
			commandHandler.delete(checkNotNull(item));
			allItems.remove(item);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static class Builder {

		private ExecutorService executorService;
		private CommandHandler commandHandler;
		private ItemReaderProvider historySourceProvider;
		private ReportWindowBuilder reportWindowBuilder;
		private ExpansionProvider expansionProvider;

		public Builder executorService(ExecutorService executorService) {
			this.executorService = executorService;
			return this;
		}

		public Builder commandHandler(CommandHandler commandHandler) {
			this.commandHandler = commandHandler;
			return this;
		}

		public Builder historySourceProvider(ItemReaderProvider historySourceProvider) {
			this.historySourceProvider = historySourceProvider;
			return this;
		}

		public Builder reportWindowBuilder(
				ReportWindowBuilder reportWindowBuilder) {
			this.reportWindowBuilder = reportWindowBuilder;
			return this;
		}

		public Builder expansionProvider(ExpansionProvider expansionProvider) {
			this.expansionProvider = expansionProvider;
			return this;
		}

		public STTApplication build() {
			return new STTApplication(this);
		}
	}

	public class ViewAdapter {

		private final Stage stage;

		@FXML
		TextArea commandText;

		@FXML
		Button finButton;

		@FXML
		Button insertButton;

		@FXML
		ListView<TimeTrackingItem> result;

		ViewAdapter(Stage stage) {
			this.stage = stage;
		}

		protected void show() throws RuntimeException {
			final ResourceBundle localization = ResourceBundle
					.getBundle("org.stt.gui.Application");
			FXMLLoader loader = new FXMLLoader(getClass().getResource(
					"/org/stt/gui/jfx/MainWindow.fxml"), localization);
			loader.setController(this);

			BorderPane pane;
			try {
				pane = (BorderPane) loader.load();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}

			Scene scene = new Scene(pane);

			stage.setScene(scene);
			stage.setTitle(localization.getString("window.title"));
			Image applicationIcon = new Image("/Logo.png", 32, 32, true, true);
			stage.getIcons().add(applicationIcon);

			stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
				@Override
				public void handle(WindowEvent arg0) {
					Platform.runLater(new Runnable() {

						@Override
						public void run() {
							shutdown();
						}
					});
				}
			});
			stage.show();
			requestFocusOnCommandText();
		}

		protected void shutdown() {
			stage.close();
			executorService.shutdown();
			Platform.exit();
			// Required because txExit() above is just swallowed...
			PlatformImpl.tkExit();
			try {
				executorService.awaitTermination(1, TimeUnit.SECONDS);
				commandHandler.close();
			} catch (InterruptedException | IOException e) {
				throw new RuntimeException(e);
			}
			// System.exit(0);
		}

		public void initialize() {
			insertButton.setMnemonicParsing(true);
			finButton.setMnemonicParsing(true);
			setupCellFactory();
			final MultipleSelectionModel<TimeTrackingItem> selectionModel = result.getSelectionModel();
			selectionModel.setSelectionMode(SelectionMode.SINGLE);

			bindCaretPosition();
			commandText.textProperty().bindBidirectional(currentCommand);
			result.itemsProperty().bindBidirectional(filteredList);
			bindItemSelection();
		}

		private void bindItemSelection() {
			result.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<TimeTrackingItem>() {

				@Override
				public void changed(ObservableValue<? extends TimeTrackingItem> observable, TimeTrackingItem oldValue, TimeTrackingItem newValue) {
					selectedItem.setValue(newValue);
				}
			});
			selectedItem.addListener(new ChangeListener<TimeTrackingItem>() {

				@Override
				public void changed(ObservableValue<? extends TimeTrackingItem> observable, TimeTrackingItem oldValue, TimeTrackingItem newValue) {
					result.getSelectionModel().select(newValue);
				}
			});
		}

		private void setupCellFactory() {
			final ObservableSet<TimeTrackingItem> firstItemOfDayBinding = createFirstItemOfDayBinding(allItems);
			result.setCellFactory(new TimeTrackingItemCellFactory(STTApplication.this,
					STTApplication.this, STTApplication.this, new TimeTrackingItemFilter() {
						@Override
						public boolean filter(TimeTrackingItem item) {
							return firstItemOfDayBinding.contains(item);
						}
					}));
		}

		private ObservableSet<TimeTrackingItem> createFirstItemOfDayBinding(
				final ObservableList<TimeTrackingItem> allItems) {
			return new SetBinding<TimeTrackingItem>() {
				{
					bind(allItems);
				}

				@Override
				protected ObservableSet<TimeTrackingItem> computeValue() {
					Set<TimeTrackingItem> result = getSetOfFirstItemOfTheDayIn(allItems);
					return FXCollections.observableSet(result);
				}

			};
		}

		private Set<TimeTrackingItem> getSetOfFirstItemOfTheDayIn(
				Collection<TimeTrackingItem> allItems) {
			SortedSet<TimeTrackingItem> items = new TreeSet<>(
					TimeTrackingItem.BY_START_COMPARATOR);
			items.addAll(allItems);
			TimeTrackingItem lastItem = null;
			for (Iterator<TimeTrackingItem> it = items.iterator(); it.hasNext();) {
				TimeTrackingItem item = it.next();
				if (lastItem != null
						&& lastItem.getStart().withTimeAtStartOfDay()
						.equals(item.getStart().withTimeAtStartOfDay())) {
					it.remove();
				}
				lastItem = item;
			}
			return items;
		}

		private void bindCaretPosition() {
			commandCaretPosition.addListener(new InvalidationListener() {
				@Override
				public void invalidated(Observable observable) {
					commandText.positionCaret(commandCaretPosition.get());
				}
			});
			commandText.caretPositionProperty().addListener(new InvalidationListener() {
				@Override
				public void invalidated(Observable observable) {
					commandCaretPosition.set(commandText.getCaretPosition());
				}
			});
		}

		protected void requestFocusOnCommandText() {
			PlatformImpl.runLater(new Runnable() {
				@Override
				public void run() {
					commandText.requestFocus();
				}
			});
		}

		protected void updateAllItems(final Collection<TimeTrackingItem> updateWith) {
			PlatformImpl.runLater(new Runnable() {
				@Override
				public void run() {
					allItems.setAll(updateWith);
					viewAdapter.requestFocusOnCommandText();
				}
			});
		}

		@FXML
		void showReportWindow() {
			try {
				reportWindowBuilder.setupStage();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		@FXML
		private void done() {
			executeCommand();
			shutdown();
		}

		@FXML
		void insert() {
			Optional<TimeTrackingItem> item = executeCommand();
			if (item.isPresent()) {
				readHistoryFrom(historySourceProvider.provideReader());
			}
		}

		@FXML
		private void fin() {
			commandHandler.endCurrentItem();
			shutdown();
		}

		@FXML
		private void onKeyPressed(KeyEvent event) {
			if (KeyCode.ENTER.equals(event.getCode()) && event.isControlDown()) {
				event.consume();
				done();
			}
			if (KeyCode.ESCAPE.equals(event.getCode())) {
				event.consume();
				shutdown();
			}
			if (KeyCode.SPACE.equals(event.getCode()) && event.isControlDown()) {
				expandCurrentCommand();
				event.consume();
			}
		}

	}
}

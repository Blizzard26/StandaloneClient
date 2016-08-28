package org.stt.gui.jfx;

import static com.google.common.base.Preconditions.checkNotNull;

import java.awt.Desktop;
import java.awt.MouseInfo;
import java.awt.Point;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.TokenStream;
import org.stt.command.Command;
import org.stt.command.CommandParser;
import org.stt.command.NothingCommand;
import org.stt.g4.EnglishCommandsLexer;
import org.stt.g4.EnglishCommandsParser;
import org.stt.gui.jfx.text.CommandHighlighter;
import org.stt.gui.jfx.text.HighlightingOverlay;

import com.google.inject.Provider;
import com.google.inject.Singleton;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

@Singleton
public class LogWorkWindowBuilder {
	
	Logger LOG = Logger.getLogger(LogWorkWindowBuilder.class.getName());

	private Provider<Stage> stageProvider;
	
	final StringProperty currentCommand = new SimpleStringProperty("");

	private CommandParser commandParser;

	private LogWorkWindowController controller;

	public LogWorkWindowBuilder(Provider<Stage> stageProvider, CommandParser commandParser) {
        this.stageProvider = checkNotNull(stageProvider);
        this.commandParser = checkNotNull(commandParser);
	}
	
	public void setupStage() throws IOException {
		// Close previous stage if it exists
		if (controller != null)
		{
			controller.closeWindow();
			controller = null;
		}
		
        Stage stage = new Stage();

        controller = new LogWorkWindowController(stage);

        ResourceBundle localization = ResourceBundle
                .getBundle("org.stt.gui.Application");
        FXMLLoader loader = new FXMLLoader(getClass().getResource(
                "/org/stt/gui/jfx/LogWorkWindow.fxml"), localization);
        loader.setController(controller);
        loader.load();
        
        

        stage.show();
        positionStage(stage);
        stage.setIconified(false);
    }

	private void positionStage(Stage stage) {
		Point mouseLocation = MouseInfo.getPointerInfo().getLocation();
        stage.setX(mouseLocation.getX() - stage.getWidth());
        stage.setY(mouseLocation.getY() - stage.getHeight());
	}
	
    protected boolean executeCommand() {
        final String text = currentCommand.get();
        if (!text.trim().isEmpty()) {
            Command command = commandParser
                    .parseCommandString(text).or(NothingCommand.INSTANCE);
//            if (command instanceof NewItemCommand) {
//                TimeTrackingItem newItem = ((NewItemCommand) command).newItem;
//                DateTime start = newItem.getStart();
//                if (!validateItemIsFirstItemAndLater(start) || !validateItemWouldCoverOtherItems(newItem)) {
//                    command = NothingCommand.INSTANCE;
//                }
//            }
            if (!NothingCommand.INSTANCE.equals(command)) {
                command.execute();
                clearCommand();
                return true;
            }
        }
        return false;
    }
    
    private void clearCommand() {
        currentCommand.set("");
    }

	public class LogWorkWindowController {
		
		private final Stage stage;
		
		@FXML
        private BorderPane borderPane;
		
		@FXML
		private TextArea commandText;
		
		@FXML
		private Button insertButton;

		private HighlightingOverlay overlay;

		private CommandHighlighter commandHighlighter;
		
		public LogWorkWindowController(Stage stage) {
			this.stage = stage;
		}
		
		@FXML
		public void initialize() {
            commandText.textProperty().bindBidirectional(currentCommand);
            overlay = new HighlightingOverlay(commandText);
            commandHighlighter = new CommandHighlighter(overlay);

            
            stage.initStyle(StageStyle.UNDECORATED);
            
            

            requestFocusOnCommandText();
            
            stage.focusedProperty().addListener(new ChangeListener<Boolean>() {

				@Override
				public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean hasFocus) {
					if (!hasFocus)
						closeWindow();
				}
			});

            commandText.textProperty().addListener(new ChangeListener<String>() {
                @Override
                public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                    overlay.clearHighlights();
                    CharStream input = new ANTLRInputStream(currentCommand.get());
                    EnglishCommandsLexer lexer = new EnglishCommandsLexer(input);
                    TokenStream tokenStream = new CommonTokenStream(lexer);
                    EnglishCommandsParser parser = new EnglishCommandsParser(tokenStream);
                    commandHighlighter.addHighlights(parser.command());
                }
            });
            
            
            addSceneToStage();
        }
		
		private void addSceneToStage() {
            Scene scene = new Scene(borderPane);
            //stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(scene);
            stage.setAlwaysOnTop(true);
            

            scene.setOnKeyPressed(new EventHandler<KeyEvent>() {

                @Override
                public void handle(KeyEvent event) {
                    if (KeyCode.ESCAPE.equals(event.getCode())) {
                        event.consume();
                        closeWindow();
                    }
                }
            });
        }
		
		@FXML
        public void closeWindow() {
            stage.close();
            controller = null;
        }
		
		@FXML
        private void done() {
            executeCommand();
            closeWindow();
        }
		
		@FXML
        void insert() {
            executeCommand();
        }
		

		@FXML
        private void onKeyPressed(KeyEvent event) {
            if (KeyCode.ENTER.equals(event.getCode()) && event.isControlDown()) {
                event.consume();
                done();
            }
            if (KeyCode.SPACE.equals(event.getCode()) && event.isControlDown()) {
                //expandCurrentCommand();
                event.consume();
            }
            if (KeyCode.F1.equals(event.getCode())) {
                try {
                    Desktop.getDesktop()
                            .browse(new URI(
                                    "https://github.com/Bytekeeper/STT/wiki/CLI"));
                } catch (IOException | URISyntaxException ex) {
                    LOG.log(Level.SEVERE, null, ex);
                }
            }
        }
		
		public void requestFocusOnCommandText() {
			Platform.runLater(new Runnable() {
	            @Override
	            public void run() {
	                commandText.requestFocus();
	            }
	        });
		}
	}


	
}

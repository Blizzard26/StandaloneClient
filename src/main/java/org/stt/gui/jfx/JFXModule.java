package org.stt.gui.jfx;

import java.util.ResourceBundle;

import org.stt.command.CommandParser;
import org.stt.config.YamlConfigService;
import org.stt.persistence.ItemReaderProvider;
import org.stt.query.TimeTrackingItemQueries;
import org.stt.text.ItemGrouper;
import org.stt.text.WorktimeCategorizer;
import org.stt.time.DurationRounder;

import com.google.inject.AbstractModule;
import com.google.inject.Provider;
import com.google.inject.Provides;

import javafx.stage.Stage;

/**
 * Created by dante on 03.12.14.
 */
public class JFXModule extends AbstractModule {
    @Override
    protected void configure() {
    	bind(ApplicationControl.class).to(STTApplication.class);
    }

    @Provides
    private ReportWindowBuilder createReportWindowBuilder(TimeTrackingItemQueries timeTrackingItemQueries, ItemReaderProvider itemReaderProvider,
                                                          DurationRounder durationRounder, ItemGrouper itemGrouper, Provider<Stage> stageProvider, ResourceBundle resourceBundle,
                                                          WorktimeCategorizer worktimeCategorizer, YamlConfigService yamlConfig) {
        return new ReportWindowBuilder(
                stageProvider, resourceBundle, itemReaderProvider,
                timeTrackingItemQueries, durationRounder, itemGrouper, worktimeCategorizer, yamlConfig.getConfig().getReportWindowConfig());
    }
    
    @Provides
    private LogWorkWindowBuilder createLogWorkWindowBuilder(Provider<Stage> stageProvider, CommandParser commandParser)
    {
    	return new LogWorkWindowBuilder(stageProvider, commandParser);
    }
}

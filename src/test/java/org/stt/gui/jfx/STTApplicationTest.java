package org.stt.gui.jfx;

import com.google.common.base.Optional;
import com.google.common.eventbus.EventBus;
import com.google.inject.Provider;
import javafx.stage.Stage;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.stt.CommandHandler;
import org.stt.analysis.ExpansionProvider;
import org.stt.analysis.ItemGrouper;
import org.stt.config.CommandTextConfig;
import org.stt.config.TimeTrackingItemListConfig;
import org.stt.fun.Achievement;
import org.stt.fun.AchievementService;
import org.stt.model.TimeTrackingItem;
import org.stt.persistence.ItemReader;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class STTApplicationTest {

    private STTApplication sut;
    @Mock
    private CommandHandler commandHandler;
    @Mock
    private ExecutorService executorService;
    @Mock
    private ReportWindowBuilder reportWindowBuilder;
    @Mock
    private ItemGrouper grouper;
    @Mock
    private ExpansionProvider expansionProvider;
    @Mock
    private ResourceBundle resourceBundle;
    private boolean shutdownCalled;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        sut = new STTApplication(new DeleteOrKeepDialog(resourceBundle), new EventBus(), commandHandler, reportWindowBuilder, expansionProvider, resourceBundle, new TimeTrackingItemListConfig(), new CommandTextConfig());
        sut.viewAdapter = sut.new ViewAdapter(null) {

            @Override
            protected void show() throws RuntimeException {
            }

            @Override
            protected void requestFocusOnCommandText() {
            }

            @Override
            protected void updateAllItems(
                    Collection<TimeTrackingItem> updateWith) {
                sut.allItems.setAll(updateWith);
            }

            @Override
            protected void shutdown() {
                shutdownCalled = true;
            }
        };
    }

    @Test
    public void shouldDelegateToExpansionProvider() {
        // GIVEN

        setTextAndPositionCaretAtEnd("test");

        given(expansionProvider.getPossibleExpansions("test")).willReturn(
                Arrays.asList("blub"));

        // WHEN
        sut.expandCurrentCommand();

        // THEN
        assertThat(sut.currentCommand.get(), is("testblub"));
    }

    @Test
    public void shouldExpandWithinText() {
        // GIVEN

        sut.currentCommand.set("al beta");
        sut.commandCaretPosition.set(2);

        given(expansionProvider.getPossibleExpansions("al")).willReturn(
                Arrays.asList("pha"));

        // WHEN
        sut.expandCurrentCommand();

        // THEN
        assertThat(sut.currentCommand.get(), is("alpha beta"));
        assertThat(sut.commandCaretPosition.get(), is(5));
    }

    @Test
    public void shouldExpandToCommonPrefix() {
        // GIVEN

        String currentText = "test";
        setTextAndPositionCaretAtEnd(currentText);

        given(expansionProvider.getPossibleExpansions(currentText)).willReturn(
                Arrays.asList("aaa", "aab"));

        // WHEN
        sut.expandCurrentCommand();

        // THEN
        assertThat(sut.currentCommand.get(), is("testaa"));
    }

    private void setTextAndPositionCaretAtEnd(String currentText) {
        sut.currentCommand.set(currentText);
        sut.commandCaretPosition.set(currentText.length());
    }

    @Test
    public void shouldDeleteItemIfRequested() throws IOException {
        // GIVEN
        TimeTrackingItem item = new TimeTrackingItem(null, DateTime.now());

        // WHEN
        sut.delete(item);

        // THEN
        verify(commandHandler).delete(item);
    }

    @Test
    public void deletedItemShouldBeRemoved() {
        // GIVEN
        givenExecutorService();
        final TimeTrackingItem item = new TimeTrackingItem("comment",
                DateTime.now());

        sut.allItems.setAll(item);

        // WHEN
        sut.delete(item);

        // THEN
        assertThat(sut.filteredList, not(hasItem(item)));
    }

    @Test
    public void shouldShowReportWindow() throws IOException {
        // GIVEN

        // WHEN
        sut.viewAdapter.showReportWindow();

        // THEN
        verify(reportWindowBuilder).setupStage();
    }

    @Test
    public void shouldClearCommandAreaOnExecuteCommand() throws Exception {
        // GIVEN
        givenCommand("test");

        // WHEN
        sut.executeCommand();

        // THEN
        assertThat(sut.currentCommand.get(), equalTo(""));
    }

    @Test
    public void shouldDelegateCommandExecutionToCommandHandler()
            throws Exception {
        // GIVEN
        String testCommand = "test";

        givenCommand(testCommand);

        // WHEN
        sut.executeCommand();

        // THEN
        verify(commandHandler).executeCommand(testCommand);
    }

    @Test
    public void shouldNotCloseWindowOnInsert() {
        // GIVEN
        givenCommand("Hello World");
        given(commandHandler.executeCommand(anyString())).willReturn(
                Optional.<TimeTrackingItem>absent());

        // WHEN
        sut.viewAdapter.insert();

        // THEN
        assertThat(shutdownCalled, is(false));
    }

    private ItemReader givenReaderThatReturns(final TimeTrackingItem item) {
        ItemReader reader = mock(ItemReader.class);
        given(reader.read()).willReturn(Optional.of(item),
                Optional.<TimeTrackingItem>absent());
        return reader;
    }

    private void givenExecutorService() {
        willAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                ((Runnable) invocation.getArguments()[0]).run();
                return null;
            }
        }).given(executorService).execute(any(Runnable.class));
    }

    private void givenCommand(String command) {
        sut.currentCommand.set(command);
    }
}

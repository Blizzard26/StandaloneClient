package org.stt.gui.jfx;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;

import javafx.scene.control.TextArea;
import javafx.stage.Stage;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.stt.CommandHandler;
import org.stt.gui.jfx.JFXTestRunner.NotOnPlatformThread;
import org.stt.model.TimeTrackingItem;
import org.stt.persistence.ItemReader;
import org.stt.reporting.ItemGrouper;
import org.stt.searching.CommentSearcher;

import com.google.common.base.Optional;

@RunWith(JFXTestRunner.class)
public class STTApplicationTest {
	private STTApplication sut;
	private final JFXTestHelper helper = new JFXTestHelper();
	private Stage stage;

	@Mock
	private CommandHandler commandHandler;

	@Mock
	private ExecutorService executorService;

	@Mock
	private ReportWindowBuilder reportWindow;

	@Mock
	protected CommentSearcher commentSearcher;

	@Mock
	private ItemGrouper grouper;

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);

		helper.invokeAndWait(new Runnable() {

			@Override
			public void run() {
				stage = helper.createStageForTest();
				ItemReader historySource = mock(ItemReader.class);
				sut = new STTApplication(stage, commandHandler, historySource,
						executorService, reportWindow, commentSearcher, grouper);
			}
		});
	}

	@Test
	@NotOnPlatformThread
	public void shouldDelegateToGrouper() {
		// GIVEN
		setupStage();

		sut.commandText.setText("test");

		given(grouper.getPossibleExpansions("test")).willReturn(
				Arrays.asList("blub"));

		// WHEN
		sut.expandCurrentCommand();

		// THEN
		assertThat(sut.commandText.getText(), is("testblub"));
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
		setupStage();

		sut.allItems.setAll(item);

		// WHEN
		sut.delete(item);

		// THEN
		assertThat(sut.result.getItems(), not(hasItem(item)));
	}

	@Test
	public void shouldShowReportWindow() throws IOException {
		// GIVEN

		// WHEN
		sut.showReportWindow();

		// THEN
		verify(reportWindow).setupStage();
	}

	@Test
	public void shouldShowWindow() throws Exception {

		// GIVEN

		// WHEN
		sut.setupStage();

		// THEN
		assertThat(stage.isShowing(), is(true));

	}

	@Ignore
	@Test
	public void shouldClearCommandAreaOnExecuteCommand() throws Exception {
		// GIVEN
		sut.setupStage();
		TextArea commandArea = getCommandArea();
		commandArea.setText("test");

		// WHEN
		sut.executeCommand();

		// THEN
		assertThat(commandArea.getText(), equalTo(""));
	}

	@Ignore
	@Test
	public void shouldDelegateCommandExecutionToCommandHandler()
			throws Exception {
		// GIVEN
		String testCommand = "test";

		sut.setupStage();
		givenCommand(testCommand);

		// WHEN
		sut.executeCommand();

		// THEN
		verify(commandHandler).executeCommand(testCommand);
	}

	@SuppressWarnings("unchecked")
	@Test
	@NotOnPlatformThread
	public void shouldReadHistoryItemsFromReader() throws Exception {
		// GIVEN
		givenExecutorService();

		final TimeTrackingItem item = new TimeTrackingItem("comment",
				DateTime.now());

		setupStage();

		ItemReader reader = givenReaderThatReturns(item);

		// WHEN
		sut.readHistoryFrom(reader);

		// THEN
		helper.invokeAndWait(new Runnable() {

			@Override
			public void run() {
				verify(executorService).execute(any(Runnable.class));
				assertThat(
						sut.result.getItems().toArray(new TimeTrackingItem[0]),
						is(new TimeTrackingItem[] { item }));
			}
		});
	}

	private ItemReader givenReaderThatReturns(final TimeTrackingItem item) {
		ItemReader reader = mock(ItemReader.class);
		given(reader.read()).willReturn(Optional.of(item),
				Optional.<TimeTrackingItem> absent());
		return reader;
	}

	private void setupStage() {
		helper.invokeAndWait(new Runnable() {

			@Override
			public void run() {
				try {
					sut.setupStage();
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		});
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
		TextArea commandArea = getCommandArea();
		commandArea.setText(command);
	}

	private TextArea getCommandArea() {
		return (TextArea) stage.getScene().lookup("*#commandText");
	}
}

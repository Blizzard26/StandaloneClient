package org.stt.gui;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import javafx.embed.swing.JFXPanel;

import org.junit.Test;
import org.stt.gui.jfx.JFXTestHelper;
import org.stt.gui.jfx.STTApplication;

import com.google.inject.Guice;
import com.google.inject.Injector;

public class MainTest<V> {
	private final Main sut = new Main();
	private final JFXTestHelper helper = new JFXTestHelper();

	@Test
	public void shouldBindSTTApplication() throws InterruptedException,
			ExecutionException {
		// GIVEN
		final Injector injector = Guice.createInjector(sut);
		new JFXPanel();

		// WHEN
		STTApplication result = helper
				.invokeAndWait(new Callable<STTApplication>() {
					@Override
					public STTApplication call() throws Exception {
						return injector.getInstance(STTApplication.class);
					}
				});

		// THEN
		assertThat(result, notNullValue());
	}

	@Test
	public void shouldDelegateStartToStartOfApplication() {
		// GIVEN

		Injector injector = mock(Injector.class);
		STTApplication application = mock(STTApplication.class);
		given(injector.getInstance(STTApplication.class)).willReturn(
				application);

		// WHEN
		sut.start(injector);

		// THEN
		verify(application).start();
	}
}

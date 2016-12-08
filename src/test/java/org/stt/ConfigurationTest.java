package org.stt;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

public class ConfigurationTest {
	private Configuration sut;

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	private File currentTempFolder;

	// Mockito can't mock method called by the constructor, hence we need this mock-class
	private static class ConfigurationMock extends Configuration
	{
		private File tempFolder;

		public ConfigurationMock(File tempFolder) {
			this.tempFolder = tempFolder;
		}

		@Override
		public File determineBaseDir() {
			return tempFolder;
		}
		
	}
	
	@Before
	public void setUp() throws IOException {
		currentTempFolder = tempFolder.newFolder();
		sut = new ConfigurationMock(currentTempFolder);
	}
	
	@Test
	public void shouldBeAbleToProvideSTTFile() {
		// GIVEN

		// WHEN
		File sttFile = sut.getSttFile();

		// THEN
		assertThat(sttFile.getAbsoluteFile(), is(new File(currentTempFolder,
				".stt").getAbsoluteFile()));
	}

	@Test
	public void shouldReturnDefaultBreakTimes() {

		// GIVEN

		// WHEN
		Collection<String> breakTimeComments = sut.getBreakTimeComments();

		// THEN
		assertThat(breakTimeComments,
				containsInAnyOrder("break", "pause", "coffee"));
	}

}

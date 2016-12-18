package org.stt.cli;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.stt.Configuration;
import org.stt.persistence.ItemPersister;
import org.stt.persistence.ItemReaderProvider;
import org.stt.persistence.ItemWriter;
import org.stt.persistence.stt.STTItemPersister;
import org.stt.persistence.stt.STTItemReader;
import org.stt.persistence.stt.STTItemWriter;
import org.stt.query.DefaultTimeTrackingItemQueries;
import org.stt.query.TimeTrackingItemQueries;
import org.stt.reporting.WorkingtimeItemProvider;
import org.stt.text.ItemCategorizer;
import org.stt.text.WorktimeCategorizer;

import com.google.inject.Provider;
import com.google.inject.ProvisionException;

public class MainTest {
	private Main sut;

	@Mock
	private Configuration configuration;

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	private File currentSttFile;
	private File currentTiFile;
	private File currentTiCurrentFile;

	private Provider<ItemPersister> providerItemPersister;

	private TimeTrackingItemQueries timeTrackingItemQueries;

	private ReportPrinter reportPrinter;

	protected Provider<Reader> readerProvider;

	protected Provider<Writer> writerProvider;

	private ItemReaderProvider itemReaderProvider;

	private Provider<ItemWriter> itemWriterProvider;
	
	private WorkingtimeItemProvider workingtimeItemProvider;

	private ItemCategorizer categorizer;

	@Before
	public void setup() throws IOException {
		MockitoAnnotations.initMocks(this);

		currentSttFile = tempFolder.newFile();
		Mockito.when(configuration.getSttFile()).thenReturn(currentSttFile);
		currentTiFile = tempFolder.newFile();
		Mockito.when(configuration.getTiFile()).thenReturn(currentTiFile);
		currentTiCurrentFile = tempFolder.newFile();
		Mockito.when(configuration.getTiCurrentFile()).thenReturn(
				currentTiCurrentFile);
		
		File tempFile = tempFolder.newFile();

		// populate test file
		FileUtils.write(tempFile,
				"2014-01-01 14\nhoursMon = 10\n2014-02-02 10 14", Charset.defaultCharset());
		// end populate

		given(configuration.getWorkingTimesFile()).willReturn(tempFile);
		
		readerProvider = () -> {
			try {
				return new FileReader(currentSttFile);
			} catch (FileNotFoundException e) {
				throw new ProvisionException(e.getMessage(), e);
			}
		};
		
		writerProvider = () -> {
			try {
				return new FileWriter(currentSttFile);
			} catch (IOException e) {
				throw new ProvisionException(e.getMessage(), e);
			}
		};
		providerItemPersister = () -> new STTItemPersister(readerProvider, writerProvider);

		itemReaderProvider = () -> new STTItemReader(readerProvider.get());
		itemWriterProvider = () -> new STTItemWriter(writerProvider.get());
		timeTrackingItemQueries = new DefaultTimeTrackingItemQueries(itemReaderProvider);
		
		workingtimeItemProvider = new WorkingtimeItemProvider(configuration);
		
		categorizer = new WorktimeCategorizer(configuration);
		
		reportPrinter = new ReportPrinter(itemReaderProvider, configuration, workingtimeItemProvider, categorizer);
		
		sut = new Main(providerItemPersister, itemReaderProvider, itemWriterProvider, timeTrackingItemQueries, reportPrinter);
	}

	@Test
	public void startingWorkWritesToConfiguredFile() throws IOException {

		// GIVEN
		String expectedComment = "some long comment we are currently working on";
		List<String> args = new ArrayList<>();
		String command = "on " + expectedComment;
		args.addAll(Arrays.asList(command.split(" ")));
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintStream ps = new PrintStream(baos, true, "UTF-8");

		// WHEN
		sut.executeCommand(args, ps);

		// THEN
		List<String> readLines = IOUtils.readLines(new InputStreamReader(
				new FileInputStream(currentSttFile), "UTF-8"));
		Assert.assertThat(readLines, contains(containsString(expectedComment)));

		String returned = baos.toString("UTF-8");
		Assert.assertThat(returned, containsString(expectedComment));

		ps.close();
	}

	@Test
	public void testOn() throws UnsupportedEncodingException
	{
		// GIVEN
		String expectedComment = "some long comment we are currently working on";
		List<String> args = new ArrayList<>();
		String command = "on " + expectedComment;
		args.addAll(Arrays.asList(command.split(" ")));
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintStream ps = new PrintStream(baos, true, "UTF-8");

		// WHEN
		sut.executeCommand(args, ps);

		// THEN
	}
	
	@Test
	public void testReport() throws UnsupportedEncodingException
	{
		String command = "report";
		List<String> args = new ArrayList<>();
		args.addAll(Arrays.asList(command.split(" ")));
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintStream ps = new PrintStream(baos, true, "UTF-8");
		
		// WHEN
		sut.executeCommand(args, ps);

		// THEN
		assertThat(baos.size(), is(Matchers.greaterThan(0)));
	}
	
	@Test
	public void testFin() throws UnsupportedEncodingException
	{
		String command = "fin";
		List<String> args = new ArrayList<>();
		args.addAll(Arrays.asList(command.split(" ")));
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintStream ps = new PrintStream(baos, true, "UTF-8");
		
		// WHEN
		sut.executeCommand(args, ps);

		// THEN
		//assertThat(baos.size(), is(Matchers.greaterThan(0)));
		System.out.println(baos);
	}
	
	@Test
	public void testSearch() throws UnsupportedEncodingException
	{
		String command = "search some comment";
		List<String> args = new ArrayList<>();
		args.addAll(Arrays.asList(command.split(" ")));
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintStream ps = new PrintStream(baos, true, "UTF-8");
		
		// WHEN
		sut.executeCommand(args, ps);

		// THEN
		//assertThat(baos.size(), is(Matchers.greaterThan(0)));
		System.out.println(baos);
	}
}

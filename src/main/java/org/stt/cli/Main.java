package org.stt.cli;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Logger;

import org.stt.BaseModule;
import org.stt.Configuration;
import org.stt.command.Command;
import org.stt.command.EndCurrentItemCommand;
import org.stt.command.ResumeCommand;
import org.stt.command.ToItemWriterCommandHandler;
import org.stt.config.ConfigModule;
import org.stt.event.EventBusAware;
import org.stt.event.EventBusModule;
import org.stt.model.TimeTrackingItem;
import org.stt.persistence.IOUtil;
import org.stt.persistence.ItemPersister;
import org.stt.persistence.ItemReader;
import org.stt.persistence.ItemReaderProvider;
import org.stt.persistence.ItemWriter;
import org.stt.persistence.db.h2.H2PersistenceModule;
import org.stt.query.DNFClause;
import org.stt.query.FilteredItemReader;
import org.stt.query.TimeTrackingItemQueries;
import org.stt.text.TextModule;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import static com.google.common.base.Preconditions.*;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provider;

/**
 * The starting point for the CLI
 */
public class Main {

	private static Logger LOG = Logger.getLogger(Main.class.getName());
	

	/*
	 * 
	 * CLI use (example from ti usage):
	 * 
	 * ti on long text containing comment //starts a new entry and inserts
	 * comment
	 * 
	 * ti on other comment //sets end time to the previous item and starts the
	 * new one
	 * 
	 * ti fin // sets end time of previous item
	 */
	public static void main(String[] args) throws IOException {
        LOG.info("Starting injector");
        Injector injector = Guice.createInjector(new H2PersistenceModule(), new BaseModule(),  new TextModule(), new ConfigModule(), new EventBusModule());
		
		// apply the desired encoding for all System.out calls
		// this is necessary if one wants to output non ASCII
		// characters on a Windows console
		Configuration configuration = injector.getInstance(Configuration.class);
		System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out),
				true, configuration.getSystemOutEncoding()));

		Main main = injector.getInstance(Main.class);
		List<String> argsList = new ArrayList<>(Arrays.asList(args));
		main.executeCommand(argsList, System.out);

		// perform backup
		//main.createNewBackupCreator(configuration).start();
	}

	//private Injector injector;
	private Provider<ItemPersister> itemPersisterProvider;
	private TimeTrackingItemQueries timeTrackingItemQueries;
	private ItemReaderProvider itemReaderProvider;
	private ReportPrinter reportPrinter;
	private Provider<ItemWriter> itemWriterProvider;

	@Inject public Main(@EventBusAware Provider<ItemPersister> itemPersisterProvider, 
			ItemReaderProvider itemReaderProvider,
			Provider<ItemWriter> itemWriterProvider,
			TimeTrackingItemQueries timeTrackingItemQueries,
			ReportPrinter reportPrinter) {
		this.itemPersisterProvider = checkNotNull(itemPersisterProvider);
		this.itemReaderProvider = checkNotNull(itemReaderProvider);
		this.itemWriterProvider = checkNotNull(itemWriterProvider);
		this.timeTrackingItemQueries = checkNotNull(timeTrackingItemQueries);
		this.reportPrinter = checkNotNull(reportPrinter);
		

	}

	private void on(Collection<String> args, PrintStream printTo)
			throws IOException {
		try (ItemPersister itemPersister = itemPersisterProvider.get())
		{
			
			String comment = Joiner.on(" ").join(args);
	
			Optional<TimeTrackingItem> currentItem = timeTrackingItemQueries
					.getCurrentTimeTrackingitem();
	
			ToItemWriterCommandHandler tiw = new ToItemWriterCommandHandler(
					itemPersister, timeTrackingItemQueries);
			Command executeCommand = tiw.executeCommand(comment);
			Optional<TimeTrackingItem> createdItem = executeCommand.getItem();
	
			if (currentItem.isPresent()) {
				StringBuilder itemString = ItemFormattingHelper
						.prettyPrintItem(currentItem);
				printTo.println("stopped working on " + itemString.toString());
			}
			printTo.println("start working on "
					+ createdItem.get().getComment().orNull());
			tiw.close();
		}
	}

	/**
	 * output all items where the comment contains (ignoring case) the given
	 * args.
	 * 
	 * Only unique comments are printed.
	 * 
	 * The ordering of the output is from newest to oldest.
	 * 
	 * Useful for completion.
	 */
	private void search(Collection<String> args, PrintStream printTo)
			throws IOException {

		SortedSet<TimeTrackingItem> sortedItems = new TreeSet<>(
				new Comparator<TimeTrackingItem>() {

					@Override
					public int compare(TimeTrackingItem o1, TimeTrackingItem o2) {
						return o2.getStart().compareTo(o1.getStart());
					}
				});

		
		ItemReader readFrom = itemReaderProvider.provideReader();

		DNFClause searchFilter = new DNFClause();
		searchFilter.withCommentContains(Joiner.on(" ")
				.join(args));
		ItemReader reader = new FilteredItemReader(readFrom, searchFilter);
		sortedItems.addAll(IOUtil.readAll(reader));

		Set<String> sortedUniqueComments = new HashSet<>(sortedItems.size());

		for (TimeTrackingItem i : sortedItems) {
			String comment = i.getComment().orNull();
			if (comment != null && !sortedUniqueComments.contains(comment)) {
				sortedUniqueComments.add(comment);
				printTo.println(comment);
			}
		}
	}

	private void report(List<String> args, PrintStream printTo) {
		reportPrinter.report(args, printTo);
	}

	private void fin(Collection<String> args, PrintStream printTo)
			throws IOException {
		
		try (ItemPersister itemPersister = itemPersisterProvider.get())
		{
			
			try (ToItemWriterCommandHandler tiw = new ToItemWriterCommandHandler(
					itemPersister, timeTrackingItemQueries)) {
				Optional<TimeTrackingItem> currentItem = timeTrackingItemQueries
						.getCurrentTimeTrackingitem();
				
				Command executeCommand = tiw
								.executeCommand(ToItemWriterCommandHandler.COMMAND_FIN
										+ " " + Joiner.on(" ").join(args));
				prettyPrintExecutedCommand(printTo, currentItem, executeCommand);
			}
		}
	}

	private void prettyPrintExecutedCommand(PrintStream printTo, Optional<TimeTrackingItem> currentItem, Command command) {
		Optional<TimeTrackingItem> updatedItem = command.getItem();
		if (updatedItem.isPresent()) {
			if (command instanceof EndCurrentItemCommand)
			{
				StringBuilder itemString = ItemFormattingHelper
						.prettyPrintItem(updatedItem);
				printTo.println("stopped working on " + itemString.toString());
			}
			else if (command instanceof ResumeCommand)
			{
				if (currentItem.isPresent()) {
					StringBuilder itemString = ItemFormattingHelper
							.prettyPrintItem(currentItem);
					printTo.println("stopped working on " + itemString.toString());
				}
				
				StringBuilder itemString = ItemFormattingHelper
						.prettyPrintItem(updatedItem);
				printTo.println("resumed working on " + itemString.toString());
			}
		}
	}


	void executeCommand(List<String> args, PrintStream printTo) {
		if (args.size() == 0) {
			usage(printTo);
			return;
		}
		
		try {
			String mainOperator = args.remove(0);
			if (mainOperator.startsWith("o")) {
				// on
				on(args, printTo);
			} else if (mainOperator.startsWith("r")) {
				// report
				report(args, printTo);
			} else if (mainOperator.startsWith("f")) {
				// fin
				fin(args, printTo);
			} else if (mainOperator.startsWith("s")) {
				// search
				search(args, printTo);
			} else if (mainOperator.startsWith("c")) {
				// convert
				new FormatConverter(itemWriterProvider.get(), args).convert();
			} else {
				usage(printTo);
			}
		} catch (IOException e) {
			LOG.throwing(Main.class.getName(), "parseCommandString", e);
		}
	}

	/**
	 * Prints usage information to the given Stream
	 */
	private static void usage(PrintStream printTo) {
		String usage = "Usage:\n"
				+ "on comment\t\t\tto start working on something\n"
				+ "report [X days] [searchstring]\tto display a report\n"
				+ "fin [and resume]\t\tto stop working\n"
				+ "search [searchstring]\t\tto get a list of all comments of items matching the given search string\n"
				+ "convert [--sourceFormat stt|ti|csv] [--source sourceFile] [--targetFormat default|stt] [--target targetFile]\tConvert between different time tracking formats\n"
				+ "\t\t\t\t--sourceFormat (optional; Default: stt): one of stt, ti, csv \n"
				+ "\t\t\t\t--sourceFile (optional; Default: StdIn): Input file\n"
				+ "\t\t\t\t--targetFormat (optional; Default: default): Target format, default will use application default\n"
				+ "\t\t\t\t--target (optional; Default: StdOut/default): File to write to; if target format is default, application default will be used and target is ignored\n";

		printTo.println(usage);
	}

//	private BackupCreator createNewBackupCreator(Configuration config) {
//		return new BackupCreator(config);
//	}
}

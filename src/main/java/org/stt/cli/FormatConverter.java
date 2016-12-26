package org.stt.cli;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.util.List;

import org.apache.commons.io.output.FileWriterWithEncoding;
import org.stt.csv.importer.CsvImporter;
import org.stt.model.TimeTrackingItem;
import org.stt.persistence.ItemReader;
import org.stt.persistence.ItemWriter;
import org.stt.persistence.stt.STTItemReader;
import org.stt.persistence.stt.STTItemWriter;
import org.stt.ti.importer.TiImporter;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

/**
 * Converts different supported time tracking formats. Currently these are:
 * 
 * - CSV - STT internal format - modified ti format
 */
public class FormatConverter {

	private ItemReader from;
	private ItemWriter to;
	private ItemWriter defaultItemWriter;

	/**
	 * 
	 * @param input
	 *            if null, System.in is assumed
	 * @param output
	 *            if null, System.out is assumed
	 * @param args
	 */
	public FormatConverter(ItemWriter itemWriter, List<String> args) {
		this.defaultItemWriter = Preconditions.checkNotNull(itemWriter);
		Preconditions.checkNotNull(args);

		File sourceFile = null;
		String sourceFormat = "stt";
		File targetFile = null;
		String targetFormat = "default";
		int sourceFormatIndex = args.indexOf("--sourceFormat");
		if (sourceFormatIndex != -1) {
			args.remove(sourceFormatIndex);
			sourceFormat = args.get(sourceFormatIndex);
			args.remove(sourceFormatIndex);
		}
		int sourceIndex = args.indexOf("--source");
		if (sourceIndex != -1) {
			args.remove(sourceIndex);
			sourceFile = new File(args.get(sourceIndex));
			args.remove(sourceIndex);
		}
		int targetFormatIndex = args.indexOf("--targetFormat");
		if (targetFormatIndex != -1) {
			args.remove(targetFormatIndex);
			targetFormat = args.get(targetFormatIndex);
			args.remove(targetFormatIndex);
		}
		int targetIndex = args.indexOf("--target");
		if (targetIndex != -1) {
			args.remove(targetIndex);
			targetFile = new File(args.get(targetIndex));
			args.remove(targetIndex);
		}

		from = getReaderFrom(sourceFile, sourceFormat);
		to = getWriterFrom(targetFile, targetFormat);
	}

	private ItemWriter getWriterFrom(File output, String targetFormat) {
		try {			
			switch (targetFormat) {
			case "stt":
				if (output == null) {
					return new STTItemWriter(new OutputStreamWriter(System.out,
							"UTF-8"));
				}
				
				return new STTItemWriter(
						new FileWriterWithEncoding(output, "UTF-8"));
			case "default":
				return defaultItemWriter;
			default:
				throw new RuntimeException("unknown input format \"" + targetFormat
						+ "\"");
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private ItemReader getReaderFrom(File input, String sourceFormat) {
		Reader inputReader = null;
		try {
			inputReader = new InputStreamReader(System.in, "UTF-8");
			if (input != null) {
				inputReader = new InputStreamReader(new FileInputStream(input),
						"UTF-8");
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		switch (sourceFormat) {
		case "stt":
			return new STTItemReader(inputReader);
		case "ti":
			return new TiImporter(inputReader);
		case "csv":
			return new CsvImporter(inputReader, 1, 4, 8);
		default:
			throw new RuntimeException("unknown input format \"" + sourceFormat
					+ "\"");
		}
	}

	public void convert() throws IOException {
		System.out.println("Converting...");
		int count = 0;
		Optional<TimeTrackingItem> current = null;
		while ((current = from.read()).isPresent()) {
			to.write(current.get());
			++count;
		}

		from.close();
		to.close();
		System.out.println(count+" items converted.");
	}
}

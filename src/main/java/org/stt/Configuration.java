package org.stt;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;

/**
 * Simple configuration mechanism with fallback values.
 * 
 * Resolves environment variables like HOME if enclosed in $ signs like so:
 * $HOME$/.stt
 */
public class Configuration {
	private static final Logger LOG = Logger.getLogger(Configuration.class
			.getName());

	private final File propertiesFile = new File(System.getenv("HOME"),
			".sttrc");

	private static Configuration instance = null;

	private Properties loadedProps;

	private static final Pattern ENV_PATTERN = Pattern
			.compile(".*\\$(.*)\\$.*");

	private Configuration() {
		loadedProps = new Properties();
		if (propertiesFile.exists()) {
			try (Reader propsReader = new InputStreamReader(
					new FileInputStream(propertiesFile), "UTF-8")) {
				loadedProps.load(propsReader);
			} catch (IOException e) {
				// if the config file cannot be read, defaults are used
				LOG.log(Level.WARNING, "cannot read config file "
						+ propertiesFile.getAbsolutePath(), e);
			}
		} else {
			// create the file from example
			createSttrc();
		}
	}

	private void createSttrc() {

		try (InputStream rcStream = this.getClass().getResourceAsStream(
				"/org/stt/sttrc.example")) {
			IOUtils.copy(rcStream, new FileOutputStream(propertiesFile));
		} catch (IOException e) {
			LOG.log(Level.WARNING, "cannot write example config file "
					+ propertiesFile.getAbsolutePath(), e);
		}
	}

	public static Configuration getInstance() {
		if (instance == null) {
			instance = new Configuration();
		}
		return instance;
	}

	public File getSttFile() {
		String sttFile = getPropertiesReplaced("sttFile", "$HOME$/.stt");
		return new File(sttFile);
	}

	public File getTiFile() {
		String tiFile = getPropertiesReplaced("tiFile", "$HOME$/.ti-sheet");
		return new File(tiFile);
	}

	public File getTiCurrentFile() {
		String tiFile = getPropertiesReplaced("tiCurrentFile",
				"$HOME$/.ti-sheet-current");
		return new File(tiFile);
	}

	public String getSystemOutEncoding() {
		String encoding = getPropertiesReplaced("sysoutEncoding", "UTF-8");
		return encoding;
	}

	public int getCliReportingWidth() {
		int encoding = Integer.parseInt(getPropertiesReplaced(
				"cliReportingWidth", "80"));
		return encoding;
	}

	private String getPropertiesReplaced(String propName, String fallback) {
		String theProperty = loadedProps.getProperty(propName, fallback);
		Matcher envMatcher = ENV_PATTERN.matcher(theProperty);
		if (envMatcher.find()) {
			String group = envMatcher.group(1);
			String getenv = System.getenv(group);
			if (getenv != null) {
				theProperty = theProperty.replace("$" + group + "$", getenv);
			}
		}
		return theProperty;
	}
}

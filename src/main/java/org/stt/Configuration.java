package org.stt;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.joda.time.Duration;
import org.stt.time.DateTimeHelper;

import com.google.common.io.PatternFilenameFilter;
import com.google.inject.Singleton;

/**
 * Simple configuration mechanism with fallback values.
 * <p/>
 * Resolves environment variables like HOME if enclosed in $ signs like so:
 * $HOME$/.stt
 */
@Singleton
public class Configuration {

    
	private static final String STTRC = ".sttrc";

	private static final Logger LOG = Logger.getLogger(Configuration.class
            .getName());
    
	private static final String STT_DIRECTORY = ".stt";
	
	private static final Pattern ENV_PATTERN = Pattern
            .compile(".*\\$(.*)\\$.*");
    private final Properties loadedProps;

    public Configuration() {

        loadedProps = new Properties();
        File propertiesFile = getPropertiesFile();
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

    @SuppressWarnings("PMD.CollapsibleIfStatements")
    public File determineBaseDir() {
    	File homeDirectory = null;
    	
        String envHOMEVariable = System.getenv("HOME");
        if (envHOMEVariable != null && !envHOMEVariable.trim().equals("")) {
			homeDirectory = new File(envHOMEVariable);
            if (homeDirectory.exists()) {
            	// Check if config file exists in %HOME%
            	if (checkForConfigFile(homeDirectory))
            		return homeDirectory;
            }
            else
            {
            	homeDirectory = null;
            }
        }
        
        // Check if config file exists in user home
        File homeDir = new File(System.getProperty("user.home"));
        if (checkForConfigFile(homeDir))
        {
        	return homeDir;
        }
        
        // Check if config file exists in user_home/.stt/
        File baseDir = new File(homeDir, STT_DIRECTORY);
        if (checkForConfigFile(baseDir))
        {
        	return baseDir;
        }
        
        // If home is defined use $HOME$
        if (homeDirectory != null)
        {
        	return homeDirectory;
        }
        
        // Else use base dir
        if (!baseDir.exists())
    	{
    		if (!baseDir.mkdir())
                throw new RuntimeException("Cannot create stt dir");
    	}
    		
		return baseDir;
    }

	private boolean checkForConfigFile(File homeDirectory) {
		File sttFile = new File(homeDirectory, STTRC);
		boolean fileExists = sttFile.exists() && sttFile.isFile();
		return fileExists;
	}

    private void createSttrc() {

        File propertiesFile = getPropertiesFile();

        LOG.info("creating " + propertiesFile.getAbsolutePath());

        try (InputStream rcStream = this.getClass().getResourceAsStream(
                "/org/stt/sttrc.example")) {
            IOUtils.copy(rcStream, new FileOutputStream(propertiesFile));
        } catch (IOException e) {
            LOG.log(Level.WARNING, "cannot write example config file "
                    + propertiesFile.getAbsolutePath(), e);
        }
    }

    private File getPropertiesFile() {
        return new File(determineBaseDir(), STTRC);
    }

    /**
     * @return the .stt file which will be created if it does not yet exist
     */
    public File getSttFile() {
        String sttFileString = getPropertiesReplaced("sttFile", "$HOME$/.stt");
        File sttFile = new File(sttFileString);
        try {
            sttFile.createNewFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return sttFile;
    }

    public File getDatabaseFile()
    {
    	return getSttFile();
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

    public File getItemLogFile() {
        String itemLogFile = getPropertiesReplaced("itemLogFile", "$HOME$/itemlog");
        return new File(itemLogFile);
    }

    public String getSystemOutEncoding() {
        return getPropertiesReplaced("sysoutEncoding", "UTF-8");
    }

    public int getCliReportingWidth() {
        return Integer.parseInt(getPropertiesReplaced(
                "cliReportingWidth", "80"));
    }

    public File getWorkingTimesFile() {
        String workigTimesFileString = getPropertiesReplaced(
                "workingTimesFile", "$HOME$/.stt_worktimes");
        File workingTimesFile = new File(workigTimesFileString);
        try {
            workingTimesFile.createNewFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return workingTimesFile;
    }

    public Collection<String> getBreakTimeComments() {
        String comments = getPropertiesReplaced("breakTimeComments",
                "pause,break,coffee");
        String[] split = comments.split(",");
        return Arrays.asList(split);
    }

    public Duration getDurationToRoundTo() {
        return DateTimeHelper.FORMATTER_PERIOD_H_M_S.parsePeriod(
                getPropertiesReplaced("durationRoundingInterval", "00:05:00"))
                .toStandardDuration();
    }

    public String getPropertiesReplaced(String propName, String fallback) {
        String theProperty = loadedProps.getProperty(propName, fallback);
        Matcher envMatcher = ENV_PATTERN.matcher(theProperty);
        if (envMatcher.find()) {
            String group = envMatcher.group(1);
            if ("HOME".equals(group)) {
                theProperty = theProperty.replace("$" + group + "$",
                        determineBaseDir().getAbsolutePath());
            }
        }
        return theProperty;
    }

    public File getBackupLocation() {
        String backupLocation = getPropertiesReplaced("backupLocation",
                "$HOME$");
        return new File(backupLocation);
    }

    public int getBackupInterval() {
        return Integer.parseInt(getPropertiesReplaced(
                "backupInterval", "7"));
    }

    public int getBackupRetentionCount() {
        return Integer.parseInt(getPropertiesReplaced(
                "backupRetention", "0"));
    }
    
    public boolean getMinimizedToTray() {
    	return Boolean.parseBoolean(getPropertiesReplaced("minimizedToTray", "true"));
    }

	public URI getJiraURI() {
		String jiraUri = getPropertiesReplaced("jiraURL",
                "");
		if (jiraUri != null && jiraUri.length() > 0)
		{
			try {
				return new URI(jiraUri);
			} catch (URISyntaxException e) {
				LOG.log(Level.SEVERE, "Unable to parse Jira URI", e);
				return null;
			}
		}
		return null;
	}

	public String getJiraPassword() {
		return getPropertiesReplaced("jiraPassword", "");
	}

	public String getJiraUserName() {
		return getPropertiesReplaced("jiraUserName", "");
	}

	public String getSplitItemGrouperRegularExpression() {
		return getPropertiesReplaced("splitItemGrouperRegularExpression", "\\s(:|\\||-)\\s");
	}
}

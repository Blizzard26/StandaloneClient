package org.stt;

import com.google.inject.Singleton;
import org.apache.commons.io.IOUtils;
import org.joda.time.Duration;
import org.stt.time.DateTimeHelper;

import java.io.*;
import java.util.Arrays;
import java.util.Collection;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Simple configuration mechanism with fallback values.
 * <p/>
 * Resolves environment variables like HOME if enclosed in $ signs like so:
 * $HOME$/.stt
 */
@Singleton
public class Configuration {

    private static final Logger LOG = Logger.getLogger(Configuration.class
            .getName());
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

    public File determineBaseDir() {
        String envHOMEVariable = System.getenv("HOME");
        if (envHOMEVariable != null) {
            File homeDirectory = new File(envHOMEVariable);
            if (homeDirectory.exists()) {
                return homeDirectory;
            }
        }
        return new File(System.getProperty("user.home"));
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
        return new File(determineBaseDir(), ".sttrc");
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
        int reportingWidth = Integer.parseInt(getPropertiesReplaced(
                "cliReportingWidth", "80"));
        return reportingWidth;
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
        int backupInterval = Integer.parseInt(getPropertiesReplaced(
                "backupInterval", "7"));
        return backupInterval;
    }

    public int getBackupRetentionCount() {
        int retentionCount = Integer.parseInt(getPropertiesReplaced(
                "backupRetention", "0"));
        return retentionCount;
    }
}

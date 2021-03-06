package org.stt.config;

public class BaseConfig implements Config {
    private TimeTrackingItemListConfig timeTrackingItemListConfig;
    private ReportWindowConfig reportWindowConfig;
    private CommonPrefixGrouperConfig prefixGrouper;
    private CommandTextConfig commandText;

    public TimeTrackingItemListConfig getTimeTrackingItemListConfig() {
        return timeTrackingItemListConfig;
    }

    public void setTimeTrackingItemListConfig(
            TimeTrackingItemListConfig timeTrackingItemListConfig) {
        this.timeTrackingItemListConfig = timeTrackingItemListConfig;
    }

    public ReportWindowConfig getReportWindowConfig() {
        return reportWindowConfig;
    }

    public void setReportWindowConfig(ReportWindowConfig reportWindowConfig) {
        this.reportWindowConfig = reportWindowConfig;
    }

    public CommonPrefixGrouperConfig getPrefixGrouper() {
        return prefixGrouper;
    }

    public void setPrefixGrouper(CommonPrefixGrouperConfig prefixGrouper) {
        this.prefixGrouper = prefixGrouper;
    }

    public CommandTextConfig getCommandText() {
        return commandText;
    }

    public void setCommandText(CommandTextConfig commandText) {
        this.commandText = commandText;
    }

    @Override
    public void applyDefaults() {
        if (timeTrackingItemListConfig == null) {
            timeTrackingItemListConfig = new TimeTrackingItemListConfig();
        }
        timeTrackingItemListConfig.applyDefaults();
        if (reportWindowConfig == null) {
            reportWindowConfig = new ReportWindowConfig();
        }
        reportWindowConfig.applyDefaults();
        if (prefixGrouper == null) {
            prefixGrouper = new CommonPrefixGrouperConfig();
        }
        prefixGrouper.applyDefaults();
        if (commandText == null) {
            commandText = new CommandTextConfig();
        }
        commandText.applyDefaults();
    }
}

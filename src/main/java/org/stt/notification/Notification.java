package org.stt.notification;

public interface Notification {
	public void error(String errorMessage);
	
	public void warning(String warningMessage);
	
	public void info(String infoMessage);
	
	public void setStatus(String statusMessage);
}

package org.stt.persistence.h2;

public class H2Constants {
	
	public static final int INDEX_LOGGED = 4;
	public static final int INDEX_COMMENT = 3;
	public static final int INDEX_END = 2;
	public static final int INDEX_START = 1;
	
	public static final String CREATE_STATEMENT = "CREATE TABLE IF NOT EXISTS TimeTrackingItems "
			+ "(START_TIME BIGINT, "
			+ "END_TIME BIGINT, "
			+ "COMMENT VARCHAR(255), "
			+ "LOGGED INT);";
	
	public static final String INSERT_STATEMENT = "INSERT INTO TimeTrackingItems "
			+ "(START_TIME, END_TIME, COMMENT, LOGGED) "
			+ "VALUES (?, ?, ?, ?)";
	
	
	public static final String SELECT_QUERY = "SELECT START_TIME, END_TIME, COMMENT, LOGGED "
			+ "FROM TimeTrackingItems "
			+ "ORDER BY START_TIME ASC";

}

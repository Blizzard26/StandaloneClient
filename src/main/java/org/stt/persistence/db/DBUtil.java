package org.stt.persistence.db;

import java.sql.Timestamp;

import org.joda.time.DateTime;

public class DBUtil {
	
	public static Timestamp transform(DateTime date) {
		return new Timestamp(date.getMillis());
	}

	public static DateTime transform(Timestamp timestamp) {
		return new DateTime(timestamp.getTime());
	}
}

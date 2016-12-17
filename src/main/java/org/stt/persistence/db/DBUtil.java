package org.stt.persistence.db;

import java.sql.Timestamp;

import org.joda.time.DateTime;

public class DBUtil {
	
	protected static Timestamp transform(DateTime date) {
		return new Timestamp(date.getMillis());
	}

	protected static DateTime transform(Timestamp timestamp) {
		return new DateTime(timestamp.getTime());
	}
}

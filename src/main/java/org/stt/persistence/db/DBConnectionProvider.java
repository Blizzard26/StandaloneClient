package org.stt.persistence.db;

import java.sql.Connection;
import java.sql.SQLException;

public interface DBConnectionProvider {

	Connection getConnection() throws SQLException;

	void releaseConnection(Connection connection) throws SQLException;

	int getConnectionCount();

}

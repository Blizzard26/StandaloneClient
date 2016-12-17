package org.stt.persistence.db;

import java.sql.Connection;
import java.sql.JDBCType;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLType;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.joda.time.DateTime;

public class PreparedStatementBuilder {


	public class SelectStatement {

	}

	public class SQLParameter {

		private Object object;
		private JDBCType sqlType;

		public SQLParameter(Object object, JDBCType sqlType) {
			this.object = object;
			this.sqlType = sqlType;
		}

	}
	
	private StringBuilder sql;
	List<SQLParameter> paramList;

	public PreparedStatementBuilder()
	{
		sql = new StringBuilder();
		paramList = new ArrayList<SQLParameter>();
	}

	public PreparedStatementBuilder append(String str) {
		sql.append(str);
		return this;
	}

	public PreparedStatementBuilder addParameter(DateTime date) {
		return addParameter(DBUtil.transform(date), JDBCType.TIMESTAMP);
	}
	
	

	public PreparedStatementBuilder addParameter(String str) {
		return addParameter(str, JDBCType.VARCHAR);
	}
	
	public PreparedStatementBuilder addParameter(Object object, JDBCType sqlType)
	{
		sql.append("?");
		paramList.add(new SQLParameter(object, sqlType));
		return this;
	}

	public PreparedStatement prepareStatement(Connection connection) throws SQLException {
		PreparedStatement ps = connection.prepareStatement(getSql());
		
		for (int i = 0; i < paramList.size(); i++) {
			SQLParameter param = paramList.get(i);
			switch (param.sqlType)
			{
			case TIMESTAMP:
				ps.setTimestamp(i+1, (Timestamp) param.object);
				break;
			case VARCHAR:
				ps.setString(i+1, (String) param.object);
				break;
			default:
				ps.setObject(i+1, param.object, param.sqlType);
			}
		}
		
		return ps;
	}

	public String getSql() {
		return sql.toString();
	}
	
	public String toString()
	{
		StringBuilder s = new StringBuilder(sql.toString());
		s.append(" [");
		String sep = "";
		for (SQLParameter param : paramList)
		{
			s.append(sep);
			sep = ", ";
			s.append(param.object);
		}
		s.append("]");
		return s.toString();
	}

}

package com.aoc.nfc.queryservice.impl.jdbc.mapper;

import java.sql.ResultSet;
import java.sql.Types;
import java.util.Map;

import com.aoc.nfc.queryservice.ResultSetMapper;
import com.aoc.nfc.queryservice.impl.util.AbstractNameMatcher;
import com.aoc.nfc.queryservice.impl.util.NameConverter;

public abstract class AbstractResultSetMapperSupport implements ResultSetMapper {
	private static AbstractNameMatcher defaultNameMatcher = new NameConverter();

	private AbstractNameMatcher nameMatcher;

	protected Map<String, String> nullchecks;

	public AbstractResultSetMapperSupport(Map<String, String> nullchecks) {
		super();

		this.nullchecks = nullchecks;
		this.nameMatcher = defaultNameMatcher;
	}

	public final void setNameMatcher(AbstractNameMatcher nameMatcher) {
		this.nameMatcher = nameMatcher;
	}

	public AbstractNameMatcher getNameMatcher() {
		return nameMatcher;
	}

	public Map<String, String> getNullchecks() {
		return nullchecks;
	}

	public void setNullCheckInfos(Map<String, String> nullCheckInfos) {
		this.nullchecks = nullCheckInfos;
	}

	protected Object getValue(ResultSet resultSet, int columnType,
			String columnName, int columnIndex) {
		Object obj;
		try {
			switch (columnType) {
			case Types.BIGINT:
				return new Long(resultSet.getLong(columnIndex));
			case Types.BINARY:
				return resultSet.getBytes(columnIndex);
			case Types.BIT:
				return new Boolean(resultSet.getBoolean(columnIndex));
			case Types.CHAR:
				obj = resultSet.getString(columnIndex);
				if (obj == null && nullchecks.size() != 0)
					obj = changeNullValue("char");
				return obj;
			case Types.DATE:
				return resultSet.getDate(columnIndex);
			case Types.DECIMAL:
				return resultSet.getBigDecimal(columnIndex);
			case Types.DOUBLE:
				return new Double(resultSet.getDouble(columnIndex));
			case Types.FLOAT:
				return new Double(resultSet.getDouble(columnIndex));
			case Types.INTEGER:
				return new Integer(resultSet.getInt(columnIndex));
			case Types.LONGVARBINARY:
				return resultSet.getBytes(columnIndex);
			case Types.LONGVARCHAR:
				obj = resultSet.getString(columnIndex);
				if (obj == null && nullchecks.size() != 0)
					obj = changeNullValue("longvarchar");
				return obj;
			case Types.NULL:
				throw new Exception("Query Service : Not supported SQL type. Column Type - NULL. Column Name - "+ columnName + ".");
			case Types.NUMERIC:
				return resultSet.getBigDecimal(columnIndex);
			case Types.REAL:
				return new Float(resultSet.getFloat(columnIndex));
			case Types.SMALLINT:
				return new Short(resultSet.getShort(columnIndex));
			case Types.TIME:
				return resultSet.getTime(columnIndex);
			case Types.TIMESTAMP:
				return resultSet.getTimestamp(columnIndex);
			case Types.TINYINT:
				return new Byte(resultSet.getByte(columnIndex));
			case Types.VARBINARY:
				return resultSet.getBytes(columnIndex);
			case Types.VARCHAR:
				obj = resultSet.getString(columnIndex);
				if (obj == null && nullchecks.size() != 0)
					obj = changeNullValue("varchar");
				return obj;
			default:
				return resultSet.getString(columnIndex);
			} 
		} catch (Exception ex) {
			System.out.println("Query Service : Not supported SQL type");
			ex.printStackTrace();
		}

		return null;
	}

	protected Object changeNullValue(String type) {
		if (nullchecks.containsKey(type)) {
			return nullchecks.get(type);
		} else {
			return null;
		}
	}
}

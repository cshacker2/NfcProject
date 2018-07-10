package com.aoc.nfc.queryservice.impl.util;

import org.springframework.jdbc.support.JdbcUtils;

public class ColumnUtil {
	public static String changeColumnName(String mappingStyle, String columnName) {
		if ("camel".equals(mappingStyle)) {
			//return StringUtil.convertToCamelCase(columnName);
			return JdbcUtils.convertUnderscoreNameToPropertyName(columnName);
		} else if ("lower".equals(mappingStyle))
			return columnName.toLowerCase();
		else if ("upper".equals(mappingStyle))
			return columnName.toUpperCase();
		return columnName; 
	}
}

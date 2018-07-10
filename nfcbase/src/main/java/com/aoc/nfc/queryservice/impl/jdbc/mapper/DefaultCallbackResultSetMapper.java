package com.aoc.nfc.queryservice.impl.jdbc.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import com.aoc.nfc.queryservice.MappingInfo;

public class DefaultCallbackResultSetMapper<T> extends CallbackResultSetMapper<T> {
	public DefaultCallbackResultSetMapper(Class<T> targetClass, MappingInfo mappingInfo, Map<String, String> nullchecks, String mappingStyle) {
		super(targetClass, mappingInfo, nullchecks, mappingStyle);
	}

	public Object generateMap(ResultSet resultSet) throws SQLException {
		if (sqlLoader.getQueryResultMappings().containsKey(queryId)) {
			mappingConfiguration = sqlLoader.getQueryResultMappings().get(queryId);
		} else {
			if (!initialized)
				makeMeta(resultSet);
			sqlLoader.getQueryResultMappings().put(queryId, mappingConfiguration);
		}

		Map<String, Object> mapOfColValues = createColumnMap(mappingConfiguration.getColumnCount());
		
		for (int i = 1; i <= mappingConfiguration.getColumnCount(); i++) {
			String key = mappingConfiguration.getColumnKeys()[i - 1];
			Object obj = getValue(resultSet, mappingConfiguration.getColumnTypes()[i - 1], mappingConfiguration.getColumnNames()[i - 1], i);
			mapOfColValues.put(key, obj);
		}
		return mapOfColValues;
	}
}

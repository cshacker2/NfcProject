package com.aoc.nfc.queryservice.impl.jdbc.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import org.apache.commons.collections.map.ListOrderedMap;
import org.springframework.util.LinkedCaseInsensitiveMap;

import com.aoc.nfc.queryservice.MappingInfo;
import com.aoc.nfc.queryservice.impl.util.SQLTypeTransfer;

public class CallbackResultSetMapper<T> extends DefaultReflectionResultSetMapper<T> {

	// 2009.05.28
	public CallbackResultSetMapper(Class<T> targetClass, MappingInfo mappingInfo, Map<String, String> nullchecks, String mappingStyle) {
		super(targetClass, mappingInfo, nullchecks);
		this.targetClass = targetClass;
		this.mappingStyle = mappingStyle;
		//setNameMatcher(new InternalNameMatcher());
	}

	@SuppressWarnings("unchecked")
	public T mapRow(ResultSet resultSet, int rowNum) throws SQLException {
		if (customResultSetMapper != null) {
			return (T)customResultSetMapper.mapRow(resultSet);
		}
		return (T)this.mapRow(resultSet);
	}

	@SuppressWarnings("unchecked")
	public T mapRow(ResultSet resultSet) throws SQLException {
		if (Map.class.isAssignableFrom(targetClass)) {
			return (T)generateMap(resultSet);
		} else {
			return (T)super.mapRow(resultSet);
		}
	}

	public Object generateMap(ResultSet resultSet) throws SQLException {

		if (!initialized)
			makeMeta(resultSet);
		Map<String, Object> mapOfColValues = createColumnMap(mappingConfiguration.getColumnCount());
		
		for (int i = 1; i <= mappingConfiguration.getColumnCount(); i++) {
			String key = mappingConfiguration.getColumnKeys()[i - 1];
			Object obj = getValue(resultSet, mappingConfiguration.getColumnTypes()[i - 1], mappingConfiguration.getColumnNames()[i - 1], i);
			mapOfColValues.put(key, obj);
		}
		return mapOfColValues;
	}

	@SuppressWarnings("rawtypes")
	public Map getColumnInfo() {
		ListOrderedMap colInfo = new ListOrderedMap();
		for (int i = 0; i < mappingConfiguration.getColumnCount(); i++)
			colInfo.put(mappingConfiguration.getColumnKeys()[i], SQLTypeTransfer.getSQLTypeName(mappingConfiguration.getColumnTypes()[i]) + ":" + mappingConfiguration.getColumnPrecisions()[i] + ":" + mappingConfiguration.getColumnScales()[i]);
		return colInfo;
	}

	
	protected Map<String, Object> createColumnMap(int initialCapacity) {
		return new LinkedCaseInsensitiveMap<Object>(initialCapacity);
	}
	
}

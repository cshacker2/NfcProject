package com.aoc.nfc.queryservice.impl.jdbc.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Map;

import com.aoc.nfc.queryservice.MappingInfo;

public class DefaultReflectionResultSetMapper<T> extends ReflectionResultSetMapper<T> {

	public DefaultReflectionResultSetMapper(Class<T> targetClass, MappingInfo mappingInfo, Map<String, String> nullchecks) {
		super(targetClass, mappingInfo, nullchecks);
	}

	@SuppressWarnings("unchecked")
	public T mapRow(ResultSet resultSet) throws SQLException {
		Object object = null;
		Iterator<Class<T>> targetClassIterator = targetClasses.iterator();
		while (targetClassIterator.hasNext() && object == null) {
			Class<T> targetClass = targetClassIterator.next();

			ResultSetMappingConfiguration mappingConfiguration;

			if (queryId != null && sqlLoader.getQueryResultMappings().containsKey(queryId)) {
				mappingConfiguration = sqlLoader.getQueryResultMappings().get(queryId);
			} else {
				mappingConfiguration = getConfig(targetClass, resultSet.getMetaData());
				if (queryId != null) { 
					sqlLoader.getQueryResultMappings().put(queryId, mappingConfiguration);
				}
			}
			object = super.toObject(resultSet, targetClass, mappingConfiguration);
		}
		return (T)object;
	}
}

package com.aoc.nfc.queryservice.impl.jdbc.setter;

import org.apache.velocity.context.Context;

public class DefaultDynamicSqlParameterSourceContext implements Context { 
	private final DefaultDynamicSqlParameterSource parameterSource;

	public DefaultDynamicSqlParameterSourceContext(DefaultDynamicSqlParameterSource parameterSource) {
		this.parameterSource = parameterSource;
	}

	public boolean containsKey(Object key) {
		return parameterSource.hasValue((String) key);
	}

	public Object get(String key) {
		try {
			return parameterSource.getValue(key);
		} catch (Exception e) {
			return null;
		}
	}

	public Object[] getKeys() {
		return parameterSource.getKeys();
	}

	public Object put(String key, Object value) {
		return parameterSource.addValue(key, value);
	}

	public Object remove(Object key) {
		return null;
	}
}

package com.aoc.nfc.queryservice.impl.jdbc.setter;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;

import org.springframework.jdbc.core.SqlTypeValue;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

import com.aoc.nfc.queryservice.DynamicSqlParameterSource;
import com.aoc.nfc.queryservice.impl.util.NameConverter;

public class DefaultDynamicSqlParameterSource extends MapSqlParameterSource implements DynamicSqlParameterSource {
	public DefaultDynamicSqlParameterSource() {
	}

	@SuppressWarnings("unchecked")
	public DefaultDynamicSqlParameterSource(Map properties) {
		super(properties);
	}

	public boolean hasValue(String paramName) {
		return this.getValue(paramName)!=null;
	}
	
	public Object getValue(String paramName) {
		Object value = getVariableFromContext(paramName);
		return value;
	}

	public Object[] getKeys() {
		Map<String, Object> values = getValues();
		Object[] keys = new Object[values.size()];

		if (keys.length != 0) {
			int i = 0;
			Iterator<String> keyIterator = values.keySet().iterator();
			if (keyIterator != null) {
				keys[i++] = keyIterator.next();
			}
		}
		return keys;
	}

	private Object getVariableFromContext(String variable) {
		Object valuesMap = getValues();
		String[] strArray = convertDelimitedStringToStringArray(variable, ".");

		for (int i = 0; i < strArray.length; i++) {
			String str = strArray[i];
			try {
				valuesMap = getProperty(valuesMap, str);
			} catch (Exception e) {
				valuesMap = null;
			}
		}
		return valuesMap;
	}

	public static Object getProperty(Object obj, String propertyName) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
		if (obj instanceof Map) {
			return ((Map) obj).get(propertyName);
		}

		Class<?>[] paramTypes = new Class[0];
		Object[] args = new Object[0];
		Object result = null;

		String methodName = buildPropertyGetterName("get", propertyName);

		Method method = null;
		try {
			method = obj.getClass().getMethod(methodName, paramTypes);
		} catch (NoSuchMethodException ne) {
			methodName = buildPropertyGetterName("is", propertyName);
			method = obj.getClass().getMethod(methodName, paramTypes);
		}

		result = method.invoke(obj, args);
		return result;
	}

	protected static String buildPropertyGetterName(String prefix, String propertyName) {
		if (propertyName.endsWith("()"))
			return propertyName.substring(0, propertyName.length() - 2);
		return buildPropertyMethodName(prefix, propertyName);
	}

	protected static String buildPropertySetterName(String propertyName) {
		return buildPropertyMethodName("set", propertyName);
	}

	protected static String buildPropertyMethodName(String prefix, String propertyName) {
		StringBuilder strBuffer = new StringBuilder(prefix);
		strBuffer.append(NameConverter.capitalise(propertyName));
		return strBuffer.toString();
	}

	public static String[] convertDelimitedStringToStringArray(String str, String delimiter) {
		StringTokenizer strTokenizer = new StringTokenizer(str, delimiter);
		int length = strTokenizer.countTokens();

		String[] strArray = new String[length];
		int i = 0;
		while (strTokenizer.hasMoreTokens()) {
			strArray[i] = strTokenizer.nextToken().trim();
			i++;
		}

		return strArray;
	}

	private final Map<String, Integer> sqlTypes = new HashMap<String, Integer>();

	public void addSqlType(String paramName, int sqlType) {
		sqlTypes.put(paramName, new Integer(sqlType));
	}

	public int getSqlType(String paramName) {
		Integer sqlType = sqlTypes.get(paramName);
		if (sqlType != null) {
			return sqlType.intValue();
		} else {
			return SqlTypeValue.TYPE_UNKNOWN;
		}
	}
}

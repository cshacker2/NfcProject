package com.aoc.nfc.queryservice.impl.util;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class ReflectionHelp {

	private ReflectionHelp() {
		super();
	}

	public static Map<String, Field> getAllDeclaredFields(Class<?> target) {
		Map<String, Field> fieldMap = new HashMap<String, Field>();

		if (target.getSuperclass() != null) {
			Map<String, Field> superFieldMap = getAllDeclaredFields(target.getSuperclass());
			fieldMap.putAll(superFieldMap);
		}

		Field[] currentFields = target.getDeclaredFields();
		AccessibleObject.setAccessible(currentFields, true);
		for (int i = 0; i < currentFields.length; i++) {
			fieldMap.put(currentFields[i].getName(), currentFields[i]);
		}

		return fieldMap;
	}

	public static void setFieldValue(Field field, Object obj, Object value)
			throws IllegalAccessException {
		if (value != null)
			field.set(obj, value);
	}

	public static Object getFieldValue(Field field, Object bean)
			throws IllegalAccessException {
		return field.get(bean);
	}

	public static Object newInstance(Class<?> createClass)
			throws InstantiationException, IllegalAccessException {
		return createClass.newInstance();
	}
}

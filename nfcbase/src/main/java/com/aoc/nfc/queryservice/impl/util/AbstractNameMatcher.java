package com.aoc.nfc.queryservice.impl.util;

import java.lang.reflect.Field;
import java.util.Map;

public abstract class AbstractNameMatcher implements Cloneable {
	public abstract boolean isMatching(String attributeName, String columnName, String parentAttributeName);
	public abstract Field isMatching(Map<String, Field> attributeMap, String columnName, String parentAttributeName, Field[] attributes);
	public abstract void setFieldPrefix(String fieldPrefix);
	public abstract void setFieldSuffix(String fieldSuffix);
}

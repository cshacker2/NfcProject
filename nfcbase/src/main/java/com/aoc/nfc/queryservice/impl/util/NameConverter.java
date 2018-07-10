package com.aoc.nfc.queryservice.impl.util;

import java.lang.reflect.Field;
import java.util.Map;

public class NameConverter extends AbstractNameMatcher {

	private String prefix = "";
	private String suffix = "";

	public static String capitalise(String name) {
		if (name == null || name.length() == 0) {
			return name;
		}
		char[] chars = name.toCharArray();
		chars[0] = Character.toUpperCase(chars[0]);
		return new String(chars);
	}

	public boolean isMatching(String fieldName, String columnName, String parentFieldName) {
		if (columnName.equalsIgnoreCase(prefix.concat(NameConverter.convertToUnderScore(fieldName)).concat(suffix))) {
			return true;
		}
		return false;
	}
	
	public Field isMatching(Map<String, Field> attributeMap, String columnName, String parentFieldName, Field[] parentAttributes) {
		
		String camelCasedColumnName = prefix.concat(NameConverter.convertToCamelCase(columnName)).concat(suffix);

		if (attributeMap.containsKey(camelCasedColumnName)) {
			return attributeMap.get(camelCasedColumnName);
		}
		return null;
	}

	public static String convertToUnderScore(String camelCaseStr) {
		String result = "";
		for (int i = 0; i < camelCaseStr.length(); i++) {
			char currentChar = camelCaseStr.charAt(i);
			if (i > 0 && Character.isUpperCase(currentChar)) {
				result = result.concat("_");
			}
			result = result.concat(Character.toString(currentChar).toLowerCase());
		}
		return result;
	}

	public static String convertToCamelCase(String originalString,
			char searchChar) {
		String result = "";
		boolean nextUpper = false;

		for (int i = 0; i < originalString.length(); i++) {
			char currentChar = originalString.charAt(i);
			if (currentChar == searchChar) {
				nextUpper = true;
			} else {
				if (nextUpper) {
					currentChar = Character.toUpperCase(currentChar);
					nextUpper = false;
				}
				result = result.concat(Character.toString(currentChar));
			}
		}
		return result;
	}

	public static String convertToCamelCase(String underScore) {
		String result = "";
		boolean nextUpper = false;
		String allLower = underScore.toLowerCase();
		for (int i = 0; i < allLower.length(); i++) {
			char currentChar = allLower.charAt(i);
			if (currentChar == '_') {
				nextUpper = true;
			} else {
				if (nextUpper) {
					currentChar = Character.toUpperCase(currentChar);
					nextUpper = false;
				}
				result = result.concat(Character.toString(currentChar));
			}
		}
		return result;
	}

	public void setFieldPrefix(String prefix) {
		if (prefix == null) {
			this.prefix = "";
		} else {
			this.prefix = prefix;
		}
	}

	public void setFieldSuffix(String suffix) {
		if (suffix == null) {
			this.suffix = "";
		} else {
			this.suffix = suffix;
		}
	}

	public static String deCapitalise(String name) {
		if (name == null || name.length() == 0) {
			return name;
		}
		char[] chars = name.toCharArray();
		chars[0] = Character.toLowerCase(chars[0]);
		return new String(chars);
	}
}

package com.aoc.nfc.queryservice.impl.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.web.util.HtmlUtils;

public class StringUtil {

	private StringUtil() {
		throw new AssertionError(); 
	}

	public static final String DEFAULT_EMPTY_STRING = "";
	private static final Random GENERATOR = new Random(System.currentTimeMillis());
	private static final int ONE_BYTE = 0x00007F;
	private static final int THREE_BYTE = 0x00FFFF;
	private static final int TWO_BYTE = 0x0007FF;

	public static String arrayToDelimitedString(Object[] objects, String delimiter) {
		if (objects == null) {
			return null;
		}
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < objects.length; i++) {
			if (i > 0 && delimiter != null) {
				sb.append(delimiter);
			}
			sb.append(objects[i]);
		}
		return sb.toString();
	}
	
	public static String collectionToDelimitedString(Collection<String> strings, String delimiter) {
		if (strings == null) {
			return null;
		}
		StringBuffer sb = new StringBuffer();
		Iterator<String> it = strings.iterator();
		int i = 0;
		for (; it.hasNext(); sb.append(it.next())) {
			if (i++ > 0 && delimiter != null) {
				sb.append(delimiter);
			}
		}
		return sb.toString();
	}

	public static int compareTo(String sourceStr, String targetStr) {
		if (sourceStr == null || targetStr == null) {
			return -1;
		}
		return sourceStr.compareTo(targetStr);
	}

	public static int compareToIgnoreCase(String sourceStr, String targetStr) {
		if (sourceStr == null || targetStr == null) {
			return -1;
		}
		return sourceStr.compareToIgnoreCase(targetStr);
	}

	public static String convertToCamelCase(String str) {
		return convertToCamelCase(str, '_');
	}

	public static String convertToCamelCase(String str, char delimiter) {
		StringBuilder result = new StringBuilder();
		boolean nextUpper = false;
		String allLower = str.toLowerCase();

		for (int i = 0; i < allLower.length(); i++) {
			char currentChar = allLower.charAt(i);
			if (currentChar == delimiter) {
				nextUpper = true;
			} else {
				if (nextUpper) {
					currentChar = Character.toUpperCase(currentChar);
					nextUpper = false;
				}
				result.append(currentChar);
			}
		}
		return result.toString();
	}

	public static String convertToUnderScore(String str) {
		String result = "";
		for (int i = 0; i < str.length(); i++) {
			char currentChar = str.charAt(i);
			if (i > 0 && Character.isUpperCase(currentChar)) {
				result = result.concat("_");
			}
			result = result.concat(Character.toString(currentChar).toLowerCase());
		}
		return result;
	}

	public static String decode(String source, String target, String result, String base) {
		if (source == null && target == null) {
			return result;
		} else if (source == null && target != null) {
			return base;
		} else if (source.trim().equals(target)) {
			return result;
		}
		return base;
	}

	public static int getByteLength(char ch) {
		int charCode = ch;

		if (charCode <= ONE_BYTE) {
			return 1;
		} else if (charCode <= TWO_BYTE) {
			return 2;
		} else if (charCode <= THREE_BYTE) {
			return 3;
		} else {
			return 4;
		}
	}

	public static int getByteLength(String str) {
		if (str == null) {
			return -1;
		}
		int size = 0;

		for (int i = 0; i < str.length(); i++) {
			size += getByteLength(str.charAt(i));
		}
		return size;
	}

	
	public static int countMatches(String str, char[] chars) {
		return countMatches(str, new String(chars));
	}

	public static int countMatches(String str, String sub) {
		return org.springframework.util.StringUtils
				.countOccurrencesOf(str, sub);
	}

	public static String getLastString(String str, String token) {
		StringTokenizer tokenizer = new StringTokenizer(str, token);
		String lastStr = "";
		while (tokenizer.hasMoreTokens()) {
			lastStr = tokenizer.nextToken();
		}
		return lastStr;
	}

	public static String getRandomString(int size, char startChar, char endChar) {
		int startInt = Integer.valueOf(startChar);
		int endInt = Integer.valueOf(endChar);

		int gap = endInt - startInt;
		StringBuilder buf = new StringBuilder();
		for (int i = 0; i < size; i++) {
			int chInt;
			do {
				chInt = StringUtil.GENERATOR.nextInt(gap + 1) + startInt;
			} while (!Character.toString((char) chInt).matches("^[a-zA-Z]$"));
			buf.append((char) chInt);
		}
		return buf.toString();
	}

	public static List<String> getTokens(String str) {
		return getTokens(str, ",");
	}
	
	public static List<String> getTokens(String str, String delimeter) {
		List<String> tokens = new ArrayList<String>();

		if (str != null) {
			StringTokenizer st = new StringTokenizer(str, delimeter);
			while (st.hasMoreTokens()) {
				String en = st.nextToken().trim();
				tokens.add(en);
			}
		}
		return tokens;
	}

	public static boolean isEmpty(String str) {
        if (str != null) {
            int len = str.length();
            for (int i = 0; i < len; ++i) {
                if (str.charAt(i) > ' ') {
                    return false;
                }
            }
        }
        return true;
	}

	public static boolean isEmptyTrimmed(String str) {
		if(str == null)
			return true;
		return isEmpty(str.trim());
	}

	public static String newLineToSpace(String str) {
		return str.replace("\r\n", " ");
	}

	public static String htmlEscape(String input) {
		return HtmlUtils.htmlEscape(input);
	}

	public static String htmlUnescape(String input) {
		return HtmlUtils.htmlUnescape(input);
	}

	public static String reverse(String str) {
		if (str == null) {
			return null;
		}
		return new StringBuilder(str).reverse().toString();
	}

	public static String abbreviateFromLeft(String str, int size) {
		if (str == null) {
			return null;
		} else if (size <= 0 || str.length() <= size) {
			return str;
		} else {
			return str.substring(0, size) + "...";
		}
	}

	public static String abbreviateFromRight(String str, int size) {
		if (str == null) {
			return null;
		} else if (size <= 0 || str.length() <= size) {
			return str;
		} else {
			return "..." + str.substring(str.length() - size);
		}
	}

	public static String stringToHex(String str) {

		String inStr = str;

		char inChar[] = inStr.toCharArray();
		StringBuffer sb = new StringBuffer();

		for (int i = 0; i < inChar.length; i++) {
			String hex = Integer.toHexString((int) inChar[i]);
			if (hex.length() == 2) {
				hex = "00" + hex;
			}
			sb.append(hex);
		}
		return sb.toString();
	}

	public static boolean isRegexPatternMatch(String str, String pattern) {
		Pattern p = Pattern.compile(pattern);
		Matcher m = p.matcher(str);
		return m.matches();
	}

}

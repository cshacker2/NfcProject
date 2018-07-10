package com.aoc.nfc.queryservice.impl.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.util.Assert;

public abstract class NamedParameterUtil {

	private NamedParameterUtil() {
		super();
	}

	private static final char[] PARAMETER_SEPARATORS = new char[] { '(', ')',',', '=' };
	private static final int BLOCK_COMMENTS = 1;
	private static final int LINE_COMMENTS = 2;

	public static ParsedSql parseSqlStatement(String sql) {
		Assert.notNull(sql, "SQL must not be null");

		List<String> parameters = new ArrayList<String>();
		Map<String, String> namedParameters = new HashMap<String, String>();
		ParsedSql parsedSql = new ParsedSql(sql);

		char[] statement = sql.toCharArray();
		StringBuilder newSql = new StringBuilder();
		boolean withinQuotes = false;
		char currentQuote = '-';
		int namedParameterCount = 0;
		int unnamedParameterCount = 0;
		int totalParameterCount = 0;

		int i = 0;
		while (i < statement.length) {
			char c = statement[i];
			// Commnents handling start
			char nextC = new Character(' ').charValue();
			if (i < statement.length - 1)
				nextC = statement[i + 1];

			int commentsType = isCommentsOpen(c, nextC);
			if (commentsType > 0) {
				while (!isCommentsClose(commentsType, statement.length, i, c,
						nextC)) {
					newSql.append(c);
					c = statement[++i];
					if (i < statement.length - 1)
						nextC = statement[i + 1];
					else
						nextC = new Character(' ').charValue();
				}
				newSql.append(c);
				newSql.append(nextC);
				i = i + 2;
				continue;
			}
			// comments handling end

			if (withinQuotes) {
				if (c == currentQuote) {
					withinQuotes = false;
					currentQuote = '-';
				}
				newSql.append(c);
			} else {
				if (c == '"' || c == '\'') {
					withinQuotes = true;
					currentQuote = c;
					newSql.append(c);
				} else {
					if (c == ':' || c == '&') {
						int j = i + 1;
						if (j < statement.length && statement[j] == ':' && c == ':') { 
							// Postgres-style "::" casting operator - to be skipped. 
							i = i + 2; 
							continue; 
						}
						
						if (j < statement.length && statement[j] == '&' && c == '&') { 
							// Postgres-style "&&" casting operator - to be skipped. 
							i = i + 2; 
							continue; 
						}
						
						while (j < statement.length
								&& !isParameterSeparator(statement[j])) {
							j++;
						}
						if (j - i > 1) {
							String parameter = sql.substring(i + 1, j);
							if (!namedParameters.containsKey(parameter)) {
								namedParameters.put(parameter, parameter);
								namedParameterCount++;
							}
							newSql.append("?");
							parameters.add(parameter);
							totalParameterCount++;
						} else {
							newSql.append(c);
						}
						i = j - 1;
					} else {
						newSql.append(c);
						if (c == '?') {
							unnamedParameterCount++;
							totalParameterCount++;
						}
					}
				}
			}
			i++;
		}
		parsedSql.setNewSql(newSql.toString());
		parsedSql.setParameterNames(parameters.toArray(new String[parameters.size()]));
		parsedSql.setNamedParameterCount(namedParameterCount);
		parsedSql.setUnnamedParameterCount(unnamedParameterCount);
		parsedSql.setTotalParameterCount(totalParameterCount);
		return parsedSql;
	}

	public static Object[] buildValueArray(ParsedSql parsedSql,
			SqlParameterSource paramSource) {
		Object[] paramArray = new Object[parsedSql.getTotalParameterCount()];
		if (parsedSql.getNamedParameterCount() > 0
				&& parsedSql.getUnnamedParameterCount() > 0) {
			throw new InvalidDataAccessApiUsageException(
					"You can't mix named and traditional ? placeholders. You have "
							+ parsedSql.getNamedParameterCount()
							+ " named parameter(s) and "
							+ parsedSql.getUnnamedParameterCount()
							+ " traditonal placeholder(s) in ["
							+ parsedSql.getSql() + "]");
		}
		String[] paramNames = parsedSql.getParameterNames();
		for (int i = 0; i < paramNames.length; i++) {
			String paramName = paramNames[i];
			try {
				paramArray[i] = paramSource.getValue(paramName);
			} catch (IllegalArgumentException ex) {
				throw new InvalidDataAccessApiUsageException(
						"No value supplied for the SQL parameter '" + paramName
								+ "': " + ex.getMessage());
			}
		}
		return paramArray;
	}

	@SuppressWarnings("rawtypes")
	public static String substituteNamedParameters(String sql, SqlParameterSource paramSource) {
		Assert.notNull(sql, "SQL must not be null");

		char[] statement = sql.toCharArray();
		StringBuilder newSql = new StringBuilder();
		boolean withinQuotes = false;
		char currentQuote = '-';

		int i = 0;
		
		while (i < statement.length) {

			char c = statement[i];

			// comments handling start
			char nextC = new Character(' ').charValue();
			
			if (i < statement.length - 1) {
				nextC = statement[i + 1];
			}
			
			int commentsType = isCommentsOpen(c, nextC);
			
			if (commentsType > 0) {
				while (!isCommentsClose(commentsType, statement.length, i, c,
						nextC)) {
					newSql.append(c);
					c = statement[++i];
					if (i < statement.length - 1)
						nextC = statement[i + 1];
					else
						nextC = new Character(' ').charValue();

				}
				newSql.append(c);
				newSql.append(nextC);
				i = i + 2;
				continue;
			}
			// comments handling end

			if (withinQuotes) {
				if (c == currentQuote) {
					withinQuotes = false;
					currentQuote = '-';
				}
				newSql.append(c);
			} else {
				if (c == '"' || c == '\'') {
					withinQuotes = true;
					currentQuote = c;
					newSql.append(c);
				} else {
					if (c == ':' || c == '&') {
						int j = i + 1;
						if (j < statement.length && statement[j] == ':' && c == ':') { 
							// Postgres-style "::" casting operator - to be skipped. 
							i = i + 2; 
							continue; 
						}
						
						if (j < statement.length && statement[j] == '&' && c == '&') { 
							// Postgres-style "&&" casting operator - to be skipped. 
							i = i + 2; 
							continue; 
						} 
						while (j < statement.length
								&& !isParameterSeparator(statement[j])) {
							j++;
						}
						if (j - i > 1) {
							String paramName = sql.substring(i + 1, j);
							if (paramSource != null && paramSource.hasValue(paramName)) {
								Object value = paramSource.getValue(paramName);
								if (value instanceof Collection) {
									Collection entries = (Collection) value;
									for (int k = 0; k < entries.size(); k++) {
										if (k > 0) {
											newSql.append(", ");
										}
										newSql.append("?");
									}
								} else {
									newSql.append("?");
								}
							} else {
								newSql.append("?");
							}
						} else {
							newSql.append(c);
						}
						i = j - 1;
					} else {
						newSql.append(c);
					}
				}
			}
			i++;
		}
		return newSql.toString();
	}

	public static int[] buildSqlTypeArray(ParsedSql parsedSql,
			SqlParameterSource paramSource) {
		int[] sqlTypes = new int[parsedSql.getTotalParameterCount()];
		String[] paramNames = parsedSql.getParameterNames();
		for (int i = 0; i < paramNames.length; i++) {
			sqlTypes[i] = paramSource.getSqlType(paramNames[i]);
		}
		return sqlTypes;
	}

	private static boolean isParameterSeparator(char c) {
		if (Character.isWhitespace(c)) {
			return true;
		}
		for (int i = 0; i < PARAMETER_SEPARATORS.length; i++) {
			if (c == PARAMETER_SEPARATORS[i]) {
				return true;
			}
		}
		return false;
	}

	private static int isCommentsOpen(char c, char nextC) {
		if (c == '/' && nextC == '*')
			return BLOCK_COMMENTS;
		if (c == '-' && nextC == '-')
			return LINE_COMMENTS;
		return -1;
	}

	private static boolean isCommentsClose(int commentsType, int length,
			int idx, char c, char nextC) {
		if (commentsType == BLOCK_COMMENTS && c == '*' && nextC == '/')
			return true;
		if (commentsType == LINE_COMMENTS && (c == '\n' || idx == (length - 1)))
			return true;
		return false;
	}

	public static String parseSqlStatementIntoString(String sql) {
		return parseSqlStatement(sql).getNewSql();
	}

	public static Object[] buildValueArray(String sql, Map<String, ?> paramMap) {
		ParsedSql parsedSql = parseSqlStatement(sql);
		return buildValueArray(parsedSql, new MapSqlParameterSource(paramMap));
	}

}

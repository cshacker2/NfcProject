package com.aoc.nfc.queryservice.impl.util;

public class ParsedSql {

	private String sql;

	private String newSql;

	private String[] parameterNames;

	private int namedParameterCount;

	private int unnamedParameterCount;

	private int totalParameterCount;

	public ParsedSql() {
	}

	public ParsedSql(String sql) {
		this.sql = sql;
	}

	public void setSql(String sql) {
		this.sql = sql;
	}

	public String getSql() {
		return sql;
	}

	public void setNewSql(String newSql) {
		this.newSql = newSql;
	}

	public String getNewSql() {
		return newSql;
	}

	public void setParameterNames(String[] parameterNames) {
		this.parameterNames = parameterNames;
	}

	public String[] getParameterNames() {
		return parameterNames;
	}

	public void setNamedParameterCount(int namedParameterCount) {
		this.namedParameterCount = namedParameterCount;
	}

	public int getNamedParameterCount() {
		return namedParameterCount;
	}

	public void setUnnamedParameterCount(int unnamedParameterCount) {
		this.unnamedParameterCount = unnamedParameterCount;
	}

	public int getUnnamedParameterCount() {
		return unnamedParameterCount;
	}

	public void setTotalParameterCount(int totalParameterCount) {
		this.totalParameterCount = totalParameterCount;
	}

	public int getTotalParameterCount() {
		return totalParameterCount;
	}

}

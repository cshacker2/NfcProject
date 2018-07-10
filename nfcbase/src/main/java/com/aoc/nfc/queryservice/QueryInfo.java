package com.aoc.nfc.queryservice;

import java.util.List;

import org.springframework.jdbc.core.SqlParameter;

public interface QueryInfo {

	String getQueryString();
	String getQueryId();
	String getResultClass();
	boolean doesNeedColumnMapping();
	boolean isDynamic();
	String getMappingStyle();
	int getFetchCountPerQuery();
	List<SqlParameter> getSqlParameterList();
	int getSqlType(int pos);
	int[] getSqlTypes();
	int getSqlType(String name);
	String getResultMapper();
	int getMaxFetchSize();
}

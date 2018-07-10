package com.aoc.nfc.queryservice;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import com.aoc.nfc.queryservice.exception.QueryException;


public interface QueryService {
	Logger LOGGER = LoggerFactory.getLogger(QueryService.class);
	String COL_INFO = "COLUMN_INFO";
	String COUNT = "COUNT";
	String LIST = "LIST";
	
	<T> List<T> find(String queryId, Object[] values) throws QueryException;

	int countQuery();
	Map<String, String> getQueryMap() throws QueryException;
	List<String[]> getQueryParams(String queryId) throws QueryException;
	JdbcTemplate getQueryServiceJdbcTemplate();
	String getStatement(String queryId);
	QueryInfo getQueryInfo(String queryId);
}

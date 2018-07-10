package com.aoc.nfc.queryservice;

import java.util.Map;

import com.aoc.nfc.queryservice.impl.jdbc.mapper.ResultSetMappingConfiguration;

public interface SqlLoader {

	boolean hasQuery(String queryId);
	Map<String, MappingInfo> getMappingInfos();
	Map<String, QueryInfo> getQueryInfos();
	int getFetchCountPerQuery(String queryId);
	Map<String, String> getNullCheck();
	int countQuery();
	MappingInfo getMappingInfo(String queryId);
	Map<String, ResultSetMappingConfiguration> getQueryResultMappings();
	String getQueryStatement(String queryId);
	boolean isDynamicQueryStatement(String queryId);
}

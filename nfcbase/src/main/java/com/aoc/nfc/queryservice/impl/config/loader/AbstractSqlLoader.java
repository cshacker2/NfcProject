package com.aoc.nfc.queryservice.impl.config.loader;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import com.aoc.nfc.queryservice.MappingInfo;
import com.aoc.nfc.queryservice.QueryInfo;
import com.aoc.nfc.queryservice.SqlLoader;
import com.aoc.nfc.queryservice.impl.config.info.DefaultMappingInfo;
import com.aoc.nfc.queryservice.impl.config.info.DefaultQueryInfo;
import com.aoc.nfc.queryservice.impl.jdbc.mapper.ResultSetMappingConfiguration;

public abstract class AbstractSqlLoader implements SqlLoader, InitializingBean,
		DisposableBean {

	private boolean skipError = false;
	private int dynamicReload = 0;
	private final Map<String, String> nullchecks = new HashMap<String, String>();
	private final Map<String, QueryInfo> queryInfos = new HashMap<String, QueryInfo>();
	private final Map<String, MappingInfo> tableMappings = new HashMap<String, MappingInfo>();
	private final Map<String, ResultSetMappingConfiguration> queryResultMappings = new HashMap<String, ResultSetMappingConfiguration>();
	private int registeredQueryCount = 0;

	public abstract void loadMappings();

	public void afterPropertiesSet() {
		loadMappings();
	}

	public void destroy() {
		clearMappings();
		getNullchecks().clear();
	}

	public int countQuery() {
		return registeredQueryCount;
	}

	public boolean hasQuery(String queryId) {
		return queryInfos.get(queryId) != null;
	}

	public String getQueryStatement(String queryId) {
		return queryInfos.get(queryId).getQueryString();
	}

	public boolean isDynamicQueryStatement(String queryId) {
		return queryInfos.get(queryId).isDynamic();
	}

	public int getFetchCountPerQuery(String queryId) {
		return queryInfos.get(queryId).getFetchCountPerQuery();
	}

	public String getTableFromClassName(String className) {
		return tableMappings.get(className).getTableName();
	}

	public Map<String, String> getNullCheck() {
		return nullchecks;
	}

	public String[] getPrimaryKeysFromClassName(String className) {
		return tableMappings.get(className).getPrimaryKeyColumns();
	}

	public Map<String, QueryInfo> getQueryInfos() {
		return queryInfos;
	}

	public Map<String, MappingInfo> getMappingInfos() {
		return tableMappings;
	}

	public MappingInfo getMappingInfo(String queryId) {
		if (!hasQuery(queryId)) {
			return null;
		}
		final DefaultQueryInfo queryInfo = (DefaultQueryInfo) queryInfos.get(queryId);

		DefaultMappingInfo mappingInfo = queryInfo.getLocalMappingInfo();
		String resultClass = queryInfo.getResultClass();

		if (mappingInfo != null) {
			return mappingInfo;
		} else {
			return mappingInfo = (DefaultMappingInfo) tableMappings.get(resultClass);
		}
		
	}

	public void setRegisteredQueryCount(int registeredQueryCount) {
		this.registeredQueryCount = registeredQueryCount;
	}

	public void setDynamicReload(int dynamicReload) {
		this.dynamicReload = dynamicReload;
	}

	public void setSkipError(boolean skipError) {
		this.skipError = skipError;
	}

	public void setNullchecks(Map<String, String> nullchecks) {
		Iterator<String> itr = nullchecks.keySet().iterator();
		while (itr.hasNext()) {
			String key = itr.next();
			String value = nullchecks.get(key);
			
			this.nullchecks.put(key.toLowerCase(), value);
		}
	}

	public int getDynamicReload() {
		return dynamicReload;
	}

	public boolean isSkipError() {
		return skipError;
	}

	public Map<String, String> getNullchecks() {
		return nullchecks;
	}

	public Map<String, ResultSetMappingConfiguration> getQueryResultMappings() {
		return queryResultMappings;
	}

	void putQueryMappingInfo(String queryId, QueryInfo queryInfo) {
		this.queryInfos.put(queryId, queryInfo);
	}

	void putTableMappingInfo(String className, MappingInfo mappingInfo) {
		this.tableMappings.put(className, mappingInfo);
	}

	void incrementQueryCount() {
		++registeredQueryCount;
	}

	void clearMappings() {
		queryInfos.clear();
		tableMappings.clear();
		this.registeredQueryCount = 0;
	}
}

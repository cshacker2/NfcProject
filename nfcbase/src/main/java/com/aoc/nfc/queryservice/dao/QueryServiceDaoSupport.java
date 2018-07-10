package com.aoc.nfc.queryservice.dao;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.aoc.nfc.queryservice.QueryService;
import com.aoc.nfc.queryservice.exception.QueryException;

public class QueryServiceDaoSupport {
	
	private QueryService queryService;

	public QueryService getQueryService() {
		return queryService;
	}

	public void setQueryService(QueryService queryService) {
		this.queryService = queryService;
	}

	protected <T> List<T> findList(String queryId, Map<String, ?> targetMap) throws QueryException {
		Object[] params = convertParams(targetMap);
		return queryService.find(queryId, params);
	}

	protected <T> List<T> findList(String queryId, Object[] targetObjs) throws QueryException {
		return queryService.find(queryId, targetObjs);
	}

	@SuppressWarnings("unchecked")
	private Object[] convertParams(Map<String, ?> targetMap) {
		Object[] params = new Object[targetMap.size()];
		Iterator targetMapIterator = targetMap.entrySet().iterator();
		int i = 0;
		while (targetMapIterator.hasNext()) {
			Map.Entry entry = (Map.Entry) targetMapIterator.next();
			params[i] = new Object[] { entry.getKey(), entry.getValue() };
			i++;
		}
		return params;
	}

}

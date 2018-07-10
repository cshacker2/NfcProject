package com.aoc.nfc.queryservice.impl;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.velocity.app.Velocity;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

import com.aoc.nfc.queryservice.MappingInfo;
import com.aoc.nfc.queryservice.QueryInfo;
import com.aoc.nfc.queryservice.QueryService;
import com.aoc.nfc.queryservice.ResultSetMapper;
import com.aoc.nfc.queryservice.exception.QueryException;
import com.aoc.nfc.queryservice.impl.jdbc.CustomJdbcTemplate;
import com.aoc.nfc.queryservice.impl.jdbc.CustomNamedParamJdbcTemplate;
import com.aoc.nfc.queryservice.impl.jdbc.mapper.CallbackResultSetMapper;
import com.aoc.nfc.queryservice.impl.jdbc.mapper.DefaultCallbackResultSetMapper;
import com.aoc.nfc.queryservice.impl.jdbc.mapper.ReflectionResultSetMapper;
import com.aoc.nfc.queryservice.impl.jdbc.setter.DefaultDynamicSqlParameterSource;
import com.aoc.nfc.queryservice.impl.jdbc.setter.DefaultDynamicSqlParameterSourceContext;
import com.aoc.nfc.queryservice.impl.util.SQLTypeTransfer;

public class QueryServiceImpl extends AbstractQueryService implements QueryService, InitializingBean {
	private static final String DELIMETER = "=";
	private CustomJdbcTemplate jdbcTemplate; 
	protected CustomNamedParamJdbcTemplate namedParamJdbcTemplate = null;

	public CustomNamedParamJdbcTemplate getNamedParamJdbcTemplate() {
		return namedParamJdbcTemplate;
	}

	public void setNamedParamJdbcTemplate(CustomNamedParamJdbcTemplate namedParamJdbcTemplate) {
		this.namedParamJdbcTemplate = namedParamJdbcTemplate;
	}

	public void setJdbcTemplate(CustomJdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
		this.namedParamJdbcTemplate = new CustomNamedParamJdbcTemplate(jdbcTemplate, jdbcTemplate.getDataSource());
		this.namedParamJdbcTemplate.setExceptionTranslator(jdbcTemplate.getExceptionTranslator());
	}

	

	public int countQuery() {
		return getSqlRepository().countQuery();
	}

	public Map<String, String> getQueryMap() {
		Map<String, String> queryMap = new HashMap<String, String>();
		try {
			Set<String> keys = getSqlRepository().getQueryInfos().keySet();
			Iterator<String> keyItr = keys.iterator();

			while (keyItr.hasNext()) {
				String queryId = keyItr.next();
				QueryInfo queryInfo = (QueryInfo) getSqlRepository().getQueryInfos().get(queryId);
				String statement = queryInfo.getQueryString();
				queryMap.put(queryId, statement);
			}

			return queryMap;
		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
	}

	
	public List<String[]> getQueryParams(String queryId) {
		try {
			QueryInfo queryInfo = (QueryInfo) getSqlRepository().getQueryInfos().get(queryId);
			List<SqlParameter> paramList = queryInfo.getSqlParameterList();
			List<String[]> results = new ArrayList<String[]>();
			
			for (int i = 0; i < paramList.size(); i++) {
				String[] params = new String[2];
				SqlParameter param = paramList.get(i);
				params[0] = param.getName();
				String paramTypeName = param.getTypeName();

				if (paramTypeName == null) {
					int type = param.getSqlType();
					paramTypeName = SQLTypeTransfer.getSQLTypeName(type);
				}

				params[1] = paramTypeName;
				results.add(params);
			}

			return results;
		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
	}

	public JdbcTemplate getQueryServiceJdbcTemplate() {
		return jdbcTemplate;
	}

	public String getStatement(String queryId) {
		QueryInfo queryInfo = (QueryInfo) getSqlRepository().getQueryInfos().get(queryId);
		return queryInfo.getQueryString();
	}

	public QueryInfo getQueryInfo(String queryId) {
		return (QueryInfo) getSqlRepository().getQueryInfos().get(queryId);
	}


	protected int[] convertTypes(String[] types) {
		int[] iTypes = new int[types.length];
		for (int i = 0; i < types.length; i++) {
			iTypes[i] = SQLTypeTransfer.getSQLType(types[i]);
		}
		return iTypes;
	}
	
	public <T> List<T> find(String queryId, Object[] values) throws QueryException {
		QueryInfo queryInfo = null;
		try {
			containesQueryId(queryId);
			queryInfo = (QueryInfo) getSqlRepository().getQueryInfos().get(queryId);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return findInternal(queryInfo, queryId, values, null);
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private <T> List<T> findInternal(QueryInfo queryInfo, String queryId, Object[] values, ReflectionResultSetMapper resultSetMapper) throws QueryException {
		String sql = "";

		try {
			sql = queryInfo.getQueryString();
			boolean isDynamic = queryInfo.isDynamic();
			int queryMaxFetchSize = queryInfo.getMaxFetchSize();
			if (queryMaxFetchSize == -1) {
				queryMaxFetchSize = jdbcTemplate.getMaxFetchSize();
			}

			if (resultSetMapper == null)
				resultSetMapper = createResultSetMapper(queryInfo, getSqlRepository().getNullCheck());

			if (isDynamic) {
				Map<Object, Object> properties = generatePropertiesMap(values, null, null);
				
				if (properties == null)
					properties = new Properties();

				DefaultDynamicSqlParameterSource sqlParameterSource = new DefaultDynamicSqlParameterSource(properties);
				sql = getRunnableSQL(sql, sqlParameterSource);

				if (isVelocity(sql)) {
					StringWriter writer = new StringWriter();
					Velocity.evaluate(new DefaultDynamicSqlParameterSourceContext(sqlParameterSource), writer, "QueryService", sql);
					sql = writer.toString();
				}
				namedParamJdbcTemplate.query(sql, queryMaxFetchSize, sqlParameterSource, resultSetMapper);
				return resultSetMapper.getObjects();
			} else {
				return jdbcTemplate.query(sql, values, queryInfo.getSqlTypes(), queryMaxFetchSize, (RowMapper) resultSetMapper);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private ReflectionResultSetMapper createResultSetMapper(QueryInfo queryInfo, Map<String, String> nullchecks) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
		Class<?> targetClazz = null;
		ResultSetMapper customResultSetMapper = null;
		Class<?> mapperClazz = null;

		if (queryInfo.getResultMapper() != null) {
			mapperClazz = Thread.currentThread().getContextClassLoader().loadClass(queryInfo.getResultMapper());
			if (ResultSetMapper.class.isAssignableFrom(mapperClazz) && !(ReflectionResultSetMapper.class.isAssignableFrom(mapperClazz))) {
				customResultSetMapper = (ResultSetMapper) mapperClazz.newInstance();
			}
		}
		if (queryInfo.doesNeedColumnMapping()) {
			targetClazz = Thread.currentThread().getContextClassLoader().loadClass(queryInfo.getResultClass());
		} else {
			targetClazz = HashMap.class;
		}
		
		MappingInfo mappingInfo = this.getSqlRepository().getMappingInfo(queryInfo.getQueryId());
		ReflectionResultSetMapper callbackResultSetMapper = null;
		if (mapperClazz != null && CallbackResultSetMapper.class.isAssignableFrom(mapperClazz)) {
			callbackResultSetMapper = new CallbackResultSetMapper(targetClazz, mappingInfo, nullchecks, queryInfo.getMappingStyle());
			customResultSetMapper = callbackResultSetMapper;
		} else if (mapperClazz != null && ReflectionResultSetMapper.class.isAssignableFrom(mapperClazz)) {
			callbackResultSetMapper = new ReflectionResultSetMapper(targetClazz, mappingInfo, nullchecks);
			customResultSetMapper = callbackResultSetMapper;
		} else {
			callbackResultSetMapper = new DefaultCallbackResultSetMapper(targetClazz, mappingInfo, nullchecks, queryInfo.getMappingStyle());
		}

		callbackResultSetMapper.setSqlLoader(this.getSqlRepository());
		callbackResultSetMapper.setQueryId(queryInfo.getQueryId());

		if (customResultSetMapper != null) {
			callbackResultSetMapper.setCustomResultSetMapper(customResultSetMapper);
		}
		
		return callbackResultSetMapper;
	}

	
	private Map<Object, Object> generatePropertiesMap(Object[] values, int[] types, MapSqlParameterSource mapSqlParameterSource) {
		Map<Object, Object> properties = new HashMap<Object, Object>();
		String tempStr = null;
		Object[] tempArray = null;

		for (int i = 0; i < values.length; i++) {
			if (values[i] instanceof String) {
				tempStr = (String) values[i];
				int pos = tempStr.indexOf(DELIMETER);
				if (pos < 0) {
					System.out.println("Query Service : Invalid Argument - Argument String must include a delimiter '='.");
				}
				properties.put(tempStr.substring(0, pos), tempStr.substring(pos + 1));
				
				if (mapSqlParameterSource != null) {
					mapSqlParameterSource.addValue(tempStr.substring(0, pos), tempStr.substring(pos + 1), types[i]);
				}
				
			} else if (values[i] instanceof Object[]) {
				tempArray = (Object[]) values[i];
				if (tempArray.length != 2) {
					System.out.println("Query Service : Invalid Argument - Argument Object Array size must be 2.");
				}
				properties.put(tempArray[0], tempArray[1]);
				if (mapSqlParameterSource != null) {
					mapSqlParameterSource.addValue((String) tempArray[0], tempArray[1], types[i]);
				}
			} else if (values[i] == null) {
				continue;
			} else {
				return null;
			}
		}
		return properties;
	}

}

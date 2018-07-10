package com.aoc.nfc.queryservice.impl.jdbc;

import javax.sql.DataSource;

import org.springframework.jdbc.core.CallableStatementCallback;
import org.springframework.jdbc.core.CallableStatementCreator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.SQLExceptionTranslator;

import com.aoc.nfc.queryservice.impl.util.NamedParameterUtil;
import com.aoc.nfc.queryservice.impl.util.ParsedSql;

public class CustomNamedParamJdbcTemplate extends NamedParameterJdbcTemplate {

	protected int maxFetchSize = -1;

	private final CustomJdbcTemplate pagingJdbcTemplate;

	public CustomJdbcTemplate getPagingJdbcTemplate() {
		return pagingJdbcTemplate;
	}

	public int getMaxFetchSize() {
		return maxFetchSize;
	}

	public void setMaxFetchSize(int maxFetchSize) {
		this.maxFetchSize = maxFetchSize;
	}

	public CustomNamedParamJdbcTemplate(CustomJdbcTemplate jdbcTemplate) {
		super(jdbcTemplate.getDataSource());
		setMaxFetchSize(jdbcTemplate.getMaxFetchSize());
		pagingJdbcTemplate = jdbcTemplate;
	}

	public CustomNamedParamJdbcTemplate(CustomJdbcTemplate jdbcTemplate, DataSource dataSouce) {
		super(dataSouce);
		setMaxFetchSize(jdbcTemplate.getMaxFetchSize());
		pagingJdbcTemplate = jdbcTemplate;
	}

	public void setExceptionTranslator(SQLExceptionTranslator exceptionTranslator) {
		pagingJdbcTemplate.setExceptionTranslator(exceptionTranslator);
		((JdbcTemplate) getJdbcOperations()).setExceptionTranslator(exceptionTranslator);
	}

	public void query(String sql, int queryMaxFetchSize, SqlParameterSource sqlParameterSource, RowCallbackHandler rch) {
		SqlParameterSetter sqlParameterSetter = setSqlParameter(sql, sqlParameterSource);
		pagingJdbcTemplate.query(sqlParameterSetter.getSubstitutedSql(), sqlParameterSetter.getArgs(), sqlParameterSetter .getArgTypes(), queryMaxFetchSize, rch);
	}

	public Object execute(CallableStatementCreator callableStatementCreator, CallableStatementCallback<?> callableStatementCallback) {
		return pagingJdbcTemplate.execute(callableStatementCreator, callableStatementCallback);
	}

	private SqlParameterSetter setSqlParameter(String sql, SqlParameterSource sqlParameterSource) {
		ParsedSql parsedSql = NamedParameterUtil.parseSqlStatement(sql);
		Object[] args = NamedParameterUtil.buildValueArray(parsedSql, sqlParameterSource);
		int[] argTypes = NamedParameterUtil.buildSqlTypeArray(parsedSql, sqlParameterSource);
		String substitutedSql = NamedParameterUtil.substituteNamedParameters(sql, sqlParameterSource);
		return new SqlParameterSetter(substitutedSql, args, argTypes);
	}

//	private SqlParameterSetter setSqlParameter(String sql, SqlParameterSource sqlParameterSource, Map<String, ?> paramMap) {
//		ParsedSql parsedSql = NamedParameterUtil.parseSqlStatement(sql);
//		Object[] args = NamedParameterUtil.buildValueArray(sql, paramMap);
//		int[] argTypes = NamedParameterUtil.buildSqlTypeArray(parsedSql, sqlParameterSource);
//		String substitutedSql = NamedParameterUtil.substituteNamedParameters(sql, sqlParameterSource);
//		return new SqlParameterSetter(substitutedSql, args, argTypes);
//	}

	private class SqlParameterSetter {

		private final Object[] args;
		private final int[] argTypes;
		private final String substitutedSql;

		public SqlParameterSetter(String substitutedSql, Object[] args, int[] argTypes) {
			this.substitutedSql = substitutedSql;
			this.args = args;
			this.argTypes = argTypes;
		}

		public Object[] getArgs() {
			return args;
		}

		public int[] getArgTypes() {
			return argTypes;
		}

		public String getSubstitutedSql() {
			return substitutedSql;
		}
	}
}

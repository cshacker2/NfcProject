package com.aoc.nfc.queryservice.impl.jdbc.mapper;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Map;

import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.jdbc.support.JdbcUtils;

import com.aoc.nfc.queryservice.QueryInfo;
import com.aoc.nfc.queryservice.SqlLoader;
import com.aoc.nfc.queryservice.impl.util.ColumnUtil;

public class MappingStyleColumnMapRowMapper extends ColumnMapRowMapper {

	protected QueryInfo queryInfo = null;

	protected SqlLoader sqlLoader = null;

	public MappingStyleColumnMapRowMapper(SqlLoader sqlLoader,
			QueryInfo queryInfo) {
		this.sqlLoader = sqlLoader;
		this.queryInfo = queryInfo;
	}

	public Map<String,Object> mapRow(ResultSet rs, int rowNum) throws SQLException {
		ResultSetMetaData rsmd = rs.getMetaData();
		int columnCount = rsmd.getColumnCount();
		Map<String, Object> mapOfColValues = createColumnMap(columnCount);
		for (int i = 1; i <= columnCount; i++) {
			String key = getColumnKey(JdbcUtils.lookupColumnName(rsmd, i));
			Object obj = getColumnValue(rs, i);
			mapOfColValues.put(getMappingStylekey(key), obj);
		}
		return mapOfColValues;
	}

	public String getMappingStylekey(String key) {
		return ColumnUtil.changeColumnName(queryInfo.getMappingStyle(),
				key);
	}
}

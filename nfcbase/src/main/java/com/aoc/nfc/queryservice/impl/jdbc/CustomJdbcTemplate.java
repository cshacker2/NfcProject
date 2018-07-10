package com.aoc.nfc.queryservice.impl.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.util.Assert;

import com.aoc.nfc.queryservice.RowMetadataCallbackHandler;
import com.aoc.nfc.queryservice.impl.jdbc.setter.PreparedStatementArgTypeSetter;

public class CustomJdbcTemplate extends JdbcTemplate { 

	protected int maxFetchSize = -1;

//	private static final String RETURN_RESULT_SET_PREFIX = "#result-set-";
//
//	private static final String RETURN_UPDATE_COUNT_PREFIX = "#update-count-";

	private boolean skipResultsProcessing = false;

	private boolean skipUndeclaredResults = false;

	public void setSkipResultsProcessing(boolean skipResultsProcessing) {
		this.skipResultsProcessing = skipResultsProcessing;
	}

	public boolean isSkipResultsProcessing() {
		return this.skipResultsProcessing;
	}

	public void setSkipUndeclaredResults(boolean skipUndeclaredResults) {
		this.skipUndeclaredResults = skipUndeclaredResults;
	}

	public boolean isSkipUndeclaredResults() {
		return this.skipUndeclaredResults;
	}

	public CustomJdbcTemplate() {
		super();
	}

	public CustomJdbcTemplate(DataSource dataSource) {
		super(dataSource);
	}

	public int getMaxFetchSize() {
		return maxFetchSize;
	}

	public void setMaxFetchSize(int maxFetchSize) {
		this.maxFetchSize = maxFetchSize;
	}

	@SuppressWarnings("unchecked")
	public void query(String sql, Object[] args, int[] argTypes, int queryMaxFetchSize, RowCallbackHandler rch) {
//		if (checkPagingSQLGenerator(paginationSQLGetter)) {
//			query(new PagingPreparedStatementCreator(sql), new PreparedStatementArgTypeSetter(args, argTypes, null), new NonPagingRowCallbackHandlerResultSetExtractor(rch, queryMaxFetchSize));
//			return;
//		}
		query(sql, new PreparedStatementArgTypeSetter(args, argTypes), new NonPagingRowCallbackHandlerResultSetExtractor(rch, queryMaxFetchSize));
	}

	
	@SuppressWarnings("rawtypes")
	public List query(String sql, Object[] args, int[] argTypes, int queryMaxFetchSize) {
		return query(sql, args, argTypes, queryMaxFetchSize, new ColumnMapRowMapper());
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public List query(String sql, Object[] args, int[] argTypes, int queryMaxFetchSize, RowMapper rowMapper) {
		return (List) query(sql, args, argTypes, new NonPagingRowMapperResultSetExtractor(rowMapper, queryMaxFetchSize));
	}


	
	@SuppressWarnings("rawtypes")
	private class NonPagingRowCallbackHandlerResultSetExtractor implements ResultSetExtractor {

		private final RowCallbackHandler rch;
		private final int queryMaxFetchSize;

		public NonPagingRowCallbackHandlerResultSetExtractor(RowCallbackHandler rch, int queryMaxFetchSize) {
			this.rch = rch;
			this.queryMaxFetchSize = queryMaxFetchSize;
		}

		public Object extractData(ResultSet rs) throws SQLException {
			int rowNum = 1;

			if (rch instanceof RowMetadataCallbackHandler && ((RowMetadataCallbackHandler) rch).isNeedColumnInfo()) {
				((RowMetadataCallbackHandler) rch).processMetaData(rs);
			}

			if (queryMaxFetchSize == -1) {
				while (rs.next()) {
					this.rch.processRow(rs);
					rowNum++;
				}
			} else {
				while (rs.next()) {
					if (rowNum > queryMaxFetchSize) {
						throw new DataRetrievalFailureException("Too many data in ResultSet. maxFetchSize is " + queryMaxFetchSize);
					}
					this.rch.processRow(rs);
					rowNum++;
				}
			}
			return null;
		}
	}

	
	@SuppressWarnings("rawtypes")
	private class NonPagingRowMapperResultSetExtractor implements ResultSetExtractor {

		private final RowMapper rowMapper;
		private final int queryMaxFetchSize;

		public NonPagingRowMapperResultSetExtractor(RowMapper rowMapper, int queryMaxFetchSize) {
			Assert.notNull(rowMapper, "Query Service : RowMapper is required");
			this.rowMapper = rowMapper;
			this.queryMaxFetchSize = queryMaxFetchSize;
		}

		public Object extractData(ResultSet rs) throws SQLException {
			List<Object> results = new ArrayList<Object>();
			int rowNum = 1;

			if (rowMapper instanceof RowMetadataCallbackHandler && ((RowMetadataCallbackHandler) rowMapper).isNeedColumnInfo()) {
				((RowMetadataCallbackHandler) rowMapper).processMetaData(rs);
			}

			if (queryMaxFetchSize == -1) {
				while (rs.next()) {
					results.add(this.rowMapper.mapRow(rs, rowNum));
					rowNum++;
				}
			} else {
				while (rs.next()) {
					if (rowNum > queryMaxFetchSize) {
						throw new DataRetrievalFailureException(
								"Too many data in ResultSet. maxFetchSize is "
										+ queryMaxFetchSize);
					}
					results.add(this.rowMapper.mapRow(rs, rowNum));
					rowNum++;
				}
			}

			return results;
		}
	}
}

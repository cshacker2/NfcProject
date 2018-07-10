package com.aoc.nfc.queryservice;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import org.springframework.jdbc.core.RowCallbackHandler;

public interface RowMetadataCallbackHandler extends RowCallbackHandler {

	void setNullCheckInfos(Map<String, String> nullCheckInfos);
	void processMetaData(ResultSet rs) throws SQLException;
	void setNeedColumnInfo(boolean needColumnInfo);
	boolean isNeedColumnInfo();
}

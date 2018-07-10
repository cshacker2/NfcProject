package com.aoc.nfc.queryservice;

import java.util.Map;

import com.aoc.nfc.queryservice.impl.util.Tree;

public interface MappingInfo {

	String[] getPrimaryKeyColumns();
	String getTableName();
	String getClassName();
	Map<String, String> getMappingInfoAsMap();
	Map<String, String[]> getCompositeColumnNames();
	Tree<String> getCompositeFieldNames();
}

package com.aoc.nfc.queryservice.impl.config.info;

import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.aoc.nfc.queryservice.MappingInfo;
import com.aoc.nfc.queryservice.impl.util.StringUtil;
import com.aoc.nfc.queryservice.impl.util.Tree;

public class DefaultMappingInfo implements MappingInfo {
	private String className = null;
	private String tableName = null;
	private String[] columnNames = new String[0];
	private String[] fieldNames = new String[0];

	private Map<String, String[]> compositeColumnNames = new HashMap<String, String[]>();

	private Tree<String> compositeFieldNames;
	private String[] primaryKeyColumnNames = new String[0];

	public void configure(Element table) {
		className = table.getAttribute("class");
		checkRequiredAttribute("table", "class", className);

		tableName = table.getAttribute("name");
		checkRequiredAttribute("table", "name", tableName);

		NodeList mappings = table.getElementsByTagName("field-mapping");
		hasMultipleElements("table", "field-mapping", mappings.getLength());

		columnNames = new String[mappings.getLength()];
		fieldNames = new String[mappings.getLength()];
		
		for (int i = 0; i < mappings.getLength(); i++) {
			NodeList dbmsColumn = ((Element) mappings.item(i)).getElementsByTagName("dbms-column");
			hasOnlyOneElements("field-mapping", "dbms-column", dbmsColumn.getLength());
			columnNames[i] = dbmsColumn.item(0).getTextContent();

			NodeList classAttribute = ((Element) mappings.item(i)).getElementsByTagName("class-attribute");
			hasOnlyOneElements("field-mapping", "class-attribute", classAttribute.getLength());
			fieldNames[i] = classAttribute.item(0).getTextContent();
		}

		NodeList primarykeys = table.getElementsByTagName("primary-key");
		hasOnlyOneElements("table", "primary-key", primarykeys.getLength());

		NodeList primaryKeyColumns = ((Element) primarykeys.item(0)).getElementsByTagName("dbms-column");
		hasMultipleElements("primary-key", "dbms-column", primaryKeyColumns.getLength());

		primaryKeyColumnNames = new String[primaryKeyColumns.getLength()];
		for (int i = 0; i < primaryKeyColumns.getLength(); i++) {
			primaryKeyColumnNames[i] = ((Element) primaryKeyColumns.item(i)).getTextContent();
		}
	}

	protected String getFieldNameCorrespondingToColumnName(String columnName) {
		for (int i = 0, size = columnNames.length; i < size; i++) {
			if (columnNames[i].equals(columnName)) {
				return fieldNames[i];
			}
		}
		return null;
	}

	public String getClassName() {
		return className;
	}

	public String[] getPrimaryKeyColumns() {
		return primaryKeyColumnNames;
	}

	public void setColumnNames(String[] columnNames) {
		this.columnNames = columnNames;
	}

	public void setFieldNames(String[] fieldNames) {
		this.fieldNames = fieldNames;
	}

	public String getTableName() {
		return tableName;
	}

	public Map<String, String> getMappingInfoAsMap() {
		Map<String, String> rtMap = new HashMap<String, String>();
		for (int i = 0, size = columnNames.length; i < size; i++) {
			rtMap.put(columnNames[i].toLowerCase(), fieldNames[i]);
		}
		return rtMap;
	}

	public Map<String, String[]> getCompositeColumnNames() {
		return compositeColumnNames;
	}

	public void setCompositeColumnNames(
			Map<String, String[]> compositeColumnNames) {
		this.compositeColumnNames = compositeColumnNames;
	}

	private void checkRequiredAttribute(String element, String name, String value) {
		if (StringUtil.isEmpty(value)) {
			System.out.println("Query Service : " + name + " is essential attribute in a <" + element + ">.");
			return;
		}
	}

	private void hasMultipleElements(String parentElement, String childElement, int length) {
		if (length == 0) {
			System.out.println("Query Service : must have <" + childElement + "> over one in a <" + parentElement + ">.");
			return;
		}
	}

	private void hasOnlyOneElements(String parentElement, String childElement, int length) {
		if (length == 0 || length > 1) {
			System.out.println("Query Service : must have one <" + childElement + "> in a <" + parentElement + ">.");
			return;
		}
	}

	public Tree<String> getCompositeFieldNames() {
		return compositeFieldNames;
	}

	public void setCompositeFieldNames(Tree<String> compositeFieldNames) {
		this.compositeFieldNames = compositeFieldNames;
	}
}

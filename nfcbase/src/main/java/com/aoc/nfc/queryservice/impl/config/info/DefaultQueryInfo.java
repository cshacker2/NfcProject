package com.aoc.nfc.queryservice.impl.config.info;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.SqlTypeValue;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.aoc.nfc.queryservice.QueryInfo;
import com.aoc.nfc.queryservice.QueryService;
import com.aoc.nfc.queryservice.impl.util.SQLTypeTransfer;
import com.aoc.nfc.queryservice.impl.util.StringUtil;
import com.aoc.nfc.queryservice.impl.util.Tree;

public class DefaultQueryInfo implements QueryInfo {
	protected static final String PROPERTY_NOT_SETTING = "";
	protected static final String CAMEL_CASE = "camel";
	protected static final String LOWER_CASE = "lower";

	private String queryId = null;
	private String statement = null;
	private String resultClass = null;
	private String resultMapper = null;
	private DefaultMappingInfo localMappingInfo = null;
	private boolean dynamic = true;
//	private String lobStatement = null;
//	private String[] lobParamTypes = null;
	private int length = 0;
	private String mappingStyle = null;
	private int maxFetchSize = -1;

	private String[] paramTypeNames = null;
	private String[] paramBindingTypes = null;
	private String[] paramBindingNames = null;
	private List<SqlParameter> sqlParameterList = null;
	private final Map<String, Integer> paramMap = new HashMap<String, Integer>();

	public void configure(Element query) {
		queryId = query.getAttribute("id");
		checkRequiredAttribute("query", "id", queryId);
		NodeList statements = query.getElementsByTagName("statement");
		Element statementElement = null;
		int statementLength = 0;
		
		for (int i = 0; i < statements.getLength(); i++) {
			Element temporaryElement = (Element) statements.item(i);
			String parentNode = temporaryElement.getParentNode().getNodeName();
			if ("query".equals(parentNode)) {
				statementElement = temporaryElement;
				statementLength++;
			}
		}

		hasOnlyOneElements("query", "statement", statementLength);
		statement = statementElement.getTextContent();

		String isDynamicValue = query.getAttribute("isDynamic");
		dynamic = "".equals(isDynamicValue) ? true : new Boolean(isDynamicValue).booleanValue();

		String mappingStyleValue = query.getAttribute("mappingStyle");
		mappingStyle = "".equals(mappingStyleValue) ? "camel" : mappingStyleValue;

		String maxFetchSizeValue = query.getAttribute("maxFetchSize");
		maxFetchSize = "".equals(maxFetchSizeValue) ? -1 : new Integer(maxFetchSizeValue).intValue();

		NodeList results = query.getElementsByTagName("result");

		if (results.getLength() > 0) {
			hasOnlyOneElements("query", "result", results.getLength());
			Element result = (Element) results.item(0);

			resultClass = result.getAttribute("class");
			if ("".equals(resultClass)) {
				resultClass = null;
			}
			resultMapper = result.getAttribute("mapper");
			if ("".equals(resultMapper)) {
				resultMapper = null;
			}

			String lengthValue = result.getAttribute("length");
			length = "".equals(lengthValue) ? 0 : new Integer(lengthValue).intValue();

			NodeList resultMappings = result.getElementsByTagName("result-mapping");
			if (resultMappings.getLength() > 0) {
				localMappingInfo = new DefaultMappingInfo();
				List<String> columns = new ArrayList<String>();
				List<String> fields = new ArrayList<String>();

				Map<String, String[]> compositeColumnMap = new HashMap<String, String[]>();
				Tree<String> compositeFiledTree = new Tree<String>("compositeFiledTree");
				
				for (int i = 0; i < resultMappings.getLength(); i++) {
					Element resultMapping = (Element) resultMappings.item(i);

					String column = resultMapping.getAttribute("column");
					String field = resultMapping.getAttribute("attribute");

					if (isComposite(column) && isComposite(field)) {
						column = column.substring(1, column.length() - 1);
						field = field.substring(1, field.length() - 1);
						String[] compositeColumns = StringUtils.trimAllWhitespace(column).split(",");
						String[] compositeFieldes = StringUtils.trimAllWhitespace(field).split(",");
						
						if (compositeColumns.length == compositeFieldes.length) {
							String compositeField = "";
							Map<String, List<String>> tmpColumnMap = new HashMap<String, List<String>>();
							for (int j = 0; j < compositeFieldes.length; j++) {
								
								String compositeFieldName = compositeFieldes[j];
								String key = compositeFieldName.substring(0, compositeFieldName.indexOf("."));
								Tree<String> child;
								
								if(compositeFiledTree.getTree(key)!=null){
									child = compositeFiledTree.getTree(key);
								}else{
									child = compositeFiledTree.addLeaf(key);
								}
									
								if (i != 0 && !key.equals(compositeField))
									QueryService.LOGGER.warn("Query Service : This mapping information is ignored. Property name is different. If you want to handle properties of user defined type, attribute should start with same property name. Please check result mapping (queryId ='{}')", queryId);
								
								compositeField = key;
								
								//Tree 구조 생성 후 매핑할 field 리턴
								compositeField = makeNdepthTree(compositeFieldName, child, key);
								
								List<String> attrs  = new ArrayList<String>();
								if(tmpColumnMap.get(compositeField)!=null && tmpColumnMap.get(compositeField).size() > 0){
									attrs = tmpColumnMap.get(compositeField);
								}
								attrs.add(compositeColumns[j]);
								tmpColumnMap.put(compositeField, attrs);

							}
							Set<String> keySet = tmpColumnMap.keySet();
							Iterator<String> keyItr = keySet.iterator();
							
							while(keyItr.hasNext()){
								String colKey = keyItr.next();
								List<String> colList = tmpColumnMap.get(colKey);
								String[] colValues = new String[colList.size()];
								for(int l=0; l < colList.size(); l++){
									colValues[l] = colList.get(l);
								}
								compositeColumnMap.put(colKey, colValues);
							}
							
						} else {
							QueryService.LOGGER.warn("Query Service : This mapping information is ignored. If you want to handle properties of user defined type, the number of column should be same as that of attribute. Please check result mapping (queryId ='{}')", queryId);
						}
					} else {
						columns.add(column);
						fields.add(field);
					}
				}

				localMappingInfo.setColumnNames(columns.toArray(new String[columns.size()]));
				localMappingInfo.setFieldNames(fields.toArray(new String[fields.size()]));
				localMappingInfo.setCompositeColumnNames(compositeColumnMap);
				localMappingInfo.setCompositeFieldNames(compositeFiledTree);
			}
		}

		NodeList params = query.getElementsByTagName("param");
		paramTypeNames = new String[params.getLength()];
		paramBindingTypes = new String[params.getLength()];
		paramBindingNames = new String[params.getLength()];

		for (int i = 0, size = params.getLength(); i < size; i++) {
			Element param = (Element) params.item(i);
			paramTypeNames[i] = param.getAttribute("type");
			paramBindingTypes[i] = param.getAttribute("binding");
			paramBindingNames[i] = param.getAttribute("name");

			if (isDynamic())
				paramMap.put(paramBindingNames[i], new Integer(SQLTypeTransfer.getSQLType(paramTypeNames[i].toUpperCase())));
		}

	}
	
	public String makeNdepthTree(String compositeFieldName, Tree<String> child, String rootKey){
		String[] fieldNames = compositeFieldName.substring(compositeFieldName.indexOf(".") + 1).split("\\.");
		
		if(fieldNames.length <= 1){
			child.addLeaf(fieldNames[0]);
		}else {
			Tree<String> nextChild;
			if(child.getTree(fieldNames[0]) != null){
				nextChild = child.getTree(fieldNames[0]);
			}else{
				nextChild = child.addLeaf(fieldNames[0]);
			}
			String nextRootKey = fieldNames[fieldNames.length-2];
			String nextCompositeFieldName = compositeFieldName.substring(compositeFieldName.indexOf('.')+1);
			makeNdepthTree(nextCompositeFieldName, nextChild, nextRootKey);
			return nextRootKey;
		}
		return rootKey;
	}

	public List<SqlParameter> getSqlParameterList() {
		if (sqlParameterList == null) {
			sqlParameterList = SQLTypeTransfer.getSqlParameterList(paramTypeNames, paramBindingTypes, paramBindingNames);
		}
		return sqlParameterList;
	}

	public int[] getSqlTypes() {
		int[] types = new int[paramTypeNames.length];
		for (int i = 0; i < paramTypeNames.length; i++) {
			types[i] = getSqlType(i);
		}

		return types;
	}

	public int getSqlType(int pos) {
		if (pos < paramTypeNames.length)
			return SQLTypeTransfer.getSQLType(paramTypeNames[pos].toUpperCase());
		else
			return SqlTypeValue.TYPE_UNKNOWN;
	}

	public int getSqlType(String paramTypeName) {
		Integer sqlType = paramMap.get(paramTypeName);
		if (sqlType == null)
			return SqlTypeValue.TYPE_UNKNOWN;
		else
			return sqlType.intValue();
	}

	public String getQueryId() {
		return queryId;
	}

	public String getQueryString() {
		return statement;
	}

	public String getResultClass() {
		return resultClass;
	}

	public boolean doesNeedColumnMapping() {
		return resultClass != null;
	}

	public boolean isDynamic() {
		return dynamic;
	}

	public int getFetchCountPerQuery() {
		return length;
	}

	public String getMappingStyle() {
		return mappingStyle;
	}

	public DefaultMappingInfo getLocalMappingInfo() {
		return localMappingInfo;
	}

	public String[] getParamBindingNames() {
		return paramBindingNames;
	}

	public void setParamBindingNames(String[] paramBindingNames) {
		this.paramBindingNames = paramBindingNames;
	}
	
	public String getResultMapper() {
		return resultMapper;
	}

	public int getMaxFetchSize() {
		return maxFetchSize;
	}

	private boolean isComposite(String str) {
		if (str.startsWith("{") && str.endsWith("}"))
			return true;
		return false;
	}

	private void checkRequiredAttribute(String element, String name, String value) {
		if (StringUtil.isEmpty(value)) {
			System.out.println("Query Service : " + name + " is essential attribute in a <" + element + ">.");
			return;
		}
	}

	private void hasOnlyOneElements(String parentElement, String childElement, int length) {
		if (length == 0 || length > 1) {
			System.out.println("Query Service : must have one <" + childElement + "> in a <" + parentElement + ">.");
			return;
		}
	}
}

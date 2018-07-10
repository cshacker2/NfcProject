package com.aoc.nfc.queryservice.impl.jdbc.mapper;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.tree.RowMapper;
import javax.swing.tree.TreePath;

import org.apache.commons.collections.map.ListOrderedMap;
import org.apache.commons.lang.ArrayUtils;

import com.aoc.nfc.queryservice.MappingInfo;
import com.aoc.nfc.queryservice.QueryService;
import com.aoc.nfc.queryservice.ResultSetMapper;
import com.aoc.nfc.queryservice.RowMetadataCallbackHandler;
import com.aoc.nfc.queryservice.SqlLoader;
import com.aoc.nfc.queryservice.impl.util.ColumnUtil;
import com.aoc.nfc.queryservice.impl.util.ReflectionHelp;
import com.aoc.nfc.queryservice.impl.util.SQLTypeTransfer;
import com.aoc.nfc.queryservice.impl.util.Tree;

public class ReflectionResultSetMapper<T> extends AbstractResultSetMapperSupport implements RowMapper, RowMetadataCallbackHandler {
	
	protected static final int PROPERTY_NOT_FOUND = -1;

	protected List<Class<T>> targetClasses;

	private final Map<Class<?>, ResultSetMappingConfiguration> classConfigMap = new HashMap<Class<?>, ResultSetMappingConfiguration>();

	private final MappingInfo mappingInfo;

	protected SqlLoader sqlLoader = null;

	protected String queryId;

	protected ResultSetMapper customResultSetMapper = null;

	@SuppressWarnings("rawtypes")
	protected List objects = new ArrayList();

	protected boolean initialized = false;

	protected boolean needColumnInfo = false;

	protected Class<?> targetClass = null;

	protected String mappingStyle = null;

	protected ResultSetMappingConfiguration mappingConfiguration;

	public void setSqlLoader(SqlLoader sqlLoader) {
		this.sqlLoader = sqlLoader;
	}

	public void setQueryId(String queryId) {
		this.queryId = queryId;
	}

	public void setCustomResultSetMapper(ResultSetMapper customResultSetMapper) {
		this.customResultSetMapper = customResultSetMapper;
	}

	@SuppressWarnings("rawtypes")
	public List getObjects() {
		return objects;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public ReflectionResultSetMapper(Class<T> targetClass, MappingInfo mappingInfo, Map<String, String> nullchecks) {
		super(nullchecks);
		this.mappingInfo = mappingInfo;
		targetClasses = new ArrayList();
		targetClasses.add(targetClass);
	}

	public MappingInfo getMappingInfo() {
		return mappingInfo;
	}

	public void processMetaData(ResultSet resultSet) throws SQLException {
		if (!this.initialized) {
			this.makeMeta(resultSet);
		}
	}

	@SuppressWarnings("unchecked")
	public void processRow(ResultSet resultSet) throws SQLException {
		objects.add(this.mapRow(resultSet, 9999));
	}

	@SuppressWarnings("unchecked")
	public T mapRow(ResultSet resultSet) throws SQLException {
		Object object = null;
		Iterator<Class<T>> targetClassIterator = targetClasses.iterator();
		while (targetClassIterator.hasNext() && object == null) {
			Class<T> targetClass = targetClassIterator.next();
			ResultSetMappingConfiguration config = getConfig(targetClass, resultSet.getMetaData());
			object = toObject(resultSet, targetClass, config);
		}
		return (T) object;
	}

	protected Object toObject(ResultSet resultSet, Class<?> targetClass, ResultSetMappingConfiguration config) {
		Object object;
		try {
			object = createObject(resultSet, targetClass, config);
			if (!config.getCompositeObjMap().isEmpty()) {
				setCompositeObject(resultSet, targetClass, config, object);
			}
			return object;
		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
	}

	private void setCompositeObject(ResultSet resultSet, Class<?> targetClass, ResultSetMappingConfiguration config, Object object) {
		
		Map<String, ResultSetMappingConfiguration> compositeObjMap = config.getCompositeObjMap();
		Set<String> keySet = compositeObjMap.keySet();
		Iterator<String> keyItr = keySet.iterator();
		
		while (keyItr.hasNext()) {
			String attribute = keyItr.next();
			ResultSetMappingConfiguration subconfiguration = config.getCompositeObjMap().get(attribute);
			Object compositeObj;
			try {
				compositeObj = createObject(resultSet, subconfiguration.getResultClass(), subconfiguration);
				subconfiguration.getCompositeClassSetter().invoke(object, new Object[] { compositeObj });
				if (!subconfiguration.getCompositeObjMap().isEmpty()) {
					setCompositeObject(resultSet, subconfiguration.getResultClass(), subconfiguration, compositeObj);
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}

	protected ResultSetMappingConfiguration getConfig(Class<?> targetClass, ResultSetMetaData resultSetMetaData) throws SQLException {
		ResultSetMappingConfiguration mappingConfiguration;

		if (classConfigMap.containsKey(targetClass)) {
			mappingConfiguration = classConfigMap.get(targetClass);
		} else {
			Map<String, Field> attributeMap = ReflectionHelp.getAllDeclaredFields(targetClass);
			mappingConfiguration = mapColumnsToAttributes(targetClass, resultSetMetaData, attributeMap, null, null, false);
			classConfigMap.put(targetClass, mappingConfiguration);
		}
		return mappingConfiguration;
	}

	@SuppressWarnings("rawtypes")
	private ResultSetMappingConfiguration mapColumnsToAttributes(Class<?> targetClass, ResultSetMetaData resultSetMetaData, Map<String, Field> attributeMap, Tree<String> compositeFields, String parentAttribute, boolean isComposite) throws SQLException {
		int totalCols = resultSetMetaData.getColumnCount();
		Field[] attributes = new Field[totalCols];
		String[] columnNames = new String[totalCols];
		int[] columnTypes = new int[totalCols];
		Method[] setters = new Method[totalCols];
		Map<String, ResultSetMappingConfiguration> compositeObjMap = new HashMap<String, ResultSetMappingConfiguration>();
		Arrays.fill(columnTypes, PROPERTY_NOT_FOUND);
		Map compositeColumnNames = this.mappingInfo.getCompositeColumnNames();
		String[] compositeColumns = null;
		
		if (compositeColumnNames != null && !compositeColumnNames.isEmpty()) {
			compositeColumns = (String[]) compositeColumnNames.get(parentAttribute);
		}
		
		if (compositeFields == null) {
			compositeFields = this.mappingInfo.getCompositeFieldNames();
		}
		
		PropertyDescriptor[] descriptors = null;
		
		try {
			BeanInfo info = Introspector.getBeanInfo(targetClass, Introspector.USE_ALL_BEANINFO);
			descriptors = info.getPropertyDescriptors();
		} catch (IntrospectionException ex) {
			ex.printStackTrace();
			descriptors = new PropertyDescriptor[0];
		}

		boolean hasCompositeField = false;
		if (compositeFields != null) {
			List<Tree<String>> subTrees = (List<Tree<String>>) compositeFields.getSubTrees();
			for (int i = 0; i < subTrees.size(); i++) {
				Tree<String> childField = subTrees.get(i);
				if (childField.getSubTrees().size() > 0) {
					hasCompositeField = true;
				}
			}
		}

		if (hasCompositeField) {
			compositeObjMap = makeCompositeObjMap(targetClass, descriptors, resultSetMetaData, compositeFields, attributeMap);
		}

		for (int idx = 0; idx < totalCols; idx++) {
			String columnName = resultSetMetaData.getColumnLabel(idx + 1);
			int columnType = resultSetMetaData.getColumnType(idx + 1);

			// 하위 어트리뷰트없고, 컴포짓클래스인 경우에는 continue 한다.
			if(isComposite && compositeColumns == null){
				continue;								
			}			
			
			if (isComposite && compositeColumns != null && ArrayUtils.indexOf(compositeColumns, columnName) < 0) {
				String lowerColumnName = new String(columnName);
				lowerColumnName= lowerColumnName.toLowerCase();
				if (isComposite && compositeColumns != null && ArrayUtils.indexOf(compositeColumns, lowerColumnName) < 0) {
					continue;
				}
			}

			Field attribute = getNameMatcher().isMatching(attributeMap,
					columnName, parentAttribute, attributes);
			if (attribute != null) {
				attributes[idx] = attribute;
				columnNames[idx] = columnName;

				int dataType = SQLTypeTransfer.getSQLType(attribute.getType());
				if (!((dataType == Types.VARCHAR && columnType == Types.CLOB) || (dataType == Types.VARBINARY && columnType == Types.BLOB))) {
					if (dataType != SQLTypeTransfer.UNDEFINED) {
						columnType = dataType;
					}
				}

				columnTypes[idx] = columnType;
				setters[idx] = findSetter(descriptors, targetClass.getName(), attribute.getName());
			}
		}

		return new ResultSetMappingConfiguration(columnNames, columnTypes, attributes, setters, compositeObjMap);
	}

	public Map<String, ResultSetMappingConfiguration> makeCompositeObjMap(Class<?> targetClass, PropertyDescriptor[] descriptors, ResultSetMetaData resultSetMetaData, Tree<String> compositeFields, Map<String, Field> attributeMap) throws SQLException {

		Collection<Tree<String>> compositeFieldList = compositeFields.getSubTrees();
		Set<String> keySet = new HashSet<String>();
		Iterator<Tree<String>> itr = compositeFieldList.iterator();

		while (itr.hasNext()) {
			Tree<String> field = itr.next();
			if (field.getSubTrees().size() > 0) {
				keySet.add(field.getHead());
			}
		}

		Iterator<String> keyItr = keySet.iterator();
		Map<String, ResultSetMappingConfiguration> compositeObjMap = new HashMap<String, ResultSetMappingConfiguration>();

		while (keyItr.hasNext()) {
			String key = keyItr.next();
			if (attributeMap.containsKey(key)) {
				Field attribute = attributeMap.get(key);
				Method compositeClassSetter = null;
				compositeClassSetter = findSetter(descriptors, targetClass.getName(), key);

				if (compositeClassSetter == null)
					continue;

				Map<String, Field> childAttributeMap = ReflectionHelp.getAllDeclaredFields(attribute.getType());
				ResultSetMappingConfiguration subconfigurations = mapColumnsToAttributes(attribute.getType(), resultSetMetaData, childAttributeMap, compositeFields.getTree(key), attribute.getName(), true);
				subconfigurations.setResultClass(attribute.getType());
				subconfigurations.setCompositeClassSetter(compositeClassSetter);
				compositeObjMap.put(attribute.getName(), subconfigurations);
			}
		}

		return compositeObjMap;
	}

	private Method findSetter(PropertyDescriptor[] descriptors, String className, String attributeName) {

		Method setter = null;
		
		for (int i = 0; i < descriptors.length; i++) {
			PropertyDescriptor descriptor = descriptors[i];
			if (descriptor.getDisplayName().equals(attributeName)) {
				setter = descriptor.getWriteMethod();
				break;
			}
		}

		if (setter == null) {
			QueryService.LOGGER.warn("Query Service : Fail to find a setter method of attribute ['{}'] from target class['{}'].", new Object[] { attributeName,
					className });
		}
		return setter;
	}

	private Object createObject(ResultSet resultSet, Class<?> targetClass, ResultSetMappingConfiguration config) throws InstantiationException,
			IllegalAccessException, InvocationTargetException {
		Object object = ReflectionHelp.newInstance(targetClass);

		String[] columnNames = config.getColumnNames();
		int[] columnTypes = config.getColumnTypes();
		Field[] attributes = config.getAttributes();
		Method[] setters = config.getSetters();

		for (int i = 0; i < attributes.length; i++) {
			if (columnTypes[i] == PROPERTY_NOT_FOUND) {
				continue;
			}

			int columnType = columnTypes[i];
			Object value = getValue(resultSet, columnType, columnNames[i], i + 1);
			setValue(attributes[i], setters[i], object, value);
		}
		return object;
	}

	private void setValue(Field field, Method setter, Object object, Object value) throws IllegalAccessException, InvocationTargetException {
		boolean valueSet = false;

		if (setter != null) {
			setter.invoke(object, new Object[] { value });
			valueSet = true;
		}

		if (!valueSet) {
			ReflectionHelp.setFieldValue(field, object, value);
		}
	}

	public T mapRow(ResultSet rs, int rowNum) throws SQLException {
		return this.mapRow(rs);
	}

	@SuppressWarnings({ "rawtypes" })
	public Map getColumnInfo() {
		return new ListOrderedMap();
	}

	protected void makeMeta(ResultSet resultSet) throws SQLException {
		ResultSetMetaData resultSetMetaData = resultSet.getMetaData();

		int columnCount = resultSetMetaData.getColumnCount();
		String[] columnKeys = new String[columnCount];
		String[] columnNames = new String[columnCount];
		int[] columnTypes = new int[columnCount];

		int[] columnPrecisions = new int[columnCount];
		int[] columnScales = new int[columnCount];

		Map<String, Field> attributeMap = new HashMap<String, Field>();
		if (this.targetClass != null && !this.targetClass.equals(HashMap.class)) {
			attributeMap = ReflectionHelp.getAllDeclaredFields(this.targetClass);
		}

		for (int i = 0; i < columnCount; i++) {
			String columnName = resultSetMetaData.getColumnLabel(i + 1);
			int columnType = resultSetMetaData.getColumnType(i + 1);

			columnNames[i] = columnName;
			columnKeys[i] = ColumnUtil.changeColumnName(this.mappingStyle, columnName);

			int dataType = SQLTypeTransfer.UNDEFINED;
			if (!(columnName == null || (this.targetClass == null || this.targetClass.equals(HashMap.class)) || getMappingInfo() == null)) {
				String attributeName = getMappingInfo().getMappingInfoAsMap().get(columnName.toLowerCase());

				if (attributeName == null) {
					attributeName = ColumnUtil.changeColumnName(this.mappingStyle, columnName);
				}
				
				Field attribute = attributeMap.get(attributeName);
				
				if (attribute == null) {
					continue;
				}

				dataType = SQLTypeTransfer.getSQLType(attribute.getType());
			}

			if (!((dataType == Types.VARCHAR && columnType == Types.CLOB) || (dataType == Types.VARBINARY && columnType == Types.BLOB))) {
				if (dataType != SQLTypeTransfer.UNDEFINED)
					columnType = dataType;
			}

			columnTypes[i] = columnType;
			try {
				columnPrecisions[i] = resultSetMetaData.getPrecision(i + 1);
			} catch (NumberFormatException ex) {
				columnPrecisions[i] = 0;
			}
			columnScales[i] = resultSetMetaData.getScale(i + 1);
		}

		this.mappingConfiguration = new ResultSetMappingConfiguration(columnCount, columnKeys, columnNames, columnTypes, columnPrecisions, columnScales);
		initialized = true;
	}

	public boolean isNeedColumnInfo() {
		return needColumnInfo;
	}

	public void setNeedColumnInfo(boolean needColumnInfo) {
		this.needColumnInfo = needColumnInfo;
	}

	@Override
	public int[] getRowsForPaths(TreePath[] arg0) {
		// TODO Auto-generated method stub
		return null;
	}

}

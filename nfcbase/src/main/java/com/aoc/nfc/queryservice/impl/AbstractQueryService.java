package com.aoc.nfc.queryservice.impl;

import java.io.File;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.velocity.app.Velocity;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import com.aoc.nfc.queryservice.SqlLoader;

public abstract class AbstractQueryService implements ApplicationContextAware,
		ResourceLoaderAware {

	protected SqlLoader sqlRepository = null;
	protected String propsFilename;
	protected ResourceLoader resourceLoader = null;

	public void setVelocityPropsFilename(String propsFilename) {
		this.propsFilename = propsFilename;
	}

	public SqlLoader getSqlRepository() {
		return sqlRepository;
	}

	public void setSqlRepository(SqlLoader sqlRepository) {
		this.sqlRepository = sqlRepository;
	}

	public void setApplicationContext(ApplicationContext context) {
	}

	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	public void afterPropertiesSet() {
		try {
			Velocity.addProperty("runtime.log.logsystem.class", "org.apache.velocity.runtime.log.NullLogSystem");
			Velocity.init();
			Resource[] resources = ((ResourcePatternResolver) resourceLoader).getResources(propsFilename);
			File velocityLogFile = resources[0].getFile();

			if (velocityLogFile.exists()) {
				Velocity.addProperty("runtime.log", velocityLogFile.getAbsolutePath());
				Velocity.init();
			} else {
				throw new Exception("Velocity log file doesn't exists.");
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected String getRunnableSQL(String sql, SqlParameterSource searchParams) {
		StringBuffer tempStatement = new StringBuffer(sql);
		SortedMap<Integer, String> replacementPositions = findTextReplacements(tempStatement);
		Iterator<Entry<Integer, String>> properties = replacementPositions.entrySet().iterator();
		int valueLengths = 0;
		try {
			while (properties.hasNext()) {
				Map.Entry<Integer, String> entry = properties.next();
				Integer pos = entry.getKey();
				String key = entry.getValue();
				Object replaceValue = searchParams.getValue(key);
				if (replaceValue == null) {
					throw new Exception("Query Service : Text replacement [" + entry.getValue() + "] has not been set.");
				}
				
				String value = replaceValue.toString();
				tempStatement.insert(pos.intValue() + valueLengths, value);
				valueLengths += value.length();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return tempStatement.toString();
	}

	protected SortedMap<Integer, String> findTextReplacements(StringBuffer sql) {
		TreeMap<Integer, String> textReplacements = new TreeMap<Integer, String>();
		int startPos = 0;
		while ((startPos = sql.indexOf("{{", startPos)) > -1) {
			int endPos = sql.indexOf("}}", startPos);
			String replacementKey = sql.substring(startPos + 2, endPos);
			sql.replace(startPos, endPos + 2, "");
			textReplacements.put(new Integer(startPos), replacementKey);
		}
		return textReplacements;
	}

	protected boolean isVelocity(String sql) {
		return ((sql.indexOf("#if") > -1 || sql.indexOf("#foreach") > -1) && sql
				.indexOf("#end") > -1);
	}

	protected void containesQueryId(String queryId) {
		if (!getSqlRepository().hasQuery(queryId)) {
			System.out.println("Query Service : Fail to find queryId [" + queryId + "] in query mappings.");
		}
	}
}

package com.aoc.nfc.queryservice.impl.config.loader;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.util.ResourceUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.aoc.nfc.queryservice.QueryService;
import com.aoc.nfc.queryservice.impl.config.info.DefaultMappingInfo;
import com.aoc.nfc.queryservice.impl.config.info.DefaultQueryInfo;
import com.aoc.nfc.queryservice.impl.util.StringUtil;

public class MappingXMLLoader extends AbstractSqlLoader implements ResourceLoaderAware {

	private Watcher watcher; 
	private ResourceLoader resourceLoader = null;
	private String mappingFiles = "";

	public void setMappingFiles(String mappingFiles) {
		this.mappingFiles = mappingFiles;
	}

	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	public void loadMappings() {
		try {
			boolean isDynamic = setUpWatcher();
			DocumentBuilder builder = getBuilder();
			loadSQLDefinitions(builder, isDynamic);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		QueryService.LOGGER.info("Query Service : There are {} defined queries in all configuration files.", countQuery());
	}

	private boolean setUpWatcher() {
		boolean isDynamic = false;
		if (getDynamicReload() > 0)
			isDynamic = true;

		if (isDynamic) {
			watcher = new Watcher(this);
			watcher.setRefreshRate(getDynamicReload());
		}

		return isDynamic;
	}

	private DocumentBuilder getBuilder() throws ParserConfigurationException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		DocumentBuilder builder = factory.newDocumentBuilder();
		return builder;
	}

	private void loadSQLDefinitions(DocumentBuilder builder, boolean isDynamic) throws SAXException, IOException {
		List<String> files = StringUtil.getTokens(this.mappingFiles);
		String location = "";

		for (int i = 0; i < files.size(); i++) {
			location = files.get(i).trim();
			if (resourceLoader instanceof ResourcePatternResolver) {
				try {
					Resource[] resources = ((ResourcePatternResolver) resourceLoader).getResources(location);
					int loadCount = loadSQLDefinitions(builder, isDynamic, resources);
					QueryService.LOGGER.debug("Loaded {} sql definitions from location pattern [{}]", new Object[] { loadCount, location });
				} catch (IOException ex) {
					ex.printStackTrace();
				}
			} 
//			else {
//				// Can only load single resources by
//				// absolute URL.
//				Resource resource = resourceLoader.getResource(location);
//				int loadCount = loadSQLDefinitions(builder, isDynamic, new Resource[] { resource });
//				QueryService.LOGGER.debug("Loaded {} sql definitions from location [{}]", new Object[] { loadCount, location });
//			}
		}

		if (isDynamic) {
			watcher.start();

			QueryService.LOGGER.debug("Query Service : Watcher is started...");
		}
	}

	private void addToWatcher(boolean dynamicSqlLoad, Resource resource)
			throws IOException {
		if (dynamicSqlLoad && !ResourceUtils.isJarURL(resource.getURL()))
			watcher.addResource(resource);
	}

	private int loadSQLDefinitions(DocumentBuilder builder, boolean dynamicSqlLoad, Resource[] resources) throws SAXException, IOException {
		int loadCount = 0;
		for (int i = 0; i < resources.length; i++) {
			loadCount += buildSQLMap(builder, resources[i]);
			addToWatcher(dynamicSqlLoad, resources[i]);
		}
		return loadCount;
	}

	private int buildSQLMap(DocumentBuilder builder, Resource resource)
			throws SAXException, IOException {
		int successCount = 0;

		try {
			Document rootConfig = builder.parse(resource.getInputStream());

			NodeList tableMappingConfig = rootConfig.getElementsByTagName("table-mapping");

			if (tableMappingConfig.getLength() > 0) {
				NodeList tables = ((Element) tableMappingConfig.item(0)).getElementsByTagName("table");

				for (int i = 0; i < tables.getLength(); i++) {
					DefaultMappingInfo mappingInfo = new DefaultMappingInfo();
					mappingInfo.configure((Element) tables.item(i));
					putTableMappingInfo(mappingInfo.getClassName(), mappingInfo);
				}
			}

			NodeList queriesConfig = rootConfig.getElementsByTagName("queries");

			if (queriesConfig.getLength() > 0) {
				NodeList queries = ((Element) queriesConfig.item(0)).getElementsByTagName("query");

				for (int i = 0; i < queries.getLength(); i++) {
					DefaultQueryInfo queryInfo = new DefaultQueryInfo();
					queryInfo.configure((Element) queries.item(i));
					putQueryMappingInfo(queryInfo.getQueryId(), queryInfo);
					++successCount;
					incrementQueryCount();
				}
			}
		} catch (SAXParseException ex) {
			Object[] args = { resource.getFilename(), ex.getSystemId(),
					ex.getMessage(), ex.getLineNumber(), ex.getSystemId() };
			QueryService.LOGGER.error(
					"Query Service : Fail to configure mapping xml file [{}]. {} is invalid.\n"
							+ "Cause - [{}]\n"
							+ "Please confirm the {} line in {}.", args, ex);

			if (!isSkipError())
				throw ex;
		}

		return successCount;
	}

	private int clearResultMap(DocumentBuilder builder, Resource resource)
			throws SAXException, IOException {
		int successCount = 0;
		try {
			Document rootConfig = builder.parse(resource.getInputStream());

			NodeList queriesConfig = rootConfig.getElementsByTagName("queries");

			if (queriesConfig.getLength() > 0) {
				NodeList queries = ((Element) queriesConfig.item(0))
						.getElementsByTagName("query");

				for (int i = 0; i < queries.getLength(); i++) {
					DefaultQueryInfo queryInfo = new DefaultQueryInfo();
					queryInfo.configure((Element) queries.item(i));
					if (getQueryResultMappings().containsKey(
							queryInfo.getQueryId())) {
						getQueryResultMappings().remove(queryInfo.getQueryId());
						++successCount;
					}
				}
			}
		} catch (SAXParseException ex) {
			Object[] args = { resource.getFilename(), ex.getSystemId(),
					ex.getMessage(), ex.getLineNumber(), ex.getSystemId() };
			QueryService.LOGGER.error(
					"Query Service : Fail to clear queryResultMapping [{}]. {} is invalid.\n"
							+ "Cause - [{}]\n"
							+ "Please confirm the {} line in {}.", args, ex);

			if (!isSkipError())
				throw ex;
		}

		return successCount;
	}

	class Watcher extends Thread {

		private final int scanRate = 10;

		private final Hashtable<Resource, Long> resources = new Hashtable<Resource, Long>();

		private boolean done = false;

		private long refreshRate = 0;

		public Watcher(Object subscriber) {
			setDaemon(true);
			setPriority(Thread.MIN_PRIORITY);
		}

		public void setRefreshRate(long refresh) {
			if (refresh > scanRate)
				this.refreshRate = refresh;
			else
				this.refreshRate = scanRate;
		}

		/**
		 * @return the refresh rate, in seconds, of this watcher
		 */
		public long getRefreshRate() {
			return refreshRate;
		}

		public void addResource(Resource resource) throws IOException {
			resources
					.put(resource, new Long(resource.getFile().lastModified()));

			QueryService.LOGGER.info("appended {} file for monitoring",
					resource.getFilename());
		}

		public void run() {
			try {
				while (!done) {
					synchronized (this) {
						boolean modificationChk = false;
						Enumeration<Resource> en = resources.keys();
						while (en.hasMoreElements()) {
							try {
								Resource resource = en.nextElement();
								long modified = resources.get(resource).longValue();

								if (!resource.exists()) {
									resources.remove(resource);
									continue;
								}

								if (resource.getFile().getAbsoluteFile().lastModified() > modified) {
									resources.put(resource, new Long(resource.getFile().lastModified()));
									// rebuildSQLMap(builder);
									modificationChk = true;
									continue;
								}
							} catch (Exception ex) {
								QueryService.LOGGER.error("Query Service : Fail to check whether mapping XML file is modified.", ex);
							}
						}

						try {
							if (modificationChk)
								rebuildSQLMap(getBuilder());
						} catch (Exception ex) {
							QueryService.LOGGER.error("Query Service : Fail to rebuild Query Mapping.", ex);
						}
						sleep(getRefreshRate());
					}
				}
			} catch (InterruptedException ex) {
				QueryService.LOGGER.error("Query Service : Fail to run Watcher.", ex);
			}
		}

		private void rebuildSQLMap(DocumentBuilder builder) {
			QueryService.LOGGER.info("Query Service : Watcher rebuilds Query Mapping.....");
			clearMappings();
			Enumeration<Resource> en = resources.keys();
			while (en.hasMoreElements()) {
				Resource resource = en.nextElement();
				try {
					buildSQLMap(builder, resource);
					clearResultMap(builder, resource);
				} catch (Exception ex) {
					QueryService.LOGGER.error("Query Service : Error Query Mapping file : {}", resource.getFilename(), ex);
				}
				QueryService.LOGGER.info("Query Service : Rebuild Query Mapping file : {}", resource.getFilename());
			}

		}

		public void setDone() {
			done = true;
		}
	}

	public void destroy() {
		clearMappings();
		getNullchecks().clear();
		if (watcher != null)
			watcher.setDone();
	}
}

package com.aoc.nfc.queryservice.config;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.aoc.nfc.queryservice.QueryService;
import com.aoc.nfc.queryservice.SqlLoader;
import com.aoc.nfc.queryservice.impl.QueryServiceImpl;
import com.aoc.nfc.queryservice.impl.config.loader.MappingXMLLoader;
import com.aoc.nfc.queryservice.impl.jdbc.CustomJdbcTemplate;

@Configuration
public class QueryConfig { 

	@Autowired
	DataSource dataSource;


	@Bean
	public QueryService queryService() {
		QueryServiceImpl queryService = new QueryServiceImpl();
		queryService.setSqlRepository(sqlRepository());
		queryService.setJdbcTemplate(jdbcTemplate());
		return queryService;
	}

	@Bean
	public CustomJdbcTemplate jdbcTemplate() {
		CustomJdbcTemplate jdbcTemplate = new CustomJdbcTemplate();
		jdbcTemplate.setDataSource(dataSource);
		return jdbcTemplate;
	}

	@Bean
	public SqlLoader sqlRepository() {
		Map<String, String> map = new ConcurrentHashMap<String, String>();
		map.put("VARCHAR", "");
		MappingXMLLoader sqlLoader = new MappingXMLLoader();
		sqlLoader.setMappingFiles("classpath:sql/query/mapping-*.xml");
		sqlLoader.setNullchecks(map);
		sqlLoader.setSkipError(true);
		return sqlLoader;
	}
}

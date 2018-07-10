package com.aoc.nfc.queryservice;

import java.sql.ResultSet;
import java.sql.SQLException;

public interface ResultSetMapper {
	Object mapRow(ResultSet resultSet) throws SQLException;
}

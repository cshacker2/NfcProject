package com.aoc.nfc.queryservice;

import org.springframework.jdbc.core.namedparam.SqlParameterSource;

public interface DynamicSqlParameterSource extends SqlParameterSource {
    void addSqlType(String name, int sqlType);
}

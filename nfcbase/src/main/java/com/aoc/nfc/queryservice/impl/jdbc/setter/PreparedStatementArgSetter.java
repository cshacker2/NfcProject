package com.aoc.nfc.queryservice.impl.jdbc.setter;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.springframework.jdbc.core.ParameterDisposer;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.SqlTypeValue;
import org.springframework.jdbc.core.StatementCreatorUtils;

public class PreparedStatementArgSetter implements PreparedStatementSetter, ParameterDisposer {

	private final Object[] args;

	public PreparedStatementArgSetter(Object[] args) {
		this.args = args;
	}

	public void setValues(PreparedStatement ps) throws SQLException {
		if (this.args != null) {
			for (int i = 0; i < this.args.length; i++) {
				StatementCreatorUtils.setParameterValue(ps, i + 1,
						SqlTypeValue.TYPE_UNKNOWN, null, this.args[i]);
			}
		}
	}

	public void cleanupParameters() {
		StatementCreatorUtils.cleanupParameters(this.args);
	}

}

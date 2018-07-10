package com.aoc.nfc.queryservice.impl.jdbc.setter;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Collection;
import java.util.Iterator;

import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.jdbc.core.ParameterDisposer;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.SqlTypeValue;
import org.springframework.jdbc.core.StatementCreatorUtils;

public class PreparedStatementArgTypeSetter implements PreparedStatementSetter,
		ParameterDisposer {

	private final Object[] args;

	private final int[] argTypes;

	public PreparedStatementArgTypeSetter(Object[] args, int[] argTypes) {
		if (args != null && argTypes == null) {
			throw new InvalidDataAccessApiUsageException("args != null && argTypes == null");
		}

		if (args == null && argTypes != null) {
			throw new InvalidDataAccessApiUsageException("args == null && argTypes != null");
		}

		if (args != null && args.length != argTypes.length) {
			throw new InvalidDataAccessApiUsageException("args != null && args.length != argTypes.length");
		}
		this.args = args;
		this.argTypes = argTypes;
	}

	public void setValues(PreparedStatement ps) throws SQLException {
		int argIndx = 1;
		if (this.args != null) {
			if (this.argTypes == null) {
				for (int i = 0; i < this.args.length; i++) {
					StatementCreatorUtils.setParameterValue(ps, i + 1, SqlTypeValue.TYPE_UNKNOWN, null, this.args[i]);
				}
			} else {
				for (int i = 0; i < this.args.length; i++) {
					Object arg = this.args[i];
					if (arg instanceof Collection<?> && this.argTypes[i] != Types.ARRAY) {
						Collection<?> entries = (Collection<?>) arg;
						
						for (Iterator<?> it = entries.iterator(); it.hasNext();) {
							Object entry = it.next();
							StatementCreatorUtils.setParameterValue(ps, argIndx++, this.argTypes[i], null, entry);
						}
						
					} else {
						StatementCreatorUtils.setParameterValue(ps, argIndx++, this.argTypes[i], null, arg);
					}
				}
			}
		}
	}

	public void cleanupParameters() {
		StatementCreatorUtils.cleanupParameters(this.args);
	}
}

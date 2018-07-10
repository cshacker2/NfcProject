package com.aoc.nfc.queryservice.exception;

public class QueryException extends Exception {

	private static final long serialVersionUID = 1L;

	private String sqlErrorCode = "";
	private String sqlErrorMessage = "";

	public QueryException(String message) {
		super(message);
	}

	public QueryException(String message, Throwable exception) {
		super(message, exception);
	}

	public String getSqlErrorCode() {
		return sqlErrorCode;
	}

	public void setSqlErrorCode(String sqlErrorCode) {
		this.sqlErrorCode = sqlErrorCode;
	}

	public String getSqlErrorMessage() {
		return sqlErrorMessage;
	}

	public void setSqlErrorMessage(String sqlErrorMessage) {
		this.sqlErrorMessage = sqlErrorMessage;
	}
}

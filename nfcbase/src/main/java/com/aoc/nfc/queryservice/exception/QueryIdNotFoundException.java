package com.aoc.nfc.queryservice.exception;

public class QueryIdNotFoundException extends QueryException {
	private static final long serialVersionUID = 1L;

	public QueryIdNotFoundException(String message) {
		super(message);
	}

	public QueryIdNotFoundException(String message, Throwable exception) {
		super(message, exception);
	}
}

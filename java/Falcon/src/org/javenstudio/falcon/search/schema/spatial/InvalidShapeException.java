package org.javenstudio.falcon.search.schema.spatial;

public class InvalidShapeException extends Exception {
	private static final long serialVersionUID = 1L;

	public InvalidShapeException(String reason, Throwable cause) {
		super(reason, cause);
	}

	public InvalidShapeException(String reason) {
		super(reason);
	}
	
}
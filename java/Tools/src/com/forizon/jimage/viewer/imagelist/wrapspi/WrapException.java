package com.forizon.jimage.viewer.imagelist.wrapspi;

public class WrapException extends Exception {
	private static final long serialVersionUID = 1L;

	public WrapException(String message, Throwable cause) {
        super(message, cause);
    }

    public WrapException(String message) {
        super(message);
    }

    public WrapException(Throwable cause) {
        super(cause);
    }

    public WrapException() {
        super();
    }
}

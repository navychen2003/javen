package com.forizon.jimage.viewer.imagelist;

/**
 *
 * @author David
 */
public class RepresentationException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public RepresentationException(String message, Throwable cause) {
        super(message, cause);
    }

    public RepresentationException(String message) {
        super(message);
    }

    public RepresentationException(Throwable cause) {
        super(cause);
    }

    public RepresentationException() {
        super();
    }
}

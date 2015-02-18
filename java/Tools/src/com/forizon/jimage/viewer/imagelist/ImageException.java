package com.forizon.jimage.viewer.imagelist;

/**
 *
 * @author David
 */
public class ImageException extends Exception {
	private static final long serialVersionUID = 1L;

	public ImageException(String message, Throwable cause) {
        super(message, cause);
    }

    public ImageException(String message) {
        super(message);
    }

    public ImageException(Throwable cause) {
        super(cause);
    }

    public ImageException() {
        super();
    }
}

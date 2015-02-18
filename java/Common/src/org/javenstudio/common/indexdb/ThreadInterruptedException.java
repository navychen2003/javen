package org.javenstudio.common.indexdb;

/**
 * Thrown by indexdb on detecting that Thread.interrupt() had
 * been called.  Unlike Java's InterruptedException, this
 * exception is not checked..
 */
public final class ThreadInterruptedException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public ThreadInterruptedException(InterruptedException ie) {
		super(ie);
	}
	
}

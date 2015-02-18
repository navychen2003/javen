package org.javenstudio.raptor.bigdb.ipc;

/**
 * An interface for calling out of RPC for error conditions.
 */
public interface DBRPCErrorHandler {
	/**
	 * Take actions on the event of an OutOfMemoryError.
	 * @param e the throwable
	 * @return if the server should be shut down
	 */
	public boolean checkOOME(final Throwable e) ;
}


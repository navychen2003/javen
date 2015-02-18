package org.javenstudio.lightning.core;

/**
 * Used to request notification when the core is closed.
 * <p/>
 * Call {@link Core#addCloseHook(CoreCloseHook)} during the {@link CoreAware#inform(Core)} 
 * method to add a close hook to your object.
 * <p/>
 * The close hook can be useful for releasing objects related to the request handler 
 * (for instance, if you have a JDBC DataSource or something like that)
 */
public interface CoreCloseHook {

	/**
	 * Method called when the given Core object is closing / shutting down 
	 * but before the update handler and searcher(s) are actually closed
	 * <br />
	 * <b>Important:</b> Keep the method implementation as short as possible. 
	 * If it were to use any heavy i/o , network connections -
	 * it might be a better idea to launch in a separate Thread so as to not 
	 * to block the process of shutting down a given Core instance.
	 *
	 * @param core Core object that is shutting down / closing
	 */
	public void preClose(Core core);

	/**
	 * Method called when the given Core object has been shut down and 
	 * update handlers and searchers are closed
	 * <br/>
	 * Use this method for post-close clean up operations e.g. deleting 
	 * the index from disk.
	 * <br/>
	 * <b>The core's passed to the method is already closed and therefore, 
	 * it's update handler or searcher should *NOT* be used</b>
	 *
	 * <b>Important:</b> Keep the method implementation as short as possible. 
	 * If it were to use any heavy i/o , network connections -
	 * it might be a better idea to launch in a separate Thread so as to 
	 * not to block the process of shutting down a given Core instance.
	 */
	public void postClose(Core core);
	
}

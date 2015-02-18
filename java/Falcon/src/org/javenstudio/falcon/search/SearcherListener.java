package org.javenstudio.falcon.search;

import org.javenstudio.common.indexdb.search.IndexSearcher;
import org.javenstudio.falcon.util.NamedListPlugin;

public interface SearcherListener extends NamedListPlugin {

	public void postCommit();
	public void postSoftCommit();

	/** 
	 * The searchers passed here are only guaranteed to be valid for the duration
	 * of this method call, so care should be taken not to spawn threads or asynchronous
	 * tasks with references to these searchers.
	 * <p/>
	 * Implementations should add the {@link EventParams#EVENT} parameter and 
	 * set it to a value of either:
	 * <ul>
	 * <li>{@link EventParams#FIRST_SEARCHER} - First Searcher event</li>
	 * <li>{@link EventParams#NEW_SEARCHER} - New Searcher event</li>
	 * </ul>
	 *
	 * Sample:
	 * <pre>
     *	if (currentSearcher != null) {
     *  	nlst.add(CommonParams.EVENT, CommonParams.NEW_SEARCHER);
     *	} else {
     * 		nlst.add(CommonParams.EVENT, CommonParams.FIRST_SEARCHER);
     *	}
     *
     * </pre>
     *
     * @see AbstractEventListener#addEventParms(Searcher, NamedList) 
     *
     * @param newSearcher The new {@link Searcher} to use
     * @param currentSearcher The existing {@link IndexSearcher}.  
     * null if this is a firstSearcher event.
     */
	public void newSearcher(Searcher newSearcher, Searcher currentSearcher);
	
}

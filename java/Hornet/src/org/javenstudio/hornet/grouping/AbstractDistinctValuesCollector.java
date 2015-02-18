package org.javenstudio.hornet.grouping;

import java.io.IOException;
import java.util.List;

import org.javenstudio.common.indexdb.IScorer;
import org.javenstudio.common.indexdb.search.Collector;

/**
 * A second pass grouping collector that keeps track of distinct values 
 * for a specified field for the top N group.
 *
 */
public abstract class AbstractDistinctValuesCollector<GC extends GroupCount<?>> 
		extends Collector {

	/**
	 * Returns all unique values for each top N group.
	 *
	 * @return all unique values for each top N group
	 */
	public abstract List<GC> getGroups();

	public boolean acceptsDocsOutOfOrder() {
		return true;
	}

	public void setScorer(IScorer scorer) throws IOException {
		// do nothing
	}

}

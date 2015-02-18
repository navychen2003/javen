package org.javenstudio.hornet.grouping;

import java.io.IOException;
import java.util.Collection;

import org.javenstudio.common.indexdb.IScorer;
import org.javenstudio.common.indexdb.search.Collector;

/**
 * A collector that collects all groups that match the
 * query. Only the group value is collected, and the order
 * is undefined.  This collector does not determine
 * the most relevant document of a group.
 *
 * <p/>
 * This is an abstract version. Concrete implementations define
 * what a group actually is and how it is internally collected.
 *
 */
public abstract class AbstractAllGroupsCollector<GT> 
		extends Collector {

	/**
	 * Returns the total number of groups for the executed search.
	 * This is a convenience method. The following code snippet 
	 * has the same effect: <pre>getGroups().size()</pre>
	 *
	 * @return The total number of groups for the executed search
	 */
	public int getGroupCount() {
		return getGroups().size();
	}

	/**
	 * Returns the group values
	 * <p/>
	 * This is an unordered collections of group values. 
	 * For each group that matched the query there is a {@link BytesRef}
	 * representing a group value.
	 *
	 * @return the group values
	 */
	public abstract Collection<GT> getGroups();

	// Empty not necessary
	public void setScorer(IScorer scorer) throws IOException { 
		// do nothing
	}

	public boolean acceptsDocsOutOfOrder() {
		return true;
	}
	
}
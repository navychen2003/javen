package org.javenstudio.falcon.search.facet;

import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.falcon.util.NamedList;

public abstract class FacetCollector {

	/*** return true to stop collection */
	public abstract boolean collect(BytesRef term, int count);
	
	public abstract NamedList<Integer> getFacetCounts();
	
}

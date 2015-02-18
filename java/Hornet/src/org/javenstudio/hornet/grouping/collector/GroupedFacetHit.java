package org.javenstudio.hornet.grouping.collector;

import org.javenstudio.common.indexdb.util.BytesRef;

public class GroupedFacetHit {

	protected final BytesRef mGroupValue;
	protected final BytesRef mFacetValue;

	public GroupedFacetHit(BytesRef groupValue, BytesRef facetValue) {
		mGroupValue = groupValue;
		mFacetValue = facetValue;
	}
	
	public final BytesRef getGroupValue() { return mGroupValue; }
	public final BytesRef getFacetValue() { return mFacetValue; }
	
}

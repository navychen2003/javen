package org.javenstudio.hornet.grouping.collector;

import java.util.ArrayList;
import java.util.List;

import org.javenstudio.common.indexdb.IDocTermsIndex;
import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.hornet.grouping.AbstractGroupFacetCollector;
import org.javenstudio.hornet.search.cache.FieldCache;
import org.javenstudio.hornet.util.SentinelIntSet;

/**
 * An implementation of {@link AbstractGroupFacetCollector} that computes grouped facets 
 * based on the indexed terms from the {@link FieldCache}.
 *
 */
public abstract class TermGroupFacetCollector extends AbstractGroupFacetCollector {

	protected final List<GroupedFacetHit> mGroupedFacetHits;
	protected final SentinelIntSet mSegmentGroupedFacetHits;
	protected final BytesRef mSpare = new BytesRef();

	protected IDocTermsIndex mGroupFieldTermsIndex;

	/**
	 * Factory method for creating the right implementation based on the fact 
	 * whether the facet field contains multiple tokens per documents.
	 *
	 * @param groupField The group field
	 * @param facetField The facet field
	 * @param facetFieldMultivalued Whether the facet field has multiple tokens per document
	 * @param facetPrefix The facet prefix a facet entry should start with to be included.
	 * @param initialSize The initial allocation size of the internal int set and 
	 * group facet list which should roughly
	 * match the total number of expected unique groups. Be aware that the heap usage is
	 * 4 bytes * initialSize.
	 * @return <code>TermGroupFacetCollector</code> implementation
	 */
	public static TermGroupFacetCollector createTermGroupFacetCollector(String groupField,
			String facetField, boolean facetFieldMultivalued, BytesRef facetPrefix, int initialSize) {
		if (facetFieldMultivalued) {
			return new MultiTermGroupFacetCollector(groupField, facetField, facetPrefix, initialSize);
		} else {
			return new SingleTermGroupFacetCollector(groupField, facetField, facetPrefix, initialSize);
		}
	}

	protected TermGroupFacetCollector(String groupField, String facetField, 
			BytesRef facetPrefix, int initialSize) {
		super(groupField, facetField, facetPrefix);
		
		mGroupedFacetHits = new ArrayList<GroupedFacetHit>(initialSize);
		mSegmentGroupedFacetHits = new SentinelIntSet(initialSize, -1);
	}

}

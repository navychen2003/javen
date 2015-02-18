package org.javenstudio.hornet.grouping.collector;

import java.io.IOException;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.IDocTermsIndex;
import org.javenstudio.common.indexdb.ISort;
import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.hornet.grouping.AbstractFirstPassGroupingCollector;
import org.javenstudio.hornet.search.cache.FieldCache;

/**
 * Concrete implementation of {@link AbstractFirstPassGroupingCollector} that groups based on
 * field values and more specifically uses {@link DocTermsIndex}
 * to collect groups.
 *
 */
public class TermFirstPassGroupingCollector 
		extends AbstractFirstPassGroupingCollector<BytesRef> {

	private final BytesRef mScratchBytesRef = new BytesRef();
	private IDocTermsIndex mIndex;

	private String mGroupField;

	/**
	 * Create the first pass collector.
	 *
	 *  @param groupField The field used to group
	 *    documents. This field must be single-valued and
	 *    indexed (FieldCache is used to access its value
	 *    per-document).
	 *  @param groupSort The {@link Sort} used to sort the
	 *    groups.  The top sorted document within each group
	 *    according to groupSort, determines how that group
	 *    sorts against other groups.  This must be non-null,
	 *    ie, if you want to groupSort by relevance use
	 *    Sort.RELEVANCE.
	 *  @param topNGroups How many top groups to keep.
	 *  @throws IOException When I/O related errors occur
	 */
	public TermFirstPassGroupingCollector(String groupField, ISort groupSort, 
			int topNGroups) throws IOException {
		super(groupSort, topNGroups);
		mGroupField = groupField;
	}

	@Override
	protected BytesRef getDocGroupValue(int doc) {
		final int ord = mIndex.getOrd(doc);
		return ord == 0 ? null : mIndex.lookup(ord, mScratchBytesRef);
	}

	@Override
	protected BytesRef copyDocGroupValue(BytesRef groupValue, BytesRef reuse) {
		if (groupValue == null) {
			return null;
			
		} else if (reuse != null) {
			reuse.copyBytes(groupValue);
			return reuse;
			
		} else {
			return BytesRef.deepCopyOf(groupValue);
		}
	}

	@Override
	public void setNextReader(IAtomicReaderRef readerContext) throws IOException {
		super.setNextReader(readerContext);
		mIndex = FieldCache.DEFAULT.getTermsIndex(readerContext.getReader(), mGroupField);
	}
	
}

package org.javenstudio.falcon.search.facet;

import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.common.indexdb.util.CharsRef;
import org.javenstudio.common.indexdb.util.UnicodeUtil;
import org.javenstudio.falcon.util.NamedList;

//This collector expects facets to be collected in index order
public class IndexSortedCollector extends FacetCollector {
	
	protected final CharsRef mSpare = new CharsRef();
	protected final NamedList<Integer> mRes = new NamedList<Integer>();
	protected final int mMinCount;
	
	protected int mOffset;
	protected int mLimit;
	
	public IndexSortedCollector(int offset, int limit, int mincount) {
		mOffset = offset;
		mLimit = limit>0 ? limit : Integer.MAX_VALUE;
		mMinCount = mincount;
	}

	@Override
	public boolean collect(BytesRef term, int count) {
		if (count < mMinCount) 
			return false;

		if (mOffset > 0) {
			mOffset --;
			return false;
		}

		if (mLimit > 0) {
			UnicodeUtil.UTF8toUTF16(term, mSpare);
			mRes.add(mSpare.toString(), count);
			mLimit --;
		}

		return mLimit <= 0;
	}

	@Override
	public NamedList<Integer> getFacetCounts() {
		return mRes;
	}
	
}

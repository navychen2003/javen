package org.javenstudio.falcon.search.filter;

import java.io.IOException;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.IDocIdSet;
import org.javenstudio.common.indexdb.IDocIdSetIterator;
import org.javenstudio.common.indexdb.ISearcher;
import org.javenstudio.common.indexdb.search.DocIdSet;
import org.javenstudio.common.indexdb.util.Bits;
import org.javenstudio.hornet.query.ValueSource;
import org.javenstudio.hornet.query.ValueSourceContext;
import org.javenstudio.hornet.search.BitsFilteredDocIdSet;

/**
 * RangeFilter over a ValueSource.
 */
public class ValueSourceRangeFilter extends SearchFilter {
	
	private final ValueSource mValueSource;
	private final String mLowerVal;
	private final String mUpperVal;
	private final boolean mIncludeLower;
	private final boolean mIncludeUpper;

	public ValueSourceRangeFilter(ValueSource valueSource,
			String lowerVal, String upperVal, 
			boolean includeLower, boolean includeUpper) {
		mValueSource = valueSource;
		mLowerVal = lowerVal;
		mUpperVal = upperVal;
		mIncludeLower = lowerVal != null && includeLower;
		mIncludeUpper = upperVal != null && includeUpper;
	}

	public ValueSource getValueSource() { return mValueSource; }
	public String getLowerVal() { return mLowerVal; }
	public String getUpperVal() { return mUpperVal; }
	public boolean isIncludeLower() { return mIncludeLower; }
	public boolean isIncludeUpper() { return mIncludeUpper; }

	@Override
	public IDocIdSet getDocIdSet(final ValueSourceContext context, 
			final IAtomicReaderRef readerContext, Bits acceptDocs) throws IOException {
		return BitsFilteredDocIdSet.wrap(new DocIdSet() {
				@Override
				public IDocIdSetIterator iterator() throws IOException {
					return mValueSource.getValues(context, readerContext).getRangeScorer(
							readerContext.getReader(), mLowerVal, mUpperVal, mIncludeLower, mIncludeUpper);
				}
				
				@Override
				public Bits getBits() {
					return null;  // don't use random access
				}
			}, acceptDocs);
	}

	@Override
	public void createWeight(ValueSourceContext context, ISearcher searcher) throws IOException {
		mValueSource.createWeight(context, searcher);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof ValueSourceRangeFilter)) 
			return false;
		
		ValueSourceRangeFilter other = (ValueSourceRangeFilter)o;

		if (!this.mValueSource.equals(other.mValueSource) || 
				this.mIncludeLower != other.mIncludeLower || 
				this.mIncludeUpper != other.mIncludeUpper) { 
			return false; 
		}
		
		if (equals(this.mLowerVal, other.mLowerVal)) 
			return false;
		
		if (equals(this.mUpperVal, other.mUpperVal)) 
			return false;
		
		return true;
	}

	private boolean equals(Object obj1, Object obj2) { 
		return obj1 != null ? !obj1.equals(obj2) : obj2 != null;
	}
	
	@Override
	public int hashCode() {
		int h = mValueSource.hashCode();
		h += mLowerVal != null ? mLowerVal.hashCode() : 0x572353db;
		h = (h << 16) | (h >>> 16);  // rotate to distinguish lower from upper
		h += (mUpperVal != null ? (mUpperVal.hashCode()) : 0xe16fe9e7);
		h += (mIncludeLower ? 0xdaa47978 : 0) + (mIncludeUpper ? 0x9e634b57 : 0);
		return h;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("frange(");
		sb.append(mValueSource);
		sb.append("):");
		sb.append(mIncludeLower ? '[' : '{');
		sb.append(mLowerVal == null ? "*" : mLowerVal);
		sb.append(" TO ");
		sb.append(mUpperVal == null ? "*" : mUpperVal);
		sb.append(mIncludeUpper ? ']' : '}');
		return sb.toString();
	}
	
}

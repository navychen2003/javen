package org.javenstudio.hornet.search.query;

import java.util.Comparator;

import org.javenstudio.common.indexdb.index.term.TermsEnum;
import org.javenstudio.common.indexdb.util.BytesRef;

/**
 * Subclass of FilteredTermEnum for enumerating all terms that match the
 * specified range parameters.
 * <p>Term enumerations are always ordered by
 * {@link #getComparator}.  Each term in the enumeration is
 * greater than all that precede it.</p>
 */
public class TermRangeTermsEnum extends FilteredTermsEnum {

	private final Comparator<BytesRef> mTermComp;
	
	private final boolean mIncludeLower;
	private final boolean mIncludeUpper;
	private final BytesRef mLowerBytesRef;
	private final BytesRef mUpperBytesRef;

	/**
	 * Enumerates all terms greater/equal than <code>lowerTerm</code>
	 * but less/equal than <code>upperTerm</code>. 
	 * 
	 * If an endpoint is null, it is said to be "open". Either or both 
	 * endpoints may be open.  Open endpoints may not be exclusive 
	 * (you can't select all but the first or last term without 
	 * explicitly specifying the term to exclude.)
	 * 
	 * @param tenum
	 *          TermsEnum to filter
	 * @param lowerTerm
	 *          The term text at the lower end of the range
	 * @param upperTerm
	 *          The term text at the upper end of the range
	 * @param includeLower
	 *          If true, the <code>lowerTerm</code> is included in the range.
	 * @param includeUpper
	 *          If true, the <code>upperTerm</code> is included in the range.
	 */
	public TermRangeTermsEnum(TermsEnum tenum, BytesRef lowerTerm, BytesRef upperTerm, 
			boolean includeLower, boolean includeUpper) {
		super(tenum);

		// do a little bit of normalization...
		// open ended range queries should always be inclusive.
		if (lowerTerm == null) {
			mLowerBytesRef = new BytesRef();
			mIncludeLower = true;
		} else {
			mLowerBytesRef = lowerTerm;
			mIncludeLower = includeLower;
		}

		if (upperTerm == null) {
			mIncludeUpper = true;
			mUpperBytesRef = null;
		} else {
			mIncludeUpper = includeUpper;
			mUpperBytesRef = upperTerm;
		}

		setInitialSeekTerm(mLowerBytesRef);
		mTermComp = getComparator();
	}

	@Override
	protected AcceptStatus accept(BytesRef term) {
		if (!mIncludeLower && term.equals(mLowerBytesRef))
			return AcceptStatus.NO;
    
		// Use this field's default sort ordering
		if (mUpperBytesRef != null) {
			final int cmp = mTermComp.compare(mUpperBytesRef, term);
			
			/**
			 * if beyond the upper term, or is exclusive and this is equal to
			 * the upper term, break out
			 */
			if ((cmp < 0) || (!mIncludeUpper && cmp==0)) 
				return AcceptStatus.END;
		}

		return AcceptStatus.YES;
	}
	
}

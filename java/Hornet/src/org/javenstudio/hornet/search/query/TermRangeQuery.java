package org.javenstudio.hornet.search.query;

import java.io.IOException;

import org.javenstudio.common.indexdb.ITerms;
import org.javenstudio.common.indexdb.ITermsEnum;
import org.javenstudio.common.indexdb.index.term.TermsEnum;
import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.common.indexdb.util.StringHelper;

/**
 * A Query that matches documents within an range of terms.
 *
 * <p>This query matches the documents looking for terms that fall into the
 * supplied range according to {@link
 * Byte#compareTo(Byte)}. It is not intended
 * for numerical ranges; use {@link NumericRangeQuery} instead.
 *
 * <p>This query uses the {@link
 * MultiTermQuery#CONSTANT_SCORE_AUTO_REWRITE_DEFAULT}
 * rewrite method.
 * @since 2.9
 */
public class TermRangeQuery extends MultiTermQuery {
	
	private BytesRef mLowerTerm;
	private BytesRef mUpperTerm;
	private boolean mIncludeLower;
	private boolean mIncludeUpper;

	/**
	 * Constructs a query selecting all terms greater/equal than <code>lowerTerm</code>
	 * but less/equal than <code>upperTerm</code>. 
	 * 
	 * <p>
	 * If an endpoint is null, it is said 
	 * to be "open". Either or both endpoints may be open.  Open endpoints may not 
	 * be exclusive (you can't select all but the first or last term without 
	 * explicitly specifying the term to exclude.)
	 * 
	 * @param field The field that holds both lower and upper terms.
	 * @param lowerTerm
	 *          The term text at the lower end of the range
	 * @param upperTerm
	 *          The term text at the upper end of the range
	 * @param includeLower
	 *          If true, the <code>lowerTerm</code> is
	 *          included in the range.
	 * @param includeUpper
	 *          If true, the <code>upperTerm</code> is
	 *          included in the range.
	 */
	public TermRangeQuery(String field, BytesRef lowerTerm, BytesRef upperTerm, 
			boolean includeLower, boolean includeUpper) {
		super(field);
		mLowerTerm = lowerTerm;
		mUpperTerm = upperTerm;
		mIncludeLower = includeLower;
		mIncludeUpper = includeUpper;
	}

	/**
	 * Factory that creates a new TermRangeQuery using Strings for term text.
	 */
	public static TermRangeQuery newStringRange(String field, String lowerTerm, String upperTerm, 
			boolean includeLower, boolean includeUpper) {
		BytesRef lower = lowerTerm == null ? null : new BytesRef(lowerTerm);
		BytesRef upper = upperTerm == null ? null : new BytesRef(upperTerm);
		return new TermRangeQuery(field, lower, upper, includeLower, includeUpper);
	}

	/** Returns the lower value of this range query */
	public BytesRef getLowerTerm() { return mLowerTerm; }

	/** Returns the upper value of this range query */
	public BytesRef getUpperTerm() { return mUpperTerm; }
  
	/** Returns <code>true</code> if the lower endpoint is inclusive */
	public boolean includesLower() { return mIncludeLower; }
  
	/** Returns <code>true</code> if the upper endpoint is inclusive */
	public boolean includesUpper() { return mIncludeUpper; }
  
	@Override
	protected ITermsEnum getTermsEnum(ITerms terms) throws IOException {
		if (mLowerTerm != null && mUpperTerm != null && mLowerTerm.compareTo(mUpperTerm) > 0) 
			return TermsEnum.EMPTY;
    
		TermsEnum tenum = (TermsEnum)terms.iterator(null);
    
		if ((mLowerTerm == null || (mIncludeLower && mLowerTerm.getLength() == 0)) && mUpperTerm == null) 
			return tenum;
		
		return new TermRangeTermsEnum(tenum,
				mLowerTerm, mUpperTerm, mIncludeLower, mIncludeUpper);
	}

	/** Prints a user-readable version of this query. */
	@Override
	public String toString(String field) {
		StringBuilder buffer = new StringBuilder();
		if (!getFieldName().equals(field)) {
			buffer.append(getFieldName());
			buffer.append(":");
		}
		buffer.append(mIncludeLower ? '[' : '{');
		// TODO: all these toStrings for queries should just output the bytes, it might not be UTF-8!
		buffer.append(mLowerTerm != null ? ("*".equals(mLowerTerm.utf8ToString()) ? "\\*" : mLowerTerm.utf8ToString()) : "*");
		buffer.append(" TO ");
		buffer.append(mUpperTerm != null ? ("*".equals(mUpperTerm.utf8ToString()) ? "\\*" : mUpperTerm.utf8ToString()) : "*");
		buffer.append(mIncludeUpper ? ']' : '}');
		buffer.append(StringHelper.toBoostString(getBoost()));
		return buffer.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + (mIncludeLower ? 1231 : 1237);
		result = prime * result + (mIncludeUpper ? 1231 : 1237);
		result = prime * result + ((mLowerTerm == null) ? 0 : mLowerTerm.hashCode());
		result = prime * result + ((mUpperTerm == null) ? 0 : mUpperTerm.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		
		TermRangeQuery other = (TermRangeQuery) obj;
		if (mIncludeLower != other.mIncludeLower)
			return false;
		if (mIncludeUpper != other.mIncludeUpper)
			return false;
		
		if (mLowerTerm == null) {
			if (other.mLowerTerm != null)
				return false;
		} else if (!mLowerTerm.equals(other.mLowerTerm)) {
			return false;
		}
		
		if (mUpperTerm == null) {
			if (other.mUpperTerm != null)
				return false;
		} else if (!mUpperTerm.equals(other.mUpperTerm)) {
			return false;
		}
		
		return true;
	}

}

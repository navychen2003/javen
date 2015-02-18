package org.javenstudio.hornet.search.cache;

import java.io.IOException;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.IDocIdSet;
import org.javenstudio.common.indexdb.search.DocIdSet;
import org.javenstudio.common.indexdb.search.Filter;
import org.javenstudio.common.indexdb.util.Bits;
import org.javenstudio.hornet.search.BitsFilteredDocIdSet;

/**
 * A {@link Filter} that accepts all documents that have one or more values in a
 * given field. This {@link Filter} request {@link Bits} from the
 * {@link FieldCache} and build the bits if not present.
 */
public class FieldValueFilter extends Filter {
	
	private final String mField;
	private final boolean mNegate;

	/**
	 * Creates a new {@link FieldValueFilter}
	 * 
	 * @param field
	 *          the field to filter
	 */
	public FieldValueFilter(String field) {
		this(field, false);
	}

	/**
	 * Creates a new {@link FieldValueFilter}
	 * 
	 * @param field
	 *          the field to filter
	 * @param negate
	 *          iff <code>true</code> all documents with no value in the given
	 *          field are accepted.
	 */
	public FieldValueFilter(String field, boolean negate) {
		mField = field;
		mNegate = negate;
	}
  
	/**
	 * Returns the field this filter is applied on.
	 * @return the field this filter is applied on.
	 */
	public String getField() {
		return mField;
	}
  
	/**
	 * Returns <code>true</code> iff this filter is negated, otherwise <code>false</code> 
	 * @return <code>true</code> iff this filter is negated, otherwise <code>false</code>
	 */
	public boolean getNegate() {
		return mNegate; 
	}

	@Override
	public IDocIdSet getDocIdSet(IAtomicReaderRef context, Bits acceptDocs)
			throws IOException {
		final Bits docsWithField = FieldCache.DEFAULT.getDocsWithField(
				context.getReader(), mField);
		if (mNegate) {
			if (docsWithField instanceof Bits.MatchAllBits) 
				return null;
			
			return new FieldCacheDocIdSet(context.getReader().getMaxDoc(), acceptDocs) {
					@Override
					protected final boolean matchDoc(int doc) {
						return !docsWithField.get(doc);
					}
				};
		} else {
			if (docsWithField instanceof Bits.MatchNoBits) 
				return null;
			
			if (docsWithField instanceof DocIdSet) {
				// UweSays: this is always the case for our current impl - but who knows
				// :-)
				return BitsFilteredDocIdSet.wrap((DocIdSet) docsWithField, acceptDocs);
			}
			return new FieldCacheDocIdSet(context.getReader().getMaxDoc(), acceptDocs) {
					@Override
					protected final boolean matchDoc(int doc) {
						return docsWithField.get(doc);
					}
				};
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((mField == null) ? 0 : mField.hashCode());
		result = prime * result + (mNegate ? 1231 : 1237);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		FieldValueFilter other = (FieldValueFilter) obj;
		if (mField == null) {
			if (other.mField != null)
				return false;
		} else if (!mField.equals(other.mField))
			return false;
		if (mNegate != other.mNegate)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "FieldValueFilter [field=" + mField + ", negate=" + mNegate + "]";
	}

}

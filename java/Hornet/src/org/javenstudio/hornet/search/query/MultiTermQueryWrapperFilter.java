package org.javenstudio.hornet.search.query;

import java.io.IOException;

import org.javenstudio.common.indexdb.IAtomicReader;
import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.IDocIdSet;
import org.javenstudio.common.indexdb.IDocsEnum;
import org.javenstudio.common.indexdb.IFields;
import org.javenstudio.common.indexdb.ITerms;
import org.javenstudio.common.indexdb.ITermsEnum;
import org.javenstudio.common.indexdb.search.DocIdSet;
import org.javenstudio.common.indexdb.search.DocIdSetIterator;
import org.javenstudio.common.indexdb.search.Filter;
import org.javenstudio.common.indexdb.search.FixedBitSet;
import org.javenstudio.common.indexdb.util.Bits;
import org.javenstudio.hornet.search.OpenFixedBitSet;

/**
 * A wrapper for {@link MultiTermQuery}, that exposes its
 * functionality as a {@link Filter}.
 * <P>
 * <code>MultiTermQueryWrapperFilter</code> is not designed to
 * be used by itself. Normally you subclass it to provide a Filter
 * counterpart for a {@link MultiTermQuery} subclass.
 * <P>
 * For example, {@link TermRangeFilter} and {@link PrefixFilter} extend
 * <code>MultiTermQueryWrapperFilter</code>.
 * This class also provides the functionality behind
 * {@link MultiTermQuery#CONSTANT_SCORE_FILTER_REWRITE};
 * this is why it is not abstract.
 */
public class MultiTermQueryWrapperFilter<Q extends MultiTermQuery> extends Filter {
    
	protected final Q mQuery;

	/**
	 * Wrap a {@link MultiTermQuery} as a Filter.
	 */
	protected MultiTermQueryWrapperFilter(Q query) {
		mQuery = query;
	}
  
	@Override
	public String toString() {
		// query.toString should be ok for the filter, too, if the query boost is 1.0f
		return mQuery.toString();
	}

	@Override
	@SuppressWarnings({"rawtypes"})
	public final boolean equals(final Object o) {
		if (o == this) return true;
		if (o == null) return false;
		if (this.getClass().equals(o.getClass())) 
			return mQuery.equals(((MultiTermQueryWrapperFilter)o).mQuery);
		
		return false;
	}

	@Override
	public final int hashCode() {
		return mQuery.hashCode();
	}

	/** Returns the field name for this query */
	public final String getFieldName() { 
		return mQuery.getFieldName(); 
	}
  
	/**
	 * Returns a DocIdSet with documents that should be permitted in search
	 * results.
	 */
	@Override
	public IDocIdSet getDocIdSet(IAtomicReaderRef context, Bits acceptDocs) throws IOException {
		final IAtomicReader reader = context.getReader();
		final IFields fields = reader.getFields();
		if (fields == null) {
			// reader has no fields
			return DocIdSet.EMPTY_DOCIDSET;
		}

		final ITerms terms = fields.getTerms(mQuery.getFieldName());
		if (terms == null) {
			// field does not exist
			return DocIdSet.EMPTY_DOCIDSET;
		}

		final ITermsEnum termsEnum = mQuery.getTermsEnum(terms);
		assert termsEnum != null;
		
		if (termsEnum.next() != null) {
			// fill into a FixedBitSet
			final FixedBitSet bitSet = new OpenFixedBitSet(context.getReader().getMaxDoc());
			IDocsEnum docsEnum = null;
			do {
				// enumerator.term().toBytesString());
				docsEnum = termsEnum.getDocs(acceptDocs, docsEnum, 0);
				int docid;
				while ((docid = docsEnum.nextDoc()) != DocIdSetIterator.NO_MORE_DOCS) {
					bitSet.set(docid);
				}
			} while (termsEnum.next() != null);

			return bitSet;
		} else {
			return DocIdSet.EMPTY_DOCIDSET;
		}
	}
	
}

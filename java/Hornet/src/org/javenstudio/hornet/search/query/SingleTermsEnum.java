package org.javenstudio.hornet.search.query;

import org.javenstudio.common.indexdb.index.term.TermsEnum;
import org.javenstudio.common.indexdb.util.BytesRef;

/**
 * Subclass of FilteredTermsEnum for enumerating a single term.
 * <p>
 * For example, this can be used by {@link MultiTermQuery}s
 * that need only visit one term, but want to preserve
 * MultiTermQuery semantics such as {@link
 * MultiTermQuery#getRewriteMethod}.
 */
public final class SingleTermsEnum extends FilteredTermsEnum {
	
	private final BytesRef mSingleRef;
  
	/**
	 * Creates a new <code>SingleTermsEnum</code>.
	 * <p>
	 * After calling the constructor the enumeration is already pointing to the term,
	 * if it exists.
	 */
	public SingleTermsEnum(TermsEnum tenum, BytesRef termText) {
		super(tenum);
		mSingleRef = termText;
		setInitialSeekTerm(termText);
	}

	@Override
	protected AcceptStatus accept(BytesRef term) {
		return term.equals(mSingleRef) ? AcceptStatus.YES : AcceptStatus.END;
	}
  
}
